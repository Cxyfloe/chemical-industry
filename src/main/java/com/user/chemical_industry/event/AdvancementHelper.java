package com.user.chemical_industry.event;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/** 成就授予工具 */
public class AdvancementHelper {

    /** 给方块周围 8 格内的所有玩家授予成就 */
    public static void grantNearby(Level level, BlockPos pos, String advancementId, String criterion) {
        if (!(level instanceof ServerLevel sl)) return;
        for (ServerPlayer p : sl.players()) {
            if (p.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) < 64) { // 8格内
                award(p, advancementId, criterion);
            }
        }
    }

    /** 直接给指定玩家授予成就（不依赖位置） */
    public static void grantPlayer(ServerPlayer p, String advancementId, String criterion) {
        award(p, advancementId, criterion);
    }

    /** 全部成就列表（root + 旧 9 个 + 新 9 个）——用于"诺贝尔化学奖"检查 */
    private static final String[] ALL_ACHIEVEMENTS = {
            "root", "niter", "steel_ingot", "aluminum_ingot", "sulfuric_acid", "anode_slime",
            "first_electrolysis", "organic", "oxygen", "benzene",
            "hydrofluoric_acid", "fluorine", "acid_into_water", "condenser_pipe",
            "rare_gas", "opium", "fertilize", "easy_chemistry", "nobel"
    };

    /** 授予成就 + 联动：第一个模组成就 → "化学，易如反掌"；全部完成 → "诺贝尔化学奖" */
    private static void award(ServerPlayer p, String id, String criterion) {
        ResourceLocation rl = ResourceLocation.fromNamespaceAndPath("chemical_industry", id);
        var adv = p.server.getAdvancements().get(rl);
        if (adv == null) return;
        p.getAdvancements().award(adv, criterion);

        // 第一个模组成就（root 除外）→ "化学，易如反掌"
        if (!id.equals("root") && !id.equals("easy_chemistry") && !id.equals("nobel")) {
            awardDirect(p, "easy_chemistry", "first");
        }
        // 检查是否集齐所有成就 → "诺贝尔化学奖"
        checkAllCompleted(p);
    }

    /** 检查玩家是否已获得全部成就，全有则授予诺贝尔奖 */
    private static void checkAllCompleted(ServerPlayer p) {
        for (String id : ALL_ACHIEVEMENTS) {
            ResourceLocation rl = ResourceLocation.fromNamespaceAndPath("chemical_industry", id);
            var adv = p.server.getAdvancements().get(rl);
            if (adv == null || !p.getAdvancements().getOrStartProgress(adv).isDone()) return;
        }
        awardDirect(p, "nobel", "all");
    }

    /** 直接授予（不触发联动检查，避免递归） */
    private static void awardDirect(ServerPlayer p, String id, String criterion) {
        ResourceLocation rl = ResourceLocation.fromNamespaceAndPath("chemical_industry", id);
        var adv = p.server.getAdvancements().get(rl);
        if (adv != null) p.getAdvancements().award(adv, criterion);
    }
}
