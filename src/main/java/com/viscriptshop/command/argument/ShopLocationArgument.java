package com.viscriptshop.command.argument;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.resources.ResourceLocation;

import java.util.concurrent.CompletableFuture;

public class ShopLocationArgument extends ResourceLocationArgument {
    public static ShopLocationArgument shop() {
        return new ShopLocationArgument();
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        if (LDLib2.isClient()) {
            return SharedSuggestionProvider.suggestResource(
                    Minecraft.getInstance().getResourceManager().listResources("shop", arg -> arg.getPath().endsWith(".shop")).keySet()
                            .stream().map(rl -> ResourceLocation.fromNamespaceAndPath(rl.getNamespace(), rl.getPath().substring(5, rl.getPath().length() - 5))),
                    builder);
        }
        return super.listSuggestions(context, builder);
    }
}
