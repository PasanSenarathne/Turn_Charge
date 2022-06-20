package com.isprid.turnchargelk.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.isprid.turnchargelk.util.Constant;
import com.isprid.turnchargelk.util.ObdHelper;

import java.util.Timer;
import java.util.TimerTask;

public class BatterySimulateService extends Service {

    public static final String ACTION_BATTERY_VALUE = "com.isprid.turnchargelk.battery";
    public static final String ACTION_BATTERY_DRAIN = "com.isprid.turnchargelk.drain";
    private final static String TAG = "BatterySimulateService";
    private final Handler mTimerHandler = new Handler();
    Intent intent = new Intent(ACTION_BATTERY_VALUE);
    int carBatteryLevel = 100;
    private Timer mTimer1;
    private TimerTask mTt1;
    private final BroadcastReceiver br = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //update update Battery Level
            if (intent.getExtras() != null) {
                int drainValue = intent.getIntExtra(Constant.EXTRA_DRAIN, 1);
                carBatteryLevel = carBatteryLevel - drainValue;
                if (carBatteryLevel < 0) {
                    carBatteryLevel = 0;
                }
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        Log.i(TAG, "Starting timer...");

        registerReceiver(br, new IntentFilter(BatterySimulateService.ACTION_BATTERY_DRAIN));
        startTimer();
    }

    private void startTimer() {
        carBatteryLevel = ObdHelper.getBatteryLevel();
        mTimer1 = new Timer();
        mTt1 = new TimerTask() {
            public void run() {
                mTimerHandler.post(() -> {
                    Log.i(TAG, "Battery remaining: " + carBatteryLevel);
                    intent.putExtra(Constant.EXTRA_BATTERY, carBatteryLevel);
                    sendBroadcast(intent);
                    carBatteryLevel = carBatteryLevel - 1;
                    if (carBatteryLevel < 0) {
                        carBatteryLevel = 0;
                        stopTimer();
                    }
                });
            }
        };

        mTimer1.schedule(mTt1, 1, 60000); //60000 = 1 min
    }

    private void stopTimer() {
        if (mTimer1 != null) {
            mTimer1.cancel();
            mTimer1.purge();
        }
    }

    @Override
    public void onDestroy() {
        stopTimer();
        Log.i(TAG, "Timer cancelled");
        unregisterReceiver(br);
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }
}