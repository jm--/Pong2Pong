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

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

/**
 * Models a game paddle: paddle dimensions, current location, etc.
 */
public class Paddle {
    /** current x coordinate of the paddle center (in virtual field coordinates) */
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
    /** half the height of the paddle */
    final private static int half_h = HEIGHT / 2;
    /** paint object used when drawing the ball */
    private final Paint p = new Paint();
    /** if the center of the ball (x,y) is inside this rect. area, then the ball hits the paddle */
    private Rect mPaddleSpace = new Rect();

    /**
     * Constructs a new paddle.
     * @param color the color of the paddle (argb value)
     * @param x the x position of the paddle (pixels in virtual field)
     * @param y the y position of the paddle
     */
    public Paddle(int color, float x, float y) {
        p.setColor(color);
        mX = x;
        mY = y;
        mNumWins = 0;
    }

    /**
     * Returns the current y position of the paddle.
     * @return the current y position of the paddle
     */
    public int getY() {
        return (int) mY;
    }

    /**
     * Sets the y position of the paddle.
     * @param y the new y position of the paddle
     */
    public void setY(float y) {
        mY = y;
    }

    /**
     * Get the current score.
     * @return the current score
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
     * @return Rect area occupied by the paddle
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

    /**
     * Draws the paddle to a graphics canvas.
     * @param c the canvas on which the paddle is drawn
     */
    void draw(Canvas c) {
        c.drawRect(
                GameView.scaleX(mX - half_w),
                GameView.scaleY(mY - half_h),
                GameView.scaleX(mX + half_w),
                GameView.scaleY(mY + half_h), p);
    }

}
