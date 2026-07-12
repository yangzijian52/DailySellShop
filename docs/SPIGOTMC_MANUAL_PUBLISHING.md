# DailySellShop SpigotMC 手动发布清单

## 发布结论

| 项目 | 建议值 |
| --- | --- |
| Resource type | **Free** |
| Price | **$0.00 USD** |
| License | **MIT License** |
| Version | **2.0.0** |
| Category | **Economy** |
| Upload file | `target/DailySellShop-2.0.0.jar` |
| JAR size | `122307` bytes（以最终校验结果为准） |
| JAR SHA-256 | `715f6fbc42ee4dbee4825444449a10dce39b14a717c6b7dc0aa3e36e796b1286` |

选择免费资源的理由：项目完整源码已经公开，本次补充 MIT License，插件没有许可证密钥、服务器授权、付费账户或高级权益系统。以免费资源发布与当前开放开发和支持方式一致，也便于服主审计经济交易代码。

## 发布文件

- SpigotMC 上传文件：`target/DailySellShop-2.0.0.jar`
- GitHub Release：[DailySellShop V2.0.0](https://github.com/yangzijian52/DailySellShop/releases/tag/2.0.0)
- GitHub 源码：[yangzijian52/DailySellShop](https://github.com/yangzijian52/DailySellShop)
- MIT License：[LICENSE](../LICENSE)
- 简短资源介绍 BBCode：[SPIGOTMC-RESOURCE.md](SPIGOTMC-RESOURCE.md)
- 完整文档 BBCode：[SPIGOTMC-RESOURCE-BBCODE.txt](SPIGOTMC-RESOURCE-BBCODE.txt)

## Suggested SpigotMC Fields

| Field | Suggested value |
| --- | --- |
| Resource title | `DailySellShop` |
| Tag line | `Dual Java/Bedrock sell and buy shops for Paper 26.2` |
| Version | `2.0.0` |
| Category | `Economy` |
| Price | `Free / $0.00` |
| Tested server | `Paper 26.2` |
| Java | `25 or newer` |
| Required dependencies | `Vault and a Vault-compatible economy plugin` |
| Optional dependencies | `Geyser and Floodgate` |
| Source code | `https://github.com/yangzijian52/DailySellShop` |
| Support URL | `https://github.com/yangzijian52/DailySellShop/issues` |
| License | `MIT` |

SpigotMC 的版本选择框如果尚未提供 `26.2`，应选择页面可用的最接近选项，并在标题、Compatibility 和 Important Notes 中继续明确写 `Paper 26.2 only`。不要勾选或宣称未经测试的 Spigot、Purpur、Folia 或旧版 Paper 兼容性。

## Important Warnings

1. 必须在资源页和完整文档中保留原句：`The SpigotMC resource page, documentation and support channel are English-only. Chinese-language support is not provided on SpigotMC.`
2. 插件默认配置注释和多数管理员消息是中文，但 SpigotMC 资源页、文档和工单支持只使用英文。
3. 上传到 SpigotMC 的文件是 JAR，不是包含配置与文档的 GitHub ZIP。
4. 插件仅声明 Paper 26.2 + Java 25；不要宣称原生 Spigot 兼容。
5. Vault 和一个 Vault 兼容经济插件是实际交易所必需的；缺少时只能浏览购买目录。
6. Geyser/Floodgate 是可选依赖；本次已确认 Java 菜单和交易实测，基岩表单应在对应环境单独复核。
7. 升级已有服务器必须手动合并 `SellShopconfig.yml` 和 `shopconfig.yml`，插件不会覆盖旧配置。
8. `[op]` 菜单动作只能写入受信任配置。
9. 不要上传 `.idea`、`*.iml`、`target` 目录、临时文件或开发工具。

## Manual Publish Steps

1. 打开 [SpigotMC Add Resource](https://www.spigotmc.org/resources/add)。
2. 选择 **Free Resource** 和 **Economy** 分类。
3. 按 Suggested SpigotMC Fields 填写标题、版本、依赖和外部链接。
4. 将 `docs/SPIGOTMC-RESOURCE.md` 全文粘贴到资源介绍页。
5. 将 `docs/SPIGOTMC-RESOURCE-BBCODE.txt` 全文粘贴到完整文档/补充说明区域。
6. 上传 `target/DailySellShop-2.0.0.jar`。
7. 根据实际界面选择 Paper 26.2 对应版本；不要勾选未测试平台。
8. 添加真实 Java GUI 截图；如有经过验证的 Floodgate 环境，再添加基岩表单截图。
9. 发布前预览 BBCode，检查标题、列表、代码块和链接是否正常。
10. 再次核对 Resource type 为 Free、价格为 $0.00、版本为 2.0.0。
11. 发布后从 SpigotMC 下载一次 JAR，并对照本文与 GitHub Release 的 SHA-256。
12. 在 GitHub README 或 Release 中补充最终 SpigotMC 资源链接。

## 发布后人工维护

- SpigotMC 的上传、截图、分类选择、版本下拉框和最终提交必须由项目所有者手动完成。
- 后续问题只通过英文处理，并优先引导用户在 GitHub Issues 提交完整环境和日志。
- 仅文档变更不要提高插件版本；代码、`pom.xml`、`plugin.yml` 或 JAR 行为变化时再创建新版本与 Release。
