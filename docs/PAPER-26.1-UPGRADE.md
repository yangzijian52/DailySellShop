# DailySellShop Paper 26.1 升级说明

## 概要

本文档记录 `DailySellShop` 对 `Paper 26.1` 的兼容升级情况。

- 项目版本：`1.1.2`
- 目标 Paper API：`26.1.1.build.29-alpha`
- 所需 Java 版本：`25+`
- 升级日期：`2026-04-09`

## 本次调整

- `paper-api` 从 `1.21.11-R0.1-SNAPSHOT` 升级到 `26.1.1.build.29-alpha`
- Java 编译目标从 `21` 升级到 `25`
- `plugin.yml` 改为自动读取 Maven 版本
- 修正源码里的颜色处理与中文提示
- 重写中文 README 并补充升级说明文档

## 说明

- 截至 `2026-04-09`，PaperMC 官方 Maven 元数据中 `paper-api` 的 `latest` 和 `release` 都是 `26.1.1.build.29-alpha`
- 当前已完成本地构建验证
- 当前尚未完成服内测试，因此暂时不能标记为正式可用
