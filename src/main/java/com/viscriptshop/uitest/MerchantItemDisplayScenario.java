package com.viscriptshop.uitest;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.ScenarioOptions;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;
import com.viscriptshop.ViscriptShop;
import com.viscriptshop.gui.data.CategoryInfo;
import com.viscriptshop.gui.data.MerchantInfo;
import com.viscriptshop.gui.data.MerchantItemDisplay;
import com.viscriptshop.gui.data.Shop;
import com.viscriptshop.gui.data.ShopInfo;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

@LDLRegisterClient(
        name = "merchant_item_display_form",
        group = ViscriptShop.MOD_ID,
        registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY
)
public final class MerchantItemDisplayScenario implements UIScenario {
    private static final String MERCHANT = "merchant_item_display_merchant";

    @Override
    public void configure(ScenarioOptions options) {
        options.requiresWorld(false)
                .guiScale(3)
                .tags("ui", "form", "merchant", "fast");
    }

    @Override
    public void define(ScenarioBuilder scenario) {
        scenario.teardown("close merchant form", context -> context.mc().setScreen(null));

        scenario.step("migrate version 3 merchant items", MerchantItemDisplayScenario::checkVersionThreeMigration)
                .openModularUI("merchant item configurator", context -> {
                    MerchantInfo merchant = context.put(MERCHANT, new MerchantInfo());
                    UIElement root = new UIElement().setId("merchant_item_form_root").layout(layout -> {
                        layout.width(320);
                        layout.height(300);
                        layout.paddingAll(8);
                    });
                    ScrollerView scroller = new ScrollerView();
                    scroller.layout(layout -> {
                        layout.widthPercent(100);
                        layout.heightPercent(100);
                    });
                    scroller.addScrollViewChild(merchant.createConfigurator(CategoryInfo.ShopType.ITEM_FOR_ITEM));
                    root.addChild(scroller);
                    return new ModularUI(UI.of(root));
                })
                .awaitModularUI()
                .awaitElement("#merchant_item_form_root")
                .checkCount(".merchant-cost-item-info", 2)
                .checkCount(".merchant-result-item-info", 1)
                .checkCount(".merchant-cost-item-info .merchant-item-match-rule", 2)
                .checkCount(".merchant-result-item-info .merchant-item-match-rule", 0)
                .checkCount(".merchant-item-display-resource-path", 0)
                .checkCount(".merchant-item-display-resource-name", 0)
                .checkCount(".merchant-item-display-render-item", 0)
                .step("select resource image mode", context -> {
                    MerchantInfo merchant = context.get(MERCHANT);
                    merchant.getItemAInfo().getDisplay().setRenderMode(MerchantItemDisplay.RenderMode.RESOURCE);
                })
                .waitUntil("resource image fields are rebuilt", context ->
                        context.count(".merchant-item-display-resource-path") == 1
                                && context.count(".merchant-item-display-resource-name") == 1)
                .checkCount(".merchant-item-display-render-item", 0)
                .screenshotElement("merchant_resource_display_fields", "#merchant_item_form_root")
                .step("select substitute item mode", context -> {
                    MerchantInfo merchant = context.get(MERCHANT);
                    merchant.getItemAInfo().getDisplay().setRenderMode(MerchantItemDisplay.RenderMode.ITEM_RENDER);
                })
                .waitUntil("substitute item field is rebuilt", context ->
                        context.count(".merchant-item-display-render-item") == 1)
                .checkCount(".merchant-item-display-resource-path", 0)
                .checkCount(".merchant-item-display-resource-name", 0)
                .screenshotElement("merchant_item_render_field", "#merchant_item_form_root")
                .step("restore actual item mode", context -> {
                    MerchantInfo merchant = context.get(MERCHANT);
                    merchant.getItemAInfo().getDisplay().setRenderMode(MerchantItemDisplay.RenderMode.ITEM);
                })
                .waitUntil("mode-specific fields are removed", context ->
                        context.count(".merchant-item-display-resource-path") == 0
                                && context.count(".merchant-item-display-resource-name") == 0
                                && context.count(".merchant-item-display-render-item") == 0)
                .closeScreen();
    }

    private static void checkVersionThreeMigration(com.lowdragmc.lowdraglib2.uitest.TestContext context) {
        ShopInfo shopInfo = new ShopInfo();
        CategoryInfo category = new CategoryInfo();
        category.setShopType(CategoryInfo.ShopType.ITEM_FOR_ITEM);
        MerchantInfo merchant = new MerchantInfo();
        merchant.setItemA(new ItemStack(Items.DIAMOND));
        merchant.setItemB(new ItemStack(Items.EMERALD));
        merchant.setItemResult(new ItemStack(Items.APPLE));
        merchant.getItemADisplay().setRenderMode(MerchantItemDisplay.RenderMode.RESOURCE);
        merchant.getItemADisplay().setResourcePath("viscript_shop:textures/icons/coin.png");
        merchant.getItemADisplay().setResourceName("Legacy Coin");
        merchant.getItemResultDisplay().setRenderMode(MerchantItemDisplay.RenderMode.ITEM_RENDER);
        merchant.getItemResultDisplay().setRenderItem(new ItemStack(Items.GOLD_INGOT));
        category.getMerchants().add(merchant);
        shopInfo.getCategoryInfos().add(category);

        CompoundTag legacy = Shop.serializeRuntimeNBT(Platform.getFrozenRegistry(), shopInfo);
        CompoundTag legacyMerchant = firstMerchant(legacy);
        flattenCostItem(legacyMerchant, "itemA", "itemAMatchRule", "itemADisplay");
        flattenCostItem(legacyMerchant, "itemB", "itemBMatchRule", "itemBDisplay");
        flattenResultItem(legacyMerchant, "itemResult", "itemResultDisplay");
        legacy.putInt(Shop.VERSION_TAG, 3);

        ShopInfo migrated = Shop.deserializeRuntimeInfo(Platform.getFrozenRegistry(), legacy, true);
        MerchantInfo migratedMerchant = migrated.getCategoryInfos().getFirst().getMerchants().getFirst();
        context.check("item A survives v3 migration", migratedMerchant.getItemA().is(Items.DIAMOND));
        context.check("item B survives v3 migration", migratedMerchant.getItemB().is(Items.EMERALD));
        context.check("result item survives v3 migration", migratedMerchant.getItemResult().is(Items.APPLE));
        context.check("resource mode survives v3 migration",
                migratedMerchant.getItemADisplay().resolvedRenderMode() == MerchantItemDisplay.RenderMode.RESOURCE);
        context.check("resource path survives v3 migration",
                "viscript_shop:textures/icons/coin.png".equals(migratedMerchant.getItemADisplay().getResourcePath()));
        context.check("resource name survives v3 migration",
                "Legacy Coin".equals(migratedMerchant.getItemADisplay().getResourceName()));
        context.check("substitute item mode survives v3 migration",
                migratedMerchant.getItemResultDisplay().resolvedRenderMode() == MerchantItemDisplay.RenderMode.ITEM_RENDER);
        context.check("substitute render item survives v3 migration",
                migratedMerchant.getItemResultDisplay().resolvedRenderItem().is(Items.GOLD_INGOT));
    }

    private static CompoundTag firstMerchant(CompoundTag shopTag) {
        ListTag categories = shopTag.getList("categoryInfos", Tag.TAG_COMPOUND);
        ListTag merchants = categories.getCompound(0).getList("merchants", Tag.TAG_COMPOUND);
        return merchants.getCompound(0);
    }

    private static void flattenCostItem(CompoundTag merchant,
                                        String itemKey,
                                        String matchRuleKey,
                                        String displayKey) {
        CompoundTag itemInfo = merchant.getCompound(itemKey);
        moveIfPresent(itemInfo, "item", merchant, itemKey);
        moveIfPresent(itemInfo, "matchRule", merchant, matchRuleKey);
        moveIfPresent(itemInfo, "display", merchant, displayKey);
    }

    private static void flattenResultItem(CompoundTag merchant,
                                          String itemKey,
                                          String displayKey) {
        CompoundTag itemInfo = merchant.getCompound(itemKey);
        moveIfPresent(itemInfo, "item", merchant, itemKey);
        moveIfPresent(itemInfo, "display", merchant, displayKey);
    }

    private static void moveIfPresent(CompoundTag source,
                                      String sourceKey,
                                      CompoundTag target,
                                      String targetKey) {
        Tag value = source.get(sourceKey);
        if (value != null) {
            target.put(targetKey, value.copy());
        }
    }
}
