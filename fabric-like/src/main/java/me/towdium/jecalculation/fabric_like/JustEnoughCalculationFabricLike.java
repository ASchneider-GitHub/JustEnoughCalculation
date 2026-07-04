package me.towdium.jecalculation.fabric_like;

import dev.architectury.platform.Platform;
import me.towdium.jecalculation.JecaCommand;
import me.towdium.jecalculation.JecaItem;
import me.towdium.jecalculation.JustEnoughCalculation;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.world.item.CreativeModeTabs;

public class JustEnoughCalculationFabricLike {

    public JustEnoughCalculationFabricLike() {
        //noinspection InstantiationOfUtilityClass
        new JustEnoughCalculation();
        if (Platform.getEnv() == EnvType.CLIENT)
            ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> JecaCommand.register(dispatcher));
        JecaConfig.load();
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries -> {
            entries.accept(JecaItem.CRAFT.get());
            entries.accept(JecaItem.MATH.get());
        });
    }

}
