package com.user.chemical_industry.ponder.scenes;

import com.user.chemical_industry.ChemicalIndustry;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

/**
 * 空气压缩机 Ponder 教程 — 2 幕（蓝图全部来自玩家搭建的 nbt）
 *
 * 模板 = 电解槽模式：地板选区 5×5（fromTo(0,0,0,4,0,4)）不含 x5 列大齿轮；
 *   大齿轮属于"装置选区"（跟齿轮组一起 showSection、一起 setKineticSpeed），不附着在底盘上。
 *
 * 场景 1 use（yasuoji.nbt）：压缩机 (3,1,1) facing=west + 齿轮组 + 泵 + 储罐柱
 *   齿轮：cogwheel (2,1,2)(4,1,1)(4,1,2)(5,1,1)x + shaft (3,1,2)x + 大齿轮 (5,0,2)x（装置一体）
 *   泵 (2,1,1) facing=west 抽压缩机 → 管道 (1,1,1)(1,1,2) → 储罐 (1,1,3)(1,2,3)
 *
 * 场景 2 distillation（yasuoji2.nbt = 玩家空气分馏装置）：
 *   压缩机 (3,1,1) + 齿轮组 + 大齿轮 + 泵 (2,1,1) + 管道环 (1,1,0)(1,1,1)(2,1,0)(2,2,0..3)
 *   + 冷凝管 (2,1,3) + 鼓风机 (3,1,3) + 铁栏水池 (0,1,2..4)(1,1,2)(1,1,4) + 水 (1,1,3)
 */
public class AirCompressorScenes {

    public static void register(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        helper.forComponents(ResourceLocation.fromNamespaceAndPath(
                        ChemicalIndustry.MOD_ID, "air_compressor"))
                .addStoryBoard("air_compressor/use", AirCompressorScenes::use)
                .addStoryBoard("air_compressor/distillation", AirCompressorScenes::distillation);
    }

    /** 地板 5×5（不含 x5 列大齿轮——电解槽模板） */
    private static void showFloor(SceneBuilder builder, SceneBuildingUtil util) {
        builder.world().showSection(util.select().fromTo(0, 0, 0, 4, 0, 4), Direction.UP);
        builder.idle(10);
    }

    // =====================================================================
    // 场景 1：空气压缩机（单独，yasuoji.nbt）
    // =====================================================================
    public static void use(SceneBuilder builder, SceneBuildingUtil util) {
        builder.title("air_compressor_use", "空气压缩机");
        builder.configureBasePlate(0, 0, 6);

        showFloor(builder, util);

        // 压缩机本体
        builder.world().showSection(util.select().position(3, 1, 1), Direction.DOWN);
        builder.idle(5);
        builder.overlay().showText(70)
                .text("空气压缩机把空气压缩成压缩空气")
                .attachKeyFrame()
                .placeNearTarget().pointAt(util.vector().topOf(3, 1, 1));
        builder.idle(80);

        // 装置一体选区：齿轮组 + 大齿轮 (5,0,2)（大齿轮不属于地板，属于装置）
        builder.world().showSection(util.select().position(2, 1, 2)
                .add(util.select().position(3, 1, 2))
                .add(util.select().fromTo(4, 1, 1, 5, 1, 1))
                .add(util.select().position(4, 1, 2))
                .add(util.select().position(5, 0, 2)), Direction.SOUTH);
        builder.idle(5);
        builder.overlay().showText(70)
                .text("它是动能机器：齿轮驱动，轴从背面接入")
                .attachKeyFrame()
                .placeNearTarget().pointAt(util.vector().topOf(3, 1, 2));
        builder.idle(80);

        // 泵 + 管道 + 储罐
        builder.world().showSection(util.select().position(2, 1, 1)
                .add(util.select().fromTo(1, 1, 1, 1, 1, 2))
                .add(util.select().fromTo(1, 1, 3, 1, 2, 3)), Direction.WEST);
        builder.idle(5);
        builder.overlay().showText(70)
                .text("用动力泵把压缩空气送入储罐")
                .attachKeyFrame()
                .placeNearTarget().pointAt(util.vector().topOf(2, 1, 1));
        builder.idle(80);

        // 传动分组（同轴同向、啮合反向，符合现实）：
        // +32 同轴网络：压缩机 (3,1,1) + cogwheel (4,1,1)(5,1,1) + 泵 (2,1,1)
        // -32 啮合网络：cogwheel (2,1,2)(4,1,2) + shaft (3,1,2) + 大齿轮 (5,0,2)
        Selection plus = util.select().position(3, 1, 1)
                .add(util.select().position(4, 1, 1))
                .add(util.select().position(5, 1, 1))
                .add(util.select().position(2, 1, 1));
        Selection minus = util.select().position(2, 1, 2)
                .add(util.select().position(4, 1, 2))
                .add(util.select().position(3, 1, 2))
                .add(util.select().position(5, 0, 2));
        new CreateSceneBuilder(builder).world().setKineticSpeed(plus, 32);
        new CreateSceneBuilder(builder).world().setKineticSpeed(minus, -32);
        builder.overlay().showText(70)
                .text("转速越高，产气越快")
                .attachKeyFrame()
                .placeNearTarget().pointAt(util.vector().topOf(3, 1, 1));
        builder.idle(80);
    }

    // =====================================================================
    // 场景 2：空气分馏（蓝图 = 玩家新版 yasuoji2.nbt 分馏塔装置）
    // 布局：压缩机 (3,1,1) facing=west + 齿轮组 + 大齿轮 (5,0,2)（装置一体，不贴底盘）
    //   泵 (2,1,1) + 管道 (1,1,1) → 4 层蒸馏塔 (1,1..4,2)
    //   输出泵 (1,2..4,3) facing=south + 输出管道 (1,2..4,4)(1,3,5)(0,2,4)(0,2,5)(2,4,4)(2,4,5)
    // =====================================================================
    public static void distillation(SceneBuilder builder, SceneBuildingUtil util) {
        builder.title("air_compressor_distillation", "空气分馏");
        builder.configureBasePlate(0, 0, 6);

        showFloor(builder, util);

        // 装置一体选区：压缩机组 + 齿轮组 + 齿轮箱 + 大齿轮（玩家新布局）
        builder.world().showSection(util.select().position(3, 1, 1)
                .add(util.select().position(3, 1, 2))   // gearbox 齿轮箱（垂直传动）
                .add(util.select().position(3, 1, 3))   // large_cogwheel（齿轮箱 north 输出）
                .add(util.select().position(2, 1, 2))
                .add(util.select().fromTo(4, 1, 1, 5, 1, 1))
                .add(util.select().position(4, 1, 2))
                .add(util.select().position(5, 0, 2)), Direction.WEST);
        builder.idle(5);
        builder.overlay().showText(70)
                .text("空气压缩机产生压缩空气")
                .attachKeyFrame()
                .placeNearTarget().pointAt(util.vector().topOf(3, 1, 1));
        builder.idle(80);

        // 输入泵 + 管道（压缩空气 → 塔底层）
        builder.world().showSection(util.select().position(2, 1, 1)
                .add(util.select().position(1, 1, 1)), Direction.WEST);
        builder.idle(5);
        builder.overlay().showText(70)
                .text("压缩空气经管道泵入分馏塔底层")
                .attachKeyFrame()
                .placeNearTarget().pointAt(util.vector().topOf(2, 1, 1));
        builder.idle(80);

        // 分馏塔整体一次出现（不逐层搭建）
        builder.world().showSection(util.select().fromTo(1, 1, 2, 1, 4, 2), Direction.DOWN);
        builder.idle(5);
        builder.overlay().showText(70)
                .text("分馏塔由模组《机械动力：柴油动力》提供")
                .attachKeyFrame()
                .placeNearTarget().pointAt(util.vector().topOf(1, 4, 2));
        builder.idle(80);
        builder.overlay().showText(70)
                .text("分离三种气体至少需要 4 层")
                .attachKeyFrame()
                .placeNearTarget().pointAt(util.vector().topOf(1, 2, 2));
        builder.idle(80);

        // 三个输出泵（第 2/3/4 层抽气）+ 传动齿轮 + 输出管道
        builder.world().showSection(util.select().position(1, 2, 3)
                .add(util.select().position(1, 3, 3))
                .add(util.select().position(1, 4, 3))
                .add(util.select().position(2, 2, 3))
                .add(util.select().position(2, 3, 3))
                .add(util.select().position(2, 4, 3))
                .add(util.select().position(1, 2, 4))
                .add(util.select().position(1, 3, 4))
                .add(util.select().position(1, 4, 4))
                .add(util.select().position(1, 3, 5))
                .add(util.select().position(0, 2, 4))
                .add(util.select().position(0, 2, 5))
                .add(util.select().position(2, 4, 4))
                .add(util.select().position(2, 4, 5)), Direction.SOUTH);
        builder.idle(5);
        builder.overlay().showText(80)
                .text("第 2、3、4 层分别输出氧气、氮气、稀有气体")
                .attachKeyFrame()
                .placeNearTarget().pointAt(util.vector().topOf(1, 3, 2));
        builder.idle(90);

        // 传动分组（玩家布局：齿轮箱 + 大齿轮垂直传动链）：
        // +32 同轴网络：压缩机 (3,1,1) + cogwheel (4,1,1)(5,1,1) + 输入泵 (2,1,1)
        //                + 侧面传动齿轮 (2,2..4,3)（被大齿轮 (3,1,3) 带动）
        // -32 啮合网络：cogwheel (2,1,2)(4,1,2) + gearbox (3,1,2)（与 (4,1,2) 同向）
        //                + large (3,1,3)（与 gearbox 同轴）+ 大齿轮 (5,0,2)（与 (5,1,1) 啮合反向）
        // 输出泵 ×3（用户指定方向）：第一 -32 逆时针、第二 +32 顺时针、第三 -32 逆时针
        Selection plus = util.select().position(3, 1, 1)
                .add(util.select().position(4, 1, 1))
                .add(util.select().position(5, 1, 1))
                .add(util.select().position(2, 1, 1))
                .add(util.select().position(2, 2, 3))
                .add(util.select().position(2, 3, 3))
                .add(util.select().position(2, 4, 3));
        Selection minus = util.select().position(2, 1, 2)
                .add(util.select().position(4, 1, 2))
                .add(util.select().position(3, 1, 2))
                .add(util.select().position(3, 1, 3))
                .add(util.select().position(5, 0, 2));
        Selection pump1 = util.select().position(1, 2, 3);   // 第一个输出泵（底层）：逆时针
        Selection pump2 = util.select().position(1, 3, 3);   // 第二个输出泵（中层）：顺时针
        Selection pump3 = util.select().position(1, 4, 3);   // 第三个输出泵（顶层）：逆时针
        new CreateSceneBuilder(builder).world().setKineticSpeed(plus, 32);
        new CreateSceneBuilder(builder).world().setKineticSpeed(minus, -32);
        new CreateSceneBuilder(builder).world().setKineticSpeed(pump1, -32);
        new CreateSceneBuilder(builder).world().setKineticSpeed(pump2, 32);
        new CreateSceneBuilder(builder).world().setKineticSpeed(pump3, -32);
        builder.idle(20);

        // 安全警示
        builder.overlay().showText(100)
                .text("注意：分馏空气，千万不要加热哟")
                .attachKeyFrame()
                .placeNearTarget().pointAt(util.vector().topOf(1, 3, 2));
        builder.idle(110);
    }
}
