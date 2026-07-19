package com.createcomplex.ambush;

import com.google.gson.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.*;

/** Loaded-position structure matching. This never performs a locate or generates chunks. */
final class StructureConditions {
    private StructureConditions() {}

    static boolean matches(ServerPlayer player, JsonObject definition) {
        JsonObject conditions=definition.has("conditions")&&definition.get("conditions").isJsonObject()?definition.getAsJsonObject("conditions"):definition;
        LinkedHashSet<String> selectors=new LinkedHashSet<>();
        addStrings(conditions.get("structures"),selectors);
        JsonObject groups=conditions.has("structure_groups")&&conditions.get("structure_groups").isJsonObject()?conditions.getAsJsonObject("structure_groups"):null;
        if(groups!=null){
            LinkedHashSet<String> selectedGroups=new LinkedHashSet<>();
            addStrings(conditions.get("structure_group"),selectedGroups);
            addStrings(conditions.get("use_structure_groups"),selectedGroups);
            for(String name:selectedGroups)if(groups.has(name))addStrings(groups.get(name),selectors);
        }
        if(selectors.isEmpty())return false;
        BlockPos pos=player.blockPosition();
        for(String selector:selectors){
            try{
                if(selector.startsWith("#")){
                    TagKey<Structure> tag=TagKey.create(Registries.STRUCTURE,ResourceLocation.parse(selector.substring(1)));
                    if(player.serverLevel().structureManager().getStructureWithPieceAt(pos,tag).isValid())return true;
                }else{
                    var structure=player.serverLevel().registryAccess().registryOrThrow(Registries.STRUCTURE).get(ResourceLocation.parse(selector));
                    if(structure!=null&&player.serverLevel().structureManager().getStructureWithPieceAt(pos,structure).isValid())return true;
                }
            }catch(RuntimeException ex){ Ambush.LOGGER.warn("Invalid structure selector {}",selector); }
        }
        return false;
    }

    private static void addStrings(JsonElement value,Set<String> out){
        if(value==null||value.isJsonNull())return;
        if(value.isJsonPrimitive())out.add(value.getAsString());
        else if(value.isJsonArray())for(JsonElement e:value.getAsJsonArray())if(e.isJsonPrimitive())out.add(e.getAsString());
    }
}
