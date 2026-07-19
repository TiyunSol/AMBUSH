package com.createcomplex.ambush;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;

/** Uses Sable's public split listener and container observer lifecycle. */
final class SableSplitCleanup {
    private record Pending(UUID parentId, long due) {}
    private static final Map<ServerLevel, UUID> PENDING_PARENT = new IdentityHashMap<>();
    private static final Map<UUID, Pending> CHILDREN = new HashMap<>();
    private static final Set<ServerLevel> INSTALLED = Collections.newSetFromMap(new IdentityHashMap<>());
    private static final Set<ServerLevel> FAILED = Collections.newSetFromMap(new IdentityHashMap<>());

    private SableSplitCleanup() {}

    static void tick(ServerLevel level) {
        install(level);
        long now=level.getGameTime();
        Iterator<Map.Entry<UUID,Pending>> iterator=CHILDREN.entrySet().iterator();
        while(iterator.hasNext()) {
            Map.Entry<UUID,Pending> item=iterator.next();
            if(now<item.getValue().due) continue;
            try { remove(level,item.getKey()); Ambush.LOGGER.info("Removed split-off Ambush Sable sublevel={} after 10-second cleanup",item.getKey()); }
            catch(Exception exception) { Ambush.LOGGER.warn("Could not remove scheduled Ambush split sublevel={}",item.getKey(),exception); }
            iterator.remove();
        }
    }

    private static void install(ServerLevel level) {
        if(INSTALLED.contains(level)||FAILED.contains(level)) return;
        try {
            Class<?> containerType=Class.forName("dev.ryanhcode.sable.api.sublevel.SubLevelContainer");
            Object container=containerType.getMethod("getContainer",ServerLevel.class).invoke(null,level);
            Class<?> observerType=Class.forName("dev.ryanhcode.sable.api.sublevel.SubLevelObserver");
            Object observer=Proxy.newProxyInstance(SableSplitCleanup.class.getClassLoader(),new Class[]{observerType},(proxy,method,args)->{
                Object objectResult=objectMethod(proxy,method,args,"AmbushSubLevelObserver");if(objectResult!=NOT_OBJECT_METHOD)return objectResult;
                if("onSubLevelAdded".equals(method.getName())&&args!=null&&args.length>0) onAdded(level,args[0]);
                return null;
            });
            containerType.getMethod("addObserver",observerType).invoke(container,observer);
            Class<?> listenerType=Class.forName("dev.ryanhcode.sable.sublevel.plot.heat.SubLevelHeatMapManager$SplitListener");
            Object listener=Proxy.newProxyInstance(SableSplitCleanup.class.getClassLoader(),new Class[]{listenerType},(proxy,method,args)->{
                Object objectResult=objectMethod(proxy,method,args,"AmbushSplitListener");if(objectResult!=NOT_OBJECT_METHOD)return objectResult;
                if("addBlocks".equals(method.getName())&&args!=null&&args.length>=3&&args[0] instanceof ServerLevel source&&args[2] instanceof Collection<?> blocks&&!blocks.isEmpty()) onSplit(source,blocks.iterator().next());
                return null;
            });
            Class.forName("dev.ryanhcode.sable.sublevel.plot.heat.SubLevelHeatMapManager").getMethod("addSplitListener",listenerType).invoke(null,listener);
            INSTALLED.add(level);
        } catch(Exception exception) { FAILED.add(level);Ambush.LOGGER.warn("Could not install optional Sable split cleanup listener; this level will not retry",exception); }
    }

    private static final Object NOT_OBJECT_METHOD=new Object();
    private static Object objectMethod(Object proxy,Method method,Object[] args,String label){
        if(method.getDeclaringClass()!=Object.class)return NOT_OBJECT_METHOD;
        return switch(method.getName()){
            case "equals"->proxy==(args==null||args.length==0?null:args[0]);
            case "hashCode"->System.identityHashCode(proxy);
            case "toString"->label+'@'+Integer.toHexString(System.identityHashCode(proxy));
            default->null;
        };
    }

    private static void onSplit(ServerLevel level,Object rawPosition) {
        if(!(rawPosition instanceof BlockPos position)) return;
        try {
            Object helper=Class.forName("dev.ryanhcode.sable.Sable").getField("HELPER").get(null);
            Object sub=helper.getClass().getMethod("getContaining",net.minecraft.world.level.Level.class,net.minecraft.core.Vec3i.class).invoke(helper,level,position);
            if(sub==null) return;
            UUID id=(UUID)sub.getClass().getMethod("getUniqueId").invoke(sub);
            for(SableAmbushState.Entry entry:SableAmbushState.get(level.getServer()).entries()) if(id.equals(entry.subLevelId)) { PENDING_PARENT.put(level,id); return; }
        } catch(Exception exception) { Ambush.LOGGER.warn("Could not correlate Sable split to Ambush parent",exception); }
    }

    private static void onAdded(ServerLevel level,Object child) {
        UUID parent=PENDING_PARENT.remove(level); if(parent==null) return;
        try {
            UUID childId=(UUID)child.getClass().getMethod("getUniqueId").invoke(child);
            if(childId.equals(parent)) return;
            SableAmbushState.Entry entry=null; for(SableAmbushState.Entry candidate:SableAmbushState.get(level.getServer()).entries())if(parent.equals(candidate.subLevelId)){entry=candidate;break;}
            if(entry==null) return;
            JsonObject action=JsonParser.parseString(entry.actionJson).getAsJsonObject();
            if(action.has("split_off_despawn")&&"normal".equalsIgnoreCase(action.get("split_off_despawn").getAsString())) return;
            int ticks=action.has("split_off_despawn_ticks")?Math.max(20,action.get("split_off_despawn_ticks").getAsInt()):200;
            CHILDREN.put(childId,new Pending(parent,level.getGameTime()+ticks));
        } catch(Exception exception) { Ambush.LOGGER.warn("Could not schedule Ambush split sublevel cleanup",exception); }
    }

    private static void remove(ServerLevel level,UUID id)throws Exception {
        Class<?> type=Class.forName("dev.ryanhcode.sable.api.sublevel.SubLevelContainer"); Object container=type.getMethod("getContainer",ServerLevel.class).invoke(null,level); Object sub=type.getMethod("getSubLevel",UUID.class).invoke(container,id); if(sub==null)return;
        Class<?> reason=Class.forName("dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason"); Object removed=Enum.valueOf((Class<Enum>)reason.asSubclass(Enum.class),"REMOVED"); type.getMethod("removeSubLevel",Class.forName("dev.ryanhcode.sable.sublevel.SubLevel"),reason).invoke(container,sub,removed);
    }
}
