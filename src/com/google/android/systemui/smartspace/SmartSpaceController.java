package com.google.android.systemui.smartspace;

import android.app.AlarmManager;
import android.app.AlarmManager.OnAlarmListener;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.UserHandle;
import android.provider.Settings.Global;
import android.util.KeyValueListParser;
import android.util.Log;

import com.android.systemui.Dumpable;
import com.android.systemui.smartspace.nano.SmartspaceProto.CardWrapper;
import com.android.systemui.util.Assert;

import java.io.FileDescriptor;
import java.io.PrintWriter;

public class SmartSpaceController implements Dumpable {
    static final boolean DEBUG = Log.isLoggable("SmartSpaceController", 3);
    private static SmartSpaceController sInstance;
    private final AlarmManager mAlarmManager;
    private boolean mAlarmRegistered;
    private final Context mAppContext;
    private final Handler mBackgroundHandler;
    private final Context mContext;
    private int mCurrentUserId;
    private final SmartSpaceData mData;
    private final OnAlarmListener mExpireAlarmAction = new OnAlarmListener () {
        @Override
        public final void onAlarm() {
            onExpire(false);
        }
    };
    private SmartSpaceUpdateListener mListener;
    private boolean mSmartSpaceEnabledBroadcastSent;
    private final ProtoStore mStore;
    private final Handler mUiHandler;

    /* renamed from: com.google.android.systemui.smartspace.SmartSpaceController$1 */
    class C14411 extends BroadcastReceiver {
        C14411() {
        }

        public void onReceive(Context context, Intent intent) {
            SmartSpaceController.this.onGsaChanged();
        }
    }

    private class UserSwitchReceiver extends BroadcastReceiver {
        private UserSwitchReceiver() {
        }

        /* synthetic */ UserSwitchReceiver(SmartSpaceController x0, C14411 x1) {
            this();
        }

        public void onReceive(Context context, Intent intent) {
            if (SmartSpaceController.DEBUG) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Switching user: ");
                stringBuilder.append(intent.getAction());
                stringBuilder.append(" uid: ");
                stringBuilder.append(UserHandle.myUserId());
                Log.d("SmartSpaceController", stringBuilder.toString());
            }
            if (intent.getAction().equals("android.intent.action.USER_SWITCHED")) {
                SmartSpaceController.this.mCurrentUserId = intent.getIntExtra("android.intent.extra.user_handle", -1);
                SmartSpaceController.this.mData.clear();
                SmartSpaceController.this.onExpire(true);
            }
            SmartSpaceController.this.onExpire(true);
        }
    }

    public static SmartSpaceController get(Context context) {
        if (sInstance == null) {
            if (DEBUG) {
                Log.d("SmartSpaceController", "controller created");
            }
            sInstance = new SmartSpaceController(context.getApplicationContext());
        }
        return sInstance;
    }

    private SmartSpaceController(Context context) {
        this.mContext = context;
        this.mUiHandler = new Handler(Looper.getMainLooper());
        this.mStore = new ProtoStore(this.mContext);
        HandlerThread loaderThread = new HandlerThread("smartspace-background");
        loaderThread.start();
        this.mBackgroundHandler = new Handler(loaderThread.getLooper());
        this.mCurrentUserId = UserHandle.myUserId();
        this.mAppContext = context;
        this.mAlarmManager = (AlarmManager) context.getSystemService(AlarmManager.class);
        this.mData = new SmartSpaceData();
        if (!isSmartSpaceDisabledByExperiments()) {
            reloadData();
            onGsaChanged();
            context.registerReceiver(new C14411(), GSAIntents.getGsaPackageFilter("android.intent.action.PACKAGE_ADDED", "android.intent.action.PACKAGE_CHANGED", "android.intent.action.PACKAGE_REMOVED", "android.intent.action.PACKAGE_DATA_CLEARED"));
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.intent.action.USER_SWITCHED");
            filter.addAction("android.intent.action.USER_UNLOCKED");
            context.registerReceiver(new UserSwitchReceiver(this, null), filter);
            context.registerReceiver(new SmartSpaceBroadcastReceiver(this), new IntentFilter("com.google.android.apps.nexuslauncher.UPDATE_SMARTSPACE"));
        }
    }

    private SmartSpaceCard loadSmartSpaceData(boolean primary) {
        CardWrapper output = new CardWrapper();
        ProtoStore protoStore = this.mStore;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("smartspace_");
        stringBuilder.append(this.mCurrentUserId);
        stringBuilder.append("_");
        stringBuilder.append(primary);
        if (protoStore.load(stringBuilder.toString(), output)) {
            return SmartSpaceCard.fromWrapper(this.mContext, output, primary);
        }
        return null;
    }

    public void onNewCard(NewCardInfo card) {
        StringBuilder stringBuilder;
        if (DEBUG) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("onNewCard: ");
            stringBuilder.append(card);
            Log.d("SmartSpaceController", stringBuilder.toString());
        }
        if (card != null) {
            if (card.getUserId() != this.mCurrentUserId) {
                if (DEBUG) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Ignore card that belongs to another user target: ");
                    stringBuilder.append(this.mCurrentUserId);
                    stringBuilder.append(" current: ");
                    stringBuilder.append(this.mCurrentUserId);
                    Log.d("SmartSpaceController", stringBuilder.toString());
                }
                return;
            }
            final class SmartSpaceControllerNewCard implements Runnable {
                private final SmartSpaceController smartSpaceController;
                private final NewCardInfo card;

                public SmartSpaceControllerNewCard(SmartSpaceController ssc, NewCardInfo newCardInfo) {
                    smartSpaceController = ssc;;
                    card = newCardInfo;
                }

                public final void run() {
                    CardWrapper message = card.toWrapper(smartSpaceController.mContext);
                    ProtoStore protoStore = smartSpaceController.mStore;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("smartspace_");
                    stringBuilder.append(smartSpaceController.mCurrentUserId);
                    stringBuilder.append("_");
                    stringBuilder.append(card.isPrimary());
                    protoStore.store(message, stringBuilder.toString());
                    smartSpaceController.mUiHandler.post(new Runnable() {
                        public final void run() {
                            handleNewCard(smartSpaceController, card, card.shouldDiscard() ? null : SmartSpaceCard.fromWrapper(smartSpaceController.mContext, message, card.isPrimary()));
                        }

                        public void handleNewCard(SmartSpaceController smartSpaceController, NewCardInfo card, SmartSpaceCard smartSpaceCard) {
                            if (card.isPrimary()) {
                                smartSpaceController.mData.mCurrentCard = smartSpaceCard;
                            } else {
                                smartSpaceController.mData.mWeatherCard = smartSpaceCard;
                            }
                            smartSpaceController.mData.handleExpire();
                            smartSpaceController.update();
                        }
                    });
                }
            }
            this.mBackgroundHandler.post(new SmartSpaceControllerNewCard(this, card));
        }
    }

    private void update() {
        Assert.isMainThread();
        if (DEBUG) {
            Log.d("SmartSpaceController", "update");
        }
        if (this.mAlarmRegistered) {
            this.mAlarmManager.cancel(this.mExpireAlarmAction);
            this.mAlarmRegistered = false;
        }
        long expiresMillis = this.mData.getExpiresAtMillis();
        if (expiresMillis > 0) {
            this.mAlarmManager.set(0, expiresMillis, "SmartSpace", this.mExpireAlarmAction, this.mUiHandler);
            this.mAlarmRegistered = true;
        }
        if (this.mListener != null) {
            if (DEBUG) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("notify listener data=");
                stringBuilder.append(this.mData);
                Log.d("SmartSpaceController", stringBuilder.toString());
            }
            this.mListener.onSmartSpaceUpdated(this.mData);
        }
    }

    private void onExpire(boolean forceExpire) {
        Assert.isMainThread();
        this.mAlarmRegistered = false;
        if (this.mData.handleExpire() || forceExpire) {
            update();
            if (UserHandle.myUserId() == 0) {
                if (DEBUG) {
                    Log.d("SmartSpaceController", "onExpire - sent");
                }
                this.mAppContext.sendBroadcast(new Intent("com.google.android.systemui.smartspace.EXPIRE_EVENT").setPackage("com.google.android.googlequicksearchbox").addFlags(268435456));
            }
        } else if (DEBUG) {
            Log.d("SmartSpaceController", "onExpire - cancelled");
        }
    }

    public void setListener(SmartSpaceUpdateListener listener) {
        Assert.isMainThread();
        this.mListener = listener;
        if (this.mData != null && this.mListener != null) {
            this.mListener.onSmartSpaceUpdated(this.mData);
        }
    }

    public void setHideSensitiveData(boolean hidePrivateData) {
        this.mListener.onSensitiveModeChanged(hidePrivateData);
    }

    private void onGsaChanged() {
        if (DEBUG) {
            Log.d("SmartSpaceController", "onGsaChanged");
        }
        if (UserHandle.myUserId() == 0) {
            this.mAppContext.sendBroadcast(new Intent("com.google.android.systemui.smartspace.ENABLE_UPDATE").setPackage("com.google.android.googlequicksearchbox").addFlags(268435456));
            this.mSmartSpaceEnabledBroadcastSent = true;
        }
        if (this.mListener != null) {
            this.mListener.onGsaChanged();
        }
    }

    public void reloadData() {
        this.mData.mCurrentCard = loadSmartSpaceData(true);
        this.mData.mWeatherCard = loadSmartSpaceData(false);
        update();
    }

    private boolean isSmartSpaceDisabledByExperiments() {
        boolean smartSpaceEnabled = true;
        String value = Global.getString(this.mContext.getContentResolver(), "always_on_display_constants");
        KeyValueListParser parser = new KeyValueListParser(',');
        try {
            parser.setString(value);
            smartSpaceEnabled = parser.getBoolean("smart_space_enabled", true);
        } catch (IllegalArgumentException e) {
            Log.e("SmartSpaceController", "Bad AOD constants");
        }
        if (smartSpaceEnabled) {
            return false;
        }
        return true;
    }

    public void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        writer.println();
        writer.println("SmartspaceController");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("  initial broadcast: ");
        stringBuilder.append(this.mSmartSpaceEnabledBroadcastSent);
        writer.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  weather ");
        stringBuilder.append(this.mData.mWeatherCard);
        writer.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  current ");
        stringBuilder.append(this.mData.mCurrentCard);
        writer.println(stringBuilder.toString());
        writer.println("serialized:");
        stringBuilder = new StringBuilder();
        stringBuilder.append("  weather ");
        stringBuilder.append(loadSmartSpaceData(false));
        writer.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("  current ");
        stringBuilder.append(loadSmartSpaceData(true));
        writer.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("disabled by experiment: ");
        stringBuilder.append(isSmartSpaceDisabledByExperiments());
        writer.println(stringBuilder.toString());
    }
}
