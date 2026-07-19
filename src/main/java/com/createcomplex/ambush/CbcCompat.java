package com.createcomplex.ambush;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import java.lang.reflect.Method;
import com.google.gson.JsonObject;
import net.minecraft.world.entity.projectile.Projectile;

final class CbcCompat {
    private CbcCompat() {}
    static boolean spawn(ServerLevel level, double x, double y, double z, String blockId, String itemId) {
        return spawn(level,x,y,z,blockId,itemId,null);
    }
    static boolean spawn(ServerLevel level, double x, double y, double z, String blockId, String itemId, JsonObject options) {
        try {
            Class<?> projectileBlock = Class.forName("rbasamoyai.createbigcannons.munitions.big_cannon.ProjectileBlock");
            ResourceLocation bid=ResourceLocation.parse(blockId);
            if(!net.minecraft.core.registries.BuiltInRegistries.BLOCK.containsKey(bid))return false;
            Block block=net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(bid);
            if(!projectileBlock.isInstance(block))return false;
            ItemStack stack=ItemStack.EMPTY;
            if(itemId!=null&&!itemId.isBlank()){
                ResourceLocation iid=ResourceLocation.parse(itemId);
                if(!net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(iid))return false;
                stack=new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(iid));
            }
            Method method=projectileBlock.getMethod("getProjectile",net.minecraft.world.level.Level.class,ItemStack.class);
            Entity projectile=(Entity)method.invoke(block,level,stack);
            if(projectile==null)return false;
            if(options!=null&&options.has("fuze")){Class<?> fuzed=Class.forName("rbasamoyai.createbigcannons.munitions.big_cannon.FuzedBigCannonProjectile"); Class<?> fuzeItem=Class.forName("rbasamoyai.createbigcannons.munitions.fuzes.FuzeItem"); ResourceLocation fid=ResourceLocation.parse(options.get("fuze").getAsString()); if(!net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(fid)||!fuzed.isInstance(projectile)||!fuzeItem.isInstance(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(fid)))return false; ItemStack fuzeStack=new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(fid)); if(options.has("fuze_ticks")){Object component=Class.forName("rbasamoyai.createbigcannons.index.CBCDataComponents").getField("FUZE_TIMER").get(null); ItemStack.class.getMethod("set",net.minecraft.core.component.DataComponentType.class,Object.class).invoke(fuzeStack,component,Math.max(1,options.get("fuze_ticks").getAsInt()));} fuzed.getMethod("setFuze",ItemStack.class).invoke(projectile,fuzeStack);}
            if(options!=null&&options.has("owner_uuid"))try{((Projectile)projectile).setOwner(level.getEntity(java.util.UUID.fromString(options.get("owner_uuid").getAsString())));}catch(Exception ignored){}
            float velocity=options!=null&&options.has("velocity")?options.get("velocity").getAsFloat():1f; float inaccuracy=options!=null&&options.has("inaccuracy")?options.get("inaccuracy").getAsFloat():0f; net.minecraft.world.phys.Vec3 direction=new net.minecraft.world.phys.Vec3(0,-1,0); if(options!=null&&options.has("target_x")&&options.has("target_y")&&options.has("target_z")){net.minecraft.world.phys.Vec3 target=new net.minecraft.world.phys.Vec3(options.get("target_x").getAsDouble(),options.get("target_y").getAsDouble(),options.get("target_z").getAsDouble());direction=target.subtract(x,y,z).normalize();if(options.has("ballistic")&&options.get("ballistic").getAsBoolean())direction=ballisticDirection(target.subtract(x,y,z),velocity,options.has("gravity")?options.get("gravity").getAsDouble():0.05D,direction);} projectile.moveTo(x,y,z,0,0); if(projectile instanceof Projectile shot)shot.shoot(direction.x,direction.y,direction.z,velocity,inaccuracy); try{projectile.getClass().getMethod("setOrientation",net.minecraft.world.phys.Vec3.class).invoke(projectile,direction);}catch(ReflectiveOperationException ignored){} level.addFreshEntity(projectile); return true;
        } catch (ReflectiveOperationException|RuntimeException ex) { return false; }
    }
    private static net.minecraft.world.phys.Vec3 ballisticDirection(net.minecraft.world.phys.Vec3 delta,float velocity,double gravity,net.minecraft.world.phys.Vec3 fallback){double horizontal=Math.sqrt(delta.x*delta.x+delta.z*delta.z),speed2=velocity*velocity;if(horizontal<0.001D||gravity<=0)return fallback;double discriminant=speed2*speed2-gravity*(gravity*horizontal*horizontal+2D*delta.y*speed2);if(discriminant<0)return fallback;double tangent=(speed2-Math.sqrt(discriminant))/(gravity*horizontal);return new net.minecraft.world.phys.Vec3(delta.x/horizontal,tangent,delta.z/horizontal).normalize();}
}
