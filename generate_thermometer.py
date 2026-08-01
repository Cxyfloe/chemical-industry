#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
generate_thermometer.py — 生成温度计 16 帧贴图（16×16）

每帧 = 表盘（蓝→红渐变背景）+ 指针（角度随温度摆动）：
  - 帧 00：温度 0.0，指针指左上（冷/蓝）
  - 帧 15：温度 1.0，指针指右上（热/红）

用法：python generate_thermometer.py
输出：src/main/resources/assets/chemical_industry/textures/item/thermometer_00~15.png
"""
import os
import math
from PIL import Image, ImageDraw

FRAMES = 16
SIZE = 16
CX, CY = 7.5, 7.5          # 表盘圆心
RADIUS = 6.5               # 表盘半径
NEEDLE_LEN = 5.5           # 指针长度

# 蓝 → 红 渐变端点颜色
COLD_COLOR = (70, 130, 255)   # 亮蓝
HOT_COLOR = (255, 70, 70)     # 亮红
FRAME_COLOR = (60, 60, 70)    # 表盘外框（深灰）
NEEDLE_COLOR = (255, 255, 255)  # 指针（白）
CENTER_COLOR = (30, 30, 35)   # 指针轴心

OUT_DIR = r"src/main/resources/assets/chemical_industry/textures/item"
os.makedirs(OUT_DIR, exist_ok=True)


def lerp_color(c1, c2, t):
    """两个颜色按 t (0~1) 线性插值"""
    return tuple(int(c1[i] + (c2[i] - c1[i]) * t) for i in range(3))


def draw_frame(t):
    """画一帧：t = 温度 0~1"""
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    # 1. 表盘底色（蓝→红渐变，水平方向）
    for x in range(SIZE):
        col = lerp_color(COLD_COLOR, HOT_COLOR, x / (SIZE - 1))
        draw.line([(x, 0), (x, SIZE)], fill=col + (255,))

    # 2. 裁成圆形（用遮罩：圆内保留，圆外透明）
    mask = Image.new("L", (SIZE, SIZE), 0)
    mask_draw = ImageDraw.Draw(mask)
    mask_draw.ellipse([CX - RADIUS, CY - RADIUS, CX + RADIUS, CY + RADIUS], fill=255)
    img.putalpha(mask)

    # 3. 外框（圆形描边）
    draw = ImageDraw.Draw(img)
    draw.ellipse([CX - RADIUS, CY - RADIUS, CX + RADIUS, CY + RADIUS],
                 outline=FRAME_COLOR + (255,), width=1)

    # 4. 指针：角度从 -150°（冷/左上）摆到 -30°（热/右上）
    angle_deg = -150 + t * 120
    rad = math.radians(angle_deg)
    tip_x = CX + math.cos(rad) * NEEDLE_LEN
    tip_y = CY + math.sin(rad) * NEEDLE_LEN
    draw.line([(CX, CY), (tip_x, tip_y)], fill=NEEDLE_COLOR + (255,), width=2)

    # 5. 轴心小圆点
    draw.ellipse([CX - 1, CY - 1, CX + 1, CY + 1], fill=CENTER_COLOR + (255,))
    return img


if __name__ == "__main__":
    for i in range(FRAMES):
        temp = i / (FRAMES - 1)
        draw_frame(temp).save(os.path.join(OUT_DIR, f"thermometer_{i:02d}.png"))
        print(f"生成 thermometer_{i:02d}.png（温度 {temp:.2f}）")
    print("全部完成")
