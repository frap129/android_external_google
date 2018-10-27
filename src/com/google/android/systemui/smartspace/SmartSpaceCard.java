package com.google.android.systemui.smartspace;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;
import android.util.Log;
import com.android.systemui.R;
import com.android.systemui.smartspace.nano.SmartspaceProto.CardWrapper;
import com.android.systemui.smartspace.nano.SmartspaceProto.SmartspaceUpdate.SmartspaceCard;
import com.android.systemui.smartspace.nano.SmartspaceProto.SmartspaceUpdate.SmartspaceCard.Message;
import com.android.systemui.smartspace.nano.SmartspaceProto.SmartspaceUpdate.SmartspaceCard.Message.FormattedText;
import com.android.systemui.smartspace.nano.SmartspaceProto.SmartspaceUpdate.SmartspaceCard.Message.FormattedText.FormatParam;

public class SmartSpaceCard {
    private final SmartspaceCard mCard;
    private final Context mContext;
    private Bitmap mIcon;
    private boolean mIconProcessed;
    private final Intent mIntent;
    private final boolean mIsIconGrayscale;
    private final boolean mIsWeather;
    private final long mPublishTime;

    public SmartSpaceCard(Context context, SmartspaceCard card, Intent intent, boolean isWeather, Bitmap icon, boolean isIconGrayscale, long publishTime) {
        this.mContext = context.getApplicationContext();
        this.mCard = card;
        this.mIsWeather = isWeather;
        this.mIntent = intent;
        this.mIcon = icon;
        this.mPublishTime = publishTime;
        this.mIsIconGrayscale = isIconGrayscale;
    }

    public Bitmap getIcon() {
        return this.mIcon;
    }

    public void setIcon(Bitmap icon) {
        this.mIcon = icon;
    }

    public void setIconProcessed(boolean iconProcessed) {
        this.mIconProcessed = iconProcessed;
    }

    public boolean isIconProcessed() {
        return this.mIconProcessed;
    }

    public String getTitle() {
        return substitute(true);
    }

    public String getSubtitle() {
        return substitute(false);
    }

    private Message getMessage() {
        long now = System.currentTimeMillis();
        long eventEnd = this.mCard.eventTimeMillis + this.mCard.eventDurationMillis;
        if (now < this.mCard.eventTimeMillis && this.mCard.preEvent != null) {
            return this.mCard.preEvent;
        }
        if (now > eventEnd && this.mCard.postEvent != null) {
            return this.mCard.postEvent;
        }
        if (this.mCard.duringEvent != null) {
            return this.mCard.duringEvent;
        }
        return null;
    }

    private FormattedText getFormattedText(boolean forTitle) {
        Message msg = getMessage();
        if (msg == null) {
            return null;
        }
        return forTitle ? msg.title : msg.subtitle;
    }

    private boolean hasParams(FormattedText text) {
        return (text == null || text.text == null || text.formatParam == null || text.formatParam.length <= 0) ? false : true;
    }

    long getMillisToEvent(FormatParam formatParam) {
        long event;
        if (formatParam.formatParamArgs == 2) {
            event = this.mCard.eventTimeMillis + this.mCard.eventDurationMillis;
        } else {
            event = this.mCard.eventTimeMillis;
        }
        return Math.abs(System.currentTimeMillis() - event);
    }

    private int getMinutesToEvent(FormatParam formatParam) {
        return (int) Math.ceil(((double) getMillisToEvent(formatParam)) / 60000.0d);
    }

    private String substitute(boolean forTitle) {
        return substitute(forTitle, null);
    }

    private String[] getTextArgs(FormatParam[] params, String truncateableReplacement) {
        String[] args = new String[params.length];
        int i = 0;
        while (i < args.length) {
            switch (params[i].formatParamArgs) {
                case 1:
                case 2:
                    args[i] = getDurationText(params[i]);
                    break;
                case 3:
                    if (truncateableReplacement != null && params[i].truncateLocation != 0) {
                        args[i] = truncateableReplacement;
                        break;
                    }
                    args[i] = params[i].text != null ? params[i].text : "";
                    break;
                default:
                    args[i] = "";
                    break;
            }
            i++;
        }
        return args;
    }

    private String getDurationText(FormatParam param) {
        int mins = getMinutesToEvent(param);
        if (mins >= 60) {
            int hrs = mins / 60;
            int min = mins % 60;
            String hoursText = this.mContext.getResources().getQuantityString(R.plurals.smartspace_hours, hrs, new Object[]{Integer.valueOf(hrs)});
            if (min <= 0) {
                return hoursText;
            }
            String minsText = this.mContext.getResources().getQuantityString(R.plurals.smartspace_minutes, min, new Object[]{Integer.valueOf(min)});
            return this.mContext.getString(R.string.smartspace_hours_mins, new Object[]{hoursText, minsText});
        }
        return this.mContext.getResources().getQuantityString(R.plurals.smartspace_minutes, mins, new Object[]{Integer.valueOf(mins)});
    }

    private String substitute(boolean forTitle, String truncateableReplacement) {
        FormattedText text = getFormattedText(forTitle);
        if (text == null || text.text == null) {
            return "";
        }
        String plain = text.text;
        if (hasParams(text)) {
            return String.format(plain, (Object[]) getTextArgs(text.formatParam, truncateableReplacement));
        }
        return plain;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > getExpiration();
    }

    public long getExpiration() {
        if (this.mCard == null || this.mCard.expiryCriteria == null) {
            return 0;
        }
        return this.mCard.expiryCriteria.expirationTimeMillis;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("title:");
        stringBuilder.append(getTitle());
        stringBuilder.append(" subtitle:");
        stringBuilder.append(getSubtitle());
        stringBuilder.append(" expires:");
        stringBuilder.append(getExpiration());
        stringBuilder.append(" published:");
        stringBuilder.append(this.mPublishTime);
        return stringBuilder.toString();
    }

    static SmartSpaceCard fromWrapper(Context context, CardWrapper proto, boolean isWeather) {
        if (proto == null) {
            return null;
        }
        try {
            Intent intent = (proto.card.tapAction == null || TextUtils.isEmpty(proto.card.tapAction.intent)) ? null : Intent.parseUri(proto.card.tapAction.intent, 0);
            return new SmartSpaceCard(context, proto.card, intent, isWeather, proto.icon != null ? BitmapFactory.decodeByteArray(proto.icon, 0, proto.icon.length, null) : null, proto.isIconGrayscale, proto.publishTime);
        } catch (Exception exc) {
            Log.e("SmartspaceCard", "from proto", exc);
            return null;
        }
    }
}
