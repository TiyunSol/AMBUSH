package com.createcomplex.ambush;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.phys.Vec3;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.List;

final class FireworkAction {
    private FireworkAction() {}

    static int spawn(ServerLevel level, Vec3 origin, JsonObject action) {
        int count=Math.max(1,Math.min(64,integer(action,"count",6)));
        double height=Math.max(0,Math.min(128,number(action,"height",8)));
        double spread=Math.max(0,Math.min(64,number(action,"spread",10)));
        int flight=Math.max(1,Math.min(3,integer(action,"flight",2)));
        FireworkExplosion.Shape shape=shape(string(action,"shape","large_ball"));
        IntArrayList colors=colors(action,"colors",0xB05CFF,0xFFFFFF);
        IntArrayList fades=colors(action,"fade_colors",0xFFFFFF);
        int made=0;
        for(int i=0;i<count;i++){
            ItemStack stack=new ItemStack(Items.FIREWORK_ROCKET);
            stack.set(DataComponents.FIREWORKS,new Fireworks(flight,List.of(new FireworkExplosion(shape,colors,fades,true,true))));
            double x=origin.x+(level.random.nextDouble()-.5)*spread;
            double z=origin.z+(level.random.nextDouble()-.5)*spread;
            FireworkRocketEntity rocket=new FireworkRocketEntity(level,x,origin.y+height,z,stack);
            rocket.setDeltaMovement((level.random.nextDouble()-.5)*.12,.35+level.random.nextDouble()*.12,(level.random.nextDouble()-.5)*.12);
            level.addFreshEntity(rocket);
            made++;
        }
        return made;
    }

    private static FireworkExplosion.Shape shape(String raw){
        try{return FireworkExplosion.Shape.valueOf(raw.toUpperCase(java.util.Locale.ROOT));}
        catch(IllegalArgumentException ignored){return FireworkExplosion.Shape.LARGE_BALL;}
    }
    private static IntArrayList colors(JsonObject action,String key,int... fallback){
        IntArrayList out=new IntArrayList();
        if(action.has(key)&&action.get(key).isJsonArray())for(JsonElement value:action.getAsJsonArray(key))out.add(color(value.getAsString()));
        if(out.isEmpty())for(int value:fallback)out.add(value);
        return out;
    }
    private static int color(String raw){try{return Integer.parseInt(raw.replace("#", ""),16)&0xFFFFFF;}catch(NumberFormatException ignored){return 0xFFFFFF;}}
    private static int integer(JsonObject object,String key,int fallback){return object.has(key)?object.get(key).getAsInt():fallback;}
    private static double number(JsonObject object,String key,double fallback){return object.has(key)?object.get(key).getAsDouble():fallback;}
    private static String string(JsonObject object,String key,String fallback){return object.has(key)?object.get(key).getAsString():fallback;}
}
