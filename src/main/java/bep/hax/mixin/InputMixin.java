package bep.hax.mixin;
import bep.hax.accessor.InputAccessor;
import net.minecraft.client.player.ClientInput;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
@Mixin(ClientInput.class)
public abstract class InputMixin implements InputAccessor {
    @Shadow
    public abstract Vec2 getMoveVector(); // 26.1: ClientInput.getMovementInput -> getMoveVector
    @Override
    public float getMovementForward() {
        return this.getMoveVector().y;
    }
    @Override
    public void setMovementForward(float value) {
    }
    @Override
    public float getMovementSideways() {
        return this.getMoveVector().x;
    }
    @Override
    public void setMovementSideways(float value) {
    }
}