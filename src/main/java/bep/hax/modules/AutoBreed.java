package bep.hax.modules;
import bep.hax.mixin.accessor.PlayerInventoryAccessor;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.fox.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.entity.animal.equine.Donkey;
import net.minecraft.world.entity.animal.panda.Panda;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.sniffer.Sniffer;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.entity.monster.Strider;
import net.minecraft.world.entity.animal.turtle.Turtle;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.animal.equine.Llama;
import net.minecraft.world.entity.animal.cow.MushroomCow;
import net.minecraft.world.entity.animal.cow.Cow;
import static meteordevelopment.meteorclient.MeteorClient.mc;
public class AutoBreed extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRotation = settings.createGroup("Rotation");
    private final SettingGroup sgTiming = settings.createGroup("Timing");
    private final Setting<Set<EntityType<?>>> entities = sgGeneral.add(new EntityTypeListSetting.Builder()
        .name("entities")
        .description("Which entities to breed.")
        .defaultValue(
            EntityType.COW, EntityType.MOOSHROOM, EntityType.SHEEP, EntityType.PIG,
            EntityType.CHICKEN, EntityType.RABBIT, EntityType.TURTLE, EntityType.HORSE,
            EntityType.DONKEY, EntityType.LLAMA, EntityType.WOLF, EntityType.CAT,
            EntityType.PANDA, EntityType.FOX, EntityType.BEE, EntityType.GOAT,
            EntityType.AXOLOTL, EntityType.STRIDER, EntityType.CAMEL, EntityType.SNIFFER
        )
        .onlyAttackable()
        .build()
    );
    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("range")
        .description("Maximum range to breed animals.")
        .defaultValue(4.5)
        .min(1.0)
        .max(6.0)
        .sliderMax(6.0)
        .build()
    );
    private final Setting<Boolean> requireBothParents = sgGeneral.add(new BoolSetting.Builder()
        .name("require-both-parents")
        .description("Only breed when there are at least 2 breedable animals of the same type.")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> ignoreBabies = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-babies")
        .description("Don't attempt to breed baby animals.")
        .defaultValue(true)
        .build()
    );
    private final Setting<Integer> clicksPerAnimal = sgGeneral.add(new IntSetting.Builder()
        .name("clicks-per-animal")
        .description("Number of interaction attempts per animal.")
        .defaultValue(3)
        .min(1)
        .max(100)
        .build()
    );
    private final Setting<Boolean> rotate = sgRotation.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Rotate towards animals before feeding.")
        .defaultValue(true)
        .build()
    );
    private final Setting<Double> rotationSpeed = sgRotation.add(new DoubleSetting.Builder()
        .name("rotation-speed")
        .description("Maximum rotation speed per tick.")
        .defaultValue(40.0)
        .min(5.0)
        .max(180.0)
        .visible(rotate::get)
        .build()
    );
    private final Setting<Integer> feedDelay = sgTiming.add(new IntSetting.Builder()
        .name("feed-delay")
        .description("Ticks between feeding different animals.")
        .defaultValue(15)
        .min(1)
        .max(100)
        .build()
    );
    private final Setting<Integer> clickDelay = sgTiming.add(new IntSetting.Builder()
        .name("click-delay")
        .description("Ticks between clicks on same animal.")
        .defaultValue(4)
        .min(1)
        .max(20)
        .build()
    );
    private final Setting<Integer> breedCooldown = sgTiming.add(new IntSetting.Builder()
        .name("breed-cooldown")
        .description("Seconds to wait after feeding before trying again.")
        .defaultValue(30)
        .min(5)
        .max(300)
        .build()
    );
    private final Setting<Boolean> debugMode = sgTiming.add(new BoolSetting.Builder()
        .name("debug")
        .description("Show debug messages.")
        .defaultValue(false)
        .build()
    );
    private final Map<UUID, Long> fedAnimals = new ConcurrentHashMap<>();
    private final Map<Class<?>, Integer> animalRotation = new ConcurrentHashMap<>();
    private AgeableMob target = null;
    private int targetSlot = -1;
    private int previousSlot = -1;
    private int clickCount = 0;
    private int tickCount = 0;
    private int stateTicks = 0;
    private State state = State.IDLE;
    private enum State {
        IDLE,
        ROTATING,
        FEEDING
    }
    public AutoBreed() {
        super(Categories.World, "auto-breed", "Automatically breeds animals.");
    }
    @Override
    public void onActivate() {
        fedAnimals.clear();
        animalRotation.clear();
        resetState();
        if (debugMode.get()) ChatUtils.info("[AutoBreed] Activated");
    }
    @Override
    public void onDeactivate() {
        fedAnimals.clear();
        restoreSlot();
        resetState();
    }
    private void resetState() {
        target = null;
        targetSlot = -1;
        previousSlot = -1;
        clickCount = 0;
        tickCount = 0;
        stateTicks = 0;
        state = State.IDLE;
    }
    private void restoreSlot() {
        if (previousSlot != -1 && mc.player != null) {
            ((PlayerInventoryAccessor) mc.player.getInventory()).setSelectedSlot(previousSlot);
            previousSlot = -1;
        }
    }
    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.level == null) return;
        tickCount++;
        stateTicks++;
        long now = System.currentTimeMillis();
        fedAnimals.entrySet().removeIf(e -> now - e.getValue() > breedCooldown.get() * 1000L);
        switch (state) {
            case ROTATING -> handleRotating();
            case FEEDING -> handleFeeding();
            case IDLE -> handleIdle();
        }
    }
    private void handleRotating() {
        if (target == null || !target.isAlive() || mc.player.distanceTo(target) > range.get()) {
            if (debugMode.get()) ChatUtils.info("[AutoBreed] Target lost during rotation");
            restoreSlot();
            resetState();
            return;
        }
        Vec3 targetPos = target.getBoundingBox().getCenter();
        Vec3 eyePos = mc.player.getEyePosition();
        Vec3 diff = targetPos.subtract(eyePos);
        double dist = Math.sqrt(diff.x * diff.x + diff.z * diff.z);
        float targetYaw = (float) Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90f;
        float targetPitch = (float) -Math.toDegrees(Math.atan2(diff.y, dist));
        float yawDiff = Mth.wrapDegrees(targetYaw - mc.player.getYRot());
        float pitchDiff = targetPitch - mc.player.getXRot();
        float maxSpeed = rotationSpeed.get().floatValue();
        if (Math.abs(yawDiff) > maxSpeed) yawDiff = Math.copySign(maxSpeed, yawDiff);
        if (Math.abs(pitchDiff) > maxSpeed) pitchDiff = Math.copySign(maxSpeed, pitchDiff);
        mc.player.setYRot(mc.player.getYRot() + yawDiff);
        mc.player.setXRot(Mth.clamp(mc.player.getXRot() + pitchDiff, -90, 90));
        if (Math.abs(yawDiff) < 2.0f && Math.abs(pitchDiff) < 2.0f && stateTicks >= 5) {
            if (targetSlot != -1) {
                previousSlot = ((PlayerInventoryAccessor) mc.player.getInventory()).getSelectedSlot();
                ((PlayerInventoryAccessor) mc.player.getInventory()).setSelectedSlot(targetSlot);
                if (debugMode.get()) {
                    ItemStack item = mc.player.getInventory().getItem(targetSlot);
                    ChatUtils.info("[AutoBreed] Switched to " + item.getHoverName().getString() + " slot " + targetSlot);
                }
            }
            state = State.FEEDING;
            stateTicks = 0;
            clickCount = 0;
        }
    }
    private void handleFeeding() {
        if (target == null || !target.isAlive() || mc.player.distanceTo(target) > range.get()) {
            if (debugMode.get()) ChatUtils.info("[AutoBreed] Target lost during feeding");
            restoreSlot();
            resetState();
            return;
        }
        if (stateTicks < 2) return;
        if (stateTicks % clickDelay.get() == 0 && clickCount < clicksPerAnimal.get()) {
            Vec3 targetPos = target.getBoundingBox().getCenter();
            Vec3 eyePos = mc.player.getEyePosition();
            Vec3 diff = targetPos.subtract(eyePos);
            double dist = Math.sqrt(diff.x * diff.x + diff.z * diff.z);
            float yaw = (float) Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90f;
            float pitch = (float) -Math.toDegrees(Math.atan2(diff.y, dist));
            mc.player.setYRot(yaw);
            mc.player.setXRot(Mth.clamp(pitch, -90, 90));
            Vec3 hitPos = targetPos;
            EntityHitResult hitResult = new EntityHitResult(target, hitPos);
            InteractionResult result = mc.gameMode.interact(mc.player, target, hitResult, InteractionHand.MAIN_HAND);
            if (!result.consumesAction()) {
                result = mc.gameMode.interact(mc.player, target, new EntityHitResult(target), InteractionHand.MAIN_HAND);
            }
            clickCount++;
            if (debugMode.get()) {
                ItemStack heldItem = mc.player.getMainHandItem();
                int countBefore = heldItem.getCount();
                ChatUtils.info("[AutoBreed] Click " + clickCount + "/" + clicksPerAnimal.get() +
                    " on " + target.getType().getDescription().getString() +
                    " - " + (result.consumesAction() ? "ACCEPTED" : "REJECTED") +
                    " - Item count: " + countBefore);
            }
        }
        if (clickCount >= clicksPerAnimal.get()) {
            fedAnimals.put(target.getUUID(), System.currentTimeMillis());
            if (debugMode.get()) {
                ChatUtils.info("[AutoBreed] Completed feeding " + target.getType().getDescription().getString());
            }
            restoreSlot();
            resetState();
        }
    }
    private void handleIdle() {
        if (tickCount < feedDelay.get()) return;
        List<AgeableMob> animals = findBreedableAnimals();
        if (animals.isEmpty()) return;
        Map<Class<?>, List<AgeableMob>> byType = new HashMap<>();
        for (AgeableMob animal : animals) {
            byType.computeIfAbsent(animal.getClass(), k -> new ArrayList<>()).add(animal);
        }
        for (Map.Entry<Class<?>, List<AgeableMob>> entry : byType.entrySet()) {
            Class<?> type = entry.getKey();
            List<AgeableMob> list = entry.getValue();
            if (requireBothParents.get() && list.size() < 2) continue;
            int start = animalRotation.getOrDefault(type, 0);
            for (int i = 0; i < list.size(); i++) {
                int idx = (start + i) % list.size();
                AgeableMob animal = list.get(idx);
                int slot = findBreedingItemSlot(animal);
                if (slot == -1) continue;
                target = animal;
                targetSlot = slot;
                state = State.ROTATING;
                stateTicks = 0;
                clickCount = 0;
                tickCount = 0;
                animalRotation.put(type, (idx + 1) % list.size());
                if (debugMode.get()) {
                    ItemStack item = mc.player.getInventory().getItem(slot);
                    ChatUtils.info("[AutoBreed] Targeting " + animal.getType().getDescription().getString() +
                        " with " + item.getHoverName().getString());
                }
                return;
            }
        }
    }
    private List<AgeableMob> findBreedableAnimals() {
        List<AgeableMob> result = new ArrayList<>();
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof AgeableMob animal)) continue;
            if (!animal.isAlive()) continue;
            if (mc.player.distanceTo(animal) > range.get()) continue;
            if (!entities.get().contains(animal.getType())) continue;
            if (ignoreBabies.get()) {
                try {
                    if (animal.isBaby()) continue;
                } catch (Exception ignored) {}
            }
            if (fedAnimals.containsKey(animal.getUUID())) continue;
            if (!canBreed(animal)) continue;
            if (findBreedingItemSlot(animal) == -1) continue;
            result.add(animal);
        }
        result.sort(Comparator.comparingDouble(a -> mc.player.distanceTo(a)));
        return result;
    }
    private int findBreedingItemSlot(AgeableMob animal) {
        List<Item> items = getBreedingItems(animal);
        if (items == null) return -1;
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = mc.player.getInventory().getItem(slot);
            if (!stack.isEmpty()) {
                for (Item item : items) {
                    if (stack.getItem() == item) return slot;
                }
            }
        }
        return -1;
    }
    private List<Item> getBreedingItems(AgeableMob animal) {
        if (animal instanceof MushroomCow || animal instanceof Cow ||
            animal instanceof Sheep || animal instanceof Goat) {
            return List.of(Items.WHEAT);
        }
        if (animal instanceof Pig) {
            return List.of(Items.CARROT, Items.POTATO, Items.BEETROOT);
        }
        if (animal instanceof Chicken) {
            return List.of(Items.WHEAT_SEEDS, Items.BEETROOT_SEEDS, Items.MELON_SEEDS,
                         Items.PUMPKIN_SEEDS, Items.TORCHFLOWER_SEEDS, Items.PITCHER_POD);
        }
        if (animal instanceof Rabbit) {
            return List.of(Items.CARROT, Items.GOLDEN_CARROT, Items.DANDELION);
        }
        if (animal instanceof Horse || animal instanceof Donkey) {
            return List.of(Items.GOLDEN_APPLE, Items.GOLDEN_CARROT);
        }
        if (animal instanceof Llama) return List.of(Items.HAY_BLOCK);
        if (animal instanceof Wolf) {
            return List.of(Items.BEEF, Items.CHICKEN, Items.PORKCHOP, Items.RABBIT, Items.MUTTON,
                         Items.COOKED_BEEF, Items.COOKED_CHICKEN, Items.COOKED_PORKCHOP,
                         Items.COOKED_RABBIT, Items.COOKED_MUTTON, Items.ROTTEN_FLESH);
        }
        if (animal instanceof Cat) return List.of(Items.COD, Items.SALMON);
        if (animal instanceof Panda) return List.of(Items.BAMBOO);
        if (animal instanceof Fox) return List.of(Items.SWEET_BERRIES, Items.GLOW_BERRIES);
        if (animal instanceof Bee) {
            return List.of(Items.DANDELION, Items.POPPY, Items.BLUE_ORCHID, Items.ALLIUM,
                         Items.AZURE_BLUET, Items.RED_TULIP, Items.ORANGE_TULIP, Items.WHITE_TULIP,
                         Items.PINK_TULIP, Items.OXEYE_DAISY, Items.CORNFLOWER, Items.LILY_OF_THE_VALLEY,
                         Items.WITHER_ROSE, Items.SUNFLOWER, Items.LILAC, Items.ROSE_BUSH,
                         Items.PEONY, Items.TORCHFLOWER, Items.PITCHER_PLANT);
        }
        if (animal instanceof Axolotl) return List.of(Items.TROPICAL_FISH_BUCKET);
        if (animal instanceof Turtle) return List.of(Items.SEAGRASS);
        if (animal instanceof Strider) return List.of(Items.WARPED_FUNGUS);
        if (animal instanceof Camel) return List.of(Items.CACTUS);
        if (animal instanceof Sniffer) return List.of(Items.TORCHFLOWER_SEEDS);
        return null;
    }
    private boolean canBreed(AgeableMob animal) {
        if (animal instanceof Horse horse) return horse.isTamed();
        if (animal instanceof Donkey donkey) return donkey.isTamed();
        if (animal instanceof Wolf wolf) return wolf.isTame();
        if (animal instanceof Cat cat) return cat.isTame();
        return true;
    }
    @Override
    public String getInfoString() {
        return String.valueOf(fedAnimals.size());
    }
}