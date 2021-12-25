package com.arrow.android.systemui.dagger;

import com.android.systemui.dagger.DefaultComponentBinder;
import com.android.systemui.dagger.DependencyProvider;
import com.android.systemui.dagger.SysUISingleton;
import com.android.systemui.dagger.SystemUIBinder;
import com.android.systemui.dagger.SysUIComponent;
import com.android.systemui.dagger.SystemUIModule;

import com.arrow.android.systemui.gamedashboard.GameDashboardModule;
import com.arrow.android.systemui.keyguard.KeyguardSliceProviderArrow;
import com.arrow.android.systemui.smartspace.KeyguardSmartspaceController;

import dagger.Subcomponent;

@SysUISingleton
@Subcomponent(modules = {
        DefaultComponentBinder.class,
        DependencyProvider.class,
        GameDashboardModule.class,
        SystemUIModule.class,
        SystemUIArrowBinder.class,
        SystemUIArrowModule.class})
public interface SysUIComponentArrow extends SysUIComponent {
    @SysUISingleton
    @Subcomponent.Builder
    interface Builder extends SysUIComponent.Builder {
        SysUIComponentArrow build();
    }

    /**
     * Member injection into the supplied argument.
     */
    void inject(KeyguardSliceProviderArrow keyguardSliceProviderArrow);

    @SysUISingleton
    KeyguardSmartspaceController createKeyguardSmartspaceController();
}
