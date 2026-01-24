# DailySellShop 💰

![Java](https://img.shields.io/badge/Java-21-orange)
![Platform](https://img.shields.io/badge/Platform-Paper_1.21-blue)
![License](https://img.shields.io/badge/License-MIT-green)

**DailySellShop** 是一个专为 Minecraft Paper 1.21+ 服务器设计的每日收购商店插件。
它完美支持 **Java版** 和 **基岩版 (Geyser/Floodgate)** 双端交互，并集成了 **Vault** 经济系统。

## ✨ 核心特性

*   **📅 每日刷新机制**：严格按照 **北京时间 (Asia/Shanghai) 00:00** 重置收购列表和玩家限额。
*   **🎲 两种商店模式**：
    *   **随机模式**：每天从配置池中随机抽取 N 个物品进行收购，每天都不一样！
    *   **固定模式**：关闭随机功能，常驻显示所有配置的物品。
*   **📱 双端完美适配**：
    *   **Java版**：自动打开箱子 GUI 界面。
    *   **基岩版**：自动识别 Floodgate 玩家，弹出原生的表单 (Form) 界面，操作更流畅。
*   **🔒 严格的安全校验**：采用原子操作检查物品数量和扣除逻辑，防止刷钱或物品丢失。
*   **📊 每日限额**：每个物品都可以单独设置每人每日的可出售上限，控制服务器经济平衡。

## 🛠️ 前置要求

在安装此插件之前，请确保你的服务器已安装以下插件：

1.  **Vault** (必需)
2.  **经济插件** (必需，如 EssentialsX, CMI, Economy 等)
3.  **Floodgate** (可选，如果需要基岩版表单支持则必须安装)

## 📥 安装步骤

1.  下载 `DailySellShop-1.0-SNAPSHOT.jar`。
2.  将文件放入服务器的 `plugins` 文件夹。
3.  重启服务器。
4.  在 `plugins/DailySellShop/config.yml` 中配置你的物品价格。

## 🎮 命令与权限

| 命令 | 别名 | 描述 | 权限 |
| :--- | :--- | :--- | :--- |
| `/ds` | 无 | 打开今日收购菜单 | 无 (默认玩家可用) |
| `/ds reload` | 无 | 重载配置文件并刷新物品池 | `dailysell.admin` (默认 OP) |

## ⚙️ 配置文件说明 (config.yml)

插件支持两种模式，通过 `daily-rotation.enabled` 切换。

```yaml
# 每日重置时区
timezone: "Asia/Shanghai"

# 随机轮换设置
daily-rotation:
  enabled: true     # [true]=开启每日随机抽取; [false]=显示下方 static-items 固定列表
  amount: 18        # 仅在随机模式下生效：每天抽取多少个物品

# [模式 A] 固定列表配置 (当 enabled: false 时读取这里)
static-items:
  COBBLESTONE:
    price: 0.5
    daily-limit: 1000
    name: "&7圆石"


# [模式 B] 随机池配置 (当 enabled: true 时读取这里)
items:
  DIAMOND:
    price: 100.0
    daily-limit: 64
    name: "&b钻石"

```


## 🏗️ 如何构建 (Build)

```yaml
本项目使用 Maven 进行管理。如果你想自己修改源码，请确保开发环境为 JDK 21。
git clone https://github.com/你的用户名/DailySellShop.git
cd DailySellShop
mvn clean package

```
构建完成后，插件将生成在 target/ 目录下。

## 🤝 贡献与反馈
欢迎提交 Issue 或 Pull Request！
如果你喜欢这个插件，请点一个 ⭐ Star！




