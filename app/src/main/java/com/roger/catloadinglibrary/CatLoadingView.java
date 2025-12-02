package com.roger.catloadinglibrary;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import virtual.camera.app.R;

/**
 * CatLoadingView - Stub implementation to replace com.roger.catloadinglibrary:catloadinglibrary:1.0.9
 * 
 * A simple loading dialog fragment that shows a loading indicator
 * This is a minimal implementation that provides the core functionality
 * used by the VCamera app.
 */
public class CatLoadingView extends DialogFragment {
    
    private int backgroundColor = Color.TRANSPARENT;
    private boolean clickCancelAble = true;
    
    public CatLoadingView() {
        // Required empty constructor for DialogFragment
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Use a simple dialog style
        setStyle(DialogFragment.STYLE_NO_TITLE, android.R.style.Theme_Material_Dialog);
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Create a simple loading view
        View view = inflater.inflate(R.layout.dialog_loading, container, false);
        
        // Apply background color if set (accepts color resource ID)
        if (backgroundColor != Color.TRANSPARENT && getContext() != null) {
            try {
                // Try to resolve as color resource ID first
                int color = androidx.core.content.ContextCompat.getColor(getContext(), backgroundColor);
                view.setBackgroundColor(color);
            } catch (Exception e) {
                // If not a valid resource ID, treat as direct color value
                view.setBackgroundColor(backgroundColor);
            }
        }
        
        return view;
    }
    
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        
        // Remove dialog title
        Window window = dialog.getWindow();
        if (window != null) {
            window.requestFeature(Window.FEATURE_NO_TITLE);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        
        // Set cancelable based on configuration
        dialog.setCancelable(clickCancelAble);
        dialog.setCanceledOnTouchOutside(clickCancelAble);
        
        return dialog;
    }
    
    /**
     * Set background color for the loading view
     * @param colorRes Color resource ID
     */
    public void setBackgroundColor(int colorRes) {
        // Accept color resource ID and store it for use in onCreateView
        this.backgroundColor = colorRes;
    }
    
    /**
     * Set whether the dialog can be cancelled by clicking outside or back button
     * @param cancelable true if cancellable, false otherwise
     */
    public void setClickCancelAble(boolean cancelable) {
        this.clickCancelAble = cancelable;
        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.setCancelable(cancelable);
            dialog.setCanceledOnTouchOutside(cancelable);
        }
    }
    
    /**
     * Show the loading dialog
     * @param manager FragmentManager
     * @param tag Optional tag for the fragment
     */
    public void show(@NonNull FragmentManager manager, @Nullable String tag) {
        try {
            super.show(manager, tag);
        } catch (IllegalStateException e) {
            // Handle case where fragment is already added
            e.printStackTrace();
        }
    }
    
    /**
     * Check if dialog is already added to fragment manager
     * @return true if added, false otherwise
     */
    public boolean isAdded() {
        return super.isAdded();
    }
}
