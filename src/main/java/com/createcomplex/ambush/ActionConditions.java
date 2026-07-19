package com.createcomplex.ambush;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;

final class ActionConditions {
    private ActionConditions() {}

    static boolean matches(ServerPlayer player, JsonObject action) {
        if (AmbushRuntime.commandOverride) return true;
        if (!action.has("conditions") || !action.get("conditions").isJsonObject()) return true;
        JsonObject conditions = action.getAsJsonObject("conditions");
        int y = player.blockPosition().getY();
        if (conditions.has("min_y") && y < conditions.get("min_y").getAsInt()) return false;
        if (conditions.has("max_y") && y > conditions.get("max_y").getAsInt()) return false;

        long dayTime = player.level().getDayTime() % 24000;
        boolean night = dayTime >= 13000 && dayTime <= 23000;
        String time = string(conditions, "time", "any");
        if ("night".equals(time) && !night || "day".equals(time) && night) return false;

        String weather = string(conditions, "weather", "any");
        if (("thunder".equals(weather) || "stormy".equals(weather)) && !player.serverLevel().isThundering()) return false;
        if ("rain".equals(weather) && !player.serverLevel().isRaining()) return false;
        if ("clear".equals(weather) && player.serverLevel().isRaining()) return false;

        if (conditions.has("over_ocean") && conditions.get("over_ocean").getAsBoolean() != isOcean(player)) return false;
        if (conditions.has("dimensions") && conditions.get("dimensions").isJsonArray()) {
            String current = player.serverLevel().dimension().location().toString();
            boolean found = false;
            for (JsonElement value : conditions.getAsJsonArray("dimensions")) if (current.equals(value.getAsString())) found = true;
            if (!found) return false;
        }
        return true;
    }

    private static boolean isOcean(ServerPlayer player) {
        var biome = player.serverLevel().getBiome(player.blockPosition());
        if (biome.is(TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath("minecraft", "is_ocean")))) return true;
        return biome.unwrapKey().map(key -> key.location().getPath().contains("ocean")).orElse(false);
    }

    private static String string(JsonObject object, String key, String fallback) {
        return object.has(key) ? object.get(key).getAsString().toLowerCase(java.util.Locale.ROOT) : fallback;
    }
}
