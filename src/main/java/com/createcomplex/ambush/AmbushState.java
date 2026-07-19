package com.createcomplex.ambush;

import net.minecraft.nbt.*;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.core.HolderLookup;
import java.util.*;

final class AmbushState extends SavedData {
    private final Map<UUID,Map<String,Long>> cooldowns=new HashMap<>();
    private final Map<UUID,Map<String,Integer>> counters=new HashMap<>();
    private final Map<UUID,Map<String,Integer>> chanceFailures=new HashMap<>();
    static AmbushState load(CompoundTag tag, HolderLookup.Provider provider){ AmbushState s=new AmbushState(); ListTag list=tag.getList("players",10); for(Tag raw:list){CompoundTag p=(CompoundTag)raw; UUID id=UUID.fromString(p.getString("uuid")); Map<String,Long> cds=s.cooldowns(id); CompoundTag c=p.getCompound("cooldowns"); for(String k:c.getAllKeys())cds.put(k,c.getLong(k)); Map<String,Integer> counts=s.counters(id); CompoundTag n=p.getCompound("counters"); for(String k:n.getAllKeys())counts.put(k,n.getInt(k)); Map<String,Integer> failures=s.chanceFailures(id); CompoundTag f=p.getCompound("chance_failures"); for(String k:f.getAllKeys())failures.put(k,f.getInt(k));} return s; }
    Map<String,Long> cooldowns(UUID id){return cooldowns.computeIfAbsent(id,x->new HashMap<>());}
    Map<String,Integer> counters(UUID id){return counters.computeIfAbsent(id,x->new HashMap<>());}
    Map<String,Integer> chanceFailures(UUID id){return chanceFailures.computeIfAbsent(id,x->new HashMap<>());}
    void saveState(){setDirty();}
    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider){ ListTag list=new ListTag(); Set<UUID> ids=new HashSet<>(cooldowns.keySet()); ids.addAll(counters.keySet()); ids.addAll(chanceFailures.keySet()); for(UUID id:ids){CompoundTag p=new CompoundTag(); p.putString("uuid",id.toString()); CompoundTag c=new CompoundTag(); cooldowns.getOrDefault(id,Map.of()).forEach(c::putLong); p.put("cooldowns",c); CompoundTag n=new CompoundTag(); counters.getOrDefault(id,Map.of()).forEach(n::putInt); p.put("counters",n); CompoundTag f=new CompoundTag(); chanceFailures.getOrDefault(id,Map.of()).forEach(f::putInt); p.put("chance_failures",f); list.add(p);} tag.put("players",list); return tag;}
}
