package com.ishow.pulltorefresh.test;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.ishow.pulltorefresh.IPullToRefreshHeader;

/**
 * Created by Bright.Yu on 2017/3/22.
 */

public class TestHeader extends android.support.v7.widget.AppCompatTextView implements IPullToRefreshHeader {


    public TestHeader(Context context) {
        super(context);
    }

    public TestHeader(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TestHeader(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @NonNull
    @Override
    public View getView() {
        return this;
    }

    @Override
    public void setStatus(@status int status) {

    }

    @Override
    public int moveing(ViewGroup parent, int movingDistance) {
        layout(0, movingDistance - getMeasuredHeight(), parent.getWidth(), movingDistance);
        return movingDistance;
    }


}
