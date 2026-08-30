package com.viscriptshop.gui.components;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.style.PropertyRegistry;
import net.minecraft.client.Minecraft;

import java.util.function.LongSupplier;

/**
 * 自定义物品数量渲染(始终使用,替代 ItemSlot 的堆叠数字)。
 *
 * <p>位置样式提取自原版 {@code GuiGraphics.renderItemDecorations}:右下角对齐
 * (右边缘 -2,数字基线 y+9,按 16px 槽缩放)。数量为 long,显示上限为
 * {@link Long#MAX_VALUE},不受物品堆叠上限约束。数量 ≤ 1 时不渲染。
 * 默认白色文字、无阴影;可通过链式方法自定义颜色/阴影/缩放/偏移。
 * 纯展示,不参与命中测试。
 */
public class CustomCountElement extends UIElement {
    private static final String[] UNITS = {"k", "M", "B", "T"};
    private final LongSupplier countSupplier;
    private int color = 0xFFFFFFFF;
    private boolean shadow = false;
    private boolean compact = true;
    private boolean strikethrough = false;
    private float extraScale = 0.f;
    private float offsetX = 0f;
    private float offsetY = 0f;

    public CustomCountElement(LongSupplier countSupplier) {
        this.countSupplier = countSupplier;
        setAllowHitTest(false);
    }

    public CustomCountElement(long count) {
        this(() -> count);
    }

    public CustomCountElement color(int color) {
        this.color = color;
        return this;
    }

    public CustomCountElement shadow(boolean shadow) {
        this.shadow = shadow;
        return this;
    }

    /** 大数自动缩写(10000 -> 10k),默认开启;关闭则显示完整数字 */
    public CustomCountElement compact(boolean compact) {
        this.compact = compact;
        return this;
    }

    /** 在原价数量下方画删除线(表示该价格已折扣) */
    public CustomCountElement strikethrough(boolean strikethrough) {
        this.strikethrough = strikethrough;
        return this;
    }

    public CustomCountElement extraScale(float extraScale) {
        this.extraScale = extraScale;
        return this;
    }

    public CustomCountElement offset(float offsetX, float offsetY) {
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        return this;
    }

    /**
     * 数量格式化:默认将大数缩写为带单位的数值(10000 -> 10k, 12500 -> 12.5k,
     * 123456 -> 123k, 1000000 -> 1M),上限 {@link Long#MAX_VALUE}。
     */
    public static String formatCount(long count) {
        if (count < 1000) return String.valueOf(count);
        double value = count;
        int unitIndex = -1;
        while (value >= 1000 && unitIndex < UNITS.length - 1) {
            value /= 1000;
            unitIndex++;
        }
        String num;
        if (value >= 100) {
            num = String.valueOf((long) Math.floor(value));
        } else {
            num = String.format(java.util.Locale.ROOT, "%.1f", value);
            if (num.endsWith(".0")) {
                num = num.substring(0, num.length() - 2);
            }
        }
        return num + UNITS[unitIndex];
    }

    @Override
    public void drawBackgroundAdditional(GUIContext guiContext) {
        long count = countSupplier.getAsLong();
        // 始终显示物品数字(包括 1),空槽(0)不显示
        if (count <= 0) return;

        String text = compact ? formatCount(count) : String.valueOf(count);
        var font = Minecraft.getInstance().font;
        var pose = guiContext.graphics.pose();
        // 缩放直接取 extraScale(0.5 = 原版 9px 字体的 50%),不再随槽尺寸二次缩放
        float scale = extraScale;
        if (scale <= 0f) return;

        float x = (float) getPositionX();
        float y = (float) getPositionY();
        float w = getSizeWidth();
        float h = getSizeHeight();
        float textW = font.width(text) * scale;
        float textH = 9f * scale;

        // 右下角对齐(贴槽右下,右 1px 下 1px),0.5 倍默认大小
        pose.pushPose();
        pose.translate(x + w - textW - 1f + offsetX, y + h - textH - 1f + offsetY, -200);
        pose.scale(scale, scale, 1);
        guiContext.graphics.drawString(font, text, 0, 0, color, shadow);
        pose.popPose();
    }
}
