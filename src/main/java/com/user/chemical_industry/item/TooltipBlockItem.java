package com.user.chemical_industry.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;

/**
 * 带 Shift 提示的方块物品
 *
 * 模仿 Create 的提示风格：
 * - 不按 Shift：显示"按住 Shift 查看详情"
 * - 按住 Shift：显示该方块的详细使用说明
 *
 * 提示文本通过语言文件配置，key 格式为：
 * - 共享提示：tooltip.chemical_industry.shift
 * - 详情文本：tooltip.chemical_industry.<方块注册名>.detail
 */
public class TooltipBlockItem extends BlockItem {

    /**
     * @param block 对应的方块
     * @param properties 物品属性（堆叠数等）
     */
    public TooltipBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        // 根据是否按下 Shift 显示不同级别的提示
        if (hasShiftDown()) {
            // 按住 Shift → 显示详细说明
            // 每个方块的详情文本 key 不同，由子类或调用方负责在语言文件中定义
            tooltip.add(Component.translatable(
                    this.getDescriptionId() + ".detail"));
        } else {
            // 没按 Shift → 显示简短的"按 Shift 查看"提示
            addShiftHint(tooltip);
        }
    }

    /** 添加"按住 Shift 查看详情"的提示行 */
    public static void addShiftHint(List<Component> tooltip) {
        tooltip.add(Component.translatable("tooltip.chemical_industry.shift"));
    }

    /** 判断玩家是否按下了 Shift 键 */
    public static boolean hasShiftDown() {
        return net.minecraft.client.gui.screens.Screen.hasShiftDown();
    }
}
