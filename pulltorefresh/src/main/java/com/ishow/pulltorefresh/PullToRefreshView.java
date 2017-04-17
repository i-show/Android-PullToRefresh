package com.ishow.pulltorefresh;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Handler;
import android.support.annotation.NonNull;
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
public class PullToRefreshView extends ViewGroup {
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

    private boolean mCanOnLayout;
    /**
     * 监听
     */
    private OnPullToRefreshListener mPullToRefreshListener;

    private Handler mHandler;

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
        mHandler = new Handler();
        mCanOnLayout = true;
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
        if (!mCanOnLayout) {
            if (mHeader != null) {
                View view = mHeader.getView();
                view.layout(view.getLeft(), view.getTop(), view.getRight(), view.getBottom());
            }

            if (mTargetView != null) {
                View view = mTargetView;
                view.layout(view.getLeft(), view.getTop(), view.getRight(), view.getBottom());
            }
            return;
        }

        if (mHeader != null) {
            View view = mHeader.getView();
            view.layout(0, -view.getMeasuredHeight(), view.getMeasuredWidth(), 0);
        }

        if (mTargetView != null) {
            final View child = mTargetView;
            final int childLeft = getPaddingLeft();
            final int childTop = getPaddingTop();
            final int childWidth = width - getPaddingLeft() - getPaddingRight();
            final int childHeight = height - getPaddingTop() - getPaddingBottom();
            child.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight);
        }
    }


    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        final boolean canRefesh = canRefresh();
        final boolean canLoadMore = canLoadMore();
        if (!isEnabled() || (!canRefesh && !canLoadMore)) {
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
        if (!isEnabled() || (!canRefesh && !canLoadMore)) {
            return false;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mIsBeingDragged = false;
                mCanOnLayout = false;
                break;
            case MotionEvent.ACTION_MOVE:
                final int y = (int) event.getY();
                startDragging(y);
                if (!mIsBeingDragged) {
                    break;
                }

                mMovingSum = (int) (y - mBeingDraggedY);
                final float offset = y - mLastY;
                mLastY = y;
                if (mIsBeingDraggedDown) {
                    final int offsetResult = mHeader.moving(this, mMovingSum, (int) offset);
                    ViewCompat.offsetTopAndBottom(mTargetView, offsetResult);
                    if (mHeader.isEffectiveDistance(mMovingSum)) {
                        mHeader.setStatus(IPullToRefreshHeader.STATUS_READY);
                    } else {
                        mHeader.setStatus(IPullToRefreshHeader.STATUS_NORMAL);
                    }
                } else if (mIsBeingDraggedUp) {
                    final int offsetResult = mFooter.moving(this, mMovingSum, (int) offset);
                    ViewCompat.offsetTopAndBottom(mTargetView, offsetResult);
                    if (mFooter.isEffectiveDistance(this, mTargetView, mMovingSum)) {
                        mFooter.setStatus(IPullToRefreshFooter.STATUS_READY);
                    } else {
                        mFooter.setStatus(IPullToRefreshFooter.STATUS_NORMAL);
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (mMovingSum > 0) {
                    updateHeaderWhenUpOrCancel();
                } else {
                    updateFooterWhenUpOrCancel();
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

    
    private void updateHeaderWhenUpOrCancel() {
        if (mHeader.isEffectiveDistance(mMovingSum)) {
            mHeader.setStatus(IPullToRefreshHeader.STATUS_REFRESHING);
            int offset = mHeader.refreshing(this, mMovingSum);
            ViewHelper.movingY(mTargetView, offset);
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

    private void updateFooterWhenUpOrCancel() {
        if (mFooter.isEffectiveDistance(this, mTargetView, mMovingSum)) {
            mFooter.setStatus(IPullToRefreshFooter.STATUS_LOADING);
            int offset = mFooter.loading(this, mTargetView, mMovingSum);
            ViewHelper.movingY(mTargetView, offset);
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
            mCanOnLayout = false;
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
        mFooter.setStatus(IPullToRefreshFooter.STATUS_NORMAL);
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

        mFooter.setStatus(IPullToRefreshFooter.STATUS_END);
        mCanOnLayout = true;
        requestLayout();
    }

    /**
     * 下拉加载失败
     */
    @SuppressWarnings("unused")
    public void setLoadMoreFailed() {
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
     * 重置为Normal状态
     */
    private Animator.AnimatorListener mSetRefreshNormalListener = new Animator.AnimatorListener() {
        @Override
        public void onAnimationStart(Animator animation) {

        }

        @Override
        public void onAnimationEnd(Animator animation) {
            mHeader.setStatus(IPullToRefreshHeader.STATUS_NORMAL);
            mCanOnLayout = true;
            requestLayout();
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
            mFooter.setStatus(IPullToRefreshFooter.STATUS_NORMAL);
            mCanOnLayout = true;
            requestLayout();
        }

        @Override
        public void onAnimationCancel(Animator animation) {

        }

        @Override
        public void onAnimationRepeat(Animator animation) {

        }
    };
}
