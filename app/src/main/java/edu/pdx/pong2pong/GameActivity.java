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

public class GameActivity extends AppCompatActivity implements SensorEventListener {

    public static String EXTRA_IP_SERVER = "EXTRA_IP_SERVER";
    public static String EXTRA_IS_SERVER = "EXTRA_IS_SERVER";

    /** hardware sensor */
    private SensorManager mSm;

    /** game graphics and logic */
    private GameView mGameView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //set window to full screen
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        //check if the accelerometer is present
        mSm = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (mSm.getSensorList(Sensor.TYPE_GRAVITY).size() != 0) {
            //setup sensor
            Sensor s = mSm.getSensorList(Sensor.TYPE_GRAVITY).get(0);
            //register listener for sensor
            mSm.registerListener(this, s, SensorManager.SENSOR_DELAY_GAME);
        }

        //get WifiDirect data
        boolean isServer = getIntent().getBooleanExtra(EXTRA_IS_SERVER, true);
        String addrServer = getIntent().getStringExtra(EXTRA_IP_SERVER);
        mGameView = new GameView(this, isServer, addrServer);
        setContentView(mGameView);
     }

    @Override
    protected void onResume() {
        super.onResume();
        mSm.registerListener(this, mSm.getDefaultSensor(Sensor.TYPE_GRAVITY),
                SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    //unregister sensor listener to free up memory
    protected void onPause(){
        mSm.unregisterListener(this);
        super.onPause();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        try {
            Thread.sleep(15); //delay to buffer sensor
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mGameView.setSensorY(event.values[0]);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}
