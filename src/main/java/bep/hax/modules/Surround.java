package bep.hax.modules;
import bep.hax.Bep;
import bep.hax.modules.PVPModule;
import bep.hax.util.InventoryManager;
import bep.hax.util.RotationUtils;
import bep.hax.util.PlacementUtils;
import bep.hax.util.BlastResistantBlocks;
import bep.hax.mixin.accessor.ExplosionS2CPacketAccessor;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.BundlePacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import java.util.*;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
public class Surround extends PVPModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgTiming = settings.createGroup("Timing");
    private final SettingGroup sgRender = settings.createGroup("Render");
    private final Setting<TimingMode> timing = sgTiming.add(new EnumSetting.Builder<TimingMode>()
        .name("timing")
        .description("Timing mode for block replacement")
        .defaultValue(TimingMode.SEQUENTIAL)
        .build()
    );
    private final Setting<Boolean> prePlaceExplosion = sgTiming.add(new BoolSetting.Builder()
        .name("pre-place-explosion")
        .description("Pre-places before explosions hit")
        .defaultValue(true)
        .visible(() -> timing.get() == TimingMode.SEQUENTIAL)
        .build()
    );
    private final Setting<Boolean> prePlaceTick = sgTiming.add(new BoolSetting.Builder()
        .name("pre-place-tick")
        .description("Pre-places before crystal spawns")
        .defaultValue(true)
        .visible(() -> timing.get() == TimingMode.SEQUENTIAL)
        .build()
    );
    private final Setting<Integer> shiftTicks = sgTiming.add(new IntSetting.Builder()
        .name("shift-ticks")
        .description("Number of blocks to place per tick")
        .defaultValue(2)
        .min(1)
        .max(10)
        .sliderRange(1, 10)
        .build()
    );
    private final Setting<Double> shiftDelay = sgTiming.add(new DoubleSetting.Builder()
        .name("shift-delay")
        .description("Delay between placement intervals (ticks)")
        .defaultValue(1.0)
        .min(0.0)
        .max(5.0)
        .sliderRange(0.0, 5.0)
        .build()
    );
    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Use silent rotations")
        .defaultValue(true)
        .build()
    );
    private final Setting<Double> placeRange = sgGeneral.add(new DoubleSetting.Builder()
        .name("place-range")
        .description("Placement range")
        .defaultValue(4.0)
        .min(0.0)
        .sliderRange(0.0, 6.0)
        .build()
    );
    private final Setting<Boolean> attack = sgGeneral.add(new BoolSetting.Builder()
        .name("attack-crystals")
        .description("Attacks crystals in the way")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> headLevel = sgGeneral.add(new BoolSetting.Builder()
        .name("head-level")
        .description("Place blocks at Y+1 level")
        .defaultValue(false)
        .build()
    );
    private final Setting<Boolean> coverHead = sgGeneral.add(new BoolSetting.Builder()
        .name("cover-head")
        .description("Place block at Y+2")
        .defaultValue(false)
        .build()
    );
    private final Setting<Boolean> support = sgGeneral.add(new BoolSetting.Builder()
        .name("support")
        .description("Creates floor support for surround blocks")
        .defaultValue(false)
        .build()
    );
    private final Setting<Boolean> mineExtend = sgGeneral.add(new BoolSetting.Builder()
        .name("mine-extend")
        .description("Extends if being mined")
        .defaultValue(false)
        .build()
    );
    private final Setting<Boolean> jumpDisable = sgGeneral.add(new BoolSetting.Builder()
        .name("auto-disable")
        .description("Disables after jumping")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> multitask = sgGeneral.add(new BoolSetting.Builder()
        .name("multitask")
        .description("Place while using items")
        .defaultValue(false)
        .build()
    );
    private final Setting<Boolean> grimPlace = sgGeneral.add(new BoolSetting.Builder()
        .name("grim-place")
        .description("Uses GrimAirPlace exploit for block placement (bypass anti-cheat)")
        .defaultValue(false)
        .build()
    );
    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
        .name("render")
        .description("Render placements")
        .defaultValue(true)
        .build()
    );
    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("Shape render mode")
        .defaultValue(ShapeMode.Both)
        .visible(render::get)
        .build()
    );
    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
        .name("side-color")
        .description("Side color")
        .defaultValue(new SettingColor(255, 0, 0, 50))
        .visible(render::get)
        .build()
    );
    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("line-color")
        .description("Line color")
        .defaultValue(new SettingColor(255, 0, 0, 255))
        .visible(render::get)
        .build()
    );
    private static final List<Block> RESISTANT_BLOCKS = Arrays.asList(
        Blocks.OBSIDIAN,
        Blocks.CRYING_OBSIDIAN,
        Blocks.ENDER_CHEST
    );
    private static final BlockState DEFAULT_OBSIDIAN_STATE = Blocks.OBSIDIAN.defaultBlockState();
    private final Map<BlockPos, Long> packets = new HashMap<>();
    private final List<BlockPos> surround = new ArrayList<>();
    private final List<BlockPos> placements = new ArrayList<>();
    private double prevY;
    private int blocksPlaced = 0;
    private InventoryManager inventoryManager;
    private int lastSlot = -1;
    public enum TimingMode {
        VANILLA,
        SEQUENTIAL
    }
    public Surround() {
        super(Bep.CATEGORY, "surround", "Surrounds feet with obsidian");
    }
    public boolean isPlacing() {
        return !placements.isEmpty() && blocksPlaced < placements.size();
    }
    @Override
    public void onActivate() {
        if (mc.player == null) return;
        prevY = mc.player.getY();
        packets.clear();
        surround.clear();
        placements.clear();
        blocksPlaced = 0;
        inventoryManager = InventoryManager.getInstance();
        lastSlot = -1;
    }
    @Override
    public void onDeactivate() {
        packets.clear();
        surround.clear();
        placements.clear();
        blocksPlaced = 0;
        if (inventoryManager != null) {
            inventoryManager.syncToClient();
        }
        lastSlot = -1;
    }
    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.level == null) return;
        blocksPlaced = 0;
        if (jumpDisable.get() && (mc.player.getY() - prevY > 0.5 || mc.player.fallDistance > 1.5f)) {
            toggle();
            return;
        }
        if (!multitask.get() && mc.player.isUsingItem() && mc.player.getUsedItemHand() == InteractionHand.MAIN_HAND) {
            surround.clear();
            placements.clear();
            return;
        }
        int slot = getResistantBlockSlot();
        if (slot == -1) {
            surround.clear();
            placements.clear();
            return;
        }
        surround.clear();
        surround.addAll(calculateSurround());
        if (surround.isEmpty()) {
            placements.clear();
            return;
        }
        if (attack.get()) {
            attackCrystals(surround);
        }
        placements.clear();
        placements.addAll(getPlacementsFromSurround(surround));
        if (placements.isEmpty()) return;
        if (support.get()) {
            List<BlockPos> supportBlocks = new ArrayList<>();
            for (BlockPos block : placements) {
                if (block.getY() > mc.player.getBlockY() + 1.0) {
                    continue;
                }
                Direction direction = getPlaceSideInternal(block);
                if (direction == null && !supportBlocks.contains(block.below())) {
                    supportBlocks.add(block.below());
                }
            }
            placements.addAll(supportBlocks);
        }
        placements.sort(Comparator.comparingInt(BlockPos::getY));
        int placementIndex = 0;
        while (placementIndex < shiftTicks.get() && placementIndex < placements.size()) {
            BlockPos targetPos = placements.get(placementIndex);
            placeBlockSequential(targetPos, slot);
            placementIndex++;
        }
        if (rotate.get()) {
            RotationUtils.getInstance().setRotationSilentSync();
        }
    }
    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (mc.player == null || mc.level == null) return;
        if (event.packet instanceof BundlePacket bundlePacket) {
            for (Object subPacketObj : bundlePacket.subPackets()) {
                if (!(subPacketObj instanceof Packet<?> subPacket)) continue;
                handlePackets(subPacket);
            }
        } else {
            handlePackets(event.packet);
        }
    }
    private void handlePackets(Packet<?> serverPacket) {
        if (timing.get() != TimingMode.SEQUENTIAL) {
            return;
        }
        if (serverPacket instanceof ClientboundBlockUpdatePacket packet) {
            final BlockState blockState = packet.getBlockState();
            final BlockPos targetPos = packet.getPos();
            if (surround.contains(targetPos)) {
                if (blockState.canBeReplaced() && mc.level.isUnobstructed(DEFAULT_OBSIDIAN_STATE, targetPos, CollisionContext.empty())) {
                    final int slot = getResistantBlockSlot();
                    if (slot == -1) {
                        return;
                    }
                    placeBlockDirect(targetPos, slot);
                } else if (BlastResistantBlocks.isBlastResistant(blockState.getBlock())) {
                    packets.remove(targetPos);
                }
            }
        }
        if (blocksPlaced > shiftTicks.get() * 2) {
            return;
        }
        if (serverPacket instanceof ClientboundExplodePacket packet && prePlaceExplosion.get()) {
            Vec3 center = ((ExplosionS2CPacketAccessor) (Object) packet).getCenter();
            BlockPos pos = BlockPos.containing(center.x, center.y, center.z);
            if (surround.contains(pos)) {
                final int slot = getResistantBlockSlot();
                if (slot == -1) {
                    return;
                }
                placeBlockDirect(pos, slot);
            }
        }
        if (serverPacket instanceof ClientboundAddEntityPacket packet &&
            packet.getType().equals(EntityType.END_CRYSTAL) && prePlaceTick.get()) {
            for (BlockPos pos : surround) {
                if (!pos.equals(BlockPos.containing(packet.getX(), packet.getY(), packet.getZ()))) {
                    continue;
                }
                final int slot = getResistantBlockSlot();
                if (slot == -1) {
                    return;
                }
                placeBlockDirect(pos, slot);
                break;
            }
        }
    }
    private List<BlockPos> getPlacementsFromSurround(List<BlockPos> surroundList) {
        List<BlockPos> placementList = new ArrayList<>();
        for (BlockPos surroundPos : surroundList) {
            Long placed = packets.get(surroundPos);
            if (shiftDelay.get() > 0.0 && placed != null &&
                System.currentTimeMillis() - placed < shiftDelay.get() * 50.0) {
                continue;
            }
            if (!mc.level.getBlockState(surroundPos).canBeReplaced()) {
                continue;
            }
            double dist = mc.player.distanceToSqr(surroundPos.getCenter());
            if (dist > placeRange.get() * placeRange.get()) {
                continue;
            }
            if (mc.level.isUnobstructed(DEFAULT_OBSIDIAN_STATE, surroundPos, CollisionContext.empty())) {
                placementList.add(surroundPos);
            }
        }
        return placementList;
    }
    private List<BlockPos> calculateSurround() {
        Set<BlockPos> positions = new HashSet<>();
        BlockPos playerPos = mc.player.blockPosition();
        int footY = playerPos.getY();
        AABB box = mc.player.getBoundingBox();
        int minX = (int) Math.floor(box.minX);
        int maxX = (int) Math.floor(box.maxX - 0.0001);
        int minZ = (int) Math.floor(box.minZ);
        int maxZ = (int) Math.floor(box.maxZ - 0.0001);
        Set<BlockPos> footBlocks = new HashSet<>();
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                footBlocks.add(new BlockPos(x, footY, z));
            }
        }
        for (BlockPos foot : footBlocks) {
            positions.add(foot.north());
            positions.add(foot.south());
            positions.add(foot.east());
            positions.add(foot.west());
        }
        positions.removeAll(footBlocks);
        if (headLevel.get()) {
            Set<BlockPos> headPositions = new HashSet<>();
            for (BlockPos foot : footBlocks) {
                BlockPos up = foot.above();
                headPositions.add(up.north());
                headPositions.add(up.south());
                headPositions.add(up.east());
                headPositions.add(up.west());
            }
            for (BlockPos foot : footBlocks) {
                headPositions.remove(foot.above());
            }
            positions.addAll(headPositions);
        }
        if (coverHead.get()) {
            for (BlockPos foot : footBlocks) {
                positions.add(foot.above(2));
            }
        }
        if (mineExtend.get()) {
            Set<BlockPos> extended = new HashSet<>();
            for (BlockPos pos : new ArrayList<>(positions)) {
                if (mc.level.getBlockState(pos).canBeReplaced()) continue;
                if (mc.level.getBlockState(pos).getDestroySpeed(mc.level, pos) < 0) continue;
                for (Direction dir : Direction.Plane.HORIZONTAL) {
                    BlockPos extPos = pos.relative(dir);
                    if (!footBlocks.contains(extPos) && !positions.contains(extPos)) {
                        extended.add(extPos);
                    }
                }
            }
            positions.addAll(extended);
        }
        return new ArrayList<>(positions);
    }
    public List<BlockPos> calculateSurround(Player player) {
        Set<BlockPos> positions = new HashSet<>();
        BlockPos playerPos = player.blockPosition();
        int footY = playerPos.getY();
        AABB box = player.getBoundingBox();
        int minX = (int) Math.floor(box.minX);
        int maxX = (int) Math.floor(box.maxX - 0.0001);
        int minZ = (int) Math.floor(box.minZ);
        int maxZ = (int) Math.floor(box.maxZ - 0.0001);
        Set<BlockPos> footBlocks = new HashSet<>();
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                footBlocks.add(new BlockPos(x, footY, z));
            }
        }
        for (BlockPos foot : footBlocks) {
            positions.add(foot.north());
            positions.add(foot.south());
            positions.add(foot.east());
            positions.add(foot.west());
        }
        positions.removeAll(footBlocks);
        if (headLevel.get()) {
            Set<BlockPos> headPositions = new HashSet<>();
            for (BlockPos foot : footBlocks) {
                BlockPos up = foot.above();
                headPositions.add(up.north());
                headPositions.add(up.south());
                headPositions.add(up.east());
                headPositions.add(up.west());
            }
            for (BlockPos foot : footBlocks) {
                headPositions.remove(foot.above());
            }
            positions.addAll(headPositions);
        }
        if (coverHead.get()) {
            for (BlockPos foot : footBlocks) {
                positions.add(foot.above(2));
            }
        }
        if (mineExtend.get()) {
            Set<BlockPos> extended = new HashSet<>();
            for (BlockPos pos : new ArrayList<>(positions)) {
                if (mc.level.getBlockState(pos).canBeReplaced()) continue;
                if (mc.level.getBlockState(pos).getDestroySpeed(mc.level, pos) < 0) continue;
                for (Direction dir : Direction.Plane.HORIZONTAL) {
                    BlockPos extPos = pos.relative(dir);
                    if (!footBlocks.contains(extPos) && !positions.contains(extPos)) {
                        extended.add(extPos);
                    }
                }
            }
            positions.addAll(extended);
        }
        return new ArrayList<>(positions);
    }
    private void placeBlockSequential(BlockPos pos, int slot) {
        if (lastSlot != slot) {
            inventoryManager.setSlot(slot, true);
            lastSlot = slot;
        }
        boolean success;
        if (grimPlace.get()) {
            airPlace(pos);
            success = true;
        } else {
            success = normalPlace(pos);
        }
        if (success) {
            packets.put(pos, System.currentTimeMillis());
            blocksPlaced++;
        }
    }
    private void placeBlockDirect(BlockPos pos, int slot) {
        if (lastSlot != slot) {
            inventoryManager.setSlot(slot, true);
            lastSlot = slot;
        }
        boolean success;
        if (grimPlace.get()) {
            airPlace(pos);
            success = true;
        } else {
            success = normalPlace(pos);
        }
        if (success) {
            packets.put(pos, System.currentTimeMillis());
            blocksPlaced++;
        }
    }
    private void airPlace(BlockPos pos) {
        Direction side = getPlaceSideInternal(pos);
        if (side == null) side = Direction.UP;
        BlockPos neighbor = pos.relative(side);
        Direction opposite = side.getOpposite();
        Vec3 hitPos = Vec3.atCenterOf(neighbor).add(Vec3.atLowerCornerOf(opposite.getUnitVec3i()).scale(0.5));
        if (rotate.get()) {
            float[] angles = bep.hax.util.RotationUtils.getRotationsTo(mc.player.getEyePosition(), hitPos);
            setRotationSilent(angles[0], angles[1]);
        }
        BlockHitResult hit = new BlockHitResult(hitPos, opposite, neighbor, false);
        mc.player.connection.send(new ServerboundPlayerActionPacket(
            ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND,
            BlockPos.ZERO,
            Direction.DOWN
        ));
        mc.player.connection.send(new ServerboundUseItemOnPacket(
            InteractionHand.OFF_HAND,
            hit,
            0
        ));
        mc.player.connection.send(new ServerboundPlayerActionPacket(
            ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND,
            BlockPos.ZERO,
            Direction.DOWN
        ));
        mc.player.connection.send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
    }
    private boolean normalPlace(BlockPos pos) {
        Direction side = getPlaceSideInternal(pos);
        if (side == null) return false;
        BlockPos neighbor = pos.relative(side);
        Direction opposite = side.getOpposite();
        Vec3 hitPos = Vec3.atCenterOf(neighbor).add(Vec3.atLowerCornerOf(opposite.getUnitVec3i()).scale(0.5));
        if (rotate.get()) {
            float[] angles = bep.hax.util.RotationUtils.getRotationsTo(mc.player.getEyePosition(), hitPos);
            setRotationSilent(angles[0], angles[1]);
        }
        BlockHitResult hitResult = new BlockHitResult(hitPos, opposite, neighbor, false);
        mc.getConnection().send(new ServerboundUseItemOnPacket(
            InteractionHand.MAIN_HAND,
            hitResult,
            0
        ));
        mc.getConnection().send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
        return true;
    }
    private void attackCrystals(List<BlockPos> positions) {
        for (BlockPos pos : positions) {
            Entity crystal = mc.level.getEntities(null, new AABB(pos)).stream()
                .filter(e -> e instanceof EndCrystal)
                .findFirst()
                .orElse(null);
            if (crystal != null) {
                mc.getConnection().send(new ServerboundAttackPacket(crystal.getId()));
                mc.getConnection().send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
                return;
            }
        }
    }
    @EventHandler
    private void onRender(Render3DEvent event) {
        if (!render.get() || placements.isEmpty()) return;
        for (BlockPos pos : placements) {
            event.renderer.box(pos, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
        }
    }
    private int getResistantBlockSlot() {
        for (Block block : RESISTANT_BLOCKS) {
            int slot = InvUtils.findInHotbar(stack ->
                stack.getItem() instanceof BlockItem item &&
                item.getBlock() == block
            ).slot();
            if (slot != -1) return slot;
        }
        return -1;
    }
    private Direction getPlaceSideInternal(BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = pos.relative(direction);
            net.minecraft.world.level.block.state.BlockState state = mc.level.getBlockState(neighbor);
            if (state.isAir() || !state.getFluidState().isEmpty()) {
                continue;
            }
            if (state.getBlock() == Blocks.ANVIL ||
                state.getBlock() == Blocks.CHIPPED_ANVIL ||
                state.getBlock() == Blocks.DAMAGED_ANVIL) {
                continue;
            }
            return direction;
        }
        return null;
    }
}