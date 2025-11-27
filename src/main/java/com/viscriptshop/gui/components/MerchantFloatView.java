package com.viscriptshop.gui.components;

import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.viscriptshop.gui.data.CategoryInfo;
import com.viscriptshop.gui.data.MerchantInfo;

import javax.annotation.Nullable;

public class MerchantFloatView extends FloatView {
    @Nullable
    private MerchantInfo merchant;
    @Nullable
    private CategoryInfo.ShopType shopType;

    public MerchantFloatView(UIElement parent, String title) {
        super(parent, title);
    }

    public MerchantFloatView(UIElement parent) {
        super(parent);
    }

    public void showEdit(MerchantInfo merchant, CategoryInfo.ShopType shopType) {
        this.merchant = merchant;
        this.shopType = shopType;

        // 清除旧内容
        this.contentContainer.clearAllChildren();

        if (merchant != null && shopType != null) {
            ConfiguratorGroup configurator = (ConfiguratorGroup) merchant.createConfigurator(shopType).layout(layout->{
                layout.setWidthPercent(100);
                layout.setHeightPercent(100);
            });
            configurator.setCollapse(false);
            this.contentContainer.addChild(configurator);
            this.show();
        } else {
            this.hide();
        }
    }

    @Override
    public void hide() {
        super.hide();
        this.merchant = null;
        this.shopType = null;
    }
}
