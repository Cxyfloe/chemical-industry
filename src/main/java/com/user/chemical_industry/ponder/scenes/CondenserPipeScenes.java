package com.user.chemical_industry.ponder.scenes;

import com.user.chemical_industry.ChemicalIndustry;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;

/**
 * 冷凝管 Ponder 教程 — 1 幕（蓝图 = 玩家 lengningguan1.nbt 水版）
 *
 * 布局（6×5×5）：
 *   冷凝管 (2,1,3) facing=west（冷风朝西）+ 鼓风机 (3,1,3) facing=west（背面）
 *   铁栏水池：iron_bars (0,1,2)(0,1,3)(0,1,4)(1,1,2)(1,1,4) + 水源 (1,1,3)
 *   供气：压缩机 (3,1,1) + 泵 (2,1,1) + 管道环 (1,1,0)(1,1,1)(2,1,0)(2,2,0..3)
 *   齿轮：cogwheel (2,1,2)(4,1,1)(4,1,2)(4,1,3)(5,1,1)x + shaft (3,1,2)x + 大齿轮 (5,0,2)x
 *
 * 结冰动画：水用普通 showSection（不擦除世界渲染）→ setBlock 冰块直接覆盖水源——
 *   世界渲染实时读 blocks map，冰块 100% 显示（此前用 showIndependentSection 会调用
 *   baseWorldSection.erase() 把该位置从世界渲染永久擦除，导致 setBlock 的冰不可见）
 *
 * 鼓风机朝向：speed>0 朝 facing 吹（Create EncasedFanBlockEntity 机制），+32 = 吹向冷凝管
 */
public class CondenserPipeScenes {

    public static void register(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        helper.forComponents(ResourceLocation.fromNamespaceAndPath(
                        ChemicalIndustry.MOD_ID, "condenser_pipe"))
                .addStoryBoard("condenser_pipe/use", CondenserPipeScenes::use);
    }

    // =====================================================================
    // 场景 1：完整流程 + 水结冰
    // =====================================================================
    public static void use(SceneBuilder builder, SceneBuildingUtil util) {
        builder.title("condenser_pipe_use", "冷凝管");
        builder.configureBasePlate(0, 0, 6);

        // 地板 5×5（不含 x5 列大齿轮——电解槽模板）
        builder.world().showSection(util.select().fromTo(0, 0, 0, 4, 0, 4), Direction.UP);
        builder.idle(10);

        // 冷凝管本体
        builder.world().showSection(util.select().position(2, 1, 3), Direction.DOWN);
        builder.idle(5);
        builder.overlay().showText(70)
                .text("冷凝管向前方吹出冷风")
                .attachKeyFrame()
                .placeNearTarget().pointAt(util.vector().topOf(2, 1, 3));
        builder.idle(80);

        // 背面鼓风机（speed>0 = 朝 facing 吹风 → 吹向冷凝管）
        builder.world().showSection(util.select().position(3, 1, 3), Direction.SOUTH);
        builder.idle(5);
        builder.overlay().showText(70)
                .text("背面必须有鼓风机，它朝冷凝管吹风")
                .attachKeyFrame()
                .placeNearTarget().pointAt(util.vector().topOf(3, 1, 3));
        builder.idle(80);

        // 供气系统：压缩机 + 齿轮组 + 大齿轮（装置一体）+ 泵 + 管道环
        builder.world().showSection(util.select().position(3, 1, 1)
                .add(util.select().position(3, 1, 2))
                .add(util.select().position(2, 1, 2))
                .add(util.select().fromTo(4, 1, 1, 5, 1, 1))
                .add(util.select().position(4, 1, 2))
                .add(util.select().position(5, 0, 2)), Direction.WEST);
        builder.idle(5);
        builder.world().showSection(util.select().position(2, 1, 1)
                .add(util.select().position(1, 1, 0))
                .add(util.select().position(1, 1, 1))
                .add(util.select().position(2, 1, 0))
                .add(util.select().fromTo(2, 2, 0, 2, 2, 3)), Direction.WEST);
        builder.idle(5);
        builder.overlay().showText(70)
                .text("压缩空气由空气压缩机提供，经管道绕行泵入")
                .attachKeyFrame()
                .placeNearTarget().pointAt(util.vector().topOf(2, 1, 1));
        builder.idle(80);

        // 铁栏水池 + 水源（普通显示，不擦除世界渲染——结冰时 setBlock 才能显示）
        builder.world().showSection(util.select().fromTo(0, 1, 2, 0, 1, 4)
                .add(util.select().position(1, 1, 2))
                .add(util.select().position(1, 1, 4))
                .add(util.select().position(1, 1, 3)), Direction.WEST);
        builder.idle(10);
        builder.overlay().showText(70)
                .text("前方有水源时，冷风会把它冻成冰")
                .attachKeyFrame()
                .placeNearTarget().pointAt(util.vector().topOf(1, 1, 3));
        builder.idle(80);

        // 传动分组（同轴同向、啮合反向，符合现实）：
        // +32 同轴网络：压缩机 (3,1,1) + cogwheel (4,1,1)(5,1,1) + 泵 (2,1,1) + cogwheel (4,1,3)
        // -32 啮合网络：cogwheel (2,1,2)(4,1,2) + shaft (3,1,2) + 大齿轮 (5,0,2)
        // 鼓风机 (3,1,3)：-32 —— 负转速经 convertToDirection 后朝 facing 吹（吹向冷凝管）
        Selection plus = util.select().position(3, 1, 1)
                .add(util.select().position(4, 1, 1))
                .add(util.select().position(5, 1, 1))
                .add(util.select().position(2, 1, 1))
                .add(util.select().position(4, 1, 3));
        Selection minus = util.select().position(2, 1, 2)
                .add(util.select().position(4, 1, 2))
                .add(util.select().position(3, 1, 2))
                .add(util.select().position(5, 0, 2));
        Selection fan = util.select().position(3, 1, 3);
        new CreateSceneBuilder(builder).world().setKineticSpeed(plus, 32);
        new CreateSceneBuilder(builder).world().setKineticSpeed(minus, -32);
        new CreateSceneBuilder(builder).world().setKineticSpeed(fan, -32);
        builder.idle(40);

        // 结冰：setBlock 冰块直接覆盖水源（世界渲染实时读 blocks map → 冰可见）
        builder.world().setBlock(new BlockPos(1, 1, 3), Blocks.ICE.defaultBlockState(), false);
        builder.idle(20);

        builder.overlay().showText(90)
                .text("4 秒后，水结成冰！")
                .attachKeyFrame()
                .placeNearTarget().pointAt(util.vector().topOf(1, 1, 3));
        builder.idle(100);
    }
}
