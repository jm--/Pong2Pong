/*
 * Copyright (C) 2015 Josef Mihalits, Randon Stasney, Dakota Ward
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.pdx.pong2pong;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

/**
 * Manages the main game screen and logic.
 */
public class GameActivity extends AppCompatActivity implements SensorEventListener {

    /** the keys for the intent passed into this activity */
    public static String EXTRA_IP_SERVER = "EXTRA_IP_SERVER";
    public static String EXTRA_IS_SERVER = "EXTRA_IS_SERVER";
    public static String EXTRA_USE_ACCELEROMETER = "EXTRA_USE_ACCELEROMETER";

    /** hardware sensor */
    private SensorManager mSm;

    /** game graphics and logic */
    private GameView mGameView;

    /** whether or not to use the accelerometer */
    private boolean mUseAcc;

    /**
     * Setup accelerometer and game view.
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //set window to full screen
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        //setup accelerometer
        mUseAcc = getIntent().getBooleanExtra(EXTRA_USE_ACCELEROMETER, true);
        if (mUseAcc) {
            //check if the accelerometer is present
            mSm = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            Sensor s = mSm.getDefaultSensor(Sensor.TYPE_GRAVITY);
            if (s == null) {
                //don't use accelerometer, as we don't have one
                mUseAcc = false;
            } else {
                //register listener for sensor
                mSm.registerListener(this, s, SensorManager.SENSOR_DELAY_GAME);
            }
        }

        //get WifiDirect data
        boolean isServer = getIntent().getBooleanExtra(EXTRA_IS_SERVER, true);
        String addrServer = getIntent().getStringExtra(EXTRA_IP_SERVER);
        mGameView = new GameView(this, isServer, addrServer);
        setContentView(mGameView);
     }

    /**
     * Register sensor listener.
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (mUseAcc) {
            mSm.registerListener(this, mSm.getDefaultSensor(Sensor.TYPE_GRAVITY),
                    SensorManager.SENSOR_DELAY_GAME);
        }
    }

    /**
     * Unregister sensor listener to free up memory.
     */
    @Override
    protected void onPause(){
        super.onPause();
        if (mUseAcc) {
            mSm.unregisterListener(this);
        }
    }

    /**
     * Callback - accelerometer sensor changed.
     * @param event the sensor and the current sensor readings
     */
    @Override
    public void onSensorChanged(SensorEvent event) {
        try {
            Thread.sleep(15); //delay to buffer sensor
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mGameView.setSensorY(event.values[0]);
    }

    /**
     * Callback - accelerometer accuracy has changed. (not implemented)
     */
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}
