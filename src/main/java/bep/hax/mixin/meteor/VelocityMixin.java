package bep.hax.mixin.meteor;
import bep.hax.mixin.accessor.BundleS2CPacketAccessor;
import bep.hax.mixin.accessor.ExplosionS2CPacketAccessor;
import bep.hax.mixin.accessor.AccessorClientWorld;
import bep.hax.mixin.accessor.EntityVelocityUpdateS2CPacketAccessor;
import bep.hax.util.RotationUtils;
import bep.hax.util.PushEntityEvent;
import bep.hax.util.PushOutOfBlocksEvent;
import bep.hax.util.PushFluidsEvent;
import bep.hax.util.InventoryManager;
import bep.hax.util.InventoryManager.VelocityMode;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.movement.Velocity;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.network.protocol.BundlePacket;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.*;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
@Mixin(value = Velocity.class, remap = false)
public abstract class VelocityMixin extends Module {
    public VelocityMixin(Category category, String name, String description) {
        super(category, name, description);
    }
    @Shadow @Final private SettingGroup sgGeneral;
    @Shadow @Final public Setting<Boolean> knockback;
    @Shadow @Final public Setting<Double> knockbackHorizontal;
    @Shadow @Final public Setting<Double> knockbackVertical;
    @Shadow @Final public Setting<Boolean> explosions;
    @Shadow @Final public Setting<Double> explosionsHorizontal;
    @Shadow @Final public Setting<Double> explosionsVertical;
    @Unique private SettingGroup bephax$sgAdvancedModes;
    @Unique private Setting<VelocityMode> bephax$mode;
    @Unique private Setting<Boolean> bephax$conceal;
    @Unique private Setting<Boolean> bephax$wallsGroundOnly;
    @Unique private Setting<Boolean> bephax$wallsTrapped;
    @Unique private Setting<Boolean> bephax$pushEntities;
    @Unique private Setting<Boolean> bephax$pushBlocks;
    @Unique private Setting<Boolean> bephax$pushLiquids;
    @Unique private Setting<Boolean> bephax$pushFishhook;
    @Unique private RotationUtils bephax$rotationManager;
    @Unique private InventoryManager bephax$inventoryManager;
    @Unique private boolean bephax$cancelVelocity = false;
    @Unique private boolean bephax$concealVelocity = false;
    @Unique private static final Random RANDOM = new Random();
    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        bephax$rotationManager = RotationUtils.getInstance();
        bephax$inventoryManager = InventoryManager.getInstance();
        bephax$sgAdvancedModes = settings.createGroup("Advanced Modes");
        bephax$mode = bephax$sgAdvancedModes.add(new EnumSetting.Builder<VelocityMode>()
            .name("mode")
            .description("Velocity mode (NORMAL = Meteor default, WALLS = only when phased, GRIM/GRIM_V3 = 2b2t bypass)")
            .defaultValue(VelocityMode.NORMAL)
            .build()
        );
        bephax$conceal = bephax$sgAdvancedModes.add(new BoolSetting.Builder()
            .name("conceal")
            .description("Fixes velocity on servers with excessive setbacks")
            .defaultValue(false)
            .build()
        );
        bephax$wallsGroundOnly = bephax$sgAdvancedModes.add(new BoolSetting.Builder()
            .name("ground-only")
            .description("Only applies velocity in walls while on ground")
            .defaultValue(false)
            .visible(() -> bephax$mode.get() == VelocityMode.WALLS)
            .build()
        );
        bephax$wallsTrapped = bephax$sgAdvancedModes.add(new BoolSetting.Builder()
            .name("walls-trapped")
            .description("Applies velocity while player head is trapped in blocks")
            .defaultValue(false)
            .visible(() -> bephax$mode.get() == VelocityMode.WALLS)
            .build()
        );
        bephax$pushEntities = bephax$sgAdvancedModes.add(new BoolSetting.Builder()
            .name("nopush-entities")
            .description("Prevents being pushed away from entities")
            .defaultValue(true)
            .build()
        );
        bephax$pushBlocks = bephax$sgAdvancedModes.add(new BoolSetting.Builder()
            .name("nopush-blocks")
            .description("Prevents being pushed out of blocks (WARNING: Can make you stuck inside blocks)")
            .defaultValue(false)
            .build()
        );
        bephax$pushLiquids = bephax$sgAdvancedModes.add(new BoolSetting.Builder()
            .name("nopush-liquids")
            .description("Prevents being pushed by flowing liquids")
            .defaultValue(true)
            .build()
        );
        bephax$pushFishhook = bephax$sgAdvancedModes.add(new BoolSetting.Builder()
            .name("nopush-fishhook")
            .description("Prevents being pulled by fishing rod hooks")
            .defaultValue(true)
            .build()
        );
    }
    @Override
    public void onActivate() {
        bephax$cancelVelocity = false;
        bephax$concealVelocity = false;
    }
    @Override
    public void onDeactivate() {
        if (bephax$cancelVelocity && bephax$mode.get() == VelocityMode.GRIM) {
            bephax$sendGrimBypass();
        }
        bephax$cancelVelocity = false;
        bephax$concealVelocity = false;
    }
    @Inject(method = "onPacketReceive", at = @At("HEAD"), cancellable = true)
    private void cancelMeteorHandler(PacketEvent.Receive event, CallbackInfo ci) {
        if (bephax$mode.get() != VelocityMode.NORMAL) {
            ci.cancel();
        }
    }
    @Unique
    @EventHandler(priority = EventPriority.HIGH)
    private void onReceivePacket(PacketEvent.Receive event) {
        if (mc.player == null || mc.level == null) return;
        if (event.packet instanceof ClientboundPlayerPositionPacket && bephax$conceal.get()) {
            bephax$concealVelocity = true;
        }
        if (event.packet instanceof ClientboundSetEntityMotionPacket packet && knockback.get()) {
            EntityVelocityUpdateS2CPacketAccessor accessor = (EntityVelocityUpdateS2CPacketAccessor) packet;
            if (accessor.getEntityId() != mc.player.getId()) return;
            Vec3 velocity = accessor.getVelocity();
            if (bephax$concealVelocity && velocity.x == 0 && velocity.y == 0 && velocity.z == 0) {
                bephax$concealVelocity = false;
                return;
            }
            if (bephax$mode.get() == VelocityMode.WALLS) {
                if (!bephax$isPhased() && (!bephax$wallsTrapped.get() || !bephax$isWallsTrapped())) {
                    return;
                }
                if (bephax$wallsGroundOnly.get() && !mc.player.onGround()) {
                    return;
                }
            }
            switch (bephax$mode.get()) {
                case NORMAL, WALLS -> {
                    if (knockbackHorizontal.get() == 0.0 && knockbackVertical.get() == 0.0) {
                        event.cancel();
                        return;
                    }
                    double hMult = knockbackHorizontal.get() / 100.0;
                    double vMult = knockbackVertical.get() / 100.0;
                    Vec3 modifiedVelocity = new Vec3(
                        velocity.x * hMult,
                        velocity.y * vMult,
                        velocity.z * hMult
                    );
                    ((meteordevelopment.meteorclient.mixin.EntityVelocityUpdateS2CPacketAccessor) packet).meteor$setVelocity(modifiedVelocity);
                }
                case GRIM -> {
                    if (!bephax$inventoryManager.hasPassed(100)) {
                        return;
                    }
                    event.cancel();
                    bephax$cancelVelocity = true;
                }
                case GRIM_V3 -> {
                    if (bephax$isPhased()) {
                        event.cancel();
                    }
                }
            }
        }
        else if (event.packet instanceof ClientboundExplodePacket packet && explosions.get()) {
            if (bephax$mode.get() == VelocityMode.WALLS && !bephax$isPhased()) {
                return;
            }
            switch (bephax$mode.get()) {
                case NORMAL, WALLS -> {
                    if (explosionsHorizontal.get() == 0.0 && explosionsVertical.get() == 0.0) {
                        event.cancel();
                    }
                }
                case GRIM -> {
                    if (!bephax$inventoryManager.hasPassed(100)) {
                        return;
                    }
                    event.cancel();
                    bephax$cancelVelocity = true;
                }
                case GRIM_V3 -> {
                    if (bephax$isPhased()) {
                        event.cancel();
                    }
                }
            }
            if (event.isCancelled()) {
                ClientboundExplodePacket explosionPacket = packet;
                Vec3 center = ((ExplosionS2CPacketAccessor) (Object) explosionPacket).getCenter();
                mc.executeIfPossible(() -> ((AccessorClientWorld) mc.level).hookPlaySound(center.x, center.y, center.z,
                    SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS,
                    4.0f, (1.0f + (RANDOM.nextFloat() - RANDOM.nextFloat()) * 0.2f) * 0.7f, false, RANDOM.nextLong()));
            }
        }
        else if (event.packet instanceof BundlePacket bundlePacket) {
            bephax$handleBundlePacket(event, bundlePacket);
        }
        else if (event.packet instanceof ClientboundDamageEventPacket packet
            && packet.entityId() == mc.player.getId()
            && bephax$mode.get() == VelocityMode.GRIM_V3
            && bephax$isPhased()) {
            mc.getConnection().send(new ServerboundMovePlayerPacket.StatusOnly(false, false));
            mc.getConnection().send(new ServerboundMovePlayerPacket.StatusOnly(true, false));
        }
        else if (event.packet instanceof ClientboundEntityEventPacket packet
            && packet.getEventId() == EntityEvent.FISHING_ROD_REEL_IN
            && bephax$pushFishhook.get()) {
            Entity entity = packet.getEntity(mc.level);
            if (entity instanceof FishingHook hook && hook.getHookedIn() == mc.player) {
                event.cancel();
            }
        }
    }
    @Unique
    private void bephax$handleBundlePacket(PacketEvent.Receive event, BundlePacket bundlePacket) {
        List<Packet<?>> allowedBundle = new ArrayList<>();
        for (Object subPacketObj : bundlePacket.subPackets()) {
            if (!(subPacketObj instanceof Packet<?> subPacket)) {
                continue;
            }
            if (subPacket instanceof ClientboundExplodePacket packet && explosions.get()) {
                if (bephax$mode.get() == VelocityMode.WALLS && !bephax$isPhased()) {
                    allowedBundle.add(subPacket);
                    continue;
                }
                boolean shouldCancel = false;
                switch (bephax$mode.get()) {
                    case NORMAL, WALLS -> {
                        if (explosionsHorizontal.get() == 0.0 && explosionsVertical.get() == 0.0) {
                            shouldCancel = true;
                        }
                    }
                    case GRIM -> {
                        if (bephax$inventoryManager.hasPassed(100)) {
                            bephax$cancelVelocity = true;
                            shouldCancel = true;
                        }
                    }
                    case GRIM_V3 -> {
                        if (bephax$isPhased()) {
                            shouldCancel = true;
                        }
                    }
                }
                if (shouldCancel) {
                    ClientboundExplodePacket explosionPacket = packet;
                    Vec3 center = ((ExplosionS2CPacketAccessor) (Object) explosionPacket).getCenter();
                    mc.executeIfPossible(() -> ((AccessorClientWorld) mc.level).hookPlaySound(center.x, center.y, center.z,
                        SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS,
                        4.0f, (1.0f + (RANDOM.nextFloat() - RANDOM.nextFloat()) * 0.2f) * 0.7f, false, RANDOM.nextLong()));
                    continue;
                }
                allowedBundle.add(subPacket);
            }
            else if (subPacket instanceof ClientboundSetEntityMotionPacket packet && knockback.get()) {
                EntityVelocityUpdateS2CPacketAccessor accessor = (EntityVelocityUpdateS2CPacketAccessor) packet;
                if (accessor.getEntityId() != mc.player.getId()) {
                    allowedBundle.add(subPacket);
                    continue;
                }
                Vec3 velocity = accessor.getVelocity();
                if (bephax$mode.get() == VelocityMode.WALLS) {
                    if (!bephax$isPhased() && (!bephax$wallsTrapped.get() || !bephax$isWallsTrapped())) {
                        allowedBundle.add(subPacket);
                        continue;
                    }
                    if (bephax$wallsGroundOnly.get() && !mc.player.onGround()) {
                        allowedBundle.add(subPacket);
                        continue;
                    }
                }
                switch (bephax$mode.get()) {
                    case NORMAL, WALLS -> {
                        if (knockbackHorizontal.get() == 0.0 && knockbackVertical.get() == 0.0) {
                            continue;
                        } else {
                            double hMult = knockbackHorizontal.get() / 100.0;
                            double vMult = knockbackVertical.get() / 100.0;
                            Vec3 modifiedVelocity = new Vec3(
                                velocity.x * hMult,
                                velocity.y * vMult,
                                velocity.z * hMult
                            );
                            ((meteordevelopment.meteorclient.mixin.EntityVelocityUpdateS2CPacketAccessor) packet).meteor$setVelocity(modifiedVelocity);
                            allowedBundle.add(subPacket);
                        }
                    }
                    case GRIM -> {
                        if (!bephax$inventoryManager.hasPassed(100)) {
                            allowedBundle.add(subPacket);
                            continue;
                        }
                        bephax$cancelVelocity = true;
                        continue;
                    }
                    case GRIM_V3 -> {
                        if (bephax$isPhased()) {
                            continue;
                        }
                        allowedBundle.add(subPacket);
                    }
                }
            } else {
                allowedBundle.add(subPacket);
            }
        }
        ((BundleS2CPacketAccessor) bundlePacket).setPackets(allowedBundle);
    }
    @Unique
    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.level == null) return;
        bephax$concealVelocity = false;
        if (bephax$cancelVelocity && bephax$mode.get() == VelocityMode.GRIM) {
            bephax$sendGrimBypass();
            bephax$cancelVelocity = false;
        }
    }
    @Unique
    @EventHandler
    private void onPushEntity(PushEntityEvent event) {
        if (bephax$pushEntities.get() && event.getPushed().equals(mc.player)) {
            event.cancel();
        }
    }
    @Unique
    @EventHandler
    private void onPushOutOfBlocks(PushOutOfBlocksEvent event) {
        if (bephax$pushBlocks.get()) {
            event.cancel();
        }
    }
    @Unique
    @EventHandler
    private void onPushFluids(PushFluidsEvent event) {
        if (bephax$pushLiquids.get()) {
            event.cancel();
        }
    }
    @Unique
    private void bephax$sendGrimBypass() {
        if (mc.player == null || mc.getConnection() == null) return;
        float yaw = bephax$rotationManager.getServerYaw();
        float pitch = bephax$rotationManager.getServerPitch();
        if (bephax$rotationManager.isRotating()) {
            yaw = bephax$rotationManager.getRotationYaw();
            pitch = bephax$rotationManager.getRotationPitch();
        }
        mc.getConnection().send(new ServerboundMovePlayerPacket.PosRot(
            mc.player.getX(),
            mc.player.getY(),
            mc.player.getZ(),
            yaw,
            pitch,
            mc.player.onGround(),
            false
        ));
        mc.getConnection().send(new ServerboundPlayerActionPacket(
            ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK,
            mc.player.isVisuallyCrawling() ? mc.player.blockPosition() : mc.player.blockPosition().above(),
            Direction.DOWN
        ));
    }
    @Unique
    private boolean bephax$isWallsTrapped() {
        if (mc.player == null || mc.level == null) return false;
        BlockPos headPos = mc.player.blockPosition().above(mc.player.isVisuallyCrawling() ? 1 : 2);
        if (mc.level.getBlockState(headPos).canBeReplaced()) {
            return false;
        }
        List<BlockPos> surroundPos = bephax$getSurroundNoDown(mc.player.blockPosition());
        return surroundPos.stream()
            .noneMatch(blockPos -> mc.level.getBlockState(mc.player.isVisuallyCrawling() ? blockPos : blockPos.above()).canBeReplaced());
    }
    @Unique
    private List<BlockPos> bephax$getSurroundNoDown(BlockPos center) {
        List<BlockPos> positions = new ArrayList<>();
        positions.add(center.north());
        positions.add(center.south());
        positions.add(center.east());
        positions.add(center.west());
        return positions;
    }
    @Unique
    private boolean bephax$isPhased() {
        if (mc.player == null || mc.level == null) return false;
        return bep.hax.util.PositionUtil.getAllInBox(mc.player.getBoundingBox()).stream()
            .anyMatch(blockPos -> !mc.level.getBlockState(blockPos).canBeReplaced());
    }
}