package com.xda.nm1557.samsungbatteryfixer;

import android.annotation.SuppressLint;

import java.io.IOException;

/**
 * Created by nmitchko on 3/16/18.
 */

public class BatteryStatsManager {

    private float BATTERY_GAIN_MODIFIER = 1.2f;
    private int NORM_BATT_CAPACITY = 0;
    private int GAIN_BATT_CAPACITY = 0;

    private boolean BatteryExpanded = false;

    private static final String SUDO = "su -c ";
    private static final String GET_BATTERY_CAPACITY = "\'cat /sys/class/power_supply/battery/batt_capacity_max\'";

    private static final String SET_BATTERY_CAPACITY = "\'echo %d > /sys/class/power_supply/battery/batt_capacity_max\'";

    public BatteryStatsManager() {
        this(1.2f);
    }

    public BatteryStatsManager(float battery_gain_modifier) {
        if (battery_gain_modifier > 1.0 && battery_gain_modifier < 1.3) {
            BATTERY_GAIN_MODIFIER = battery_gain_modifier;
        }
        NORM_BATT_CAPACITY = getBatteryCapacity();
        GAIN_BATT_CAPACITY = calculateExpandedBattery();
    }

    public int calculateExpandedBattery() {
        return (int) ((float) this.NORM_BATT_CAPACITY * this.BATTERY_GAIN_MODIFIER);
    }

    public int getBatteryCapacity() {
        try {
            String output = executeCommand(SUDO + GET_BATTERY_CAPACITY);
            return Integer.parseInt(output.trim());
        } catch (IOException | NumberFormatException e) {
            return 800;
        }
    }

    public boolean extendCapacity(){
        if (setBatteryCapacity(GAIN_BATT_CAPACITY)){
            BatteryExpanded = true;
            return true;
        }
        return false;
    }

    public boolean normalizeCapacity(){
        if (setBatteryCapacity(GAIN_BATT_CAPACITY)){
            BatteryExpanded = false;
            return true;
        }
        return false;
    }

    public boolean setBatteryCapacity(int capacity) {
        try {
            @SuppressLint("DefaultLocale") String output = executeCommand(SUDO + String.format(SET_BATTERY_CAPACITY, capacity));
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean isBatteryExpanded(){
        return BatteryExpanded;
    }

    private static String executeCommand(String cmd) throws java.io.IOException {
        java.util.Scanner s = new java.util.Scanner(Runtime.getRuntime().exec(cmd).getInputStream()).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    public float getBATTERY_GAIN_MODIFIER() {
        return BATTERY_GAIN_MODIFIER;
    }

    public int getNORM_BATT_CAPACITY() {
        return NORM_BATT_CAPACITY;
    }

    public int getGAIN_BATT_CAPACITY() {
        return GAIN_BATT_CAPACITY;
    }
}
