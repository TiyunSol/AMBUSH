package com.createcomplex.ambush;

import com.mojang.blaze3d.shaders.FogShape;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

final class AmbushClient {
    private static FogPayload fog;
    private static int age;
    private static int remaining;

    static void apply(FogPayload payload) {
        if (payload.durationTicks() <= 0) { clear(); return; }
        if (Minecraft.getInstance().level == null || !Minecraft.getInstance().level.dimension().location().equals(payload.dimension())) return;
        fog = payload; age = 0; remaining = payload.durationTicks();
    }

    private static void clear() { fog = null; age = 0; remaining = 0; }

    @SubscribeEvent void tick(ClientTickEvent.Post event) {
        if (Minecraft.getInstance().level == null) { clear(); return; }
        if (fog != null && !Minecraft.getInstance().level.dimension().location().equals(fog.dimension())) { clear(); return; }
        if (fog != null) { age++; if (--remaining <= 0) clear(); }
    }

    @SubscribeEvent void login(net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingIn event) { clear(); }
    @SubscribeEvent void logout(net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) { clear(); }
    @SubscribeEvent void clone(net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.Clone event) { clear(); }

    @SubscribeEvent void renderFog(ViewportEvent.RenderFog event) {
        if (fog == null) return;
        if (!fog.overrideFluidFog() && event.getType() != net.minecraft.world.level.material.FogType.NONE) return;
        float alpha = alpha();
        float near = Mth.lerp(alpha, event.getNearPlaneDistance(), fog.nearDistance());
        float far = Mth.lerp(alpha, event.getFarPlaneDistance(), fog.farDistance());
        event.setNearPlaneDistance(Math.max(0, Math.min(near, far - .01f)));
        event.setFarPlaneDistance(Math.max(.01f, far));
        event.setFogShape(fog.spherical() ? FogShape.SPHERE : FogShape.CYLINDER);
        event.setCanceled(true);
    }

    @SubscribeEvent void fogColor(ViewportEvent.ComputeFogColor event) {
        if (fog == null) return;
        if (!fog.overrideFluidFog() && event.getCamera().getFluidInCamera() != net.minecraft.world.level.material.FogType.NONE) return;
        float alpha = alpha();
        event.setRed(Mth.lerp(alpha, event.getRed(), fog.red()));
        event.setGreen(Mth.lerp(alpha, event.getGreen(), fog.green()));
        event.setBlue(Mth.lerp(alpha, event.getBlue(), fog.blue()));
    }

    private static float alpha() {
        if (fog == null) return 0;
        float in = fog.fadeInTicks() <= 0 ? 1 : Math.min(1, age / (float)fog.fadeInTicks());
        float out = fog.fadeOutTicks() <= 0 ? 1 : Math.min(1, remaining / (float)fog.fadeOutTicks());
        return Math.min(in, out);
    }
}
