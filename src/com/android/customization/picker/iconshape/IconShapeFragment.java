/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.customization.picker.iconshape;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.ContentLoadingProgressBar;
import androidx.recyclerview.widget.RecyclerView;

import com.android.customization.model.CustomizationManager.Callback;
import com.android.customization.model.CustomizationManager.OptionsFetchedListener;
import com.android.customization.model.CustomizationOption;
import com.android.customization.model.iconshape.IconShapeOption;
import com.android.customization.model.iconshape.IconShapeManager;
import com.android.customization.model.theme.OverlayManagerCompat;
import com.android.customization.widget.OptionSelectorController;
import com.android.customization.widget.OptionSelectorController.CheckmarkStyle;
import com.android.themepicker.R;
import com.android.wallpaper.picker.AppbarFragment;
import com.android.wallpaper.widget.BottomActionBar;

import java.util.List;

public class IconShapeFragment extends AppbarFragment {

    private static final String TAG = "IconShapeFragment";
    
    private Button mApplyButton;
    
    // Tracks the option currently applied to the device
    private IconShapeOption mActiveOption;
    // Tracks the option currently selected in the UI
    private IconShapeOption mSelectedOption;

    public static IconShapeFragment newInstance(CharSequence title) {
        IconShapeFragment fragment = new IconShapeFragment();
        fragment.setArguments(AppbarFragment.createArguments(title));
        return fragment;
    }

    private RecyclerView mOptionsContainer;
    private OptionSelectorController<IconShapeOption> mOptionsController;
    private IconShapeManager mIconShapeManager;
    
    private ContentLoadingProgressBar mLoading;
    private ViewGroup mContent;
    private View mError;
    
    private BottomActionBar mBottomActionBar;

    private final Callback mApplyIconShapeCallback = new Callback() {
        @Override
        public void onSuccess() {
            // Success! Disable the button (Ghosted look)
            if (mApplyButton != null) {
                mApplyButton.setEnabled(false);
                // mApplyButton.setText("Apply"); // Optional reset if you changed text during apply
            }
            // Update our baseline
            mActiveOption = mSelectedOption;
        }

        @Override
        public void onError(@Nullable Throwable throwable) {
            // Error! Re-enable
            if (mApplyButton != null) {
                mApplyButton.setEnabled(true);
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(
                R.layout.fragment_icon_shape_picker, container, /* attachToRoot */ false);
        
        setUpToolbar(view);
        
        mContent = view.findViewById(R.id.content_section);
        mOptionsContainer = view.findViewById(R.id.options_container);
        mLoading = view.findViewById(R.id.loading_indicator);
        mError = view.findViewById(R.id.error_section);
        
        // [UPDATED] Find the button by the new native ID
        mApplyButton = view.findViewById(R.id.apply_button);
        mApplyButton.setOnClickListener(v -> {
            if (mSelectedOption != null) {
                applyCustomization(mSelectedOption);
            }
        });

        // Edge-to-edge padding
        view.setOnApplyWindowInsetsListener((v, windowInsets) -> {
            v.setPadding(
                    v.getPaddingLeft(),
                    windowInsets.getSystemWindowInsetTop(),
                    v.getPaddingRight(),
                    windowInsets.getSystemWindowInsetBottom());
            return windowInsets.consumeSystemWindowInsets();
        });

        mIconShapeManager = IconShapeManager.getInstance(getContext(), new OverlayManagerCompat(getContext()));
        setUpOptions(savedInstanceState);

        return view;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onBottomActionBarReady(BottomActionBar bottomActionBar) {
        super.onBottomActionBarReady(bottomActionBar);
    }

    private void applyCustomization(IconShapeOption iconPackOption) {
        if (mApplyButton != null) {
            mApplyButton.setEnabled(false); // Prevent double clicks
            // Optional: mApplyButton.setText("Applying..."); 
        }
        mIconShapeManager.apply(iconPackOption, mApplyIconShapeCallback);
    }

    private void setUpOptions(@Nullable Bundle savedInstanceState) {
        hideError();
        mLoading.show();
        mIconShapeManager.fetchOptions(new OptionsFetchedListener<IconShapeOption>() {
            @Override
            public void onOptionsLoaded(List<IconShapeOption> options) {
                mLoading.hide();
                mOptionsController = new OptionSelectorController<>(
                        mOptionsContainer, options, /* useGrid= */ false, CheckmarkStyle.CORNER);
                mOptionsController.initOptions(mIconShapeManager);
                
                mSelectedOption = getActiveOption(options);
                mActiveOption = mSelectedOption;

                mOptionsController.setSelectedOption(mSelectedOption);
                
                // Initial Load: Button visible but disabled
                onOptionSelected(mSelectedOption, false);

                mOptionsController.addListener(selectedOption -> {
                    // User Interaction: Check logic
                    onOptionSelected(selectedOption, true);
                });
            }

            @Override
            public void onError(@Nullable Throwable throwable) {
                if (throwable != null) {
                    Log.e(TAG, "Error loading iconpack options", throwable);
                }
                showError();
            }
        }, /*reload= */ true);
    }

    private IconShapeOption getActiveOption(List<IconShapeOption> options) {
        return options.stream()
                .filter(option -> option.isActive(mIconShapeManager))
                .findAny()
                .orElse(options.get(0));
    }

    private void hideError() {
        mContent.setVisibility(View.VISIBLE);
        mError.setVisibility(View.GONE);
    }

    private void showError() {
        mLoading.hide();
        mContent.setVisibility(View.GONE);
        mError.setVisibility(View.VISIBLE);
    }

    private void onOptionSelected(CustomizationOption selectedOption) {
        onOptionSelected(selectedOption, true);
    }

    private void onOptionSelected(CustomizationOption selectedOption, boolean isUserInteraction) {
        mSelectedOption = (IconShapeOption) selectedOption;
        refreshPreview();
        
        if (mApplyButton != null) {
            if (!isUserInteraction) {
                // Initial load: Always disabled
                mApplyButton.setEnabled(false);
            } else {
                // User clicked: Enable only if different from Active
                boolean hasChange = mActiveOption != null && !mSelectedOption.equals(mActiveOption);
                mApplyButton.setEnabled(hasChange);
            }
        }
    }

    private void refreshPreview() {
        mSelectedOption.bindPreview(mContent);
    }
}
