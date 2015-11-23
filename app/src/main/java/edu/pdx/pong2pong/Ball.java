package edu.pdx.pong2pong;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;

public class Ball {
    /** x coordinate of center of ball */
    private float mX;
    /** y coordinate of center of ball */
    private float mY;
    /** the radius of the ball */
    private int mRadius = 20;
    /** speed of the ball (in pixels per iteration) (should ideally be device dependent...) */
    private int mSpeed = 30;
    /** x component of ball velocity */
    private float mVx;
    /** y component of ball velocity */
    private float mVy;
    /** screen height (x coordinate of bottom wall */
    private float mScreenW;
    /** screen height (y coordinate of bottom wall */
    private float mScreenH;

    public Ball(float screenW, float screenH) {
        mScreenW = screenW;
        mScreenH = screenH;
        start();
    }

    /**
     * Initialize or reset ball to start coordinates and start conditions.
     */
    public void start() {
        // set location to middle of the screen
        mX = mScreenW / 2;
        mY = mScreenH / 2;

        double startAngle = 1; //this could be random
        mVx = (float) (mSpeed * Math.cos(startAngle));
        mVy = (float) (mSpeed * Math.sin(startAngle));
    }

    public float getX() {
        return mX;
    }

    private final static Paint p = new Paint();

    public void draw(Canvas c) {
        c.drawCircle(mX, mY, mRadius, p);
    }



    private final static double MAXBOUNCEANGLE = 5 * Math.PI / 12;

    public void move(Paddle left, Paddle right) {
        /*
        Ball reflection logic as described here:
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

        // the paddle the ball is approaching
        Paddle paddle = mVx > 0 ? right : left;
        Rect rec = paddle.getSpace(mRadius);
        int halfPaddleH = rec.height() / 2;
        // check if ball hits paddle
        if (rec.contains((int)mX, (int)mY)) {
            float intersectY = rec.top + halfPaddleH - mY;
            float normalized = intersectY / halfPaddleH;
            double bounceAngle = normalized * MAXBOUNCEANGLE;
            mVx = (float) (mSpeed * Math.cos(bounceAngle));
            mVy = (float) (mSpeed * -Math.sin(bounceAngle));
            //Log.d("JM", "bounceangle " + bounceAngle + " vx:" + mVx + " vy:" + mVy);
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
        mX += mVx;
        mY += mVy;

        // check if ball hits top wall
        if (mY - mRadius <= 0) {
            mY = mRadius;
            mVy = -mVy;
            return;
        }
        // check if ball hits bottom wall
        if (mY + mRadius >= mScreenH) {
            mY = mScreenH -mRadius;
            mVy = -mVy;
            return;
        }
    }
}
