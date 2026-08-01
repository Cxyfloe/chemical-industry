package com.user.chemical_industry.screen;

import com.user.chemical_industry.ChemicalIndustry;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class FluidizedBedScreen extends AbstractContainerScreen<FluidizedBedMenu> {
    private static final ResourceLocation TEX = ResourceLocation
            .fromNamespaceAndPath(ChemicalIndustry.MOD_ID, "textures/gui/fluidized_bed.png");

    public FluidizedBedScreen(FluidizedBedMenu m, Inventory inv, Component t) {
        super(m, inv, t); imageWidth=176; imageHeight=166;
    }

    @Override protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1,1,1,1);
        int x=(width-imageWidth)/2, y=(height-imageHeight)/2;
        g.blit(TEX, x, y, 0, 0, imageWidth, imageHeight);
        // 只画 3 个输入槽（催化剂+2输入）
        drawSlot(g,x+30,y+17); drawSlot(g,x+30,y+35); drawSlot(g,x+30,y+53);
        int p=menu.getProgress(), pm=menu.getMaxProgress();
        if(p>0&&pm>0){ int h=(int)((float)p/pm*20); g.blit(TEX,x+79,y+34+(20-h),176,20-h,24,h); }
    }
    private void drawSlot(GuiGraphics g, int sx, int sy) {
        g.fill(sx-1,sy-1,sx+17,sy+17,0xFF373737); g.fill(sx,sy,sx+16,sy+16,0xFF8B8B8B);
    }
    @Override public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt); renderTooltip(g, mx, my);
    }
    @Override protected void renderLabels(GuiGraphics g, int mx, int my) {
        g.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);
        g.drawString(font, Component.literal("催化"), 32, 6, 0xFFFF44, false);
        g.drawString(font, Component.literal("输入"), 32, 23, 0xFFFFFF, false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
    }
}
