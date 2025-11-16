package com.viscriptshop.network.s2c;

import com.viscriptshop.ViscriptShop;
import com.viscriptshop.util.ViScriptShopClientUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record OpenShopEditorPayload() implements CustomPacketPayload {
    public static final Type<OpenShopEditorPayload> TYPE = new Type<>(ViscriptShop.id("open_shop_editor"));
    public static final StreamCodec<FriendlyByteBuf, OpenShopEditorPayload> CODEC = StreamCodec.ofMember(
            OpenShopEditorPayload::write,
            OpenShopEditorPayload::new
    );

    public OpenShopEditorPayload(FriendlyByteBuf friendlyByteBuf) {
        this();
    }

    private void write(FriendlyByteBuf buf) {
    }


    public static void execute(OpenShopEditorPayload payload, IPayloadContext context) {
        ViScriptShopClientUtil.clientOpenNpcEditor();
    }

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
