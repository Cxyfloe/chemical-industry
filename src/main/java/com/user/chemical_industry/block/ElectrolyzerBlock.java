package com.user.chemical_industry.block;

import com.user.chemical_industry.block_entity.ElectrolyzerBlockEntity;
import com.user.chemical_industry.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/** 电解槽——3格宽水平多方块：CATHODE|CENTER|ANODE */
public class ElectrolyzerBlock extends BaseEntityBlock {

    public enum Part implements StringRepresentable {
        CATHODE("cathode"), CENTER("center"), ANODE("anode");
        final String n; Part(String n) { this.n = n; }
        @Override public String getSerializedName() { return n; }
    }
    public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);
    public static final DirectionProperty FACING = DirectionProperty.create("facing", Direction.Plane.HORIZONTAL);

    public ElectrolyzerBlock() { this(Properties.of().mapColor(MapColor.METAL).strength(2f,3f).requiresCorrectToolForDrops().sound(SoundType.METAL)); }
    public ElectrolyzerBlock(Properties p) { super(p); registerDefaultState(stateDefinition.any().setValue(PART, Part.CENTER).setValue(FACING, Direction.NORTH)); }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return simpleCodec(ElectrolyzerBlock::new); }
    @Override public RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(PART, FACING); }

    @Nullable @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction facing = ctx.getHorizontalDirection().getOpposite();
        BlockState state = defaultBlockState().setValue(FACING, facing);
        Part role = detectRole(ctx.getLevel(), ctx.getClickedPos(), facing);
        return state.setValue(PART, role);
    }

    /** 根据左右邻居检测角色 */
    private Part detectRole(Level level, BlockPos pos, Direction facing) {
        Direction leftD = facing.getCounterClockWise(), rightD = facing.getClockWise();
        boolean L = isSame(level, pos.relative(leftD), facing), R = isSame(level, pos.relative(rightD), facing);
        if (L && R) return Part.CENTER;
        if (R) return Part.CATHODE;  // 右边有→我是左=阴极
        if (L) return Part.ANODE;   // 左边有→我是右=阳极
        return Part.CENTER;
    }
    private boolean isSame(Level l, BlockPos p, Direction f) {
        BlockState s = l.getBlockState(p);
        return s.is(this) && s.getValue(FACING) == f;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block nb, BlockPos np, boolean mv) {
        super.neighborChanged(state, level, pos, nb, np, mv);
        if (level.isClientSide()) return;
        Direction facing = state.getValue(FACING);
        Part d = detectRole(level, pos, facing);
        if (d != state.getValue(PART)) level.setBlock(pos, state.setValue(PART, d), Block.UPDATE_ALL);
        // 更新相邻电解槽
        for (Direction sd : new Direction[]{facing.getClockWise(), facing.getCounterClockWise()}) {
            BlockPos ap = pos.relative(sd);
            BlockState as = level.getBlockState(ap);
            if (as.is(this) && as.getValue(FACING) == facing) {
                Part ad = detectRole(level, ap, facing);
                if (ad != as.getValue(PART)) level.setBlock(ap, as.setValue(PART, ad), Block.UPDATE_ALL);
            }
        }
    }

    @Nullable @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState s) { return new ElectrolyzerBlockEntity(pos, s); }
    @Nullable @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l, BlockState s, BlockEntityType<T> t) {
        return l.isClientSide() ? null : createTickerHelper(t, ModBlockEntities.ELECTROLYZER.get(), ElectrolyzerBlockEntity::tick);
    }

    @Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
            BlockPos pos, net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) return net.minecraft.world.ItemInteractionResult.sidedSuccess(true);
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof ElectrolyzerBlockEntity e && e.isPart(Part.CENTER) && e.isFront())
            player.openMenu(e, pos);
        return net.minecraft.world.ItemInteractionResult.sidedSuccess(false);
    }

    @Override protected void onRemove(BlockState s, Level l, BlockPos p, BlockState ns, boolean m) {
        if (s.getBlock() == ns.getBlock()) return;
        if (l.getBlockEntity(p) instanceof ElectrolyzerBlockEntity e) e.dropContents();
        super.onRemove(s, l, p, ns, m);
    }
}
