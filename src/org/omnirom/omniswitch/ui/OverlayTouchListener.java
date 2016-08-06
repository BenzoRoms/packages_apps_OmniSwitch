/*
 *  Copyright (C) 2016 The OmniROM Project
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.omnirom.omniswitch.ui;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewConfiguration;

import org.omnirom.omniswitch.RecentTasksLoader;
import org.omnirom.omniswitch.SwitchConfiguration;
import org.omnirom.omniswitch.SwitchManager;
import org.omnirom.omniswitch.SwitchService;


public class OverlayTouchListener implements OnTouchListener {
    private static final String TAG = "SwipeLayerLayout";
    private static final boolean DEBUG = true;
    private Handler mHandler = new Handler();
    private float[] mInitDownPoint = new float[2];
    private boolean mFlingEnable;
    protected boolean mEnabled;
    private int mSlop;
    private float mLastX;
    private boolean mMoveStarted;
    private boolean mWrongMoveStarted;
    private GestureDetector mGestureDetector;
    private SwitchConfiguration mConfiguration;
    private boolean mSwipeActive;
    private Context mContext;

    private GestureDetector.OnGestureListener mGestureListener = new GestureDetector.OnGestureListener() {
        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            return false;
        }

        @Override
        public void onShowPress(MotionEvent e) {
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            return false;
        }

        @Override
        public void onLongPress(MotionEvent e) {
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            float distanceX = Math.abs(mInitDownPoint[0] - e2.getRawX());
            if (distanceX > mSlop) {
                if (DEBUG) {
                    Log.d(TAG, "onFling open " + velocityX);
                }
                mEnabled = false;
                if (getRecentsManager() != null) {
                    getRecentsManager().openSlideLayout(true);
                }
            }
            return false;
        }

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }
    };

    public OverlayTouchListener(Context context) {
        mContext = context;
        mConfiguration = SwitchConfiguration.getInstance(context);
        ViewConfiguration vc = ViewConfiguration.get(context);
        mSlop = vc.getScaledTouchSlop() / 2;
        mGestureDetector = new GestureDetector(context, mGestureListener);
        mGestureDetector.setIsLongpressEnabled(false);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int action = event.getAction();
        float xRaw = event.getRawX();
        float yRaw = event.getRawY();
        float distanceX = mInitDownPoint[0] - xRaw;
        float distanceY = mInitDownPoint[1] - yRaw;

        if (!mSwipeActive) {
            return true;
        }
        if (getRecentsManager() == null) {
            return true;
        }
        if (DEBUG) {
            Log.d(TAG, "onTouchListener " + action + ":" + (int) xRaw + ":" + (int) yRaw + " mFlingEnable=" + mFlingEnable +
                    " mEnabled=" + mEnabled + " mMoveStarted=" + mMoveStarted +
                    " mWrongMoveStarted=" + mWrongMoveStarted);
        }
        if (mFlingEnable) {
            mGestureDetector.onTouchEvent(event);
        }
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                v.setPressed(true);
                mFlingEnable = false;
                mEnabled = true;
                mMoveStarted = false;
                mWrongMoveStarted = false;

                if (getRecentsManager() != null) {
                    getRecentsManager().clearTasks();
                    RecentTasksLoader.getInstance(mContext).cancelLoadingTasks();
                    RecentTasksLoader.getInstance(mContext).setSwitchManager(getRecentsManager());
                    RecentTasksLoader.getInstance(mContext).preloadTasks();
                }

                mInitDownPoint[0] = xRaw;
                mInitDownPoint[1] = yRaw;
                mLastX = xRaw;
                break;
            case MotionEvent.ACTION_CANCEL:
                v.setPressed(false);
                mFlingEnable = false;
                mEnabled = true;
                mMoveStarted = false;
                mWrongMoveStarted = false;
                break;
            case MotionEvent.ACTION_MOVE:
                if (!mEnabled) {
                    return true;
                }
                v.setPressed(false);
                mFlingEnable = false;
                if (Math.abs(distanceX) > mSlop) {
                    if (mLastX > xRaw) {
                        // move left
                        if (mConfiguration.mLocation == 0 && !mWrongMoveStarted) {
                            mFlingEnable = true;
                            mMoveStarted = true;
                            if (getRecentsManager() != null) {
                                getRecentsManager().showHidden();
                            }
                        } else {
                            mWrongMoveStarted = true;
                        }
                    } else {
                        // move right
                        if (mConfiguration.mLocation != 0 && !mWrongMoveStarted) {
                            mFlingEnable = true;
                            mMoveStarted = true;
                            if (getRecentsManager() != null) {
                                getRecentsManager().showHidden();
                            }
                        } else {
                            mWrongMoveStarted = true;
                        }
                    }
                    if (mMoveStarted) {
                        if (getRecentsManager() != null) {
                            getRecentsManager().slideLayout(distanceX);
                        }
                    }
                }
                mLastX = xRaw;
                break;
            case MotionEvent.ACTION_UP:
                v.setPressed(false);
                mFlingEnable = false;

                if (mEnabled) {
                    if (mMoveStarted) {
                        if (getRecentsManager() != null) {
                            getRecentsManager().finishSlideLayout();
                        }
                    } else {
                        if (getRecentsManager() != null) {
                            getRecentsManager().hideHidden();
                        }
                    }
                }
                mEnabled = true;
                mMoveStarted = false;
                mWrongMoveStarted = false;
                break;
        }
        return true;
    }

    public void setSwipeActive() {
        mSwipeActive = true;
    }

    private SwitchManager getRecentsManager() {
        return SwitchService.getRecentsManager();
    }
}

