package com.android.customization.model.monet;

import static android.provider.Settings.Secure.MONET_MODE_DISABLED;
import static android.provider.Settings.Secure.MONET_MODE_ENABLED;

import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.widget.Switch;

import androidx.annotation.MainThread;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

import com.android.customization.picker.monet.MonetModeSectionView;
import com.android.wallpaper.R;
import com.android.wallpaper.model.CustomizationSectionController;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MonetModeSectionController implements
        CustomizationSectionController<MonetModeSectionView>, LifecycleObserver {

    private static final ExecutorService sExecutorService = Executors.newSingleThreadExecutor();
    private final Lifecycle mLifecycle;
    private Context mContext;
    private MonetModeSectionView mMonetModeSectionView;

    private final ContentObserver mContentObserver = new ContentObserver(
            new Handler(Looper.getMainLooper())) {
        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            mMonetModeSectionView.setChecked(isMonetEnabled(mContext));
        }
    };

    public MonetModeSectionController(Context context, Lifecycle lifecycle) {
        mContext = context;
        mLifecycle = lifecycle;
        mLifecycle.addObserver(this);
    }


    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    @MainThread
    public void onStart() {
        sExecutorService.submit(() -> {
            if (mContext != null && mLifecycle.getCurrentState().isAtLeast(
                    Lifecycle.State.STARTED)) {

                mContext.getContentResolver().registerContentObserver(
                        Settings.Secure.getUriFor(Settings.Secure.MONET_MODE), false,
                        mContentObserver);
            }
        });
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    @MainThread
    public void onStop() {
        sExecutorService.submit(() -> {
            mContext.getContentResolver().unregisterContentObserver(mContentObserver);
        });
    }

    @Override
    public void release() {
        mLifecycle.removeObserver(this);
        mContext = null;
    }

    @Override
    public boolean isAvailable(Context context) {
        return context != null;
    }

    @Override
    public MonetModeSectionView createView(Context context) {
        mMonetModeSectionView = (MonetModeSectionView) LayoutInflater.from(context).inflate(
                R.layout.monet_mode_section_view, /* root= */ null);
        mMonetModeSectionView.setViewListener(this::onViewActivated);
        mMonetModeSectionView.setEnabled(isAvailable(context));
        return mMonetModeSectionView;
    }

    private void onViewActivated(Context context, boolean viewActivated) {
        if (context == null) {
            return;
        }
        int shortDelay = context.getResources().getInteger(android.R.integer.config_shortAnimTime);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    mMonetModeSectionView.announceForAccessibility(
                            context.getString(R.string.mode_changed));
                    setMonetEnabled(viewActivated, context);
                },
                /* delayMillis= */ shortDelay);
    }

    public static boolean isMonetEnabled(Context context) {
        return Settings.Secure.getIntForUser(
                context.getContentResolver(),
                Settings.Secure.MONET_MODE,
                MONET_MODE_DISABLED,
                context.getUserId()
        ) == MONET_MODE_ENABLED;
    }

    public static void setMonetEnabled(boolean isEnabled, Context context) {
        Settings.Secure.putIntForUser(context.getContentResolver(),
                Settings.Secure.MONET_MODE,
                isEnabled ? MONET_MODE_ENABLED : MONET_MODE_DISABLED,
                context.getUserId()
        );
    }


}
