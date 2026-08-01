package com.cxy.chemical_industry.block_entity;

import com.cxy.chemical_industry.registry.ModBlockEntities;
import com.cxy.chemical_industry.registry.ModFluids;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;

/** 空气压缩机 — 齿轮驱动，将空气压缩为压缩空气流体 */
public class AirCompressorBlockEntity extends KineticBlockEntity {

    static final int TANK_CAP = 8000;           // 容量 8 桶
    static final int MAX_FILL_PER_TICK = 100;   // 每 tick 最多产 100 mB

    final FluidTank tank = new FluidTank(TANK_CAP) {
        @Override protected void onContentsChanged() { setChanged(); }
    };

    public AirCompressorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.AIR_COMPRESSOR.get(), pos, state);
    }

    /** 每 tick 执行：转速不为 0 时产生压缩空气 */
    @Override public void tick() {
        super.tick();
        if (level == null || level.isClientSide()) return;
        float speed = getSpeed();
        if (speed == 0) return;
        // 产量 = abs(转速)/16 mB/tick，上限 100
        int amount = Math.min((int) (Math.abs(speed) / 16), MAX_FILL_PER_TICK);
        if (amount > 0)
            tank.fill(new FluidStack(ModFluids.COMPRESSED_AIR.getSource(), amount), IFluidHandler.FluidAction.EXECUTE);
    }

    /** 应力消耗：4.0 SU/RPM（与搅拌器同级） */
    @Override public float calculateStressApplied() {
        lastStressApplied = 4.0f;
        return lastStressApplied;
    }

    /** 转速最低要求：无（转速再低也能工作，只是慢） */
    @Override public boolean isSpeedRequirementFulfilled() { return true; }

    // ====== 流体能力 ======

    public IFluidHandler fluidHandler(@Nullable Direction side) {
        return new IFluidHandler() {
            @Override public int getTanks() { return 1; }
            @Override public FluidStack getFluidInTank(int t) { return tank.getFluid(); }
            @Override public int getTankCapacity(int t) { return TANK_CAP; }
            @Override public boolean isFluidValid(int t, FluidStack s) { return false; }
            @Override public int fill(FluidStack r, FluidAction a) { return 0; }  // 只产出不接受输入
            @Override public FluidStack drain(FluidStack r, FluidAction a) {
                if (r.getFluid().isSame(ModFluids.COMPRESSED_AIR.getSource()))
                    return tank.drain(r, a);
                return FluidStack.EMPTY;
            }
            @Override public FluidStack drain(int max, FluidAction a) {
                return tank.drain(max, a);
            }
        };
    }

    // ====== 持久化 ======

    @Override protected void write(CompoundTag t, HolderLookup.Provider r, boolean clientPacket) {
        super.write(t, r, clientPacket);
        t.put("Tank", tank.writeToNBT(r, new CompoundTag()));
    }
    @Override protected void read(CompoundTag t, HolderLookup.Provider r, boolean clientPacket) {
        super.read(t, r, clientPacket);
        if (t.contains("Tank")) tank.readFromNBT(r, t.getCompound("Tank"));
    }
}
