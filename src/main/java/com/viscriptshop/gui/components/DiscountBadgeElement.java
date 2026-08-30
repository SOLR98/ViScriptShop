package com.viscriptshop.gui.components;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.style.PropertyRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;

/**
 * 叠画在成本槽上的折扣徽标:右上角折率(0.4 倍,折扣绿/涨价红)。
 * 数量(折后价)由 {@link CustomCountElement} 在右下角统一渲染,两者不重叠。
 */
public class DiscountBadgeElement extends UIElement {
    private final double rate;

    public DiscountBadgeElement(double rate) {
        this.rate = rate;
        // 装饰元素不参与命中测试,避免阻挡下方物品槽的悬浮提示
        setAllowHitTest(false);
        layout(layout -> {
            layout.width(16);
            layout.height(16);
        });
    }

    @Override
    public void drawBackgroundAdditional(GUIContext guiContext) {
        if (rate == 0.0) return;
        var font = Minecraft.getInstance().font;
        float x = (float) getPositionX();
        float y = (float) getPositionY();
        float w = getSizeWidth();

        // 右上角,右对齐(贴右上,右 1px 上 1px),0.4 倍
        String text = formatRate(rate);
        float scale = 0.4f;
        float textW = font.width(text) * scale;
        drawScaled(guiContext, font, text, x + w - textW - 1f, y + 1f, scale,
                rate < 0 ? 0xFF55FF55 : 0xFFFF5555, false);
    }

    private static void drawScaled(GUIContext guiContext, Font font, String text,
                                   float px, float py, float scale, int color, boolean rightAligned) {
        if (text.isEmpty()) return;
        var graphics = guiContext.graphics;
        var pose = graphics.pose();
        pose.pushPose();
        // 与物品图标同层(-200),后画覆盖,避免被 ItemSlot 的深度遮挡
        pose.translate(px, py, -200);
        pose.scale(scale, scale, 1);
        int offsetX = rightAligned ? -font.width(text) : 0;
        graphics.drawString(font, text, offsetX, 0, color);
        pose.popPose();
    }

    public static String formatRate(double rate) {
        long percent = Math.round(rate * 100.0);
        return percent > 0 ? "+" + percent + "%" : percent + "%";
    }
}
