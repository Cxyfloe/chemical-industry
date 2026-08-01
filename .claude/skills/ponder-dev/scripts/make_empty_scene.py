#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
make_empty_scene.py — 生成全空气的空 Ponder 场景蓝图（结构方块 nbt 格式）
用法: python make_empty_scene.py <输出.nbt> [宽 高 深]   (默认 9 6 9)
之后场景方块全部由代码 setBlock 摆放
"""
import gzip, struct, sys

def nbt_string(s):
    if isinstance(s, str): s = s.encode('utf-8')
    return struct.pack('>H', len(s)) + s

def nbt_int(v): return struct.pack('>i', v)

def nbt_list(elem_type, payloads):
    return struct.pack('>Bi', elem_type, len(payloads)) + b''.join(payloads)

def nbt_compound(fields):
    out = b''
    for name, payload in fields:
        out += bytes([10]) + nbt_string(name) + payload
    return out + b'\x00'

def main():
    if len(sys.argv) < 2:
        print('用法: python make_empty_scene.py <输出.nbt> [宽 高 深]')
        return
    out = sys.argv[1]
    w, h, d = 9, 6, 9
    if len(sys.argv) >= 5:
        w, h, d = int(sys.argv[2]), int(sys.argv[3]), int(sys.argv[4])

    palette_item = nbt_compound([(b'Name', nbt_string('minecraft:air'))])
    palette = nbt_list(10, [palette_item])
    size = nbt_list(3, [nbt_int(w), nbt_int(h), nbt_int(d)])
    entities = nbt_list(0, [])
    blocks = nbt_list(10, [])
    root = nbt_compound([
        (b'size', size), (b'entities', entities), (b'blocks', blocks),
        (b'palette', palette), (b'DataVersion', nbt_int(3955)),
    ])
    gzip.open(out, 'wb').write(root)
    print('已生成空场景: %s (%dx%dx%d)' % (out, w, h, d))

if __name__ == '__main__':
    main()
