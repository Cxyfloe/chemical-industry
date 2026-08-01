package com.cxy.chemical_industry.block;

import com.cxy.chemical_industry.block_entity.FluidizedBedBlockEntity;
import com.cxy.chemical_industry.block_entity.FluidizedBedBlockEntity;
import com.cxy.chemical_industry.registry.ModBlockEntities;
import com.cxy.chemical_industry.registry.ModBlocks;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;

/** 沸腾炉 — DG DistillationTank 模式：同一类型、BFS 连接、IWrenchable 还原 */
public class FluidizedBedBlock extends BaseEntityBlock {

    public enum Layer implements StringRepresentable {
        BOTTOM("bottom"), MIDDLE("middle"), TOP("top");
        final String n; Layer(String n) { this.n = n; }
        @Override public String getSerializedName() { return n; }
    }

    public static final EnumProperty<Layer> LAYER = EnumProperty.create("layer", Layer.class);
    public static final BooleanProperty LIT = BooleanProperty.create("lit");
    public static final BooleanProperty TOP = BooleanProperty.create("top");
    public static final BooleanProperty BOTTOM = BooleanProperty.create("bottom");

    public FluidizedBedBlock() { this(Properties.of().strength(2f,3f).requiresCorrectToolForDrops().sound(SoundType.METAL)); }
    public FluidizedBedBlock(Properties p) { super(p); registerDefaultState(stateDefinition.any().setValue(LAYER,Layer.MIDDLE).setValue(LIT,false).setValue(TOP,false).setValue(BOTTOM,false)); }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return simpleCodec(FluidizedBedBlock::new); }
    @Override public RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b) { b.add(LAYER,LIT,TOP,BOTTOM); }

    @Nullable @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState s) { return new FluidizedBedBlockEntity(pos,s); }
    @Nullable @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l, BlockState s, BlockEntityType<T> t) {
        if (l.isClientSide()) return null;
        return createTickerHelper(t, ModBlockEntities.FLUIDIZED_BED.get(), FluidizedBedBlockEntity::tick);
    }

    // ====== DG DistillationTank 模式 ======

    /** 放置 → 触发 BFS */
    @Override public void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
        if (old.getBlock() == state.getBlock()) return;
        if (moved) return;
        if (level.getBlockEntity(pos) instanceof FluidizedBedBlockEntity be)
            be.scheduleConnectivityUpdate();
    }

    /** 破坏 → 拆开连接 + 掉落物品 */
    @Override public void onRemove(BlockState s, Level l, BlockPos p, BlockState ns, boolean m) {
        if (s.hasBlockEntity() && (s.getBlock() != ns.getBlock() || !ns.hasBlockEntity())) {
            BlockEntity be = l.getBlockEntity(p);
            if (be instanceof FluidizedBedBlockEntity fbe) {
                fbe.dropContents();             // 掉落槽内物品
                fbe.removeController(true);     // 保留流体，拆开连接
                l.removeBlockEntity(p);
            }
        }
    }

    /** 选取 → Create 储罐 */
    @Override public ItemStack getCloneItemStack(BlockState s, HitResult t, LevelReader l, BlockPos p, Player pl) {
        String key = "create:fluid_tank";
        Block tank = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.parse(key));
        return tank != null ? new ItemStack(tank) : new ItemStack(net.minecraft.world.level.block.Blocks.IRON_BLOCK);
    }

    /** 右键 → 任意中层都打开同一个 GUI */
    @Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
            BlockPos pos, Player player, net.minecraft.world.InteractionHand hand, net.minecraft.world.phys.BlockHitResult hit) {
        String key = stack.getItem().builtInRegistryHolder().key().location().toString();
        // 扳手 → 还原全部
        if (key.equals("create:wrench")) {
            if (!level.isClientSide() && level.getBlockEntity(pos) instanceof FluidizedBedBlockEntity be) {
                var ctrl = be.guiBE();
                // BFS 收集所有连接方块
                var all = new java.util.HashSet<BlockPos>();
                var q = new java.util.ArrayDeque<BlockPos>();
                q.add(ctrl.getBlockPos());
                while (!q.isEmpty()) {
                    BlockPos pp = q.poll();
                    if (!all.add(pp)) continue;
                    for (net.minecraft.core.Direction d : net.minecraft.core.Direction.values()) {
                        BlockPos np = pp.relative(d);
                        if (!all.contains(np) && level.getBlockState(np).is(ModBlocks.FLUIDIZED_BED.get()))
                            q.add(np);
                    }
                }
                // 还原为储罐
                for (BlockPos pp : all) {
                    String tankKey = "create:fluid_tank";
                    Block tank = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(
                            net.minecraft.resources.ResourceLocation.parse(tankKey));
                    if (tank != null) level.setBlock(pp, tank.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
            return net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        // 中层 → GUI
        if (state.getValue(LAYER) == Layer.MIDDLE) {
            var be = level.getBlockEntity(pos);
            if (be instanceof FluidizedBedBlockEntity fbe && !level.isClientSide())
                player.openMenu(fbe.guiBE(), fbe.guiBE().getBlockPos());
        }
        return net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
}
