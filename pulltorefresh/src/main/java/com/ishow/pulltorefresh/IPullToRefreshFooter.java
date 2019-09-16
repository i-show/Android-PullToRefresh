package com.ishow.pulltorefresh;

import androidx.annotation.IntDef;
import android.view.View;
import android.view.ViewGroup;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by Bright.Yu on 2017/3/20.
 * PullToRefreshFooter
 */

public interface IPullToRefreshFooter {
    /**
     * 普通状态
     */
    int STATUS_NORMAL = 0;
    /**
     * 准备状态
     */
    int STATUS_READY = 1;
    /**
     * 加载状态
     */
    int STATUS_LOADING = 2;
    /**
     * 刷新成功
     */
    int STATUS_SUCCESS = 3;
    /**
     * 刷新失败
     */
    int STATUS_FAILED = 4;
    /**
     * 全部加载完毕
     */
    int STATUS_END = 5;

    @IntDef({STATUS_NORMAL, STATUS_READY, STATUS_LOADING, STATUS_SUCCESS, STATUS_FAILED, STATUS_END})
    @Retention(RetentionPolicy.SOURCE)
    @interface status {
    }

    /**
     * 初始化
     */
    void init(ViewGroup parent);

    /**
     * 设置状态
     */
    void setStatus(@IPullToRefreshFooter.status int status);

    /**
     * 获取当前状态
     */
    int getStatus();


    /**
     * 移动
     *
     * @param parent 父View
     * @param total  总共移动了多少距离
     * @param offset 当前事件移动的距离
     * @return header移动的距离
     */
    int moving(ViewGroup parent, View targetView, final int total, final int offset);

    /**
     * 刷新中....
     */
    int loading(ViewGroup parent, View targetView, final int total);

    /**
     * 取消刷新
     */
    int cancelLoadMore(ViewGroup parent, View targetView);

    /**
     * 加载成功
     */
    int loadSuccess(ViewGroup parent);

    /**
     * 加载失败
     */
    int loadFailed(ViewGroup parent);


    /**
     * 判断当前移动距离是否是有效距离
     */
    boolean isEffectiveDistance(ViewGroup parent, View targetView, int movingDistance);


    /**
     * 获取上拉的最大高度
     */
    int getMaxPullUpHeight();

    void setEnabled(boolean enable);
}
