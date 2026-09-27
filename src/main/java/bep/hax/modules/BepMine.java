package bep.hax.modules;
import bep.hax.mixin.accessor.PlayerInventoryAccessor;
import bep.hax.Bep;
import bep.hax.util.RotationUtils;
import bep.hax.util.InventoryManager;
import org.lwjgl.glfw.GLFW;
import meteordevelopment.meteorclient.mixininterface.IClientPlayerInteractionManager;
import meteordevelopment.meteorclient.events.entity.player.StartBreakingBlockEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IClientPlayerInteractionManager;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.BreakIndicators;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.Pool;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.tags.FluidTags;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.level.BlockGetter;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
public class BepMine extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgAutoMine = settings.createGroup("Auto Mine");
    private final SettingGroup sgRender = settings.createGroup("Render");
    private final Setting<SpeedmineMode> modeConfig = sgGeneral.add(new EnumSetting.Builder<SpeedmineMode>()
        .name("mode")
        .description("The mining mode for speedmine")
        .defaultValue(SpeedmineMode.PACKET)
        .build()
    );
    private final Setting<Boolean> multitaskConfig = sgGeneral.add(new BoolSetting.Builder()
        .name("multitask")
        .description("Allows mining while using items")
        .defaultValue(false)
        .visible(() -> modeConfig.get() == SpeedmineMode.PACKET)
        .build()
    );
    public final Setting<Boolean> doubleBreakConfig = sgGeneral.add(new BoolSetting.Builder()
        .name("double-break")
        .description("Allows you to mine two blocks at once")
        .defaultValue(true)
        .visible(() -> modeConfig.get() == SpeedmineMode.PACKET)
        .build()
    );
    private final Setting<Double> rangeConfig = sgGeneral.add(new DoubleSetting.Builder()
        .name("range")
        .description("The range to mine blocks")
        .defaultValue(4.5)
        .min(0.1)
        .sliderRange(0.1, 6.0)
        .visible(() -> modeConfig.get() == SpeedmineMode.PACKET)
        .build()
    );
    private final Setting<Double> speedConfig = sgGeneral.add(new DoubleSetting.Builder()
        .name("speed")
        .description("The speed to mine blocks")
        .defaultValue(1.0)
        .min(0.1)
        .sliderRange(0.1, 1.0)
        .build()
    );
    private final Setting<Boolean> instantConfig = sgGeneral.add(new BoolSetting.Builder()
        .name("instant")
        .description("Instantly mines already broken blocks")
        .defaultValue(true)
        .build()
    );
    private final Setting<Keybind> instantToggleKey = sgGeneral.add(new KeybindSetting.Builder()
        .name("instant-toggle-key")
        .description("Key to toggle the instant mining option")
        .defaultValue(Keybind.none())
        .build()
    );
    private final Setting<Boolean> persistentConfig = sgGeneral.add(new BoolSetting.Builder()
        .name("persistent")
        .description("Keeps packet mine exploit active even when module is disabled (prevents exploit from breaking)")
        .defaultValue(false)
        .visible(() -> modeConfig.get() == SpeedmineMode.PACKET)
        .onChanged(enabled -> {
            if (enabled && mc.player != null) {
                mc.player.sendSystemMessage(Component.literal("§7[§bBepMine§7] §aPersistent mode enabled! Module cannot be disabled until you turn this off or disconnect."), false);
            }
        })
        .build()
    );
    private final Setting<Swap> swapConfig = sgGeneral.add(new EnumSetting.Builder<Swap>()
        .name("auto-swap")
        .description("Swaps to the best tool once the mining is complete")
        .defaultValue(Swap.SILENT)
        .visible(() -> modeConfig.get() == SpeedmineMode.PACKET)
        .build()
    );
    private final Setting<Boolean> rotateConfig = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Rotates when mining the block")
        .defaultValue(false)
        .visible(() -> modeConfig.get() == SpeedmineMode.PACKET)
        .build()
    );
    private final Setting<Boolean> switchResetConfig = sgGeneral.add(new BoolSetting.Builder()
        .name("switch-reset")
        .description("Resets mining after switching items")
        .defaultValue(false)
        .visible(() -> modeConfig.get() == SpeedmineMode.PACKET)
        .build()
    );
    private final Setting<Boolean> grimConfig = sgGeneral.add(new BoolSetting.Builder()
        .name("grim")
        .description("Uses grim block breaking speeds")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> grimNewConfig = sgGeneral.add(new BoolSetting.Builder()
        .name("grim-v3")
        .description("Uses new grim block breaking speeds")
        .defaultValue(true)
        .visible(grimConfig::get)
        .build()
    );
    private final Setting<Boolean> miningFix = sgGeneral.add(new BoolSetting.Builder()
        .name("mining-fix")
        .description("Mining fix for grim v3")
        .defaultValue(false)
        .visible(() -> grimConfig.get() && grimNewConfig.get())
        .build()
    );
    private final Setting<Keybind> autoMineKey = sgAutoMine.add(new KeybindSetting.Builder()
        .name("auto-mine-key")
        .description("Key to toggle auto-mining enemies")
        .defaultValue(Keybind.none())
        .build()
    );
    private final Setting<Boolean> autoMine = sgAutoMine.add(new BoolSetting.Builder()
        .name("auto-mine")
        .description("Automatically mines blocks around nearby enemies")
        .defaultValue(false)
        .build()
    );
    private final Setting<Double> enemyRange = sgAutoMine.add(new DoubleSetting.Builder()
        .name("enemy-range")
        .description("Range to search for enemy players")
        .defaultValue(5.0)
        .min(1.0)
        .sliderRange(1.0, 10.0)
        .visible(autoMine::get)
        .build()
    );
    private final Setting<Boolean> strictDirection = sgAutoMine.add(new BoolSetting.Builder()
        .name("strict-direction")
        .description("Only mines blocks on visible faces")
        .defaultValue(false)
        .visible(autoMine::get)
        .build()
    );
    private final Setting<Boolean> targetHead = sgAutoMine.add(new BoolSetting.Builder()
        .name("target-head")
        .description("Also targets blocks at head level (Y+1)")
        .defaultValue(false)
        .visible(autoMine::get)
        .build()
    );
    private final Setting<Boolean> autoRotate = sgAutoMine.add(new BoolSetting.Builder()
        .name("auto-rotate")
        .description("Rotates to enemy blocks (uses silent rotations)")
        .defaultValue(true)
        .visible(autoMine::get)
        .build()
    );
    private final Setting<Boolean> antiCrawl = sgAutoMine.add(new BoolSetting.Builder()
        .name("anti-crawl")
        .description("Automatically mines block above your head when crawling to stand up")
        .defaultValue(true)
        .visible(autoMine::get)
        .build()
    );
    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
        .name("render")
        .description("Whether or not to render the block being mined")
        .defaultValue(true)
        .build()
    );
    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("How the shapes are rendered")
        .defaultValue(ShapeMode.Both)
        .build()
    );
    private final Setting<SettingColor> colorConfig = sgRender.add(new ColorSetting.Builder()
        .name("mine-color")
        .description("The mine render color")
        .defaultValue(new SettingColor(Color.BLUE))
        .visible(() -> modeConfig.get() == SpeedmineMode.PACKET)
        .build()
    );
    private final Setting<SettingColor> colorDoneConfig = sgRender.add(new ColorSetting.Builder()
        .name("done-color")
        .description("The done render color")
        .defaultValue(new SettingColor(Color.CYAN))
        .visible(() -> modeConfig.get() == SpeedmineMode.PACKET)
        .build()
    );
    private final Setting<Integer> fadeTimeConfig = sgRender.add(new IntSetting.Builder()
        .name("fade-time")
        .description("Time to fade")
        .defaultValue(250)
        .min(0)
        .sliderRange(0, 1000)
        .visible(() -> false)
        .build()
    );
    private final Map<MiningData, Animation> fadeList = new HashMap<>();
    private FirstOutQueue<MiningData> miningQueue;
    private long lastBreak;
    private boolean instantTogglePressed = false;
    private boolean autoMineTogglePressed = false;
    private Player currentTarget = null;
    private BlockPos lastAutoMineBlock = null;
    private long lastAutoMineTime = 0;
    private BlockPos lastAntiCrawlBlock = null;
    private long lastAntiCrawlTime = 0;
    private static final long AUTO_MINE_DELAY_MS = 250;
    private static final long ANTI_CRAWL_DELAY_MS = 100;
    private int swappedToSlot = -1;
    private int originalSlot = -1;
    private int swapBackTicks = 0;
    private InventoryManager inventoryManager;
    public BepMine() {
        super(Bep.CATEGORY, "bep-mine", "Mines blocks faster");
    }
    public Setting<Double> getSpeedConfig() {
        return speedConfig;
    }
    public Setting<SpeedmineMode> getModeConfig() {
        return modeConfig;
    }
    @Override
    public void toggle() {
        if (isActive() && persistentConfig.get() && mc.getConnection() != null) {
            if (mc.player != null) {
                mc.player.sendSystemMessage(Component.literal("§7[§bBepMine§7] §cCannot disable while Persistent mode is active! Disable Persistent first or disconnect from server."), false);
            }
            return;
        }
        super.toggle();
    }
    @Override
    public void onActivate() {
        if (doubleBreakConfig.get()) {
            miningQueue = new FirstOutQueue<>(2);
        } else {
            miningQueue = new FirstOutQueue<>(1);
        }
        if (swapConfig.get() == Swap.SILENT) {
            inventoryManager = InventoryManager.getInstance();
        }
        swappedToSlot = -1;
        originalSlot = -1;
        swapBackTicks = 0;
        lastAutoMineBlock = null;
        lastAutoMineTime = 0;
        lastAntiCrawlBlock = null;
        lastAntiCrawlTime = 0;
    }
    @Override
    public void onDeactivate() {
        if (persistentConfig.get() && mc.getConnection() != null) {
            return;
        }
        if (miningQueue != null) {
            miningQueue.clear();
        }
        fadeList.clear();
        if (swapConfig.get() == Swap.SILENT && inventoryManager != null) {
            inventoryManager.syncToClient();
        }
        swappedToSlot = -1;
        originalSlot = -1;
        swapBackTicks = 0;
        lastAutoMineBlock = null;
        lastAutoMineTime = 0;
        lastAntiCrawlBlock = null;
        lastAntiCrawlTime = 0;
        currentTarget = null;
    }
    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        if (miningQueue != null) {
            miningQueue.clear();
        }
        fadeList.clear();
        swappedToSlot = -1;
        originalSlot = -1;
        swapBackTicks = 0;
        lastAutoMineBlock = null;
        lastAutoMineTime = 0;
        lastAntiCrawlBlock = null;
        lastAntiCrawlTime = 0;
        currentTarget = null;
    }
    @EventHandler
    public void onPlayerTick(final TickEvent.Pre event) {
        if (mc.player.isCreative() || mc.player.isSpectator()) {
            return;
        }
        if (swapBackTicks > 0) {
            swapBackTicks--;
            if (swapBackTicks == 0 && swappedToSlot != -1 && originalSlot != -1) {
                swapBack(originalSlot);
                swappedToSlot = -1;
                originalSlot = -1;
            }
        }
        if (autoMineKey.get().isPressed() && mc.screen == null) {
            if (!autoMineTogglePressed) {
                autoMineTogglePressed = true;
                autoMine.set(!autoMine.get());
                if (mc.player != null) {
                    String status = autoMine.get() ? "§aenabled" : "§cdisabled";
                    mc.player.sendSystemMessage(Component.literal("§7[§bBepMine§7] §fAuto-mine " + status), false);
                }
            }
        } else {
            autoMineTogglePressed = false;
        }
        if (instantToggleKey.get().isPressed() && mc.screen == null) {
            if (!instantTogglePressed) {
                instantTogglePressed = true;
                instantConfig.set(!instantConfig.get());
                if (!instantConfig.get()) {
                    miningQueue.clear();
                }
                if (mc.player != null) {
                    String status = instantConfig.get() ? "§aenabled" : "§cdisabled";
                    mc.player.sendSystemMessage(Component.literal("§7[§bBepMine§7] §fInstant mining " + status), false);
                }
            }
        } else {
            instantTogglePressed = false;
        }
        if (modeConfig.get() == SpeedmineMode.DAMAGE) {
            return;
        }
        if (autoMine.get() && modeConfig.get() == SpeedmineMode.PACKET) {
            int maxQueueSize = doubleBreakConfig.get() ? 2 : 1;
            long currentTime = System.currentTimeMillis();
            if (antiCrawl.get() && mc.player.getPose() == Pose.SWIMMING) {
                if (miningQueue.size() < maxQueueSize && currentTime - lastAntiCrawlTime >= ANTI_CRAWL_DELAY_MS) {
                    BlockPos crawlBlock = getAntiCrawlBlock();
                    if (crawlBlock != null && !isMiningBlock(crawlBlock)) {
                        Direction direction = Direction.DOWN;
                        if (autoRotate.get() && rotateConfig.get()) {
                            float[] rotations = getRotationsTo(mc.player.getEyePosition(), crawlBlock.getCenter());
                            if (grimConfig.get()) {
                                RotationUtils.getInstance().setRotationSilent(rotations[0], rotations[1]);
                            } else {
                                Rotations.rotate(rotations[0], rotations[1]);
                            }
                        }
                        MiningData data = new MiningData(crawlBlock, direction);
                        queueMiningData(data);
                        lastAntiCrawlBlock = crawlBlock;
                        lastAntiCrawlTime = currentTime;
                        if (miningQueue.isEmpty()) {
                            return;
                        }
                    }
                }
            }
            currentTarget = getClosestEnemy();
            if (currentTarget != null) {
                if (miningQueue.size() < maxQueueSize) {
                    if (currentTime - lastAutoMineTime >= AUTO_MINE_DELAY_MS) {
                        BlockPos targetBlock = findBestEnemyBlock(currentTarget);
                        if (targetBlock != null && !isMiningBlock(targetBlock)) {
                            Direction direction = getInteractDirection(targetBlock);
                            if (direction == null && strictDirection.get()) {
                            } else {
                                if (direction == null) direction = Direction.UP;
                                if (autoRotate.get() && rotateConfig.get()) {
                                    float[] rotations = getRotationsTo(mc.player.getEyePosition(), targetBlock.getCenter());
                                    if (grimConfig.get()) {
                                        RotationUtils.getInstance().setRotationSilent(rotations[0], rotations[1]);
                                    } else {
                                        Rotations.rotate(rotations[0], rotations[1]);
                                    }
                                }
                                MiningData data = new MiningData(targetBlock, direction);
                                queueMiningData(data);
                                lastAutoMineBlock = targetBlock;
                                lastAutoMineTime = currentTime;
                            }
                        }
                    }
                }
            } else {
                lastAutoMineBlock = null;
            }
        }
        if (miningQueue.isEmpty()) {
            return;
        }
        List<MiningData> toRemove = new ArrayList<>();
        for (MiningData data : miningQueue) {
            if (data.getState().isAir()) {
                data.resetBreakTime();
            }
            if (isDataPacketMine(data) && (data.getState().isAir() || data.hasAttemptedBreak() && data.passedAttemptedBreakTime(500))) {
                toRemove.add(data);
                continue;
            }
            final float damageDelta = calcBlockBreakingDelta(data.getState(), mc.level, data.getPos());
            data.damage(damageDelta);
            if (isDataPacketMine(data) && data.getBlockDamage() >= 1.0f && data.getSlot() != -1) {
                if (mc.player.isUsingItem() && !multitaskConfig.get()) {
                    return;
                }
                if (!data.hasAttemptedBreak()) {
                    data.setAttemptedBreak(true);
                }
            }
        }
        miningQueue.removeAll(toRemove);
        MiningData miningData2 = miningQueue.getFirst();
        if (miningData2 == null) return;
        final double distance = mc.player.getEyePosition().distanceToSqr(miningData2.getPos().getCenter());
        if (distance > rangeConfig.get() * rangeConfig.get()) {
            miningQueue.remove(miningData2);
            return;
        }
        if (miningData2.getState().isAir()) {
            return;
        }
        if (miningData2.getBlockDamage() >= speedConfig.get() && miningData2.hasAttemptedBreak() && miningData2.passedAttemptedBreakTime(500)) {
            abortMining(miningData2);
            miningQueue.remove(miningData2);
        }
        if (miningData2.getBlockDamage() >= speedConfig.get()) {
            if (mc.player.isUsingItem() && !multitaskConfig.get()) {
                return;
            }
            stopMining(miningData2);
            if (!miningData2.hasAttemptedBreak()) {
                miningData2.setAttemptedBreak(true);
            }
            if (!instantConfig.get()) {
                miningQueue.remove(miningData2);
            }
        }
    }
    @EventHandler
    public void onAttackBlock(final StartBreakingBlockEvent event) {
        if (mc.player.isCreative() || mc.player.isSpectator() || modeConfig.get() != SpeedmineMode.PACKET) {
            return;
        }
        event.cancel();
        BlockState blockState = mc.level.getBlockState(event.blockPos);
        if (blockState.getDestroySpeed(mc.level, event.blockPos) == -1.0f || blockState.isAir()) {
            return;
        }
        startManualMine(event.blockPos, event.direction);
        mc.player.swing(InteractionHand.MAIN_HAND);
    }
    @EventHandler
    public void onPacketOutbound(PacketEvent.Send event) {
        if (event.packet instanceof ServerboundPlayerActionPacket packet
            && packet.getAction() == ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK
            && modeConfig.get() == SpeedmineMode.DAMAGE && grimConfig.get()) {
            mc.getConnection().send(new ServerboundPlayerActionPacket(
                ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, packet.getPos().above(500), packet.getDirection()));
        }
        if (event.packet instanceof ServerboundSetCarriedItemPacket && switchResetConfig.get()
            && modeConfig.get() == SpeedmineMode.PACKET) {
            for (MiningData data : miningQueue) {
                data.resetDamage();
            }
        }
    }
    @EventHandler
    public void onPacketInbound(PacketEvent.Receive event) {
        if (mc.player == null || modeConfig.get() != SpeedmineMode.PACKET) {
            return;
        }
        if (event.packet instanceof ClientboundBlockUpdatePacket packet) {
            handleBlockUpdatePacket(packet);
        } else if (event.packet instanceof ClientboundBundlePacket packet) {
            for (Packet<?> packet1 : packet.subPackets()) {
                if (packet1 instanceof ClientboundBlockUpdatePacket packet2) {
                    handleBlockUpdatePacket(packet2);
                }
            }
        }
    }
    private void handleBlockUpdatePacket(ClientboundBlockUpdatePacket packet) {
        if (!packet.getBlockState().isAir()) {
            return;
        }
        for (MiningData data : miningQueue) {
            if (data.hasAttemptedBreak() && data.getPos().equals(packet.getPos())) {
                data.setAttemptedBreak(false);
            }
        }
    }
    @EventHandler
    public void onRenderWorld(final Render3DEvent event) {
        if (mc.player.isCreative() || modeConfig.get() != SpeedmineMode.PACKET || !render.get()) {
            return;
        }
        for (MiningData data : miningQueue) {
            if (data.getState().isAir()) {
                continue;
            }
            if (!fadeList.containsKey(data)) {
                fadeList.put(data, new Animation(true, fadeTimeConfig.get()));
            }
        }
        for (Map.Entry<MiningData, Animation> entry : fadeList.entrySet()) {
            MiningData data = entry.getKey();
            boolean isActive = miningQueue.contains(data) && !data.getState().isAir();
            entry.getValue().setState(isActive);
        }
        for (Map.Entry<MiningData, Animation> set : fadeList.entrySet()) {
            MiningData data = set.getKey();
            int boxAlpha = (int) (40 * set.getValue().getFactor());
            int lineAlpha = (int) (100 * set.getValue().getFactor());
            int boxColor;
            int lineColor;
            boxColor = data.getBlockDamage() >= 0.95f || data.getState().isAir() ?
                    colorDoneConfig.get().getPacked() : colorConfig.get().getPacked();
            lineColor = data.getBlockDamage() >= 0.95f || data.getState().isAir() ?
                    colorDoneConfig.get().getPacked() : colorConfig.get().getPacked();
            boxColor = (boxColor & 0x00FFFFFF) | (boxAlpha << 24);
            lineColor = (lineColor & 0x00FFFFFF) | (lineAlpha << 24);
            BlockPos mining = data.getPos();
            VoxelShape outlineShape = data.getState().getShape(mc.level, mining);
            outlineShape = outlineShape.isEmpty() ? Shapes.block() : outlineShape;
            AABB render1 = outlineShape.bounds();
            AABB render = new AABB(mining.getX() + render1.minX, mining.getY() + render1.minY,
                mining.getZ() + render1.minZ, mining.getX() + render1.maxX,
                mining.getY() + render1.maxY, mining.getZ() + render1.maxZ);
            net.minecraft.world.phys.Vec3 center = render.getCenter();
            float total = isDataPacketMine(data) ? 1.0f : speedConfig.get().floatValue();
            float scale = data.getState().isAir() ? 1.0f : Mth.clamp((data.getBlockDamage() + (data.getBlockDamage() - data.getLastDamage()) * event.tickDelta) / total, 0.0f, 1.0f);
            double dx = (render1.maxX - render1.minX) / 2.0;
            double dy = (render1.maxY - render1.minY) / 2.0;
            double dz = (render1.maxZ - render1.minZ) / 2.0;
            final AABB scaled = new AABB(center, center).inflate(dx * scale, dy * scale, dz * scale);
            event.renderer.box(scaled.minX, scaled.minY, scaled.minZ, scaled.maxX, scaled.maxY, scaled.maxZ,
                new SettingColor(boxColor), new SettingColor(lineColor), shapeMode.get(), 0);
        }
        fadeList.entrySet().removeIf(e -> e.getValue().getFactor() == 0.0);
    }
    private void startManualMine(BlockPos pos, Direction direction) {
        clickMine(new MiningData(pos, direction));
    }
    public void clickMine(MiningData miningData) {
        int queueSize = miningQueue.size();
        if (queueSize <= 2) {
            queueMiningData(miningData);
        }
    }
    public void queueMiningData(MiningData data) {
        if (data.getState().isAir()) {
            return;
        }
        if (startMining(data)) {
            if (miningQueue.stream().anyMatch(p1 -> data.getPos().equals(p1.getPos()))) {
                return;
            }
            miningQueue.addFirst(data);
        }
    }
    private boolean startMining(MiningData data) {
        if (data.isStarted()) {
            return false;
        }
        data.setStarted();
        float breakDelta = calcBlockBreakingDelta(data.getState(), mc.level, data.getPos());
        boolean isInstantBreak = breakDelta >= 1.0f;
        if (grimNewConfig.get()) {
            if (!miningFix.get()) {
                mc.getConnection().send(new ServerboundPlayerActionPacket(
                    ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, data.getPos(), data.getDirection()));
                mc.getConnection().send(new ServerboundPlayerActionPacket(
                    ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, data.getPos(), data.getDirection()));
                mc.getConnection().send(new ServerboundPlayerActionPacket(
                    ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, data.getPos(), data.getDirection()));
            } else {
                mc.getConnection().send(new ServerboundPlayerActionPacket(
                    ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, data.getPos(), data.getDirection()));
            }
            mc.getConnection().send(new ServerboundPlayerActionPacket(
                ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, data.getPos(), data.getDirection()));
            mc.getConnection().send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
            if (!isInstantBreak) {
                mc.getConnection().send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
                mc.getConnection().send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
            }
            return true;
        }
        mc.getConnection().send(new ServerboundPlayerActionPacket(
            ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, data.getPos(), data.getDirection()));
        mc.getConnection().send(new ServerboundPlayerActionPacket(
            ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, data.getPos(), data.getDirection()));
        mc.getConnection().send(new ServerboundPlayerActionPacket(
            ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, data.getPos(), data.getDirection()));
        mc.getConnection().send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
        if (!isInstantBreak) {
            mc.getConnection().send(new ServerboundPlayerActionPacket(
                ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, data.getPos(), data.getDirection()));
            mc.getConnection().send(new ServerboundPlayerActionPacket(
                ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, data.getPos(), data.getDirection()));
            mc.getConnection().send(new ServerboundPlayerActionPacket(
                ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, data.getPos(), data.getDirection()));
            mc.getConnection().send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
        }
        return true;
    }
    private void abortMining(MiningData data) {
        if (!data.isStarted() || data.getState().isAir()) {
            return;
        }
        mc.getConnection().send(new ServerboundPlayerActionPacket(
            ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, data.getPos(), data.getDirection()));
    }
    private void stopMining(MiningData data) {
        if (!data.isStarted() || data.getState().isAir()) {
            return;
        }
        if (rotateConfig.get()) {
            float[] rotations = getRotationsTo(mc.player.getEyePosition(), data.getPos().getCenter());
            if (grimConfig.get()) {
                Rotations.rotate(rotations[0], rotations[1]);
            } else {
                Rotations.rotate(rotations[0], rotations[1]);
            }
        }
        int bestSlot = data.getSlot();
        int currentSlot = ((PlayerInventoryAccessor) mc.player.getInventory()).getSelectedSlot();
        boolean needsSwap = bestSlot != -1 && bestSlot != currentSlot;
        if (needsSwap && swappedToSlot == -1) {
            originalSlot = currentSlot;
        }
        if (needsSwap) {
            swapTo(bestSlot);
            swappedToSlot = bestSlot;
            swapBackTicks = 3;
        } else if (swappedToSlot != -1) {
            swapBackTicks = 3;
        }
        stopMiningInternal(data);
        lastBreak = System.currentTimeMillis();
    }
    private void swapTo(int slot) {
        switch (swapConfig.get()) {
            case NORMAL -> {
                ((PlayerInventoryAccessor) mc.player.getInventory()).setSelectedSlot(slot);
                mc.getConnection().send(new ServerboundSetCarriedItemPacket(slot));
            }
            case SILENT -> {
                if (inventoryManager == null) {
                    inventoryManager = InventoryManager.getInstance();
                }
                inventoryManager.setSlot(slot);
            }
        }
    }
    private void swapBack(int originalSlot) {
        switch (swapConfig.get()) {
            case NORMAL -> {
                ((PlayerInventoryAccessor) mc.player.getInventory()).setSelectedSlot(originalSlot);
                mc.getConnection().send(new ServerboundSetCarriedItemPacket(originalSlot));
            }
            case SILENT -> {
                if (inventoryManager == null) {
                    inventoryManager = InventoryManager.getInstance();
                }
                inventoryManager.syncToClient();
            }
        }
    }
    private void stopMiningInternal(MiningData data) {
        mc.getConnection().send(new ServerboundPlayerActionPacket(
            ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, data.getPos(), data.getDirection()));
        mc.getConnection().send(new ServerboundPlayerActionPacket(
            ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, data.getPos(), data.getDirection()));
    }
    public boolean isBlockDelayGrim() {
        return System.currentTimeMillis() - lastBreak <= 280 && grimConfig.get();
    }
    private boolean isDataPacketMine(MiningData data) {
        return miningQueue.size() == 2 && data == miningQueue.getLast();
    }
    public float calcBlockBreakingDelta(BlockState state, BlockGetter world, BlockPos pos) {
        if (swapConfig.get() == Swap.OFF) {
            return state.getDestroyProgress(mc.player, mc.level, pos);
        }
        float f = state.getDestroySpeed(world, pos);
        if (f == -1.0f) {
            return 0.0f;
        } else {
            int i = canHarvest(state) ? 30 : 100;
            return getBlockBreakingSpeed(state) / f / (float) i;
        }
    }
    private float getBlockBreakingSpeed(BlockState block) {
        int tool = getBestTool(block);
        float f = mc.player.getInventory().getItem(tool).getDestroySpeed(block);
        if (f > 1.0F) {
            ItemStack stack = mc.player.getInventory().getItem(tool);
            int i = 0;
            var enchantments = stack.getEnchantments();
            for (var entry : enchantments.entrySet()) {
                if (entry.getKey().is(Enchantments.EFFICIENCY)) {
                    i = entry.getIntValue();
                    break;
                }
            }
            if (i > 0 && !stack.isEmpty()) {
                f += (float) (i * i + 1);
            }
        }
        if (MobEffectUtil.hasDigSpeed(mc.player)) {
            f *= 1.0f + (float) (MobEffectUtil.getDigSpeedAmplification(mc.player) + 1) * 0.2f;
        }
        if (mc.player.hasEffect(MobEffects.MINING_FATIGUE)) {
            float g = switch (mc.player.getEffect(MobEffects.MINING_FATIGUE).getAmplifier()) {
                case 0 -> 0.3f;
                case 1 -> 0.09f;
                case 2 -> 0.0027f;
                default -> 8.1e-4f;
            };
            f *= g;
        }
        if (mc.player.isEyeInFluid(FluidTags.WATER)) {
            boolean hasAquaAffinity = false;
            ItemStack helmet = mc.player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD);
            if (!helmet.isEmpty()) {
                var enchantments = helmet.getEnchantments();
                for (var entry : enchantments.entrySet()) {
                    if (entry.getKey().is(Enchantments.AQUA_AFFINITY)) {
                        hasAquaAffinity = true;
                        break;
                    }
                }
            }
            if (!hasAquaAffinity) {
                f /= 5.0f;
            }
        }
        if (!mc.player.onGround()) {
            f /= 5.0f;
        }
        return f;
    }
    private boolean canHarvest(BlockState state) {
        if (state.requiresCorrectToolForDrops()) {
            int tool = getBestTool(state);
            return mc.player.getInventory().getItem(tool).isCorrectToolForDrops(state);
        }
        return true;
    }
    private int getBestTool(BlockState state) {
        int bestSlot = -1;
        float bestSpeed = 0;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            float speed = stack.getDestroySpeed(state);
            if (speed > bestSpeed) {
                bestSpeed = speed;
                bestSlot = i;
            }
        }
        return bestSlot == -1 ? ((PlayerInventoryAccessor) mc.player.getInventory()).getSelectedSlot() : bestSlot;
    }
    public boolean isMining() {
        return !miningQueue.isEmpty();
    }
    private static float[] getRotationsTo(net.minecraft.world.phys.Vec3 src, net.minecraft.world.phys.Vec3 dest) {
        float yaw = (float) (Math.toDegrees(Math.atan2(dest.subtract(src).z,
            dest.subtract(src).x)) - 90);
        float pitch = (float) Math.toDegrees(-Math.atan2(dest.subtract(src).y,
            Math.hypot(dest.subtract(src).x, dest.subtract(src).z)));
        return new float[] {
            Mth.wrapDegrees(yaw),
            Mth.wrapDegrees(pitch)
        };
    }
    private SettingColor interpolateColor(float value, SettingColor start, SettingColor end) {
        float sr = start.r / 255.0f;
        float sg = start.g / 255.0f;
        float sb = start.b / 255.0f;
        float sa = start.a / 255.0f;
        float er = end.r / 255.0f;
        float eg = end.g / 255.0f;
        float eb = end.b / 255.0f;
        float ea = end.a / 255.0f;
        return new SettingColor(
            (int)((sr * value + er * (1.0f - value)) * 255),
            (int)((sg * value + eg * (1.0f - value)) * 255),
            (int)((sb * value + eb * (1.0f - value)) * 255),
            (int)((sa * value + ea * (1.0f - value)) * 255)
        );
    }
    public class MiningData {
        private boolean attemptedBreak;
        private long breakTime;
        private final BlockPos pos;
        private final Direction direction;
        private float lastDamage;
        private float blockDamage;
        private boolean started;
        public MiningData(BlockPos pos, Direction direction) {
            this.pos = pos;
            this.direction = direction;
        }
        public void setAttemptedBreak(boolean attemptedBreak) {
            this.attemptedBreak = attemptedBreak;
            if (attemptedBreak) {
                resetBreakTime();
            }
        }
        public void resetBreakTime() {
            breakTime = System.currentTimeMillis();
        }
        public boolean hasAttemptedBreak() {
            return attemptedBreak;
        }
        public boolean passedAttemptedBreakTime(long time) {
            return System.currentTimeMillis() - breakTime >= time;
        }
        public float damage(final float dmg) {
            lastDamage = blockDamage;
            blockDamage += dmg;
            return blockDamage;
        }
        public void setDamage(float blockDamage) {
            this.blockDamage = blockDamage;
        }
        public void resetDamage() {
            started = false;
            blockDamage = 0.0f;
        }
        public BlockPos getPos() {
            return pos;
        }
        public Direction getDirection() {
            return direction;
        }
        public int getSlot() {
            return getBestToolNoFallback(getState());
        }
        public BlockState getState() {
            return mc.level.getBlockState(pos);
        }
        public float getBlockDamage() {
            return blockDamage;
        }
        public float getLastDamage() {
            return lastDamage;
        }
        public boolean isStarted() {
            return started;
        }
        public void setStarted() {
            this.started = true;
        }
        private int getBestToolNoFallback(BlockState state) {
            int bestSlot = -1;
            float bestSpeed = 0;
            for (int i = 0; i < 9; i++) {
                ItemStack stack = mc.player.getInventory().getItem(i);
                float speed = stack.getDestroySpeed(state);
                if (speed > bestSpeed) {
                    bestSpeed = speed;
                    bestSlot = i;
                }
            }
            return bestSlot;
        }
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MiningData that = (MiningData) o;
            return pos.equals(that.pos);
        }
        @Override
        public int hashCode() {
            return pos.hashCode();
        }
    }
    private static class FirstOutQueue<T> extends java.util.ArrayList<T> {
        private final int maxSize;
        public FirstOutQueue(int maxSize) {
            this.maxSize = maxSize;
        }
        public void addFirst(T element) {
            add(0, element);
            while (size() > maxSize) {
                remove(size() - 1);
            }
        }
        public T getFirst() {
            return isEmpty() ? null : get(0);
        }
        public T getLast() {
            return isEmpty() ? null : get(size() - 1);
        }
    }
    private static class Animation {
        private boolean state;
        private long time;
        private final long duration;
        public Animation(boolean state, long duration) {
            this.state = state;
            this.duration = duration;
            this.time = System.currentTimeMillis();
        }
        public void setState(boolean state) {
            if (this.state != state) {
                this.state = state;
                this.time = System.currentTimeMillis();
            }
        }
        public float getFactor() {
            if (state) return 1.0f;
            long elapsed = System.currentTimeMillis() - time;
            float progress = Math.min(1.0f, elapsed / (float) duration);
            return 1.0f - progress;
        }
    }
    public enum SpeedmineMode {
        PACKET,
        DAMAGE
    }
    public enum Swap {
        NORMAL,
        SILENT,
        OFF
    }
    private Player getClosestEnemy() {
        if (mc.level == null || mc.player == null) return null;
        Player closest = null;
        double closestDist = enemyRange.get() * enemyRange.get();
        for (Player player : mc.level.players()) {
            if (player == mc.player || player.isSpectator() || player.isDeadOrDying()) continue;
            if (Friends.get().isFriend(player)) continue;
            double dist = mc.player.distanceToSqr(player);
            if (dist < closestDist) {
                closestDist = dist;
                closest = player;
            }
        }
        return closest;
    }
    private BlockPos findBestEnemyBlock(Player enemy) {
        if (enemy == null) return null;
        BlockPos enemyPos = enemy.blockPosition();
        BlockState feetState = mc.level.getBlockState(enemyPos);
        if (!feetState.isAir() && feetState.getDestroySpeed(mc.level, enemyPos) != -1.0f) {
            double feetDist = mc.player.getEyePosition().distanceToSqr(enemyPos.getCenter());
            if (feetDist <= rangeConfig.get() * rangeConfig.get()) {
                if (!isMiningBlock(enemyPos) && isResistantBlock(feetState)) {
                    if (!isOwnSurroundBlock(enemyPos)) {
                        return enemyPos;
                    }
                }
            }
        }
        List<BlockPos> surroundBlocks = new ArrayList<>();
        surroundBlocks.add(enemyPos.north());
        surroundBlocks.add(enemyPos.south());
        surroundBlocks.add(enemyPos.east());
        surroundBlocks.add(enemyPos.west());
        BlockPos bestSurround = findBestBlock(surroundBlocks);
        if (bestSurround != null) {
            return bestSurround;
        }
        if (targetHead.get()) {
            BlockPos aboveHead = enemyPos.above(2);
            BlockState aboveState = mc.level.getBlockState(aboveHead);
            if (!aboveState.isAir() && aboveState.getDestroySpeed(mc.level, aboveHead) != -1.0f) {
                double dist = mc.player.getEyePosition().distanceToSqr(aboveHead.getCenter());
                if (dist <= rangeConfig.get() * rangeConfig.get()) {
                    if (!isMiningBlock(aboveHead) && isResistantBlock(aboveState)) {
                        if (!isOwnSurroundBlock(aboveHead)) {
                            return aboveHead;
                        }
                    }
                }
            }
        }
        return null;
    }
    private BlockPos findBestBlock(List<BlockPos> positions) {
        BlockPos bestBlock = null;
        double bestDist = rangeConfig.get() * rangeConfig.get();
        for (BlockPos pos : positions) {
            double dist = mc.player.getEyePosition().distanceToSqr(pos.getCenter());
            if (dist > rangeConfig.get() * rangeConfig.get()) continue;
            BlockState state = mc.level.getBlockState(pos);
            if (state.isAir() || state.getDestroySpeed(mc.level, pos) == -1.0f) continue;
            if (isMiningBlock(pos)) continue;
            if (isOwnSurroundBlock(pos)) continue;
            if (!isResistantBlock(state)) continue;
            if (dist < bestDist) {
                bestDist = dist;
                bestBlock = pos;
            }
        }
        return bestBlock;
    }
    private BlockPos getAntiCrawlBlock() {
        if (mc.player == null || mc.level == null) return null;
        BlockPos playerPos = mc.player.blockPosition();
        BlockPos blockAbove = playerPos.above();
        BlockState state = mc.level.getBlockState(blockAbove);
        if (!state.isAir() && state.getDestroySpeed(mc.level, blockAbove) != -1.0f) {
            if (!state.is(Blocks.BEDROCK) &&
                !state.is(Blocks.REINFORCED_DEEPSLATE) &&
                !state.is(Blocks.BARRIER)) {
                double dist = mc.player.getEyePosition().distanceToSqr(blockAbove.getCenter());
                if (dist <= rangeConfig.get() * rangeConfig.get()) {
                    return blockAbove;
                }
            }
        }
        return null;
    }
    private boolean isMiningBlock(BlockPos pos) {
        if (miningQueue == null) return false;
        for (MiningData data : miningQueue) {
            if (data.getPos().equals(pos)) return true;
        }
        return false;
    }
    private boolean isOwnSurroundBlock(BlockPos pos) {
        BlockPos playerPos = mc.player.blockPosition();
        if (pos.equals(playerPos.north()) || pos.equals(playerPos.south()) ||
            pos.equals(playerPos.east()) || pos.equals(playerPos.west())) {
            return true;
        }
        if (pos.equals(playerPos.above()) || pos.equals(playerPos.above(2))) {
            return true;
        }
        return false;
    }
    private boolean isResistantBlock(BlockState state) {
        if (state.is(Blocks.BEDROCK) ||
            state.is(Blocks.REINFORCED_DEEPSLATE) ||
            state.is(Blocks.BARRIER) ||
            state.is(Blocks.COMMAND_BLOCK) ||
            state.is(Blocks.STRUCTURE_BLOCK)) {
            return false;
        }
        return state.is(Blocks.OBSIDIAN) ||
               state.is(Blocks.CRYING_OBSIDIAN) ||
               state.is(Blocks.ENDER_CHEST) ||
               state.is(Blocks.ANCIENT_DEBRIS) ||
               state.is(Blocks.RESPAWN_ANCHOR);
    }
    private Direction getInteractDirection(BlockPos pos) {
        Vec3 eyePos = mc.player.getEyePosition();
        Vec3 posVec = Vec3.atCenterOf(pos);
        Direction bestDir = null;
        double bestDot = -1;
        for (Direction dir : Direction.values()) {
            Vec3 dirVec = Vec3.atLowerCornerOf(dir.getUnitVec3i());
            double dot = eyePos.subtract(posVec).normalize().dot(dirVec);
            if (dot > bestDot) {
                bestDot = dot;
                bestDir = dir;
            }
        }
        return bestDir;
    }
    public BlockPos getLastAutoMineBlock() {
        return lastAutoMineBlock;
    }
    public BlockPos getLastAntiCrawlBlock() {
        return lastAntiCrawlBlock;
    }
}