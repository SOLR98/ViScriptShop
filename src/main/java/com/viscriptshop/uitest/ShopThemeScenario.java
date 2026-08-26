package com.viscriptshop.uitest;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Toggle;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;
import com.viscriptshop.Config;
import com.viscriptshop.ViscriptShop;
import com.viscriptshop.gui.ShopUI;
import com.viscriptshop.gui.components.theme.ShopButton;
import com.viscriptshop.gui.data.CategoryInfo;
import com.viscriptshop.gui.data.MerchantInfo;
import com.viscriptshop.gui.data.MerchantItemDisplay;
import com.viscriptshop.gui.data.ShopInfo;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

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

        scenario.step("select gray cat workshop theme", context -> {
                    context.put(PREVIOUS_THEME, Config.shopUiTheme.get());
                    Config.shopUiTheme.set(Config.ShopUiTheme.GRAY_CAT_WORKSHOP);
                })
                .openModularUI("production gray cat shop UI", context -> {
                    ShopUI shopUi = new ShopUI("uitest/theme", createShop(), "Theme Test Shop");
                    return new ModularUI(UI.of(shopUi, ShopUI::getAutoGuiScaledSize));
                })
                .awaitModularUI()
                .awaitElement("#shop_ui_shell")
                .checkClass("#shop_ui_shell", "shop-theme-gray-cat-workshop")
                .checkVisible("#shop_ui_categories")
                .checkVisible("#shop_ui_merchants")
                .checkVisible("#shop_ui_summary")
                .checkCount(".merchant-item-display-actual", 7)
                .checkCount(".merchant-item-display-item-render", 1)
                .checkCount(".merchant-item-display-resource", 1)
                .step("gray cat search and layout toggles share a frame", context -> {
                    var searchToggle = (Toggle) context.el("#shop_search_mode_toggle").element();
                    var layoutToggle = (Toggle) context.el("#shop_currency_layout_toggle").element();
                    context.check(
                            "gray cat toggle frames match",
                            searchToggle.toggleButton.getButtonStyle().baseTexture()
                                    == layoutToggle.toggleButton.getButtonStyle().baseTexture()
                    );
                    var categoryScrollHeadTarget = context.el(
                            "#shop_ui_categories .__scroller_view_vertical-scroller__ > .__scroller_head_button__"
                    );
                    var scrollHead = (Button) categoryScrollHeadTarget.element();
                    var scrollTail = (Button) context.el(
                            "#shop_ui_categories .__scroller_view_vertical-scroller__ > .__scroller_tail_button__"
                    ).element();
                    context.check(
                            "gray cat scroll arrows use separate normal and pressed textures",
                            scrollHead.getButtonStyle().baseTexture()
                                    != scrollHead.getButtonStyle().pressedTexture()
                                    && scrollTail.getButtonStyle().baseTexture()
                                    != scrollTail.getButtonStyle().pressedTexture()
                    );
                    context.check(
                            "gray cat scroll arrows keep their source dimensions",
                            Math.abs(scrollHead.getSizeWidth() - 7f) <= 0.01f
                                    && Math.abs(scrollHead.getSizeHeight() - 5f) <= 0.01f
                                    && Math.abs(scrollTail.getSizeWidth() - 7f) <= 0.01f
                                    && Math.abs(scrollTail.getSizeHeight() - 5f) <= 0.01f,
                            List.of(
                                    scrollHead.getSizeWidth(),
                                    scrollHead.getSizeHeight(),
                                    scrollTail.getSizeWidth(),
                                    scrollTail.getSizeHeight()
                            ),
                            "expected [7, 5, 7, 5]"
                    );
                    var categoryScrollerBounds = context.el(
                            "#shop_ui_categories .__scroller_view_vertical-scroller__"
                    ).bounds();
                    var categoryScrollerContainerBounds = context.el(
                            "#shop_ui_categories .__scroller_view_vertical-container__"
                    ).bounds();
                    var categoryScrollHeadBounds = categoryScrollHeadTarget.bounds();
                    var selectedCategoryBounds = context.el(".shop-category-selected").bounds();
                    context.check(
                            "gray cat category scroller clears the selected cat detail",
                            categoryScrollerBounds.x() - selectedCategoryBounds.right()
                                    >= categoryScrollerBounds.width() - 0.1f,
                            selectedCategoryBounds,
                            categoryScrollerBounds
                    );
                    context.check(
                            "gray cat category scroll arrow clears the top frame",
                            categoryScrollerBounds.y() - categoryScrollerContainerBounds.y()
                                    >= categoryScrollHeadBounds.height() * 0.75f,
                            categoryScrollerContainerBounds,
                            categoryScrollerBounds
                    );
                    var merchantPanelBounds = context.el("#shop_ui_merchants").bounds();
                    var merchantScrollerBounds = context.el(
                            "#shop_ui_merchants .__scroller_view_vertical-scroller__"
                    ).bounds();
                    var merchantScrollHeadBounds = context.el(
                            "#shop_ui_merchants .__scroller_view_vertical-scroller__ > .__scroller_head_button__"
                    ).bounds();
                    context.check(
                            "gray cat merchant scroller stays inside its panel frame",
                            merchantScrollerBounds.y() - merchantPanelBounds.y()
                                    >= merchantScrollHeadBounds.height() * 0.75f
                                    && merchantPanelBounds.bottom() - merchantScrollerBounds.bottom() > 1f,
                            merchantPanelBounds,
                            merchantScrollerBounds
                    );
                    var buyButton = (ShopButton) context.el("#shop_buy_button").element();
                    context.check(
                            "gray cat buy hover texture is distinct",
                            buyButton.getButtonStyle().baseTexture()
                                    != buyButton.getButtonStyle().hoverTexture()
                    );
                    context.check(
                            "gray cat buy pressed texture is distinct",
                            buyButton.getButtonStyle().baseTexture()
                                    != buyButton.getButtonStyle().pressedTexture()
                    );
                    var consumptionBounds = context.el("#shop_consumption_panel").bounds();
                    var stashBounds = context.el("#shop_stash_button").bounds();
                    context.check(
                            "gray cat summary buttons leave a gap below consumption",
                            stashBounds.y() - consumptionBounds.bottom() > 1f,
                            consumptionBounds,
                            stashBounds
                    );
                    var categoriesBounds = context.el("#shop_ui_categories").bounds();
                    var summaryBounds = context.el("#shop_ui_summary").bounds();
                    var buyBounds = context.el("#shop_buy_button").bounds();
                    context.check(
                            "gray cat buy button aligns with category column bottom",
                            Math.abs(buyBounds.bottom() - categoriesBounds.bottom()) <= 1f,
                            categoriesBounds.bottom(),
                            buyBounds.bottom()
                    );
                    context.check(
                            "gray cat buy button aligns with summary column bottom",
                            Math.abs(buyBounds.bottom() - summaryBounds.bottom()) <= 1f,
                            summaryBounds.bottom(),
                            buyBounds.bottom()
                    );
                })
                .screenshot("shop_gray_cat_workshop")
                .screenshotElement("shop_gray_cat_workshop_shell", "#shop_ui_shell")
                .checkNotExists(".shop-merchant-grid-card")
                .click("#shop_currency_layout_toggle")
                .awaitElement("#shop_merchant_grid_0")
                .checkCount(".shop-merchant-grid-card", 9)
                .step("gray cat grid cards use the dedicated texture ratio", context -> {
                    var target = context.el("#shop_merchant_grid_0");
                    var card = target.element();
                    var bounds = target.bounds();
                    context.check(
                            "gray cat grid card logical size is 47 by 76",
                            Math.abs(card.getSizeWidth() - 47f) <= 0.01f
                                    && Math.abs(card.getSizeHeight() - 76f) <= 0.01f,
                            List.of(card.getSizeWidth(), card.getSizeHeight()),
                            "expected [47, 76]"
                    );
                    context.check(
                            "gray cat grid card keeps the 47 to 76 render ratio",
                            Math.abs(bounds.width() / bounds.height() - 47f / 76f) <= 0.001f,
                            bounds,
                            "expected ratio " + 47f / 76f
                    );
                    var countBounds = context.el("#shop_merchant_grid_count_0").bounds();
                    context.check(
                            "gray cat grid count field clears the card bottom frame",
                            bounds.bottom() - countBounds.bottom() > 2f,
                            bounds,
                            countBounds
                    );
                })
                .screenshot("shop_gray_cat_grid")
                .click("#shop_currency_layout_toggle")
                .checkNotExists(".shop-merchant-grid-card")
                .click("#shop_item_search")
                .type("e")
                .waitUntil("gray cat search candidates are visible", context ->
                        context.count(".shop-item-search-candidate") > 0)
                .step("gray cat search dropdown matches input width", context -> {
                    float inputWidth = context.el("#shop_item_search").bounds().width();
                    float dropdownWidth = context.el(".__search-component_dialog__").bounds().width();
                    context.check(
                            "gray cat dropdown width matches search input",
                            Math.abs(inputWidth - dropdownWidth) <= 1f,
                            inputWidth,
                            dropdownWidth
                    );
                })
                .screenshot("shop_gray_cat_search_dropdown")
                .blur()
                .checkNotExists(".__search-component_dialog__")
                .hover("#shop_buy_button")
                .checkHovered("#shop_buy_button")
                .screenshotElement("shop_gray_cat_buy_hover", "#shop_buy_button")
                .closeScreen()
                .step("select dark glass theme", context -> {
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
                .checkCount(".merchant-item-display-actual", 7)
                .checkCount(".merchant-item-display-item-render", 1)
                .checkCount(".merchant-item-display-resource", 1)
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
        MerchantInfo substituteIconMerchant = merchant(Items.BEETROOT_SEEDS, 2);
        substituteIconMerchant.getItemResultDisplay().setRenderMode(MerchantItemDisplay.RenderMode.ITEM_RENDER);
        substituteIconMerchant.getItemResultDisplay().setRenderItem(new ItemStack(Items.DIAMOND));
        seeds.getMerchants().add(substituteIconMerchant);

        MerchantInfo resourceIconMerchant = merchant(Items.PUMPKIN_SEEDS, 3);
        resourceIconMerchant.getItemResultDisplay().setRenderMode(MerchantItemDisplay.RenderMode.RESOURCE);
        resourceIconMerchant.getItemResultDisplay().setResourcePath("viscript_shop:textures/icons/coin.png");
        resourceIconMerchant.getItemResultDisplay().setResourceName("Coin Icon");
        seeds.getMerchants().add(resourceIconMerchant);
        seeds.getMerchants().add(merchant(Items.MELON_SEEDS, 4));
        seeds.getMerchants().add(merchant(Items.CARROT, 5));
        seeds.getMerchants().add(merchant(Items.POTATO, 6));
        seeds.getMerchants().add(merchant(Items.APPLE, 7));
        seeds.getMerchants().add(merchant(Items.COOKIE, 8));
        seeds.getMerchants().add(merchant(Items.BREAD, 9));

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
