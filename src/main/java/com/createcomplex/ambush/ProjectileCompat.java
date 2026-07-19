package com.createcomplex.ambush;

import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.phys.Vec3;

/** Registry-backed projectile construction for vanilla and modded content. */
final class ProjectileCompat {
    private ProjectileCompat() {}

    static Entity create(ServerPlayer player, JsonObject action) {
        String actionType=string(action,"type","");
        String kind=string(action,"kind",string(action,"projectile_kind","entity"));
        if("arrow".equals(kind)||actionType.contains("arrow")||action.has("arrow")||action.has("arrow_item"))return arrow(player,action);
        if("potion".equals(kind)||actionType.contains("potion")||action.has("potion"))return potion(player,action);
        ResourceLocation id=parse(string(action,"entity","minecraft:arrow"));
        if(id==null||!BuiltInRegistries.ENTITY_TYPE.containsKey(id))return null;
        EntityType<?> type=BuiltInRegistries.ENTITY_TYPE.get(id);
        return type.create(player.serverLevel());
    }

    static int spawnVertical(ServerPlayer player,JsonObject action,int count){
        double spread=number(action,"spread",12),height=number(action,"height",18),speed=number(action,"velocity",1.0);
        int made=0;
        for(int i=0;i<count;i++){
            Entity entity=create(player,action); if(entity==null)continue;
            double x=player.getX()+(player.getRandom().nextDouble()-.5)*spread;
            double z=player.getZ()+(player.getRandom().nextDouble()-.5)*spread;
            entity.moveTo(x,player.getY()+height,z,player.getYRot(),90);
            own(player,entity); entity.setDeltaMovement(0,-Math.abs(speed),0);
            if(player.serverLevel().addFreshEntity(entity))made++;
        }
        return made;
    }

    private static Entity arrow(ServerPlayer player,JsonObject action){
        String itemId=string(action,"arrow",string(action,"arrow_item",string(action,"item","minecraft:arrow")));
        ResourceLocation id=parse(itemId);
        if(id==null||!BuiltInRegistries.ITEM.containsKey(id))return null;
        Item item=BuiltInRegistries.ITEM.get(id);
        if(!(item instanceof ArrowItem arrowItem))return null;
        ItemStack ammunition=new ItemStack(item);
        applyPotion(player,ammunition,action);
        // ArrowItem validates the firing weapon. Supply a bow so an unrelated held item
        // cannot make vanilla reject this data-driven projectile as an arrow shot.
        AbstractArrow arrow=arrowItem.createArrow(player.serverLevel(),ammunition,player,new ItemStack(Items.BOW));
        if(action.has("pickup")&&"allowed".equalsIgnoreCase(action.get("pickup").getAsString()))arrow.pickup=AbstractArrow.Pickup.ALLOWED;
        else arrow.pickup=AbstractArrow.Pickup.DISALLOWED;
        return arrow;
    }

    private static Entity potion(ServerPlayer player,JsonObject action){
        String itemId=string(action,"item",string(action,"potion_item","minecraft:splash_potion"));
        ResourceLocation id=parse(itemId);
        if(id==null||!BuiltInRegistries.ITEM.containsKey(id))return null;
        Item item=BuiltInRegistries.ITEM.get(id);
        if(!(item instanceof SplashPotionItem)&&!(item instanceof LingeringPotionItem))return null;
        ItemStack stack=new ItemStack(item); applyPotion(player,stack,action);
        ThrownPotion projectile=new ThrownPotion(player.serverLevel(),player.getX(),player.getEyeY(),player.getZ());
        projectile.setItem(stack);
        return projectile;
    }

    private static void applyPotion(ServerPlayer player,ItemStack stack,JsonObject action){
        if(!action.has("potion"))return;
        ResourceLocation potionId=parse(action.get("potion").getAsString()); if(potionId==null)return;
        var registry=player.registryAccess().registryOrThrow(Registries.POTION);
        registry.getHolder(potionId).ifPresent(holder->stack.set(net.minecraft.core.component.DataComponents.POTION_CONTENTS,new PotionContents(holder)));
    }

    static void own(ServerPlayer player,Entity entity){
        entity.addTag("ambush_owned");
        entity.addTag("ambush_owner_"+player.getUUID().toString().replace("-",""));
    }
    private static ResourceLocation parse(String id){try{return ResourceLocation.parse(id);}catch(RuntimeException ignored){return null;}}
    private static String string(JsonObject o,String key,String fallback){return o.has(key)?o.get(key).getAsString():fallback;}
    private static double number(JsonObject o,String key,double fallback){return o.has(key)?o.get(key).getAsDouble():fallback;}
}
