# 化学工业 (Chemical Industry) — 项目开发手册

## 项目概况

- **模组名称**：Chemical Industry（化学工业）
- **定位**：Create（机械动力）的附属模组
- **内容方向**：化学工程流程 + 材料合成
- **开发轴心**：自然方块 → 物品 → 反应配方
- **开发者**：cxy（编程初学者）
- **交流语言**：中文

## 技术栈

| 组件 | 版本 | 说明 |
|------|------|------|
| Minecraft | 1.21.1 | 游戏本体 |
| NeoForge | 21.1.80 | 模组加载器 |
| Create | 6.0.9 (本地 jar) | 机械动力，核心前置 |
| Create: Crafts & Additions | 1.5.10 | 电力系统（电解槽供电），运行时前置 |
| Create: Diesel Generators | 1.3.8 | 分馏塔，运行时前置 |
| Ponder API | 1.0.87 | Create 内部依赖（KineticBlockEntity 需要） |
| Gradle | 8.11 (Tencent 镜像) | 构建工具 |
| Java | 21 | 编译和运行环境 |

## 架构设计

### Create Controller 模式（沸腾炉）

沸腾炉采用与 **Create FluidTank / DG DistillationTank** 一致的多方块模式：

```
所有方块同一类型 → BFS 找到 Controller → 所有 I/O 委托到 Controller
```

| 机制 | 实现 |
|------|------|
| Controller 识别 | `originalTankKey != null` 的中层 BE |
| BFS 连接 | `updateConnectivity()` 扫描 6 方向 |
| 流体 I/O | `handlerForCapability(side)` 递归委托 |
| GUI 委托 | `guiBE()` → `getControllerBE()` |
| NBT 持久化 | `Ctrl` 标签存 controller BlockPos |

### 电解槽多方块

- **3×N 设计**：水平 3 段（阴/中/阳），深度 1~3 层沿 FACING 反方向
- **前排处理**：`isFront()` 检测，只有最前排 CENTER 运行 tick
- **后排委托**：`findFront()` → 所有槽 I/O 委托到前排
- **容量公式**：`depthCount() * 3 * PER_BLOCK_MB`（4000mB/方块）
- **能耗/速度**：`ENERGY_PER_TICK * depth`，`progress += depth`（线性缩放）

## 项目文件结构

```
chemical industry/
├── build.gradle              # 构建配置
├── CLAUDE.md                  # ← 本文件（项目开发手册）
├── 开发计划.md                # 分阶段开发计划 + 版本记录
├── 项目结构.md                # 项目结构速查手册
├── generate_textures.js       # 批量生成纯色占位贴图
├── libs/                      # 本地 jar 依赖（含 ponder 库）
├── .claude/skills/ponder-dev/ # Ponder 开发 skill（手册+脚本+模板）
├── Create-mc1.21.1-6.0.9/    # Create 源码（参考）
├── create craft & additions/  # CA 源码（参考）
├── Create-Diesel-Generators-/ # DG 源码（参考）
└── src/main/
    ├── java/com/cxy/chemical_industry/
    │   ├── ChemicalIndustry.java           # 主模组类（含 Ponder 注册）
    │   ├── registry/                       # 注册中心（8 个文件）
    │   ├── block/                          # 9 个自定义方块（含罂粟作物）
    │   ├── block_entity/                   # 4 个方块实体（+冷凝管）
    │   ├── fluid/                          # 流体类型
    │   ├── item/                           # 6 个自定义物品（含鸦片系列）
    │   ├── effect/                         # 腐蚀效果
    │   ├── screen/                         # 4 个 GUI 文件
    │   ├── event/                          # 6 个事件处理器（气体泄漏/鸦片/植物等）
    │   └── ponder/                         # Ponder 教程（插件 + 场景代码）
    └── resources/
        ├── META-INF/neoforge.mods.toml
        └── assets/chemical_industry/       # 客户端资源
            └── ponder/                     # Ponder 场景蓝图 nbt
        └── data/chemical_industry/         # 数据驱动内容
```

## 关键规则

### 1. Minecraft 1.21 数据包路径

| 旧路径（1.20.x） | 新路径（1.21+） |
|---|---|
| `loot_tables/` | **`loot_table/`** |
| `recipes/` | **`recipe/`** |
| `tags/blocks/` | **`tags/block/`** |
| `tags/fluids/` | **`tags/fluid/`** |

### 2. 掉落表与方块标签缺一不可

1. 方块属性：`.requiresCorrectToolForDrops()`
2. 挖掘工具标签：`data/minecraft/tags/block/mineable/pickaxe.json`
3. 工具等级标签：`data/minecraft/tags/block/needs_stone_tool.json`
4. 掉落表：`data/<modid>/loot_table/blocks/<方块名>.json`

### 3. Create 配方格式（已验证通过）

```json
// ✅ 正确格式（Create 6.0.9 / NeoForge 1.21）
{
  "type": "create:mixing",
  "ingredients": [
    {"item": "mod:item"},
    {"type": "fluid_stack", "fluid": "mod:fluid", "amount": 1000}
  ],
  "results": [
    {"id": "mod:item", "amount": 1, "chance": 0.8},
    {"id": "mod:fluid", "amount": 1000}
  ],
  "heatRequirement": "heated"
}
```

| 字段 | 格式 | 说明 |
|------|------|------|
| 物品原料 | `{"item": "mod:item"}` | |
| 流体原料 | `{"type": "fluid_stack", "fluid": "mod:fluid", "amount": 1000}` | **必须有 type** |
| 物品产物 | `{"id": "mod:item", "amount": 1, "chance": 0.8}` | chance 可选 |
| 流体产物 | `{"id": "mod:fluid", "amount": 1000}` | |

### 4. 流体注册 — 液体 vs 气体

**液体**：`registerFluid()` → FluidType + Source + Flowing + LiquidBlock + BucketItem
**气体**：`registerGas()` → FluidType + Source/Flowing（`createLegacyBlock` → `Blocks.AIR`）

贴图通过 `ChemicalFluidType` 设置 `still`/`flow` 纹理路径。

### 5. 新增内容检查清单

- [ ] **注册类**（ModBlocks/ModItems/ModFluids）
- [ ] **创造标签页**（ModCreativeTabs）
- [ ] **翻译**（zh_cn.json + en_us.json）
- [ ] **资源文件**（model JSON + texture PNG + blockstate/loot_table）
- [ ] **方块标签**（pickaxe + needs_stone_tool）
- [ ] **流体标签**（water.json — 溶液类流体）

### 6. 旧存档兼容

- 修改 `ItemStackHandler` 槽位数 → 必须在 `loadAdditional` 中加 NBT 迁移
- 修改流体槽数量 → 旧 key 不存在时跳过
- 新增字段 → 用 `t.contains(key)` 检查

### 7. Ponder 开发速记（详见 .claude/skills/ponder-dev/SKILL.md）

- 场景 = 蓝图 nbt（结构方块格式，`saves/<世界>/generated/minecraft/structures/`）+ Java 剧本
- 蓝图路径 `assets/<modid>/ponder/<storyboard>.nbt`；每个 storyboard 独立加载自己的蓝图
- 玩家搭场景 → parse_scene.py 解析布局 → 按实际坐标写代码
- 翻译 key 用 `scene.title()` 第一个参数（不是 storyboard 名）

### 8. 高频错误清单

1. **GUI 槽位处理器必须用 IItemHandlerModifiable**，否则打开 GUI 闪退
2. **方块转换/BE 销毁前先收集信息**（先读再改）
3. **编译前检查 Javadoc `/**` 闭合**
4. **GUI 槽位数变 → Menu/Screen 跟着变**
5. **新增流体 = 注册 + 贴图 + 翻译 + 标签，四条缺一不可**
6. **方向：`getClockWise()` = 右，`getCounterClockWise()` = 左**
7. **实体/流体处理区分 source vs flowing**
8. **`FluidStack` 用 `.getSource()` 不是 `.getFlowing()`**
9. **BFS 扫描先验证方块类型再加进集合**（否则会把相邻的烈焰人燃烧室等误收，getValue 崩溃）
10. **1.21.1 没有 `minecraft:fuel` 配方类型**，燃料值用 `Item.getBurnTime()` 代码注册
11. **配方引用的方块/物品必须已注册**（silver_block 未注册导致数据包加载失败）
12. **Ponder 场景必须在主类构造器注册**（PonderIndex.addPlugin），不能放 FMLClientSetupEvent
13. **Ponder 文字必须加 lang 翻译**（key = `<modid>.ponder.<titleKey>.header/.text_N`），否则显示空白
14. **Ponder 用原生 `builder`**（SceneBuilder），showSection 返回 void，要移动用 showIndependentSection

## 当前进度 (v0.8.0)

### ✅ 已完成
- [x] 11 种矿石（+朱砂/方铅矿/锡石，各带深层变种）+ 世界生成 + 掉落表
- [x] 21 种流体（溶液/酸/液体金属/有机液体 + 气体）
- [x] 70+ 种物品（铝/钢/硬铝/青铜工具盔甲、有机原料、盐类、朱砂）
- [x] 4 台机器：沸腾炉（Controller 模式）、电解槽（3×N）、空气压缩机、冷凝管
- [x] 10 种电解配方（NaCl/CuSO₄/转移/精炼/铝/钠/水/盐酸/MgCl₂/氢氟酸）
- [x] 沸腾炉 20+ 配方（硫/黄铁/铜/铝/钢/氨/甲醇/甲醛/尿素/焦油/水煤气/铅/锡/水银等）
- [x] 空气分离系统（压缩机 → DG 分馏塔 → O₂/N₂/稀有气体）
- [x] 储气罐（支持 9 种气体）
- [x] 事故系统（堵管爆炸 + 分馏塔热源爆炸 + 管道泄漏中毒 + 氟气爆炸）
- [x] 19 个成就（含 9 个新成就 + 全成就联动）
- [x] 氟气系统（萤石粉+硫酸→氢氟酸 → 电解爆炸制氟）
- [x] 罂粟作物 + 鸦片/果实（两段式效果：正面→负面）
- [x] 朱砂 → 水银（流体，有桶）
- [x] 创造栏 3 标签页（化工装置/无机化学/有机化学）
- [x] 沸腾炉层容量规则（层总容量 = 方块数×8 桶，按流体种类平分）
- [x] 电解配方简化（除铝外不需要催化剂/膜）
- [x] 电解槽 Ponder 教程（6 幕剧本：搭建/动能/通电/电极/加热/爆炸警示）
- [x] Ponder 扩展：沸腾炉 + 空气压缩机 + 空气分馏 + 冷凝管 4 个场景（电解槽模板）
- [x] 成就树状化（parent 分层）+ "化学易如反掌"/"诺贝尔化学奖"隐藏成就
- [x] 冷凝管结冰修复（耗气 10→2 mB/tick，与压缩机 32RPM 产量持平）
- [x] git 版本管理初始化 + ponder-creator skill 发布 GitHub（双语 + Release v1.0.0）

### 🔜 待开发
- [ ] 有机化学配方完善
- [ ] 美术（正式贴图，发布前必需）
- [ ] 银块/硬铝块配方验证
- [ ] JEI 兼容
- [ ] 模组整体发布（等美术完成后推 GitHub + Release）

## 代码规范

1. **注释**：关键方法必须有 JavaDoc
2. **命名**：英文注册名，中文在注释和语言文件中
3. **注册方式**：统一 `DeferredRegister` 模式
4. **格式**：缩进 4 空格，逻辑块之间空行分隔
5. **流体 I/O**：`FluidHandler` 需要传入 `side` 参数
