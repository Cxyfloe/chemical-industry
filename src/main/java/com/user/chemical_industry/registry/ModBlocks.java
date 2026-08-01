package com.user.chemical_industry.registry;

import com.user.chemical_industry.ChemicalIndustry;
import com.user.chemical_industry.block.AirCompressorBlock;
import com.user.chemical_industry.block.ElectrolyzerBlock;
import com.user.chemical_industry.block.FluidizedBedBlock;
import com.user.chemical_industry.block.ReactiveMetalBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 模组方块注册表
 *
 * 根据构思文档，注册以下矿石方块：
 * - 硫磺矿 → 挖掘掉落硫磺粉
 * - 黄铁矿 → 挖掘掉落黄铁原矿
 * - 岩盐矿 → 挖掘掉落氯化钠
 * - 硝石矿 → 挖掘掉落硝酸钾
 *
 * 后续阶段将添加：
 * - 沸腾炉（功能性方块）
 */
public class ModBlocks {

    private static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(ChemicalIndustry.MOD_ID);

    // ---------- 矿石类 ----------

    /** 硫磺矿石 (S) — 黄色的硫磺晶体矿 */
    public static final DeferredBlock<Block> SULFUR_ORE = BLOCKS.register(
            "sulfur_ore",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_YELLOW)       // 黄色
                    .strength(3.0f, 3.0f)                  // 硬度、爆炸抗性
                    .requiresCorrectToolForDrops()          // 需要镐
                    .sound(SoundType.STONE))
    );

    /** 黄铁矿 (FeS₂) — 有金属光泽的金色矿石，又称"愚人金" */
    public static final DeferredBlock<Block> PYRITE_ORE = BLOCKS.register(
            "pyrite_ore",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.GOLD)               // 金色
                    .strength(3.5f, 3.5f)                  // 比硫磺稍硬
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE))
    );

    /** 岩盐矿 (NaCl) — 地下盐层中开采的氯化钠 */
    public static final DeferredBlock<Block> ROCK_SALT_ORE = BLOCKS.register(
            "rock_salt_ore",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.SNOW)               // 白色
                    .strength(3.0f, 3.0f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE))
    );

    /** 硝石矿 (KNO₃) — 硝酸钾矿物，灰白色结晶体 */
    public static final DeferredBlock<Block> NITER_ORE = BLOCKS.register(
            "niter_ore",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_WHITE)   // 灰白色
                    .strength(3.0f, 3.0f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE))
    );

    /** 银矿 (Ag) — 银白色金属矿脉 */
    public static final DeferredBlock<Block> SILVER_ORE = BLOCKS.register(
            "silver_ore",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(3.0f, 3.0f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE))
    );

    /** 稀土矿 — 含多种稀土元素的棕色矿石 */
    public static final DeferredBlock<Block> RARE_EARTH_ORE = BLOCKS.register(
            "rare_earth_ore",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_BROWN)
                    .strength(3.5f, 3.5f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE))
    );

    /** 铝土矿 — 铝的主要来源，橙棕色 */
    public static final DeferredBlock<Block> BAUXITE_ORE = BLOCKS.register(
            "bauxite_ore",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_ORANGE)
                    .strength(3.0f, 3.0f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE))
    );

    /** 石墨矿 — 碳的同素异形体，深灰色 */
    public static final DeferredBlock<Block> GRAPHITE_ORE = BLOCKS.register(
            "graphite_ore",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(2.5f, 2.5f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE))
    );

    /** 铝块 — 铝锭合成的装饰/存储方块，遇酸和碱腐蚀 */
    public static final DeferredBlock<Block> ALUMINUM_BLOCK = BLOCKS.register(
            "aluminum_block",
            () -> new ReactiveMetalBlock(ReactiveMetalBlock.ALUMINUM,
                    BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(3.0f, 4.0f)
                    .requiresCorrectToolForDrops()
                    .randomTicks()
                    .sound(SoundType.METAL))
    );

    /** 钢块 — 钢锭合成的装饰/存储方块 */
    public static final DeferredBlock<Block> STEEL_BLOCK = BLOCKS.register(
            "steel_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(5.0f, 10.0f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL))
    );

    /** 镁块 — 镁锭合成的装饰/存储方块，遇酸迅速腐蚀 */
    public static final DeferredBlock<Block> MAGNESIUM_BLOCK = BLOCKS.register(
            "magnesium_block",
            () -> new ReactiveMetalBlock(ReactiveMetalBlock.MAGNESIUM,
                    BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(2.5f, 3.0f)
                    .requiresCorrectToolForDrops()
                    .randomTicks()
                    .sound(SoundType.METAL))
    );

    /** 石墨块 — 石墨合成的装饰/存储方块，也可做燃料 */
    public static final DeferredBlock<Block> GRAPHITE_BLOCK = BLOCKS.register(
            "graphite_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(2.5f, 2.5f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE))
    );

    // ====== 深度矿石变种 ======
    private static BlockBehaviour.Properties deepslateOre() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.DEEPSLATE)
                .strength(4.5f, 3.0f).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE);
    }
    public static final DeferredBlock<Block> DEEPSLATE_SULFUR_ORE = BLOCKS.register(
            "deepslate_sulfur_ore", () -> new Block(deepslateOre()));
    public static final DeferredBlock<Block> DEEPSLATE_PYRITE_ORE = BLOCKS.register(
            "deepslate_pyrite_ore", () -> new Block(deepslateOre()));
    public static final DeferredBlock<Block> DEEPSLATE_ROCK_SALT_ORE = BLOCKS.register(
            "deepslate_rock_salt_ore", () -> new Block(deepslateOre()));
    public static final DeferredBlock<Block> DEEPSLATE_NITER_ORE = BLOCKS.register(
            "deepslate_niter_ore", () -> new Block(deepslateOre()));
    public static final DeferredBlock<Block> DEEPSLATE_SILVER_ORE = BLOCKS.register(
            "deepslate_silver_ore", () -> new Block(deepslateOre()));
    public static final DeferredBlock<Block> DEEPSLATE_RARE_EARTH_ORE = BLOCKS.register(
            "deepslate_rare_earth_ore", () -> new Block(deepslateOre()));
    public static final DeferredBlock<Block> DEEPSLATE_BAUXITE_ORE = BLOCKS.register(
            "deepslate_bauxite_ore", () -> new Block(deepslateOre()));
    public static final DeferredBlock<Block> DEEPSLATE_GRAPHITE_ORE = BLOCKS.register(
            "deepslate_graphite_ore", () -> new Block(deepslateOre()));

    // ---------- 功能性方块 ----------

    /** 沸腾炉 — 利用热源驱动化学反应的工业炉 */
    public static final DeferredBlock<FluidizedBedBlock> FLUIDIZED_BED = BLOCKS.register(
            "fluidized_bed",
            () -> new FluidizedBedBlock()  // 使用无参构造器，用默认属性
    );

    /** 电解槽 — 将工作盆用电极转化后得到的电解反应器 */
    public static final DeferredBlock<ElectrolyzerBlock> ELECTROLYZER = BLOCKS.register(
            "electrolyzer",
            () -> new ElectrolyzerBlock()
    );

    /** 空气压缩机 — 齿轮驱动，产生压缩空气 */
    public static final DeferredBlock<AirCompressorBlock> AIR_COMPRESSOR = BLOCKS.register(
            "air_compressor",
            () -> new AirCompressorBlock()
    );

    // ---------- 新矿石 ----------

    /** 方铅矿 (PbS) — 铅的主要来源，深灰色金属光泽 */
    public static final DeferredBlock<Block> GALENA_ORE = BLOCKS.register("galena_ore",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY)
                    .strength(3.5f, 3.5f).requiresCorrectToolForDrops().sound(SoundType.STONE)));
    public static final DeferredBlock<Block> DEEPSLATE_GALENA_ORE = BLOCKS.register("deepslate_galena_ore",
            () -> new Block(deepslateOre()));
    /** 锡石 (SnO₂) — 锡的主要来源，暗棕色 */
    public static final DeferredBlock<Block> CASSITERITE_ORE = BLOCKS.register("cassiterite_ore",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_BROWN)
                    .strength(3.5f, 3.5f).requiresCorrectToolForDrops().sound(SoundType.STONE)));
    public static final DeferredBlock<Block> DEEPSLATE_CASSITERITE_ORE = BLOCKS.register("deepslate_cassiterite_ore",
            () -> new Block(deepslateOre()));
    /** 朱砂 (HgS) — 硫化汞，鲜红色，加热分解得到水银 */
    public static final DeferredBlock<Block> CINNABAR_ORE = BLOCKS.register("cinnabar_ore",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED)
                    .strength(3.0f, 3.0f).requiresCorrectToolForDrops().sound(SoundType.STONE)));
    public static final DeferredBlock<Block> DEEPSLATE_CINNABAR_ORE = BLOCKS.register("deepslate_cinnabar_ore",
            () -> new Block(deepslateOre()));

    // ---------- 金属块 ----------

    public static final DeferredBlock<Block> LEAD_BLOCK = BLOCKS.register("lead_block",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY)
                    .strength(3.5f, 4.0f).requiresCorrectToolForDrops().sound(SoundType.METAL)));
    public static final DeferredBlock<Block> TIN_BLOCK = BLOCKS.register("tin_block",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(3.0f, 3.5f).requiresCorrectToolForDrops().sound(SoundType.METAL)));
    public static final DeferredBlock<Block> BRONZE_BLOCK = BLOCKS.register("bronze_block",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE)
                    .strength(4.0f, 5.0f).requiresCorrectToolForDrops().sound(SoundType.METAL)));
    /** 银块 — 银锭存储方块，银白色贵金属 */
    public static final DeferredBlock<Block> SILVER_BLOCK = BLOCKS.register("silver_block",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(3.5f, 4.0f).requiresCorrectToolForDrops().sound(SoundType.METAL)));
    /** 硬铝块 — 硬铝锭存储方块，轻质高强度合金 */
    public static final DeferredBlock<Block> DURALUMIN_BLOCK = BLOCKS.register("duralumin_block",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .strength(4.0f, 5.0f).requiresCorrectToolForDrops().sound(SoundType.METAL)));

    // ---------- 功能性方块 ----------

    /** 冷凝管 — 消耗压缩空气，将鼓风机气流变为冷风 */
    public static final DeferredBlock<com.user.chemical_industry.block.CondenserPipeBlock> CONDENSER_PIPE = BLOCKS.register(
            "condenser_pipe", com.user.chemical_industry.block.CondenserPipeBlock::new);

    // ---------- 作物 ----------

    /** 罂粟 — 开花植物，3 阶段生长，成熟收获罂粟果实（无 BlockItem，只能种子种植） */
    public static final DeferredBlock<com.user.chemical_industry.block.OpiumPoppyBlock> OPIUM_POPPY = BLOCKS.register(
            "opium_poppy", com.user.chemical_industry.block.OpiumPoppyBlock::new);

    /**
     * 向 NeoForge 事件总线注册所有方块
     */
    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
