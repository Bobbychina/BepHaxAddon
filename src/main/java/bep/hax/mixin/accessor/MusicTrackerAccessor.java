package bep.hax.mixin.accessor;
import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.client.sounds.MusicManager;
import net.minecraft.client.resources.sounds.SoundInstance;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(MusicManager.class)
public interface MusicTrackerAccessor {
    @Accessor("nextSongDelay") // 26.1: timeUntilNextSong -> nextSongDelay
    void setTimeUntilNextSong(int time);
    @Accessor("currentMusic") // 26.1: current -> currentMusic
    SoundInstance getCurrent();
}