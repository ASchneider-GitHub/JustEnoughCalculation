package me.towdium.jecalculation.neoforge;

import dev.architectury.utils.EnvExecutor;
import dev.architectury.utils.Env;
import me.towdium.jecalculation.JecaCommand;
import me.towdium.jecalculation.JustEnoughCalculation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(JustEnoughCalculation.MODID)
public class JustEnoughCalculationNeoForge {
    public JustEnoughCalculationNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        JecaAttachments.ATTACHMENT_TYPES.register(modEventBus);
        //noinspection InstantiationOfUtilityClass
        new JustEnoughCalculation();
        modContainer.registerConfig(ModConfig.Type.COMMON, JecaConfig.common);

        EnvExecutor.runInEnv(Env.CLIENT, () -> () -> NeoForge.EVENT_BUS.addListener(
                (RegisterClientCommandsEvent event) -> JecaCommand.register(event.getDispatcher())));
    }
}
