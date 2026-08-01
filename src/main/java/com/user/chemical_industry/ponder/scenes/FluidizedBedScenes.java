package com.user.chemical_industry.ponder.scenes;

import com.user.chemical_industry.ChemicalIndustry;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

/**
 * 沸腾炉 Ponder 教程 — 2 幕（蓝图全部来自玩家搭建的 nbt）
 *
 * 场景 1 structure（feitenglu.nbt）：2×2×3 储罐柱整体一次性出现
 * 场景 2 use（feitenglu2.nbt，玩家最新版）：
 *   沸腾炉 2×2×3 一次性出现：(3,2,2)-(4,4,3)
 *   燃烧室 ×4 (3,1,2)(3,1,3)(4,1,2)(4,1,3)
 *   传送带喂料：水平 (1,2,1)-(5,2,1) + 垂直 (5,2,2)(5,3,2)(5,4,2) + 漏斗 (3,3,1)
 *   输入泵 (2,2,3) + 竖管 (0,0,3)(0,1,3)(0,2,3)
 *   输出泵（中层！）(4,3,1) + 管道 (4,3,0)(5,3,0)(6,3,0) + 竖管 (6,0,0)(6,1,0)(6,2,0)
 *   齿轮组：large (2,1,4)x (3,1,5)z (4,3,4)z + cogwheel (4,0,5)(3,1,4)(3,2,4)(4,4,1)(5,4,1)(5,4,4)z + shaft (5,4,3)z
 */
public class FluidizedBedScenes {

    public static void register(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        // 沸腾炉由控制器右键转化储罐柱而成，Ponder 挂在控制器物品上
        helper.forComponents(ResourceLocation.fromNamespaceAndPath(
                        ChemicalIndustry.MOD_ID, "fluidized_bed_controller"))
                .addStoryBoard("fluidized_bed/structure", FluidizedBedScenes::structure)
                .addStoryBoard("fluidized_bed/use", FluidizedBedScenes::use);
    }

    // =====================================================================
    // 场景 1：搭建（储罐柱一次性搭好）
    // =====================================================================
    public static void structure(SceneBuilder builder, SceneBuildingUtil util) {
        builder.title("fluidized_bed_structure", "搭建沸腾炉");
        builder.configureBasePlate(0, 0, 5);

        // 地板
        builder.world().showSection(util.select().fromTo(0, 0, 0, 4, 0, 4), Direction.UP);
        builder.idle(10);

        // 储罐柱 2×2×3 一次性出现（不分层）
        builder.world().showSection(util.select().fromTo(2, 1, 2, 3, 3, 3), Direction.DOWN);
        builder.idle(5);
        builder.overlay().showText(80)
                .text("先搭一个 2×2×3 的 Create 储罐柱")
                .attachKeyFrame()
                .placeNearTarget().pointAt(util.vector().topOf(2, 3, 2));
        builder.idle(90);
        builder.overlay().showText(80)
                .text("柱子越高，容量越大")
                .attachKeyFrame()
                .placeNearTarget().pointAt(util.vector().topOf(2, 3, 2));
        builder.idle(90);

        // 右键手势：控制器转化
        builder.overlay().showControls(util.vector().blockSurface(new BlockPos(2, 2, 2), Direction.UP),
                Pointing.DOWN, 40).rightClick();
        builder.idle(10);
        builder.overlay().showText(90)
                .text("手持沸腾炉控制器右键储罐，柱体就会变成沸腾炉")
                .attachKeyFrame()
                .placeNearTarget().pointAt(util.vector().topOf(2, 2, 2));
        builder.idle(100);
    }

    // =====================================================================
    // 场景 2：完整装置运行（沸腾炉整体一次性出现）
    // =====================================================================
    public static void use(SceneBuilder builder, SceneBuildingUtil util) {
        builder.title("fluidized_bed_use", "沸腾炉运行");
        builder.configureBasePlate(0, 0, 7);

        // 地板
        builder.world().showSection(util.select().fromTo(0, 0, 0, 6, 0, 5), Direction.UP);
        builder.idle(10);

        // 沸腾炉 2×2×3 一次性出现
        builder.world().showSection(util.select().fromTo(3, 2, 2, 4, 4, 3), Direction.DOWN);
        builder.idle(5);
        builder.overlay().showText(80)
                .text("转化后的沸腾炉：底层进料、中层反应、顶层出气")
                .attachKeyFrame()
                .placeNearTarget().pointAt(util.vector().topOf(3, 4, 2));
        builder.idle(90);

        // 热源（4 个燃烧室）
        builder.world().showSection(util.select().fromTo(3, 1, 2, 4, 1, 3), Direction.UP);
        builder.idle(5);
        builder.overlay().showOutline(PonderPalette.RED, new Object(),
                util.select().fromTo(3, 1, 2, 4, 1, 3), 70);
        builder.overlay().showText(70)
                .text("下方是烈焰人燃烧室——沸腾炉需要热源")
                .attachKeyFrame()
                .placeNearTarget().pointAt(util.vector().topOf(3, 1, 2));
        builder.idle(80);

        // 传送带 + 漏斗喂料
        builder.world().showSection(util.select().fromTo(1, 2, 1, 5, 2, 1)
                .add(util.select().fromTo(5, 2, 2, 5, 4, 2))
                .add(util.select().position(3, 3, 1)), Direction.NORTH);
        builder.idle(5);
        builder.overlay().showText(70)
                .text("传送带和漏斗把原料送入中层")
                .attachKeyFrame()
                .placeNearTarget().pointAt(util.vector().topOf(3, 3, 1));
        builder.idle(80);

        // 输入泵 + 竖管（水从底层进入）
        builder.world().showSection(util.select().position(2, 2, 3)
                .add(util.select().position(1, 2, 3))
                .add(util.select().fromTo(0, 0, 3, 0, 2, 3)), Direction.WEST);
        builder.idle(5);
        builder.overlay().showText(70)
                .text("液体原料（如水）从底层泵入")
                .attachKeyFrame()
                .placeNearTarget().pointAt(util.vector().topOf(2, 2, 3));
        builder.idle(80);

        // 输出泵 + 管道（中层出气）
        builder.world().showSection(util.select().position(4, 3, 1)
                .add(util.select().fromTo(4, 3, 0, 6, 3, 0))
                .add(util.select().fromTo(6, 0, 0, 6, 2, 0)), Direction.EAST);
        builder.idle(5);
        builder.overlay().showText(70)
                .text("气体产物从中层排出")
                .attachKeyFrame()
                .placeNearTarget().pointAt(util.vector().topOf(4, 3, 1));
        builder.idle(80);

        // 齿轮组 + 配方说明
        builder.world().showSection(util.select().position(2, 1, 4)
                .add(util.select().position(3, 1, 4))
                .add(util.select().position(3, 2, 4))
                .add(util.select().position(3, 1, 5))
                .add(util.select().position(4, 0, 5))
                .add(util.select().position(4, 3, 4))
                .add(util.select().position(4, 4, 1))
                .add(util.select().position(5, 4, 1))
                .add(util.select().position(5, 4, 3))
                .add(util.select().position(5, 4, 4)), Direction.SOUTH);
        builder.idle(5);
        builder.overlay().showText(80)
                .text("把硫磺粉放入中层，加热后与水反应生成硫酸")
                .attachKeyFrame()
                .placeNearTarget().pointAt(util.vector().topOf(3, 3, 2));
        builder.idle(90);

        // 传动分组（同轴同向、啮合反向，符合现实）：
        // +32：大齿轮 (2,1,4)(3,1,5)(4,3,4) + 塔顶齿轮 (4,4,1)(5,4,1) + 输出泵 (4,3,1)（同轴）
        // -32：cogwheel (3,1,4)(3,2,4)（与 (2,1,4) 啮合反向）+ (4,0,5)（与 (3,1,5) 反向）
        //      + (5,4,4)（与 (4,3,4) 反向）+ shaft (5,4,3)（同轴）+ 输入泵 (2,2,3)
        Selection plus = util.select().position(2, 1, 4)
                .add(util.select().position(3, 1, 5))
                .add(util.select().position(4, 3, 4))
                .add(util.select().position(4, 4, 1))
                .add(util.select().position(5, 4, 1))
                .add(util.select().position(4, 3, 1));
        Selection minus = util.select().position(3, 1, 4)
                .add(util.select().position(3, 2, 4))
                .add(util.select().position(4, 0, 5))
                .add(util.select().position(5, 4, 4))
                .add(util.select().position(5, 4, 3))
                .add(util.select().position(2, 2, 3));
        new CreateSceneBuilder(builder).world().setKineticSpeed(plus, 32);
        new CreateSceneBuilder(builder).world().setKineticSpeed(minus, -32);
        builder.idle(60);
    }
}
