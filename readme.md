[![Ask DeepWiki](https://deepwiki.com/badge.svg)](https://deepwiki.com/zhenshiz/ViScriptShop)
# ViScriptShop

服务于ViScriptNpc的商店模组

模组前置：ldlib2

模组联动：KubeJS，精妙背包，超越维度

## 指令

/viscript\_shop editor 打开商店编辑器

/viscript\_shop open <商店id>  <商店title> 打开商店

/viscript\_shop reload 刷新商店指令补全

/viscript\_shop reload <商店id> 重置商店信息

/viscript\_shop setStage <商店id> <阶段值> 设置商店阶段

## 功能简述

参考至原版以及cnpc的商店系统，以物换物的形式支持最多2个物品换1个物品的兑换方式。支持用可视化界面来创建一个商店，支持新增，编辑和删除商品信息。购买物品还可以获得经验并自定义执行一段指令(可以用;分隔来执行多条指令)。可以通过商店阶段值(大于等于0的整数)来控制商店中能出售的物品，默认0，所有小于等于商店阶段值的物品解锁出售。

## 商店信息

* List<商品> 商店中所有商品信息

* int 商店阶段值

## 商品信息

* ItemStack 商品A(ItemStack.Empty)

* ItemStack 商品B(ItemStack.Empty)

* ItemStack 兑换商品(ItemStack.Empty)

* int 经验(0)

* String 执行的指令("")

* int 阶段值(0)

  ## 事件系统

  为KubeJS添加可用事件

* 打开商店的事件

* 关闭商店的事件

* 商品购买判断前的事件

* 商品购买失败的事件

* 商品购买成功的事件

* 商店界面tick事件

## 未来可能添加的功能列表

1. 添加物品分类
2. 添加通用货币商店
3. 修改缓存设计

## Bug

* 无法序列化带附魔组件的物品（需要等待ldlib2更新）

