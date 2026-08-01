package com.user.chemical_industry.event;

import com.user.chemical_industry.ChemicalIndustry;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CropBlock;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * 植物相关成就
 *
 * 监听玩家用骨粉类物品（骨粉/尿素/硝酸钾）右键作物：
 * 催熟成功 → 授予"现代农业"成就
 */
@EventBusSubscriber(modid = ChemicalIndustry.MOD_ID)
public class PlantHandler {

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        if (level.isClientSide()) return;

        // 手持骨粉类物品（尿素/硝酸钾都是 BoneMealItem 子类，行为同骨粉）
        ItemStack held = event.getItemStack();
        if (!(held.getItem() instanceof BoneMealItem)) return;

        // 目标必须是作物方块（催熟生效才给成就）
        if (level.getBlockState(event.getPos()).getBlock() instanceof CropBlock) {
            AdvancementHelper.grantNearby(level, event.getPos(), "fertilize", "fertilize");
        }
    }
}
