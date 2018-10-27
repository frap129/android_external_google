package com.google.android.systemui;

import android.content.Context;
import android.util.ArrayMap;

import com.android.systemui.Dependency;
import com.android.systemui.Dependency.DependencyProvider;
import com.android.systemui.statusbar.NotificationLockscreenUserManager;
import com.android.systemui.statusbar.policy.DeviceProvisionedController;
import com.android.systemui.SystemUIFactory;
import com.android.systemui.assist.AssistManager;

public class SystemUIGoogleFactory extends SystemUIFactory {
    public void injectDependencies(ArrayMap<Object, DependencyProvider> providers, Context context) {
        super.injectDependencies(providers, context);
        providers.put(AssistManager.class,
                () -> new AssistManagerGoogle((DeviceProvisionedController) Dependency.get(DeviceProvisionedController.class), context));
        providers.put(NotificationLockscreenUserManager.class,
                () -> new NotificationLockscreenUserManagerGoogle(context));
    }
}
