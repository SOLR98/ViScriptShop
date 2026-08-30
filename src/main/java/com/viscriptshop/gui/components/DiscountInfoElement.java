package com.viscriptshop.gui.components;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import net.minecraft.client.Minecraft;

/**
 * 成本槽右侧的折扣信息(与槽同属一个物品):两行显示——
 * 第一行折率(如 "-10%",折扣绿/涨价红),第二行折后价格(如 "17",白色)。
 * 仅在有折扣时创建。纯展示,不参与命中测试。
 */
public class DiscountInfoElement extends UIElement {
    private final long finalCount;
    private final double rate;

    public DiscountInfoElement(long finalCount, double rate) {
        this.finalCount = finalCount;
        this.rate = rate;
        setAllowHitTest(false);
    }

    /** 折率格式化:-0.15 -> "-15%",0.1 -> "+10%" */
    public static String formatRate(double rate) {
        long percent = Math.round(rate * 100.0);
        return percent > 0 ? "+" + percent + "%" : percent + "%";
    }

    @Override
    public void drawBackgroundAdditional(GUIContext guiContext) {
        var font = Minecraft.getInstance().font;
        String rateText = formatRate(rate);
        String priceText = CustomCountElement.formatCount(finalCount);
        float rateScale = 0.5f;
        float priceScale = 0.7f;
        float rateH = 9f * rateScale;
        float priceH = 9f * priceScale;
        float x = (float) getPositionX();
        float y = (float) getPositionY();
        float h = getSizeHeight();
        int rateColor = rate < 0 ? 0xFF55FF55 : 0xFFFF5555;
        int priceColor = 0xFFFFD700;

        float totalH = rateH + priceH + 1f;
        float startY = y + (h - totalH) / 2f;
        var pose = guiContext.graphics.pose();
        // 第一行:折率(0.5 倍,折扣绿/涨价红)
        pose.pushPose();
        pose.translate(x + 1f, startY, -200);
        pose.scale(rateScale, rateScale, 1);
        guiContext.graphics.drawString(font, rateText, 0, 0, rateColor);
        pose.popPose();
        // 第二行:折后价格(0.7 倍,金色,比折率更大更醒目)
        pose.pushPose();
        pose.translate(x + 1f, startY + rateH + 1f, -200);
        pose.scale(priceScale, priceScale, 1);
        guiContext.graphics.drawString(font, priceText, 0, 0, priceColor);
        pose.popPose();
    }
}
