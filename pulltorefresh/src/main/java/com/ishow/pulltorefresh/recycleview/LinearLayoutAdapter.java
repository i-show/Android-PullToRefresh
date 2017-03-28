package com.ishow.pulltorefresh.recycleview;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

    public LinearLayoutAdapter(Context context) {
        mData = new ArrayList<>();
        mContext = context;
        mLayoutInflater = LayoutInflater.from(context);
    }

    public void setData(List<DATA> datas) {
        if (datas == null) {
            mData.clear();
        } else {
            mData = datas;
        }
        notifyDataSetChanged();
    }


    @Override
    public VH onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == TYPE_FOOTER) {
            View item = mLayoutInflater.inflate(R.layout.pull_to_refresh_footer, parent, false);
            return (VH) new Holder(item, viewType);
        } else {
            return onCreateBodyHolder(parent, viewType);
        }
    }

    @Override
    public void onBindViewHolder(VH holder, int position) {
        int viewType = getItemViewType(position);
        if (viewType == TYPE_FOOTER) {
            holder.setFooterStatus(mStatus);
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
        return mData.size();
    }

    public DATA getItem(int position) {
        return mData.get(position);
    }

    @Override
    public void init() {
        mStatus = IPullToRefreshFooter.STATUS_NORMAL;
    }

    @Override
    public void setStatus(@status int status) {
        mStatus = status;
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
    public boolean isEffectiveDistance(ViewGroup parent, View targetView, int movingDistance) {
        return targetView.getBottom() > 130;
    }


    public static class Holder extends RecyclerView.ViewHolder {
        private View mItemView;
        private int mType;
        private TextView mFooterView;

        public Holder(View item, int type) {
            super(item);
            mItemView = item;
            mType = type;
            if (type == TYPE_FOOTER) {
                findFooter(item);
            } else {
                findBody(item);
            }
        }

        private void findFooter(View item) {
            mFooterView = (TextView) item.findViewById(R.id.pull_to_refesh_footer);
        }

        public void findBody(View item) {

        }

        public void setFooterStatus(int status) {
            switch (status) {
                case STATUS_NORMAL:
                    mFooterView.setText("normal");
                    break;
                case STATUS_READY:
                    mFooterView.setText("ready");
                    break;
                case STATUS_LOADING:
                    mFooterView.setText("loading");
                    break;
                case STATUS_END:
                    mFooterView.setText("end");
                    break;
            }
        }

        public int getType() {
            return mType;
        }

        public View getItemView() {
            return mItemView;
        }
    }
}
