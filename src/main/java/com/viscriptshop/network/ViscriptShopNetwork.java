package com.viscriptshop.network;

import com.viscriptshop.ViscriptShop;
import com.viscriptshop.network.c2s.BuyMerchantPayload;
import com.viscriptshop.network.c2s.GetItemCountC2SPayload;
import com.viscriptshop.network.s2c.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = ViscriptShop.MOD_ID)
public class ViscriptShopNetwork {

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(ViscriptShop.MOD_ID);
        //s2c
        registrar.playToClient(OpenShopEditorPayload.TYPE, OpenShopEditorPayload.CODEC, OpenShopEditorPayload::execute);
        registrar.playToClient(OpenShopUIPayload.TYPE, OpenShopUIPayload.CODEC, OpenShopUIPayload::execute);
        registrar.playToClient(ReloadShopUIPayload.TYPE, ReloadShopUIPayload.CODEC, ReloadShopUIPayload::execute);
        registrar.playToClient(GetItemCountS2CPayload.TYPE, GetItemCountS2CPayload.CODEC, GetItemCountS2CPayload::execute);
        registrar.playToClient(SendMessagePayload.TYPE, SendMessagePayload.CODEC, SendMessagePayload::execute);

        //c2s
        registrar.playToServer(BuyMerchantPayload.TYPE, BuyMerchantPayload.CODEC, BuyMerchantPayload::execute);
        registrar.playToServer(GetItemCountC2SPayload.TYPE, GetItemCountC2SPayload.CODEC, GetItemCountC2SPayload::execute);
    }
}
