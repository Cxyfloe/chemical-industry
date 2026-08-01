package com.user.chemical_industry.fluid;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidType;

import java.util.function.Consumer;

/**
 * 化学流体类型 — 为自定义流体指定贴图路径
 *
 * NeoForge 1.21.1 中，流体贴图通过 initializeClient() 注册，
 * 使用 IClientFluidTypeExtensions 返回贴图路径。
 * 不这样做的话渲染时会空指针崩溃。
 */
public class ChemicalFluidType extends FluidType {

    /** 静态流体贴图（源方块的颜色） */
    private final ResourceLocation stillTexture;
    /** 流动流体贴图 */
    private final ResourceLocation flowingTexture;

    /**
     * @param properties     流体物理属性
     * @param stillTexture   静态贴图路径
     * @param flowingTexture 流动贴图路径
     */
    public ChemicalFluidType(Properties properties,
                             ResourceLocation stillTexture,
                             ResourceLocation flowingTexture) {
        super(properties);
        this.stillTexture = stillTexture;
        this.flowingTexture = flowingTexture;
    }

    /**
     * 向客户端注册流体渲染扩展
     * 这是 NeoForge 1.21.1 中设置自定义流体外观的标准方式
     */
    @Override
    public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
        consumer.accept(new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return stillTexture;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return flowingTexture;
            }

            @Override
            public int getTintColor() {
                // 0xFFFFFFFF = 白色（不染色，使用贴图原色）
                return 0xFFFFFFFF;
            }
        });
    }
}
