package com.google.android.systemui;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.provider.Settings.Secure;
import android.util.Log;
import com.android.internal.app.AssistUtils;
import com.android.internal.widget.ILockSettings;
import com.android.internal.widget.ILockSettings.Stub;
import java.util.ArrayList;
import java.util.List;

public class OpaEnabledReceiver {
    private final BroadcastReceiver mBroadcastReceiver = new OpaEnabledBroadcastReceiver();
    private final ContentObserver mContentObserver;
    private final ContentResolver mContentResolver;
    private final Context mContext;
    private boolean mIsAGSAAssistant;
    private boolean mIsOpaEligible;
    private boolean mIsOpaEnabled;
    private final List<OpaEnabledListener> mListeners = new ArrayList();
    private final ILockSettings mLockSettings;

    private class AssistantContentObserver extends ContentObserver {
        public AssistantContentObserver(Context context) {
            super(new Handler(context.getMainLooper()));
        }

        public void onChange(boolean selfChange, Uri uri) {
            OpaEnabledReceiver.this.updateOpaEnabledState(OpaEnabledReceiver.this.mContext);
            OpaEnabledReceiver.this.dispatchOpaEnabledState(OpaEnabledReceiver.this.mContext);
        }
    }

    private class OpaEnabledBroadcastReceiver extends BroadcastReceiver {
        private OpaEnabledBroadcastReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("com.google.android.systemui.OPA_ENABLED")) {
                Secure.putIntForUser(context.getContentResolver(), "systemui.google.opa_enabled", intent.getBooleanExtra("OPA_ENABLED", false) ? 1 : 0, UserHandle.USER_CURRENT);
            } else if (intent.getAction().equals("com.google.android.systemui.OPA_USER_ENABLED")) {
                try {
                    OpaEnabledReceiver.this.mLockSettings.setBoolean("systemui.google.opa_user_enabled", intent.getBooleanExtra("OPA_USER_ENABLED", false), UserHandle.USER_CURRENT);
                } catch (RemoteException e) {
                    Log.e("OpaEnabledReceiver", "RemoteException on OPA_USER_ENABLED", e);
                }
            }
            OpaEnabledReceiver.this.updateOpaEnabledState(context);
            OpaEnabledReceiver.this.dispatchOpaEnabledState(context);
        }
    }

    public OpaEnabledReceiver(Context context) {
        this.mContext = context;
        this.mContentResolver = this.mContext.getContentResolver();
        this.mContentObserver = new AssistantContentObserver(this.mContext);
        this.mLockSettings = Stub.asInterface(ServiceManager.getService("lock_settings"));
        updateOpaEnabledState(this.mContext);
        registerContentObserver();
        registerEnabledReceiver(-2);
    }

    public void addOpaEnabledListener(OpaEnabledListener listener) {
        this.mListeners.add(listener);
        listener.onOpaEnabledReceived(this.mContext, this.mIsOpaEligible, this.mIsAGSAAssistant, this.mIsOpaEnabled);
    }

    public void onUserSwitching(int userId) {
        updateOpaEnabledState(this.mContext);
        dispatchOpaEnabledState(this.mContext);
        this.mContentResolver.unregisterContentObserver(this.mContentObserver);
        registerContentObserver();
        this.mContext.unregisterReceiver(this.mBroadcastReceiver);
        registerEnabledReceiver(userId);
    }

    private boolean isOpaEligible(Context context) {
        return Secure.getIntForUser(context.getContentResolver(), "systemui.google.opa_enabled", 0, UserHandle.USER_CURRENT) != 0;
    }

    private boolean isAGSACurrentAssistant(Context context) {
        ComponentName assistant = new AssistUtils(context).getAssistComponentForUser(UserHandle.USER_CURRENT);
        return assistant != null && "com.google.android.googlequicksearchbox/com.google.android.voiceinteraction.GsaVoiceInteractionService".equals(assistant.flattenToString());
    }

    private boolean isOpaEnabled(Context context) {
        try {
            return this.mLockSettings.getBoolean("systemui.google.opa_user_enabled", false, UserHandle.USER_CURRENT);
        } catch (RemoteException e) {
            Log.e("OpaEnabledReceiver", "isOpaEnabled RemoteException", e);
            return false;
        }
    }

    private void updateOpaEnabledState(Context context) {
        this.mIsOpaEligible = isOpaEligible(context);
        this.mIsAGSAAssistant = isAGSACurrentAssistant(context);
        this.mIsOpaEnabled = isOpaEnabled(context);
    }

    public void dispatchOpaEnabledState() {
        dispatchOpaEnabledState(this.mContext);
    }

    private void dispatchOpaEnabledState(Context context) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Dispatching OPA eligble = ");
        stringBuilder.append(this.mIsOpaEligible);
        stringBuilder.append("; AGSA = ");
        stringBuilder.append(this.mIsAGSAAssistant);
        stringBuilder.append("; OPA enabled = ");
        stringBuilder.append(this.mIsOpaEnabled);
        Log.i("OpaEnabledReceiver", stringBuilder.toString());
        for (int i = 0; i < this.mListeners.size(); i++) {
            ((OpaEnabledListener) this.mListeners.get(i)).onOpaEnabledReceived(context, this.mIsOpaEligible, this.mIsAGSAAssistant, this.mIsOpaEnabled);
        }
    }

    private void registerContentObserver() {
        this.mContentResolver.registerContentObserver(Secure.getUriFor("assistant"), false, this.mContentObserver, UserHandle.USER_CURRENT);
    }

    private void registerEnabledReceiver(int userId) {
        this.mContext.registerReceiverAsUser(this.mBroadcastReceiver, new UserHandle(userId), new IntentFilter("com.google.android.systemui.OPA_ENABLED"), null, null);
        this.mContext.registerReceiverAsUser(this.mBroadcastReceiver, new UserHandle(userId), new IntentFilter("com.google.android.systemui.OPA_USER_ENABLED"), null, null);
    }
}
