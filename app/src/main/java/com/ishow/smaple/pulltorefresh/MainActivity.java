package com.ishow.smaple.pulltorefresh;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.ishow.pulltorefresh.OnPullToRefreshListener;
import com.ishow.pulltorefresh.PullToRefreshView;
import com.ishow.pulltorefresh.classic.ClassicHeader;

import java.util.ArrayList;
import java.util.List;

import jp.wasabeef.recyclerview.animators.FadeInRightAnimator;
import jp.wasabeef.recyclerview.animators.ScaleInRightAnimator;
import jp.wasabeef.recyclerview.animators.SlideInLeftAnimator;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ClassicHeader header = new ClassicHeader(this);

        final TestAdapter adapter = new TestAdapter(this);
        adapter.setData(getData(adapter));
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.list);
        recyclerView.setAdapter(adapter);
        recyclerView.setItemAnimator(new ScaleInRightAnimator());
        recyclerView.getItemAnimator().setAddDuration(5000);
        recyclerView.getItemAnimator().setChangeDuration(5000);
        recyclerView.getItemAnimator().setMoveDuration(5000);
        recyclerView.getItemAnimator().setRemoveDuration(5000);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        final PullToRefreshView pullToRefreshView = (PullToRefreshView) findViewById(R.id.pulltorefresh);
        pullToRefreshView.setHeader(header);
        pullToRefreshView.setFooter(adapter);
        pullToRefreshView.setOnPullToRefreshListener(new OnPullToRefreshListener() {
            @Override
            public void onRefresh(View v) {
                v.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        pullToRefreshView.setRefreshSuccess();
                        pullToRefreshView.setLoadMoreNormal();
                        adapter.setData(getData(null));
                    }
                }, 3000);
            }

            @Override
            public void onLoadMore(View v) {
                v.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        adapter.addData(getData(adapter));
                        if (adapter.getItemCount() >= 15) {
                            pullToRefreshView.setLoadMoreEnd();
                        }
                    }
                }, 3000);
            }
        });


    }


    private List<String> getData(TestAdapter adapter) {
        List<String> list = new ArrayList<>();

        int size = 0;
        if (adapter != null) {
            size = adapter.getItemCount();
        }
        for (int i = 0; i < 10; i++) {
            int index = size + i;
            list.add("postion " + index);
        }
        return list;
    }

}
