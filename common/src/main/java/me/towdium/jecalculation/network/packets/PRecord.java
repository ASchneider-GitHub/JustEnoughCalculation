package me.towdium.jecalculation.network.packets;

import dev.architectury.networking.NetworkManager;
import me.towdium.jecalculation.JustEnoughCalculation;
import me.towdium.jecalculation.data.Controller;
import me.towdium.jecalculation.data.label.labels.LPlaceholder;
import me.towdium.jecalculation.data.structure.RecordPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.function.Supplier;

// S2C only: sent by Controller via NetworkManager.sendToPlayer
public class PRecord implements CustomPacketPayload {
    public static final Type<PRecord> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(JustEnoughCalculation.MODID, "record"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PRecord> STREAM_CODEC =
            StreamCodec.ofMember(PRecord::write, PRecord::new);

    public static final String KEY_RECIPES = "recipes";
    public static final String KEY_LAST = "last";
    RecordPlayer record;

    public PRecord(RecordPlayer record) {
        this.record = record;
    }

    public PRecord() {
    }

    public PRecord(FriendlyByteBuf buf) {
        CompoundTag tag = Objects.requireNonNull(buf.readNbt());
        LPlaceholder.state = false;
        record = new RecordPlayer(tag);
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeNbt(record.serialize());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(Supplier<NetworkManager.PacketContext> ctx) {
        ctx.get().queue(() -> Controller.setRecordsServer(record));
    }
}
