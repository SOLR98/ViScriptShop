package com.viscriptshop.event.neoforge;

import com.viscriptshop.gui.data.CategoryInfo;
import com.viscriptshop.gui.data.DiscountResult;
import com.viscriptshop.gui.data.MerchantInfo;
import com.viscriptshop.gui.data.ShopInfo;
import lombok.Getter;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;

import java.util.ArrayList;
import java.util.List;

/**
 * 买赠计算事件。内置 BUY_GET 规则命中值先写入 {@link #bonusDetails},
 * 监听者可追加/覆盖/移除赠品。事件贡献独立成条,可带来源文本。
 *
 * <p>双端触发:客户端(展示)与服务端(权威结算)各触发一次。
 */
@Getter
public class ShopBonusEvent extends Event {
    private final Player player;
    private final ShopInfo shopInfo;
    private final CategoryInfo categoryInfo;
    private final MerchantInfo merchantInfo;
    private final int buyCount;
    private final List<DiscountResult.BonusDetail> bonusDetails = new ArrayList<>();

    public ShopBonusEvent(Player player, ShopInfo shopInfo, CategoryInfo categoryInfo,
                          MerchantInfo merchantInfo, int buyCount) {
        this.player = player;
        this.shopInfo = shopInfo;
        this.categoryInfo = categoryInfo;
        this.merchantInfo = merchantInfo;
        this.buyCount = buyCount;
    }

    /** 追加赠品,来源归入默认"外部赠品"(无翻译键时直接显示该字符串)。 */
    public void addGift(ItemStack item, int count) {
        addGift(item, count, (String) null);
    }

    /**
     * 追加赠品并指定来源文本(翻译键优先,无翻译时显示键本身)。
     *
     * @param item   赠品物品(空栈/负数量忽略)
     * @param count  赠品数量(须 > 0)
     * @param source 来源标识(翻译键或纯文本)
     */
    public void addGift(ItemStack item, int count, String source) {
        if (item == null || item.isEmpty() || count <= 0) return;
        bonusDetails.add(new DiscountResult.BonusDetail(item.copy(), count,
                source == null || source.isBlank() ? "viscript_shop.discount.source.external" : source));
    }

    /** 追加赠品,来源使用文本组件(支持翻译/样式),序列化为 JSON 存储 */
    public void addGift(ItemStack item, int count, net.minecraft.network.chat.Component source) {
        if (source == null) {
            addGift(item, count, (String) null);
            return;
        }
        if (item == null || item.isEmpty() || count <= 0) return;
        bonusDetails.add(new DiscountResult.BonusDetail(item.copy(), count,
                net.minecraft.network.chat.Component.Serializer.toJson(source, net.minecraft.core.RegistryAccess.EMPTY)));
    }

    /** 整体覆盖赠品列表(清空后追加给定列表)。 */
    public void setGifts(List<DiscountResult.BonusDetail> gifts) {
        bonusDetails.clear();
        if (gifts != null) {
            bonusDetails.addAll(gifts);
        }
    }

    /** 按物品类型与组件移除匹配的赠品(用于监听者主动削减赠品)。 */
    public void removeGift(ItemStack item) {
        bonusDetails.removeIf(detail -> detail.getItem().is(item.getItem())
                && net.minecraft.world.item.ItemStack.isSameItemSameComponents(detail.getItem(), item));
    }
}
