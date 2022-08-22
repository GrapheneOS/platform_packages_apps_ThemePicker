package com.android.customization.picker.monet;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Switch;

import androidx.annotation.Nullable;

import com.android.customization.model.monet.MonetModeSectionController;
import com.android.wallpaper.R;
import com.android.wallpaper.picker.SectionView;

public final class MonetModeSectionView extends SectionView {

    private boolean mIsMonetEnabled;
    private Switch switchView;

    public MonetModeSectionView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setTitle(context.getString(R.string.mode_title));
        mIsMonetEnabled = MonetModeSectionController.isMonetEnabled(context);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        switchView = findViewById(R.id.monet_mode_toggle);
        setChecked(mIsMonetEnabled);
        switchView.setOnCheckedChangeListener((buttonView, isChecked) ->
                switchView.setChecked(mIsMonetEnabled)
        );
        setOnClickListener(view -> modeToggleClicked());
    }

    private void modeToggleClicked() {
        mIsMonetEnabled = !mIsMonetEnabled;
        viewActivated(mIsMonetEnabled);
    }

    private void viewActivated(boolean isChecked) {
        if (mSectionViewListener != null) {
            mSectionViewListener.onViewActivated(getContext(), isChecked);
        }
    }

    public void setChecked(boolean isChecked) {
        if (switchView != null){
            switchView.setChecked(isChecked);
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        final int numOfChildViews = getChildCount();
        for (int i = 0; i < numOfChildViews; i++) {
            getChildAt(i).setEnabled(enabled);
        }
    }
}
