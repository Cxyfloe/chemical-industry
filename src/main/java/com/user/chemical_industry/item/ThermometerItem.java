package com.user.chemical_industry.item;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

/**
 * 温度计 — 水银的用途之一。
 *
 * 类似指南针的"活物品"：客户端注册 temperature 属性（0~1），
 * 模型按属性值切换 16 帧指针贴图（0=蓝冷 → 1=红热）。
 *
 * 温度算法 = 多因素综合（都在 getTemperatureValue 里）：
 *   基准 0.5（温和）＋ 维度 ＋ 群系温度 ＋ 高度（雪线以上越冷）
 *   ＋ 周围方块扫描（热源/冷源，按距离衰减）
 */
public class ThermometerItem extends Item {

    /** 周围方块扫描半径（格） */
    private static final int SCAN_RADIUS = 4;

    public ThermometerItem(Properties properties) {
        super(properties);
    }

    /**
     * 计算指定位置的环境温度，返回 0~1（0=极冷，1=极热）
     * 只在客户端调用（物品渲染属性）
     *
     * @param level 客户端世界
     * @param pos   玩家所在位置
     * @return 0~1 的温度值
     */
    public static float getTemperatureValue(ClientLevel level, BlockPos pos) {
        float temp = 0.5F; // 基准：温和

        // ---------- ① 维度 ----------
        if (level.dimension() == Level.NETHER) {
            temp += 0.6F;                  // 下界：直接顶到红色区
        } else if (level.dimension() == Level.END) {
            temp -= 0.1F;                  // 末地：略冷
        }

        // ---------- ② 群系温度 ----------
        // 群系基准温度约 -0.7（雪原）~ 2.0（沙漠），0.5 附近是温和
        float biomeTemp = level.getBiome(pos).value().getBaseTemperature();
        temp += (biomeTemp - 0.5F) * 0.15F;

        // ---------- ③ 高度（雪线以上越高越冷） ----------
        if (pos.getY() > 96) {
            temp -= (pos.getY() - 96) * 0.004F;
        }

        // ---------- ④ 周围方块扫描（热源/冷源，按距离衰减） ----------
        int r = SCAN_RADIUS;
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    BlockState state = level.getBlockState(pos.offset(dx, dy, dz));
                    if (state.isAir()) continue;

                    // 距离衰减权重：越近影响越大（1 ~ 0）
                    float weight = 1.0F - (float) Math.sqrt(dx * dx + dy * dy + dz * dz) / r;
                    if (weight <= 0) continue;

                    temp += heatContribution(state) * weight;
                }
            }
        }

        // 结果限定在 0~1
        return Mth.clamp(temp, 0.0F, 1.0F);
    }

    /**
     * 单个方块的热量贡献（正=热源，负=冷源）
     */
    private static float heatContribution(BlockState state) {
        // ---------- 热源 ----------
        if (state.is(Blocks.LAVA)) {
            return 0.12F;                  // 岩浆（含流动岩浆）
        }
        if (state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE) || state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE)) {
            return 0.08F;                  // 火/营火
        }
        if (state.is(Blocks.FURNACE) || state.is(Blocks.SMOKER) || state.is(Blocks.BLAST_FURNACE)) {
            // 只有点燃的熔炉类才发热
            return state.getValue(AbstractFurnaceBlock.LIT) ? 0.06F : 0.0F;
        }
        if (state.getBlockHolder().is(ResourceLocation.fromNamespaceAndPath("create", "blaze_burner"))) {
            // 烈焰人燃烧室（Create）：heat_level 不是 none 就是点燃状态
            return isLitBlazeBurner(state) ? 0.10F : 0.0F;
        }
        // 本模组的沸腾炉（点燃状态视为热源）——沸腾炉没有 LIT 属性，跳过

        // ---------- 冷源 ----------
        if (state.is(Blocks.SNOW) || state.is(Blocks.SNOW_BLOCK) || state.is(Blocks.POWDER_SNOW)) {
            return -0.08F;                 // 雪/雪块/粉末雪
        }
        if (state.is(Blocks.ICE) || state.is(Blocks.FROSTED_ICE) || state.is(Blocks.PACKED_ICE) || state.is(Blocks.BLUE_ICE)) {
            return -0.07F;                 // 冰/浮冰/蓝冰
        }
        return 0.0F;
    }

    /**
     * 判断烈焰人燃烧室是否点燃（热度属性不是 none）
     * Create 6.0.9 属性名是 "blaze"（见 BlazeBurnerBlock.java:72），
     * 兼容旧版/其他模组可能用的 "heat_level" 名
     */
    private static boolean isLitBlazeBurner(BlockState state) {
        for (Property<?> property : state.getProperties()) {
            if (property.getName().equals("blaze") || property.getName().equals("heat_level")) {
                return !"none".equals(state.getValue(property).toString());
            }
        }
        return false;
    }
}
