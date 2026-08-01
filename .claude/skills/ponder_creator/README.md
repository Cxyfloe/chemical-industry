# Ponder Creator

**用 AI 开发 Create 模组 Ponder 教程场景的完整指南与工具集**

一份经过真实模组项目实战验证的 Ponder 场景开发手册。所有机制说明均经反编译
ponder 库源码确认，并针对"开发者 + AI 助手协作"的工作流做了优化。

## 适用环境

| 组件 | 版本 |
|------|------|
| Minecraft | 1.20.1+（手册基于 1.21.1 验证） |
| 加载器 | Forge / NeoForge |
| Create | 6.x（内含 ponder 库，实际使用场景） |
| Ponder | net.createmod.ponder 1.0.x |

> **不装 Create 能用吗？** Ponder 库本身只依赖 NeoForge + Minecraft + Flywheel
> （jar 内零引用 Create 类，依赖声明已验证）。没有 Create 时无法使用动能指令
> （setKineticSpeed），其余场景功能全部可用；也可参考手册 §1 自行实现原版
> Ponder 系统（核心原理不依赖任何 Create 机制）。

## 快速开始

```bash
# 1. 解析玩家保存的场景蓝图（结构方块导出的 nbt）
python scripts/parse_scene.py path/to/scene.nbt

# 2. 按手册 §7 骨架模板写场景代码（Java）

# 3. 编译 → 复制 jar → 游戏内对物品按 W 预览
```

## 内容

- 📘 `docs/SKILL.md` — 完整手册（13 章：蓝图格式 / API 速查 / 机制真相 / 传动系统 / 踩坑清单 / AI 工作流）
- 🐍 `scripts/` — 零依赖 Python 工具（解析 / 抬升 / 生成蓝图 nbt）
- 📝 `examples/` — 场景代码模板

## 关键要点（速览）

1. **蓝图 nbt 的 size/pos 必须是 List\<Int\> 不是 IntArray** — 否则场景全空白
2. **所有方块搭进蓝图**，代码只做 showSection 编排 — 最稳原则
3. **大齿轮 = 装置一体件** — 进传动选区，绝不进地板选区
4. **鼓风机吸/吹由转速符号决定** — 负方向 facing 时转速取反
5. **showIndependentSection 会擦除世界渲染** — 隐藏后该位置 setBlock 不显示
6. **Ponder 场景启动时加载** — 改完必须重启游戏

## 许可证

MIT（代码）/ CC BY 4.0（文档）

## 致谢

- [Create](https://github.com/Creators-of-Create/Create)（MIT）
- [Ponder](https://github.com/Creators-of-Create/Ponder)（MIT）
- 实战验证项目：Chemical Industry 模组（NeoForge 1.21.1 + Create 6.0.9）
