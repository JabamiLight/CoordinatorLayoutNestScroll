package com.example.cyandev.androidplayground.widget;

import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.OverScroller;
import android.widget.Scroller;

import com.example.cyandev.androidplayground.R;

import java.lang.ref.WeakReference;

import static android.support.v4.view.ViewCompat.TYPE_NON_TOUCH;
import static android.support.v4.view.ViewCompat.TYPE_TOUCH;

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
    private boolean isFiling;
    
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
    public boolean onLayoutChild(CoordinatorLayout parent, RecyclerView child, int
            layoutDirection) {
        return super.onLayoutChild(parent, child, layoutDirection);
    }
    
    @Override
    public boolean onDependentViewChanged(CoordinatorLayout parent, RecyclerView child, View
            dependency) {
        Resources resources = getDependentView().getResources();
        final float progress = 1.f -
                Math.abs(dependency.getTranslationY() / (dependency.getHeight() - resources
                        .getDimension(R
                                .dimen.collapsed_header_height)));
        child.setTranslationY(dependency.getHeight() + dependency.getTranslationY());
        float scale = 1 + 0.4f * (1.f - progress);
        dependency.setScaleX(scale);
        dependency.setScaleY(scale);
        dependency.setAlpha(progress);
        return true;
    }
    
    @Override
    public boolean onStartNestedScroll(@NonNull CoordinatorLayout coordinatorLayout, @NonNull
            RecyclerView child, @NonNull View directTargetChild, @NonNull View target, int axes,
                                       int type) {
        return (axes & ViewCompat.SCROLL_AXIS_VERTICAL) != 0;
    }
    
    @Override
    public void onNestedScrollAccepted(CoordinatorLayout coordinatorLayout, RecyclerView child, View
            directTargetChild, View target, int nestedScrollAxes) {
        scroller.abortAnimation();
        overScroller.abortAnimation();
        isScrolling = false;
        super.onNestedScrollAccepted(coordinatorLayout, child, directTargetChild, target,
                nestedScrollAxes);
    }
    
    
    @Override
    public void onNestedPreScroll(CoordinatorLayout coordinatorLayout, RecyclerView child, View
            target, int
                                          dx, int dy, int[] consumed, int type) {
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
                    consumed[1] = (int) (dy - (dependentView.getTranslationY() -
                            minHeaderTranslate));
                    dependentView.setTranslationY(minHeaderTranslate);
                }
            }
        } else {
            //0  dependentViewT -5   dy=-8    newT =3   防止过度滑动
            if (!ViewCompat.canScrollVertically(target, -1)) {
                if (newTranslateY <= 0) {
                    Log.d("tedu", "小于: ");
                    dependentView.setTranslationY(newTranslateY);
                    consumed[1] = dy;
                } else {
                    Log.d("tedu", "大于: ");
                    dependentView.setTranslationY(0);
                    consumed[1] = (int) (dy + (newTranslateY - 0));
                }
            }
        }
        
    }
    
//
    @Override
    public boolean onNestedPreFling(@NonNull CoordinatorLayout coordinatorLayout, @NonNull
            RecyclerView child, @NonNull View target, float velocityX, float velocityY) {
        //子View将会fling
        isFiling = true;
        return super.onNestedPreFling(coordinatorLayout, child, target, velocityX, velocityY);
    }
    
    @Override
    public void onStopNestedScroll(@NonNull CoordinatorLayout coordinatorLayout, @NonNull
            RecyclerView child, @NonNull View target, int type) {
        Log.d("tedu", "onStopNestedScroll: type="+type);
        if(type==TYPE_TOUCH&&!isFiling){
            onUserStopDragging(800);
        }else if(type==TYPE_NON_TOUCH){
            onUserStopDragging(800);
        }
        isFiling=false;
    
    }
    
    private float getDependentViewCollapsedHeight() {
        return getDependentView().getResources().getDimension(R.dimen.collapsed_header_height);
    }
    
    private View getDependentView() {
        return dependentView.get();
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
        
        scroller.startScroll(0, (int) translateY, 0, (int) (targetTranslateY - translateY), (int)
                (1000000
                        / Math.abs(velocity)));
        handler.post(scrollRunnable);
        return true;
    }
    
    private Runnable scrollRunnable = new Runnable() {
        @Override
        public void run() {
            if (scroller.computeScrollOffset()) {
                getDependentView().setTranslationY(scroller.getCurrY());
                handler.post(this);
            } else {
                isExpanded = getDependentView().getTranslationY() != 0;
            }
        }
    };
    
}
