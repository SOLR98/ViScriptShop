package com.viscriptshop.gui.data;

import com.lowdragmc.lowdraglib2.configurator.ConfiguratorParser;
import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.configurator.accessors.ItemStackAccessor;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.configurator.ui.NumberConfigurator;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.utils.PersistedParser;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;

/**
 * 保存商品的实际物品及其独立图标配置。
 *
 * <p>实际物品参与交易、校验和库存处理，图标配置只决定客户端如何展示该物品。
 * 此类型用于不需要物品组件匹配规则的商品位置，例如 {@code itemResult}。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MerchantItemInfo implements IConfigurable, IPersistedSerializable {
    public static final StreamCodec<ByteBuf, MerchantItemInfo> STREAM_CODEC;
    public static final Codec<MerchantItemInfo> CODEC;

    @Configurable(name = "viscript_shop.data.merchant.item.actual")
    private ItemStack item = ItemStack.EMPTY;

    /** 物品数量(独立存储,不受 ItemStack 堆叠上限约束,上限 Long.MAX_VALUE) */
    @Configurable(name = "viscript_shop.data.merchant.item.count")
    private long count = 1;

    @Configurable(showName = false, subConfigurable = true, subFlattenConfigurable = true)
    private MerchantItemDisplay display = new MerchantItemDisplay();

    static {
        CODEC = PersistedParser.createCodec(MerchantItemInfo::new);
        STREAM_CODEC = PersistedParser.createStreamCodec(MerchantItemInfo::new);
    }

    @Override
    public void beforeSerialize() {
        // 数量不序列化进 ItemStack(堆叠上限会引发序列化崩溃),统一归一为 1
        if (item != null && !item.isEmpty() && item.getCount() != 1) {
            item = item.copyWithCount(1);
        }
    }

    @Override
    public void afterDeserialize() {
        // 兼容旧版本数据:旧版数量直接存于 ItemStack 内(无独立 count 字段),
        // 加载时提取到 count(自定义物品堆叠),避免数量静默丢失。
        // 新版自身保存的数据 item 数量恒为 1,不会误触发。
        if (item != null && !item.isEmpty() && item.getCount() > 1) {
            this.count = item.getCount();
            item.setCount(1);
        }
    }

    public MerchantItemInfo(ItemStack item, MerchantItemDisplay display) {
        this.item = item;
        this.display = display;
    }

    @Override
    public void buildConfigurator(ConfiguratorGroup father) {
        getItem();
        getDisplay();
        addItemConfigurator(father);
        addFieldConfigurator(father, MerchantItemInfo.class, "display")
                .addClass("merchant-item-display-settings");
    }

    /**
     * 物品配置器(槽 + 物品搜索 + 组件 + 数量输入):槽与数量输入框直接绑定
     * 自定义数量 {@link #count}(ItemStack 数量恒为 1 不再存储),数量修改经
     * {@link #setItem(ItemStack)} 同步到 count。
     */
    private void addItemConfigurator(ConfiguratorGroup father) {
        try {
            Field field = MerchantItemInfo.class.getDeclaredField("item");
            ConfiguratorGroup group = (ConfiguratorGroup) new ItemStackAccessor().create(
                    "viscript_shop.data.merchant.item.actual",
                    this::getItemWithCount,
                    this::setItem,
                    true,
                    field,
                    this
            );
            replaceCountConfigurator(group);
            group.addClass("merchant-item-actual");
            father.addConfigurator(group);
        } catch (NoSuchFieldException exception) {
            throw new IllegalStateException("Missing merchant item field: item", exception);
        }
    }

    /**
     * 将 ItemStackAccessor 自带的 int 数量输入框替换为绑定 {@link #count} 的
     * long 输入框(范围 0 ~ {@link Long#MAX_VALUE},与自定义物品堆叠一致)。
     */
    private void replaceCountConfigurator(ConfiguratorGroup group) {
        List<Configurator> configurators = group.getConfigurators();
        for (int i = 0; i < configurators.size(); i++) {
            Configurator configurator = configurators.get(i);
            if (configurator instanceof NumberConfigurator
                    && "ldlib.gui.editor.configurator.count".equals(configurator.getLabel().getString())) {
                group.removeConfigurator(configurator);
                group.addConfiguratorAt(
                        new NumberConfigurator("viscript_shop.data.merchant.item.count",
                                this::getCount,
                                value -> setCount(value.longValue()),
                                count,
                                true)
                                .setType(ConfigNumber.Type.LONG)
                                .setRange(0L, Long.MAX_VALUE)
                                .setWheel(1),
                        i);
                return;
            }
        }
    }

    /**
     * 返回携带自定义数量的展示栈(上限 {@link Integer#MAX_VALUE},ItemStack 数量上限),
     * 使 Inspector 槽的堆叠数字与数量输入框直接反映 {@link #count}。
     */
    private ItemStack getItemWithCount() {
        ItemStack stack = getItem();
        if (stack.isEmpty() || count <= 0) {
            return stack;
        }
        return count > Integer.MAX_VALUE
                ? stack.copyWithCount(Integer.MAX_VALUE)
                : stack.copyWithCount((int) count);
    }

    /**
     * 获取参与交易的实际物品。
     *
     * @return 非 {@code null} 的实际物品堆
     */
    public ItemStack getItem() {
        if (item == null) {
            item = ItemStack.EMPTY;
        }
        return item;
    }

    /**
     * 设置实际物品:ItemStack 数量不再存储(恒为 1),真实数量全部提取到独立的
     * {@link #count} 字段(自定义物品堆叠,不受堆叠上限约束)。
     */
    public void setItem(ItemStack item) {
        if (item == null || item.isEmpty()) {
            this.item = ItemStack.EMPTY;
            this.count = 0;
            return;
        }
        this.item = item.copyWithCount(1);
        this.count = item.getCount();
    }

    /**
     * 获取只影响客户端图标的展示配置。
     *
     * @return 非 {@code null} 的图标展示配置
     */
    public MerchantItemDisplay getDisplay() {
        if (display == null) {
            display = new MerchantItemDisplay();
        }
        return display;
    }

    /**
     * 为指定字段创建一个配置组件。
     *
     * @param father 配置组件的父分组
     * @param declaringClass 声明目标字段的类
     * @param fieldName 目标字段名称
     * @return 新增到父分组的配置组件
     */
    protected Configurator addFieldConfigurator(ConfiguratorGroup father,
                                                Class<?> declaringClass,
                                                String fieldName) {
        try {
            Field field = declaringClass.getDeclaredField(fieldName);
            int previousSize = father.getConfigurators().size();
            ConfiguratorParser.createFieldConfigurator(
                    field,
                    father,
                    declaringClass,
                    new HashMap<>(),
                    this
            );
            if (father.getConfigurators().size() <= previousSize) {
                throw new IllegalStateException("No configurator created for merchant item field: " + fieldName);
            }
            return father.getConfigurators().getLast();
        } catch (NoSuchFieldException exception) {
            throw new IllegalStateException("Missing merchant item field: " + fieldName, exception);
        }
    }
}
