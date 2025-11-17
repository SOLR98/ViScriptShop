package com.viscriptshop.event.neoforge;

import com.viscriptshop.gui.data.MerchantInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

@Getter
@AllArgsConstructor
public class ShopServerEvent extends Event {
    private final ServerPlayer player;
    private final MerchantInfo merchantInfo;

    public static class BuyPre extends ShopServerEvent implements ICancellableEvent {
        public BuyPre(ServerPlayer player, MerchantInfo merchantInfo) {
            super(player, merchantInfo);
        }
    }

    public static class BuyFail extends ShopServerEvent {
        public BuyFail(ServerPlayer player, MerchantInfo merchantInfo) {
            super(player, merchantInfo);
        }
    }

    public static class BuySuccess extends ShopServerEvent {
        public BuySuccess(ServerPlayer player, MerchantInfo merchantInfo) {
            super(player, merchantInfo);
        }
    }
}
