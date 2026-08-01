package com.user.chemical_industry.item;

import com.user.chemical_industry.block.FluidizedBedBlock;
import com.user.chemical_industry.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * 沸腾炉控制器
 *
 * 右键 Create 流体储罐柱（≥3 个垂直堆叠）→ 全部转化为三层沸腾炉：
 *   底层(BOTTOM)：进水+空气（需动力泵）
 *   中层(MIDDLE)：GUI（催化剂+2输入+2输出）+ 液体输出(自动)+固体输出(漏斗)
 *   顶层(TOP)：气体输出(自动)
 */
public class FluidizedBedControllerItem extends Item {

    public FluidizedBedControllerItem() { super(new Properties().stacksTo(64)); }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        var player = ctx.getPlayer();
        if (player == null) return InteractionResult.PASS;

        String key = level.getBlockState(pos).getBlock().builtInRegistryHolder().key().location().toString();
        if (!key.equals("create:fluid_tank")) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        // 扫描垂直柱（BFS 找所有连接储罐）
        var allTanks = new java.util.HashSet<BlockPos>();
        var queue = new java.util.ArrayDeque<BlockPos>();
        queue.add(pos);
        while (!queue.isEmpty()) {
            BlockPos p = queue.poll();
            if (!allTanks.add(p)) continue;
            for (Direction d : Direction.values()) {
                BlockPos np = p.relative(d);
                if (!allTanks.contains(np) && level.getBlockState(np).getBlock()
                        .builtInRegistryHolder().key().location().toString().equals("create:fluid_tank"))
                    queue.add(np);
            }
        }
        // 需要至少 3 层
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        for (BlockPos tp : allTanks) { int y = tp.getY(); if (y < minY) minY = y; if (y > maxY) maxY = y; }
        int height = maxY - minY + 1;
        if (height < 3) {
            player.displayClientMessage(Component.literal("§c需要至少3层垂直堆叠的流体储罐！"), true);
            return InteractionResult.SUCCESS;
        }
        int countPerLayer = allTanks.size() / height; // 每层的方块数

        // 保存原始储罐类型（在转换之前！）
        String tankKey = level.getBlockState(pos).getBlock().builtInRegistryHolder().key().location().toString();

        // 转换为沸腾炉
        BlockPos masterPos = null;
        for (BlockPos tp : allTanks) {
            int layer = tp.getY() - minY;
            FluidizedBedBlock.Layer l = layer == 0 ? FluidizedBedBlock.Layer.BOTTOM :
                    (layer == height - 1 ? FluidizedBedBlock.Layer.TOP : FluidizedBedBlock.Layer.MIDDLE);
            level.setBlock(tp, ModBlocks.FLUIDIZED_BED.get().defaultBlockState()
                    .setValue(FluidizedBedBlock.LAYER, l), Block.UPDATE_ALL);
            if (l == FluidizedBedBlock.Layer.MIDDLE && masterPos == null) masterPos = tp;
        }
        // ⚡ 只有一个 master：第一个中层方块
        if (masterPos != null) {
            var be = level.getBlockEntity(masterPos);
            if (be instanceof com.user.chemical_industry.block_entity.FluidizedBedBlockEntity fbe) {
                fbe.setTankBlock(tankKey);            // 标记为 controller
                fbe.scheduleConnectivityUpdate();      // 触发 BFS 统一所有方块
            }
        }
        if (!player.isCreative()) ctx.getItemInHand().shrink(1);
        player.displayClientMessage(Component.literal("§a已转化 §f" + allTanks.size() + " §a个储罐为 §f" + height + " §a层沸腾炉"), true);
        return InteractionResult.SUCCESS;
    }
}
