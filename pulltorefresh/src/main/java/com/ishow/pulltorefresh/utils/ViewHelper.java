package com.ishow.pulltorefresh.utils;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewCompat;
import android.view.View;

import com.ishow.pulltorefresh.R;

/**
 * Created by Bright.Yu on 2017/3/27.
 * View Helper
 */

public class ViewHelper {
    private static final int DEFAULT_1000_PIXELS_TIMES = 1600;

    public static void movingY(final @NonNull View view, int distance) {
        int duration = calculatingDuration(distance);
        movingY(view, duration, distance, null);
    }

    @SuppressWarnings("WeakerAccess")
    public static void movingY(final @NonNull View view, int distance, Animator.AnimatorListener listener) {
        int duration = calculatingDuration(distance);
        movingY(view, duration, distance, listener);
    }

    @SuppressWarnings("WeakerAccess")
    public static void movingY(final @NonNull View view, @IntRange(from = 1) int duration, int distance, Animator.AnimatorListener listener) {
        ValueAnimator animator = ValueAnimator.ofInt(distance);
        animator.setTarget(view);
        animator.setDuration(duration);
        if (listener != null) {
            animator.addListener(listener);
        }
        animator.start();
        view.setTag(R.id.tag_pull_to_refresh_moving_y, 0);
        view.setTag(R.id.tag_pull_to_refresh_animation, animator);
        
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                final int last = (int) view.getTag(R.id.tag_pull_to_refresh_moving_y);
                final int move = (int) animation.getAnimatedValue();
                int offset = move - last;
                view.setTag(R.id.tag_pull_to_refresh_moving_y, move);
                ViewCompat.offsetTopAndBottom(view, offset);
            }
        });
    }

    private static int calculatingDuration(final int distance) {
        int desTime = DEFAULT_1000_PIXELS_TIMES * Math.abs(distance) / 1000;
        return Math.max(desTime, 500);
    }
}
