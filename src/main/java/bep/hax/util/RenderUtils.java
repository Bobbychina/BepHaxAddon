package bep.hax.util;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.*;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import org.joml.Matrix4f;
import com.mojang.blaze3d.opengl.GlProgram;
import net.minecraft.client.renderer.GameRenderer;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.systems.modules.render.blockesp.ESPBlockData;
import static meteordevelopment.meteorclient.MeteorClient.mc;
public class RenderUtils {
    private static final MultiBufferSource.BufferSource vertex = MultiBufferSource.immediate(new ByteBufferBuilder(2048));
    public static boolean shouldRenderBox(ESPBlockData esp) {
        return switch (esp.shapeMode) {
            case Both -> esp.lineColor.a > 0 || esp.sideColor.a > 0;
            case Lines -> esp.lineColor.a > 0;
            case Sides -> esp.sideColor.a > 0;
        };
    }
    public static boolean shouldRenderTracer(ESPBlockData esp) {
        return esp.tracer && esp.tracerColor.a > 0;
    }
    public static void renderTracerTo(Render3DEvent event, BlockPos pos, Color tracerColor) {
        Vec3 tracerPos = pos.getCenter();
        event.renderer.line(
            meteordevelopment.meteorclient.utils.render.RenderUtils.center.x,
            meteordevelopment.meteorclient.utils.render.RenderUtils.center.y,
            meteordevelopment.meteorclient.utils.render.RenderUtils.center.z,
            tracerPos.x, tracerPos.y, tracerPos.z, tracerColor
        );
    }
    public static void renderBlock(Render3DEvent event, BlockPos pos, Color lineColor, Color sideColor, ShapeMode mode) {
        event.renderer.box(
            pos.getX(), pos.getY(), pos.getZ(),
            pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1, sideColor, lineColor, mode, 0
        );
    }
    public static void renderBlock(Render3DEvent event, BlockPos pos, Color color) {
        event.renderer.box(
            pos.getX(), pos.getY(), pos.getZ(),
            pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1, color, color, ShapeMode.Lines, 0
        );
    }
    public static void text(String text, PoseStack stack, float x, float y, int color) {
        mc.font.drawInBatch(text, x, y, color, false, stack.last().pose(), vertex, Font.DisplayMode.NORMAL, 0, 15728880);
        vertex.endBatch();
    }
    public enum RenderMode {
        Solid,
        Fade,
        Pulse,
        Shrink
    }
}