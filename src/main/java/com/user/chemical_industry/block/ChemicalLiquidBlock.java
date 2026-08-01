package com.user.chemical_industry.block;

import com.user.chemical_industry.registry.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;

/**
 * 化学流体方块 — 腐蚀 + 化学反应
 *
 * 【流体接触反应】
 * 1. 硫酸 + NaOH溶液 → 爆炸 2.5 + 硫酸钠溶液 (Na₂SO₄)
 * 2. 盐酸 + NaOH溶液 → 中和无爆炸 → NaCl 溶液
 * 3. 硝酸 + NaOH溶液 → 中和无爆炸 → 硝酸钠溶液 (NaNO₃)
 * 4. 硫酸 + 水 → 爆炸 2.0
 * 5. Cl₂ + H₂ → 剧烈爆炸 3.5
 *
 * 【腐蚀】所有酸/碱液体接触生物都会造成伤害+腐蚀+护甲磨损（含流动）
 */
public class ChemicalLiquidBlock extends LiquidBlock {

    private final String fluidName;

    public ChemicalLiquidBlock(FlowingFluid fluid, Properties properties, String fluidName) {
        super(fluid, properties);
        this.fluidName = fluidName;
    }

    // 源方块也会随机刻——给酸腐蚀铁块多次机会
    @Override protected boolean isRandomlyTicking(BlockState s) { return true; }
    @Override
    public void randomTick(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos,
                           net.minecraft.util.RandomSource random) {
        if (state.getValue(LEVEL) == 0) reactWithNeighbors(state, level, pos);
    }

    // ---------- 放置和邻居变化时检查反应 ----------

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos,
                        BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide() && state.getValue(LEVEL) == 0) {
            reactWithNeighbors(state, level, pos);
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos,
                                   Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (!level.isClientSide() && state.hasProperty(LEVEL) && state.getValue(LEVEL) == 0) {
            reactWithNeighbors(state, level, pos);
        }
    }

    /**
     * 检查四邻方块，触发化学反应
     */
    private void reactWithNeighbors(BlockState state, Level level, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);
            BlockState neighborState = level.getBlockState(neighborPos);

            // ---- 酸-金属反应（固体方块检查，无需是液体）----
            String blockKey = net.minecraft.core.registries.BuiltInRegistries.BLOCK
                    .getKey(neighborState.getBlock()).toString();
            reactMetal(level, pos, neighborPos, blockKey);
            if (level.getBlockState(pos).isAir()) return; // 自身被反应掉了

            if (!(neighborState.getBlock() instanceof LiquidBlock)) continue;
            if (!neighborState.hasProperty(LEVEL) || neighborState.getValue(LEVEL) != 0) continue;

            String neighborName = getFluidName(neighborState);
            if (neighborName == null) continue;

            // ---- 规则 1: 硫酸 + NaOH → 爆炸 + 生成硫酸钠溶液 (Na₂SO₄) ----
            // 浓硫酸遇水/碱剧烈放热，所以带爆炸。
            boolean acidBase = (fluidName.equals("sulfuric_acid")
                    && neighborName.equals("sodium_hydroxide_solution"))
                || (fluidName.equals("sodium_hydroxide_solution")
                    && neighborName.equals("sulfuric_acid"));

            if (acidBase) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                level.setBlock(neighborPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                // 在两格中点引爆
                level.explode(null,
                        (pos.getX() + neighborPos.getX()) / 2.0 + 0.5,
                        (pos.getY() + neighborPos.getY()) / 2.0 + 0.5,
                        (pos.getZ() + neighborPos.getZ()) / 2.0 + 0.5,
                        2.5f, Level.ExplosionInteraction.BLOCK);
                // 爆炸后在原位生成硫酸钠溶液
                BlockState na2so4 = ModFluids.SODIUM_SULFATE_SOLUTION.getSource()
                        .defaultFluidState().createLegacyBlock();
                level.setBlock(pos, na2so4, Block.UPDATE_ALL);
                level.setBlock(neighborPos, na2so4, Block.UPDATE_ALL);
                return;
            }

            // ---- 规则 1b: 盐酸 + NaOH → 中和（无爆炸）→ NaCl 溶液 ----
            boolean hclBase = (fluidName.equals("hydrochloric_acid")
                    && neighborName.equals("sodium_hydroxide_solution"))
                || (fluidName.equals("sodium_hydroxide_solution")
                    && neighborName.equals("hydrochloric_acid"));
            if (hclBase) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                level.setBlock(neighborPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                BlockState nacl = ModFluids.NACL_SOLUTION.getSource()
                        .defaultFluidState().createLegacyBlock();
                level.setBlock(pos, nacl, Block.UPDATE_ALL);
                level.setBlock(neighborPos, nacl, Block.UPDATE_ALL);
                return;
            }

            // ---- 规则 1c: 硝酸 + NaOH → 中和（无爆炸）→ 硝酸钠溶液 (NaNO₃) ----
            boolean hno3Base = (fluidName.equals("nitric_acid")
                    && neighborName.equals("sodium_hydroxide_solution"))
                || (fluidName.equals("sodium_hydroxide_solution")
                    && neighborName.equals("nitric_acid"));
            if (hno3Base) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                level.setBlock(neighborPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                BlockState nano3 = ModFluids.SODIUM_NITRATE_SOLUTION.getSource()
                        .defaultFluidState().createLegacyBlock();
                level.setBlock(pos, nano3, Block.UPDATE_ALL);
                level.setBlock(neighborPos, nano3, Block.UPDATE_ALL);
                return;
            }

            // ---- 规则 2: 硫酸 + 水 → 爆炸 ----
            boolean acidWater = (fluidName.equals("sulfuric_acid") && neighborName.equals("water"))
                || (neighborName.equals("sulfuric_acid") && fluidName.equals("water"));

            if (acidWater) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                level.setBlock(neighborPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                level.explode(null,
                        (pos.getX() + neighborPos.getX()) / 2.0 + 0.5,
                        (pos.getY() + neighborPos.getY()) / 2.0 + 0.5,
                        (pos.getZ() + neighborPos.getZ()) / 2.0 + 0.5,
                        2.0f, Level.ExplosionInteraction.BLOCK);
                // 成就：第一次硫酸遇水爆炸
                com.user.chemical_industry.event.AdvancementHelper.grantNearby(level, pos, "acid_into_water", "acid_water");
                return;
            }

            // ---- 规则 3: 氯气 + 氢气 → 剧烈爆炸 (3.5) ----
            boolean gasExplosion = (fluidName.equals("chlorine_gas") && neighborName.equals("hydrogen_gas"))
                || (fluidName.equals("hydrogen_gas") && neighborName.equals("chlorine_gas"));

            if (gasExplosion) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                level.setBlock(neighborPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                level.explode(null,
                        (pos.getX() + neighborPos.getX()) / 2.0 + 0.5,
                        (pos.getY() + neighborPos.getY()) / 2.0 + 0.5,
                        (pos.getZ() + neighborPos.getZ()) / 2.0 + 0.5,
                        3.5f, Level.ExplosionInteraction.BLOCK);
                return;
            }
        }
    }

    // ---------- 实体接触：所有酸碱液体（含流动部分）都会伤害 ----------

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        super.entityInside(state, level, pos, entity);
        if (level.isClientSide()) return;
        if (!(entity instanceof LivingEntity living)) return;
        if (!isCorrosiveFluid(fluidName)) return;

        long tick = level.getGameTime();
        // 硫酸 = 2级腐蚀（更高伤害、更快护甲磨损），其他 = 1级
        int corrLevel = fluidName.equals("sulfuric_acid") ? 1 : 0; // amplifier: 0=Lv1, 1=Lv2

        // 氯气特殊处理
        if (fluidName.equals("chlorine_gas")) {
            if (tick % 10 == 0) {
                living.hurt(level.damageSources().generic(), 1.0f);
                applyCorrosion(living, corrLevel);
                applyPoison(living);
            }
            if (tick % 20 == 0) damageArmor(living, corrLevel);
            return;
        }

        // 所有酸/碱液体：伤害 + 腐蚀 + 中毒
        if (tick % 10 == 0) {
            living.hurt(level.damageSources().generic(), 1.0f);
            applyCorrosion(living, corrLevel);
            applyPoison(living);
        }
        if (tick % 20 == 0) damageArmor(living, corrLevel);
    }

    /** 判断流体是否为腐蚀性酸/碱液体 */
    private static boolean isCorrosiveFluid(String name) {
        return name.equals("sulfuric_acid")
                || name.equals("sodium_hydroxide_solution")
                || name.equals("hydrochloric_acid")
                || name.equals("nitric_acid")
                || name.equals("sodium_aluminate_solution")
                || name.equals("chlorine_gas");
    }

    /** 施加腐蚀效果（支持分级） */
    private static void applyCorrosion(LivingEntity living, int amplifier) {
        living.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                com.user.chemical_industry.registry.ModEffects.corrosionHolder(), 100, amplifier));
    }

    /** 施加中毒效果 */
    private static void applyPoison(LivingEntity living) {
        living.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.POISON, 60, 0));
    }

    /** 腐蚀护甲（amplifier: 0=Lv1, 1=Lv2，影响耐久消耗速度） */
    private void damageArmor(LivingEntity living, int amplifier) {
        int dmg = amplifier >= 1 ? 4 : 2; // 硫酸腐蚀更快
        for (EquipmentSlot s : EquipmentSlot.values()) {
            if (s.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
                ItemStack a = living.getItemBySlot(s);
                if (!a.isEmpty()) a.hurtAndBreak(dmg, living, s);
            }
        }
    }

    /** 酸-金属 & 碱-铝 反应（锌不被酸腐蚀！） */
    private void reactMetal(Level level, BlockPos pos, BlockPos neighborPos, String blockKey) {
        // HCl + 铁系方块 → 逐渐腐蚀（随机概率 + 计划刻重试）
        if (fluidName.equals("hydrochloric_acid")) {
            if (isIronBlock(blockKey)) {
                if (level.random.nextFloat() < 0.15f) { // 15%概率腐蚀，流体每次流动都会重试
                    level.setBlock(neighborPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                    level.setBlock(pos, Blocks.WATER.defaultBlockState(), Block.UPDATE_ALL);
                }
                return;
            }
            // 锌及其衍生物：不反应！
            if (blockKey.startsWith("create:zinc")) return;
        }
        // H₂SO₄ + 锌：不反应（锌耐酸）
        if (fluidName.equals("sulfuric_acid") && blockKey.startsWith("create:zinc")) return;
        // HNO₃ + 锌：不反应（锌耐酸）
        if (fluidName.equals("nitric_acid") && blockKey.startsWith("create:zinc")) return;
        // NaOH 溶液 + 铝块 → 铝块消失
        if (fluidName.equals("sodium_hydroxide_solution")) {
            if (blockKey.equals("chemical_industry:aluminum_block")) {
                level.setBlock(neighborPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                return;
            }
        }
    }

    /** 判断是否为可被盐酸腐蚀的铁系方块 */
    private static boolean isIronBlock(String key) {
        return key.equals("minecraft:iron_block")
                || key.equals("minecraft:iron_door")
                || key.equals("minecraft:iron_trapdoor")
                || key.equals("minecraft:iron_bars")
                || key.equals("minecraft:anvil")
                || key.equals("minecraft:chipped_anvil")
                || key.equals("minecraft:damaged_anvil")
                || key.equals("minecraft:heavy_weighted_pressure_plate");
    }

    private String getFluidName(BlockState state) {
        ResourceLocation key = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return key.getPath();
    }
}
