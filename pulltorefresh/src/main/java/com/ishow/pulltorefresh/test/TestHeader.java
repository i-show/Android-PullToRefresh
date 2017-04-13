package com.ishow.pulltorefresh.test;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;

import com.ishow.pulltorefresh.IPullToRefreshHeader;
import com.ishow.pulltorefresh.utils.ViewHelper;

/**
 * Created by Bright.Yu on 2017/3/22.
 */

public class TestHeader extends android.support.v7.widget.AppCompatTextView implements IPullToRefreshHeader {

    private int mStatus;

    public TestHeader(Context context) {
        super(context);
        setGravity(Gravity.CENTER);
    }

    public TestHeader(Context context, AttributeSet attrs) {
        super(context, attrs);
        setGravity(Gravity.CENTER);
    }

    public TestHeader(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setGravity(Gravity.CENTER);
    }

    @Override
    public void init(ViewGroup parent) {
        mStatus = IPullToRefreshHeader.STATUS_NORMAL;
    }

    @NonNull
    @Override
    public View getView() {
        return this;
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
            case STATUS_REFRESHING:
                setText("refreshing...");
                break;
            case STATUS_FAILED:
                setText("failed");
                break;
            case STATUS_SUCCESS:
                setText("success");
                break;
        }
        mStatus = status;
    }

    @Override
    public int getStatus() {
        return mStatus;
    }

    @Override
    public int moving(ViewGroup parent, final int total, final int offset) {
        if (total >= getMaxPullDownHeight()) {
            return 0;
        } else if (getTop() + offset < -getHeaderHeight()) {
            int ajust = getHeaderHeight() + getTop();
            ViewCompat.offsetTopAndBottom(this, ajust);
            return ajust;
        } else {
            ViewCompat.offsetTopAndBottom(this, offset);
            return offset;
        }

    }


    @Override
    public int refreshing(ViewGroup parent, int total) {
        int offset = -getTop();
        ViewHelper.movingY(this, offset);
        return offset;
    }

    @Override
    public int cancelRefresh(ViewGroup parent) {
        int offset = -getTop() - getHeaderHeight();
        ViewHelper.movingY(this, offset);
        return offset;
    }

    @Override
    public int refreshSuccess(ViewGroup parent) {
        int offset = -getHeaderHeight();
        ViewHelper.movingY(this, offset);
        return offset;
    }

    @Override
    public int refreshFailed(ViewGroup parent) {
        int offset = -getHeaderHeight();
        ViewHelper.movingY(this, offset);
        return offset;
    }

    @Override
    public int getMaxPullDownHeight() {
        return (getMeasuredHeight() * 5);
    }

    @Override
    public int getHeaderHeight() {
        return getMeasuredHeight();
    }

    @Override
    public boolean isEffectiveDistance(int movingDistance) {
        return Math.abs(movingDistance) > getHeaderHeight();
    }


}
