package edu.pdx.pong2pong;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

public class Paddle {
    /** current x coordinate of the paddle center */
    float mX;
    /** current y coordinate of the paddle center */
    float mY;


    /** the width of the paddle */
    final static int w = 20;
    /** the height of the paddle */
    final static int h = 200;
    /** half the width of the paddle */
    final static int half_w = w / 2;
    /** half the width of the paddle */
    final static int half_h = h / 2;

    private final static Paint p = new Paint();

    public Paddle(float x, float y) {
        mX = x;
        mY = y;
    }

    public void setY(float y) {
        mY = y;
    }

    /**
     * If the center of the ball is inside this rect area, then the ball is hitting this paddle.
     * @param radius the radius of the ball
     * @return
     */
    public Rect getSpace(int radius) {
        return new Rect(
                (int)mX - half_w - radius,
                (int)mY - half_h - radius,
                (int)mX + half_w + radius,
                (int)mY + half_h + radius
        );
    }

    void draw(Canvas c) {
        c.drawRect(mX - half_w, mY - half_h, mX + half_w, mY + half_h, p);
    }

}
