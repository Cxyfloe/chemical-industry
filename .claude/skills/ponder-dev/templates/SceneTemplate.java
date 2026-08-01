package com.cxy.chemical_industry.ponder.scenes;

import com.cxy.chemical_industry.ChemicalIndustry;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * 机器 Ponder 场景模板
 *
 * 蓝图：assets/<modid>/ponder/<机器>/<场景>.nbt（玩家结构方块保存 or 脚本生成）
 * 翻译：lang 里 <modid>.ponder.<titleKey>.header / .text_N（不加 = 文字空白！）
 * 注册：ChemicalPonderPlugin.registerScenes 里 addStoryBoard
 */
public class SceneTemplate {

    public static void register(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        ResourceLocation item = ResourceLocation.fromNamespaceAndPath(ChemicalIndustry.MOD_ID, "机器物品注册名");
        helper.forComponents(item)
                .addStoryBoard("机器/场景", SceneTemplate::scene);
    }

    public static void scene(SceneBuilder builder, SceneBuildingUtil util) {
        // 用原生 builder（不要包 CreateSceneBuilder——它的 world() 返回 void）
        builder.title("场景key", "标题");
        builder.configureBasePlate(0, 0, 9);

        // ---- 分步展示（第 1 步：地板 + 核心方块）----
        builder.world().showSection(util.select().layer(0), Direction.UP);
        builder.idle(10);
        // 需要后续移动的区块用 showIndependentSection 拿 link
        ElementLink<WorldSectionElement> main =
                builder.world().showIndependentSection(util.select().fromTo(
                        new BlockPos(1, 1, 1), new BlockPos(3, 1, 3)), Direction.DOWN);
        builder.idle(15);

        // ---- 文字讲解 ----
        builder.overlay().showText(80)
                .text("讲解内容（lang 翻译 key: <modid>.ponder.<场景key>.text_N）")
                .placeNearTarget().pointAt(util.vector().centerOf(2, 1, 2));
        builder.idle(90);

        // ---- 鼠标手势提示 ----
        builder.overlay().showControls(util.vector().blockSurface(new BlockPos(2, 1, 2), Direction.DOWN),
                Pointing.DOWN, 40).rightClick();
        builder.idle(10);

        // ---- 高亮 ----
        builder.overlay().showOutline(PonderPalette.RED, new Object(),
                util.select().position(2, 1, 2), 80);
        builder.idle(90);

        // ---- 抬升动画（整机抬高 1 格）----
        builder.world().moveSection(main, new Vec3(0, 1, 0), 20);
        builder.idle(25);

        // ---- 动态放方块（如点燃的烈焰人燃烧室）----
        BlockState burner = BuiltInRegistries.BLOCK.get(
                        ResourceLocation.fromNamespaceAndPath("create", "blaze_burner"))
                .defaultBlockState()
                .setValue(BlazeBurnerBlock.HEAT_LEVEL, BlazeBurnerBlock.HeatLevel.KINDLED);
        builder.world().setBlock(new BlockPos(2, 1, 2), burner, false);
        builder.idle(10);

        // ---- 爆炸（破坏动画）----
        builder.world().destroyBlock(new BlockPos(2, 1, 2));
        builder.idle(10);

        builder.overlay().showText(120)
                .text("警示文字")
                .placeNearTarget().pointAt(util.vector().centerOf(2, 1, 2));
        builder.idle(130);
    }
}
