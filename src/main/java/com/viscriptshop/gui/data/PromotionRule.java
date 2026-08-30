package com.viscriptshop.gui.data;

import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigSelector;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
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
public class PromotionRule implements IConfigurable, IPersistedSerializable {
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

    @Configurable(name = "viscript_shop.data.promotion.id")
    @Persisted
    private String id = "";
    @Configurable(name = "viscript_shop.data.promotion.type")
    @Persisted
    private PromotionType type = PromotionType.DISCOUNT;
    @Configurable(name = "viscript_shop.data.promotion.slot")
    @Persisted
    private CostSlot slot = CostSlot.ALL;
    @Configurable(name = "viscript_shop.data.promotion.conditions")
    @Persisted
    private List<DiscountCondition> conditions = new ArrayList<>();
    @Configurable(name = "viscript_shop.data.promotion.enabled")
    @Persisted
    private boolean enabled = true;
    // DISCOUNT
    @Configurable(name = "viscript_shop.data.promotion.discount")
    @Persisted
    private double discount = 0.0;
    /** 折扣方向(默认减少/打折),rate 始终为正幅度 */
    @Configurable(name = "viscript_shop.data.promotion.direction")
    @Persisted
    private DiscountDirection direction = DiscountDirection.REDUCE;
    /** 折扣计算类型(默认按百分比减少) */
    @Configurable(name = "viscript_shop.data.promotion.calcType")
    @Persisted
    private DiscountCalcType calcType = DiscountCalcType.PERCENT_REDUCE;
    /** 动态声望折扣:命中 REPUTATION 条件时,折扣率 = -floor(声望 × 0.05) / 基础数量(原版村民折扣语义,按商品各自折算) */
    @Configurable(name = "viscript_shop.data.promotion.dynamicReputation")
    @Persisted
    private boolean dynamicReputation = false;
    // BUY_GET
    @Configurable(name = "viscript_shop.data.promotion.buyThreshold")
    @Persisted
    private int buyThreshold = 0;
    @Configurable(name = "viscript_shop.data.promotion.giftCount")
    @Persisted
    private int giftCount = 0;
    @Configurable(name = "viscript_shop.data.promotion.giftItem")
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
    public static class DiscountCondition implements IConfigurable, IPersistedSerializable {
        public static final StreamCodec<ByteBuf, DiscountCondition> STREAM_CODEC;
        public static final Codec<DiscountCondition> CODEC;

        @Configurable(name = "viscript_shop.data.promotion.condition.type")
        @Persisted
        private ConditionType type = ConditionType.HAS_FLAG;
        @Configurable(name = "viscript_shop.data.promotion.condition.value")
        @Persisted
        private String value = "";
        @Configurable(name = "viscript_shop.data.promotion.condition.op")
        @Persisted
        private ComparisonOp op = ComparisonOp.GE;
        @Configurable(name = "viscript_shop.data.promotion.condition.threshold")
        @Persisted
        private double threshold = 0.0;

        static {
            CODEC = PersistedParser.createCodec(DiscountCondition::new);
            STREAM_CODEC = PersistedParser.createStreamCodec(DiscountCondition::new);
        }
    }
}
