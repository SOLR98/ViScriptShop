package com.viscriptshop.gui.components;

import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.viscriptshop.gui.data.CategoryInfo;

import javax.annotation.Nullable;

public class CategoryFloatView extends FloatView {
    @Nullable
    private CategoryInfo categoryInfo;

    public CategoryFloatView(UIElement parent, String title) {
        super(parent, title);
    }

    public CategoryFloatView(UIElement parent) {
        super(parent);
    }

    /**
     * 显示浮窗以编辑给定的 CategoryInfo 实例。
     *
     * @param info 要编辑的 CategoryInfo 实例，null 表示添加新类别。
     */
    public void showEdit(@Nullable CategoryInfo info) {
        this.categoryInfo = info;

        // 清除旧内容
        this.contentContainer.clearAllChildren();

        if (info != null) {
            ConfiguratorGroup configuratorGroup = (ConfiguratorGroup) new ConfiguratorGroup().layout(layout -> {
                layout.setWidthPercent(100);
                layout.setHeightPercent(100);
            });
            configuratorGroup.setCollapse(false);

            info.buildConfigurator(configuratorGroup);
            this.contentContainer.addChild(configuratorGroup);
            this.show();
        } else {
            this.hide();
        }
    }

    @Override
    public void hide() {
        super.hide();
        this.categoryInfo = null;
    }
}