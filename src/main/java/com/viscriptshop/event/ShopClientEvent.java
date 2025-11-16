package com.viscriptshop.event;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUIScreen;
import com.viscriptshop.ViscriptShop;
import com.viscriptshop.event.neoforge.ShopEvent;
import com.viscriptshop.gui.ShopUI;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

@EventBusSubscriber(modid = ViscriptShop.MOD_ID, value = Dist.CLIENT)
public class ShopClientEvent {
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
