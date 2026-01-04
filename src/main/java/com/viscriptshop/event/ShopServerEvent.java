package com.viscriptshop.event;

import com.viscriptshop.ViscriptShop;
import com.viscriptshop.gui.data.ShopSavedData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;

@EventBusSubscriber(modid = ViscriptShop.MOD_ID)
public class ShopServerEvent {
    @SubscribeEvent
    public static void onWorldLoad(LevelEvent.Load event) {
        LevelAccessor levelAccessor = event.getLevel();
        //只需要保存在主世界的data目录下即可
        if (levelAccessor instanceof ServerLevel world && world.dimension() == Level.OVERWORLD) {
            ViscriptShop.setShopSavedData(world.getDataStorage().computeIfAbsent(ShopSavedData.factory(), "shop_info"));
        }
    }
}
