package com.user.chemical_industry.block;

import com.user.chemical_industry.registry.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * 活泼金属方块（铝块、镁块）— 遇酸（或铝遇碱）会迅速腐蚀消失
 *
 * 铝：遇酸 + NaOH 溶液腐蚀
 * 镁：遇酸腐蚀
 */
public class ReactiveMetalBlock extends Block {

    /** 铝块：酸和碱都腐蚀 */
    public static final int ALUMINUM = 0;
    /** 镁块：仅酸腐蚀 */
    public static final int MAGNESIUM = 1;

    private final int metalType;

    public ReactiveMetalBlock(int metalType, Properties properties) {
        super(properties);
        this.metalType = metalType;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        checkCorrosion(level, pos);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
                                    BlockPos neighborPos, boolean movedByPiston) {
        if (!level.isClientSide() && level instanceof ServerLevel sl) {
            checkCorrosion(sl, pos);
        }
    }

    /** 检查相邻6面是否有腐蚀性流体 */
    private void checkCorrosion(ServerLevel level, BlockPos pos) {
        for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.values()) {
            BlockPos neighbor = pos.relative(dir);
            FluidState fs = level.getFluidState(neighbor);
            if (fs.isEmpty() || !fs.isSource()) continue;

            String key = fs.getType().builtInRegistryHolder().key().location().toString();

            // 酸类腐蚀（铝和镁都会）
            if (key.equals("chemical_industry:sulfuric_acid")
                    || key.equals("chemical_industry:hydrochloric_acid")
                    || key.equals("chemical_industry:nitric_acid")
                    || key.equals("chemical_industry:acetic_acid")) {
                corrode(level, pos, dir);
                return;
            }

            // 铝还会被 NaOH 溶液腐蚀
            if (metalType == ALUMINUM && key.equals("chemical_industry:sodium_hydroxide_solution")) {
                corrode(level, pos, dir);
                return;
            }
        }
    }

    /** 腐蚀：破坏方块 + 粒子效果 */
    private void corrode(ServerLevel level, BlockPos pos, net.minecraft.core.Direction fromDir) {
        level.destroyBlock(pos, false);
        // 产生气泡粒子
        level.sendParticles(
                net.minecraft.core.particles.ParticleTypes.BUBBLE,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                10, 0.5, 0.5, 0.5, 0.02);
    }
}
