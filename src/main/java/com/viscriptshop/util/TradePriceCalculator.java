package com.viscriptshop.util;

import com.viscriptshop.ShopRegistries;
import com.viscriptshop.event.neoforge.ShopBonusEvent;
import com.viscriptshop.event.neoforge.ShopDiscountEvent;
import com.viscriptshop.gui.data.CategoryInfo;
import com.viscriptshop.gui.data.DiscountResult;
import com.viscriptshop.gui.data.MerchantInfo;
import com.viscriptshop.gui.data.PromotionRule;
import com.viscriptshop.gui.data.ShopInfo;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

/**
 * 商店价格唯一权威计算入口。
 *
 * <p>原价存于 {@link MerchantInfo},折后价按玩家上下文现算:
 * 规则引擎(商店内配置的促销规则)→ {@link ShopDiscountEvent}(脚本/模组可叠加/覆盖)
 * → {@code finalCount = max(1, floor(baseCount * (1 + rate)))}。
 * 双路径(客户端展示 / 服务端扣费)必须调用本类,保证一致。
 */
public class TradePriceCalculator {

    /** 原版村民折扣的 priceMultiplier 常量(多数交易为 0.05) */
    private static final double DYNAMIC_REPUTATION_MULTIPLIER = 0.05;

    /**
     * 村民声望提供者扩展点。默认不命中 {@code REPUTATION} 条件。
     * 村民商店模组可注册:服务端读村民实体声望,客户端读上下文缓存。
     */
    private static BiFunction<Player, ShopInfo, Integer> reputationProvider = (player, shopInfo) -> 0;

    public static void setReputationProvider(BiFunction<Player, ShopInfo, Integer> provider) {
        if (provider != null) {
            reputationProvider = provider;
        }
    }

    public static int getReputation(Player player, ShopInfo shopInfo) {
        return reputationProvider.apply(player, shopInfo);
    }

    /**
     * 规则聚合结果:多条命中折扣合并后的总折率、方向与计算类型。
     *
     * @param rate     聚合后的总折率(带符号:负=打折,正=涨价)
     * @param direction 首个命中规则的方向(减少/涨价)
     * @param calcType  首个命中规则的计算类型
     */
    private record RuleAggregation(double rate, PromotionRule.DiscountDirection direction,
                                   PromotionRule.DiscountCalcType calcType) {
    }

    /**
     * 计算单个成本槽的折后价(权威入口,双端共用)。
     *
     * <p>流程:
     * <ol>
     *   <li>取原价数量:ITEM_A/ITEM_B 槽从 {@code merchantInfo} 的独立 count 字段读取
     *       (不受 ItemStack 堆叠上限约束),其余场景取 {@code baseCost} 栈数量;</li>
     *   <li>聚合商店内置 DISCOUNT 规则得到基础折率(含动态声望/村庄英雄);</li>
     *   <li>广播 {@link ShopDiscountEvent} 允许脚本/模组叠加或覆盖折率,并收集外部明细;</li>
     *   <li>应用统一公式 {@code finalCount = max(1, floor(baseCount * (1 + rate)))},
     *       最低 1 保证交易仍可支付。</li>
     * </ol>
     *
     * @param player      计算折扣的玩家上下文(声望/权限/效果等条件来源)
     * @param slot        目标成本槽(ALL/ITEM_A/ITEM_B)
     * @param baseCost    原价成本栈(数量仅作 fallback)
     * @return 包含原价、折后价、总折率与全部明细的 {@link DiscountResult}
     */
    public static DiscountResult calculate(Player player, ShopInfo shopInfo, CategoryInfo categoryInfo,
                                           MerchantInfo merchantInfo, PromotionRule.CostSlot slot, ItemStack baseCost) {
        List<DiscountResult.DiscountDetail> details = new ArrayList<>();
        RuleAggregation aggregation = aggregateDiscountRules(player, shopInfo, categoryInfo, merchantInfo, slot, details);

        // 原价数量取自独立 count 字段(ItemStack 数量恒为 1,不受堆叠上限约束)
        long baseCount;
        if (merchantInfo != null && (slot == PromotionRule.CostSlot.ITEM_A || slot == PromotionRule.CostSlot.ITEM_B)) {
            baseCount = slot == PromotionRule.CostSlot.ITEM_A
                    ? merchantInfo.getItemACount()
                    : merchantInfo.getItemBCount();
        } else {
            baseCount = baseCost == null ? 0 : baseCost.getCount();
        }

        ShopDiscountEvent event = new ShopDiscountEvent(player, shopInfo, categoryInfo, merchantInfo, slot,
                baseCost, baseCount, aggregation.rate(), aggregation.direction(), aggregation.calcType());
        NeoForge.EVENT_BUS.post(event);
        details.addAll(event.getExternalDetails());

        int finalCount = Math.max(1, (int) Math.floor(baseCount * (1.0 + event.getDiscountRate())));
        return new DiscountResult(slot, (int) baseCount, finalCount, event.getDiscountRate(),
                aggregation.direction(), aggregation.calcType(), details);
    }

    /**
     * 返回应用折扣后的成本栈(数量为折后价),不修改原栈。
     *
     * <p>供结算/展示层直接取用:先 {@link #calculate} 现算折后价,
     * 再复制原栈并覆写数量,原始栈保持数量 1 不变。
     *
     * @return 折后成本栈(数量 = {@code finalCount})
     */
    public static ItemStack apply(Player player, ShopInfo shopInfo, CategoryInfo categoryInfo,
                                  MerchantInfo merchantInfo, PromotionRule.CostSlot slot, ItemStack baseCost) {
        DiscountResult result = calculate(player, shopInfo, categoryInfo, merchantInfo, slot, baseCost);
        ItemStack copy = baseCost.copy();
        copy.setCount(result.getFinalCount());
        return copy;
    }

    /**
     * 计算买赠(内置 BUY_GET 规则 + {@link ShopBonusEvent}),返回最终赠品明细。
     *
     * <p>内置规则公式:{@code 赠品总数 = floor(购买数量 / 买满阈值) × 每满赠数量};
     * 未指定赠品时默认赠送商品自身({@code itemResult})。内置命中与事件贡献合并为最终列表。
     *
     * @param buyCount 当前购物车购买数量
     * @return 赠品明细列表(可能为空)
     */
    public static List<DiscountResult.BonusDetail> calculateBonus(Player player, ShopInfo shopInfo,
                                                                  CategoryInfo categoryInfo,
                                                                  MerchantInfo merchantInfo, int buyCount) {
        ShopBonusEvent event = new ShopBonusEvent(player, shopInfo, categoryInfo, merchantInfo, buyCount);

        if (shopInfo != null && shopInfo.getPromotionRules() != null) {
            for (PromotionRule rule : shopInfo.getPromotionRules()) {
                if (!rule.isEnabled() || rule.getType() != PromotionRule.PromotionType.BUY_GET) continue;
                if (rule.getBuyThreshold() <= 0 || rule.getGiftCount() <= 0) continue;
                if (!conditionsMet(player, shopInfo, categoryInfo, merchantInfo, rule.getConditions())) continue;

                int giftTotal = (buyCount / rule.getBuyThreshold()) * rule.getGiftCount();
                if (giftTotal <= 0) continue;
                ItemStack gift = rule.getGiftItem().isEmpty() ? merchantInfo.getItemResult() : rule.getGiftItem();
                if (gift.isEmpty()) continue;
                event.addGift(gift, giftTotal, "viscript_shop.discount.source.rule");
            }
        }

        NeoForge.EVENT_BUS.post(event);
        return event.getBonusDetails();
    }

    /**
     * 聚合商店内所有命中的 DISCOUNT 规则,合并为单一折率。
     *
     * <p>每条命中规则产出一个带符号的有效折率(负=打折,正=涨价),按
     * {@link PromotionRule.DiscountAggregationMode} 合并:
     * <ul>
     *   <li>{@code ADD}:直接求和(默认,多重折扣叠加);</li>
     *   <li>{@code MAX}:取最大(打折中最轻的生效);</li>
     *   <li>{@code MIN}:取最小(打折中最狠的生效);</li>
     *   <li>{@code MULTIPLY}:连乘,{@code 总率 = Π(1 - 各率) - 1}(语义上等效"先折再折")。</li>
     * </ul>
     *
     * <p>未命中任何规则时返回零折率(原价)。
     */
    private static RuleAggregation aggregateDiscountRules(Player player, ShopInfo shopInfo, CategoryInfo categoryInfo,
                                                          MerchantInfo merchantInfo, PromotionRule.CostSlot slot,
                                                          List<DiscountResult.DiscountDetail> details) {
        List<Double> hits = new ArrayList<>();
        PromotionRule.DiscountDirection hitDirection = PromotionRule.DiscountDirection.REDUCE;
        PromotionRule.DiscountCalcType hitCalcType = PromotionRule.DiscountCalcType.PERCENT_REDUCE;
        boolean hasHit = false;
        if (shopInfo != null && shopInfo.getPromotionRules() != null) {
            for (PromotionRule rule : shopInfo.getPromotionRules()) {
                if (!rule.isEnabled() || rule.getType() != PromotionRule.PromotionType.DISCOUNT) continue;
                if (!rule.matchesSlot(slot)) continue;
                if (!conditionsMet(player, shopInfo, categoryInfo, merchantInfo, rule.getConditions())) continue;

                String source = rule.getId().isEmpty() ? "viscript_shop.discount.source.rule" : rule.getId();
                if (rule.isDynamicReputation()) {
                    // 原版村民折扣语义:specialPriceDiff = -floor(声望 × priceMultiplier) + 村庄英雄折扣,
                    // 按商品数量折算为折扣率;两项都无折扣时规则不生效
                    int reputation = getReputation(player, shopInfo);
                    boolean hasHero = player.hasEffect(MobEffects.HERO_OF_THE_VILLAGE);
                    if (reputation == 0 && !hasHero) continue;

                    int baseCount = (int) (slot == PromotionRule.CostSlot.ITEM_A
                            ? merchantInfo.getItemACount()
                            : merchantInfo.getItemBCount());
                    if (baseCount <= 0) continue;

                    if (reputation != 0) {
                        double repRate = -(double) Math.floor(reputation * DYNAMIC_REPUTATION_MULTIPLIER) / baseCount;
                        hits.add(repRate);
                        details.add(new DiscountResult.DiscountDetail(source, repRate, slot));
                        if (!hasHit) { hasHit = true; }
                    }
                    if (hasHero) {
                        int amplifier = player.getEffect(MobEffects.HERO_OF_THE_VILLAGE).getAmplifier();
                        double heroFactor = 0.3 + 0.0625 * amplifier;
                        int heroDiff = -Math.max((int) Math.floor(heroFactor * baseCount), 1);
                        double heroRate = (double) heroDiff / baseCount;
                        hits.add(heroRate);
                        details.add(new DiscountResult.DiscountDetail(
                                "viscript_shop.discount.source.hero", heroRate, slot));
                        if (!hasHit) { hasHit = true; }
                    }
                    continue;
                }

                // 普通折扣规则:rate 为正幅度,方向/类型决定符号与计算
                PromotionRule.DiscountDirection direction = rule.getDirection() == null
                        ? PromotionRule.DiscountDirection.REDUCE
                        : rule.getDirection();
                PromotionRule.DiscountCalcType calcType = rule.getCalcType() == null
                        ? PromotionRule.DiscountCalcType.PERCENT_REDUCE
                        : rule.getCalcType();
                double abs = Math.abs(rule.getDiscount());
                int baseCount = (int) (slot == PromotionRule.CostSlot.ITEM_A
                        ? merchantInfo.getItemACount()
                        : merchantInfo.getItemBCount());
                double effective;
                if (calcType == PromotionRule.DiscountCalcType.FLAT_REDUCE) {
                    // 绝对值折算为比例(基于原价)
                    double flat = baseCount > 0 ? abs / baseCount : abs;
                    effective = direction == PromotionRule.DiscountDirection.INCREASE ? flat : -flat;
                } else {
                    // PERCENT_DIRECT / PERCENT_REDUCE:比例,方向决定正负
                    effective = direction == PromotionRule.DiscountDirection.INCREASE ? abs : -abs;
                }
                hits.add(effective);
                details.add(new DiscountResult.DiscountDetail(source, effective, slot));
                if (!hasHit) {
                    hasHit = true;
                    hitDirection = direction;
                    hitCalcType = calcType;
                }
            }
        }

        if (hits.isEmpty()) return new RuleAggregation(0.0, PromotionRule.DiscountDirection.REDUCE,
                PromotionRule.DiscountCalcType.PERCENT_REDUCE);
        PromotionRule.DiscountAggregationMode mode = shopInfo == null
                || shopInfo.getDiscountAggregation() == null
                ? PromotionRule.DiscountAggregationMode.ADD
                : shopInfo.getDiscountAggregation();
        return new RuleAggregation(switch (mode) {
            case MAX -> hits.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
            case MIN -> hits.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
            case MULTIPLY -> hits.stream().mapToDouble(v -> 1.0 - v).reduce(1.0, (a, b) -> a * b) - 1.0;
            default -> hits.stream().mapToDouble(Double::doubleValue).sum();
        }, hitDirection, hitCalcType);
    }

    /**
     * 条件列表全部满足才视为命中(AND 语义);空列表恒命中。
     *
     * @return 全部条件满足返回 {@code true}
     */
    private static boolean conditionsMet(Player player, ShopInfo shopInfo, CategoryInfo categoryInfo,
                                         MerchantInfo merchantInfo, List<PromotionRule.DiscountCondition> conditions) {
        if (conditions == null || conditions.isEmpty()) return true;
        for (PromotionRule.DiscountCondition condition : conditions) {
            if (!conditionMet(player, shopInfo, categoryInfo, merchantInfo, condition)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 判定单个折扣条件(按 {@link PromotionRule.ConditionType} 分发):
     * <ul>
     *   <li>{@code HAS_FLAG}:玩家 Money 数据中的旗帜标记是否包含指定值;</li>
     *   <li>{@code HAS_PERMISSION}:权限等级(0~4)是否达标;</li>
     *   <li>{@code HAS_EFFECT}:是否拥有指定药水效果;</li>
     *   <li>{@code REPUTATION}/{@code PLAYER_XP_LEVEL}:数值与阈值按比较符(GE/LE/EQ/BETWEEN)判定;</li>
     *   <li>{@code SHOP_MATCH}/{@code CATEGORY_MATCH}/{@code MERCHANT_MATCH}:名称/ID 精确匹配;</li>
     *   <li>{@code ITEM_IN_CART}:购物车内是否存在指定物品(结果/成本槽均可);</li>
     *   <li>{@code GAME_TIME}:世界昼夜时间比较,支持 "min,max" 区间写法。</li>
     * </ul>
     */
    private static boolean conditionMet(Player player, ShopInfo shopInfo, CategoryInfo categoryInfo,
                                        MerchantInfo merchantInfo, PromotionRule.DiscountCondition condition) {
        if (condition == null || condition.getType() == null) return true;
        String value = condition.getValue() == null ? "" : condition.getValue();
        return switch (condition.getType()) {
            case HAS_FLAG -> player.getData(ShopRegistries.MONEY).getFlags().contains(value);
            case HAS_PERMISSION -> {
                try {
                    yield player.hasPermissions(Integer.parseInt(value));
                } catch (NumberFormatException e) {
                    yield false;
                }
            }
            case HAS_EFFECT -> {
                ResourceLocation effectId;
                try {
                    effectId = ResourceLocation.parse(value);
                } catch (Exception e) {
                    yield false;
                }
                Holder<MobEffect> effect = BuiltInRegistries.MOB_EFFECT.getHolder(effectId).orElse(null);
                yield effect != null && player.hasEffect(effect);
            }
            case REPUTATION -> compareValue(getReputation(player, shopInfo), condition);
            case PLAYER_XP_LEVEL -> compareValue(player.experienceLevel, condition);
            case SHOP_MATCH -> shopInfo != null && value.equals(shopInfo.getName());
            case CATEGORY_MATCH -> categoryInfo != null && value.equals(categoryInfo.getId());
            case MERCHANT_MATCH -> merchantInfo != null && value.equals(merchantInfo.getId());
            case ITEM_IN_CART -> cartContains(shopInfo, value);
            case GAME_TIME -> {
                long dayTime = player.level().getDayTime();
                if (value.contains(",")) {
                    String[] parts = value.split(",");
                    try {
                        long min = Long.parseLong(parts[0].trim());
                        long max = Long.parseLong(parts[1].trim());
                        yield dayTime >= min && dayTime <= max;
                    } catch (NumberFormatException e) {
                        yield false;
                    }
                } else {
                    try {
                        yield compareLong(dayTime, condition, Long.parseLong(value));
                    } catch (NumberFormatException e) {
                        yield false;
                    }
                }
            }
        };
    }

    /**
     * 判定购物车内是否存在指定物品 ID。
     *
     * <p>遍历商店全部分类/商品,任一商品处于购物车(购买数量 > 0)且
     * 结果槽或成本槽 A/B 的物品 ID 与目标一致即视为命中。
     */
    private static boolean cartContains(ShopInfo shopInfo, String itemId) {
        if (shopInfo == null || itemId == null || itemId.isBlank()) return false;
        for (CategoryInfo category : shopInfo.getCategoryInfos()) {
            for (MerchantInfo merchant : category.getMerchants()) {
                if (merchant.getBuyCount().intValue() <= 0) continue;
                if (sameItem(merchant.getItemResult(), itemId)
                        || sameItem(merchant.getItemA(), itemId)
                        || sameItem(merchant.getItemB(), itemId)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** 按注册表 ID(命名空间:路径)比较物品是否相同;空栈恒不匹配。 */
    private static boolean sameItem(ItemStack stack, String itemId) {
        if (stack == null || stack.isEmpty()) return false;
        return stack.getItem().builtInRegistryHolder().key().location().toString().equals(itemId);
    }

    /**
     * 数值条件比较(声望/经验等级等):
     * {@code BETWEEN} 时从条件 value 解析 "min,max" 区间,其余按比较符判定。
     */
    private static boolean compareValue(double actual, PromotionRule.DiscountCondition condition) {
        PromotionRule.ComparisonOp op = condition.getOp() == null
                ? PromotionRule.ComparisonOp.GE
                : condition.getOp();
        return switch (op) {
            case GE -> actual >= condition.getThreshold();
            case LE -> actual <= condition.getThreshold();
            case EQ -> Math.abs(actual - condition.getThreshold()) < 1e-6;
            case BETWEEN -> {
                String value = condition.getValue() == null ? "" : condition.getValue();
                if (!value.contains(",")) yield false;
                String[] parts = value.split(",");
                try {
                    double min = Double.parseDouble(parts[0].trim());
                    double max = Double.parseDouble(parts[1].trim());
                    yield actual >= min && actual <= max;
                } catch (NumberFormatException e) {
                    yield false;
                }
            }
        };
    }

    /** 长整型比较(游戏时间等):{@code BETWEEN} 不支持,其余按比较符判定。 */
    private static boolean compareLong(long actual, PromotionRule.DiscountCondition condition, long threshold) {
        PromotionRule.ComparisonOp op = condition.getOp() == null
                ? PromotionRule.ComparisonOp.GE
                : condition.getOp();
        return switch (op) {
            case GE -> actual >= threshold;
            case LE -> actual <= threshold;
            case EQ -> actual == threshold;
            case BETWEEN -> false;
        };
    }
}
