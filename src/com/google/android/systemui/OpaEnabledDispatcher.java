package com.google.android.systemui;

import android.content.Context;
import android.os.UserManager;
import android.view.View;
import com.android.systemui.SysUiServiceProvider;
import com.android.systemui.statusbar.phone.StatusBar;
import java.util.ArrayList;

public class OpaEnabledDispatcher implements OpaEnabledListener {
    public void onOpaEnabledReceived(Context context, boolean eligible, boolean agsaEnabled, boolean opaEnabled) {
        boolean z = (eligible && agsaEnabled) || UserManager.isDeviceInDemoMode(context);
        dispatchUnchecked(context, z);
    }

    private void dispatchUnchecked(Context context, boolean enabled) {
        StatusBar bar = (StatusBar) SysUiServiceProvider.getComponent(context, StatusBar.class);
        if (bar != null && bar.getNavigationBarView() != null) {
            ArrayList<View> views = bar.getNavigationBarView().getHomeButton().getViews();
            for (int i = 0; i < views.size(); i++) {
                ((OpaLayout) ((View) views.get(i))).setOpaEnabled(enabled);
            }
        }
    }
}
