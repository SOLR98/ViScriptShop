package com.viscriptshop;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    public static final ModConfigSpec CONFIG_SPEC;

    //是否打开FTB Library的按钮来允许打开商店
    public static ModConfigSpec.BooleanValue showFtbLibraryButton = null;

    //是否使用MagicCoins的货币来替换本模组的货币
    public static ModConfigSpec.BooleanValue isReplaceMoneyToMagicCoin = null;

    static {
        ModConfigSpec.Builder CONFIG_BUILDER = new ModConfigSpec.Builder();
        CONFIG_BUILDER.push("config");
        if (ViscriptShop.isFtbLibraryLoaded()) {
            showFtbLibraryButton = CONFIG_BUILDER.define("showFtbLibraryButton", false);
        }
        if (ViscriptShop.isMagicCoinsLoaded()) {
            isReplaceMoneyToMagicCoin = CONFIG_BUILDER.define("isReplaceMoneyToMagicCoin", false);
        }
        CONFIG_BUILDER.pop();
        CONFIG_SPEC = CONFIG_BUILDER.build();
    }
}
