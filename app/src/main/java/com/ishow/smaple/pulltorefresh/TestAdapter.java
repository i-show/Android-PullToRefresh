package com.ishow.smaple.pulltorefresh;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ishow.pulltorefresh.recycleview.LinearLayoutAdapter;

/**
 * Created by Bright.Yu on 2017/3/23.
 */

class TestAdapter extends LinearLayoutAdapter<String, TestAdapter.ViewHolder> {

    public TestAdapter(Context context) {
        super(context);
    }

    @Override
    public ViewHolder onCreateBodyHolder(ViewGroup parent, int viewType) {
        View item = mLayoutInflater.inflate(R.layout.item_test, parent, false);
        return new ViewHolder(item, viewType);
    }

    @Override
    public void onBindBodyHolder(ViewHolder holder, int postion) {
        String entry = getItem(postion);
        holder.textView.setText(entry);
    }


    class ViewHolder extends LinearLayoutAdapter.Holder {
        TextView textView;

        public ViewHolder(View item, int type) {
            super(item, type);
        }

        @Override
        public void findBody(View item) {
            super.findBody(item);
            textView = (TextView) item.findViewById(R.id.textView);
        }
    }
}
