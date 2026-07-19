package com.createcomplex.ambush;

import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

final class AmbushRegistry extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new Gson();
    private final Map<String, AmbushDefinition> definitions = new ConcurrentHashMap<>();
    AmbushRegistry(){super(GSON,"ambushes");}
    @Override protected void apply(Map<ResourceLocation,JsonElement> map, ResourceManager manager, net.minecraft.util.profiling.ProfilerFiller profiler){
        Map<String,AmbushDefinition> refreshed=new HashMap<>();
        int rejected=0;
        for(Map.Entry<ResourceLocation,JsonElement> entry:map.entrySet()){
            ResourceLocation id=entry.getKey(); JsonElement json=entry.getValue();
            if(!json.isJsonObject()){rejected++; Ambush.LOGGER.warn("Rejected ambush {}: root must be a JSON object",id); continue;}
            try{refreshed.put(id.toString(),AmbushDefinition.read(id.toString(),json.getAsJsonObject()));}
            catch(RuntimeException ex){rejected++; Ambush.LOGGER.warn("Rejected ambush {} during datapack reload: {}",id,ex.getMessage());}
        }
        definitions.clear(); definitions.putAll(refreshed);
        Ambush.LOGGER.info("Loaded {} Ambush definitions from datapack reload ({} rejected)",refreshed.size(),rejected);
    }
    Collection<AmbushDefinition> all(){return definitions.values();}
    AmbushDefinition find(String id){ return definitions.get(id.contains(":")?id:("ambush:"+id)); }
}
