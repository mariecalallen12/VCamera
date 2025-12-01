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
        showStateView(loadingView, loadingLayoutId);
    }
    
    /**
     * Show empty state
     */
    public void showEmpty() {
        currentState = STATE_EMPTY;
        showStateView(emptyView, emptyLayoutId);
    }
    
    /**
     * Show error state
     */
    public void showError() {
        currentState = STATE_ERROR;
        showStateView(errorView, errorLayoutId);
    }
    
    /**
     * Helper method to show a state view
     */
    private void showStateView(View view, int layoutId) {
        setVisibility(VISIBLE);
        hideAllStates();
        
        // Inflate view if needed
        if (view == null && layoutId != 0) {
            view = LayoutInflater.from(getContext()).inflate(layoutId, this, false);
            addView(view);
            
            // Update the corresponding state view reference
            if (currentState == STATE_LOADING) {
                loadingView = view;
            } else if (currentState == STATE_EMPTY) {
                emptyView = view;
            } else if (currentState == STATE_ERROR) {
                errorView = view;
            }
        }
        
        // Show the view
        if (view != null) {
            view.setVisibility(VISIBLE);
            currentStateView = view;
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
