package com.viscriptshop.gui.data;

import com.viscriptshop.util.ItemUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * 汇总购物车中所需支付或获得的物品、货币和经验值。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AggregatedResources {
    public static final StreamCodec<RegistryFriendlyByteBuf, AggregatedResources> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.map(HashMap::new, ItemUtil.ITEM_STACK_STREAM_CODEC, ByteBufCodecs.VAR_INT),
            AggregatedResources::getItems,
            ByteBufCodecs.INT,
            AggregatedResources::getTotalMoney,
            ByteBufCodecs.INT,
            AggregatedResources::getTotalXp,
            AggregatedResources::new
    );

    private Map<ItemStack, Integer> items = new HashMap<>();
    private int totalMoney = 0;
    private int totalXp = 0;

    public boolean isEmpty() {
        return items.isEmpty() && totalMoney == 0 && totalXp == 0;
    }

    /**
     * 将一个 ItemStack 合并到汇总中。
     *
     * @param stack 要合并的物品（通常数量为1，但也可以是任意数量）
     * @param count 购买数量 (buyCount)
     */
    public void addItem(ItemStack stack, int count) {
        if (stack.isEmpty() || count <= 0) return;

        // 计算总数量
        int totalQuantity = stack.getCount() * count;

        // 尝试找到已存在的相同物品
        ItemStack foundKey = null;
        for (ItemStack key : items.keySet()) {
            if (ItemStack.isSameItemSameComponents(stack, key)) {
                foundKey = key;
                break;
            }
        }

        if (foundKey != null) {
            // 更新累计数量
            items.put(foundKey, items.get(foundKey) + totalQuantity);
        } else {
            ItemStack newKey = stack.copy();
            newKey.setCount(1);
            items.put(newKey, totalQuantity);
        }
    }

    /**
     * 合并货币花费。
     *
     * @param money 花费的货币值
     * @param count 购买数量
     */
    public void addMoney(int money, int count) {
        if (money > 0 && count > 0) {
            this.totalMoney += money * count;
        }
    }

    /**
     * 合并经验值。
     *
     * @param xp    获得的经验值
     * @param count 购买数量
     */
    public void addXp(int xp, int count) {
        if (xp > 0 && count > 0) {
            this.totalXp += xp * count;
        }
    }

    /**
     * 计算购物车中所有商品的成本（玩家需要支付的）。
     *
     * @param shopInfo 商店信息，包括各个分类里所有的购物车列表
     * @return 购物车中所有商品的成本
     */
    public static AggregatedResources getCostSummary(ShopInfo shopInfo) {
        AggregatedResources cost = new AggregatedResources();
        for (CategoryInfo categoryInfo : shopInfo.getCategoryInfos()) {
            for (MerchantInfo merchant : categoryInfo.getMerchants()) {
                int count = (int) merchant.getBuyCount();
                if (count <= 0) continue;

                switch (categoryInfo.getShopType()) {
                    case ITEM_FOR_ITEM -> {
                        // 以物换物商店：成本是 itemA 和 itemB
                        cost.addItem(merchant.getItemA(), count);
                        cost.addItem(merchant.getItemB(), count);
                    }
                    case CURRENCY -> {
                        switch (merchant.getTradeType()) {
                            case BUY -> // 购买物品：成本是货币
                                    cost.addMoney(merchant.getMoney(), count);
                            case SELL -> // 出售物品：成本是玩家出售的物品 (itemResult)
                                    cost.addItem(merchant.getItemResult(), count);
                        }
                    }
                }
            }
        }
        return cost;
    }

    /**
     * 计算购物车中所有商品的收益（玩家可以获得的）。
     *
     * @param shopInfo 商店信息，包括各个分类里所有的购物车列表
     * @return 购物车中所有商品的收益
     */
    public static AggregatedResources getGainSummary(ShopInfo shopInfo) {
        AggregatedResources gain = new AggregatedResources();
        for (CategoryInfo categoryInfo : shopInfo.getCategoryInfos()) {
            for (MerchantInfo merchant : categoryInfo.getMerchants()) {
                int count = (int) merchant.getBuyCount();
                if (count <= 0) continue;
                switch (categoryInfo.getShopType()) {
                    case ITEM_FOR_ITEM -> {
                        // 以物换物商店：收益是 itemResult
                        gain.addItem(merchant.getItemResult(), count);
                    }
                    case CURRENCY -> {
                        // 通用货币商店：根据 TradeType 决定收益
                        switch (merchant.getTradeType()) {
                            case BUY -> {
                                // 购买物品：收益是 itemResult
                                gain.addItem(merchant.getItemResult(), count);
                            }
                            case SELL -> {
                                // 出售物品：收益是货币和经验值
                                gain.addMoney(merchant.getMoney(), count);
                                gain.addXp(merchant.getXp(), count);
                            }
                        }
                    }
                }
            }
        }
        return gain;
    }
}
