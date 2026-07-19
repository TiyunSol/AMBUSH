package com.createcomplex.ambush;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

final class FogAction {
    private FogAction() {}
    static int apply(ServerPlayer player, JsonObject action) {
        boolean clear = action.has("clear") && action.get("clear").getAsBoolean();
        float near = clamp(number(action, "near_distance", number(action, "fog_start", 0)), 0, 2048);
        float far = clamp(number(action, "far_distance", number(action, "render_distance", 32)), .01f, 2048);
        near = Math.min(near, far - .01f);
        float[] color = color(action);
        int duration = action.has("duration_ticks") ? integer(action, "duration_ticks", 200)
            : Math.round(number(action, "duration_seconds", 10) * 20);
        if (clear) duration = 0;
        duration = Math.max(0, Math.min(1_728_000, duration));
        int fadeIn = Math.max(0, Math.min(duration, integer(action, "fade_in_ticks", 0)));
        int fadeOut = Math.max(0, Math.min(duration, integer(action, "fade_out_ticks", 0)));
        boolean sphere = !action.has("shape") || !"cylinder".equalsIgnoreCase(action.get("shape").getAsString());
        boolean overrideFluids=action.has("override_fluid_fog")&&action.get("override_fluid_fog").getAsBoolean();
        PacketDistributor.sendToPlayer(player, new FogPayload(player.serverLevel().dimension().location(), near, far, color[0], color[1], color[2], duration, fadeIn, fadeOut, sphere, overrideFluids));
        return 1;
    }

    private static float[] color(JsonObject action) {
        if (action.has("color") && action.get("color").isJsonArray()) {
            JsonArray array=action.getAsJsonArray("color");
            if(array.size()>=3)return new float[]{unit(array.get(0).getAsFloat()),unit(array.get(1).getAsFloat()),unit(array.get(2).getAsFloat())};
        }
        if (action.has("color") && action.get("color").isJsonPrimitive()) {
            String value=action.get("color").getAsString().replace("#","");
            try { int rgb=Integer.parseInt(value,16); return new float[]{((rgb>>16)&255)/255f,((rgb>>8)&255)/255f,(rgb&255)/255f}; }
            catch(NumberFormatException ignored) { }
        }
        return new float[]{unit(number(action,"red",.45f)),unit(number(action,"green",.5f)),unit(number(action,"blue",.55f))};
    }
    private static float unit(float value){return value>1?clamp(value/255f,0,1):clamp(value,0,1);}
    private static float clamp(float value,float min,float max){return Math.max(min,Math.min(max,value));}
    private static int integer(JsonObject o,String key,int fallback){return o.has(key)?o.get(key).getAsInt():fallback;}
    private static float number(JsonObject o,String key,float fallback){return o.has(key)?o.get(key).getAsFloat():fallback;}
}
