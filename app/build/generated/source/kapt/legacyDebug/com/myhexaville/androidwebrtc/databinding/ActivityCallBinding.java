package com.myhexaville.androidwebrtc.databinding;
import com.myhexaville.androidwebrtc.R;
import com.myhexaville.androidwebrtc.BR;
import android.view.View;
public class ActivityCallBinding extends android.databinding.ViewDataBinding  {

    private static final android.databinding.ViewDataBinding.IncludedLayouts sIncludes;
    private static final android.util.SparseIntArray sViewsWithIds;
    static {
        sIncludes = null;
        sViewsWithIds = new android.util.SparseIntArray();
        sViewsWithIds.put(R.id.remote_video_layout, 1);
        sViewsWithIds.put(R.id.remote_video_view, 2);
        sViewsWithIds.put(R.id.local_video_layout, 3);
        sViewsWithIds.put(R.id.local_video_view, 4);
        sViewsWithIds.put(R.id.call_fragment_container, 5);
        sViewsWithIds.put(R.id.buttons_call_container, 6);
        sViewsWithIds.put(R.id.capture_format_text_call, 7);
        sViewsWithIds.put(R.id.capture_format_slider_call, 8);
    }
    // views
    public final android.widget.LinearLayout buttonsCallContainer;
    public final android.widget.FrameLayout callFragmentContainer;
    public final android.widget.SeekBar captureFormatSliderCall;
    public final android.widget.TextView captureFormatTextCall;
    public final com.myhexaville.androidwebrtc.view.PercentFrameLayout localVideoLayout;
    public final org.webrtc.SurfaceViewRenderer localVideoView;
    private final android.widget.RelativeLayout mboundView0;
    public final com.myhexaville.androidwebrtc.view.PercentFrameLayout remoteVideoLayout;
    public final org.webrtc.SurfaceViewRenderer remoteVideoView;
    // variables
    // values
    // listeners
    // Inverse Binding Event Handlers

    public ActivityCallBinding(android.databinding.DataBindingComponent bindingComponent, View root) {
        super(bindingComponent, root, 0);
        final Object[] bindings = mapBindings(bindingComponent, root, 9, sIncludes, sViewsWithIds);
        this.buttonsCallContainer = (android.widget.LinearLayout) bindings[6];
        this.callFragmentContainer = (android.widget.FrameLayout) bindings[5];
        this.captureFormatSliderCall = (android.widget.SeekBar) bindings[8];
        this.captureFormatTextCall = (android.widget.TextView) bindings[7];
        this.localVideoLayout = (com.myhexaville.androidwebrtc.view.PercentFrameLayout) bindings[3];
        this.localVideoView = (org.webrtc.SurfaceViewRenderer) bindings[4];
        this.mboundView0 = (android.widget.RelativeLayout) bindings[0];
        this.mboundView0.setTag(null);
        this.remoteVideoLayout = (com.myhexaville.androidwebrtc.view.PercentFrameLayout) bindings[1];
        this.remoteVideoView = (org.webrtc.SurfaceViewRenderer) bindings[2];
        setRootTag(root);
        // listeners
        invalidateAll();
    }

    @Override
    public void invalidateAll() {
        synchronized(this) {
                mDirtyFlags = 0x1L;
        }
        requestRebind();
    }

    @Override
    public boolean hasPendingBindings() {
        synchronized(this) {
            if (mDirtyFlags != 0) {
                return true;
            }
        }
        return false;
    }

    public boolean setVariable(int variableId, Object variable) {
        switch(variableId) {
        }
        return false;
    }

    @Override
    protected boolean onFieldChange(int localFieldId, Object object, int fieldId) {
        switch (localFieldId) {
        }
        return false;
    }

    @Override
    protected void executeBindings() {
        long dirtyFlags = 0;
        synchronized(this) {
            dirtyFlags = mDirtyFlags;
            mDirtyFlags = 0;
        }
        // batch finished
    }
    // Listener Stub Implementations
    // callback impls
    // dirty flag
    private  long mDirtyFlags = 0xffffffffffffffffL;

    public static ActivityCallBinding inflate(android.view.LayoutInflater inflater, android.view.ViewGroup root, boolean attachToRoot) {
        return inflate(inflater, root, attachToRoot, android.databinding.DataBindingUtil.getDefaultComponent());
    }
    public static ActivityCallBinding inflate(android.view.LayoutInflater inflater, android.view.ViewGroup root, boolean attachToRoot, android.databinding.DataBindingComponent bindingComponent) {
        return android.databinding.DataBindingUtil.<ActivityCallBinding>inflate(inflater, com.myhexaville.androidwebrtc.R.layout.activity_call, root, attachToRoot, bindingComponent);
    }
    public static ActivityCallBinding inflate(android.view.LayoutInflater inflater) {
        return inflate(inflater, android.databinding.DataBindingUtil.getDefaultComponent());
    }
    public static ActivityCallBinding inflate(android.view.LayoutInflater inflater, android.databinding.DataBindingComponent bindingComponent) {
        return bind(inflater.inflate(com.myhexaville.androidwebrtc.R.layout.activity_call, null, false), bindingComponent);
    }
    public static ActivityCallBinding bind(android.view.View view) {
        return bind(view, android.databinding.DataBindingUtil.getDefaultComponent());
    }
    public static ActivityCallBinding bind(android.view.View view, android.databinding.DataBindingComponent bindingComponent) {
        if (!"layout/activity_call_0".equals(view.getTag())) {
            throw new RuntimeException("view tag isn't correct on view:" + view.getTag());
        }
        return new ActivityCallBinding(bindingComponent, view);
    }
    /* flag mapping
        flag 0 (0x1L): null
    flag mapping end*/
    //end
}