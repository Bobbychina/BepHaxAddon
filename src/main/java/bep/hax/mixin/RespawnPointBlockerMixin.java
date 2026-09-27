package bep.hax.mixin;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.systems.modules.Modules;
import bep.hax.modules.RespawnPointBlocker;
import net.minecraft.world.level.block.Block;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(MultiPlayerGameMode.class)
public class RespawnPointBlockerMixin {
    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true) // 26.1: interactBlock -> useItemOn
    private void onInteractBlock(LocalPlayer player, InteractionHand hand, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        RespawnPointBlocker module = Modules.get().get(RespawnPointBlocker.class);
        if (!module.isActive()) return;
        BlockPos blockPos = hitResult.getBlockPos();
        BlockState blockState = MeteorClient.mc.level.getBlockState(blockPos);
        Block block = blockState.getBlock();
        if (isRespawnPointBlock(block)) {
            boolean shouldBlock = false;
            if (isBed(block) && module.blockBeds.get()) {
                shouldBlock = true;
            } else if (block == Blocks.RESPAWN_ANCHOR && module.blockRespawnAnchors.get()) {
                shouldBlock = true;
            }
            if (shouldBlock) {
                cir.setReturnValue(InteractionResult.FAIL);
                provideFeedback(module, block);
            }
        }
    }
    private boolean isRespawnPointBlock(Block block) {
        return isBed(block) || block == Blocks.RESPAWN_ANCHOR;
    }
    private boolean isBed(Block block) {
        return block == Blocks.WHITE_BED || block == Blocks.ORANGE_BED || block == Blocks.MAGENTA_BED ||
               block == Blocks.LIGHT_BLUE_BED || block == Blocks.YELLOW_BED || block == Blocks.LIME_BED ||
               block == Blocks.PINK_BED || block == Blocks.GRAY_BED || block == Blocks.LIGHT_GRAY_BED ||
               block == Blocks.CYAN_BED || block == Blocks.PURPLE_BED || block == Blocks.BLUE_BED ||
               block == Blocks.BROWN_BED || block == Blocks.GREEN_BED || block == Blocks.RED_BED ||
               block == Blocks.BLACK_BED;
    }
    private void provideFeedback(RespawnPointBlocker module, Block block) {
        String blockName = isBed(block) ? "Bed" : "Respawn Anchor";
        if (module.chatFeedback.get()) {
            module.info("Blocked %s interaction", blockName);
        }
        if (module.soundFeedback.get() && !module.feedbackSound.get().isEmpty()) {
            SoundEvent sound = module.feedbackSound.get().get(0);
            float volume = module.soundVolume.get() / 100.0f;
            MeteorClient.mc.getSoundManager().play(SimpleSoundInstance.forUI(sound, volume, 1.0f));
        }
    }
}