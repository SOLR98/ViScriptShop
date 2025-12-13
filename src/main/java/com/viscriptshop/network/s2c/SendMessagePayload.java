package com.viscriptshop.network.s2c;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.viscriptshop.ViscriptShop;
import com.viscriptshop.gui.ShopUI;
import com.viscriptshop.gui.components.Message;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record SendMessagePayload(Message.Type messageType, String message) implements CustomPacketPayload {
    public static final Type<SendMessagePayload> TYPE = new Type<>(ViscriptShop.id("send_message"));
    public static final StreamCodec<FriendlyByteBuf, SendMessagePayload> CODEC = StreamCodec.composite(
            Message.Type.STREAM_CODEC,
            SendMessagePayload::messageType,
            ByteBufCodecs.STRING_UTF8,
            SendMessagePayload::message,
            SendMessagePayload::new
    );


    public static void execute(SendMessagePayload payload, IPayloadContext context) {
        if (Minecraft.getInstance().screen instanceof ModularUIScreen screen && screen.modularUI.ui.rootElement instanceof ShopUI shopUI) {
            Message.send(payload.messageType, payload.message, shopUI);
        }
    }

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
