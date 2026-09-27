package bep.hax.mixin.accessor;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(ClientboundSetEntityMotionPacket.class)
public interface EntityVelocityUpdateS2CPacketAccessor {
    @Accessor("entityId")
    int getEntityId();
    @Accessor("velocity")
    Vec3 getVelocity();
}