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
 * Models the physical properties (location, speed) of the ball.
 * Position and speed of the ball are measured in "virtual field coordinates,"
 * which is defined and bounded by GameView.FIELD_X and GameView.FIELD_Y.
 */
public class Ball {
    /** the radius of the ball in virtual field coordinate system */
    final static int RADIUS = 10;
    /** x coordinate of center of ball (in field coordinate system */
    private float mX;
    /** y coordinate of center of ball */
    private float mY;
    /** the radius of the ball in screen resolution (pixels) */
    private int mScreenRadius;
    /** speed of the ball (in pixels per iteration) */
    int mSpeed = 30;
    /** the maximum speed of the ball */
    private int mMaxSpeed = Paddle.WIDTH + 2 * RADIUS;
    /** x component of ball velocity */
    private float mVx;
    /** y component of ball velocity */
    private float mVy;
    /** the number or rounds played (number of times start() was called) */
    private int mRounds = 0;

    /**
     * Default constructor.
     */
    public Ball() {
        mScreenRadius = (int) GameView.scaleX(RADIUS);
        start();
    }

    /**
     * Initialize/reset the ball to the start location and start velocity.
     */
    public void start() {
        // set location to middle of the screen
        mX = GameView.FIELD_X / 2;
        mY = GameView.FIELD_Y / 2;

        // alternate start direction: right, left, right, left, right, ...
        double startAngle = Math.PI * mRounds++;
        mVx = (float) Math.cos(startAngle);
        mVy = (float) Math.sin(startAngle);
    }

    /**
     * Returns the current x location of the ball.
     * @return current x position of ball
     */
    public int getX() {
        return (int) mX;
    }

    /**
     * Returns the current y location of the ball.
     * @return current y position of ball
     */
    public int getY() {
        return (int) mY;
    }

    /**
     * Sets the current location of the ball.
     * @param x the x position of the ball
     * @param y the y position of the ball
     */
    public void setCoord(int x, int y) {
        mX = x;
        mY = y;
    }

    /** the paint object used for drawing the ball */
    private final static Paint p = new Paint();

    /**
     * Draws the ball to a graphics canvas.
     * @param c the canvas on which the ball is drawn
     */
    public void draw(Canvas c) {
        c.drawCircle(GameView.scaleX(mX), GameView.scaleY(mY), mScreenRadius, p);
    }

    /** the largest possible angle the ball is ever reflected */
    private final static double MAXBOUNCEANGLE = 5 * Math.PI / 12;

    /**
     * Calculates the new position of the ball based on the current position and velocity.
     * The ball is reflected if the ball hits the top or bottom walls or the paddles.
     * @param left the left paddle
     * @param right the right paddle
     * @param dt the time (in ms) that has past since this method was called last
     */
    public void move(Paddle left, Paddle right, int dt) {

        // adapt speed dynamically
        // speed [px / ms] to cross screen in one second is: GameView.FIELD_X / 1000 [px / ms]
        // assuming constant frame rate, then speed to cross screen in 1000 ms is:
        mSpeed = GameView.FIELD_X * dt / 1000;

        // But let's limit the speed of the ball. If dt is large (because the program thread was
        // delayed for whatever reason), then the ball would jump a large distance,
        // which could confusing. Let's limit the speed to the arbitrary value of (2 * max speed).
        if (mSpeed > mMaxSpeed && (mX < 100 || mX > GameView.FIELD_X - 100)) {
            //when ball is close to paddle, cap speed because ball could fly through paddle
            mSpeed = mMaxSpeed;
        } else if (mSpeed > mMaxSpeed + mMaxSpeed) {
            //cap speed of ball
            mSpeed = mMaxSpeed + mMaxSpeed;
        }

        /*
        Ball reflection logic modelled after this:
        http://gamedev.stackexchange.com/questions/4253/in-pong-how-do-you-calculate-the-balls-direction-when-it-bounces-off-the-paddl

        Essentially, when the ball collides with the paddle, its direction is completely
        disregarded; it is given a new direction according to how far from the center of
        the paddle it collided. If the ball hits the paddle right in the center, it is sent
        away exactly horizontal; if it hits right on the edge, it flies off at an extreme
        angle (75 degrees). And it always travels at a constant speed.

        var relativeIntersectY = (paddle1Y+(PADDLEHEIGHT/2)) - intersectY;

        Take the middle Y value of the paddle, and subtract the Y intersection of the ball.
        If the paddle is 10 pixels high, this number will be between -5 and 5. I call this
        the "relative intersect" because it is in "paddle space" now, the ball's intersection
        relative to the middle of the paddle.

        var normalizedRelativeIntersectionY = (relativeIntersectY/(PADDLEHEIGHT/2));
        var bounceAngle = normalizedRelativeIntersectionY * MAXBOUNCEANGLE;

        Take the relative intersection and divide it by half the paddle height. Now our
        -5 to 5 number is a decimal from -1 to 1; it's normalized. Then multiply it by the
        maximum angle by which you want the ball to bounce.
        I set it to 5*Pi/12 radians (75 degrees).

        ballVx = BALLSPEED*Math.cos(bounceAngle);
        ballVy = BALLSPEED*-Math.sin(bounceAngle);

        Finally, calculate new ball velocities, using simple trigonometry.
        */

        // the paddle (left or right) the ball is approaching
        Paddle paddle = mVx > 0 ? right : left;
        Rect rec = paddle.getSpace();
        int halfPaddleH = rec.height() / 2;

        // check if ball hits paddle
        if (rec.contains((int)mX, (int)mY)) {
            float intersectY = rec.top + halfPaddleH - mY;
            float normalized = intersectY / halfPaddleH;
            double bounceAngle = normalized * MAXBOUNCEANGLE;
            mVx = (float) (Math.cos(bounceAngle));
            mVy = (float) (-Math.sin(bounceAngle));

            if (paddle == right) {
                mVx = -mVx;
                // fix up x coordinate of ball so that ball is not "inside" paddle
                mX = rec.left;
            } else {
                mX = rec.right;
            }
            return;
        }

        // move ball one step
        mX += mVx * mSpeed;
        mY += mVy * mSpeed;

        // check if ball hits top wall
        if (mY - RADIUS <= 0) {
            mY = RADIUS;
            mVy = -mVy;
            return;
        }
        // check if ball hits bottom wall
        if (mY + RADIUS >= GameView.FIELD_Y) {
            mY = GameView.FIELD_Y - RADIUS;
            mVy = -mVy;
        }
    }
}
