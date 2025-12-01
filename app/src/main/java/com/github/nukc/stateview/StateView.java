package com.github.nukc.stateview;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * StateView - Stub implementation to replace com.github.nukc.stateview:kotlin:2.2.0
 * 
 * A custom view that manages different states: loading, empty, content, error
 * This is a minimal implementation that provides the core functionality
 * used by the VCamera app.
 */
public class StateView extends FrameLayout {
    
    private View loadingView;
    private View emptyView;
    private View errorView;
    private View contentView;
    private View currentStateView;
    
    private int loadingLayoutId = 0;
    private int emptyLayoutId = 0;
    private int errorLayoutId = 0;
    
    private static final int STATE_CONTENT = 0;
    private static final int STATE_LOADING = 1;
    private static final int STATE_EMPTY = 2;
    private static final int STATE_ERROR = 3;
    
    private int currentState = STATE_CONTENT;
    
    public StateView(@NonNull Context context) {
        super(context);
        init();
    }
    
    public StateView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    public StateView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    
    private void init() {
        // Initialize with content state visible by default
        setVisibility(GONE);
    }
    
    /**
     * Show content state - hide the state view overlay
     */
    public void showContent() {
        currentState = STATE_CONTENT;
        setVisibility(GONE);
        hideAllStates();
    }
    
    /**
     * Show loading state
     */
    public void showLoading() {
        currentState = STATE_LOADING;
        setVisibility(VISIBLE);
        hideAllStates();
        
        if (loadingView == null && loadingLayoutId != 0) {
            loadingView = LayoutInflater.from(getContext()).inflate(loadingLayoutId, this, false);
            addView(loadingView);
        }
        
        if (loadingView != null) {
            loadingView.setVisibility(VISIBLE);
            currentStateView = loadingView;
        }
    }
    
    /**
     * Show empty state
     */
    public void showEmpty() {
        currentState = STATE_EMPTY;
        setVisibility(VISIBLE);
        hideAllStates();
        
        if (emptyView == null && emptyLayoutId != 0) {
            emptyView = LayoutInflater.from(getContext()).inflate(emptyLayoutId, this, false);
            addView(emptyView);
        }
        
        if (emptyView != null) {
            emptyView.setVisibility(VISIBLE);
            currentStateView = emptyView;
        }
    }
    
    /**
     * Show error state
     */
    public void showError() {
        currentState = STATE_ERROR;
        setVisibility(VISIBLE);
        hideAllStates();
        
        if (errorView == null && errorLayoutId != 0) {
            errorView = LayoutInflater.from(getContext()).inflate(errorLayoutId, this, false);
            addView(errorView);
        }
        
        if (errorView != null) {
            errorView.setVisibility(VISIBLE);
            currentStateView = errorView;
        }
    }
    
    /**
     * Hide all state views
     */
    private void hideAllStates() {
        if (loadingView != null) {
            loadingView.setVisibility(GONE);
        }
        if (emptyView != null) {
            emptyView.setVisibility(GONE);
        }
        if (errorView != null) {
            errorView.setVisibility(GONE);
        }
        currentStateView = null;
    }
    
    /**
     * Set custom layout for loading state
     */
    public void setLoadingResource(@LayoutRes int layoutId) {
        this.loadingLayoutId = layoutId;
    }
    
    /**
     * Set custom layout for empty state
     */
    public void setEmptyResource(@LayoutRes int layoutId) {
        this.emptyLayoutId = layoutId;
    }
    
    /**
     * Set custom layout for error state
     */
    public void setErrorResource(@LayoutRes int layoutId) {
        this.errorLayoutId = layoutId;
    }
    
    /**
     * Get current state
     */
    public int getState() {
        return currentState;
    }
}
