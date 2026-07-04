package me.towdium.jecalculation.network.packets;

import dev.architectury.networking.NetworkManager;
import me.towdium.jecalculation.JecaItem;
import me.towdium.jecalculation.JustEnoughCalculation;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;
import java.util.function.Supplier;

// C2S only: sent by Controller via NetworkManager.sendToServer
public class PCalculator implements CustomPacketPayload {
    public static final Type<PCalculator> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(JustEnoughCalculation.MODID, "calculator"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PCalculator> STREAM_CODEC =
            StreamCodec.ofMember(PCalculator::write, PCalculator::new);

    ItemStack stack;
    int slot;

    public PCalculator(FriendlyByteBuf buf) {
        stack = ItemStack.STREAM_CODEC.decode((RegistryFriendlyByteBuf) buf);
        slot = buf.readInt();
    }

    public PCalculator(ItemStack stack, int slot) {
        this.stack = stack;
        this.slot = slot;
    }

    public void write(FriendlyByteBuf buf) {
        ItemStack.STREAM_CODEC.encode((RegistryFriendlyByteBuf) buf, stack);
        buf.writeInt(slot);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(Supplier<NetworkManager.PacketContext> ctx) {
        ctx.get().queue(() -> {
            Inventory inventory = Objects.requireNonNull(ctx.get().getPlayer()).getInventory();
            ItemStack calculator = inventory.getItem(slot);
            if (!calculator.isEmpty() && calculator.getItem() instanceof JecaItem) {
                inventory.setItem(slot, stack);
                return;
            }
            calculator = inventory.offhand.get(0);
            if (!calculator.isEmpty() && calculator.getItem() instanceof JecaItem) {
                inventory.offhand.set(0, stack);
            }
        });
    }
}