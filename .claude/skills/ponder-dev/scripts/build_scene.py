#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
build_scene.py — 用代码直接生成 Ponder 场景蓝图（结构方块 nbt，gzip）
用法: python build_scene.py [场景名...]    (不传参数 = 生成全部)

每个场景定义:
    size:   [x, y, z] 蓝图尺寸
    blocks: [(x, y, z, "mod:block", {属性}), ...]  —— 只写实体方块，空气自动省略
    属性可省略默认值（结构方块与 BlockState 解析规则一致）
"""
import gzip, struct, sys, os

# ---------------- NBT 序列化（与 shift_scene_y.py 同套读写） ----------------

def nbt_string(s):
    if isinstance(s, str): s = s.encode('utf-8')
    return struct.pack('>H', len(s)) + s

def write_payload(t, v):
    if t == 1: return struct.pack('>b', v)
    if t == 2: return struct.pack('>h', v)
    if t == 3: return struct.pack('>i', v)
    if t == 4: return struct.pack('>q', v)
    if t == 8: return nbt_string(v)
    if t == 9:
        items, et = v
        return nbt_list(et, [write_payload(x[1], x[0]) for x in items])
    if t == 10:
        return nbt_compound([(ft, k.encode(), write_payload(ft, x)) for k, (ft, x) in v.items()])
    if t == 11: return struct.pack('>i', len(v)) + struct.pack('>%di' % len(v), *v)
    raise Exception('unsupported type %d' % t)

def nbt_list(elem_type, payloads):
    return struct.pack('>Bi', elem_type, len(payloads)) + b''.join(payloads)

def nbt_compound(fields):
    out = b''
    for ft, name, payload in fields:
        out += bytes([ft]) + nbt_string(name) + payload
    return out + b'\x00'

def write_nbt(root):
    return b'\x0a\x00\x00' + write_payload(10, root)   # 根 tag: 0x0A + 空名字

# ---------------- 场景定义 ----------------
# 方块格式: (x, y, z, "注册名", {属性})
# 所有场景 y0 为地板（white_concrete），结构从 y1 起

SCENES = {}

# ============ 场景 A：沸腾炉（硫磺粉 → 硫酸） ============
# 三层塔(1x1x3)：bottom 底层进液 / middle 中层反应 / top 顶层出气
# 热源在 y0（燃烧室，getHeat 向下跳过热源检测）
SCENES['fluidized_bed/use'] = {
    'size': [5, 5, 5],
    'blocks': [
        # 地板（y0，z0-4）
        *[(x, 0, z, "minecraft:white_concrete", {}) for x in range(5) for z in range(5)],
        # 热源：点燃的烈焰人燃烧室（y0 替换地板中心格）
        (2, 0, 2, "create:blaze_burner", {"blaze": "seething"}),
        # 三层沸腾炉（y1-3）
        (2, 1, 2, "chemical_industry:fluidized_bed", {"layer": "bottom"}),
        (2, 2, 2, "chemical_industry:fluidized_bed", {"layer": "middle"}),
        (2, 3, 2, "chemical_industry:fluidized_bed", {"layer": "top"}),
        # 底层输入泵（水）：pump facing=east 吐入 bottom 西面，输入端朝西开口
        (1, 1, 2, "create:mechanical_pump", {"facing": "east"}),
        (0, 1, 2, "create:fluid_pipe", {"axis": "x"}),
        # 中层输出泵（硫酸）：pump facing=east，输入面(west)贴 middle 东面，抽出朝东
        (3, 2, 2, "create:mechanical_pump", {"facing": "east"}),
        (4, 2, 2, "create:fluid_pipe", {"axis": "x"}),
    ],
}

# ============ 场景 B：空气压缩机 ============
# 压缩机 facing=north → 轴只能从背面(south)接：shaft → creative_motor
# pump 从压缩机 east 面抽气 → 管道输出
SCENES['air_compressor/use'] = {
    'size': [6, 2, 7],
    'blocks': [
        *[(x, 0, z, "minecraft:white_concrete", {}) for x in range(6) for z in range(7)],
        # 动能：压缩机 (2,1,2) facing=north，背面(south)接轴
        (2, 1, 2, "chemical_industry:air_compressor", {"facing": "north"}),
        (2, 1, 3, "create:shaft", {"axis": "z"}),
        (2, 1, 4, "create:shaft", {"axis": "z"}),
        (2, 1, 5, "create:creative_motor", {}),
        # 输出泵：pump facing=east（输入面 west 贴压缩机 east 面），输出朝 east
        (3, 1, 2, "create:mechanical_pump", {"facing": "east"}),
        (4, 1, 2, "create:fluid_pipe", {"axis": "x"}),
        (4, 1, 3, "create:fluid_pipe", {"axis": "z"}),
    ],
}

# ============ 场景 C：空气压缩机 → 分馏塔（DG 蒸馏塔） ============
# 压缩机组 = 玩家 yasuoji 布局（压缩机 (3,1,1) facing=west + 齿轮组 + 大齿轮 (5,0,2)）
# pump (2,1,1) 从压缩机抽气 → 管道 (1,1,1)(1,1,2) → 塔底层 (1,1,3)
# 塔 4 层：底层输入压缩空气，2/3/4 层输出 O₂/N₂/稀有气体（配方结果 i → i+1 层）
SCENES['air_compressor/distillation'] = {
    'size': [7, 5, 6],
    'blocks': [
        *[(x, 0, z, "minecraft:white_concrete", {}) for x in range(7) for z in range(6)],
        # 压缩机组（用户 yasuoji 风格）：压缩机 facing=west，轴端 east 接 shaft/cogwheel
        (3, 1, 1, "chemical_industry:air_compressor", {"facing": "west"}),
        (3, 1, 2, "create:shaft", {"axis": "x"}),
        (2, 1, 2, "create:cogwheel", {"axis": "x"}),
        (4, 1, 1, "create:cogwheel", {"axis": "x"}),
        (4, 1, 2, "create:cogwheel", {"axis": "x"}),
        (5, 1, 1, "create:cogwheel", {"axis": "x"}),
        (5, 0, 2, "create:large_cogwheel", {"axis": "x"}),
        # 抽气泵：pump (2,1,1) facing=west（输入面 east 贴压缩机），输出朝 west
        (2, 1, 1, "create:mechanical_pump", {"facing": "west"}),
        # 输入管道 → 塔底层 (1,1,3)
        (1, 1, 1, "create:fluid_pipe", {"axis": "x"}),
        (1, 1, 2, "create:fluid_pipe", {"axis": "z"}),
        # 4 层分馏塔（底层 y1，最高 y4）
        (1, 1, 3, "createdieselgenerators:distillation_tank", {}),
        (1, 2, 3, "createdieselgenerators:distillation_tank", {}),
        (1, 3, 3, "createdieselgenerators:distillation_tank", {}),
        (1, 4, 3, "createdieselgenerators:distillation_tank", {}),
        # 三个输出泵（第 2/3/4 层 west 面抽）：pump facing=east → 输出管道朝 east
        (1, 2, 2, "create:mechanical_pump", {"facing": "east"}),
        (1, 3, 2, "create:mechanical_pump", {"facing": "east"}),
        (1, 4, 2, "create:mechanical_pump", {"facing": "east"}),
        (1, 2, 1, "create:fluid_pipe", {"axis": "x"}),
        (1, 3, 1, "create:fluid_pipe", {"axis": "x"}),
        (1, 4, 1, "create:fluid_pipe", {"axis": "x"}),
    ],
}

# ============ 场景 D：空气压缩机 → 冷凝管（水结冰） ============
# 冷凝管 (2,1,2) facing=north（冷风朝北）；背面(south) = 鼓风机 (2,1,3) facing=north
# 鼓风机轴端 south：shaft (2,1,4) → motor (2,1,5)
# 前方(north) 水 (2,1,6)(2,1,7)
# 压缩机 (5,1,2) facing=north → 轴端 south (5,1,3)(5,1,4) → motor (5,1,5)
# pump (4,1,2) facing=west（输入 east 贴压缩机，输出 west 经管道 (3,1,2) 吐入冷凝管）
SCENES['condenser_pipe/use'] = {
    'size': [6, 2, 9],
    'blocks': [
        *[(x, 0, z, "minecraft:white_concrete", {}) for x in range(6) for z in range(9)],
        # 冷凝管 + 背面鼓风机 + 鼓风机动力
        (2, 1, 2, "chemical_industry:condenser_pipe", {"facing": "north"}),
        (2, 1, 3, "create:encased_fan", {"facing": "north"}),
        (2, 1, 4, "create:shaft", {"axis": "z"}),
        (2, 1, 5, "create:creative_motor", {}),
        # 前方水源（冷风冻结目标）
        (2, 1, 6, "minecraft:water", {"level": "0"}),
        (2, 1, 7, "minecraft:water", {"level": "0"}),
        # 压缩机（供气）+ 动力
        (5, 1, 2, "chemical_industry:air_compressor", {"facing": "north"}),
        (5, 1, 3, "create:shaft", {"axis": "z"}),
        (5, 1, 4, "create:shaft", {"axis": "z"}),
        (5, 1, 5, "create:creative_motor", {}),
        # 抽气泵 + 管道（泵输出 west → 管道 → 冷凝管 east 面）
        (4, 1, 2, "create:mechanical_pump", {"facing": "west"}),
        (3, 1, 2, "create:fluid_pipe", {"axis": "x"}),
    ],
}

# ---------------- 生成 ----------------

def build(scene_name, scene):
    size = scene['size']
    # palette 收集：state 字符串 → 索引
    palette = []
    palette_index = {}
    blocks = []
    for (x, y, z, block_id, props) in scene['blocks']:
        state_key = block_id + json_props(props)
        if state_key not in palette_index:
            palette_index[state_key] = len(palette)
            # 所有字段值都是 (类型, 值) 元组；Properties 是 Compound(t=10)
            entry = {'Name': (8, block_id)}
            if props:
                entry['Properties'] = (10, {k: (8, str(v)) for k, v in props.items()})
            palette.append(entry)
        # 注意！pos 必须是 List<Int>（t=9 元素类型 3），不是 IntArray！
        # MC 的 StructureTemplate.getList("pos", 3) 只认 Int 列表，IntArray 会解析为空 → 场景无方块
        # List 元素格式 = (值, 元素类型)，如 (x, 3)
        blocks.append({
            'pos': (9, ([(x, 3), (y, 3), (z, 3)], 3)),
            'state': (3, palette_index[state_key]),
        })

    root = {
        # size 同样是 List<Int>（玩家结构方块保存格式，与 pos 一致）
        'size': (9, ([(size[0], 3), (size[1], 3), (size[2], 3)], 3)),
        'entities': (9, ([], 0)),   # 空列表元素类型 End=0（玩家保存格式）
        'blocks': (9, ([(b, 10) for b in blocks], 10)),
        'palette': (9, ([(p, 10) for p in palette], 10)),
        'DataVersion': (3, 3955),   # MC 1.21.1
    }
    return write_nbt(root)

def json_props(props):
    if not props:
        return '{}'
    return '{' + ','.join('%s=%s' % (k, v) for k, v in props.items()) + '}'

def main():
    # scripts → ponder-dev → skills → .claude → 项目根（4 层）
    out_dir = os.path.join(os.path.dirname(__file__), '..', '..', '..', '..',
                           'src', 'main', 'resources', 'assets',
                           'chemical_industry', 'ponder')
    names = sys.argv[1:] or list(SCENES.keys())
    for name in names:
        if name not in SCENES:
            print('未知场景: %s（可用: %s）' % (name, list(SCENES.keys())))
            continue
        path = os.path.join(out_dir, name + '.nbt')
        os.makedirs(os.path.dirname(path), exist_ok=True)
        data = build(name, SCENES[name])
        with open(path, 'wb') as f:
            f.write(gzip.compress(data))
        n = len(SCENES[name]['blocks'])
        print('已生成 %s （%d 方块，%d 字节）' % (name, n, len(data)))

if __name__ == '__main__':
    main()
