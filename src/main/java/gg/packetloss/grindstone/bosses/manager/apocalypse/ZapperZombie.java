package gg.packetloss.grindstone.bosses.manager.apocalypse;

import com.sk89q.commandbook.CommandBook;
import com.skelril.OSBL.bukkit.entity.BukkitBoss;
import com.skelril.OSBL.bukkit.util.BukkitUtil;
import com.skelril.OSBL.entity.LocalControllable;
import com.skelril.OSBL.entity.LocalEntity;
import com.skelril.OSBL.instruction.*;
import com.skelril.OSBL.util.AttackDamage;
import gg.packetloss.grindstone.bosses.detail.GenericDetail;
import gg.packetloss.grindstone.bosses.impl.SimpleRebindableBoss;
import gg.packetloss.grindstone.items.custom.CustomItemCenter;
import gg.packetloss.grindstone.items.custom.CustomItems;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.item.ItemUtil;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.EntityEquipment;

import java.util.List;
import java.util.logging.Logger;

public class ZapperZombie {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    private SimpleRebindableBoss<Zombie> zapperZombie;

    public static final String BOUND_NAME = "Zapper Zombie";

    public ZapperZombie() {
        zapperZombie = new SimpleRebindableBoss<>(BOUND_NAME, Zombie.class, inst, new SimpleInstructionDispatch<>());
        setupZapperZombie();
    }

    public void bind(Damageable entity) {
        zapperZombie.bind(new BukkitBoss<>(entity, new GenericDetail()));
    }

    private void setupZapperZombie() {
        List<BindInstruction<GenericDetail>> bindInstructions = zapperZombie.bindInstructions;
        bindInstructions.add(new BindInstruction<>() {
            @Override
            public InstructionResult<GenericDetail, BindInstruction<GenericDetail>> process(LocalControllable<GenericDetail> controllable) {
                Entity anEntity = BukkitUtil.getBukkitEntity(controllable);
                if (anEntity instanceof Zombie) {
                    anEntity.setCustomName(BOUND_NAME);

                    // Ensure zapper zombies are sometimes babies
                    ((Zombie) anEntity).setBaby(ChanceUtil.getChance(3));

                    // Ensure thor zombies cannot pickup items, they're just suicide "bombers"
                    ((Zombie) anEntity).setCanPickupItems(false);

                    // Set health
                    ((LivingEntity) anEntity).setMaxHealth(20);
                    ((LivingEntity) anEntity).setHealth(20);

                    // Gear them up
                    EntityEquipment equipment = ((LivingEntity) anEntity).getEquipment();
                    equipment.setArmorContents(ItemUtil.GOLD_ARMOR);
                }
                return null;
            }
        });

        List<UnbindInstruction<GenericDetail>> unbindInstructions = zapperZombie.unbindInstructions;
        unbindInstructions.add(new UnbindInstruction<>() {
            @Override
            public InstructionResult<GenericDetail, UnbindInstruction<GenericDetail>> process(LocalControllable<GenericDetail> controllable) {
                Entity boss = BukkitUtil.getBukkitEntity(controllable);
                Location target = boss.getLocation();

                if (ChanceUtil.getChance(1000)) {
                    target.getWorld().dropItem(target, CustomItemCenter.build(CustomItems.TOME_OF_THE_RIFT_SPLITTER));
                }

                return null;
            }
        });

        List<DamageInstruction<GenericDetail>> damageInstructions = zapperZombie.damageInstructions;
        damageInstructions.add(new DamageInstruction<>() {
            @Override
            public InstructionResult<GenericDetail, DamageInstruction<GenericDetail>> process(LocalControllable<GenericDetail> controllable, LocalEntity entity, AttackDamage damage) {
                Entity boss = BukkitUtil.getBukkitEntity(controllable);
                ((LivingEntity) boss).setHealth(0);

                for (int i =  ((Zombie) boss).isBaby() ? 1 : 5; i > 0; --i) {
                    boss.getWorld().strikeLightning(boss.getLocation());
                }

                zapperZombie.silentUnbind(controllable);
                return null;
            }
        });
    }
}
