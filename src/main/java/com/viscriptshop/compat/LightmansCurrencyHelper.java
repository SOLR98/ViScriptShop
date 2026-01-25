package com.viscriptshop.compat;

import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import io.github.lightman314.lightmanscurrency.LightmansCurrency;
import io.github.lightman314.lightmanscurrency.api.money.coins.CoinAPI;
import io.github.lightman314.lightmanscurrency.common.attachments.WalletHandler;
import io.github.lightman314.lightmanscurrency.common.items.WalletItem;
import io.github.lightman314.lightmanscurrency.common.items.data.WalletDataWrapper;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

@Slf4j
@LDLRegister(name = LightmansCurrency.MODID, registry = IContainerHelper.CONTAINER_HELPER_ID, modID = LightmansCurrency.MODID)
public class LightmansCurrencyHelper implements IContainerHelper {
    @Override
    public int getItemStackCount(ServerPlayer player, ItemStack item) {
        if (!CoinAPI.getApi().IsCoin(item, false)) {
            return 0;
        }

        WalletHandler walletHandler = WalletHandler.get(player);
        ItemStack wallet = walletHandler.getWallet();
        if (!WalletItem.isWallet(wallet)) {
            return 0;
        }

        Container contents = WalletItem.getDataWrapper(wallet).getContents();

        int count = 0;
        for (int i = 0; i < contents.getContainerSize(); i++) {
            ItemStack stack = contents.getItem(i);
            if (ItemStack.isSameItem(stack, item)) {
                count += stack.getCount();
            }
        }

        return count;
    }

    @Override
    public int removeItemStackByCount(ServerPlayer player, ItemStack item, int count) {
        if (!CoinAPI.getApi().IsCoin(item, false)) {
            return 0;
        }

        WalletHandler walletHandler = WalletHandler.get(player);
        ItemStack wallet = walletHandler.getWallet();
        if (!WalletItem.isWallet(wallet)) {
            return 0;
        }

        WalletDataWrapper wrapper = WalletItem.getDataWrapper(wallet);
        Container contents = wrapper.getContents();

        int remaining = count;
        for (int i = 0; i < contents.getContainerSize() && remaining > 0; i++) {
            ItemStack stack = contents.getItem(i);
            if (ItemStack.isSameItem(stack, item)) {
                int toRemove = Math.min(remaining, stack.getCount());
                System.out.println(toRemove);
                stack.shrink(toRemove);
                remaining -= toRemove;
            }
        }

        wrapper.setContents(contents, player);

        return count - remaining;
    }
}
