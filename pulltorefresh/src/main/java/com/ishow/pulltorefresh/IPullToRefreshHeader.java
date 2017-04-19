package com.ishow.pulltorefresh;

import android.animation.Animator;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by Bright.Yu on 2017/3/20.
 * PullToRefreshHeader
 */

public interface IPullToRefreshHeader {

    /**
     * 正常状态
     */
    int STATUS_NORMAL = 0;
    /**
     * 准备状态
     */
    int STATUS_READY = 1;
    /**
     * 正在刷新
     */
    int STATUS_REFRESHING = 2;
    /**
     * 刷新成功
     */
    int STATUS_SUCCESS = 3;
    /**
     * 刷新失败
     */
    int STATUS_FAILED = 4;

    @IntDef({STATUS_NORMAL, STATUS_READY, STATUS_REFRESHING, STATUS_SUCCESS, STATUS_FAILED})
    @Retention(RetentionPolicy.SOURCE)
    @interface status {
    }

    void init(ViewGroup parent);

    /**
     * 获取Header
     */
    @NonNull
    View getView();

    /**
     * 设置状态
     */
    void setStatus(@IPullToRefreshHeader.status int status);

    /**
     * 获取当前状态
     */
    int getStatus();

    /**
     * 获取距离顶部的距离
     */
    int getBottom();

    /**
     * 移动
     *
     * @param parent 父View
     * @param total  总共移动了多少距离
     * @param offset 当前事件移动的距离
     * @return header移动的距离
     */
    int moving(ViewGroup parent, final int total, final int offset);

    /**
     * 刷新中....
     */
    int refreshing(ViewGroup parent, final int total, @Nullable Animator.AnimatorListener listener);

    /**
     * 取消刷新
     */
    int cancelRefresh(ViewGroup parent);

    /**
     * 刷新成功
     */
    int refreshSuccess(ViewGroup parent);

    /**
     * 刷新失败
     */
    int refreshFailed(ViewGroup parent);

    /**
     * 获取下拉的最大高度
     */
    int getMaxPullDownHeight();

    /**
     * 获取Header的高度
     */
    int getHeaderHeight();

    /**
     * 判断当前移动距离是否是有效距离
     */
    boolean isEffectiveDistance(int movingDistance);
}
