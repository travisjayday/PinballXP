/*
 *  Copyright (C) 2012 Fishstix (Gene Ruebsamen - ruebsamen.gene@gmail.com)
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */

package com.fishstix.dosboxfree;

import java.lang.ref.WeakReference;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

import com.fishstix.dosboxfree.dosboxprefs.DosBoxPreferences;
import com.fishstix.dosboxfree.joystick.JoystickClickedListener;
import com.fishstix.dosboxfree.joystick.JoystickMovedListener;
import com.fishstix.dosboxfree.touchevent.TouchEventWrapper;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.InputDevice;
import android.view.InputDevice.MotionRange;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.Toast;

import static com.fishstix.dosboxfree.DBMain.TAG;


public class DBGLSurfaceView extends GLSurfaceView implements SurfaceHolder.Callback {
    private final static int DEFAULT_WIDTH = 320;//800;
    private final static int DEFAULT_HEIGHT = 160;//600;

    private DBMain mParent = null;
    private boolean mSurfaceViewRunning = false;
    public DosBoxVideoThread mVideoThread = null;
    public float controlSpace = 0.2f;
    ///public DosBoxMouseThread mMouseThread = null;

    boolean mScale = false;
    boolean mAbsolute = true;
    boolean mInputLowLatency = false;
    boolean mUseLeftAltOn = false;
    public boolean mDebug = false;
    public boolean mScreenTop = false;
    public boolean mGPURendering = false;
    boolean mMaintainAspect = true;
    public boolean startPlaying = false;
    int	mContextMenu = 0;

    public Bitmap mBitmap = null;
    private Paint mBitmapPaint = null;
    private Paint mTextPaint = null;

    int mSrc_width = 0;
    int mSrc_height = 0;
    final AtomicBoolean bDirtyCoords = new AtomicBoolean(false);
    private int mScroll_x = 0;
    private int mScroll_y = 0;

    final AtomicBoolean mDirty = new AtomicBoolean(false);
    boolean isLandscape = false;
    int mStartLine = 0;
    int mEndLine = 0;

    public int mActionBarHeight;
    public OpenGLRenderer mRenderer;

    class DosBoxVideoThread extends Thread {
        private static final int UPDATE_INTERVAL = 25;
        private static final int UPDATE_INTERVAL_MIN = 20;
        private static final int RESET_INTERVAL = 100;

        private boolean mVideoRunning = false;

        private long startTime = 0;
        private int frameCount = 0;
        private long curTime, nextUpdateTime, sleepTime;

        void setRunning(boolean running) {
            mVideoRunning = running;
        }

        public void run() {
            mVideoRunning = true;

            while (mVideoRunning) {
                if (mSurfaceViewRunning) {

                    curTime = System.currentTimeMillis();

                    if (frameCount > RESET_INTERVAL)
                        frameCount = 0;

                    if (frameCount == 0) {
                        startTime = curTime - UPDATE_INTERVAL;
                    }

                    frameCount++;

                    synchronized (mDirty) {
                        if (mDirty.get()) {
                            if (bDirtyCoords.get()) {
                                calcScreenCoordinates(mSrc_width, mSrc_height, mStartLine, mEndLine);
                            }
                            VideoRedraw(mBitmap, mSrc_width, mSrc_height, mStartLine, mEndLine);
                            mDirty.set(false);
                        }
                    }

                    try {
                        nextUpdateTime = startTime + (frameCount+1) * UPDATE_INTERVAL;
                        sleepTime = nextUpdateTime - System.currentTimeMillis();
                        Thread.sleep(Math.max(sleepTime, UPDATE_INTERVAL_MIN));
                    } catch (InterruptedException e) {
                    }
                }
                else {
                    try {
                        frameCount = 0;
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                    }
                }
            }
        }
    }

    public DBGLSurfaceView(Context context) {
        super(context);
        mDirty.set(false);
        if (!this.isInEditMode()) {
            setup(context);
        }
        Log.i("DosBoxTurbo", "Surface constructor - Default Form");
    }

    public DBGLSurfaceView(Context context, AttributeSet attrs) {
        super(context,attrs);
        if (!this.isInEditMode()) {
            setup(context);
        }
        Log.i("DosBoxTurbo", "Surface constructor - Default Form");
    }

    public DBGLSurfaceView(Context context, AttributeSet attrs, int defStyle) {
        super(context,attrs);

        if (!this.isInEditMode()) {
            setup(context);
        }
        Log.i("DosBoxTurbo", "Surface constructor - Default Form");
    }

    private void setup(Context context) {
        mParent = (DBMain) context;
        //setRenderMode(RENDERMODE_WHEN_DIRTY);

        //gestureScanner = new GestureDetector(context, new MyGestureDetector());
        mBitmapPaint = new Paint();
        mBitmapPaint.setFilterBitmap(true);
        mTextPaint = new Paint();
        mTextPaint.setTextSize(15 * getResources().getDisplayMetrics().density);
        mTextPaint.setTypeface(Typeface.DEFAULT_BOLD);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        mTextPaint.setStyle(Paint.Style.FILL);
        mTextPaint.setSubpixelText(false);

        mBitmap = Bitmap.createBitmap(DEFAULT_WIDTH, DEFAULT_HEIGHT, Bitmap.Config.RGB_565);

        //setEGLContextClientVersion(1);
        mRenderer = new OpenGLRenderer(mParent);
        mRenderer.setBitmap(mBitmap);
        setRenderer(mRenderer);
        setRenderMode(RENDERMODE_WHEN_DIRTY);
        if (mGPURendering) {
            requestRender();
        }

        mVideoThread = new DosBoxVideoThread();
        mVideoThread.setPriority(Thread.MAX_PRIORITY);

        // Receive keyboard events
        requestFocus();
        setFocusableInTouchMode(true);
        setFocusable(true);
        requestFocus();
        requestFocusFromTouch();

        getHolder().addCallback(this);
        getHolder().setFormat(PixelFormat.RGB_565);
        getHolder().setKeepScreenOn(true);
        if (Build.VERSION.SDK_INT >= 14) {
            setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
            setOnSystemUiVisibilityChangeListener(new MySystemUiVisibilityChangeListener());
        } else if (Build.VERSION.SDK_INT >= 11) {
            setSystemUiVisibility(View.STATUS_BAR_HIDDEN);
            setOnSystemUiVisibilityChangeListener(new MySystemUiVisibilityChangeListener());
        }
        updateScreenWidth(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT);
    }

    private class MySystemUiVisibilityChangeListener implements View.OnSystemUiVisibilityChangeListener {
        @Override
        public void onSystemUiVisibilityChange(int visibility) {
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    mParent.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (Build.VERSION.SDK_INT >= 14) {
                                setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
                            } else if (Build.VERSION.SDK_INT >= 11) {
                                setSystemUiVisibility(View.STATUS_BAR_HIDDEN);
                            }
                        }
                    });
                }
            }, 6000);
        }
    }

    public void shutDown() {
        Log.i(DBMain.TAG, "Shutdown called!");
        mBitmap = null;
        mVideoThread = null;
        //	mMouseThread = null;
    }


    public boolean checkGameLoaded() {
        int p = mBitmap.getPixel(300, 100);
        int R = (p & 0xff0000) >> 16;
        int G = (p & 0xff00) >> 8;
        int B = p & 0xff;
        ///Log.i(DBMain.TAG, "pixel: " + R + " G: " + G + "B: " + B);
        if (R != 0 && G !=0  && B != 0) {
            startPlaying = true;
            setDirty();
            return true;
        }
        return false;
    }

    public void calcScreenCoordinates(int src_width, int src_height, int startLine, int endLine) {
        //Log.i("DosBoxTurbo","calcScreenCoordinates()");
        if ((src_width <= 0) || (src_height <= 0))
            return;

        if (startPlaying) {
            mRenderer.width = getWidth();
            mRenderer.height = getHeight();
        }
        else {
            mRenderer.width = DEFAULT_WIDTH;
            mRenderer.height= DEFAULT_HEIGHT;
        }

        isLandscape = (mRenderer.width > mRenderer.height);
        if (!mScale) {
            if (!mMaintainAspect && isLandscape) {
                mRenderer.x = 0;
            } else {
                mRenderer.x = src_width * mRenderer.height /src_height;

                if (mRenderer.x < mRenderer.width) {
                    mRenderer.width = mRenderer.x;
                }
                else if (mRenderer.x > mRenderer.width) {
                    mRenderer.height = src_height * mRenderer.width /src_width;
                }
                mRenderer.x = (getWidth() - mRenderer.width)/2;
            }

            if (isLandscape) {
                mRenderer.width *= (mParent.mPrefScaleFactor * 0.01f);
                mRenderer.height *= (mParent.mPrefScaleFactor * 0.01f);
                mRenderer.x = (getWidth() - mRenderer.width)/2;
                if (!mScreenTop)
                    mRenderer.y = (getHeight() - mRenderer.height)/2;
                else
                    mRenderer.y = 0;
            } else {
                // portrait
                mRenderer.y = mActionBarHeight;
            }
            // no power of two extenstion
            mRenderer.mCropWorkspace[0] = 0;
            mRenderer.mCropWorkspace[1] = src_height;
            mRenderer.mCropWorkspace[2] = src_width;
            mRenderer.mCropWorkspace[3] = -src_height;
        } else {
            if (!mMaintainAspect && isLandscape) {
                mRenderer.x = 0;
            } else {
                mRenderer.x = src_width * mRenderer.height /src_height;

                if (mRenderer.x < mRenderer.width) {
                    mRenderer.width = mRenderer.x;
                }
                else if (mRenderer.x > mRenderer.width) {
                    mRenderer.height = src_height * mRenderer.width /src_width;
                }
                mRenderer.x = (getWidth() - mRenderer.width)/2;
            }

            if (isLandscape) {
                mRenderer.width *= (mParent.mPrefScaleFactor * 0.01f);
                mRenderer.height *= (mParent.mPrefScaleFactor * 0.01f);
                mRenderer.x = (getWidth() - mRenderer.width)/2;
                if (!mScreenTop)
                    mRenderer.y = (getHeight() - mRenderer.height)/2;
                else
                    mRenderer.y = 0;
            } else {
                // portrait
                mRenderer.y = mActionBarHeight;
                mRenderer.width *= 1.6777;
                mRenderer.height *= 1.6777;
                mRenderer.x -= mRenderer.width * 0.01333;
                //mRenderer.y *= 1.5;
                mRenderer.y = (int)((getHeight() - mRenderer.height) * 0.3);
            }
            // no power of two extenstion
            mRenderer.mCropWorkspace[0] = 0;
            mRenderer.mCropWorkspace[1] = src_height;
            mRenderer.mCropWorkspace[2] = src_width;
            mRenderer.mCropWorkspace[3] = -src_height;
            /*
            if ((mScroll_x + src_width) < mRenderer.width)
                mScroll_x = mRenderer.width - src_width;

            if ((mScroll_y + src_height) < mRenderer.height)
                mScroll_y = mRenderer.height - src_height;

            mScroll_x = Math.min(mScroll_x, 0);
            mScroll_y = Math.min(mScroll_y, 0);
            mRenderer.mCropWorkspace[0] = -mScroll_x; // left
            mRenderer.mCropWorkspace[1] = Math.min(mRenderer.height - mScroll_y, src_height) + mScroll_y; // bottom - top
            mRenderer.mCropWorkspace[2] = Math.min(mRenderer.width - mScroll_x, src_width); // right
            mRenderer.mCropWorkspace[3] = -mRenderer.mCropWorkspace[1]; // -(bottom - top)
            mRenderer.width = mRenderer.mCropWorkspace[2]-mRenderer.mCropWorkspace[0];//Math.min(mRenderer.width - mScroll_x, src_width) + mScroll_x;
            mRenderer.height = (Math.max(-mScroll_y, 0) + mScroll_y + mRenderer.mCropWorkspace[1]) - (Math.max(-mScroll_y, 0) + mScroll_y);

            if (isLandscape) {
                mRenderer.x = (getWidth() - mRenderer.width)/2;
                mRenderer.y = 0;
            } else {
                mRenderer.x = (getWidth() - mRenderer.width)/2;
                mRenderer.y = mActionBarHeight;
            }*/

        }
        bDirtyCoords.set(false);
        mRenderer.filter_on = mParent.mPrefScaleFilterOn;
    }
    private Rect mSrcRect = new Rect();
    private Rect mDstRect = new Rect();
    private Rect mDirtyRect = new Rect();
    private int mDirtyCount = 0;
    private void canvasDraw(Bitmap bitmap, int src_width, int src_height, int startLine, int endLine) {
        SurfaceHolder surfaceHolder = getHolder();
        Surface surface = surfaceHolder.getSurface();
        Canvas canvas = null;

        try {
            synchronized (surfaceHolder)
            {

                boolean isDirty = false;

                if (mDirtyCount < 3) {
                    mDirtyCount++;
                    isDirty =  true;
                    startLine = 0;
                    endLine = src_height;
                }

                if (mScale) {
                    mDstRect.set(0, 0, mRenderer.width, mRenderer.height);
                    mSrcRect.set(0, 0, src_width, src_height);
                    mDstRect.offset(mRenderer.x, mRenderer.y);

                    mDirtyRect.set(0, startLine * mRenderer.height / src_height, mRenderer.width, endLine * mRenderer.height / src_height+1);

                    //locnet, 2011-04-21, a strip on right side not updated
                    mDirtyRect.offset(mRenderer.x, mRenderer.y);
                } else {
                    //L,T,R,B
                    mSrcRect.set(-mScroll_x, Math.max(-mScroll_y, startLine), mRenderer.mCropWorkspace[2], Math.min(Math.min(getHeight() - mScroll_y, src_height), endLine));
                    mDstRect.set(0, mSrcRect.top + mScroll_y, mSrcRect.width(), mSrcRect.top + mScroll_y + mSrcRect.height());
                    if (isLandscape) {
                        mDstRect.offset((getWidth() - mSrcRect.width())/2, 0);
                    } else {
                        mDstRect.offset((getWidth() - mSrcRect.width())/2, mActionBarHeight);
                    }

                    mDirtyRect.set(mDstRect);
                }
                if (surface != null && surface.isValid()) {
                    if (isDirty) {
                        canvas = surfaceHolder.lockCanvas(null);
                        canvas.drawColor(0xff000000);
                    } else {
                        canvas = surfaceHolder.lockCanvas(mDirtyRect);
                    }

                    if (mScale) {
                        canvas.drawBitmap(bitmap, mSrcRect, mDstRect, (mParent.mPrefScaleFilterOn)?mBitmapPaint:null);
                    } else {
                        canvas.drawBitmap(bitmap, mSrcRect, mDstRect, null);
                    }

                }
            }
        } finally {
            if (canvas != null && surface != null && surface.isValid()) {
                surfaceHolder.unlockCanvasAndPost(canvas);
            }
        }

        surfaceHolder = null;
    }

    public void VideoRedraw(Bitmap bitmap, int src_width, int src_height, int startLine, int endLine) {
        if (!mSurfaceViewRunning || (bitmap == null) || (src_width <= 0) || (src_height <= 0))
            return;

        if (mGPURendering) {
            mRenderer.setBitmap(bitmap);
            //Log.i("DosBoxTurbo", "rendering with gpu");
            requestRender();
        } else {
            canvasDraw(bitmap,src_width,src_height,startLine,endLine);
            //Log.i("DosBoxTurbo", "rendering wiWITHOUTth gpu");
        }
    }

    int scrnWidth = 0;
    int scrnHeight = 0;
    int scrnMiddle = 0;

    public void updateScreenWidth(boolean portrait) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        ((Activity)getContext()).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        //scrnHeight = displayMetrics.heightPixels;
        scrnWidth = displayMetrics.widthPixels;
        scrnHeight = displayMetrics.heightPixels;
        scrnMiddle = scrnWidth / 2;
    }

    long lastTimeClickA = 0;
    long lastTimeClickD = 0;
    int rightId = MotionEvent.INVALID_POINTER_ID;
    int leftId = MotionEvent.INVALID_POINTER_ID;
    int plungerId = MotionEvent.INVALID_POINTER_ID;

    private void smallWait() {
        try {
            Thread.sleep(15);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Thread.yield();
    }

    private static final int msDelay = 0;
    @Override
    public boolean onTouchEvent(final MotionEvent event) {
        float x = event.getX(event.getActionIndex());
        float y = event.getY(event.getActionIndex());

        if (event.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN || event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            if (y > scrnHeight * (1.0f - controlSpace)) {
                // middle plunger button
                plungerId = event.getPointerId(event.getActionIndex());
                DosBoxControl.sendNativeKey(101, true, false, false, false);
                //Log.d("DosBoxTurbo", "plunger down; id = " + plungerId);
            }
            // left flipper click
            else if ((int) x < scrnMiddle) {
                leftId = event.getPointerId(event.getActionIndex());
              //  if (System.currentTimeMillis() - lastTimeClickA > msDelay) {
                DosBoxControl.sendNativeKey(100, true, false, false, false);
                //    lastTimeClickA = System.currentTimeMillis();
                //}
                //Log.d("DosBoxTurbo", "left flipper down; id = " + leftId);
            }
            // right flipper
            else {
                rightId = event.getPointerId(event.getActionIndex());
                //Log.d("DosBoxTurbo", "right flipper down; id: " + rightId);
                //if (System.currentTimeMillis() - lastTimeClickD > msDelay) {
                DosBoxControl.sendNativeKey(102, true, false, false, false);
                  //  lastTimeClickD = System.currentTimeMillis();
                //}
            }
        }
        else if (event.getActionMasked() == MotionEvent.ACTION_POINTER_UP || event.getActionMasked() == MotionEvent.ACTION_UP) {
            //Log.d("DosBoxTurbo", "" + event.getPointerId(event.getActionIndex()));
            if (event.getPointerId(event.getActionIndex()) == leftId) {
                DosBoxControl.sendNativeKey(100, false, false, false, false);
                leftId = MotionEvent.INVALID_POINTER_ID;
            } else if (event.getPointerId(event.getActionIndex()) == rightId) {
                DosBoxControl.sendNativeKey(102, false, false, false, false);
                rightId = MotionEvent.INVALID_POINTER_ID;
            }
            else {
                DosBoxControl.sendNativeKey(101, false, false, false, false);
                plungerId = MotionEvent.INVALID_POINTER_ID;
            }
        }

        smallWait();
        return true;//gestureScanner.onTouchEvent(event);
    }

    public void setDirty() {
        mDirtyCount = 0;
        bDirtyCoords.set(true);
        mDirty.set(true);
    }

    public void resetScreen(boolean redraw) {
        setDirty();
        mScroll_x = 0;
        mScroll_y = 0;

        if (redraw)
            forceRedraw();
    }

    public void forceRedraw() {
        setDirty();
        VideoRedraw(mBitmap, mSrc_width, mSrc_height, 0, mSrc_height);
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {
        resetScreen(true);
        if (mGPURendering)
            super.surfaceChanged(holder, format, width, height);
    }

    public void surfaceCreated(SurfaceHolder holder) {
        mSurfaceViewRunning = true;
        if (mGPURendering)
            super.surfaceCreated(holder);
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        mSurfaceViewRunning = false;
        if (mGPURendering)
            super.surfaceDestroyed(holder);
    }
}
