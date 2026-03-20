package com.viscriptshop.gui.data;

import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.utils.PersistedParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.viscriptshop.util.CodecUtil;
import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 汇总购物车中所需支付或获得的物品、货币和经验值。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AggregatedResources {
    public static final StreamCodec<ByteBuf, AggregatedResources> STREAM_CODEC;
    public static final Codec<AggregatedResources> CODEC;

    private Map<ItemStack, Integer> items = new HashMap<>();
    private List<String> commands = new ArrayList<>();
    private int totalMoney = 0;
    private int totalXp = 0;
    private List<PurchaseEntry> purchaseEntries = new ArrayList<>();

    static {
        CODEC = RecordCodecBuilder.create(instance -> instance.group(
                CodecUtil.createMapCodec(ItemStack.OPTIONAL_CODEC, Codec.INT, Platform.getFrozenRegistry())
                        .optionalFieldOf("items", new HashMap<>())
                        .forGetter(AggregatedResources::getItems),
                Codec.STRING.listOf()
                        .optionalFieldOf("commands", new ArrayList<>())
                        .forGetter(AggregatedResources::getCommands),
                Codec.INT.optionalFieldOf("totalMoney", 0)
                        .forGetter(AggregatedResources::getTotalMoney),
                Codec.INT.optionalFieldOf("totalXp", 0)
                        .forGetter(AggregatedResources::getTotalXp),
                PurchaseEntry.CODEC.listOf()
                        .optionalFieldOf("purchaseEntries", new ArrayList<>())
                        .forGetter(AggregatedResources::getPurchaseEntries)
        ).apply(instance, AggregatedResources::new));

        STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);
    }

    /**
     * 购买条目，记录具体购买了哪个商品多少数量
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PurchaseEntry implements IPersistedSerializable {
        public static final StreamCodec<ByteBuf, PurchaseEntry> STREAM_CODEC;
        public static final Codec<PurchaseEntry> CODEC;

        @Persisted
        private String categoryId;
        @Persisted
        private String merchantId;
        @Persisted
        private int buyCount;

        static {
            CODEC = PersistedParser.createCodec(PurchaseEntry::new);
            STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);
        }
    }

    public boolean isEmpty() {
        return purchaseEntries.isEmpty();
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
     * 合并指令
     *
     * @param command 指令
     */
    public void addCommand(String command) {
        if (!command.isEmpty()) {
            commands.add(command);
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

                // 记录购买条目
                cost.getPurchaseEntries().add(new PurchaseEntry(categoryInfo.getId(), merchant.getId(), count));

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

                // 记录购买条目（只需要记录一次即可）
                gain.getPurchaseEntries().add(new PurchaseEntry(categoryInfo.getId(), merchant.getId(), count));

                //通用收益
                gain.addXp(merchant.getXp(), count);
                gain.addCommand(merchant.getCommand());
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
                                // 出售物品：收益是货币
                                gain.addMoney(merchant.getMoney(), count);
                            }
                        }
                    }
                }
            }
        }
        return gain;
    }
}
