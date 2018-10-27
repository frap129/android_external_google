package com.google.android.systemui.smartspace;

import android.util.Log;

public class SmartSpaceData {
    SmartSpaceCard mCurrentCard;
    SmartSpaceCard mWeatherCard;

    public boolean hasWeather() {
        return this.mWeatherCard != null;
    }

    public boolean hasCurrent() {
        return this.mCurrentCard != null;
    }

    public long getExpiresAtMillis() {
        if (hasCurrent() && hasWeather()) {
            return Math.min(this.mCurrentCard.getExpiration(), this.mWeatherCard.getExpiration());
        }
        if (hasCurrent()) {
            return this.mCurrentCard.getExpiration();
        }
        if (hasWeather()) {
            return this.mWeatherCard.getExpiration();
        }
        return 0;
    }

    public void clear() {
        this.mWeatherCard = null;
        this.mCurrentCard = null;
    }

    public boolean handleExpire() {
        StringBuilder stringBuilder;
        boolean anyExpired = false;
        if (hasWeather() && this.mWeatherCard.isExpired()) {
            if (SmartSpaceController.DEBUG) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("weather expired ");
                stringBuilder.append(this.mWeatherCard.getExpiration());
                Log.d("SmartspaceData", stringBuilder.toString());
            }
            this.mWeatherCard = null;
            anyExpired = true;
        }
        if (!hasCurrent() || !this.mCurrentCard.isExpired()) {
            return anyExpired;
        }
        if (SmartSpaceController.DEBUG) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("current expired ");
            stringBuilder.append(this.mCurrentCard.getExpiration());
            Log.d("SmartspaceData", stringBuilder.toString());
        }
        this.mCurrentCard = null;
        return true;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{");
        stringBuilder.append(this.mCurrentCard);
        stringBuilder.append(",");
        stringBuilder.append(this.mWeatherCard);
        stringBuilder.append("}");
        return stringBuilder.toString();
    }

    public SmartSpaceCard getWeatherCard() {
        return this.mWeatherCard;
    }

    public SmartSpaceCard getCurrentCard() {
        return this.mCurrentCard;
    }
}
