package bep.hax.modules;
import meteordevelopment.meteorclient.events.entity.player.InteractBlockEvent;
import meteordevelopment.meteorclient.events.entity.player.StartBreakingBlockEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import static meteordevelopment.meteorclient.MeteorClient.mc;
public class StashMoverSelectionHandler {
    private static StashMoverSelectionHandler INSTANCE;
    public static void init() {
        if (INSTANCE == null) {
            INSTANCE = new StashMoverSelectionHandler();
            meteordevelopment.meteorclient.MeteorClient.EVENT_BUS.subscribe(INSTANCE);
            System.out.println("[StashMover] Selection handler initialized and subscribed to events");
            if (meteordevelopment.meteorclient.MeteorClient.mc != null && meteordevelopment.meteorclient.MeteorClient.mc.player != null) {
                meteordevelopment.meteorclient.utils.player.ChatUtils.info("[StashMover] Selection handler ready");
            }
        } else {
            System.out.println("[StashMover] Selection handler already initialized");
        }
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    private void onInteractBlock(InteractBlockEvent event) {
        StashMover module = Modules.get().get(StashMover.class);
        if (module == null) return;
        if (!module.isSelecting()) return;
        if (event.hand != InteractionHand.MAIN_HAND) return;
        event.cancel();
        module.handleBlockSelectionPublic(event.result.getBlockPos());
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    private void onStartBreakingBlock(StartBreakingBlockEvent event) {
        StashMover module = Modules.get().get(StashMover.class);
        if (module == null) return;
        if (!module.isSelecting()) return;
        event.cancel();
        module.handleBlockSelectionPublic(event.blockPos);
    }
    private boolean wasSelecting = false;
    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.level == null) return;
        StashMover module = Modules.get().get(StashMover.class);
        if (module == null) return;
        if (module.isSelecting()) {
            if (!wasSelecting) {
                wasSelecting = true;
            }
            if (mc.options.keyAttack.isDown()) {
                mc.options.keyAttack.setDown(false);
                if (mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK) {
                    BlockHitResult hit = (BlockHitResult) mc.hitResult;
                    BlockPos pos = hit.getBlockPos();
                    module.handleBlockSelectionPublic(pos);
                }
            }
            if (mc.options.keyInventory.consumeClick()) {
                module.cancelSelection();
                meteordevelopment.meteorclient.utils.player.ChatUtils.info("§cSelection cancelled");
                return;
            }
        } else {
            wasSelecting = false;
        }
    }
    @EventHandler
    private void onRender3D(Render3DEvent event) {
        StashMover module = Modules.get().get(StashMover.class);
        if (module == null) return;
        if (module.getSelectionMode() != StashMover.SelectionMode.NONE) {
            BlockPos selectionPos1 = module.getSelectionPos1();
            if (selectionPos1 != null) {
                BlockPos currentPos = mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK ?
                    ((BlockHitResult)mc.hitResult).getBlockPos() : mc.player.blockPosition();
                AABB selectionBox = new AABB(
                    Math.min(selectionPos1.getX(), currentPos.getX()),
                    Math.min(selectionPos1.getY(), currentPos.getY()),
                    Math.min(selectionPos1.getZ(), currentPos.getZ()),
                    Math.max(selectionPos1.getX(), currentPos.getX()) + 1,
                    Math.max(selectionPos1.getY(), currentPos.getY()) + 1,
                    Math.max(selectionPos1.getZ(), currentPos.getZ()) + 1
                );
                boolean isInput = module.getSelectionMode() == StashMover.SelectionMode.INPUT_FIRST ||
                                 module.getSelectionMode() == StashMover.SelectionMode.INPUT_SECOND;
                SettingColor color = isInput ?
                    new SettingColor(0, 255, 0, 100) :
                    new SettingColor(0, 100, 255, 100);
                event.renderer.box(selectionBox, color, color, ShapeMode.Both, 0);
                AABB corner1 = new AABB(
                    selectionPos1.getX(), selectionPos1.getY(), selectionPos1.getZ(),
                    selectionPos1.getX() + 1, selectionPos1.getY() + 1, selectionPos1.getZ() + 1
                );
                event.renderer.box(corner1, new SettingColor(255, 255, 0, 200),
                                 new SettingColor(255, 255, 0, 100), ShapeMode.Both, 0);
            }
        }
        module.renderAreas(event);
    }
}