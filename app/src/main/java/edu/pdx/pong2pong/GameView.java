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

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

/**
 * The main Pong2Pong game logic. Once instance of this app runs in server-mode, another
 * in client-mode.
 * (Uses code snippets from the Android SDK LunarLander sample program.)
 */
public class GameView extends SurfaceView
        implements SurfaceHolder.Callback, Runnable {

    /** tag strings for logging */
    private static String TAG_ERROR = "PONGLOG_ERROR_GameView";
    private static String TAG_MSG = "PONGLOG_MSG_GameView";

    /** the "virtual field" - all math is done using these dimensions */
    public static final int FIELD_X = 1000;
    public static final int FIELD_Y = 500;

    /** the Pong ball */
    Ball mBall;

    /** the left paddle */
    Paddle mLeftPaddle;

    /** the right paddle */
    Paddle mRightPaddle;

    /** the paddle the user controls; either the left or the right paddle */
    Paddle mMyPaddle = new Paddle(0, 0, 0);

    /** the width of the screen (max x; number of actual screen pixels) */
    static int mScreenW = 0;

    /** the height of the screen (max y; number of actual screen pixels) */
    static int mScreenH = 0;

    /** The thread that actually draws the animation */
    private Thread mThread;

    /** Style and color information for debug text output */
    private Paint mDebugText = new Paint();

    /** Style and color information for the text used to print the current score */
    private Paint mScoreText;

    /** y location (in px) where the text for the current score is drawn */
    private int mScoreTextY;

    /** Indicate whether the surface has been created & is ready to draw */
    private boolean mRun = false;

    /** Handle to the surface manager object we interact with */
    private SurfaceHolder mHolder;

    /** a reference to the parent context */
    private Context mContext;

    /** time (ms) between frames; one iteration of the main processing loop in run() */
    private int mDt;

    /** comma separated list of local IP addresses */
    private String mIpAddress;

    /** sockets for the TCP/IP connection to the other device used in the game */
    private Socket mSocket = null;
    private ServerSocket mServerSocket = null;

    /** input and output streams for reading and writing to the socket */
    private DataInputStream mInStream;
    private DataOutputStream mOutStream;

    /** the IP address of the device running in server mode */
    private String mAddrServer;

    /** true if the program is running in server mode; false if the program runs in client mode */
    private boolean mIsServer;

    /** the last reading from the gravity sensor */
    private float mSensorY;

    /** a flag for enabling/disabling the printing of some debug information */
    private boolean mIsDebug = false;

    public GameView(Context context, boolean isServer, String addrServer) {
        super(context);
        mContext = context;
        mIsServer = isServer;
        mAddrServer = addrServer;

        // register our interest in hearing about changes to our surface
        mHolder = getHolder();
        mHolder.addCallback(this);

        // make sure we get key events
        setFocusable(true);

        // keep screen on
        setKeepScreenOn(true);

        //paint objects used for drawing text
        mDebugText.setStrokeWidth(1);
        mDebugText.setStyle(Paint.Style.FILL);
        mDebugText.setTextSize(30);

        //get local IP addresses
        mIpAddress = getIpAddresses();

        // there is no wifi-direct on emulators, so to test on emulators, start e.g. 2 emulators:
        // "emulator -avd MY_AVD1 -shared-net-id 42"
        // "emulator -avd MY_AVD2 -shared-net-id 43"
        // this starts a virtual LAN with IPs 10.1.2.42 and 10.1.2.43
        // R.string.test_server_addr needs to be one of the two IP addresses
        if (mAddrServer == null || mAddrServer.equals("")) {
            mAddrServer = context.getResources().getString(R.string.test_server_addr);
            mIsServer = mIpAddress.contains(mAddrServer);
        }
    }

    /**
     * Callback to tell the view about the size of the screen when the view is first created.
     * (Or to notify the view when the size of the screen has changed.)
     * @param w the current screen width
     * @param h the current screen height
     * @param oldw the old screen width
     * @param oldh the old screen height
     */
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mScreenW = w;
        mScreenH = h;
    }

    /**
     * Callback invoked when the Surface has been created and is ready to be
     * used. The thread that draws on the view can be started.
     */
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
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
     * A touch screen event (MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP,
     * MotionEvent.ACTION_MOVE) has occurred.
     * @param event x/y position of the touch event (in screen coordinates)
     * @return true so that multiple events are processed
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float y = event.getY();
        //scale screen coordinates ot virtual field coordinates
        mMyPaddle.setY(y * FIELD_Y / mScreenH);
        return true;
    }

    /**
     * Gravity sensor change callback from activity.
     * @param value the current reading of the sensor
     */
    public void setSensorY(float value) {
        mSensorY = value;
        final int middle = FIELD_Y / 2;
        // The range of sensor values is between -9.8 and +9.8 (i.e. 1g), but let's use
        // a lower max value so that one is not required to tilt the device all the way (90 degree)
        final int maxSensorValue = 4;
        mMyPaddle.setY(middle + middle / maxSensorValue * value);
    }

    /**
     * The entry point of the processing thread. Contains the main game loop.
     */
    @Override
    public void run() {
        try {
            openNetwork();
            setupGame();
        } catch(Exception e) {
            Log.d(TAG_ERROR, "Network error: " + e);
            e.printStackTrace();
            drawText("Network error: " + e);
            return;
        }
        long timeEnd = System.currentTimeMillis();
        mBall.start();

        while (mRun) {
            long timeStart = System.currentTimeMillis();
            //time between frames; adding 1 guarantees that the value is never 0
            mDt = (int) (timeStart - timeEnd + 1);
            timeEnd = timeStart;

            //update the UI
            Canvas c = mHolder.lockCanvas();
            if (c != null) {
                doDraw(c);
                mHolder.unlockCanvasAndPost(c);
            }

            if (isServer()) {
                //the server program controls the ball
                mBall.move(mLeftPaddle, mRightPaddle, mDt);
                sendReceiveServer();
            } else {
                networkClient();
            }

            //check if the ball is outside the game area
            if (mBall.getX() < 0) {
                mRightPaddle.incScore();
                mBall.start();
            } else if (mBall.getX() > FIELD_X) {
                mLeftPaddle.incScore();
                mBall.start();
            }
        }
        closeNetwork();
    }

    /**
     * Setup the ball and the paddles.
     */
    private void setupGame()  {
        mBall = new Ball();
        mLeftPaddle = new Paddle(Color.rgb(200, 0, 0), 20 , FIELD_Y / 2);
        mRightPaddle = new Paddle(Color.rgb(0, 0, 200), FIELD_X - 20, FIELD_Y / 2);
        mMyPaddle = isServer() ? mRightPaddle : mLeftPaddle;

        //init Paint object used for drawing the score text
        mScoreText = new Paint();
        mScoreText.setColor(Color.rgb(100, 100, 100));
        mScoreText.setFakeBoldText(true);
        mScoreText.setTextAlign(Paint.Align.CENTER);
        mScoreText.setTextSize(mScreenH / 3);
        Rect rec = new Rect();
        mScoreText.getTextBounds("1234567890:", 0, 11, rec);
        //the y position of where to draw the text
        mScoreTextY = (mScreenH + Math.abs(rec.top)) / 2;
    }

    /**
     * The network code that runs on the server program.
     */
    private void sendReceiveServer() {
        try {
            //send ball coordinates
            mOutStream.writeInt(mBall.getX());
            mOutStream.writeInt(mBall.getY());
            //send coordinates of right paddle
            mOutStream.writeInt(mRightPaddle.getY());
            mOutStream.flush();
            //read coordinates of left paddle
            int y = mInStream.readInt();
            mLeftPaddle.setY(y);

        } catch(IOException e) {
            Log.d(TAG_ERROR, "read/write error (server): " + e);
            mRun = false;
        }
    }

    /**
     * The network code that runs on the client program.
     */
    private void networkClient() {
        try {
            //read coordinates of ball
            int x = mInStream.readInt();
            int y = mInStream.readInt();
            mBall.setCoord(x, y);
            //read coordinates of right paddle
            y = mInStream.readInt();
            mRightPaddle.setY(y);
            //send coordinates of left paddle
            mOutStream.writeInt(mLeftPaddle.getY());
            mOutStream.flush();
        } catch(IOException e) {
            Log.d(TAG_ERROR, "read/write error (client): " + e);
            mRun = false;
        }
    }

    /**
     * Draw ball, paddles, and everything else to canvas.
     * @param c canvas
     */
    private void doDraw(Canvas c) {
        c.drawColor(Color.LTGRAY); //background
        if (mIsDebug) {
            c.drawText("time between frames (ms): " + mDt, 10, 60, mDebugText);
            int fps = 1000 / mDt;
            c.drawText("frames per second: " + fps, 10, 100, mDebugText);
            c.drawText("screen: " + mScreenW + "x" + mScreenH, 10, 140, mDebugText);
            c.drawText("speed of ball: " + mBall.mSpeed, 10, 180, mDebugText);
            c.drawText("IP addresses: " + mIpAddress + "(" + (isServer() ? "server" : "client") + ")", 10, 220, mDebugText);
            c.drawText("sensorY: " + mSensorY, 10, 260, mDebugText);
        }
        String score = mLeftPaddle.getScore() + " : " + mRightPaddle.getScore();
        c.drawText(score, mScreenW / 2, mScoreTextY, mScoreText);

        mBall.draw(c);
        mLeftPaddle.draw(c);
        mRightPaddle.draw(c);
    }


    /**
     * @return a comma separated list of IP addresses found on the local device
     */
    private String getIpAddresses() {
        String ip = "";
        try {
            Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            while (ifaces.hasMoreElements()) {
                NetworkInterface iface = ifaces.nextElement();
                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();

                    if (addr.isSiteLocalAddress()) {
                        if (!ip.equals("")) {
                            ip += ", ";
                        }
                        ip += addr.getHostAddress();
                    }
                }
            }

        } catch (SocketException e) {
            e.printStackTrace();
            ip += "Something Wrong! " + e.toString() + "\n";
        }

        return ip;
    }

    /**
     * Prints a message to the screen.
     * @param msg the message printed on the screen
     */
    private void drawText(String msg) {
        Canvas c = mHolder.lockCanvas();
        if (c == null) {
            return;
        }
        c.drawColor(Color.LTGRAY);
        c.drawText(msg, 10, 60, mDebugText);
        mHolder.unlockCanvasAndPost(c);
    }

    /**
     * Delay the current thread for some time.
     * @param ms the time (in ms) the current thread sleeps
     */
    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {}
    }

    /**
     * Opens a server socket and waits for a client connection.
     * @param port the port on which the server socket listens for incoming connections
     * @throws IOException
     */
    private void openServerSocket(int port) throws IOException {
        drawText("My IP: " + mIpAddress + "  Waiting for client.");
        mServerSocket = new ServerSocket(port);
        mSocket = mServerSocket.accept();
    }

    /**
     * Tries to open a socket connection in an infinite loop till it succeeds.
     * @param port the port on the target host to connect to
     */
    private void openClientSocket(int port) {
        String dots = ""; //a visual indicator; number of dots is number of connect attempts

        while (mSocket == null) {
            try {
                //try connect to server
                drawText("Connecting to " + mAddrServer + "." + dots);
                sleep(1000);
                mSocket = new Socket(mAddrServer, port);
            } catch (UnknownHostException e) {
                Log.d(TAG_ERROR, "connect error: hostname cannot be resolved: " + e);
            } catch (IOException e) {
                Log.d(TAG_ERROR, "connect error (client): " + e);
            }
            dots += ".";
        }
    }

    /**
     * Opens and initializes a socket connection. And gets streams for reading from
     * and writing to the socket.
     * @throws IOException
     */
    private void openNetwork() throws IOException {
        final int PORT = 8080;

        mSocket = null;
        if (isServer()) {
            openServerSocket(PORT);
        } else {
            openClientSocket(PORT);
        }

        //send data immediately; do not buffer
        mSocket.setTcpNoDelay(true);
        //indicate that latency is important
        mSocket.setPerformancePreferences(0, 1, 0);
        //get streams
        mInStream =  new DataInputStream(mSocket.getInputStream());
        mOutStream = new DataOutputStream(mSocket.getOutputStream());
    }

    /**
     * Closes all streams and sockets.
     */
    private void closeNetwork() {
        try {
            if (mSocket != null) {
                mSocket.shutdownInput();
                mSocket.shutdownOutput();
                mSocket.close();
            }
            if (mServerSocket != null) {
                mServerSocket.close();
            }
        } catch (IOException e) {
            Log.d(TAG_ERROR, "network shutdown error:" + e);
            e.printStackTrace();
        }
    }

    /**
     * This program is either running in server or client mode.
     * @return true if running in server mode
     */
    private boolean isServer() {
        return mIsServer;
    }

    /**
     * Convert an x position from virtual coordinates to screen coordinates.
     * @param x an x position (in virtual coordinates)
     * @return the x position in screen coordinates
     */
    public static float scaleX(float x) {
        return x / FIELD_X * mScreenW;
    }

    /**
     * Convert a y position from virtual coordinates to screen coordinates.
     * @param y an y position (in virtual coordinates)
     * @return the y position in screen coordinates
     */
    public static float scaleY(float y) {
        return y / FIELD_Y * mScreenH;
    }

}