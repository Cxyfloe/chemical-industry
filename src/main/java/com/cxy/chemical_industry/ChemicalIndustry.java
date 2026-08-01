package com.cxy.chemical_industry;

import com.cxy.chemical_industry.block_entity.AirCompressorBlockEntity;
import com.cxy.chemical_industry.block_entity.ElectrolyzerBlockEntity;
import com.cxy.chemical_industry.block_entity.FluidizedBedBlockEntity;
import com.cxy.chemical_industry.registry.*;
import com.cxy.chemical_industry.screen.ElectrolyzerScreen;
import com.cxy.chemical_industry.screen.FluidizedBedScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/**
 * 化学工业模组 - 主入口类
 *
 * @Mod 注解告诉 NeoForge："这是一个模组，请在启动时加载我"
 * modid 参数必须与 neoforge.mods.toml 中的 modId 完全一致
 */
@Mod(ChemicalIndustry.MOD_ID)  // ← 这个括号里的名字必须和下面的常量一致
public class ChemicalIndustry {

    /** 模组 ID — 整个项目中引用本模组资源的唯一标识符 */
    public static final String MOD_ID = "chemical_industry";

    /**
     * 构造器 — 模组加载时会自动执行
     * NeoForge 通过参数传入 IEventBus（事件总线），
     * 我们可以在这里向总线注册方块、物品等
     */
    public ChemicalIndustry(IEventBus modEventBus) {
        // 依次向 NeoForge 注册本模组的各项内容
        // 注册顺序不重要，但建议先注册方块再注册物品
        // （因为物品可能是方块对应的 BlockItem）

        ModCreativeTabs.register(modEventBus);    // ① 创造模式物品栏
        ModBlocks.register(modEventBus);          // ② 方块
        ModBlockEntities.register(modEventBus);   // ③ 方块实体类型
        ModItems.register(modEventBus);           // ④ 物品
        ModMenus.register(modEventBus);           // ⑤ 容器菜单类型
        ModFluids.register(modEventBus);          // ⑥ 流体（类型 + 方块 + 桶）
        ModEffects.register(modEventBus);         // ⑦ 自定义状态效果

        // 客户端专用：注册 Ponder 教程场景
        // 必须在构造阶段注册（和 Create 保持一致），否则 Ponder 索引里找不到场景
        if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) {
            net.createmod.ponder.foundation.PonderIndex.addPlugin(
                    new com.cxy.chemical_industry.ponder.ChemicalPonderPlugin());
        }
    }

    // ---------- 能力注册（漏斗/溜槽 I/O） ----------

    @EventBusSubscriber(modid = MOD_ID, bus = EventBusSubscriber.Bus.MOD)
    public static class CapabilityEvents {
        @SubscribeEvent
        public static void registerCapabilities(RegisterCapabilitiesEvent event) {
            // 沸腾炉：物品 I/O（漏斗/溜槽）
            event.registerBlockEntity(
                    Capabilities.ItemHandler.BLOCK,
                    ModBlockEntities.FLUIDIZED_BED.get(),
                    (be, side) -> be.getAutomationHandler()
            );
            // 沸腾炉：流体 I/O（委托到 Controller）
            event.registerBlockEntity(
                    Capabilities.FluidHandler.BLOCK,
                    ModBlockEntities.FLUIDIZED_BED.get(),
                    (be, side) -> be.handlerForCapability(side)
            );
            // 电解槽：物品 I/O
            event.registerBlockEntity(
                    Capabilities.ItemHandler.BLOCK,
                    ModBlockEntities.ELECTROLYZER.get(),
                    (be, side) -> be.autoItems()
            );
            // 电解槽：流体 I/O
            event.registerBlockEntity(
                    Capabilities.FluidHandler.BLOCK,
                    ModBlockEntities.ELECTROLYZER.get(),
                    (be, side) -> be.fluidHandler(side)
            );
            // 电解槽：能量 I/O（Create: Crafts & Additions 电线）
            event.registerBlockEntity(
                    Capabilities.EnergyStorage.BLOCK,
                    ModBlockEntities.ELECTROLYZER.get(),
                    (be, side) -> be.energyStorage()
            );
            // 空气压缩机：流体 I/O（背面泵出压缩空气）
            event.registerBlockEntity(
                    Capabilities.FluidHandler.BLOCK,
                    ModBlockEntities.AIR_COMPRESSOR.get(),
                    (be, side) -> be.fluidHandler(side)
            );
            // 冷凝管：流体 I/O（输入压缩空气）
            event.registerBlockEntity(
                    Capabilities.FluidHandler.BLOCK,
                    ModBlockEntities.CONDENSER_PIPE.get(),
                    (be, side) -> be.fluidHandler(side)
            );
        }
    }

    // ---------- 客户端事件处理 ----------

    /**
     * 客户端专用的模组事件
     * @EventBusSubscriber 表示这个内部类订阅了 NeoForge 事件总线
     * Dist.CLIENT 表示只在客户端运行
     */
    @EventBusSubscriber(modid = MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientEvents {

        /**
         * 注册 GUI 界面
         * 当游戏注册菜单界面时，把我们的 Menu 和 Screen 绑定在一起
         */
        @SubscribeEvent
        public static void registerScreens(RegisterMenuScreensEvent event) {
            event.register(ModMenus.FLUIDIZED_BED_MENU.get(), FluidizedBedScreen::new);  // 沸腾炉物品控制器
            event.register(ModMenus.ELECTROLYZER_MENU.get(), ElectrolyzerScreen::new);
        }

        /**
         * 注册温度计的"活指针"属性（模仿指南针 angle）
         * 属性值 = 环境温度 0~1，模型按值切换 16 帧指针贴图
         */
        @SubscribeEvent
        public static void registerItemProperties(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
            event.enqueueWork(() -> {
                net.minecraft.client.renderer.item.ItemProperties.register(
                        ModItems.THERMOMETER.get(),
                        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MOD_ID, "temperature"),
                        (stack, level, entity, seed) -> {
                            // 没有可用的实体/世界时返回中间值（指针居中）
                            if (level == null || entity == null) {
                                return 0.5F;
                            }
                            return com.cxy.chemical_industry.item.ThermometerItem.getTemperatureValue(level, entity.blockPosition());
                        });
            });
        }
    }
}
