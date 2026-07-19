package com.createcomplex.ambush;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;

final class DirectionalRainCompat {
    private DirectionalRainCompat() {}

    static int execute(ServerPlayer player, JsonObject action, Vec3 storedOrigin) {
        if (!ActionConditions.matches(player, action)) return 0;
        String type = action.has("type") ? action.get("type").getAsString() : "";
        if ("fog".equals(type)) return FogAction.apply(player, action);
        if ("directional_cbc_shell_rain".equals(type)) return shells(player, action, storedOrigin);
        if ("directional_arrow_rain".equals(type) || "directional_entity_rain".equals(type) || "directional_potion_rain".equals(type))
            return entities(player, action, storedOrigin);
        return 0;
    }

    /** Returns true while a data-driven rain waits for its source to close in. */
    static boolean waitingForStartDistance(ServerPlayer player, JsonObject action, Vec3 storedOrigin) {
        if (!action.has("start_distance")) return false;
        double range = Math.max(0, number(action, "start_distance", 0));
        if (range <= 0 || storedOrigin == null) return false;
        Vec3 source = source(player, action, storedOrigin);
        double dx = player.getX() - source.x;
        double dz = player.getZ() - source.z;
        return dx * dx + dz * dz > range * range;
    }

    private static int shells(ServerPlayer player, JsonObject action, Vec3 storedOrigin) {
        int count = Math.max(1, Math.min(128, integer(action, "count", 1)));
        double spread = number(action, "spread", 6);
        double speed = number(action, "velocity", 2.0);
        Vec3 origin = source(player, action, storedOrigin);
        Vec3 baseDirection = player.getEyePosition().subtract(origin).normalize();
        Vec3 side = new Vec3(-baseDirection.z, 0, baseDirection.x).normalize();
        double targetSpread = Math.max(0, number(action, "target_spread", 2));
        double targetSafeRadius = Math.max(0, Math.min(targetSpread, number(action, "target_safe_radius", 0)));
        int made = 0;
        for (int i = 0; i < count; i++) {
            double lateral = count == 1 ? 0 : (player.getRandom().nextDouble() - .5) * spread;
            Vec3 spawn = origin.add(side.scale(lateral));
            double targetAngle = player.getRandom().nextDouble() * Math.PI * 2;
            double targetRadius = targetSafeRadius + player.getRandom().nextDouble() * (targetSpread - targetSafeRadius);
            Vec3 target = player.position().add(Math.cos(targetAngle) * targetRadius,
                number(action, "target_height_offset", 0), Math.sin(targetAngle) * targetRadius);
            JsonObject options = action.deepCopy();
            options.addProperty("target_x", target.x);
            options.addProperty("target_y", target.y);
            options.addProperty("target_z", target.z);
            options.addProperty("velocity", speed);
            options.addProperty("fuze", action.has("fuze") ? action.get("fuze").getAsString() : "createbigcannons:timed_fuze");
            if (CbcCompat.spawn(player.serverLevel(), spawn.x, spawn.y, spawn.z,
                string(action, "block", "createbigcannons:he_shell"),
                string(action, "item", "createbigcannons:he_shell"), options)) made++;
        }
        return made;
    }

    private static int entities(ServerPlayer player, JsonObject action, Vec3 storedOrigin) {
        int count = Math.max(1, Math.min(128, integer(action, "count", 8)));
        Vec3 origin = source(player, action, storedOrigin);
        double spread = number(action, "spread", 12);
        double targetSpread = Math.max(0, number(action, "target_spread", 0));
        int made = 0;
        for (int i = 0; i < count; i++) {
            Entity entity = ProjectileCompat.create(player, action);
            if (entity == null) continue;
            Vec3 spawn = origin.add((player.getRandom().nextDouble() - .5) * spread, 0,
                (player.getRandom().nextDouble() - .5) * spread);
            double angle=player.getRandom().nextDouble()*Math.PI*2;
            double radius=Math.sqrt(player.getRandom().nextDouble())*targetSpread;
            Vec3 target=player.position().add(Math.cos(angle)*radius,number(action,"target_height_offset",1),Math.sin(angle)*radius);
            Vec3 delta=target.subtract(spawn);double speed=number(action,"velocity",1.5);Vec3 direction;
            if(!action.has("ballistic")||action.get("ballistic").getAsBoolean()){
                double horizontal=Math.sqrt(delta.x*delta.x+delta.z*delta.z),time=Math.max(1,horizontal/Math.max(.1,speed));
                double gravity=number(action,"gravity",.05),horizontalSpeed=Math.min(speed,horizontal/time);
                direction=new Vec3(delta.x/Math.max(.001,horizontal)*horizontalSpeed,(delta.y+.5*gravity*time*time)/time,delta.z/Math.max(.001,horizontal)*horizontalSpeed);
            }else direction=delta.normalize().scale(speed);
            entity.moveTo(spawn.x, spawn.y, spawn.z, player.getYRot(), 0);
            ProjectileCompat.own(player,entity);
            entity.setDeltaMovement(direction);
            if (player.serverLevel().addFreshEntity(entity)) made++;
        }
        return made;
    }

    private static Vec3 source(ServerPlayer player, JsonObject action, Vec3 storedOrigin) {
        double height = number(action, "source_height", number(action, "height", 24));
        if (action.has("source_structure")) {
            Vec3 current = SableCompat.currentStructureOrigin(player, action.get("source_structure").getAsString());
            if (current != null) storedOrigin = current;
        }
        if (storedOrigin != null) {
            Vec3 towardPlayer = player.position().subtract(storedOrigin).multiply(1, 0, 1);
            if (towardPlayer.lengthSqr() > 0.0001) towardPlayer = towardPlayer.normalize();
            return storedOrigin.add(towardPlayer.scale(number(action, "source_forward_offset", 0))).add(0, height, 0);
        }
        return player.position().add(number(action, "origin_offset_x", 0), height,
            number(action, "origin_offset_z", -32));
    }

    private static int integer(JsonObject object, String key, int fallback) { return object.has(key) ? object.get(key).getAsInt() : fallback; }
    private static double number(JsonObject object, String key, double fallback) { return object.has(key) ? object.get(key).getAsDouble() : fallback; }
    private static String string(JsonObject object, String key, String fallback) { return object.has(key) ? object.get(key).getAsString() : fallback; }
}
