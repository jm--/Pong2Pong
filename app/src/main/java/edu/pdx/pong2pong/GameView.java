package edu.pdx.pong2pong;
/**
 * Uses code snippets from the Android SDK LunarLander sample program:
 * http://grepcode.com/file/repository.grepcode.com/java/ext/com.google.android/android-apps/2.3.1_r1/com/example/android/lunarlander/LunarView.java
 *
 */

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class GameView extends SurfaceView
        implements SurfaceHolder.Callback, Runnable {

    Ball mBall;
    Paddle[] mPaddle;

    /** the width of the screen (max x) */
    int mScreenW;

    /** the height of the screen (max y) */
    int mScreenH;

    /** The thread that actually draws the animation */
    private Thread mThread;

    /** Style and color information for text output */
    private Paint paintText = new Paint();

    /** Indicate whether the surface has been created & is ready to draw */
    private boolean mRun = false;

    /** Lock to sync access to mRun */
    private final Object mRunLock = new Object();

    /** Handle to the surface manager object we interact with */
    private SurfaceHolder mHolder;

    /** time (ms) between frames; one iteration of the main processing loop in run() */
    private int mDt;

    public GameView(Context context) {
        this(context, null);
    }

    public GameView(final Context context, final AttributeSet attrs) {
        super(context, attrs);

        // register our interest in hearing about changes to our surface
        mHolder = getHolder();
        mHolder.addCallback(this);

        setFocusable(true); // make sure we get key events

        paintText.setStrokeWidth(1);
        paintText.setStyle(Paint.Style.FILL);
        paintText.setTextSize(30);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mScreenW = w;
        mScreenH = h;
    }

    /**
     * Callback invoked when the Surface has been created and is ready to be
     * used.
     */
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mBall = new Ball(mScreenW, mScreenH);
        mPaddle = new Paddle[] {
                new Paddle(20, mScreenH / 2),
                new Paddle(mScreenW - 20, mScreenH / 2)
        };
        mRun = true;
        mThread = new Thread(this);
        mThread.start();
    }

    /**
     * Callback invoked when the surface dimensions change.
     */
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    /**
     * Callback invoked when the Surface has been destroyed and must no longer
     * be touched. WARNING: after this method returns, the Surface/Canvas must
     * never be touched again!
     */
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // tell thread to shut down
        mRun = false;
        // wait for it to finish
        while(true) {
            try {
                mThread.join();
                break;
            } catch(InterruptedException e) {}
        }
        mThread = null;
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
            mPaddle[0].setY(y);
        } else {
            //right paddle
            mPaddle[1].setY(y);
        }
        return true;
    }

    @Override
    public void run() {
        long timeEnd = System.currentTimeMillis();
        while (mRun) {
            long timeStart = System.currentTimeMillis();
            //time between frames; adding 1 guarantees that value is never 0
            mDt = (int) (timeStart - timeEnd + 1);
            timeEnd = timeStart;

            Canvas c = mHolder.lockCanvas();
            if (c != null) {
                doDraw(c);
                mHolder.unlockCanvasAndPost(c);
            }

            mBall.move(mPaddle[0], mPaddle[1], mDt);

            if (mBall.getX() < 0 || mBall.getX() > mScreenW) {
                //ball is outside game area - create a new ball/game
                mBall.start();
            }
        }
    }

    /**
     * Draw ball, paddles, and everything else to canvas.
     * @param c canvas
     */
    private void doDraw(Canvas c) {
        c.drawColor(Color.LTGRAY); //background
        c.drawText("time between frames (ms): " + mDt, 10, 60, paintText);
        int fps = 1000 / mDt;
        c.drawText("frames per second: " + fps, 10, 100, paintText);
        c.drawText("screen: " + mScreenW + "x" + mScreenH, 10, 140, paintText);
        c.drawText("speed of ball: " + mBall.mSpeed, 10, 180, paintText);
        mBall.draw(c);
        mPaddle[0].draw(c);
        mPaddle[1].draw(c);
    }
}
