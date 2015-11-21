package edu.pdx.pong2pong;

import android.graphics.Canvas;
import android.graphics.Paint;

public class Ball {
    float mX;
    float mY;
    float mRadius = 20;
    float mVx;
    float mVy;
    float mSreenH;


    public Ball(float screenW, float screenH) {
        mSreenH = screenH;
        mX = screenW / 2;
        mY = screenH / 2;
        mVx = 30;
        mVy = 10;
    }

    private final static Paint p = new Paint();
    void draw(Canvas c) {
        c.drawCircle(mX, mY, mRadius, p);
    }

    void move(Paddle left, Paddle right) {
        mX += mVx;
        mY += mVy;
    }
}
