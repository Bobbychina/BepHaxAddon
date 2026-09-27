package bep.hax.mixin;
import bep.hax.accessor.InputAccessor;
import bep.hax.mixin.accessor.PlayerInventoryAccessor;
import bep.hax.util.InventoryManager;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;
import bep.hax.modules.RocketMan;
import net.minecraft.sounds.Music;
import bep.hax.modules.MusicTweaks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import net.minecraft.util.Mth;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import net.minecraft.client.player.LocalPlayer;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(Minecraft.class)
public class MinecraftClientMixin {
    @Shadow
    public LocalPlayer player;
    @Unique
    private long lastFrameTime = System.nanoTime();
    @Unique
    private void changeLookDirection(LocalPlayer player, double cursorDeltaX, double cursorDeltaY) {
        float f = (float) cursorDeltaY * 0.15F;
        float g = (float) cursorDeltaX * 0.15F;
        player.setYRot(player.getYRot() + g);
        player.setXRot(Mth.clamp(player.getXRot() + f, -90.0F, 90.0F));
    }
    @Inject(method = "render", at = @At("HEAD"))
    private void mixinRender(CallbackInfo ci) {
        long currentTime = System.nanoTime();
        float deltaTime = (currentTime - lastFrameTime) / 10000000f;
        Modules modules = Modules.get();
        if (modules == null ) return;
        RocketMan rocketMan = modules.get(RocketMan.class);
        if (!rocketMan.isActive() || !rocketMan.shouldTickRotation()) return;
        Minecraft mc = rocketMan.getClientInstance();
        if (mc.player == null) return;
        if (!rocketMan.hoverMode.get().equals(RocketMan.HoverMode.Off)) {
            if (mc.player.input.keyPresses.shift() && !rocketMan.shouldLockYLevel() && !rocketMan.isHovering) {
                changeLookDirection(mc.player, 0.0f, rocketMan.getPitchSpeed() * deltaTime);
            } else if (mc.player.input.keyPresses.jump() && !rocketMan.shouldLockYLevel() && !rocketMan.isHovering) {
                changeLookDirection(mc.player, 0.0f, -rocketMan.getPitchSpeed() * deltaTime);
            } else if (Input.isKeyPressed(GLFW.GLFW_KEY_UP)) {
                changeLookDirection(mc.player, 0.0f, -rocketMan.getPitchSpeed() * deltaTime);
            } else if (Input.isKeyPressed(GLFW.GLFW_KEY_DOWN)) {
                changeLookDirection(mc.player, 0.0f, rocketMan.getPitchSpeed() * deltaTime);
            }
        } else {
            boolean inverted = rocketMan.shouldInvertPitch();
            RocketMan.RocketMode mode = rocketMan.usageMode.get();
            switch (mode) {
                case OnKey -> {
                    if (mc.player.input.keyPresses.shift()) {
                        changeLookDirection(mc.player, 0.0f, rocketMan.getPitchSpeed() * deltaTime);
                    } else if (mc.player.input.keyPresses.jump()) {
                        changeLookDirection(mc.player, 0.0f, -rocketMan.getPitchSpeed() * deltaTime);
                    } else if (Input.isKeyPressed(GLFW.GLFW_KEY_UP)) {
                        changeLookDirection(mc.player, 0.0f, -rocketMan.getPitchSpeed() * deltaTime);
                    } else if (Input.isKeyPressed(GLFW.GLFW_KEY_DOWN)) {
                        changeLookDirection(mc.player, 0.0f, rocketMan.getPitchSpeed() * deltaTime);
                    }
                }
                case Static, Dynamic -> {
                    if (inverted) {
                        if ((mc.player.input.keyPresses.forward() || mc.player.input.keyPresses.shift()) && !rocketMan.shouldLockYLevel()) {
                            changeLookDirection(mc.player, 0.0f, rocketMan.getPitchSpeed() * deltaTime);
                        } else if ((mc.player.input.keyPresses.backward() || mc.player.input.keyPresses.jump()) && !rocketMan.shouldLockYLevel()) {
                            changeLookDirection(mc.player, 0.0f, -rocketMan.getPitchSpeed() * deltaTime);
                        } else if (Input.isKeyPressed(GLFW.GLFW_KEY_DOWN)) {
                            changeLookDirection(mc.player, 0.0f, -rocketMan.getPitchSpeed() * deltaTime);
                        } else if (Input.isKeyPressed(GLFW.GLFW_KEY_UP)) {
                            changeLookDirection(mc.player, 0.0f, rocketMan.getPitchSpeed() * deltaTime);
                        }
                    } else {
                        if ((mc.player.input.keyPresses.backward() || mc.player.input.keyPresses.shift()) && !rocketMan.shouldLockYLevel()) {
                            changeLookDirection(mc.player, 0.0f, rocketMan.getPitchSpeed() * deltaTime);
                        } else if ((mc.player.input.keyPresses.forward() || mc.player.input.keyPresses.jump()) && !rocketMan.shouldLockYLevel()) {
                            changeLookDirection(mc.player, 0.0f, -rocketMan.getPitchSpeed() * deltaTime);
                        }else if (Input.isKeyPressed(GLFW.GLFW_KEY_UP)) {
                            changeLookDirection(mc.player, 0.0f, -rocketMan.getPitchSpeed() * deltaTime);
                        } else if (Input.isKeyPressed(GLFW.GLFW_KEY_DOWN)) {
                            changeLookDirection(mc.player, 0.0f, rocketMan.getPitchSpeed() * deltaTime);
                        }
                    }
                }
            }
        }
        if (mc.player.input.keyPresses.right() && !rocketMan.isHovering) {
            changeLookDirection(mc.player, rocketMan.getYawSpeed() * deltaTime, 0.0f);
        } else if (mc.player.input.keyPresses.left() && !rocketMan.isHovering) {
            changeLookDirection(mc.player, -rocketMan.getYawSpeed() * deltaTime, 0.0f);
        } else if (Input.isKeyPressed(GLFW.GLFW_KEY_RIGHT)) {
            changeLookDirection(mc.player, rocketMan.getYawSpeed() * deltaTime, 0.0f);
        } else if (Input.isKeyPressed(GLFW.GLFW_KEY_LEFT)) {
            changeLookDirection(mc.player, -rocketMan.getYawSpeed() * deltaTime, 0.0f);
        }
        lastFrameTime = currentTime;
    }
    @Inject(method = "doItemUse", at = @At("HEAD"))
    private void onDoItemUse(CallbackInfo ci) {
        if (player == null) return;
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        boolean mainHandIsFood = !mainHand.isEmpty() && mainHand.get(DataComponents.FOOD) != null;
        boolean offHandIsFood = !offHand.isEmpty() && offHand.get(DataComponents.FOOD) != null;
        if (mainHandIsFood || offHandIsFood) {
            InventoryManager invManager = InventoryManager.getInstance();
            int currentSlot = ((PlayerInventoryAccessor) player.getInventory()).getSelectedSlot();
            int serverSlot = invManager.getServerSlot();
            if (serverSlot != currentSlot) {
                invManager.setSlotForced(currentSlot);
            }
            invManager.setEating(true);
        }
    }
    @Inject(method = "getMusicInstance", at = @At("HEAD"), cancellable = true)
    public void mixinGetMusicType(CallbackInfoReturnable<Music> cir) {
        Modules modules = Modules.get();
        if (modules == null ) return;
        MusicTweaks tweaks = modules.get(MusicTweaks.class);
        if (tweaks == null || !tweaks.isActive()) return;
        Music type = tweaks.getType();
        if (type != null) {
            cir.setReturnValue(type);
        }
    }
}