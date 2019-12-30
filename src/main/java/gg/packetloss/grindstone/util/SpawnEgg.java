package gg.packetloss.grindstone.util;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public enum SpawnEgg {

    // FRIENDLY - 7
    BAT(EntityType.BAT, Material.BAT_SPAWN_EGG),
    CHICKEN(EntityType.CHICKEN, Material.CHICKEN_SPAWN_EGG),
    COW(EntityType.COW, Material.COW_SPAWN_EGG),
    MUSHROOM_COW(EntityType.MUSHROOM_COW, Material.MOOSHROOM_SPAWN_EGG),
    OCELOT(EntityType.OCELOT, Material.OCELOT_SPAWN_EGG),
    PIG(EntityType.PIG, Material.PIG_SPAWN_EGG),
    SHEEP(EntityType.SHEEP, Material.SHEEP_SPAWN_EGG),

    // NEUTRAL - 1
    WOLF(EntityType.WOLF, Material.WOLF_SPAWN_EGG),

    // MEAN - 8
    ENDERMAN(EntityType.ENDERMAN, Material.ENDERMAN_SPAWN_EGG),
    SPIDER(EntityType.SPIDER, Material.SPIDER_SPAWN_EGG),
    CAVE_SPIDER(EntityType.CAVE_SPIDER, Material.CAVE_SPIDER_SPAWN_EGG),
    SLIME(EntityType.SLIME, Material.SLIME_SPAWN_EGG),
    MAGMA_CUBE(EntityType.MAGMA_CUBE, Material.MAGMA_CUBE_SPAWN_EGG),
    WITCH(EntityType.WITCH, Material.WITCH_SPAWN_EGG),
    SKELETON(EntityType.SKELETON, Material.SKELETON_SPAWN_EGG),
    ZOMBIE(EntityType.ZOMBIE, Material.ZOMBIE_SPAWN_EGG);

    private static final Map<EntityType, SpawnEgg> ENTITY_TYPE_TO_SPAWN_EGG = new EnumMap<>(EntityType.class);
    private static final Map<Material, SpawnEgg> MATERIAL_TO_SPAWN_EGG = new HashMap<>();

    private final EntityType entityType;
    private final Material itemType;

    private SpawnEgg(EntityType entityType, Material itemType) {
        this.entityType = entityType;
        this.itemType = itemType;
    }

    public EntityType getEntityType() {
        return entityType;
    }

    public Material getMaterial() {
        return itemType;
    }

    public ItemStack toItemStack(int amt) {
        return new ItemStack(itemType, amt);
    }

    public ItemStack toItemStack() {
        return toItemStack(1);
    }

    public static SpawnEgg fromMaterial(Material type) {
        return MATERIAL_TO_SPAWN_EGG.get(type);
    }

    public static SpawnEgg fromEntityType(EntityType type) {
        return ENTITY_TYPE_TO_SPAWN_EGG.get(type);
    }
}
