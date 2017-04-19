package com.ishow.pulltorefresh;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.view.NestedScrollingChild;
import android.support.v4.view.NestedScrollingChildHelper;
import android.support.v4.view.NestedScrollingParent;
import android.support.v4.view.NestedScrollingParentHelper;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.AbsListView;

import com.ishow.pulltorefresh.utils.ViewHelper;

/**
 * Created by Bright.Yu on 2017/3/20.
 * PullToRefresh
 */
public class PullToRefreshView extends ViewGroup implements NestedScrollingParent, NestedScrollingChild {
    private static final String TAG = "PullToRefreshView";
    /**
     * 设置了时间的时候进行配置一个时间间隔
     */
    private static final int ANI_INTERVAL = 500;
    /**
     * HeaderView
     */
    private IPullToRefreshHeader mHeader;
    /**
     * TargetView
     */
    private View mTargetView;
    /**
     * FooterView
     */
    private IPullToRefreshFooter mFooter;

    /**
     * 上次的位置
     */
    private int mMovingSum;
    private float mLastY;
    private float mBeingDraggedY;

    private float mInitialDownY;
    /**
     * 滑动距离的判断
     */
    private int mTouchSlop;

    private boolean mIsBeingDragged;
    private boolean mIsBeingDraggedUp;
    private boolean mIsBeingDraggedDown;

    /**
     * 监听
     */
    private OnPullToRefreshListener mPullToRefreshListener;

    private Handler mHandler;

    // 在调用onLayout的时候使用
    private int mHeaderOffsetBottom;
    private int mTargetOffsetTop;

    // If nested scrolling is enabled, the total amount that needed to be
    // consumed by this as the nested scrolling parent is used in place of the
    // overscroll determined by MOVE events in the onTouch handler
    private float mRefreshTotalUnconsumed;
    private float mLoadMoreTotalUnconsumed;
    private final NestedScrollingParentHelper mNestedScrollingParentHelper;
    private final NestedScrollingChildHelper mNestedScrollingChildHelper;
    private final int[] mParentScrollConsumed = new int[2];
    private final int[] mParentOffsetInWindow = new int[2];
    private boolean mNestedScrollInProgress;

    public PullToRefreshView(Context context) {
        this(context, null);
    }

    public PullToRefreshView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PullToRefreshView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        final ViewConfiguration configuration = ViewConfiguration.get(context);
        mTouchSlop = configuration.getScaledTouchSlop();
        mHandler = new Handler();

        mNestedScrollingParentHelper = new NestedScrollingParentHelper(this);
        mNestedScrollingChildHelper = new NestedScrollingChildHelper(this);
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
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mHandler.removeCallbacksAndMessages(null);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        final int width = getMeasuredWidth();
        final int height = getMeasuredHeight();


        if (mHeader != null) {
            View view = mHeader.getView();
            measureChild(view, widthMeasureSpec, heightMeasureSpec);
        }
        if (mTargetView != null) {
            int widthM = MeasureSpec.makeMeasureSpec(width - getPaddingLeft() - getPaddingRight(), MeasureSpec.EXACTLY);
            int heightM = MeasureSpec.makeMeasureSpec(height - getPaddingTop() - getPaddingBottom(), MeasureSpec.EXACTLY);
            mTargetView.measure(widthM, heightM);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        final int width = getMeasuredWidth();
        final int height = getMeasuredHeight();
        Log.i(TAG, "onLayout: ============================");
        Log.i(TAG, "onLayout: mHeaderOffsetBottom = " + mHeaderOffsetBottom);
        Log.i(TAG, "onLayout: mTargetOffsetTop = " + mTargetOffsetTop);
        if (mHeader != null) {
            View view = mHeader.getView();
            view.layout(0, -view.getMeasuredHeight() + mHeaderOffsetBottom, view.getMeasuredWidth(), mHeaderOffsetBottom);
        }

        if (mTargetView != null) {
            final View child = mTargetView;
            final int childLeft = getPaddingLeft();
            final int childTop = getPaddingTop();
            final int childWidth = width - getPaddingLeft() - getPaddingRight();
            final int childHeight = height - getPaddingTop() - getPaddingBottom();
            child.layout(childLeft, childTop + mTargetOffsetTop, childLeft + childWidth, childTop + childHeight + mTargetOffsetTop);
        }
    }


    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        final boolean canRefesh = canRefresh();
        final boolean canLoadMore = canLoadMore();
        if (!isEnabled() || (!canRefesh && !canLoadMore) || mNestedScrollInProgress) {
            return false;
        }

        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mIsBeingDragged = false;
                mIsBeingDraggedUp = false;
                mIsBeingDraggedDown = false;
                mInitialDownY = ev.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                final float y = ev.getY();
                startDragging(y);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mIsBeingDragged = false;
                mIsBeingDraggedUp = false;
                mIsBeingDraggedDown = false;
                break;
        }

        if (mIsBeingDraggedUp && canLoadMore) {
            return true;
        } else if (mIsBeingDraggedDown && canRefesh) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final boolean canRefesh = canRefresh();
        final boolean canLoadMore = canLoadMore();
        if (!isEnabled() || (!canRefesh && !canLoadMore) || mNestedScrollInProgress) {
            return false;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mIsBeingDragged = false;
                break;
            case MotionEvent.ACTION_MOVE:
                final int y = (int) event.getY();
                startDragging(y);
                if (!mIsBeingDragged) {
                    break;
                }

                mMovingSum = (int) (y - mBeingDraggedY);
                final float offset = mLastY - y;
                mLastY = y;
                if (mIsBeingDraggedDown) {
                    movingHeader(mMovingSum, (int) offset);
                } else if (mIsBeingDraggedUp) {
                    movingFooter(mMovingSum, (int) offset);
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (mMovingSum > 0) {
                    updateHeaderWhenUpOrCancel(mMovingSum);
                } else {
                    updateFooterWhenUpOrCancel(mMovingSum);
                }
                mIsBeingDragged = false;
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
    public void setHeader(@NonNull IPullToRefreshHeader header) {
        if (mHeader != null) {
            removeView(mHeader.getView());
        }

        mHeader = header;
        LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        addView(header.getView(), lp);
        requestLayout();
    }

    /**
     * Header 移动
     */
    private synchronized void movingHeader(final int total, final int offset) {
        if (mHeader == null) {
            Log.i(TAG, "movingHeader: header is null");
            return;
        }

        final int offsetResult = mHeader.moving(this, total, offset);
        mHeaderOffsetBottom = mHeader.getBottom();

        ViewCompat.offsetTopAndBottom(mTargetView, offsetResult);
        mTargetOffsetTop = mTargetView.getTop();

        if (mHeader.isEffectiveDistance(total)) {
            mHeader.setStatus(IPullToRefreshHeader.STATUS_READY);
        } else {
            mHeader.setStatus(IPullToRefreshHeader.STATUS_NORMAL);
        }
    }

    private synchronized void updateHeaderWhenUpOrCancel(final int total) {
        if (mHeader == null) {
            Log.i(TAG, "updateHeaderWhenUpOrCancel: header is null");
            return;
        }

        if (mHeader.isEffectiveDistance(total)) {
            mHeader.setStatus(IPullToRefreshHeader.STATUS_REFRESHING);
            int offset = mHeader.refreshing(this, total, mRefreshListener);
            mHeaderOffsetBottom = mHeader.getBottom();
            ViewHelper.movingY(mTargetView, offset, mRefreshListener);
            notifyRefresh();
        } else {
            int offset = mHeader.cancelRefresh(this);
            ViewHelper.movingY(mTargetView, offset, mSetRefreshNormalListener);
        }
    }

    /**
     * 在这里Footer比较特殊不能直接Add到 这里面，需要在Target里面实现
     */
    @SuppressWarnings("unused")
    public void setFooter(@NonNull IPullToRefreshFooter footer) {
        mFooter = footer;
    }

    /**
     * Footer的移动
     */
    private void movingFooter(final int total, final int offset) {
        if (mFooter == null) {
            Log.i(TAG, "movingFooter: mFooter is null");
            return;
        }
        final int offsetResult = mFooter.moving(this, mTargetView, total, offset);
        ViewCompat.offsetTopAndBottom(mTargetView, offsetResult);
        mTargetOffsetTop = mTargetView.getTop();
        if (mFooter.isEffectiveDistance(this, mTargetView, mMovingSum)) {
            mFooter.setStatus(IPullToRefreshFooter.STATUS_READY);
        } else {
            mFooter.setStatus(IPullToRefreshFooter.STATUS_NORMAL);
        }
    }

    /**
     * 取消时候进行的操作
     */
    private void updateFooterWhenUpOrCancel(final int total) {
        if (mFooter == null) {
            Log.i(TAG, "updateFooterWhenUpOrCancel: mFooter is null");
            return;
        }

        if (mFooter.isEffectiveDistance(this, mTargetView, total)) {
            mFooter.setStatus(IPullToRefreshFooter.STATUS_LOADING);
            int offset = mFooter.loading(this, mTargetView, total);
            ViewHelper.movingY(mTargetView, offset, mRefreshListener);
            notifyLoadMore();
        } else {
            int offset = mFooter.cancelLoadMore(this, mTargetView);
            ViewHelper.movingY(mTargetView, offset, mSetLoadNormalListener);
        }
    }

    /**
     * 判断是否可以进行拖拽
     */
    private void startDragging(float y) {
        final float yDiff = y - mInitialDownY;
        if (Math.abs(yDiff) > mTouchSlop && !mIsBeingDragged) {
            mLastY = y;
            mIsBeingDragged = true;
            mBeingDraggedY = y;
            if (yDiff > 0) {
                // 开始下拉刷新
                mIsBeingDraggedDown = true;
            } else {
                // 开始上拉
                mIsBeingDraggedUp = true;
            }
        }
    }

    /**
     * 是否可以进行刷新操作
     */
    private boolean canRefresh() {
        if (mHeader == null) {
            return false;
        }

        final int status = mHeader.getStatus();

        boolean isStatusOk = status == IPullToRefreshHeader.STATUS_NORMAL ||
                status == IPullToRefreshHeader.STATUS_READY;

        return !canScrollUp() && isStatusOk;
    }


    /**
     * 是否可以进行加载更多操作
     */
    private boolean canLoadMore() {

        if (mFooter == null) {
            return false;
        }

        final int status = mFooter.getStatus();

        boolean isStatusOk = (status == IPullToRefreshFooter.STATUS_NORMAL ||
                status == IPullToRefreshFooter.STATUS_READY);

        return !canScrollDown() && isStatusOk;
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

    /**
     * 刷新成功
     */
    @SuppressWarnings("unused")
    public void setRefreshSuccess() {
        mHeader.setStatus(IPullToRefreshHeader.STATUS_SUCCESS);
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                int offset = mHeader.refreshSuccess(PullToRefreshView.this);
                ViewHelper.movingY(mTargetView, offset, mSetRefreshNormalListener);
            }
        }, ANI_INTERVAL);

    }

    /**
     * 刷新失败
     */
    @SuppressWarnings("unused")
    public void setRefreshFailed() {
        mHeader.setStatus(IPullToRefreshHeader.STATUS_FAILED);
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                int offset = mHeader.refreshSuccess(PullToRefreshView.this);
                ViewHelper.movingY(mTargetView, offset, mSetRefreshNormalListener);
            }
        }, ANI_INTERVAL);
    }

    /**
     * 加载完成
     */
    @SuppressWarnings("unused")
    public void setLoadMoreNormal() {
        if (mFooter != null) {
            mFooter.setStatus(IPullToRefreshFooter.STATUS_NORMAL);
        }
    }

    /**
     * 加载完成
     */
    @SuppressWarnings("unused")
    public void setLoadMoreEnd() {
        if (mTargetView == null) {
            return;
        }

        Object object = mTargetView.getTag(R.id.tag_pull_to_refresh_animation);
        if (object != null) {
            ValueAnimator animator = (ValueAnimator) object;
            animator.removeAllListeners();
        }

        if (mFooter != null) {
            mFooter.setStatus(IPullToRefreshFooter.STATUS_END);
        }
        requestLayout();
    }

    /**
     * 下拉加载失败
     */
    @SuppressWarnings("unused")
    public void setLoadMoreFailed() {
        if (mFooter == null) {
            Log.i(TAG, "setLoadMoreFailed: mFooter is null");
            return;
        }
        mFooter.setStatus(IPullToRefreshFooter.STATUS_FAILED);
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                int offset = mFooter.loadFailed(PullToRefreshView.this);
                ViewHelper.movingY(mTargetView, offset, mSetLoadNormalListener);
            }
        }, ANI_INTERVAL);
    }

    /**
     * 下拉加载成功
     */
    @SuppressWarnings("unused")
    public void setLoadMoreSuccess() {
        if (mFooter == null) {
            Log.i(TAG, "setLoadMoreSuccess: mFooter is null");
            return;
        }
        mFooter.setStatus(IPullToRefreshFooter.STATUS_SUCCESS);
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                int offset = mFooter.loadSuccess(PullToRefreshView.this);
                ViewHelper.movingY(mTargetView, offset, mSetLoadNormalListener);
            }
        }, ANI_INTERVAL);
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

    private boolean isRefreshingOrLoading() {
        if (mHeader != null && mHeader.getStatus() == IPullToRefreshHeader.STATUS_REFRESHING) {
            return true;
        } else if (mFooter != null && mFooter.getStatus() == IPullToRefreshFooter.STATUS_LOADING) {
            return true;
        } else {
            return false;
        }
    }

    private Animator.AnimatorListener mRefreshListener = new Animator.AnimatorListener() {
        @Override
        public void onAnimationStart(Animator animation) {

        }

        @Override
        public void onAnimationEnd(Animator animation) {
            if (mHeader != null) {
                mHeaderOffsetBottom = mHeader.getBottom();
            }

            if (mTargetView != null) {
                mTargetOffsetTop = mTargetView.getTop();
            }
        }

        @Override
        public void onAnimationCancel(Animator animation) {

        }

        @Override
        public void onAnimationRepeat(Animator animation) {

        }
    };

    /**
     * 重置为Normal状态
     */
    private Animator.AnimatorListener mSetRefreshNormalListener = new Animator.AnimatorListener() {
        @Override
        public void onAnimationStart(Animator animation) {

        }

        @Override
        public void onAnimationEnd(Animator animation) {
            if (mHeader != null) {
                mHeader.setStatus(IPullToRefreshHeader.STATUS_NORMAL);
                mHeaderOffsetBottom = mHeader.getBottom();
            }

            if (mTargetView != null) {
                mTargetOffsetTop = mTargetView.getTop();
            }
        }

        @Override
        public void onAnimationCancel(Animator animation) {

        }

        @Override
        public void onAnimationRepeat(Animator animation) {

        }
    };


    /**
     * 重置为Normal状态
     */
    private Animator.AnimatorListener mSetLoadNormalListener = new Animator.AnimatorListener() {
        @Override
        public void onAnimationStart(Animator animation) {

        }

        @Override
        public void onAnimationEnd(Animator animation) {

            if (mHeader != null) {
                mHeader.setStatus(IPullToRefreshHeader.STATUS_NORMAL);
                mHeaderOffsetBottom = mHeader.getBottom();
            }

            if (mFooter != null) {
                mFooter.setStatus(IPullToRefreshFooter.STATUS_NORMAL);
            }

            if (mTargetView != null) {
                mTargetOffsetTop = mTargetView.getTop();
            }
        }

        @Override
        public void onAnimationCancel(Animator animation) {

        }

        @Override
        public void onAnimationRepeat(Animator animation) {

        }
    };

    // NestedScrollingParent
    @Override
    public boolean onStartNestedScroll(View child, View target, int nestedScrollAxes) {
        // Enable && 没有在刷新OrLoading && 是Y轴滑动
        return isEnabled()
                && !isRefreshingOrLoading()
                && (nestedScrollAxes & ViewCompat.SCROLL_AXIS_VERTICAL) != 0;
    }

    @Override
    public void onNestedScrollAccepted(View child, View target, int axes) {
        // Reset the counter of how much leftover scroll needs to be consumed.
        mNestedScrollingParentHelper.onNestedScrollAccepted(child, target, axes);
        // Dispatch up to the nested parent
        startNestedScroll(axes & ViewCompat.SCROLL_AXIS_VERTICAL);
        mRefreshTotalUnconsumed = 0;
        mLoadMoreTotalUnconsumed = 0;
        mNestedScrollInProgress = true;
    }

    @Override
    public void onNestedPreScroll(View target, int dx, int dy, int[] consumed) {
        // If we are in the middle of consuming, a scroll, then we want to move the spinner back up
        // before allowing the list to scroll
        if (dy > 0 && mRefreshTotalUnconsumed > 0) {
            if (dy > mRefreshTotalUnconsumed) {
                consumed[1] = dy - (int) mRefreshTotalUnconsumed;
                mRefreshTotalUnconsumed = 0;
            } else {
                mRefreshTotalUnconsumed -= dy;
                consumed[1] = dy;
            }
            movingHeader((int) mRefreshTotalUnconsumed, dy);
        }

        if (dy < 0 && mLoadMoreTotalUnconsumed > 0) {
            if (Math.abs(dy) > mLoadMoreTotalUnconsumed) {
                consumed[1] = dy - (int) mLoadMoreTotalUnconsumed;
                mLoadMoreTotalUnconsumed = 0;
            } else {
                mLoadMoreTotalUnconsumed -= Math.abs(dy);
                consumed[1] = dy;
            }
            movingFooter((int) mLoadMoreTotalUnconsumed, dy);
        }

        // Now let our nested parent consume the leftovers
        final int[] parentConsumed = mParentScrollConsumed;
        if (dispatchNestedPreScroll(dx - consumed[0], dy - consumed[1], parentConsumed, null)) {
            consumed[0] += parentConsumed[0];
            consumed[1] += parentConsumed[1];
        }
    }

    @Override
    public void onNestedScroll(final View target, final int dxConsumed, final int dyConsumed,
                               final int dxUnconsumed, final int dyUnconsumed) {
        // Dispatch up to the nested parent first
        dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed,
                mParentOffsetInWindow);

        // This is a bit of a hack. Nested scrolling works from the bottom up, and as we are
        // sometimes between two nested scrolling views, we need a way to be able to know when any
        // nested scrolling parent has stopped handling events. We do that by using the
        // 'offset in window 'functionality to see if we have been moved from the event.
        // This is a decent indication of whether we should take over the event stream or not.
        final int dy = dyUnconsumed + mParentOffsetInWindow[1];

        if (dy < 0 && canRefresh()) {
            mRefreshTotalUnconsumed += Math.abs(dy);
            movingHeader((int) mRefreshTotalUnconsumed, dy);
        } else if (dy > 0 && canLoadMore()) {
            mLoadMoreTotalUnconsumed += Math.abs(dy);
            movingFooter((int) mLoadMoreTotalUnconsumed, dy);
        }
    }

    @Override
    public int getNestedScrollAxes() {
        return mNestedScrollingParentHelper.getNestedScrollAxes();
    }

    @Override
    public void onStopNestedScroll(View target) {
        mNestedScrollingParentHelper.onStopNestedScroll(target);
        mNestedScrollInProgress = false;

        // Finish the spinner for nested scrolling if we ever consumed any
        // unconsumed nested scroll
        if (mRefreshTotalUnconsumed > 0) {
            updateHeaderWhenUpOrCancel((int) mRefreshTotalUnconsumed);
            mRefreshTotalUnconsumed = 0;
        }

        if (mLoadMoreTotalUnconsumed > 0) {
            updateFooterWhenUpOrCancel((int) mLoadMoreTotalUnconsumed);
            mLoadMoreTotalUnconsumed = 0;
        }
        // Dispatch up our nested parent
        stopNestedScroll();
    }


    // NestedScrollingChild
    @Override
    public void setNestedScrollingEnabled(boolean enabled) {
        mNestedScrollingChildHelper.setNestedScrollingEnabled(enabled);
    }

    @Override
    public boolean isNestedScrollingEnabled() {
        return mNestedScrollingChildHelper.isNestedScrollingEnabled();
    }

    @Override
    public boolean startNestedScroll(int axes) {
        return mNestedScrollingChildHelper.startNestedScroll(axes);
    }

    @Override
    public void stopNestedScroll() {
        mNestedScrollingChildHelper.stopNestedScroll();
    }

    @Override
    public boolean hasNestedScrollingParent() {
        return mNestedScrollingChildHelper.hasNestedScrollingParent();
    }

    @Override
    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed,
                                        int dyUnconsumed, int[] offsetInWindow) {
        return mNestedScrollingChildHelper.dispatchNestedScroll(dxConsumed, dyConsumed,
                dxUnconsumed, dyUnconsumed, offsetInWindow);
    }

    @Override
    public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow) {
        return mNestedScrollingChildHelper.dispatchNestedPreScroll(
                dx, dy, consumed, offsetInWindow);
    }

    @Override
    public boolean onNestedPreFling(View target, float velocityX,
                                    float velocityY) {
        return dispatchNestedPreFling(velocityX, velocityY);
    }

    @Override
    public boolean onNestedFling(View target, float velocityX, float velocityY,
                                 boolean consumed) {
        return dispatchNestedFling(velocityX, velocityY, consumed);
    }

    @Override
    public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
        return mNestedScrollingChildHelper.dispatchNestedFling(velocityX, velocityY, consumed);
    }

    @Override
    public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
        return mNestedScrollingChildHelper.dispatchNestedPreFling(velocityX, velocityY);
    }

}
