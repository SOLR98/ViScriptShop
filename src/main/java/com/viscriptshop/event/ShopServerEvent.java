package com.viscriptshop.event;

import com.lowdragmc.lowdraglib2.networking.rpc.RPCPacketDistributor;
import com.viscriptshop.ViscriptShop;
import com.viscriptshop.command.ShopCommand;
import com.viscriptshop.gui.data.ShopInfo;
import com.viscriptshop.gui.data.ShopSavedData;
import com.viscriptshop.network.s2c.S2CPayload;
import com.viscriptshop.util.ShopHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;

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

    @SubscribeEvent
    public static void tick(ServerTickEvent.Post event) {
//        if (event.getServer().getTickCount() % 40 == 0) {
//            List<ShopInfo> shopInfos = new ArrayList<>();
//            for (String shopFile : ShopCommand.getServerShopFiles()) {
//                shopInfos.add(ShopHelper.getShop(shopFile));
//            }
//            RPCPacketDistributor.rpcToAllPlayers(S2CPayload.GET_SHOP_INFO_S2C, shopInfos.toArray());
//        }
    }
}
