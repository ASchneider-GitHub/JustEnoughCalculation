package me.towdium.jecalculation;

import dev.architectury.event.events.client.ClientLifecycleEvent;
import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import me.towdium.jecalculation.data.Controller;
import me.towdium.jecalculation.data.label.ILabel;
import me.towdium.jecalculation.data.label.labels.LPlaceholder;
import me.towdium.jecalculation.events.GuiScreenEventHandler;
import me.towdium.jecalculation.gui.JecaGui;
import me.towdium.jecalculation.network.packets.PCalculator;
import me.towdium.jecalculation.network.packets.PEdit;
import me.towdium.jecalculation.network.packets.PRecord;
import me.towdium.jecalculation.utils.Utilities;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;

import static me.towdium.jecalculation.gui.JecaGui.keyOpenGuiCraft;
import static me.towdium.jecalculation.gui.JecaGui.keyOpenGuiMath;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class JustEnoughCalculation {
    public static final String MODID = "jecalculation";
    public static final String MODNAME = "Just Enough Calculation";
    public static Logger logger = LogManager.getLogger(MODID);

    @Environment(EnvType.CLIENT)
    public static class Client {
        @Environment(EnvType.CLIENT)
        public static GuiScreenEventHandler GUI_HANDLER =
                new GuiScreenEventHandler();
    }

    public JustEnoughCalculation() {
        JecaItem.register();
        registerEvents();
        if (Platform.getEnv() == EnvType.CLIENT)
            registerClientEvents();

        //noinspection ResultOfMethodCallIgnored
        Utilities.config().mkdirs();
    }

    private static void registerEvents() {
        LifecycleEvent.SETUP.register(JustEnoughCalculation::setupCommon);
        PlayerEvent.PLAYER_JOIN.register(Controller.Server::onJoin);
    }

    @Environment(EnvType.CLIENT)
    private static void registerClientEvents() {
        ClientLifecycleEvent.CLIENT_SETUP.register(JustEnoughCalculation::setupClient);
        JecaGui.registerEvents();
        ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(LPlaceholder::onLogOut);
        ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(Controller.Client::onLogOut);
        // Touching the field forces Client to initialize now, which registers GUI_HANDLER's
        // SET_SCREEN/RENDER_POST hooks up front. Without this, the nested class only loads
        // whenever JEI/REI first happen to query GUI_HANDLER.getGuiAreas() for exclusion zones,
        // which is too late to catch the SET_SCREEN event for whatever screen is already open.
        Objects.requireNonNull(Client.GUI_HANDLER);
    }

    public static void setupCommon() {
        Utilities.Greetings.send(logger, MODID);

        // Each packet is only ever sent in one direction (see Controller), so it is registered
        // for that direction alone: registering the same id for both C2S and S2C (as the old
        // deprecated NetworkChannel wrapper did) makes NeoForge reject the second registration
        // with "payload already registered", since it tracks ids per-protocol regardless of flow.
        NetworkManager.registerReceiver(NetworkManager.c2s(), PCalculator.TYPE, PCalculator.STREAM_CODEC,
                (payload, context) -> payload.handle(() -> context));
        NetworkManager.registerReceiver(NetworkManager.c2s(), PEdit.TYPE, PEdit.STREAM_CODEC,
                (payload, context) -> payload.handle(() -> context));
        if (Platform.getEnv() == EnvType.CLIENT) {
            NetworkManager.registerReceiver(NetworkManager.s2c(), PRecord.TYPE, PRecord.STREAM_CODEC,
                    (payload, context) -> payload.handle(() -> context));
        }

        ILabel.initServer();
    }

    public static void setupClient(Minecraft minecraft) {
        ILabel.initClient();
        Controller.loadFromLocal();
        KeyMappingRegistry.register(keyOpenGuiCraft);
        KeyMappingRegistry.register(keyOpenGuiMath);
    }
}
