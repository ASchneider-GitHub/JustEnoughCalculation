package me.towdium.jecalculation.data.label.labels;

import me.towdium.jecalculation.JustEnoughCalculation;
import me.towdium.jecalculation.data.label.ILabel;
import me.towdium.jecalculation.data.label.ILabel.Serializer.SerializationException;
import me.towdium.jecalculation.gui.JecaGui;
import me.towdium.jecalculation.gui.Resource;
import me.towdium.jecalculation.utils.Utilities;
import com.mojang.serialization.DynamicOps;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;

/**
 * Author: towdium
 * Date:   8/11/17.
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class LItemStack extends LStack<Item> {
    public static final String IDENTIFIER = "itemStack";

    public static final String KEY_ITEM = "item";
    public static final String KEY_NBT = "nbt";
    public static final String KEY_CAP = "cap";
    public static final String KEY_F_META = "fMeta";
    public static final String KEY_F_CAP = "fCap";
    public static final String KEY_F_NBT = "fNbt";

    Item item;
    CompoundTag nbt;
    CompoundTag cap;
    boolean fMeta;
    boolean fNbt;
    boolean fCap;
    transient ItemStack rep;

    // Convert from itemStack
    public LItemStack(ItemStack is) {
        super(is.getCount(), false);
        init(is.getItem(), getCap(is), encodePatch(is.getComponentsPatch()), false, false, false);
    }

    public LItemStack(CompoundTag tag) {
        super(tag);
        String id = tag.getString(KEY_ITEM);
        Optional<Item> i = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(id));
        if (i.isEmpty()) throw new SerializationException("Item " + id + " cannot be resolved, ignoring");
        init(i.get(), tag.contains(KEY_CAP) ? tag.getCompound(KEY_CAP) : null,
                tag.contains(KEY_NBT) ? tag.getCompound(KEY_NBT) : null,
                tag.getBoolean(KEY_F_META),
                tag.getBoolean(KEY_F_CAP),
                tag.getBoolean(KEY_F_NBT)
        );
    }

    @Override
    public Item get() {
        return item;
    }

    @Override
    public Context<Item> getContext() {
        return Context.ITEM;
    }

    private LItemStack(LItemStack lis) {
        super(lis);
        item = lis.item;
        nbt = lis.nbt == null ? null : lis.nbt.copy();
        cap = lis.cap == null ? null : lis.cap.copy();
        fMeta = lis.fMeta;
        fNbt = lis.fNbt;
        fCap = lis.fCap;
        rep = lis.rep;
    }

    private void init(@Nullable Item item, @Nullable CompoundTag cap,
                      @Nullable CompoundTag nbt, boolean fMeta, boolean fCap, boolean fNbt) {
        Objects.requireNonNull(item);
        this.item = item;
        this.cap = cap;
        this.nbt = nbt;
        this.fMeta = fMeta;
        this.fCap = fCap;
        this.fNbt = fNbt;
        rep = Utilities.createItemStackWithCap(item, 1, this.cap);
        DataComponentPatch patch = decodePatch(this.nbt);
        if (!patch.isEmpty()) rep.applyComponents(patch);
    }

    @Nullable
    private static CompoundTag getCap(ItemStack is) {
        return Utilities.getCap(is);
    }

    // Damage is tracked separately via fMeta/rep.getDamageValue(), so it is excluded here to
    // avoid the durability-fuzzy toggle being defeated by an always-exact patch comparison.
    @Nullable
    private static CompoundTag encodePatch(DataComponentPatch patch) {
        DataComponentPatch filtered = patch.forget(type -> type == DataComponents.DAMAGE);
        if (filtered.isEmpty()) return null;
        return DataComponentPatch.CODEC.encodeStart(registryOps(), filtered)
                .resultOrPartial(JustEnoughCalculation.logger::warn)
                .map(t -> (CompoundTag) t)
                .orElse(null);
    }

    private static DataComponentPatch decodePatch(@Nullable CompoundTag tag) {
        if (tag == null) return DataComponentPatch.EMPTY;
        return DataComponentPatch.CODEC.parse(registryOps(), tag)
                .resultOrPartial(JustEnoughCalculation.logger::warn)
                .orElse(DataComponentPatch.EMPTY);
    }

    // DataComponentPatch can carry components (e.g. enchantments) backed by dynamic registries
    // that plain NbtOps can't resolve, which spams "Can't access registry ..." warnings. Route
    // through the client level's RegistryAccess when one is loaded, falling back to NbtOps
    // otherwise (e.g. before a world is joined).
    private static DynamicOps<Tag> registryOps() {
        Level level = Minecraft.getInstance().level;
        if (level == null) return NbtOps.INSTANCE;
        return RegistryOps.create(NbtOps.INSTANCE, level.registryAccess());
    }

    public static boolean merge(ILabel a, ILabel b) {
        if (a instanceof LItemStack lisA && b instanceof LItemStack lisB) {

            if (lisA.rep.getDamageValue() != lisB.rep.getDamageValue()
                    && !lisA.fMeta && !lisB.fMeta) return false;
            if (!lisA.fNbt && !lisB.fNbt) {
                if (lisA.nbt == null) {
                    if (lisB.nbt != null) return false;
                } else if (lisB.nbt == null || !lisA.nbt.equals(lisB.nbt)) return false;
            }
            if (!lisA.fCap && !lisB.fCap) {
                if (lisA.cap == null) {
                    if (lisB.cap != null) return false;
                } else if (lisB.cap == null || !lisA.cap.equals(lisB.cap)) return false;
            }
            return lisA.item == lisB.item;
        }
        return false;
    }

    public static List<ILabel> suggest(List<ILabel> iss, @Nullable Class<?> context) {
        if (iss.size() == 0) return new ArrayList<>();
        for (ILabel i : iss) if (!(i instanceof LItemStack)) return new ArrayList<>();
        LItemStack lis = (LItemStack) iss.get(0);
        boolean fMeta = false;
        boolean fNbt = false;
        boolean fCap = false;
        for (ILabel i : iss) {
            LItemStack ii = (LItemStack) i;
            if (ii.item != lis.item) return new ArrayList<>();
            if (ii.rep.getDamageValue() != lis.rep.getDamageValue() || ii.fMeta) fMeta = true;
            if (!Objects.equals(ii.nbt, lis.nbt)) fNbt = true;
            if (!Objects.equals(ii.cap, lis.cap)) fCap = true;
        }
        if (fMeta || fNbt || fCap) return Collections.singletonList(
                lis.copy().setFCap(fCap).setFMeta(fMeta).setFNbt(fNbt));
        else return new ArrayList<>();
    }

    public static List<ILabel> fallback(List<ILabel> iss, @Nullable Class<?> context) {
        List<ILabel> ret = new ArrayList<>();
        if (iss.size() == 1) {
            ILabel label = iss.get(0);
            if (!(label instanceof LItemStack lis)) return ret;
            if (lis.fCap || lis.fNbt || lis.fMeta) return new ArrayList<>();
            ret.add(lis.copy().setFMeta(true));
            ret.add(lis.copy().setFNbt(true));
            ret.add(lis.copy().setFCap(true));
            ret.add(lis.copy().setFMeta(true).setFNbt(true).setFCap(true));
        }
        return ret;
    }

    public LItemStack setFMeta(boolean f) {
        fMeta = f;
        return this;
    }

    public LItemStack setFNbt(boolean f) {
        fNbt = f;
        return this;
    }

    public LItemStack setFCap(boolean f) {
        fCap = f;
        return this;
    }

    @Override
    public void getToolTip(List<String> existing, boolean detailed) {
        super.getToolTip(existing, detailed);
        if (fMeta) existing.add(FORMAT_GREY + Utilities.I18n.get("label.item_stack.fuzzy_meta"));
        if (fNbt) existing.add(FORMAT_GREY + Utilities.I18n.get("label.item_stack.fuzzy_nbt"));
        if (fCap) existing.add(FORMAT_GREY + Utilities.I18n.get("label.item_stack.fuzzy_cap"));
        existing.add(FORMAT_BLUE + FORMAT_ITALIC + Utilities.getModName(item));
    }

    @Override
    public ItemStack getRepresentation() {
        return rep;
    }

    @Override
    public String getDisplayName() {
        return rep.getHoverName().getString();
    }

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public boolean matches(Object l) {
        if (l instanceof LItemStack lis) {
            return Objects.equals(nbt, lis.nbt)
                    && Objects.equals(cap, lis.cap)
                    && item == lis.item
                    && fNbt == lis.fNbt && super.matches(l)
                    && fCap == lis.fCap && fMeta == lis.fMeta;
        } else return false;
    }


    @Override
    public LItemStack copy() {
        return new LItemStack(this);
    }

    @Override
    public CompoundTag toNbt() {
        ResourceLocation rl = BuiltInRegistries.ITEM.getKey(item);
        if (rl == null) return ILabel.EMPTY.toNbt();
        CompoundTag ret = super.toNbt();
        ret.putString(KEY_ITEM, rl.toString());
        if (nbt != null) ret.put(KEY_NBT, nbt);
        if (cap != null) ret.put(KEY_CAP, cap);
        if (fMeta) ret.putBoolean(KEY_F_META, true);
        if (fNbt) ret.putBoolean(KEY_F_NBT, true);
        if (fCap) ret.putBoolean(KEY_F_CAP, true);
        return ret;
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void drawLabel(int xPos, int yPos, JecaGui gui, boolean hand) {
        gui.drawItemStack(xPos, yPos, rep, false, hand);
        if (fCap || fNbt || fMeta) gui.drawResource(Resource.LBL_FRAME, xPos, yPos);
        if (fCap) gui.drawResource(Resource.LBL_FR_LL, xPos, yPos);
        if (fNbt) gui.drawResource(Resource.LBL_FR_UL, xPos, yPos);
        if (fMeta) gui.drawResource(Resource.LBL_FR_UR, xPos, yPos);
    }

    @Override
    public int hashCode() {
        return (nbt == null ? 0 : nbt.hashCode()) ^ (cap == null ? 0 : cap.hashCode())
                ^ item.getDescriptionId().hashCode() ^ super.hashCode();
    }
}
