package com.cxy.chemical_industry.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * 腐蚀状态效果 — 化学腐蚀，快速消耗护甲 + 扣血
 *
 * 【与原版中毒的区别】
 * - 中毒（Poison）：扣血，不伤护甲
 * - 腐蚀（Corrosion）：扣血 + 护甲耐久快速下降
 *
 * 【效果】
 * - 每 40 tick（2 秒）：扣 1 点生命值
 * - 每 20 tick（1 秒）：所有护甲耐久 -2
 * - 伤害不受难度影响
 */
public class CorrosionEffect extends MobEffect {

    public CorrosionEffect() {
        super(MobEffectCategory.HARMFUL, 0x4A7A3A); // 暗黄绿色，区别于中毒的绿色
    }

    @Override
    public boolean applyEffectTick(LivingEntity living, int amplifier) {
        // amplifier 0 = Lv1（普通酸碱）, amplifier 1+ = Lv2（硫酸）
        float hpDmg = amplifier >= 1 ? 2.0f : 1.0f;
        int armorDmg = amplifier >= 1 ? 4 : 2;
        living.hurt(living.damageSources().generic(), hpDmg);

        // 腐蚀所有护甲
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
                ItemStack armor = living.getItemBySlot(slot);
                if (!armor.isEmpty()) {
                    armor.hurtAndBreak(armorDmg, living, slot);
                }
            }
        }
        // 腐蚀双手物品
        var mainHand = living.getItemBySlot(EquipmentSlot.MAINHAND);
        if (!mainHand.isEmpty() && mainHand.isDamageableItem()) {
            mainHand.hurtAndBreak(armorDmg, living, EquipmentSlot.MAINHAND);
        }
        var offHand = living.getItemBySlot(EquipmentSlot.OFFHAND);
        if (!offHand.isEmpty() && offHand.isDamageableItem()) {
            offHand.hurtAndBreak(armorDmg, living, EquipmentSlot.OFFHAND);
        }

        return true;
    }

    /** 每 30 tick（1.5 秒）触发一次 */
    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        int interval = 30;
        return duration % interval == 0;
    }
}
