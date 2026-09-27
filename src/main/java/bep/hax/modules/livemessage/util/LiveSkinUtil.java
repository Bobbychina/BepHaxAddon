package bep.hax.modules.livemessage.util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.Identifier;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
public class LiveSkinUtil {
    private static final Map<UUID, LiveSkinUtil> SKIN_CACHE = new ConcurrentHashMap<>();
    private final UUID uuid;
    private LiveSkinUtil(UUID uuid) {
        this.uuid = uuid;
    }
    public static LiveSkinUtil get(UUID uuid) {
        return SKIN_CACHE.computeIfAbsent(uuid, LiveSkinUtil::new);
    }
    public static void clearCache() {
        SKIN_CACHE.clear();
    }
    public void reloadSkin() {
    }
    public boolean hasLocationSkin() {
        return getLocationSkin() != null;
    }
    public String getSkinType() {
        return "default";
    }
    public boolean customSkinLoaded() {
        return true;
    }
    public Identifier getLocationSkin() {
        return DefaultPlayerSkin.getDefaultTexture();
    }
    public Identifier getLocationCape() {
        return null;
    }
    public Identifier getLocationElytra() {
        return null;
    }
}