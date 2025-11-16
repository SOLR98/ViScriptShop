package com.viscriptshop.event.neoforge;

import com.viscriptshop.gui.ShopUI;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

@Getter
@AllArgsConstructor
public abstract class ShopEvent extends Event {
    private final ShopUI shopUI;

    public static class Opening extends ShopEvent {
        public Opening(ShopUI shopUI) {
            super(shopUI);
        }
    }

    public static class Closing extends ShopEvent {
        public Closing(ShopUI shopUI) {
            super(shopUI);
        }
    }

    public static class Tick extends ShopEvent {
        public Tick(ShopUI shopUI) {
            super(shopUI);
        }
    }

    public static class BuyPre extends ShopEvent implements ICancellableEvent {
        public BuyPre(ShopUI shopUI) {
            super(shopUI);
        }
    }

    public static class BuyFail extends ShopEvent {
        public BuyFail(ShopUI shopUI) {
            super(shopUI);
        }
    }

    public static class BuySuccess extends ShopEvent {
        public BuySuccess(ShopUI shopUI) {
            super(shopUI);
        }
    }
}
