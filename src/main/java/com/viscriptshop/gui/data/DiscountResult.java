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

    /**
     * 解析来源文本为显示组件:
     * 以 "{" 开头视为 {@link Component.Serializer} 序列化(文本组件写法,原样解析);
     * 否则按翻译键优先(无翻译时显示键本身,即直接字符串写法)。
     */
    public static net.minecraft.network.chat.Component parseSource(String source) {
        if (source == null || source.isBlank()) {
            return net.minecraft.network.chat.Component.translatable("viscript_shop.discount.source.external");
        }
        if (source.startsWith("{")) {
            try {
                net.minecraft.network.chat.Component parsed =
                        net.minecraft.network.chat.Component.Serializer.fromJson(source,
                                net.minecraft.core.RegistryAccess.EMPTY);
                if (parsed != null) return parsed;
            } catch (Exception ignored) {
            }
        }
        return net.minecraft.network.chat.Component.translatable(source);
    }

    /** 序列化来源为存储字符串:Component → JSON;String 原样 */
    public static String serializeSource(String source) {
        return source == null ? "" : source;
    }
}
