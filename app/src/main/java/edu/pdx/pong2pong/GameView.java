package edu.pdx.pong2pong;
/**
 * Uses code snippets from the Android SDK LunarLander sample program
 */

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class GameView extends SurfaceView
        implements SurfaceHolder.Callback {

    Ball mBall;
    Paddle[] mPaddle;

    /** the width of the screen (max x) */
    int mScreenW;

    /** the height of the screen (max y) */
    int mScreenH;

    /** The thread that actually draws the animation */
    private final GameThread mThread;

    private Paint paintText = new Paint();

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

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

        mBall = new Ball(mScreenW, mScreenH);
        mPaddle = new Paddle[] {
                new Paddle(20, mScreenH / 2),
                new Paddle(mScreenW - 20, mScreenH / 2)
        };

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

        /** time (ms) between frames; one iteration of the main processing loop in run() */
        private int mDt;



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
            long timeEnd = System.currentTimeMillis();
            while (true) {
                long timeStart = System.currentTimeMillis();
                //time between frames; adding 1 guarantees that value is never 0
                mDt = (int) (timeStart - timeEnd + 1);
                timeEnd = timeStart;

                Canvas c = mHolder.lockCanvas();
                doDraw(c);
                mHolder.unlockCanvasAndPost(c);
                mBall.move(mPaddle[0], mPaddle[1], mDt);

                if (mBall.getX() < 0 || mBall.getX() > mScreenW) {
                    //ball is outside game area - create a new ball/game
                    mBall.start();
                }

            }
        }


        private void doDraw(Canvas c) {
            c.drawColor(Color.LTGRAY); //background
            c.drawText("time between frames (ms): " + mDt, 10, 60, paintText);
            int fps = (int) (1000 / mDt);
            c.drawText("frames per second: " + fps, 10, 100, paintText);
            c.drawText("screen: " + mScreenW + "x" + mScreenH, 10, 140, paintText);
            c.drawText("speed of ball: " + mBall.mSpeed, 10, 180, paintText);
            mBall.draw(c);
            mPaddle[0].draw(c);
            mPaddle[1].draw(c);
        }
    }
}
