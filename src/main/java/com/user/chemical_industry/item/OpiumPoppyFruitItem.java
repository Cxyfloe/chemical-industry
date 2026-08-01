package com.user.chemical_industry.item;

import com.user.chemical_industry.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

/**
 * 罂粟果实 — 既是种子，也是食物
 *
 * 1. 种植：右键耕地（FARMLAND）在其上种植罂粟作物
 * 2. 食用：低配版鸦片（15 秒正面效果 → 240 秒负面效果），
 *    效果内容与鸦片相同但时长更短
 */
public class OpiumPoppyFruitItem extends OpiumItem {

    public OpiumPoppyFruitItem(Properties p) {
        // 潜伏 300 tick（15 秒），副作用 4800 tick（240 秒）
        super(p, 300, 4800);
    }

    /** 右键耕地种植罂粟 */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        // 目标是耕地
        if (context.getLevel().getBlockState(context.getClickedPos()).is(
                net.minecraft.world.level.block.Blocks.FARMLAND)) {
            BlockPos above = context.getClickedPos().above();
            // 耕地上方必须空着才能种
            if (context.getLevel().getBlockState(above).isAir()) {
                if (!context.getLevel().isClientSide()) {
                    context.getLevel().setBlock(above, ModBlocks.OPIUM_POPPY.get().defaultBlockState(), 3);
                    context.getItemInHand().shrink(1);  // 消耗 1 个果实
                }
                return InteractionResult.sidedSuccess(context.getLevel().isClientSide());
            }
        }
        return InteractionResult.PASS;
    }
}
