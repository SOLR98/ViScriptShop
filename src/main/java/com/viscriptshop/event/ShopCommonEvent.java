package com.viscriptshop.event;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUIScreen;
import com.viscriptshop.ViscriptShop;
import com.viscriptshop.command.ShopCommand;
import com.viscriptshop.event.neoforge.ShopEvent;
import com.viscriptshop.gui.ShopUI;
import com.viscriptshop.gui.data.ShopSavedData;
import com.viscriptshop.util.ShopHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

@EventBusSubscriber(modid = ViscriptShop.MOD_ID)
public class ShopCommonEvent {

    @SubscribeEvent
    public static void reloadCommandSuggestions(ServerStartedEvent event) {
        ShopCommand.shopFilesPath.clear();
        for (String path : ShopHelper.scanShopFiles()) {
            ShopCommand.shopFilesPath.add(ViscriptShop.id(path));
        }
    }

    @SubscribeEvent
    public static void onWorldLoad(LevelEvent.Load event) {
        LevelAccessor levelAccessor = event.getLevel();
        //只需要保存在主世界的data目录下即可
        if (levelAccessor instanceof ServerLevel world && world.dimension() == Level.OVERWORLD) {
            ViscriptShop.setShopSavedData(world.getDataStorage().computeIfAbsent(ShopSavedData.factory(world), "shop_info"));
        }
    }

    @SubscribeEvent
    public static void shopUiOpening(ScreenEvent.Opening event) {
        if (event.getScreen() instanceof ModularUIScreen screen && screen.modularUI.ui.rootElement instanceof ShopUI shopUI) {
            NeoForge.EVENT_BUS.post(new ShopEvent.Opening(shopUI));
        }
    }

    @SubscribeEvent
    public static void shopUiClosing(ScreenEvent.Closing event) {
        if (event.getScreen() instanceof ModularUIScreen screen && screen.modularUI.ui.rootElement instanceof ShopUI shopUI) {
            NeoForge.EVENT_BUS.post(new ShopEvent.Closing(shopUI));
        }
    }
}
