package com.ishow.smaple.pulltorefresh;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Created by Bright.Yu on 2017/3/23.
 */

class Test2Adapter extends RecyclerAdapter<String, Test2Adapter.ViewHolder> {

    public Test2Adapter(Context context) {
        super(context);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int type) {
        View item = mLayoutInflater.inflate(R.layout.item_test, parent, false);
        return new ViewHolder(item, type);
    }


    @Override
    public void onBindViewHolder(ViewHolder holder, int position, int type) {
        String entry = getItem(position);
        holder.textView.setText(entry);
    }


    class ViewHolder extends RecyclerAdapter.Holder {
        TextView textView;

        ViewHolder(View item, int type) {
            super(item, type);
            textView = (TextView) item.findViewById(R.id.textView);
        }


    }
}
