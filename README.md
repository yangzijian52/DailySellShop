# DailySellShop

一个适用于 Paper 服务端的每日收购商店插件，支持 Java 玩家 GUI 和基岩版 Floodgate 表单。

## 当前版本

- 插件版本：`1.1.2`
- Paper API：`26.1.1.build.29-alpha`
- Java 要求：`25+`

## 主要功能

- 支持每日刷新或每小时刷新两种模式
- 支持 Java 版箱子 GUI
- 支持基岩版 Floodgate 表单
- 集成 Vault 经济系统
- 支持玩家每日出售限额

## 运行要求

- Paper `26.1.x`
- Java `25+`
- Vault
- 任意 Vault 兼容经济插件
- Floodgate（如需基岩版表单支持）

## 命令

- `/ds`：打开收购界面
- `/ds reload`：重载配置
- 权限节点：`dailysell.admin`

## 当前状态

- 已升级到官方当前最新可用 `paper-api 26.1.1.build.29-alpha`
- 已切换到 `Java 25`
- 已修正源码中的颜色与文本处理
- 当前已完成本地构建验证
- 当前尚未完成服内实测

## 构建

```bash
mvn clean package
```
