package bep.hax.modules;
import bep.hax.Bep;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import bep.hax.util.InventoryManager;
import bep.hax.util.InventoryManager.IPlayerInteractEntityC2SPacket;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.InteractionHand;
import bep.hax.modules.PVPModule;
import bep.hax.util.CacheTimer;
import bep.hax.util.EntityUtil;
import bep.hax.util.MovementUtil;
import bep.hax.util.PlacementUtils;
import static meteordevelopment.meteorclient.MeteorClient.mc;
public class Criticals extends PVPModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Boolean> multitask = sgGeneral.add(new BoolSetting.Builder()
        .name("multitask")
        .description("Allows crits when other combat modules are enabled")
        .defaultValue(true)
        .build()
    );
    private final Setting<CritMode> mode = sgGeneral.add(new EnumSetting.Builder<CritMode>()
        .name("mode")
        .description("Mode for critical attack modifier")
        .defaultValue(CritMode.PACKET)
        .build()
    );
    private final Setting<Boolean> phaseOnly = sgGeneral.add(new BoolSetting.Builder()
        .name("phase-only")
        .description("Only attempts criticals when phased")
        .defaultValue(false)
        .visible(() -> mode.get() == CritMode.GRIM_V3 || mode.get() == CritMode.GRIM)
        .build()
    );
    private final Setting<Boolean> wallsOnly = sgGeneral.add(new BoolSetting.Builder()
        .name("walls-only")
        .description("Only attempts criticals in walls")
        .defaultValue(false)
        .visible(() -> (mode.get() == CritMode.GRIM_V3 || mode.get() == CritMode.GRIM) && phaseOnly.get())
        .build()
    );
    private final Setting<Boolean> moveFix = sgGeneral.add(new BoolSetting.Builder()
        .name("move-fix")
        .description("Pauses crits when moving")
        .defaultValue(false)
        .visible(() -> mode.get() == CritMode.GRIM_V3 || mode.get() == CritMode.GRIM)
        .build()
    );
    private final CacheTimer attackTimer = new CacheTimer();
    private boolean postUpdateGround;
    private boolean postUpdateSprint;
    public Criticals() {
        super(Bep.CATEGORY, "criticals", "Modifies attacks to always land critical hits");
    }
    @Override
    public void onDeactivate() {
        postUpdateGround = false;
        postUpdateSprint = false;
    }
    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (mc.player == null || mc.level == null) return;
        if (isOtherCombatActive()) return;
        if (event.packet instanceof ServerboundInteractPacket packet) {
            IPlayerInteractEntityC2SPacket accessor = (IPlayerInteractEntityC2SPacket) packet;
            if (!accessor.isAttackPacket()) return;
            Entity target = null;
            if (mc.level != null) {
                int entityId = accessor.getTargetEntityId();
                for (Entity entity : mc.level.entitiesForRendering()) {
                    if (entity.getId() == entityId) {
                        target = entity;
                        break;
                    }
                }
            }
            if (!isValidTarget(target)) return;
            if (EntityUtil.isVehicle(target)) {
                handleVehicleAttack(target);
                return;
            }
            postUpdateSprint = mc.player.isSprinting();
            if (postUpdateSprint) {
                mc.getConnection().send(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.STOP_SPRINTING));
            }
            performCriticalAttack(target);
        }
    }
    @EventHandler
    private void onSentPacket(PacketEvent.Sent event) {
        if (mc.player == null) return;
        if (event.packet instanceof ServerboundInteractPacket) {
            if (postUpdateGround) {
                mc.getConnection().send(new ServerboundMovePlayerPacket.Pos(
                    mc.player.getX(), mc.player.getY(), mc.player.getZ(), false, false
                ));
                postUpdateGround = false;
            }
            if (postUpdateSprint) {
                mc.getConnection().send(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.START_SPRINTING));
                postUpdateSprint = false;
            }
        }
    }
    private boolean isOtherCombatActive() {
        if (!multitask.get()) {
            return false;
        }
        return false;
    }
    private boolean isValidTarget(Entity entity) {
        if (entity == null || !entity.isAlive() || !(entity instanceof LivingEntity)) {
            return false;
        }
        return !(mc.player.isHandsBusy() ||
            mc.player.isFallFlying() ||
            mc.player.isInWater() ||
            mc.player.isInLava() ||
            mc.player.isSuppressingSlidingDownLadder() ||
            mc.player.hasEffect(MobEffects.BLINDNESS) ||
            InventoryManager.isHolding32k());
    }
    private void handleVehicleAttack(Entity target) {
        if (mode.get() == CritMode.PACKET) {
            for (int i = 0; i < 5; i++) {
                mc.getConnection().send(ServerboundInteractPacket.createAttackPacket(target, mc.player.isShiftKeyDown()));
                mc.getConnection().send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
            }
        }
    }
    private void performCriticalAttack(Entity target) {
        double x = mc.player.getX();
        double y = mc.player.getY();
        double z = mc.player.getZ();
        switch (mode.get()) {
            case VANILLA -> {
                if (mc.player.onGround() && !mc.options.keyJump.isDown()) {
                    double d = 1.0e-7 + 1.0e-7 * (1.0 + RANDOM.nextInt(RANDOM.nextBoolean() ? 34 : 43));
                    mc.getConnection().send(new ServerboundMovePlayerPacket.Pos(
                        x, y + 0.1016f + d * 3.0f, z, false, false));
                    mc.getConnection().send(new ServerboundMovePlayerPacket.Pos(
                        x, y + 0.0202f + d * 2.0f, z, false, false));
                    mc.getConnection().send(new ServerboundMovePlayerPacket.Pos(
                        x, y + 3.239e-4 + d, z, false, false));
                    mc.player.crit(target);
                }
            }
            case PACKET -> {
                if (mc.player.onGround() && !mc.options.keyJump.isDown()) {
                    mc.getConnection().send(new ServerboundMovePlayerPacket.Pos(
                        x, y + 0.0625f, z, false, false));
                    mc.getConnection().send(new ServerboundMovePlayerPacket.Pos(
                        x, y, z, false, false));
                    mc.player.crit(target);
                }
            }
            case PACKET_STRICT -> {
                if (attackTimer.passed(500) && mc.player.onGround() && !mc.options.keyJump.isDown()) {
                    mc.getConnection().send(new ServerboundMovePlayerPacket.Pos(
                        x, y + 1.1e-7f, z, false, false));
                    mc.getConnection().send(new ServerboundMovePlayerPacket.Pos(
                        x, y + 1.0e-8f, z, false, false));
                    postUpdateGround = true;
                    attackTimer.reset();
                }
            }
            case GRIM -> {
                if (phaseOnly.get() && (wallsOnly.get() ? !PlacementUtils.isDoublePhased() : !PlacementUtils.isPhased())) {
                    return;
                }
                if (moveFix.get() && MovementUtil.isMovingInput()) {
                    return;
                }
                if (attackTimer.passed(250) && mc.player.onGround() && !mc.player.isVisuallyCrawling()) {
                    float yaw = mc.player.getYRot();
                    float pitch = mc.player.getXRot();
                    mc.getConnection().send(new ServerboundMovePlayerPacket.PosRot(
                        x, y + 0.0625, z, yaw, pitch, false, false));
                    mc.getConnection().send(new ServerboundMovePlayerPacket.PosRot(
                        x, y + 0.0625013579, z, yaw, pitch, false, false));
                    mc.getConnection().send(new ServerboundMovePlayerPacket.PosRot(
                        x, y + 1.3579e-6, z, yaw, pitch, false, false));
                    attackTimer.reset();
                }
            }
            case GRIM_V3 -> {
                if (phaseOnly.get() && (wallsOnly.get() ? !PlacementUtils.isDoublePhased() : !PlacementUtils.isPhased())) {
                    return;
                }
                if (moveFix.get() && MovementUtil.isMovingInput()) {
                    return;
                }
                if (mc.player.onGround() && !mc.player.isVisuallyCrawling()) {
                    float yaw = mc.player.getYRot();
                    float pitch = mc.player.getXRot();
                    mc.getConnection().send(new ServerboundMovePlayerPacket.PosRot(
                        x, y, z, yaw, pitch, true, false));
                    mc.getConnection().send(new ServerboundMovePlayerPacket.PosRot(
                        x, y + 0.0625f, z, yaw, pitch, false, false));
                    mc.getConnection().send(new ServerboundMovePlayerPacket.PosRot(
                        x, y + 0.04535f, z, yaw, pitch, false, false));
                }
            }
            case LOW_HOP -> {
                mc.player.setDeltaMovement(mc.player.getDeltaMovement().x, 0.3425, mc.player.getDeltaMovement().z);
            }
        }
    }
    public enum CritMode {
        PACKET("Packet"),
        PACKET_STRICT("Packet Strict"),
        VANILLA("Vanilla"),
        GRIM("Grim"),
        GRIM_V3("Grim V3"),
        LOW_HOP("Low Hop");
        private final String displayName;
        CritMode(String displayName) {
            this.displayName = displayName;
        }
        @Override
        public String toString() {
            return displayName;
        }
    }
}