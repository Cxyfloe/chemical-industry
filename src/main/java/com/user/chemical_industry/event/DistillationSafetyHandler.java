package com.user.chemical_industry.event;

import com.user.chemical_industry.ChemicalIndustry;
import com.jesz.createdieselgenerators.content.distillation.DistillationTankBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

/** 分馏塔安全保护：压缩空气 + 热源 = 爆炸（强度 3） */
@EventBusSubscriber(modid = ChemicalIndustry.MOD_ID)
public class DistillationSafetyHandler {

    /** 方块放置时立即检测危险组合 */
    @SubscribeEvent
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        LevelAccessor la = event.getLevel();
        if (la.isClientSide() || !(la instanceof Level level)) return;

        String key = event.getPlacedBlock().getBlock().builtInRegistryHolder().key().location().toString();
        BlockPos pos = event.getPos();

        if (key.equals("createdieselgenerators:distillation_tank")) {
            // 放了蒸馏塔 → 检查下方热源 + 塔内是否含压缩空气
            if (hasBlaze(level, pos.below())) {
                checkAndExplode(level, pos);
            }
        } else if (key.equals("create:blaze_burner")) {
            // 放了烈焰人燃烧室 → 检查上方塔内是否含压缩空气
            checkAndExplode(level, pos.above());
        }
    }

    private static void checkAndExplode(Level level, BlockPos tankPos) {
        BlockEntity be = level.getBlockEntity(tankPos);
        if (be instanceof DistillationTankBlockEntity tank
                && !tank.tankInventory.isEmpty()
                && tank.tankInventory.getFluid().getFluid().builtInRegistryHolder().key().location()
                .toString().equals("chemical_industry:compressed_air")) {
            if (level instanceof ServerLevel sl)
                sl.explode(null, tankPos.getX() + 0.5, tankPos.getY() + 0.5, tankPos.getZ() + 0.5,
                        3.0f, Level.ExplosionInteraction.BLOCK);
        }
    }

    private static boolean hasBlaze(Level level, BlockPos pos) {
        return level.getBlockState(pos).getBlock().builtInRegistryHolder()
                .key().location().toString().equals("create:blaze_burner");
    }
}
