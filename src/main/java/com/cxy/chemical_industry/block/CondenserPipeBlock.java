package com.cxy.chemical_industry.block;

import com.cxy.chemical_industry.block_entity.CondenserPipeBlockEntity;
import com.cxy.chemical_industry.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/** 冷凝管 — 消耗压缩空气产生冷风，透风方块（非完整方块） */
public class CondenserPipeBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = DirectionalBlock.FACING;
    private static final MapCodec<CondenserPipeBlock> CODEC = simpleCodec(p -> new CondenserPipeBlock(p));

    // 管道形状：中间空心，四边框（类似铁栏杆但更小）
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(0,0,0,16,16,4),   // 正面
            Block.box(0,0,12,16,16,16), // 背面
            Block.box(0,0,0,4,16,16),   // 左
            Block.box(12,0,0,16,16,16)  // 右
    );

    public CondenserPipeBlock() {
        this(Properties.of().strength(2f,2f).requiresCorrectToolForDrops()
                .sound(SoundType.METAL).noOcclusion());
    }
    private CondenserPipeBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override public RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING);
    }

    @Nullable @Override public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getNearestLookingDirection().getOpposite());
    }

    // 非完整方块 → 透风
    @Override public VoxelShape getShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c) { return SHAPE; }
    @Override public VoxelShape getCollisionShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c) { return SHAPE; }

    @Nullable @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState s) {
        return new CondenserPipeBlockEntity(pos, s);
    }

    @Nullable @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l, BlockState s, BlockEntityType<T> t) {
        if (l.isClientSide()) return null;
        return createTickerHelper(t, ModBlockEntities.CONDENSER_PIPE.get(), CondenserPipeBlockEntity::tick);
    }

    @Override public BlockState rotate(BlockState s, Rotation r) { return s.setValue(FACING, r.rotate(s.getValue(FACING))); }
    @Override public BlockState mirror(BlockState s, Mirror m) { return s.rotate(m.getRotation(s.getValue(FACING))); }
}
