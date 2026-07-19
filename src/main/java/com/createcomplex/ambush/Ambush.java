package com.createcomplex.ambush;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
import net.neoforged.neoforge.common.world.chunk.TicketController;

@Mod(Ambush.MOD_ID)
public final class Ambush {
    public static final String MOD_ID = "ambush";
    public static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("Ambush");
    static final TicketController SABLE_ASSEMBLY_TICKETS =
        new TicketController(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MOD_ID, "sable_assembly"));

    public Ambush(IEventBus modBus) {
        modBus.addListener(Ambush::registerTicketControllers);
        modBus.addListener(AmbushNetworking::register);
        NeoForge.EVENT_BUS.register(new AmbushRuntime());
        if (FMLEnvironment.dist == Dist.CLIENT) NeoForge.EVENT_BUS.register(new AmbushClient());
    }

    private static void registerTicketControllers(RegisterTicketControllersEvent event) {
        event.register(SABLE_ASSEMBLY_TICKETS);
    }
}
