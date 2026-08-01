# ============================================================
# generate_textures.py — 生成占位贴图
# 用 Python 的纯标准库创建简单的 16x16 PNG 文件
# ============================================================

import struct
import zlib
import os


def create_png(path, r, g, b):
    """
    生成一个 16x16 像素的纯色 PNG 文件
    参数：
        path — 输出文件路径
        r, g, b — RGB 颜色值 (0-255)
    """
    width, height = 16, 16

    # 创建 PNG 数据块
    def make_chunk(chunk_type, data):
        """构建一个 PNG 数据块（length + type + data + crc）"""
        chunk = chunk_type + data
        crc = struct.pack('>I', zlib.crc32(chunk) & 0xffffffff)
        return struct.pack('>I', len(data)) + chunk + crc

    # ---- PNG 文件结构 ----
    # 1. PNG 签名（8 字节，固定值）
    signature = b'\x89PNG\r\n\x1a\n'

    # 2. IHDR 块 — 图像头信息
    #    宽度, 高度, 位深度=8, 颜色类型=2(RGB), 压缩=0, 滤波=0, 隔行=0
    ihdr_data = struct.pack('>IIBBBBB', width, height, 8, 2, 0, 0, 0)

    # 3. IDAT 块 — 压缩后的像素数据
    #    先构建原始像素行（每行前面加一个过滤字节 0x00）
    raw_pixels = b''
    for y in range(height):
        raw_pixels += b'\x00'  # 过滤器类型：None（无过滤）
        for x in range(width):
            raw_pixels += bytes([r, g, b])

    compressed_pixels = zlib.compress(raw_pixels)

    # 4. IEND 块 — 图像结束标记（空数据）
    # ---- 写入文件 ----
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'wb') as f:
        f.write(signature)
        f.write(make_chunk(b'IHDR', ihdr_data))
        f.write(make_chunk(b'IDAT', compressed_pixels))
        f.write(make_chunk(b'IEND', b''))


# ---- 生成所有贴图 ----
base = 'src/main/resources/assets/chemical_industry/textures'

# 方块贴图（16x16 的简单色块）
create_png(f'{base}/block/rock_salt_ore.png',  180, 170, 160)  # 灰白带粉（岩盐矿）
create_png(f'{base}/block/sulfur_ore.png',     200, 185, 50)   # 黄绿（硫磺矿）
create_png(f'{base}/block/salt_block.png',      240, 235, 230)  # 白色（盐块）

# 物品贴图（16x16 的简单色块）
create_png(f'{base}/item/salt.png',             250, 250, 245)  # 白色（食盐）
create_png(f'{base}/item/sulfur_powder.png',    230, 210, 60)   # 黄色（硫磺粉）
create_png(f'{base}/item/soda_ash.png',         225, 220, 210)  # 浅灰（纯碱）

print('6 张贴图已成功生成！')
print('位置：src/main/resources/assets/chemical_industry/textures/')
