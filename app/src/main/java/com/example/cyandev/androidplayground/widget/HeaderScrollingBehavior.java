package com.example.cyandev.androidplayground.widget;

import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;
import android.widget.OverScroller;
import android.widget.Scroller;

import com.example.cyandev.androidplayground.R;

import java.lang.ref.WeakReference;

/**
 * Created by cyandev on 2016/11/3.
 */
public class HeaderScrollingBehavior extends CoordinatorLayout.Behavior<RecyclerView> {

    private boolean isExpanded = false;
    private boolean isScrolling = false;

    private WeakReference<View> dependentView;
    private Scroller scroller;
    private OverScroller overScroller;
    private Handler handler;
    private WeakReference<RecyclerView> reChild;

    public HeaderScrollingBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
        scroller = new Scroller(context);
        overScroller = new OverScroller(context);
        handler = new Handler();
    }

    public boolean isExpanded() {
        return isExpanded;
    }

    @Override
    public boolean layoutDependsOn(CoordinatorLayout parent, RecyclerView child, View dependency) {
        if (dependency != null && dependency.getId() == R.id.scrolling_header) {
            dependentView = new WeakReference<>(dependency);
            reChild = new WeakReference<>(child);
            return true;
        }
        return false;
    }

    @Override
    public boolean onLayoutChild(CoordinatorLayout parent, RecyclerView child, int layoutDirection) {
        return super.onLayoutChild(parent, child, layoutDirection);
    }

    @Override
    public boolean onDependentViewChanged(CoordinatorLayout parent, RecyclerView child, View dependency) {
        Resources resources = getDependentView().getResources();
        final float progress = 1.f -
                Math.abs(dependency.getTranslationY() / (dependency.getHeight() - resources.getDimension(R
                        .dimen.collapsed_header_height)));
        child.setTranslationY(dependency.getHeight() + dependency.getTranslationY());
        float scale = 1 + 0.4f * (1.f - progress);
        dependency.setScaleX(scale);
        dependency.setScaleY(scale);
        dependency.setAlpha(progress);
        return true;
    }

    @Override
    public boolean onStartNestedScroll(CoordinatorLayout coordinatorLayout, RecyclerView child, View
            directTargetChild, View target, int nestedScrollAxes) {
        return (nestedScrollAxes & ViewCompat.SCROLL_AXIS_VERTICAL) != 0;
    }

    @Override
    public void onNestedScrollAccepted(CoordinatorLayout coordinatorLayout, RecyclerView child, View
            directTargetChild, View target, int nestedScrollAxes) {
        scroller.abortAnimation();
        overScroller.abortAnimation();
        isScrolling = false;
        super.onNestedScrollAccepted(coordinatorLayout, child, directTargetChild, target, nestedScrollAxes);
    }

    @Override
    public void onNestedPreScroll(CoordinatorLayout coordinatorLayout, RecyclerView child, View target, int
            dx, int dy, int[] consumed) {

        //往上滑动
        View dependentView = getDependentView();
        //或者头部移动的距离
        float newTranslateY = dependentView.getTranslationY() - dy;
        float minHeaderTranslate = -(dependentView.getHeight() - getDependentViewCollapsedHeight());
        if (dy > 0) {
            //-500       -498    dy=5  防止过度滑动
            if (newTranslateY > minHeaderTranslate) {
                dependentView.setTranslationY(newTranslateY);
                consumed[1] = dy;
            } else {
                if (dependentView.getTranslationY() != minHeaderTranslate) {
                    dependentView.setTranslationY(minHeaderTranslate);
                    consumed[1] = (int) (dy - (newTranslateY - minHeaderTranslate));
                }
            }
        } else {
            //0  dependentViewT -5   dy=-8    newT =3   防止过度滑动
            if (!target.canScrollVertically(-1)) {
                if (newTranslateY <= 0) {
                    dependentView.setTranslationY(newTranslateY);
                    consumed[1] = dy;
                } else {
                    dependentView.setTranslationY(0);
                    consumed[1] = (int) (dy + (newTranslateY - 0));
                }
            }
        }

    }

    @Override
    public boolean onNestedPreFling(CoordinatorLayout coordinatorLayout, RecyclerView child, View target,
                                    float velocityX, float velocityY) {
        overScroller.abortAnimation();
        mLastFlingX=0;
        mLastFlingY=0;
        overScroller.fling(0, 0, 0, (int) ((int) velocityY), Integer.MIN_VALUE, Integer.MAX_VALUE,
                Integer.MIN_VALUE, Integer.MAX_VALUE);
        isScrolling = true;
        handler.post(filingRunnable);
        return true;
    }

    @Override
    public boolean onNestedFling(CoordinatorLayout coordinatorLayout, RecyclerView child, View target,
                                 float velocityX, float velocityY, boolean consumed) {
        return super.onNestedFling(coordinatorLayout, child, target, velocityX, velocityY, consumed);
    }


    @Override
    public void onStopNestedScroll(CoordinatorLayout coordinatorLayout, RecyclerView child, View target) {
        if (!isScrolling) {
            onUserStopDragging(800);
        }
    }

    private boolean onUserStopDragging(float velocity) {
        View dependentView = getDependentView();
        float translateY = dependentView.getTranslationY();
        float minHeaderTranslate = -(dependentView.getHeight() - getDependentViewCollapsedHeight());

        if (translateY == 0 || translateY == minHeaderTranslate) {
            return false;
        }

        boolean targetState; // Flag indicates whether to expand the content.
        if (Math.abs(velocity) <= 800) {
            if (Math.abs(translateY) < Math.abs(translateY - minHeaderTranslate)) {
                targetState = false;
            } else {
                targetState = true;
            }
            velocity = 800; // Limit velocity's minimum value.
        } else {
            if (velocity > 0) {
                targetState = true;
            } else {
                targetState = false;
            }
        }

        float targetTranslateY = targetState ? minHeaderTranslate : 0;

        scroller.startScroll(0, (int) translateY, 0, (int) (targetTranslateY - translateY), (int) (1000000
                / Math.abs(velocity)));
        handler.post(scrollRunnable);
        isScrolling = true;
        return true;
    }

    private float getDependentViewCollapsedHeight() {
        return getDependentView().getResources().getDimension(R.dimen.collapsed_header_height);
    }

    private View getDependentView() {
        return dependentView.get();
    }

    private Runnable scrollRunnable = new Runnable() {
        @Override
        public void run() {
            if (scroller.computeScrollOffset()) {
                getDependentView().setTranslationY(scroller.getCurrY());
                handler.post(this);
            } else {
                isExpanded = getDependentView().getTranslationY() != 0;
                isScrolling = false;
            }
        }
    };

    private int mLastFlingX;
    private int mLastFlingY;
    private Runnable filingRunnable = new Runnable() {
        @Override
        public void run() {
            if (overScroller.computeScrollOffset()) {
//                int dy = overScroller.getFinalY() - overScroller.getCurrY();
                final int x = overScroller.getCurrX();
                final int y = overScroller.getCurrY();
                final int dx = x - mLastFlingX;
                final int dy = y - mLastFlingY;
                mLastFlingX = x;
                mLastFlingY = y;
                //向下fling
                int UnconsumedY = dy;
                View dependentView = getDependentView();
                float newTranslateY = dependentView.getTranslationY() - dy;
                float minHeaderTranslate = -(dependentView.getHeight() -
                        getDependentViewCollapsedHeight());
                if (dy > 0) {
                    //-500       -498    dy=5  防止过度滑动
                    if (newTranslateY > minHeaderTranslate) {
                        dependentView.setTranslationY(newTranslateY);
                        UnconsumedY = 0;
                    } else {
                        if (dependentView.getTranslationY() != minHeaderTranslate) {
                            dependentView.setTranslationY(minHeaderTranslate);
                            UnconsumedY = (int) (newTranslateY - minHeaderTranslate);
                        }
                    }
                } else {
                    //0  dependentViewT -5   dy=-8    newT =3   防止过度滑动
                    if (!reChild.get().canScrollVertically(-1)) {
                        if (newTranslateY <= 0) {
                            dependentView.setTranslationY(newTranslateY);
                            UnconsumedY = 0;
                        } else {
                            dependentView.setTranslationY(0);
                            UnconsumedY = (int) dependentView.getTranslationY();
                        }
                    }
                }
                if (UnconsumedY != 0) {
                    reChild.get().scrollBy(0, UnconsumedY);
                }
//                ViewCompat.postOnAnimation(reChild.get(), this);
                handler.post(this);
                if (overScroller.isFinished() && dependentView.getTranslationY() >= minHeaderTranslate &&
                        dependentView.getTranslationY() <= 0) {
                    onUserStopDragging(800);
                }
            }
        }
    };

}
