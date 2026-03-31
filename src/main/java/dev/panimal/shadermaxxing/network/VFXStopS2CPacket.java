package dev.panimal.shadermaxxing.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record VFXStopS2CPacket(String shaderName) implements CustomPayload {
    public static final CustomPayload.Id<VFXStopS2CPacket> ID =
            new CustomPayload.Id<>(Identifier.of("shadermaxxing", "vfx_stop"));

    public static final PacketCodec<PacketByteBuf, VFXStopS2CPacket> CODEC =
            PacketCodec.of(
                    (packet, buf) -> buf.writeString(packet.shaderName),
                    buf -> new VFXStopS2CPacket(buf.readString())
            );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}