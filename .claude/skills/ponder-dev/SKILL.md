---
name: ponder-dev
description: 为 Create 附属模组编写 Ponder 教程场景——蓝图处理、场景代码、翻译、注册的完整手册。当任务涉及 Ponder / 思索 / 教程动画时使用。
---

# Ponder 开发手册（Create 6.x / ponder 1.0.x，MC 1.21.1）

本 skill 总结自 Chemical Industry 模组的实战经验。Ponder 已独立成库
（`net.createmod.ponder`，jar-in-jar 打包在 Create 6.0.9 内），场景 = **蓝图 nbt + Java 剧本代码**。

## 1. 完整流程

```
① 蓝图 nbt → ② 场景代码 → ③ 翻译 → ④ 注册 → ⑤ 编译复制
```

1. **蓝图**：玩家在游戏里用**结构方块**搭场景保存（或脚本生成空场景），
   导出路径：`saves/<世界>/generated/minecraft/structures/<名字>.nbt`
   复制到 `assets/<modid>/ponder/<storyboard路径>.nbt`
2. **场景代码**：写场景类（见 templates/SceneTemplate.java），按剧本编排
3. **翻译**：必须加 lang，否则 Ponder 文字显示**空白**（见 §4）
4. **注册**：PonderPlugin + 主类构造里 `PonderIndex.addPlugin()`（见 §5）
5. 编译 → 复制 jar 到 mods → 游戏里对物品按 **W** 预览

## 2. 蓝图格式

- Ponder 蓝图 = **标准结构方块 nbt**（gzip 压缩）：`size` / `entities` / `blocks` / `palette` / `DataVersion`
- 方块 `pos` = 蓝图内**相对坐标**，即场景代码里 `util.grid().at(x,y,z)` 的坐标
- 场景代码里没有的方块可用 `setBlock` **动态放置**（如点燃的燃烧室）
- 常用脚本（scripts/ 目录，用 paddle_env 的 python）：
  - `parse_scene.py <nbt路径>` → 打印 palette + 所有方块坐标/状态（读玩家场景用）
  - `shift_scene_y.py <输入.nbt> <输出.nbt> [y偏移量=1]` → 所有方块 y 坐标整体上移
    （玩家把结构**直接放地上（y0 起）**时 +1；改完用 parse_scene.py 回读验证）
  - `build_scene.py [场景名...]` → **代码直接生成场景蓝图**（没有玩家搭场景时的方案）：
    脚本里定义 `size` + `blocks` 列表（方块 = 注册名 + 属性字典），自动收集 palette 写入 nbt
  - `make_empty_scene.py [x y z]` → 生成全空气空场景蓝图

### ⚠️ 结构 nbt 硬格式（脚本生成时必读，踩坑无数）

| 字段 | 要求 |
|------|------|
| `size` | **List\<Int\>**（t=9 元素类型 3），**不是 IntArray(t=11)** |
| `blocks[].pos` | 同上：**List\<Int\>**（t=9 元素类型 3） |
| `entities` | 空 List 元素类型 = **0（End）** |
| `DataVersion` | 3955（1.21.1） |
| List 元素 | (值, 元素类型) 元组 |
| Compound 字段值 | (类型, 值) 元组，如 `'Name': (8, "mod:block")` |
| palette 的 Properties | `(10, {属性名: (8, 值)})`——必须带 Compound 类型标记 |
| 方块属性 | 可省略默认值（如 fluidized_bed 的 lit 不写 = false） |

> **为什么 size/pos 必须是 List\<Int\>**：MC 的 `StructureTemplate.getList("pos", 3)` 只认 Int 列表，
> IntArray 会被解析为空 → size=0 → **场景一个方块都不显示**（4 场景全空白事故）。
> 生成后务必用 parse_scene.py 回读验证顶层结构（size 类型应为 9）。

## 3. 场景代码 API 速查（ponder 1.0.x）

场景方法签名：`public static void xxx(SceneBuilder builder, SceneBuildingUtil util)`
**直接用 `builder`**（ponder 原生接口），不要包 CreateSceneBuilder（它的 world() 返回 void 版本）。
唯一例外：`setKineticSpeed` 只在 `new CreateSceneBuilder(builder).world()` 上有（Create 扩展指令）。

| 指令 | 签名/用法 | 说明 |
|------|-----------|------|
| title | `builder.title("场景名", "标题")` | 翻译 key 前缀用第一个参数 |
| configureBasePlate | `builder.configureBasePlate(0, 0, 宽度)` | 场景底板 |
| idle | `builder.idle(tick)` | 停顿 |
| **showSection** | `builder.world().showSection(选区, 方向)` | 显示区块，**返回 void**，不可移动 |
| **showIndependentSection** | `builder.world().showIndependentSection(选区, 方向)` | 返回 `ElementLink<WorldSectionElement>`，**可移动/隐藏** |
| **moveSection** | `builder.world().moveSection(link, new Vec3(0,1,0), 20)` | 移动已显示区块（抬升/平移动画） |
| hideIndependentSection | `builder.world().hideIndependentSection(link, 方向)` | 淡出隐藏（**只隐藏，不恢复世界方块**） |
| destroyBlock | `builder.world().destroyBlock(pos)` | 破坏方块（带动画）；**必须删数据原位**（见 §机制） |
| setBlock | `builder.world().setBlock(pos, 状态, false)` | 动态放方块（主世界会渲染，但可能被场景清理——**尽量不用**） |
| setBlocks | `builder.world().setBlocks(选区, 状态, 粒子)` | 批量替换 |
| setKineticSpeed | `new CreateSceneBuilder(builder).world().setKineticSpeed(选区, ±速度)` | **Create 扩展**；正负控制方向 |
| toggleRedstonePower | `builder.world().toggleRedstonePower(选区)` | 拉红石 |
| showText | `builder.overlay().showText(时长).text("内容").attachKeyFrame().placeNearTarget().pointAt(vec)` | 文字讲解 + 进度条节点 |
| showControls | `builder.overlay().showControls(vec, Pointing.DOWN, 40).rightClick()` | 鼠标手势提示 |
| showOutline | `builder.overlay().showOutline(PonderPalette.RED, new Object(), 选区, 时长)` | 高亮（GREEN/BLUE/RED…） |
| 选区 | `util.select().layer(0)` / `.position(pos)` / `.fromTo(a, b)` / `.add(...)` 合并 / `.substract(...)` 相减 | Selection |
| 坐标 | `util.grid().at(x,y,z)`；`util.vector().centerOf(pos)` / `.topOf(x,y,z)` / `.blockSurface(pos, dir)` | |

注意：showSection 分步出现时，把每步的区块用 `showIndependentSection` 获取 link，
后面统一 `moveSection` 即可实现"整体抬升"。

## 3.5 机制真相（反编译 ponder 1.0.81 确认，写代码前先想清楚）

1. **独立元素 = Selection + 渲染偏移**：`WorldSectionElementImpl` 持有选区，渲染时 PoseStack 整体平移。
   **方块数据留在原位**——moveSection 只移动渲染！
2. **元素渲染实时读世界方块**（不是快照）——隐藏淡出动画期间，如果 setBlock 改了元素选区内的方块，
   元素会把新方块画在偏移位置（"幽灵方块"）。**setBlock 必须在元素淡出完成后执行**。
3. **元素渲染盖住世界方块**（重叠时元素优先）——setBlock 的方块被元素盖住 = 看不见。
4. **主世界方块（setBlock）本身会渲染**（PonderLevel 渲染 blocks map），不需要再"显示"一次
   （之前误加 showIndependentSection 导致双渲染）。
5. **hide = setVisible(false) + 淡出**，不恢复方块数据。
6. **setKineticSpeed = modifyBlockEntityNBT(Speed)**——直接改 BE 的 Speed NBT，正负控制方向。
7. **未显示过的方块 = 不存在**（不渲染）。蓝图里有但没 showSection = 看不见。
8. **最稳原则：动态方块（燃烧室/大齿轮等）全部搭进蓝图**，代码只做分区显示 + 文本。
   这符合"玩家搭场景、代码写剧本"的分工，彻底避开 setBlock 清理/重叠问题。

## 4. 翻译规则（重要！不加 = 文字空白）

Ponder 按 key 查翻译：`<modid>.ponder.<title第一个参数>.<header|text_N>`

```json
"chemical_industry.ponder.electrolyzer_structure.header": "搭建电解槽",
"chemical_industry.ponder.electrolyzer_structure.text_1": "合法的电解槽由三个横排方块组成",
"chemical_industry.ponder.electrolyzer_structure.text_2": "它有 3×1、3×2、3×3 三种规格",
```

- `text_N` 按场景内 `showText` **出现顺序**编号（1,2,3…）
- zh_cn.json + en_us.json **都要加**
- `text()` 参数写中文原文作为 fallback，双保险

## 5. 注册（时机关键！）

```java
// ChemicalPonderPlugin implements PonderPlugin（net.createmod.ponder.api.registration）
public String getModId() { return "modid"; }
public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
    helper.forComponents(ResourceLocation.fromNamespaceAndPath("modid", "机器物品注册名"))
        .addStoryBoard("机器/场景", 场景类::方法);
}
```

**必须在主类构造器里注册**（和 Create 一致），不能放 FMLClientSetupEvent（太晚，索引里找不到场景）：

```java
if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) {
    net.createmod.ponder.foundation.PonderIndex.addPlugin(new ChemicalPonderPlugin());
}
```

## 6. 坑清单（都是实战踩过的）

1. `showSection` 返回 **void**，要移动必须用 `showIndependentSection` 拿 link
2. `CreateSceneBuilder.world()` 是 void 包装，用原生 `builder`；`setKineticSpeed` 只在 CreateSceneBuilder 上
3. 蓝图路径 = `addStoryBoard` 参数 + `.nbt`（如 `"electrolyzer/structure"` → `ponder/electrolyzer/structure.nbt`）
4. 每个 storyboard **独立加载自己的蓝图**；多个 storyboard 可共用同一 nbt（但要**同步更新**，曾因不同步导致部分场景旧布局）
5. 玩家保存的结构方块 nbt 在 `generated/minecraft/structures/`（不是 structures/ 根目录）
6. 翻译 key 用 **title 的第一个参数**（不是 storyboard 名）
7. 场景方块从 y=1 开始放（y=0 是底板）；玩家场景的地板常在 y0；**地板选区用 fromTo(0,0,0, w,0,d) 精确框出**（layer(0) 会带上地板外的方块，如大齿轮）
8. 点燃的烈焰人燃烧室：`create:blaze_burner` + `BlazeBurnerBlock.HEAT_LEVEL` = `HeatLevel.KINDLED`
9. 依赖：build.gradle 需要 `implementation "net.createmod.ponder:ponder-neoforge:<版本>+mc1.21.1"`（Create 6 自带该依赖）
10. **setBlock 的方块可能被场景清理/元素重叠**（反复踩坑）——动态方块（燃烧室）**搭进蓝图**，代码最后 showSection 它
11. **隐藏动画期间元素实时读世界**——先 `idle(50)` 等淡出完成，再 setBlock 到该区域
12. **destroyBlock 删数据原位**（moveSection 只移渲染，数据留在原坐标）
13. **齿轮方向**：同轴同向、啮合反向——`setKineticSpeed` 按啮合关系手动分组正负（官方 ChainDriveScenes 同款）
14. 文本指向方块用 `pointAt(util.vector().topOf(pos))`（topOf 指向顶部，centerOf 指向内部会被模型遮挡，线看不见）
15. 右键手势 `showControls(util.vector().blockSurface(pos, Direction.UP), Pointing.DOWN, 40).rightClick()`（图标在方块上方朝下指）
16. `Selection.substract()` 可以相减选区（分组速度/排除区域用）
17. 大齿轮等"装置一体件"要**加进独立元素选区**才能随 moveSection 一起抬升
18. 孤立组件（与主传动不连接的 pump/齿轮）setKineticSpeed 后可能不转——检查蓝图连接关系
19. **玩家搭场景常把结构直接放地上（y0 起）**——整蓝图 y+1 用 `shift_scene_y.py`，
    **代码坐标同步 +1**（heat 场景踩过：只改代码不改 nbt → 底座/两个齿轮消失）
20. **手写/改写 nbt 二进制**（shift_scene_y.py 踩过的 5 个坑）：
    - 根 tag 要补 `b'\x0a\x00\x00'` 前缀（0x0A=Compound 类型 + 名字长度 0x0000）
    - 字段类型按实际值写，不能硬编码 0x0A
    - IntArray(t=11) 要先写长度（int）再写数据
    - List(t=9) 结构是 (元素列表, 元素类型)，每个元素是 (值, 类型) 元组
    - 写完用 parse_scene.py 回读验证关键方块坐标
21. 多个 storyboard 可用**不同蓝图**（heat 场景用燃烧室变体 nbt，其余 3 个共用原 nbt）；
    共用同一蓝图则必须同步更新（坑 4）
22. **结构 nbt 的 size/pos 必须是 List\<Int\> 不是 IntArray**——IntArray 会让 MC 解析为空，
    场景全空白（4 场景事故；生成后用 parse_scene.py 验证 size 类型=9）
23. **Ponder 挂哪个物品 = 玩家实际拿哪个物品**：沸腾炉由控制器右键转化（方块没有常规摆放路径），
    所以 Ponder 挂 `fluidized_bed_controller` 而不是 `fluidized_bed`
24. **先读 BE 代码确认运行机制再写场景**——既能保证演示真实，还能发现真 bug：
    冷凝管耗气 10mB/tick vs 压缩机 32RPM 只产 2mB/tick → 产量远低于消耗 → 永不结冰（修复：耗气降到 2）
25. **动能机连接规则（DirectionalKineticBlock）**：轴只能从 **facing 的背面**接入
    （hasShaftTowards = face == facing.getOpposite()），泵从其它面抽气——布局设计前先查 block 源码
26. **"转变"动画模式（水→冰）**：独立元素显示 → hide 淡出 → idle(50) 等淡出完成 →
    setBlock 新方块（冰不在蓝图）——元素已隐藏无重叠，100% 稳定
27. **脚本生成蓝图时结构从 y1 起**（y0 地板），代码直接用蓝图坐标，**无需 +1 偏移**
    （电解槽场景的 +1 是玩家把结构放地上导致的，不是常态）
28. **DG 蒸馏塔（distillation_tank）**：配方只在**底层 controller** 处理，第 i 个结果输出到
    **第 i+1 层**（压缩空气 3 产物 → 至少 4 层塔）；方块无状态属性；多层垂直堆叠

## 6.5 电解槽模板（所有机器场景的通用骨架）

玩家明确要求"所有 Ponder 以电解槽为模板"，写场景前先对照：

1. **地板选区必须是 5×5**：`fromTo(0,0,0,4,0,4)`——玩家蓝图的地板棋盘格就是 5×5，
   恰好**排除大齿轮所在列**（大齿轮常被搭在 x5/z5 列）
2. **大齿轮 = 装置一体件**：加进"传动/装置选区"（与齿轮组同一个 showSection 出现、
   同一个 setKineticSpeed 转动）——**绝不放进地板选区**（否则"附着在底盘上"）
3. 不需要独立元素/抬升（电解槽大齿轮直接在地板外列，随装置显示）
4. 分屏分区顺序：地板 → 装置主体 → 传动组（含大齿轮）→ 外围（泵/管道/储罐）→ 文本 → 动画

## 7. 玩家协作模式

- **玩家搭场景**：告诉玩家搭布局要点 + 结构方块保存步骤，保存后从
  `saves/<世界>/generated/minecraft/structures/` 找 nbt，用 `parse_scene.py` 解析布局再写代码
- **场景放地上的处理**：玩家把结构直接放地上（y0 起）时，用 `shift_scene_y.py` 整体 +1，
  代码坐标同步 +1（heat 场景就是此模式）
- **没有玩家搭场景时 → 脚本生成**：用 `build_scene.py` 在代码里定义
  `size` + `blocks`（注册名+属性），自动生成 nbt（注意 §2 硬格式：List\<Int\> 的 size/pos）；
  生成后 parse_scene.py 验证关键方块坐标；脚本生成时结构从 y1 起，代码直接用蓝图坐标
- **新机器的场景流程**：先读 block/BE 源码确认机制（热源/轴接入面/配方条件/输出层序）→
  设计布局（蓝图）→ 写场景 → 翻译 → 编译。Ponder 演示的内容必须与真实机制一致
- **剧本式需求**（"第 1 步出现 X，文本…"）：每步 = showSection/idle/showText 序列；
  动态效果：moveSection（抬升，官方可靠）、hideIndependentSection（隐藏）
- **⚠️ 动态方块（燃烧室/特殊状态方块）→ 让玩家搭进蓝图**（变体场景），代码最后 showSection 它——
  这是唯一 100% 稳定显示动态方块的方式；代价是放弃 setBlock 动画
- **先讨论再动手**：Ponder 机制细节多，玩家有明确意图时先复述理解（哪个方块、什么位置、
  什么时候出现/消失）再写代码，避免反复试错

## 8. 调试工具

- **反编译 ponder 库**（确认机制，别猜）：
  ```bash
  "/c/Program Files/Microsoft/jdk-21.0.9.10-hotspot/bin/java.exe" -jar /tmp/cfr.jar \
    "libs/ponder-neoforge-1.0.81+mc1.21.1.jar" --jarfilter "类名关键词"
  ```
  常用类：WorldSectionElementImpl（元素=选区+偏移）、PonderLevel（setBlock/restore）、
  FadeOutOfSceneInstruction（hide=淡出）、DisplayWorldSectionInstruction（showIndependentSection）
- CFR 下载：`curl -L -o /tmp/cfr.jar https://www.benf.org/other/cfr/cfr-0.152.jar`
- 本机 JDK 在 `C:\Program Files\Microsoft\jdk-21.0.9.10-hotspot`（java 不在 PATH）
