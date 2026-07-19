package com.createcomplex.ambush;

import com.google.gson.*;
import java.util.*;

record AmbushDefinition(String id, int interval, int cooldown, String cooldownGroup, double chance, ChancePolicy chancePolicy, int minY, int maxY,
                        int minTime, int maxTime, List<String> biomes, List<String> dimensions,
                        List<String> activeBlocks, String trigger, int radius, int attempts,
                        List<SpawnSpec> spawns, List<String> effects, List<String> sounds, JsonObject raw) {
    static AmbushDefinition read(String id, JsonObject o) {
        JsonObject trigger=o.has("trigger")&&o.get("trigger").isJsonObject()?o.getAsJsonObject("trigger"):o;
        JsonObject conditions=o.has("conditions")?o.getAsJsonObject("conditions"):o;
        JsonObject height=conditions.has("height")?conditions.getAsJsonObject("height"):conditions;
        JsonObject time=conditions.has("time")?conditions.getAsJsonObject("time"):conditions;
        JsonObject wave=o.has("wave")?o.getAsJsonObject("wave"):o;
        validateActions(o);
        List<SpawnSpec> spawns = new ArrayList<>();
        JsonArray groups=wave.has("groups")?wave.getAsJsonArray("groups"):wave.getAsJsonArray("spawns");
        if(groups!=null)for (JsonElement e : groups) spawns.add(SpawnSpec.read(e.getAsJsonObject()));
        String triggerName=trigger.has("type")?s(trigger,"type","interval"):s(o,"trigger","interval");
        if(!Set.of("interval","portal","block_active","structure","kill").contains(triggerName))throw new JsonParseException("unknown trigger type: "+triggerName);
        JsonObject chanceObject=trigger.has("chance")&&trigger.get("chance").isJsonObject()?trigger.getAsJsonObject("chance"):null;
        double chance=chanceObject!=null?d(chanceObject,"base",.01)*100:d(o,"chance",1);
        String chanceMode=chanceObject!=null?s(chanceObject,"mode","flat"):"flat";
        if(!Set.of("flat","build_up","buildup","escalating").contains(chanceMode))throw new JsonParseException("unknown chance mode: "+chanceMode);
        double failureIncrease=chanceObject!=null?d(chanceObject,"increase_on_failure",d(chanceObject,"failure_increment",0))*100:0;
        double maximum=chanceObject!=null?d(chanceObject,"max",d(chanceObject,"maximum",chance/100))*100:chance;
        ChancePolicy chancePolicy=new ChancePolicy(chanceMode,Math.max(0,Math.min(100,chance)),Math.max(0,Math.min(100,failureIncrease)),Math.max(0,Math.min(100,maximum)),chanceObject==null||!chanceObject.has("reset_on_success")||chanceObject.get("reset_on_success").getAsBoolean());
        if (bundledCommandOnly(id)) chance=0;
        if (bundledCommandOnly(id)) chancePolicy=new ChancePolicy(chancePolicy.mode(),0,0,0,chancePolicy.resetOnSuccess());
        return new AmbushDefinition(id, trigger.has("check_every_ticks")?i(trigger,"check_every_ticks",20):i(o,"interval",20), trigger.has("cooldown_ticks")?i(trigger,"cooldown_ticks",600)/20:i(o,"cooldown",600), s(o,"cooldown_group","default"), Math.max(0,Math.min(100,chance)), chancePolicy, i(height,"min",i(o,"min_y",-64)), i(height,"max",i(o,"max_y",320)), i(time,"min",i(o,"min_time",0)), i(time,"max",i(o,"max_time",24000)), strings(conditions,"biomes",strings(o,"biomes")), strings(conditions,"dimensions",strings(o,"dimensions")), strings(conditions,"active_blocks",strings(o,"active_blocks")), triggerName, i(wave,"radius",i(o,"radius",24)), i(wave,"maximum_attempts_per_member",i(o,"attempts",16)), spawns, strings(o,"effects"), strings(o,"sounds"), o);
    }
    static int i(JsonObject o,String k,int d){return o.has(k)?o.get(k).getAsInt():d;}
    static double d(JsonObject o,String k,double d){return o.has(k)?o.get(k).getAsDouble():d;}
    private static boolean bundledCommandOnly(String id){
        return id.startsWith("ambush:example_");
    }
    static String s(JsonObject o,String k,String d){return o.has(k)?o.get(k).getAsString():d;}
    static List<String> strings(JsonObject o,String k){ if(!o.has(k))return List.of(); List<String> r=new ArrayList<>(); o.getAsJsonArray(k).forEach(x->r.add(x.getAsString())); return r; }
    static List<String> strings(JsonObject o,String k,List<String> fallback){ return o.has(k)?strings(o,k):fallback; }
    private static void validateActions(JsonObject definition){
        if(!definition.has("actions"))return;
        if(!definition.get("actions").isJsonArray())throw new JsonParseException("actions must be an array");
        Set<String> types=Set.of("directional_cbc_shell_rain","directional_entity_wave","directional_arrow_rain","directional_entity_rain","directional_potion_rain","conditional_spawn","sound","fog","entity_wave","structure","sable_substructure","block_platform","potion_rain","arrow_rain","entity_rain","cbc_shell_rain","shell_rain","sable_structure","sable_formation","sable_sublevel","sable_sublevel_direct");
        int index=0;
        for(JsonElement raw:definition.getAsJsonArray("actions")){
            if(!raw.isJsonObject())throw new JsonParseException("actions["+index+"] must be an object");
            JsonObject action=raw.getAsJsonObject(); String type=s(action,"type","");
            if(!types.contains(type))throw new JsonParseException("unknown action type at actions["+index+"]: "+type);
            if(type.equals("fog")){
                double near=d(action,"near_distance",d(action,"fog_start",0)),far=d(action,"far_distance",d(action,"render_distance",32));
                if(!Double.isFinite(near)||!Double.isFinite(far)||near<0||far<=near||far>2048)throw new JsonParseException("fog distances must be finite and satisfy 0 <= near < far <= 2048 at actions["+index+"]");
            }
            if((type.startsWith("sable_")||type.equals("sable_structure"))&&action.has("redstone_activations")){
                if(!action.get("redstone_activations").isJsonArray())throw new JsonParseException("redstone_activations must be an array at actions["+index+"]");
                for(JsonElement activation:action.getAsJsonArray("redstone_activations")){if(!activation.isJsonObject())throw new JsonParseException("redstone activation entries must be objects at actions["+index+"]");JsonObject activationObject=activation.getAsJsonObject();if(activationObject.has("player_y_bands")){if(!activationObject.get("player_y_bands").isJsonArray())throw new JsonParseException("player_y_bands must be an array");List<double[]> bands=new ArrayList<>();for(JsonElement rawBand:activationObject.getAsJsonArray("player_y_bands")){if(!rawBand.isJsonObject())throw new JsonParseException("player_y_bands entries must be objects");double min=d(rawBand.getAsJsonObject(),"min_y",-2048),max=d(rawBand.getAsJsonObject(),"max_y",2048);if(!Double.isFinite(min)||!Double.isFinite(max)||min>max)throw new JsonParseException("invalid player Y band");for(double[] existing:bands)if(min<=existing[1]&&max>=existing[0])throw new JsonParseException("overlapping player_y_bands are not allowed");bands.add(new double[]{min,max});}}}
            }
            if((type.startsWith("sable_")||type.equals("sable_structure"))&&action.has("steering_controls")){
                if(!action.get("steering_controls").isJsonArray())throw new JsonParseException("steering_controls must be an array at actions["+index+"]");
                for(JsonElement rawControl:action.getAsJsonArray("steering_controls")){if(!rawControl.isJsonObject())throw new JsonParseException("steering_controls entries must be objects");JsonObject control=rawControl.getAsJsonObject();String mode=s(control,"mode","sectors");if(!Set.of("sectors","continuous").contains(mode))throw new JsonParseException("steering control mode must be sectors or continuous");double limit=d(control,"max_angle",45);if(!Double.isFinite(limit)||limit<=0||limit>45)throw new JsonParseException("steering max_angle must be within 0 < angle <= 45");String behind=s(control,"behind_direction","last");if(!Set.of("last","left","right").contains(behind))throw new JsonParseException("behind_direction must be last, left, or right");if(control.has("direction_angles")){if(!control.get("direction_angles").isJsonObject())throw new JsonParseException("direction_angles must be an object");for(Map.Entry<String,JsonElement> angle:control.getAsJsonObject("direction_angles").entrySet()){if(!Set.of("front","front_right","right","back_right","behind","back_left","left","front_left","above","below").contains(angle.getKey()))throw new JsonParseException("unknown steering direction: "+angle.getKey());double value=angle.getValue().getAsDouble();if(!Double.isFinite(value)||value < -45||value > 45)throw new JsonParseException("steering direction angle must be between -45 and 45");}}}
            }
            if((type.startsWith("sable_")||type.equals("sable_structure"))&&action.has("engine_direction")&&!Set.of("forward","reverse").contains(s(action,"engine_direction","forward").toLowerCase(Locale.ROOT)))throw new JsonParseException("engine_direction must be forward or reverse at actions["+index+"]");
            if((type.startsWith("sable_")||type.equals("sable_structure"))&&action.has("propeller_direction")&&!Set.of("forward","reverse").contains(s(action,"propeller_direction","forward").toLowerCase(Locale.ROOT)))throw new JsonParseException("propeller_direction must be forward or reverse at actions["+index+"]");
            for(String directionField:List.of("engine_direction_by_player_direction","propeller_direction_by_player_direction"))if((type.startsWith("sable_")||type.equals("sable_structure"))&&action.has(directionField)){if(!action.get(directionField).isJsonObject())throw new JsonParseException(directionField+" must be an object at actions["+index+"]");for(Map.Entry<String,JsonElement> direction:action.getAsJsonObject(directionField).entrySet()){if(!Set.of("default","front","front_right","right","back_right","behind","back_left","left","front_left","above","below").contains(direction.getKey()))throw new JsonParseException("unknown player direction in "+directionField+": "+direction.getKey());if(!Set.of("forward","reverse").contains(direction.getValue().getAsString().toLowerCase(Locale.ROOT)))throw new JsonParseException(directionField+" values must be forward or reverse");}}
            if((type.startsWith("sable_")||type.equals("sable_structure"))&&action.has("boss_bar")&&!action.get("boss_bar").isJsonObject()&&(!action.get("boss_bar").isJsonPrimitive()||!action.getAsJsonPrimitive("boss_bar").isBoolean()))throw new JsonParseException("boss_bar must be a boolean or object at actions["+index+"]");
            if((type.startsWith("sable_")||type.equals("sable_structure"))&&action.has("sable_events")){
                if(!action.get("sable_events").isJsonArray())throw new JsonParseException("sable_events must be an array at actions["+index+"]");
                Set<String> eventIds=new HashSet<>();int eventIndex=0;
                for(JsonElement rawEvent:action.getAsJsonArray("sable_events")){
                    if(!rawEvent.isJsonObject())throw new JsonParseException("sable_events["+eventIndex+"] must be an object at actions["+index+"]");
                    JsonObject event=rawEvent.getAsJsonObject();String eventId=s(event,"id","event_"+eventIndex);if(!eventIds.add(eventId))throw new JsonParseException("duplicate Sable event id: "+eventId);
                    JsonObject eventTrigger=event.has("trigger")&&event.get("trigger").isJsonObject()?event.getAsJsonObject("trigger"):event;String eventType=s(eventTrigger,"type","spawn");
                    if(!Set.of("spawn","range","time","player_y","block_percent","health_percent","percent","death","destroyed").contains(eventType))throw new JsonParseException("unknown Sable event trigger: "+eventType);
                    if((eventType.equals("block_percent")||eventType.equals("health_percent")||eventType.equals("percent"))){double percent=d(eventTrigger,"at_or_below_percent",d(eventTrigger,"percent",50));if(!Double.isFinite(percent)||percent<0||percent>100)throw new JsonParseException("Sable event percent must be between 0 and 100");}
                    if(event.has("repeat_ticks")&&(eventType.equals("block_percent")||eventType.equals("health_percent")||eventType.equals("percent")||eventType.equals("death")||eventType.equals("destroyed")))throw new JsonParseException("repeat_ticks is not allowed for percentage or death Sable events: "+eventId);
                    if(!event.has("actions")||!event.get("actions").isJsonArray())throw new JsonParseException("Sable event actions must be an array: "+eventId);
                    for(JsonElement nested:event.getAsJsonArray("actions")){if(!nested.isJsonObject())throw new JsonParseException("Sable event action must be an object: "+eventId);String nestedType=s(nested.getAsJsonObject(),"type","");if(!Set.of("sound","redstone","redstone_activation","ambush","sable_structure","sable_formation","fog","fireworks","effect","directional_entity_wave","conditional_spawn").contains(nestedType))throw new JsonParseException("unknown Sable event action: "+nestedType);}
                    eventIndex++;
                }
            }
            index++;
        }
    }
}

record ChancePolicy(String mode, double basePercent, double increaseOnFailurePercent, double maximumPercent, boolean resetOnSuccess) {
    boolean buildsUp(){ return "build_up".equals(mode)||"buildup".equals(mode)||"escalating".equals(mode); }
    double current(int failures){ return Math.min(Math.max(basePercent,maximumPercent),basePercent+(buildsUp()?Math.max(0,failures)*increaseOnFailurePercent:0)); }
}

record SpawnSpec(String entity, int count, String passenger, List<SpawnSpec> passengers, boolean avoidLineOfSight, boolean persistent, List<String> tags, String placement, String target, boolean aggroThroughWalls, double followDistance, List<String> effects, String mainHand, String offHand, double crossbowRange, double targetRange, double aggroRange, boolean friendlyFire) {
    static SpawnSpec read(JsonObject o){
        List<String> tags=new ArrayList<>(); if(o.has("tags"))o.getAsJsonArray("tags").forEach(x->tags.add(x.getAsString()));
        int count=1; if(o.has("count")){ if(o.get("count").isJsonPrimitive()) count=o.get("count").getAsInt(); else { JsonObject r=o.getAsJsonObject("count"); int min=r.has("min")?r.get("min").getAsInt():1; int max=r.has("max")?r.get("max").getAsInt():min; count=min+(int)(Math.random()*(max-min+1)); } }
        List<SpawnSpec> passengers=new ArrayList<>(); if(o.has("passengers"))o.getAsJsonArray("passengers").forEach(x->passengers.add(read(x.getAsJsonObject())));
        String passenger=o.has("passenger")?o.get("passenger").getAsString():null; if(passenger!=null)passengers.add(new SpawnSpec(passenger,1,null,List.of(),true,true,List.of(),"inherit","owner",false,0,List.of(),"","",16,32,0,true));
        JsonObject equipment=o.has("equipment")&&o.get("equipment").isJsonObject()?o.getAsJsonObject("equipment"):new JsonObject();
        double targetRange=o.has("target_range")?o.get("target_range").getAsDouble():(o.has("attack_range")?o.get("attack_range").getAsDouble():32);
        double aggroRange=o.has("aggro_range")?o.get("aggro_range").getAsDouble():0;
        boolean friendlyFire=o.has("friendly_fire")?o.get("friendly_fire").getAsBoolean():
            o.has("allow_friendly_fire")?o.get("allow_friendly_fire").getAsBoolean():true;
        return new SpawnSpec(o.get("entity").getAsString(),count,null,passengers,!o.has("avoid_line_of_sight")||o.get("avoid_line_of_sight").getAsBoolean(),o.has("persistent")&&o.get("persistent").getAsBoolean(),tags,o.has("placement")?o.get("placement").getAsString():"inherit",o.has("target")?o.get("target").getAsString():"owner",o.has("aggro_through_walls")&&o.get("aggro_through_walls").getAsBoolean(),o.has("follow_distance")?o.get("follow_distance").getAsDouble():0,strings(o,"effects"),s(equipment,"mainhand",s(o,"mainhand","")),s(equipment,"offhand",s(o,"offhand","")),o.has("crossbow_range")?o.get("crossbow_range").getAsDouble():16,targetRange,aggroRange,friendlyFire);
    }
    static List<String> strings(JsonObject o,String k){ if(!o.has(k)||!o.get(k).isJsonArray())return List.of(); List<String> r=new ArrayList<>(); o.getAsJsonArray(k).forEach(x->r.add(x.getAsString())); return r; }
    static String s(JsonObject o,String k,String fallback){return o.has(k)?o.get(k).getAsString():fallback;}
}
