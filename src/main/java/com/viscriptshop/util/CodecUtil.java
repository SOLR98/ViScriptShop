package com.viscriptshop.util;

import com.mojang.serialization.Codec;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;

public class CodecUtil {

    public static <T> T deserializeNBT(Codec<T> codec, CompoundTag tag, HolderLookup.Provider provider) {
        return codec.decode(provider.createSerializationContext(NbtOps.INSTANCE), tag).getOrThrow().getFirst();
    }

    public static <T> CompoundTag serializeNBT(Codec<T> codec, T object, HolderLookup.Provider provider) {
        return (CompoundTag) codec.encodeStart(provider.createSerializationContext(NbtOps.INSTANCE), object).result().orElse(new CompoundTag());
    }
}
