package com.cxy.chemical_industry.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.joml.Vector3f;

import java.util.List;

/**
 * 储气罐 — 储存与运输气体（氯气、氢气）
 *
 * 【使用方式】
 * - 空罐右键 Create 流体储罐 → 从储罐中装气
 * - 满罐右键 Create 流体储罐 → 向储罐中放气
 * - 满罐右键普通方块 → 释放气体（彩色粒子效果）
 * - 最大容量：1000mB
 */
public class GasCanisterItem extends Item {

    public static final int MAX_GAS = 1000; // 最大储气量 (mB)

    public GasCanisterItem() {
        super(new Properties().stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        Player player = ctx.getPlayer();
        ItemStack stack = ctx.getItemInHand();
        if (player == null) return InteractionResult.PASS;

        String gasType = getGasType(stack);

        if (gasType == null) {
            // 空罐 → 尝试从储罐装气
            if (!level.isClientSide()) {
                IFluidHandler tank = level.getCapability(
                        Capabilities.FluidHandler.BLOCK, pos, ctx.getClickedFace());
                if (tank != null) {
                    for (int i = 0; i < tank.getTanks(); i++) {
                        FluidStack fs = tank.getFluidInTank(i);
                        String key = fs.getFluid().builtInRegistryHolder().key().location().toString();
                        if (isGasFluid(key)) {
                            int drain = Math.min(fs.getAmount(), MAX_GAS);
                            FluidStack drained = tank.drain(
                                    new FluidStack(fs.getFluid(), drain),
                                    IFluidHandler.FluidAction.EXECUTE);
                            if (!drained.isEmpty()) {
                                setGas(stack, key, drained.getAmount());
                                // 成就：第一次获得稀有气体
                                if (key.endsWith("rare_gas"))
                                    com.cxy.chemical_industry.event.AdvancementHelper.grantNearby(level, pos, "rare_gas", "rare");
                                return InteractionResult.SUCCESS;
                            }
                        }
                    }
                }
            }
            return InteractionResult.SUCCESS;
        } else {
            // 有气 → 右键储罐放气 或 右键方块释放
            if (!level.isClientSide()) {
                IFluidHandler tank = level.getCapability(
                        Capabilities.FluidHandler.BLOCK, pos, ctx.getClickedFace());
                if (tank != null) {
                    // 尝试向储罐放气
                    int ci = gasType.indexOf(':');
                    var fluidKey = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                        gasType.substring(0, ci), gasType.substring(ci+1));
                    net.minecraft.world.level.material.Fluid targetFluid =
                        net.minecraft.core.registries.BuiltInRegistries.FLUID.get(fluidKey);
                    if (targetFluid == null) return InteractionResult.SUCCESS;
                    int filled = tank.fill(new FluidStack(targetFluid, getGasAmount(stack)),
                            IFluidHandler.FluidAction.EXECUTE);
                    if (filled > 0) {
                        int remaining = getGasAmount(stack) - filled;
                        setGas(stack, remaining > 0 ? gasType : null, remaining);
                        return InteractionResult.SUCCESS;
                    }
                }
                // 储罐放不了 → 直接释放到空气
                releaseGas(level, pos, gasType, getGasAmount(stack));
                setGas(stack, null, 0);
            }
            return InteractionResult.SUCCESS;
        }
    }

    /** 释放气体：每种气体不同颜色粒子 + 特定效果 */
    public static void releaseGas(Level level, BlockPos pos, String gas, int amount) {
        if (!(level instanceof ServerLevel sl)) return;
        float r, g, b;
        boolean poison = false, corrode = false;
        switch (gas != null ? gas : "") {
            case "chemical_industry:chlorine_gas":    r=0.3f; g=1.0f; b=0.3f; corrode=true; poison=true; break;
            case "chemical_industry:ammonia_gas":     r=0.8f; g=1.0f; b=0.5f; poison=true; break;
            case "chemical_industry:carbon_monoxide": r=0.6f; g=0.6f; b=0.6f; poison=true; break;  // CO 无色无味但有毒
            case "chemical_industry:oxygen_gas":      r=0.5f; g=0.8f; b=1.0f; break;
            case "chemical_industry:nitrogen_gas":    r=0.8f; g=0.6f; b=1.0f; break;
            case "chemical_industry:rare_gas":        r=1.0f; g=0.9f; b=0.4f; break;
            case "chemical_industry:compressed_air":  r=0.8f; g=0.85f; b=0.9f; break;
            case "chemical_industry:carbon_dioxide":  r=0.9f; g=0.9f; b=0.9f; break;  // CO₂ 白色烟雾
            default: r=0.7f; g=0.8f; b=1.0f; break;  // hydrogen_gas 等
        }
        int count = Math.min(amount / 10, 50);
        for (int i = 0; i < count; i++) {
            sl.sendParticles(new DustParticleOptions(new Vector3f(r, g, b), 0.5f),
                    pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5,
                    1, 0.3, 0.3, 0.3, 0.02);
        }
        if (corrode || poison) {
            for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class,
                    new net.minecraft.world.phys.AABB(pos).inflate(3.0))) {
                if (corrode) {
                    e.hurt(sl.damageSources().generic(), 1.0f);
                    e.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                            com.cxy.chemical_industry.registry.ModEffects.corrosionHolder(), 100, 0));
                }
                if (poison)
                    e.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                            net.minecraft.world.effect.MobEffects.POISON, 100, 0));
            }
        }
    }

    // ---------- NBT 读写（通过 DataComponents）----------

    public static String getGasType(ItemStack s) {
        CustomData cd = s.get(DataComponents.CUSTOM_DATA);
        if (cd == null) return null;
        CompoundTag t = cd.copyTag();
        return t.contains("Gas") ? t.getString("Gas") : null;
    }

    public static int getGasAmount(ItemStack s) {
        CustomData cd = s.get(DataComponents.CUSTOM_DATA);
        if (cd == null) return 0;
        return cd.copyTag().getInt("Amount");
    }

    public static void setGas(ItemStack s, String type, int amt) {
        CompoundTag t = new CompoundTag();
        if (type != null) {
            t.putString("Gas", type);
            t.putInt("Amount", amt);
        }
        s.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
    }

    /** 判断是否为可装入储气罐的气体（匹配所有气体类流体） */
    private static boolean isGasFluid(String key) {
        if (!key.startsWith("chemical_industry:")) return false;
        // 匹配 _gas 后缀、compressed_air、rare_gas、carbon_monoxide、carbon_dioxide
        return key.endsWith("_gas") || key.contains("compressed_air") || key.contains("rare_gas")
                || key.endsWith("carbon_monoxide") || key.endsWith("carbon_dioxide");
    }
    /** 气体注册名 → 中文显示名 */
    private static String getGasDisplayName(String key) {
        if (key == null) return "?";
        return switch (key) {
            case "chemical_industry:chlorine_gas" -> "氯气";
            case "chemical_industry:hydrogen_gas" -> "氢气";
            case "chemical_industry:oxygen_gas" -> "氧气";
            case "chemical_industry:nitrogen_gas" -> "氮气";
            case "chemical_industry:ammonia_gas" -> "氨气";
            case "chemical_industry:rare_gas" -> "稀有气体";
            case "chemical_industry:compressed_air" -> "压缩空气";
            case "chemical_industry:carbon_monoxide" -> "一氧化碳";
            case "chemical_industry:carbon_dioxide" -> "二氧化碳";
            default -> key.substring(key.lastIndexOf(':') + 1);
        };
    }

    // ---------- 提示栏（Shift 查看详情）----------

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx,
                                List<Component> tips, TooltipFlag flag) {
        if (TooltipBlockItem.hasShiftDown()) {
            // 按住 Shift → 显示详细使用说明
            tips.add(Component.translatable("tooltip.chemical_industry.gas_canister.detail"));
        } else {
            // 没按 Shift → 显示当前状态 + 简短提示
            String gas = getGasType(stack);
            if (gas != null) {
                tips.add(Component.literal(
                        (getGasDisplayName(gas)) + ": " + getGasAmount(stack) + "mB"));
            } else {
                tips.add(Component.literal("空"));
            }
            TooltipBlockItem.addShiftHint(tips);
        }
    }

    // ---------- 耐久条（显示填充程度）----------

    @Override
    public boolean isBarVisible(ItemStack s) {
        return getGasType(s) != null;
    }

    @Override
    public int getBarWidth(ItemStack s) {
        return Math.round(13f * getGasAmount(s) / MAX_GAS);
    }

    @Override
    public int getBarColor(ItemStack s) {
        String g = getGasType(s);
        if (g == null) return 0xFF888888;
        if (g.contains("chlorine")) return 0xFF88FF44;
        if (g.contains("ammonia")) return 0xFFAAFF88;
        if (g.contains("rare")) return 0xFFFFCC44;
        return 0xFF88BBFF;
    }
}
