package com.cxy.chemical_industry.registry;

import com.cxy.chemical_industry.ChemicalIndustry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * 创造模式物品栏 — 分为 3 个标签页：
 *   1. 化工装置：机器方块、耗材、储气罐
 *   2. 无机化学：矿石、金属、盐类、无机流体
 *   3. 有机化学：有机物、有机流体、罂粟
 */
public class ModCreativeTabs {

    private static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ChemicalIndustry.MOD_ID);

    // =====================================================================
    // ① 化工装置
    // =====================================================================
    public static final Supplier<CreativeModeTab> DEVICES_TAB = TABS.register(
            "devices_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.chemical_industry_devices"))
                    .icon(() -> new ItemStack(ModItems.ELECTROLYZER.get()))
                    .displayItems((params, output) -> {
                        // ---- 机器 ----
                        output.accept(ModItems.FLUIDIZED_BED_CONTROLLER.get());
                        output.accept(ModItems.ELECTROLYZER.get());
                        output.accept(ModItems.AIR_COMPRESSOR.get());
                        output.accept(ModItems.CONDENSER_PIPE.get());

                        // ---- 耗材 ----
                        output.accept(ModItems.CATION_EXCHANGE_MEMBRANE.get());
                        output.accept(ModItems.FILTER_MESH.get());
                        output.accept(ModItems.GRAPHITE_ROD.get());
                        output.accept(ModItems.ANODE_SLIME.get());

                        // ---- 储气罐 ----
                        output.accept(ModItems.GAS_CANISTER.get());
                    })
                    .build()
    );

    // =====================================================================
    // ② 无机化学
    // =====================================================================
    public static final Supplier<CreativeModeTab> INORGANIC_TAB = TABS.register(
            "inorganic_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.chemical_industry_inorganic"))
                    .icon(() -> new ItemStack(ModItems.SULFUR_POWDER.get()))
                    .displayItems((params, output) -> {
                        // ---- 矿石 ----
                        output.accept(ModItems.SULFUR_ORE.get());
                        output.accept(ModItems.PYRITE_ORE.get());
                        output.accept(ModItems.ROCK_SALT_ORE.get());
                        output.accept(ModItems.NITER_ORE.get());
                        output.accept(ModItems.SILVER_ORE.get());
                        output.accept(ModItems.RARE_EARTH_ORE.get());
                        output.accept(ModItems.BAUXITE_ORE.get());
                        output.accept(ModItems.GRAPHITE_ORE.get());
                        output.accept(ModItems.GALENA_ORE.get());
                        output.accept(ModItems.CASSITERITE_ORE.get());
                        output.accept(ModItems.CINNABAR_ORE.get());
                        // 深层矿石变种
                        output.accept(ModItems.DEEPSLATE_SULFUR_ORE.get());
                        output.accept(ModItems.DEEPSLATE_PYRITE_ORE.get());
                        output.accept(ModItems.DEEPSLATE_ROCK_SALT_ORE.get());
                        output.accept(ModItems.DEEPSLATE_NITER_ORE.get());
                        output.accept(ModItems.DEEPSLATE_SILVER_ORE.get());
                        output.accept(ModItems.DEEPSLATE_RARE_EARTH_ORE.get());
                        output.accept(ModItems.DEEPSLATE_BAUXITE_ORE.get());
                        output.accept(ModItems.DEEPSLATE_GRAPHITE_ORE.get());
                        output.accept(ModItems.DEEPSLATE_GALENA_ORE.get());
                        output.accept(ModItems.DEEPSLATE_CASSITERITE_ORE.get());
                        output.accept(ModItems.DEEPSLATE_CINNABAR_ORE.get());

                        // ---- 矿石原料 ----
                        output.accept(ModItems.SULFUR_POWDER.get());
                        output.accept(ModItems.RAW_PYRITE.get());
                        output.accept(ModItems.SODIUM_CHLORIDE.get());
                        output.accept(ModItems.POTASSIUM_NITRATE.get());
                        output.accept(ModItems.RAW_SILVER.get());
                        output.accept(ModItems.RAW_RARE_EARTH.get());
                        output.accept(ModItems.BAUXITE.get());
                        output.accept(ModItems.GRAPHITE.get());
                        output.accept(ModItems.CINNABAR.get());
                        output.accept(ModItems.CRYOLITE.get());
                        output.accept(ModItems.ALUMINA.get());

                        // ---- 化工产品 ----
                        output.accept(ModItems.IRON_OXIDE.get());
                        output.accept(ModItems.SODIUM_HYDROXIDE.get());
                        output.accept(ModItems.COPPER_OXIDE.get());
                        output.accept(ModItems.COPPER_SULFATE.get());

                        // ---- 金属 ----
                        output.accept(ModItems.SILVER_INGOT.get());
                        output.accept(ModItems.RARE_EARTH_ELEMENT.get());
                        output.accept(ModItems.PLATINUM_CONCENTRATE.get());
                        output.accept(ModItems.ALUMINUM_INGOT.get());
                        output.accept(ModItems.ALUMINUM_NUGGET.get());
                        output.accept(ModItems.STEEL_INGOT.get());
                        output.accept(ModItems.STEEL_NUGGET.get());
                        output.accept(ModItems.DURALUMIN_INGOT.get());
                        output.accept(ModItems.DURALUMIN_NUGGET.get());
                        output.accept(ModItems.LEAD_INGOT.get());
                        output.accept(ModItems.TIN_INGOT.get());
                        output.accept(ModItems.BRONZE_INGOT.get());
                        output.accept(ModItems.BRONZE_NUGGET.get());
                        output.accept(ModItems.MAGNESIUM_INGOT.get());
                        output.accept(ModItems.MAGNESIUM_NUGGET.get());
                        output.accept(ModItems.SODIUM_INGOT.get());

                        // ---- 无机化合物 ----
                        output.accept(ModItems.CALCIUM_OXIDE.get());
                        output.accept(ModItems.SILICON_DIOXIDE.get());
                        output.accept(ModItems.SILICON.get());
                        output.accept(ModItems.COPPER_NUGGET.get());
                        output.accept(ModItems.SODIUM_CARBONATE.get());
                        output.accept(ModItems.SODIUM_SULFATE.get());
                        output.accept(ModItems.SODIUM_NITRATE.get());
                        output.accept(ModItems.MAGNESIUM_OXIDE.get());
                        output.accept(ModItems.MAGNESIUM_CHLORIDE.get());
                        output.accept(ModItems.ALUMINUM_CHLORIDE.get());

                        // ---- 金属方块 ----
                        output.accept(ModItems.ALUMINUM_BLOCK.get());
                        output.accept(ModItems.STEEL_BLOCK.get());
                        output.accept(ModItems.MAGNESIUM_BLOCK.get());
                        output.accept(ModItems.GRAPHITE_BLOCK.get());
                        output.accept(ModItems.LEAD_BLOCK.get());
                        output.accept(ModItems.TIN_BLOCK.get());
                        output.accept(ModItems.BRONZE_BLOCK.get());
                        output.accept(ModItems.SILVER_BLOCK.get());
                        output.accept(ModItems.DURALUMIN_BLOCK.get());

                        // ---- 工具 ----
                        output.accept(ModItems.STEEL_SWORD.get());
                        output.accept(ModItems.STEEL_PICKAXE.get());
                        output.accept(ModItems.STEEL_AXE.get());
                        output.accept(ModItems.STEEL_SHOVEL.get());
                        output.accept(ModItems.STEEL_HOE.get());
                        output.accept(ModItems.ALUMINUM_SWORD.get());
                        output.accept(ModItems.ALUMINUM_PICKAXE.get());
                        output.accept(ModItems.ALUMINUM_AXE.get());
                        output.accept(ModItems.ALUMINUM_SHOVEL.get());
                        output.accept(ModItems.ALUMINUM_HOE.get());
                        output.accept(ModItems.DURALUMIN_SWORD.get());
                        output.accept(ModItems.DURALUMIN_PICKAXE.get());
                        output.accept(ModItems.DURALUMIN_AXE.get());
                        output.accept(ModItems.DURALUMIN_SHOVEL.get());
                        output.accept(ModItems.DURALUMIN_HOE.get());
                        output.accept(ModItems.BRONZE_SWORD.get());
                        output.accept(ModItems.BRONZE_PICKAXE.get());
                        output.accept(ModItems.BRONZE_AXE.get());
                        output.accept(ModItems.BRONZE_SHOVEL.get());
                        output.accept(ModItems.BRONZE_HOE.get());

                        // ---- 盔甲 ----
                        output.accept(ModItems.STEEL_HELMET.get());
                        output.accept(ModItems.STEEL_CHESTPLATE.get());
                        output.accept(ModItems.STEEL_LEGGINGS.get());
                        output.accept(ModItems.STEEL_BOOTS.get());
                        output.accept(ModItems.ALUMINUM_HELMET.get());
                        output.accept(ModItems.ALUMINUM_CHESTPLATE.get());
                        output.accept(ModItems.ALUMINUM_LEGGINGS.get());
                        output.accept(ModItems.ALUMINUM_BOOTS.get());
                        output.accept(ModItems.DURALUMIN_HELMET.get());
                        output.accept(ModItems.DURALUMIN_CHESTPLATE.get());
                        output.accept(ModItems.DURALUMIN_LEGGINGS.get());
                        output.accept(ModItems.DURALUMIN_BOOTS.get());
                        output.accept(ModItems.BRONZE_HELMET.get());
                        output.accept(ModItems.BRONZE_CHESTPLATE.get());
                        output.accept(ModItems.BRONZE_LEGGINGS.get());
                        output.accept(ModItems.BRONZE_BOOTS.get());

                        // ---- 无机流体桶 ----
                        output.accept(ModFluids.NACL_SOLUTION.bucket.get());
                        output.accept(ModFluids.SULFURIC_ACID.bucket.get());
                        output.accept(ModFluids.SODIUM_HYDROXIDE_SOLUTION.bucket.get());
                        output.accept(ModFluids.COPPER_SULFATE_SOLUTION.bucket.get());
                        output.accept(ModFluids.HYDROCHLORIC_ACID.bucket.get());
                        output.accept(ModFluids.NITRIC_ACID.bucket.get());
                        output.accept(ModFluids.SODIUM_ALUMINATE_SOLUTION.bucket.get());
                        output.accept(ModFluids.SODIUM_SULFATE_SOLUTION.bucket.get());
                        output.accept(ModFluids.SODIUM_NITRATE_SOLUTION.bucket.get());
                        output.accept(ModFluids.POTASSIUM_NITRATE_SOLUTION.bucket.get());
                        output.accept(ModFluids.BRINE.bucket.get());
                        output.accept(ModFluids.LIME_WATER.bucket.get());
                        output.accept(ModFluids.AMMONIA_WATER.bucket.get());
                        output.accept(ModFluids.ALUMINUM_CHLORIDE_SOLUTION.bucket.get());
                        output.accept(ModFluids.SODIUM_CARBONATE_SOLUTION.bucket.get());
                        output.accept(ModFluids.HYDROFLUORIC_ACID.bucket.get());
                        output.accept(ModFluids.MERCURY.bucket.get());
                    })
                    .build()
    );

    // =====================================================================
    // ③ 有机化学
    // =====================================================================
    public static final Supplier<CreativeModeTab> ORGANIC_TAB = TABS.register(
            "organic_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.chemical_industry_organic"))
                    .icon(() -> new ItemStack(ModItems.UREA.get()))
                    .displayItems((params, output) -> {
                        // ---- 有机原料 ----
                        output.accept(ModItems.UREA.get());
                        output.accept(ModItems.METHANOL.get());
                        output.accept(ModItems.FORMALDEHYDE.get());
                        output.accept(ModItems.BENZENE.get());
                        output.accept(ModItems.PHENOL.get());
                        output.accept(ModItems.ACETONE.get());
                        output.accept(ModItems.METHANE.get());

                        // ---- 有机合成产物 ----
                        output.accept(ModItems.PHENOLIC_RESIN.get());
                        output.accept(ModItems.PICRIC_ACID.get());

                        // ---- 罂粟 ----
                        output.accept(ModItems.OPIUM_POPPY_FRUIT.get());
                        output.accept(ModItems.OPIUM.get());

                        // ---- 有机流体桶 ----
                        output.accept(ModFluids.ACETALDEHYDE.bucket.get());
                        output.accept(ModFluids.ACETIC_ACID.bucket.get());
                        output.accept(ModFluids.COAL_TAR.bucket.get());
                        output.accept(ModFluids.BENZENE.bucket.get());
                        output.accept(ModFluids.PHENOL.bucket.get());
                        output.accept(ModFluids.METHANOL.bucket.get());
                        output.accept(ModFluids.FORMALDEHYDE.bucket.get());
                        output.accept(ModFluids.ACETONE.bucket.get());
                    })
                    .build()
    );

    public static void register(IEventBus eventBus) {
        TABS.register(eventBus);
    }
}
