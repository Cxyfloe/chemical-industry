package com.user.chemical_industry.registry;

import com.user.chemical_industry.ChemicalIndustry;
import com.user.chemical_industry.block.FluidizedBedBlock;
import com.user.chemical_industry.block_entity.AirCompressorBlockEntity;
import com.user.chemical_industry.block_entity.CondenserPipeBlockEntity;
import com.user.chemical_industry.block_entity.ElectrolyzerBlockEntity;
import com.user.chemical_industry.block_entity.FluidizedBedBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * 模组方块实体类型注册表
 *
 * 【什么是 BlockEntity（方块实体）？】
 * 普通的方块（如石头）是"死"的——它们只有外观，没有内部数据。
 * 方块实体给方块加上了"大脑"，让它能：
 * - 存储物品（像箱子）
 * - 每 tick 执行逻辑（像熔炉烧东西）
 * - 记住状态（如加工进度）
 *
 * 每个需要 GUI 或内部存储的方块都需要注册一个 BlockEntityType。
 */
public class ModBlockEntities {

    /** 延迟注册器 — NeoForge 的标准注册方式 */
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, ChemicalIndustry.MOD_ID);

    /**
     * 沸腾炉方块实体类型
     *
     * BlockEntityType.Builder 的参数含义：
     * - FluidizedBedBlockEntity::new  — 如何创建方块实体实例
     * - ModBlocks.FLUIDIZED_BED.get() — 这个实体类型属于哪个方块
     */
    public static final Supplier<BlockEntityType<FluidizedBedBlockEntity>> FLUIDIZED_BED =
            BLOCK_ENTITIES.register("fluidized_bed",
                    () -> BlockEntityType.Builder.of(
                            FluidizedBedBlockEntity::new,
                            ModBlocks.FLUIDIZED_BED.get()
                    ).build(null)
            );

    /**
     * 电解槽方块实体类型
     * 用于存储电极物品，后续扩展为存储流体和电能
     */
    public static final Supplier<BlockEntityType<ElectrolyzerBlockEntity>> ELECTROLYZER =
            BLOCK_ENTITIES.register("electrolyzer",
                    () -> BlockEntityType.Builder.of(
                            ElectrolyzerBlockEntity::new,
                            ModBlocks.ELECTROLYZER.get()
                    ).build(null)
            );

    public static final Supplier<BlockEntityType<AirCompressorBlockEntity>> AIR_COMPRESSOR =
            BLOCK_ENTITIES.register("air_compressor",
                    () -> BlockEntityType.Builder.of(
                            AirCompressorBlockEntity::new,
                            ModBlocks.AIR_COMPRESSOR.get()
                    ).build(null)
            );

    public static final Supplier<BlockEntityType<CondenserPipeBlockEntity>> CONDENSER_PIPE =
            BLOCK_ENTITIES.register("condenser_pipe",
                    () -> BlockEntityType.Builder.of(
                            CondenserPipeBlockEntity::new,
                            ModBlocks.CONDENSER_PIPE.get()
                    ).build(null)
            );

    /**
     * 向事件总线注册所有方块实体类型
     */
    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
