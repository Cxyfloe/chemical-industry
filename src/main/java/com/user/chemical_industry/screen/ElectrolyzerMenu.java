package com.user.chemical_industry.screen;

import com.user.chemical_industry.block_entity.ElectrolyzerBlockEntity;
import com.user.chemical_industry.registry.ModBlocks;
import com.user.chemical_industry.registry.ModMenus;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

/** 电解槽容器菜单 — 5 槽：阴电极 阳电极 催化剂 膜 滤网 */
public class ElectrolyzerMenu extends AbstractContainerMenu {
    private final ElectrolyzerBlockEntity be;
    private final ContainerData data;

    public ElectrolyzerMenu(int id, Inventory inv, ElectrolyzerBlockEntity be, ContainerData data) {
        super(ModMenus.ELECTROLYZER_MENU.get(), id);
        this.be = be; this.data = data;
        var h = be.guiItems();

        addSlot(new SlotItemHandler(h, 0, 26, 28));  // 阴极电极（BE槽0）
        addSlot(new SlotItemHandler(h, 2, 79, 28));  // 催化剂（BE槽2）
        addSlot(new SlotItemHandler(h, 1, 132, 28)); // 阳极电极（BE槽1）
        addSlot(new SlotItemHandler(h, 3, 63, 50));  // 膜（BE槽3）
        addSlot(new SlotItemHandler(h, 4, 95, 50));  // 滤网（BE槽4）

        for (int r=0; r<3; r++) for (int c=0; c<9; c++)
            addSlot(new Slot(inv, c+r*9+9, 8+c*18, 84+r*18));
        for (int c=0; c<9; c++) addSlot(new Slot(inv, c, 8+c*18, 142));
        addDataSlots(data);
    }

    public int getProgress() { return data.get(0); }
    public int getMaxProgress() { return data.get(1); }

    @Override public ItemStack quickMoveStack(Player p, int idx) {
        ItemStack copy = ItemStack.EMPTY; Slot s = slots.get(idx);
        if (!s.hasItem()) return copy;
        ItemStack st = s.getItem(); copy = st.copy();
        if (idx < 5) { if (!moveItemStackTo(st, 5, 41, true)) return ItemStack.EMPTY; }
        else { if (!moveItemStackTo(st, 0, 5, false)) return ItemStack.EMPTY; }
        if (st.isEmpty()) s.set(ItemStack.EMPTY); else s.setChanged();
        return copy;
    }
    @Override public boolean stillValid(Player p) {
        return stillValid(ContainerLevelAccess.create(be.getLevel(), be.getBlockPos()), p, ModBlocks.ELECTROLYZER.get());
    }
}
