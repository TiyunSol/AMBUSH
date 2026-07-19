package com.createcomplex.ambush;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

record FogPayload(ResourceLocation dimension, float nearDistance, float farDistance, float red, float green, float blue,
                  int durationTicks, int fadeInTicks, int fadeOutTicks, boolean spherical, boolean overrideFluidFog) implements CustomPacketPayload {
    static final Type<FogPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Ambush.MOD_ID, "fog"));
    static final StreamCodec<RegistryFriendlyByteBuf, FogPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override public FogPayload decode(RegistryFriendlyByteBuf buffer) {
            return new FogPayload(buffer.readResourceLocation(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat(),
                buffer.readFloat(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readBoolean(), buffer.readBoolean());
        }
        @Override public void encode(RegistryFriendlyByteBuf buffer, FogPayload payload) {
            buffer.writeResourceLocation(payload.dimension);
            buffer.writeFloat(payload.nearDistance); buffer.writeFloat(payload.farDistance);
            buffer.writeFloat(payload.red); buffer.writeFloat(payload.green); buffer.writeFloat(payload.blue);
            buffer.writeVarInt(payload.durationTicks); buffer.writeVarInt(payload.fadeInTicks);
            buffer.writeVarInt(payload.fadeOutTicks); buffer.writeBoolean(payload.spherical); buffer.writeBoolean(payload.overrideFluidFog);
        }
    };
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
