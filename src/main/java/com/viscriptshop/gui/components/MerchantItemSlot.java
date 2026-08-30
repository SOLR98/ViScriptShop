package com.viscriptshop.gui.components;

import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;

/**
 * 支持自定义悬浮提示的 ItemSlot。
 *
 * <p>LDLib2 的 {@code addEventListener} 将新监听器插入头部,而 ItemSlot 构造时注册的
 * {@code onHoverTooltips} 会最后执行并覆盖外部挂载的悬浮提示。本类覆写
 * {@link #onHoverTooltips(UIEvent)}:优先使用外部设置的 {@link HoverTooltips}。
 */
public class MerchantItemSlot extends ItemSlot {
    private HoverTooltips tooltipOverride;

    public MerchantItemSlot setTooltipOverride(HoverTooltips tooltipOverride) {
        this.tooltipOverride = tooltipOverride;
        return this;
    }

    @Override
    protected void onHoverTooltips(UIEvent event) {
        if (tooltipOverride != null) {
            event.hoverTooltips = tooltipOverride;
            return;
        }
        super.onHoverTooltips(event);
    }
}
