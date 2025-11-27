package com.viscriptshop;

import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.utils.PersistedParser;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import lombok.Data;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class ShopRegistries {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, ViscriptShop.MOD_ID);

    public static final Supplier<AttachmentType<Money>> MONEY = ATTACHMENT_TYPES.register("money", () -> AttachmentType.builder(Money::new)
            .serialize(Money.CODEC)
            .sync(Money.STREAM_CODEC)
            .copyOnDeath()
            .build()
    );


    @Data
    public static class Money implements IPersistedSerializable {
        public static final Codec<Money> CODEC = PersistedParser.createCodec(Money::new);
        public static final StreamCodec<ByteBuf, Money> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);
        @Persisted
        private int money;
    }
}
