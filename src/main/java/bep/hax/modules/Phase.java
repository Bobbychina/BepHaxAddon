package bep.hax.modules;
import bep.hax.mixin.accessor.PlayerInventoryAccessor;
import bep.hax.Bep;
import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.events.world.CollisionShapeEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.ScaffoldingBlock;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.Items;
import net.minecraft.network.protocol.game.*;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.*;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import bep.hax.util.PushOutOfBlocksEvent;
import bep.hax.util.RotationUtils;
import bep.hax.util.PlacementUtils;
import bep.hax.util.RotationUtils;
import bep.hax.util.InventoryManager;
import meteordevelopment.meteorclient.utils.render.Box;
import net.minecraft.world.phys.Vec3;
public class Phase extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgPearl = settings.createGroup("Pearl");
    private final SettingGroup sgClipping = settings.createGroup("Clipping");
    private final Setting<PhaseMode> mode = sgGeneral.add(new EnumSetting.Builder<PhaseMode>()
        .name("mode")
        .description("The phase mode for clipping into blocks.")
        .defaultValue(PhaseMode.Pearl)
        .build()
    );
    private final Setting<Integer> pitch = sgPearl.add(new IntSetting.Builder()
        .name("pitch")
        .description("The pitch angle to throw pearls.")
        .defaultValue(85)
        .range(70, 90)
        .visible(() -> mode.get() == PhaseMode.Pearl)
        .build()
    );
    private final Setting<Boolean> swapAlternative = sgPearl.add(new BoolSetting.Builder()
        .name("swap-alternative")
        .description("Uses inventory swap for swapping to pearls.")
        .defaultValue(true)
        .visible(() -> mode.get() == PhaseMode.Pearl)
        .build()
    );
    private final Setting<Boolean> attack = sgPearl.add(new BoolSetting.Builder()
        .name("attack")
        .description("Attacks entities in the way of the pearl phase.")
        .defaultValue(false)
        .visible(() -> mode.get() == PhaseMode.Pearl)
        .build()
    );
    private final Setting<Boolean> swing = sgPearl.add(new BoolSetting.Builder()
        .name("swing")
        .description("Swings the hand when throwing pearls.")
        .defaultValue(true)
        .visible(() -> mode.get() == PhaseMode.Pearl)
        .build()
    );
    private final Setting<Boolean> selfFill = sgPearl.add(new BoolSetting.Builder()
        .name("self-fill")
        .description("Automatically fills blocks you are phasing on.")
        .defaultValue(false)
        .visible(() -> mode.get() == PhaseMode.Pearl)
        .build()
    );
    private final Setting<Double> blocks = sgClipping.add(new DoubleSetting.Builder()
        .name("blocks")
        .description("The block distance to phase clip.")
        .defaultValue(0.003)
        .range(0.001, 10.0)
        .sliderMax(1.0)
        .visible(() -> mode.get() != PhaseMode.Pearl && mode.get() != PhaseMode.Clip)
        .build()
    );
    private final Setting<Double> distance = sgClipping.add(new DoubleSetting.Builder()
        .name("distance")
        .description("The distance to phase.")
        .defaultValue(0.2)
        .range(0.0, 10.0)
        .sliderMax(1.0)
        .visible(() -> mode.get() != PhaseMode.Pearl && mode.get() != PhaseMode.Clip)
        .build()
    );
    private final Setting<Boolean> autoClip = sgClipping.add(new BoolSetting.Builder()
        .name("auto-clip")
        .description("Automatically clips into the block.")
        .defaultValue(true)
        .visible(() -> mode.get() != PhaseMode.Pearl && mode.get() != PhaseMode.Clip)
        .build()
    );
    private boolean wasPhasing = false;
    private int tickCounter = 0;
    private InventoryManager inventoryManager;
    public Phase() {
        super(Bep.CATEGORY, "phase", "Allows player to phase through solid blocks.");
        inventoryManager = InventoryManager.getInstance();
    }
    @Override
    public void onActivate() {
        if (mc.player == null || mc.level == null) {
            toggle();
            return;
        }
        if (mode.get() == PhaseMode.Pearl) {
            performPearlPhase();
            toggle();
            return;
        }
        if (mode.get() == PhaseMode.Clip) {
            performClipPhase();
            toggle();
            return;
        }
        if (autoClip.get() && mode.get() == PhaseMode.Normal) {
            performAutoClip();
        }
        wasPhasing = false;
        tickCounter = 0;
    }
    @Override
    public void onDeactivate() {
        if (mc.player != null) {
            mc.player.noPhysics = false;
        }
    }
    @EventHandler(priority = EventPriority.HIGH)
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.level == null) return;
        tickCounter++;
        if (mode.get() == PhaseMode.Clip && mc.player.onGround() && !mc.player.isPassenger()) {
            performClipTick();
            toggle();
        }
    }
    @EventHandler
    private void onPlayerMove(PlayerMoveEvent event) {
        if (mc.player == null || mc.level == null) return;
        switch (mode.get()) {
            case Normal -> handleNormalMovement(event);
            case Sand -> handleSandMovement(event);
            case Climb -> handleClimbMovement(event);
        }
    }
    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (event.packet instanceof ClientboundSetEntityMotionPacket packet) {
            bep.hax.mixin.accessor.EntityVelocityUpdateS2CPacketAccessor accessor = (bep.hax.mixin.accessor.EntityVelocityUpdateS2CPacketAccessor) packet;
            if (accessor.getEntityId() == mc.player.getId() && isActive()) {
                Vec3 velocity = accessor.getVelocity();
                if (velocity.lengthSqr() < 0.1) {
                    event.cancel();
                }
            }
        }
    }
    @EventHandler
    private void onCollisionShape(CollisionShapeEvent event) {
        if (mc.player == null || mc.level == null) return;
        switch (mode.get()) {
            case Normal -> {
                if (event.shape != Shapes.empty() &&
                    event.shape.bounds().maxY > mc.player.getBoundingBox().minY &&
                    mc.player.isShiftKeyDown()) {
                    event.cancel();
                    event.shape = Shapes.empty();
                }
            }
            case Sand -> {
                event.cancel();
                event.shape = Shapes.empty();
                mc.player.noPhysics = true;
            }
            case Climb -> {
                if (mc.player.horizontalCollision) {
                    event.cancel();
                    event.shape = Shapes.empty();
                }
                if (mc.options.keyShift.isDown() || (mc.options.keyJump.isDown() && event.pos.getY() > mc.player.getY())) {
                    event.cancel();
                }
            }
        }
    }
    @EventHandler
    private void onPushOutOfBlocks(PushOutOfBlocksEvent event) {
        if (isActive()) {
            event.cancel();
        }
    }
    private void performPearlPhase() {
        int pearlSlot = PlacementUtils.getEnderPearlSlot();
        if (pearlSlot == -1 || mc.player.getCooldowns().isOnCooldown(Items.ENDER_PEARL.getDefaultInstance())) {
            return;
        }
        final Vec3 pearlTargetVec = new Vec3(Math.floor(mc.player.getX()) + 0.5, 0.0, Math.floor(mc.player.getZ()) + 0.5);
        float[] rotations = RotationUtils.getRotationsTo(mc.player.getEyePosition(), pearlTargetVec);
        float yaw = rotations[0] + 180.0f;
        if (attack.get()) {
            handlePearlAttacks(yaw);
        }
        if (selfFill.get()) {
            handleSelfFill(yaw);
        }
        RotationUtils rotationManager = RotationUtils.getInstance();
        int targetSlot;
        if (swapAlternative.get()) {
            targetSlot = ((PlayerInventoryAccessor) mc.player.getInventory()).getSelectedSlot();
            performInventorySwapPVP(pearlSlot);
        } else if (pearlSlot < 9) {
            targetSlot = pearlSlot;
        } else {
            return;
        }
        inventoryManager.setSlot(targetSlot, InventoryManager.Priority.PEARL_PHASE);
        rotationManager.setRotationSilent(yaw, pitch.get());
        mc.getConnection().send(new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, 0, yaw, pitch.get()));
        if (swing.get()) {
            mc.player.swing(InteractionHand.MAIN_HAND);
        } else {
            mc.getConnection().send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
        }
        if (swapAlternative.get()) {
            performInventorySwapPVP(pearlSlot);
        }
        inventoryManager.syncToClient();
        rotationManager.setRotationSilentSync();
    }
    private void handlePearlAttacks(float yaw) {
        BlockHitResult hitResult = (BlockHitResult) mc.player.pick(3.0, 0, false);
        Box searchBox = Box.from(Vec3.atCenterOf(hitResult.getBlockPos())).inflate(0.2);
        for (Entity entity : mc.level.getEntities(null, searchBox)) {
            if (entity instanceof ItemFrame itemFrame) {
                mc.getConnection().send(PlayerInteractEntityC2SPacket.attack(entity, mc.player.isShiftKeyDown()));
                mc.getConnection().send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
            }
        }
        BlockState state = mc.level.getBlockState(mc.player.blockPosition());
        if (state.getBlock() instanceof ScaffoldingBlock) {
            BlockPos pos = mc.player.blockPosition();
            mc.getConnection().send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, pos, Direction.UP));
            mc.getConnection().send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, pos, Direction.UP));
        }
    }
    private void handleSelfFill(float yaw) {
        float yaw1 = yaw % 360.0f;
        if (yaw1 < 0.0f) {
            yaw1 += 360.0f;
        }
        BlockPos blockPos = mc.player.blockPosition();
        if (yaw1 >= 22.5 && yaw1 < 67.5) {
            blockPos = blockPos.south().west();
        } else if (yaw1 >= 67.5 && yaw1 < 112.5) {
            blockPos = blockPos.west();
        } else if (yaw1 >= 112.5 && yaw1 < 157.5) {
            blockPos = blockPos.north().west();
        } else if (yaw1 >= 157.5 && yaw1 < 202.5) {
            blockPos = blockPos.north();
        } else if (yaw1 >= 202.5 && yaw1 < 247.5) {
            blockPos = blockPos.north().east();
        } else if (yaw1 >= 247.5 && yaw1 < 292.5) {
            blockPos = blockPos.east();
        } else if (yaw1 >= 292.5 && yaw1 < 337.5) {
            blockPos = blockPos.south().east();
        } else {
            blockPos = blockPos.south();
        }
        FindItemResult resistantBlock = PlacementUtils.findResistantBlock();
        if (resistantBlock.found() && blockPos != null && !mc.level.getBlockState(blockPos.below()).canBeReplaced()) {
            RotationUtils rotationManager = RotationUtils.getInstance();
            PlacementUtils.placeBlock(blockPos, true, true, true);
        }
    }
    private void performInventorySwapPVP(int pearlSlot) {
        mc.gameMode.handleContainerInput(0, pearlSlot < 9 ? pearlSlot + 36 : pearlSlot, 0, ContainerInput.PICKUP, mc.player);
        mc.gameMode.handleContainerInput(0, ((PlayerInventoryAccessor) mc.player.getInventory()).getSelectedSlot() + 36, 0, ContainerInput.PICKUP, mc.player);
        mc.gameMode.handleContainerInput(0, pearlSlot < 9 ? pearlSlot + 36 : pearlSlot, 0, ContainerInput.PICKUP, mc.player);
    }
    private void performAutoClip() {
        if (mc.player == null || mc.level == null) return;
        double cos = Math.cos(Math.toRadians(mc.player.getYRot() + 90.0f));
        double sin = Math.sin(Math.toRadians(mc.player.getYRot() + 90.0f));
        double newX = mc.player.getX() + (blocks.get() * cos);
        double newZ = mc.player.getZ() + (blocks.get() * sin);
        mc.player.setPosition(newX, mc.player.getY(), newZ);
    }
    private void performClipTick() {
        Vec3 center = mc.player.blockPosition().getCenter();
        boolean flagX = (center.x - mc.player.getX()) > 0;
        boolean flagZ = (center.z - mc.player.getZ()) > 0;
        double x = center.x + 0.2 * (flagX ? -1 : 1);
        double z = center.z + 0.2 * (flagZ ? -1 : 1);
        mc.player.setPosition(x, mc.player.getY(), z);
    }
    private void performClipPhase() {
        performClipTick();
    }
    private void handleNormalMovement(PlayerMoveEvent event) {
        if (!mc.player.isShiftKeyDown() || !PlacementUtils.isPhasing()) return;
        float yaw = mc.player.getYRot();
        double offsetX = distance.get() * Math.cos(Math.toRadians(yaw + 90.0f));
        double offsetZ = distance.get() * Math.sin(Math.toRadians(yaw + 90.0f));
        Box newBB = mc.player.getBoundingBox().move(offsetX, 0.0, offsetZ);
        mc.player.setBoundingBox(newBB);
    }
    private void handleSandMovement(PlayerMoveEvent event) {
        mc.player.noPhysics = true;
        double yMotion = 0.0;
        if (mc.options.keyJump.isDown()) {
            yMotion = 0.3;
        } else if (mc.options.keyShift.isDown()) {
            yMotion = -0.3;
        }
        event.movement = new Vec3(event.movement.x, yMotion, event.movement.z);
    }
    private void handleClimbMovement(PlayerMoveEvent event) {
        if (mc.player.horizontalCollision) {
            double yMotion = event.movement.y;
            if (mc.options.keyJump.isDown()) {
                yMotion = 0.3;
            } else if (mc.options.keyShift.isDown()) {
                yMotion = -0.3;
            }
            event.movement = new Vec3(event.movement.x, yMotion, event.movement.z);
        }
    }
    @Override
    public String getInfoString() {
        return mode.get().toString();
    }
    public enum PhaseMode {
        Normal("Normal"),
        Sand("Sand"),
        Climb("Climb"),
        Pearl("Pearl"),
        Clip("Clip");
        private final String title;
        PhaseMode(String title) {
            this.title = title;
        }
        @Override
        public String toString() {
            return title;
        }
    }
}