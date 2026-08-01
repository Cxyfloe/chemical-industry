#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""add_cogs.py — 给分馏塔蓝图输出泵 east 侧添加传动齿轮（视觉动能件）
用法: python add_cogs.py <nbt路径> [x y z ...]  追加 cogwheel(axis=x) 到指定坐标"""
import gzip, sys, os
sys.path.insert(0, os.path.dirname(__file__))
from shift_scene_y import read_payload, write_payload

def main():
    if len(sys.argv) < 5:
        print('用法: python add_cogs.py <nbt> x y z [x y z ...]')
        return
    path = sys.argv[1]
    coords = [(int(sys.argv[i]), int(sys.argv[i+1]), int(sys.argv[i+2]))
              for i in range(2, len(sys.argv), 3)]

    raw = gzip.open(path, 'rb').read()
    root, _ = read_payload(raw, raw[0], 3)
    blocks = root['blocks'][1][0]
    palette = root['palette'][1][0]

    # cogwheel axis=x 的 palette 索引（没有则追加）
    cog_idx = None
    for i, p in enumerate(palette):
        name = p[0]['Name'][1]
        props = p[0].get('Properties')
        if name == 'create:cogwheel' and props and props[1].get('axis', (8, ''))[1] == 'x':
            cog_idx = i
            break
    if cog_idx is None:
        cog_idx = len(palette)
        palette.append(({'Name': (8, 'create:cogwheel'),
                         'Properties': (10, {'axis': (8, 'x')})}, 10))
        root['palette'] = (9, (palette, 10))

    added = 0
    air_idx = None
    for p in coords:
        found = None
        for b in blocks:
            pos = tuple(x[0] for x in b[0]['pos'][1][0])
            if pos == p:
                found = b
                break
        if found is None:
            blocks.append(({'pos': (9, ([(p[0], 3), (p[1], 3), (p[2], 3)], 3)),
                            'state': (3, cog_idx)}, 10))
            added += 1
        else:
            # 已存在（可能是 air 占位）→ 替换 state 为 cogwheel
            cur = found[0]['state'][1]
            name = palette[cur][0]['Name'][1] if cur < len(palette) else '?'
            if name == 'minecraft:air':
                found[0]['state'] = (3, cog_idx)
                added += 1
    root['blocks'] = (9, (blocks, 10))

    gzip.open(path, 'wb').write(b'\x0a\x00\x00' + write_payload(10, root))
    print('已添加 %d 个 cogwheel(axis=x)，palette 索引 %d' % (added, cog_idx))

if __name__ == '__main__':
    main()
