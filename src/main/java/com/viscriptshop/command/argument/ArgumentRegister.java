package com.viscriptshop.command.argument;

import com.viscriptshop.ViscriptShop;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ArgumentRegister {
    public static final DeferredRegister<ArgumentTypeInfo<?, ?>> ARGUMENT_TYPE = DeferredRegister.create(Registries.COMMAND_ARGUMENT_TYPE, ViscriptShop.MOD_ID);

    public static final DeferredHolder<ArgumentTypeInfo<?, ?>, SingletonArgumentInfo<ShopLocationArgument>> SHOP_LOCATION_ARGUMENT
            = ARGUMENT_TYPE.register("shop_location", () -> ArgumentTypeInfos.registerByClass(ShopLocationArgument.class,
            SingletonArgumentInfo.contextFree(ShopLocationArgument::new)));
}
