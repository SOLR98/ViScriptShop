package com.viscriptshop;

import com.lowdragmc.lowdraglib2.registry.AutoRegistry;
import com.viscriptshop.command.ICommand;

import java.util.function.Supplier;

public class ViScriptShopRegistries {
    public static AutoRegistry.LDLibRegister<ICommand, Supplier<ICommand>> COMMANDS;

    static {
        COMMANDS = AutoRegistry.LDLibRegister
                .create(ViscriptShop.id("command"), ICommand.class, AutoRegistry::noArgsCreator);
    }
}
