package com.viscriptshop;

import com.lowdragmc.lowdraglib2.registry.AutoRegistry;
import com.viscriptshop.command.ICommand;
import com.viscriptshop.compat.IContainerHelper;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;

public class ViScriptShopRegistries {
    public static AutoRegistry.LDLibRegister<ICommand, Supplier<ICommand>> COMMANDS;
    public static AutoRegistry.LDLibRegister<IContainerHelper, Supplier<IContainerHelper>> ContainerHelper;

    static {
        COMMANDS = AutoRegistry.LDLibRegister
                .create(ResourceLocation.parse(ICommand.COMMAND_ID), ICommand.class, AutoRegistry::noArgsCreator);
        ContainerHelper = AutoRegistry.LDLibRegister
                .create(ResourceLocation.parse(IContainerHelper.CONTAINER_HELPER_ID), IContainerHelper.class, AutoRegistry::noArgsCreator);
    }
}
