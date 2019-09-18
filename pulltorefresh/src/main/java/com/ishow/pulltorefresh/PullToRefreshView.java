package com.ishow.pulltorefresh;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
    private static final int ANI_INTERVAL = 800;
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
    private float mLastY;
    private float mInitialDownY;
    /**
     * 滑动距离的判断
     */
    private int mTouchSlop;

    private boolean mIsBeingDragged;
    private boolean isRefreshEnable;
    private boolean isLoadMoreEnable;
    /**
     * 监听
     */
    private OnPullToRefreshListener mPullToRefreshListener;
    private OnPullToRefreshStatusChangedListener mPullToRefreshStatusChangedListener;

    private Handler mHandler;

    /**
     * 在调用onLayout的时候使用
     */
    private int mHeaderMovingDistance;
    private int mTargetOffsetTop;
    private int mScrollViewId;
    /**
     * RecyclerView默认的Footer或者HeaderCount
     */
    private int mCustomFooterOrHeaderCount;

    private PullToRefreshAnimatorListener mRefreshingListener;
    private PullToRefreshAnimatorListener mRefreshingHeaderListener;
    private PullToRefreshAnimatorListener mSetRefreshNormalListener;

    public PullToRefreshView(Context context) {
        this(context, null);
    }

    public PullToRefreshView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PullToRefreshView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PullToRefreshView);
        isLoadMoreEnable = a.getBoolean(R.styleable.PullToRefreshView_loadMoreEnable, true);
        isRefreshEnable = a.getBoolean(R.styleable.PullToRefreshView_refreshEnable, true);
        mScrollViewId = a.getResourceId(R.styleable.PullToRefreshView_scrollViewId, View.NO_ID);
        a.recycle();

        init();
    }

    private void init() {
        final ViewConfiguration configuration = ViewConfiguration.get(getContext());
        mTouchSlop = configuration.getScaledTouchSlop();
        mHandler = new Handler();
        mCustomFooterOrHeaderCount = 0;

        mRefreshingListener = new PullToRefreshAnimatorListener(PullToRefreshAnimatorListener.TYPE_REFRESHING);
        mRefreshingHeaderListener = new PullToRefreshAnimatorListener(PullToRefreshAnimatorListener.TYPE_HEADER_REFRESHING);
        mSetRefreshNormalListener = new PullToRefreshAnimatorListener(PullToRefreshAnimatorListener.TYPE_REFRESH_NORMAL);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        final int childCount = getChildCount();
        if (childCount != 1) {
            throw new IllegalStateException("need only one a child");
        }
        mTargetView = getChildAt(0);
        View scrollView = findViewById(mScrollViewId);
        if (scrollView == null) {
            scrollView = mTargetView;
        }
        if (scrollView == null) {
            return;
        }

        if (scrollView instanceof RecyclerView) {
            RecyclerView recyclerView = (RecyclerView) scrollView;
            recyclerView.addOnScrollListener(mRecycleScrollListener);
        }
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
            view.layout(0, -view.getMeasuredHeight() + mHeaderMovingDistance, view.getMeasuredWidth(), mHeaderMovingDistance);
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
        if (!isAlreadyStatus() || canScrollUp()) {
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
    @SuppressLint("ClickableViewAccessibility")
    public boolean onTouchEvent(MotionEvent event) {
        if (!isAlreadyStatus() || canScrollUp()) {
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
                final float offset = mLastY - y;
                mLastY = y;
                movingHeader((int) offset);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                updateHeaderWhenUpOrCancel();
                mIsBeingDragged = false;
                break;
        }
        return true;
    }


    @SuppressWarnings("unused")
    public boolean isLoadMoreEnable() {
        return isLoadMoreEnable;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        setLoadMoreEnable(enabled);
        setRefreshEnable(enabled);
    }

    public void setLoadMoreEnable(boolean loadMoreEnable) {
        isLoadMoreEnable = loadMoreEnable;
        if (mFooter != null) mFooter.setEnabled(loadMoreEnable);
    }

    @SuppressWarnings("unused")
    public boolean isRefreshEnable() {
        return isRefreshEnable;
    }

    public void setRefreshEnable(boolean refreshEnable) {
        isRefreshEnable = refreshEnable;
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
    private synchronized void movingHeader(final int offset) {
        if (mHeader == null) {
            return;
        }

        final int offsetResult = mHeader.moving(this, offset);
        mHeaderMovingDistance = mHeader.getMovingDistance();

        ViewCompat.offsetTopAndBottom(mTargetView, offsetResult);
        mTargetOffsetTop = mTargetView.getTop();

        if (mHeader.isEffectiveDistance()) {
            notifyRefreshStatusChanged(IPullToRefreshHeader.STATUS_READY);
        } else {
            notifyRefreshStatusChanged(IPullToRefreshHeader.STATUS_NORMAL);
        }
    }

    private synchronized void updateHeaderWhenUpOrCancel() {
        if (mHeader == null) {
            Log.i(TAG, "updateHeaderWhenUpOrCancel: header is null");
            return;
        }

        if (mHeader.isEffectiveDistance()) {
            notifyRefreshStatusChanged(IPullToRefreshHeader.STATUS_REFRESHING);
            int offset = mHeader.refreshing(this, mRefreshingHeaderListener);
            mHeaderMovingDistance = mHeader.getMovingDistance();
            ViewHelper.movingY(mTargetView, offset, mRefreshingListener);
        } else {
            int offset = mHeader.cancelRefresh(this);
            ViewHelper.movingY(mTargetView, offset, mSetRefreshNormalListener);
        }
    }

    /**
     * 在这里Footer比较特殊不能直接Add到 这里面，需要在Target里面实现
     */
    public void setFooter(@NonNull IPullToRefreshFooter footer) {
        mFooter = footer;
    }

    /**
     * 判断是否可以进行拖拽
     */
    private void startDragging(float y) {
        final float yDiff = y - mInitialDownY;
        if (yDiff > mTouchSlop && !mIsBeingDragged) {
            mLastY = y;
            mIsBeingDragged = true;
        }
    }

    /**
     * 是否已经加载完毕
     */
    @SuppressWarnings("unused")
    private boolean isLoadEnd() {
        return mFooter != null && mFooter.getStatus() == IPullToRefreshFooter.STATUS_END;
    }


    /**
     * 是否可以上滑
     * 下拉刷新的时候使用
     */
    private boolean canScrollUp() {
        return mTargetView != null && mTargetView.canScrollVertically(-1);
    }

    /**
     * 设置成普通状态
     */
    @SuppressWarnings("unused")
    public void setRefreshNormal() {
        if (mHeader == null) {
            return;
        }
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                int offset = -mHeader.getMovingDistance();
                ViewHelper.movingY(mTargetView, offset, mSetRefreshNormalListener);
            }
        }, ANI_INTERVAL);
    }

    /**
     * 刷新成功
     */
    @SuppressWarnings("unused")
    public void setRefreshSuccess() {
        if (mHeader == null || mHeader.getStatus() != IPullToRefreshHeader.STATUS_REFRESHING) {
            return;
        }
        // 通知状态改变要在setStatus之前
        notifyRefreshStatusChanged(IPullToRefreshHeader.STATUS_SUCCESS);
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
        if (mHeader == null || mHeader.getStatus() != IPullToRefreshHeader.STATUS_REFRESHING) {
            Log.e(TAG, "setRefreshFailed: status error ");
            return;
        }
        // 通知状态改变要在setStatus之前
        notifyRefreshStatusChanged(IPullToRefreshHeader.STATUS_FAILED);
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                int offset = mHeader.refreshFailed(PullToRefreshView.this);
                ViewHelper.movingY(mTargetView, offset, mSetRefreshNormalListener);
            }
        }, ANI_INTERVAL);
    }

    /**
     * 下拉加载失败
     */
    public void setLoadMoreFailed() {
        if (mFooter == null) {
            Log.i(TAG, "setLoadMoreFailed: mFooter is null");
            return;
        }
        if (mFooter.getStatus() != IPullToRefreshFooter.STATUS_LOADING) {
            Log.i(TAG, "setLoadMoreFailed: status error");
            return;
        }
        // 通知状态改变要在setStatus之前
        notifyLoadMoreStatusChanged(IPullToRefreshFooter.STATUS_FAILED);
        requestLayout();
    }

    /**
     * 下拉加载成功
     */
    public void setLoadMoreSuccess() {
        if (mFooter == null) {
            Log.i(TAG, "setLoadMoreSuccess: mFooter is null");
            return;
        }
        if (mFooter.getStatus() != IPullToRefreshFooter.STATUS_LOADING) {
            return;
        }
        // 通知状态改变要在setStatus之前
        notifyLoadMoreStatusChanged(IPullToRefreshFooter.STATUS_NORMAL);
        requestLayout();
    }

    /**
     * 加载完成
     */
    @SuppressWarnings("unused")
    public void setLoadMoreEnd() {
        if (mTargetView == null) {
            return;
        }
        notifyLoadMoreStatusChanged(IPullToRefreshFooter.STATUS_END);
        requestLayout();
    }

    /**
     * 自定义的Footer和Header的高度
     */
    @SuppressWarnings("unused")
    public void setCustomFooterOrHeaderCount(int count) {
        mCustomFooterOrHeaderCount = count;
    }

    /**
     * 状态已经可以进行下拉刷新
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean isAlreadyStatus() {
        return isEnabled() && isHeaderAlreadyForRefresh() && isFooterAlreadyForRefresh();
    }

    private boolean isHeaderAlreadyForRefresh() {
        if (!isRefreshEnable || mHeader == null) {
            return false;
        }
        switch (mHeader.getStatus()) {
            case IPullToRefreshHeader.STATUS_NORMAL:
            case IPullToRefreshHeader.STATUS_READY:
                return true;
            default:
                return false;
        }
    }

    private boolean isFooterAlreadyForRefresh() {
        // 当Footer不存在或者使用的时候那么也可以认为可以进行下拉刷新
        if (!isLoadMoreEnable || mFooter == null) {
            return true;
        }
        switch (mFooter.getStatus()) {
            case IPullToRefreshFooter.STATUS_NORMAL:
            case IPullToRefreshFooter.STATUS_END:
            case IPullToRefreshFooter.STATUS_FAILED:
                return true;
            default:
                return false;
        }
    }

    private boolean isHeaderAlreadyForLoadMore() {
        // 当Header不存在或者使用的时候那么也可以认为可以进行加载更多
        if (!isRefreshEnable || mHeader == null) {
            return true;
        }
        switch (mHeader.getStatus()) {
            case IPullToRefreshHeader.STATUS_NORMAL:
            case IPullToRefreshHeader.STATUS_READY:
                return true;
            default:
                return false;
        }
    }

    /**
     * 是否可以加载更多
     */
    private boolean isCanLoadMore() {
        if (!isLoadMoreEnable || mFooter == null) {
            return false;
        }

        switch (mFooter.getStatus()) {
            case IPullToRefreshFooter.STATUS_NORMAL:
            case IPullToRefreshFooter.STATUS_FAILED:
                return true;
            default:
                return false;
        }
    }

    private void setRefreshing() {
        computeStatus();
        notifyRefresh();
    }

    private void setLoading() {
        computeStatus();
        notifyLoadMore();
    }

    private void computeStatus() {
        if (mHeader != null) {
            mHeaderMovingDistance = mHeader.getMovingDistance();
        }

        if (mTargetView != null) {
            mTargetOffsetTop = mTargetView.getTop();
        }
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
     * 设置状态改变
     */
    @SuppressWarnings("unused")
    public void setOnPullToRefreshStatusChangedListener(OnPullToRefreshStatusChangedListener listener) {
        mPullToRefreshStatusChangedListener = listener;
    }

    /**
     * 通知刷新的状态有改变
     */
    private void notifyRefreshStatusChanged(int status) {
        if (mHeader == null || mHeader.getStatus() == status) {
            return;
        }
        mHeader.setStatus(status);

        if (mPullToRefreshStatusChangedListener != null) {
            mPullToRefreshStatusChangedListener.onRefreshStatusChanged(status);
        }
    }

    /**
     * 通知加载更多的状态有改变
     */
    private void notifyLoadMoreStatusChanged(int status) {
        if (mFooter == null || mFooter.getStatus() == status) {
            return;
        }
        mFooter.setStatus(status);

        if (mPullToRefreshStatusChangedListener != null) {
            mPullToRefreshStatusChangedListener.onLoadMoreStatusChanged(status);
        }
    }

    private class PullToRefreshAnimatorListener extends AbsAnimatorListener {
        /**
         * 设置为刷新默认状态
         */
        private static final int TYPE_REFRESH_NORMAL = 1;
        /**
         * 正在刷新
         */
        private static final int TYPE_REFRESHING = 2;
        /**
         * 头部刷新
         */
        private static final int TYPE_HEADER_REFRESHING = 3;


        private int mType;

        private PullToRefreshAnimatorListener(int type) {
            mType = type;
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            switch (mType) {
                case TYPE_REFRESH_NORMAL:
                    setRefreshNormal();
                    break;
                case TYPE_REFRESHING:
                    setRefreshing();
                    break;
                case TYPE_HEADER_REFRESHING:
                    computeStatus();
                    break;
            }
        }

        private void setRefreshNormal() {
            // 通知状态改变要在setStatus之前
            notifyRefreshStatusChanged(IPullToRefreshHeader.STATUS_NORMAL);

            if (mHeader != null) {
                mHeaderMovingDistance = mHeader.getMovingDistance();
            }

            if (mTargetView != null) {
                mTargetOffsetTop = mTargetView.getTop();
            }
        }

        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            computeStatus();
        }
    }


    private RecyclerView.OnScrollListener mRecycleScrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);
            if (!isLoadMoreEnable || !isCanLoadMore() || !isHeaderAlreadyForLoadMore()) {
                return;
            }

            RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
            if (layoutManager == null) {
                Log.i(TAG, "onScrollStateChanged:  layoutManager is null");
                return;
            }
            // 如果item == 1 不进行任何操作 默认是有一个footer的
            if (layoutManager.getItemCount() <= (1 + mCustomFooterOrHeaderCount)) {
                return;
            }

            if (layoutManager instanceof GridLayoutManager) {
                GridLayoutManager gridLayoutManager = (GridLayoutManager) layoutManager;
                //获取最后一个可见view的位置
                final int lastPosition = gridLayoutManager.findLastVisibleItemPosition();
                final int itemCount = gridLayoutManager.getItemCount();
                final int spanCount = gridLayoutManager.getSpanCount();
                // 提前3个进行预加载
                if (lastPosition + spanCount * 3 >= itemCount - 1) {
                    // 通知状态改变要在setStatus之前
                    notifyLoadMoreStatusChanged(IPullToRefreshFooter.STATUS_LOADING);
                    mFooter.setStatus(IPullToRefreshFooter.STATUS_LOADING);
                    setLoading();
                }
            } else if (layoutManager instanceof LinearLayoutManager) {
                LinearLayoutManager linearManager = (LinearLayoutManager) layoutManager;
                //获取最后一个可见view的位置
                final int lastPosition = linearManager.findLastVisibleItemPosition();
                final int itemCount = linearManager.getItemCount();
                // 提前3个进行预加载
                if (lastPosition >= itemCount - 4) {
                    // 通知状态改变要在setStatus之前
                    notifyLoadMoreStatusChanged(IPullToRefreshFooter.STATUS_LOADING);
                    mFooter.setStatus(IPullToRefreshFooter.STATUS_LOADING);
                    setLoading();
                }
            }
        }
    };
}
