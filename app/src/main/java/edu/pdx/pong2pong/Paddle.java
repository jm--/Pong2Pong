package edu.pdx.pong2pong;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

public class Paddle {
    /** current x coordinate of the paddle center */
    private float mX;
    /** current y coordinate of the paddle center */
    private float mY;
    /** the number of won rounds (current score) */
    private int mNumWins;
    /** the width of the paddle */
    final static int WIDTH = 20;
    /** the height of the paddle */
    final static int HEIGHT = 100;
    /** half the width of the paddle */
    final private static int half_w = WIDTH / 2;
    /** half the width of the paddle */
    final private static int half_h = HEIGHT / 2;
    /** paint object used when drawing the ball */
    private final Paint p = new Paint();
    /** if the center of the ball (x,y) is inside this rect. area, then the ball hits the paddle */
    private Rect mPaddleSpace = new Rect();

    public Paddle(int color, float x, float y) {
        p.setColor(color);
        mX = x;
        mY = y;
        mNumWins = 0;
    }

    public int getY() {
        return (int) mY;
    }

    public void setY(float y) {
        mY = y;
    }

    /**
     * Get the current score.
     * @return current score
     */
    public int getScore() {
        return mNumWins;
    }

    /**
     * Increase the score by one.
     */
    public void incScore() {
        mNumWins++;
    }

    /**
     * If the center of the ball is inside this rect area, then the ball is hitting this paddle.
     * @return
     */
    public Rect getSpace() {
        mPaddleSpace.set(
                (int)mX - half_w - Ball.RADIUS,
                (int)mY - half_h - Ball.RADIUS,
                (int)mX + half_w + Ball.RADIUS,
                (int)mY + half_h + Ball.RADIUS
        );
        return mPaddleSpace;
    }


    void draw(Canvas c) {
        c.drawRect(
                GameView.scaleX(mX - half_w),
                GameView.scaleY(mY - half_h),
                GameView.scaleX(mX + half_w),
                GameView.scaleY(mY + half_h), p);
    }

}
