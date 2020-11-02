package com.streamamg.androidapp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;

import androidx.annotation.Nullable;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.mediarouter.app.MediaRouteActionProvider;
import androidx.mediarouter.app.MediaRouteButton;


public class ThemeableMediaRouteActionProvider extends MediaRouteActionProvider
{

    public ThemeableMediaRouteActionProvider(Context context) {
        super(context);
    }

    @Override
    public MediaRouteButton onCreateMediaRouteButton() {
        MediaRouteButton button = super.onCreateMediaRouteButton();
        colorWorkaroundForCastIcon(button);
        return button;
    }

    @Nullable
    @Override
    public MediaRouteButton getMediaRouteButton() {
        MediaRouteButton button = super.getMediaRouteButton();
        colorWorkaroundForCastIcon(button);
        return button;
    }

    private void colorWorkaroundForCastIcon(MediaRouteButton button) {
        if (button == null) return;
        @SuppressLint("RestrictedApi") Context castContext = new ContextThemeWrapper(getContext(), androidx.mediarouter.R.style.Theme_MediaRouter);

        TypedArray a = castContext.obtainStyledAttributes(null,
                androidx.mediarouter.R.styleable.MediaRouteButton, androidx.mediarouter.R.attr.mediaRouteButtonStyle, 0);
        Drawable drawable = a.getDrawable(
                androidx.mediarouter.R.styleable.MediaRouteButton_externalRouteEnabledDrawable);
        a.recycle();
        DrawableCompat.setTint(drawable, Color.WHITE);
        drawable.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
        drawable.setState(button.getDrawableState());
        button.setRemoteIndicatorDrawable(drawable);
    }
}
