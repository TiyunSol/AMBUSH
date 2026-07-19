package com.createcomplex.ambush;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

final class AmbushNetworking {
    private AmbushNetworking() {}
    static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(FogPayload.TYPE, FogPayload.STREAM_CODEC, (payload, context) -> {
            if (FMLEnvironment.dist == Dist.CLIENT) context.enqueueWork(() -> AmbushClient.apply(payload));
        });
    }
}
