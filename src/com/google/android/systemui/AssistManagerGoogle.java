package com.google.android.systemui;

import android.content.Context;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.systemui.assist.AssistManager;
import com.android.systemui.statusbar.policy.DeviceProvisionedController;

public class AssistManagerGoogle extends AssistManager {
    private final OpaEnabledDispatcher mOpaEnabledDispatcher = new OpaEnabledDispatcher();
    private final OpaEnabledReceiver mOpaEnabledReceiver = new OpaEnabledReceiver(mContext);
    private final KeyguardUpdateMonitorCallback mUserSwitchCallback = new KeyguardUpdateMonitorCallback() {
        @Override
        public void onUserSwitching(int userId) {
            mOpaEnabledReceiver.onUserSwitching(userId);
        }
    };

    public AssistManagerGoogle(DeviceProvisionedController controller, Context context) {
        super(controller, context);
        addOpaEnabledListener(mOpaEnabledDispatcher);
        KeyguardUpdateMonitor.getInstance(mContext).registerCallback(mUserSwitchCallback);
    }

    public boolean shouldShowOrb() {
        return false;
    }

    public void addOpaEnabledListener(OpaEnabledListener listener) {
        mOpaEnabledReceiver.addOpaEnabledListener(listener);
    }

    public void dispatchOpaEnabledState() {
        mOpaEnabledReceiver.dispatchOpaEnabledState();
    }
}
