# 移植进度（WIP）—— BepHaxAddon / stardust → Minecraft 26.1.2

> 这份是中途存档，记录当前状态与剩余工作；完成后会改写为正式 PORT-NOTES。

## 现状（本轮结束时）
| 项目 | 源码规模 | 起始编译错误 | 当前编译错误 |
|---|---|---|---|
| BepHaxAddon（起点 1.21.11） | 231 文件 / 46,697 行 | 4686 | **460** |
| stardust（起点 1.21.4） | 168 文件 / 31,934 行 | 2794 | **345** |

## 已完成的自动化（工具在 `E:\Files\tools\portkit\`）
1. `migrate_mappings.py`：obf 名做桥，yarn↔官方映射（1.21.4 与 1.21.11 两套映射都在 `.mappings\`），整体改名类名/简单名/mixin 描述符。
2. `classhier.py`：从 26.1.2 真实 jar 解析继承链（修继承来的成员用）。
3. `compile.py`：复用 MAIN 端真实 `-cp` + 本地 mod jar 的 javac 校验环（含从 fabric-loader 里解出的 MixinExtras）。
4. `fix_classes.py` / `fix_symbols.py`：按 (类,符号) 精确定位改名（描述符精确配对，避免同名串号）。
5. `fix_rules.py`：通用改名规则表（背包访问器、Vec3/Identifier/Registry 静态工厂、距离比较、注册键、
   实体遍历、movement 包类、`sendPacket→send`、`handleInventoryMouseClick→handleContainerInput`、
   `displayClientMessage→sendSystemMessage`、`GuiGraphics→GuiGraphicsExtractor` 等）。
6. `fix_imports.py`：yarn 名残留 → 官方名；挪包/大小写类 → 真实类表；ChunkPos 私有字段 → 访问器。
7. `fix_nested.py`：嵌套类名（`ServerboundMovePlayerPacket.Full → .PosRot` 这类）。
8. `fix_aw.py`：access widener 换 `official` 命名空间 + 类/成员改名。

## 剩余工作（按类）
- **API 被 26.1 重做的部分**（要改代码逻辑，不是改名）：
  - GUI 渲染：`GuiGraphics` → `GuiGraphicsExtractor`（方法也变了：`drawString`→`text`、`drawBorder`→`outline`）；
  - 物品：`DiggerItem`/`ArmorItem` 已不存在（26.1 用 data component/`Equippable`）；
  - 书本：`BookViewScreen.Contents` 没了、`BookEditScreen` 相关 API 变化；
  - 音乐：`MusicInfo` 没了（`MusicManager` 体系重做）；
  - 文本：`ClickEvent`/`HoverEvent` 变成 record；
  - 包/命令：`then(LiteralArgumentBuilder<ClientSuggestionProvider>)`（Meteor 命令 API 变化）；
  - 杂项：`Reference<Item>`→`Item`、`ClientboundDisconnectPacket`→`DisconnectionDetails`、
    `Entity.getRotationVector()` 返回 `Vec2`、`SignBlock.getWoodType()`→`type()`。
- **mixin 复核**：stardust ~70 个、BepHax 若干，需逐个对着 26.1.2 真实成员核注入目标（`verify_mixins.py`）。
- **构建验证**：`gradlew build`（Loom 1.17 / JDK 25 / Gradle 9.6.1，镜像已配好）。
- **实机冒烟 + fork 推送**。

## 复现
```powershell
$env:PORT_PROJ='E:\Files\BepHaxAddon-26.1.2'; $env:PORT_MCVER='1.21.11'
python E:\Files\tools\portkit\fix_rules.py
python E:\Files\tools\portkit\compile.py
python E:\Files\tools\portkit\port_loop.py 8
```

## 2026-09-28 02:30 存档（重启 dsh 前）
- 本轮自动化已收敛到平台期：**BepHax 213 错 / stardust 274 错**（自动 pass 每轮几乎无改动）。
- 剩余属于「26.1 重做 API」清单（见上）：GUI extractor 方法族、packet 记录化、`PlayerFaceRenderer`、`Box.from` 等零散点。
- 工具链：`E:\Files\tools\portkit\`（grind.py 串起全部 pass；`PORT_PROJ` / `PORT_MCVER` / `PORT_MAPS` 环境变量指定项目与映射）。
- 恢复方式：设好环境变量 → `python E:\Files\tools\portkit\grind.py 3` 看收敛情况，再按 docs 里清单人工改。


## mixin 注入点对齐（26.1）

> 2026-09-28 by AI。口径：`tools/portkit/verify_mixins2.py` → `build/mixin-verify.txt` **miss=0**（88 文件）。
> 另有两个补充核对器（放在 `archive/sessions/2026-09/scripts/`，**未改动 portkit**）：
> `bephax_v3.py`（抓全部 `method="..."` 规格，支持嵌套类/泛型父类）、
> `bephax_check_shadows.py`（核对 `@Shadow` 字段/方法 + 显式 `@Accessor/@Invoker`）。

### 一、为什么 54 条 MISS 清 0 只是及格线：原核对器有三类盲区

1. **`method=` 规格漏抓**：`verify_mixins2.py` 只认 `@Inject\s*\(\s*method=`，
   `@Inject(at = @At(...), method = "...")`（参数顺序相反）和全限定写法
   `method = "Lnet/minecraft/world/entity/LivingEntity;tickMovement()V"` 整条不进报告
   → `LivingEntityMixin`、`KeyBindingMixin` 从未被核对过。补跑 `bephax_v3.py` 后又抓出 3 处。
2. **`@Shadow` 完全不查**：字段/方法名对不上不进 MISS 报告，但每个都是运行时硬崩
   （`InvalidMixinException: @Shadow method ... was not located in the target class`）。
   本轮共修 **约 40 处、横跨 22 个文件**（`x/y→leftPos/topPos`、`getScreenHandler→getMenu`、
   `handler→menu`、`currentStack→lastToolHighlight`、`jumpingCooldown→noJumpDelay`、
   `shooter→attachedToEntity`、`id→name`(KeyMapping)、`getEntityWorld→level()`、
   `isOnGround→onGround()`、`isInPose→hasPose`、`sounds→list`、`splashText→splash` …）。
3. **隐式 `@Accessor`**：不带值的 `@Accessor` 按方法名推导字段名（`getLevelCost()` → `levelCost`），
   26.1 字段改名后推导不会报 MISS，只在实机抛
   `InvalidAccessorException: No candidates were found matching levelCost:...`。
   修 4 处：`AnvilMenu.cost`、`AnvilScreen.name`、`BookEditScreen.page`、`MultiLineEditBox.textField`。

### 二、改法类别

**A. GUI 渲染族全部 `render* → extract*`（渲染上下文类型是 `GuiGraphicsExtractor`）**

| 旧 | 26.1 |
|---|---|
| `Screen/ChatScreen/AbstractContainerScreen/InventoryScreen/BookViewScreen/BookEditScreen/PauseScreen/SplashRenderer.render` | `extractRenderState(GuiGraphicsExtractor,int,int,float)` |
| `AbstractContainerScreen.drawSlot` | `extractSlot(GuiGraphicsExtractor,Slot,int,int)` |
| `Gui.renderHeldItemTooltip` | `extractSelectedItemName` |
| `Gui.renderHotbar` | `extractItemHotbar` |
| `Gui.renderOverlay` | `extractTextureOverlay` |
| `Gui.renderChat` | `extractChat` |
| `BossHealthOverlay.renderBossBar` | `extractBar` |
| `PauseScreen.initWidgets` + `GridLayout.refreshPositions()` | `createPauseMenu` + `GridLayout.arrangeElements()` |
| `GuiGraphicsExtractor.drawItem(ItemStack,int,int)` | `item(ItemStack,int,int)` |
| `GuiGraphicsExtractor.drawItemWithoutEntity(...)` | `item(ItemStack,int,int,int)` |
| `GuiGraphicsExtractor.drawCenteredTextWithShadow` | `centeredText` |

**B. 玩家 / 输入 / 交互**

- `LocalPlayer.tickMovement` → `aiStep`（3 处；GrimV3 的 `@ModifyExpressionValue` 目标
  `isUsingItem()` → `isSlowDueToUsingItem()`，26.1 物品使用减速由它统一决定）
- `LocalPlayer.pushOutOfBlocks` → `moveTowardsClosestSpace(double,double)`
- `LocalPlayer.isSneaking` → `isCrouching`
- `Player.isPushedByFluids` → `isPushedByFluid`；`Player.clipAtLedge` → `isStayingOnGroundSurface`
- `MultiPlayerGameMode`：`stopUsingItem`→`releaseUsingItem(Player)`（handler 需补 `Player` 形参）、
  `updateBlockBreakingProgress`→`continueDestroyBlock`、`clickSlot`→`handleContainerInput`、
  `interactBlock`→`useItemOn`；`currentBreakingProgress`→`destroyProgress`
- `Minecraft.render`→`renderFrame(boolean)`、`doItemUse`→`startUseItem`、`getMusicInstance`→`getSituationalMusic`
- `LivingEntity.tickMovement`→`aiStep`、`isGliding`→`isFallFlying`、
  `jumpingCooldown`→`noJumpDelay`、`getJumpVelocity`→`getJumpPower`
- `ClientInput.getMovementInput` → `getMoveVector`

**C. 方块 / 实体 / 物品**

- `BlockBehaviour.BlockStateBase.calcBlockBreakingDelta` → `getDestroyProgress`
- `DoorBlock.playOpenCloseSound` → 私有 `playSound(Entity,Level,BlockPos,boolean)`（注入私有方法）
- `InstrumentItem.playSound` → 私有静态 `play(Level,Player,Instrument)`
- `Entity`：`pushAwayFrom`→`push(Entity)`、`getEntityWorld`→`level()`、`isOnGround`→`onGround()`、
  `getStepHeight`→`maxUpStep()`、`isInPose`→`hasPose`、`fall`→`checkFallDamage`
- `FireworkRocketEntity.shooter` → `attachedToEntity`
- `ItemStack`：`getName`→`getHoverName`、`getFormattedName`/`toHoverableText`→`getStyledHoverName`；
  组件判断 `contains(DataComponentType)` → `has(DataComponentType)`
- `Inventory`：`selectedSlot`→`selected`、`main`→`items`

**D. packet 记录化 / 杂项**

- `ClientboundSetEntityDataPacket`：`trackedValues`→`packedItems`
- `ClientboundSetEntityMotionPacket`：`entityId`→`id`、`velocity`→`movement`
- `Options.viewDistance`→`renderDistance`；`MusicManager`：`timeUntilNextSong`→`nextSongDelay`、`current`→`currentMusic`
- `KeyMapping.id`→`name`、`isPressed`→`isDown`；`ChatComponent.client`→`minecraft`
- `Gui.chatHud`→`chat`；`AbstractContainerScreen.getScreenHandler`→`getMenu`、`handler`→`menu`
- `BundleMouseActions.sendPacket`→`toggleSelectedBundleItem`，其发包调用
  `ClientPacketListener.sendPacket(Packet)`→`send(Packet)`
- `GrindstoneMenu.grind`→`removeNonCursesFrom`、`transferEnchantments`→`mergeEnchantsFrom`
- `ClientLevel.getPendingUpdateManager`→`getBlockStatePredictionHandler`
- 书本：`BookEditScreen.finalizeBook`→`appendPageToBook`、`openPreviousPage`→`pageBack`、
  `openNextPage`→`pageForward`、`updatePage`→`updatePageContent`；
  `BookViewScreen.updatePageButtons`→`updateButtonVisibility`；
  `InventoryScreen.handledScreenTick`→`containerTick`

### 三、特殊处理（挂载点变更，非纯改名）

1. **`GameRendererMixin`（NoHurtCam）**：`tiltViewWhenHurt(PoseStack,float)` 在 26.1 拆成
   `bobHurt(CameraRenderState,PoseStack)`（受击镜头抖动）与 `bobView(...)`（行走视角摇晃）。
   语义上 NoHurtCam 要的是"去掉受击抖动"，因此挂 **`bobHurt`**（不是名字更像的 `bobView`），
   handler 形参顺序改为 `(CameraRenderState, PoseStack, CallbackInfo)`。
2. **`meteor/PeekScreenMixin`**：`PeekScreen` 是 Meteor 自身类，26.1 起不再覆写 `Screen.init()`，
   继承链上的 `init` 不能作为注入点。改为在 `extractBackground`（原 `drawBackground`）TAIL 里
   惰性创建搜索框（`if (bephax$searchField != null) return;`），原 `init`/`drawBackground` 两个注入
   合并到同一方法，功能等价（ItemSearchBar 在 Meteor 的 peek 界面里照常可用）。
3. **`MixinEntity`**：`Entity.interact(Player,InteractionHand)` 在 26.1 变
   `interact(Player,InteractionHand,Vec3)`，`stepOnBlock` 已整体删除。这两条本就是**未被使用**的
   @Shadow 声明，直接摘除，避免运行时 shadow 校验失败（无功能损失）。
4. **`ItemStackMixin`**：`getFormattedName`/`toHoverableText` 合并为 `getStyledHoverName()`，
   两条注入（HEAD cancellable 改写 + `@At(INVOKE ItemStack.has(DataComponentType))` 改局部变量）
   都改挂到它上面。
5. **`AbstractSignEditScreenMixin`**：`setCurrentRowMessage` 现为 `private void setMessage(String)`，
   private 目标不能声明成 `abstract` shadow → 写成
   `@Shadow private void setMessage(String message) { throw new AssertionError(); }`（Mixin 丢弃方法体）；
   同时 `currentRow→line`、`blockEntity→sign`、`selectionManager→signField`，
   未使用的 `@Shadow close()` 直接摘除。
6. **`LivingEntityMixin`**：`jumpingCooldown` 的新名 `noJumpDelay` 与本类已有的 module 字段同名
   → module 字段改名 `bephax$noJumpDelayModule`。
7. **`require = 0` 类豁免**：`SplashTextRendererMixin` 的两条注入带 `require = 0`，
   目标不存在时只是静默跳过、不会崩游戏。已按新名改正，但这类注入在历史核对中容易"看起来没崩=没问题"，
   排查时要单独确认。

### 四、遗留风险 / 降级项

- **与 stardust 的 Meteor mixin 冲突**：两者都 mixin Meteor 的同一个方法，日志里会出现
  `Method overwrite conflict for onDeactivate ... previously written by bep.hax.mixin.meteor.AutoLogMixin`
  —— 同时装 bephax + stardust 时其中一个实现会被跳过。需要二选一或改用不同注入点。
- **NoSlow 输入倍率静默降级**：26.1 的 `ClientInput` 只剩 `keyPresses`/`moveVector`，
  不再有 `movementForward/Sideways`，`InputAccessor` 的 setter 是空实现
  （`bep/hax/mixin/InputMixin.java`）。`ClientPlayerEntityMixin` 里"使用物品时放大移动输入"
  的逻辑因此实际不生效（注入点保留、不再崩），待 26.1 提供可写移动向量后恢复。
- `SplashTextRendererMixin` 的 `@ModifyArg` 依赖
  `@At(INVOKE GuiGraphicsExtractor.centeredText(Font,String,III)V)` 的 `index = 4`，
  26.2 若再动签名需重新核对。
- **懒加载特性**：Fabric mixin 按类加载时应用，主菜单只能覆盖启动路径；
  GUI 类（inventory/chat/pause/book/sign/container）的注入要等对应界面被打开才会真正执行，
  深度验收需进世界逐个开界面。

### 五、静态核对清 0 之后：只有实机才能暴露的四类启动期崩溃

> 方法：**隔离实例**跑真机（`archive/sessions/2026-09/scripts/bephax-verify3.ps1`）——
> 把共享实例的 `mods/` 用 robocopy 复制到 `D:\mc\bephax-verify`（排除 stardust，**不改共享目录**），
> 从 `LatestLaunch.bat` 派生一份指向该 gameDir 的启动脚本，`--quickPlaySingleplayer` 直接进世界，
> 再用 AttachThreadInput 把 MC 窗口拉到前台截图。每一步的日志/崩溃报告都单独落盘。

1. **`@At` 目标描述符失效（第 4 类盲区）** —— 方法名对得上、但方法体里根本没有那条调用：
   - `FireworkRocketEntityMixin`：`LivingEntity.getVelocity()Vec3` → **`getDeltaMovement()Vec3`**；
     `FireworkRocketEntity.explodeAndRemove(ServerLevel)` → **`explode(ServerLevel)`**。
   - 现象：`InjectionError: Critical injection failure: Callback method spoofRotationVector ... failed
     injection check, (0/1) succeeded. Scanned 0 target(s).`（在 Bootstrap 阶段 transform
     `FireworkRocketEntity` 时炸，整局起不来）。
2. **`method=` 自带描述符的签名/包名漂移（第 5 类盲区）** —— 只写方法名不会暴露：
   - `ChatComponent`：`addMessage(Component, MessageSignature, GuiMessageTag)` 拆成
     **`addPlayerMessage(...)`**；`addMessage(Component)` 拆成
     **`addClientSystemMessage(Component)` / `addServerSystemMessage(Component)`**（两条都挂了）。
   - 参数类型包名迁移：`net.minecraft.client.GuiMessageTag` →
     **`net.minecraft.client.multiplayer.chat.GuiMessageTag`**（描述符里写旧包名同样匹配不到）。
3. **启动早期路径上 `Modules.get()` 仍为 null（Minecraft 构造期）**：
   - `KeyMapping.isDown`（26.1 由 `isPressed` 改名）在 `KeyMapping.releaseAll()` 里被调用，
     那时 Meteor 的 `Modules` 还没初始化 → `NullPointerException: Cannot invoke "Modules.get(Class)"`。
   - 修法：`KeyBindingMixin` 先 `Modules.get()` 判空再取模块；
     `LivingEntityMixin`/`EntityMixin` 的 `getEfly()`/`getNoJumpDelay()` 同样加空守卫；
     `ItemStackMixin` 补 `antiToS == null` 判空。
   - 这一条 javac 与全部静态核对器都发现不了，只能靠实机。
4. **mod 初始化期访问未绑定的文本组件**：
   - `MusicTweaks` / `RocketMan` 的字段初始化 `private String rcc = StardustUtil.rCC();`
     在 `Bep.onInitialize()` 阶段执行 → `ExceptionInInitializerError ... NullPointerException:
     Components not bound yet`（26.1 的文本组件要等 Bootstrap 绑定完）。
   - 修法：字段改 `= null`，新增 `private String rcc()` 懒取（`MusicTweaks.java`、`RocketMan.java`）。
   - 通用规则：**字段/构造器/静态块里不要碰 `StardustUtil.*` / `Component.*` / `Items.*` /
     `BuiltInRegistries.*`**，统一挪进 `onActivate()` 或首次使用处。

排查顺序建议（省时间）：`crash-reports\crash-*.txt` 里的 `Description:` 行 → 若是 `Initializing game`
就看 `Caused by` 的第一帧（往往是字段初始化/构造器）；若是 `Bootstrap` 就看是不是某个 mixin 的
`@At`/描述符失效；两者都不是才回去看 `latest.log` 的 `Mixin apply for mod X failed`。
