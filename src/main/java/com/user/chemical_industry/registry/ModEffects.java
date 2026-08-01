package com.user.chemical_industry.registry;

import com.user.chemical_industry.ChemicalIndustry;
import com.user.chemical_industry.effect.CorrosionEffect;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * 自定义状态效果注册表
 */
public class ModEffects {

    private static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(BuiltInRegistries.MOB_EFFECT, ChemicalIndustry.MOD_ID);

    /** 腐蚀效果 — 化学品接触导致护甲损坏 + 扣血 */
    public static final Supplier<MobEffect> CORROSION = EFFECTS.register(
            "corrosion", CorrosionEffect::new);

    /** 返回腐蚀效果的 Holder（MobEffectInstance 构造函数需要） */
    public static net.minecraft.core.Holder<MobEffect> corrosionHolder() {
        return net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.wrapAsHolder(CORROSION.get());
    }

    public static void register(IEventBus bus) {
        EFFECTS.register(bus);
    }
}
