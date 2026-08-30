package com.viscriptshop.event.neoforge;

import com.viscriptshop.gui.data.CategoryInfo;
import com.viscriptshop.gui.data.DiscountResult;
import com.viscriptshop.gui.data.MerchantInfo;
import com.viscriptshop.gui.data.PromotionRule;
import com.viscriptshop.gui.data.ShopInfo;
import lombok.Getter;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;

import java.util.ArrayList;
import java.util.List;

/**
 * 折扣计算事件。规则引擎命中后的基础折扣率会先写入 {@link #discountRate},
 * 监听者可叠加({@link #addDiscount(double)})或覆盖({@link #setDiscount(double)})。
 * 事件贡献(含来源文本)会独立记录进明细,未提供来源的归入默认"外部折扣"。
 *
 * <p>双端触发:客户端(展示)与服务端(权威扣费)各触发一次,使用同一公式。
 */
@Getter
public class ShopDiscountEvent extends Event {
    private final Player player;
    private final ShopInfo shopInfo;
    private final CategoryInfo categoryInfo;
    private final MerchantInfo merchantInfo;
    private final PromotionRule.CostSlot slot;
    private final ItemStack baseCost;
    /** 原价数量(独立字段,不受 ItemStack 堆叠上限约束),供脚本/监听者读取 */
    private final long baseCount;
    private final double baseRate;
    /** 折扣方向(减少/涨价)与计算类型,供脚本/监听者识别 */
    private final PromotionRule.DiscountDirection direction;
    private final PromotionRule.DiscountCalcType calcType;
    private double discountRate;
    private final List<DiscountResult.DiscountDetail> externalDetails = new ArrayList<>();

    public ShopDiscountEvent(Player player, ShopInfo shopInfo, CategoryInfo categoryInfo,
                             MerchantInfo merchantInfo, PromotionRule.CostSlot slot,
                             ItemStack baseCost, long baseCount, double baseRate,
                             PromotionRule.DiscountDirection direction, PromotionRule.DiscountCalcType calcType) {
        this.player = player;
        this.shopInfo = shopInfo;
        this.categoryInfo = categoryInfo;
        this.merchantInfo = merchantInfo;
        this.slot = slot;
        this.baseCost = baseCost;
        this.baseCount = baseCount;
        this.baseRate = baseRate;
        this.direction = direction;
        this.calcType = calcType;
        this.discountRate = baseRate;
    }

    public void addDiscount(double rate) {
        addDiscount(rate, (String) null);
    }

    public void addDiscount(double rate, String source) {
        this.discountRate += rate;
        this.externalDetails.add(new DiscountResult.DiscountDetail(
                source == null || source.isBlank() ? "viscript_shop.discount.source.external" : source,
                rate, slot));
    }

    /** 来源使用文本组件(支持翻译/样式),序列化为 JSON 存储 */
    public void addDiscount(double rate, net.minecraft.network.chat.Component source) {
        if (source == null) {
            addDiscount(rate, (String) null);
            return;
        }
        this.discountRate += rate;
        this.externalDetails.add(new DiscountResult.DiscountDetail(
                net.minecraft.network.chat.Component.Serializer.toJson(source,
                        net.minecraft.core.RegistryAccess.EMPTY), rate, slot));
    }

    public void setDiscount(double rate) {
        this.discountRate = rate;
    }
}
