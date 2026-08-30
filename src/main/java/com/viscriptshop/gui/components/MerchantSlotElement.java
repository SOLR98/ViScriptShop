package com.viscriptshop.gui.components;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * 自绘商品槽:物品图标 + 数量数字(始终显示,long 上限)+ 可选删除线(§m 删除线格式)+
 * 槽右侧折扣信息(折率 + 折后价格)+ 可选"赠"字。
 *
 * <p>所有内容在同一个 {@link #drawBackgroundAdditional} 中按序绘制;数字/文本绘制
 * 在 z=200(原版 renderItemDecorations 的做法),确保盖在物品贴图之上。
 */
public class MerchantSlotElement extends UIElement {
    private static final int TEXT_Z = 200;
    private ItemStack item = ItemStack.EMPTY;
    private long displayCount = 0;
    private boolean strikethrough = false;
    private boolean giftTag = false;
    private long finalCount = 0;
    private double rate = 0.0;
    private boolean hasDiscount = false;
    private HoverTooltips customTooltips;

    public MerchantSlotElement() {
        setAllowHitTest(true);
        addEventListener(UIEvents.HOVER_TOOLTIPS, event -> {
            if (customTooltips != null) {
                event.hoverTooltips = customTooltips;
            } else if (!item.isEmpty()) {
                Minecraft mc = Minecraft.getInstance();
                TooltipFlag flag = mc.options.advancedItemTooltips
                        ? TooltipFlag.ADVANCED
                        : TooltipFlag.NORMAL;
                List<net.minecraft.network.chat.Component> lines =
                        item.getTooltipLines(Item.TooltipContext.of(mc.level), mc.player, flag);
                event.hoverTooltips = new HoverTooltips(lines, item.getTooltipImage().orElse(null), null, item);
            }
        });
    }

    public MerchantSlotElement item(ItemStack item) {
        this.item = item == null ? ItemStack.EMPTY : item.copy();
        return this;
    }

    public MerchantSlotElement displayCount(long displayCount) {
        this.displayCount = displayCount;
        return this;
    }

    public MerchantSlotElement strikethrough(boolean strikethrough) {
        this.strikethrough = strikethrough;
        return this;
    }

    public MerchantSlotElement giftTag(boolean giftTag) {
        this.giftTag = giftTag;
        return this;
    }

    public MerchantSlotElement discount(long finalCount, double rate) {
        this.hasDiscount = true;
        this.finalCount = finalCount;
        this.rate = rate;
        return this;
    }

    public MerchantSlotElement customTooltips(HoverTooltips customTooltips) {
        this.customTooltips = customTooltips;
        return this;
    }

    @Override
    public void drawBackgroundAdditional(GUIContext guiContext) {
        var graphics = guiContext.graphics;
        var font = Minecraft.getInstance().font;
        float x = getPositionX();
        float y = getPositionY();
        float w = getSizeWidth();
        float h = getSizeHeight();

        // 空槽:不渲染图标/数字/折扣信息/赠字
        if (item.isEmpty()) return;

        // 1. 物品图标(缩放到槽尺寸)
        {
            var pose = graphics.pose();
            pose.pushPose();
            pose.scale(w / 16f, h / 16f, 1);
            pose.translate(x * 16f / w, y * 16f / h, 0);
            graphics.renderFakeItem(item, 0, 0);
            pose.popPose();
        }

        // 2. 数量数字(右下角,始终显示,0.5 倍;折扣时用 §m 删除线格式)
        String countText = CustomCountElement.formatCount(displayCount);
        float countScale = 0.5f;
        Component countComponent = strikethrough
                ? Component.literal(countText).withStyle(ChatFormatting.STRIKETHROUGH)
                : Component.literal(countText);
        float countW = font.width(countComponent) * countScale;
        float countH = 9f * countScale;
        drawScaled(guiContext, font, countComponent, x + w - countW - 1f, y + h - countH - 1f,
                countScale, 0xFFFFFFFF, false);

        // 3. 折扣信息(紧贴槽右缘,两行:折率 + 折后价格)
        if (hasDiscount) {
            Component rateText = Component.literal(DiscountInfoElement.formatRate(rate));
            Component priceText = Component.literal(CustomCountElement.formatCount(finalCount));
            float rateScale = 0.5f;
            float priceScale = 0.7f;
            float rateH = 9f * rateScale;
            float priceH = 9f * priceScale;
            float infoX = x + w;
            float startY = y + (h - (rateH + priceH + 1f)) / 2f;
            int rateColor = rate < 0 ? 0xFF55FF55 : 0xFFFF5555;
            drawScaled(guiContext, font, rateText, infoX, startY, rateScale, rateColor, false);
            drawScaled(guiContext, font, priceText, infoX, startY + rateH + 1f, priceScale, 0xFFFFD700, false);
        }

        // 4. "赠"字(左上,买赠槽)
        if (giftTag) {
            drawScaled(guiContext, font, Component.literal("赠"), x + 1f, y + 1f, 0.6f, 0xFFFFAA00, false);
        }
    }

    private static void drawScaled(GUIContext guiContext, Font font, Component text,
                                   float px, float py, float scale, int color, boolean rightAligned) {
        if (text == null || text.getString().isEmpty()) return;
        var pose = guiContext.graphics.pose();
        pose.pushPose();
        // z=200 确保盖在物品贴图之上(原版 renderItemDecorations 的做法)
        pose.translate(px, py, TEXT_Z);
        pose.scale(scale, scale, 1);
        int offsetX = rightAligned ? -font.width(text) : 0;
        guiContext.graphics.drawString(font, text, offsetX, 0, color);
        pose.popPose();
    }
}
