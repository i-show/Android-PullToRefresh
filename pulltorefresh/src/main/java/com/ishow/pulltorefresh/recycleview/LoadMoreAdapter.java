package com.ishow.pulltorefresh.recycleview;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import com.ishow.pulltorefresh.IPullToRefreshFooter;
import com.ishow.pulltorefresh.R;


public class LoadMoreAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements IPullToRefreshFooter {
    /**
     * 是否是加载更多的Item
     */
    private static final int TYPE_LOAD_MORE = Integer.MAX_VALUE - 2;
    /**
     * 加载动画的时间
     */
    private static final int ROTATE_ANIM_DURATION = 380;

    /**
     * 包裹的Adapter
     */
    private RecyclerView.Adapter mInnerAdapter;
    /**
     * LayoutInflater
     */
    private LayoutInflater mLayoutInflater;
    /**
     * LoadMore de Views
     */
    private View mLoadMoreView;
    private TextView mLoadMoreTextView;
    private ImageView mLoadMoreLoadingView;
    /**
     * 旋转动画
     */
    private RotateAnimation mRotateLoading;
    /**
     * 当前的Load More状态
     */
    private int mStatus;

    public LoadMoreAdapter(@NonNull Context context, @NonNull RecyclerView.Adapter adapter) {
        mInnerAdapter = adapter;
        mInnerAdapter.registerAdapterDataObserver(mDataObserver);

        mLayoutInflater = LayoutInflater.from(context);

        mRotateLoading = new RotateAnimation(0, 360.0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        mRotateLoading.setDuration(ROTATE_ANIM_DURATION * 2);
        mRotateLoading.setRepeatCount(Animation.INFINITE);
        mRotateLoading.setFillAfter(false);
    }


    @Override
    public int getItemViewType(int position) {
        int realCount = mInnerAdapter.getItemCount();
        if (position >= realCount) {
            return TYPE_LOAD_MORE;
        } else {
            return mInnerAdapter.getItemViewType(position);
        }
    }


    /**
     * 是否是 LoadMore状态
     */
    private boolean isLoadMoreItem(int position) {
        int realCount = mInnerAdapter.getItemCount();
        return position >= realCount;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case TYPE_LOAD_MORE:
                mLoadMoreView = mLayoutInflater.inflate(R.layout.pull_to_refresh_footer, parent, false);
                mLoadMoreTextView = (TextView) mLoadMoreView.findViewById(R.id.pull_to_refesh_footer_text);
                mLoadMoreLoadingView = (ImageView) mLoadMoreView.findViewById(R.id.pull_to_refesh_footer_loading);
                return new ViewHolder(mLoadMoreView);
            default:
                return mInnerAdapter.onCreateViewHolder(parent, viewType);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        int itemType = getItemViewType(position);
        switch (itemType) {
            case TYPE_LOAD_MORE:
                setFooterStatus(mStatus);
                break;
            default:
                mInnerAdapter.onBindViewHolder(holder, position);
                break;
        }

    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        mInnerAdapter.onAttachedToRecyclerView(recyclerView);
        RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();

        if (layoutManager != null && !(layoutManager instanceof GridLayoutManager)) {
            // 如果不是GridLayoutManager 那么不进行任何操作
            return;
        }

        final GridLayoutManager gridLayoutManager = (GridLayoutManager) layoutManager;
        final GridLayoutManager.SpanSizeLookup spanSizeLookup = gridLayoutManager.getSpanSizeLookup();
        gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (isLoadMoreItem(position)) {
                    return gridLayoutManager.getSpanCount();
                }
                if (spanSizeLookup != null) {
                    return spanSizeLookup.getSpanSize(position);
                }
                return 1;
            }
        });
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        mInnerAdapter.unregisterAdapterDataObserver(mDataObserver);
    }

    @Override
    public void onViewAttachedToWindow(RecyclerView.ViewHolder holder) {
        mInnerAdapter.onViewAttachedToWindow(holder);
        if (isLoadMoreItem(holder.getLayoutPosition())) {
            setFullSpan(holder);
        }
    }

    private void setFullSpan(RecyclerView.ViewHolder holder) {
        ViewGroup.LayoutParams lp = holder.itemView.getLayoutParams();
        if (lp != null && lp instanceof StaggeredGridLayoutManager.LayoutParams) {
            StaggeredGridLayoutManager.LayoutParams p = (StaggeredGridLayoutManager.LayoutParams) lp;
            p.setFullSpan(true);
        }
    }

    @Override
    public int getItemCount() {
        return mInnerAdapter.getItemCount() + 1;
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
            return -offset;
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
        if (targetView == null || mLoadMoreView == null) {
            return false;
        } else {
            return targetView.getBottom() > mLoadMoreView.getMeasuredHeight();
        }
    }

    @Override
    public int getMaxPullUpHeight() {
        return mLoadMoreView.getMeasuredHeight() * 3;
    }

    private void setFooterStatus(int status) {
        if (mLoadMoreView == null) {
            return;
        }
        switch (status) {
            case STATUS_NORMAL:
                mLoadMoreLoadingView.setVisibility(View.GONE);
                mLoadMoreTextView.setText(R.string.pulltorefresh_footer_normal);
                break;
            case STATUS_READY:
                mLoadMoreLoadingView.setVisibility(View.GONE);
                mLoadMoreTextView.setText(R.string.pulltorefresh_footer_ready);
                break;
            case STATUS_LOADING:
                mLoadMoreLoadingView.clearAnimation();
                mLoadMoreLoadingView.startAnimation(mRotateLoading);
                mLoadMoreLoadingView.setVisibility(View.VISIBLE);
                mLoadMoreTextView.setText(R.string.pulltorefresh_footer_loading);
                break;
            case STATUS_SUCCESS:
                mLoadMoreLoadingView.setVisibility(View.GONE);
                mLoadMoreTextView.setText(R.string.pulltorefresh_footer_success);
                break;
            case STATUS_FAILED:
                mLoadMoreLoadingView.setVisibility(View.GONE);
                mLoadMoreTextView.setText(R.string.pulltorefresh_footer_fail);
                break;
            case STATUS_END:
                mLoadMoreLoadingView.setVisibility(View.GONE);
                mLoadMoreTextView.setText(R.string.pulltorefresh_footer_end);
                break;
        }
    }

    private class ViewHolder extends RecyclerView.ViewHolder {
        ViewHolder(View itemView) {
            super(itemView);
        }
    }


    /**
     * 注册监听
     */
    private RecyclerView.AdapterDataObserver mDataObserver = new RecyclerView.AdapterDataObserver() {
        @Override
        public void onChanged() {
            super.onChanged();
            notifyDataSetChanged();
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount) {
            super.onItemRangeChanged(positionStart, itemCount);
            notifyItemRangeChanged(positionStart, itemCount);
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount, Object payload) {
            super.onItemRangeChanged(positionStart, itemCount, payload);
            notifyItemRangeChanged(positionStart, itemCount, payload);
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            super.onItemRangeInserted(positionStart, itemCount);
            notifyItemRangeInserted(positionStart, itemCount);
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            super.onItemRangeRemoved(positionStart, itemCount);
            notifyItemRangeRemoved(positionStart, itemCount);
        }

        @Override
        public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
            super.onItemRangeMoved(fromPosition, toPosition, itemCount);
            notifyItemMoved(fromPosition, toPosition);
        }

    };
}
