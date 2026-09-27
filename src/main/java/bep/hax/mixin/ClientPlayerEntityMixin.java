package bep.hax.mixin;
import bep.hax.accessor.InputAccessor;
import bep.hax.mixin.accessor.PlayerInventoryAccessor;
import bep.hax.modules.RocketMan;
import bep.hax.util.InventoryManager;
import bep.hax.util.PushOutOfBlocksEvent;
import meteordevelopment.meteorclient.systems.modules.movement.NoSlow;
import net.minecraft.client.player.ClientInput;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.sounds.SoundSource;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import net.minecraft.client.player.LocalPlayer;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.MeteorClient;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(LocalPlayer.class)
public abstract class ClientPlayerEntityMixin {
    @Shadow public ClientInput input;
    @Shadow public abstract boolean isUsingItem();
    @Shadow public abstract boolean isCrouching(); // 26.1: isSneaking -> isCrouching

    // Note: playSoundToPlayer method removed in 1.21.11

    @Inject(method = "moveTowardsClosestSpace", at = @At("HEAD"), cancellable = true) // 26.1: pushOutOfBlocks -> moveTowardsClosestSpace
    private void onPushOutOfBlocks(double x, double z, CallbackInfo ci) {
        PushOutOfBlocksEvent event = new PushOutOfBlocksEvent();
        MeteorClient.EVENT_BUS.post(event);
        if (event.isCanceled()) {
            ci.cancel();
        }
    }
    @Inject(method = "tick", at = @At("HEAD"))
    private void onTickStart(CallbackInfo ci) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        if (player == null) return;
        bephax$checkStartEating(player);
    }
    @Inject(method = "aiStep", at = @At(value = "FIELD", target = "Lnet/minecraft/client/player/LocalPlayer;input:Lnet/minecraft/client/player/ClientInput;", ordinal = 0, shift = At.Shift.AFTER))
    private void bephax$multiplyInputAfterInputTick(CallbackInfo ci) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        NoSlow noSlow = Modules.get().get(NoSlow.class);
        if (!noSlow.isActive()) return;
        if (bephax$isGrimV3Enabled(noSlow)) {
            if (player.isUsingItem() && bephax$checkGrimV3Timing()) {
                float multiplier = bephax$getGrimV3Multiplier();
                InputAccessor inputAccessor = (InputAccessor) input;
                inputAccessor.setMovementForward(inputAccessor.getMovementForward() * multiplier);
                inputAccessor.setMovementSideways(inputAccessor.getMovementSideways() * multiplier);
            }
            return;
        }
        if (bephax$shouldMultiplyInput(noSlow)) {
            float multiplier = bephax$getInputMultiplier();
            InputAccessor inputAccessor = (InputAccessor) input;
            inputAccessor.setMovementForward(inputAccessor.getMovementForward() * multiplier);
            inputAccessor.setMovementSideways(inputAccessor.getMovementSideways() * multiplier);
        }
        if (noSlow.sneaking() && isCrouching()) {
            float sneakMultiplier = 1.0f / 0.3f;
            InputAccessor inputAccessor = (InputAccessor) input;
            inputAccessor.setMovementForward(inputAccessor.getMovementForward() * sneakMultiplier);
            inputAccessor.setMovementSideways(inputAccessor.getMovementSideways() * sneakMultiplier);
        }
    }
    @Inject(method = "aiStep", at = @At("TAIL")) // 26.1: tickMovement -> aiStep
    private void bephax$handleManualEatingAtTail(CallbackInfo ci) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        bephax$handleManualEating(player);
    }
    @Unique
    private boolean bephax$shouldMultiplyInput(NoSlow noSlow) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        if (player.isPassenger() || isCrouching()) return false;
        return isUsingItem() && noSlow.items();
    }
    @Unique
    private boolean bephax$isGrimV3Enabled(NoSlow noSlow) {
        try {
            var field = noSlow.getClass().getDeclaredField("bephax$grimV3Bypass");
            field.setAccessible(true);
            var setting = field.get(noSlow);
            var getMethod = setting.getClass().getMethod("get");
            Object value = getMethod.invoke(setting);
            return value instanceof Boolean && (Boolean) value;
        } catch (Exception e) {
            return false;
        }
    }
    @Unique
    private boolean bephax$checkGrimV3Timing() {
        LocalPlayer player = (LocalPlayer) (Object) this;
        if (player == null) return false;
        return (!player.isShiftKeyDown() && !player.isPassenger() && player.getUseItemRemainingTicks() < 5)
            || (player.getTicksUsingItem() > 1 && player.getTicksUsingItem() % 2 != 0);
    }
    @Unique
    private float bephax$getGrimV3Multiplier() {
        NoSlow noSlow = Modules.get().get(NoSlow.class);
        try {
            var field = noSlow.getClass().getDeclaredField("bephax$grimV3Multiplier");
            field.setAccessible(true);
            var setting = field.get(noSlow);
            var valueMethod = setting.getClass().getMethod("get");
            Object value = valueMethod.invoke(setting);
            if (value instanceof Number) {
                return ((Number) value).floatValue();
            }
        } catch (Exception e) {
        }
        return 5.0f;
    }
    @Unique
    private float bephax$getInputMultiplier() {
        NoSlow noSlow = Modules.get().get(NoSlow.class);
        try {
            var field = noSlow.getClass().getDeclaredField("bephax$inputMultiplier");
            field.setAccessible(true);
            var setting = field.get(noSlow);
            var valueMethod = setting.getClass().getMethod("get");
            Object value = valueMethod.invoke(setting);
            if (value instanceof Number) {
                return ((Number) value).floatValue();
            }
        } catch (Exception e) {
        }
        return 5.0f;
    }
    @Unique
    private boolean bephax$wasManuallyEating = false;
    @Unique
    private int bephax$lastManualEatingSlot = -1;
    @Unique
    private int bephax$lastItemUseTime = 0;
    @Unique
    private void bephax$checkStartEating(LocalPlayer player) {
        if (player == null) return;
        int currentUseTime = player.getTicksUsingItem();
        if (currentUseTime == 1 && bephax$lastItemUseTime == 0) {
            ItemStack activeStack = player.getActiveItem();
            if (!activeStack.isEmpty() && activeStack.get(DataComponents.FOOD) != null) {
                InventoryManager invManager = InventoryManager.getInstance();
                int currentSlot = ((PlayerInventoryAccessor) player.getInventory()).getSelectedSlot();
                int serverSlot = invManager.getServerSlot();
                if (serverSlot != currentSlot) {
                    invManager.setSlotForced(currentSlot);
                }
                invManager.setEating(true);
                bephax$wasManuallyEating = true;
                bephax$lastManualEatingSlot = currentSlot;
            }
        }
        bephax$lastItemUseTime = currentUseTime;
    }
    @Unique
    private void bephax$handleManualEating(LocalPlayer player) {
        if (player == null) return;
        boolean isEatingNow = bephax$isManuallyEatingFood(player);
        if (!isEatingNow && bephax$wasManuallyEating) {
            bephax$wasManuallyEating = false;
            InventoryManager.getInstance().setEating(false);
            bephax$lastManualEatingSlot = -1;
        }
    }
    @Unique
    private boolean bephax$isManuallyEatingFood(LocalPlayer player) {
        if (!player.isUsingItem()) return false;
        ItemStack stack = player.getActiveItem();
        if (stack.isEmpty()) return false;
        return stack.get(DataComponents.FOOD) != null;
    }
}