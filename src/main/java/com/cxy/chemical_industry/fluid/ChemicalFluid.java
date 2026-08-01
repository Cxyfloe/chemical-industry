package com.cxy.chemical_industry.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.fluids.FluidType;

import java.util.function.Supplier;

/**
 * 化学流体 — 本模组所有自定义流体的基类
 *
 * 【设计思路】
 * 不每种化学物质单独写一个流体类。所有化学流体共用这一个类，
 * 通过构造器传入不同的参数（流体类型、方块、桶物品）来区分。
 *
 * 【继承关系】
 * FlowingFluid → Minecraft 流体基类（水源/岩浆就是这个）
 *
 * 【Source vs Flowing】
 * Minecraft 的流体系统区分两种状态：
 * - Source：源方块（满的一格流体，可以从中取水）
 * - Flowing：流动状态（从源头向低处扩散的流体）
 * 两者是独立的方块，但在代码中通过同一个 ChemicalFluid 类的不同实例来处理。
 */
public abstract class ChemicalFluid extends FlowingFluid {

    /** 流体类型（密度、黏度、温度等物理属性） */
    private final Supplier<FluidType> fluidType;
    /** 流体对应的方块（源方块） */
    private final Supplier<LiquidBlock> block;
    /** 流体对应的桶物品 */
    private final Supplier<Item> bucket;
    /** 流动方块（梯度扩散用） */
    private final Supplier<FlowingFluid> flowing;
    /** 源流体（取水用） */
    private final Supplier<FlowingFluid> source;

    /**
     * @param fluidType 流体类型
     * @param block     源方块
     * @param bucket    桶物品
     * @param flowing   流动版本
     * @param source    源版本
     */
    protected ChemicalFluid(Supplier<FluidType> fluidType,
                            Supplier<LiquidBlock> block,
                            Supplier<Item> bucket,
                            Supplier<FlowingFluid> flowing,
                            Supplier<FlowingFluid> source) {
        this.fluidType = fluidType;
        this.block = block;
        this.bucket = bucket;
        this.flowing = flowing;
        this.source = source;
    }

    // ---------- 属性 ----------

    @Override
    public FluidType getFluidType() {
        return fluidType.get();
    }

    @Override
    public Item getBucket() {
        return bucket.get();
    }

    @Override
    public Fluid getFlowing() {
        return flowing.get();
    }

    @Override
    public Fluid getSource() {
        return source.get();
    }

    // ---------- 方块行为 ----------

    @Override
    protected BlockState createLegacyBlock(FluidState state) {
        // 流体源 → 方块状态
        return block.get().defaultBlockState()
                .setValue(LiquidBlock.LEVEL, getLegacyLevel(state));
    }

    @Override
    public boolean isSame(Fluid fluid) {
        // 判断是否是同一种流体（源和流动都算）
        return fluid == source.get() || fluid == flowing.get();
    }

    // ---------- 物理属性 ----------

    @Override
    protected boolean canConvertToSource(Level level) {
        // 设为 false：流体不会像水一样在两个源之间自动生成新源
        // 保持流体总量守恒，更像化学溶液的行为
        return false;
    }

    @Override
    protected void beforeDestroyingBlock(LevelAccessor level, BlockPos pos, BlockState state) {
        // 流体冲走方块时，掉落方块的物品（和水流、岩浆流一致）
        BlockEntity blockEntity = state.hasBlockEntity() ? level.getBlockEntity(pos) : null;
        Block.dropResources(state, level, pos, blockEntity);
    }

    @Override
    protected int getSlopeFindDistance(LevelReader level) {
        // 流体扩散距离（和水相同 = 4 格）
        return 4;
    }

    @Override
    protected int getDropOff(LevelReader level) {
        // 流体每扩散一格减少的深度（和水相同 = 1）
        return 1;
    }

    @Override
    public int getTickDelay(LevelReader level) {
        // 流体扩散速度（tick 间隔，越小越快，水 = 5，岩浆 = 30）
        // 化学溶液扩散速度设为 8，比水慢一点
        return 8;
    }

    @Override
    protected float getExplosionResistance() {
        // 防爆性（水 = 100）
        return 100.0f;
    }

    @Override
    protected boolean canBeReplacedWith(FluidState state, BlockGetter level,
                                        BlockPos pos, Fluid fluid, Direction direction) {
        // 严格隔离：不允许任何其他流体替换
        // 溶液和水/溶液不混合，像油和水一样分相
        // 化学反应只在机器（工作盆/混合器）内发生
        return false;
    }

    // ---------- Source（源流体）----------
    public static class Source extends ChemicalFluid {
        public Source(Supplier<FluidType> fluidType,
                      Supplier<LiquidBlock> block,
                      Supplier<Item> bucket,
                      Supplier<FlowingFluid> flowing,
                      Supplier<FlowingFluid> source) {
            super(fluidType, block, bucket, flowing, source);
        }

        @Override
        public boolean isSource(FluidState state) {
            return true;
        }

        @Override
        public int getAmount(FluidState state) {
            // 源方块始终是满的（8 格）
            return 8;
        }
    }

    // ---------- Flowing（流动流体）----------
    public static class Flowing extends ChemicalFluid {
        public Flowing(Supplier<FluidType> fluidType,
                       Supplier<LiquidBlock> block,
                       Supplier<Item> bucket,
                       Supplier<FlowingFluid> flowing,
                       Supplier<FlowingFluid> source) {
            super(fluidType, block, bucket, flowing, source);
        }

        @Override
        public boolean isSource(FluidState state) {
            return false;
        }

        @Override
        public int getAmount(FluidState state) {
            // 流动状态下从 LEVEL 属性读取
            return state.getValue(LEVEL);
        }

        @Override
        protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
            super.createFluidStateDefinition(builder);
            builder.add(LEVEL);
        }
    }
}
