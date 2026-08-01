package com.cxy.chemical_industry.screen;

import com.cxy.chemical_industry.ChemicalIndustry;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class ElectrolyzerScreen extends AbstractContainerScreen<ElectrolyzerMenu> {
    private static final ResourceLocation TEX = ResourceLocation
            .fromNamespaceAndPath(ChemicalIndustry.MOD_ID, "textures/gui/electrolyzer.png");

    public ElectrolyzerScreen(ElectrolyzerMenu m, Inventory inv, Component t) {
        super(m, inv, t); imageWidth=176; imageHeight=166;
    }

    @Override protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1,1,1,1);
        int x=(width-imageWidth)/2, y=(height-imageHeight)/2;
        g.blit(TEX, x, y, 0, 0, imageWidth, imageHeight);

        drawSlot(g,x+26,y+28); drawSlot(g,x+79,y+28); drawSlot(g,x+132,y+28);
        drawSlot(g,x+63,y+50); drawSlot(g,x+95,y+50);

        int p=menu.getProgress(), pm=menu.getMaxProgress();
        if(p>0&&pm>0){ int w=(int)((float)p/pm*22); g.fill(x+77,y+70,x+77+w,y+70+16,0xFF40A0FF); }
    }
    private void drawSlot(GuiGraphics g, int sx, int sy) {
        g.fill(sx-1,sy-1,sx+17,sy+17,0xFF373737); g.fill(sx,sy,sx+16,sy+16,0xFF8B8B8B);
    }
    @Override public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt); renderTooltip(g, mx, my);
    }
    @Override protected void renderLabels(GuiGraphics g, int mx, int my) {
        g.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);
        g.drawString(font, Component.literal("阴"), 28, 14, 0xFF4444, false);
        g.drawString(font, Component.literal("催化剂"), 73, 14, 0xFFFF44, false);
        g.drawString(font, Component.literal("阳"), 134, 14, 0x4444FF, false);
        g.drawString(font, Component.literal("膜"), 67, 38, 0xFF88FF, false);
        g.drawString(font, Component.literal("固"), 97, 38, 0xAA8855, false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
    }
}
