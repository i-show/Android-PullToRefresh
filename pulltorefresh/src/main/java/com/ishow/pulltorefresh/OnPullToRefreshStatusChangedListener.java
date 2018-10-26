package com.ishow.pulltorefresh;

public interface OnPullToRefreshStatusChangedListener {
    /**
     * 下拉刷新状态的改变
     */
    void onRefreshStatusChanged(int status);

    /**
     * 上拉加载更多的装改变
     */
    void onLoadMoreStatusChanged(int status);

    /**
     * 是否已经移动了fitSystemWindows的距离
     * header移动的距离， fitTop
     */
    void onMovingFitTop(final int headerDistance, final int fit);
}
