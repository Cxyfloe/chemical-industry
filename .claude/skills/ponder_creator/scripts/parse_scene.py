#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
parse_scene.py — 解析结构方块/Ponder 蓝图 nbt，打印方块布局
用法: python parse_scene.py <场景.nbt>
输出: 尺寸、palette（方块状态索引）、所有方块坐标
"""
import gzip, struct, sys

def read_nbt(data):
    def read_payload(t, o):
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
                v, o = read_payload(et, o); items.append(v)
            return items, o
        if t == 10:
            out = {}
            while True:
                ft = data[o]; o += 1
                if ft == 0: break
                nl = struct.unpack('>H', data[o:o+2])[0]
                name = data[o+2:o+2+nl].decode('utf-8', 'ignore'); o += 2+nl
                v, o = read_payload(ft, o); out[name] = v
            return out, o
        if t == 11:
            l = struct.unpack('>i', data[o:o+4])[0]; o += 4
            return list(struct.unpack('>%di' % l, data[o:o+4*l])), o+4*l
        raise Exception('不支持的类型 %d' % t)
    # 跳过根 tag 类型 + 根名字（2 字节）
    return read_payload(data[0], 3)[0]

def main():
    if len(sys.argv) < 2:
        print('用法: python parse_scene.py <场景.nbt>')
        return
    raw = open(sys.argv[1], 'rb').read()
    if raw[:2] == b'\x1f\x8b':
        raw = gzip.decompress(raw)
    root = read_nbt(raw)
    print('尺寸:', root['size'])
    palette = root['palette']
    print('palette:')
    for i, s in enumerate(palette):
        print('  [%d] %s %s' % (i, s['Name'], s.get('Properties', {})))
    print('方块:')
    for b in root['blocks']:
        st = palette[b['state']]
        print('  (%d,%d,%d)  %s %s' % (b['pos'][0], b['pos'][1], b['pos'][2],
              st['Name'], st.get('Properties', {})))

if __name__ == '__main__':
    main()
