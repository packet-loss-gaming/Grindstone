package gg.packetloss.hackbook.entity;

import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.templates.TaggedChoice;
import gg.packetloss.hackbook.DataMigrator;
import net.minecraft.server.v1_15_R1.*;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_15_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_15_R1.entity.CraftEntity;
import org.bukkit.entity.Giant;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

public class HBGiant extends EntityGiantZombie {
    private static boolean registered = false;
    private static  EntityTypes<?> registration;

    public HBGiant(EntityTypes<? extends EntityGiantZombie> var0, World var1) {
        super(EntityTypes.GIANT, var1); // This ensures the giant shows and saves as a giant, instead of some custom
                                        // invalid custom type. However, this also means that the this giant will
                                        // not be restored to this class. So, we have to be careful to ensure we
                                        // recreate the giant when coming from disk.
    }

    @Override
    protected void initAttributes() {
        super.initAttributes();
        this.getAttributeInstance(GenericAttributes.MOVEMENT_SPEED).setValue(0.23D);
        this.getAttributeInstance(GenericAttributes.ATTACK_DAMAGE).setValue(50.0D);
    }

    @Override
    protected void initPathfinder() {
        this.goalSelector.a(2, new PathfinderGoalMeleeAttack(this, 1.0D, false));
        this.goalSelector.a(8, new PathfinderGoalLookAtPlayer(this, EntityHuman.class, 8.0F));
        this.goalSelector.a(8, new PathfinderGoalRandomLookaround(this));
        this.goalSelector.a(7, new PathfinderGoalRandomStrollLand(this, 1.0D));
        this.targetSelector.a(2, new PathfinderGoalNearestAttackableTarget<>(this, EntityHuman.class, true));
        this.targetSelector.a(2, new PathfinderGoalHurtByTarget(this, new Class[0]));
    }

    private static void register() {
        if (registered) {
            return;
        }

        registered = true;

        DataFixer registry = DataConverterRegistry.a();
        Schema currentSchema = registry.getSchema(DataFixUtils.makeKey(DataMigrator.getCurrentVersion()));
        TaggedChoice.TaggedChoiceType<?> entityNameConverter = currentSchema.findChoiceType(DataConverterTypes.ENTITY);

        Map<Object, Type<?>> dataTypes = (Map<Object, Type<?>>) entityNameConverter.types();
        dataTypes.put("minecraft:hb_giant", dataTypes.get("minecraft:giant"));

        try {
            Method m = EntityTypes.class.getDeclaredMethod("a", String.class, EntityTypes.a.class);
            m.setAccessible(true);
            registration = (EntityTypes<?>) m.invoke(null, "hb_giant", EntityTypes.a.a(HBGiant::new, EnumCreatureType.MONSTER).a(3.6F, 12.0F));
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
            ex.printStackTrace();
        }
    }

    public static Giant spawn(Location loc) {
        register();

        World world = ((CraftWorld) loc.getWorld()).getHandle();
        net.minecraft.server.v1_15_R1.Entity nmsEntity = registration.a(world);
        nmsEntity.setPosition(loc.getX(), loc.getY(), loc.getZ());
        world.addEntity(nmsEntity);

        return (Giant) nmsEntity.getBukkitEntity();
    }

    public static boolean is(org.bukkit.entity.Entity entity) {
        return ((CraftEntity) entity).getHandle() instanceof HBGiant;
    }
}