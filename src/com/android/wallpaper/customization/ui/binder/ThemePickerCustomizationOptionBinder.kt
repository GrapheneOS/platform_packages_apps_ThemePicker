/*
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.wallpaper.customization.ui.binder

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.drawable.AdaptiveIconDrawable
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.ui.platform.ComposeView
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.android.customization.model.color.ColorOptionImpl
import com.android.customization.picker.clock.shared.ClockSize
import com.android.customization.picker.clock.ui.view.ClockConstraintLayoutHostView
import com.android.customization.picker.clock.ui.view.ClockConstraintLayoutHostView.Companion.addClockViews
import com.android.customization.picker.clock.ui.view.ClockViewFactory
import com.android.customization.picker.color.ui.binder.ColorOptionIconBinder2
import com.android.customization.picker.color.ui.view.ColorOptionIconView2
import com.android.customization.picker.color.ui.viewmodel.ColorOptionIconViewModel
import com.android.customization.picker.icon.ui.util.IconStyleViewUtil
import com.android.customization.picker.settings.ui.binder.ColorContrastSectionViewBinder2
import com.android.systemui.plugins.clocks.ClockAxisStyle
import com.android.systemui.shared.Flags
import com.android.themepicker.R
import com.android.wallpaper.config.BaseFlags
import com.android.wallpaper.customization.ui.compose.ShortcutsFloatingSheet
import com.android.wallpaper.customization.ui.util.ThemePickerCustomizationOptionUtil.ThemePickerHomeCustomizationOption
import com.android.wallpaper.customization.ui.util.ThemePickerCustomizationOptionUtil.ThemePickerLockCustomizationOption
import com.android.wallpaper.customization.ui.viewmodel.ThemePickerCustomizationOptionsData
import com.android.wallpaper.customization.ui.viewmodel.ThemePickerCustomizationOptionsViewModel
import com.android.wallpaper.picker.common.icon.ui.viewbinder.IconViewBinder
import com.android.wallpaper.picker.common.text.ui.viewbinder.TextViewBinder
import com.android.wallpaper.picker.customization.ui.binder.ColorUpdateBinder
import com.android.wallpaper.picker.customization.ui.binder.CustomizationOptionsBinder
import com.android.wallpaper.picker.customization.ui.binder.DefaultCustomizationOptionsBinder
import com.android.wallpaper.picker.customization.ui.util.CustomizationOptionUtil.CustomizationOption
import com.android.wallpaper.picker.customization.ui.util.ViewAlphaAnimator.animateToAlpha
import com.android.wallpaper.picker.customization.ui.viewmodel.ColorUpdateViewModel
import com.android.wallpaper.picker.customization.ui.viewmodel.CustomizationOptionsData
import com.android.wallpaper.picker.customization.ui.viewmodel.CustomizationOptionsViewModel
import com.android.wallpaper.picker.customization.ui.viewmodel.CustomizationPickerViewModel2
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import com.android.customization.model.CustomizationManager
import com.android.customization.model.iconshape.IconShapeManager
import com.android.customization.model.iconshape.IconShapeOption
import com.android.customization.model.theme.OverlayManagerCompat

@Singleton
class ThemePickerCustomizationOptionsBinder
@Inject
constructor(private val defaultCustomizationOptionsBinder: DefaultCustomizationOptionsBinder) :
    CustomizationOptionsBinder {

    override fun bind(
        customizationOptionsData: CustomizationOptionsData,
        view: View,
        lockScreenCustomizationOptionEntries: List<Pair<CustomizationOption, View>>,
        homeScreenCustomizationOptionEntries: List<Pair<CustomizationOption, View>>,
        customizationOptionFloatingSheetViewMap: Map<CustomizationOption, View>?,
        viewModel: CustomizationPickerViewModel2,
        colorUpdateViewModel: ColorUpdateViewModel,
        lifecycleOwner: LifecycleOwner,
        navigateToMoreLockScreenSettingsActivity: () -> Unit,
        navigateToColorContrastSettingsActivity: () -> Unit,
        navigateToLockScreenNotificationsSettingsActivity: () -> Unit,
        navigateToPackThemeActivity: (Intent) -> Unit,
        iconStyleViewUtil: IconStyleViewUtil,
    ) {
        defaultCustomizationOptionsBinder.bind(
            customizationOptionsData,
            view,
            lockScreenCustomizationOptionEntries,
            homeScreenCustomizationOptionEntries,
            customizationOptionFloatingSheetViewMap,
            viewModel,
            colorUpdateViewModel,
            lifecycleOwner,
            navigateToMoreLockScreenSettingsActivity,
            navigateToColorContrastSettingsActivity,
            navigateToLockScreenNotificationsSettingsActivity,
            navigateToPackThemeActivity,
            iconStyleViewUtil,
        )

        customizationOptionsData as ThemePickerCustomizationOptionsData

        val isComposeRefactorEnabled = BaseFlags.get().isComposeRefactorEnabled()

        val showPackEntry =
            Settings.Secure.getInt(
                view.context.contentResolver,
                Settings.Secure.PACK_THEME_FEATURE_ENABLED,
                /* def= */ 0,
            ) == 1

        val optionsViewModel =
            viewModel.customizationOptionsViewModel as ThemePickerCustomizationOptionsViewModel

        val isOnMainScreen = { optionsViewModel.selectedOption.value == null }

        val allCustomizationOptionEntries =
            lockScreenCustomizationOptionEntries + homeScreenCustomizationOptionEntries
        allCustomizationOptionEntries.forEach { (_, view) ->
            ColorUpdateBinder.bind(
                setColor = { color ->
                    DrawableCompat.setTint(DrawableCompat.wrap(view.background), color)
                },
                color = colorUpdateViewModel.colorSurfaceBright,
                shouldAnimate = isOnMainScreen,
                lifecycleOwner = lifecycleOwner,
            )
            ColorUpdateBinder.bind(
                setColor = { color ->
                    view
                        .findViewById<ViewGroup>(R.id.option_entry_icon_container)
                        ?.background
                        ?.let { DrawableCompat.setTint(DrawableCompat.wrap(it), color) }
                },
                color = colorUpdateViewModel.colorSurfaceContainerHigh,
                shouldAnimate = isOnMainScreen,
                lifecycleOwner = lifecycleOwner,
            )
            ColorUpdateBinder.bind(
                setColor = { color ->
                    view.findViewById<TextView>(R.id.option_entry_title)?.setTextColor(color)
                },
                color = colorUpdateViewModel.colorOnSurface,
                shouldAnimate = isOnMainScreen,
                lifecycleOwner = lifecycleOwner,
            )
            ColorUpdateBinder.bind(
                setColor = { color ->
                    view.findViewById<TextView>(R.id.option_entry_description)?.setTextColor(color)
                },
                color = colorUpdateViewModel.colorOnSurfaceVariant,
                shouldAnimate = isOnMainScreen,
                lifecycleOwner = lifecycleOwner,
            )
        }

        val optionClock: View =
            lockScreenCustomizationOptionEntries
                .first { it.first == ThemePickerLockCustomizationOption.CLOCK }
                .second
        val optionClockIcon: ImageView = optionClock.requireViewById(R.id.option_entry_icon)

        val isKeyguardQuickAffordanceEnabled =
            BaseFlags.get().isKeyguardQuickAffordanceEnabled(view.context)
        var optionShortcut: View? = null
        var optionShortcutDescription: TextView? = null
        var optionShortcutIcon1: ImageView? = null
        var optionShortcutIcon2: ImageView? = null
        if (isKeyguardQuickAffordanceEnabled) {
            optionShortcut =
                lockScreenCustomizationOptionEntries
                    .first { it.first == ThemePickerLockCustomizationOption.SHORTCUTS }
                    .second
            optionShortcutDescription =
                optionShortcut.requireViewById(R.id.option_entry_description)
            optionShortcutIcon1 = optionShortcut.requireViewById(R.id.option_entry_icon_1)
            optionShortcutIcon2 = optionShortcut.requireViewById(R.id.option_entry_icon_2)
        }

        val optionLockScreenNotificationsSettings: View =
            lockScreenCustomizationOptionEntries
                .first { it.first == ThemePickerLockCustomizationOption.LOCK_SCREEN_NOTIFICATIONS }
                .second
        optionLockScreenNotificationsSettings.setOnClickListener {
            navigateToLockScreenNotificationsSettingsActivity.invoke()
        }

        val optionMoreLockScreenSettings: View =
            lockScreenCustomizationOptionEntries
                .first { it.first == ThemePickerLockCustomizationOption.MORE_LOCK_SCREEN_SETTINGS }
                .second
        optionMoreLockScreenSettings.setOnClickListener {
            navigateToMoreLockScreenSettingsActivity.invoke()
        }

        var optionPackThemeIconHome: ImageView? = null
        var optionPackThemeIconLock: ImageView? = null
        var optionPackThemeHome: View? = null
        var optionPackThemeLock: View? = null
        if (BaseFlags.get().isPackThemeEnabled() && showPackEntry) {
            optionPackThemeHome =
                homeScreenCustomizationOptionEntries
                    .first { it.first == ThemePickerHomeCustomizationOption.PACK_THEME }
                    .second
            optionPackThemeIconHome = optionPackThemeHome.requireViewById(R.id.option_entry_icon)

            optionPackThemeLock =
                lockScreenCustomizationOptionEntries
                    .first { it.first == ThemePickerHomeCustomizationOption.PACK_THEME }
                    .second
            optionPackThemeIconLock = optionPackThemeLock.requireViewById(R.id.option_entry_icon)
        }

        val optionColors: View =
            homeScreenCustomizationOptionEntries
                .first { it.first == ThemePickerHomeCustomizationOption.COLORS }
                .second
        val optionColorsIcon: ColorOptionIconView2 =
            optionColors.requireViewById(R.id.option_entry_icon)

        val optionAppIcons: View? =
            if (customizationOptionsData.isIconCustomizationAvailable) {
                homeScreenCustomizationOptionEntries
                    .first { it.first == ThemePickerHomeCustomizationOption.APP_ICONS }
                    .second
            } else null
        val optionAppIconsDescription: TextView? =
            optionAppIcons?.requireViewById(R.id.option_entry_description)
        val optionAppIconsIcon: ImageView? = optionAppIcons?.requireViewById(R.id.option_entry_icon)

        var optionGrid: View? = null
        var optionGridDescription: TextView? = null
        var optionGridIcon: ImageView? = null
        if (customizationOptionsData.isGridCustomizationAvailable) {
            optionGrid =
                homeScreenCustomizationOptionEntries
                    .first { it.first == ThemePickerHomeCustomizationOption.GRID }
                    .second
            optionGridDescription = optionGrid.requireViewById(R.id.option_entry_description)
            optionGridIcon = optionGrid.requireViewById(R.id.option_entry_icon)
        }

// [START CUSTOM CODE] ----------------------------------------------------
        val shapeEntry = homeScreenCustomizationOptionEntries.firstOrNull {
            it.first.toString() == "ICON_SHAPE"
        }

        if (shapeEntry != null) {
            val optionShape = shapeEntry.second
            
            val title = optionShape.findViewById<TextView>(R.id.option_entry_title)
            val desc = optionShape.findViewById<TextView>(R.id.option_entry_description)
            val icon = optionShape.findViewById<ImageView>(R.id.option_entry_icon)

            // 1. Set static Title
            title?.text = "Icon shape" // Capitalization to match "Color contrast"

            // 2. Async Fetch for Title & Icon
            val context = view.context
            val manager = IconShapeManager.getInstance(context, OverlayManagerCompat(context))

            manager.fetchOptions(object : CustomizationManager.OptionsFetchedListener<IconShapeOption> {
                override fun onOptionsLoaded(options: MutableList<IconShapeOption>?) {
                    val activeOption = options?.firstOrNull { it.isActive(manager) }
                    
                    if (activeOption != null) {
                        desc?.text = activeOption.title
                        icon?.setImageDrawable(activeOption.shapeDrawable)
                    } else {
                        desc?.text = "Default"
                    }
                }

                override fun onError(t: Throwable?) {
                   desc?.text = "Tap to edit"
                }
            }, false)

            // 3. Click Listener - Launch Fragment
            optionShape.setOnClickListener {
                 try {
                    // Unwrap context to find the hosting FragmentActivity
                    var ctx = view.context
                    while (ctx is android.content.ContextWrapper) {
                        if (ctx is androidx.fragment.app.FragmentActivity) break
                        ctx = ctx.baseContext
                    }
                    val activity = ctx as? androidx.fragment.app.FragmentActivity

                    if (activity != null) {
                         // Instantiate the Legacy Fragment via Reflection
                         val fragmentClass = Class.forName("com.android.customization.picker.iconshape.IconShapeFragment")
                         val fragment = fragmentClass.newInstance() as androidx.fragment.app.Fragment

                         // Robust Container Search strategy
                         val res = view.resources
                         val pkg = view.context.packageName
                         val wallpaperPkg = "com.android.wallpaper"
                         
                         var containerId = res.getIdentifier("fragment_container", "id", pkg)
                         if (containerId == 0) containerId = res.getIdentifier("nav_host_fragment", "id", pkg)
                         if (containerId == 0) containerId = res.getIdentifier("fragment_container", "id", wallpaperPkg)
                         if (containerId == 0) containerId = android.R.id.content

                         // Launch
                         activity.supportFragmentManager.beginTransaction()
                            .replace(containerId, fragment)
                            .addToBackStack("IconShape")
                            .commit()
                    }
                 } catch (e: Exception) {
                    android.util.Log.e("ThemePicker", "Failed to launch IconShapeFragment", e)
                 }
            }

            // 4. Inject View into Parent Container
            val res = view.resources
            var containerId = res.getIdentifier("customization_option_container", "id", view.context.packageName)
            if (containerId == 0) containerId = res.getIdentifier("customization_option_container", "id", "com.android.wallpaper")

            var container: ViewGroup? = null
            if (containerId != 0) container = view.findViewById(containerId)
            
            // Fallback: Attempt to find container via sibling (COLORS)
            if (container == null) {
                 val colorsEntry = homeScreenCustomizationOptionEntries.firstOrNull { it.first.toString() == "COLORS" }
                 if (colorsEntry?.second?.parent != null) container = colorsEntry.second.parent as? ViewGroup
            }

            if (container != null && optionShape.parent == null) {
                 container.addView(optionShape)
            }
        }
// [END CUSTOM CODE] ------------------------------------------------------

        val optionColorContrast: View =
            homeScreenCustomizationOptionEntries
                .first { it.first == ThemePickerHomeCustomizationOption.COLOR_CONTRAST }
                .second
        optionColorContrast.setOnClickListener { navigateToColorContrastSettingsActivity.invoke() }

        ColorUpdateBinder.bind(
            setColor = { color ->
                optionClockIcon.setColorFilter(color)
                if (isKeyguardQuickAffordanceEnabled) {
                    optionShortcutIcon1?.setColorFilter(color)
                    optionShortcutIcon2?.setColorFilter(color)
                }
                if (customizationOptionsData.isGridCustomizationAvailable) {
                    optionGridIcon?.setColorFilter(color)
                }
                if (BaseFlags.get().isPackThemeEnabled()) {
                    optionPackThemeIconHome?.setColorFilter(color)
                    optionPackThemeIconLock?.setColorFilter(color)
                }
            },
            color = colorUpdateViewModel.colorOnSurfaceVariant,
            shouldAnimate = isOnMainScreen,
            lifecycleOwner = lifecycleOwner,
        )

        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    optionsViewModel.onCustomizeClockClicked.collect {
                        optionClock.setOnClickListener { _ -> it?.invoke() }
                    }
                }

                launch {
                    optionsViewModel.clockPickerViewModel.selectedClock.collect {
                        optionClockIcon.setImageDrawable(it.thumbnail)
                    }
                }

                if (isKeyguardQuickAffordanceEnabled) {
                    launch {
                        optionsViewModel.onCustomizeShortcutClicked.collect {
                            optionShortcut?.setOnClickListener { _ -> it?.invoke() }
                        }
                    }
                }

                if (isKeyguardQuickAffordanceEnabled) {
                    launch {
                        optionsViewModel.keyguardQuickAffordancePickerViewModel2.summary.collect {
                            summary ->
                            optionShortcutDescription?.let {
                                TextViewBinder.bind(view = it, viewModel = summary.description)
                            }
                            summary.icon1?.let { icon ->
                                optionShortcutIcon1?.let {
                                    IconViewBinder.bind(view = it, viewModel = icon)
                                }
                            }
                            optionShortcutIcon1?.isVisible = summary.icon1 != null

                            summary.icon2?.let { icon ->
                                optionShortcutIcon2?.let {
                                    IconViewBinder.bind(view = it, viewModel = icon)
                                }
                            }
                            optionShortcutIcon2?.isVisible = summary.icon2 != null
                        }
                    }
                }

                launch {
                    optionsViewModel.onCustomizeColorsClicked.collect {
                        optionColors.setOnClickListener { _ -> it?.invoke() }
                    }
                }

                if (customizationOptionsData.isIconCustomizationAvailable) {
                    launch {
                        optionsViewModel.onCustomizeIconsClicked.collect {
                            optionAppIcons?.setOnClickListener { _ -> it?.invoke() }
                        }
                    }

                    launch {
                        var disposableHandle: DisposableHandle? = null
                        val previewIconPackageName =
                            view.context.resources.getString(R.string.camera_package)
                        val appIconDrawable =
                            ShapeIconViewBinder.loadAppIcon(view.context, previewIconPackageName)
                        optionsViewModel.appIconPickerViewModel.summary.collect { summary ->
                            disposableHandle?.dispose()
                            summary.iconShape?.let {
                                disposableHandle =
                                    optionAppIconsIcon?.let { it1 ->
                                        // TODO (b/397782741): bind icons correctly for additional
                                        //  themes
                                        ShapeIconViewBinder.bindPreviewIcon(
                                            view = it1,
                                            appIconDrawable =
                                                appIconDrawable as? AdaptiveIconDrawable,
                                            shapeIcon = summary.iconShape,
                                            isThemed = summary.isThemed,
                                            colorUpdateViewModel = colorUpdateViewModel,
                                            shouldAnimateColor = isOnMainScreen,
                                            lifecycleOwner = lifecycleOwner,
                                        )
                                    }
                            }
                            optionAppIconsDescription?.let {
                                TextViewBinder.bind(view = it, viewModel = summary.description)
                            }
                        }
                    }
                }

                if (customizationOptionsData.isGridCustomizationAvailable) {
                    launch {
                        optionsViewModel.onCustomizeShapeGridClicked.collect {
                            optionGrid?.setOnClickListener { _ -> it?.invoke() }
                        }
                    }

                    launch {
                        optionsViewModel.gridPickerViewModel.selectedGridOption.collect { gridOption
                            ->
                            optionGridDescription?.let { TextViewBinder.bind(it, gridOption.text) }
                            gridOption.payload?.let { optionGridIcon?.setImageDrawable(it) }
                        }
                    }
                }

                launch {
                    var binding: ColorContrastSectionViewBinder2.Binding? = null
                    optionsViewModel.colorContrastSectionViewModel.contrast.collectLatest { contrast
                        ->
                        binding?.destroy()
                        binding =
                            ColorContrastSectionViewBinder2.bind(
                                view = optionColorContrast,
                                contrast = contrast,
                                colorUpdateViewModel = colorUpdateViewModel,
                                shouldAnimateColor = isOnMainScreen,
                                lifecycleOwner = lifecycleOwner,
                            )
                    }
                }

                launch {
                    var binding: ColorOptionIconBinder2.Binding? = null
                    optionsViewModel.colorPickerViewModel2.selectedColorOption.collect { colorOption
                        ->
                        (colorOption as? ColorOptionImpl)?.let {
                            binding?.destroy()
                            binding =
                                ColorOptionIconBinder2.bind(
                                    view = optionColorsIcon,
                                    viewModel =
                                        ColorOptionIconViewModel.fromColorOption(colorOption),
                                    colorUpdateViewModel = colorUpdateViewModel,
                                    shouldAnimateColor = isOnMainScreen,
                                    lifecycleOwner = lifecycleOwner,
                                )
                        }
                    }
                }

                if (BaseFlags.get().isPackThemeEnabled()) {
                    launch {
                        optionsViewModel.packThemeViewModel.packThemeData.collect { packThemeData ->
                            val homeTitle =
                                optionPackThemeHome?.findViewById<TextView>(R.id.option_entry_title)
                            val lockTitle =
                                optionPackThemeLock?.findViewById<TextView>(R.id.option_entry_title)
                            val homeDescription =
                                optionPackThemeHome?.findViewById<TextView>(
                                    R.id.option_entry_description
                                )
                            val lockDescription =
                                optionPackThemeLock?.findViewById<TextView>(
                                    R.id.option_entry_description
                                )
                            if (packThemeData.currentThemePackInfo.title.isNotEmpty()) {
                                homeTitle?.text = packThemeData.currentThemePackInfo.title
                                lockTitle?.text = packThemeData.currentThemePackInfo.title
                            }
                            if (packThemeData.currentThemePackInfo.description.isNotEmpty()) {
                                homeDescription?.text =
                                    packThemeData.currentThemePackInfo.description
                                lockDescription?.text =
                                    packThemeData.currentThemePackInfo.description
                            }
                            if (packThemeData.currentThemePackInfo.thumbnailUri.isNotEmpty()) {
                                val uri = packThemeData.currentThemePackInfo.thumbnailUri.toUri()
                                val corner =
                                    (THUMBNAIL_CORNER_RADIUS *
                                            view.context.resources.displayMetrics.density)
                                        .toInt()
                                optionPackThemeIconHome?.let {
                                    Glide.with(view.context)
                                        .load(uri)
                                        .transform(RoundedCorners(corner))
                                        .into(it)
                                    it.colorFilter = null
                                }
                                optionPackThemeIconLock?.let {
                                    Glide.with(view.context)
                                        .load(uri)
                                        .transform(RoundedCorners(corner))
                                        .into(it)
                                    it.colorFilter = null
                                }
                            }
                        }
                    }
                    launch {
                        optionsViewModel.packThemeViewModel.startThemePackActivityIntent.collect {
                            intent ->
                            if (intent != null) {
                                optionPackThemeHome?.setOnClickListener {
                                    navigateToPackThemeActivity.invoke(intent)
                                }
                                optionPackThemeLock?.setOnClickListener {
                                    navigateToPackThemeActivity.invoke(intent)
                                }
                            } else {
                                optionPackThemeHome?.setOnClickListener(null)
                                optionPackThemeLock?.setOnClickListener(null)
                            }
                        }
                    }
                }
            }
        }

        customizationOptionFloatingSheetViewMap
            ?.get(ThemePickerLockCustomizationOption.CLOCK)
            ?.let {
                ClockFloatingSheetBinder.bind(
                    it,
                    optionsViewModel,
                    colorUpdateViewModel,
                    lifecycleOwner,
                )
            }
        if (isComposeRefactorEnabled) {
            customizationOptionFloatingSheetViewMap
                ?.get(ThemePickerLockCustomizationOption.SHORTCUTS)
                ?.let {
                    // TODO(b/409112907) Evaluate Compose performance before enabling flag
                    (it as ComposeView).setContent {
                        ShortcutsFloatingSheet(
                            optionsViewModel.keyguardQuickAffordancePickerViewModel2
                        )
                    }
                }
        } else {
            customizationOptionFloatingSheetViewMap
                ?.get(ThemePickerLockCustomizationOption.SHORTCUTS)
                ?.let {
                    ShortcutFloatingSheetBinder.bind(
                        it,
                        optionsViewModel,
                        colorUpdateViewModel,
                        lifecycleOwner,
                    )
                }
        }

        if (!isComposeRefactorEnabled) {
            customizationOptionFloatingSheetViewMap
                ?.get(ThemePickerHomeCustomizationOption.COLORS)
                ?.let {
                    ColorsFloatingSheetBinder.bind(
                        it,
                        optionsViewModel,
                        colorUpdateViewModel,
                        lifecycleOwner,
                    )
                }
        }

        customizationOptionFloatingSheetViewMap
            ?.get(ThemePickerHomeCustomizationOption.APP_ICONS)
            ?.let {
                AppIconFloatingSheetBinder.bind(
                    it,
                    optionsViewModel,
                    iconStyleViewUtil,
                    colorUpdateViewModel,
                    lifecycleOwner,
                    Dispatchers.IO,
                )
            }

        customizationOptionFloatingSheetViewMap?.get(ThemePickerHomeCustomizationOption.GRID)?.let {
            GridFloatingSheetBinder.bind(
                it,
                optionsViewModel,
                colorUpdateViewModel,
                lifecycleOwner,
                Dispatchers.IO,
            )
        }
    }

    // Track the current show clock flag. If it turns from false to true, animate fade-in.
    private var isClockCurrentlyShown: Boolean? = null

    override fun bindClockPreview(
        context: Context,
        rootView: View,
        clockHostView: View,
        clockFaceClickDelegateView: View,
        viewModel: CustomizationPickerViewModel2,
        colorUpdateViewModel: ColorUpdateViewModel,
        lifecycleOwner: LifecycleOwner,
        clockViewFactory: ClockViewFactory,
    ) {
        clockHostView as ClockConstraintLayoutHostView
        val clockPickerViewModel =
            (viewModel.customizationOptionsViewModel as ThemePickerCustomizationOptionsViewModel)
                .clockPickerViewModel

        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    combine(
                            clockPickerViewModel.previewingClock,
                            clockPickerViewModel.previewingClockSize,
                            clockPickerViewModel.showPickerClockControllerView,
                            ::Triple,
                        )
                        .collect { (clock, size, showClock) ->
                            clockHostView.removeAllViews()
                            // For new customization picker, we should get views from clocklayout
                            if (Flags.newCustomizationPickerUi()) {
                                if (showClock) {
                                    clockViewFactory.getController(clock.clockId)?.run {
                                        val cs = ConstraintSet()
                                        clockHostView.addClockViews(this, size, cs)
                                        val cfg = clockPickerViewModel.buildPreviewConfig(context)
                                        largeClock.layout.applyPreviewConstraints(cfg, cs)
                                        smallClock.layout.applyPreviewConstraints(cfg, cs)
                                        cs.applyTo(clockHostView)
                                    }
                                    clockViewFactory.updateTimeFormat(clock.clockId)
                                }
                                val shouldFadeIn = (isClockCurrentlyShown == false) && showClock
                                if (shouldFadeIn) {
                                    clockHostView.alpha = 0F
                                    clockHostView.animateToAlpha(1F)
                                }
                                isClockCurrentlyShown = showClock
                            } else {
                                val clockView =
                                    when (size) {
                                        ClockSize.DYNAMIC ->
                                            clockViewFactory.getLargeView(clock.clockId)
                                        ClockSize.SMALL ->
                                            clockViewFactory.getSmallView(clock.clockId)
                                    }
                                // The clock view might still be attached to an existing parent.
                                // Detach
                                // before adding to another parent.
                                (clockView.parent as? ViewGroup)?.removeView(clockView)
                                clockHostView.addView(clockView)
                            }
                        }
                }

                launch {
                    combine(
                            clockPickerViewModel.previewingSeedColor,
                            clockPickerViewModel.previewingClock,
                            clockPickerViewModel.previewingClockPresetIndexedStyle,
                            colorUpdateViewModel.systemColorsUpdated,
                            ::Quadruple,
                        )
                        .collect { quadruple ->
                            val (color, clock, clockPresetIndexedStyle, _) = quadruple
                            clockViewFactory.updateColor(clock.clockId, color)
                            clockViewFactory.updateFontAxes(
                                clock.clockId,
                                clockPresetIndexedStyle?.style ?: ClockAxisStyle(),
                            )
                        }
                }

                launch {
                    combine(
                            viewModel.customizationOptionsViewModel.selectedOption,
                            clockPickerViewModel.onClockFaceClicked,
                            ::Pair,
                        )
                        .collect { (selectedOption, onClockFaceClicked) ->
                            if (
                                selectedOption == ThemePickerLockCustomizationOption.CLOCK &&
                                    onClockFaceClicked != null
                            ) {
                                clockFaceClickDelegateView.isVisible = true
                                clockFaceClickDelegateView.setOnClickListener {
                                    onClockFaceClicked.invoke()
                                }
                            } else {
                                clockFaceClickDelegateView.isVisible = false
                                clockFaceClickDelegateView.setOnClickListener(null)
                            }
                            clockFaceClickDelegateView.contentDescription =
                                context.getString(R.string.clock_style_round_clock)
                        }
                }

                launch {
                    clockPickerViewModel.showClockFacePresetGroupIndexUpdateToast.collect {
                        presetGroupIndex ->
                        val clockStyle: String =
                            rootView.resources.getString(
                                if (presetGroupIndex == 0) R.string.clock_style_round
                                else R.string.clock_style_sharp
                            )
                        val toastMessage: String =
                            rootView.resources.getString(
                                R.string.clock_style_update_toast,
                                clockStyle,
                            )
                        android.widget.Toast.makeText(rootView.context, toastMessage, android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun bindDiscardChangesDialog(
        customizationOptionsViewModel: CustomizationOptionsViewModel,
        lifecycleOwner: LifecycleOwner,
        activity: Activity,
    ) {
        defaultCustomizationOptionsBinder.bindDiscardChangesDialog(
            customizationOptionsViewModel,
            lifecycleOwner,
            activity,
        )
    }

    data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

    companion object {
        private const val THUMBNAIL_CORNER_RADIUS = 18
    }
}
