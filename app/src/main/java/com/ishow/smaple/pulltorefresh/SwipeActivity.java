package com.ishow.smaple.pulltorefresh;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.ishow.pulltorefresh.recycleview.LoadMoreAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by yuhaiyang on 2017/4/18.
 */

public class SwipeActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_swipe);

        final Test2Adapter adapter = new Test2Adapter(this);
        adapter.setData(getData(adapter));
        LoadMoreAdapter wrapper = new LoadMoreAdapter(this, adapter);
        GridLayoutManager manager = new GridLayoutManager(this, 2);

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.list);
        recyclerView.setLayoutManager(manager);
        recyclerView.setAdapter(wrapper);
    }


    private List<String> getData(RecyclerView.Adapter adapter) {
        List<String> list = new ArrayList<>();

        int size = 0;
        if (adapter != null) {
            size = adapter.getItemCount();
        }
        for (int i = 0; i < 20; i++) {
            int index = size + i;
            list.add("postion " + index);
        }
        return list;
    }
}
