package com.ishow.pulltorefresh.recycleview;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
    public static final int TYPE_BODY = 0;
    public static final int TYPE_FOOTER = 1;
    private int mStatus;
    private List<DATA> mData;
    protected Context mContext;
    protected LayoutInflater mLayoutInflater;

    private View mFooterView;
    private TextView mFooterTextView;
    private ImageView mFooterLoadingView;

    public LinearLayoutAdapter(Context context) {
        mData = new ArrayList<>();
        mContext = context;
        mLayoutInflater = LayoutInflater.from(context);
    }

    public void setData(@NonNull List<DATA> datas) {
        mData = datas;
        notifyDataSetChanged();
    }


    public void addData(@NonNull List<DATA> datas) {
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
        return targetView.getBottom() > mFooterView.getMeasuredHeight();
    }

    private void setFooterStatus(int status) {
        if (mFooterView == null) {
            return;
        }
        switch (status) {
            case STATUS_NORMAL:
                mFooterTextView.setText("normal");
                break;
            case STATUS_READY:
                mFooterTextView.setText("ready");
                break;
            case STATUS_LOADING:
                mFooterTextView.setText("loading");
                break;
            case STATUS_SUCCESS:
                mFooterTextView.setText("success");
                break;
            case STATUS_FAILED:
                mFooterTextView.setText("failed");
                break;
            case STATUS_END:
                mFooterTextView.setText("end");
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
