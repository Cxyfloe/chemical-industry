package com.cxy.chemical_industry.block_entity;

import com.cxy.chemical_industry.block.ElectrolyzerBlock;
import com.cxy.chemical_industry.registry.*;
import com.cxy.chemical_industry.screen.ElectrolyzerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import static com.cxy.chemical_industry.block.ElectrolyzerBlock.*;

public class ElectrolyzerBlockEntity extends BlockEntity implements MenuProvider {

    static final int PER_BLOCK_MB = 4000, FLUID_PER_OP = 250, PER_ROW_ENERGY = 20000, ENERGY_PER_TICK = 100, PROCESS_TIME = 200;
    static final int MAX_TANK = PER_BLOCK_MB * 9; // 3×3

    /** 向后扫描计数同列电解槽层数 */
    int depthCount() {
        int n = 1; Direction back = facing().getOpposite();
        BlockPos p = worldPosition.relative(back);
        while (level != null && level.getBlockState(p).is(ModBlocks.ELECTROLYZER.get())
                && level.getBlockState(p).getValue(FACING) == facing()) { n++; p = p.relative(back); }
        return Math.min(n, 3);
    }
    int tankCap() { return depthCount() * 3 * PER_BLOCK_MB; } // 3列×N层×4b
    int energyCap() { return depthCount() * PER_ROW_ENERGY; }

    static final int SLOT_CATHODE = 0, SLOT_ANODE = 1, SLOT_CATALYST = 2, SLOT_MEMBRANE = 3, SLOT_FILTER = 4;
    static final int SLOT_OUT = 5, TOTAL_SLOTS = 7;

    private final ItemStackHandler items = new ItemStackHandler(TOTAL_SLOTS) {
        @Override protected void onContentsChanged(int s) { setChanged(); }
        @Override public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == SLOT_CATHODE || slot == SLOT_ANODE) return isElectrode(stack);
            if (slot == SLOT_MEMBRANE) return stack.is(ModItems.CATION_EXCHANGE_MEMBRANE.get());
            if (slot == SLOT_FILTER) return true; // 固槽：开放所有物品
            if (slot >= SLOT_OUT) return false;
            return true;
        }
        @Override public int getSlotLimit(int slot) { return (slot >= 2 && slot <= 3) ? 1 : super.getSlotLimit(slot); }
    };

    final FluidTank waterIn  = new FluidTank(MAX_TANK) { @Override protected void onContentsChanged() { setChanged(); } };
    final FluidTank naohOut  = new FluidTank(MAX_TANK) { @Override protected void onContentsChanged() { setChanged(); } };
    final FluidTank h2Out    = new FluidTank(MAX_TANK) { @Override protected void onContentsChanged() { setChanged(); } };
    final FluidTank naclIn   = new FluidTank(MAX_TANK) { @Override protected void onContentsChanged() { setChanged(); } };
    final FluidTank waterOut = new FluidTank(MAX_TANK) { @Override protected void onContentsChanged() { setChanged(); } };
    final FluidTank cl2Out   = new FluidTank(MAX_TANK) { @Override protected void onContentsChanged() { setChanged(); } };
    final FluidTank o2Out    = new FluidTank(MAX_TANK) { @Override protected void onContentsChanged() { setChanged(); } };

    void syncCaps() { int c = tankCap(); waterIn.setCapacity(c); naohOut.setCapacity(c); h2Out.setCapacity(c); naclIn.setCapacity(c); waterOut.setCapacity(c); cl2Out.setCapacity(c); o2Out.setCapacity(c); }

    private int energy, progress;

    public final ContainerData data = new ContainerData() {
        @Override public int get(int i) { return i == 0 ? progress : PROCESS_TIME; }
        @Override public void set(int i, int v) { if (i == 0) progress = v; }
        @Override public int getCount() { return 2; }
    };

    public ElectrolyzerBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.ELECTROLYZER.get(), pos, state); }

    public boolean isPart(Part p) { return getBlockState().getValue(PART) == p; }
    private Direction facing() { return getBlockState().getValue(FACING); }
    /** 正面（FRONT）没有电解槽挡在前面 */
    public boolean isFront() { return level != null && !level.getBlockState(worldPosition.relative(facing())).is(ModBlocks.ELECTROLYZER.get()); }

    @Nullable BlockPos findPart(Part target, Direction dir) {
        BlockPos np = worldPosition.relative(dir);
        BlockState ns = level != null ? level.getBlockState(np) : null;
        if (ns != null && ns.is(ModBlocks.ELECTROLYZER.get()) && ns.getValue(PART) == target && ns.getValue(FACING) == facing()) return np;
        return null;
    }
    @Nullable BlockPos cathPos() { return isPart(Part.CENTER) ? findPart(Part.CATHODE, facing().getCounterClockWise()) : null; }
    @Nullable BlockPos anodPos() { return isPart(Part.CENTER) ? findPart(Part.ANODE, facing().getClockWise()) : null; }
    @Nullable BlockPos centerPos() {
        if (isPart(Part.CENTER)) return worldPosition;
        Direction toCenter = isPart(Part.CATHODE) ? facing().getClockWise() : facing().getCounterClockWise();
        return findPart(Part.CENTER, toCenter);
    }
    @Nullable ElectrolyzerBlockEntity getCenter() { return getNeighbor(centerPos()); }
    /** 向前扫描找到最前排的同列 BE（有 isFront() 的那个） */
    @Nullable ElectrolyzerBlockEntity findFront() {
        if (isFront()) return this;
        if (level == null) return null;
        BlockPos p = worldPosition.relative(facing());
        while (level.getBlockState(p).is(ModBlocks.ELECTROLYZER.get())
                && level.getBlockState(p).getValue(FACING) == facing()) {
            BlockEntity be = level.getBlockEntity(p);
            if (be instanceof ElectrolyzerBlockEntity e && e.isFront()) return e;
            p = p.relative(facing());
        }
        return null;
    }
    // ---- 槽位委托到前排 ----
    private FluidTank frontWaterIn()  { ElectrolyzerBlockEntity f = findFront(); return f!=null ? f.waterIn  : waterIn; }
    private FluidTank frontNaOHOut() { ElectrolyzerBlockEntity f = findFront(); return f!=null ? f.naohOut  : naohOut; }
    private FluidTank frontH2Out()   { ElectrolyzerBlockEntity f = findFront(); return f!=null ? f.h2Out    : h2Out; }
    private FluidTank frontNaClIn()  { ElectrolyzerBlockEntity f = findFront(); return f!=null ? f.naclIn   : naclIn; }
    private FluidTank frontWaterOut(){ ElectrolyzerBlockEntity f = findFront(); return f!=null ? f.waterOut : waterOut; }
    private FluidTank frontCl2Out()  { ElectrolyzerBlockEntity f = findFront(); return f!=null ? f.cl2Out   : cl2Out; }
    private FluidTank frontO2Out()   { ElectrolyzerBlockEntity f = findFront(); return f!=null ? f.o2Out    : o2Out; }
    @Nullable ElectrolyzerBlockEntity getNeighbor(@Nullable BlockPos p) { return (p != null && level != null && level.getBlockEntity(p) instanceof ElectrolyzerBlockEntity e) ? e : null; }
    boolean structureOk() { return isPart(Part.CENTER) && cathPos() != null && anodPos() != null; }

    // ====== Tick ======
    public static void tick(Level level, BlockPos pos, BlockState state, ElectrolyzerBlockEntity self) {
        if (level.isClientSide()) return;
        self.syncCaps();
        // 只有最前排 CENTER 执行处理逻辑
        if (!self.isPart(Part.CENTER) || !self.structureOk() || !self.isFront()) { self.progress = 0; return; }
        ElectrolyzerBlockEntity cath = self.getNeighbor(self.cathPos());
        ElectrolyzerBlockEntity anod = self.getNeighbor(self.anodPos());
        if (cath == null || anod == null) { self.progress = 0; return; }
        int depth = self.depthCount();
        int ept = ENERGY_PER_TICK * depth;     // 能耗随层数线性增长
        if (cath.energy < ept || anod.energy < ept) { self.progress = 0; return; }
        boolean isAluPre = self.items.getStackInSlot(SLOT_CATALYST).is(ModItems.CRYOLITE.get())
                && self.items.getStackInSlot(SLOT_CATHODE).is(ModItems.GRAPHITE_ROD.get())
                && self.items.getStackInSlot(SLOT_ANODE).is(ModItems.GRAPHITE_ROD.get())
                && self.items.getStackInSlot(SLOT_FILTER).is(ModItems.ALUMINA.get())
                && hasBlazeBurner(level, pos);
        // 配方 6：熔融 NaCl（热源 + 固体NaCl + 石墨电极）
        boolean isMolten = !isAluPre && hasBlazeBurner(level, pos)
                && self.items.getStackInSlot(SLOT_CATHODE).is(ModItems.GRAPHITE_ROD.get())
                && self.items.getStackInSlot(SLOT_ANODE).is(ModItems.GRAPHITE_ROD.get())
                && self.items.getStackInSlot(SLOT_FILTER).is(ModItems.SODIUM_CHLORIDE.get());
        boolean cuCath = cath.waterIn.getFluid().getFluid().isSame(ModFluids.COPPER_SULFATE_SOLUTION.getSource());
        boolean cuAnod = anod.naclIn.getFluid().getFluid().isSame(ModFluids.COPPER_SULFATE_SOLUTION.getSource());
        boolean isCu = cuCath || cuAnod;
        // 配方 7：电解水（阴阳极都通入水/NaOH溶液/硫酸 → H₂ + O₂）
        boolean isWaterElec = !isAluPre && !isMolten && !isCu
                && isElectrolyte(cath.waterIn.getFluid()) && isElectrolyte(anod.naclIn.getFluid())
                && !cath.waterIn.isEmpty() && !anod.naclIn.isEmpty();
        // 配方 8：电解盐酸（阴极=水/盐酸，阳极=盐酸 → H₂ + Cl₂，不需要膜）
        boolean hclCath = cath.waterIn.getFluid().getFluid().isSame(ModFluids.HYDROCHLORIC_ACID.getSource())
                || cath.waterIn.getFluid().getFluid().isSame(Fluids.WATER);
        boolean hclAnod = anod.naclIn.getFluid().getFluid().isSame(ModFluids.HYDROCHLORIC_ACID.getSource());
        boolean isHCl = !isAluPre && !isMolten && !isCu && !isWaterElec
                && hclCath && hclAnod && !cath.waterIn.isEmpty() && !anod.naclIn.isEmpty();
        // 配方 9：熔融氯化镁（热源 + MgCl₂固体 + 石墨电极 → Mg锭 + Cl₂）
        boolean isMoltenMg = !isAluPre && !isMolten && !isCu && !isWaterElec && !isHCl
                && hasBlazeBurner(level, pos)
                && self.items.getStackInSlot(SLOT_CATHODE).is(ModItems.GRAPHITE_ROD.get())
                && self.items.getStackInSlot(SLOT_ANODE).is(ModItems.GRAPHITE_ROD.get())
                && self.items.getStackInSlot(SLOT_FILTER).is(ModItems.MAGNESIUM_CHLORIDE.get());
        // 配方 10：电解氢氟酸（阴极+阳极都通入氢氟酸 → 阴极 H₂，阳极氟气【生成即爆炸】）
        boolean hfCath = cath.waterIn.getFluid().getFluid().isSame(ModFluids.HYDROFLUORIC_ACID.getSource());
        boolean hfAnod = anod.naclIn.getFluid().getFluid().isSame(ModFluids.HYDROFLUORIC_ACID.getSource());
        boolean isHF = !isAluPre && !isMolten && !isCu && !isWaterElec && !isHCl && !isMoltenMg
                && hfCath && hfAnod && !cath.waterIn.isEmpty() && !anod.naclIn.isEmpty();

        // 配方表已更新：除电解制铝（recipe 5，需冰晶石作催化剂）外，
        // 所有电解都不再需要催化剂槽（SLOT_CATALYST）和阳离子交换膜（SLOT_MEMBRANE），
        // 只要求阴阳极都放上有效电极。
        if (!isElectrode(self.items.getStackInSlot(SLOT_CATHODE)) || !isElectrode(self.items.getStackInSlot(SLOT_ANODE))) {
            self.progress = 0; return;
        }

        boolean refining = isCu && isCopperSheet(self.items.getStackInSlot(SLOT_CATHODE)) && self.items.getStackInSlot(SLOT_ANODE).is(Items.RAW_COPPER);
        boolean transfer = isCu && !refining && isCopperSheet(self.items.getStackInSlot(SLOT_CATHODE)) && isCopperSheet(self.items.getStackInSlot(SLOT_ANODE));
        int recipe = isAluPre ? 5 : (isMolten ? 6 : (isWaterElec ? 7 : (isHCl ? 8 : (isMoltenMg ? 9 : (isHF ? 10 : (refining ? 4 : (transfer ? 3 : (isCu ? 2 : 1))))))));

        // 热源配方中途失去热源则立即停止
        if ((recipe == 5 || recipe == 6 || recipe == 9) && !hasBlazeBurner(level, pos)) { self.progress = 0; return; }

        boolean hasFluid = (recipe == 5 || recipe == 6 || recipe == 9) || (recipe != 5 && cath.waterIn.getFluidAmount() >= FLUID_PER_OP && anod.naclIn.getFluidAmount() >= FLUID_PER_OP);
        boolean hasSpace;
        if (recipe == 5) {
            hasSpace = self.items.getStackInSlot(SLOT_OUT).isEmpty() || self.items.getStackInSlot(SLOT_OUT).is(ModItems.ALUMINUM_INGOT.get());
        } else if (recipe == 6) {
            hasSpace = (self.items.getStackInSlot(SLOT_OUT).isEmpty() || self.items.getStackInSlot(SLOT_OUT).is(ModItems.SODIUM_INGOT.get()))
                    && anod.cl2Out.getFluidAmount() + FLUID_PER_OP <= anod.tankCap();
        } else if (recipe == 7) {
            hasSpace = cath.h2Out.getFluidAmount() + FLUID_PER_OP <= cath.tankCap()
                    && anod.o2Out.getFluidAmount() + FLUID_PER_OP/2 <= anod.tankCap();
        } else if (recipe == 8) {
            // 电解盐酸：H₂ + Cl₂，不需要膜
            hasSpace = cath.h2Out.getFluidAmount() + FLUID_PER_OP <= cath.tankCap()
                    && anod.cl2Out.getFluidAmount() + FLUID_PER_OP <= anod.tankCap();
        } else if (recipe == 9) {
            // 熔融 MgCl₂：Mg锭 + Cl₂
            hasSpace = (self.items.getStackInSlot(SLOT_OUT).isEmpty() || self.items.getStackInSlot(SLOT_OUT).is(ModItems.MAGNESIUM_INGOT.get()))
                    && anod.cl2Out.getFluidAmount() + FLUID_PER_OP <= anod.tankCap();
        } else if (recipe == 10) {
            // 电解氢氟酸：阴极 H₂ 有空间即可（阳极氟气直接爆炸，不存储）
            hasSpace = cath.h2Out.getFluidAmount() + FLUID_PER_OP <= cath.tankCap();
        } else if (recipe == 4) {
            hasSpace = self.items.getStackInSlot(SLOT_FILTER).is(ModItems.FILTER_MESH.get())
                    && (self.items.getStackInSlot(SLOT_OUT).isEmpty() || self.items.getStackInSlot(SLOT_OUT).is(ModItems.ANODE_SLIME.get()));
        } else if (recipe == 3) {
            hasSpace = self.items.getStackInSlot(SLOT_CATHODE).getCount() < 64;
        } else if (recipe == 2) {
            hasSpace = self.items.getStackInSlot(SLOT_FILTER).is(ModItems.FILTER_MESH.get())
                    && cath.h2Out.getFluidAmount() + FLUID_PER_OP <= cath.tankCap() && anod.o2Out.getFluidAmount() + FLUID_PER_OP <= anod.tankCap();
        } else {
            hasSpace = cath.naohOut.getFluidAmount() + FLUID_PER_OP <= cath.tankCap() && cath.h2Out.getFluidAmount() + FLUID_PER_OP <= cath.tankCap()
                    && anod.waterOut.getFluidAmount() + FLUID_PER_OP <= anod.tankCap() && anod.cl2Out.getFluidAmount() + FLUID_PER_OP <= anod.tankCap();
        }
        if (!hasFluid || !hasSpace) { self.progress = 0; return; }

        // 注意：电解槽内部同时存有 H₂ 和 Cl₂ 是【正常状态】（电解 NaCl 的产物），
        // 不能因此爆炸！H₂+Cl₂ 混合爆炸只发生在输出罐堵塞溢出时（见 doProcess）。
        // 固体产物事故：仅热配方（铝/钠/镁），阴极积攒 ≥4 个 → 爆炸
        if ((recipe == 5 || recipe == 6 || recipe == 9) && cath.items.getStackInSlot(SLOT_OUT).getCount() >= 4) {
            cath.items.setStackInSlot(SLOT_OUT, ItemStack.EMPTY);
            level.explode(null, cath.worldPosition.getX()+0.5, cath.worldPosition.getY()+0.5, cath.worldPosition.getZ()+0.5, 2.5f, Level.ExplosionInteraction.BLOCK);
        }

        cath.energy -= ept; anod.energy -= ept;
        self.progress += depth;  // 更深 → 更快（线性增长）
        if (self.progress >= PROCESS_TIME) { self.progress = 0; doProcess(level, self, cath, anod, recipe); }
    }

    private static void doProcess(Level level, ElectrolyzerBlockEntity self, ElectrolyzerBlockEntity cath, ElectrolyzerBlockEntity anod, int recipe) {
        if (recipe != 5 && recipe != 6 && recipe != 9) {
            cath.waterIn.drain(FLUID_PER_OP, IFluidHandler.FluidAction.EXECUTE);
            anod.naclIn.drain(FLUID_PER_OP, IFluidHandler.FluidAction.EXECUTE);
        }
        // 成就
        com.cxy.chemical_industry.event.AdvancementHelper.grantNearby(level, self.worldPosition, "first_electrolysis", "elec");
        if (recipe == 2 || recipe == 5) com.cxy.chemical_industry.event.AdvancementHelper.grantNearby(level, self.worldPosition, "oxygen", "o2");

        // ==== 活泼阳极消耗：Zn/Fe/Cu 板材在水溶液电解中会缓慢消耗 ====
        if ((recipe >= 1 && recipe <= 4) || recipe == 7 || recipe == 8) {
            ItemStack anodeStack = self.items.getStackInSlot(SLOT_ANODE);
            if (isActiveAnode(anodeStack) && level.random.nextFloat() < 0.3f) {  // 30% 概率每次完成消耗 1 个
                anodeStack.shrink(1);
            }
        }

        if (recipe == 7) {
            // 电解水：2H₂O → 2H₂ + O₂
            cath.h2Out.fill(new FluidStack(ModFluids.HYDROGEN_GAS.getSource(), FLUID_PER_OP), IFluidHandler.FluidAction.EXECUTE);
            anod.o2Out.fill(new FluidStack(ModFluids.OXYGEN_GAS.getSource(), FLUID_PER_OP/2), IFluidHandler.FluidAction.EXECUTE);
        } else if (recipe == 6) {
            self.items.getStackInSlot(SLOT_FILTER).shrink(1);
            addSolid(cath, ModItems.SODIUM_INGOT.get(), 1);
            anod.cl2Out.fill(new FluidStack(ModFluids.CHLORINE_GAS.getSource(), FLUID_PER_OP), IFluidHandler.FluidAction.EXECUTE);
        } else if (recipe == 5) {
            self.items.getStackInSlot(SLOT_FILTER).shrink(1);
            addSolid(cath, ModItems.ALUMINUM_INGOT.get(), 1);
            anod.o2Out.fill(new FluidStack(ModFluids.OXYGEN_GAS.getSource(), FLUID_PER_OP), IFluidHandler.FluidAction.EXECUTE);
        } else if (recipe == 9) {
            // 熔融 MgCl₂：MgCl₂ → Mg + Cl₂（石墨电极不消耗）
            self.items.getStackInSlot(SLOT_FILTER).shrink(1);
            addSolid(cath, ModItems.MAGNESIUM_INGOT.get(), 1);
            anod.cl2Out.fill(new FluidStack(ModFluids.CHLORINE_GAS.getSource(), FLUID_PER_OP), IFluidHandler.FluidAction.EXECUTE);
        } else if (recipe == 8) {
            // 电解盐酸：2HCl → H₂ + Cl₂
            cath.h2Out.fill(new FluidStack(ModFluids.HYDROGEN_GAS.getSource(), FLUID_PER_OP), IFluidHandler.FluidAction.EXECUTE);
            int cl2Filled = anod.cl2Out.fill(new FluidStack(ModFluids.CHLORINE_GAS.getSource(), FLUID_PER_OP), IFluidHandler.FluidAction.EXECUTE);
            if (level instanceof ServerLevel sl && cl2Filled < FLUID_PER_OP) {
                BlockPos ap = anod.worldPosition.above();
                spawnParticles(sl, ap, 0.3f, 1.0f, 0.3f);
                harm(sl, ap);
            }
        } else if (recipe == 10) {
            // 电解氢氟酸：2HF → H₂ + F₂。氟气生成即爆炸！
            cath.h2Out.fill(new FluidStack(ModFluids.HYDROGEN_GAS.getSource(), FLUID_PER_OP), IFluidHandler.FluidAction.EXECUTE);
            BlockPos ap = anod.worldPosition.above();
            if (level instanceof ServerLevel sl) {
                // 黄绿色氟气粒子（模拟氟气喷涌）
                for (int i = 0; i < 10; i++) spawnParticles(sl, ap, 0.3f, 1.0f, 0.3f);
                // 严重中毒 + 腐蚀
                harmFluorine(sl, ap);
                // 立即爆炸（NONE 模式：不破坏方块，只造成冲击伤害）
                level.explode(null, ap.getX() + 0.5, ap.getY() + 0.5, ap.getZ() + 0.5,
                        2.5f, Level.ExplosionInteraction.NONE);
            }
            com.cxy.chemical_industry.event.AdvancementHelper.grantNearby(level, self.worldPosition, "fluorine", "f2");
        } else if (recipe == 4) {
            self.items.getStackInSlot(SLOT_ANODE).shrink(1); self.items.getStackInSlot(SLOT_CATHODE).grow(1);
            addSolid(anod, ModItems.ANODE_SLIME.get(), 1);
        } else if (recipe == 3) {
            self.items.getStackInSlot(SLOT_ANODE).shrink(1); self.items.getStackInSlot(SLOT_CATHODE).grow(1);
        } else if (recipe == 2) {
            addSolid(cath, ModItems.COPPER_NUGGET.get(), 2);
            anod.o2Out.fill(new FluidStack(ModFluids.OXYGEN_GAS.getSource(), FLUID_PER_OP), IFluidHandler.FluidAction.EXECUTE);
        } else {
            cath.naohOut.fill(new FluidStack(ModFluids.SODIUM_HYDROXIDE_SOLUTION.getSource(), FLUID_PER_OP), IFluidHandler.FluidAction.EXECUTE);
            int h2 = cath.h2Out.fill(new FluidStack(ModFluids.HYDROGEN_GAS.getSource(), FLUID_PER_OP), IFluidHandler.FluidAction.EXECUTE);
            anod.waterOut.fill(new FluidStack(Fluids.WATER, FLUID_PER_OP), IFluidHandler.FluidAction.EXECUTE);
            int cl2 = anod.cl2Out.fill(new FluidStack(ModFluids.CHLORINE_GAS.getSource(), FLUID_PER_OP), IFluidHandler.FluidAction.EXECUTE);
            if (level instanceof ServerLevel sl) {
                BlockPos cp = cath.worldPosition.above(), ap = anod.worldPosition.above();
                // 罐还有空间（本次只装了一部分）不算堵管，只有两个罐都完全装不下（fill=0）才爆炸
                if (h2 == 0) spawnParticles(sl, cp, 0.7f, 0.8f, 1.0f);
                if (cl2 == 0) { spawnParticles(sl, ap, 0.3f, 1.0f, 0.3f); harm(sl, ap); }
                if (h2 == 0 && cl2 == 0)
                    level.explode(null, (cp.getX()+ap.getX())/2.0+0.5, cp.getY()+0.5, (cp.getZ()+ap.getZ())/2.0+0.5, 3.5f, Level.ExplosionInteraction.BLOCK);
            }
        }
    }

    // ====== 工具（粒子/伤害保留给 doProcess 中使用） ======

    // ====== 流体能力 ======
    public IFluidHandler fluidHandler(@Nullable Direction side) {
        if (!isFront()) return EMPTY;
        // JADE 等工具用 null 查询时只显示输入槽（不显示空的输出槽）
        if (side == null) {
            if (isPart(Part.CATHODE)) return new IFluidHandler() {
                @Override public int getTanks() { return 1; } @Override public FluidStack getFluidInTank(int t) { return waterIn.getFluid(); }
                @Override public int getTankCapacity(int t) { return tankCap(); } @Override public boolean isFluidValid(int t, FluidStack s) { return true; }
                @Override public int fill(FluidStack r, FluidAction a) { return waterIn.fill(r, a); }
                @Override public FluidStack drain(FluidStack r, FluidAction a) { return FluidStack.EMPTY; } @Override public FluidStack drain(int max, FluidAction a) { return FluidStack.EMPTY; }
            };
            if (isPart(Part.ANODE)) return new IFluidHandler() {
                @Override public int getTanks() { return 1; } @Override public FluidStack getFluidInTank(int t) { return naclIn.getFluid(); }
                @Override public int getTankCapacity(int t) { return tankCap(); } @Override public boolean isFluidValid(int t, FluidStack s) { return true; }
                @Override public int fill(FluidStack r, FluidAction a) { return naclIn.fill(r, a); }
                @Override public FluidStack drain(FluidStack r, FluidAction a) { return FluidStack.EMPTY; } @Override public FluidStack drain(int max, FluidAction a) { return FluidStack.EMPTY; }
            };
            return EMPTY;
        }
        if (isPart(Part.CATHODE)) {
            Direction left = facing().getCounterClockWise();
            return new IFluidHandler() {
                @Override public int getTanks() { return 3; } @Override public FluidStack getFluidInTank(int t) { return t==0?waterIn.getFluid():t==1?naohOut.getFluid():h2Out.getFluid(); }
                @Override public int getTankCapacity(int t) { return tankCap(); } @Override public boolean isFluidValid(int t, FluidStack s) { return t==0; }
                @Override public int fill(FluidStack r, FluidAction a) { return waterIn.fill(r, a); }
                @Override public FluidStack drain(FluidStack r, FluidAction a) {
                    if (side==left&&r.getFluid().isSame(ModFluids.SODIUM_HYDROXIDE_SOLUTION.getSource())) return naohOut.drain(r,a);
                    if (side==Direction.UP&&r.getFluid().isSame(ModFluids.HYDROGEN_GAS.getSource())) return h2Out.drain(r,a); return FluidStack.EMPTY; }
                @Override public FluidStack drain(int max, FluidAction a) { return side==left?naohOut.drain(max,a):side==Direction.UP?h2Out.drain(max,a):FluidStack.EMPTY; }
            };
        }
        if (isPart(Part.ANODE)) {
            Direction right = facing().getClockWise();
            return new IFluidHandler() {
                @Override public int getTanks() { return 4; } @Override public FluidStack getFluidInTank(int t) { return t==0?naclIn.getFluid():t==1?waterOut.getFluid():t==2?cl2Out.getFluid():o2Out.getFluid(); }
                @Override public int getTankCapacity(int t) { return tankCap(); } @Override public boolean isFluidValid(int t, FluidStack s) { return t==0; }
                @Override public int fill(FluidStack r, FluidAction a) { return naclIn.fill(r, a); }
                @Override public FluidStack drain(FluidStack r, FluidAction a) {
                    if (side==right&&r.getFluid().isSame(Fluids.WATER)) return waterOut.drain(r,a);
                    if (side==Direction.UP) {
                        if (r.getFluid().isSame(ModFluids.CHLORINE_GAS.getSource())) return cl2Out.drain(r,a);
                        if (r.getFluid().isSame(ModFluids.OXYGEN_GAS.getSource())) return o2Out.drain(r,a);
                    }
                    return FluidStack.EMPTY; }
                @Override public FluidStack drain(int max, FluidAction a) {
                    if (side==right) return waterOut.drain(max,a);
                    if (side==Direction.UP) { FluidStack d = cl2Out.drain(max,a); return d.isEmpty()?o2Out.drain(max,a):d; }
                    return FluidStack.EMPTY; }
            };
        }
        return EMPTY;
    }

    // ====== 能量 + 物品 ======
    public IEnergyStorage energyStorage() {
        if (!isFront() || isPart(Part.CENTER)) return null;
        return new IEnergyStorage() {
            @Override public int receiveEnergy(int max, boolean sim) { int a = Math.min(max, energyCap() - energy); if (!sim) energy += a; return a; }
            @Override public int extractEnergy(int max, boolean sim) { return 0; } @Override public int getEnergyStored() { return energy; }
            @Override public int getMaxEnergyStored() { return energyCap(); } @Override public boolean canExtract() { return false; } @Override public boolean canReceive() { return true; }
        };
    }
    public IItemHandlerModifiable guiItems() {
        return new IItemHandlerModifiable() {
            @Override public int getSlots() { return 5; } @Override public ItemStack getStackInSlot(int s) { return items.getStackInSlot(s); }
            @Override public void setStackInSlot(int s, ItemStack st) { items.setStackInSlot(s, st); }
            @Override public ItemStack insertItem(int s, ItemStack st, boolean sim) { return s < 5 ? items.insertItem(s, st, sim) : st; }
            @Override public ItemStack extractItem(int s, int a, boolean sim) { return s < 5 ? items.extractItem(s, a, sim) : ItemStack.EMPTY; }
            @Override public int getSlotLimit(int s) { return items.getSlotLimit(s); } @Override public boolean isItemValid(int s, ItemStack st) { return items.isItemValid(s, st); }
        };
    }
    public IItemHandler autoItems() {
        // 电极：暴露自身的输出槽（漏斗从电极侧面拉取固体产物）
        if (isPart(Part.CATHODE) || isPart(Part.ANODE)) {
            return new IItemHandler() {
                @Override public int getSlots() { return 1; }
                @Override public ItemStack getStackInSlot(int s) { return items.getStackInSlot(SLOT_OUT); }
                @Override public ItemStack insertItem(int s, ItemStack st, boolean sim) { return st; }
                @Override public ItemStack extractItem(int s, int a, boolean sim) { return items.extractItem(SLOT_OUT, a, sim); }
                @Override public int getSlotLimit(int s) { return items.getSlotLimit(SLOT_OUT); }
                @Override public boolean isItemValid(int s, ItemStack st) { return false; }
            };
        }
        // CENTER：漏斗/溜槽在此输入物品到槽 0-4，提取产物从槽 5+
        if (!isPart(Part.CENTER)) return EMPTY_ITEMS;
        return new IItemHandler() {
            @Override public int getSlots() { return TOTAL_SLOTS; } @Override public ItemStack getStackInSlot(int s) { return items.getStackInSlot(s); }
            @Override public ItemStack insertItem(int s, ItemStack st, boolean sim) { return s < SLOT_OUT ? items.insertItem(s, st, sim) : st; }
            @Override public ItemStack extractItem(int s, int a, boolean sim) { return s >= SLOT_OUT ? items.extractItem(s, a, sim) : ItemStack.EMPTY; }
            @Override public int getSlotLimit(int s) { return items.getSlotLimit(s); } @Override public boolean isItemValid(int s, ItemStack st) { return items.isItemValid(s, st); }
        };
    }

    @Override public Component getDisplayName() { return Component.translatable("block.chemical_industry.electrolyzer"); }
    @Nullable @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) { return new ElectrolyzerMenu(id, inv, this, data); }
    public void dropContents() { if (level != null && !level.isClientSide() && isPart(Part.CENTER)) for (int i = 0; i < TOTAL_SLOTS; i++) { ItemStack s = items.getStackInSlot(i); if (!s.isEmpty()) net.minecraft.world.Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), s); } }

    // ====== 工具 ======
    /** 检查物品是否为有效电极（必须是板材！Create的板材 + 石墨棒 + 粗铜用于精炼） */
    private static boolean isElectrode(ItemStack s) {
        if (s.isEmpty()) return false;
        String k = s.getItem().builtInRegistryHolder().key().location().toString();
        // 禁用铝和钠做电极（太活泼，会和电解质反应）
        if (s.is(ModItems.ALUMINUM_INGOT.get()) || s.is(ModItems.ALUMINUM_NUGGET.get())
                || s.is(ModItems.SODIUM_INGOT.get())) return false;
        // Create 板材：铁板、铜板、锌板、金板
        if (k.startsWith("create:") && k.contains("_sheet")) return true;
        // 石墨棒（惰性电极）
        if (s.is(ModItems.GRAPHITE_ROD.get())) return true;
        // 粗铜用于精炼
        if (s.is(Items.RAW_COPPER)) return true;
        return false;
    }
    /** 检查是否为活泼阳极（Zn/Fe/Cu，电解时会被消耗） */
    private static boolean isActiveAnode(ItemStack s) {
        if (s.isEmpty()) return false;
        String k = s.getItem().builtInRegistryHolder().key().location().toString();
        return k.equals("create:iron_sheet") || k.equals("create:copper_sheet")
                || k.equals("create:zinc_sheet");
    }
    private static boolean isCopperSheet(ItemStack s) { return !s.isEmpty() && s.getItem().builtInRegistryHolder().key().location().toString().equals("create:copper_sheet"); }
    /** 电解水电解质：水、NaOH 溶液、硫酸 */
    private static boolean isElectrolyte(FluidStack s) { return s.getFluid().isSame(Fluids.WATER) || s.getFluid().isSame(com.cxy.chemical_industry.registry.ModFluids.SODIUM_HYDROXIDE_SOLUTION.getSource()) || s.getFluid().isSame(com.cxy.chemical_industry.registry.ModFluids.SULFURIC_ACID.getSource()); }
    private static Item getCopperNugget() { return net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("create", "copper_nugget")); }
    /** 检查 CENTER 下方是否为烈焰人燃烧室 */
    private static boolean hasBlazeBurner(Level level, BlockPos pos) {
        String k = level.getBlockState(pos.below()).getBlock().builtInRegistryHolder().key().location().toString();
        return k.equals("create:blaze_burner");
    }
    private static void addSolid(ElectrolyzerBlockEntity self, Item item, int count) { ItemStack ex = self.items.getStackInSlot(SLOT_OUT); if (ex.isEmpty()) self.items.setStackInSlot(SLOT_OUT, new ItemStack(item, count)); else if (ex.is(item)) ex.grow(count); }
    static void spawnParticles(ServerLevel l, BlockPos p, float r, float g, float b) { for (int i = 0; i < 5; i++) l.sendParticles(new DustParticleOptions(new Vector3f(r, g, b), 0.5f), p.getX() + 0.3 + l.random.nextDouble() * 0.4, p.getY() + 1.2 + l.random.nextDouble() * 0.5, p.getZ() + 0.3 + l.random.nextDouble() * 0.4, 1, 0.1, 0.1, 0.1, 0.01); }
    static void harm(ServerLevel l, BlockPos p) { for (LivingEntity e : l.getEntitiesOfClass(LivingEntity.class, new AABB(p).inflate(3.0))) { e.hurt(l.damageSources().generic(), 1.0f); e.addEffect(new MobEffectInstance(ModEffects.corrosionHolder(), 100, 0)); } }
    /** 氟气伤害：范围更大，中毒 II + 腐蚀 10 秒 */
    static void harmFluorine(ServerLevel l, BlockPos p) {
        for (LivingEntity e : l.getEntitiesOfClass(LivingEntity.class, new AABB(p).inflate(5.0))) {
            e.hurt(l.damageSources().generic(), 4.0f);
            e.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 1));
            e.addEffect(new MobEffectInstance(ModEffects.corrosionHolder(), 200, 1));
        }
    }

    // ====== 持久化 ======
    @Override protected void saveAdditional(CompoundTag t, HolderLookup.Provider r) {
        super.saveAdditional(t, r); t.put("Items", items.serializeNBT(r)); t.putInt("E", energy); t.putInt("P", progress);
        t.put("wIn", waterIn.writeToNBT(r, new CompoundTag())); t.put("nOut", naohOut.writeToNBT(r, new CompoundTag()));
        t.put("hOut", h2Out.writeToNBT(r, new CompoundTag())); t.put("nIn", naclIn.writeToNBT(r, new CompoundTag()));
        t.put("wOut", waterOut.writeToNBT(r, new CompoundTag())); t.put("cOut", cl2Out.writeToNBT(r, new CompoundTag()));
        t.put("oOut", o2Out.writeToNBT(r, new CompoundTag()));
    }
    @Override protected void loadAdditional(CompoundTag t, HolderLookup.Provider r) {
        super.loadAdditional(t, r);
        if (t.contains("Items")) { CompoundTag it = t.getCompound("Items"); if (it.getInt("Size") < TOTAL_SLOTS) { CompoundTag f = new CompoundTag(); f.putInt("Size", TOTAL_SLOTS); for (int i = 0; i < it.getInt("Size"); i++) if (it.contains("Slot" + i)) f.put("Slot" + i, it.get("Slot" + i)); items.deserializeNBT(r, f); } else items.deserializeNBT(r, it); }
        energy = t.getInt("E"); progress = t.getInt("P");
        if (t.contains("wIn")) waterIn.readFromNBT(r, t.getCompound("wIn")); if (t.contains("nOut")) naohOut.readFromNBT(r, t.getCompound("nOut"));
        if (t.contains("hOut")) h2Out.readFromNBT(r, t.getCompound("hOut")); if (t.contains("nIn")) naclIn.readFromNBT(r, t.getCompound("nIn"));
        if (t.contains("wOut")) waterOut.readFromNBT(r, t.getCompound("wOut")); if (t.contains("cOut")) cl2Out.readFromNBT(r, t.getCompound("cOut"));
        if (t.contains("oOut")) o2Out.readFromNBT(r, t.getCompound("oOut"));
    }
    static final IFluidHandler EMPTY = new IFluidHandler() { @Override public int getTanks() { return 0; } @Override public FluidStack getFluidInTank(int t) { return FluidStack.EMPTY; } @Override public int getTankCapacity(int t) { return 0; } @Override public boolean isFluidValid(int t, FluidStack s) { return false; } @Override public int fill(FluidStack r, FluidAction a) { return 0; } @Override public FluidStack drain(FluidStack r, FluidAction a) { return FluidStack.EMPTY; } @Override public FluidStack drain(int max, FluidAction a) { return FluidStack.EMPTY; } };
    static final IItemHandler EMPTY_ITEMS = new IItemHandler() { @Override public int getSlots() { return 0; } @Override public ItemStack getStackInSlot(int s) { return ItemStack.EMPTY; } @Override public ItemStack insertItem(int s, ItemStack st, boolean sim) { return st; } @Override public ItemStack extractItem(int s, int a, boolean sim) { return ItemStack.EMPTY; } @Override public int getSlotLimit(int s) { return 0; } @Override public boolean isItemValid(int s, ItemStack st) { return false; } };
}
