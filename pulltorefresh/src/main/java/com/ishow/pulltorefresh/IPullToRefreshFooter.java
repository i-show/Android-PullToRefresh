package com.ishow.pulltorefresh;

import android.support.annotation.IntDef;

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

    @IntDef({STATUS_NORMAL, STATUS_READY, STATUS_LOADING})
    @Retention(RetentionPolicy.SOURCE)
    @interface status {
    }

    /**
     * 设置状态
     */
    void setStatus(@IPullToRefreshFooter.status int status);
}
