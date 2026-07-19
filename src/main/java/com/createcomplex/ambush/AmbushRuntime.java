package com.createcomplex.ambush;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.SharedSuggestionProvider;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.*;
import com.google.gson.*;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;

final class AmbushRuntime {
    private static AmbushRuntime INSTANCE;
    private static final ThreadLocal<ArrayDeque<String>> CHAIN_STACK=ThreadLocal.withInitial(ArrayDeque::new);
    private static final ThreadLocal<Integer> GENERATION_DEPTH=ThreadLocal.withInitial(()->0);
    static boolean commandOverride;
    private AmbushRegistry registry = new AmbushRegistry();
    private final Map<UUID,String> reports = new HashMap<>();
    private final List<ScheduledRain> scheduledRains = new ArrayList<>();
    private boolean debug;
    AmbushRuntime(){INSTANCE=this;}
    static boolean triggerChained(ServerPlayer player,String id,boolean force){return triggerChained(player,id,force,GENERATION_DEPTH.get()+1);}
    static boolean triggerChained(ServerPlayer player,String id,boolean force,int depth){
        if(INSTANCE==null)return false;String normalized=id.contains(":")?id:"ambush:"+id;ArrayDeque<String> stack=CHAIN_STACK.get();
        if(depth>8||stack.size()>=8||stack.contains(normalized)){Ambush.LOGGER.warn("Rejected recursive chained ambush {} depth={} for {}",normalized,depth,player.getUUID());return false;}
        int previous=GENERATION_DEPTH.get();GENERATION_DEPTH.set(depth);stack.push(normalized);try{return INSTANCE.trigger(player,normalized,force,force);}finally{stack.pop();GENERATION_DEPTH.set(previous);if(stack.isEmpty())CHAIN_STACK.remove();if(previous==0)GENERATION_DEPTH.remove();}
    }
    static int currentGenerationDepth(){return GENERATION_DEPTH.get();}
    static int executeSableLifecycleAction(ServerPlayer player,JsonObject action,Vec3 origin){
        if(INSTANCE==null||!ActionConditions.matches(player,action))return 0;
        String type=action.has("type")?action.get("type").getAsString():"";
        if(type.equals("directional_entity_wave")){INSTANCE.scheduleDirectionalWaves(player,action);return 1;}
        if(type.equals("conditional_spawn")){long delay=action.has("after_ticks")?action.get("after_ticks").getAsLong():0;INSTANCE.scheduleDataAction(player,action,origin,delay);return 1;}
        return 0;
    }
    private record ScheduledRain(long tick, ServerPlayer player, JsonObject action) {}
    @SubscribeEvent void reload(AddReloadListenerEvent e){ e.addListener(registry); }
    @SubscribeEvent void commands(RegisterCommandsEvent e){
        e.getDispatcher().register(Commands.literal("ambush").requires(s->s.hasPermission(2))
            .then(Commands.literal("list").executes(c->{ c.getSource().sendSuccess(()->net.minecraft.network.chat.Component.literal(registry.all().stream().map(AmbushDefinition::id).sorted().reduce((a,b)->a+"\n"+b).orElse("No ambushes loaded")),false); return 1; }))
            .then(Commands.literal("validate").executes(c->{ c.getSource().sendSuccess(()->net.minecraft.network.chat.Component.literal("Loaded "+registry.all().size()+" valid ambush definitions."),false); return 1; }))
            .then(Commands.literal("debug").executes(c->{ debug=!debug; Ambush.LOGGER.info("Debug mode {} by {}",debug,c.getSource().getTextName()); c.getSource().sendSuccess(()->net.minecraft.network.chat.Component.literal("Ambush debug "+(debug?"enabled":"disabled")+"; details are now logged to the server console."),false); return 1; }))
            .then(Commands.literal("weights").executes(c->{ ServerPlayer player=c.getSource().getPlayer(); String text=registry.all().stream().sorted(Comparator.comparing(AmbushDefinition::id)).map(d->{int failures=player==null?0:state(player).chanceFailures(player.getUUID()).getOrDefault(d.id(),0);return d.id()+" chance="+d.chancePolicy().current(failures)+"% base="+d.chancePolicy().basePercent()+"% mode="+d.chancePolicy().mode()+" failures="+failures+" max="+d.chancePolicy().maximumPercent()+"% trigger="+d.trigger()+" every="+d.interval()+"t cooldown="+d.cooldown()+"s";}).reduce((a,b)->a+"\n"+b).orElse("No ambushes loaded"); Ambush.LOGGER.info("Ambush weights:\n{}",text); c.getSource().sendSuccess(()->net.minecraft.network.chat.Component.literal(text),false); return 1; }))
            .then(Commands.literal("state").executes(c->{ ServerPlayer p=c.getSource().getPlayerOrException(); c.getSource().sendSuccess(()->net.minecraft.network.chat.Component.literal(stateText(p)),false); return 1; }))
            .then(Commands.literal("always").then(Commands.argument("type", ResourceLocationArgument.id()).suggests((c,b)->SharedSuggestionProvider.suggest(registry.all().stream().map(AmbushDefinition::id),b)).executes(c->{ ServerPlayer target=c.getSource().getPlayerOrException(); boolean ok=trigger(target,ResourceLocationArgument.getId(c,"type").toString(),true,true); c.getSource().sendSuccess(()->net.minecraft.network.chat.Component.literal(reports.getOrDefault(target.getUUID(),"Ambush failed: no safe spawn was found.")),true); return ok?1:0; }).then(Commands.argument("player", EntityArgument.player()).executes(c->{ ServerPlayer target=EntityArgument.getPlayer(c,"player"); boolean ok=trigger(target,ResourceLocationArgument.getId(c,"type").toString(),true,true); c.getSource().sendSuccess(()->net.minecraft.network.chat.Component.literal(reports.getOrDefault(target.getUUID(),"Ambush failed: no safe spawn was found for "+target.getGameProfile().getName()+".")),true); return ok?1:0; }))))
            .then(Commands.argument("type", ResourceLocationArgument.id()).suggests((c,b)->SharedSuggestionProvider.suggest(registry.all().stream().map(AmbushDefinition::id),b)).executes(c->{ ServerPlayer target=c.getSource().getPlayerOrException(); boolean ok=trigger(target,ResourceLocationArgument.getId(c,"type").toString(),true,true); c.getSource().sendSuccess(()->net.minecraft.network.chat.Component.literal(reports.getOrDefault(target.getUUID(),"Ambush failed: no safe spawn was found.")),true); return ok?1:0; })
                .then(Commands.argument("player", EntityArgument.player()).executes(c->{ ServerPlayer target=EntityArgument.getPlayer(c,"player"); boolean ok=trigger(target,ResourceLocationArgument.getId(c,"type").toString(),true,true); c.getSource().sendSuccess(()->net.minecraft.network.chat.Component.literal(reports.getOrDefault(target.getUUID(),"Ambush failed: no safe spawn was found for "+target.getGameProfile().getName()+".")),true); return ok?1:0; }))));
    }
    @SubscribeEvent void start(ServerAboutToStartEvent e){ SableCompat.sanitizeBeforePhysics(e.getServer()); }
    @SubscribeEvent void stopping(ServerStoppingEvent e){ SableCompat.releaseLoadingTicketsForShutdown(e.getServer()); }
    boolean trigger(ServerPlayer p,String id){return trigger(p,id,false,false);}
    boolean trigger(ServerPlayer p,String id,boolean force){return trigger(p,id,force,force);}
    boolean trigger(ServerPlayer p,String id,boolean force,boolean forceActions){ AmbushDefinition d=registry.find(id); if(d==null){reports.put(p.getUUID(),"Ambush failed: unknown definition "+id+".");if(debug)Ambush.LOGGER.info("Unknown ambush {} for {}",id,p.getGameProfile().getName());return false;} if(!force&&!eligible(p,d)){reports.put(p.getUUID(),"Ambush failed: conditions are not satisfied.");if(debug)Ambush.LOGGER.info("Ambush {} rejected for {}: conditions",id,p.getGameProfile().getName());return false;} state(p).cooldowns(p.getUUID()).remove(d.cooldownGroup()); boolean ok=spawn(p,d,forceActions); if(debug){Ambush.LOGGER.info("Ambush command id={} target={} force={} result={}",id,p.getGameProfile().getName(),force,ok);debugOwnedEntities(p);} return ok; }
    private String stateText(ServerPlayer p){ int owned=0; for(Entity e:p.serverLevel().getEntities().getAll()) if(e.getTags().contains("ambush_owned"))owned++; return "Ambush definitions="+registry.all().size()+", loaded owned entities="+owned+", tracked player cooldowns="+state(p).cooldowns(p.getUUID()).size(); }
    /**
     * Keep the assembly queue at NeoForge's normal post-level-tick priority, the
     * same lifecycle used by Discovery. LOWEST deferred assembly until after the
     * normal Sable tracking pass, allowing physics movement to reach clients
     * before the initial plot-start packet.
     */
    @SubscribeEvent
    void levelTick(LevelTickEvent.Post e){ if(e.getLevel() instanceof ServerLevel level) SableCompat.tick(level); }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    void preventAmbushFriendlyFire(LivingIncomingDamageEvent e){
        Entity attacker=e.getSource().getEntity();
        if(attacker==null&&e.getSource().getDirectEntity() instanceof net.minecraft.world.entity.projectile.Projectile projectile)
            attacker=projectile.getOwner();
        if(attacker==null||!e.getEntity().getTags().contains("ambush_no_friendly_fire"))return;
        String owner=ambushOwnerTag(attacker);
        if(owner!=null&&e.getEntity().getTags().contains(owner))e.setCanceled(true);
    }
    private static String ambushOwnerTag(Entity entity){
        for(String tag:entity.getTags())if(tag.startsWith("ambush_owner_"))return tag;
        if(entity instanceof net.minecraft.world.entity.projectile.Projectile projectile&&projectile.getOwner()!=null)
            for(String tag:projectile.getOwner().getTags())if(tag.startsWith("ambush_owner_"))return tag;
        return null;
    }
    @SubscribeEvent void tick(ServerTickEvent.Post e){ long now=e.getServer().overworld().getGameTime(); runScheduledRains(now); runPersistedScheduledActions(e.getServer(),now); List<ServerPlayer> players=e.getServer().getPlayerList().getPlayers(); retargetAirshipCrew(players); if(now%20!=0)return; for(ServerPlayer p:players) for(AmbushDefinition d:registry.all()) check(now,p,d); retargetWallAggro(players); followAirTargets(players); if(now%100==0)cleanup(players); }
    @SubscribeEvent void death(LivingDeathEvent e){ if(!(e.getSource().getEntity() instanceof ServerPlayer p))return; for(AmbushDefinition d:registry.all()) if(d.trigger().equals("kill")){String wanted=d.raw().has("kill_entity")?d.raw().get("kill_entity").getAsString():""; if(wanted.isBlank()||wanted.equals(e.getEntity().getType().builtInRegistryHolder().key().location().toString())){int n=state(p).counters(p.getUUID()).merge(d.id(),1,Integer::sum); if(n>=(d.raw().has("kill_count")?d.raw().get("kill_count").getAsInt():1)){state(p).counters(p.getUUID()).put(d.id(),0); trigger(p,d.id()); state(p).saveState();}}}}
    private void retargetWallAggro(List<ServerPlayer> players){ for(ServerPlayer p:players){String owner="ambush_owner_"+p.getUUID().toString().replace("-","");for(Entity e:p.serverLevel().getEntities().getAll())if(e instanceof Mob m&&e.getTags().contains(owner)){if(e.getTags().contains("ambush_aggro_walls")&&e.distanceToSqr(p)<512*512)m.setTarget(p);for(String tag:e.getTags())if(tag.startsWith("ambush_aggro_range_"))try{double range=Double.parseDouble(tag.substring(19));if(e.distanceToSqr(p)<=range*range&&(e.getTags().contains("ambush_aggro_walls")||m.hasLineOfSight(p)))m.setTarget(p);}catch(NumberFormatException ignored){}}}}
    private void retargetAirshipCrew(List<ServerPlayer> players){ for(ServerPlayer p:players){String owner="ambush_owner_"+p.getUUID().toString().replace("-","");for(Entity e:SableCompat.airshipCrew(p))if(e instanceof Mob m&&e.getTags().contains(owner)){double range=64;for(String tag:e.getTags())if(tag.startsWith("ambush_aggro_range_"))try{range=Double.parseDouble(tag.substring(19));}catch(NumberFormatException ignored){}Vec3 worldEye=SableCompat.worldEyePosition(e);if(worldEye.distanceToSqr(p.getEyePosition())<=range*range)m.setTarget(p);}}}
    private void followAirTargets(List<ServerPlayer> players){ for(ServerPlayer p:players) for(Entity e:p.serverLevel().getEntities().getAll()) if(e.getTags().contains("ambush_follow_target")&&e.getTags().contains("ambush_owner_"+p.getUUID().toString().replace("-",""))){ double limit=512; for(String tag:e.getTags())if(tag.startsWith("ambush_follow_distance_"))try{limit=Double.parseDouble(tag.substring(23));}catch(NumberFormatException ignored){} if(e.distanceToSqr(p)<limit*limit){ Vec3 delta=p.getEyePosition().subtract(e.position()); if(delta.lengthSqr()>1){ Vec3 v=delta.normalize().scale(0.16); e.setDeltaMovement(e.getDeltaMovement().scale(0.72).add(v)); e.hasImpulse=true; } if(e instanceof Mob m)m.setTarget(p); } } }
    private void cleanup(List<ServerPlayer> players){ Set<String> owners=new HashSet<>(); for(ServerPlayer p:players)owners.add("ambush_owner_"+p.getUUID().toString().replace("-","")); for(ServerPlayer p:players) for(Entity e:p.serverLevel().getEntities().getAll()) if(e.getTags().contains("ambush_owned")&&!e.getTags().stream().anyMatch(owners::contains)&&e.tickCount>24000)e.discard(); }
    private AmbushState state(ServerPlayer p){return p.server.overworld().getDataStorage().computeIfAbsent(new net.minecraft.world.level.saveddata.SavedData.Factory<AmbushState>(AmbushState::new,AmbushState::load,null),"ambush_state");}
    private void check(long tick,ServerPlayer p,AmbushDefinition d){
        if(d.trigger().equals("kill"))return; if(!eligible(p,d))return; AmbushState state=state(p); var map=state.cooldowns(p.getUUID()); long ready=map.getOrDefault(d.cooldownGroup(),0L); if(tick<ready)return; if(d.interval()>0&&tick%d.interval()!=0)return; var failures=state.chanceFailures(p.getUUID()); int failed=failures.getOrDefault(d.id(),0); double current=d.chancePolicy().current(failed); if(Math.random()*100>=current){if(d.chancePolicy().buildsUp()){failures.put(d.id(),failed+1);state.saveState();}return;} if(spawn(p,d,false)){map.put(d.cooldownGroup(),tick+(long)d.cooldown()*20);if(d.chancePolicy().resetOnSuccess())failures.remove(d.id());state.saveState();}
    }
    private boolean eligible(ServerPlayer p,AmbushDefinition d){ if(!Set.of("interval","portal","block_active","structure").contains(d.trigger()))return false; int y=p.blockPosition().getY(),time=(int)(p.level().getDayTime()%24000); if(y<d.minY()||y>d.maxY()||time<d.minTime()||time>d.maxTime())return false; if(d.trigger().equals("portal")&&!nearPortal(p))return false; if(d.trigger().equals("block_active")&&!activeBlockNearby(p,d.activeBlocks()))return false; if(d.trigger().equals("structure")&&!StructureConditions.matches(p,d.raw()))return false; String biome=p.level().getBiome(p.blockPosition()).unwrapKey().map(k->k.location().toString()).orElse(""); if(!d.biomes().isEmpty()&&!d.biomes().stream().anyMatch(x->x.startsWith("#")?p.level().getBiome(p.blockPosition()).is(TagKey.create(Registries.BIOME,ResourceLocation.parse(x.substring(1)))):x.equals(biome)))return false; if(!d.dimensions().isEmpty()&&!d.dimensions().contains(p.level().dimension().location().toString()))return false; return true; }
    private boolean nearPortal(ServerPlayer p){ BlockPos c=p.blockPosition(); for(BlockPos q:BlockPos.betweenClosed(c.offset(-3,-3,-3),c.offset(3,3,3))) if(p.level().getBlockState(q).is(Blocks.NETHER_PORTAL))return true; return false; }
    private boolean hasStructureAction(AmbushDefinition d){ if(!d.raw().has("actions")||!d.raw().get("actions").isJsonArray())return false; for(JsonElement e:d.raw().getAsJsonArray("actions")) if(e.isJsonObject()){String t=e.getAsJsonObject().has("type")?e.getAsJsonObject().get("type").getAsString():""; if(t.equals("sable_structure")||t.equals("sable_formation")||t.equals("sable_sublevel")||t.equals("sable_sublevel_direct"))return true;} return false; }
    private boolean activeBlockNearby(ServerPlayer p,List<String> ids){ BlockPos c=p.blockPosition(); for(BlockPos q:BlockPos.betweenClosed(c.offset(-4,-4,-4),c.offset(4,4,4))){ String id=p.level().getBlockState(q).getBlock().builtInRegistryHolder().key().location().toString(); if(ids.contains(id))return true; if(p.level().getBlockEntity(q)!=null){ var tag=p.level().getBlockEntity(q).saveWithoutMetadata(p.level().registryAccess()); if(tag.contains("progress")&&tag.getDouble("progress")>0)return true; }} return false; }
    private boolean spawn(ServerPlayer p,AmbushDefinition d,boolean forceActions){
        ServerLevel level=p.serverLevel(); int made=0,requested=0; Map<String,Integer> counts=new LinkedHashMap<>();
        double minX=p.getX(),minY=p.getY(),minZ=p.getZ(),maxX=p.getX(),maxY=p.getY(),maxZ=p.getZ();
        for(SpawnSpec spec:d.spawns()){
            requested+=Math.min(128,spec.count());
            for(int n=0;n<spec.count()&&n<128;n++)for(int attempt=0;attempt<d.attempts();attempt++){
                double angle=Math.random()*Math.PI*2,distance=8+Math.random()*d.radius();
                BlockPos pos=BlockPos.containing(p.getX()+Math.cos(angle)*distance,p.getY()+Math.random()*7-3,p.getZ()+Math.sin(angle)*distance);
                String placement=spec.placement();
                if(placement.equals("water")){if(!level.getFluidState(pos).is(net.minecraft.tags.FluidTags.WATER))continue;}
                else if(placement.equals("air")){if(!level.isEmptyBlock(pos)||!level.getFluidState(pos).isEmpty())continue;}
                else if(!level.isEmptyBlock(pos)||!level.isEmptyBlock(pos.above())||!level.getFluidState(pos).isEmpty())continue;
                Vec3 eye=p.getEyePosition(),target=Vec3.atCenterOf(pos);
                if(spec.avoidLineOfSight()&&level.clip(new net.minecraft.world.level.ClipContext(eye,target,net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,p)).getType()==net.minecraft.world.phys.HitResult.Type.MISS)continue;
                if(spawnOne(level,p,pos,spec)){made++;counts.merge(spec.entity(),1,Integer::sum);minX=Math.min(minX,pos.getX());minY=Math.min(minY,pos.getY());minZ=Math.min(minZ,pos.getZ());maxX=Math.max(maxX,pos.getX());maxY=Math.max(maxY,pos.getY());maxZ=Math.max(maxZ,pos.getZ());break;}
            }
        }
        boolean complete=requested==0||made==requested;
        boolean mayRunActions=complete||forceActions||d.raw().has("allow_partial_actions")&&d.raw().get("allow_partial_actions").getAsBoolean();
        int sableQueued=mayRunActions?SableCompat.apply(p,d.raw(),forceActions):0;
        Vec3 structureOrigin=SableCompat.lastStructureOrigin(p);
        int acceptedActions=0;
        if(mayRunActions){feedback(p,d);acceptedActions=actions(p,d,structureOrigin);}
        boolean ok=made>0||sableQueued>0||acceptedActions>0;
        if(ok)reports.put(p.getUUID(),"Ambush succeeded: "+counts+" spawned; Sable structures queued="+sableQueued+"; actions accepted="+acceptedActions+" for "+p.getGameProfile().getName()+" in "+p.serverLevel().dimension().location()+"; bounds "+(int)minX+","+(int)minY+","+(int)minZ+" to "+(int)maxX+","+(int)maxY+","+(int)maxZ+". Cooldown overridden for this command.");
        else reports.put(p.getUUID(),"Ambush failed: no safe spawn positions or valid actions were found.");
        return ok;
    }
    private int actions(ServerPlayer p,AmbushDefinition d,Vec3 structureOrigin){
        if(!d.raw().has("actions")||!d.raw().get("actions").isJsonArray())return 0;
        int accepted=0;
        for(JsonElement element:d.raw().getAsJsonArray("actions")){
            if(!element.isJsonObject())continue;
            JsonObject a=element.getAsJsonObject();
            String type=a.has("type")?a.get("type").getAsString():"";
            Vec3 selectedOrigin=SableCompat.structureOrigin(p,a.has("source_structure")?a.get("source_structure").getAsString():"");
            if(!ActionConditions.matches(p,a))continue;
            if(!runtimeActionType(type))continue;
            accepted++;
            if(type.equals("directional_cbc_shell_rain")){scheduleDirectionalShells(p,a);continue;}
            if(type.equals("directional_entity_wave")){scheduleDirectionalWaves(p,a);continue;}
            if(type.equals("directional_arrow_rain")||type.equals("directional_entity_rain")||type.equals("directional_potion_rain")||type.equals("conditional_spawn")||type.equals("sound")||type.equals("fog")){
                scheduleDataAction(p,a,selectedOrigin,a.has("after_ticks")?a.get("after_ticks").getAsLong():0);continue;
            }
            if(type.equals("entity_wave")){scheduleEntityWaves(p,a);continue;}
            int count=Math.min(128,a.has("count")?a.get("count").getAsInt():1);
            if(type.equals("structure")||type.equals("sable_substructure")){
                String id=a.has("template")?a.get("template").getAsString():"";
                int ox=a.has("offset_x")?a.get("offset_x").getAsInt():0,oy=a.has("offset_y")?a.get("offset_y").getAsInt():0,oz=a.has("offset_z")?a.get("offset_z").getAsInt():0;
                if(!id.isBlank())p.server.getCommands().performPrefixedCommand(p.createCommandSourceStack(),"place template "+id+" ~"+ox+" ~"+oy+" ~"+oz);
                if(type.equals("sable_substructure"))p.server.getCommands().performPrefixedCommand(p.createCommandSourceStack(),"summon minecraft:pillager ~"+ox+" ~"+(oy+1)+" ~"+oz+" {Tags:[\"ambush_owned\",\"ambush_sable_guard\"]}");
                continue;
            }
            if(type.equals("block_platform")){
                String block=a.has("block")?a.get("block").getAsString():"minecraft:iron_block";
                int width=a.has("width")?a.get("width").getAsInt():5,depth=a.has("depth")?a.get("depth").getAsInt():5,ox=a.has("offset_x")?a.get("offset_x").getAsInt():0,oy=a.has("offset_y")?a.get("offset_y").getAsInt():-1,oz=a.has("offset_z")?a.get("offset_z").getAsInt():0;
                p.server.getCommands().performPrefixedCommand(p.createCommandSourceStack(),"fill ~"+(ox-width/2)+" ~"+oy+" ~"+(oz-depth/2)+" ~"+(ox+width/2)+" ~"+oy+" ~"+(oz+depth/2)+" "+block);
                p.server.getCommands().performPrefixedCommand(p.createCommandSourceStack(),"summon minecraft:pillager ~"+ox+" ~"+(oy+1)+" ~"+oz+" {Tags:[\"ambush_owned\",\"ambush_block_guard\"]}");continue;
            }
            if(type.equals("potion_rain")||type.equals("arrow_rain")||type.equals("entity_rain")){
                JsonObject projectile=a.deepCopy();
                if(type.equals("potion_rain"))projectile.addProperty("kind","potion");
                if(type.equals("arrow_rain"))projectile.addProperty("kind","arrow");
                ProjectileCompat.spawnVertical(p,projectile,count);continue;
            }
            if(type.equals("cbc_shell_rain")||type.equals("shell_rain")){
                JsonArray bursts=a.has("bursts")&&a.get("bursts").isJsonArray()?a.getAsJsonArray("bursts"):null;
                if(bursts!=null)for(JsonElement b:bursts)if(b.isJsonObject()){JsonObject copy=a.deepCopy();copy.remove("bursts");copy.addProperty("count",b.getAsJsonObject().has("count")?b.getAsJsonObject().get("count").getAsInt():1);scheduledRains.add(new ScheduledRain(effectiveTick(p,b.getAsJsonObject().has("after_ticks")?b.getAsJsonObject().get("after_ticks").getAsLong():0),p,copy));}
                else rainCbc(p,a,count);
            }
        }
        return accepted;
    }
    private boolean runtimeActionType(String type){return Set.of("directional_cbc_shell_rain","directional_entity_wave","directional_arrow_rain","directional_entity_rain","directional_potion_rain","conditional_spawn","sound","fog","entity_wave","structure","sable_substructure","block_platform","potion_rain","arrow_rain","entity_rain","cbc_shell_rain","shell_rain").contains(type);}
    private void scheduleDirectionalWaves(ServerPlayer player,JsonObject action){
        if(!action.has("waves")||!action.get("waves").isJsonArray())return;
        for(JsonElement rawWave:action.getAsJsonArray("waves")){
            if(!rawWave.isJsonObject())continue;
            JsonObject wave=merged(action,rawWave.getAsJsonObject());
            wave.remove("waves");
            long waveDelay=wave.has("after_ticks")?wave.get("after_ticks").getAsLong():0;
            if(wave.has("entries")&&wave.get("entries").isJsonArray()){
                for(JsonElement rawEntry:wave.getAsJsonArray("entries"))if(rawEntry.isJsonObject()){long entryDelay=rawEntry.getAsJsonObject().has("after_ticks")?rawEntry.getAsJsonObject().get("after_ticks").getAsLong():0;scheduleDirectionalEntry(player,merged(wave,rawEntry.getAsJsonObject()),waveDelay+entryDelay);}
            }else scheduleDirectionalEntry(player,wave,waveDelay);
        }
    }
    private void scheduleDirectionalEntry(ServerPlayer player,JsonObject entry,long delay){
        entry.remove("entries");
        String kind=entry.has("kind")?entry.get("kind").getAsString():entry.has("arrow")||entry.has("arrow_item")?"arrow":entry.has("potion")?"potion":"entity";
        entry.addProperty("kind",kind);
        entry.addProperty("type",switch(kind){case "arrow"->"directional_arrow_rain";case "potion"->"directional_potion_rain";default->"directional_entity_rain";});
        List<String> keys=new ArrayList<>();
        if(entry.has("source_structures")&&entry.get("source_structures").isJsonArray())for(JsonElement key:entry.getAsJsonArray("source_structures"))keys.add(key.getAsString());
        else keys.add(entry.has("source_structure")?entry.get("source_structure").getAsString():"");
        long sourceDelay=Math.max(0,Math.min(72000,entry.has("source_delay_ticks")?entry.get("source_delay_ticks").getAsLong():0));
        for(int i=0;i<keys.size();i++){
            JsonObject copy=entry.deepCopy();copy.remove("source_structures");copy.addProperty("source_structure",keys.get(i));
            scheduleDataAction(player,copy,SableCompat.structureOrigin(player,keys.get(i)),delay+i*sourceDelay);
        }
    }
    private JsonObject merged(JsonObject parent,JsonObject child){JsonObject out=parent.deepCopy();for(var value:child.entrySet())out.add(value.getKey(),value.getValue().deepCopy());return out;}
    private void scheduleDirectionalShells(ServerPlayer player,JsonObject action){List<String> keys=new ArrayList<>();if(action.has("source_structures")&&action.get("source_structures").isJsonArray())for(JsonElement key:action.getAsJsonArray("source_structures"))keys.add(key.getAsString());else keys.add(action.has("source_structure")?action.get("source_structure").getAsString():"");JsonArray bursts=action.has("bursts")&&action.get("bursts").isJsonArray()?action.getAsJsonArray("bursts"):null;long sourceDelay=Math.max(0,Math.min(72000,action.has("source_delay_ticks")?action.get("source_delay_ticks").getAsLong():0));for(int index=0;index<keys.size();index++){String key=keys.get(index);Vec3 origin=SableCompat.structureOrigin(player,key);long stagger=index*sourceDelay;if(bursts!=null){for(JsonElement rawBurst:bursts)if(rawBurst.isJsonObject()){JsonObject copy=action.deepCopy();copy.remove("bursts");copy.remove("source_structures");copy.addProperty("source_structure",key);JsonObject burst=rawBurst.getAsJsonObject();copy.addProperty("count",burst.has("count")?burst.get("count").getAsInt():1);scheduleDataAction(player,copy,origin,stagger+(burst.has("after_ticks")?burst.get("after_ticks").getAsLong():0));}}else scheduleDataAction(player,action,origin,stagger+(action.has("after_ticks")?action.get("after_ticks").getAsLong():0));}}
    private void scheduleDataAction(ServerPlayer player,JsonObject action,Vec3 origin,long delay){ AmbushScheduleState.Entry entry=new AmbushScheduleState.Entry();entry.id=UUID.randomUUID();entry.ownerId=player.getUUID();entry.dimension=player.serverLevel().dimension().location().toString();entry.dueGameTime=player.server.overworld().getGameTime()+Math.max(0,delay);entry.actionJson=action.toString();entry.origin=origin;AmbushScheduleState.get(player.server).add(entry); }
    private void runPersistedScheduledActions(net.minecraft.server.MinecraftServer server,long now){
        AmbushScheduleState state=AmbushScheduleState.get(server);int executed=0;
        for(AmbushScheduleState.Entry entry:new ArrayList<>(state.entries())){
            if(executed>=64||now<entry.dueGameTime)continue;
            ServerPlayer player=server.getPlayerList().getPlayer(entry.ownerId);
            if(player==null||!player.serverLevel().dimension().location().toString().equals(entry.dimension))continue;
            boolean deferred=false;
            try{
                JsonObject action=JsonParser.parseString(entry.actionJson).getAsJsonObject();String type=action.has("type")?action.get("type").getAsString():"";
                if(DirectionalRainCompat.waitingForStartDistance(player,action,entry.origin)){long poll=Math.max(1,Math.min(1200,action.has("start_distance_poll_ticks")?action.get("start_distance_poll_ticks").getAsLong():20));state.reschedule(entry,now+poll);deferred=true;continue;}
                int made=type.equals("conditional_spawn")?spawnConditionalGroup(player,action):type.equals("sound")?playActionSound(player,action,entry.origin):type.equals("sable_event_action")?SableCompat.executeScheduledEventAction(player,action):DirectionalRainCompat.execute(player,action,entry.origin);
                executed++;
                if(made==0)Ambush.LOGGER.warn("Persisted ambush action produced no result: id={} type={} owner={}",entry.id,type,entry.ownerId);else Ambush.LOGGER.info("Executed persisted ambush action: id={} type={} results={} owner={}",entry.id,type,made,entry.ownerId);
            }catch(Exception ex){Ambush.LOGGER.warn("Persisted ambush action failed: id={} owner={}",entry.id,entry.ownerId,ex);}
            finally{if(!deferred)state.remove(entry.id);}
        }
    }
    private int playActionSound(ServerPlayer player,JsonObject action,Vec3 structureOrigin){if(!ActionConditions.matches(player,action)||!action.has("sound"))return 0;ResourceLocation id=ResourceLocation.parse(action.get("sound").getAsString());if(!net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.containsKey(id))return 0;Vec3 pos="structure".equals(action.has("at")?action.get("at").getAsString():"player")&&structureOrigin!=null?structureOrigin:player.position();float volume=action.has("volume")?action.get("volume").getAsFloat():1f,pitch=action.has("pitch")?action.get("pitch").getAsFloat():1f;player.serverLevel().playSound(null,pos.x,pos.y,pos.z,net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.get(id),net.minecraft.sounds.SoundSource.HOSTILE,volume,pitch);return 1;}
    private int spawnConditionalGroup(ServerPlayer player,JsonObject action){if(!ActionConditions.matches(player,action)||!action.has("spawns")||!action.get("spawns").isJsonArray())return 0;double radius=Math.max(4,Math.min(128,action.has("radius")?action.get("radius").getAsDouble():24));double minRadius=Math.max(0,Math.min(radius,action.has("min_radius")?action.get("min_radius").getAsDouble():8));boolean front="front".equals(action.has("direction")?action.get("direction").getAsString():"");double arc=Math.toRadians(Math.max(0,Math.min(360,action.has("arc_degrees")?action.get("arc_degrees").getAsDouble():60)));int attempts=Math.max(1,Math.min(128,action.has("attempts")?action.get("attempts").getAsInt():32));int made=0;for(JsonElement raw:action.getAsJsonArray("spawns")){if(!raw.isJsonObject())continue;SpawnSpec spec=SpawnSpec.read(raw.getAsJsonObject());for(int n=0;n<spec.count()&&made<128;n++)for(int attempt=0;attempt<attempts;attempt++){double angle=front?Math.toRadians(player.getYRot()+90)+(Math.random()-.5)*arc:Math.random()*Math.PI*2,dist=minRadius+Math.random()*(radius-minRadius);int x=(int)Math.floor(player.getX()+Math.cos(angle)*dist),z=(int)Math.floor(player.getZ()+Math.sin(angle)*dist);BlockPos pos=conditionalSpawnPosition(player,spec,x,z);if(pos==null)continue;Vec3 eye=player.getEyePosition(),target=Vec3.atCenterOf(pos);if(spec.avoidLineOfSight()&&player.serverLevel().clip(new net.minecraft.world.level.ClipContext(eye,target,net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,player)).getType()==net.minecraft.world.phys.HitResult.Type.MISS)continue;if(spawnOne(player.serverLevel(),player,pos,spec)){made++;break;}}}return made;}
    private BlockPos conditionalSpawnPosition(ServerPlayer player,SpawnSpec spec,int x,int z){ServerLevel level=player.serverLevel();if("water".equals(spec.placement())){int top=Math.min(level.getMaxBuildHeight()-1,Math.max(player.blockPosition().getY()+8,level.getSeaLevel()+8));for(int y=top;y>=level.getMinBuildHeight();y--){BlockPos pos=new BlockPos(x,y,z);if(level.getFluidState(pos).is(net.minecraft.tags.FluidTags.WATER))return pos;}return null;}if("air".equals(spec.placement())){for(int i=0;i<16;i++){BlockPos pos=new BlockPos(x,player.blockPosition().getY()+8+i,z);if(level.isEmptyBlock(pos)&&level.getFluidState(pos).isEmpty())return pos;}return null;}int y=level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z);BlockPos pos=new BlockPos(x,y,z);return level.isEmptyBlock(pos)&&level.isEmptyBlock(pos.above())?pos:null;}
    private void scheduleEntityWaves(ServerPlayer p, JsonObject a){ if(!a.has("waves")||!a.get("waves").isJsonArray())return; for(JsonElement w:a.getAsJsonArray("waves"))if(w.isJsonObject()){JsonObject wave=w.getAsJsonObject().deepCopy(); if(!wave.has("entity"))continue; wave.addProperty("wave_entity",wave.get("entity").getAsString()); wave.remove("entity"); scheduledRains.add(new ScheduledRain(effectiveTick(p,wave.has("after_ticks")?wave.get("after_ticks").getAsLong():0),p,wave));} }
    private void scheduleFromDefinition(ServerPlayer p,AmbushDefinition d){ if(!d.raw().has("actions"))return; for(JsonElement e:d.raw().getAsJsonArray("actions"))if(e.isJsonObject()&&"entity_wave".equals(e.getAsJsonObject().get("type").getAsString()))scheduleEntityWaves(p,e.getAsJsonObject()); }
    private long effectiveTick(ServerPlayer p,long delay){return p.server.overworld().getGameTime()+Math.max(0,delay);}
    private void runScheduledRains(long now){ Iterator<ScheduledRain> it=scheduledRains.iterator(); while(it.hasNext()){ScheduledRain r=it.next(); if(now<r.tick())continue; if(r.player().isRemoved()){it.remove();continue;} if(r.action().has("wave_entity"))spawnWave(r.player(),r.action()); else rainCbc(r.player(),r.action(),Math.min(128,r.action().has("count")?r.action().get("count").getAsInt():1)); it.remove(); } }
    private void spawnWave(ServerPlayer p,JsonObject a){ String id=a.get("wave_entity").getAsString(); if(!net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.containsKey(ResourceLocation.parse(id)))return; EntityType<?> type=net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(id)); int count=Math.min(128,a.has("count")?a.get("count").getAsInt():1); String owner="ambush_owner_"+p.getUUID().toString().replace("-",""); for(int i=0;i<count;i++){double ang=Math.random()*Math.PI*2,dist=8+Math.random()*12; Entity e=type.create(p.serverLevel()); if(e==null)continue; BlockPos pos=BlockPos.containing(p.getX()+Math.cos(ang)*dist,p.getY()+1,p.getZ()+Math.sin(ang)*dist); e.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,p.getYRot(),0); e.addTag("ambush_owned"); e.addTag(owner); e.addTag("ambush_wave"); if(a.has("aggro_through_walls")&&a.get("aggro_through_walls").getAsBoolean())e.addTag("ambush_aggro_walls"); if(a.has("follow_target")&&a.get("follow_target").getAsBoolean())e.addTag("ambush_follow_target"); p.serverLevel().addFreshEntity(e); if(e instanceof Mob m)m.setTarget(p); applyMobEffects(p.serverLevel(),e,a.has("effects")?stringsArray(a.getAsJsonArray("effects")):List.of()); } }
    private List<String> stringsArray(JsonArray a){List<String> out=new ArrayList<>();for(JsonElement e:a)if(e.isJsonPrimitive())out.add(e.getAsString());return out;}
    private void rainCbc(ServerPlayer p,JsonObject a,int count){String block=a.has("block")?a.get("block").getAsString():""; String item=a.has("item")?a.get("item").getAsString():""; double spread=a.has("spread")?a.get("spread").getAsDouble():16,height=a.has("height")?a.get("height").getAsDouble():24; for(int i=0;i<count;i++)CbcCompat.spawn(p.serverLevel(),p.getX()+Math.random()*spread-spread/2,p.getY()+height,p.getZ()+Math.random()*spread-spread/2,block,item,a);}
    private void feedback(ServerPlayer p,AmbushDefinition d){ for(String effect:d.effects()){String[] x=effect.split(":"); if(x.length>=3){var id=ResourceLocation.parse(x[0]+":"+x[1]); var holder=net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.getHolder(id); if(holder.isPresent())p.addEffect(new net.minecraft.world.effect.MobEffectInstance(holder.get(),Integer.parseInt(x[2])*20,x.length>3?Integer.parseInt(x[3]):0));}} for(String sound:d.sounds()){var id=ResourceLocation.parse(sound); if(net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.containsKey(id))p.playSound(net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.get(id),1f,1f);} }
    private boolean spawnOne(ServerLevel l,ServerPlayer p,BlockPos pos,SpawnSpec s){ var id=ResourceLocation.parse(s.entity()); if(!net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.containsKey(id))return false; Entity e=net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.get(id).create(l); if(e==null)return false; e.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,p.getYRot(),0); s.tags().forEach(e::addTag); e.addTag("ambush_owned"); e.addTag("ambush_owner_"+p.getUUID().toString().replace("-","")); if(!s.friendlyFire())e.addTag("ambush_no_friendly_fire"); if(s.aggroRange()>0)e.addTag("ambush_aggro_range_"+Math.max(4,Math.min(512,(int)Math.round(s.aggroRange())))); if(s.followDistance()>0){e.addTag("ambush_follow_target");e.addTag("ambush_follow_distance_"+s.followDistance());} if(s.aggroThroughWalls())e.addTag("ambush_aggro_walls"); if(e instanceof Mob m)prepareMob(l,p,pos,m,s); l.addFreshEntity(e); applyMobEffects(l,e,s.effects()); spawnPassengers(l,p,pos,e,s.passengers()); return true; }
    private void spawnPassengers(ServerLevel level,ServerPlayer owner,BlockPos pos,Entity vehicle,List<SpawnSpec> specs){for(SpawnSpec spec:specs)for(int n=0;n<Math.max(1,Math.min(16,spec.count()));n++){ResourceLocation id=ResourceLocation.parse(spec.entity());if(!net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.containsKey(id))continue;Entity rider=net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.get(id).create(level);if(rider==null)continue;rider.moveTo(vehicle.position());spec.tags().forEach(rider::addTag);rider.addTag("ambush_owned");rider.addTag("ambush_owner_"+owner.getUUID().toString().replace("-",""));if(!spec.friendlyFire())rider.addTag("ambush_no_friendly_fire");if(spec.aggroRange()>0)rider.addTag("ambush_aggro_range_"+Math.max(4,Math.min(512,(int)Math.round(spec.aggroRange()))));if(spec.followDistance()>0){rider.addTag("ambush_follow_target");rider.addTag("ambush_follow_distance_"+spec.followDistance());}if(spec.aggroThroughWalls())rider.addTag("ambush_aggro_walls");if(rider instanceof Mob mob)prepareMob(level,owner,pos,mob,spec);level.addFreshEntity(rider);rider.startRiding(vehicle,true);applyMobEffects(level,rider,spec.effects());spawnPassengers(level,owner,pos,rider,spec.passengers());}}
    private void prepareMob(ServerLevel level,ServerPlayer owner,BlockPos pos,Mob mob,SpawnSpec spec){mob.finalizeSpawn(level,level.getCurrentDifficultyAt(pos),net.minecraft.world.entity.MobSpawnType.EVENT,null);equip(mob,spec);double targetRange=Math.max(4,Math.min(512,spec.targetRange()));var followRange=mob.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE);if(followRange!=null)followRange.setBaseValue(targetRange);mob.getPersistentData().putDouble("ambush_target_range",targetRange);if(mob instanceof net.minecraft.world.entity.monster.Pillager pillager&&pillager.getMainHandItem().is(net.minecraft.world.item.Items.CROSSBOW))pillager.goalSelector.addGoal(1,new net.minecraft.world.entity.ai.goal.RangedCrossbowAttackGoal<>(pillager,1.0,Math.max(4f,Math.min(48f,(float)spec.crossbowRange()))));if(!spec.target().equals("none")&&mob.distanceToSqr(owner)<=targetRange*targetRange)mob.setTarget(owner);if(spec.persistent())mob.setPersistenceRequired();}
    private void equip(Mob mob,SpawnSpec spec){
        if(!spec.mainHand().isBlank()) mob.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,item(spec.mainHand()));
        else if(mob instanceof net.minecraft.world.entity.monster.Vindicator) mob.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_AXE));
        if(!spec.offHand().isBlank()) mob.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND,item(spec.offHand()));
    }
    private net.minecraft.world.item.ItemStack item(String id){ResourceLocation key=ResourceLocation.parse(id);return net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(key)?new net.minecraft.world.item.ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(key)):net.minecraft.world.item.ItemStack.EMPTY;}
    private void applyMobEffects(ServerLevel l,Entity e,List<String> effects){ for(String effect:effects){String[] x=effect.split(":"); if(x.length>=3&&e instanceof LivingEntity living){var id=ResourceLocation.parse(x[0]+":"+x[1]); var holder=net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.getHolder(id); if(holder.isPresent())living.addEffect(new net.minecraft.world.effect.MobEffectInstance(holder.get(),Integer.parseInt(x[2])*20,x.length>3?Integer.parseInt(x[3]):0));}} }
    private void debugOwnedEntities(ServerPlayer p){ if(!debug)return; for(Entity e:p.serverLevel().getEntities().getAll()) if(e.getTags().contains("ambush_owned")) Ambush.LOGGER.info("/data get entity @e[tag=ambush_owned,limit=1,sort=nearest] => type={}, uuid={}, pos={}, tags={}",e.getType().builtInRegistryHolder().key().location(),e.getUUID(),e.position(),e.getTags()); }
}
