package com.ishow.pulltorefresh;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ScrollerCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.AbsListView;

/**
 * Created by Bright.Yu on 2017/3/20.
 * PullToRefresh
 */
public class PullToRefreshView extends ViewGroup {
    private static final String TAG = "PullToRefreshView";
    /**
     * HeaderView
     */
    private IPullToRefreshHeader mHeaderView;
    /**
     * TargetView
     */
    private View mTargetView;
    /**
     * FooterView
     */
    private IPullToRefreshFooter mFooterView;

    /**
     * 上次的位置
     */
    private int mMovingSum;
    private float mLastY;
    private float mBeingDraggedY;

    private float mInitialDownY;
    private float mInitialMotionY;
    /**
     * 滑动距离的判断
     */
    private int mTouchSlop;

    private boolean mIsBeingDragged;

    private ScrollerCompat mScroller;
    /**
     * 监听
     */
    private OnPullToRefreshListener mPullToRefreshListener;

    public PullToRefreshView(Context context) {
        this(context, null);
    }

    public PullToRefreshView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PullToRefreshView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        final ViewConfiguration configuration = ViewConfiguration.get(context);
        mTouchSlop = configuration.getScaledTouchSlop() * 2;
        mScroller = ScrollerCompat.create(context);
    }


    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        final int childCount = getChildCount();
        if (childCount <= 0) {
            throw new IllegalStateException("need a child");
        } else if (childCount == 1) {
            mTargetView = getChildAt(0);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (mHeaderView != null) {
            View view = mHeaderView.getView();
            measureChild(view, widthMeasureSpec, heightMeasureSpec);
        }

    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {

        if (mHeaderView != null) {
            View view = mHeaderView.getView();
            view.layout(0, -view.getMeasuredHeight(), view.getMeasuredWidth(), 0);
        }

        if (mTargetView != null) {
            mTargetView.layout(0, 0, getMeasuredWidth(), getMeasuredHeight());
        }
    }


    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (!isEnabled() || canScrollUp() || isRefreshing()) {
            return false;
        }

        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mIsBeingDragged = false;
                mInitialDownY = ev.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                final float y = ev.getY();
                startDragging(y);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mIsBeingDragged = false;
                break;
        }
        return mIsBeingDragged;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled() || canScrollUp() || isRefreshing()) {
            return false;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mIsBeingDragged = false;
                break;
            case MotionEvent.ACTION_MOVE:
                final int y = (int) event.getY();
                startDragging(y);
                if (mIsBeingDragged) {
                    final float offset = y - mLastY;
                    mMovingSum = (int) (y - mBeingDraggedY);
                    mLastY = y;
                    final int offsetResult = mHeaderView.moving(this, mMovingSum, (int) offset);
                    ViewCompat.offsetTopAndBottom(mTargetView, offsetResult);
                }

                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (mHeaderView.isEffectiveDistance(mMovingSum)) {
                    Log.i(TAG, "onTouchEvent: getTop = " + mTargetView.getTop());
                    mHeaderView.setStatus(IPullToRefreshHeader.STATUS_REFRESHING);
                    int tagetOffset = mHeaderView.refreshing(this, mMovingSum);
                    mScroller.startScroll(0, 0, 0, tagetOffset, 1000);
                    postInvalidate();
                    notifyRefresh();
                }
                break;
        }
        return true;
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean b) {
        // if this is a List < L or another view that doesn't support nested
        // scrolling, ignore this request so that the vertical scroll event
        // isn't stolen
        if ((android.os.Build.VERSION.SDK_INT < 21 && mTargetView instanceof AbsListView)
                || (mTargetView != null && !ViewCompat.isNestedScrollingEnabled(mTargetView))) {
            // Nope.
        } else {
            super.requestDisallowInterceptTouchEvent(b);
        }
    }


    @SuppressWarnings("unused")
    public void setHeaderView(@NonNull IPullToRefreshHeader header) {
        if (mHeaderView != null) {
            removeView(mHeaderView.getView());
        }

        mHeaderView = header;
        LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        addView(header.getView(), lp);
        requestLayout();
    }

    private void startDragging(float y) {
        final float yDiff = y - mInitialDownY;
        if (yDiff > mTouchSlop && !mIsBeingDragged) {
            mLastY = y;
            mInitialMotionY = mInitialDownY + mTouchSlop;
            mIsBeingDragged = true;
            mBeingDraggedY = y;
        }
    }

    private boolean isRefreshing() {
        return mHeaderView != null && mHeaderView.getStatus() == IPullToRefreshHeader.STATUS_REFRESHING;
    }

    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
            postInvalidate();
        }
        super.computeScroll();
    }

    /**
     * 是否可以上滑
     * 下拉刷新的时候使用
     */
    private boolean canScrollUp() {
        return mTargetView != null && ViewCompat.canScrollVertically(mTargetView, -1);
    }

    /**
     * 是否可以下滑
     */
    private boolean canScrollDown() {
        return mTargetView != null && ViewCompat.canScrollVertically(mTargetView, 1);
    }

    public void setRefreshSuccess() {
        mHeaderView.setStatus(IPullToRefreshHeader.STATUS_SUCCESS);
        postInvalidate();
    }

    public void setRefreshFailed() {
        mHeaderView.setStatus(IPullToRefreshHeader.STATUS_FAILED);
        postInvalidate();
    }

    public void setOnPullToRefreshListener(OnPullToRefreshListener listener) {
        mPullToRefreshListener = listener;
    }

    private void notifyRefresh() {
        if (mPullToRefreshListener != null) {
            mPullToRefreshListener.onRefresh(this);
        }
    }

    private void notifyLoadMore() {
        if (mPullToRefreshListener != null) {
            mPullToRefreshListener.onLoadMore(this);
        }
    }
}
