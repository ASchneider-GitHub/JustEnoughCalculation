package me.towdium.jecalculation.utils.neoforge;

import dev.architectury.fluid.FluidStack;
import dev.architectury.hooks.fluid.forge.FluidStackHooksForge;
import me.towdium.jecalculation.data.structure.RecordPlayer;
import me.towdium.jecalculation.neoforge.JecaAttachments;
import me.towdium.jecalculation.neoforge.JecaConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class UtilitiesImpl {
    public static CompoundTag getCap(ItemStack itemStack) {
        return null;
    }

    public static ItemStack createItemStackWithCap(Item item, int count, CompoundTag cap) {
        return new ItemStack(item, count);
    }

    public static RecordPlayer getRecord(Player player) {
        return JecaAttachments.getRecord(player);
    }

    public static boolean isClientMode() {
        return JecaConfig.clientMode.get();
    }

    public static boolean areCapsCompatible(ItemStack itemStack, ItemStack itemStack1) {
        return true;
    }

    public static FluidStack createFluidStackFromJeiIngredient(Object object) {
        if (object instanceof net.neoforged.neoforge.fluids.FluidStack fluidStack)
            return FluidStackHooksForge.fromForge(fluidStack);
        return null;
    }
}
