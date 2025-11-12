package com.osdmod.android.customviews;

import android.content.Context;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Scroller;

public final class HorizontalPager extends ViewGroup {
    private static final int VELOCITY_UNIT_PIXELS_PER_SECOND = 1000;
    private int mCurrentScreen;
    private int mDensityAdjustedSnapVelocity;
    private boolean mFirstLayout = true;
    private float mLastMotionX;
    private float mLastMotionY;
    private int mMaximumVelocity;
    private int mNextScreen = -1;
    private OnScreenSwitchListener mOnScreenSwitchListener;
    private Scroller mScroller;
    private int mTouchSlop;
    private int mTouchState = 0;
    private VelocityTracker mVelocityTracker;

    public HorizontalPager(Context context) {
        super(context);
        init();
    }

    public HorizontalPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        mScroller = new Scroller(getContext());
        DisplayMetrics displayMetrics = new DisplayMetrics();
        ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay()
                .getMetrics(displayMetrics);
        mDensityAdjustedSnapVelocity = (int) (displayMetrics.density * 600.0f);
        ViewConfiguration configuration = ViewConfiguration.get(getContext());
        mTouchSlop = configuration.getScaledTouchSlop();
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = View.MeasureSpec.getSize(widthMeasureSpec);
        //int mode = View.MeasureSpec.getMode(widthMeasureSpec);
        //int mode2 = View.MeasureSpec.getMode(heightMeasureSpec);
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            getChildAt(i).measure(widthMeasureSpec, heightMeasureSpec);
        }
        if (mFirstLayout) {
            scrollTo(mCurrentScreen * width, 0);
            mFirstLayout = false;
        }
    }

    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int childLeft = 0;
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != View.GONE) {
                int childWidth = child.getMeasuredWidth();
                child.layout(childLeft, 0, childLeft + childWidth, child.getMeasuredHeight());
                childLeft += childWidth;
            }
        }
    }

    /** @noinspection ReassignedVariable*/
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        boolean xMoved;
        boolean yMoved = true;
        switch (ev.getAction()) {
            case 0:
                mLastMotionY = ev.getY();
                mLastMotionX = ev.getX();
                return false;

            case 1:
            case 3:
                mTouchState = 0;
                return false;

            case 2:
                if (mTouchState == 1) {
                    if (mOnScreenSwitchListener != null) {
                        mOnScreenSwitchListener.onScreenSwitched(mCurrentScreen);
                    }
                    return true;
                } else if (mTouchState == -1) {
                    return false;
                } else {
                    float x = ev.getX();
                    xMoved = ((int) Math.abs(x - mLastMotionX)) > mTouchSlop;
                    if (xMoved) {
                        mTouchState = 1;
                        mLastMotionX = x;
                    }
                    if (((int) Math.abs(ev.getY() - mLastMotionY)) <= mTouchSlop) {
                        yMoved = false;
                    }
                    if (!yMoved) {
                        return false;
                    }
                    mTouchState = -1;
                    return false;
                }
            default:
                return false;
        }
    }

    public boolean onTouchEvent(MotionEvent ev) {
        boolean xMoved;
        int availableToScroll;
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(ev);
        int action = ev.getAction();
        float x = ev.getX();
        switch (action) {
            case 0:
                if (!mScroller.isFinished()) {
                    mScroller.abortAnimation();
                }
                mLastMotionX = x;
                if (!mScroller.isFinished()) {
                    mTouchState = 1;
                    break;
                } else {
                    mTouchState = 0;
                    break;
                }
            case 1:
                if (mTouchState == 1) {
                    VelocityTracker velocityTracker = mVelocityTracker;
                    velocityTracker.computeCurrentVelocity(VELOCITY_UNIT_PIXELS_PER_SECOND,
                            (float) mMaximumVelocity);
                    int velocityX = (int) velocityTracker.getXVelocity();
                    if (velocityX > mDensityAdjustedSnapVelocity && mCurrentScreen > 0) {
                        snapToScreen(mCurrentScreen - 1);
                    } else if (velocityX >= (-mDensityAdjustedSnapVelocity) || mCurrentScreen >= getChildCount() - 1) {
                        snapToDestination();
                    } else {
                        snapToScreen(mCurrentScreen + 1);
                    }
                    if (mVelocityTracker != null) {
                        mVelocityTracker.recycle();
                        mVelocityTracker = null;
                    }
                }
                mTouchState = 0;
                break;

            case 2:
                xMoved = ((int) Math.abs(x - mLastMotionX)) > mTouchSlop;
                if (xMoved) {
                    mTouchState = 1;
                }
                if (mTouchState == 1) {
                    int deltaX = (int) (mLastMotionX - x);
                    mLastMotionX = x;
                    int scrollX = getScrollX();
                    if (deltaX >= 0) {
                        if (deltaX > 0 && (availableToScroll = (getChildAt(
                                getChildCount() - 1).getRight() - scrollX) - getWidth()) > 0) {
                            scrollBy(Math.min(availableToScroll, deltaX), 0);
                            break;
                        }
                    } else if (scrollX > 0) {
                        scrollBy(Math.max(-scrollX, deltaX), 0);
                        break;
                    }
                }
                break;

            case 3:
                mTouchState = 0;
                break;
        }
        return true;
    }

    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
            postInvalidate();
        } else if (mNextScreen != -1) {
            mCurrentScreen = Math.max(0, Math.min(mNextScreen, getChildCount() - 1));
            if (mOnScreenSwitchListener != null) {
                mOnScreenSwitchListener.onScreenSwitched(mCurrentScreen);
            }
            mNextScreen = -1;
        }
    }

    public void setOnScreenSwitchListener(OnScreenSwitchListener onScreenSwitchListener) {
        mOnScreenSwitchListener = onScreenSwitchListener;
    }

    private void snapToDestination() {
        int screenWidth = getWidth();
        int scrollX = getScrollX();
        int whichScreen = mCurrentScreen;
        int deltaX = scrollX - (mCurrentScreen * screenWidth);
        if (deltaX < 0 && mCurrentScreen != 0 && screenWidth / 4 < (-deltaX)) {
            whichScreen--;
        } else if (deltaX > 0 && mCurrentScreen + 1 != getChildCount() && screenWidth / 4 < deltaX) {
            whichScreen++;
        }
        snapToScreen(whichScreen);
    }

    private void snapToScreen(int whichScreen) {
        mNextScreen = Math.max(0, Math.min(whichScreen, getChildCount() - 1));
        int delta = (mNextScreen * getWidth()) - getScrollX();
        if (mOnScreenSwitchListener != null) {
            mOnScreenSwitchListener.onScreenSwitched(whichScreen);
        }
        mScroller.startScroll(getScrollX(), 0, delta, 0,
                (int) ((((float) Math.abs(delta)) / ((float) getWidth())) * 500.0f));
        invalidate();
    }

    public interface OnScreenSwitchListener {
        void onScreenSwitched(int i);
    }
}
