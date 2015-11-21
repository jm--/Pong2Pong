package edu.pdx.pong2pong;
/**
 * Uses code snippets from the Android SDK LunarLander sample program
 */

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

public class GameView extends SurfaceView
        implements SurfaceHolder.Callback {

    /** ball velocity x */
    float mVx;
    /** ball velocity y */
    float mVy;

    float[] mX = new float[3];
    float[] mY = new float[3];

    /** the width of the screen (max x) */
    int mScreenW;

    /** the height of the screen (max y) */
    int mScreenH;

    /** The thread that actually draws the animation */
    private final GameThread mThread;

    //private PointF paddle[] = {new PointF(10,10), new PointF(200, 10)};

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
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mScreenW = w;
        mScreenH = h;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mX[0] = mY[0] = 10;
        mVx = 20;
        mVy = 10;
        mX[1] = 5;
        mY[1] = mScreenH / 2;
        mX[2] = mScreenW - 30;
        mY[2] = mScreenH / 2;

        mThread.start();
        Log.d("JM", "surfaceCreated");

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        //Log.d("JM", "surfaceChanged" + width + " " + height);
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
     * handle events: MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP, MotionEvent.ACTION_MOVE
     * @param event
     * @return
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //super.onTouchEvent(event);

        float x = event.getX();
        float y = event.getY();
        if (x < mScreenW / 2) {
            //left paddle
            mY[1] = y;
        } else {
            //right paddle
            mY[2] = y;
        }
        return true;
    }


    /**
     *
     */
    class GameThread extends Thread {

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
                mX[0] += mVx;
                mY[0] += mVy;
            }
        }


        private void doDraw (Canvas c) {
            Paint p = new Paint();

            c.drawARGB(255, 150,150,0); //background
            //c.drawBitmap(ball, x - ball.getWidth()/2, y - ball.getHeight()/2, null);
            c.drawCircle(mX[0], mY[0], 10, p);
            c.drawRect(mX[1], mY[1], mX[1]+20, mY[1]+200, p);
            c.drawRect(mX[2], mY[2], mX[2]+20, mY[2]+200, p);
        }
    }
}
