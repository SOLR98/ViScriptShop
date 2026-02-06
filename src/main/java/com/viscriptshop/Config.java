package com.viscriptshop;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    public static final ModConfigSpec CONFIG_SPEC;

    //是否打开FTB Library的按钮来允许打开商店
    public static ModConfigSpec.BooleanValue showFtbLibraryButton = null;

    //商店主题
    public static ModConfigSpec.ConfigValue<String> shopUiStyleSheet;

    static {
        ModConfigSpec.Builder CONFIG_BUILDER = new ModConfigSpec.Builder();
        CONFIG_BUILDER.push("config");
        if (ViscriptShop.isFtbLibraryLoaded()) {
            showFtbLibraryButton = CONFIG_BUILDER.define("showFtbLibraryButton", false);
        }
        shopUiStyleSheet = CONFIG_BUILDER.comment("ldlib2:lss/gdp.lss or ldlib2:lss/gdp.lss or ldlib2:lss/mc.lss")
                .define("shopUiStyleSheet", "ldlib2:lss/gdp.lss");
        CONFIG_BUILDER.pop();
        CONFIG_SPEC = CONFIG_BUILDER.build();
    }
}
