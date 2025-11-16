package com.viscriptshop.event;

import com.viscriptshop.event.kubejs.ShopEventJS;
import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;

public interface ViScriptShopEventsJS {
    EventGroup GROUP = EventGroup.of("ViScriptShopEvents");

    EventHandler OPENING = GROUP.client("opening", () -> ShopEventJS.Opening.class);
    EventHandler CLOSING = GROUP.client("closing", () -> ShopEventJS.Closing.class);
    EventHandler TICK = GROUP.client("tick", () -> ShopEventJS.Tick.class);
    EventHandler BUY_PRE = GROUP.client("buyPre", () -> ShopEventJS.BuyPre.class).hasResult();
    EventHandler BUY_FAIL = GROUP.client("buyFail", () -> ShopEventJS.BuyFail.class);
    EventHandler BUY_SUCCESS = GROUP.client("buySuccess", () -> ShopEventJS.BuySuccess.class);
}
