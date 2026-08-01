package com.user.chemical_industry.block;

import com.user.chemical_industry.registry.ModItems;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/**
 * 罂粟 — 开花植物（作物）
 *
 * 生长阶段：0~3（共 4 阶段），只能用罂粟果实种在耕地上。
 * 成熟（age=3）后收获得到罂粟果实（掉落表控制）。
 * 继承 CropBlock：自动获得随机生长、骨粉加速、耕地支持等原版作物行为。
 */
public class OpiumPoppyBlock extends CropBlock {

    public OpiumPoppyBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.PLANT)      // 植物绿色
                .noCollission()                // 无碰撞，可穿过
                .randomTicks()                 // 随机生长
                .instabreak()                  // 徒手瞬间破坏
                .sound(SoundType.CROP));       // 作物音效
    }

    /** 种子物品 = 罂粟果实（成熟掉落物也是它，形成循环） */
    @Override
    protected ItemLike getBaseSeedId() {
        return ModItems.OPIUM_POPPY_FRUIT.get();
    }

    /** 最大生长阶段：3（0~3 共 4 个阶段） */
    @Override
    public int getMaxAge() {
        return 3;
    }
}
