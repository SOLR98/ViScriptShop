package com.viscriptshop.uitest;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;
import com.viscriptshop.Config;
import com.viscriptshop.ViscriptShop;
import com.viscriptshop.gui.ShopUI;
import com.viscriptshop.gui.data.CategoryInfo;
import com.viscriptshop.gui.data.MerchantInfo;
import com.viscriptshop.gui.data.ShopInfo;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

@LDLRegisterClient(
        name = "shop_glass_theme",
        group = ViscriptShop.MOD_ID,
        registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY
)
public final class ShopThemeScenario implements UIScenario {
    private static final String PREVIOUS_THEME = "previous_shop_theme";

    @Override
    public void configure(ScenarioOptions options) {
        options.guiScale(3).tags("ui", "theme", "fast");
    }

    @Override
    public void define(ScenarioBuilder scenario) {
        scenario.teardown("restore theme and close screen", context -> {
            Config.ShopUiTheme previous = context.get(PREVIOUS_THEME);
            if (previous != null) {
                Config.shopUiTheme.set(previous);
            }
            context.mc().setScreen(null);
        });

        scenario.step("select dark glass theme", context -> {
                    context.put(PREVIOUS_THEME, Config.shopUiTheme.get());
                    Config.shopUiTheme.set(Config.ShopUiTheme.GLASS_DARK);
                })
                .openModularUI("production shop UI", context -> {
                    ShopUI shopUi = new ShopUI("uitest/theme", createShop(), "Theme Test Shop");
                    return new ModularUI(UI.of(shopUi, ShopUI::getAutoGuiScaledSize));
                })
                .awaitModularUI()
                .awaitElement("#shop_ui_shell")
                .checkClass("#shop_ui_shell", "shop-theme-glass-dark")
                .checkVisible("#shop_ui_categories")
                .checkVisible("#shop_ui_merchants")
                .checkVisible("#shop_ui_summary")
                .screenshot("shop_glass_dark")
                .screenshotElement("shop_glass_dark_shell", "#shop_ui_shell")
                .closeScreen();
    }

    static ShopInfo createShop() {
        ShopInfo shop = new ShopInfo();
        shop.setName("Theme Test Shop");

        CategoryInfo seeds = new CategoryInfo();
        seeds.setName("Seeds");
        seeds.setShopType(CategoryInfo.ShopType.CURRENCY);
        seeds.setIconItem(new ItemStack(Items.WHEAT_SEEDS));
        seeds.getMerchants().add(merchant(Items.WHEAT_SEEDS, 1));
        seeds.getMerchants().add(merchant(Items.BEETROOT_SEEDS, 2));
        seeds.getMerchants().add(merchant(Items.PUMPKIN_SEEDS, 3));
        seeds.getMerchants().add(merchant(Items.MELON_SEEDS, 4));

        CategoryInfo supplies = new CategoryInfo();
        supplies.setName("Supplies");
        supplies.setShopType(CategoryInfo.ShopType.CURRENCY);
        supplies.setIconItem(new ItemStack(Items.BREAD));
        supplies.getMerchants().add(merchant(Items.BREAD, 5));

        shop.getCategoryInfos().add(seeds);
        shop.getCategoryInfos().add(supplies);
        for (int i = 1; i <= 8; i++) {
            CategoryInfo category = new CategoryInfo();
            category.setName("Category " + i);
            category.setShopType(CategoryInfo.ShopType.CURRENCY);
            category.setIconItem(new ItemStack(Items.CHEST));
            shop.getCategoryInfos().add(category);
        }
        return shop;
    }

    private static MerchantInfo merchant(net.minecraft.world.item.Item item, int price) {
        MerchantInfo merchant = new MerchantInfo();
        merchant.setItemResult(new ItemStack(item));
        merchant.setTradeType(MerchantInfo.TradeType.BUY);
        merchant.setMoney(price);
        return merchant;
    }
}
