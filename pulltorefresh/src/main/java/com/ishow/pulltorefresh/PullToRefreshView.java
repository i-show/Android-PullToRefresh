package com.ishow.pulltorefresh;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ScrollerCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by Bright.Yu on 2017/3/20.
 * PullToRefresh
 */

public class PullToRefreshView extends ViewGroup {
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

    private ScrollerCompat mScroller;

    private float mLastY;
    private float mMovingSum;

    public PullToRefreshView(Context context) {
        this(context, null);
    }

    public PullToRefreshView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PullToRefreshView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
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
            Log.i("nian", "onMeasure: view width = " + view.getMeasuredWidth());
            Log.i("nian", "onMeasure: view height = " + view.getMeasuredHeight());
        }

    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {

        if (mHeaderView != null) {
            View view = mHeaderView.getView();
            view.layout(0, 0, view.getWidth(), view.getHeight());
        }

        if (mTargetView != null) {
            mTargetView.layout(0, 0, getWidth(), getHeight());
        }
    }

//    @Override
//    public boolean dispatchTouchEvent(MotionEvent event) {
//        if (!isEnabled() || mTargetView == null || mHeaderView == null) {
//            return super.dispatchTouchEvent(event);
//        }
//
//        return super.dispatchTouchEvent(event);
//    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mLastY = event.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                float moveY = event.getY();
                mMovingSum += moveY - mLastY;
                mLastY = moveY;
                int moving = mHeaderView.moveing(this, (int) mMovingSum);
                mTargetView.layout(0, moving, getWidth(), mTargetView.getHeight() + moving);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                break;
        }
        return true;
    }

    @SuppressWarnings("unused")
    public void setHeaderView(@NonNull IPullToRefreshHeader header) {
        if (mHeaderView != null) {
            removeView(mHeaderView.getView());
        }

        mHeaderView = header;
        addView(header.getView());
        requestLayout();
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
}
