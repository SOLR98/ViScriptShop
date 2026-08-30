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

    public void addGift(ItemStack item, int count) {
        addGift(item, count, null);
    }

    public void addGift(ItemStack item, int count, String source) {
        if (item == null || item.isEmpty() || count <= 0) return;
        bonusDetails.add(new DiscountResult.BonusDetail(item.copy(), count,
                source == null || source.isBlank() ? "viscript_shop.discount.source.external" : source));
    }

    public void setGifts(List<DiscountResult.BonusDetail> gifts) {
        bonusDetails.clear();
        if (gifts != null) {
            bonusDetails.addAll(gifts);
        }
    }

    public void removeGift(ItemStack item) {
        bonusDetails.removeIf(detail -> detail.getItem().is(item.getItem())
                && net.minecraft.world.item.ItemStack.isSameItemSameComponents(detail.getItem(), item));
    }
}
