package me.towdium.jecalculation.events;

import dev.architectury.event.CompoundEventResult;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.event.events.client.ClientScreenInputEvent;
import dev.architectury.event.events.client.ClientTooltipEvent;
import me.towdium.jecalculation.gui.JecaGui;
import me.towdium.jecalculation.utils.wrappers.Trio;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.entity.player.Inventory;
import org.joml.Matrix4f;
import org.joml.Vector2ic;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static dev.architectury.event.EventResult.interruptFalse;
import static dev.architectury.event.EventResult.pass;

@Environment(EnvType.CLIENT)
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GuiScreenEventHandler {

    protected GuiScreenOverlayHandler overlayHandler = null;
    protected JecaGui gui = null;
    protected InventorySummary cachedInventory;
    protected Trio<List<? extends ClientTooltipComponent>, Integer, Integer> cachedTooltipEvent;

    public GuiScreenEventHandler() {
        registerEvents();
    }

    private void registerEvents() {
        ClientGuiEvent.SET_SCREEN.register(this::onGuiOpen);
        ClientTooltipEvent.RENDER_PRE.register(this::onTooltip);
        ClientGuiEvent.RENDER_POST.register(this::onDrawForeground);
        ClientScreenInputEvent.MOUSE_SCROLLED_PRE.register(this::onMouseScroll);
        ClientScreenInputEvent.MOUSE_CLICKED_PRE.register(this::onMouseClicked);
        ClientScreenInputEvent.MOUSE_DRAGGED_PRE.register(this::onMouseDragged);
        ClientScreenInputEvent.MOUSE_RELEASED_PRE.register(this::onMouseReleased);
        ClientScreenInputEvent.KEY_PRESSED_PRE.register(this::onKeyPressed);
        ClientScreenInputEvent.KEY_RELEASED_PRE.register(this::onKeyReleased);
        ClientScreenInputEvent.CHAR_TYPED_PRE.register(this::onCharTyped);
    }

    public CompoundEventResult<Screen> onGuiOpen(Screen screen) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !(screen instanceof AbstractContainerScreen)) {
            return CompoundEventResult.pass();
        }

        overlayHandler = new GuiScreenOverlayHandler(player.getInventory());
        gui = new JecaGui(null, false, overlayHandler, true);
        gui.initWidget(Minecraft.getInstance(), screen.width, screen.height);
        overlayHandler.setGui(gui);
        return CompoundEventResult.pass();
    }

    protected boolean isScreenValidForOverlay(Screen screen) {
        return screen instanceof AbstractContainerScreen
                && !(screen instanceof JecaGui);
    }

    public void onDrawForeground(Screen screen, GuiGraphics graphics, int mouseX, int mouseY, DeltaTracker delta) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || overlayHandler == null || !isScreenValidForOverlay(screen)) {
            return;
        }

        Inventory inventory = player.getInventory();
        if (didInventoryChange(inventory)) {
            overlayHandler = new GuiScreenOverlayHandler(inventory);
            gui = new JecaGui(null, false, overlayHandler, true);
            gui.initWidget(Minecraft.getInstance(), screen.width, screen.height);
            overlayHandler.setGui(gui);
        } else if (screen.width != gui.width || screen.height != gui.height) {
            gui.initWidget(Minecraft.getInstance(), screen.width, screen.height);
        }

        gui.setGraphics(graphics);
        mouseX = gui.getGlobalMouseX();
        mouseY = gui.getGlobalMouseY();

        graphics.pose().pushPose();
        graphics.pose().translate(gui.getGuiLeft(), gui.getGuiTop(), 0);
        overlayHandler.onDraw(gui, mouseX, mouseY);
        graphics.pose().popPose();

        List<String> tooltip = new ArrayList<>();
        overlayHandler.onTooltip(gui, mouseX, mouseY, tooltip);
        gui.drawHoveringText(graphics, tooltip, mouseX + gui.getGuiLeft(), mouseY + gui.getGuiTop(), minecraft.font);
        if (cachedTooltipEvent != null) {
            renderCachedTooltip(graphics, minecraft.font);
            cachedTooltipEvent = null;
        }
    }

    /**
     * Re-implements {@code GuiGraphics#renderTooltipInternal} using only public API. That method is
     * private in vanilla, and on NeoForge the access-widened call crashes at runtime (the class is
     * patched separately from the plain client jar our access widener targets), so we draw the
     * deferred tooltip ourselves via the same public building blocks Mojang's method is built from.
     */
    private void renderCachedTooltip(GuiGraphics graphics, Font font) {
        @SuppressWarnings("unchecked")
        List<ClientTooltipComponent> components = (List<ClientTooltipComponent>) cachedTooltipEvent.one;
        int mouseX = cachedTooltipEvent.two;
        int mouseY = cachedTooltipEvent.three;
        if (components.isEmpty())
            return;

        int width = 0;
        int height = components.size() == 1 ? -2 : 0;
        for (ClientTooltipComponent c : components) {
            width = Math.max(width, c.getWidth(font));
            height += c.getHeight();
        }

        Vector2ic pos = DefaultTooltipPositioner.INSTANCE.positionTooltip(
                graphics.guiWidth(), graphics.guiHeight(), mouseX, mouseY, width, height);
        int left = pos.x();
        int top = pos.y();
        int z = 400;

        graphics.pose().pushPose();
        TooltipRenderUtil.renderTooltipBackground(graphics, left, top, width, height, z);
        graphics.pose().translate(0.0F, 0.0F, z);

        int lineY = top;
        for (int i = 0; i < components.size(); i++) {
            ClientTooltipComponent c = components.get(i);
            c.renderImage(font, left, lineY, graphics);
            lineY += c.getHeight() + (i == 0 ? 2 : 0);
        }
        graphics.flush();

        Matrix4f matrix = graphics.pose().last().pose();
        MultiBufferSource.BufferSource bufferSource = graphics.bufferSource();
        lineY = top;
        for (int i = 0; i < components.size(); i++) {
            ClientTooltipComponent c = components.get(i);
            c.renderText(font, left, lineY, matrix, bufferSource);
            lineY += c.getHeight() + (i == 0 ? 2 : 0);
        }
        graphics.flush();

        graphics.pose().popPose();
    }

    public EventResult onTooltip(GuiGraphics graphics, List<? extends ClientTooltipComponent> components, int x, int y) {
        if (overlayHandler == null || cachedTooltipEvent != null || !overlayHandler.hasAnyWindow())
            return pass();

        boolean overlap = overlayHandler.onTooltip(gui, x - gui.getGuiLeft(), y - gui.getGuiTop(), new ArrayList<>());
        if (!overlap)
            cachedTooltipEvent = new Trio<>(components, x, y);
        return interruptFalse();
    }

    public EventResult onMouseScroll(Minecraft client, Screen screen, double mouseX, double mouseY, double amountX, double amountY) {
        if (overlayHandler == null || !isScreenValidForOverlay(screen))
            return pass();
        return amountY != 0 && overlayHandler.onMouseScroll(gui, gui.getGlobalMouseX(), gui.getGlobalMouseY(), (int) amountY) ? interruptFalse() : pass();
    }

    public EventResult onMouseClicked(Minecraft client, Screen screen, double mouseX, double mouseY, int button) {
        if (overlayHandler == null || !isScreenValidForOverlay(screen))
            return pass();
        int xMouse = gui.getGlobalMouseX();
        int yMouse = gui.getGlobalMouseY();
        overlayHandler.onMouseFocused(gui, xMouse, yMouse, button);
        return overlayHandler.onMouseClicked(gui, xMouse, yMouse, button) ? interruptFalse() : pass();
    }

    public EventResult onMouseReleased(Minecraft minecraft, Screen screen, double mouseX, double mouseY, int button) {
        if (overlayHandler == null || !isScreenValidForOverlay(screen))
            return pass();
        return overlayHandler.onMouseReleased(gui, gui.getGlobalMouseX(), gui.getGlobalMouseY(), button) ? interruptFalse() : pass();
    }

    public EventResult onMouseDragged(Minecraft client, Screen screen, double mouseX1, double mouseY1, int button, double mouseX2, double mouseY2) {
        if (overlayHandler == null || !isScreenValidForOverlay(screen))
            return pass();
        return overlayHandler.onMouseDragged(gui, gui.getGlobalMouseX(), gui.getGlobalMouseY(), (int) mouseX2, (int) mouseY2) ? interruptFalse() : pass();
    }


    public EventResult onKeyPressed(Minecraft client, Screen screen, int keyCode, int scanCode, int modifiers) {
        if (overlayHandler == null || !isScreenValidForOverlay(screen))
            return pass();
        return overlayHandler.onKeyPressed(gui, keyCode, modifiers) ? interruptFalse() : pass();
    }

    public EventResult onKeyReleased(Minecraft client, Screen screen, int keyCode, int scanCode, int modifiers) {
        if (overlayHandler == null || !isScreenValidForOverlay(screen))
            return pass();
        return overlayHandler.onKeyReleased(gui, keyCode, modifiers) ? interruptFalse() : pass();
    }

    public EventResult onCharTyped(Minecraft minecraft, Screen screen, char codePoint, int modifiers) {
        if (overlayHandler == null || !isScreenValidForOverlay(screen))
            return pass();
        return overlayHandler.onChar(gui, codePoint, modifiers) ? interruptFalse() : pass();
    }

    private boolean didInventoryChange(Inventory inventory) {
        if (cachedInventory == null) {
            cachedInventory = new InventorySummary(inventory);
            return false;
        }

        InventorySummary newSummery = new InventorySummary(inventory);
        if (newSummery.equals(cachedInventory)) {
            return false;
        }

        cachedInventory = newSummery;
        return true;
    }

    public List<Rect2i> getGuiAreas() {
        if (overlayHandler != null && gui != null && isScreenValidForOverlay(Minecraft.getInstance().screen)) {
            return overlayHandler.getGuiExtraAreas(gui.getGuiLeft(), gui.getGuiTop());
        }
        return Collections.emptyList();
    }
}
