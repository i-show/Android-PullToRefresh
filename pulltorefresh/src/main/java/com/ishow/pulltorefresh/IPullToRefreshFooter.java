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
     * 加载状态
     */
    int STATUS_LOADING = 1;
    /**
     * 刷新失败
     */
    int STATUS_FAILED = 2;
    /**
     * 全部加载完毕
     */
    int STATUS_END = 3;

    @IntDef({STATUS_NORMAL, STATUS_LOADING, STATUS_FAILED, STATUS_END})
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
     * 是否可用
     */
    void setEnabled(boolean enable);
}
