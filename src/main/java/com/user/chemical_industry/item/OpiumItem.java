package com.user.chemical_industry.item;

import com.user.chemical_industry.event.OpiumHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 鸦片类物品基类
 *
 * 食用机制（两段式）：
 * 1. 立即：由 FoodProperties 施加正面效果（伤害吸收 II / 生命恢复 II / 抗性提升 II）
 * 2. 潜伏期结束后：由 OpiumHandler 施加 7 种负面效果（中毒/凋零/反胃/缓慢/饥饿/挖掘疲劳/虚弱）
 *
 * 罂粟果实（低配）和鸦片（浓缩）共用此类，只是时长不同。
 */
public class OpiumItem extends Item {

    /** 潜伏期（tick）：正面效果结束后，隔多久触发副作用 */
    private final int latencyTicks;
    /** 副作用时长（tick）：负面效果持续多久 */
    private final int sideEffectTicks;

    public OpiumItem(Properties p, int latencyTicks, int sideEffectTicks) {
        super(p);
        this.latencyTicks = latencyTicks;
        this.sideEffectTicks = sideEffectTicks;
    }

    /** 吃完后：记录潜伏倒计时（倒计时在服务端 OpiumHandler 里走） */
    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        super.finishUsingItem(stack, level, entity);
        if (!level.isClientSide() && entity instanceof Player player) {
            OpiumHandler.armLatency(player, latencyTicks, sideEffectTicks);
        }
        return stack;
    }
}
