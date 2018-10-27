package com.google.android.systemui.smartspace;

import android.content.Context;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.content.pm.PackageInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore.Images.Media;
import android.text.TextUtils;
import android.util.Log;
import com.android.systemui.smartspace.nano.SmartspaceProto.CardWrapper;
import com.android.systemui.smartspace.nano.SmartspaceProto.SmartspaceUpdate.SmartspaceCard;
import com.android.systemui.smartspace.nano.SmartspaceProto.SmartspaceUpdate.SmartspaceCard.Image;
import java.io.ByteArrayOutputStream;

public class NewCardInfo {
    private final SmartspaceCard mCard;
    private final Intent mIntent;
    private final boolean mIsPrimary;
    private final PackageInfo mPackageInfo;
    private final long mPublishTime;

    public NewCardInfo(SmartspaceCard card, Intent intent, boolean isPrimary, long publishTime, PackageInfo packageInfo) {
        this.mCard = card;
        this.mIsPrimary = isPrimary;
        this.mIntent = intent;
        this.mPublishTime = publishTime;
        this.mPackageInfo = packageInfo;
    }

    public boolean isPrimary() {
        return this.mIsPrimary;
    }

    public Bitmap retrieveIcon(Context context) {
        Image img = this.mCard.icon;
        if (img == null) {
            return null;
        }
        Bitmap icon = (Bitmap) retrieveFromIntent(img.key, this.mIntent);
        if (icon != null) {
            return icon;
        }
        try {
            if (TextUtils.isEmpty(img.uri)) {
                if (!TextUtils.isEmpty(img.gsaResourceName)) {
                    ShortcutIconResource resInfo = new ShortcutIconResource();
                    resInfo.packageName = "com.google.android.googlequicksearchbox";
                    resInfo.resourceName = img.gsaResourceName;
                    return createIconBitmap(resInfo, context);
                }
                return null;
            }
            return Media.getBitmap(context.getContentResolver(), Uri.parse(img.uri));
        } catch (Exception e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("retrieving bitmap uri=");
            stringBuilder.append(img.uri);
            stringBuilder.append(" gsaRes=");
            stringBuilder.append(img.gsaResourceName);
            Log.e("NewCardInfo", stringBuilder.toString());
            return null;
        }
    }

    public CardWrapper toWrapper(Context context) {
        CardWrapper proto = new CardWrapper();
        Bitmap icon = retrieveIcon(context);
        if (icon != null) {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            icon.compress(CompressFormat.PNG, 100, stream);
            proto.icon = stream.toByteArray();
        }
        proto.card = this.mCard;
        proto.publishTime = this.mPublishTime;
        if (this.mPackageInfo != null) {
            proto.gsaVersionCode = this.mPackageInfo.versionCode;
            proto.gsaUpdateTime = this.mPackageInfo.lastUpdateTime;
        }
        return proto;
    }

    private static <T> T retrieveFromIntent(String bundleKey, Intent intent) {
        if (TextUtils.isEmpty(bundleKey)) {
            return null;
        }
        return intent.getParcelableExtra(bundleKey);
    }

    static Bitmap createIconBitmap(ShortcutIconResource iconRes, Context context) {
        try {
            Resources resources = context.getPackageManager().getResourcesForApplication(iconRes.packageName);
            if (resources != null) {
                return BitmapFactory.decodeResource(resources, resources.getIdentifier(iconRes.resourceName, null, null));
            }
        } catch (Exception e) {
        }
        return null;
    }

    public int getUserId() {
        return this.mIntent.getIntExtra("uid", -1);
    }

    public boolean shouldDiscard() {
        return this.mCard == null || this.mCard.shouldDiscard;
    }
}
