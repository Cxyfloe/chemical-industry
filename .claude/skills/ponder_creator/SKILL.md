---
name: ponder_creator
description: 通用 Ponder 教程场景开发指南——蓝图 nbt、场景代码、翻译、注册、传动动画、AI 协作工作流的完整手册。适用于任何 Create 附属模组（Forge/NeoForge，MC 1.20.1+ / 1.21.x，Create 6.x + ponder 1.0.x）
---

# Ponder Creator — Create 模组 Ponder 教程场景开发指南

> 一份经过真实模组项目（机械动力附属模组，MC 1.21.1 / NeoForge / Create 6.0.9 / ponder 1.0.81）
> 反复实战验证的开发手册。所有机制说明均经**反编译 ponder 库源码确认**，不是猜测。
>
> 特别适合**开发者 + AI 助手协作**的工作流：AI 负责解析蓝图、生成场景代码、排查机制问题，
> 开发者负责在游戏内搭场景、测试反馈。

---

## 目录

1. [核心概念](#1-核心概念)
2. [完整开发流程](#2-完整开发流程)
3. [蓝图 nbt 格式（硬性要求）](#3-蓝图-nbt-格式硬性要求)
4. [脚本工具](#4-脚本工具)
5. [场景代码 API 速查](#5-场景代码-api-速查)
6. [机制真相（反编译确认）](#6-机制真相反编译确认)
7. [场景骨架模板（推荐模式）](#7-场景骨架模板推荐模式)
8. [动能传动系统（齿轮方向科学）](#8-动能传动系统齿轮方向科学)
9. [动态效果与安全模式](#9-动态效果与安全模式)
10. [踩坑清单](#10-踩坑清单)
11. [AI 协作工作流](#11-ai-协作工作流)
12. [调试工具](#12-调试工具)
13. [发布到 GitHub](#13-发布到-github)

---

## 1. 核心概念

**Ponder 场景 = 三部分：**

```
① 蓝图 nbt（assets/<modid>/ponder/<路径>.nbt）
   标准结构方块（Structure Block）格式，定义场景里出现的所有方块
② Java 剧本代码（场景类）
   按时间线编排：什么方块何时出现、文字、动画
③ 语言文件（lang）
   所有文字必须有翻译条目，否则显示空白
```

**关键设计哲学（实战总结）：**

- **场景方块全部来自蓝图**。代码只做"显示编排"（showSection 分区出现），不要用 setBlock 动态搭场景
- 玩家在游戏里用结构方块搭场景（所见即所得），AI 解析 nbt 写代码——**分工明确，迭代最快**
- 一个物品可以挂多个场景（storyboard），按 W 预览后空格翻页

**适用环境：**

| 组件 | 版本 |
|------|------|
| Minecraft | 1.20.1+（本手册基于 1.21.1 验证） |
| 加载器 | Forge / NeoForge |
| Create | 6.x（内含 ponder 库，实际使用场景） |
| Ponder API | `net.createmod.ponder`（jar-in-jar 打包在 Create 内） |

### ⚠️ 脱离 Create 的可行性（反编译 + 依赖声明验证）

**Ponder 库本身不依赖 Create！** 它的 `neoforge.mods.toml` 依赖声明只有：
`neoforge` + `minecraft` + `flywheel`（Create 的渲染库，独立发布），jar 内**零引用**
`com.simibubi.create` 类。

| 方案 | 可行性 |
|------|--------|
| 用现成 Ponder 库 + 不装 Create | **技术上可行**：依赖 NeoForge + MC + Flywheel + Catnip 即可；但 `setKineticSpeed`/`CreateSceneBuilder` 等**动能指令是 Create 的扩展**（没有 Create 时场景不能转动能方块，其余功能全可用）；实际分发需自行把 ponder jar 打包进模组 |
| 自研原版 Ponder 系统（不装任何 Create 系模组） | **完全可行**：核心原理只有四块——① 虚拟世界（渲染 blocks map 的快照世界）② 按 tick 排队的指令系统 ③ UI 覆盖层（文字/进度条/高亮/手势）④ 蓝图 nbt 加载（标准结构方块格式，见 §3）。入门版约 1000~2000 行代码；Create 团队本身就是独立开发 Ponder 项目（[Creators-of-Create/Ponder](https://github.com/Creators-of-Create/Ponder)），仅深度集成进 Create |

**本手册的通用性**：§3 蓝图格式、§5 API（除 setKineticSpeed 外的全部指令）、
§6 机制真相、§7 骨架模板、§10 踩坑清单——全部适用于非 Create 环境；
仅 §8 传动系统和 `setKineticSpeed` 属于 Create 扩展。

---

## 2. 完整开发流程

```
① 蓝图 nbt → ② 场景代码 → ③ 翻译 → ④ 注册 → ⑤ 编译 → ⑥ 游戏测试
```

1. **蓝图**：玩家在游戏里用**结构方块**搭好场景（或 AI 用脚本生成），
   导出路径：`saves/<世界>/generated/minecraft/structures/<名字>.nbt`
   复制到 `assets/<modid>/ponder/<storyboard路径>.nbt`
2. **场景代码**：写场景类（见 §7 模板），按剧本编排
3. **翻译**：必须加 lang，否则文字显示**空白**（见 §5 翻译规则）
4. **注册**：实现 PonderPlugin + 主类构造器里注册（见 §5 注册）
5. 编译 → 复制 jar 到 mods
6. 游戏内对物品按 **W** 预览，空格翻页

> ⚠️ **Ponder 场景在游戏启动时注册加载**——修改代码/nbt 后必须**完全重启游戏**，
> 游戏内 F3+T 资源刷新**不会**重新加载 Ponder！

---

## 3. 蓝图 nbt 格式（硬性要求）

Ponder 蓝图 = **标准结构方块 nbt**（gzip 压缩）。以下是经过"场景全空白事故"验证的硬性格式：

### 3.1 顶层结构

```
{
  "size":        List<Int> [x, y, z],     // ⚠️ 必须是 Int 列表，不是 IntArray！
  "entities":    List<Compound>,          // 通常为空，元素类型 0（End）
  "blocks":      List<Compound>,          // 每个：{pos: List<Int>[3], state: Int, nbt?: Compound}
  "palette":     List<Compound>,          // 每个：{Name: String, Properties?: Compound}
  "DataVersion": Int                      // 见下表
}
```

### 3.2 ⚠️ 硬性格式要求（脚本生成时最容易错）

| 字段 | 要求 | 错误后果 |
|------|------|---------|
| `size` | **List\<Int\>**（t=9，元素类型 3） | IntArray → 解析为空 → **场景无方块** |
| `blocks[].pos` | **List\<Int\>**（t=9，元素类型 3） | 同上 |
| `entities` | 空 List 元素类型 = 0（End） | — |
| 方块属性 | 可省略默认值（如 `lit` 不写 = false） | — |
| `DataVersion` | 1.20.1=3465 / 1.20.4=3700 / 1.21=3953 / **1.21.1=3955** | 版本不匹配可能加载失败 |

> **为什么 size/pos 必须是 List\<Int\>**：MC 的 `StructureTemplate.getList("pos", 3)`
> 只认 Int 列表。传 IntArray 会被当成空 → size=0 → 场景一个方块都不显示。
> 这是最常见"场景空白"的根因。

### 3.3 NBT 二进制细节（手写/脚本改写 nbt 时）

- 根 tag 需前缀 `b'\x0a\x00\x00'`（0x0A=Compound 类型 + 名字长度 0x0000）
- 所有字段值都是 **(类型, 值)** 元组；`Properties` 是 Compound(t=10)
- List 元素是 **(值, 元素类型)** 元组
- IntArray(t=11) 先写长度再写数据

### 3.4 常用方块状态（Create）

| 方块 | 状态示例 |
|------|---------|
| 轴 | `create:shaft {"axis":"z"}` |
| 齿轮 | `create:cogwheel {"axis":"z"}` / `create:large_cogwheel {"axis":"x"}` |
| 动力泵 | `create:mechanical_pump {"facing":"west"}`（轴沿 facing 方向） |
| 鼓风机 | `create:encased_fan {"facing":"west"}` |
| 传送带 | `create:belt {"part":"start","facing":"east","slope":"horizontal"}` |
| 创造马达 | `create:creative_motor {}` |
| 燃烧室 | `create:blaze_burner {"blaze":"seething"}` |
| 水 | `minecraft:water {"level":"0"}`（水源） |

---

## 4. 脚本工具

位于 `scripts/` 目录，需要 Python 3（解析器用 `gzip` + `struct` 手写 NBT 读写，零依赖）。

### 4.1 `parse_scene.py` — 解析蓝图

```bash
python parse_scene.py <nbt路径>
```

输出 palette（方块状态）+ 所有方块坐标。**写场景代码前必跑**——坐标全部来自这里。

### 4.2 `shift_scene_y.py` — 整体抬升

```bash
python shift_scene_y.py <输入.nbt> <输出.nbt> [y偏移量=1]
```

所有方块 y 坐标 +N。用于玩家把结构直接放地上（y0 起）时整体抬升 1 格。
改完用 parse_scene.py 回读验证。

### 4.3 `build_scene.py` — 代码生成蓝图

```bash
python build_scene.py [场景名...]    # 不传参数 = 生成全部
```

在脚本里定义 `size` + `blocks`（方块 = 注册名 + 属性字典），自动收集 palette 写入 nbt。
**没有玩家搭场景时的方案**。注意 §3.2 的硬性格式（List\<Int\> 的 size/pos）。

---

## 5. 场景代码 API 速查

场景方法签名：

```java
public static void 场景名(SceneBuilder builder, SceneBuildingUtil util)
```

**直接用 `builder`**（ponder 原生接口），不要包 CreateSceneBuilder（它的 world() 返回 void 版本）。
唯一例外：`setKineticSpeed` 只在 `new CreateSceneBuilder(builder).world()` 上有。

### 5.1 指令表

| 指令 | 签名/用法 | 说明 |
|------|-----------|------|
| title | `builder.title("场景key", "标题")` | 翻译 key 前缀用第一个参数 |
| configureBasePlate | `builder.configureBasePlate(0, 0, 宽度)` | 场景底板 |
| idle | `builder.idle(tick)` | 停顿 |
| **showSection** | `builder.world().showSection(选区, 方向)` | 显示区块，**返回 void**，不可移动 |
| **showIndependentSection** | `builder.world().showIndependentSection(选区, 方向)` | 返回 `ElementLink<WorldSectionElement>`，可移动/隐藏 |
| **moveSection** | `builder.world().moveSection(link, new Vec3(0,1,0), 20)` | 移动已显示区块（抬升/平移动画）——**官方可靠** |
| hideIndependentSection | `builder.world().hideIndependentSection(link, 方向)` | 淡出隐藏（**只隐藏，不恢复世界方块**） |
| destroyBlock | `builder.world().destroyBlock(pos)` | 破坏方块（带动画）——官方可靠 |
| setBlock | `builder.world().setBlock(pos, 状态, false)` | 动态放方块（**有条件可靠**，见 §9） |
| setBlocks | `builder.world().setBlocks(选区, 状态, 粒子)` | 批量替换 |
| setKineticSpeed | `new CreateSceneBuilder(builder).world().setKineticSpeed(选区, ±速度)` | **Create 扩展**；正负控制方向 |
| toggleRedstonePower | `builder.world().toggleRedstonePower(选区)` | 拉红石 |
| showText | `builder.overlay().showText(时长).text("内容").attachKeyFrame().placeNearTarget().pointAt(vec)` | 文字 + 进度条节点 |
| showControls | `builder.overlay().showControls(vec, Pointing.DOWN, 40).rightClick()` | 鼠标手势 |
| showOutline | `builder.overlay().showOutline(PonderPalette.RED, new Object(), 选区, 时长)` | 高亮（GREEN/BLUE/RED） |
| 选区 | `util.select().position(pos)` / `.fromTo(a, b)` / `.add(...)` / `.substract(...)` | Selection 合并/相减 |
| 坐标 | `util.vector().topOf(pos)`（指向顶部）/ `.centerOf(pos)` / `.blockSurface(pos, dir)` | |

### 5.2 翻译规则（重要！不加 = 文字空白）

Ponder 按 key 查翻译：`<modid>.ponder.<title第一个参数>.<header|text_N>`

```json
"yourmod.ponder.my_scene.header": "场景标题",
"yourmod.ponder.my_scene.text_1": "第一句讲解",
"yourmod.ponder.my_scene.text_2": "第二句讲解"
```

- `text_N` 按场景内 `showText` **出现顺序**编号（1,2,3…）
- 语言文件（如 zh_cn.json / en_us.json）**都要加**
- `.text()` 参数写原文作为 fallback，双保险

### 5.3 注册（时机关键！）

```java
// YourPonderPlugin implements PonderPlugin（net.createmod.ponder.api.registration）
public String getModId() { return "yourmod"; }
public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
    helper.forComponents(ResourceLocation.fromNamespaceAndPath("yourmod", "机器物品注册名"))
        .addStoryBoard("机器/场景", YourScenes::场景方法);
}
```

**必须在主类构造器里注册**（和 Create 一致），不能放 FMLClientSetupEvent（太晚，索引里找不到场景）：

```java
if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) {
    net.createmod.ponder.foundation.PonderIndex.addPlugin(new YourPonderPlugin());
}
```

### 5.4 Ponder 挂哪个物品

**挂玩家实际拿到的那个物品**。例如：方块由"控制器"右键转化生成（玩家手里拿的是控制器），
Ponder 就要挂控制器物品，而不是方块本身。

---

## 6. 机制真相（反编译确认）

写代码前先想清楚这些，可以避免 90% 的疑难 bug：

1. **独立元素 = Selection + 渲染偏移**：`WorldSectionElementImpl` 持有选区，渲染时 PoseStack 整体平移。
   **方块数据留在原位**——moveSection 只移动渲染！
2. **元素渲染实时读世界方块**（不是快照）——隐藏淡出动画期间，如果改了元素选区内的方块，
   元素会把新方块画在偏移位置（"幽灵方块"）。
3. **元素渲染盖住世界方块**（重叠时元素优先）。
4. **showIndependentSection = 从世界渲染永久擦除**：内部调用
   `baseWorldSection.erase(选区)`——该位置不再属于世界渲染。**隐藏元素后位置就空了**，
   之后 setBlock 到该位置的方块**也不会显示**（这是"结冰动画失败"的根因，见 §9 安全模式）。
5. **hide = setVisible(false) + 淡出**，不恢复方块数据。
6. **setKineticSpeed = 直接修改 BE 的 Speed NBT**，正负控制方向。
7. **未显示过的方块 = 不存在**（不渲染）。蓝图里有但没 showSection = 看不见。
   反过来：想"删除"某个蓝图方块，只要不显示它即可。
8. **最稳原则：所有方块搭进蓝图**，代码只做分区显示 + 文本 + 动画。
9. **restoreBlocks(选区)**：把选区还原成初始状态——只有场景代码显式调用才生效（不会自动触发）。
10. **场景结束/重置时 restore()**：整个场景还原成蓝图初始状态（所有 setBlock 清空）。

---

## 7. 场景骨架模板（推荐模式）

经过实战验证的通用骨架（来自成熟模组项目的"电解槽模板"，适用于所有机器场景）：

```
分区顺序：① 地板 → ② 装置主体 → ③ 传动组（含大齿轮等一体件）→ ④ 外围（泵/管道/储罐）→ ⑤ 文本讲解 → ⑥ 动画
```

```java
public static void myMachine(SceneBuilder builder, SceneBuildingUtil util) {
    builder.title("my_machine_use", "我的机器");
    builder.configureBasePlate(0, 0, 6);

    // ① 地板：5×5 精确选区（玩家蓝图的地板棋盘格通常就是 5×5）
    //    ⚠️ 关键：如果大齿轮等被搭在地板外的列（x5/z5），这个选区必须精确框出地板，
    //    否则会把地板外的大齿轮也显示出来（"附着在底盘上"问题）
    builder.world().showSection(util.select().fromTo(0, 0, 0, 4, 0, 4), Direction.UP);
    builder.idle(10);

    // ② 装置主体
    builder.world().showSection(util.select().position(3, 1, 1), Direction.DOWN);
    builder.idle(5);
    builder.overlay().showText(70).text("机器主体")
            .attachKeyFrame().placeNearTarget().pointAt(util.vector().topOf(3, 1, 1));
    builder.idle(80);

    // ③ 传动组（齿轮 + 大齿轮一体出现）
    builder.world().showSection(util.select().position(2, 1, 2)
            .add(util.select().position(5, 0, 2)), Direction.SOUTH);
    builder.idle(5);

    // ④ 外围：泵/管道/储罐
    // ⑤ 更多文本……
    // ⑥ 动画：setKineticSpeed 按啮合分组（见 §8）
}
```

**核心规则：大齿轮等"装置一体件"永远不进地板选区，进传动选区**（跟齿轮组一起出现、一起转）。

---

## 8. 动能传动系统（齿轮方向科学）

用户对 Ponder 的传动动画要求"符合现实"。规则如下：

### 8.1 基础规则

- **同轴同向**：轴（shaft）、齿轮（cogwheel）、泵（pump）沿同一轴线相邻 = 同向同速
- **啮合反向**：相邻的齿轮（轴平行或垂直）互相啮合 = 反向
- **大齿轮（2×2）覆盖区**：large_cogwheel 的 2×2 区域内的 cogwheel 与它啮合（反向）

### 8.2 连接判定速查

| 组合 | 连接方式 |
|------|---------|
| shaft — shaft | 同轴相邻 → 同向 |
| cogwheel — cogwheel | 相邻（任意方向）→ 啮合反向 |
| cogwheel — large_cogwheel | 覆盖区内/相邻 → 啮合反向 |
| **pump 的轴端** | 只沿 facing 和 opposite 两个面（DirectionalKineticBlock） |
| pump — cogwheel（同轴） | 泵轴端对着齿轮轴端 → 同向 |
| pump — cogwheel（侧面） | **不连接**（齿轮不啮合泵） |
| **鼓风机（fan）的轴端** | facing 的反方向（hasShaftTowards = face == facing.getOpposite()） |
| **压缩机类方向方块** | 轴端 = facing 的反方向 |

### 8.3 ⚠️ 鼓风机的吸/吹方向（最易错）

`EncasedFanBlockEntity` 源码：

```java
Direction facing = getBlockState().getValue(BlockStateProperties.FACING);
speed = convertToDirection(speed, facing);   // 负方向 facing 时 speed 取反
return speed > 0 ? facing : facing.getOpposite();
```

- **正转速 → 朝 facing 吹**（对正方向 facing 如 north/east/up）
- **负转速 → 朝 facing 吹**（对负方向 facing 如 south/west/down，因 convertToDirection 取反）
- 简单记：**fan 要吹向冷凝管等目标，先看 facing 方向的正负，转速符号要让它取反后为正**

### 8.5 动力泵的方向（易返工区）

- **泵的流体流向由 facing 决定**（泵口朝 facing = 输出方向），**速度正负只影响视觉旋转方向**
- **正转（speed 与 facing 匹配）才代表"工作状态"**（流体从 facing 反面抽向 facing 面）
- **泵的旋转方向是纯视觉偏好，没有"物理正确"答案**——同一台泵 +32 或 -32 流体流向不变
- ⚠️ **多轮返工教训**：泵/齿轮的旋转方向如果改了 2-3 次用户仍不满意，
  **不要再猜，直接用提问工具给出选项让用户选**（同向/反向/不转/自定义）
- 用户常按自己的观察指定方向（如"从下到上第一个逆时针、第二个顺时针、第三个逆时针"）——
  每个泵单独设速即可，注意先确认编号顺序（如"从下到上"）

### 8.6 成就是树状结构的（进阶）

- 成就用 `parent` 字段分层：`"parent": "modid:前置成就"`
- 树状逻辑按流程：产物 → 应用 → 进阶（如 硫酸 → 氢氟酸 → 电解制氟）
- 隐藏成就：`display.hidden = true`（如进度终点"全成就"类）

```java
// 按啮合关系手动分组正负（官方 ChainDriveScenes 同款做法）
Selection plus = util.select().position(3, 1, 1)          // 同轴网络
        .add(util.select().position(4, 1, 1));
Selection minus = util.select().position(4, 1, 2)         // 啮合网络
        .add(util.select().position(5, 0, 2));
new CreateSceneBuilder(builder).world().setKineticSpeed(plus, 32);
new CreateSceneBuilder(builder).world().setKineticSpeed(minus, -32);
```

- **孤立组件**（与主传动无连接）：方向任意，视觉统一即可
- 同一装置内的**泵流体方向**应一致（输入/输出泵同向转）

---

## 9. 动态效果与安全模式

### 9.1 setBlock 的可靠条件

`setBlock`（ReplaceBlocksInstruction）直接调用 `level.setBlockAndUpdate`——**在"世界渲染区"内可靠**。
但要满足：

- 目标位置必须**属于世界渲染**（未被 showIndependentSection 擦除）
- 目标位置不能有**显示中的独立元素**覆盖

### 9.2 "水→冰"转变动画（安全模式）

```java
// ✅ 安全：水用普通 showSection（属于世界渲染），结冰直接 setBlock 覆盖
builder.world().showSection(util.select().position(1, 1, 3), Direction.WEST);  // 水
...
builder.world().setBlock(new BlockPos(1, 1, 3), Blocks.ICE.defaultBlockState(), false);  // 冰覆盖

// ❌ 错误：水用 showIndependentSection（世界渲染被擦除），hide 后 setBlock 冰不可见
```

### 9.3 抬升动画（官方可靠）

```java
ElementLink<WorldSectionElement> gear = builder.world().showIndependentSection(
        util.select().position(5, 0, 2), Direction.SOUTH);
builder.world().moveSection(gear, new Vec3(0, 1, 0), 20);   // 抬升 1 格
```

注意：独立元素会擦除世界渲染，若后续还要在该位置 setBlock，不要用独立元素。

---

## 10. 踩坑清单

1. `showSection` 返回 **void**，要移动必须用 `showIndependentSection` 拿 link
2. `CreateSceneBuilder.world()` 是 void 包装，用原生 `builder`；`setKineticSpeed` 只在 CreateSceneBuilder 上
3. 蓝图路径 = `addStoryBoard` 参数 + `.nbt`（如 `"machine/use"` → `ponder/machine/use.nbt`）
4. 每个 storyboard **独立加载自己的蓝图**；多个 storyboard 可共用同一 nbt（但要**同步更新**）
5. 玩家保存的结构方块 nbt 在 `generated/minecraft/structures/`（不是 structures/ 根目录）
6. 翻译 key 用 **title 的第一个参数**（不是 storyboard 名）
7. **结构 nbt 的 size/pos 必须是 List\<Int\> 不是 IntArray**——IntArray 导致场景全空白
8. **地板选区必须精确 5×5**（`fromTo(0,0,0,4,0,4)`）——否则把地板外的大齿轮也显示出来
9. **大齿轮 = 装置一体件**：进传动选区（跟齿轮组一起出现/转动），绝不进地板选区
10. **Ponder 挂哪个物品 = 玩家实际拿哪个**（控制器转化的方块挂控制器）
11. **先读 BE 代码确认机制再写场景**——既能保证演示真实，还能发现真实 bug
    （实例：冷凝管耗气 10mB/tick vs 压缩机 32RPM 只产 2mB/tick → 永不结冰的设计 bug）
12. **动能机轴只能从 facing 的背面接入**（DirectionalKineticBlock.hasShaftTowards）
13. **鼓风机吸/吹由转速符号决定**（负方向 facing 时取反）——要吹风先算符号
14. **showIndependentSection 会永久擦除世界渲染**——隐藏后该位置 setBlock 不显示
15. **setKineticSpeed 修改 BE NBT，作用于数据位置**（即使元素被 moveSection 移动）
16. **齿轮方向**：同轴同向、啮合反向——按啮合关系手动分组正负
17. 文本指向方块用 `pointAt(util.vector().topOf(pos))`（topOf 指向顶部）
18. 右键手势 `showControls(util.vector().blockSurface(pos, Direction.UP), Pointing.DOWN, 40).rightClick()`
19. `Selection.substract()` 可以相减选区（分组速度/排除区域用）
20. 孤立组件（与主传动不连接）setKineticSpeed 后可能不转——检查蓝图连接关系
21. 多个 storyboard 可用不同蓝图（变体场景）；共用蓝图必须同步更新
22. 手写/改写 nbt 二进制：根 tag 补 `\x0a\x00\x00`；字段类型按实际写；IntArray 先写长度；
    List 元素是 (值,类型) 元组；写完用 parse_scene.py 回读验证
23. **Ponder 场景启动时加载**——改完必须完全重启游戏，F3+T 不生效
24. 玩家把结构直接放地上（y0 起）→ 整蓝图 y+1 用 shift_scene_y.py，**代码坐标同步 +1**
25. 新机器场景流程：先读 block/BE 源码确认机制（热源/轴接入面/配方条件/输出层序）→
    设计布局 → 写场景 → 翻译 → 编译
26. 传送带动画：setKineticSpeed 对 belt 有效但需要两端 pulley 结构，未接传动时可能不转
27. **不要脑补机制**：场景文本只描述真实存在的功能（实例教训：脑补"传送带运冰"被用户否决）
28. **泵的旋转方向是视觉偏好**：流体流向由 facing 决定，速度符号只影响视觉旋转；
    同一泵 +32/-32 都是"工作"——方向问题反复返工时不猜，直接问用户期望
29. **用户指定方向时先确认编号顺序**（如"第一个/第二个"是从下到上还是从上到下），
    再逐泵单独设速
30. **场景分段显示尊重用户偏好**：整体一次性出现 vs 逐块搭建——用户明确要求时照做
    （如"分馏塔整体出现，不要一块块搭"）
31. **外部模组来源要写明**：场景里的方块来自其他模组时，文本注明"由模组《X》提供"

---

## 11. AI 协作工作流

**推荐分工：**

```
玩家（游戏内）              AI（开发侧）
──────────────────────────────────────────
1. 用结构方块搭场景
2. 保存 nbt ──────────────► 3. parse_scene.py 解析布局
                            4. 写场景代码（骨架模板）
                            5. 翻译 + 注册
                            6. 编译 → 复制 jar
7. 重启游戏测试 ◄────────── 8. 反馈问题 → 循环
```

**AI 侧准则：**

- 写代码前先反编译/读源码确认机制（§12），**别猜**
- 场景代码坐标全部来自 parse_scene.py 输出，**逐块核对**
- 玩家反馈问题时要先复述理解（哪个场景、哪一步、什么现象），再动手
- 每轮修改后明确提醒玩家**重启游戏**（Ponder 启动时加载）
- 玩家修改过的 nbt 要重新复制（检查存档文件时间戳）
- **方向/偏好类问题改 2-3 次未满足 → 用提问工具问用户**（给出当前状态 + 2-4 个选项），
  绝不继续猜（实例：输出泵方向往返 5 轮返工的教训）

---

## 12. 调试工具

- **反编译 ponder 库**（确认机制，别猜）：
  ```bash
  java -jar cfr.jar "libs/ponder-neoforge-<版本>.jar" --jarfilter "类名关键词"
  ```
  常用类：`WorldSectionElementImpl`（元素=选区+偏移）、`PonderLevel`（setBlock/restore）、
  `FadeOutOfSceneInstruction`（hide=淡出）、`DisplayWorldSectionInstruction`（showIndependentSection）、
  `ReplaceBlocksInstruction`（setBlock 指令）、`EncasedFanBlockEntity`（鼓风机吸/吹符号）
- CFR 下载：`curl -L -o cfr.jar https://www.benf.org/other/cfr/cfr-0.152.jar`
- 本仓库 `scripts/` 三个 Python 工具（§4）

---

## 13. 发布到 GitHub

### 13.1 建议的仓库结构

```
ponder-creator/
├── README.md               # 中文 + 英文摘要
├── LICENSE                 # 建议 MIT（代码）或 CC BY 4.0（文档）
├── docs/
│   └── SKILL.md            # 本手册（即本文件）
├── scripts/                # Python 工具
│   ├── parse_scene.py
│   ├── shift_scene_y.py
│   └── build_scene.py
└── examples/               # 示例场景代码（骨架模板 + 完整实例）
    └── MachineScenes.java.example
```

### 13.2 README 要点

- 标题 + 一句话简介（"用 AI 开发 Create 模组 Ponder 教程场景的完整指南与工具集"）
- 适用版本表格（MC / Create / Ponder）
- 快速开始：三步（解析 nbt → 写场景 → 编译测试）
- 截图/演示动图（Ponder 预览效果）
- 关联项目：本指南源自真实模组项目（附链接）

### 13.3 通用性说明

本手册所有机制（蓝图格式、API、传动规则、反编译结论）均基于 ponder 库源码与
MC 1.21.1 实测，与具体模组无关，适用于**任何 Create 附属模组**。脚本工具为
Python 3 标准库实现（gzip/struct），零第三方依赖，跨平台可用。

---

## 附录：许可与致谢

- 本指南基于 [Create](https://github.com/Creators-of-Create/Create)（MIT）与
  [Ponder](https://github.com/Creators-of-Create/Ponder)（MIT）生态
- 反编译分析基于 ponder-neoforge 1.0.81+mc1.21.1
- 实战验证项目：Chemical Industry 模组（NeoForge 1.21.1 + Create 6.0.9）
