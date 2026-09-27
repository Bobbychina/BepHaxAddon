package bep.hax.util;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.BlockPos;
import java.util.Set;
import static meteordevelopment.meteorclient.MeteorClient.mc;
public class BlastResistantBlocks {
    private static final Set<Block> BLAST_RESISTANT = new ReferenceOpenHashSet<>(Set.of(
        Blocks.OBSIDIAN,
        Blocks.ANVIL,
        Blocks.ENCHANTING_TABLE,
        Blocks.ENDER_CHEST,
        Blocks.BEACON
    ));
    private static final Set<Block> UNBREAKABLE = new ReferenceOpenHashSet<>(Set.of(
        Blocks.BEDROCK,
        Blocks.COMMAND_BLOCK,
        Blocks.CHAIN_COMMAND_BLOCK,
        Blocks.END_PORTAL_FRAME,
        Blocks.BARRIER
    ));
    public static boolean isBreakable(BlockPos pos) {
        if (mc.level == null) {
            return false;
        }
        return isBreakable(mc.level.getBlockState(pos).getBlock());
    }
    public static boolean isBreakable(Block block) {
        return !UNBREAKABLE.contains(block);
    }
    public static boolean isUnbreakable(BlockPos pos) {
        if (mc.level == null) {
            return false;
        }
        return isUnbreakable(mc.level.getBlockState(pos).getBlock());
    }
    public static boolean isUnbreakable(Block block) {
        return UNBREAKABLE.contains(block);
    }
    public static boolean isBlastResistant(BlockPos pos) {
        if (mc.level == null) {
            return false;
        }
        return isBlastResistant(mc.level.getBlockState(pos).getBlock());
    }
    public static boolean isBlastResistant(BlockState state) {
        return isBlastResistant(state.getBlock());
    }
    public static boolean isBlastResistant(Block block) {
        return BLAST_RESISTANT.contains(block);
    }
}