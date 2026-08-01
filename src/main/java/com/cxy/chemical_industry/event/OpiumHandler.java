package com.cxy.chemical_industry.event;

import com.cxy.chemical_industry.ChemicalIndustry;
import com.cxy.chemical_industry.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * 鸦片副作用计时器
 *
 * 吃鸦片/罂粟果实后：
 * 1. 正面效果（伤害吸收 II / 生命恢复 II / 抗性提升 II）由食物属性立即施加
 * 2. 潜伏期（鸦片 25s / 果实 15s）结束后，由本处理器施加 7 种负面效果
 *    （中毒 / 凋零 / 反胃 / 缓慢 / 饥饿 / 挖掘疲劳 / 虚弱，均为 I 级）
 *
 * 倒计时存在玩家 NBT 中：重进游戏也会继续倒计时，不会白嫖。
 * 若潜伏期内再次食用，倒计时刷新为最新一次（不叠加）。
 */
@EventBusSubscriber(modid = ChemicalIndustry.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class OpiumHandler {

    /** NBT 键：剩余潜伏 tick */
    public static final String LATENCY_TAG = "CIOpiumLatency";
    /** NBT 键：副作用时长 tick */
    public static final String SIDE_TAG = "CIOpiumSide";

    /** 设置潜伏倒计时（OpiumItem.finishUsingItem 中调用） */
    public static void armLatency(Player player, int latencyTicks, int sideEffectTicks) {
        CompoundTag data = player.getPersistentData();
        data.putInt(LATENCY_TAG, latencyTicks);
        data.putInt(SIDE_TAG, sideEffectTicks);
    }

    /** 每秒检查一次所有玩家的潜伏倒计时 */
    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        Level level = event.getLevel();
        if (level.isClientSide()) return;
        if (level.getGameTime() % 20 != 0) return;  // 每秒一次

        for (Player p : level.players()) {
            CompoundTag data = p.getPersistentData();
            if (!data.contains(LATENCY_TAG)) continue;

            int left = data.getInt(LATENCY_TAG) - 20;
            if (left <= 0) {
                // 潜伏期结束 → 施加负面效果
                int side = data.getInt(SIDE_TAG);
                data.remove(LATENCY_TAG);
                data.remove(SIDE_TAG);
                applySideEffects(p, side);
            } else {
                data.putInt(LATENCY_TAG, left);
            }
        }

        // 成就"制得鸦片"：扫描玩家周围工作盆（Create Basin）中是否有鸦片
        scanBasins(level);
    }

    /** 扫描玩家周围的工作盆：物品里的鸦片 → "制得鸦片"；流体里的氢氟酸 → "真的是你？" */
    private static void scanBasins(Level level) {
        if (!(level instanceof ServerLevel sl)) return;
        for (var player : sl.players()) {
            // 还没拿到的成就才扫描（各成就独立跳过）
            boolean needOpium = !hasAchievement(sl, player, "opium");
            boolean needHF = !hasAchievement(sl, player, "hydrofluoric_acid");
            if (!needOpium && !needHF) continue;

            int cx = player.blockPosition().getX() >> 4;
            int cz = player.blockPosition().getZ() >> 4;
            for (int dx = -4; dx <= 4; dx++) {
                for (int dz = -4; dz <= 4; dz++) {
                    LevelChunk chunk = sl.getChunkSource().getChunk(cx + dx, cz + dz, false);
                    if (chunk == null) continue;
                    for (BlockEntity be : chunk.getBlockEntities().values()) {
                        // 只检查 Create 工作盆
                        String key = be.getBlockState().getBlock().builtInRegistryHolder()
                                .key().location().toString();
                        if (!key.equals("create:basin")) continue;

                        // 物品槽：鸦片
                        if (needOpium) {
                            IItemHandler inv = level.getCapability(
                                    Capabilities.ItemHandler.BLOCK, be.getBlockPos(), null);
                            if (inv != null) {
                                for (int t = 0; t < inv.getSlots(); t++) {
                                    if (inv.getStackInSlot(t).is(ModItems.OPIUM.get())) {
                                        AdvancementHelper.grantNearby(level, be.getBlockPos(), "opium", "opium");
                                        needOpium = false;
                                        break;
                                    }
                                }
                            }
                        }
                        // 流体槽：氢氟酸（首次获得）
                        if (needHF) {
                            net.neoforged.neoforge.fluids.capability.IFluidHandler fh = level.getCapability(
                                    Capabilities.FluidHandler.BLOCK, be.getBlockPos(), null);
                            if (fh != null) {
                                for (int t = 0; t < fh.getTanks(); t++) {
                                    if (fh.getFluidInTank(t).getFluid().isSame(
                                            com.cxy.chemical_industry.registry.ModFluids.HYDROFLUORIC_ACID.getSource())) {
                                        AdvancementHelper.grantNearby(level, be.getBlockPos(), "hydrofluoric_acid", "hf");
                                        needHF = false;
                                        break;
                                    }
                                }
                            }
                        }
                        if (!needOpium && !needHF) return;  // 都拿到了，不用继续扫
                    }
                }
            }
        }
    }

    /** 判断玩家是否已获得指定成就 */
    private static boolean hasAchievement(ServerLevel sl, ServerPlayer player, String id) {
        var adv = sl.getServer().getAdvancements().get(
                ResourceLocation.fromNamespaceAndPath("chemical_industry", id));
        return adv != null && player.getAdvancements().getOrStartProgress(adv).isDone();
    }

    /** 施加 7 种负面效果（均为 I 级） */
    private static void applySideEffects(Player p, int durationTicks) {
        p.addEffect(new MobEffectInstance(MobEffects.POISON, durationTicks, 0));             // 中毒
        p.addEffect(new MobEffectInstance(MobEffects.WITHER, durationTicks, 0));             // 凋零
        p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, durationTicks, 0));          // 反胃
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, durationTicks, 0));  // 缓慢
        p.addEffect(new MobEffectInstance(MobEffects.HUNGER, durationTicks, 0));             // 饥饿
        p.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, durationTicks, 0));       // 挖掘疲劳
        p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, durationTicks, 0));           // 虚弱
    }
}
