package com.ishow.pulltorefresh.test;

import android.content.Context;
import android.support.v7.widget.AppCompatTextView;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;

import com.ishow.pulltorefresh.IPullToRefreshFooter;

/**
 * Created by Bright.Yu on 2017/3/28.
 */

public class TestFooter extends AppCompatTextView implements IPullToRefreshFooter {
    private int mStatus;

    public TestFooter(Context context) {
        this(context, null);
    }

    public TestFooter(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TestFooter(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setGravity(Gravity.CENTER);
    }

    @Override
    public void init() {
        mStatus = STATUS_NORMAL;
    }

    @Override
    public void setStatus(@status int status) {
        switch (status) {
            case STATUS_NORMAL:
                setText("normal");
                break;
            case STATUS_READY:
                setText("ready");
                break;
            case STATUS_LOADING:
                setText("loading");
                break;
            case STATUS_END:
                setText("end");
                break;
        }
    }

    @Override
    public int getStatus() {
        return mStatus;
    }

    @Override
    public int moving(ViewGroup parent, int total, int offset) {
        return offset;
    }

    @Override
    public int loading(ViewGroup parent, View targetView, int total) {
        return parent.getHeight() - targetView.getBottom();
    }

    @Override
    public int cancelLoadMore(ViewGroup parent, View targetView) {
        return parent.getHeight() - targetView.getBottom();
    }

    @Override
    public int loadSuccess(ViewGroup parent) {
        return 0;
    }

    @Override
    public int loadFailed(ViewGroup parent) {
        return 0;
    }

    @Override
    public boolean isEffectiveDistance(ViewGroup parent, View targetView, int movingDistance) {
        return targetView.getBottom() > 130;
    }
}
