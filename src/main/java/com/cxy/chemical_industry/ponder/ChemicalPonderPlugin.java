package com.cxy.chemical_industry.ponder;

import com.cxy.chemical_industry.ChemicalIndustry;
import com.cxy.chemical_industry.ponder.scenes.AirCompressorScenes;
import com.cxy.chemical_industry.ponder.scenes.CondenserPipeScenes;
import com.cxy.chemical_industry.ponder.scenes.ElectrolyzerScenes;
import com.cxy.chemical_industry.ponder.scenes.FluidizedBedScenes;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.resources.ResourceLocation;

/**
 * 本模组的 Ponder 教程插件
 *
 * 实现 Ponder 库的 PonderPlugin 接口，注册所有机器的教程场景。
 * 在客户端启动时通过 PonderIndex.addPlugin() 注册（见 ChemicalIndustry.ClientEvents）。
 */
public class ChemicalPonderPlugin implements PonderPlugin {

    @Override
    public String getModId() {
        return ChemicalIndustry.MOD_ID;
    }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        ElectrolyzerScenes.register(helper);
        FluidizedBedScenes.register(helper);
        AirCompressorScenes.register(helper);
        CondenserPipeScenes.register(helper);
    }
}
