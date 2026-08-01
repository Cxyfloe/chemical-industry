package com.cxy.chemical_industry.screen;

import com.cxy.chemical_industry.block_entity.FluidizedBedBlockEntity;
import com.cxy.chemical_industry.registry.ModBlocks;
import com.cxy.chemical_industry.registry.ModMenus;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

public class FluidizedBedMenu extends AbstractContainerMenu {
    private final FluidizedBedBlockEntity be;
    private final ContainerData data;

    public FluidizedBedMenu(int id, Inventory inv, FluidizedBedBlockEntity be, ContainerData data) {
        super(ModMenus.FLUIDIZED_BED_MENU.get(), id);
        this.be = be; this.data = data;
        var h = be.getGuiItemHandler(); // 只暴露 3 槽（催化剂+2输入），输出由漏斗提取
        addSlot(new SlotItemHandler(h, 0, 30, 17)); // 催化剂
        addSlot(new SlotItemHandler(h, 1, 30, 35)); // 输入 1
        addSlot(new SlotItemHandler(h, 2, 30, 53)); // 输入 2
        for (int r=0; r<3; r++) for (int c=0; c<9; c++) addSlot(new Slot(inv, c+r*9+9, 8+c*18, 84+r*18));
        for (int c=0; c<9; c++) addSlot(new Slot(inv, c, 8+c*18, 142));
        addDataSlots(data);
    }
    public FluidizedBedBlockEntity getBlockEntity() { return be; }
    public int getProgress() { return data.get(0); }
    public int getMaxProgress() { return data.get(1); }
    @Override public ItemStack quickMoveStack(Player p, int idx) {
        ItemStack copy = ItemStack.EMPTY; Slot s = slots.get(idx);
        if (!s.hasItem()) return copy;
        ItemStack st = s.getItem(); copy = st.copy();
        if (idx < 3) { if(!moveItemStackTo(st, 3, 39, true)) return ItemStack.EMPTY; }
        else { if(!moveItemStackTo(st, 0, 3, false)) return ItemStack.EMPTY; }
        if (st.isEmpty()) s.set(ItemStack.EMPTY); else s.setChanged();
        return copy;
    }
    @Override public boolean stillValid(Player p) {
        return stillValid(ContainerLevelAccess.create(be.getLevel(), be.getBlockPos()), p, ModBlocks.FLUIDIZED_BED.get());
    }
}
