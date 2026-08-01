package com.cxy.chemical_industry.registry;

import com.cxy.chemical_industry.ChemicalIndustry;
import com.cxy.chemical_industry.item.GasCanisterItem;
import com.cxy.chemical_industry.item.TooltipBlockItem;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraft.world.item.BlockItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 模组物品注册表
 *
 * 根据构思文档，注册以下物品：
 * 【矿石掉落物】硫磺粉、黄铁原矿、氯化钠、硝酸钾
 * 【化工产品】氧化铁、氢氧化钠
 * 【气体瓶】氯气瓶、氢气瓶
 *
 * 后续阶段将添加：
 * - 硫酸、硝酸（流体，在流体系统中注册）
 * - 氢氧化钠溶液（流体）
 */
public class ModItems {

    private static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(ChemicalIndustry.MOD_ID);

    // ========== 矿石掉落物 ==========

    /** 硫磺粉 (S) — 从硫磺矿石挖掘得到 */
    public static final DeferredItem<Item> SULFUR_POWDER = ITEMS.register(
            "sulfur_powder",
            () -> new Item(new Item.Properties())
    );

    /** 黄铁原矿 (FeS₂) — 从黄铁矿石挖掘得到，需进一步加工 */
    public static final DeferredItem<Item> RAW_PYRITE = ITEMS.register(
            "raw_pyrite",
            () -> new Item(new Item.Properties())
    );

    /** 氯化钠 (NaCl) — 从岩盐矿石挖掘得到，就是食盐 */
    public static final DeferredItem<Item> SODIUM_CHLORIDE = ITEMS.register(
            "sodium_chloride",
            () -> new Item(new Item.Properties())
    );

    /** 硝酸钾 (KNO₃) — 从硝石矿石挖掘得到，化肥（同骨粉）和火药原料 */
    public static final DeferredItem<Item> POTASSIUM_NITRATE = ITEMS.register(
            "potassium_nitrate",
            () -> new net.minecraft.world.item.BoneMealItem(new Item.Properties())
    );

    // ========== 化工产品（固体） ==========

    /** 氧化铁 (Fe₂O₃) — 黄铁矿煅烧的副产物，重要工业原料 */
    public static final DeferredItem<Item> IRON_OXIDE = ITEMS.register(
            "iron_oxide",
            () -> new Item(new Item.Properties())
    );

    /** 氢氧化钠 (NaOH) — 强碱，电解氯化钠的产物之一 */
    public static final DeferredItem<Item> SODIUM_HYDROXIDE = ITEMS.register(
            "sodium_hydroxide",
            () -> new Item(new Item.Properties())
    );

    // ========== 铜化工产物 ==========

    /** 氧化铜 (CuO) — 粗铜煅烧得到，黑色粉末 */
    public static final DeferredItem<Item> COPPER_OXIDE = ITEMS.register(
            "copper_oxide", () -> new Item(new Item.Properties()));

    /** 硫酸铜 (CuSO₄) — 蓝色晶体，溶于水得到硫酸铜溶液 */
    public static final DeferredItem<Item> COPPER_SULFATE = ITEMS.register(
            "copper_sulfate", () -> new Item(new Item.Properties()));

    // ========== 新矿石掉落物 ==========

    /** 粗银 (Ag) — 银矿石的掉落物 */
    public static final DeferredItem<Item> RAW_SILVER = ITEMS.register(
            "raw_silver", () -> new Item(new Item.Properties()));
    /** 粗稀土 — 稀土矿石的掉落物 */
    public static final DeferredItem<Item> RAW_RARE_EARTH = ITEMS.register(
            "raw_rare_earth", () -> new Item(new Item.Properties()));

    // ========== 贵金属产物 ==========

    /** 银锭 (Ag) — 冶炼粗银得到 */
    public static final DeferredItem<Item> SILVER_INGOT = ITEMS.register(
            "silver_ingot", () -> new Item(new Item.Properties()));
    /** 稀土元素 — 从阳极泥中提取，用作催化剂 */
    public static final DeferredItem<Item> RARE_EARTH_ELEMENT = ITEMS.register(
            "rare_earth_element", () -> new Item(new Item.Properties()));
    /** 铂系精矿 — 从阳极泥中提取 */
    public static final DeferredItem<Item> PLATINUM_CONCENTRATE = ITEMS.register(
            "platinum_concentrate", () -> new Item(new Item.Properties()));

    // ========== 镁系列 ==========

    /** 镁锭 (Mg) — 熔融 MgCl₂ 电解产物，轻质活泼金属 */
    public static final DeferredItem<Item> MAGNESIUM_INGOT = ITEMS.register(
            "magnesium_ingot", () -> new Item(new Item.Properties()));
    /** 镁粒 (Mg) — 镁锭拆解 */
    public static final DeferredItem<Item> MAGNESIUM_NUGGET = ITEMS.register(
            "magnesium_nugget", () -> new Item(new Item.Properties()));
    /** 氧化镁 (MgO) — 镁/镁粒在沸腾炉煅烧得到，白色粉末 */
    public static final DeferredItem<Item> MAGNESIUM_OXIDE = ITEMS.register(
            "magnesium_oxide", () -> new Item(new Item.Properties()));
    /** 氯化镁 (MgCl₂) — 卤水蒸发产物之一，镁的来源 */
    public static final DeferredItem<Item> MAGNESIUM_CHLORIDE = ITEMS.register(
            "magnesium_chloride", () -> new Item(new Item.Properties()));

    // ========== 铝盐 ==========

    /** 氯化铝 (AlCl₃) — 铝或氧化铝与盐酸反应得到 */
    public static final DeferredItem<Item> ALUMINUM_CHLORIDE = ITEMS.register(
            "aluminum_chloride", () -> new Item(new Item.Properties()));

    // ========== 矿物加工产物 ==========

    /** 氧化钙 (CaO) — 石头高温煅烧产物，生石灰 */
    public static final DeferredItem<Item> CALCIUM_OXIDE = ITEMS.register(
            "calcium_oxide", () -> new Item(new Item.Properties()));
    /** 二氧化硅 (SiO₂) — 石头高温煅烧副产物，石英砂 */
    public static final DeferredItem<Item> SILICON_DIOXIDE = ITEMS.register(
            "silicon_dioxide", () -> new Item(new Item.Properties()));
    /** 硅 (Si) — SiO₂ 碳热还原产物，半导体原料 */
    public static final DeferredItem<Item> SILICON = ITEMS.register(
            "silicon", () -> new Item(new Item.Properties()));

    // ========== 盐类补充 ==========

    /** 铜粒 — 电解精炼铜的产物 */
    public static final DeferredItem<Item> COPPER_NUGGET = ITEMS.register(
            "copper_nugget", () -> new Item(new Item.Properties()));
    /** 碳酸钠 (Na₂CO₃) — NaOH + CO₂ 产物，纯碱/苏打 */
    public static final DeferredItem<Item> SODIUM_CARBONATE = ITEMS.register(
            "sodium_carbonate", () -> new Item(new Item.Properties()));

    // ========== 有机合成产物 ==========

    /** 酚醛树脂 — 苯酚 + 甲醛缩聚产物，最早的人工合成塑料 */
    public static final DeferredItem<Item> PHENOLIC_RESIN = ITEMS.register(
            "phenolic_resin", () -> new Item(new Item.Properties()));
    /** 苦味酸 — 苯酚硝化产物，烈性炸药 */
    public static final DeferredItem<Item> PICRIC_ACID = ITEMS.register(
            "picric_acid", () -> new Item(new Item.Properties()));

    // ========== 硬铝（Al+Mg 合金）==========
    public static final DeferredItem<Item> DURALUMIN_INGOT = ITEMS.register("duralumin_ingot", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> DURALUMIN_NUGGET = ITEMS.register("duralumin_nugget", () -> new Item(new Item.Properties()));
    public static final DeferredItem<SwordItem> DURALUMIN_SWORD = ITEMS.register("duralumin_sword",
            () -> new SwordItem(ModMaterials.DURALUMIN_TIER, new Item.Properties().attributes(SwordItem.createAttributes(ModMaterials.DURALUMIN_TIER, 3, -2.4f))));
    public static final DeferredItem<PickaxeItem> DURALUMIN_PICKAXE = ITEMS.register("duralumin_pickaxe",
            () -> new PickaxeItem(ModMaterials.DURALUMIN_TIER, new Item.Properties().attributes(PickaxeItem.createAttributes(ModMaterials.DURALUMIN_TIER, 1.0f, -2.8f))));
    public static final DeferredItem<AxeItem> DURALUMIN_AXE = ITEMS.register("duralumin_axe",
            () -> new AxeItem(ModMaterials.DURALUMIN_TIER, new Item.Properties().attributes(AxeItem.createAttributes(ModMaterials.DURALUMIN_TIER, 5.5f, -3.1f))));
    public static final DeferredItem<ShovelItem> DURALUMIN_SHOVEL = ITEMS.register("duralumin_shovel",
            () -> new ShovelItem(ModMaterials.DURALUMIN_TIER, new Item.Properties().attributes(ShovelItem.createAttributes(ModMaterials.DURALUMIN_TIER, 1.5f, -3.0f))));
    public static final DeferredItem<HoeItem> DURALUMIN_HOE = ITEMS.register("duralumin_hoe",
            () -> new HoeItem(ModMaterials.DURALUMIN_TIER, new Item.Properties().attributes(HoeItem.createAttributes(ModMaterials.DURALUMIN_TIER, -2.0f, -1.0f))));
    public static final DeferredItem<ArmorItem> DURALUMIN_HELMET = ITEMS.register("duralumin_helmet",
            () -> new ArmorItem(net.minecraft.core.Holder.direct(ModMaterials.DURALUMIN_ARMOR), ArmorItem.Type.HELMET, new Item.Properties().durability(220)));
    public static final DeferredItem<ArmorItem> DURALUMIN_CHESTPLATE = ITEMS.register("duralumin_chestplate",
            () -> new ArmorItem(net.minecraft.core.Holder.direct(ModMaterials.DURALUMIN_ARMOR), ArmorItem.Type.CHESTPLATE, new Item.Properties().durability(320)));
    public static final DeferredItem<ArmorItem> DURALUMIN_LEGGINGS = ITEMS.register("duralumin_leggings",
            () -> new ArmorItem(net.minecraft.core.Holder.direct(ModMaterials.DURALUMIN_ARMOR), ArmorItem.Type.LEGGINGS, new Item.Properties().durability(300)));
    public static final DeferredItem<ArmorItem> DURALUMIN_BOOTS = ITEMS.register("duralumin_boots",
            () -> new ArmorItem(net.minecraft.core.Holder.direct(ModMaterials.DURALUMIN_ARMOR), ArmorItem.Type.BOOTS, new Item.Properties().durability(260)));

    // ========== 铅/锡 ==========
    public static final DeferredItem<Item> LEAD_INGOT = ITEMS.register("lead_ingot", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> TIN_INGOT = ITEMS.register("tin_ingot", () -> new Item(new Item.Properties()));

    // ========== 青铜（Cu+Sn 合金）==========
    public static final DeferredItem<Item> BRONZE_INGOT = ITEMS.register("bronze_ingot", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> BRONZE_NUGGET = ITEMS.register("bronze_nugget", () -> new Item(new Item.Properties()));
    public static final DeferredItem<SwordItem> BRONZE_SWORD = ITEMS.register("bronze_sword",
            () -> new SwordItem(ModMaterials.BRONZE_TIER, new Item.Properties().attributes(SwordItem.createAttributes(ModMaterials.BRONZE_TIER, 3, -2.4f))));
    public static final DeferredItem<PickaxeItem> BRONZE_PICKAXE = ITEMS.register("bronze_pickaxe",
            () -> new PickaxeItem(ModMaterials.BRONZE_TIER, new Item.Properties().attributes(PickaxeItem.createAttributes(ModMaterials.BRONZE_TIER, 1.0f, -2.8f))));
    public static final DeferredItem<AxeItem> BRONZE_AXE = ITEMS.register("bronze_axe",
            () -> new AxeItem(ModMaterials.BRONZE_TIER, new Item.Properties().attributes(AxeItem.createAttributes(ModMaterials.BRONZE_TIER, 5.5f, -3.1f))));
    public static final DeferredItem<ShovelItem> BRONZE_SHOVEL = ITEMS.register("bronze_shovel",
            () -> new ShovelItem(ModMaterials.BRONZE_TIER, new Item.Properties().attributes(ShovelItem.createAttributes(ModMaterials.BRONZE_TIER, 1.5f, -3.0f))));
    public static final DeferredItem<HoeItem> BRONZE_HOE = ITEMS.register("bronze_hoe",
            () -> new HoeItem(ModMaterials.BRONZE_TIER, new Item.Properties().attributes(HoeItem.createAttributes(ModMaterials.BRONZE_TIER, -2.0f, -1.0f))));
    public static final DeferredItem<ArmorItem> BRONZE_HELMET = ITEMS.register("bronze_helmet",
            () -> new ArmorItem(net.minecraft.core.Holder.direct(ModMaterials.BRONZE_ARMOR), ArmorItem.Type.HELMET, new Item.Properties().durability(165)));
    public static final DeferredItem<ArmorItem> BRONZE_CHESTPLATE = ITEMS.register("bronze_chestplate",
            () -> new ArmorItem(net.minecraft.core.Holder.direct(ModMaterials.BRONZE_ARMOR), ArmorItem.Type.CHESTPLATE, new Item.Properties().durability(240)));
    public static final DeferredItem<ArmorItem> BRONZE_LEGGINGS = ITEMS.register("bronze_leggings",
            () -> new ArmorItem(net.minecraft.core.Holder.direct(ModMaterials.BRONZE_ARMOR), ArmorItem.Type.LEGGINGS, new Item.Properties().durability(225)));
    public static final DeferredItem<ArmorItem> BRONZE_BOOTS = ITEMS.register("bronze_boots",
            () -> new ArmorItem(net.minecraft.core.Holder.direct(ModMaterials.BRONZE_ARMOR), ArmorItem.Type.BOOTS, new Item.Properties().durability(195)));

    // ========== 冷凝管 ==========
    public static final DeferredItem<BlockItem> CONDENSER_PIPE = ITEMS.register("condenser_pipe",
            () -> new BlockItem(ModBlocks.CONDENSER_PIPE.get(), new Item.Properties()));

    // ========== 新矿石 + 金属块 BlockItem ==========
    public static final DeferredItem<BlockItem> GALENA_ORE = ITEMS.register("galena_ore",
            () -> new TooltipBlockItem(ModBlocks.GALENA_ORE.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> DEEPSLATE_GALENA_ORE = ITEMS.register("deepslate_galena_ore",
            () -> new TooltipBlockItem(ModBlocks.DEEPSLATE_GALENA_ORE.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> CASSITERITE_ORE = ITEMS.register("cassiterite_ore",
            () -> new TooltipBlockItem(ModBlocks.CASSITERITE_ORE.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> DEEPSLATE_CASSITERITE_ORE = ITEMS.register("deepslate_cassiterite_ore",
            () -> new TooltipBlockItem(ModBlocks.DEEPSLATE_CASSITERITE_ORE.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> CINNABAR_ORE = ITEMS.register("cinnabar_ore",
            () -> new TooltipBlockItem(ModBlocks.CINNABAR_ORE.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> DEEPSLATE_CINNABAR_ORE = ITEMS.register("deepslate_cinnabar_ore",
            () -> new TooltipBlockItem(ModBlocks.DEEPSLATE_CINNABAR_ORE.get(), new Item.Properties()));
    /** 朱砂 (HgS) — 朱砂矿石的挖掘掉落物，沸腾炉加热得到水银 */
    public static final DeferredItem<Item> CINNABAR = ITEMS.register("cinnabar",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<BlockItem> LEAD_BLOCK = ITEMS.register("lead_block",
            () -> new TooltipBlockItem(ModBlocks.LEAD_BLOCK.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> TIN_BLOCK = ITEMS.register("tin_block",
            () -> new TooltipBlockItem(ModBlocks.TIN_BLOCK.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> BRONZE_BLOCK = ITEMS.register("bronze_block",
            () -> new TooltipBlockItem(ModBlocks.BRONZE_BLOCK.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> SILVER_BLOCK = ITEMS.register("silver_block",
            () -> new TooltipBlockItem(ModBlocks.SILVER_BLOCK.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> DURALUMIN_BLOCK = ITEMS.register("duralumin_block",
            () -> new TooltipBlockItem(ModBlocks.DURALUMIN_BLOCK.get(), new Item.Properties()));

    // ========== 有机化工（乙醛、乙酸也以流体形式存在于 ModFluids 中）==========
    // 注：乙醇由柴油动力(DG)提供，本模组不重复注册

    public static final DeferredItem<Item> UREA = ITEMS.register(
            "urea", () -> new net.minecraft.world.item.BoneMealItem(new Item.Properties()));  // 尿素 CO(NH₂)₂ — 氮肥
    public static final DeferredItem<Item> METHANOL = ITEMS.register(
            "methanol", () -> new Item(new Item.Properties()));        // 甲醇 CH₃OH
    public static final DeferredItem<Item> FORMALDEHYDE = ITEMS.register(
            "formaldehyde", () -> new Item(new Item.Properties()));    // 甲醛 CH₂O
    // 乙醛和乙酸只有流体版（ModFluids），不注册物品
    public static final DeferredItem<Item> BENZENE = ITEMS.register(
            "benzene", () -> new Item(new Item.Properties()));         // 苯 C₆H₆
    public static final DeferredItem<Item> PHENOL = ITEMS.register(
            "phenol", () -> new Item(new Item.Properties()));          // 苯酚 C₆H₅OH
    public static final DeferredItem<Item> ACETONE = ITEMS.register(
            "acetone", () -> new Item(new Item.Properties()));         // 丙酮 CH₃COCH₃
    public static final DeferredItem<Item> METHANE = ITEMS.register(
            "methane", () -> new Item(new Item.Properties()));         // 甲烷 CH₄


    // ========== 罂粟系列 ==========

    /**
     * 罂粟果实 — 种子（种耕地）+ 食物（15 秒正面 → 240 秒负面）。
     * 正面：伤害吸收 II / 生命恢复 II / 抗性提升 II（300 tick = 15 秒）
     */
    public static final DeferredItem<com.cxy.chemical_industry.item.OpiumPoppyFruitItem> OPIUM_POPPY_FRUIT = ITEMS.register(
            "opium_poppy_fruit",
            () -> new com.cxy.chemical_industry.item.OpiumPoppyFruitItem(new Item.Properties()
                    .food(new FoodProperties.Builder()
                            .nutrition(2).saturationModifier(0.2f)   // 能恢复一点饥饿
                            .effect(() -> new net.minecraft.world.effect.MobEffectInstance(
                                    net.minecraft.world.effect.MobEffects.ABSORPTION, 300, 1), 1.0f)        // 伤害吸收 II
                            .effect(() -> new net.minecraft.world.effect.MobEffectInstance(
                                    net.minecraft.world.effect.MobEffects.REGENERATION, 300, 1), 1.0f)      // 生命恢复 II
                            .effect(() -> new net.minecraft.world.effect.MobEffectInstance(
                                    net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 300, 1), 1.0f) // 抗性提升 II
                            .effect(() -> new net.minecraft.world.effect.MobEffectInstance(
                                    net.minecraft.world.effect.MobEffects.DAMAGE_BOOST, 300, 1), 1.0f)       // 力量 II
                            .effect(() -> new net.minecraft.world.effect.MobEffectInstance(
                                    net.minecraft.world.effect.MobEffects.DIG_SPEED, 300, 1), 1.0f)         // 急迫 II
                            .effect(() -> new net.minecraft.world.effect.MobEffectInstance(
                                    net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, 300, 1), 1.0f)    // 速度 II
                            .effect(() -> new net.minecraft.world.effect.MobEffectInstance(
                                    net.minecraft.world.effect.MobEffects.JUMP, 300, 1), 1.0f)              // 跳跃提升 II
                            .alwaysEdible()
                            .build())));

    /**
     * 鸦片 — 罂粟果实 + 水加热搅拌的浓缩产物。
     * 25 秒正面（吸收/恢复/抗性 II）→ 300 秒负面（7 种 I 级），
     * 效果比果实强、副作用也更久
     */
    public static final DeferredItem<com.cxy.chemical_industry.item.OpiumItem> OPIUM = ITEMS.register(
            "opium",
            () -> new com.cxy.chemical_industry.item.OpiumItem(new Item.Properties()
                    .food(new FoodProperties.Builder()
                            .nutrition(1).saturationModifier(0.1f)
                            .effect(() -> new net.minecraft.world.effect.MobEffectInstance(
                                    net.minecraft.world.effect.MobEffects.ABSORPTION, 500, 1), 1.0f)        // 伤害吸收 II
                            .effect(() -> new net.minecraft.world.effect.MobEffectInstance(
                                    net.minecraft.world.effect.MobEffects.REGENERATION, 500, 1), 1.0f)      // 生命恢复 II
                            .effect(() -> new net.minecraft.world.effect.MobEffectInstance(
                                    net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 500, 1), 1.0f) // 抗性提升 II
                            .effect(() -> new net.minecraft.world.effect.MobEffectInstance(
                                    net.minecraft.world.effect.MobEffects.DAMAGE_BOOST, 500, 1), 1.0f)       // 力量 II
                            .effect(() -> new net.minecraft.world.effect.MobEffectInstance(
                                    net.minecraft.world.effect.MobEffects.DIG_SPEED, 500, 1), 1.0f)         // 急迫 II
                            .effect(() -> new net.minecraft.world.effect.MobEffectInstance(
                                    net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, 500, 1), 1.0f)    // 速度 II
                            .effect(() -> new net.minecraft.world.effect.MobEffectInstance(
                                    net.minecraft.world.effect.MobEffects.JUMP, 500, 1), 1.0f)              // 跳跃提升 II
                            .alwaysEdible()
                            .build()),
                    500,   // 潜伏 500 tick = 25 秒
                    6000)  // 副作用 6000 tick = 300 秒
    );

    // ========== 机器耗材 ==========

    /** 阳离子交换膜 — 电解槽必备耗材，允许阳离子通过而阻挡阴离子 */
    public static final DeferredItem<Item> CATION_EXCHANGE_MEMBRANE = ITEMS.register(
            "cation_exchange_membrane",
            () -> new Item(new Item.Properties().stacksTo(16))
    );

    /** 滤网 — 电解槽产出固体产物时必备，配合漏斗/溜槽使用 */
    public static final DeferredItem<Item> FILTER_MESH = ITEMS.register(
            "filter_mesh",
            () -> new Item(new Item.Properties().stacksTo(16))
    );

    /** 阳极泥 — 电解精炼铜的副产物，含贵金属，后续可提取金银 */
    public static final DeferredItem<Item> ANODE_SLIME = ITEMS.register(
            "anode_slime",
            () -> new Item(new Item.Properties())
    );

    // ========== 储气罐 ==========

    public static final DeferredItem<GasCanisterItem> GAS_CANISTER = ITEMS.register(
            "gas_canister",
            GasCanisterItem::new
    );

    // ========== 方块物品 (BlockItem) — 矿石类（带 Shift 提示）==========

    public static final DeferredItem<BlockItem> SULFUR_ORE = ITEMS.register(
            "sulfur_ore",
            () -> new TooltipBlockItem(ModBlocks.SULFUR_ORE.get(), new Item.Properties())
    );

    public static final DeferredItem<BlockItem> PYRITE_ORE = ITEMS.register(
            "pyrite_ore",
            () -> new TooltipBlockItem(ModBlocks.PYRITE_ORE.get(), new Item.Properties())
    );

    public static final DeferredItem<BlockItem> ROCK_SALT_ORE = ITEMS.register(
            "rock_salt_ore",
            () -> new TooltipBlockItem(ModBlocks.ROCK_SALT_ORE.get(), new Item.Properties())
    );

    public static final DeferredItem<BlockItem> NITER_ORE = ITEMS.register(
            "niter_ore",
            () -> new TooltipBlockItem(ModBlocks.NITER_ORE.get(), new Item.Properties())
    );

    public static final DeferredItem<BlockItem> SILVER_ORE = ITEMS.register(
            "silver_ore",
            () -> new TooltipBlockItem(ModBlocks.SILVER_ORE.get(), new Item.Properties())
    );
    public static final DeferredItem<BlockItem> RARE_EARTH_ORE = ITEMS.register(
            "rare_earth_ore",
            () -> new TooltipBlockItem(ModBlocks.RARE_EARTH_ORE.get(), new Item.Properties())
    );
    public static final DeferredItem<BlockItem> BAUXITE_ORE = ITEMS.register(
            "bauxite_ore", () -> new TooltipBlockItem(ModBlocks.BAUXITE_ORE.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> GRAPHITE_ORE = ITEMS.register(
            "graphite_ore", () -> new TooltipBlockItem(ModBlocks.GRAPHITE_ORE.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> ALUMINUM_BLOCK = ITEMS.register(
            "aluminum_block", () -> new TooltipBlockItem(ModBlocks.ALUMINUM_BLOCK.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> STEEL_BLOCK = ITEMS.register(
            "steel_block", () -> new TooltipBlockItem(ModBlocks.STEEL_BLOCK.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> MAGNESIUM_BLOCK = ITEMS.register(
            "magnesium_block", () -> new TooltipBlockItem(ModBlocks.MAGNESIUM_BLOCK.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> GRAPHITE_BLOCK = ITEMS.register(
            "graphite_block", () -> new TooltipBlockItem(ModBlocks.GRAPHITE_BLOCK.get(), new Item.Properties()) {
                // 石墨块 = 9×石墨 = 9×1600 tick 燃料（等价煤炭块）
                @Override public int getBurnTime(net.minecraft.world.item.ItemStack stack,
                                                 net.minecraft.world.item.crafting.RecipeType<?> recipeType) { return 14400; }
            });
    public static final DeferredItem<BlockItem> DEEPSLATE_SULFUR_ORE = ITEMS.register(
            "deepslate_sulfur_ore", () -> new TooltipBlockItem(ModBlocks.DEEPSLATE_SULFUR_ORE.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> DEEPSLATE_PYRITE_ORE = ITEMS.register(
            "deepslate_pyrite_ore", () -> new TooltipBlockItem(ModBlocks.DEEPSLATE_PYRITE_ORE.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> DEEPSLATE_ROCK_SALT_ORE = ITEMS.register(
            "deepslate_rock_salt_ore", () -> new TooltipBlockItem(ModBlocks.DEEPSLATE_ROCK_SALT_ORE.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> DEEPSLATE_NITER_ORE = ITEMS.register(
            "deepslate_niter_ore", () -> new TooltipBlockItem(ModBlocks.DEEPSLATE_NITER_ORE.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> DEEPSLATE_SILVER_ORE = ITEMS.register(
            "deepslate_silver_ore", () -> new TooltipBlockItem(ModBlocks.DEEPSLATE_SILVER_ORE.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> DEEPSLATE_RARE_EARTH_ORE = ITEMS.register(
            "deepslate_rare_earth_ore", () -> new TooltipBlockItem(ModBlocks.DEEPSLATE_RARE_EARTH_ORE.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> DEEPSLATE_BAUXITE_ORE = ITEMS.register(
            "deepslate_bauxite_ore", () -> new TooltipBlockItem(ModBlocks.DEEPSLATE_BAUXITE_ORE.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> DEEPSLATE_GRAPHITE_ORE = ITEMS.register(
            "deepslate_graphite_ore", () -> new TooltipBlockItem(ModBlocks.DEEPSLATE_GRAPHITE_ORE.get(), new Item.Properties()));

    // ========== 铝/钢/石墨材料 ==========
    public static final DeferredItem<Item> BAUXITE = ITEMS.register("bauxite", () -> new Item(new Item.Properties()));
    /** 石墨 — 完全等价煤炭：可燃（1600 tick = 8 个物品） */
    public static final DeferredItem<Item> GRAPHITE = ITEMS.register("graphite", () -> new Item(new Item.Properties()) {
        // 覆写 NeoForge 的燃烧时间钩子（1.21.1 没有 minecraft:fuel 配方类型，必须用代码注册）
        @Override public int getBurnTime(net.minecraft.world.item.ItemStack stack,
                                         net.minecraft.world.item.crafting.RecipeType<?> recipeType) { return 1600; }
    });
    public static final DeferredItem<Item> GRAPHITE_ROD = ITEMS.register("graphite_rod", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> CRYOLITE = ITEMS.register("cryolite", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ALUMINA = ITEMS.register("alumina", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ALUMINUM_INGOT = ITEMS.register("aluminum_ingot", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ALUMINUM_NUGGET = ITEMS.register("aluminum_nugget", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> STEEL_INGOT = ITEMS.register("steel_ingot", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> STEEL_NUGGET = ITEMS.register("steel_nugget", () -> new Item(new Item.Properties()));

    // ========== 盐类产物 ==========
    /** 硫酸钠 (Na₂SO₄) — 硫酸与氢氧化钠中和的产物 */
    public static final DeferredItem<Item> SODIUM_SULFATE = ITEMS.register("sodium_sulfate", () -> new Item(new Item.Properties()));
    /** 硝酸钠 (NaNO₃) — 硝酸与氢氧化钠中和的产物 */
    public static final DeferredItem<Item> SODIUM_NITRATE = ITEMS.register("sodium_nitrate", () -> new Item(new Item.Properties()));

    /** 钠锭 (Na) — 熔融 NaCl 电解产物 */
    public static final DeferredItem<Item> SODIUM_INGOT = ITEMS.register("sodium_ingot", () -> new Item(new Item.Properties()));

    // ========== 钢制工具 ==========
    public static final DeferredItem<SwordItem> STEEL_SWORD = ITEMS.register("steel_sword",
            () -> new SwordItem(ModMaterials.STEEL_TIER, new Item.Properties().attributes(SwordItem.createAttributes(ModMaterials.STEEL_TIER, 3, -2.4f))));
    public static final DeferredItem<PickaxeItem> STEEL_PICKAXE = ITEMS.register("steel_pickaxe",
            () -> new PickaxeItem(ModMaterials.STEEL_TIER, new Item.Properties().attributes(PickaxeItem.createAttributes(ModMaterials.STEEL_TIER, 1.0f, -2.8f))));
    public static final DeferredItem<AxeItem> STEEL_AXE = ITEMS.register("steel_axe",
            () -> new AxeItem(ModMaterials.STEEL_TIER, new Item.Properties().attributes(AxeItem.createAttributes(ModMaterials.STEEL_TIER, 5.5f, -3.1f))));
    public static final DeferredItem<ShovelItem> STEEL_SHOVEL = ITEMS.register("steel_shovel",
            () -> new ShovelItem(ModMaterials.STEEL_TIER, new Item.Properties().attributes(ShovelItem.createAttributes(ModMaterials.STEEL_TIER, 1.5f, -3.0f))));
    public static final DeferredItem<HoeItem> STEEL_HOE = ITEMS.register("steel_hoe",
            () -> new HoeItem(ModMaterials.STEEL_TIER, new Item.Properties().attributes(HoeItem.createAttributes(ModMaterials.STEEL_TIER, -2.0f, -1.0f))));

    // ========== 钢制盔甲 ==========
    public static final DeferredItem<ArmorItem> STEEL_HELMET = ITEMS.register("steel_helmet",
            () -> new ArmorItem(net.minecraft.core.Holder.direct(ModMaterials.STEEL_ARMOR), ArmorItem.Type.HELMET, new Item.Properties().durability(275)));
    public static final DeferredItem<ArmorItem> STEEL_CHESTPLATE = ITEMS.register("steel_chestplate",
            () -> new ArmorItem(net.minecraft.core.Holder.direct(ModMaterials.STEEL_ARMOR), ArmorItem.Type.CHESTPLATE, new Item.Properties().durability(400)));
    public static final DeferredItem<ArmorItem> STEEL_LEGGINGS = ITEMS.register("steel_leggings",
            () -> new ArmorItem(net.minecraft.core.Holder.direct(ModMaterials.STEEL_ARMOR), ArmorItem.Type.LEGGINGS, new Item.Properties().durability(375)));
    public static final DeferredItem<ArmorItem> STEEL_BOOTS = ITEMS.register("steel_boots",
            () -> new ArmorItem(net.minecraft.core.Holder.direct(ModMaterials.STEEL_ARMOR), ArmorItem.Type.BOOTS, new Item.Properties().durability(325)));

    // ========== 铝制工具 ==========
    public static final DeferredItem<SwordItem> ALUMINUM_SWORD = ITEMS.register("aluminum_sword",
            () -> new SwordItem(ModMaterials.ALUMINUM_TIER, new Item.Properties().attributes(SwordItem.createAttributes(ModMaterials.ALUMINUM_TIER, 3, -2.4f))));
    public static final DeferredItem<PickaxeItem> ALUMINUM_PICKAXE = ITEMS.register("aluminum_pickaxe",
            () -> new PickaxeItem(ModMaterials.ALUMINUM_TIER, new Item.Properties().attributes(PickaxeItem.createAttributes(ModMaterials.ALUMINUM_TIER, 1.0f, -2.8f))));
    public static final DeferredItem<AxeItem> ALUMINUM_AXE = ITEMS.register("aluminum_axe",
            () -> new AxeItem(ModMaterials.ALUMINUM_TIER, new Item.Properties().attributes(AxeItem.createAttributes(ModMaterials.ALUMINUM_TIER, 5.5f, -3.1f))));
    public static final DeferredItem<ShovelItem> ALUMINUM_SHOVEL = ITEMS.register("aluminum_shovel",
            () -> new ShovelItem(ModMaterials.ALUMINUM_TIER, new Item.Properties().attributes(ShovelItem.createAttributes(ModMaterials.ALUMINUM_TIER, 1.5f, -3.0f))));
    public static final DeferredItem<HoeItem> ALUMINUM_HOE = ITEMS.register("aluminum_hoe",
            () -> new HoeItem(ModMaterials.ALUMINUM_TIER, new Item.Properties().attributes(HoeItem.createAttributes(ModMaterials.ALUMINUM_TIER, -2.0f, -1.0f))));

    // ========== 铝制盔甲 ==========
    public static final DeferredItem<ArmorItem> ALUMINUM_HELMET = ITEMS.register("aluminum_helmet",
            () -> new ArmorItem(net.minecraft.core.Holder.direct(ModMaterials.ALUMINUM_ARMOR), ArmorItem.Type.HELMET, new Item.Properties().durability(132)));
    public static final DeferredItem<ArmorItem> ALUMINUM_CHESTPLATE = ITEMS.register("aluminum_chestplate",
            () -> new ArmorItem(net.minecraft.core.Holder.direct(ModMaterials.ALUMINUM_ARMOR), ArmorItem.Type.CHESTPLATE, new Item.Properties().durability(192)));
    public static final DeferredItem<ArmorItem> ALUMINUM_LEGGINGS = ITEMS.register("aluminum_leggings",
            () -> new ArmorItem(net.minecraft.core.Holder.direct(ModMaterials.ALUMINUM_ARMOR), ArmorItem.Type.LEGGINGS, new Item.Properties().durability(180)));
    public static final DeferredItem<ArmorItem> ALUMINUM_BOOTS = ITEMS.register("aluminum_boots",
            () -> new ArmorItem(net.minecraft.core.Holder.direct(ModMaterials.ALUMINUM_ARMOR), ArmorItem.Type.BOOTS, new Item.Properties().durability(156)));

    // ========== 功能性方块物品（带 Shift 提示）==========

    /** 沸腾炉控制器 — 右键 Create 流体储罐绑定，右键空气打开 GUI */
    public static final DeferredItem<com.cxy.chemical_industry.item.FluidizedBedControllerItem> FLUIDIZED_BED_CONTROLLER = ITEMS.register(
            "fluidized_bed_controller",
            com.cxy.chemical_industry.item.FluidizedBedControllerItem::new
    );
    /** 电解槽 */
    public static final DeferredItem<BlockItem> ELECTROLYZER = ITEMS.register(
            "electrolyzer",
            () -> new TooltipBlockItem(ModBlocks.ELECTROLYZER.get(), new Item.Properties())
    );

    /** 空气压缩机 */
    public static final DeferredItem<BlockItem> AIR_COMPRESSOR = ITEMS.register(
            "air_compressor",
            () -> new TooltipBlockItem(ModBlocks.AIR_COMPRESSOR.get(), new Item.Properties())
    );

    /**
     * 向 NeoForge 事件总线注册所有物品
     */
    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
