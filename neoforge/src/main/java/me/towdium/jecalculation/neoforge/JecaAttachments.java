package me.towdium.jecalculation.neoforge;

import me.towdium.jecalculation.JustEnoughCalculation;
import me.towdium.jecalculation.data.structure.RecordPlayer;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class JecaAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, JustEnoughCalculation.MODID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<RecordPlayer>> RECORD = ATTACHMENT_TYPES.register("record",
            () -> AttachmentType.builder((java.util.function.Supplier<RecordPlayer>) RecordPlayer::new)
                    .serialize(new IAttachmentSerializer<CompoundTag, RecordPlayer>() {
                        @Override
                        public RecordPlayer read(IAttachmentHolder holder, CompoundTag tag, HolderLookup.Provider provider) {
                            return new RecordPlayer(tag);
                        }

                        @Override
                        public CompoundTag write(RecordPlayer attachment, HolderLookup.Provider provider) {
                            return attachment.serialize();
                        }
                    })
                    .copyOnDeath()
                    .build());

    public static RecordPlayer getRecord(Player player) {
        return player.getData(RECORD);
    }
}
