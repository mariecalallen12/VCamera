package com.imuxuan.floatingview;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * FloatingMagnetView - Stub implementation to replace com.imuxuan:floatingview:1.6
 * 
 * A draggable floating view that can stick to screen edges (magnetic behavior)
 * This is a minimal implementation that provides the core functionality
 * used by the VCamera app.
 */
public class FloatingMagnetView extends FrameLayout {
    
    private float initialX;
    private float initialY;
    private float initialTouchX;
    private float initialTouchY;
    private boolean isDragging = false;
    private int touchSlop;
    private long lastTouchTime = 0;
    
    // Magnetic behavior - stick to edges
    private boolean magneticToEdge = true;
    private int screenWidth;
    private int screenHeight;
    
    public FloatingMagnetView(@NonNull Context context) {
        super(context);
        init(context);
    }
    
    public FloatingMagnetView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }
    
    public FloatingMagnetView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }
    
    private void init(Context context) {
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        
        // Get screen dimensions
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (wm != null) {
            android.util.DisplayMetrics metrics = new android.util.DisplayMetrics();
            wm.getDefaultDisplay().getMetrics(metrics);
            screenWidth = metrics.widthPixels;
            screenHeight = metrics.heightPixels;
        }
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                initialX = getX();
                initialY = getY();
                initialTouchX = event.getRawX();
                initialTouchY = event.getRawY();
                lastTouchTime = System.currentTimeMillis();
                isDragging = false;
                return true;
                
            case MotionEvent.ACTION_MOVE:
                float deltaX = event.getRawX() - initialTouchX;
                float deltaY = event.getRawY() - initialTouchY;
                
                // Check if movement exceeds touch slop
                if (!isDragging && (Math.abs(deltaX) > touchSlop || Math.abs(deltaY) > touchSlop)) {
                    isDragging = true;
                }
                
                if (isDragging) {
                    // Move the view
                    setX(initialX + deltaX);
                    setY(initialY + deltaY);
                }
                return true;
                
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (isDragging && magneticToEdge) {
                    // Animate to nearest edge
                    animateToNearestEdge();
                }
                
                // Check for click (quick touch without drag)
                long touchDuration = System.currentTimeMillis() - lastTouchTime;
                if (!isDragging && touchDuration < 200) {
                    performClick();
                }
                
                isDragging = false;
                return true;
        }
        
        return super.onTouchEvent(event);
    }
    
    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }
    
    /**
     * Animate the view to the nearest screen edge
     */
    private void animateToNearestEdge() {
        float currentX = getX();
        float currentY = getY();
        
        // Calculate center position
        float centerX = currentX + getWidth() / 2f;
        
        // Determine which edge is closer (left or right)
        float targetX;
        if (centerX < screenWidth / 2f) {
            // Stick to left edge
            targetX = 0;
        } else {
            // Stick to right edge
            targetX = screenWidth - getWidth();
        }
        
        // Animate to target position
        ObjectAnimator animator = ObjectAnimator.ofFloat(this, "x", currentX, targetX);
        animator.setDuration(300);
        animator.start();
        
        // Keep Y position within bounds
        float targetY = currentY;
        if (targetY < 0) {
            targetY = 0;
        } else if (targetY > screenHeight - getHeight()) {
            targetY = screenHeight - getHeight();
        }
        
        if (targetY != currentY) {
            ObjectAnimator yAnimator = ObjectAnimator.ofFloat(this, "y", currentY, targetY);
            yAnimator.setDuration(300);
            yAnimator.start();
        }
    }
    
    /**
     * Enable or disable magnetic behavior (sticking to edges)
     */
    public void setMagneticToEdge(boolean magnetic) {
        this.magneticToEdge = magnetic;
    }
    
    /**
     * Update screen dimensions (call when configuration changes)
     */
    public void updateScreenSize(int width, int height) {
        this.screenWidth = width;
        this.screenHeight = height;
    }
}
