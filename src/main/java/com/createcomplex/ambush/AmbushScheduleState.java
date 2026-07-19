package com.createcomplex.ambush;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;

import java.util.*;

final class AmbushScheduleState extends SavedData {
    private static final String DATA_NAME = "ambush_scheduled_actions";

    static final class Entry {
        UUID id;
        UUID ownerId;
        String dimension;
        long dueGameTime;
        String actionJson;
        Vec3 origin;
    }

    private final Map<UUID, Entry> entries = new LinkedHashMap<>();

    static AmbushScheduleState get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
            new SavedData.Factory<>(AmbushScheduleState::new, AmbushScheduleState::load, null), DATA_NAME);
    }

    static AmbushScheduleState load(CompoundTag root, HolderLookup.Provider provider) {
        AmbushScheduleState state = new AmbushScheduleState();
        for (Tag raw : root.getList("entries", Tag.TAG_COMPOUND)) {
            CompoundTag tag = (CompoundTag) raw;
            if (!tag.hasUUID("id") || !tag.hasUUID("owner_id")) continue;
            Entry entry = new Entry();
            entry.id = tag.getUUID("id");
            entry.ownerId = tag.getUUID("owner_id");
            entry.dimension = tag.getString("dimension");
            entry.dueGameTime = tag.getLong("due_game_time");
            entry.actionJson = tag.getString("action_json");
            if (tag.getBoolean("has_origin")) entry.origin = new Vec3(tag.getDouble("origin_x"), tag.getDouble("origin_y"), tag.getDouble("origin_z"));
            state.entries.put(entry.id, entry);
        }
        return state;
    }

    Collection<Entry> entries() { return entries.values(); }
    void add(Entry entry) { entries.put(entry.id, entry); setDirty(); }
    void remove(UUID id) { if (entries.remove(id) != null) setDirty(); }
    void reschedule(Entry entry, long dueGameTime) { entry.dueGameTime = dueGameTime; setDirty(); }

    @Override public CompoundTag save(CompoundTag root, HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        for (Entry entry : entries.values()) {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("id", entry.id);
            tag.putUUID("owner_id", entry.ownerId);
            tag.putString("dimension", entry.dimension);
            tag.putLong("due_game_time", entry.dueGameTime);
            tag.putString("action_json", entry.actionJson);
            tag.putBoolean("has_origin", entry.origin != null);
            if (entry.origin != null) {
                tag.putDouble("origin_x", entry.origin.x);
                tag.putDouble("origin_y", entry.origin.y);
                tag.putDouble("origin_z", entry.origin.z);
            }
            list.add(tag);
        }
        root.put("entries", list);
        return root;
    }
}
