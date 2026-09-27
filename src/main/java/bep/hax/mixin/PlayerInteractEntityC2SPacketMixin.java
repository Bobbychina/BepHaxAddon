package bep.hax.mixin;

import bep.hax.util.InventoryManager.IPlayerInteractEntityC2SPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/** 26.1 起攻击走独立的 ServerboundAttackPacket，交互包只承载 use/useOn，故本 mixin 仅暴露实体 id。 */
@Mixin(ServerboundInteractPacket.class)
public class PlayerInteractEntityC2SPacketMixin implements IPlayerInteractEntityC2SPacket {
    @Shadow
    @Final
    private int entityId;

    @Override
    public boolean isAttackPacket() {
        return false;
    }

    @Override
    public int getTargetEntityId() {
        return entityId;
    }
}
