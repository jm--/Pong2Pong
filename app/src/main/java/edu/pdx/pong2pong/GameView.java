package edu.pdx.pong2pong;
/**
 * Uses code snippets from the Android SDK LunarLander sample program:
 * http://grepcode.com/file/repository.grepcode.com/java/ext/com.google.android/android-apps/2.3.1_r1/com/example/android/lunarlander/LunarView.java
 *
 */

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

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

public class GameView extends SurfaceView
        implements SurfaceHolder.Callback, Runnable {

    private static String TAG_ERROR = "PONGLOG_ERROR_GameView";
    private static String TAG_MSG = "PONGLOG_MSG_GameView";

    /** the Pong ball */
    Ball mBall;

    /** the left paddle */
    Paddle mLeftPaddle;

    /** the right paddle */
    Paddle mRightPaddle;

    /** the paddle the user controls; either the left or the right paddle */
    Paddle mMyPaddle;

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
    //private final Object mRunLock = new Object();

    /** Handle to the surface manager object we interact with */
    private SurfaceHolder mHolder;

    private Context mContext;

    /** time (ms) between frames; one iteration of the main processing loop in run() */
    private int mDt;

    /** comma separated list of local IP addresses */
    private String mIpAddress;

    private DataInputStream mInStream;
    private DataOutputStream mOutStream;

    private Socket mSocket = null;
    private ServerSocket mServerSocket = null;

    /** the IP address of the device running in server mode */
    private String mAddrServer;

    /** true if the program is running in server mode; false if program runs in client mode */
    private boolean mIsServer;

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

        //paint objects used for drawing text
        paintText.setStrokeWidth(1);
        paintText.setStyle(Paint.Style.FILL);
        paintText.setTextSize(30);

        //get local IP addresses
        mIpAddress = getIpAddresses();

        // there is no wifi-direct on emulators, so to test on emulators, start e.g. 2 emulators:
        // "emulator -avd MY_AVD1 -shared-net-id 42"
        // "emulator -avd MY_AVD2 -shared-net-id 43"
        // this starts a virtual LAN with IPs 10.1.2.42 and 10.1.2.43
        // R.string.test_server_addr needs to be one of the two IP addresses
        if (mAddrServer == null || mAddrServer == "") {
            mAddrServer = context.getResources().getString(R.string.test_server_addr);
            mIsServer = mIpAddress.contains(mAddrServer);
        }
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

        float y = event.getY();
        mMyPaddle.setY(y);
        return true;
    }

    @Override
    public void run() {
        try {
            openNetwork();
            setupResolution();
        } catch(Exception e) {
            Log.d(TAG_ERROR, "Network error: " + e);
            e.printStackTrace();
            drawText("Network error: " + e);
            //Toast.makeText(mContext, "Network Error", Toast.LENGTH_SHORT).show();
            return;
        }
        long timeEnd = System.currentTimeMillis();
        mBall.start();

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

            if (isServer()) {
                //the server program controls the ball
                mBall.move(mLeftPaddle, mRightPaddle, mDt);

                if (mBall.getX() < 0 || mBall.getX() > mScreenW) {
                    //ball is outside game area - create a new ball/game
                    mBall.start();
                }
                sendReceiveServer();
            } else {
                networkClient();
            }
        }
        closeNetwork();
    }

    /**
     * Exchange screen resolution.
     */
    private void setupResolution() throws IOException {
        //send own resolution
        mOutStream.writeInt(mScreenW);
        mOutStream.writeInt(mScreenH);
        //read resolution of other device
        int x = mInStream.readInt();
        int y = mInStream.readInt();

        if (x < mScreenW) {
            mScreenW = x;
        }
        if (y < mScreenH) {
            mScreenH = y;
        }

        mBall = new Ball(mScreenW, mScreenH);
        mLeftPaddle = new Paddle(20, mScreenH / 2);
        mRightPaddle = new Paddle(mScreenW - 20, mScreenH / 2);
        mMyPaddle = isServer() ? mRightPaddle : mLeftPaddle;
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
            mOutStream.writeInt(mRightPaddle.getX());
            mOutStream.writeInt(mRightPaddle.getY());
            mOutStream.flush();
            //read coordinates of left paddle
            int x = mInStream.readInt();
            int y = mInStream.readInt();
            mLeftPaddle.setCoord(x, y);

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
            x = mInStream.readInt();
            y = mInStream.readInt();
            mRightPaddle.setCoord(x, y);
            //send coordinates of left paddle
            mOutStream.writeInt(mLeftPaddle.getX());
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
        c.drawText("time between frames (ms): " + mDt, 10, 60, paintText);
        int fps = 1000 / mDt;
        c.drawText("frames per second: " + fps, 10, 100, paintText);
        c.drawText("screen: " + mScreenW + "x" + mScreenH, 10, 140, paintText);
        c.drawText("speed of ball: " + mBall.mSpeed, 10, 180, paintText);
        c.drawText("IP addresses: " + mIpAddress + "(" + (isServer() ? "server":"client") + ")", 10, 220, paintText);

        mBall.draw(c);
        mLeftPaddle.draw(c);
        mRightPaddle.draw(c);
    }


    /**
     * @return a comma separated list of IP addresses found on local device
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
                        if (ip != "") {
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
     * Print msg to the screen.
     * @param msg
     */
    private void drawText(String msg) {
        Canvas c = mHolder.lockCanvas();
        if (c == null) {
            return;
        }
        c.drawColor(Color.LTGRAY);
        c.drawText(msg, 10, 60, paintText);
        mHolder.unlockCanvasAndPost(c);
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {}
    }

    private void openServerSocket(int port) throws IOException {
        drawText("My IP: " + mIpAddress + " Waiting for client.");
        mServerSocket = new ServerSocket(port);
        mSocket = mServerSocket.accept();
    }

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

    private void openNetwork() throws IOException {
        final int PORT = 8080;

        mSocket = null;
        if (isServer()) {
            openServerSocket(PORT);
        } else {
            openClientSocket(PORT);
        }

        //indicate that latency is important
        mSocket.setPerformancePreferences(0, 1, 0);
        //get streams
        mInStream =  new DataInputStream(mSocket.getInputStream());
        mOutStream = new DataOutputStream(mSocket.getOutputStream());
    }

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

}