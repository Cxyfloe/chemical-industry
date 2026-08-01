package com.cxy.chemical_industry.registry;

import com.cxy.chemical_industry.ChemicalIndustry;
import com.cxy.chemical_industry.block.ChemicalLiquidBlock;
import com.cxy.chemical_industry.fluid.ChemicalFluid;
import com.cxy.chemical_industry.fluid.ChemicalFluidType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/**
 * 模组流体注册表
 *
 * 【本模组的化学流体】
 * - 氯化钠溶液 (NaCl Solution) — 电解原料
 * - 硫酸 (Sulfuric Acid)        — 煅烧产物
 * - 氢氧化钠溶液 (NaOH Solution) — 电解产物
 *
 * 【与其他模组的兼容性】
 * 使用 NeoForge 标准流体注册，自动兼容 Create 管道系统。
 * Create 管道可以像传输原版水/岩浆一样传输这些化学流体。
 */
public class ModFluids {

    // ---------- 注册器 ----------

    /** 流体类型注册器（物理属性：密度、黏度等） */
    private static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, ChemicalIndustry.MOD_ID);

    /** 流体注册器（源 + 流动） */
    private static final DeferredRegister<net.minecraft.world.level.material.Fluid> FLUIDS =
            DeferredRegister.create(BuiltInRegistries.FLUID, ChemicalIndustry.MOD_ID);

    /** 流体方块注册器 */
    private static final DeferredRegister<Block> FLUID_BLOCKS =
            DeferredRegister.create(BuiltInRegistries.BLOCK, ChemicalIndustry.MOD_ID);

    /** 桶物品注册器 */
    private static final DeferredRegister<Item> FLUID_BUCKETS =
            DeferredRegister.create(BuiltInRegistries.ITEM, ChemicalIndustry.MOD_ID);

    // ---------- 氯化钠溶液 ----------

    public static final FluidBuilder NACL_SOLUTION = registerFluid(
            "nacl_solution",
            FluidType.Properties.create()
                    .descriptionId("fluid.chemical_industry.nacl_solution")
                    .density(1100)                // 比水重（水 = 1000）
                    .viscosity(1200)              // 比水黏（水 = 1000）
                    .temperature(300)             // 常温（水 = 300）
                    .canExtinguish(false)         // 不能灭火
                    .supportsBoating(false)       // 不能行船
                    .motionScale(0.008)           // 实体移动减速程度（水 = 0.014）
                    .fallDistanceModifier(0.0f)   // 不减少摔落伤害
                    .canConvertToSource(false)    // 不能无限水源
    );

    // ---------- 硫酸 ----------

    public static final FluidBuilder SULFURIC_ACID = registerFluid(
            "sulfuric_acid",
            FluidType.Properties.create()
                    .descriptionId("fluid.chemical_industry.sulfuric_acid")
                    .density(1840)                // 浓硫酸比水重得多
                    .viscosity(2600)              // 浓稠油状
                    .temperature(300)
                    .canExtinguish(false)
                    .supportsBoating(false)
                    .motionScale(0.004)           // 很黏，走得慢
                    .fallDistanceModifier(0.0f)
                    .canConvertToSource(false)
    );

    // ---------- 氯气（气体！无方块、无桶）----------

    public static final FluidBuilder CHLORINE_GAS = registerGas(
            "chlorine_gas",
            FluidType.Properties.create()
                    .descriptionId("fluid.chemical_industry.chlorine_gas")
                    .density(-500).viscosity(500).temperature(300)
                    .canExtinguish(false).supportsBoating(false)
                    .motionScale(0.014).fallDistanceModifier(0.0f)
                    .canConvertToSource(false)
    );

    // ---------- 氧气（气体！无方块、无桶）----------

    public static final FluidBuilder OXYGEN_GAS = registerGas(
            "oxygen_gas",
            FluidType.Properties.create()
                    .descriptionId("fluid.chemical_industry.oxygen_gas")
                    .density(-600).viscosity(500).temperature(300)
                    .canExtinguish(false).supportsBoating(false)
                    .motionScale(0.014).fallDistanceModifier(0.0f)
                    .canConvertToSource(false)
    );

    // ---------- 氨气（气体！无方块、无桶）----------

    public static final FluidBuilder AMMONIA_GAS = registerGas(
            "ammonia_gas",
            FluidType.Properties.create()
                    .descriptionId("fluid.chemical_industry.ammonia_gas")
                    .density(-400).viscosity(500).temperature(300)
                    .canExtinguish(false).supportsBoating(false)
                    .motionScale(0.014).fallDistanceModifier(0.0f)
                    .canConvertToSource(false)
    );

    // ---------- 压缩空气（气体！无方块、无桶）----------

    public static final FluidBuilder COMPRESSED_AIR = registerGas(
            "compressed_air",
            FluidType.Properties.create()
                    .descriptionId("fluid.chemical_industry.compressed_air")
                    .density(-400).viscosity(600).temperature(300)
                    .canExtinguish(false).supportsBoating(false)
                    .motionScale(0.014).fallDistanceModifier(0.0f)
                    .canConvertToSource(false)
    );

    // ---------- 氮气（气体！无方块、无桶）----------

    public static final FluidBuilder NITROGEN_GAS = registerGas(
            "nitrogen_gas",
            FluidType.Properties.create()
                    .descriptionId("fluid.chemical_industry.nitrogen_gas")
                    .density(-550).viscosity(450).temperature(300)
                    .canExtinguish(false).supportsBoating(false)
                    .motionScale(0.014).fallDistanceModifier(0.0f)
                    .canConvertToSource(false)
    );

    // ---------- 稀有气体（气体！无方块、无桶）----------

    public static final FluidBuilder RARE_GAS = registerGas(
            "rare_gas",
            FluidType.Properties.create()
                    .descriptionId("fluid.chemical_industry.rare_gas")
                    .density(-700).viscosity(400).temperature(300)
                    .canExtinguish(false).supportsBoating(false)
                    .motionScale(0.014).fallDistanceModifier(0.0f)
                    .canConvertToSource(false)
    );

    // ---------- 氢气（气体！无方块、无桶）----------

    public static final FluidBuilder HYDROGEN_GAS = registerGas(
            "hydrogen_gas",
            FluidType.Properties.create()
                    .descriptionId("fluid.chemical_industry.hydrogen_gas")
                    .density(-800).viscosity(400).temperature(300)
                    .canExtinguish(false).supportsBoating(false)
                    .motionScale(0.014).fallDistanceModifier(0.0f)
                    .canConvertToSource(false)
    );

    // ---------- 氢氧化钠溶液 ----------

    public static final FluidBuilder SODIUM_HYDROXIDE_SOLUTION = registerFluid(
            "sodium_hydroxide_solution",
            FluidType.Properties.create()
                    .descriptionId("fluid.chemical_industry.sodium_hydroxide_solution")
                    .density(1100)
                    .viscosity(1200)
                    .temperature(300)
                    .canExtinguish(false)
                    .supportsBoating(false)
                    .motionScale(0.008)
                    .fallDistanceModifier(0.0f)
                    .canConvertToSource(false)
    );

    // ---------- 盐酸 ----------

    public static final FluidBuilder HYDROCHLORIC_ACID = registerFluid(
            "hydrochloric_acid",
            FluidType.Properties.create()
                    .descriptionId("fluid.chemical_industry.hydrochloric_acid")
                    .density(1180).viscosity(1100).temperature(300)
                    .canExtinguish(false).supportsBoating(false)
                    .motionScale(0.008).fallDistanceModifier(0.0f)
                    .canConvertToSource(false)
    );

    // ---------- 硝酸 ----------

    public static final FluidBuilder NITRIC_ACID = registerFluid(
            "nitric_acid",
            FluidType.Properties.create()
                    .descriptionId("fluid.chemical_industry.nitric_acid")
                    .density(1500).viscosity(900).temperature(300)
                    .canExtinguish(false).supportsBoating(false)
                    .motionScale(0.007).fallDistanceModifier(0.0f)
                    .canConvertToSource(false)
    );

    // ---------- 四羟基合铝酸钠溶液 ----------

    public static final FluidBuilder SODIUM_ALUMINATE_SOLUTION = registerFluid(
            "sodium_aluminate_solution",
            FluidType.Properties.create()
                    .descriptionId("fluid.chemical_industry.sodium_aluminate_solution")
                    .density(1200).viscosity(1300).temperature(300)
                    .canExtinguish(false).supportsBoating(false)
                    .motionScale(0.007).fallDistanceModifier(0.0f)
                    .canConvertToSource(false)
    );

    // ---------- 硫酸铜溶液 ----------

    public static final FluidBuilder COPPER_SULFATE_SOLUTION = registerFluid(
            "copper_sulfate_solution",
            FluidType.Properties.create()
                    .descriptionId("fluid.chemical_industry.copper_sulfate_solution")
                    .density(1200)
                    .viscosity(1100)
                    .temperature(300)
                    .canExtinguish(false)
                    .supportsBoating(false)
                    .motionScale(0.008)
                    .fallDistanceModifier(0.0f)
                    .canConvertToSource(false)
    );

    // ---------- 硫酸钠溶液 ----------

    public static final FluidBuilder SODIUM_SULFATE_SOLUTION = registerFluid(
            "sodium_sulfate_solution",
            FluidType.Properties.create()
                    .descriptionId("fluid.chemical_industry.sodium_sulfate_solution")
                    .density(1100).viscosity(1200).temperature(300)
                    .canExtinguish(false).supportsBoating(false)
                    .motionScale(0.008).fallDistanceModifier(0.0f)
                    .canConvertToSource(false)
    );

    // ---------- 硝酸钾溶液 ----------

    public static final FluidBuilder POTASSIUM_NITRATE_SOLUTION = registerFluid(
            "potassium_nitrate_solution",
            FluidType.Properties.create()
                    .descriptionId("fluid.chemical_industry.potassium_nitrate_solution")
                    .density(1100).viscosity(1100).temperature(300)
                    .canExtinguish(false).supportsBoating(false)
                    .motionScale(0.008).fallDistanceModifier(0.0f)
                    .canConvertToSource(false)
    );

    // ---------- 硝酸钠溶液 ----------

    public static final FluidBuilder SODIUM_NITRATE_SOLUTION = registerFluid(
            "sodium_nitrate_solution",
            FluidType.Properties.create()
                    .descriptionId("fluid.chemical_industry.sodium_nitrate_solution")
                    .density(1100).viscosity(1200).temperature(300)
                    .canExtinguish(false).supportsBoating(false)
                    .motionScale(0.008).fallDistanceModifier(0.0f)
                    .canConvertToSource(false)
    );

    // ---------- 卤水 (Brine) — 水在沸腾炉中加热得到 ----------

    public static final FluidBuilder BRINE = registerFluid(
            "brine",
            FluidType.Properties.create()
                    .descriptionId("fluid.chemical_industry.brine")
                    .density(1150).viscosity(1100).temperature(300)
                    .canExtinguish(false).supportsBoating(false)
                    .motionScale(0.008).fallDistanceModifier(0.0f)
                    .canConvertToSource(false)
    );

    // ---------- 石灰水 (Limewater) — CaO + H₂O ----------

    public static final FluidBuilder LIME_WATER = registerFluid(
            "limewater",
            FluidType.Properties.create()
                    .descriptionId("fluid.chemical_industry.limewater")
                    .density(1050).viscosity(1000).temperature(300)
                    .canExtinguish(false).supportsBoating(false)
                    .motionScale(0.010).fallDistanceModifier(0.0f)
                    .canConvertToSource(false)
    );

    // ---------- 氨水 (Ammonia Water) — NH₃ + H₂O ----------

    public static final FluidBuilder AMMONIA_WATER = registerFluid(
            "ammonia_water",
            FluidType.Properties.create()
                    .descriptionId("fluid.chemical_industry.ammonia_water")
                    .density(1000).viscosity(1000).temperature(300)
                    .canExtinguish(false).supportsBoating(false)
                    .motionScale(0.010).fallDistanceModifier(0.0f)
                    .canConvertToSource(false)
    );

    // ---------- 氯化铝溶液 (AlCl₃ Solution) — Al₂O₃ / Al + HCl ----------

    public static final FluidBuilder ALUMINUM_CHLORIDE_SOLUTION = registerFluid(
            "aluminum_chloride_solution",
            FluidType.Properties.create()
                    .descriptionId("fluid.chemical_industry.aluminum_chloride_solution")
                    .density(1200).viscosity(1300).temperature(300)
                    .canExtinguish(false).supportsBoating(false)
                    .motionScale(0.008).fallDistanceModifier(0.0f)
                    .canConvertToSource(false)
    );

    // ---------- 碳酸钠溶液 (Na₂CO₃ Solution) — NaOH + CO₂ ----------

    public static final FluidBuilder SODIUM_CARBONATE_SOLUTION = registerFluid(
            "sodium_carbonate_solution",
            FluidType.Properties.create()
                    .descriptionId("fluid.chemical_industry.sodium_carbonate_solution")
                    .density(1100).viscosity(1200).temperature(300)
                    .canExtinguish(false).supportsBoating(false)
                    .motionScale(0.008).fallDistanceModifier(0.0f)
                    .canConvertToSource(false)
    );

    // ---------- 水银 (Hg) — 朱砂加热分解得到，液态金属 ----------

    public static final FluidBuilder MERCURY = registerFluid(
            "mercury",
            FluidType.Properties.create()
                    .descriptionId("fluid.chemical_industry.mercury")
                    .density(13500).viscosity(500).temperature(300)  // 比水重 13.5 倍，流动快（液态金属）
                    .canExtinguish(false).supportsBoating(false)
                    .motionScale(0.005).fallDistanceModifier(0.0f)
                    .canConvertToSource(false)
    );

    // ---------- 氢氟酸 (HF) — 萤石粉 + 硫酸加热制取，剧毒！电解制氟 ----------

    public static final FluidBuilder HYDROFLUORIC_ACID = registerFluid(
            "hydrofluoric_acid",
            FluidType.Properties.create()
                    .descriptionId("fluid.chemical_industry.hydrofluoric_acid")
                    .density(1150).viscosity(1100).temperature(300)
                    .canExtinguish(false).supportsBoating(false)
                    .motionScale(0.008).fallDistanceModifier(0.0f)
                    .canConvertToSource(false)
    );

    // ---------- 乙醛 (Acetaldehyde CH₃CHO) — 乙醇氧化产物，液体 ----------

    public static final FluidBuilder ACETALDEHYDE = registerFluid(
            "acetaldehyde",
            FluidType.Properties.create()
                    .descriptionId("fluid.chemical_industry.acetaldehyde")
                    .density(780).viscosity(600).temperature(300)
                    .canExtinguish(false).supportsBoating(false)
                    .motionScale(0.012).fallDistanceModifier(0.0f)
                    .canConvertToSource(false)
    );

    // ---------- 乙酸 (Acetic Acid CH₃COOH) — 乙醛氧化产物，液体 ----------

    public static final FluidBuilder ACETIC_ACID = registerFluid(
            "acetic_acid",
            FluidType.Properties.create()
                    .descriptionId("fluid.chemical_industry.acetic_acid")
                    .density(1050).viscosity(1200).temperature(300)
                    .canExtinguish(false).supportsBoating(false)
                    .motionScale(0.008).fallDistanceModifier(0.0f)
                    .canConvertToSource(false)
    );

    // ---------- 二氧化碳 (CO₂ Gas) — 碳酸盐分解/燃烧产物 ----------

    public static final FluidBuilder CARBON_DIOXIDE = registerGas(
            "carbon_dioxide",
            FluidType.Properties.create()
                    .descriptionId("fluid.chemical_industry.carbon_dioxide")
                    .density(-600).viscosity(500).temperature(300)
                    .canExtinguish(true).supportsBoating(false)   // CO₂可灭火
                    .motionScale(0.014).fallDistanceModifier(0.0f)
                    .canConvertToSource(false)
    );

    // ---------- 一氧化碳 (CO Gas) — 有毒！不完全燃烧产物 ----------

    public static final FluidBuilder CARBON_MONOXIDE = registerGas(
            "carbon_monoxide",
            FluidType.Properties.create()
                    .descriptionId("fluid.chemical_industry.carbon_monoxide")
                    .density(-500).viscosity(500).temperature(300)
                    .canExtinguish(false).supportsBoating(false)
                    .motionScale(0.014).fallDistanceModifier(0.0f)
                    .canConvertToSource(false)
    );

    // ---------- 甲烷 (CH₄ Gas) — 天然气主要成分 ----------

    public static final FluidBuilder METHANE = registerGas(
            "methane",
            FluidType.Properties.create()
                    .descriptionId("fluid.chemical_industry.methane")
                    .density(-450).viscosity(400).temperature(300)
                    .canExtinguish(false).supportsBoating(false)
                    .motionScale(0.014).fallDistanceModifier(0.0f)
                    .canConvertToSource(false)
    );

    // ---------- 焦油 (Coal Tar) — 煤炭干馏的液态产物 ----------

    public static final FluidBuilder COAL_TAR = registerFluid(
            "coal_tar",
            FluidType.Properties.create()
                    .descriptionId("fluid.chemical_industry.coal_tar")
                    .density(1200).viscosity(3000).temperature(350)
                    .canExtinguish(false).supportsBoating(false)
                    .motionScale(0.004).fallDistanceModifier(0.0f)
                    .canConvertToSource(false)
    );

    // ---------- 苯 (Benzene) — 流体版，分馏/有机合成用 ----------

    public static final FluidBuilder BENZENE = registerFluid(
            "benzene",
            FluidType.Properties.create()
                    .descriptionId("fluid.chemical_industry.benzene")
                    .density(880).viscosity(700).temperature(300)
                    .canExtinguish(false).supportsBoating(false)
                    .motionScale(0.010).fallDistanceModifier(0.0f)
                    .canConvertToSource(false)
    );

    // ---------- 苯酚 (Phenol) — 流体版 ----------

    public static final FluidBuilder PHENOL = registerFluid(
            "phenol",
            FluidType.Properties.create()
                    .descriptionId("fluid.chemical_industry.phenol")
                    .density(1070).viscosity(1500).temperature(320)
                    .canExtinguish(false).supportsBoating(false)
                    .motionScale(0.007).fallDistanceModifier(0.0f)
                    .canConvertToSource(false)
    );

    // ---------- 甲醇 (Methanol) — 流体版 ----------

    public static final FluidBuilder METHANOL = registerFluid(
            "methanol",
            FluidType.Properties.create()
                    .descriptionId("fluid.chemical_industry.methanol")
                    .density(790).viscosity(600).temperature(300)
                    .canExtinguish(false).supportsBoating(false)
                    .motionScale(0.011).fallDistanceModifier(0.0f)
                    .canConvertToSource(false)
    );

    // ---------- 甲醛 (Formaldehyde) — 流体版 ----------

    public static final FluidBuilder FORMALDEHYDE = registerFluid(
            "formaldehyde",
            FluidType.Properties.create()
                    .descriptionId("fluid.chemical_industry.formaldehyde")
                    .density(820).viscosity(700).temperature(300)
                    .canExtinguish(false).supportsBoating(false)
                    .motionScale(0.010).fallDistanceModifier(0.0f)
                    .canConvertToSource(false)
    );

    // ---------- 丙酮 (Acetone) — 流体版 ----------

    public static final FluidBuilder ACETONE = registerFluid(
            "acetone",
            FluidType.Properties.create()
                    .descriptionId("fluid.chemical_industry.acetone")
                    .density(790).viscosity(500).temperature(300)
                    .canExtinguish(false).supportsBoating(false)
                    .motionScale(0.011).fallDistanceModifier(0.0f)
                    .canConvertToSource(false)
    );

    // ---------- 批量注册方法 ----------

    /**
     * 注册一个化学流体及其关联项（类型、源、流动、方块、桶）
     *
     * @param name        流体名称（如 "sulfuric_acid"）
     * @param typeProps   流体物理属性
     * @return FluidBuilder 包含流体所有注册引用的容器
     */
    private static FluidBuilder registerFluid(String name, FluidType.Properties typeProps) {
        // ① 流体类型 — 用 ChemicalFluidType 设置贴图路径
        ResourceLocation stillTex = ResourceLocation
                .fromNamespaceAndPath(ChemicalIndustry.MOD_ID, "block/" + name + "_still");
        ResourceLocation flowingTex = ResourceLocation
                .fromNamespaceAndPath(ChemicalIndustry.MOD_ID, "block/" + name + "_flow");

        Supplier<FluidType> fluidType = FLUID_TYPES.register(name,
                () -> new ChemicalFluidType(typeProps, stillTex, flowingTex));

        // ② 源流体 + 流动流体（先注册占位，通过 FluidBuilder 建立关联）
        // 由于 Source 和 Flowing 需要相互引用（循环依赖），使用 Supplier 延迟获取

        FluidBuilder builder = new FluidBuilder();

        // ③ 方块 — 用 ChemicalLiquidBlock 实现流体混合 + 腐蚀效果
        Supplier<LiquidBlock> block = FLUID_BLOCKS.register(name,
                () -> new ChemicalLiquidBlock(builder.source.get(), BlockBehaviour.Properties.of()
                        .mapColor(MapColor.WATER)
                        .noLootTable()
                        .replaceable()
                        .noCollission()
                        .strength(100.0f)
                        .pushReaction(PushReaction.DESTROY)
                        .liquid(), name));  // 传入流体名用于判断类型

        // ④ 源流体
        builder.source = FLUIDS.register(name,
                () -> new ChemicalFluid.Source(
                        fluidType,
                        block,
                        () -> builder.bucket.get(),
                        () -> builder.flowing.get(),
                        () -> builder.source.get()));

        // ⑤ 流动流体
        builder.flowing = FLUIDS.register("flowing_" + name,
                () -> new ChemicalFluid.Flowing(
                        fluidType,
                        block,
                        () -> builder.bucket.get(),
                        () -> builder.flowing.get(),
                        () -> builder.source.get()));

        // ⑥ 桶物品
        builder.bucket = FLUID_BUCKETS.register(name + "_bucket",
                () -> new BucketItem(builder.source.get(),
                        new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));

        builder.fluidType = fluidType;
        builder.block = block;
        builder.name = name;

        return builder;
    }

    /**
     * 注册气体（无方块形态、不能用桶装）
     */
    private static FluidBuilder registerGas(String name, FluidType.Properties typeProps) {
        ResourceLocation stillTex = ResourceLocation.fromNamespaceAndPath(ChemicalIndustry.MOD_ID, "block/" + name + "_still");
        ResourceLocation flowingTex = ResourceLocation.fromNamespaceAndPath(ChemicalIndustry.MOD_ID, "block/" + name + "_flow");

        Supplier<FluidType> fluidType = FLUID_TYPES.register(name,
                () -> new ChemicalFluidType(typeProps, stillTex, flowingTex));

        FluidBuilder builder = new FluidBuilder();

        // 气体流体仍需要方块引用（createLegacyBlock），但不注册方块——使用空气占位
        // FlowingFluid.createLegacyBlock 返回 AIR 即可，因为气体不能放置
        builder.source = FLUIDS.register(name, () -> new ChemicalFluid.Source(
                fluidType, () -> null, () -> null,
                () -> builder.flowing.get(), () -> builder.source.get()) {
            @Override protected BlockState createLegacyBlock(FluidState state) {
                return Blocks.AIR.defaultBlockState();  // 气体不能作为方块存在
            }
        });

        builder.flowing = FLUIDS.register("flowing_" + name, () -> new ChemicalFluid.Flowing(
                fluidType, () -> null, () -> null,
                () -> builder.flowing.get(), () -> builder.source.get()) {
            @Override protected BlockState createLegacyBlock(FluidState state) {
                return Blocks.AIR.defaultBlockState();
            }
        });

        builder.fluidType = fluidType;
        builder.name = name;
        return builder;
    }

    /**
     * 向事件总线注册所有流体相关的内容
     */
    public static void register(IEventBus eventBus) {
        FLUID_TYPES.register(eventBus);
        FLUIDS.register(eventBus);
        FLUID_BLOCKS.register(eventBus);
        FLUID_BUCKETS.register(eventBus);
    }

    // ---------- FluidBuilder：持有流体所有注册引用的容器 ----------

    /**
     * 由于 Source 和 Flowing 需要互相引用（循环依赖），
     * 不能直接在注册时完成所有连接。
     * FluidBuilder 在注册后持有所有 Supplier 引用，
     * 各流体可以在构造时通过 Supplier::get 延迟获取依赖。
     */
    public static class FluidBuilder {
        public Supplier<FluidType> fluidType;
        public Supplier<FlowingFluid> source;
        public Supplier<FlowingFluid> flowing;
        public Supplier<LiquidBlock> block;
        public Supplier<BucketItem> bucket;
        public String name;

        public FlowingFluid getSource() { return source.get(); }
        public FlowingFluid getFlowing() { return flowing.get(); }
        public LiquidBlock getBlock() { return block.get(); }
        public BucketItem getBucket() { return bucket.get(); }
    }
}
