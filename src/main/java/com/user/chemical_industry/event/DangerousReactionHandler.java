package com.user.chemical_industry.event;

import com.user.chemical_industry.ChemicalIndustry;
import com.user.chemical_industry.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * 危险反应处理器：钠锭 + 酸 → 立即爆炸
 *
 * 监听玩家右键 Create 工作盆（Basin），检测其中是否同时有钠锭和酸，
 * 若有则触发爆炸（模拟剧烈的放热反应）。
 */
@EventBusSubscriber(modid = ChemicalIndustry.MOD_ID)
public class DangerousReactionHandler {

    /** 判断是否为强酸流体 */
    private static boolean isStrongAcid(FluidStack s) {
        String key = s.getFluid().builtInRegistryHolder().key().location().toString();
        return key.equals("chemical_industry:sulfuric_acid")
                || key.equals("chemical_industry:hydrochloric_acid")
                || key.equals("chemical_industry:nitric_acid");
    }

    @SubscribeEvent
    public static void onBlockRightClick(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        if (level.isClientSide()) return;
        BlockPos pos = event.getHitVec().getBlockPos();
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) return;

        // 检测是否为 Create Basin（工作盆）
        String beKey = be.getType().builtInRegistryHolder().key().location().toString();
        if (!beKey.equals("create:basin")) return;

        ItemStack held = event.getItemStack();
        // 玩家手持钠锭右键 Basin
        if (!held.is(ModItems.SODIUM_INGOT.get())) return;

        // 检查 Basin 内是否含酸
        IFluidHandler tank = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, event.getFace());
        if (tank == null) return;

        for (int i = 0; i < tank.getTanks(); i++) {
            FluidStack fs = tank.getFluidInTank(i);
            if (!fs.isEmpty() && isStrongAcid(fs)) {
                // 钠锭 + 酸 → 爆炸！
                held.shrink(1);
                tank.drain(fs.getAmount(), IFluidHandler.FluidAction.EXECUTE);
                level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        3.5f, Level.ExplosionInteraction.BLOCK);
                event.setCanceled(true);
                return;
            }
        }
    }
}
