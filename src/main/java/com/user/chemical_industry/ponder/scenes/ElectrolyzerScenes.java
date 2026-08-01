package com.user.chemical_industry.ponder.scenes;

import com.user.chemical_industry.ChemicalIndustry;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * 电解槽 Ponder 教程 — 4 个独立场景（按 W 后空格翻页）
 *
 * 场景蓝图：玩家结构方块保存的 dianjiecao.nbt（5×5×6，4 个场景共用）
 * 布局（面朝西）：
 *   电解槽 3 块 y2：阳极 (3,2,1) | 中心 (3,2,2) | 阴极 (3,2,3)
 *   传动 y1：齿轮/轴/泵（z0-1 段 + z3-5 段）；接线器/漏斗 y2：(2,2,1..3)
 *
 * 设计原则（遵守分工：场景方块全部来自蓝图，代码只做展示编排）：
 *   ① 所有方块来自玩家蓝图，按分区 showSection/showIndependentSection 显示
 *   ② z2 列（shaft (2,1,2) + cogwheel y (3,1,2)）不显示——
 *      抬升后该位置就是中心下方，直接 setBlock 燃烧室（不显示=不存在，无删齿轮问题）
 *   ③ 大齿轮 (1,0,5)：底盘显示后清掉，抬升后用 setBlock 放回装置传动层下方
 *   ④ 独立元素只用于"抬升"（moveSection）；setBlock/destroyBlock 只碰主世界可见区域
 *
 * 场景：① 搭建  ② 动能与通电  ③ 放入电极  ④ 加热与警示
 */
public class ElectrolyzerScenes {

    public static void register(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        ResourceLocation electrolyzer = ResourceLocation.fromNamespaceAndPath(ChemicalIndustry.MOD_ID, "electrolyzer");
        helper.forComponents(electrolyzer)
                .addStoryBoard("electrolyzer/structure", ElectrolyzerScenes::structure)
                .addStoryBoard("electrolyzer/power", ElectrolyzerScenes::power)
                .addStoryBoard("electrolyzer/use", ElectrolyzerScenes::use)
                .addStoryBoard("electrolyzer/heat", ElectrolyzerScenes::heat);
    }

    // 关键坐标
    private static final BlockPos ANODE   = new BlockPos(3, 2, 1);
    private static final BlockPos CENTER  = new BlockPos(3, 2, 2);
    private static final BlockPos CATHODE = new BlockPos(3, 2, 3);
    /** 燃烧室位置：电解槽中间方块正下方 (3,1,2)——原来 cogwheel y 齿轮的位置，燃烧室取代齿轮 */
    private static final BlockPos BURNER_POS = new BlockPos(3, 1, 2);
    /** 大齿轮（装置一体件）：蓝图位于 (1,0,5)，随装置一起显示/抬升/转动 */
    private static final BlockPos GEAR_POS = new BlockPos(1, 0, 5);
    /** cogwheel y：中间方块正下方的传动齿轮，第 4 场景抬升后由燃烧室取代 */
    private static final BlockPos COGWHEEL_POS = new BlockPos(3, 1, 2);

    /**
     * 传动层选区：y1 齿轮/轴/泵 + y2/y3 泵 + 大齿轮 (1,0,5)。
     * 大齿轮与装置一体（显示/抬升/转动一起）；
     * 含 z2 传动杆 shaft (2,1,2)；不含 cogwheel y (3,1,2)（单独处理，第 4 场景被燃烧室取代）。
     */
    private static net.createmod.ponder.api.scene.Selection driveSelection(SceneBuildingUtil util) {
        return util.select().fromTo(1, 1, 0, 3, 1, 1)
                .add(util.select().position(2, 1, 2))       // z2 传动杆
                .add(util.select().fromTo(1, 1, 3, 3, 1, 5))
                .add(util.select().position(GEAR_POS))      // 大齿轮（装置一体件）
                .add(util.select().position(3, 2, 0))
                .add(util.select().position(3, 2, 4))
                .add(util.select().position(3, 3, 1))
                .add(util.select().position(3, 3, 2))
                .add(util.select().position(3, 3, 3));
    }

    /**
     * 公共开场：地板 + 电解槽三块。
     * 地板用 z0-4（5×5 真实地板区），大齿轮 (1,0,5) 在 z5 列、不在其中——
     * 大齿轮由 gear 元素单独显示（heat 场景），与装置一体抬升。
     */
    private static ElementLink<WorldSectionElement> showBedAndElec(SceneBuilder builder, SceneBuildingUtil util) {
        builder.world().showSection(util.select().fromTo(0, 0, 0, 4, 0, 4), Direction.UP);
        builder.idle(10);
        ElementLink<WorldSectionElement> elec =
                builder.world().showIndependentSection(util.select().fromTo(ANODE, CATHODE), Direction.DOWN);
        builder.idle(15);
        return elec;
    }

    /** create:large_cogwheel（axis=z，与蓝图一致） */
    private static BlockState largeCogwheelState() {
        return BuiltInRegistries.BLOCK.get(
                        ResourceLocation.fromNamespaceAndPath("create", "large_cogwheel"))
                .defaultBlockState()
                .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS,
                        Direction.Axis.Z);
    }

    /** 点燃的烈焰人燃烧室方块状态 */
    private static BlockState burnerState() {
        return BuiltInRegistries.BLOCK.get(
                        ResourceLocation.fromNamespaceAndPath("create", "blaze_burner"))
                .defaultBlockState()
                .setValue(BlazeBurnerBlock.HEAT_LEVEL, BlazeBurnerBlock.HeatLevel.KINDLED);
    }

    // =====================================================================
    // 场景 1：搭建
    // =====================================================================
    public static void structure(SceneBuilder builder, SceneBuildingUtil util) {
        builder.title("electrolyzer_structure", "搭建电解槽");
        builder.configureBasePlate(0, 0, 5);

        showBedAndElec(builder, util);

        // 注意：小齿轮属于传动装置，场景 1 不显示（场景 2 传动出现时才出现）

        builder.overlay().showText(80)
                .text("合法的电解槽由三个横排电解槽方块组成")
                .attachKeyFrame()
                .placeNearTarget().pointAt(util.vector().topOf(CENTER));
        builder.idle(90);
        builder.overlay().showText(80)
                .text("它有 3×1、3×2、3×3 三种规格")
                .attachKeyFrame()
                .placeNearTarget().pointAt(util.vector().topOf(CENTER));
        builder.idle(90);
    }

    // =====================================================================
    // 场景 2：动能与通电
    // =====================================================================
    public static void power(SceneBuilder builder, SceneBuildingUtil util) {
        builder.title("electrolyzer_power", "动能与通电");
        builder.configureBasePlate(0, 0, 5);

        showBedAndElec(builder, util);

        // 全套传动设施出现（齿轮/轴/动力泵/大齿轮，大齿轮在选区里）+ 中间下方小齿轮（传动装置一部分，一起出现）
        builder.world().showSection(driveSelection(util), Direction.SOUTH);
        builder.world().showSection(util.select().position(COGWHEEL_POS), Direction.DOWN);
        builder.idle(15);

        builder.overlay().showText(90)
                .text("流体物质需要通过动力泵输入和输出")
                .attachKeyFrame()
                .placeNearTarget().pointAt(util.vector().topOf(3, 1, 1));
        builder.idle(100);

        // 接线器 + 漏斗
        builder.world().showSection(util.select().fromTo(2, 2, 1, 2, 2, 3), Direction.WEST);
        builder.idle(15);

        builder.overlay().showText(90)
                .text("电解槽需要通电才能工作")
                .attachKeyFrame()
                .placeNearTarget().pointAt(util.vector().topOf(2, 2, 1));
        builder.idle(100);

        // 电极位置提示（蓝=阳极，红=阴极），同时出现
        builder.overlay().showText(60)
                .text("阳极")
                .colored(PonderPalette.BLUE)
                .attachKeyFrame()
                .placeNearTarget().pointAt(util.vector().topOf(ANODE));
        builder.overlay().showText(60)
                .text("阴极")
                .colored(PonderPalette.RED)
                .placeNearTarget().pointAt(util.vector().topOf(CATHODE));
        builder.idle(70);

        // 整个装置跑起来：按传动关系分配旋转方向（相邻啮合反向，符合现实）
        // x2 列（cogwheel (2,1,0/4/5) + shaft）与大齿轮 (1,0,5) 啮合 → -32
        // 大齿轮、cogwheel (3,1,0/4/5)、cogwheel y → +32
        // 动力泵与驱动齿轮反向 → -32
        Selection minus = util.select().fromTo(2, 1, 0, 2, 1, 5);
        // 动力泵组（y1/y2 与齿轮反向）；顶部 y3 组（pump + cogwheel y）与中间 cogwheel y 同向
        Selection pumps = util.select().position(3, 1, 1)
                .add(util.select().position(3, 1, 3))
                .add(util.select().position(3, 2, 0))
                .add(util.select().position(3, 2, 4));
        // 顶部 y3 三个方块：中间 cogwheel y 与两侧泵视觉"啮合" → 反向转
        // 注：它们下方是电解槽、侧面泵不参与齿轮啮合，蓝图里无真实传动连接，
        //     Ponder 强制转动，方向按"齿轮带动两侧泵"的视觉直觉设定
        Selection topCog = util.select().position(3, 3, 2);          // 顶部 cogwheel y（中间）
        Selection topPumps = util.select().position(3, 3, 1)
                .add(util.select().position(3, 3, 3));               // 顶部两侧动力泵
        Selection plus = driveSelection(util).substract(minus).substract(pumps)
                .substract(topCog).substract(topPumps);
        new CreateSceneBuilder(builder).world().setKineticSpeed(plus, 32);
        new CreateSceneBuilder(builder).world().setKineticSpeed(minus, -32);
        new CreateSceneBuilder(builder).world().setKineticSpeed(pumps, -32);
        new CreateSceneBuilder(builder).world().setKineticSpeed(topCog, 32);
        new CreateSceneBuilder(builder).world().setKineticSpeed(topPumps, -32);
        builder.idle(60);
    }

    // =====================================================================
    // 场景 3：放入电极
    // =====================================================================
    public static void use(SceneBuilder builder, SceneBuildingUtil util) {
        builder.title("electrolyzer_use", "放入电极");
        builder.configureBasePlate(0, 0, 5);

        showBedAndElec(builder, util);

        // 注意：小齿轮属于传动装置，场景 3 不显示

        // 鼠标手势：右键中心方块
        builder.overlay().showControls(util.vector().blockSurface(CENTER, Direction.UP), Pointing.DOWN, 40)
                .rightClick();
        builder.idle(10);

        builder.overlay().showText(80)
                .text("电解槽需要电极")
                .attachKeyFrame()
                .placeNearTarget().pointAt(util.vector().topOf(CENTER));
        builder.idle(90);
        builder.overlay().showText(80)
                .text("有时需要催化剂和别的一些物质")
                .attachKeyFrame()
                .placeNearTarget().pointAt(util.vector().topOf(CENTER));
        builder.idle(90);
    }

    // =====================================================================
    // 场景 4：加热与警示（纯显示：燃烧室搭在蓝图里，最后出现）
    // 专用蓝图 dianjiecaoheat.nbt（用户搭：cogwheel y 换成点燃的烈焰人燃烧室 (3,1,2)）
    // 用户结构直接放地上（y0 起）→ 代码整体 +1y 显示
    // =====================================================================
    public static void heat(SceneBuilder builder, SceneBuildingUtil util) {
        builder.title("electrolyzer_heat", "加热与警示");
        builder.configureBasePlate(0, 0, 5);

        // 地板（y1，用户结构 +1）
        builder.world().showSection(util.select().fromTo(0, 1, 0, 4, 1, 4), Direction.UP);
        builder.idle(10);

        // 电解槽（y3）
        builder.world().showSection(util.select().fromTo(3, 3, 1, 3, 3, 3), Direction.DOWN);
        builder.idle(15);

        // 传动（y2 层避开 z2 燃烧室列：z0-1 + z3-5 + z2 传动杆；大齿轮 y1；泵 y3/y4；漏斗 y3）
        builder.world().showSection(util.select().fromTo(1, 2, 0, 3, 2, 1)
                .add(util.select().fromTo(1, 2, 3, 3, 2, 5))
                .add(util.select().position(2, 2, 2))       // z2 传动杆
                .add(util.select().position(1, 1, 5))       // 大齿轮（装置一体件）
                .add(util.select().position(3, 3, 0))       // 泵 y3
                .add(util.select().position(3, 3, 4))
                .add(util.select().fromTo(3, 4, 1, 3, 4, 3)) // 顶部泵 + cogwheel y（y4）
                .add(util.select().position(2, 3, 2)),      // 漏斗
                Direction.SOUTH);
        builder.idle(15);

        // 点燃的烈焰人燃烧室最后出现（蓝图自带，seething 点燃状态）——100% 稳定显示
        builder.world().showSection(util.select().position(3, 2, 2), Direction.UP);
        builder.idle(15);

        builder.overlay().showOutline(PonderPalette.RED, new Object(),
                util.select().position(3, 2, 2), 80);
        builder.overlay().showText(90)
                .text("有时电解槽需要加热才能运行")
                .attachKeyFrame()
                .placeNearTarget().pointAt(util.vector().topOf(3, 2, 2));
        builder.idle(100);

        // 爆炸：整个装置（含燃烧室）全部炸毁
        for (int x = 1; x <= 3; x++) {
            for (int y = 2; y <= 4; y++) {
                for (int z = 0; z <= 5; z++) {
                    builder.world().destroyBlock(new BlockPos(x, y, z));
                }
            }
        }
        builder.idle(10);

        builder.overlay().showText(120)
                .text("注意：电解千万条，安全第一条！")
                .attachKeyFrame()
                .placeNearTarget().pointAt(util.vector().topOf(3, 3, 2));
        builder.idle(130);
    }
}
