package com.ishow.pulltorefresh.recycleview;

import android.content.Context;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import com.ishow.pulltorefresh.IPullToRefreshFooter;
import com.ishow.pulltorefresh.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Bright.Yu on 2017/3/28.
 * PullToRefreshAdapter
 */

public abstract class LinearLayoutAdapter<DATA, VH extends LinearLayoutAdapter.Holder> extends RecyclerView.Adapter<VH> implements IPullToRefreshFooter {
    private static final int ROTATE_ANIM_DURATION = 380;
    public static final int TYPE_BODY = 0;
    public static final int TYPE_FOOTER = 1;

    private int mStatus;
    private List<DATA> mData;
    protected Context mContext;
    protected LayoutInflater mLayoutInflater;

    private View mFooterView;
    private TextView mFooterTextView;
    private ImageView mFooterLoadingView;

    private RotateAnimation mRotateLoading;

    public LinearLayoutAdapter(Context context) {
        mData = new ArrayList<>();
        mContext = context;
        mLayoutInflater = LayoutInflater.from(context);

        mRotateLoading = new RotateAnimation(0, 360.0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        mRotateLoading.setDuration(ROTATE_ANIM_DURATION * 2);
        mRotateLoading.setRepeatCount(Animation.INFINITE);
        mRotateLoading.setFillAfter(false);
    }

    public void setData(@NonNull List<DATA> datas) {
        mData = datas;
        notifyDataSetChanged();
    }


    public void addData(@NonNull List<DATA> datas) {
        int index = mData.size();
        mData.addAll(datas);
        notifyDataSetChanged();
    }

    @Override
    public VH onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == TYPE_FOOTER) {
            mFooterView = mLayoutInflater.inflate(R.layout.pull_to_refresh_footer, parent, false);
            mFooterTextView = (TextView) mFooterView.findViewById(R.id.pull_to_refesh_footer_text);
            mFooterLoadingView = (ImageView) mFooterView.findViewById(R.id.pull_to_refesh_footer_loading);
            return (VH) new Holder(mFooterView, viewType);
        } else {
            return onCreateBodyHolder(parent, viewType);
        }
    }

    @Override
    public void onBindViewHolder(VH holder, int position) {
        int viewType = getItemViewType(position);
        if (viewType == TYPE_FOOTER) {
            setFooterStatus(mStatus);
        } else {
            onBindBodyHolder(holder, position);
        }
    }

    public abstract VH onCreateBodyHolder(ViewGroup parent, int viewType);


    public abstract void onBindBodyHolder(VH holder, int postion);

    @Override
    public int getItemViewType(int position) {
        return position >= mData.size() ? TYPE_FOOTER : TYPE_BODY;
    }


    @Override
    public int getItemCount() {
        return mData.size() + 1;
    }

    public DATA getItem(int position) {
        return mData.get(position);
    }

    @Override
    public void init(ViewGroup parent) {
        mStatus = IPullToRefreshFooter.STATUS_NORMAL;
    }

    @Override
    public void setStatus(@status int status) {
        mStatus = status;
        setFooterStatus(status);
    }

    @Override
    public int getStatus() {
        return mStatus;
    }

    @Override
    public int moving(ViewGroup parent, int total, int offset) {
        if (Math.abs(total) >= getMaxPullUpHeight()) {
            return 0;
        } else {
            return offset;
        }
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
        if (targetView == null || mFooterView == null) {
            return false;
        } else {
            return targetView.getBottom() > mFooterView.getMeasuredHeight();
        }
    }

    @Override
    public int getMaxPullUpHeight() {
        return mFooterView.getMeasuredHeight() * 3;
    }

    private void setFooterStatus(int status) {
        if (mFooterView == null) {
            return;
        }
        switch (status) {
            case STATUS_NORMAL:
                mFooterLoadingView.setVisibility(View.GONE);
                mFooterTextView.setText(R.string.pulltorefresh_footer_normal);
                break;
            case STATUS_READY:
                mFooterLoadingView.setVisibility(View.GONE);
                mFooterTextView.setText(R.string.pulltorefresh_footer_ready);
                break;
            case STATUS_LOADING:
                mFooterLoadingView.clearAnimation();
                mFooterLoadingView.startAnimation(mRotateLoading);
                mFooterLoadingView.setVisibility(View.VISIBLE);
                mFooterTextView.setText(R.string.pulltorefresh_footer_loading);
                break;
            case STATUS_SUCCESS:
                mFooterLoadingView.setVisibility(View.GONE);
                mFooterTextView.setText(R.string.pulltorefresh_footer_success);
                break;
            case STATUS_FAILED:
                mFooterLoadingView.setVisibility(View.GONE);
                mFooterTextView.setText(R.string.pulltorefresh_footer_fail);
                break;
            case STATUS_END:
                mFooterLoadingView.setVisibility(View.GONE);
                mFooterTextView.setText(R.string.pulltorefresh_footer_end);
                break;
        }
    }


    public static class Holder extends RecyclerView.ViewHolder {
        private View mItemView;
        private int mType;

        public Holder(View item, int type) {
            super(item);
            mItemView = item;
            mType = type;
            if (type == TYPE_BODY) {
                findBody(item);
            }
        }

        public void findBody(View item) {

        }

        public int getType() {
            return mType;
        }

        public View getItemView() {
            return mItemView;
        }
    }
}
