package com.viscriptshop.event.kubejs;

import com.viscriptshop.event.neoforge.ShopEvent;
import com.viscriptshop.gui.ShopUI;
import dev.latvian.mods.kubejs.event.KubeEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ShopEventJS implements KubeEvent {
    private final ShopUI shopUI;

    public static class Opening extends ShopEventJS {
        public Opening(ShopEvent.Opening event) {
            super(event.getShopUI());
        }
    }

    public static class Closing extends ShopEventJS {
        public Closing(ShopEvent.Closing event) {
            super(event.getShopUI());
        }
    }

    public static class Tick extends ShopEventJS {
        public Tick(ShopEvent.Tick event) {
            super(event.getShopUI());
        }
    }

    public static class BuyPre extends ShopEventJS {
        public BuyPre(ShopEvent.BuyPre event) {
            super(event.getShopUI());
        }
    }

    public static class BuyFail extends ShopEventJS {
        public BuyFail(ShopEvent.BuyFail event) {
            super(event.getShopUI());
        }
    }

    public static class BuySuccess extends ShopEventJS {
        public BuySuccess(ShopEvent.BuySuccess event) {
            super(event.getShopUI());
        }
    }
}
