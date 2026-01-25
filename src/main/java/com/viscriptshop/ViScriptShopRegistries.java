package com.viscriptshop;

import com.lowdragmc.lowdraglib2.registry.AutoRegistry;
import com.viscriptshop.command.ICommand;
import com.viscriptshop.compat.IContainerHelper;

import java.util.function.Supplier;

public class ViScriptShopRegistries {
    public static AutoRegistry.LDLibRegister<ICommand, Supplier<ICommand>> COMMANDS;
    public static AutoRegistry.LDLibRegister<IContainerHelper, Supplier<IContainerHelper>> ContainerHelper;

    static {
        COMMANDS = AutoRegistry.LDLibRegister
                .create(ViscriptShop.id("command"), ICommand.class, AutoRegistry::noArgsCreator);
        ContainerHelper = AutoRegistry.LDLibRegister
                .create(ViscriptShop.id("container_helper"), IContainerHelper.class, AutoRegistry::noArgsCreator);
    }
}
