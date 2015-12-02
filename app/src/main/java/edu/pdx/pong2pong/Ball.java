package edu.pdx.pong2pong;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

public class Ball {
    /** the radius of the ball in virtual field coordinate system */
    final static int RADIUS = 10;
    /** x coordinate of center of ball (in field coordinate system */
    private float mX;
    /** y coordinate of center of ball */
    private float mY;
    /** the radius of the ball in screen coordinate system */
    private int mScreenRadius;
    /** speed of the ball (in pixels per iteration) */
    int mSpeed = 30;
    /** the maximum speed of the ball */
    int mMaxSpeed = Paddle.WIDTH + 2 * RADIUS;
    /** x component of ball velocity */
    private float mVx;
    /** y component of ball velocity */
    private float mVy;

    /** the number or rounds played (number of times start() was called) */
    private int mRounds = 0;

    public Ball() {
        mScreenRadius = (int) GameView.scaleX(RADIUS);
        start();
    }

    /**
     * Initialize or reset ball to start coordinates and start conditions.
     */
    public void start() {
        // set location to middle of the screen
        mX = GameView.FIELD_X / 2;
        mY = GameView.FIELD_Y / 2;

        double startAngle = Math.PI * mRounds++;
        mVx = (float) Math.cos(startAngle);
        mVy = (float) Math.sin(startAngle);
    }

    public int getX() {
        return (int) mX;
    }
    public int getY() {
        return (int) mY;
    }

    public void setCoord(int x, int y) {
        mX = x;
        mY = y;
    }

    private final static Paint p = new Paint();

    public void draw(Canvas c) {
        c.drawCircle(GameView.scaleX(mX), GameView.scaleY(mY), mScreenRadius, p);
    }


    private final static double MAXBOUNCEANGLE = 5 * Math.PI / 12;

    public void move(Paddle left, Paddle right, int dt) {

        // adapt speed dynamically
        // speed [px / ms] to cross screen in one second is: screenW / 1000 [px / ms]
        // assuming constant frame rate, then speed to cross screen in 1000 ms is:
        mSpeed = GameView.FIELD_X * dt / 1000;

        if (mSpeed > mMaxSpeed && (mX < 100 || mX > GameView.FIELD_X - 100)) {
            //when ball is close to paddle, cap speed because ball could fly through paddle
            mSpeed = mMaxSpeed;
        } else if (mSpeed > mMaxSpeed + mMaxSpeed) {
            //cap speed of ball
            mSpeed = mMaxSpeed + mMaxSpeed;
        }

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
        Rect rec = paddle.getSpace();
        int halfPaddleH = rec.height() / 2;

        // check if ball hits paddle
        if (rec.contains((int)mX, (int)mY)) {
            float intersectY = rec.top + halfPaddleH - mY;
            float normalized = intersectY / halfPaddleH;
            double bounceAngle = normalized * MAXBOUNCEANGLE;
            mVx = (float) (Math.cos(bounceAngle));
            mVy = (float) (-Math.sin(bounceAngle));

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
            return;
        }
    }
}
