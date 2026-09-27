package bep.hax.mixin.accessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
@Mixin(ServerboundMovePlayerPacket.class)
public interface PlayerMoveC2SPacketAccessor {
    @Accessor("xRot")
    void setXRot(float pitch);
    @Accessor("yRot")
    void setYRot(float yaw);
}