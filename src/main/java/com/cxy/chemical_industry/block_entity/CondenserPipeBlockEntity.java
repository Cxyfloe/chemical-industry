package com.cxy.chemical_industry.block_entity;

import com.cxy.chemical_industry.registry.ModBlockEntities;
import com.cxy.chemical_industry.registry.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import static com.cxy.chemical_industry.block.CondenserPipeBlock.FACING;

/** 冷凝管 — 消耗压缩空气，需要背面有鼓风机，向前方吹出冷风 */
public class CondenserPipeBlockEntity extends BlockEntity {

    private static final int TANK_CAP = 4000;
    // 每 tick 耗气量 = 2mB，与空气压缩机 32 RPM 的产量（abs(speed)/16 = 2mB/tick）持平，
    // 保证低转速下也能持续工作（原 10mB/tick 消耗远高于产量，罐子永远攒不满 → 永不结冰）
    private static final int AIR_PER_TICK = 2;
    private static final int FREEZE_TIME = 80;  // 4 秒连续冷风 → 结冰
    private static final int RANGE = 5;

    private int freezeProgress = 0;

    private final FluidTank airTank = new FluidTank(TANK_CAP, s ->
            s.getFluid().isSame(ModFluids.COMPRESSED_AIR.getSource())) {
        @Override protected void onContentsChanged() { setChanged(); }
    };

    public CondenserPipeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CONDENSER_PIPE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, CondenserPipeBlockEntity self) {
        if (level.isClientSide()) return;

        Direction facing = state.getValue(FACING);

        // 检查背面是否为鼓风机
        BlockPos behind = pos.relative(facing.getOpposite());
        String behindKey = level.getBlockState(behind).getBlock().builtInRegistryHolder().key().location().toString();
        boolean hasFan = behindKey.equals("create:encased_fan");

        if (!hasFan || self.airTank.getFluidAmount() < AIR_PER_TICK) {
            self.freezeProgress = 0;
            return;
        }

        self.airTank.drain(AIR_PER_TICK, IFluidHandler.FluidAction.EXECUTE);
        BlockPos front = pos.relative(facing);

        // ---- 扫描前方区域：水→冰、岩浆→原石 ----
        boolean foundTarget = false;
        for (int i = 0; i < RANGE; i++) {
            BlockPos target = front.relative(facing, i);
            BlockState ts = level.getBlockState(target);

            // 检查目标自身 + 上下左右相邻方块（使用世界 FluidState 确保可靠检测）
            for (BlockPos neighbor : new BlockPos[]{target, target.above(), target.below(),
                    target.north(), target.south(), target.east(), target.west()}) {
                FluidState fluid = level.getFluidState(neighbor);
                BlockState ns = level.getBlockState(neighbor);
                // 水源检测：先查 FluidState（最可靠），再查方块类型（兜底）
                if ((fluid.is(Fluids.WATER) || ns.is(Blocks.WATER)) && fluid.isSource()) {
                    foundTarget = true;
                    break;
                }
                // 岩浆源检测
                if ((fluid.is(Fluids.LAVA) || ns.is(Blocks.LAVA)) && fluid.isSource()) {
                    foundTarget = true;
                    break;
                }
                if (ns.is(Blocks.MAGMA_BLOCK)) {
                    foundTarget = true;
                    break;
                }
            }
            if (foundTarget) break;

            // 非空气非水非岩浆非冰 → 阻挡冷风（冰不阻挡，让冷风可持续冻结更深的水）
            BlockState bs = level.getBlockState(target);
            if (!bs.isAir() && !bs.is(Blocks.WATER) && !bs.is(Blocks.LAVA)
                    && !bs.is(Blocks.ICE) && !bs.is(Blocks.FROSTED_ICE)) break;
        }

        if (foundTarget) {
            self.freezeProgress++;
        } else {
            self.freezeProgress = Math.max(0, self.freezeProgress - 2);
        }

        // 结冰/固化
        if (self.freezeProgress >= FREEZE_TIME) {
            self.freezeProgress = 0;
            // 成就：第一次成功运行冷凝管
            com.cxy.chemical_industry.event.AdvancementHelper.grantNearby(level, pos, "condenser_pipe", "run");
            for (int i = 0; i < RANGE; i++) {
                BlockPos target = front.relative(facing, i);
                BlockState ts = level.getBlockState(target);
                // 冰不阻挡冷风，让冷风可以继续冻结更深的水
                if (!ts.isAir() && !ts.is(Blocks.WATER) && !ts.is(Blocks.LAVA)
                        && !ts.is(Blocks.MAGMA_BLOCK)
                        && !ts.is(Blocks.ICE) && !ts.is(Blocks.FROSTED_ICE)) break;

                // 目标自身 + 上下左右相邻方块
                for (BlockPos neighbor : new BlockPos[]{target, target.above(), target.below(),
                        target.north(), target.south(), target.east(), target.west()}) {
                    BlockState ns = level.getBlockState(neighbor);
                    FluidState fluid = level.getFluidState(neighbor);
                    // 水源 → 冰
                    if ((fluid.is(Fluids.WATER) || ns.is(Blocks.WATER)) && fluid.isSource()) {
                        level.setBlock(neighbor, Blocks.ICE.defaultBlockState(), 3);
                        break;
                    }
                    // 岩浆源 → 原石
                    if ((fluid.is(Fluids.LAVA) || ns.is(Blocks.LAVA)) && fluid.isSource()) {
                        level.setBlock(neighbor, Blocks.COBBLESTONE.defaultBlockState(), 3);
                        break;
                    }
                    // 岩浆块 → 石头
                    if (ns.is(Blocks.MAGMA_BLOCK)) {
                        level.setBlock(neighbor, Blocks.STONE.defaultBlockState(), 3);
                        break;
                    }
                }
            }
        }

        // 雪花粒子（冰不阻挡粒子，让视觉效果持续）
        if (level instanceof ServerLevel sl) {
            for (int i = 0; i < RANGE; i++) {
                BlockPos target = front.relative(facing, i);
                BlockState particleBs = level.getBlockState(target);
                if (!particleBs.isAir() && !particleBs.is(Blocks.WATER)
                        && !particleBs.is(Blocks.LAVA)
                        && !particleBs.is(Blocks.ICE)
                        && !particleBs.is(Blocks.FROSTED_ICE)) break;
                sl.sendParticles(ParticleTypes.SNOWFLAKE,
                        target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5,
                        1, 0.3, 0.3, 0.3, 0.01);
            }
        }

        // 前方生物：细雪式冻伤（无缓慢）
        BlockPos endPos = front.relative(facing, RANGE - 1);
        AABB coldZone = new AABB(
                front.getX(), front.getY(), front.getZ(),
                endPos.getX() + 1, endPos.getY() + 1, endPos.getZ() + 1);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, coldZone)) {
            // 细雪冻伤机制：逐渐增加冰冻值，满值后扣血
            e.setTicksFrozen(Math.min(e.getTicksFrozen() + 15, 300));
            if (e.getTicksFrozen() >= 300 && level.getGameTime() % 20 == 0) {
                e.hurt(level.damageSources().freeze(), 1.0f);
            }
        }
    }

    // ---- 流体 I/O（任意面输入） ----
    public IFluidHandler fluidHandler(Direction side) {
        return new IFluidHandler() {
            @Override public int getTanks() { return 1; }
            @Override public FluidStack getFluidInTank(int t) { return airTank.getFluid(); }
            @Override public int getTankCapacity(int t) { return TANK_CAP; }
            @Override public boolean isFluidValid(int t, FluidStack s) { return airTank.isFluidValid(s); }
            @Override public int fill(FluidStack r, FluidAction a) { return airTank.fill(r, a); }
            @Override public FluidStack drain(FluidStack r, FluidAction a) { return FluidStack.EMPTY; }
            @Override public FluidStack drain(int max, FluidAction a) { return FluidStack.EMPTY; }
        };
    }

    public FluidTank getTank() { return airTank; }

    @Override protected void saveAdditional(CompoundTag t, HolderLookup.Provider r) {
        super.saveAdditional(t, r);
        t.put("Air", airTank.writeToNBT(r, new CompoundTag()));
        t.putInt("Freeze", freezeProgress);
    }
    @Override protected void loadAdditional(CompoundTag t, HolderLookup.Provider r) {
        super.loadAdditional(t, r);
        if (t.contains("Air")) airTank.readFromNBT(r, t.getCompound("Air"));
        freezeProgress = t.getInt("Freeze");
    }
}
