package com.google.android.systemui.keyguard;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BlurMaskFilter;
import android.graphics.BlurMaskFilter.Blur;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;
import androidx.slice.Slice;
import androidx.slice.builders.ListBuilder;
import androidx.slice.builders.ListBuilder.HeaderBuilder;
import androidx.slice.builders.ListBuilder.RowBuilder;
import com.android.systemui.R;
import com.android.systemui.keyguard.KeyguardSliceProvider;
import com.android.systemui.util.Assert;
import com.google.android.systemui.smartspace.SmartSpaceCard;
import com.google.android.systemui.smartspace.SmartSpaceController;
import com.google.android.systemui.smartspace.SmartSpaceData;
import com.google.android.systemui.smartspace.SmartSpaceUpdateListener;
import java.lang.ref.WeakReference;

public class KeyguardSliceProviderGoogle extends KeyguardSliceProvider implements SmartSpaceUpdateListener {
    private static final boolean DEBUG = Log.isLoggable("KeyguardSliceProvider", 3);
    private boolean mHideSensitiveContent;
    private final Object mLock = new Object();
    private SmartSpaceData mSmartSpaceData;
    private final Uri mSmartSpaceMainUri = Uri.parse("content://com.android.systemui.keyguard/smartSpace/main");
    private final Uri mSmartSpaceSecondaryUri = Uri.parse("content://com.android.systemui.keyguard/smartSpace/secondary");
    private final Uri mWeatherUri = Uri.parse("content://com.android.systemui.keyguard/smartSpace/weather");

    private static class AddShadowTask extends AsyncTask<Bitmap, Void, Bitmap> {
        private final float mBlurRadius;
        private final WeakReference<KeyguardSliceProviderGoogle> mProviderReference;
        private final SmartSpaceCard mWeatherCard;

        AddShadowTask(KeyguardSliceProviderGoogle provider, SmartSpaceCard weatherCard) {
            this.mProviderReference = new WeakReference(provider);
            this.mWeatherCard = weatherCard;
            this.mBlurRadius = provider.getContext().getResources().getDimension(R.dimen.smartspace_icon_shadow);
        }

        protected Bitmap doInBackground(Bitmap... bitmaps) {
            return applyShadow(bitmaps[0]);
        }

        protected void onPostExecute(Bitmap bitmap) {
            this.mWeatherCard.setIcon(bitmap);
            KeyguardSliceProviderGoogle provider = (KeyguardSliceProviderGoogle) this.mProviderReference.get();
            if (provider != null) {
                provider.notifyChange();
            }
        }

        private Bitmap applyShadow(Bitmap icon) {
            BlurMaskFilter blurMask = new BlurMaskFilter(this.mBlurRadius, Blur.NORMAL);
            Paint blurPaint = new Paint();
            blurPaint.setMaskFilter(blurMask);
            int[] offset = new int[2];
            Bitmap shadow = icon.extractAlpha(blurPaint, offset);
            Bitmap target = Bitmap.createBitmap(icon.getWidth(), icon.getHeight(), Config.ARGB_8888);
            Canvas out = new Canvas(target);
            Paint drawPaint = new Paint();
            drawPaint.setAlpha(70);
            out.drawBitmap(shadow, (float) offset[0], ((float) offset[1]) + (this.mBlurRadius / 2.0f), drawPaint);
            shadow.recycle();
            drawPaint.setAlpha(255);
            out.drawBitmap(icon, 0.0f, 0.0f, drawPaint);
            return target;
        }
    }

    public boolean onCreateSliceProvider() {
        boolean created = super.onCreateSliceProvider();
        SmartSpaceController.get(getContext()).setListener(this);
        this.mSmartSpaceData = new SmartSpaceData();
        return created;
    }

    public Slice onBindSlice(Uri sliceUri) {
        boolean hideSensitiveData;
        SmartSpaceCard currentCard = this.mSmartSpaceData.getCurrentCard();
        SmartSpaceCard weatherCard = this.mSmartSpaceData.getWeatherCard();
        synchronized (this.mLock) {
            hideSensitiveData = this.mHideSensitiveContent;
        }
        ListBuilder sliceBuilder = new ListBuilder(getContext(), this.mSliceUri);
        if (isDndSuppressingNotifications() || currentCard == null || currentCard.isExpired() || TextUtils.isEmpty(currentCard.getTitle())) {
            sliceBuilder.addRow(new RowBuilder(sliceBuilder, this.mDateUri).setTitle(getFormattedDate()));
        } else if (hideSensitiveData) {
            if (DEBUG) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Not showing current card. SmartSpaceCard: ");
                stringBuilder.append(currentCard);
                stringBuilder.append(" hide sensitive data: true");
                Log.d("KeyguardSliceProvider", stringBuilder.toString());
            }
            sliceBuilder.addRow(new RowBuilder(sliceBuilder, this.mDateUri).setTitle(getFormattedDate()));
        } else {
            HeaderBuilder headerBuilder = new HeaderBuilder(sliceBuilder, this.mSmartSpaceMainUri).setTitle(currentCard.getTitle());
            RowBuilder contentBuilder = new RowBuilder(sliceBuilder, this.mSmartSpaceSecondaryUri).setTitle(currentCard.getSubtitle());
            Bitmap icon = currentCard.getIcon();
            if (icon != null) {
                contentBuilder.addEndItem(Icon.createWithBitmap(icon));
            }
            sliceBuilder.setHeader(headerBuilder).addRow(contentBuilder);
        }
        if (!(weatherCard == null || weatherCard.isExpired())) {
            RowBuilder weatherBuilder = new RowBuilder(sliceBuilder, this.mWeatherUri).setTitle(weatherCard.getTitle());
            Bitmap icon2 = weatherCard.getIcon();
            if (icon2 != null) {
                Icon weatherIcon = Icon.createWithBitmap(icon2);
                weatherIcon.setTintMode(Mode.DST);
                weatherBuilder.addEndItem(weatherIcon);
            }
            sliceBuilder.addRow(weatherBuilder);
        }
        addNextAlarm(sliceBuilder);
        addZenMode(sliceBuilder);
        addPrimaryAction(sliceBuilder);
        Slice slice = sliceBuilder.build();
        if (DEBUG) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Binding slice: ");
            stringBuilder2.append(slice);
            Log.d("KeyguardSliceProvider", stringBuilder2.toString());
        }
        return slice;
    }

    public void onSmartSpaceUpdated(SmartSpaceData smartspaceData) {
        Assert.isMainThread();
        this.mSmartSpaceData = smartspaceData;
        SmartSpaceCard weatherCard = this.mSmartSpaceData.getWeatherCard();
        if (weatherCard == null || weatherCard.getIcon() == null || weatherCard.isIconProcessed()) {
            notifyChange();
            return;
        }
        weatherCard.setIconProcessed(true);
        new AddShadowTask(this, weatherCard).execute(new Bitmap[]{weatherCard.getIcon()});
    }

    public void onGsaChanged() {
    }

    public void onSensitiveModeChanged(boolean hidePrivateData) {
        boolean changed = false;
        synchronized (this.mLock) {
            if (this.mHideSensitiveContent != hidePrivateData) {
                this.mHideSensitiveContent = hidePrivateData;
                changed = true;
                if (DEBUG) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Public mode changed, hide data: ");
                    stringBuilder.append(hidePrivateData);
                    Log.d("KeyguardSliceProvider", stringBuilder.toString());
                }
            }
        }
        if (changed) {
            notifyChange();
        }
    }

    protected void updateClock() {
        notifyChange();
    }

    void notifyChange() {
        getContext().getContentResolver().notifyChange(this.mSliceUri, null);
    }
}
