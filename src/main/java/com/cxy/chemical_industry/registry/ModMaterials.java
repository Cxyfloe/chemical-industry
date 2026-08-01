package com.cxy.chemical_industry.registry;

import com.cxy.chemical_industry.ChemicalIndustry;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;

import java.util.*;

/**
 * 自定义工具等级和盔甲材料
 *
 * 钢：耐久750/速度7.5/伤害+3，盔甲防3-6-5-3 韧性1.5
 * 铝：耐久200/速度5.5/伤害+1.5，盔甲防1-3-2-1 韧性0
 */
public class ModMaterials {

    /** 钢工具等级 */
    public static final Tier STEEL_TIER = new Tier() {
        @Override public int getUses() { return 750; }
        @Override public float getSpeed() { return 7.5f; }
        @Override public float getAttackDamageBonus() { return 3.0f; }
        @Override public int getEnchantmentValue() { return 12; }
        @Override public TagKey<Block> getIncorrectBlocksForDrops() { return BlockTags.INCORRECT_FOR_DIAMOND_TOOL; }
        @Override public Ingredient getRepairIngredient() { return Ingredient.of(ModItems.STEEL_INGOT.get()); }
    };

    /** 铝工具等级 */
    public static final Tier ALUMINUM_TIER = new Tier() {
        @Override public int getUses() { return 200; }
        @Override public float getSpeed() { return 5.5f; }
        @Override public float getAttackDamageBonus() { return 1.5f; }
        @Override public int getEnchantmentValue() { return 18; }
        @Override public TagKey<Block> getIncorrectBlocksForDrops() { return BlockTags.INCORRECT_FOR_IRON_TOOL; }
        @Override public Ingredient getRepairIngredient() { return Ingredient.of(ModItems.ALUMINUM_INGOT.get()); }
    };

    private static final List<ArmorMaterial.Layer> STEEL_LAYERS = List.of(
            new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(ChemicalIndustry.MOD_ID, "steel")));
    private static final List<ArmorMaterial.Layer> ALUMINUM_LAYERS = List.of(
            new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(ChemicalIndustry.MOD_ID, "aluminum")));

    /** 钢盔甲 */
    public static final ArmorMaterial STEEL_ARMOR = new ArmorMaterial(
            Map.of(ArmorItem.Type.HELMET, 3, ArmorItem.Type.CHESTPLATE, 6,
                    ArmorItem.Type.LEGGINGS, 5, ArmorItem.Type.BOOTS, 3, ArmorItem.Type.BODY, 5),
            12, SoundEvents.ARMOR_EQUIP_IRON,
            () -> Ingredient.of(ModItems.STEEL_INGOT.get()),
            STEEL_LAYERS, 1.5f, 0.0f);

    /** 铝盔甲 */
    public static final ArmorMaterial ALUMINUM_ARMOR = new ArmorMaterial(
            Map.of(ArmorItem.Type.HELMET, 1, ArmorItem.Type.CHESTPLATE, 3,
                    ArmorItem.Type.LEGGINGS, 2, ArmorItem.Type.BOOTS, 1, ArmorItem.Type.BODY, 3),
            18, SoundEvents.ARMOR_EQUIP_CHAIN,
            () -> Ingredient.of(ModItems.ALUMINUM_INGOT.get()),
            ALUMINUM_LAYERS, 0.0f, 0.0f);

    // ========== 硬铝 ==========

    /** 硬铝工具等级 — Al+Mg 合金，介于钢和铝之间 */
    public static final Tier DURALUMIN_TIER = new Tier() {
        @Override public int getUses() { return 500; }
        @Override public float getSpeed() { return 6.5f; }
        @Override public float getAttackDamageBonus() { return 2.5f; }
        @Override public int getEnchantmentValue() { return 14; }
        @Override public TagKey<Block> getIncorrectBlocksForDrops() { return BlockTags.INCORRECT_FOR_IRON_TOOL; }
        @Override public Ingredient getRepairIngredient() { return Ingredient.of(ModItems.DURALUMIN_INGOT.get()); }
    };

    private static final List<ArmorMaterial.Layer> DURALUMIN_LAYERS = List.of(
            new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(ChemicalIndustry.MOD_ID, "duralumin")));

    public static final ArmorMaterial DURALUMIN_ARMOR = new ArmorMaterial(
            Map.of(ArmorItem.Type.HELMET, 2, ArmorItem.Type.CHESTPLATE, 5,
                    ArmorItem.Type.LEGGINGS, 4, ArmorItem.Type.BOOTS, 2, ArmorItem.Type.BODY, 4),
            14, SoundEvents.ARMOR_EQUIP_IRON,
            () -> Ingredient.of(ModItems.DURALUMIN_INGOT.get()),
            DURALUMIN_LAYERS, 1.0f, 0.0f);

    // ========== 青铜 ==========

    /** 青铜工具等级 — Cu+Sn 合金，介于石和铁之间 */
    public static final Tier BRONZE_TIER = new Tier() {
        @Override public int getUses() { return 200; }
        @Override public float getSpeed() { return 5.5f; }
        @Override public float getAttackDamageBonus() { return 2.0f; }
        @Override public int getEnchantmentValue() { return 15; }
        @Override public TagKey<Block> getIncorrectBlocksForDrops() { return BlockTags.INCORRECT_FOR_IRON_TOOL; }
        @Override public Ingredient getRepairIngredient() { return Ingredient.of(ModItems.BRONZE_INGOT.get()); }
    };

    private static final List<ArmorMaterial.Layer> BRONZE_LAYERS = List.of(
            new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(ChemicalIndustry.MOD_ID, "bronze")));

    /** 青铜盔甲 — 低于铁（铁=2-5-4-2），耐久偏低 */
    public static final ArmorMaterial BRONZE_ARMOR = new ArmorMaterial(
            Map.of(ArmorItem.Type.HELMET, 2, ArmorItem.Type.CHESTPLATE, 4,
                    ArmorItem.Type.LEGGINGS, 3, ArmorItem.Type.BOOTS, 1, ArmorItem.Type.BODY, 3),
            15, SoundEvents.ARMOR_EQUIP_CHAIN,
            () -> Ingredient.of(ModItems.BRONZE_INGOT.get()),
            BRONZE_LAYERS, 0.0f, 0.0f);
}
