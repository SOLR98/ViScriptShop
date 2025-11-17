package com.viscriptshop.event.kubejs;

import com.viscriptshop.event.neoforge.ShopServerEvent;
import com.viscriptshop.gui.data.MerchantInfo;
import dev.latvian.mods.kubejs.event.KubeEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.server.level.ServerPlayer;

@Getter
@AllArgsConstructor
public class ShopServerEventJS implements KubeEvent {
    private final ServerPlayer player;
    private final MerchantInfo merchantIo;

    public static class BuyPre extends ShopServerEventJS {
        public BuyPre(ShopServerEvent.BuyPre event) {
            super(event.getPlayer(), event.getMerchantInfo());
        }
    }

    public static class BuyFail extends ShopServerEventJS {
        public BuyFail(ShopServerEvent.BuyFail event) {
            super(event.getPlayer(), event.getMerchantInfo());
        }
    }

    public static class BuySuccess extends ShopServerEventJS {
        public BuySuccess(ShopServerEvent.BuySuccess event) {
            super(event.getPlayer(), event.getMerchantInfo());
        }
    }
}
