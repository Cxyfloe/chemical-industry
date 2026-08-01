package com.user.chemical_industry.block_entity;

import com.user.chemical_industry.block.FluidizedBedBlock;
import com.user.chemical_industry.registry.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

public class FluidizedBedBlockEntity extends BlockEntity implements MenuProvider {

    public static final int SLOT_CATALYST=0,SLOT_INPUT_1=1,SLOT_INPUT_2=2,SLOT_HIDDEN_OUT1=3,SLOT_HIDDEN_OUT2=4,SLOT_COUNT=5;
    static final int FLUID_PER_OP=250;
    /** 每层总容量 = 层方块数 × 8 桶（8000mB），按层内活跃流体种类自动平分：
     *  例：2×2 层（4 方块）→ 总 32 桶；泵入 2 种液体 → 各 16 桶；4 种 → 各 8 桶 */
    static final int TANK_CAP=8000;

    public final ItemStackHandler items = new ItemStackHandler(SLOT_COUNT) {
        @Override protected void onContentsChanged(int s) { setChanged(); }
        @Override public boolean isItemValid(int slot,ItemStack s) { return slot<3; }
    };
    private static boolean isWater(FluidStack s)  { return !s.isEmpty(); }  // 接受任意液体（水、卤水、乙醇、乙醛等）
    private static boolean isH2(FluidStack s)     { return s.getFluid().isSame(ModFluids.HYDROGEN_GAS.getSource()); }
    private static boolean isN2(FluidStack s)     { return s.getFluid().isSame(ModFluids.NITROGEN_GAS.getSource()); }
    private final FluidTank waterTank = new FluidTank(TANK_CAP) { @Override protected void onContentsChanged() { setChanged(); } };  // 通用液体输入槽
    private final FluidTank h2Tank    = new FluidTank(TANK_CAP,s->isH2(s))    { @Override protected void onContentsChanged() { setChanged(); } };
    private final FluidTank n2Tank    = new FluidTank(TANK_CAP,s->isN2(s))    { @Override protected void onContentsChanged() { setChanged(); } };
    private final FluidTank acidTank  = new FluidTank(TANK_CAP) { @Override protected void onContentsChanged() { setChanged(); } };    // 酸类产物
    private final FluidTank midTank   = new FluidTank(TANK_CAP) { @Override protected void onContentsChanged() { setChanged(); } };    // 中层液体产物（卤水、乙醛、乙酸等）
    private final FluidTank gasTank   = new FluidTank(TANK_CAP) { @Override protected void onContentsChanged() { setChanged(); } };    // 气体产物（CO、CO₂、NH₃等）
    private final FluidTank gasTank2  = new FluidTank(TANK_CAP) { @Override protected void onContentsChanged() { setChanged(); } };    // 气体产物2（H₂、O₂等）

    private Block originalTank; private int progress; private String originalTankKey;

    public final ContainerData dataAccess = new ContainerData() {
        @Override public int get(int i) { return i==0?progress:200; }
        @Override public void set(int i,int v) { if(i==0)progress=v; }
        @Override public int getCount() { return 2; }
    };

    private static final IFluidHandler NULL = new IFluidHandler() {
        @Override public int getTanks() { return 0; } @Override public FluidStack getFluidInTank(int t) { return FluidStack.EMPTY; } @Override public int getTankCapacity(int t) { return 0; } @Override public boolean isFluidValid(int t,FluidStack s) { return false; } @Override public int fill(FluidStack r,FluidAction a) { return 0; } @Override public FluidStack drain(FluidStack r,FluidAction a) { return FluidStack.EMPTY; } @Override public FluidStack drain(int max,FluidAction a) { return FluidStack.EMPTY; }
    };

    public FluidizedBedBlockEntity(BlockPos pos,BlockState state) { super(ModBlockEntities.FLUIDIZED_BED.get(),pos,state); }
    public void setTankBlock(String key) { originalTankKey = key; originalTank = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(ResourceLocation.parse(key)); setChanged(); }
    public Block getOriginalTankBlock() { if (originalTank != null) return originalTank; if (originalTankKey != null) { originalTank = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(ResourceLocation.parse(originalTankKey)); } return originalTank; }

    // ---- Create 式 Controller 多方块 ----
    BlockPos controller;          // 指向 controller 的 BlockPos，null=自己就是 controller
    boolean updateConnectivity;   // 需要重新扫描
    int width=1, height=1;       // 与 Create 一致的尺寸字段

    public boolean isController() { return controller == null || controller.equals(worldPosition); }
    @Nullable public FluidizedBedBlockEntity getControllerBE() {
        if (isController()) return this;
        if (level == null || controller == null) return null;
        BlockEntity be = level.getBlockEntity(controller);
        return be instanceof FluidizedBedBlockEntity f ? f : null;
    }
    public BlockPos getController() { return isController() ? worldPosition : controller; }
    public void setController(BlockPos c) { controller = c; setChanged(); }
    public void removeController(boolean keep) { controller = null; updateConnectivity=true; if (!keep) { waterTank.drain(waterTank.getCapacity(), IFluidHandler.FluidAction.EXECUTE); h2Tank.drain(h2Tank.getCapacity(), IFluidHandler.FluidAction.EXECUTE); n2Tank.drain(n2Tank.getCapacity(), IFluidHandler.FluidAction.EXECUTE); acidTank.drain(acidTank.getCapacity(), IFluidHandler.FluidAction.EXECUTE); midTank.drain(midTank.getCapacity(), IFluidHandler.FluidAction.EXECUTE); gasTank.drain(gasTank.getCapacity(), IFluidHandler.FluidAction.EXECUTE); gasTank2.drain(gasTank2.getCapacity(), IFluidHandler.FluidAction.EXECUTE); } setChanged(); }
    public void preventConnectivityUpdate() { updateConnectivity = false; }

    /** BFS 扫描所有相连沸腾炉，统一指向同一 controller */
    void updateConnectivity() {
        if (level == null || level.isClientSide()) return;
        updateConnectivity = false;
        if (!isController()) return; // 只有 controller 执行扫描
        // 清空所有已连接的方块
        java.util.Set<BlockPos> all = new java.util.HashSet<>();
        java.util.ArrayDeque<BlockPos> q = new java.util.ArrayDeque<>();
        q.add(worldPosition); all.add(worldPosition);
        while (!q.isEmpty()) {
            for (net.minecraft.core.Direction d : net.minecraft.core.Direction.values()) {
                BlockPos np = q.peek().relative(d);
                // 先检查类型再加进集合（否则相邻的烈焰人燃烧室等会被误加入 all，导致下面 getValue(LAYER) 崩溃）
                if (!level.getBlockState(np).is(ModBlocks.FLUIDIZED_BED.get())) continue;
                if (!all.add(np)) continue;
                q.add(np);
            }
            q.poll();
        }
        // 统计每层方块数（决定每层总容量 = 方块数 × 8 桶）
        bottomCount = middleCount = topCount = 0;
        for (BlockPos p : all) {
            FluidizedBedBlock.Layer l = level.getBlockState(p).getValue(FluidizedBedBlock.LAYER);
            switch (l) {
                case BOTTOM -> bottomCount++;
                case MIDDLE -> middleCount++;
                case TOP -> topCount++;
            }
        }
        if (bottomCount == 0) bottomCount = 1;
        if (middleCount == 0) middleCount = 1;
        if (topCount == 0) topCount = 1;

        // 所有方块设 controller 指向自己
        for (BlockPos p : all) {
            BlockEntity be = level.getBlockEntity(p);
            if (be instanceof FluidizedBedBlockEntity f && f != this) {
                f.controller = worldPosition;
                f.updateConnectivity = false;
                f.setChanged();
            }
        }
    }
    /** 激活连接扫描 */
    public void scheduleConnectivityUpdate() { updateConnectivity = true; setChanged(); }

    // ---- 每层方块数（controller 统计，决定层总容量） ----
    private int bottomCount = 1, middleCount = 1, topCount = 1;

    /**
     * 同步每层储罐容量：层总容量 = 层方块数 × 8 桶，按活跃流体种类自动平分。
     * 例：2×2 层（4 方块）= 32 桶；泵入 2 种液体 → 各 16 桶。
     */
    private static void syncLayerCaps(FluidizedBedBlockEntity mid, FluidizedBedBlockEntity bot, FluidizedBedBlockEntity top) {
        FluidizedBedBlockEntity c = mid.getControllerBE();
        if (c == null) return;
        // 底层（水/H₂/N₂）
        if (bot != null) shareCap(bot, c.bottomCount * TANK_CAP, bot.waterTank, bot.h2Tank, bot.n2Tank);
        // 中层（酸/中间产物）
        shareCap(mid, c.middleCount * TANK_CAP, mid.acidTank, mid.midTank);
        // 顶层（气体 ×2）
        if (top != null) shareCap(top, c.topCount * TANK_CAP, top.gasTank, top.gasTank2);
    }

    /** 层内各罐平分总容量：容量 = 总容量 ÷ 活跃组分（非空罐数，至少 1） */
    private static void shareCap(FluidizedBedBlockEntity be, int totalCap, FluidTank... tanks) {
        int active = 0;
        for (FluidTank t : tanks) if (!t.isEmpty()) active++;
        int perCap = totalCap / Math.max(1, active);
        for (FluidTank t : tanks) t.setCapacity(perCap);
    }
    /** 垂直向下找同列底层 */
    @Nullable private FluidizedBedBlockEntity findBottomInColumn() {
        if (level == null) return null;
        BlockPos p = worldPosition.below();
        while (level.getBlockState(p).is(ModBlocks.FLUIDIZED_BED.get())) {
            if (level.getBlockState(p).getValue(FluidizedBedBlock.LAYER) == FluidizedBedBlock.Layer.BOTTOM)
                return (FluidizedBedBlockEntity) level.getBlockEntity(p);
            p = p.below();
        }
        return null;
    }
    /** 垂直向上找同列顶层 */
    @Nullable private FluidizedBedBlockEntity findTopInColumn() {
        if (level == null) return null;
        BlockPos p = worldPosition.above();
        while (level.getBlockState(p).is(ModBlocks.FLUIDIZED_BED.get())) {
            if (level.getBlockState(p).getValue(FluidizedBedBlock.LAYER) == FluidizedBedBlock.Layer.TOP)
                return (FluidizedBedBlockEntity) level.getBlockEntity(p);
            p = p.above();
        }
        return null;
    }

    // ---- 通过 Controller 访问共享槽 ----
    public FluidTank sharedWater() { FluidizedBedBlockEntity c = getControllerBE(); return c!=null ? c.waterTank : waterTank; }
    FluidTank sharedH2()    { FluidizedBedBlockEntity c = getControllerBE(); return c!=null ? c.h2Tank    : h2Tank; }
    FluidTank sharedN2()    { FluidizedBedBlockEntity c = getControllerBE(); return c!=null ? c.n2Tank    : n2Tank; }
    public FluidTank sharedAcid()  { FluidizedBedBlockEntity c = getControllerBE(); return c!=null ? c.acidTank  : acidTank; }
    public FluidTank sharedMid()   { FluidizedBedBlockEntity c = getControllerBE(); return c!=null ? c.midTank   : midTank; }
    public FluidTank sharedGas()   { FluidizedBedBlockEntity c = getControllerBE(); return c!=null ? c.gasTank   : gasTank; }
    public FluidTank sharedGas2()  { FluidizedBedBlockEntity c = getControllerBE(); return c!=null ? c.gasTank2  : gasTank2; }

    // ---- 流体能力（每层独立槽，Controller 只用于 GUI） ----
    public IFluidHandler handlerForCapability(@Nullable Direction side) {
        return getFluidHandler(side);
    }

    public static void tick(Level level,BlockPos pos,BlockState state,FluidizedBedBlockEntity e) {
        if (level.isClientSide()) return;
        if (e.updateConnectivity) { e.updateConnectivity(); return; }
        var layer = state.getValue(FluidizedBedBlock.LAYER);
        if (layer != FluidizedBedBlock.Layer.MIDDLE) return;

        FluidizedBedBlockEntity bot = e.findBottomInColumn();
        FluidizedBedBlockEntity top = e.findTopInColumn();

        // 同步层容量：每层总容量 = 层方块数 × 8 桶，按活跃流体种类平分
        syncLayerCaps(e, bot, top);

        FluidTank waterSrc = (bot != null) ? bot.waterTank : e.waterTank;
        FluidTank h2Src    = (bot != null) ? bot.h2Tank    : e.h2Tank;
        FluidTank n2Src    = (bot != null) ? bot.n2Tank    : e.n2Tank;
        FluidTank acidDst  = e.acidTank;
        FluidTank midDst   = e.midTank;       // 中层液体产物（卤水、乙醛、乙酸等）
        FluidTank gasDst   = (top != null) ? top.gasTank   : e.gasTank;
        FluidTank gasDst2  = (top != null) ? top.gasTank2  : e.gasTank2;  // 第二气槽（H₂等）

        int heat = getHeat(level, pos);
        ItemStack cat = e.items.getStackInSlot(SLOT_CATALYST);
        ItemStack in1 = e.items.getStackInSlot(SLOT_INPUT_1), in2 = e.items.getStackInSlot(SLOT_INPUT_2);

        // ===== 安全阀：没有任何输入则停止 =====
        boolean anyInput = !in1.isEmpty() || !in2.isEmpty() || !cat.isEmpty()
                || !waterSrc.isEmpty() || !h2Src.isEmpty() || !n2Src.isEmpty()
                || !gasDst.isEmpty() || !gasDst2.isEmpty() || !acidDst.isEmpty() || !midDst.isEmpty();
        if (!anyInput) { e.progress = 0; return; }

        // =====================================================================
        // 氨合成：铁板催化剂 + H₂ + N₂ → NH₃（需要热源）
        // =====================================================================
        if (isIronSheet(cat)) {
            if (heat < 0) { e.progress = 0; return; }
            if (h2Src.getFluidAmount() >= 150 && n2Src.getFluidAmount() >= 50
                    && (gasDst.isEmpty() || gasDst.getFluid().getFluid().isSame(ModFluids.AMMONIA_GAS.getSource()))
                    && gasDst.getFluidAmount() + 100 <= TANK_CAP) {
                e.progress++; if (e.progress >= 200) { e.progress = 0;
                    h2Src.drain(150, IFluidHandler.FluidAction.EXECUTE);
                    n2Src.drain(50, IFluidHandler.FluidAction.EXECUTE);
                    gasDst.fill(new FluidStack(ModFluids.AMMONIA_GAS.getSource(), 100), IFluidHandler.FluidAction.EXECUTE);
                }
                return;
            }
            e.progress = 0; return;
        }

        // =====================================================================
        // 甲烷合成：稀土元素 + CO + H₂ → CH₄（需要热源）
        // CO和产物CH₄共用gasTank，必须精确排空CO后才能灌入CH₄
        // =====================================================================
        if (heat >= 0 && cat.is(ModItems.RARE_EARTH_ELEMENT.get())
                && !gasDst.isEmpty() && gasDst.getFluid().getFluid().isSame(ModFluids.CARBON_MONOXIDE.getSource())
                && gasDst.getFluidAmount() >= 100 && h2Src.getFluidAmount() >= 300) {
            int coAmt = gasDst.getFluidAmount();
            int drainAmt = Math.min(coAmt, 100);
            // 只处理刚好能一次清空的情况（CO→CH₄流体类型切换）
            if (coAmt > drainAmt) { e.progress = 0; return; }
            e.progress++; if (e.progress >= 200) { e.progress = 0;
                gasDst.drain(drainAmt, IFluidHandler.FluidAction.EXECUTE);
                h2Src.drain(300, IFluidHandler.FluidAction.EXECUTE);
                gasDst.fill(new FluidStack(ModFluids.METHANE.getSource(), drainAmt), IFluidHandler.FluidAction.EXECUTE); }
            return;
        }

        // =====================================================================
        // 甲醇合成：氧化铜 + CO + H₂ → CH₃OH（需要热源）
        // CO来自gasTank(TOP), H₂来自h2Tank(BOTTOM), 产物甲醇→midTank(MIDDLE)
        // =====================================================================
        if (heat >= 0 && cat.is(ModItems.COPPER_OXIDE.get())
                && !gasDst.isEmpty() && gasDst.getFluid().getFluid().isSame(ModFluids.CARBON_MONOXIDE.getSource())
                && gasDst.getFluidAmount() >= 100 && h2Src.getFluidAmount() >= 200
                && (midDst.isEmpty() || midDst.getFluid().getFluid().isSame(ModFluids.METHANOL.getSource()))
                && midDst.getFluidAmount() + FLUID_PER_OP <= TANK_CAP) {
            e.progress++; if (e.progress >= 200) { e.progress = 0;
                gasDst.drain(100, IFluidHandler.FluidAction.EXECUTE);
                h2Src.drain(200, IFluidHandler.FluidAction.EXECUTE);
                midDst.fill(new FluidStack(ModFluids.METHANOL.getSource(), FLUID_PER_OP), IFluidHandler.FluidAction.EXECUTE); }
            return;
        }

        // =====================================================================
        // 乙醇氧化：氧化铜 + 乙醇(DG) → 乙醛（不需要热源！）
        // 与上面甲醇合成的区别：流体是乙醇而不是CO
        // =====================================================================
        if (cat.is(ModItems.COPPER_OXIDE.get()) && !waterSrc.isEmpty()
                && waterSrc.getFluid().getFluid().builtInRegistryHolder().key().location().toString().equals("createdieselgenerators:ethanol")
                && waterSrc.getFluidAmount() >= FLUID_PER_OP
                && (midDst.isEmpty() || midDst.getFluid().getFluid().isSame(ModFluids.ACETALDEHYDE.getSource()))
                && midDst.getFluidAmount() + FLUID_PER_OP <= TANK_CAP) {
            e.progress++; if (e.progress >= 200) { e.progress = 0;
                waterSrc.drain(FLUID_PER_OP, IFluidHandler.FluidAction.EXECUTE);
                midDst.fill(new FluidStack(ModFluids.ACETALDEHYDE.getSource(), FLUID_PER_OP), IFluidHandler.FluidAction.EXECUTE); }
            return;
        }

        // =====================================================================
        // 甲醛合成：银锭 + 甲醇 + O₂(空气) → 甲醛（需要热源）
        // =====================================================================
        if (heat >= 0 && isSilver(cat) && !waterSrc.isEmpty()
                && waterSrc.getFluid().getFluid().isSame(ModFluids.METHANOL.getSource())
                && waterSrc.getFluidAmount() >= FLUID_PER_OP
                && (midDst.isEmpty() || midDst.getFluid().getFluid().isSame(ModFluids.FORMALDEHYDE.getSource()))
                && midDst.getFluidAmount() + FLUID_PER_OP <= TANK_CAP) {
            e.progress++; if (e.progress >= 200) { e.progress = 0;
                waterSrc.drain(FLUID_PER_OP, IFluidHandler.FluidAction.EXECUTE);
                midDst.fill(new FluidStack(ModFluids.FORMALDEHYDE.getSource(), FLUID_PER_OP), IFluidHandler.FluidAction.EXECUTE); }
            return;
        }

        // =====================================================================
        // 钢生产：铁锭 + 碳 → 钢锭（需要热源）
        // =====================================================================
        if (heat >= 0 && hasIron(in1,in2) && hasCarbon(in1,in2)) {
            ItemStack out = e.items.getStackInSlot(SLOT_HIDDEN_OUT1);
            if (!out.isEmpty() && (!out.is(ModItems.STEEL_INGOT.get()) || out.getCount() >= 64)) { e.progress = 0; return; }
            e.progress++; if (e.progress >= 200) { e.progress = 0; consume(e, true, true);
                if (out.isEmpty()) e.items.setStackInSlot(SLOT_HIDDEN_OUT1, new ItemStack(ModItems.STEEL_INGOT.get())); else out.grow(1); }
            return;
        }

        // =====================================================================
        // 石头高温分解：石头 → CaO + SiO₂ + CO₂（需要热源）
        // =====================================================================
        if (heat >= 0 && isNaturalStone(in1)) {
            ItemStack out1 = e.items.getStackInSlot(SLOT_HIDDEN_OUT1);
            ItemStack out2 = e.items.getStackInSlot(SLOT_HIDDEN_OUT2);
            boolean o1ok = out1.isEmpty() || (out1.is(ModItems.CALCIUM_OXIDE.get()) && out1.getCount() < 64);
            boolean o2ok = out2.isEmpty() || (out2.is(ModItems.SILICON_DIOXIDE.get()) && out2.getCount() < 64);
            boolean gasOk = gasDst.isEmpty() || gasDst.getFluid().getFluid().isSame(ModFluids.CARBON_DIOXIDE.getSource());
            if (!o1ok || !o2ok || !gasOk || gasDst.getFluidAmount() + FLUID_PER_OP > TANK_CAP) { e.progress = 0; return; }
            e.progress++; if (e.progress >= 200) { e.progress = 0; in1.shrink(1);
                addToSlot(e, SLOT_HIDDEN_OUT1, new ItemStack(ModItems.CALCIUM_OXIDE.get()));
                addToSlot(e, SLOT_HIDDEN_OUT2, new ItemStack(ModItems.SILICON_DIOXIDE.get()));
                gasDst.fill(new FluidStack(ModFluids.CARBON_DIOXIDE.getSource(), FLUID_PER_OP), IFluidHandler.FluidAction.EXECUTE);
            }
            return;
        }

        // =====================================================================
        // 方铅矿炼铅：方铅矿 + C → Pb + SO₂... 简化: 方铅矿+C → Pb + CO
        // =====================================================================
        if (heat >= 0 && hasOreAndCarbon(in1, in2, "galena_ore")) {
            e.progress++; if (e.progress >= 200) { e.progress = 0;
                consumeOreAndCarbon(e, "galena_ore");
                addToSlot(e, SLOT_HIDDEN_OUT1, new ItemStack(ModItems.LEAD_INGOT.get()));
                int coFilled = gasDst.fill(new FluidStack(ModFluids.CARBON_MONOXIDE.getSource(), FLUID_PER_OP/2), IFluidHandler.FluidAction.EXECUTE);
                if (level instanceof net.minecraft.server.level.ServerLevel sl && coFilled < FLUID_PER_OP/2) coLeak(sl, pos);
            }
            return;
        }
        // =====================================================================
        // 锡石炼锡：锡石 + C → Sn + CO
        // =====================================================================
        if (heat >= 0 && hasOreAndCarbon(in1, in2, "cassiterite_ore")) {
            e.progress++; if (e.progress >= 200) { e.progress = 0;
                consumeOreAndCarbon(e, "cassiterite_ore");
                addToSlot(e, SLOT_HIDDEN_OUT1, new ItemStack(ModItems.TIN_INGOT.get()));
                int coFilled = gasDst.fill(new FluidStack(ModFluids.CARBON_MONOXIDE.getSource(), FLUID_PER_OP/2), IFluidHandler.FluidAction.EXECUTE);
                if (level instanceof net.minecraft.server.level.ServerLevel sl && coFilled < FLUID_PER_OP/2) coLeak(sl, pos);
            }
            return;
        }
        // =====================================================================
        // 朱砂炼水银：朱砂加热 → Hg（液态金属，不需碳）
        // =====================================================================
        if (heat >= 0 && (in1.is(ModItems.CINNABAR.get()) || in2.is(ModItems.CINNABAR.get()))) {
            // 产物空间检查：中间罐为空/水银，且放得下
            boolean midOk = (midDst.isEmpty() || midDst.getFluid().getFluid().isSame(ModFluids.MERCURY.getSource()))
                    && midDst.getFluidAmount() + FLUID_PER_OP <= TANK_CAP;
            if (!midOk) { e.progress = 0; return; }
            e.progress++; if (e.progress >= 200) { e.progress = 0;
                // 消耗 1 个朱砂
                if (in1.is(ModItems.CINNABAR.get())) in1.shrink(1); else in2.shrink(1);
                midDst.fill(new FluidStack(ModFluids.MERCURY.getSource(), FLUID_PER_OP), IFluidHandler.FluidAction.EXECUTE);
            }
            return;
        }

        // =====================================================================
        // 碳热还原：SiO₂ + C → Si + CO（需要热源，CO有毒！）
        // =====================================================================
        if (heat >= 0 && hasSilica(in1,in2) && hasCarbon(in1,in2)) {
            ItemStack out1 = e.items.getStackInSlot(SLOT_HIDDEN_OUT1);
            boolean o1ok = out1.isEmpty() || (out1.is(ModItems.SILICON.get()) && out1.getCount() < 64);
            boolean gasOk = gasDst.isEmpty() || gasDst.getFluid().getFluid().isSame(ModFluids.CARBON_MONOXIDE.getSource());
            if (!o1ok || !gasOk || gasDst.getFluidAmount() + FLUID_PER_OP > TANK_CAP) { e.progress = 0; return; }
            e.progress++; if (e.progress >= 200) { e.progress = 0; consumeSilica(e); consumeCarbon(e);
                addToSlot(e, SLOT_HIDDEN_OUT1, new ItemStack(ModItems.SILICON.get()));
                int coFill = gasDst.fill(new FluidStack(ModFluids.CARBON_MONOXIDE.getSource(), FLUID_PER_OP), IFluidHandler.FluidAction.EXECUTE);
                if (level instanceof net.minecraft.server.level.ServerLevel sl && coFill < FLUID_PER_OP)
                    coLeak(sl, pos);
            }
            return;
        }

        // =====================================================================
        // 水煤气：H₂O + 煤 → H₂ + CO（需要热源，等比例产出）
        // H₂ → gasTank2(TOP), CO → gasTank(TOP)
        // =====================================================================
        if (heat >= 0 && (isCoal(in1) || isCoal(in2))
                && !waterSrc.isEmpty() && waterSrc.getFluid().getFluid().isSame(Fluids.WATER)
                && waterSrc.getFluidAmount() >= FLUID_PER_OP
                && (gasDst2.isEmpty() || gasDst2.getFluid().getFluid().isSame(ModFluids.HYDROGEN_GAS.getSource()))
                && gasDst2.getFluidAmount() + FLUID_PER_OP <= TANK_CAP
                && (gasDst.isEmpty() || gasDst.getFluid().getFluid().isSame(ModFluids.CARBON_MONOXIDE.getSource()))
                && gasDst.getFluidAmount() + FLUID_PER_OP <= TANK_CAP) {
            ItemStack coal = isCoal(in1) ? in1 : in2;
            e.progress++; if (e.progress >= 200) { e.progress = 0; coal.shrink(1);
                waterSrc.drain(FLUID_PER_OP, IFluidHandler.FluidAction.EXECUTE);
                gasDst2.fill(new FluidStack(ModFluids.HYDROGEN_GAS.getSource(), FLUID_PER_OP), IFluidHandler.FluidAction.EXECUTE);
                int coFilled = gasDst.fill(new FluidStack(ModFluids.CARBON_MONOXIDE.getSource(), FLUID_PER_OP), IFluidHandler.FluidAction.EXECUTE);
                if (level instanceof net.minecraft.server.level.ServerLevel sl && coFilled < FLUID_PER_OP)
                    coLeak(sl, pos);
            }
            return;
        }

        // =====================================================================
        // 煤炭干馏：煤 → 焦油 + CO（需要热源，无氧加热）
        // =====================================================================
        if (heat >= 0 && (isCoal(in1) || isCoal(in2))
                && (midDst.isEmpty() || midDst.getFluid().getFluid().isSame(ModFluids.COAL_TAR.getSource()))
                && midDst.getFluidAmount() + FLUID_PER_OP <= TANK_CAP
                && (gasDst.isEmpty() || gasDst.getFluid().getFluid().isSame(ModFluids.CARBON_MONOXIDE.getSource()))
                && gasDst.getFluidAmount() + FLUID_PER_OP <= TANK_CAP) {
            ItemStack coal = isCoal(in1) ? in1 : in2;
            e.progress++; if (e.progress >= 200) { e.progress = 0; coal.shrink(1);
                midDst.fill(new FluidStack(ModFluids.COAL_TAR.getSource(), FLUID_PER_OP), IFluidHandler.FluidAction.EXECUTE);
                int coFilled = gasDst.fill(new FluidStack(ModFluids.CARBON_MONOXIDE.getSource(), FLUID_PER_OP), IFluidHandler.FluidAction.EXECUTE);
                if (level instanceof net.minecraft.server.level.ServerLevel sl && coFilled < FLUID_PER_OP)
                    coLeak(sl, pos);
            }
            return;
        }

        // =====================================================================
        // 煅烧类（旧配方）：硫磺→硫酸、黄铁→硫酸+氧化铁、铜→氧化铜、铝→氧化铝
        // =====================================================================
        if (heat >= 0) {
            int inSlot = canProcess(in1) ? SLOT_INPUT_1 : (canProcess(in2) ? SLOT_INPUT_2 : -1);
            if (inSlot >= 0) {
                ItemStack input = e.items.getStackInSlot(inSlot);
                boolean needsWater = producesFluid(input);
                if (needsWater && waterSrc.getFluidAmount() < FLUID_PER_OP) { e.progress = 0; return; }
                if (needsWater && acidDst.getFluidAmount() + FLUID_PER_OP > TANK_CAP) { e.progress = 0; return; }
                e.progress++; if (e.progress >= 200) { e.progress = 0; input.shrink(1);
                    ItemStack solid = getSolidResult(input);
                    if (!solid.isEmpty()) addToSlot(e, SLOT_HIDDEN_OUT1, solid.copy());
                    if (needsWater) { waterSrc.drain(FLUID_PER_OP, IFluidHandler.FluidAction.EXECUTE);
                        acidDst.fill(new FluidStack(ModFluids.SULFURIC_ACID.getSource(), FLUID_PER_OP), IFluidHandler.FluidAction.EXECUTE); }
                }
                return;
            }
            // 镁煅烧：Mg锭/Mg粒 → MgO
            if (isMagnesium(in1)) {
                ItemStack out = e.items.getStackInSlot(SLOT_HIDDEN_OUT1);
                if (!out.isEmpty() && (!out.is(ModItems.MAGNESIUM_OXIDE.get()) || out.getCount() >= 64)) { e.progress = 0; return; }
                e.progress++; if (e.progress >= 200) { e.progress = 0; in1.shrink(1);
                    addToSlot(e, SLOT_HIDDEN_OUT1, new ItemStack(ModItems.MAGNESIUM_OXIDE.get())); }
                return;
            }
            if (isMagnesium(in2)) {
                ItemStack out = e.items.getStackInSlot(SLOT_HIDDEN_OUT2);
                if (!out.isEmpty() && (!out.is(ModItems.MAGNESIUM_OXIDE.get()) || out.getCount() >= 64)) { e.progress = 0; return; }
                e.progress++; if (e.progress >= 200) { e.progress = 0; in2.shrink(1);
                    addToSlot(e, SLOT_HIDDEN_OUT2, new ItemStack(ModItems.MAGNESIUM_OXIDE.get())); }
                return;
            }
        }

        // =====================================================================
        // 原油蒸馏：原油(DG) → 焦油（需要热源）
        // =====================================================================
        if (heat >= 0 && !waterSrc.isEmpty()
                && waterSrc.getFluid().getFluid().builtInRegistryHolder().key().location().toString().equals("createdieselgenerators:crude_oil")
                && waterSrc.getFluidAmount() >= FLUID_PER_OP
                && (midDst.isEmpty() || midDst.getFluid().getFluid().isSame(ModFluids.COAL_TAR.getSource()))
                && midDst.getFluidAmount() + FLUID_PER_OP <= TANK_CAP) {
            e.progress++; if (e.progress >= 200) { e.progress = 0;
                waterSrc.drain(FLUID_PER_OP, IFluidHandler.FluidAction.EXECUTE);
                midDst.fill(new FluidStack(ModFluids.COAL_TAR.getSource(), FLUID_PER_OP), IFluidHandler.FluidAction.EXECUTE); }
            return;
        }

        // =====================================================================
        // 水蒸发 → 卤水（需要热源，water → brine 250→25 mB）
        // =====================================================================
        if (heat >= 0 && !waterSrc.isEmpty() && waterSrc.getFluid().getFluid().isSame(Fluids.WATER)
                && waterSrc.getFluidAmount() >= FLUID_PER_OP
                && (midDst.isEmpty() || midDst.getFluid().getFluid().isSame(ModFluids.BRINE.getSource()))
                && midDst.getFluidAmount() + 25 <= TANK_CAP) {
            e.progress++; if (e.progress >= 200) { e.progress = 0;
                waterSrc.drain(FLUID_PER_OP, IFluidHandler.FluidAction.EXECUTE);
                midDst.fill(new FluidStack(ModFluids.BRINE.getSource(), 25), IFluidHandler.FluidAction.EXECUTE); }
            return;
        }

        // =====================================================================
        // 卤水蒸发 → MgCl₂ + NaCl + KNO₃（需要热源）
        // =====================================================================
        if (heat >= 0 && !waterSrc.isEmpty() && waterSrc.getFluid().getFluid().isSame(ModFluids.BRINE.getSource())
                && waterSrc.getFluidAmount() >= FLUID_PER_OP
                && hasSpaceForSolids(e, 3)) {
            e.progress++; if (e.progress >= 200) { e.progress = 0;
                waterSrc.drain(FLUID_PER_OP, IFluidHandler.FluidAction.EXECUTE);
                if (level.random.nextFloat() < 0.3f) addToSlot(e, SLOT_HIDDEN_OUT1, new ItemStack(ModItems.MAGNESIUM_CHLORIDE.get()));
                if (level.random.nextFloat() < 0.3f) addToSlot(e, SLOT_HIDDEN_OUT1, new ItemStack(ModItems.SODIUM_CHLORIDE.get()));
                if (level.random.nextFloat() < 0.3f) addToSlot(e, SLOT_HIDDEN_OUT2, new ItemStack(ModItems.POTASSIUM_NITRATE.get())); }
            return;
        }

        // =====================================================================
        // 乙醛氧化：乙醛 → 乙酸（不需要热源！无催化剂）
        // =====================================================================
        if (!waterSrc.isEmpty() && waterSrc.getFluid().getFluid().isSame(ModFluids.ACETALDEHYDE.getSource())
                && waterSrc.getFluidAmount() >= FLUID_PER_OP
                && (midDst.isEmpty() || midDst.getFluid().getFluid().isSame(ModFluids.ACETIC_ACID.getSource()))
                && midDst.getFluidAmount() + FLUID_PER_OP <= TANK_CAP) {
            e.progress++; if (e.progress >= 200) { e.progress = 0;
                waterSrc.drain(FLUID_PER_OP, IFluidHandler.FluidAction.EXECUTE);
                midDst.fill(new FluidStack(ModFluids.ACETIC_ACID.getSource(), FLUID_PER_OP), IFluidHandler.FluidAction.EXECUTE); }
            return;
        }

        // =====================================================================
        // 尿素合成：NH₃ + CO₂ → 尿素（需要热源，高温高压）
        // NH₃ 来自 gasTank，CO₂ 来自 waterTank 泵入
        // =====================================================================
        if (heat >= 0 && !gasDst.isEmpty() && gasDst.getFluid().getFluid().isSame(ModFluids.AMMONIA_GAS.getSource())
                && gasDst.getFluidAmount() >= 200
                && !waterSrc.isEmpty() && waterSrc.getFluid().getFluid().isSame(ModFluids.CARBON_DIOXIDE.getSource())
                && waterSrc.getFluidAmount() >= 100
                && hasSpaceForSolids(e, 1)) {
            e.progress++; if (e.progress >= 200) { e.progress = 0;
                gasDst.drain(200, IFluidHandler.FluidAction.EXECUTE);
                waterSrc.drain(100, IFluidHandler.FluidAction.EXECUTE);
                addToSlot(e, SLOT_HIDDEN_OUT1, new ItemStack(ModItems.UREA.get())); }
            return;
        }

        // =====================================================================
        // 苯酚合成：苯 + O₂(空气) → 苯酚（需要热源、无催化剂）
        // =====================================================================
        if (heat >= 0 && !waterSrc.isEmpty() && waterSrc.getFluid().getFluid().isSame(ModFluids.BENZENE.getSource())
                && waterSrc.getFluidAmount() >= FLUID_PER_OP
                && (midDst.isEmpty() || midDst.getFluid().getFluid().isSame(ModFluids.PHENOL.getSource()))
                && midDst.getFluidAmount() + FLUID_PER_OP <= TANK_CAP) {
            e.progress++; if (e.progress >= 200) { e.progress = 0;
                waterSrc.drain(FLUID_PER_OP, IFluidHandler.FluidAction.EXECUTE);
                midDst.fill(new FluidStack(ModFluids.PHENOL.getSource(), FLUID_PER_OP), IFluidHandler.FluidAction.EXECUTE); }
            return;
        }

        // =====================================================================
        // 丙酮合成：乙酸 → 丙酮 + CO₂（需要热源）
        // =====================================================================
        if (heat >= 0 && !waterSrc.isEmpty() && waterSrc.getFluid().getFluid().isSame(ModFluids.ACETIC_ACID.getSource())
                && waterSrc.getFluidAmount() >= FLUID_PER_OP
                && (midDst.isEmpty() || midDst.getFluid().getFluid().isSame(ModFluids.ACETONE.getSource()))
                && midDst.getFluidAmount() + FLUID_PER_OP <= TANK_CAP
                && (gasDst.isEmpty() || gasDst.getFluid().getFluid().isSame(ModFluids.CARBON_DIOXIDE.getSource()))
                && gasDst.getFluidAmount() + FLUID_PER_OP <= TANK_CAP) {
            e.progress++; if (e.progress >= 200) { e.progress = 0;
                waterSrc.drain(FLUID_PER_OP, IFluidHandler.FluidAction.EXECUTE);
                midDst.fill(new FluidStack(ModFluids.ACETONE.getSource(), FLUID_PER_OP), IFluidHandler.FluidAction.EXECUTE);
                gasDst.fill(new FluidStack(ModFluids.CARBON_DIOXIDE.getSource(), FLUID_PER_OP), IFluidHandler.FluidAction.EXECUTE); }
            return;
        }

        e.progress = 0;
    }

    public IFluidHandler getFluidHandler(@Nullable Direction side) {
        var layer = getBlockState().getValue(FluidizedBedBlock.LAYER);
        if (side==null) return makeDisplay(layer);
        if (layer==FluidizedBedBlock.Layer.BOTTOM) return new IFluidHandler() {
            @Override public int getTanks() { return 3; }
            @Override public FluidStack getFluidInTank(int t) {
                if (t==0) return waterTank.getFluid();
                if (t==1) return h2Tank.getFluid();
                return n2Tank.getFluid();
            }
            @Override public int getTankCapacity(int t) { return t==0 ? waterTank.getCapacity() : (t==1 ? h2Tank.getCapacity() : n2Tank.getCapacity()); }
            @Override public boolean isFluidValid(int t,FluidStack s) {
                if (t==0) return true;  // 通用液体输入
                if (t==1) return h2Tank.isFluidValid(s);
                return n2Tank.isFluidValid(s);
            }
            @Override public int fill(FluidStack r,FluidAction a) {
                if (n2Tank.isFluidValid(r)) return n2Tank.fill(r,a);
                if (h2Tank.isFluidValid(r)) return h2Tank.fill(r,a);
                return waterTank.fill(r,a);  // 默认进通用液体槽
            }
            @Override public FluidStack drain(FluidStack r,FluidAction a) {
                if (r.getFluid().isSame(h2Tank.getFluid().getFluid())) return h2Tank.drain(r,a);
                if (r.getFluid().isSame(n2Tank.getFluid().getFluid())) return n2Tank.drain(r,a);
                return FluidStack.EMPTY;
            }
            @Override public FluidStack drain(int max,FluidAction a) {
                FluidStack d = h2Tank.drain(max,a);
                if (!d.isEmpty()) return d;
                return n2Tank.drain(max,a);
            }
        };
        if (layer==FluidizedBedBlock.Layer.MIDDLE) return new IFluidHandler() {
            @Override public int getTanks() { return 2; }
            @Override public FluidStack getFluidInTank(int t) { return t==0 ? acidTank.getFluid() : midTank.getFluid(); }
            @Override public int getTankCapacity(int t) { return TANK_CAP; }
            @Override public boolean isFluidValid(int t,FluidStack s) { return false; }
            @Override public int fill(FluidStack r,FluidAction a2) { return 0; }
            @Override public FluidStack drain(FluidStack r,FluidAction a2) {
                if (acidTank.getFluid().getFluid().isSame(r.getFluid())) return acidTank.drain(r,a2);
                if (midTank.getFluid().getFluid().isSame(r.getFluid())) return midTank.drain(r,a2);
                return FluidStack.EMPTY;
            }
            @Override public FluidStack drain(int max,FluidAction a2) {
                FluidStack d = acidTank.drain(max,a2);
                return d.isEmpty() ? midTank.drain(max,a2) : d;
            }
        };
        if (layer==FluidizedBedBlock.Layer.TOP) return new IFluidHandler() {
            @Override public int getTanks() { return 2; }
            @Override public FluidStack getFluidInTank(int t) { return t==0 ? gasTank.getFluid() : gasTank2.getFluid(); }
            @Override public int getTankCapacity(int t) { return t==0 ? gasTank.getCapacity() : gasTank2.getCapacity(); }
            @Override public boolean isFluidValid(int t,FluidStack s) { return false; }
            @Override public int fill(FluidStack r,FluidAction a2) { return 0; }
            @Override public FluidStack drain(FluidStack r,FluidAction a2) {
                if (gasTank.getFluid().getFluid().isSame(r.getFluid())) return gasTank.drain(r,a2);
                return gasTank2.drain(r,a2);
            }
            @Override public FluidStack drain(int max,FluidAction a2) {
                FluidStack d = gasTank.drain(max,a2);
                return d.isEmpty() ? gasTank2.drain(max,a2) : d;
            }
        };
        return NULL;
    }
    private IFluidHandler makeDisplay(FluidizedBedBlock.Layer layer) {
        return new IFluidHandler() {
        @Override public int getTanks() { return layer==FluidizedBedBlock.Layer.BOTTOM?3:(layer==FluidizedBedBlock.Layer.MIDDLE?2:2); }
        @Override public FluidStack getFluidInTank(int t) {
            if (layer==FluidizedBedBlock.Layer.BOTTOM) {
                if (t==0) return waterTank.getFluid();
                if (t==1) return h2Tank.getFluid();
                return n2Tank.getFluid();
            }
            if (layer==FluidizedBedBlock.Layer.MIDDLE) {
                if (t==1) return midTank.getFluid();
                return acidTank.getFluid();
            }
            // TOP: 2 gas tanks
            if (t==1) return gasTank2.getFluid();
            return gasTank.getFluid();
        }
        @Override public int getTankCapacity(int t) { return t==0 ? gasTank.getCapacity() : gasTank2.getCapacity(); }
        @Override public boolean isFluidValid(int t,FluidStack s) { return false; }
        @Override public int fill(FluidStack r,FluidAction a2) { return 0; }
        @Override public FluidStack drain(FluidStack r,FluidAction a2) { return FluidStack.EMPTY; }
        @Override public FluidStack drain(int max,FluidAction a2) { return FluidStack.EMPTY; }
    }; }

    private static boolean canProcess(ItemStack s) { if(s.isEmpty())return false; var i=s.getItem(); if(i==ModItems.SULFUR_POWDER.get()||i==ModItems.RAW_PYRITE.get())return true; if(i==Items.COPPER_INGOT||i==Items.RAW_COPPER||i==Items.COPPER_BLOCK)return true; if(i==ModItems.ALUMINUM_INGOT.get()||i==ModItems.ALUMINUM_NUGGET.get())return true; String k=i.builtInRegistryHolder().key().location().toString(); return k.equals("create:copper_sheet")||k.equals("create:aluminum_sheet"); }
    private static boolean producesFluid(ItemStack s) { var i=s.getItem(); return i==ModItems.SULFUR_POWDER.get()||i==ModItems.RAW_PYRITE.get(); }
    private static ItemStack getSolidResult(ItemStack in) { if(in.isEmpty())return ItemStack.EMPTY; var i=in.getItem(); if(i==ModItems.RAW_PYRITE.get())return new ItemStack(ModItems.IRON_OXIDE.get()); if(i==Items.COPPER_BLOCK)return new ItemStack(ModItems.COPPER_OXIDE.get(),9); if(i==Items.COPPER_INGOT||i==Items.RAW_COPPER)return new ItemStack(ModItems.COPPER_OXIDE.get()); if(i==ModItems.ALUMINUM_INGOT.get())return new ItemStack(ModItems.ALUMINA.get()); if(i==ModItems.ALUMINUM_NUGGET.get())return new ItemStack(ModItems.ALUMINA.get()); return ItemStack.EMPTY; }
    private static boolean hasIron(ItemStack a,ItemStack b) { return isIron(a)||isIron(b); } private static boolean hasCarbon(ItemStack a,ItemStack b) { return isCarbon(a)||isCarbon(b); }
    private static boolean isIron(ItemStack s) { return !s.isEmpty()&&s.getItem()==Items.IRON_INGOT; }
    private static boolean isCarbon(ItemStack s) { if(s.isEmpty())return false; var i=s.getItem(); return i==Items.COAL||i==Items.CHARCOAL||i==ModItems.GRAPHITE.get(); }
    private static boolean isIronSheet(ItemStack s) { return !s.isEmpty()&&s.getItem().builtInRegistryHolder().key().location().toString().equals("create:iron_sheet"); }
    /** 煤/木炭 */
    private static boolean isCoal(ItemStack s) { if(s.isEmpty())return false; var i=s.getItem(); return i==Items.COAL||i==Items.CHARCOAL; }
    /** 银锭 — 甲醛合成催化剂 */
    private static boolean isSilver(ItemStack s) { return !s.isEmpty()&&s.getItem()==ModItems.SILVER_INGOT.get(); }
    /** 矿石+碳组合检查（方铅矿/锡石 + 煤/木炭/石墨） */
    private static boolean hasOreAndCarbon(ItemStack a, ItemStack b, String oreName) {
        return (isOre(a, oreName) && isCarbon(b)) || (isOre(b, oreName) && isCarbon(a));
    }
    private static boolean isOre(ItemStack s, String oreName) {
        if (s.isEmpty()) return false;
        String k = s.getItem().builtInRegistryHolder().key().location().toString();
        return k.equals("chemical_industry:" + oreName) || k.equals("chemical_industry:deepslate_" + oreName);
    }
    private static void consumeOreAndCarbon(FluidizedBedBlockEntity e, String oreName) {
        for (int s = SLOT_INPUT_1; s <= SLOT_INPUT_2; s++) {
            ItemStack st = e.items.getStackInSlot(s);
            if (isOre(st, oreName)) { st.shrink(1); break; }
        }
        for (int s = SLOT_INPUT_1; s <= SLOT_INPUT_2; s++) {
            if (isCarbon(e.items.getStackInSlot(s))) { e.items.getStackInSlot(s).shrink(1); break; }
        }
    }
    private static void consume(FluidizedBedBlockEntity e,boolean iron,boolean carbon) { for(int s=SLOT_INPUT_1;s<=SLOT_INPUT_2;s++){ItemStack st=e.items.getStackInSlot(s); if(iron&&isIron(st)){st.shrink(1);iron=false;}else if(carbon&&isCarbon(st)){st.shrink(1);carbon=false;}} }
    /** 检查是否为 Minecraft 自然生成的石头（完整方块） */
    private static boolean isNaturalStone(ItemStack s) { if(s.isEmpty())return false; String k=s.getItem().builtInRegistryHolder().key().location().toString(); return k.equals("minecraft:stone")||k.equals("minecraft:cobblestone")||k.equals("minecraft:granite")||k.equals("minecraft:diorite")||k.equals("minecraft:andesite")||k.equals("minecraft:deepslate")||k.equals("minecraft:tuff")||k.equals("minecraft:calcite")||k.equals("minecraft:dripstone_block")||k.equals("minecraft:basalt")||k.equals("minecraft:smooth_basalt")||k.equals("minecraft:sandstone")||k.equals("minecraft:red_sandstone")||k.equals("minecraft:netherrack")||k.equals("minecraft:blackstone")||k.equals("minecraft:end_stone"); }
    /** SiO₂ 碳热还原 */
    private static boolean hasSilica(ItemStack a,ItemStack b) { return isSilica(a)||isSilica(b); }
    private static boolean isSilica(ItemStack s) { return !s.isEmpty()&&s.is(ModItems.SILICON_DIOXIDE.get()); }
    private static void consumeSilica(FluidizedBedBlockEntity e) { for(int s=SLOT_INPUT_1;s<=SLOT_INPUT_2;s++){if(isSilica(e.items.getStackInSlot(s))){e.items.getStackInSlot(s).shrink(1);break;}} }
    private static void consumeCarbon(FluidizedBedBlockEntity e) { for(int s=SLOT_INPUT_1;s<=SLOT_INPUT_2;s++){if(isCarbon(e.items.getStackInSlot(s))){e.items.getStackInSlot(s).shrink(1);break;}} }
    /** 镁锭/镁粒 */
    private static boolean isMagnesium(ItemStack s) { return !s.isEmpty()&&(s.is(ModItems.MAGNESIUM_INGOT.get())||s.is(ModItems.MAGNESIUM_NUGGET.get())); }
    /** 检查固体输出槽位是否有足够空间放 N 个不同物品 */
    private static boolean hasSpaceForSolids(FluidizedBedBlockEntity e, int types) { int space=0; for(int s=SLOT_HIDDEN_OUT1;s<=SLOT_HIDDEN_OUT2;s++){ItemStack st=e.items.getStackInSlot(s); if(st.isEmpty()||st.getCount()<64)space++;} return space>=types; }
    /** 将物品添加到固体输出槽（合并同种或放入空槽） */
    private static void addToSlot(FluidizedBedBlockEntity e, int startSlot, ItemStack toAdd) {
        for (int s = startSlot; s <= SLOT_HIDDEN_OUT2; s++) {
            ItemStack st = e.items.getStackInSlot(s);
            if (st.isEmpty()) { e.items.setStackInSlot(s, toAdd); return; }
            if (st.is(toAdd.getItem()) && st.getCount() + toAdd.getCount() <= 64) { st.grow(toAdd.getCount()); return; }
        }
    }
    /** CO 溢出：范围中毒效果 */
    private static void coLeak(net.minecraft.server.level.ServerLevel sl, BlockPos pos) {
        for (net.minecraft.world.entity.LivingEntity entity : sl.getEntitiesOfClass(
                net.minecraft.world.entity.LivingEntity.class,
                new net.minecraft.world.phys.AABB(pos).inflate(5.0))) {
            entity.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.POISON, 200, 1));  // 10秒中毒II
        }
    }
    /** 检测热源：向下跳过所有沸腾炉层，找到真正的热源方块 */
    private static int getHeat(Level l, BlockPos p) {
        BlockPos check = p.below();
        // 跳过沸腾炉自身的层（BOTTOM/MIDDLE/TOP）
        while (l.getBlockState(check).is(ModBlocks.FLUIDIZED_BED.get())) check = check.below();
        String k = l.getBlockState(check).getBlock().builtInRegistryHolder().key().location().toString();
        if (k.equals("minecraft:lava") || k.equals("minecraft:campfire") || k.equals("minecraft:magma_block")) return 1;
        if (k.equals("create:blaze_burner")) return 2;
        return -1;
    }

    @Override public Component getDisplayName() { return Component.translatable("block.chemical_industry.fluidized_bed"); }
    /** GUI 共享：任意中层打开同一个 Controller 的物品槽 */
    public FluidizedBedBlockEntity guiBE() {
        if (isController()) return this;
        FluidizedBedBlockEntity c = getControllerBE();
        return c!=null ? c : this;
    }
    @Nullable @Override public AbstractContainerMenu createMenu(int id,Inventory inv,Player p) { FluidizedBedBlockEntity g = guiBE(); return new com.user.chemical_industry.screen.FluidizedBedMenu(id,inv,g,g.dataAccess); }
    public IItemHandler getItemHandler() { return guiBE().items; }
    public net.neoforged.neoforge.items.IItemHandlerModifiable getGuiItemHandler() {
        ItemStackHandler it = guiBE().items;
        return new net.neoforged.neoforge.items.IItemHandlerModifiable() {
            @Override public int getSlots() { return 3; }
            @Override public ItemStack getStackInSlot(int s) { return it.getStackInSlot(s); }
            @Override public void setStackInSlot(int s, ItemStack st) { it.setStackInSlot(s, st); }
            @Override public ItemStack insertItem(int s, ItemStack st, boolean sim) { if (s < 3) return it.insertItem(s, st, sim); return st; }
            @Override public ItemStack extractItem(int s, int a, boolean sim) { return s < 3 ? it.extractItem(s, a, sim) : ItemStack.EMPTY; }
            @Override public int getSlotLimit(int s) { return s < 3 ? it.getSlotLimit(s) : 0; }
            @Override public boolean isItemValid(int s, ItemStack st) { return s < 3 && it.isItemValid(s, st); }
        };
    }
    public IItemHandler getAutomationHandler() {
        ItemStackHandler it = guiBE().items;
        return new IItemHandler() {
            // 漏斗/管道等自动化只能输入两个原料槽（1、2）；
            // 催化剂槽(0)必须玩家手动放入，防止漏斗误投
            @Override public int getSlots() { return SLOT_COUNT; }
            @Override public ItemStack getStackInSlot(int s) { return it.getStackInSlot(s); }
            @Override public ItemStack insertItem(int s, ItemStack st, boolean sim) {
                if (s != SLOT_INPUT_1 && s != SLOT_INPUT_2) return st;  // 拒绝其他槽
                return it.insertItem(s, st, sim);
            }
            @Override public ItemStack extractItem(int s, int a, boolean sim) {
                if (s < SLOT_HIDDEN_OUT1) return ItemStack.EMPTY;  // 原料槽/催化剂槽不可自动取出
                return it.extractItem(s, a, sim);
            }
            @Override public int getSlotLimit(int s) { return it.getSlotLimit(s); }
            @Override public boolean isItemValid(int s, ItemStack st) {
                return s == SLOT_INPUT_1 || s == SLOT_INPUT_2;  // 只认可两个原料槽
            }
        };
    }

    @Override protected void saveAdditional(CompoundTag t,HolderLookup.Provider r) { super.saveAdditional(t,r); t.put("Items",items.serializeNBT(r)); t.putInt("Prog",progress); if(!isController())t.put("Ctrl",net.minecraft.nbt.NbtUtils.writeBlockPos(getController())); t.put("Water",waterTank.writeToNBT(r,new CompoundTag())); t.put("H2",h2Tank.writeToNBT(r,new CompoundTag())); t.put("N2",n2Tank.writeToNBT(r,new CompoundTag())); t.put("Acid",acidTank.writeToNBT(r,new CompoundTag())); t.put("Mid",midTank.writeToNBT(r,new CompoundTag())); t.put("Gas",gasTank.writeToNBT(r,new CompoundTag())); t.put("Gas2",gasTank2.writeToNBT(r,new CompoundTag())); if(originalTankKey!=null)t.putString("Tank",originalTankKey); }
    @Override protected void loadAdditional(CompoundTag t,HolderLookup.Provider r) { super.loadAdditional(t,r); if(t.contains("Items")){CompoundTag it=t.getCompound("Items"); if(it.getInt("Size")<SLOT_COUNT){CompoundTag f=new CompoundTag();f.putInt("Size",SLOT_COUNT);for(int i=0;i<it.getInt("Size");i++)if(it.contains("Slot"+i))f.put("Slot"+i,it.get("Slot"+i));items.deserializeNBT(r,f);}else items.deserializeNBT(r,it);} progress=t.getInt("Prog"); if(t.contains("Ctrl"))controller=net.minecraft.nbt.NbtUtils.readBlockPos(t,"Ctrl").orElse(null); if(t.contains("Water"))waterTank.readFromNBT(r,t.getCompound("Water")); if(t.contains("H2"))h2Tank.readFromNBT(r,t.getCompound("H2")); if(t.contains("N2"))n2Tank.readFromNBT(r,t.getCompound("N2")); if(t.contains("Acid"))acidTank.readFromNBT(r,t.getCompound("Acid")); if(t.contains("Mid"))midTank.readFromNBT(r,t.getCompound("Mid")); if(t.contains("Gas"))gasTank.readFromNBT(r,t.getCompound("Gas")); if(t.contains("Gas2"))gasTank2.readFromNBT(r,t.getCompound("Gas2")); if(t.contains("Tank"))originalTankKey=t.getString("Tank"); updateConnectivity=true; }
    public void dropContents() { dropContentsAt(worldPosition); }
    /** 只掉物品，控制器由 loot_table 负责 */
    public void dropContentsAt(BlockPos pos) { if(level==null||level.isClientSide())return; for(int i=0;i<SLOT_COUNT;i++){ItemStack s=items.getStackInSlot(i); if(!s.isEmpty())net.minecraft.world.Containers.dropItemStack(level,pos.getX(),pos.getY(),pos.getZ(),s);} }
    /** 清空所有物品槽（扳手还原前调用，防止 onRemove 重复掉落） */
    public void clearItems() { for (int i = 0; i < SLOT_COUNT; i++) items.setStackInSlot(i, ItemStack.EMPTY); setChanged(); }
}
