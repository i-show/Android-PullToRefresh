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
}
