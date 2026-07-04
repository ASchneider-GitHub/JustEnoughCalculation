package me.towdium.jecalculation.utils;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

//Thanks to Forge
@Environment(EnvType.CLIENT)
public class GuiUtils {
    private static final int ATLAS_SIZE = 256;

    public static void drawContinuousTexturedBox(GuiGraphics graphics, ResourceLocation res, int x, int y, int u, int v, int width, int height, int textureWidth, int textureHeight, int topBorder, int bottomBorder, int leftBorder, int rightBorder) {
        int fillerWidth = textureWidth - leftBorder - rightBorder;
        int fillerHeight = textureHeight - topBorder - bottomBorder;
        int canvasWidth = width - leftBorder - rightBorder;
        int canvasHeight = height - topBorder - bottomBorder;
        int xPasses = canvasWidth / fillerWidth;
        int remainderWidth = canvasWidth % fillerWidth;
        int yPasses = canvasHeight / fillerHeight;
        int remainderHeight = canvasHeight % fillerHeight;
        drawTexturedModalRect(graphics, res, x, y, u, v, leftBorder, topBorder);
        drawTexturedModalRect(graphics, res, x + leftBorder + canvasWidth, y, u + leftBorder + fillerWidth, v, rightBorder, topBorder);
        drawTexturedModalRect(graphics, res, x, y + topBorder + canvasHeight, u, v + topBorder + fillerHeight, leftBorder, bottomBorder);
        drawTexturedModalRect(graphics, res, x + leftBorder + canvasWidth, y + topBorder + canvasHeight, u + leftBorder + fillerWidth, v + topBorder + fillerHeight, rightBorder, bottomBorder);

        int i;
        for (i = 0; i < xPasses + (remainderWidth > 0 ? 1 : 0); ++i) {
            drawTexturedModalRect(graphics, res, x + leftBorder + i * fillerWidth, y, u + leftBorder, v, i == xPasses ? remainderWidth : fillerWidth, topBorder);
            drawTexturedModalRect(graphics, res, x + leftBorder + i * fillerWidth, y + topBorder + canvasHeight, u + leftBorder, v + topBorder + fillerHeight, i == xPasses ? remainderWidth : fillerWidth, bottomBorder);

            for (int j = 0; j < yPasses + (remainderHeight > 0 ? 1 : 0); ++j) {
                drawTexturedModalRect(graphics, res, x + leftBorder + i * fillerWidth, y + topBorder + j * fillerHeight, u + leftBorder, v + topBorder, i == xPasses ? remainderWidth : fillerWidth, j == yPasses ? remainderHeight : fillerHeight);
            }
        }

        for (i = 0; i < yPasses + (remainderHeight > 0 ? 1 : 0); ++i) {
            drawTexturedModalRect(graphics, res, x, y + topBorder + i * fillerHeight, u, v + topBorder, leftBorder, i == yPasses ? remainderHeight : fillerHeight);
            drawTexturedModalRect(graphics, res, x + leftBorder + canvasWidth, y + topBorder + i * fillerHeight, u + leftBorder + fillerWidth, v + topBorder, rightBorder, i == yPasses ? remainderHeight : fillerHeight);
        }
    }

    private static void drawTexturedModalRect(GuiGraphics graphics, ResourceLocation res, int x, int y, int u, int v, int width, int height) {
        graphics.blit(res, x, y, (float) u, (float) v, width, height, ATLAS_SIZE, ATLAS_SIZE);
    }
}
