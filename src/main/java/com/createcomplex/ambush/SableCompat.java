package com.createcomplex.ambush;

import com.google.gson.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.joml.Quaterniond;
import org.joml.Vector3d;

/** Aeronautics Discovery owns assembly; this adapter owns Ambush correlation and entities. */
final class SableCompat {
    private static final Gson GSON = new Gson();
    private static final Set<String> ACTION_TYPES = Set.of("sable_structure", "sable_sublevel", "sable_sublevel_direct");

    static void releaseLoadingTicketsForShutdown(MinecraftServer server) {
        if (!ModList.get().isLoaded("sable")) return;
        int released = 0;
        try {
            Class<?> containerType = Class.forName("dev.ryanhcode.sable.api.sublevel.SubLevelContainer");
            Object ticketType = Class.forName("dev.ryanhcode.sable.api.sublevel.ticket.SubLevelLoadingTicketType")
                .getField("COMMAND_FORCED").get(null);
            Object unit = Class.forName("net.minecraft.util.Unit").getField("INSTANCE").get(null);
            SableAmbushState state = SableAmbushState.get(server);
            for (ServerLevel level : server.getAllLevels()) {
                Object container = callStatic(containerType, "getContainer", level);
                if (container == null) continue;
                for (SableAmbushState.Entry entry : state.entries()) {
                    if (entry.subLevelId == null || !entry.dimension.equals(level.dimension().location().toString())) continue;
                    Object subLevel = call(container, "getSubLevel", entry.subLevelId);
                    if (subLevel == null) continue;
                    try {
                        call(container, "removeForceLoadTicket", subLevel, ticketType, unit);
                        entry.loadingTicketApplied = false;
                        released++;
                    } catch (ReflectiveOperationException ignored) {
                        // A missing ticket is already in the desired shutdown state.
                    }
                }
            }
            Ambush.LOGGER.info("Released {} Ambush Sable loading ticket(s) before shutdown save", released);
        } catch (Exception exception) {
            Ambush.LOGGER.warn("Could not release Ambush Sable loading tickets before shutdown; continuing normal save", exception);
        }
    }
    private static final Map<UUID, Object> LIVE_CONTEXTS = new ConcurrentHashMap<>();
    private static final Map<UUID, LinkedHashMap<String, Vec3>> STRUCTURE_ORIGINS = new ConcurrentHashMap<>();
    private static final Map<UUID, LinkedHashMap<String, UUID>> STRUCTURE_ACTIONS = new ConcurrentHashMap<>();
    private static final Map<UUID, Object> FIXED_CHILD_HANDLES = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> CHILD_ATTACHMENT_WARN_AFTER = new ConcurrentHashMap<>();
    private static final Map<UUID, net.minecraft.server.level.ServerBossEvent> BOSS_BARS = new ConcurrentHashMap<>();
    private static final Map<String,Long> STEERING_LAST_UPDATE = new ConcurrentHashMap<>();
    private static final Map<String,Float> STEERING_LAST_ANGLE = new ConcurrentHashMap<>();
    private static final Map<String,String> PROPULSION_LAST_DIRECTION = new ConcurrentHashMap<>();
    private static long HEALTH_BUDGET_TICK=Long.MIN_VALUE;
    private static int HEALTH_SCAN_BUDGET_REMAINING;
    private static final long LOAD_GRACE_TICKS = 1200;
    private static final long CLEANUP_RETRY_TICKS = 100;

    private SableCompat() {}

    private static boolean available() { return ModList.get().isLoaded("sable"); }

    static int apply(ServerPlayer player, JsonObject definition) {
        return apply(player, definition, false);
    }

    static int apply(ServerPlayer player, JsonObject definition, boolean force) {
        STRUCTURE_ORIGINS.remove(player.getUUID());
        STRUCTURE_ACTIONS.remove(player.getUUID());
        if (!available()) return 0;
        if (!definition.has("actions") || !definition.get("actions").isJsonArray()) return 0;
        int queued = 0;
        for (JsonElement element : definition.getAsJsonArray("actions")) {
            if (!element.isJsonObject()) continue;
            JsonObject action = element.getAsJsonObject();
            String type = string(action, "type", "");
            if (!surfaceAllowed(player, action)) continue;
            if ("sable_formation".equals(type)) {
                if ((!force && !ActionConditions.matches(player, action)) || !action.has("members") || !action.get("members").isJsonArray()) continue;
                for (JsonElement rawMember : action.getAsJsonArray("members")) {
                    if (!rawMember.isJsonObject()) continue;
                    JsonObject member = action.deepCopy();
                    member.remove("members");
                    member.addProperty("type", "sable_structure");
                    for (Map.Entry<String, JsonElement> override : rawMember.getAsJsonObject().entrySet())
                        member.add(override.getKey(), override.getValue().deepCopy());
                    try {
                        directAssemble(player, member);
                        queued++;
                    } catch (Exception ex) {
                        Ambush.LOGGER.warn("Sable formation member could not be queued for {}", player.getGameProfile().getName(), ex);
                        return 0;
                    }
                }
                continue;
            }
            if (!ACTION_TYPES.contains(type)) continue;
            if (!force && !ActionConditions.matches(player, action)) continue;
            try {
                directAssemble(player, action);
                queued++;
            } catch (Exception ex) {
                Ambush.LOGGER.warn("Sable structure action could not be queued for {}", player.getGameProfile().getName(), ex);
            }
        }
        return queued;
    }

    static Vec3 lastStructureOrigin(ServerPlayer player) {
        LinkedHashMap<String, Vec3> origins = STRUCTURE_ORIGINS.get(player.getUUID());
        if (origins == null || origins.isEmpty()) return null;
        Vec3 last = null;
        for (Vec3 origin : origins.values()) last = origin;
        return last;
    }

    static Vec3 structureOrigin(ServerPlayer player, String key) {
        LinkedHashMap<String, Vec3> origins = STRUCTURE_ORIGINS.get(player.getUUID());
        if (origins == null || origins.isEmpty()) return null;
        if (key != null && !key.isBlank() && origins.containsKey(key)) return origins.get(key);
        return lastStructureOrigin(player);
    }

    /** Current world-space position of a completed named structure, with anchor fallback. */
    static Vec3 currentStructureOrigin(ServerPlayer player, String key) {
        UUID actionId = Optional.ofNullable(STRUCTURE_ACTIONS.get(player.getUUID()))
            .map(actions -> actions.get(key)).orElse(null);
        if (actionId == null) for (SableAmbushState.Entry entry : SableAmbushState.get(player.server).entries()) {
            if (!player.getUUID().equals(entry.ownerId)) continue;
            try {
                JsonObject action = JsonParser.parseString(entry.actionJson).getAsJsonObject();
                if (key.equals(string(action, "structure_key", "default"))) { actionId = entry.actionId; break; }
            } catch (RuntimeException ignored) { }
        }
        if (actionId != null) {
            String tag = actionEntityTag(actionId);
            for (Entity entity : player.serverLevel().getEntities().getAll())
                if (entity.getTags().contains(tag)) return worldEyePosition(entity);
        }
        return structureOrigin(player, key);
    }

    /** Projects a seated entity's plot-space eye position into the parent world. */
    static Vec3 worldEyePosition(Entity entity) {
        if (!available()) return entity.getEyePosition();
        try {
            Object helper = Class.forName("dev.ryanhcode.sable.Sable").getField("HELPER").get(null);
            Object subLevel = call(helper, "getTrackingOrVehicleSubLevel", entity);
            if (subLevel == null) subLevel = call(helper, "getContaining", entity);
            return subLevel == null ? entity.getEyePosition()
                : (Vec3) call(helper, "projectOutOfSubLevel", entity.level(), entity.getEyePosition());
        } catch (Exception ignored) {
            return entity.getEyePosition();
        }
    }

    static Vec3 localAimDirection(Entity entity, Vec3 worldUnitDirection) {
        if (!available()) return worldUnitDirection;
        try {
            Object helper = Class.forName("dev.ryanhcode.sable.Sable").getField("HELPER").get(null);
            Object subLevel = call(helper, "getTrackingOrVehicleSubLevel", entity);
            if (subLevel == null) subLevel = call(helper, "getContaining", entity);
            if (subLevel == null) return worldUnitDirection;
            return (Vec3) call(call(subLevel, "logicalPose"), "transformNormalInverse", worldUnitDirection);
        } catch (Exception ignored) { return worldUnitDirection; }
    }

    static List<Entity> airshipCrew(ServerPlayer player) {
        if (!available()) return List.of();
        List<Entity> result = new ArrayList<>();
        try {
            for (SableAmbushState.Entry entry : SableAmbushState.get(player.server).entries()) {
                if (!player.getUUID().equals(entry.ownerId) || entry.subLevelId == null
                    || !player.serverLevel().dimension().location().toString().equals(entry.dimension)) continue;
                Object subLevel = findSubLevel(player.serverLevel(), entry.subLevelId, null);
                if (subLevel == null) continue;
                Level subLevelLevel = (Level) call(subLevel, "getLevel");
                AABB bounds = (AABB) call(call(call(subLevel, "getPlot"), "getBoundingBox"), "toAABB");
                result.addAll(subLevelLevel.getEntitiesOfClass(Entity.class, bounds.inflate(1),
                    entity -> entity.getTags().contains("ambush_airship_aggro")));
            }
        } catch (Exception ex) { Ambush.LOGGER.debug("Could not enumerate seated Ambush crew", ex); }
        return result;
    }

    private static void directAssemble(ServerPlayer player, JsonObject action) throws Exception {
        ServerLevel level = player.serverLevel();
        ResourceLocation templateId = ResourceLocation.parse(action.has("template") ? action.get("template").getAsString() : string(action, "schematic", ""));
        StructureTemplate template = level.getServer().getStructureManager().get(templateId).orElseThrow();
        BlockPos requested;
        if (action.has("spawn_distance")) {
            double distance = Math.max(0, Math.min(512, number(action, "spawn_distance", 0)));
            double angle = action.has("spawn_bearing_degrees")
                ? Math.toRadians(player.getYRot() + 90 + number(action, "spawn_bearing_degrees", 0))
                : action.has("spawn_angle_degrees")
                    ? Math.toRadians(number(action, "spawn_angle_degrees", 0))
                    : level.random.nextDouble() * Math.PI * 2;
            requested = BlockPos.containing(
                player.getX() + Math.cos(angle) * distance + integer(action, "offset_x", 0),
                player.getY() + integer(action, "offset_y", 0),
                player.getZ() + Math.sin(angle) * distance + integer(action, "offset_z", 0));
        } else {
            requested = player.blockPosition().offset(integer(action,"offset_x",0),integer(action,"offset_y",0),integer(action,"offset_z",0));
        }
        BlockPos anchor = selectAnchor(level, action, template, requested);
        Vec3i size=template.getSize();
        double yawDegrees=resolveYawDegrees(player,anchor,action);
        action.addProperty("resolved_yaw_degrees",yawDegrees);
        action.addProperty("resolved_throttle_signal",resolveThrottleSignal(player,action));
        UUID actionId=UUID.randomUUID(); String name="ambush_"+actionId.toString().replace("-","");
        SableAmbushState.Entry entry=new SableAmbushState.Entry(); entry.actionId=actionId; entry.ownerId=player.getUUID(); entry.dimension=level.dimension().location().toString(); entry.template=templateId.toString(); entry.subLevelName=name; entry.anchor=anchor; entry.actionJson=GSON.toJson(action); entry.parentActionId=action.has("_ambush_parent_action_id")?UUID.fromString(action.get("_ambush_parent_action_id").getAsString()):null; entry.generationDepth=Math.max(integer(action,"_ambush_generation_depth",0),AmbushRuntime.currentGenerationDepth()); entry.assemblyPhase=0; entry.assemblyAttempts=0; entry.assemblyReadyAfter=level.getGameTime(); entry.missingSince=level.getGameTime(); entry.initialSyncComplete=false;
        SableAmbushState.get(player.server).add(entry); String structureKey=string(action,"structure_key","default"); STRUCTURE_ORIGINS.computeIfAbsent(player.getUUID(),ignored->new LinkedHashMap<>()).put(structureKey,Vec3.atCenterOf(anchor)); STRUCTURE_ACTIONS.computeIfAbsent(player.getUUID(),ignored->new LinkedHashMap<>()).put(structureKey,actionId);
        Ambush.LOGGER.info("Queued Sable ambush template={} anchor={} yaw={} owner={}; awaiting staged Simulated assembly",templateId,anchor.toShortString(),yawDegrees,player.getUUID());
    }

    private static void clearPlacedTemplate(ServerLevel level,BlockPos min,BlockPos max){
        for(BlockPos pos:BlockPos.betweenClosed(min,max)) if(!level.getBlockState(pos).isAir()) level.setBlock(pos,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3);
    }
    private static void removeNewSublevels(Object container,Set<UUID> existing){
        if(container==null)return; try{Object reason=Enum.valueOf((Class<? extends Enum>)Class.forName("dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason").asSubclass(Enum.class),"REMOVED"); for(Object sub:(List<?>)call(container,"getAllSubLevels")){UUID id=(UUID)call(sub,"getUniqueId"); if(!existing.contains(id))call(container,"removeSubLevel",sub,reason);}}catch(Exception cleanup){Ambush.LOGGER.warn("Could not remove failed direct Sable assembly",cleanup);}
    }

    /** Mirrors Discovery 1.4.4's place -> glue readiness -> Simulated assembly -> valid body -> rotate -> ticket sequence. */
    private static void advanceAssembly(ServerLevel level, SableAmbushState.Entry entry, SableAmbushState state) throws Exception {
        long now = level.getGameTime();
        JsonObject action = JsonParser.parseString(entry.actionJson).getAsJsonObject();
        StructureTemplate template = level.getServer().getStructureManager().get(ResourceLocation.parse(entry.template)).orElseThrow();
        Vec3i size = template.getSize();
        BlockPos max = entry.anchor.offset(size.getX() - 1, size.getY() - 1, size.getZ() - 1);

        if (entry.assemblyPhase == 0) {
            forceTemplateChunks(level, entry.anchor, size, true);
            if (!allTemplateChunksTicking(level, entry.anchor, size)) return;
            if (!template.placeInWorld(level, entry.anchor, entry.anchor,
                new StructurePlaceSettings(), level.random, 2)) {
                forceTemplateChunks(level, entry.anchor, size, false);
                throw new IllegalStateException("Could not place Sable staging template: " + entry.template);
            }
            entry.assemblyPhase = 1;
            entry.assemblyReadyAfter = now + 1;
            entry.missingSince = now;
            state.changed();
            return;
        }

        if (entry.assemblyPhase == 1) {
            if (now < entry.assemblyReadyAfter) return;
            // Direct port of Discovery's PlaceBlocksStep.forceEntityUpdate:
            // template placement is followed by one delayed entity tick before
            // the Simulated/Sable assembly starts.
            if (entry.assemblyReadyAfter != 0) {
                forceDiscoveryEntityUpdate(level, entry.anchor, max);
                entry.assemblyReadyAfter = 0;
                state.changed();
            }
            AABB templateBounds = new AABB(entry.anchor.getX(), entry.anchor.getY(), entry.anchor.getZ(), max.getX(), max.getY(), max.getZ());
            if (!hasHoneyGlue(level, templateBounds.inflate(1))) {
                if (now - entry.missingSince >= 200) retryStagedAssembly(level, entry, state, action, template, max,
                    new IllegalStateException("staging honey glue did not become ready"));
                return;
            }

            AssemblyStart start = findAssemblyStart(level, entry.anchor, max);
            if (start == null) {
                retryStagedAssembly(level, entry, state, action, template, max,
                    new IllegalStateException("staging template contains no assembly block"));
                return;
            }

            Object container = callStatic(Class.forName("dev.ryanhcode.sable.api.sublevel.SubLevelContainer"), "getContainer", level);
            Set<UUID> existing = new HashSet<>();
            if (container != null) for (Object sub : (List<?>)call(container, "getAllSubLevels")) existing.add((UUID)call(sub, "getUniqueId"));
            try {
                Object result = callStatic(Class.forName("dev.simulated_team.simulated.util.SimAssemblyHelper"),
                    "assembleFromSingleBlock", level, start.assemblerPos(), start.blockToAssemble(), true, true);
                if (result == null) throw new IllegalStateException("Simulated returned no assembly result");
                Object sub = call(result, "subLevel");
                entry.subLevelId = (UUID)call(sub, "getUniqueId");
                entry.assemblyPhase = 2;
                entry.assemblyReadyAfter = now;
                entry.missingSince = now;
                state.changed();
                Ambush.LOGGER.info("Simulated assembled Sable ambush template={} sublevel={}; awaiting valid physics body",
                    entry.template, entry.subLevelId);
            } catch (Exception failure) {
                removeNewSublevels(container, existing);
                retryStagedAssembly(level, entry, state, action, template, max, failure);
            }
            return;
        }

        Object sub = findSubLevel(level, entry.subLevelId, null);
        if (sub == null) {
            if (now - entry.missingSince >= LOAD_GRACE_TICKS) {
                Ambush.LOGGER.warn("Sable ambush sublevel vanished while assembly was finalizing: action={} sublevel={}", entry.actionId, entry.subLevelId);
                state.remove(entry.actionId);
            }
            return;
        }

        if (entry.assemblyPhase == 2) {
            if (now < entry.assemblyReadyAfter) return;
            Object handle = callStatic(Class.forName("dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle"), "of", sub);
            if (!(boolean)call(handle, "isValid")) return;
            Vector3d bodyPosition = new Vector3d(
                entry.anchor.getX() + size.getX() / 2.0,
                entry.anchor.getY() + size.getY() / 2.0,
                entry.anchor.getZ() + size.getZ() / 2.0);
            double yawRadians = Math.toRadians(number(action, "resolved_yaw_degrees", 0));
            call(handle, "teleport", bodyPosition, new Quaterniond().rotationY(yawRadians));
            entry.assemblyPhase = 3;
            // Discovery 1.4.4 yields exactly one level tick after rotating the valid body.
            entry.assemblyReadyAfter = now + 1;
            state.changed();
            return;
        }

        if (entry.assemblyPhase == 3 && now >= entry.assemblyReadyAfter) {
            Object container = callStatic(Class.forName("dev.ryanhcode.sable.api.sublevel.SubLevelContainer"), "getContainer", level);
            if (container == null) return;
            Object ticketType = Class.forName("dev.ryanhcode.sable.api.sublevel.ticket.SubLevelLoadingTicketType").getField("COMMAND_FORCED").get(null);
            Object unit = Class.forName("net.minecraft.util.Unit").getField("INSTANCE").get(null);
            call(container, "addForceLoadTicket", sub, ticketType, unit);
            entry.loadingTicketApplied = true;
            call(sub, "setName", entry.subLevelName);
            // Do not manually re-notify Sable's tracking system here. Simulated
            // already registered this plot during assembly; a second
            // onSubLevelAdded notification emits a duplicate start-tracking packet
            // and crashes clients with "Plot already exists".
            long lifetime = lifetimeTicks(action);
            entry.expiresAt = lifetime < 0 ? 0 : now + lifetime;
            entry.assemblyPhase = 4;
            entry.clientSyncReadyAfter = now + 2;
            entry.missingSince = 0;
            forceTemplateChunks(level, entry.anchor, size, false);
            state.changed();
            Ambush.LOGGER.info("Finalized Sable ambush assembly template={} sublevel={}; normal Sable client tracking now owns visibility",
                entry.template, entry.subLevelId);
        }
    }

    private static void forceTemplateChunks(ServerLevel level, BlockPos anchor, Vec3i size, boolean force) {
        int minX = net.minecraft.core.SectionPos.blockToSectionCoord(anchor.getX());
        int minZ = net.minecraft.core.SectionPos.blockToSectionCoord(anchor.getZ());
        int maxX = net.minecraft.core.SectionPos.blockToSectionCoord(anchor.getX() + size.getX());
        int maxZ = net.minecraft.core.SectionPos.blockToSectionCoord(anchor.getZ() + size.getZ());
        for (int x = minX; x <= maxX; x++)
            for (int z = minZ; z <= maxZ; z++)
                Ambush.SABLE_ASSEMBLY_TICKETS.forceChunk(level, anchor, x, z, force, true);
    }

    /** Exact body of Discovery's AssemblyStep.forceEntityUpdate, adapted to Ambush state. */
    private static void forceDiscoveryEntityUpdate(ServerLevel level, BlockPos min, BlockPos max) {
        AABB box = new AABB(min.getX(), min.getY(), min.getZ(), max.getX(), max.getY(), max.getZ());
        level.getEntities((Entity) null, box, entity -> true).forEach(Entity::tick);
    }

    private static boolean allTemplateChunksTicking(ServerLevel level, BlockPos anchor, Vec3i size) {
        int minX = net.minecraft.core.SectionPos.blockToSectionCoord(anchor.getX());
        int minZ = net.minecraft.core.SectionPos.blockToSectionCoord(anchor.getZ());
        int maxX = net.minecraft.core.SectionPos.blockToSectionCoord(anchor.getX() + size.getX());
        int maxZ = net.minecraft.core.SectionPos.blockToSectionCoord(anchor.getZ() + size.getZ());
        for (int x = minX; x <= maxX; x++)
            for (int z = minZ; z <= maxZ; z++)
                if (!level.getChunkSource().isPositionTicking(net.minecraft.world.level.ChunkPos.asLong(x, z)))
                    return false;
        return true;
    }

    private static boolean hasHoneyGlue(ServerLevel level, AABB bounds) {
        EntityType<?> glue = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse("simulated:honey_glue"));
        return glue != null && !level.getEntities(glue, bounds, entity -> true).isEmpty();
    }

    private static AssemblyStart findAssemblyStart(ServerLevel level, BlockPos min, BlockPos max) throws Exception {
        Class<?> assemblerClass = Class.forName("dev.simulated_team.simulated.content.blocks.physics_assembler.PhysicsAssemblerBlock");
        BlockPos firstNonAir = null;
        for (BlockPos cursor : BlockPos.betweenClosed(min, max)) {
            BlockPos pos = cursor.immutable();
            var blockState = level.getBlockState(pos);
            if (blockState.isAir()) continue;
            if (firstNonAir == null) firstNonAir = pos;
            if (!assemblerClass.isInstance(blockState.getBlock())) continue;
            net.minecraft.core.Direction facing = (net.minecraft.core.Direction)callStatic(assemblerClass, "getStickyFacing", blockState);
            BlockPos attached = pos.relative(facing);
            if (!level.getBlockState(attached).isAir()) return new AssemblyStart(pos, attached);
        }
        return firstNonAir == null ? null : new AssemblyStart(firstNonAir, firstNonAir);
    }

    private static void retryStagedAssembly(ServerLevel level, SableAmbushState.Entry entry, SableAmbushState state,
                                            JsonObject action, StructureTemplate template, BlockPos max, Exception failure) {
        entry.assemblyAttempts++;
        int maxRetries = Math.max(1, Math.min(60, integer(action, "max_retries", 5)));
        clearStagingTemplate(level, entry.anchor, max);
        if (entry.assemblyAttempts >= maxRetries || !template.placeInWorld(level, entry.anchor, entry.anchor,
            new StructurePlaceSettings(), level.random, 2)) {
            forceTemplateChunks(level, entry.anchor, template.getSize(), false);
            Ambush.LOGGER.warn("Sable ambush assembly failed permanently after {} attempt(s): action={} template={}",
                entry.assemblyAttempts, entry.actionId, entry.template, failure);
            state.remove(entry.actionId);
            return;
        }
        entry.assemblyPhase = 1;
        entry.assemblyReadyAfter = level.getGameTime() + 1;
        entry.missingSince = level.getGameTime();
        state.changed();
        Ambush.LOGGER.warn("Retrying staged Sable ambush assembly attempt {}/{}: action={} template={}",
            entry.assemblyAttempts + 1, maxRetries, entry.actionId, entry.template, failure);
    }

    private static void clearStagingTemplate(ServerLevel level, BlockPos min, BlockPos max) {
        clearPlacedTemplate(level, min, max);
        AABB bounds = new AABB(min.getX(), min.getY(), min.getZ(), max.getX() + 1, max.getY() + 1, max.getZ() + 1).inflate(2);
        level.getEntities((Entity)null, bounds, entity -> !(entity instanceof Player))
            .forEach(entity -> entity.remove(Entity.RemovalReason.DISCARDED));
    }

    private record AssemblyStart(BlockPos assemblerPos, BlockPos blockToAssemble) {}

    static void tick(ServerLevel level) {
        SableSplitCleanup.tick(level);
        if (!available()) return;
        MinecraftServer server = level.getServer();
        SableAmbushState state = SableAmbushState.get(server);
        for (SableAmbushState.Entry entry : new ArrayList<>(state.entries())) {
            if (level(server, entry.dimension) != level) continue;
            try {
                if (entry.assemblyPhase >= 0 && entry.assemblyPhase < 4) {
                    advanceAssembly(level, entry, state);
                    continue;
                }
                if (!entry.active()) {
                    UUID subLevelId = completedSubLevelId(level, entry);
                    if (subLevelId != null) {
                        entry.subLevelId = subLevelId;
                        entry.missingSince = 0;
                        JsonObject action = JsonParser.parseString(entry.actionJson).getAsJsonObject();
                        long lifetime = lifetimeTicks(action);
                        entry.expiresAt = lifetime < 0 ? 0 : level.getGameTime() + lifetime;
                        entry.redstoneTriggerStartedAt = level.getGameTime();
                        spawnConfiguredEntities(level, entry, action);
                        entry.initialSyncComplete = true;
                        state.changed();
                        Ambush.LOGGER.info("Completed Sable ambush action={} template={} sublevel={} entities={}",
                            entry.actionId, entry.template, entry.subLevelId, entry.entityIds.size());
                    } else if (entry.subLevelId == null) {
                        long now = level.getGameTime();
                        if (entry.missingSince == 0) {
                            entry.missingSince = now;
                            // The sublevel is already gone. Remove tagged riders immediately instead
                            // of leaving plot-space mobs floating until the persistence grace expires.
                            discardOwnedEntities(level, entry);
                            state.changed();
                        } else if (now - entry.missingSince >= LOAD_GRACE_TICKS) {
                            Ambush.LOGGER.warn("Sable ambush assembly ended without a sublevel after load grace: action={} template={}", entry.actionId, entry.template);
                            LIVE_CONTEXTS.remove(entry.actionId);
                            state.remove(entry.actionId);
                        }
                    } else if (entry.missingSince != 0) {
                        entry.missingSince = 0;
                        state.changed();
                    }
                    continue;
                }

                if (!subLevelExists(level, entry.subLevelId)) {
                    if (entry.cleanupRequested) {
                        finishCleanup(level, entry, state);
                    } else {
                        long now = level.getGameTime();
                        if (entry.missingSince == 0) {
                            entry.missingSince = now;
                            state.changed();
                        } else if (now - entry.missingSince >= LOAD_GRACE_TICKS) {
                            Ambush.LOGGER.info("Sable ambush sublevel disappeared externally after load grace: action={} sublevel={}", entry.actionId, entry.subLevelId);
                            finishCleanup(level, entry, state);
                        }
                    }
                } else {
                    ensureRuntimeLoadingTicket(level, entry);
                    if (entry.missingSince != 0) {
                        entry.missingSince = 0;
                        state.changed();
                    }
                    JsonObject action = JsonParser.parseString(entry.actionJson).getAsJsonObject();
                    if (entry.redstoneTriggerStartedAt == 0) {
                        entry.redstoneTriggerStartedAt = level.getGameTime();
                        state.changed();
                    }
                    if (entry.cleanupRequested) {
                        if (level.getGameTime() - entry.lastCleanupRequest >= CLEANUP_RETRY_TICKS)
                            requestCleanup(level, entry, state);
                        continue;
                    }
                    if (!entry.initialSyncComplete) {
                        if (level.getGameTime() < entry.clientSyncReadyAfter) continue;
                        Object subLevel = findSubLevel(level, entry.subLevelId, null);
                        if (subLevel == null) continue;
                        spawnConfiguredEntities(level,entry,action);
                        entry.initialSyncComplete=true;
                        state.changed();
                        Ambush.LOGGER.info("Sable post-assembly delay complete; spawned configured entities: action={} sublevel={} entities={}",
                            entry.actionId,entry.subLevelId,entry.entityIds.size());
                    }
                    if (entry.seatAttempts < 40) populateRequestedSeats(level, entry, state);
                    try {
                        ensureChildAttachments(level, entry, action, state);
                    } catch (Exception attachmentFailure) {
                        long now = level.getGameTime();
                        if (now >= CHILD_ATTACHMENT_WARN_AFTER.getOrDefault(entry.actionId, 0L)) {
                            CHILD_ATTACHMENT_WARN_AFTER.put(entry.actionId, now + 100);
                            Ambush.LOGGER.warn("Could not attach Sable child sublevels yet; retrying: action={} parent={}",
                                entry.actionId, entry.subLevelId, attachmentFailure);
                        }
                    }
                    applyPostAssemblyData(level, entry, action, state);
                    applyRedstoneActivations(level, entry, action, state);
                    applySteeringControls(level,entry,action);
                    BlockHealth blockHealth=updateBlockHealth(level,entry,action,state);
                    BlockHealth encounterHealth=sharedBossHealth(entry,action,state,blockHealth);
                    boolean sharedFollower=action.has("shared_boss_id")&&!booleanValue(action,"shared_boss_leader",false);
                    if(!sharedFollower)updateBossBar(level,entry,action,encounterHealth);
                    if(!sharedFollower)fireSableEvents(level,entry,action,state,encounterHealth,false);
                    if(!sharedFollower&&encounterHealth!=null&&encounterHealth.intact()<=0)fireSableEvents(level,entry,action,state,encounterHealth,true);
                    if (!sharedFollower&&damageCleanupReached(action, encounterHealth)) {
                        fireSableEvents(level,entry,action,state,encounterHealth,true);
                        try {
                            playDespawnEffect(level, entry, action, state);
                        } catch (Exception effectFailure) {
                            entry.despawnEffectPlayed = true;
                            state.changed();
                            Ambush.LOGGER.warn("Sable damage-cleanup effect failed; cleanup will continue: action={} sublevel={}",
                                entry.actionId, entry.subLevelId, effectFailure);
                        }
                        requestSharedBossCleanup(level,entry,action,state);
                        continue;
                    }
                    if (entry.expiresAt <= 0 || level.getGameTime() < entry.expiresAt) continue;
                    try {
                        playDespawnEffect(level, entry, action, state);
                    } catch (Exception effectFailure) {
                        entry.despawnEffectPlayed = true;
                        state.changed();
                        Ambush.LOGGER.warn("Sable despawn effect failed; cleanup will continue: action={} sublevel={}",
                            entry.actionId, entry.subLevelId, effectFailure);
                    }
                    requestCleanup(level, entry, state);
                }
            } catch (Exception ex) {
                Ambush.LOGGER.warn("Sable ambush lifecycle check failed for action={}", entry.actionId, ex);
            }
        }
    }

    private static void ensureRuntimeLoadingTicket(ServerLevel level, SableAmbushState.Entry entry) throws Exception {
        if (entry.loadingTicketApplied || entry.subLevelId == null) return;
        Object container = callStatic(Class.forName("dev.ryanhcode.sable.api.sublevel.SubLevelContainer"), "getContainer", level);
        if (container == null) return;
        Object subLevel = call(container, "getSubLevel", entry.subLevelId);
        if (subLevel == null) return;
        Object ticketType = Class.forName("dev.ryanhcode.sable.api.sublevel.ticket.SubLevelLoadingTicketType")
            .getField("COMMAND_FORCED").get(null);
        Object unit = Class.forName("net.minecraft.util.Unit").getField("INSTANCE").get(null);
        call(container, "addForceLoadTicket", subLevel, ticketType, unit);
        entry.loadingTicketApplied = true;
        Ambush.LOGGER.debug("Restored runtime loading ticket for Ambush Sable sublevel={}", entry.subLevelId);
    }

    static void sanitizeBeforePhysics(MinecraftServer server){
        if(!available())return;
        try{
            ServerLevel level=server.overworld();
            Object container=callStatic(Class.forName("dev.ryanhcode.sable.api.sublevel.SubLevelContainer"),"getContainer",level);
            if(container==null)return;
            Object reason=Enum.valueOf((Class<? extends Enum>)Class.forName("dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason").asSubclass(Enum.class),"REMOVED");
            for(Object sub:new ArrayList<>((List<?>)call(container,"getAllSubLevels"))){
                Object center=call(call(sub,"getMassTracker"),"getCenterOfMass");
                Object pose=call(sub,"logicalPose");
                Object position=pose==null?null:call(pose,"position");
                boolean sane=center!=null && position!=null && finiteVector(position) && boundedVector(position,1_000_000.0);
                if(!sane){ Ambush.LOGGER.warn("Removing invalid Sable sublevel before physics: {}",call(sub,"getUniqueId")); call(container,"removeSubLevel",sub,reason); }
            }
        }catch(Exception ex){ Ambush.LOGGER.warn("Could not sanitize persisted Sable sublevels before physics",ex); }
    }

    private static boolean finiteVector(Object vector) {
        try { return Double.isFinite(((Number)call(vector,"x")).doubleValue()) && Double.isFinite(((Number)call(vector,"y")).doubleValue()) && Double.isFinite(((Number)call(vector,"z")).doubleValue()); }
        catch(Exception ignored){ return false; }
    }
    private static boolean boundedVector(Object vector,double limit) {
        try { return Math.abs(((Number)call(vector,"x")).doubleValue())<=limit && Math.abs(((Number)call(vector,"y")).doubleValue())<=limit && Math.abs(((Number)call(vector,"z")).doubleValue())<=limit; }
        catch(Exception ignored){ return false; }
    }

    private static UUID completedSubLevelId(ServerLevel level, SableAmbushState.Entry entry) throws Exception {
        if (entry.subLevelId != null && subLevelExists(level, entry.subLevelId)) return entry.subLevelId;
        Object subLevel = findSubLevel(level, null, entry.subLevelName);
        return subLevel == null ? null : (UUID) call(subLevel, "getUniqueId");
    }

    private static boolean subLevelExists(ServerLevel level, UUID id) throws Exception {
        return findSubLevel(level, id, null) != null;
    }

    private static Object findSubLevel(ServerLevel level, UUID id, String name) throws Exception {
        Class<?> containerClass = Class.forName("dev.ryanhcode.sable.api.sublevel.SubLevelContainer");
        Object container = callStatic(containerClass, "getContainer", level);
        if (container == null) return null;
        if (id != null) return call(container, "getSubLevel", id);
        for (Object subLevel : (List<?>) call(container, "getAllSubLevels"))
            if (name.equals(call(subLevel, "getName"))) return subLevel;
        return null;
    }

    private static void spawnConfiguredEntities(ServerLevel level, SableAmbushState.Entry entry, JsonObject action) throws Exception {
        JsonArray entities = action.has("entities") && action.get("entities").isJsonArray()
            ? action.getAsJsonArray("entities") : legacyEntity(action);
        if (entities.isEmpty()) return;

        StructureTemplate template = level.getServer().getStructureManager().get(ResourceLocation.parse(entry.template)).orElseThrow();
        var size = template.getSize();
        double yaw = Math.toRadians(number(action, "resolved_yaw_degrees", number(action, "yaw_degrees", 0)));
        Object subLevel = findSubLevel(level, entry.subLevelId, null);
        if (subLevel == null) throw new IllegalStateException("Completed Sable sublevel is not loaded: " + entry.subLevelId);
        Level subLevelLevel = (Level) call(subLevel, "getLevel");
        Object sableHelper = Class.forName("dev.ryanhcode.sable.Sable").getField("HELPER").get(null);
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(entry.ownerId);
        Object plot = call(subLevel, "getPlot");
        AABB plotBounds = (AABB) call(call(plot, "getBoundingBox"), "toAABB");
        List<BlockPos> unassignedSeats = seatPositions(subLevelLevel, plotBounds);
        int total = 0;
        for (JsonElement raw : entities) {
            if (!raw.isJsonObject()) continue;
            JsonObject spec = raw.getAsJsonObject();
            List<BlockPos> blockPositions=spawnPositionsOnBlocks(subLevelLevel,plotBounds,spec);
            int requested=booleanValue(spec,"fill_all_seats",false)?unassignedSeats.size():integer(spec,"count",1);
            if(spec.has("spawn_on_blocks"))requested=Math.min(requested,blockPositions.size());
            int count = Math.min(128 - total, requested);
            if(count<=0)continue;
            for (int i = 0; i < count; i++) {
                Entity entity = createEntity(level, spec);
                if (entity == null) continue;
                applyDefaultEquipment(entity, spec);
                double lx = local(spec, 0, "local_x", (size.getX() - 1) / 2.0);
                double ly = local(spec, 1, "local_y", integer(action, "entity_offset_y", 1));
                double lz = local(spec, 2, "local_z", (size.getZ() - 1) / 2.0);
                boolean seatRequested = booleanValue(spec, "seat", false);
                BlockPos assignedSeat = seatRequested && !unassignedSeats.isEmpty() ? unassignedSeats.remove(0) : null;
                BlockPos blockPosition=!blockPositions.isEmpty()?blockPositions.remove(level.random.nextInt(blockPositions.size())):null;
                Vec3 plotPosition = assignedSeat == null&&blockPosition==null
                    ? new Vec3(entry.anchor.getX() + lx + .5, entry.anchor.getY() + ly, entry.anchor.getZ() + lz + .5)
                    : assignedSeat!=null?new Vec3(assignedSeat.getX() + .5, assignedSeat.getY(), assignedSeat.getZ() + .5)
                    : new Vec3(blockPosition.getX()+.5,blockPosition.getY(),blockPosition.getZ()+.5);
                Vec3 worldPosition = (Vec3) call(sableHelper, "projectOutOfSubLevel", subLevelLevel, plotPosition);
                entity.moveTo(worldPosition.x, worldPosition.y, worldPosition.z, (float)Math.toDegrees(yaw), 0);
                entity.addTag("ambush_owned");
                entity.addTag("ambush_sable_entity");
                entity.getPersistentData().putUUID("discovery_sublevel_id", entry.subLevelId);
                if (seatRequested) {
                    entity.addTag("ambush_sable_seat_requested");
                    entity.addTag("ambush_airship_aggro");
                }
                entity.addTag(actionEntityTag(entry.actionId));
                entity.addTag("ambush_owner_" + entry.ownerId.toString().replace("-", ""));
                boolean friendlyFire = spec.has("friendly_fire") ? spec.get("friendly_fire").getAsBoolean()
                    : spec.has("allow_friendly_fire") ? spec.get("allow_friendly_fire").getAsBoolean() : false;
                if (!friendlyFire) entity.addTag("ambush_no_friendly_fire");
                if (spec.has("tags")) for (JsonElement tag : spec.getAsJsonArray("tags")) entity.addTag(tag.getAsString());
                if (entity instanceof Mob mob) {
                    if (booleanValue(spec, "persistent", true)) mob.setPersistenceRequired();
                    if (spec.has("aggro_range")) {
                        int range = Math.max(4, Math.min(512, integer(spec, "aggro_range", 32)));
                        entity.addTag("ambush_aggro_range_" + range);
                        var followRange = mob.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE);
                        if (followRange != null) followRange.setBaseValue(range);
                    }
                }
                if (level.addFreshEntity(entity)) {
                    entry.entityIds.add(entity.getUUID());
                    if(entity instanceof LivingEntity&&booleanValue(spec,"crew",true))entry.crewEntityIds.add(entity.getUUID());
                    total++;
                }
            }
        }
    }

    private static List<BlockPos> spawnPositionsOnBlocks(Level level,AABB bounds,JsonObject spec){
        if(!spec.has("spawn_on_blocks"))return new ArrayList<>();JsonArray selectors=spec.get("spawn_on_blocks").isJsonArray()?spec.getAsJsonArray("spawn_on_blocks"):singletonArray(spec.get("spawn_on_blocks"));List<BlockPos> result=new ArrayList<>();
        for(BlockPos floor:positions(bounds)){var state=level.getBlockState(floor);boolean match=false;for(JsonElement raw:selectors){String selector=raw.getAsString();if(selector.startsWith("#")?state.is(net.minecraft.tags.TagKey.create(Registries.BLOCK,ResourceLocation.parse(selector.substring(1)))):BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString().equals(selector)){match=true;break;}}if(match&&level.isEmptyBlock(floor.above())&&level.isEmptyBlock(floor.above(2)))result.add(floor.above().immutable());}
        return result;
    }

    private static void applyDefaultEquipment(Entity entity, JsonObject spec) {
        if (!(entity instanceof net.minecraft.world.entity.monster.Vindicator vindicator)) return;
        boolean explicitlyConfigured = spec.has("mainhand") || spec.has("main_hand") ||
            (spec.has("equipment") && spec.get("equipment").isJsonObject() &&
                (spec.getAsJsonObject("equipment").has("mainhand") || spec.getAsJsonObject("equipment").has("main_hand")));
        if (!explicitlyConfigured && vindicator.getMainHandItem().isEmpty())
            vindicator.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,
                new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_AXE));
    }

    /**
     * Discovery deliberately waits a tick between spawning a sublevel-owned entity and seating it.
     * The discovery_sublevel_id hook first transfers the entity into Sable's plot; Create's seat
     * entity can then resolve the actual SeatBlock in that plot on every tick.
     */
    private static void populateRequestedSeats(ServerLevel level, SableAmbushState.Entry entry, SableAmbushState state) throws Exception {
        if (entry.seatAttempts >= 40) return;
        Object subLevel = findSubLevel(level, entry.subLevelId, null);
        if (subLevel == null) return;
        Level subLevelLevel = (Level) call(subLevel, "getLevel");
        Object plot = call(subLevel, "getPlot");
        AABB plotBounds = (AABB) call(call(plot, "getBoundingBox"), "toAABB");
        List<Entity> requested = subLevelLevel.getEntitiesOfClass(Entity.class, plotBounds.inflate(1), entity ->
            entity.getTags().contains(actionEntityTag(entry.actionId)) &&
            entity.getTags().contains("ambush_sable_seat_requested"));
        if (!requested.isEmpty() && requested.stream().allMatch(Entity::isPassenger)) {
            entry.seatAttempts = 40;
            state.changed();
            return;
        }
        List<Entity> riders = requested.stream().filter(entity -> !entity.isPassenger()).toList();
        if (riders.isEmpty()) {
            entry.seatAttempts++;
            state.changed();
            if (entry.seatAttempts >= 40)
                Ambush.LOGGER.warn("No Sable-attached seat riders were found: action={} sublevel={}", entry.actionId, entry.subLevelId);
            return;
        }

        Class<?> seatBlockClass = Class.forName("com.simibubi.create.content.contraptions.actors.seat.SeatBlock");
        List<BlockPos> seats = seatPositions(subLevelLevel, plotBounds);

        List<UUID> seatEntities = new ArrayList<>();
        for (Entity rider : riders) {
            BlockPos nearest = seats.stream()
                .filter(pos -> {
                    try { return !(boolean) callStatic(seatBlockClass, "isSeatOccupied", subLevelLevel, pos); }
                    catch (Exception ex) { throw new RuntimeException(ex); }
                })
                .min(Comparator.comparingDouble(pos -> rider.distanceToSqr(Vec3.atCenterOf(pos))))
                .orElse(null);
            if (nearest == null) break;
            callStatic(seatBlockClass, "sitDown", subLevelLevel, nearest, rider);
            if (rider.isPassenger() && rider.getVehicle() != null) {
                Entity seat = rider.getVehicle();
                seat.addTag("ambush_owned");
                seat.addTag("ambush_sable_seat");
                seat.addTag(actionEntityTag(entry.actionId));
                seat.addTag("ambush_owner_" + entry.ownerId.toString().replace("-", ""));
                seat.getPersistentData().putUUID("discovery_sublevel_id", entry.subLevelId);
                seatEntities.add(seat.getUUID());
                seats.remove(nearest);
                Ambush.LOGGER.info("Seated Sable ambush entity={} at {} in sublevel={}", rider.getUUID(), nearest.toShortString(), entry.subLevelId);
            }
        }
        entry.entityIds.addAll(seatEntities);
        entry.seatAttempts++;
        state.changed();
        if (entry.seatAttempts >= 40 && riders.stream().anyMatch(entity -> !entity.isPassenger()))
            Ambush.LOGGER.warn("Could not seat all requested Sable ambush entities: action={} sublevel={} seats={}", entry.actionId, entry.subLevelId, seats.size());
    }

    private static List<BlockPos> seatPositions(Level subLevelLevel, AABB plotBounds) throws Exception {
        Class<?> seatBlockClass = Class.forName("com.simibubi.create.content.contraptions.actors.seat.SeatBlock");
        List<BlockPos> seats = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(
            BlockPos.containing(plotBounds.minX, plotBounds.minY, plotBounds.minZ),
            BlockPos.containing(plotBounds.maxX, plotBounds.maxY, plotBounds.maxZ))) {
            if (seatBlockClass.isInstance(subLevelLevel.getBlockState(pos).getBlock())) seats.add(pos.immutable());
        }
        return seats;
    }

    private static void applyPostAssemblyData(ServerLevel level, SableAmbushState.Entry entry, JsonObject action, SableAmbushState state) throws Exception {
        if (entry.envelopeFillApplied && entry.containerLootApplied && entry.engineBurnApplied && entry.throttleSignalApplied) return;
        Object subLevel = findSubLevel(level, entry.subLevelId, null);
        if (subLevel == null) return;
        Level subLevelLevel = (Level) call(subLevel, "getLevel");
        AABB plotBounds = (AABB) call(call(call(subLevel, "getPlot"), "getBoundingBox"), "toAABB");

        if (!entry.envelopeFillApplied)
            entry.envelopeFillApplied = applyEnvelopeFill(subLevelLevel, plotBounds, entry, action);
        if (!entry.containerLootApplied)
            entry.containerLootApplied = applyContainerLoot(level, entry, action);
        if (!entry.engineBurnApplied)
            entry.engineBurnApplied = applyEngineBurn(subLevelLevel, plotBounds, entry, action);
        if (!entry.throttleSignalApplied && (!booleanValue(action,"throttle_requires_living_crew",true)||hasLivingCrew(level,entry)))
            entry.throttleSignalApplied = applyThrottleSignal(subLevelLevel, plotBounds, entry, action);
        entry.postProcessAttempts++;
        state.changed();
        if (entry.postProcessAttempts == 100) {
            if (!entry.envelopeFillApplied)
                Ambush.LOGGER.warn("Envelope fill could not be applied: action={} sublevel={}", entry.actionId, entry.subLevelId);
            if (!entry.containerLootApplied)
                Ambush.LOGGER.warn("Container loot could not be applied: action={} sublevel={}", entry.actionId, entry.subLevelId);
            if (!entry.engineBurnApplied)
                Ambush.LOGGER.warn("Portable-engine burn time could not be applied: action={} sublevel={}", entry.actionId, entry.subLevelId);
            if (!entry.throttleSignalApplied)
                Ambush.LOGGER.warn("Throttle signal could not be applied: action={} sublevel={}", entry.actionId, entry.subLevelId);
        }
    }

    private static void applyRedstoneActivations(ServerLevel level, SableAmbushState.Entry entry,
                                                  JsonObject action, SableAmbushState state) throws Exception {
        if (!action.has("redstone_activations") || !action.get("redstone_activations").isJsonArray()) return;
        Object subLevel = findSubLevel(level, entry.subLevelId, null);
        if (subLevel == null) return;
        Level subLevelLevel = (Level) call(subLevel, "getLevel");
        AABB plotBounds = (AABB) call(call(call(subLevel, "getPlot"), "getBoundingBox"), "toAABB");
        JsonArray activations = action.getAsJsonArray("redstone_activations");
        for (int index = 0; index < activations.size(); index++) {
            if (!activations.get(index).isJsonObject()) continue;
            JsonObject original = activations.get(index).getAsJsonObject();
            long repeat=Math.max(0,integer(original,"repeat_ticks",0));String repeatKey="redstone_"+index;
            if(repeat<=0&&entry.redstoneActivations.contains(index))continue;
            if(repeat>0&&level.getGameTime()-entry.eventLastFired.getOrDefault(repeatKey,Long.MIN_VALUE/2)<repeat)continue;
            JsonObject activation=selectPlayerYBand(level,entry,original);
            if(activation==null){if("complete".equalsIgnoreCase(string(original,"on_no_match","wait"))){entry.redstoneActivations.add(index);state.changed();}continue;}
            if (!redstoneTriggerReady(level, entry, subLevelLevel, plotBounds, activation)) continue;
            int changed = activateRedstone(subLevelLevel, plotBounds, activation);
            if (changed <= 0) {
                Ambush.LOGGER.warn("Sable redstone activation matched no components: action={} sublevel={} activation={}",
                    entry.actionId, entry.subLevelId, index);
                if(repeat>0)entry.eventLastFired.put(repeatKey,level.getGameTime());else entry.redstoneActivations.add(index);
                state.changed();
                continue;
            }
            if(repeat>0)entry.eventLastFired.put(repeatKey,level.getGameTime());else entry.redstoneActivations.add(index);
            state.changed();
            Ambush.LOGGER.info("Activated {} Sable redstone component(s): action={} sublevel={} activation={}",
                changed, entry.actionId, entry.subLevelId, index);
        }
    }

    private static JsonObject selectPlayerYBand(ServerLevel level,SableAmbushState.Entry entry,JsonObject activation){
        if(!activation.has("player_y_bands")||!activation.get("player_y_bands").isJsonArray())return activation;
        ServerPlayer owner=level.getServer().getPlayerList().getPlayer(entry.ownerId);if(owner==null)return null;double y=owner.getY();
        for(JsonElement raw:activation.getAsJsonArray("player_y_bands"))if(raw.isJsonObject()){
            JsonObject band=raw.getAsJsonObject();if(y<number(band,"min_y",-2048)||y>number(band,"max_y",2048))continue;
            JsonObject selected=activation.deepCopy();selected.remove("player_y_bands");for(Map.Entry<String,JsonElement> value:band.entrySet())selected.add(value.getKey(),value.getValue().deepCopy());return selected;
        }
        return null;
    }

    private static boolean redstoneTriggerReady(ServerLevel level, SableAmbushState.Entry entry, Level subLevelLevel,
                                                 AABB plotBounds, JsonObject activation) throws Exception {
        if (booleanValue(activation,"require_living_crew",true) && !hasLivingCrew(level,entry)) return false;
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(entry.ownerId);
        if (activation.has("min_player_y") || activation.has("max_player_y")) {
            if (owner == null) return false;
            double y=owner.getY();
            if (y<number(activation,"min_player_y",-2048)||y>number(activation,"max_player_y",2048)) return false;
        }
        if(activation.has("player_direction")){
            if(owner==null)return false;Object sub=findSubLevel(level,entry.subLevelId,null);Vec3 center=structureWorldCenter(level,entry);if(sub==null||center==null)return false;
            Vec3 world=owner.position().subtract(center),local=(Vec3)call(call(sub,"logicalPose"),"transformNormalInverse",world);
            if(!matchesPlayerDirection(local,string(activation,"player_direction","front"),number(activation,"direction_tolerance_degrees",30)))return false;
        }
        boolean hasTime = activation.has("after_ticks") || activation.has("after_seconds");
        long delay = activation.has("after_ticks") ? Math.max(0, activation.get("after_ticks").getAsLong())
            : Math.max(0, Math.round(number(activation, "after_seconds", 0) * 20));
        boolean timeReady = hasTime && level.getGameTime() - entry.redstoneTriggerStartedAt >= delay;
        boolean hasRange = activation.has("range") || activation.has("distance");
        double range = Math.max(0, number(activation, "range", number(activation, "distance", 0)));
        boolean rangeReady = false;
        if (hasRange) {
            if (owner != null && owner.serverLevel() == level) {
                Vec3 plotCenter = plotBounds.getCenter();
                Object helper = Class.forName("dev.ryanhcode.sable.Sable").getField("HELPER").get(null);
                Vec3 worldCenter = (Vec3) call(helper, "projectOutOfSubLevel", subLevelLevel, plotCenter);
                double dx = owner.getX() - worldCenter.x, dy = owner.getY() - worldCenter.y, dz = owner.getZ() - worldCenter.z;
                double distanceSquared = booleanValue(activation, "horizontal_only", false)
                    ? dx * dx + dz * dz : dx * dx + dy * dy + dz * dz;
                rangeReady = distanceSquared <= range * range;
            }
        }
        if (!hasTime && !hasRange) return true;
        String requirement = string(activation, "require", string(activation, "trigger_mode", "any"));
        return "all".equalsIgnoreCase(requirement)
            ? (!hasTime || timeReady) && (!hasRange || rangeReady)
            : timeReady || rangeReady;
    }

    private static boolean matchesPlayerDirection(Vec3 local,String requested,double tolerance){
        double horizontal=Math.sqrt(local.x*local.x+local.z*local.z),verticalAngle=Math.toDegrees(Math.atan2(local.y,Math.max(.001,horizontal)));
        String direction=requested.toLowerCase(Locale.ROOT);if(direction.equals("below"))return verticalAngle<=-Math.max(1,tolerance);if(direction.equals("above"))return verticalAngle>=Math.max(1,tolerance);
        double angle=Math.toDegrees(Math.atan2(local.x,-local.z)),target=switch(direction){case "front_right","right_front"->45;case "right"->90;case "back_right","right_back"->135;case "behind","back"->180;case "back_left","left_back"->-135;case "left"->-90;case "front_left","left_front"->-45;default->0;};
        double difference=Math.abs(net.minecraft.util.Mth.wrapDegrees((float)(angle-target)));return difference<=Math.max(1,Math.min(89,tolerance));
    }

    private static void applySteeringControls(ServerLevel level,SableAmbushState.Entry entry,JsonObject action)throws Exception{
        if(!action.has("steering_controls")||!action.get("steering_controls").isJsonArray())return;
        ServerPlayer owner=level.getServer().getPlayerList().getPlayer(entry.ownerId);if(owner==null||owner.serverLevel()!=level)return;
        Object sub=findSubLevel(level,entry.subLevelId,null);Vec3 center=structureWorldCenter(level,entry);if(sub==null||center==null)return;
        Level plotLevel=(Level)call(sub,"getLevel");AABB bounds=(AABB)call(call(call(sub,"getPlot"),"getBoundingBox"),"toAABB");
        Vec3 world=owner.position().subtract(center),local=(Vec3)call(call(sub,"logicalPose"),"transformNormalInverse",world);
        JsonArray controls=action.getAsJsonArray("steering_controls");
        for(int index=0;index<controls.size();index++){
            if(!controls.get(index).isJsonObject())continue;JsonObject control=controls.get(index).getAsJsonObject();
            if(booleanValue(control,"require_living_crew",true)&&!hasLivingCrew(level,entry))continue;
            double range=Math.max(0,number(control,"range",number(control,"distance",256)));boolean horizontal=booleanValue(control,"horizontal_only",true);
            if(range>0&&(horizontal?world.x*world.x+world.z*world.z:world.lengthSqr())>range*range)continue;
            int updateTicks=Math.max(1,Math.min(1200,integer(control,"update_ticks",10)));String key=entry.actionId+":"+index;long now=level.getGameTime();
            if(now-STEERING_LAST_UPDATE.getOrDefault(key,Long.MIN_VALUE/2)<updateTicks)continue;
            String direction=playerDirection(local,number(control,"vertical_sector_degrees",50));applyLivePropulsionDirection(plotLevel,bounds,entry,action,direction);Float previous=STEERING_LAST_ANGLE.get(key);boolean engineReverse="reverse".equalsIgnoreCase(directionValue(action,"engine_direction_by_player_direction",direction,string(action,"engine_direction",booleanValue(action,"reverse_engines",false)?"reverse":"forward")));boolean propellerReverse="reverse".equalsIgnoreCase(directionValue(action,"propeller_direction_by_player_direction",direction,string(action,"propeller_direction",booleanValue(action,"reverse_propellers",false)?"reverse":"forward")));boolean propulsionInvert=booleanValue(control,"invert_with_propulsion_direction",booleanValue(action,"steering_follows_propulsion_direction",true))&&(engineReverse^propellerReverse);Float angle=steeringAngle(control,direction,local,previous,propulsionInvert);if(angle==null)continue;
            STEERING_LAST_UPDATE.put(key,now);if(previous!=null&&Math.abs(previous-angle)<.01f)continue;
            int changed=0;for(BlockPos pos:redstonePositions(bounds,control)){
                var state=plotLevel.getBlockState(pos);if(!matchesConfiguredBlock(state,control))continue;BlockEntity blockEntity=plotLevel.getBlockEntity(pos);if(!looksSteeringWheel(state,blockEntity))continue;
                try{blockEntity.getClass().getField("targetAngleToUpdate").setFloat(blockEntity,angle);call(blockEntity,"updateTargetAngle",angle);blockEntity.setChanged();plotLevel.sendBlockUpdated(pos,state,state,3);changed++;}catch(Exception ignored){}
            }
            if(changed>0)STEERING_LAST_ANGLE.put(key,angle);
        }
    }

    private static String directionValue(JsonObject action,String field,String direction,String fallback){
        if(!action.has(field)||!action.get(field).isJsonObject())return fallback;
        JsonObject values=action.getAsJsonObject(field);String value=values.has(direction)?values.get(direction).getAsString():values.has("default")?values.get("default").getAsString():fallback;
        return "reverse".equalsIgnoreCase(value)?"reverse":"forward";
    }

    private static void applyLivePropulsionDirection(Level plotLevel,AABB bounds,SableAmbushState.Entry entry,JsonObject action,String playerDirection)throws Exception{
        boolean engineTracked=action.has("engine_direction_by_player_direction"),propellerTracked=action.has("propeller_direction_by_player_direction");if(!engineTracked&&!propellerTracked)return;
        String engine=directionValue(action,"engine_direction_by_player_direction",playerDirection,string(action,"engine_direction","forward"));String propeller=directionValue(action,"propeller_direction_by_player_direction",playerDirection,string(action,"propeller_direction","forward"));String state=engine+":"+propeller,key=entry.actionId+":propulsion";if(state.equals(PROPULSION_LAST_DIRECTION.get(key)))return;
        Class<?> engineClass=null,propellerClass=null;if(engineTracked)try{engineClass=Class.forName("dev.simulated_team.simulated.content.blocks.portable_engine.PortableEngineBlockEntity");}catch(ClassNotFoundException ignored){}if(propellerTracked)try{propellerClass=Class.forName("dev.eriksonn.aeronautics.content.blocks.propeller.bearing.propeller_bearing.PropellerBearingBlockEntity");}catch(ClassNotFoundException ignored){}
        int changed=0;for(BlockPos pos:positions(bounds)){BlockEntity blockEntity=plotLevel.getBlockEntity(pos);if(engineClass!=null&&engineClass.isInstance(blockEntity)){Object behavior=fieldValue(blockEntity,"movementDirection");call(behavior,"setValue","reverse".equals(engine)?1:0);blockEntity.setChanged();changed++;}if(propellerClass!=null&&propellerClass.isInstance(blockEntity)){Object option=call(blockEntity,"getThrustDirectionOption");call(option,"setValue","reverse".equals(propeller)?1:0);call(blockEntity,"onDirectionChanged");blockEntity.setChanged();changed++;}}
        PROPULSION_LAST_DIRECTION.put(key,state);Ambush.LOGGER.debug("Updated live propulsion direction action={} playerDirection={} engine={} propeller={} components={}",entry.actionId,playerDirection,engine,propeller,changed);
    }

    private static boolean looksSteeringWheel(net.minecraft.world.level.block.state.BlockState state,BlockEntity blockEntity){
        String path=state.getBlock().builtInRegistryHolder().key().location().getPath(),className=blockEntity==null?"":blockEntity.getClass().getName().toLowerCase(Locale.ROOT);
        return path.contains("steering_wheel")||className.contains("steering_wheel");
    }

    private static String playerDirection(Vec3 local,double verticalSector){
        double horizontal=Math.sqrt(local.x*local.x+local.z*local.z),vertical=Math.toDegrees(Math.atan2(local.y,Math.max(.001,horizontal)));
        if(vertical>=Math.max(1,verticalSector))return "above";if(vertical<=-Math.max(1,verticalSector))return "below";
        double angle=net.minecraft.util.Mth.wrapDegrees((float)Math.toDegrees(Math.atan2(local.x,-local.z)));int sector=Math.floorMod((int)Math.round(angle/45.0),8);
        return switch(sector){case 1->"front_right";case 2->"right";case 3->"back_right";case 4->"behind";case 5->"back_left";case 6->"left";case 7->"front_left";default->"front";};
    }

    private static Float steeringAngle(JsonObject control,String direction,Vec3 local,Float previous,boolean propulsionInvert){
        double limit=Math.max(1,Math.min(45,number(control,"max_angle",45))),angle;
        if("continuous".equalsIgnoreCase(string(control,"mode","sectors"))){
            angle=net.minecraft.util.Mth.wrapDegrees((float)Math.toDegrees(Math.atan2(local.x,-local.z)));
            if(local.z>0&&Math.abs(local.x)<=Math.max(.01,number(control,"behind_lateral_deadzone",1))){String behind=string(control,"behind_direction","last");angle="left".equalsIgnoreCase(behind)?-limit:"right".equalsIgnoreCase(behind)?limit:previous!=null&&previous<0?-limit:limit;}
        }else{JsonObject configured=control.has("direction_angles")&&control.get("direction_angles").isJsonObject()?control.getAsJsonObject("direction_angles"):null;if(configured!=null&&!configured.has(direction))return null;angle=configured==null?switch(direction){case "front_right"->22.5;case "right","back_right","behind"->45;case "back_left","left"->-45;case "front_left"->-22.5;default->0;}:configured.get(direction).getAsDouble();}
        angle=-angle;if(booleanValue(control,"invert",false))angle=-angle;if(propulsionInvert)angle=-angle;angle=angle*number(control,"angle_scale",1)+number(control,"angle_offset",0);return (float)Math.max(-limit,Math.min(limit,angle));
    }

    private static int activateRedstone(Level subLevelLevel, AABB plotBounds, JsonObject activation) throws Exception {
        String component = string(activation, "component", "any").toLowerCase(Locale.ROOT);
        int signal = Math.max(0, Math.min(15, integer(activation, "signal", 15)));
        String desired = string(activation, "state", component.equals("button") ? "press" : "on").toLowerCase(Locale.ROOT);
        int buttonTicks = Math.max(1, Math.min(1200, integer(activation, "button_ticks", 20)));
        int changed = 0;
        for (BlockPos pos : redstonePositions(plotBounds, activation)) {
            var blockState = subLevelLevel.getBlockState(pos);
            var block = blockState.getBlock();
            if (!matchesConfiguredBlock(blockState, activation)) continue;
            boolean analog = component.equals("analog_lever") || component.equals("analog") || component.equals("throttle_lever");
            boolean lever = block instanceof net.minecraft.world.level.block.LeverBlock;
            boolean button = block instanceof net.minecraft.world.level.block.ButtonBlock;
            if (analog) {
                if (!looksAnalog(subLevelLevel, pos, blockState) || !setAnalogSignal(subLevelLevel, pos, blockState, signal)) continue;
            } else if (component.equals("lever")) {
                if (!lever || !setPowered(subLevelLevel, pos, blockState, desired)) continue;
            } else if (component.equals("button")) {
                if (!button || !setPowered(subLevelLevel, pos, blockState, "on")) continue;
                subLevelLevel.scheduleTick(pos, block, buttonTicks);
            } else if (lever || button) {
                if (!setPowered(subLevelLevel, pos, blockState, desired)) continue;
                if (button) subLevelLevel.scheduleTick(pos, block, buttonTicks);
            } else if (!setAnalogSignal(subLevelLevel, pos, blockState, signal)) continue;
            notifyRedstoneNeighbors(subLevelLevel, pos, block);
            changed++;
        }
        return changed;
    }

    private static Iterable<BlockPos> redstonePositions(AABB bounds, JsonObject activation) {
        if (!activation.has("positions") || !activation.get("positions").isJsonArray()) return positions(bounds);
        BlockPos min = BlockPos.containing(bounds.minX, bounds.minY, bounds.minZ);
        List<BlockPos> result = new ArrayList<>();
        boolean absolute = booleanValue(activation, "absolute_positions", false);
        for (JsonElement raw : activation.getAsJsonArray("positions")) {
            int x, y, z;
            if (raw.isJsonArray() && raw.getAsJsonArray().size() >= 3) {
                x = raw.getAsJsonArray().get(0).getAsInt(); y = raw.getAsJsonArray().get(1).getAsInt(); z = raw.getAsJsonArray().get(2).getAsInt();
            } else if (raw.isJsonObject()) {
                JsonObject object = raw.getAsJsonObject(); x = integer(object, "x", 0); y = integer(object, "y", 0); z = integer(object, "z", 0);
            } else continue;
            result.add(absolute ? new BlockPos(x, y, z) : min.offset(x, y, z));
        }
        return result;
    }

    private static boolean matchesConfiguredBlock(net.minecraft.world.level.block.state.BlockState state, JsonObject activation) {
        JsonArray selectors = new JsonArray();
        if (activation.has("block")) selectors.add(activation.get("block"));
        if (activation.has("blocks") && activation.get("blocks").isJsonArray())
            for (JsonElement value : activation.getAsJsonArray("blocks")) selectors.add(value);
        if (selectors.isEmpty()) return true;
        for (JsonElement raw : selectors) {
            String selector = raw.getAsString();
            try {
                if (selector.startsWith("#") && state.is(net.minecraft.tags.TagKey.create(Registries.BLOCK, ResourceLocation.parse(selector.substring(1))))) return true;
                if (!selector.startsWith("#") && state.is(BuiltInRegistries.BLOCK.get(ResourceLocation.parse(selector)))) return true;
            } catch (RuntimeException ignored) { }
        }
        return false;
    }

    private static boolean looksAnalog(Level level, BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        String path = state.getBlock().builtInRegistryHolder().key().location().getPath();
        BlockEntity blockEntity = level.getBlockEntity(pos);
        String className = blockEntity == null ? "" : blockEntity.getClass().getName().toLowerCase(Locale.ROOT);
        return path.contains("analog_lever") || path.contains("throttle_lever") || className.contains("analog") || className.contains("throttle_lever");
    }

    private static boolean setPowered(Level level, BlockPos pos, net.minecraft.world.level.block.state.BlockState state, String desired) {
        if (!state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.POWERED)) return false;
        boolean current = state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.POWERED);
        boolean powered = "toggle".equals(desired) ? !current : !"off".equals(desired) && !"false".equals(desired);
        level.setBlock(pos, state.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.POWERED, powered), 3);
        return true;
    }

    private static boolean setAnalogSignal(Level level, BlockPos pos, net.minecraft.world.level.block.state.BlockState state, int signal) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity != null) for (String method : List.of("setSignal", "setPower", "setOutputSignal", "setLevel")) {
            try {
                call(blockEntity, method, signal);
                blockEntity.setChanged();
                level.sendBlockUpdated(pos, state, state, 3);
                return true;
            } catch (Exception ignored) { }
        }
        for (var property : state.getProperties()) if (property instanceof net.minecraft.world.level.block.state.properties.IntegerProperty integerProperty
            && Set.of("power", "signal", "level", "strength").contains(property.getName()) && integerProperty.getPossibleValues().contains(signal)) {
            level.setBlock(pos, state.setValue(integerProperty, signal), 3);
            return true;
        }
        return false;
    }

    private static void notifyRedstoneNeighbors(Level level, BlockPos pos, net.minecraft.world.level.block.Block block) {
        level.updateNeighborsAt(pos, block);
        for (net.minecraft.core.Direction direction : net.minecraft.core.Direction.values()) level.updateNeighborsAt(pos.relative(direction), block);
    }

    private static boolean applyEngineBurn(Level subLevelLevel, AABB plotBounds, SableAmbushState.Entry entry,
                                           JsonObject action) throws Exception {
        boolean hasBurnTime = action.has("engine_burn_ticks") || action.has("engine_burn_seconds");
        boolean hasSuperheated = action.has("engine_superheated");
        boolean hasDirection=action.has("engine_direction")||action.has("reverse_engines");
        boolean hasPropellerDirection=action.has("propeller_direction")||action.has("reverse_propellers");
        if (!hasBurnTime && !hasSuperheated && !hasDirection&&!hasPropellerDirection) return true;
        if (entry.postProcessAttempts >= 100) return false;
        int ticks = action.has("engine_burn_ticks") ? integer(action, "engine_burn_ticks", 0)
            : (int)Math.round(number(action, "engine_burn_seconds", 0) * 20);
        ticks = Math.max(0, Math.min(1_728_000, ticks));
        boolean superheated = hasSuperheated && action.get("engine_superheated").getAsBoolean();
        boolean reverse=action.has("engine_direction")?"reverse".equalsIgnoreCase(string(action,"engine_direction","forward")):booleanValue(action,"reverse_engines",false);
        boolean reversePropellers=action.has("propeller_direction")?"reverse".equalsIgnoreCase(string(action,"propeller_direction","forward")):booleanValue(action,"reverse_propellers",false);
        Class<?> engineClass;try{engineClass=Class.forName("dev.simulated_team.simulated.content.blocks.portable_engine.PortableEngineBlockEntity");}catch(ClassNotFoundException missingOptionalRuntime){return true;}
        Class<?> propellerBearingClass=null;if(hasPropellerDirection)try{propellerBearingClass=Class.forName("dev.eriksonn.aeronautics.content.blocks.propeller.bearing.propeller_bearing.PropellerBearingBlockEntity");}catch(ClassNotFoundException ignored){}
        int applied = 0,propellersApplied=0;
        for (BlockPos pos : positions(plotBounds)) {
            BlockEntity blockEntity = subLevelLevel.getBlockEntity(pos);
            if(engineClass.isInstance(blockEntity)){
                if (hasBurnTime) call(blockEntity, "setCurrentBurnTime", ticks);
                if (hasSuperheated) call(blockEntity, "setSuperHeated", superheated);
                if(hasDirection){Object directionBehaviour=fieldValue(blockEntity,"movementDirection");call(directionBehaviour,"setValue",reverse?1:0);}
                blockEntity.setChanged();applied++;
            }
            if(propellerBearingClass!=null&&propellerBearingClass.isInstance(blockEntity)){Object option=call(blockEntity,"getThrustDirectionOption");call(option,"setValue",reversePropellers?1:0);call(blockEntity,"onDirectionChanged");blockEntity.setChanged();propellersApplied++;}
        }
        if (applied > 0)
            Ambush.LOGGER.info("Applied portable-engine burn time={} ticks, superheated={}, engineDirection={} to {} engine(s), propellerDirection={} to {} bearing(s): action={} sublevel={}",
                ticks, superheated, reverse?"reverse":"forward", applied,reversePropellers?"reverse":"forward",propellersApplied, entry.actionId, entry.subLevelId);
        return applied > 0||hasPropellerDirection;
    }

    private static boolean applyThrottleSignal(Level subLevelLevel, AABB plotBounds, SableAmbushState.Entry entry,
                                               JsonObject action) throws Exception {
        if (!action.has("throttle_signal") && !action.has("throttle_signal_by_y") && !cannonballoonFlightProfile(action)) return true;
        if (entry.postProcessAttempts >= 100) return false;
        int signal = Math.max(1, Math.min(15, integer(action, "resolved_throttle_signal",
            integer(action, "throttle_signal", 1))));
        Class<?> throttleClass = Class.forName("dev.simulated_team.simulated.content.blocks.throttle_lever.ThrottleLeverBlockEntity");
        int applied = 0;
        for (BlockPos pos : positions(plotBounds)) {
            BlockEntity blockEntity = subLevelLevel.getBlockEntity(pos);
            if (!throttleClass.isInstance(blockEntity)) continue;
            call(blockEntity, "setSignal", signal);
            blockEntity.setChanged();
            applied++;
        }
        if (applied > 0)
            Ambush.LOGGER.info("Applied throttle signal={} to {} lever(s): action={} sublevel={}",
                signal, applied, entry.actionId, entry.subLevelId);
        return applied > 0;
    }

    private static boolean applyEnvelopeFill(Level subLevelLevel, AABB plotBounds, SableAmbushState.Entry entry, JsonObject action) throws Exception {
        if (!action.has("envelope_fill") && !cannonballoonFlightProfile(action)) return true;
        if (entry.postProcessAttempts >= 100) return false;
        double fill = cannonballoonFlightProfile(action) ? 1.0D
            : Math.max(0, Math.min(1, action.get("envelope_fill").getAsDouble()));
        Class<?> providerClass = Class.forName("dev.eriksonn.aeronautics.content.blocks.hot_air.BlockEntityLiftingGasProvider");
        Set<Object> balloons = Collections.newSetFromMap(new IdentityHashMap<>());
        for (BlockPos pos : positions(plotBounds)) {
            BlockEntity blockEntity = subLevelLevel.getBlockEntity(pos);
            if (blockEntity == null || !providerClass.isInstance(blockEntity)) continue;
            Object balloon = call(blockEntity, "getBalloon");
            if (balloon != null) balloons.add(balloon);
        }
        if (balloons.isEmpty()) return false;

        int applied = 0;
        for (Object balloon : balloons) {
            int capacity = (int) call(balloon, "getCapacity");
            List<?> holders = (List<?>) call(balloon, "getLiftingGasHolders");
            if (capacity <= 0 || holders.isEmpty()) continue;
            boolean first = true;
            for (Object holder : holders) {
                Object data = call(holder, "data");
                double amount = first ? capacity * fill : 0;
                data.getClass().getField("amount").setDouble(data, amount);
                data.getClass().getField("target").setDouble(data, amount);
                data.getClass().getField("nudge").setDouble(data, 0);
                first = false;
            }
            call(balloon, "updateGasAmounts");
            applied++;
            Ambush.LOGGER.info("Applied envelope fill={} capacity={} action={} sublevel={}", fill, capacity, entry.actionId, entry.subLevelId);
        }
        return applied == balloons.size();
    }

    private static boolean applyContainerLoot(ServerLevel level, SableAmbushState.Entry entry, JsonObject action) throws Exception {
        if (!action.has("container_loot")) return true;
        if (entry.postProcessAttempts >= 100) return false;
        JsonArray rules = action.get("container_loot").isJsonArray()
            ? action.getAsJsonArray("container_loot") : singletonArray(action.get("container_loot"));
        boolean allMatched = true;
        int total = 0;
        for (JsonElement raw : rules) {
            if (!raw.isJsonObject()) continue;
            JsonObject rule = raw.getAsJsonObject();
            ResourceLocation lootId = ResourceLocation.parse(string(rule, "loot_table", ""));
            ResourceKey<net.minecraft.world.level.storage.loot.LootTable> lootKey = ResourceKey.create(Registries.LOOT_TABLE, lootId);
            Set<String> blocks = stringSet(rule.get("blocks"));
            boolean replace = booleanValue(rule, "replace_existing", false);
            long seed = rule.has("seed") ? rule.get("seed").getAsLong() : 0L;
            int matched = 0;
            for (Object subLevel : parentAndChildren(level, entry.subLevelId)) {
                Level subLevelLevel = (Level) call(subLevel, "getLevel");
                AABB plotBounds = (AABB) call(call(call(subLevel, "getPlot"), "getBoundingBox"), "toAABB");
                for (BlockPos pos : positions(plotBounds)) {
                    BlockEntity blockEntity = subLevelLevel.getBlockEntity(pos);
                    if (!(blockEntity instanceof RandomizableContainerBlockEntity container)) continue;
                    String blockId = BuiltInRegistries.BLOCK.getKey(subLevelLevel.getBlockState(pos).getBlock()).toString();
                    if (!blocks.isEmpty() && !blocks.contains(blockId)) continue;
                    if (!replace && (container.getLootTable() != null || !container.isEmpty())) continue;
                    container.setLootTable(lootKey);
                    container.setLootTableSeed(seed);
                    container.setChanged();
                    matched++;
                }
            }
            allMatched &= matched > 0;
            total += matched;
        }
        if (allMatched)
            Ambush.LOGGER.info("Applied container loot to {} container(s): action={} sublevel={}", total, entry.actionId, entry.subLevelId);
        return allMatched;
    }

    private static List<Object> parentAndChildren(ServerLevel level, UUID parentId) throws Exception {
        Object parent = findSubLevel(level, parentId, null);
        if (parent == null) return List.of();
        // Ambush is intentionally independent of Aeronautics Discovery. Sable does not
        // expose a child-sublevel relationship, so process the owned parent only here.
        return List.of(parent);
    }

    private record BlockHealth(int initial,int intact,double fraction){}

    private static BlockHealth sharedBossHealth(SableAmbushState.Entry entry,JsonObject action,SableAmbushState state,BlockHealth own){
        if(!action.has("shared_boss_id"))return own;String group=action.get("shared_boss_id").getAsString();int initial=0,intact=0;
        for(SableAmbushState.Entry member:state.entries())if(member.ownerId.equals(entry.ownerId))try{JsonObject other=JsonParser.parseString(member.actionJson).getAsJsonObject();if(group.equals(string(other,"shared_boss_id",""))){initial+=member.initialBlockCount;intact+=member.intactBlockCount;}}catch(RuntimeException ignored){}
        return initial<=0?own:new BlockHealth(initial,Math.max(0,intact),Math.max(0,Math.min(1,intact/(double)initial)));
    }

    private static void requestSharedBossCleanup(ServerLevel level,SableAmbushState.Entry entry,JsonObject action,SableAmbushState state)throws Exception{
        if(!action.has("shared_boss_id")){requestCleanup(level,entry,state);return;}String group=action.get("shared_boss_id").getAsString();
        for(SableAmbushState.Entry member:new ArrayList<>(state.entries()))if(member.ownerId.equals(entry.ownerId)&&!member.cleanupRequested)try{JsonObject other=JsonParser.parseString(member.actionJson).getAsJsonObject();if(group.equals(string(other,"shared_boss_id","")))requestCleanup(level,member,state);}catch(RuntimeException ignored){}
    }

    private static BlockHealth updateBlockHealth(ServerLevel level,SableAmbushState.Entry entry,JsonObject action,
                                                 SableAmbushState state)throws Exception{
        if(!tracksBlockHealth(action))return null;
        long now = level.getGameTime();
        if (entry.damageBaseline.isEmpty()) {
            int total = 0;
            for (Object subLevel : parentAndChildren(level, entry.subLevelId)) {
                UUID id = (UUID) call(subLevel, "getUniqueId");
                Level plotLevel = (Level) call(subLevel, "getLevel");
                AABB bounds = (AABB) call(call(call(subLevel, "getPlot"), "getBoundingBox"), "toAABB");
                List<Long> occupied = new ArrayList<>();
                for (BlockPos pos : positions(bounds)) {
                    if (!plotLevel.isEmptyBlock(pos)) occupied.add(pos.asLong());
                    if (++total > 262_144) throw new IllegalArgumentException("Sable damage baseline exceeds safe scan limit");
                }
                long[] packed = new long[occupied.size()];
                for (int i = 0; i < packed.length; i++) packed[i] = occupied.get(i);
                entry.damageBaseline.put(id, packed);
            }
            entry.nextDamageCheck = now + 20;
            entry.initialBlockCount=entry.damageBaseline.values().stream().mapToInt(values->values.length).sum();
            entry.intactBlockCount=entry.initialBlockCount;
            state.changed();
            Ambush.LOGGER.info("Captured Sable block-health baseline: action={} sublevels={} occupied_blocks={}",
                entry.actionId, entry.damageBaseline.size(), entry.initialBlockCount);
            return entry.initialBlockCount<=0?null:new BlockHealth(entry.initialBlockCount,entry.intactBlockCount,1);
        }
        if(now<entry.nextDamageCheck&&entry.initialBlockCount>0)
            return new BlockHealth(entry.initialBlockCount,entry.intactBlockCount,entry.intactBlockCount/(double)entry.initialBlockCount);
        int budget=claimHealthScanBudget(now,Math.max(64,Math.min(16384,integer(action,"health_scan_budget",4096))));
        if(budget<=0)return entry.initialBlockCount<=0?null:new BlockHealth(entry.initialBlockCount,entry.intactBlockCount,entry.intactBlockCount/(double)entry.initialBlockCount);
        List<Map.Entry<UUID,long[]>> baselines=new ArrayList<>(entry.damageBaseline.entrySet());int checked=0;
        while(checked<budget&&!baselines.isEmpty()){
            if(entry.healthSublevelCursor>=baselines.size()){entry.healthSublevelCursor=0;entry.healthPositionCursor=0;entry.nextDamageCheck=now+20;break;}
            Map.Entry<UUID,long[]> baseline=baselines.get(entry.healthSublevelCursor);long[] positions=baseline.getValue();
            if(entry.healthPositionCursor>=positions.length){entry.healthSublevelCursor++;entry.healthPositionCursor=0;continue;}
            long packed=positions[entry.healthPositionCursor++];Set<Long> destroyed=entry.destroyedBlocks.computeIfAbsent(baseline.getKey(),ignored->new LinkedHashSet<>());
            if(!destroyed.contains(packed)){Object subLevel=findSubLevel(level,baseline.getKey(),null);if(subLevel!=null){Level plotLevel=(Level)call(subLevel,"getLevel");if(plotLevel.isEmptyBlock(BlockPos.of(packed)))destroyed.add(packed);}}
            checked++;
        }
        if(checked>0)state.changed();
        int initial=entry.damageBaseline.values().stream().mapToInt(values->values.length).sum();int destroyed=entry.destroyedBlocks.values().stream().mapToInt(Set::size).sum();int intact=Math.max(0,initial-destroyed);
        if(initial!=entry.initialBlockCount||intact!=entry.intactBlockCount){entry.initialBlockCount=initial;entry.intactBlockCount=intact;state.changed();}
        return initial<=0?null:new BlockHealth(initial,intact,intact/(double)initial);
    }

    private static int claimHealthScanBudget(long tick,int requested){if(HEALTH_BUDGET_TICK!=tick){HEALTH_BUDGET_TICK=tick;HEALTH_SCAN_BUDGET_REMAINING=8192;}int granted=Math.min(requested,HEALTH_SCAN_BUDGET_REMAINING);HEALTH_SCAN_BUDGET_REMAINING-=granted;return granted;}

    private static boolean tracksBlockHealth(JsonObject action){
        if(destroyedCleanupFraction(action)>=0||action.has("boss_bar"))return true;
        if(action.has("sable_events")&&action.get("sable_events").isJsonArray())for(JsonElement raw:action.getAsJsonArray("sable_events"))if(raw.isJsonObject()){
            JsonObject event=raw.getAsJsonObject(),trigger=event.has("trigger")&&event.get("trigger").isJsonObject()?event.getAsJsonObject("trigger"):event;
            String type=trigger.has("type")?trigger.get("type").getAsString():"";
            if(type.equals("block_percent")||type.equals("health_percent")||type.equals("percent"))return true;
        }
        return false;
    }

    private static boolean damageCleanupReached(JsonObject action,BlockHealth health){
        double destroyedThreshold=destroyedCleanupFraction(action);
        return destroyedThreshold>=0&&health!=null&&(1-health.fraction())+1.0e-9>=destroyedThreshold;
    }

    private static double destroyedCleanupFraction(JsonObject action) {
        if (!action.has("destroyed_cleanup_percent") || action.get("destroyed_cleanup_percent").isJsonNull()) return -1;
        JsonElement raw = action.get("destroyed_cleanup_percent");
        if (raw.isJsonPrimitive() && raw.getAsJsonPrimitive().isString()) {
            String value = raw.getAsString();
            if (value.equalsIgnoreCase("none") || value.equalsIgnoreCase("disabled")) return -1;
        }
        double percent = raw.getAsDouble();
        if (percent <= 0) return -1;
        return Math.min(100, percent) / 100.0;
    }

    private static void updateBossBar(ServerLevel level,SableAmbushState.Entry entry,JsonObject action,BlockHealth health)throws Exception{
        if(!action.has("boss_bar")||action.get("boss_bar").isJsonNull()||action.get("boss_bar").isJsonPrimitive()&&!action.get("boss_bar").getAsBoolean()){
            removeBossBar(entry.actionId);return;
        }
        JsonObject config=action.get("boss_bar").isJsonObject()?action.getAsJsonObject("boss_bar"):new JsonObject();
        String defaultName=ResourceLocation.parse(entry.template).getPath().replace('_',' ');
        String name=string(config,"name",defaultName);
        net.minecraft.world.BossEvent.BossBarColor color=bossColor(string(config,"color","red"));
        net.minecraft.world.BossEvent.BossBarOverlay overlay=bossOverlay(string(config,"overlay","progress"));
        net.minecraft.server.level.ServerBossEvent bar=BOSS_BARS.computeIfAbsent(entry.actionId,id->new net.minecraft.server.level.ServerBossEvent(net.minecraft.network.chat.Component.literal(name),color,overlay));
        bar.setName(net.minecraft.network.chat.Component.literal(name));bar.setColor(color);bar.setOverlay(overlay);
        bar.setDarkenScreen(booleanValue(config,"darken_screen",false));bar.setPlayBossMusic(booleanValue(config,"play_music",false));bar.setCreateWorldFog(booleanValue(config,"create_world_fog",false));
        bar.setProgress((float)Math.max(0,Math.min(1,health==null?1:health.fraction())));
        Set<ServerPlayer> desired=new LinkedHashSet<>();
        String visibility=string(config,"visibility","owner");
        ServerPlayer owner=level.getServer().getPlayerList().getPlayer(entry.ownerId);
        if("all".equals(visibility))desired.addAll(level.players());
        else if("nearby".equals(visibility)){
            Vec3 center=structureWorldCenter(level,entry);
            double range=Math.max(1,Math.min(2048,number(config,"range",128)));
            if(center!=null)for(ServerPlayer player:level.players())if(player.position().distanceToSqr(center)<=range*range)desired.add(player);
        }else if(owner!=null&&owner.serverLevel()==level)desired.add(owner);
        for(ServerPlayer player:new ArrayList<>(bar.getPlayers()))if(!desired.contains(player))bar.removePlayer(player);
        for(ServerPlayer player:desired)if(!bar.getPlayers().contains(player))bar.addPlayer(player);
        bar.setVisible(!desired.isEmpty());
    }

    private static net.minecraft.world.BossEvent.BossBarColor bossColor(String value){
        try{return net.minecraft.world.BossEvent.BossBarColor.valueOf(value.toUpperCase(Locale.ROOT));}catch(IllegalArgumentException ignored){return net.minecraft.world.BossEvent.BossBarColor.RED;}
    }
    private static net.minecraft.world.BossEvent.BossBarOverlay bossOverlay(String value){
        try{return net.minecraft.world.BossEvent.BossBarOverlay.valueOf(value.toUpperCase(Locale.ROOT));}catch(IllegalArgumentException ignored){return net.minecraft.world.BossEvent.BossBarOverlay.PROGRESS;}
    }
    private static void removeBossBar(UUID actionId){net.minecraft.server.level.ServerBossEvent bar=BOSS_BARS.remove(actionId);if(bar!=null){bar.setVisible(false);bar.removeAllPlayers();}}

    private static void fireSableEvents(ServerLevel level,SableAmbushState.Entry entry,JsonObject definition,
                                        SableAmbushState state,BlockHealth health,boolean deathPhase)throws Exception{
        if(!definition.has("sable_events")||!definition.get("sable_events").isJsonArray())return;
        JsonArray events=definition.getAsJsonArray("sable_events");
        List<Integer> order=new ArrayList<>();for(int i=0;i<events.size();i++)order.add(i);
        order.sort((left,right)->{double a=eventHealthThreshold(events.get(left)),b=eventHealthThreshold(events.get(right));if(a<0&&b<0)return Integer.compare(left,right);if(a<0)return -1;if(b<0)return 1;return Double.compare(b,a);});
        for(int index:order){
            if(!events.get(index).isJsonObject())continue;
            JsonObject event=events.get(index).getAsJsonObject();
            String key=string(event,"id","event_"+index);
            long repeat=Math.max(0,event.has("repeat_ticks")?event.get("repeat_ticks").getAsLong():0);
            if(repeat<=0&&entry.firedEvents.contains(key))continue;
            if(repeat>0&&level.getGameTime()-entry.eventLastFired.getOrDefault(key,Long.MIN_VALUE/2)<repeat)continue;
            JsonObject trigger=event.has("trigger")&&event.get("trigger").isJsonObject()?event.getAsJsonObject("trigger"):event;
            String type=trigger.has("type")?trigger.get("type").getAsString():"spawn";
            boolean deathType=type.equals("death")||type.equals("destroyed");
            if(deathPhase!=deathType)continue;
            if(!eventTriggerReady(level,entry,trigger,type,health))continue;
            boolean defaultCrewGate=!type.equals("death")&&!type.equals("destroyed");
            if(booleanValue(event,"require_living_crew",defaultCrewGate)&&!hasLivingCrew(level,entry))continue;
            int results=enqueueEventActions(level,entry,key,event);
            if(results<=0){Ambush.LOGGER.warn("Sable event produced no result: action={} event={}",entry.actionId,key);continue;}
            if(repeat>0)entry.eventLastFired.put(key,level.getGameTime());else entry.firedEvents.add(key);
            state.changed();
            Ambush.LOGGER.info("Executed Sable event: action={} event={} results={}",entry.actionId,key,results);
        }
    }

    private static double eventHealthThreshold(JsonElement raw){if(!raw.isJsonObject())return -1;JsonObject event=raw.getAsJsonObject(),trigger=event.has("trigger")&&event.get("trigger").isJsonObject()?event.getAsJsonObject("trigger"):event;String type=string(trigger,"type","");return type.equals("block_percent")||type.equals("health_percent")||type.equals("percent")?number(trigger,"at_or_below_percent",number(trigger,"percent",50)):-1;}

    private static boolean eventTriggerReady(ServerLevel level,SableAmbushState.Entry entry,JsonObject trigger,String type,BlockHealth health)throws Exception{
        ServerPlayer owner=level.getServer().getPlayerList().getPlayer(entry.ownerId);
        if(type.equals("spawn")||type.equals("death")||type.equals("destroyed"))return true;
        if(type.equals("time")){
            long delay=trigger.has("after_ticks")?Math.max(0,trigger.get("after_ticks").getAsLong()):Math.max(0,Math.round(number(trigger,"after_seconds",0)*20));
            return level.getGameTime()-entry.redstoneTriggerStartedAt>=delay;
        }
        if(type.equals("range")){
            if(owner==null||owner.serverLevel()!=level)return false;Vec3 center=structureWorldCenter(level,entry);if(center==null)return false;
            double range=Math.max(0,number(trigger,"range",number(trigger,"distance",64))),dx=owner.getX()-center.x,dy=owner.getY()-center.y,dz=owner.getZ()-center.z;
            return booleanValue(trigger,"horizontal_only",false)?dx*dx+dz*dz<=range*range:dx*dx+dy*dy+dz*dz<=range*range;
        }
        if(type.equals("player_y"))return owner!=null&&owner.getY()>=number(trigger,"min_y",-2048)&&owner.getY()<=number(trigger,"max_y",2048);
        if(type.equals("block_percent")||type.equals("health_percent")||type.equals("percent")){
            double threshold=Math.max(0,Math.min(100,number(trigger,"at_or_below_percent",number(trigger,"percent",50))));
            return health!=null&&health.fraction()*100<=threshold+1.0e-9;
        }
        return false;
    }

    private static int enqueueEventActions(ServerLevel level,SableAmbushState.Entry entry,String eventId,JsonObject event)throws Exception{
        if(!event.has("actions")||!event.get("actions").isJsonArray())return 0;int queued=0,index=0;Vec3 origin=structureWorldCenter(level,entry);
        for(JsonElement raw:event.getAsJsonArray("actions")){if(!raw.isJsonObject()){index++;continue;}JsonObject wrapper=new JsonObject();wrapper.addProperty("type","sable_event_action");wrapper.addProperty("source_action_id",entry.actionId.toString());wrapper.addProperty("generation_depth",entry.generationDepth);if(origin!=null){wrapper.addProperty("origin_x",origin.x);wrapper.addProperty("origin_y",origin.y);wrapper.addProperty("origin_z",origin.z);}wrapper.add("event_action",raw.deepCopy());AmbushScheduleState.Entry scheduled=new AmbushScheduleState.Entry();scheduled.id=UUID.nameUUIDFromBytes((entry.actionId+"/"+eventId+"/"+index).getBytes(java.nio.charset.StandardCharsets.UTF_8));scheduled.ownerId=entry.ownerId;scheduled.dimension=entry.dimension;scheduled.dueGameTime=level.getServer().overworld().getGameTime()+1;scheduled.actionJson=wrapper.toString();scheduled.origin=origin;AmbushScheduleState.get(level.getServer()).add(scheduled);queued++;index++;}
        return queued;
    }

    static int executeScheduledEventAction(ServerPlayer owner,JsonObject wrapper)throws Exception{
        if(!wrapper.has("source_action_id")||!wrapper.has("event_action"))return 0;UUID sourceId=UUID.fromString(wrapper.get("source_action_id").getAsString());SableAmbushState.Entry entry=null;
        for(SableAmbushState.Entry candidate:SableAmbushState.get(owner.server).entries())if(candidate.actionId.equals(sourceId)){entry=candidate;break;}
        if(entry==null||!entry.active()||!entry.dimension.equals(owner.serverLevel().dimension().location().toString()))return executeDetachedEventAction(owner,wrapper);
        return executeEventAction(owner.serverLevel(),entry,wrapper.getAsJsonObject("event_action"));
    }

    private static int executeDetachedEventAction(ServerPlayer owner,JsonObject wrapper){
        JsonObject action=wrapper.getAsJsonObject("event_action");String type=string(action,"type","");int depth=integer(wrapper,"generation_depth",0)+1;
        if(type.equals("sound")&&action.has("sound")){ResourceLocation id=ResourceLocation.parse(action.get("sound").getAsString());if(!BuiltInRegistries.SOUND_EVENT.containsKey(id))return 0;double x=number(wrapper,"origin_x",owner.getX()),y=number(wrapper,"origin_y",owner.getY()),z=number(wrapper,"origin_z",owner.getZ());owner.serverLevel().playSound(null,x,y,z,BuiltInRegistries.SOUND_EVENT.get(id),net.minecraft.sounds.SoundSource.HOSTILE,(float)number(action,"volume",1),(float)number(action,"pitch",1));return 1;}
        if(type.equals("ambush")){String id=string(action,"ambush",string(action,"id",""));return !id.isBlank()&&AmbushRuntime.triggerChained(owner,id,booleanValue(action,"force",true),depth)?1:0;}
        if(type.equals("sable_structure")||type.equals("sable_formation")){if(depth>8||activeShipCount(owner)>=Math.max(1,Math.min(128,integer(action,"max_active_ships",16))))return 0;JsonObject child=action.deepCopy();child.addProperty("_ambush_parent_action_id",wrapper.get("source_action_id").getAsString());child.addProperty("_ambush_generation_depth",depth);JsonObject definition=new JsonObject();JsonArray actions=new JsonArray();actions.add(child);definition.add("actions",actions);return SableCompat.apply(owner,definition,true)>0?1:0;}
        if(type.equals("fog"))return FogAction.apply(owner,action);
        if(type.equals("fireworks"))return FireworkAction.spawn(owner.serverLevel(),new Vec3(number(wrapper,"origin_x",owner.getX()),number(wrapper,"origin_y",owner.getY()),number(wrapper,"origin_z",owner.getZ())),action);
        if(type.equals("effect"))return applyPlayerEffect(owner,action);
        if(type.equals("directional_entity_wave")||type.equals("conditional_spawn"))return AmbushRuntime.executeSableLifecycleAction(owner,action,new Vec3(number(wrapper,"origin_x",owner.getX()),number(wrapper,"origin_y",owner.getY()),number(wrapper,"origin_z",owner.getZ())));
        return 0;
    }

    private static int executeEventAction(ServerLevel level,SableAmbushState.Entry entry,JsonObject action)throws Exception{
        ServerPlayer owner=level.getServer().getPlayerList().getPlayer(entry.ownerId);
        Object subLevel=findSubLevel(level,entry.subLevelId,null);Level subLevelLevel=subLevel==null?null:(Level)call(subLevel,"getLevel");
        AABB bounds=subLevel==null?null:(AABB)call(call(call(subLevel,"getPlot"),"getBoundingBox"),"toAABB");
        String type=string(action,"type","");if(booleanValue(action,"require_living_crew",false)&&!hasLivingCrew(level,entry))return 0;
        if(type.equals("sound"))return playSableSound(level,entry,action)?1:0;
        if((type.equals("redstone")||type.equals("redstone_activation"))&&subLevelLevel!=null&&bounds!=null){JsonObject activation=action.has("activation")&&action.get("activation").isJsonObject()?action.getAsJsonObject("activation"):action;return activateRedstone(subLevelLevel,bounds,activation)>0?1:0;}
        int nextDepth=entry.generationDepth+1;int maxDepth=Math.max(0,Math.min(16,integer(action,"max_generation_depth",8)));int maxShips=Math.max(1,Math.min(128,integer(action,"max_active_ships",16)));
        if(type.equals("ambush")&&owner!=null){if(nextDepth>Math.min(8,maxDepth))return 0;String id=string(action,"ambush",string(action,"id",""));return !id.isBlank()&&AmbushRuntime.triggerChained(owner,id,booleanValue(action,"force",true),nextDepth)?1:0;}
        if((type.equals("sable_structure")||type.equals("sable_formation"))&&owner!=null){if(nextDepth>Math.min(8,maxDepth)||activeShipCount(owner)>=maxShips)return 0;JsonObject child=action.deepCopy();child.addProperty("_ambush_parent_action_id",entry.actionId.toString());child.addProperty("_ambush_generation_depth",nextDepth);JsonObject definition=new JsonObject();JsonArray actions=new JsonArray();actions.add(child);definition.add("actions",actions);return SableCompat.apply(owner,definition,true)>0?1:0;}
        if(type.equals("fog")&&owner!=null)return FogAction.apply(owner,action);
        if(type.equals("fireworks")){Vec3 center=structureWorldCenter(level,entry);if(center==null&&owner!=null)center=owner.position();return center==null?0:FireworkAction.spawn(level,center,action);}
        if(type.equals("effect")&&owner!=null)return applyPlayerEffect(owner,action);
        if((type.equals("directional_entity_wave")||type.equals("conditional_spawn"))&&owner!=null)return AmbushRuntime.executeSableLifecycleAction(owner,action,structureWorldCenter(level,entry));
        return 0;
    }

    private static int activeShipCount(ServerPlayer owner){int count=0;for(SableAmbushState.Entry entry:SableAmbushState.get(owner.server).entries())if(entry.ownerId.equals(owner.getUUID())&&entry.active()&&!entry.cleanupRequested)count++;return count;}

    private static boolean playSableSound(ServerLevel level,SableAmbushState.Entry entry,JsonObject action)throws Exception{
        if(!action.has("sound"))return false;ResourceLocation id=ResourceLocation.parse(action.get("sound").getAsString());
        if(!BuiltInRegistries.SOUND_EVENT.containsKey(id))return false;Vec3 center=structureWorldCenter(level,entry);ServerPlayer owner=level.getServer().getPlayerList().getPlayer(entry.ownerId);
        if(center==null&&owner!=null)center=owner.position();if(center==null)return false;
        if("player".equals(string(action,"at","structure"))&&owner!=null)center=owner.position();
        else if(owner!=null&&action.has("audible_distance")){double distance=Math.max(1,number(action,"audible_distance",32));Vec3 delta=center.subtract(owner.position());if(delta.length()>distance)center=owner.position().add(delta.normalize().scale(distance));}
        level.playSound(null,center.x,center.y,center.z,BuiltInRegistries.SOUND_EVENT.get(id),net.minecraft.sounds.SoundSource.HOSTILE,(float)number(action,"volume",1),(float)number(action,"pitch",1));return true;
    }

    private static int applyPlayerEffect(ServerPlayer player,JsonObject action){ResourceLocation id=ResourceLocation.parse(string(action,"effect","minecraft:hero_of_the_village"));var holder=BuiltInRegistries.MOB_EFFECT.getHolder(id);if(holder.isEmpty())return 0;int duration=Math.max(1,integer(action,"duration_ticks",integer(action,"duration_seconds",300)*20)),amplifier=Math.max(0,integer(action,"amplifier",0));player.addEffect(new net.minecraft.world.effect.MobEffectInstance(holder.get(),duration,amplifier));return 1;}

    private static Vec3 structureWorldCenter(ServerLevel level,SableAmbushState.Entry entry)throws Exception{
        Object subLevel=findSubLevel(level,entry.subLevelId,null);if(subLevel==null)return null;Level plotLevel=(Level)call(subLevel,"getLevel");
        AABB bounds=(AABB)call(call(call(subLevel,"getPlot"),"getBoundingBox"),"toAABB");Object helper=Class.forName("dev.ryanhcode.sable.Sable").getField("HELPER").get(null);
        return (Vec3)call(helper,"projectOutOfSubLevel",plotLevel,bounds.getCenter());
    }

    private static boolean hasLivingCrew(ServerLevel level,SableAmbushState.Entry entry)throws Exception{
        Set<UUID> crewIds=entry.crewEntityIds.isEmpty()?new LinkedHashSet<>(entry.entityIds):entry.crewEntityIds;if(crewIds.isEmpty())return false;
        for(UUID id:crewIds){Entity entity=level.getEntity(id);if(entity instanceof LivingEntity living&&living.isAlive()&&!living.isRemoved())return true;}
        Object subLevel=findSubLevel(level,entry.subLevelId,null);if(subLevel==null)return false;Level plotLevel=(Level)call(subLevel,"getLevel");AABB bounds=(AABB)call(call(call(subLevel,"getPlot"),"getBoundingBox"),"toAABB");
        return !plotLevel.getEntitiesOfClass(LivingEntity.class,bounds.inflate(2),entity->entity.isAlive()&&crewIds.contains(entity.getUUID())).isEmpty();
    }

    private static void ensureChildAttachments(ServerLevel level, SableAmbushState.Entry entry, JsonObject action,
                                               SableAmbushState state) throws Exception {
        if (!booleanValue(action, "attach_child_sublevels", false)) return;
        List<Object> family = parentAndChildren(level, entry.subLevelId);
        if (family.size() < 2) return;
        Object parent = family.getFirst();
        Object parentPose = call(parent, "logicalPose");
        Object container = callStatic(Class.forName("dev.ryanhcode.sable.api.sublevel.SubLevelContainer"), "getContainer", level);
        if (container == null) return;
        Object pipeline = call(call(container, "physicsSystem"), "getPipeline");

        for (int i = 1; i < family.size(); i++) {
            Object child = family.get(i);
            UUID childId = (UUID) call(child, "getUniqueId");
            Object existing = FIXED_CHILD_HANDLES.get(childId);
            if (existing != null) {
                try { if ((boolean) call(existing, "isValid")) continue; }
                catch (Exception ignored) {}
                FIXED_CHILD_HANDLES.remove(childId);
            }

            double[] constraint = entry.childAttachments.get(childId);
            if (constraint == null) {
                AABB childBounds = (AABB) call(call(call(child, "getPlot"), "getBoundingBox"), "toAABB");
                Vec3 childAnchor = childBounds.getCenter();
                Vec3 worldAnchor = (Vec3) call(call(child, "logicalPose"), "transformPosition", childAnchor);
                Vec3 parentAnchor = (Vec3) call(parentPose, "transformPositionInverse", worldAnchor);
                Quaterniond parentOrientation = quaternion(call(parentPose, "orientation"));
                Quaterniond childOrientation = quaternion(call(call(child, "logicalPose"), "orientation"));
                Quaterniond relativeOrientation = parentOrientation.invert(new Quaterniond()).mul(childOrientation);
                constraint = new double[] { parentAnchor.x, parentAnchor.y, parentAnchor.z,
                    childAnchor.x, childAnchor.y, childAnchor.z,
                    relativeOrientation.x, relativeOrientation.y, relativeOrientation.z, relativeOrientation.w };
                entry.childAttachments.put(childId, constraint);
                state.changed();
            }

            Object configuration = Class.forName("dev.ryanhcode.sable.api.physics.constraint.FixedConstraintConfiguration")
                .getConstructor(Class.forName("org.joml.Vector3dc"), Class.forName("org.joml.Vector3dc"),
                    Class.forName("org.joml.Quaterniondc"))
                .newInstance(new Vector3d(constraint[0], constraint[1], constraint[2]),
                    new Vector3d(constraint[3], constraint[4], constraint[5]),
                    new Quaterniond(constraint[6], constraint[7], constraint[8], constraint[9]));
            Object handle = call(pipeline, "addConstraint", parent, child, configuration);
            call(handle, "setContactsEnabled", false);
            FIXED_CHILD_HANDLES.put(childId, handle);
            Ambush.LOGGER.info("Fixed child Sable sublevel={} to parent={} for action={}", childId, entry.subLevelId, entry.actionId);
        }
    }

    private static Quaterniond quaternion(Object value) throws Exception {
        return new Quaterniond(((Number) call(value, "x")).doubleValue(),
            ((Number) call(value, "y")).doubleValue(), ((Number) call(value, "z")).doubleValue(),
            ((Number) call(value, "w")).doubleValue());
    }

    private static Iterable<BlockPos> positions(AABB bounds) {
        return BlockPos.betweenClosed(
            BlockPos.containing(bounds.minX, bounds.minY, bounds.minZ),
            BlockPos.containing(bounds.maxX, bounds.maxY, bounds.maxZ));
    }

    private static JsonArray singletonArray(JsonElement element) {
        JsonArray array = new JsonArray();
        array.add(element);
        return array;
    }

    private static Set<String> stringSet(JsonElement element) {
        if (element == null || element.isJsonNull()) return Set.of();
        if (element.isJsonPrimitive()) return Set.of(element.getAsString());
        Set<String> values = new HashSet<>();
        if (element.isJsonArray()) for (JsonElement value : element.getAsJsonArray()) values.add(value.getAsString());
        return values;
    }

    private static String actionEntityTag(UUID actionId) {
        return "ambush_sable_action_" + actionId.toString().replace("-", "");
    }

    private static BlockPos selectAnchor(ServerLevel level, JsonObject action, StructureTemplate template, BlockPos requested) {
        if (!"air".equals(string(action, "placement", "offset"))) return requested;
        int attempts = Math.max(1, Math.min(16, integer(action, "air_search_attempts", 8)));
        int step = Math.max(1, Math.min(32, integer(action, "air_step", 4)));
        for (int attempt = 0; attempt < attempts; attempt++) for (int ring = 0; ring <= 2; ring++) {
            int dx = ring == 0 ? 0 : (attempt % 2 == 0 ? ring * step : -ring * step);
            int dz = ring == 0 ? 0 : (attempt % 2 == 0 ? -ring * step : ring * step);
            BlockPos candidate = requested.offset(dx, attempt * step, dz);
            if (airSpaceClear(level, candidate, template.getSize())) return candidate;
        }
        throw new IllegalStateException("No clear, already-loaded air volume for template at or above " + requested.toShortString());
    }

    private static boolean airSpaceClear(ServerLevel level, BlockPos anchor, Vec3i size) {
        if (size.getX() <= 0 || size.getY() <= 0 || size.getZ() <= 0) return false;
        long volume = (long) size.getX() * size.getY() * size.getZ();
        if (volume > 262_144L) throw new IllegalArgumentException("Air-placement template volume exceeds safe scan limit: " + volume);
        BlockPos max = anchor.offset(size.getX() - 1, size.getY() - 1, size.getZ() - 1);
        if (anchor.getY() < level.getMinBuildHeight() || max.getY() >= level.getMaxBuildHeight()) return false;
        if (!level.hasChunkAt(anchor) || !level.hasChunkAt(max) ||
            !level.hasChunkAt(new BlockPos(anchor.getX(), anchor.getY(), max.getZ())) ||
            !level.hasChunkAt(new BlockPos(max.getX(), anchor.getY(), anchor.getZ()))) return false;
        for (BlockPos pos : BlockPos.betweenClosed(anchor, max)) if (!level.isEmptyBlock(pos)) return false;
        return true;
    }

    private static Entity createEntity(ServerLevel level, JsonObject spec) throws Exception {
        CompoundTag nbt = new CompoundTag();
        if (spec.has("nbt")) {
            JsonElement raw = spec.get("nbt");
            nbt = TagParser.parseTag(raw.isJsonPrimitive() ? raw.getAsString() : GSON.toJson(raw));
        }
        String entityId = string(spec, "entity", nbt.getString("id"));
        if (entityId.isBlank()) throw new IllegalArgumentException("Sable entity requires entity or nbt.id");
        nbt.putString("id", ResourceLocation.parse(entityId).toString());
        return EntityType.loadEntityRecursive(nbt, level, entity -> entity);
    }

    private static JsonArray legacyEntity(JsonObject action) {
        JsonArray array = new JsonArray();
        if (!action.has("entity")) return array;
        JsonObject entity = new JsonObject();
        entity.addProperty("entity", action.get("entity").getAsString());
        entity.addProperty("local_y", integer(action, "entity_offset_y", 1));
        entity.addProperty("persistent", true);
        entity.addProperty("target", "owner");
        entity.add("tags", JsonParser.parseString("[\"ambush_sable_guard\"]"));
        array.add(entity);
        return array;
    }

    private static void requestCleanup(ServerLevel level, SableAmbushState.Entry entry, SableAmbushState state) throws Exception {
        discardOwnedEntities(level, entry);
        removeFixedConstraints(entry);
        destroySubLevelFamily(level, entry);
        entry.cleanupRequested = true;
        entry.lastCleanupRequest = level.getGameTime();
        state.changed();
        LIVE_CONTEXTS.remove(entry.actionId);
        Ambush.LOGGER.info("Requested cleanup for Sable ambush action={} sublevel={}", entry.actionId, entry.subLevelId);
    }

    private static void removeFixedConstraints(SableAmbushState.Entry entry) {
        for (UUID childId : new ArrayList<>(entry.childAttachments.keySet())) {
            Object handle = FIXED_CHILD_HANDLES.remove(childId);
            if (handle == null) continue;
            try { call(handle, "remove"); }
            catch (Exception ex) { Ambush.LOGGER.warn("Could not remove fixed Sable child constraint {}", childId, ex); }
        }
    }

    private static void destroySubLevelFamily(ServerLevel level, SableAmbushState.Entry entry) throws Exception {
        Class<?> containerClass = Class.forName("dev.ryanhcode.sable.api.sublevel.SubLevelContainer");
        Object container = callStatic(containerClass, "getContainer", level);
        if (container == null) throw new IllegalStateException("Sable container unavailable during Ambush cleanup");
        LinkedHashMap<UUID, Object> family = new LinkedHashMap<>();
        ArrayDeque<UUID> pending = new ArrayDeque<>();
        pending.add(entry.subLevelId);
        while (!pending.isEmpty()) {
            UUID id = pending.removeFirst();
            if (family.containsKey(id)) continue;
            Object subLevel = call(container, "getSubLevel", id);
            if (subLevel == null) continue;
            family.put(id, subLevel);
        }
        for (UUID tracked : entry.childAttachments.keySet()) {
            if (family.containsKey(tracked)) continue;
            Object subLevel = call(container, "getSubLevel", tracked);
            if (subLevel != null) family.put(tracked, subLevel);
        }
        for (UUID baseline : entry.damageBaseline.keySet()) {
            if (family.containsKey(baseline)) continue;
            Object subLevel = call(container, "getSubLevel", baseline);
            if (subLevel != null) family.put(baseline, subLevel);
        }
        List<Map.Entry<UUID, Object>> removalOrder = new ArrayList<>(family.entrySet());
        Collections.reverse(removalOrder);
        Class<?> ticketTypeClass = Class.forName("dev.ryanhcode.sable.api.sublevel.ticket.SubLevelLoadingTicketType");
        Object commandForced = ticketTypeClass.getField("COMMAND_FORCED").get(null);
        Object unit = Class.forName("net.minecraft.util.Unit").getField("INSTANCE").get(null);
        Class<?> reasonClass = Class.forName("dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason");
        Object removed = Enum.valueOf((Class<? extends Enum>) reasonClass.asSubclass(Enum.class), "REMOVED");
        int deleted = 0;
        for (Map.Entry<UUID, Object> member : removalOrder) {
            Object subLevel = member.getValue();
            try { call(container, "removeForceLoadTicket", subLevel, commandForced, unit); }
            catch (Exception ticketFailure) {
                Ambush.LOGGER.debug("No COMMAND_FORCED ticket removed for Sable sublevel={}", member.getKey(), ticketFailure);
            }
            call(container, "removeSubLevel", subLevel, removed);
            deleted++;
        }
        Ambush.LOGGER.info("Directly removed Sable ambush family: action={} parent={} sublevels={}",
            entry.actionId, entry.subLevelId, deleted);
    }

    private static void finishCleanup(ServerLevel level, SableAmbushState.Entry entry, SableAmbushState state) {
        removeBossBar(entry.actionId);
        discardOwnedEntities(level, entry);
        removeFixedConstraints(entry);
        CHILD_ATTACHMENT_WARN_AFTER.remove(entry.actionId);
        LIVE_CONTEXTS.remove(entry.actionId);
        state.remove(entry.actionId);
    }

    private static void playDespawnEffect(ServerLevel level, SableAmbushState.Entry entry, JsonObject action,
                                          SableAmbushState state) throws Exception {
        if (entry.despawnEffectPlayed) return;
        JsonElement raw = action.get("despawn_effect");
        if (raw == null || raw.isJsonNull()) {
            entry.despawnEffectPlayed = true;
            state.changed();
            return;
        }
        JsonObject effect = raw.isJsonObject() ? raw.getAsJsonObject() : new JsonObject();
        String type = raw.isJsonPrimitive() ? raw.getAsString() : string(effect, "type", "none");
        if ("explosion".equals(type)) {
            Object subLevel = findSubLevel(level, entry.subLevelId, null);
            if (subLevel != null) {
                Level subLevelLevel = (Level) call(subLevel, "getLevel");
                AABB bounds = (AABB) call(call(call(subLevel, "getPlot"), "getBoundingBox"), "toAABB");
                Vec3 plotCenter = bounds.getCenter();
                Object sableHelper = Class.forName("dev.ryanhcode.sable.Sable").getField("HELPER").get(null);
                Vec3 worldCenter = (Vec3) call(sableHelper, "projectOutOfSubLevel", subLevelLevel, plotCenter);
                float power = (float)Math.max(0, Math.min(16, number(effect, "power", 3.0)));
                boolean fire = booleanValue(effect, "fire", false);
                Level.ExplosionInteraction interaction = booleanValue(effect, "block_damage", false)
                    ? Level.ExplosionInteraction.BLOCK : Level.ExplosionInteraction.NONE;
                level.explode(null, worldCenter.x, worldCenter.y, worldCenter.z, power, fire, interaction);
            }
        } else if (!"none".equals(type)) {
            Ambush.LOGGER.warn("Ignoring unsupported Sable despawn_effect type '{}' for action={}", type, entry.actionId);
        }
        entry.despawnEffectPlayed = true;
        state.changed();
    }

    private static void discardOwnedEntities(ServerLevel level, SableAmbushState.Entry entry) {
        String actionTag = actionEntityTag(entry.actionId);
        Set<UUID> trackedIds = new HashSet<>(entry.entityIds);
        List<Entity> loadedEntities = new ArrayList<>();
        level.getEntities().getAll().forEach(loadedEntities::add);
        for (Entity entity : loadedEntities) {
            if (!trackedIds.contains(entity.getUUID()) && !entity.getTags().contains(actionTag)) continue;
            Entity vehicle = entity.getVehicle();
            entity.stopRiding();
            if (vehicle != null && (vehicle.getTags().contains("ambush_sable_seat") ||
                    vehicle.getTags().contains(actionTag))) vehicle.discard();
            entity.discard();
        }
    }

    private static ServerLevel level(MinecraftServer server, String id) {
        return server.getLevel(ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(id)));
    }

    private static double local(JsonObject spec, int index, String key, double fallback) {
        if (spec.has("local") && spec.get("local").isJsonArray() && spec.getAsJsonArray("local").size() > index)
            return spec.getAsJsonArray("local").get(index).getAsDouble();
        return number(spec, key, fallback);
    }

    private static double resolveYawDegrees(ServerPlayer player, BlockPos anchor, JsonObject action) {
        boolean explicitlyDirected = action.has("facing") || action.has("direction") || booleanValue(action, "face_player", false);
        // Formation members commonly inherit yaw_degrees: 0 from their parent action.
        // For distance-based ships, an unspecified heading should face the player;
        // explicit facing/direction still wins, and non-distance structures preserve
        // their legacy yaw_degrees behavior.
        if (!explicitlyDirected && action.has("yaw_degrees") && !action.has("spawn_distance")) return number(action, "yaw_degrees", 0);
        String facing = booleanValue(action, "face_player", false) ? "player"
            : string(action, "facing", string(action, "direction", "north")).toLowerCase(Locale.ROOT);
        double towardPlayer = Math.toDegrees(Math.atan2(
            -(player.getX() - (anchor.getX() + .5)), -(player.getZ() - (anchor.getZ() + .5))));
        double desired = switch (facing) {
            case "player" -> towardPlayer;
            case "orbit_clockwise", "clockwise" -> towardPlayer + 90;
            case "orbit_counterclockwise", "counterclockwise" -> towardPlayer - 90;
            default -> cardinalYaw(facing);
        };
        String baseFacing = string(action, "base_facing", "north").toLowerCase(Locale.ROOT);
        return wrapDegrees(desired - cardinalYaw(baseFacing));
    }

    private static double cardinalYaw(String facing) {
        return switch (facing) {
            case "east" -> -90;
            case "south" -> 180;
            case "west" -> 90;
            default -> 0;
        };
    }

    private static double wrapDegrees(double degrees) {
        double wrapped = degrees % 360;
        return wrapped <= -180 ? wrapped + 360 : wrapped > 180 ? wrapped - 360 : wrapped;
    }

    private static int resolveThrottleSignal(ServerPlayer player, JsonObject action) {
        if (cannonballoonFlightProfile(action)) {
            int y = player.blockPosition().getY();
            return y <= 80 ? 3 : 4;
        }
        int fallback = Math.max(1, Math.min(15, integer(action, "throttle_signal", 1)));
        if (!action.has("throttle_signal_by_y") || !action.get("throttle_signal_by_y").isJsonArray()) return fallback;
        int y = player.blockPosition().getY();
        for (JsonElement raw : action.getAsJsonArray("throttle_signal_by_y")) {
            if (!raw.isJsonObject()) continue;
            JsonObject rule = raw.getAsJsonObject();
            int min = integer(rule, "min_y", Integer.MIN_VALUE);
            int max = integer(rule, "max_y", Integer.MAX_VALUE);
            if (y >= min && y <= max) return Math.max(1, Math.min(15, integer(rule, "signal", fallback)));
        }
        return fallback;
    }

    /** Optional data-driven safeguard for airship encounters. */
    private static boolean surfaceAllowed(ServerPlayer player, JsonObject action) {
        if (!booleanValue(action, "require_surface", cannonballoonFlightProfile(action))) return true;
        int surface = player.serverLevel().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            player.blockPosition().getX(), player.blockPosition().getZ());
        return player.blockPosition().getY() >= surface - 1;
    }

    /** Built-in profile for the bundled cannonballoon template; datapacks may opt out. */
    private static boolean cannonballoonFlightProfile(JsonObject action) {
        if (action.has("cannonballoon_flight_profile"))
            return action.get("cannonballoon_flight_profile").getAsBoolean();
        return string(action, "template", "").equals("ambush:cannonballoon");
    }

    private static Object callStatic(Class<?> type, String name, Object... args) throws Exception {
        return invoke(null, type, name, args);
    }
    private static Object call(Object target, String name, Object... args) throws Exception {
        return invoke(target, target.getClass(), name, args);
    }
    private static Object fieldValue(Object target,String name)throws Exception{for(Class<?> type=target.getClass();type!=null;type=type.getSuperclass())try{Field field=type.getDeclaredField(name);field.setAccessible(true);return field.get(target);}catch(NoSuchFieldException ignored){}throw new NoSuchFieldException(target.getClass().getName()+'.'+name);}
    private static Object invoke(Object target, Class<?> type, String name, Object... args) throws Exception {
        for (Class<?> invocableType : invocableTypes(type)) for (Method method : invocableType.getMethods()) {
            if (!method.getName().equals(name) || method.getParameterCount() != args.length) continue;
            Class<?>[] params = method.getParameterTypes();
            boolean matches = true;
            for (int i = 0; i < params.length; i++) if (!compatible(params[i], args[i])) { matches = false; break; }
            if (!matches) continue;
            try { return method.invoke(target, args); }
            catch (InvocationTargetException ex) {
                if (ex.getCause() instanceof Exception cause) throw cause;
                throw ex;
            }
        }
        throw new NoSuchMethodException(type.getName() + "." + name + "/" + args.length);
    }
    private static List<Class<?>> invocableTypes(Class<?> type) {
        LinkedHashSet<Class<?>> result = new LinkedHashSet<>();
        collectInvocableTypes(type, result);
        return new ArrayList<>(result);
    }
    private static void collectInvocableTypes(Class<?> type, Set<Class<?>> result) {
        if (type == null) return;
        for (Class<?> iface : type.getInterfaces()) {
            if (Modifier.isPublic(iface.getModifiers())) result.add(iface);
            collectInvocableTypes(iface, result);
        }
        if (Modifier.isPublic(type.getModifiers())) result.add(type);
        collectInvocableTypes(type.getSuperclass(), result);
    }
    private static boolean compatible(Class<?> parameter, Object value) {
        if (value == null) return !parameter.isPrimitive();
        if (parameter.isInstance(value)) return true;
        return (parameter == int.class && value instanceof Integer) ||
            (parameter == long.class && value instanceof Long) ||
            (parameter == double.class && value instanceof Double) ||
            (parameter == float.class && value instanceof Float) ||
            (parameter == boolean.class && value instanceof Boolean);
    }
    private static int integer(JsonObject o, String k, int d) { return o.has(k) ? o.get(k).getAsInt() : d; }
    private static double number(JsonObject o, String k, double d) { return o.has(k) ? o.get(k).getAsDouble() : d; }
    private static String string(JsonObject o, String k, String d) { return o.has(k) ? o.get(k).getAsString() : d; }
    private static boolean booleanValue(JsonObject o, String k, boolean d) { return o.has(k) ? o.get(k).getAsBoolean() : d; }
    private static long lifetimeTicks(JsonObject action) {
        if (!action.has("lifetime_ticks")) return 6000;
        if (action.get("lifetime_ticks").isJsonNull()) return -1;
        JsonElement value = action.get("lifetime_ticks");
        if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
            String text = value.getAsString().trim();
            if (text.equalsIgnoreCase("none") || text.equalsIgnoreCase("permanent")) return -1;
            try { return Math.max(20, Long.parseLong(text)); }
            catch (NumberFormatException ex) { throw new IllegalArgumentException("lifetime_ticks must be a number, null, none, or permanent", ex); }
        }
        return Math.max(20, value.getAsLong());
    }
}
