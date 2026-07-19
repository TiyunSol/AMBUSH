package com.createcomplex.ambush;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;

final class SableAmbushState extends SavedData {
    static final String DATA_NAME = "ambush_sable_structures";

    static final class Entry {
        UUID actionId;
        UUID parentActionId;
        int generationDepth;
        UUID ownerId;
        String dimension;
        String template;
        String subLevelName;
        BlockPos anchor;
        String actionJson;
        UUID subLevelId;
        long expiresAt;
        int seatAttempts;
        int postProcessAttempts;
        boolean envelopeFillApplied;
        boolean containerLootApplied;
        boolean engineBurnApplied;
        boolean throttleSignalApplied;
        long redstoneTriggerStartedAt;
        final Set<Integer> redstoneActivations = new LinkedHashSet<>();
        final Set<String> firedEvents = new LinkedHashSet<>();
        final Map<String, Long> eventLastFired = new LinkedHashMap<>();
        int initialBlockCount;
        int intactBlockCount;
        int healthSublevelCursor;
        int healthPositionCursor;
        int assemblyPhase;
        int assemblyAttempts;
        long assemblyReadyAfter;
        long clientSyncReadyAfter;
        boolean initialSyncComplete;
        long missingSince;
        boolean cleanupRequested;
        long lastCleanupRequest;
        boolean despawnEffectPlayed;
        final List<UUID> entityIds = new ArrayList<>();
        final Set<UUID> crewEntityIds = new LinkedHashSet<>();
        final Map<UUID, double[]> childAttachments = new LinkedHashMap<>();
        final Map<UUID, long[]> damageBaseline = new LinkedHashMap<>();
        final Map<UUID, Set<Long>> destroyedBlocks = new LinkedHashMap<>();
        long nextDamageCheck;
        transient boolean loadingTicketApplied;

        boolean active() { return subLevelId != null; }
    }

    private final Map<UUID, Entry> entries = new LinkedHashMap<>();

    static SableAmbushState get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
            new SavedData.Factory<>(SableAmbushState::new, SableAmbushState::load, null), DATA_NAME);
    }

    static SableAmbushState load(CompoundTag root, HolderLookup.Provider provider) {
        SableAmbushState state = new SableAmbushState();
        for (Tag raw : root.getList("entries", Tag.TAG_COMPOUND)) {
            CompoundTag tag = (CompoundTag) raw;
            try {
                Entry entry = new Entry();
                entry.actionId = tag.getUUID("action_id");
                if(tag.hasUUID("parent_action_id"))entry.parentActionId=tag.getUUID("parent_action_id");
                entry.generationDepth=tag.getInt("generation_depth");
                entry.ownerId = tag.getUUID("owner_id");
                entry.dimension = tag.getString("dimension");
                entry.template = tag.getString("template");
                entry.subLevelName = tag.getString("sublevel_name");
                entry.anchor = BlockPos.of(tag.getLong("anchor"));
                entry.actionJson = tag.getString("action_json");
                if (tag.hasUUID("sublevel_id")) entry.subLevelId = tag.getUUID("sublevel_id");
                entry.expiresAt = tag.getLong("expires_at");
                entry.seatAttempts = tag.getInt("seat_attempts");
                entry.postProcessAttempts = tag.getInt("post_process_attempts");
                entry.envelopeFillApplied = tag.getBoolean("envelope_fill_applied");
                entry.containerLootApplied = tag.getBoolean("container_loot_applied");
                entry.engineBurnApplied = tag.getBoolean("engine_burn_applied");
                entry.throttleSignalApplied = tag.getBoolean("throttle_signal_applied");
                entry.redstoneTriggerStartedAt = tag.getLong("redstone_trigger_started_at");
                for (int index : tag.getIntArray("redstone_activations")) entry.redstoneActivations.add(index);
                for (Tag fired : tag.getList("fired_events", Tag.TAG_STRING)) entry.firedEvents.add(fired.getAsString());
                CompoundTag eventTimes = tag.getCompound("event_last_fired");
                for (String key : eventTimes.getAllKeys()) entry.eventLastFired.put(key, eventTimes.getLong(key));
                entry.initialBlockCount = tag.getInt("initial_block_count");
                entry.intactBlockCount = tag.getInt("intact_block_count");
                entry.healthSublevelCursor=tag.getInt("health_sublevel_cursor");
                entry.healthPositionCursor=tag.getInt("health_position_cursor");
                entry.assemblyPhase = tag.contains("assembly_phase") ? tag.getInt("assembly_phase")
                    : entry.subLevelId != null ? 4 : 0;
                entry.assemblyAttempts = tag.getInt("assembly_attempts");
                entry.assemblyReadyAfter = tag.getLong("assembly_ready_after");
                entry.clientSyncReadyAfter = tag.getLong("client_sync_ready_after");
                entry.initialSyncComplete = !tag.contains("initial_sync_complete") || tag.getBoolean("initial_sync_complete");
                entry.missingSince = tag.getLong("missing_since");
                entry.cleanupRequested = tag.getBoolean("cleanup_requested");
                entry.lastCleanupRequest = tag.getLong("last_cleanup_request");
                entry.despawnEffectPlayed = tag.getBoolean("despawn_effect_played");
                for (Tag entity : tag.getList("entity_ids", Tag.TAG_INT_ARRAY))
                    entry.entityIds.add(NbtUtils.loadUUID(entity));
                for(Tag entity:tag.getList("crew_entity_ids",Tag.TAG_INT_ARRAY))entry.crewEntityIds.add(NbtUtils.loadUUID(entity));
                for (Tag rawAttachment : tag.getList("child_attachments", Tag.TAG_COMPOUND)) {
                    CompoundTag attachment = (CompoundTag) rawAttachment;
                    if (!attachment.hasUUID("child_id")) continue;
                    ListTag serialized = attachment.getList("constraint", Tag.TAG_DOUBLE);
                    if (serialized.size() == 10) {
                        double[] values = new double[10];
                        for (int i = 0; i < values.length; i++) values[i] = ((NumericTag) serialized.get(i)).getAsDouble();
                        entry.childAttachments.put(attachment.getUUID("child_id"), values);
                    }
                }
                for (Tag rawBaseline : tag.getList("damage_baseline", Tag.TAG_COMPOUND)) {
                    CompoundTag baseline = (CompoundTag) rawBaseline;
                    if (baseline.hasUUID("sublevel_id"))
                        entry.damageBaseline.put(baseline.getUUID("sublevel_id"), baseline.getLongArray("positions"));
                }
                for(Tag rawDestroyed:tag.getList("destroyed_blocks",Tag.TAG_COMPOUND)){
                    CompoundTag destroyed=(CompoundTag)rawDestroyed;if(!destroyed.hasUUID("sublevel_id"))continue;
                    Set<Long> positions=new LinkedHashSet<>();for(long position:destroyed.getLongArray("positions"))positions.add(position);
                    entry.destroyedBlocks.put(destroyed.getUUID("sublevel_id"),positions);
                }
                state.entries.put(entry.actionId, entry);
            } catch (RuntimeException ex) {
                Ambush.LOGGER.warn("Discarding invalid persisted Sable ambush entry", ex);
            }
        }
        return state;
    }

    Collection<Entry> entries() { return entries.values(); }
    void add(Entry entry) { entries.put(entry.actionId, entry); setDirty(); }
    void remove(UUID id) { if (entries.remove(id) != null) setDirty(); }
    void changed() { setDirty(); }

    @Override public CompoundTag save(CompoundTag root, HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        for (Entry entry : entries.values()) {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("action_id", entry.actionId);
            if(entry.parentActionId!=null)tag.putUUID("parent_action_id",entry.parentActionId);
            tag.putInt("generation_depth",entry.generationDepth);
            tag.putUUID("owner_id", entry.ownerId);
            tag.putString("dimension", entry.dimension);
            tag.putString("template", entry.template);
            tag.putString("sublevel_name", entry.subLevelName);
            tag.putLong("anchor", entry.anchor.asLong());
            tag.putString("action_json", entry.actionJson);
            if (entry.subLevelId != null) tag.putUUID("sublevel_id", entry.subLevelId);
            tag.putLong("expires_at", entry.expiresAt);
            tag.putInt("seat_attempts", entry.seatAttempts);
            tag.putInt("post_process_attempts", entry.postProcessAttempts);
            tag.putBoolean("envelope_fill_applied", entry.envelopeFillApplied);
            tag.putBoolean("container_loot_applied", entry.containerLootApplied);
            tag.putBoolean("engine_burn_applied", entry.engineBurnApplied);
            tag.putBoolean("throttle_signal_applied", entry.throttleSignalApplied);
            tag.putLong("redstone_trigger_started_at", entry.redstoneTriggerStartedAt);
            tag.putIntArray("redstone_activations", entry.redstoneActivations.stream().mapToInt(Integer::intValue).toArray());
            ListTag firedEvents = new ListTag();
            entry.firedEvents.forEach(value -> firedEvents.add(StringTag.valueOf(value)));
            tag.put("fired_events", firedEvents);
            CompoundTag eventTimes = new CompoundTag();
            entry.eventLastFired.forEach(eventTimes::putLong);
            tag.put("event_last_fired", eventTimes);
            tag.putInt("initial_block_count", entry.initialBlockCount);
            tag.putInt("intact_block_count", entry.intactBlockCount);
            tag.putInt("health_sublevel_cursor",entry.healthSublevelCursor);
            tag.putInt("health_position_cursor",entry.healthPositionCursor);
            tag.putInt("assembly_phase", entry.assemblyPhase);
            tag.putInt("assembly_attempts", entry.assemblyAttempts);
            tag.putLong("assembly_ready_after", entry.assemblyReadyAfter);
            tag.putLong("client_sync_ready_after", entry.clientSyncReadyAfter);
            tag.putBoolean("initial_sync_complete", entry.initialSyncComplete);
            tag.putLong("missing_since", entry.missingSince);
            tag.putBoolean("cleanup_requested", entry.cleanupRequested);
            tag.putLong("last_cleanup_request", entry.lastCleanupRequest);
            tag.putBoolean("despawn_effect_played", entry.despawnEffectPlayed);
            ListTag entities = new ListTag();
            entry.entityIds.forEach(id -> entities.add(NbtUtils.createUUID(id)));
            tag.put("entity_ids", entities);
            ListTag crew=new ListTag();entry.crewEntityIds.forEach(id->crew.add(NbtUtils.createUUID(id)));tag.put("crew_entity_ids",crew);
            ListTag attachments = new ListTag();
            entry.childAttachments.forEach((childId, values) -> {
                CompoundTag attachment = new CompoundTag();
                attachment.putUUID("child_id", childId);
                ListTag serialized = new ListTag();
                for (double value : values) serialized.add(DoubleTag.valueOf(value));
                attachment.put("constraint", serialized);
                attachments.add(attachment);
            });
            tag.put("child_attachments", attachments);
            ListTag baseline = new ListTag();
            entry.damageBaseline.forEach((subLevelId, positions) -> {
                CompoundTag subLevel = new CompoundTag();
                subLevel.putUUID("sublevel_id", subLevelId);
                subLevel.putLongArray("positions", positions);
                baseline.add(subLevel);
            });
            tag.put("damage_baseline", baseline);
            ListTag destroyed=new ListTag();entry.destroyedBlocks.forEach((sublevelId,positions)->{CompoundTag item=new CompoundTag();item.putUUID("sublevel_id",sublevelId);item.putLongArray("positions",positions.stream().mapToLong(Long::longValue).toArray());destroyed.add(item);});tag.put("destroyed_blocks",destroyed);
            list.add(tag);
        }
        root.put("entries", list);
        return root;
    }
}
