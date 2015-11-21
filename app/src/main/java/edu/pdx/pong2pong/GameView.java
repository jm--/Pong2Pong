package edu.pdx.pong2pong;
/**
 * Uses code snippets from the Android SDK LunarLander sample program
 */

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class GameView extends SurfaceView implements SurfaceHolder.Callback {

    /** The thread that actually draws the animation */
    private final GameThread mThread;

    public GameView(Context context) {
        this(context, null);
    }

    public GameView(final Context context, final AttributeSet attrs) {
        super(context, attrs);

        // register our interest in hearing about changes to our surface
        final SurfaceHolder holder = getHolder();
        holder.addCallback(this);

        // create thread only; it's started in surfaceCreated()
        mThread = new GameThread(holder, context, new Handler() {
//            @Override
//            public void handleMessage(final Message m) {
//                mStatusText.setVisibility(m.getData().getInt("viz"));
//                mStatusText.setText(m.getData().getString("text"));
//            }
        });

        setFocusable(true); // make sure we get key events
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mThread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        while(true) {
            try {
                mThread.join();
                return;
            } catch(InterruptedException e) {}
        }
    }


    /**
     *
     */
    class GameThread extends Thread {
        private float mX = 0;
        private float mY = 0;

        private boolean mInBackground = false;

        /** Handle to the surface manager object we interact with */
        private SurfaceHolder mHolder;

        /** Message handler used by thread to interact with TextView */
        private Handler mHandler;

        /** Handle to the application context, used to e.g. fetch Drawables. */
        private Context mContext;

        public GameThread(final SurfaceHolder holder,
                           final Context context,
                           final Handler handler) {
            // get handles to some important objects
            mHolder = holder;
            mContext = context;
            mHandler = handler;
        }

        @Override
        public void run() {
            while (true) {
                Canvas c = mHolder.lockCanvas();
                doDraw(c);
                mHolder.unlockCanvasAndPost(c);
                mX += 10;
                mY += 10;
            }
        }


        private void doDraw (Canvas c) {
            c.drawARGB(255, 150,150,0); //background
            //c.drawBitmap(ball, x - ball.getWidth()/2, y - ball.getHeight()/2, null);
            c.drawCircle(mX, mY, 10, new Paint());
        }
    }
}
