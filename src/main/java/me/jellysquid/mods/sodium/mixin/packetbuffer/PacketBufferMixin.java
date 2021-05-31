package me.jellysquid.mods.sodium.mixin.packetbuffer;

import net.minecraft.network.PacketBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * ### Spoorn
 * Crashes server for Spoorn pack
 */
@Mixin(PacketBuffer.class)
public abstract class PacketBufferMixin {

    @Shadow
    public abstract PacketBuffer writeString(String string, int maxLength);

    private static final int MAX_STRING_SIZE = 16777216;

    @Redirect(method = "writeString(Ljava/lang/String;)Lnet/minecraft/network/PacketBuffer;", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/network/PacketBuffer;writeString(Ljava/lang/String;I)Lnet/minecraft/network/PacketBuffer;"))
    public PacketBuffer replaceWriteStringMaxSize(PacketBuffer packetBuffer, String string, int maxLength) {
        return this.writeString(string, MAX_STRING_SIZE);
    }
}
