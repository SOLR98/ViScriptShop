package com.viscriptshop.gui.components;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.style.PropertyRegistry;
import net.minecraft.client.Minecraft;

/**
 * 叠画在买赠槽左上角的"赠"小字标记。
 * 纯展示,不参与命中测试,不阻挡槽的悬浮提示。
 */
public class GiftTagElement extends UIElement {

    public GiftTagElement() {
        setAllowHitTest(false);
    }

    @Override
    public void drawBackgroundAdditional(GUIContext guiContext) {
        var font = Minecraft.getInstance().font;
        var pose = guiContext.graphics.pose();
        int x = (int) getPositionX();
        int y = (int) getPositionY();
        pose.pushPose();
        // 与物品图标同层(-200),后画覆盖,避免被 ItemSlot 的深度遮挡
        pose.translate(x + 1, y + 1, -200);
        pose.scale(0.6f, 0.6f, 1);
        guiContext.graphics.drawString(font, "赠", 0, 0, 0xFFFFAA00);
        pose.popPose();
    }
}
