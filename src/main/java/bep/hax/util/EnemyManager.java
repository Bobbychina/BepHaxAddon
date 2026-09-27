package bep.hax.util;
import net.minecraft.world.entity.player.Player;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;
import static meteordevelopment.meteorclient.MeteorClient.mc;
public class EnemyManager {
    private static EnemyManager INSTANCE;
    private final Set<UUID> enemies = new CopyOnWriteArraySet<>();
    private final Set<String> enemyNames = new CopyOnWriteArraySet<>();
    private EnemyManager() {}
    public static EnemyManager getInstance() {
        if (INSTANCE == null) INSTANCE = new EnemyManager();
        return INSTANCE;
    }
    public boolean add(String name) {
        if (name == null || name.isEmpty()) return false;
        enemyNames.add(name);
        if (mc.level != null) {
            for (Player p : mc.level.players())
                if (p.getName().getString().equalsIgnoreCase(name)) enemies.add(p.getUUID());
        }
        return true;
    }
    public boolean add(Player player) {
        if (player == null) return false;
        enemyNames.add(player.getName().getString());
        enemies.add(player.getUUID());
        return true;
    }
    public boolean remove(String name) {
        if (name == null || name.isEmpty()) return false;
        boolean removed = enemyNames.remove(name);
        if (mc.level != null)
            for (Player p : mc.level.players())
                if (p.getName().getString().equalsIgnoreCase(name))
                    enemies.remove(p.getUUID());
        return removed;
    }
    public boolean remove(Player player) {
        if (player == null) return false;
        enemyNames.remove(player.getName().getString());
        enemies.remove(player.getUUID());
        return true;
    }
    public boolean isEnemy(String name) {
        for (String stored : enemyNames)
            if (stored.equalsIgnoreCase(name)) return true;
        return false;
    }
    public boolean isEnemy(UUID uuid) { return enemies.contains(uuid); }
    public boolean isEnemy(Player player) {
        return player != null && (isEnemy(player.getUUID()) || isEnemy(player.getName().getString()));
    }
    public Set<String> getEnemyNames() { return Set.copyOf(enemyNames); }
    public Set<UUID> getEnemyUUIDs() { return Set.copyOf(enemies); }
    public void clear() { enemies.clear(); enemyNames.clear(); }
    public int count() { return enemyNames.size(); }
    public void updateUUIDs() {
        if (mc.level == null) return;
        enemies.clear();
        for (String name : enemyNames)
            for (Player p : mc.level.players())
                if (p.getName().getString().equalsIgnoreCase(name)) {
                    enemies.add(p.getUUID());
                    break;
                }
    }
}