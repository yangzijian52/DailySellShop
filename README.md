# DailySellShop

面向 Paper 26.2 的 Java/基岩双端双向商店插件。

- `/ds`：每日或每小时轮换的玩家物品收购商店
- `/dss`：按原版创造模式分类的长期购买商店
- Java 玩家使用箱子 GUI
- Geyser/Floodgate 基岩玩家使用功能对应的表单

当前版本：`2.0.0`

验证状态：已在 Paper 26.2 服务器完成插件加载、Java 菜单、收购与购买交易流程实测。

## 功能

### 收购商店

- 支持每日刷新和每小时刷新两种模式
- 支持固定商品或从完整物品池随机轮换
- 每个商品可配置收购价、显示名和每轮个人限额
- Java 箱子菜单的标题、大小、槽位、Lore、装饰和命令按钮均可配置
- 基岩版表单同步显示商品、价格、出售进度和命令按钮
- 经济入账失败时自动恢复被移除的物品

### 玩家购买商店

- 10 个商品分类：建筑方块、染色方块、自然方块、功能方块、红石方块、工具与实用物品、战斗物品、食物与饮品、原材料、刷怪蛋
- 1477 个可交易商品，按照 Mojang 26.2 创造模式目录分类和排序
- 20 种最高等级普通药水和 43 本最高等级附魔书，统一购买价和收购价为 `10`
- 88 种刷怪蛋独立配置；Boss 和高风险实体默认关闭
- 支持分页、中文名/Material 搜索、预设数量、自定义数量和二次确认
- 自定义输入支持超时、移动、切换世界、死亡和退出取消，输入错误可重试一次
- 背包空间不足时只购买能够放入的数量，不会把剩余物品丢到地面
- 扣款后发放失败会恢复背包并自动退款
- 独立购买日志、成功/失败音效和自定义菜单命令按钮

## 运行环境

| 组件 | 要求 |
| --- | --- |
| 服务端 | Paper `26.2` |
| Java | `25+` |
| 经济 | Vault + 任意 Vault 兼容经济插件 |
| 基岩支持 | Geyser + Floodgate（可选） |

Vault 或经济插件不可用时，插件仍会启用且购买菜单可以浏览，但所有收购和购买交易都会被禁止。

## 安装

1. 安装 Vault 和一个 Vault 兼容经济插件。
2. 如需基岩版表单，安装 Geyser 与 Floodgate。
3. 将 `DailySellShop-2.0.0.jar` 放入服务端 `plugins` 目录。
4. 启动服务器。插件首次启动会生成 `SellShopconfig.yml` 和 `shopconfig.yml`。
5. 修改配置后执行 `/dss reload` 或 `/ds reload`。

两条重载命令都会重新读取两份配置。重载会取消正在等待自定义数量输入的交易。

## 从 1.x 升级

升级前先停止服务器，并备份 `plugins/DailySellShop`。

1. 用 2.0.0 JAR 替换旧插件。
2. 将原 `config.yml` 的自定义菜单和刷新设置迁移到 `SellShopconfig.yml`。
3. 同步发行包中的新增商品、价格、药水和附魔书节点。
4. 根据服务器需求调整新的 `shopconfig.yml`。
5. 启动服务器并检查控制台加载数量，再分别测试一次 `/ds` 出售和 `/dss` 购买。

> **重要：** 插件不会覆盖已经存在的 `SellShopconfig.yml` 或 `shopconfig.yml`。首次迁移时可以把旧 `config.yml` 复制为 `SellShopconfig.yml`，但这只完成文件改名，不会自动合并 2.0.0 新增的商品和价格。升级现有服务器必须手动同步本次配置内容，否则新商品和价格不会生效。

## 配置文件

| 文件 | 用途 | 是否由玩家修改 |
| --- | --- | --- |
| `SellShopconfig.yml` | `/ds` 刷新、限额、菜单、1477 个商品名称和基础价格 | 是 |
| `shopconfig.yml` | `/dss` 分类、GUI、购买规则、覆盖价格、刷怪蛋、音效和日志 | 是 |
| `creative-order.yml` | Mojang 26.2 分类和排序目录 | 否 |
| `plugin.yml` | Bukkit 命令、权限和插件元数据 | 否 |

两份玩家配置均附带完整中文注释。请保持 UTF-8 编码，使用空格缩进，不要使用 Tab。

### 价格规则

- 普通购买价默认等于 `SellShopconfig.yml` 中的收购价
- `settings.buy-price-multiplier` 可全局调整普通购买价格
- `product-overrides.<商品键>.price` 可覆盖单个普通商品的购买价
- 基础资源按获取难度定价，可合成物按 Mojang 26.2 最低合法配方成本计算
- 矿石价格计入时运产出风险，可逆压缩材料保持数量对应，避免合成刷钱
- 刷怪蛋价格在 `shopconfig.yml` 的 `spawn-eggs` 中独立配置

### 菜单按钮

`menu.buttons` 和 `menus.*.buttons` 支持以下动作：

```yaml
commands:
  - "[command] quickmenu open"
  - "[console] say {player} 打开了商店"
  - "[message] &a欢迎使用商店"
  - "[sound] UI_BUTTON_CLICK"
  - "[close]"
```

还支持 `[op] command`，该动作会临时授予玩家 OP 后执行命令，只应在受信任的服务器配置中使用。

常用占位符：`{player}`、`{balance}`、`{item}`、`{material}`、`{price}`、`{amount}`、`{total}`、`{slots}`、`{capacity}`、`{count}`、`{page}`、`{pages}`。

## 命令与权限

| 命令 | 权限 | 说明 |
| --- | --- | --- |
| `/ds` | 无 | 打开收购商店 |
| `/ds reload` | `dailysell.admin` | 重载全部配置 |
| `/dss` | `dailysellshop.shop.use` | 打开玩家购买商店 |
| `/dss search <名称或 Material>` | `dailysellshop.shop.use` | 搜索商品 |
| `/dss open <玩家>` | `dailysellshop.shop.admin` | 为在线玩家打开商店 |
| `/dss reload` | `dailysellshop.shop.admin` | 重载全部配置 |
| `/dss help` | 无 | 显示命令帮助 |

购买权限：

| 权限 | 默认 | 说明 |
| --- | --- | --- |
| `dailysellshop.shop.buy` | 所有玩家 | 购买普通商品 |
| `dailysellshop.shop.spawnegg` | 所有玩家 | 购买已启用的刷怪蛋 |
| 商品配置中的 `permission` | 未设置 | 单个商品或刷怪蛋的附加权限 |

## 交易日志

购买日志默认写入：

```text
plugins/DailySellShop/logs/purchases.log
```

每条记录包含时间、UUID、玩家名、商品键、数量、单价和总价。可在 `shopconfig.yml` 中关闭文件日志或额外输出到控制台。

## 构建

项目使用 Maven，构建需要 JDK 25：

```bash
mvn clean package
```

构建产物：

```text
target/DailySellShop-2.0.0.jar
```

Maven 只对 `plugin.yml` 执行版本变量替换；玩家配置不会经过资源过滤，因此 `${price}` 等菜单占位符会保持原样。

## 更新日志

完整变更见 [CHANGELOG.md](CHANGELOG.md)。

## SpigotMC 发布资料

- [DailySellShop on SpigotMC](https://www.spigotmc.org/resources/dailysellshop.137013/)
- [SpigotMC 资源介绍 BBCode](docs/SPIGOTMC-RESOURCE.md)
- [SpigotMC 完整文档 BBCode](docs/SPIGOTMC-RESOURCE-BBCODE.txt)
- [SpigotMC 手动发布清单](docs/SPIGOTMC_MANUAL_PUBLISHING.md)
- [更新日志](CHANGELOG.md)
- [MIT License](LICENSE)

## 问题反馈

提交问题时请附带：

- Paper 完整版本号
- Java 版本
- DailySellShop、Vault、经济插件、Geyser/Floodgate 版本
- 控制台报错和复现步骤
- 已去除敏感信息的相关配置片段

问题入口：[GitHub Issues](https://github.com/yangzijian52/DailySellShop/issues)
