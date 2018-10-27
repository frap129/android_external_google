package com.google.android.systemui.statusbar.phone;

import com.android.systemui.Dependency;
import com.android.systemui.statusbar.NotificationLockscreenUserManager;
import com.android.systemui.statusbar.phone.StatusBar;
import com.google.android.systemui.NotificationLockscreenUserManagerGoogle;
import com.google.android.systemui.smartspace.SmartSpaceController;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class StatusBarGoogle extends StatusBar {
    public void start() {
        super.start();
        ((NotificationLockscreenUserManagerGoogle) Dependency.get(NotificationLockscreenUserManager.class)).updateAodVisibilitySettings();
    }

    protected void setLockscreenUser(int newUserId) {
        super.setLockscreenUser(newUserId);
        SmartSpaceController.get(this.mContext).reloadData();
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        super.dump(fd, pw, args);
        SmartSpaceController.get(this.mContext).dump(fd, pw, args);
    }
}
