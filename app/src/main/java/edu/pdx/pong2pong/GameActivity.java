package edu.pdx.pong2pong;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

public class GameActivity extends AppCompatActivity {

    public static String EXTRA_IP_SERVER = "EXTRA_IP_SERVER";
    public static String EXTRA_IS_SERVER = "EXTRA_IS_SERVER";


    GameView mGameView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        boolean isServer = getIntent().getBooleanExtra(EXTRA_IS_SERVER, true);
        String addrServer = getIntent().getStringExtra(EXTRA_IP_SERVER);

        mGameView = new GameView(this, isServer, addrServer);
        setContentView(mGameView);
     }
}
