package com.arrow.android.systemui;

import android.content.Context;

import com.arrow.android.systemui.dagger.DaggerGlobalRootComponentArrow;
import com.arrow.android.systemui.dagger.GlobalRootComponentArrow;

import com.android.systemui.SystemUIFactory;
import com.android.systemui.dagger.GlobalRootComponent;

public class SystemUIArrowFactory extends SystemUIFactory {
    @Override
    protected GlobalRootComponent buildGlobalRootComponent(Context context) {
        return DaggerGlobalRootComponentArrow.builder()
                .context(context)
                .build();
    }
}
