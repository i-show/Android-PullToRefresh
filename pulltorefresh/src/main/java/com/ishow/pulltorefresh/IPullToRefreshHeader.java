package com.ishow.pulltorefresh;

import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
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
     * 移动
     *
     * @param parent         父View
     * @param movingDistance 手指移动的距离
     * @return header移动的距离
     */
    int moveing(ViewGroup parent, int movingDistance);
}
