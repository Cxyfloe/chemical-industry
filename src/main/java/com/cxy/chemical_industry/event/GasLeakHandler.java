package com.cxy.chemical_industry.event;

import com.cxy.chemical_industry.ChemicalIndustry;
import com.cxy.chemical_industry.registry.ModEffects;
import com.cxy.chemical_industry.registry.ModFluids;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.content.fluids.PipeConnection;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.*;

/**
 * 气体管道末端泄露检测
 *
 * 每 1 秒扫描所有含有毒/可燃气体的流体容器（Create 管道/储罐），
 * 如果容器的某面邻接空气（= 管道末端无接收方），触发泄露效果：
 *   Cl₂ → 高伤害 + 腐蚀效果
 *   CO  → 高伤害（窒息性中毒）
 *   NH₃ → 伤害 + 腐蚀效果
 *   H₂  → 低伤害
 *   Cl₂ + H₂ 近距离同时泄露 → 爆炸
 */
@EventBusSubscriber(modid = ChemicalIndustry.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class GasLeakHandler {

    /** 泄露检测范围：泄漏点周围多少格会伤到生物 */
    private static final int LEAK_RANGE = 3;
    /** 检测半径（区块数） */
    private static final int CHUNK_RADIUS = 8;
    /** 同一泄漏点的伤害冷却（tick）：避免断管残留气体造成"永远持续中毒" */
    private static final int DAMAGE_COOLDOWN = 60;  // 3 秒
    /** 记录每个泄漏点最后一次伤害的时间 */
    private static final Map<BlockPos, Long> damageCooldown = new HashMap<>();

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        Level level = event.getLevel();
        if (level.isClientSide()) return;
        if (level.getGameTime() % 20 != 0) return; // 每秒一次
        if (!(level instanceof ServerLevel sl)) return;

        // 收集泄露位置（按气体分别记录）
        List<BlockPos> h2Leaks = new ArrayList<>();
        List<BlockPos> cl2Leaks = new ArrayList<>();
        List<BlockPos> coLeaks = new ArrayList<>();
        List<BlockPos> nh3Leaks = new ArrayList<>();

        // 遍历玩家周围的已加载区块
        for (var player : sl.players()) {
            int cx = player.blockPosition().getX() >> 4;
            int cz = player.blockPosition().getZ() >> 4;
            for (int dx = -CHUNK_RADIUS; dx <= CHUNK_RADIUS; dx++) {
                for (int dz = -CHUNK_RADIUS; dz <= CHUNK_RADIUS; dz++) {
                    LevelChunk chunk = sl.getChunkSource().getChunk(cx + dx, cz + dz, false);
                    if (chunk == null) continue;
                    for (BlockEntity be : chunk.getBlockEntities().values()) {
                        checkBlockEntity(level, be, h2Leaks, cl2Leaks, coLeaks, nh3Leaks);
                    }
                }
            }
        }

        // Cl₂ + H₂ 混合爆炸检测
        for (BlockPos hp : h2Leaks) {
            for (BlockPos cp : cl2Leaks) {
                if (hp.distSqr(cp) <= 9.0) { // 3 格内
                    sl.explode(null, (hp.getX() + cp.getX()) / 2.0 + 0.5,
                            (hp.getY() + cp.getY()) / 2.0 + 0.5,
                            (hp.getZ() + cp.getZ()) / 2.0 + 0.5,
                            3.5f, Level.ExplosionInteraction.BLOCK);
                    break;
                }
            }
        }

        // CO 泄露伤害（一氧化碳 — 窒息性中毒，高伤害）
        for (BlockPos leakPos : coLeaks) {
            damageNearby(level, leakPos, GasType.CO);
        }

        // NH₃ 泄露伤害（氨气 — 腐蚀性，中伤害 + 腐蚀）
        for (BlockPos leakPos : nh3Leaks) {
            damageNearby(level, leakPos, GasType.NH3);
        }
    }

    /** 气体类型枚举 — 每种有毒/可燃气体的泄露效果不同 */
    private enum GasType {
        CL2,  // 氯气：高伤害 + 腐蚀
        H2,   // 氢气：低伤害（主要是爆炸风险）
        CO,   // 一氧化碳：窒息性中毒，高伤害
        NH3   // 氨气：腐蚀性，中伤害 + 腐蚀
    }

    /** 检查单个方块实体：Create 管道中含气体且末端开放才泄露 */
    private static void checkBlockEntity(Level level, BlockEntity be,
                                          List<BlockPos> h2Leaks, List<BlockPos> cl2Leaks,
                                          List<BlockPos> coLeaks, List<BlockPos> nh3Leaks) {
        // 只检测 Create 管道类方块（流体储罐里的气是密封储存的，不泄露）
        String key = be.getBlockState().getBlock().builtInRegistryHolder()
                .key().location().toString();
        if (!key.startsWith("create:fluid_pipe")
                && !key.equals("create:smart_fluid_pipe")
                && !key.equals("create:fluid_valve")
                && !key.equals("create:mechanical_pump")) return;

        // 关键：Create 6.0.9 的流体管道【不注册】标准 IFluidHandler 能力！
        // 之前 getCapability() 永远返回 null，导致检测永远不生效。
        // 改用 Create 内部的 FluidTransportBehaviour 直接读取管道里流动的流体。
        FluidTransportBehaviour behaviour = BlockEntityBehaviour.get(
                level, be.getBlockPos(), FluidTransportBehaviour.TYPE);
        if (behaviour == null) return;

        // 要求管道有压力（泵正在推动）才算泄漏：
        // 泵停止后残留在管道里的气体只是"停在原地"，不会从端口溢出，
        // 否则爆炸后的管道遗址会永远持续中毒（bug）。
        if (!behaviour.hasAnyPressure()) return;

        // 只统计【正在向外排出】的流体（provideOutboundFlow），
        // 管道吸入的气体（getProvidedFluid）不算泄漏——它还在管道里。
        // 用位掩码记录气体类型：1=Cl₂ 2=H₂ 4=CO 8=NH₃
        int gasFlags = 0;
        if (behaviour.interfaces != null) {
            for (PipeConnection conn : behaviour.interfaces.values()) {
                if (conn == null) continue;
                gasFlags |= classifyFluid(conn.provideOutboundFlow());
            }
        }
        boolean hasCl2 = (gasFlags & 1) != 0;
        boolean hasH2 = (gasFlags & 2) != 0;
        boolean hasCO = (gasFlags & 4) != 0;
        boolean hasNH3 = (gasFlags & 8) != 0;
        if (gasFlags == 0) return;

        // 检查是否有邻接空气的面（管道末端/断口 = 泄漏点）
        BlockPos pos = be.getBlockPos();
        for (Direction d : Direction.values()) {
            BlockState neighbor = level.getBlockState(pos.relative(d));
            if (!neighbor.isAir()) continue;

            // 管道里有气 + 末端开放 = 泄露！
            BlockPos leakPos = pos.relative(d);
            if (hasCl2) {
                cl2Leaks.add(leakPos);
                damageNearby(level, leakPos, GasType.CL2);
            }
            if (hasH2) {
                h2Leaks.add(leakPos);
                damageNearby(level, leakPos, GasType.H2);
            }
            if (hasCO) {
                coLeaks.add(leakPos);
                damageNearby(level, leakPos, GasType.CO);
            }
            if (hasNH3) {
                nh3Leaks.add(leakPos);
                damageNearby(level, leakPos, GasType.NH3);
            }
            break;
        }
    }

    /** 判断流体类型并返回位标志：1=Cl₂ 2=H₂ 4=CO 8=NH₃，其他返回 0 */
    private static int classifyFluid(FluidStack fs) {
        if (fs == null || fs.isEmpty()) return 0;
        if (fs.getFluid().isSame(ModFluids.CHLORINE_GAS.getSource())) return 1;
        if (fs.getFluid().isSame(ModFluids.HYDROGEN_GAS.getSource())) return 2;
        if (fs.getFluid().isSame(ModFluids.CARBON_MONOXIDE.getSource())) return 4;
        if (fs.getFluid().isSame(ModFluids.AMMONIA_GAS.getSource())) return 8;
        return 0;
    }

    /** 对泄露点周围的生物施加中毒效果（不同气体效果不同） */
    private static void damageNearby(Level level, BlockPos leakPos, GasType gas) {
        // 冷却检查：同一泄漏点在冷却期内不重复伤害。
        // 这样爆炸遗址的断管残留气体只会间歇性造成影响，
        // 玩家可以及时跑开，而不是被"永远持续"的中毒困住。
        long now = level.getGameTime();
        Long last = damageCooldown.get(leakPos);
        if (last != null && now - last < DAMAGE_COOLDOWN) return;
        damageCooldown.put(leakPos, now);

        AABB area = new AABB(leakPos).inflate(LEAK_RANGE);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, area)) {
            // 中毒效果（原版）：等级 0 = 每 1.25 秒扣 1 点血，等级 1 = 每 0.625 秒扣 1 点血
            // 时长比冷却短 → 中毒会明显间歇（毒 3 秒、停 3 秒），不会"永远持续"
            switch (gas) {
                case CL2 -> {  // 氯气：中毒 I + 腐蚀
                    e.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0));
                    e.addEffect(new MobEffectInstance(ModEffects.corrosionHolder(), 60, 0));
                }
                case H2 -> {   // 氢气无毒（主要危险是可燃爆炸），不施加效果
                }
                case CO -> {   // 一氧化碳：中毒 I
                    e.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0));
                }
                case NH3 -> {  // 氨气：中毒 I + 腐蚀
                    e.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0));
                    e.addEffect(new MobEffectInstance(ModEffects.corrosionHolder(), 60, 0));
                }
            }
        }
    }
}
