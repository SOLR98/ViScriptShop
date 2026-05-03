package com.viscriptshop;

import com.lowdragmc.lowdraglib2.editor.ui.EditorWindow;
import com.lowdragmc.lowdraglib2.gui.factory.PlayerUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.registry.AutoRegistry;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.syncdata.AccessorRegistries;
import com.lowdragmc.lowdraglib2.syncdata.accessor.direct.CustomDirectAccessor;
import com.mojang.logging.LogUtils;
import com.viscriptshop.command.ICommand;
import com.viscriptshop.compat.ModComPat;
import com.viscriptshop.gui.ShopEditor;
import com.viscriptshop.gui.data.*;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;

import java.util.function.Supplier;

@Mod(ViscriptShop.MOD_ID)
public class ViscriptShop {
    public static final String MOD_ID = "viscript_shop";
    public static final Logger LOGGER = LogUtils.getLogger();
    @Setter
    @Getter
    private static ShopSavedData shopSavedData;

    public ViscriptShop(IEventBus modEventBus, ModContainer modContainer, Dist dist) {
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
        ShopRegistries.ATTACHMENT_TYPES.register(modEventBus);
        ModComPat.init(dist);
        AccessorRegistries.setPriority(0);
        AccessorRegistries.registerAccessor(CustomDirectAccessor.builder(ShopInfo.class)
                .codec(ShopInfo.CODEC)
                .streamCodec(ShopInfo.STREAM_CODEC)
                .codecMark()
                .build()
        );
        AccessorRegistries.registerAccessor(CustomDirectAccessor.builder(CategoryInfo.class)
                .codec(CategoryInfo.CODEC)
                .streamCodec(CategoryInfo.STREAM_CODEC)
                .codecMark()
                .build()
        );
        AccessorRegistries.registerAccessor(CustomDirectAccessor.builder(MerchantInfo.class)
                .codec(MerchantInfo.CODEC)
                .streamCodec(MerchantInfo.STREAM_CODEC)
                .codecMark()
                .build()
        );
        AccessorRegistries.registerAccessor(CustomDirectAccessor.builder(AggregatedResources.class)
                .codec(AggregatedResources.CODEC)
                .streamCodec(AggregatedResources.STREAM_CODEC)
                .codecMark()
                .build()
        );
        AccessorRegistries.registerAccessor(CustomDirectAccessor.builder(AggregatedResources.PurchaseEntry.class)
                .codec(AggregatedResources.PurchaseEntry.CODEC)
                .streamCodec(AggregatedResources.PurchaseEntry.STREAM_CODEC)
                .codecMark()
                .build()
        );
        PlayerUIMenuType.register(ShopEditor.SHOP_ID, ignored -> player -> {
            if (player.level().isClientSide) {
                return new ModularUI(UI.of(EditorWindow.open(ShopEditor.SHOP_ID, ShopEditor::new)))
                        .shouldCloseOnKeyInventory(false);
            }
            return new ModularUI(UI.empty());
        });
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.CONFIG_SPEC, String.format("%s_config.toml", MOD_ID));
        if (dist == Dist.CLIENT) {
            modContainer.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        }
    }

    //注册指令
    private void onRegisterCommands(RegisterCommandsEvent event) {
        for (AutoRegistry.Holder<LDLRegister, ICommand, Supplier<ICommand>> command : ViScriptShopRegistries.COMMANDS) {
            command.value().get().register(event.getDispatcher(), event.getBuildContext(), event.getCommandSelection());
        }
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public static String formattedMod(String path) {
        return ("%s:" + path).formatted(MOD_ID);
    }

    public static boolean isPresentResource(ResourceLocation resourceLocation) {
        return Minecraft.getInstance().getResourceManager().getResource(resourceLocation).isPresent();
    }

    //精妙背包
    public static boolean isSophisticatedBackpacksLoaded() {
        return isModLoaded("sophisticatedbackpacks");
    }

    //超越维度
    public static boolean isBeyondDimensionsLoaded() {
        return isModLoaded("beyonddimensions");
    }

    //jei
    public static boolean isJEILoaded() {
        return isModLoaded("jei");
    }

    //Just Enough Characters
    public static boolean isJECharactersLoaded() {
        return isModLoaded("jecharacters");
    }

    //FtbLibrary
    public static boolean isFtbLibraryLoaded() {
        return isModLoaded("ftblibrary");
    }

    //Lightmans Currency
    public static boolean isLightmansCurrencyLoaded() {
        return isModLoaded("lightmanscurrency");
    }

    //汇流来世
    public static boolean isConfluenceLoaded() {
        return isModLoaded("confluence");
    }

    //Magic Coins
    public static boolean isMagicCoinsLoaded() {
        return isModLoaded("magic_coins");
    }

    private static boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }
}
