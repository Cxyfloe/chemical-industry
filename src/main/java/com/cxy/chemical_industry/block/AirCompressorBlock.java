package com.cxy.chemical_industry.block;

import com.cxy.chemical_industry.block_entity.AirCompressorBlockEntity;
import com.cxy.chemical_industry.registry.ModBlockEntities;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.Nullable;

/** 空气压缩机 — 齿轮驱动的动能方块，产生压缩空气 */
public class AirCompressorBlock extends DirectionalKineticBlock implements IBE<AirCompressorBlockEntity> {

    public AirCompressorBlock() {
        this(Properties.of().mapColor(MapColor.METAL).strength(2f, 3f)
                .requiresCorrectToolForDrops().sound(SoundType.METAL));
    }
    public AirCompressorBlock(Properties p) { super(p); }

    @Override public Class<AirCompressorBlockEntity> getBlockEntityClass() { return AirCompressorBlockEntity.class; }
    @Override public BlockEntityType<? extends AirCompressorBlockEntity> getBlockEntityType() { return ModBlockEntities.AIR_COMPRESSOR.get(); }
    @Override public Direction.Axis getRotationAxis(BlockState state) { return state.getValue(FACING).getAxis(); }

    /** 只有背面接受轴连接 */
    @Override public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face == state.getValue(FACING).getOpposite();
    }

    @Override @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction facing = ctx.getHorizontalDirection().getOpposite();
        return defaultBlockState().setValue(FACING, facing);
    }
}
