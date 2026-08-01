// ============================================================
// generate_gui.js — 生成原版 Minecraft 风格的沸腾炉 GUI 贴图
// 输出：src/main/resources/assets/chemical_industry/textures/gui/fluidized_bed.png
// 尺寸：200 × 166（主区域 176 + 右侧箭头区域 24）
// ============================================================

const zlib = require('zlib');
const fs = require('fs');
const path = require('path');

// ---------- 颜色调色板（原版 Minecraft 容器 GUI 标准色） ----------
const C_BORDER_DARK    = [ 55,  55,  55];  // 深色边框（左上阴影）
const C_BORDER_LIGHT   = [255, 255, 255];  // 亮色边框（右下高光）
const C_BORDER_MID     = [139, 139, 139];  // 中等边框色
const C_BG_MAIN        = [198, 198, 198];  // 主背景灰
const C_SLOT_BG        = [139, 139, 139];  // 槽位背景灰
const C_SLOT_BORDER    = [ 55,  55,  55];  // 槽位边框
const C_SLOT_HIGHLIGHT = [255, 255, 255];  // 槽位高光边
const C_ARROW_BG       = [ 85,  85,  85];  // 进度条底色
const C_ARROW_FILL     = [200, 200, 200];  // 进度条填充色
const C_CORNER         = [139, 139, 139];  // 四角颜色

const WIDTH  = 200;   // 总宽度：176 主区域 + 24 进度条区域
const HEIGHT = 166;   // 总高度
const BORDER = 4;     // 边框厚度

function makeChunk(chunkType, data) {
    const length = Buffer.alloc(4);
    length.writeUInt32BE(data.length);
    const typeAndData = Buffer.concat([chunkType, data]);
    const crc = Buffer.alloc(4);
    crc.writeUInt32BE(zlib.crc32(typeAndData));
    return Buffer.concat([length, typeAndData, crc]);
}

function createPng(filePath, width, height, pixelFn) {
    // IHDR
    const ihdrData = Buffer.alloc(13);
    ihdrData.writeUInt32BE(width, 0);
    ihdrData.writeUInt32BE(height, 4);
    ihdrData.writeUInt8(8, 8);      // 8 bits per channel
    ihdrData.writeUInt8(2, 9);      // RGB color
    ihdrData.writeUInt8(0, 10);     // no compression
    ihdrData.writeUInt8(0, 11);     // no filter
    ihdrData.writeUInt8(0, 12);     // no interlace

    // Pixel data
    const rawRows = [];
    for (let y = 0; y < height; y++) {
        const row = Buffer.alloc(1 + width * 3);  // filter byte + RGB pixels
        row.writeUInt8(0, 0);  // no filter
        for (let x = 0; x < width; x++) {
            const [r, g, b] = pixelFn(x, y);
            const offset = 1 + x * 3;
            row.writeUInt8(r, offset);
            row.writeUInt8(g, offset + 1);
            row.writeUInt8(b, offset + 2);
        }
        rawRows.push(row);
    }

    const compressed = zlib.deflateSync(Buffer.concat(rawRows));
    const signature = Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]);

    const pngBuffer = Buffer.concat([
        signature,
        makeChunk(Buffer.from('IHDR'), ihdrData),
        makeChunk(Buffer.from('IDAT'), compressed),
        makeChunk(Buffer.from('IEND'), Buffer.alloc(0))
    ]);

    const dir = path.dirname(filePath);
    if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true });
    fs.writeFileSync(filePath, pngBuffer);
    console.log('  OK  ' + filePath + ` (${width}×${height})`);
}

// ============================================================
// 逐像素绘制函数
// ============================================================
function drawPixel(x, y) {
    // ---- 右侧进度条区域 (x >= 176) ----
    if (x >= 176) {
        return drawProgressArrow(x - 176, y);
    }

    // ---- 主区域边框 ----
    const isTop    = y < BORDER;
    const isBottom = y >= HEIGHT - BORDER;
    const isLeft   = x < BORDER;
    const isRight  = x >= WIDTH - 24 - BORDER;  // x >= 172

    // 四角
    if ((isTop || isBottom) && (isLeft || isRight)) {
        return C_CORNER;
    }

    // 上边框
    if (isTop) {
        return y === BORDER - 1 ? C_BORDER_DARK : C_BORDER_DARK.map(c => c + 10);
    }

    // 下边框
    if (isBottom) {
        return y === HEIGHT - BORDER ? C_BORDER_LIGHT : C_BORDER_MID;
    }

    // 左边框
    if (isLeft) {
        return x === BORDER - 1 ? C_BORDER_DARK : C_BORDER_DARK.map(c => c + 10);
    }

    // 右边框
    if (isRight) {
        return x === WIDTH - 24 - BORDER ? C_BORDER_LIGHT : C_BORDER_MID;
    }

    // ---- 主背景（#C6C6C6） ----
    return C_BG_MAIN;
}

// 进度箭头区域（24×16 像素）
function drawProgressArrow(x, y) {
    if (y >= 16) {
        return C_BG_MAIN;  // 余下区域填背景色
    }

    // 箭头底色
    if (x === 0 || x === 23 || y === 0 || y === 15) {
        return C_BORDER_DARK;  // 边框
    }

    // 箭头填充（左半深、右半浅，做出立体感）
    const innerX = x - 1;
    const innerY = y - 1;
    const innerW = 22;
    const innerH = 14;

    // 箭头形状：中间宽、两端窄
    const mid = innerH / 2;
    const distFromMid = Math.abs(innerY - mid);
    const maxWidthAt = (innerH - 1) / 2;
    const halfWidth = (innerW / 2) * (1 - distFromMid / (maxWidthAt + 0.01));

    const leftEdge  = Math.floor((innerW / 2) - halfWidth);
    const rightEdge = Math.ceil((innerW / 2) + halfWidth);

    if (innerX >= leftEdge && innerX <= rightEdge) {
        // 加点渐变：左暗右亮
        const ratio = innerX / (innerW - 1);
        const r = Math.floor(180 + ratio * 60);  // 180→240
        const g = Math.floor(180 + ratio * 60);
        const b = Math.floor(180 + ratio * 60);
        return [Math.min(255, r), Math.min(255, g), Math.min(255, b)];
    }

    return C_ARROW_BG;  // 空白区
}

// ============================================================
// 生成 PNG
// ============================================================
const outPath = 'src/main/resources/assets/chemical_industry/textures/gui/fluidized_bed.png';

console.log('生成原版风格 GUI 贴图...\n');
createPng(outPath, WIDTH, HEIGHT, drawPixel);
console.log('\n完成！');
