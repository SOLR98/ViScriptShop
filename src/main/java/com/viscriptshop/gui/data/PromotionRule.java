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
 * 商店促销规则(折扣 / 买赠)。
 *
 * <p>{@code DISCOUNT}:按 {@code slot} 对成本槽打折,{@code discount} 为折扣率(0.1=9折,-0.2=涨价20%)。
 * {@code BUY_GET}:买 {@code buyThreshold} 送 {@code giftCount} 个 {@code giftItem}(空=默认送 itemResult 同款)。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PromotionRule implements IPersistedSerializable {
    public static final StreamCodec<ByteBuf, PromotionRule> STREAM_CODEC;
    public static final Codec<PromotionRule> CODEC;

    public enum PromotionType {
        DISCOUNT,
        BUY_GET
    }

    public enum CostSlot {
        ALL,
        ITEM_A,
        ITEM_B
    }

    public enum DiscountAggregationMode {
        ADD,
        MAX,
        MIN,
        MULTIPLY
    }

    public enum ComparisonOp {
        GE,
        LE,
        EQ,
        BETWEEN
    }

    public enum ConditionType {
        HAS_FLAG,
        HAS_PERMISSION,
        HAS_EFFECT,
        REPUTATION,
        PLAYER_XP_LEVEL,
        SHOP_MATCH,
        CATEGORY_MATCH,
        MERCHANT_MATCH,
        ITEM_IN_CART,
        GAME_TIME
    }

    /** 折扣方向:减少(打折,默认)/ 涨价 */
    public enum DiscountDirection {
        REDUCE,
        INCREASE
    }

    /** 折扣计算类型 */
    public enum DiscountCalcType {
        /** 按百分比直接加/减:base ± base×|rate| */
        PERCENT_DIRECT,
        /** 按百分比缩放:base × (1 ± |rate|)(默认) */
        PERCENT_REDUCE,
        /** 按绝对值加/减:base ± |rate| */
        FLAT_REDUCE
    }

    @Persisted
    private String id = "";
    @Persisted
    private PromotionType type = PromotionType.DISCOUNT;
    @Persisted
    private CostSlot slot = CostSlot.ALL;
    @Persisted
    private List<DiscountCondition> conditions = new ArrayList<>();
    @Persisted
    private boolean enabled = true;
    // DISCOUNT
    @Persisted
    private double discount = 0.0;
    /** 折扣方向(默认减少/打折),rate 始终为正幅度 */
    @Persisted
    private DiscountDirection direction = DiscountDirection.REDUCE;
    /** 折扣计算类型(默认按百分比减少) */
    @Persisted
    private DiscountCalcType calcType = DiscountCalcType.PERCENT_REDUCE;
    /** 动态声望折扣:命中 REPUTATION 条件时,折扣率 = -floor(声望 × 0.05) / 基础数量(原版村民折扣语义,按商品各自折算) */
    @Persisted
    private boolean dynamicReputation = false;
    // BUY_GET
    @Persisted
    private int buyThreshold = 0;
    @Persisted
    private int giftCount = 0;
    @Persisted
    private ItemStack giftItem = ItemStack.EMPTY;

    static {
        CODEC = PersistedParser.createCodec(PromotionRule::new);
        STREAM_CODEC = PersistedParser.createStreamCodec(PromotionRule::new);
    }

    public boolean matchesSlot(CostSlot currentSlot) {
        return slot == CostSlot.ALL || slot == currentSlot;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DiscountCondition implements IPersistedSerializable {
        public static final StreamCodec<ByteBuf, DiscountCondition> STREAM_CODEC;
        public static final Codec<DiscountCondition> CODEC;

        @Persisted
        private ConditionType type = ConditionType.HAS_FLAG;
        @Persisted
        private String value = "";
        @Persisted
        private ComparisonOp op = ComparisonOp.GE;
        @Persisted
        private double threshold = 0.0;

        static {
            CODEC = PersistedParser.createCodec(DiscountCondition::new);
            STREAM_CODEC = PersistedParser.createStreamCodec(DiscountCondition::new);
        }
    }
}
