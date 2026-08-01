#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
shift_scene_y.py — 把结构方块 nbt 的所有方块整体上移 N 格（y+offset）
用法: python shift_scene_y.py <输入.nbt> <输出.nbt> [y偏移量]   (默认 1)
"""
import gzip, struct, sys

def read_payload(data, t, o):
    if t == 1: return struct.unpack('>b', data[o:o+1])[0], o+1
    if t == 2: return struct.unpack('>h', data[o:o+2])[0], o+2
    if t == 3: return struct.unpack('>i', data[o:o+4])[0], o+4
    if t == 4: return struct.unpack('>q', data[o:o+8])[0], o+8
    if t == 5: return struct.unpack('>f', data[o:o+4])[0], o+4
    if t == 6: return struct.unpack('>d', data[o:o+8])[0], o+8
    if t == 8:
        l = struct.unpack('>H', data[o:o+2])[0]
        return data[o+2:o+2+l].decode('utf-8', 'ignore'), o+2+l
    if t == 9:
        et = data[o]; l = struct.unpack('>i', data[o+1:o+5])[0]; o += 5
        items = []
        for _ in range(l):
            v, o = read_payload(data, et, o); items.append((v, et))
        return (items, et), o
    if t == 10:
        out = {}
        while True:
            ft = data[o]; o += 1
            if ft == 0: break
            nl = struct.unpack('>H', data[o:o+2])[0]
            name = data[o+2:o+2+nl].decode('utf-8', 'ignore'); o += 2+nl
            v, o = read_payload(data, ft, o); out[name] = (ft, v)
        return out, o
    if t == 11:
        l = struct.unpack('>i', data[o:o+4])[0]; o += 4
        return list(struct.unpack('>%di' % l, data[o:o+4*l])), o+4*l
    raise Exception('不支持的类型 %d' % t)

def nbt_string(s):
    if isinstance(s, str): s = s.encode('utf-8')
    return struct.pack('>H', len(s)) + s

def nbt_list(elem_type, payloads):
    return struct.pack('>Bi', elem_type, len(payloads)) + b''.join(payloads)

def nbt_compound(fields):
    # fields: [(字段类型, 字段名bytes, payload)]
    out = b''
    for ft, name, payload in fields:
        out += bytes([ft]) + nbt_string(name) + payload
    return out + b'\x00'

def write_payload(t, v):
    if t == 1: return struct.pack('>b', v)
    if t == 2: return struct.pack('>h', v)
    if t == 3: return struct.pack('>i', v)
    if t == 4: return struct.pack('>q', v)
    if t == 5: return struct.pack('>f', v)
    if t == 6: return struct.pack('>d', v)
    if t == 8: return nbt_string(v)
    if t == 9:
        items, et = v
        # items 元素是 (值, 元素类型) 元组
        return nbt_list(et, [write_payload(x[1], x[0]) for x in items])
    if t == 10:
        return nbt_compound([(ft, k.encode(), write_payload(ft, x)) for k, (ft, x) in v.items()])
    if t == 11: return struct.pack('>i', len(v)) + struct.pack('>%di' % len(v), *v)  # IntArray: 长度 + 数据
    raise Exception('写入类型 %d 不支持' % t)

def main():
    if len(sys.argv) < 3:
        print('用法: python shift_scene_y.py <输入.nbt> <输出.nbt> [y偏移量]')
        return
    src, dst = sys.argv[1], sys.argv[2]
    offset = int(sys.argv[3]) if len(sys.argv) > 3 else 1
    raw = gzip.open(src, 'rb').read()
    root, _ = read_payload(raw, raw[0], 3)
    blocks = root['blocks'][1][0]  # root['blocks'] = (ft, ((items, et))) → items
    moved = 0
    for entry in blocks:
        comp = entry[0]
        if isinstance(comp, dict) and 'pos' in comp:
            # comp['pos'] = (类型, ((元素列表, 元素类型))) —— List 结构，元素是 (值, 元素类型) 元组
            items, et = comp['pos'][1]
            pos = list(items)
            pos[1] = (pos[1][0] + offset, pos[1][1])  # y 坐标上移
            comp['pos'] = (comp['pos'][0], (pos, et))
            moved += 1
    # write_payload(10, root) 只产出字段流，需补根 tag 类型(0x0A) + 根名字长度(0x0000)
    gzip.open(dst, 'wb').write(b'\x0a\x00\x00' + write_payload(10, root))
    print('已将 %d 个方块整体上移 %d 格: %s' % (moved, offset, dst))

if __name__ == '__main__':
    main()
