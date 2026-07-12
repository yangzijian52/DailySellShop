[CENTER][SIZE=7][B]DailySellShop[/B][/SIZE]
[SIZE=4]Dual Java and Bedrock Sell/Buy Shops for Paper 26.2[/SIZE][/CENTER]

[COLOR=#ff4d4d][B]Language notice:[/B][/COLOR] The SpigotMC resource page, documentation and support channel are English-only. Chinese-language support is not provided on SpigotMC.

[SIZE=5][B]About DailySellShop[/B][/SIZE]
DailySellShop is a two-way economy shop plugin built specifically for Paper 26.2. Players can sell rotating items through [B]/ds[/B] and purchase a permanent catalog through [B]/dss[/B]. Java Edition players use protected chest GUIs, while Geyser/Floodgate players receive functionally equivalent forms.

The catalog contains 1,477 tradable entries arranged using the Mojang 26.2 creative inventory order. It includes ordinary survival items, 20 highest-tier normal potions, 43 maximum-level enchanted books and 88 separately configurable spawn eggs.

[SIZE=5][B]Compatibility[/B][/SIZE]
[LIST]
[*][B]Server software:[/B] Paper 26.2 only
[*][B]Java:[/B] 25 or newer
[*][B]Economy bridge:[/B] Vault and a Vault-compatible economy plugin are required for transactions
[*][B]Bedrock forms:[/B] Geyser and Floodgate are optional
[*][B]Live testing:[/B] Plugin loading, Java GUIs, selling and purchasing were tested successfully on a live Paper 26.2 server
[*]Spigot, Purpur, Folia and older Paper versions are not tested or claimed
[/LIST]

[SIZE=5][B]Free Resource[/B][/SIZE]
DailySellShop is published as a [B]free resource for $0.00[/B] under the MIT License. The complete source code is public, the plugin has no license-key or paid-entitlement system, and all release builds can be audited against the GitHub repository. A free listing is the clearest fit for this community economy utility and its open development model.

[SIZE=5][B]Main Features[/B][/SIZE]
[LIST]
[*]Rotating player sell shop with DAILY and HOURLY refresh modes
[*]Permanent player buy shop with ten creative-style categories
[*]Java chest GUIs and Floodgate Bedrock forms
[*]1,477 tradable catalog entries in Mojang creative inventory order
[*]20 highest-tier normal potions and 43 maximum-level enchanted books
[*]88 individually configurable spawn eggs; dangerous entities disabled by default
[*]Product pages, Chinese/Material search, preset quantities and custom quantities
[*]Second confirmation step before money is withdrawn
[*]Fully configurable menu titles, sizes, slots, icons, lore, fillers and command buttons
[*]Global buy-price multiplier and per-product price/category/permission overrides
[*]Configurable success/failure sounds and UTF-8 purchase logs
[/LIST]

[SIZE=5][B]Transaction Safety[/B][/SIZE]
[LIST]
[*]Every purchase revalidates the product, category, permission, amount, balance and inventory capacity
[*]Duplicate submissions are blocked while a transaction is processing
[*]Money is withdrawn before delivery; failed delivery restores the inventory snapshot and refunds the payment
[*]Inventory overflow is never dropped on the ground
[*]A failed sell-side economy deposit restores the removed items and does not consume the sell limit
[*]Potions and enchanted books are matched using exact item metadata
[*]Shift-clicking, number keys, double-clicking and dragging cannot remove GUI display items
[*]Custom amount input is cancelled on timeout, excessive movement, world change, death, logout or reload
[/LIST]

[SIZE=5][B]Catalog and Pricing[/B][/SIZE]
Regular buy prices are read from the sell catalog and multiplied by a configurable global multiplier. Individual products can override their final buy price. Base resources are priced by acquisition difficulty, craftable products follow the lowest supported Mojang 26.2 recipe cost, and rare non-craftable loot receives a rarity floor. Reversible storage recipes and Fortune-mineable ores are balanced to reduce straightforward buy/craft/sell arbitrage.

[SIZE=5][B]Dependencies[/B][/SIZE]
[LIST]
[*][B]Vault:[/B] required for all money transactions
[*][B]Vault-compatible economy plugin:[/B] required for balances, withdrawals and deposits
[*][B]Geyser + Floodgate:[/B] optional, required only for Bedrock form support
[/LIST]
Without an available economy provider, the plugin remains enabled and the buy catalog can be viewed, but buying and selling are disabled.

[SIZE=5][B]Links[/B][/SIZE]
[LIST]
[*][URL=https://github.com/yangzijian52/DailySellShop]Source Code and README[/URL]
[*][URL=https://github.com/yangzijian52/DailySellShop/releases]Downloads[/URL]
[*][URL=https://github.com/yangzijian52/DailySellShop/releases/tag/2.0.0]DailySellShop 2.0.0[/URL]
[*][URL=https://github.com/yangzijian52/DailySellShop/issues]Bug Reports and Support[/URL]
[*][URL=https://github.com/yangzijian52/DailySellShop/blob/main/LICENSE]MIT License[/URL]
[/LIST]

[SIZE=5][B]Important Notes[/B][/SIZE]
[LIST]
[*]This plugin targets Paper 26.2 and Java 25. Do not advertise it as tested on Spigot or older Minecraft/Paper versions.
[*]Existing configuration files are never overwritten. Upgrading from 1.x requires manually merging the new products, prices and configuration nodes.
[*]Default configuration comments and most in-game administrator messages are Chinese.
[*]The Java transaction flow was live-tested. Bedrock forms require a separate Geyser/Floodgate environment and should be verified by the server owner.
[*]The [B][op][/B] configurable button action temporarily grants operator status and must only be used in trusted configuration files.
[*]Per-player sell progress is held in memory and resets after a full server restart.
[/LIST]
