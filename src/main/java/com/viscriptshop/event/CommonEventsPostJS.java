package com.viscriptshop.event;

import com.viscriptshop.event.kubejs.ShopEventJS;
import com.viscriptshop.event.neoforge.ShopEvent;
import dev.latvian.mods.kubejs.event.EventResult;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;

public class CommonEventsPostJS {
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void shopOpening(ShopEvent.Opening event) {
        if (ViScriptShopEventsJS.OPENING.hasListeners()) {
            ViScriptShopEventsJS.OPENING.post(new ShopEventJS.Opening(event));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void shopClosing(ShopEvent.Closing event) {
        if (ViScriptShopEventsJS.CLOSING.hasListeners()) {
            ViScriptShopEventsJS.CLOSING.post(new ShopEventJS.Closing(event));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void shopTick(ShopEvent.Tick event) {
        if (ViScriptShopEventsJS.TICK.hasListeners()) {
            ViScriptShopEventsJS.TICK.post(new ShopEventJS.Tick(event));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void shopBuyPre(ShopEvent.BuyPre event) {
        if (ViScriptShopEventsJS.BUY_PRE.hasListeners()) {
            EventResult result = ViScriptShopEventsJS.BUY_PRE.post(new ShopEventJS.BuyPre(event));
            if (result.interruptFalse()) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void shopBuyFail(ShopEvent.BuyFail event) {
        if (ViScriptShopEventsJS.BUY_FAIL.hasListeners()) {
            ViScriptShopEventsJS.BUY_FAIL.post(new ShopEventJS.BuyFail(event));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void shopBuyFail(ShopEvent.BuySuccess event) {
        if (ViScriptShopEventsJS.BUY_SUCCESS.hasListeners()) {
            ViScriptShopEventsJS.BUY_SUCCESS.post(new ShopEventJS.BuySuccess(event));
        }
    }
}
