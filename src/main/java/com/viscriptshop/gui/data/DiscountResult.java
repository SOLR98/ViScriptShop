package com.viscriptshop.gui.data;

import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.utils.PersistedParser;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 单槽折扣计算结果:原价、折后价、总折率与明细(内置规则 + 事件贡献)。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DiscountResult {
    private PromotionRule.CostSlot slot = PromotionRule.CostSlot.ALL;
    private int baseCount = 0;
    private int finalCount = 0;
    /** 最终折率(带符号:负=减少,正=涨价) */
    private double rate = 0.0;
    private PromotionRule.DiscountDirection direction = PromotionRule.DiscountDirection.REDUCE;
    private PromotionRule.DiscountCalcType calcType = PromotionRule.DiscountCalcType.PERCENT_REDUCE;
    private List<DiscountDetail> details = new ArrayList<>();

    public boolean hasDiscount() {
        return baseCount != finalCount;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DiscountDetail {
        private String source;
        private double rate;
        private PromotionRule.CostSlot slot;
    }

    /** 买赠明细(可序列化,随 {@link AggregatedResources#getBonusItems()} 传输) */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BonusDetail implements IPersistedSerializable {
        public static final StreamCodec<ByteBuf, BonusDetail> STREAM_CODEC;
        public static final Codec<BonusDetail> CODEC;

        @Persisted
        private ItemStack item = ItemStack.EMPTY;
        @Persisted
        private int count = 0;
        @Persisted
        private String source = "";

        static {
            CODEC = PersistedParser.createCodec(BonusDetail::new);
            STREAM_CODEC = PersistedParser.createStreamCodec(BonusDetail::new);
        }
    }
}
