// ============================================================
// generate_textures.js — 生成占位贴图
// 根据构思文档更新：4 种矿石 + 8 种物品
// ============================================================

const zlib = require('zlib');
const fs = require('fs');
const path = require('path');

function createPng(filePath, r, g, b) {
    const width = 16, height = 16;

    function makeChunk(chunkType, data) {
        const length = Buffer.alloc(4);
        length.writeUInt32BE(data.length);
        const typeAndData = Buffer.concat([chunkType, data]);
        const crc = Buffer.alloc(4);
        crc.writeUInt32BE(zlib.crc32(typeAndData));
        return Buffer.concat([length, typeAndData, crc]);
    }

    const signature = Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]);

    const ihdrData = Buffer.alloc(13);
    ihdrData.writeUInt32BE(width, 0);
    ihdrData.writeUInt32BE(height, 4);
    ihdrData.writeUInt8(8, 8);
    ihdrData.writeUInt8(2, 9);
    ihdrData.writeUInt8(0, 10);
    ihdrData.writeUInt8(0, 11);
    ihdrData.writeUInt8(0, 12);

    const rawRows = [];
    for (let y = 0; y < height; y++) {
        const row = Buffer.alloc(1 + width * 3);
        row.writeUInt8(0, 0);
        for (let x = 0; x < width; x++) {
            const offset = 1 + x * 3;
            row.writeUInt8(r, offset);
            row.writeUInt8(g, offset + 1);
            row.writeUInt8(b, offset + 2);
        }
        rawRows.push(row);
    }
    const compressed = zlib.deflateSync(Buffer.concat(rawRows));

    const iendData = Buffer.alloc(0);
    const pngBuffer = Buffer.concat([
        signature,
        makeChunk(Buffer.from('IHDR'), ihdrData),
        makeChunk(Buffer.from('IDAT'), compressed),
        makeChunk(Buffer.from('IEND'), iendData)
    ]);

    const dir = path.dirname(filePath);
    if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true });
    fs.writeFileSync(filePath, pngBuffer);
    console.log(`  OK  ${filePath}`);
}

const base = 'src/main/resources/assets/chemical_industry/textures';

console.log('Generating textures...\n');

// ---- 方块贴图 ----
console.log('[Blocks]');
createPng(`${base}/block/sulfur_ore.png`,    200, 185, 50);   // 黄色 (硫磺)
createPng(`${base}/block/pyrite_ore.png`,    195, 175, 70);   // 金色 (黄铁)
createPng(`${base}/block/rock_salt_ore.png`, 210, 200, 190);  // 灰白 (岩盐)
createPng(`${base}/block/niter_ore.png`,     190, 185, 180);  // 灰白晶体 (硝石)

// ---- 物品贴图 ----
console.log('[Items]');
// 矿石掉落物
createPng(`${base}/item/sulfur_powder.png`,      230, 210, 60);   // 黄色粉末
createPng(`${base}/item/raw_pyrite.png`,         200, 180, 80);   // 金色原矿
createPng(`${base}/item/sodium_chloride.png`,    250, 250, 245);  // 白色晶体
createPng(`${base}/item/potassium_nitrate.png`,  220, 215, 205);  // 灰白粉末
// 化工产品
createPng(`${base}/item/iron_oxide.png`,          180, 70, 50);    // 红棕色
createPng(`${base}/item/sodium_hydroxide.png`,   240, 235, 230);  // 白色颗粒
// 气体瓶
createPng(`${base}/item/chlorine_bottle.png`,    180, 220, 100);  // 黄绿色瓶
createPng(`${base}/item/hydrogen_bottle.png`,    200, 210, 230);  // 淡蓝瓶

// ---- 罂粟 ----
createPng(`${base}/block/opium_poppy_stage0.png`,  90, 160, 60);    // 小绿苗
createPng(`${base}/block/opium_poppy_stage1.png`,  80, 150, 55);    // 中苗
createPng(`${base}/block/opium_poppy_stage2.png`,  70, 140, 50);    // 带花苞苗
createPng(`${base}/block/opium_poppy_stage3.png`,  210, 60, 90);    // 红色花朵（成熟）
createPng(`${base}/item/opium_poppy_fruit.png`,    150, 110, 70);   // 棕色果实
createPng(`${base}/item/opium.png`,                 90, 60, 45);    // 深棕鸦片膏

// 删除旧贴图
for (const old of ['salt.png', 'soda_ash.png']) {
    const p = `${base}/item/${old}`;
    if (fs.existsSync(p)) { fs.unlinkSync(p); console.log(`  DEL ${p}`); }
}

console.log('\nDone!');

// ---- 氢氟酸 ----
createPng(`${base}/block/hydrofluoric_acid_still.png`, 150, 200, 120);  // 淡黄绿色
createPng(`${base}/block/hydrofluoric_acid_flow.png`,  150, 200, 120);
createPng(`${base}/item/hydrofluoric_acid_bucket.png`, 150, 200, 120);

// ---- 朱砂 + 水银 ----
createPng(`${base}/block/cinnabar_ore.png`,        200, 40, 40);     // 鲜红色
createPng(`${base}/block/deepslate_cinnabar_ore.png`, 150, 60, 60);  // 深色板岩红
createPng(`${base}/block/mercury_still.png`,       200, 205, 210);   // 银白色
createPng(`${base}/block/mercury_flow.png`,        200, 205, 210);
createPng(`${base}/item/mercury_bucket.png`,       200, 205, 210);

// ---- 朱砂物品 ----
createPng(`${base}/item/cinnabar.png`, 200, 40, 40);   // 鲜红色矿石

// ---- 银块/硬铝块 ----
createPng(`${base}/block/silver_block.png`,    210, 210, 215);  // 银白色
createPng(`${base}/block/duralumin_block.png`, 175, 180, 185);  // 灰白金属
