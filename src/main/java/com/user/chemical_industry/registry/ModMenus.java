package com.user.chemical_industry.registry;

import com.user.chemical_industry.ChemicalIndustry;
import com.user.chemical_industry.screen.FluidizedBedMenu;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * 模组容器菜单类型注册表
 *
 * 【什么是 MenuType？】
 * MenuType 是容器菜单的"注册模板"。
 * 当玩家打开一个容器时，游戏用 MenuType 来创建对应的 Menu 实例。
 * 每个有 GUI 的方块都需要在这里注册一个 MenuType。
 */
public class ModMenus {

    private static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(BuiltInRegistries.MENU, ChemicalIndustry.MOD_ID);

    /**
     * 沸腾炉菜单类型
     *
     * IMenuTypeExtension.create 的参数：
     * - 一个 lambda，接收 (containerId, playerInventory, extraData)
     * - extraData 包含方块坐标（由游戏自动传入）
     * - 返回一个 FluidizedBedMenu 实例
     *
     * 注意：这里创建的是客户端版本的构造器
     * （服务端由 FluidizedBedBlockEntity.createMenu 处理）
     */
    public static final Supplier<MenuType<FluidizedBedMenu>> FLUIDIZED_BED_MENU = MENUS.register(
            "fluidized_bed_menu",
            () -> IMenuTypeExtension.create((id, inv, data) -> {
                var pos = data.readBlockPos();
                var be = inv.player.level().getBlockEntity(pos);
                if (be instanceof com.user.chemical_industry.block_entity.FluidizedBedBlockEntity e)
                    return new FluidizedBedMenu(id, inv, e, e.dataAccess);
                return null;
            })
    );

    public static final Supplier<MenuType<com.user.chemical_industry.screen.ElectrolyzerMenu>> ELECTROLYZER_MENU = MENUS.register(
            "electrolyzer_menu",
            () -> IMenuTypeExtension.create((id, inv, data) -> {
                net.minecraft.core.BlockPos pos = data.readBlockPos();
                net.minecraft.world.level.block.entity.BlockEntity be = inv.player.level().getBlockEntity(pos);
                if (be instanceof com.user.chemical_industry.block_entity.ElectrolyzerBlockEntity e)
                    return new com.user.chemical_industry.screen.ElectrolyzerMenu(id, inv, e, e.data);
                return null;
            })
    );

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}
