package com.viscriptshop.util;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.editor.ui.Editor;
import com.lowdragmc.lowdraglib2.editor.ui.EditorWindow;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.viscriptshop.Config;
import com.viscriptshop.ShopRegistries;
import com.viscriptshop.ViscriptShop;
import com.viscriptshop.gui.ShopEditor;
import com.viscriptshop.gui.ShopUI;
import com.viscriptshop.gui.data.ShopInfo;
import com.viscriptshop.gui.project.ShopProject;
import dev.latvian.mods.kubejs.typings.Info;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.sirgrantd.sg_economy.api.SGEconomyApi;

public class ViScriptShopClientUtil {
    @Info("客户端打开商店编辑器")
    public static void clientOpenNpcEditor(CompoundTag tag) {
        Minecraft minecraft = Minecraft.getInstance();
        EditorWindow editorWindow = EditorWindow.open(ShopEditor.SHOP_ID, ShopEditor::new);
        ModularUI ui = new ModularUI(UI.of(editorWindow));
        if (!Platform.isDevEnv()) ui.shouldCloseOnEsc(false).shouldCloseOnKeyInventory(false);
        minecraft.setScreen(new ModularUIScreen(ui, Component.empty()));

        Editor editor = editorWindow.getCurrentEditor();
        if (editor == null) return;
        if (tag != null && !tag.isEmpty()) {
            var project = (ShopProject) ShopProject.PROVIDER.projectCreator.get();
            project.initNewProject();
            try {
                project.shop.deserializeNBT(Platform.getFrozenRegistry(), tag);
                editor.loadProject(project, null);
                return;
            } catch (Exception ignored) {
            }
        }
    }

    @Info("客户端打开商店")
    public static void clientOpenShop(ShopInfo shopInfo) {
        clientOpenShop(shopInfo, null, null);
    }

    @Info("客户端打开商店（带分类和商品参数）")
    public static void clientOpenShop(ShopInfo shopInfo, String categoryId, String merchantId) {
        Minecraft minecraft = Minecraft.getInstance();
        ShopUI shopUI = new ShopUI(shopInfo, shopInfo.getName().isEmpty() ? "viscript_shop.ui.title" : shopInfo.getName(), categoryId, merchantId);
        if (shopInfo.getCategoryInfos().isEmpty()) {
            ViscriptShop.LOGGER.error("不合规的商店信息：商店分类栏为空");
            return;
        }
        ModularUI modularUI = new ModularUI(UI.of(shopUI));
        minecraft.setScreen(new ModularUIScreen(modularUI, Component.empty()));
    }

    @Info("获取玩家钱")
    public static int getMoney(LocalPlayer player) {
        if (ViscriptShop.isMagicCoinsLoaded() && Config.isReplaceMoneyToMagicCoin.get()) {
            return SGEconomyApi.get().getBalanceAsInt(player);
        }
        return player.getData(ShopRegistries.MONEY).getMoney();
    }
}
