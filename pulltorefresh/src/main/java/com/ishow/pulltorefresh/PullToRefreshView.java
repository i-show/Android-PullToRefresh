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
        // 只有当Header和Footer 都ok的时候才能进行 下拉或者上拉
        if (!isAlreadyStatus()) {
            return false;
        }

        final boolean canLoadMore = canLoadMore();
        final boolean canRefesh = canRefresh();

        if (!isEnabled() || (!canLoadMore && !canRefesh)) {
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
        if (!isAlreadyStatus()) {
            return false;
        }

        final boolean canRefesh = canRefresh();
        final boolean canLoadMore = canLoadMore();
        if (!isEnabled() || (!canRefesh && !canLoadMore)) {
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
                if (mIsBeingDraggedDown) {
                    updateHeaderWhenUpOrCancel(mMovingSum);
                } else if (mIsBeingDraggedUp) {
                    updateFooterWhenUpOrCancel(mMovingSum);
                }
                mIsBeingDragged = false;
                mIsBeingDraggedUp = false;
                mIsBeingDraggedDown = false;
                break;
        }
        return true;
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
        Log.i(TAG, "movingHeader: mTargetOffsetTop ==" + mTargetOffsetTop);

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

        if (total > 0 && mHeader.isEffectiveDistance(total)) {
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

        if (total < 0 && mFooter.isEffectiveDistance(this, mTargetView, total)) {
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
        return mHeader != null && !canScrollUp();
    }


    /**
     * 是否可以进行加载更多操作
     */
    private boolean canLoadMore() {
        return mFooter != null && mFooter.getStatus() != IPullToRefreshFooter.STATUS_END && !canScrollDown();
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

    /**
     * 状态已经可以进行下拉刷新或者上拉加载更多
     */
    private boolean isAlreadyStatus() {
        return isAlreadyHeaderStatus() && isAlreadyFooterStatus();
    }

    private boolean isAlreadyHeaderStatus() {
        if (mHeader == null) {
            return false;
        }
        final int status = mHeader.getStatus();
        return status == IPullToRefreshHeader.STATUS_NORMAL || status == IPullToRefreshHeader.STATUS_READY;
    }

    private boolean isAlreadyFooterStatus() {
        if (mFooter == null) {
            return false;
        }
        final int status = mFooter.getStatus();
        return status == IPullToRefreshFooter.STATUS_NORMAL || status == IPullToRefreshFooter.STATUS_READY || status == IPullToRefreshFooter.STATUS_END;
    }

    private boolean isRefreshing() {
        return mHeader != null && mHeader.getStatus() == IPullToRefreshHeader.STATUS_REFRESHING;
    }

    private boolean isLoading() {
        return mFooter != null && mFooter.getStatus() == IPullToRefreshFooter.STATUS_LOADING;
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
                Log.i(TAG, "mRefreshListener, onAnimationEnd: mTargetOffsetTop =" + mTargetOffsetTop);
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
                Log.i(TAG, "mSetRefreshNormalListener onAnimationEnd: mTargetOffsetTop = " + mTargetOffsetTop);
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
                Log.i(TAG, "mSetLoadNormalListener onAnimationEnd: mTargetOffsetTop = " + mTargetOffsetTop);
            }
        }

        @Override
        public void onAnimationCancel(Animator animation) {

        }

        @Override
        public void onAnimationRepeat(Animator animation) {

        }
    };

}
