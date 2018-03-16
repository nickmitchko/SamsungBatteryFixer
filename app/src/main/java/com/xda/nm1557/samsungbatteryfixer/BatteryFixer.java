package com.xda.nm1557.samsungbatteryfixer;

import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

/**
 * Created by nmitchko on 3/16/18.
 */

public class BatteryFixer implements IXposedHookLoadPackage {

    private static boolean WAS_CHARGING = false;     // Whether System is charging
    private static boolean mCharging = false;
    private static boolean mCharged = false;

    private static final String PACKAGENAME = "com.android.systemui";     // Package to hook
    private static final String CLASSNAME = "com.android.systemui.statusbar.policy.BatteryControllerImpl";    // classname to hook (we use the battery controller)
    private static final String METHODNAME = "onReceive";    // Method to hook within the battery controller

    private static BatteryStatsManager BatteryUtil = null;


    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (lpparam.packageName.contains(PACKAGENAME)) {
            findAndHookMethod(CLASSNAME, lpparam.classLoader, METHODNAME, Context.class, Intent.class, new XC_MethodHook() {

                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    // this will be called before the battery was updated by the original method
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    // this will be called after the battery was updated by the original method
                    if (param.args.length == 2) {
                        if (param.args[1] != null) {
                            Intent intent = (Intent) param.args[1];
                            final String action = intent.getAction();
                            if (action != null) {
                                // Here is the trigger for the battery changed behavior
                                // I look for the fact that we become charging and then change the system parameters
                                if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
                                    // Current Status of the Battery Manager
                                    final int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS,
                                            BatteryManager.BATTERY_STATUS_UNKNOWN);
                                    int mLevel = (int) (100f
                                            * intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
                                            / intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100));
                                    if (mLevel >= 78) {
                                        if (BatteryUtil == null) {
                                            BatteryUtil = new BatteryStatsManager();
                                            XposedBridge.log("Samsung Battery Fixer: Hooked into battery (" + BatteryUtil.getBatteryCapacity() + "*" + BatteryUtil.getBATTERY_GAIN_MODIFIER() + ")");
                                        }
                                        mCharged = status == BatteryManager.BATTERY_STATUS_FULL;
                                        // Are we now charging?
                                        mCharging = mCharged || status == BatteryManager.BATTERY_STATUS_CHARGING;

                                        if (!WAS_CHARGING && mCharging) {// Did we go from Discharging -> Charging?
                                            XposedBridge.log("Samsung Battery Fixer: Plugged the charger in");
                                            // Let's expand our battery capacity here
                                            if (!BatteryUtil.isBatteryExpanded()) {
                                                if (BatteryUtil.extendCapacity()) {
                                                    XposedBridge.log("Samsung Battery Fixer: Extended battery capacity to accommodate more charging");
                                                    WAS_CHARGING = true;
                                                } else {
                                                    XposedBridge.log("Samsung Battery Fixer: Could not expand the charger!!! See log, possible root issues.");
                                                }
                                            } else {
                                                XposedBridge.log("Samsung Battery Fixer: Battery is already Expanded");
                                            }
                                        } else if (WAS_CHARGING && !mCharging) { // Did we go from Charging -> Discharging?
                                            XposedBridge.log("Samsung Battery Fixer: Removed the charger");
                                            // Let's contract our battery capacity here
                                            if (BatteryUtil.isBatteryExpanded()) {
                                                if (BatteryUtil.normalizeCapacity()) {
                                                    WAS_CHARGING = true;
                                                    XposedBridge.log("Samsung Battery Fixer: Normalized battery capacity more charging");
                                                } else {
                                                    XposedBridge.log("Samsung Battery Fixer: Could not contract the charger!!! See log, possible root issues.");
                                                }
                                            } else {
                                                XposedBridge.log("Samsung Battery Fixer: Battery is already contracted");
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            });
        }
    }
}
