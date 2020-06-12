package gg.packetloss.grindstone.city.engine.area.areas.RogueChaser;

import com.destroystokyo.paper.ParticleBuilder;
import com.sk89q.commandbook.CommandBook;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import gg.packetloss.grindstone.city.engine.area.AreaComponent;
import gg.packetloss.grindstone.guild.GuildComponent;
import gg.packetloss.grindstone.managedworld.ManagedWorldComponent;
import gg.packetloss.grindstone.managedworld.ManagedWorldGetQuery;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.LocationUtil;
import gg.packetloss.grindstone.util.bridge.WorldGuardBridge;
import gg.packetloss.grindstone.util.checker.NonSolidRegionChecker;
import gg.packetloss.grindstone.util.listener.FlightBlockingListener;
import org.apache.commons.lang.Validate;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.concurrent.TimeUnit;

@ComponentInformation(friendlyName = "Rogue Chaser", desc = "Rogue chaser")
@Depend(components = {ManagedWorldComponent.class, GuildComponent.class},
        plugins = {"WorldGuard"})
public class RogueChaser extends AreaComponent<RogueChaserConfig> {
    @InjectComponent
    private ManagedWorldComponent managedWorld;
    @InjectComponent
    private GuildComponent guild;

    private static final int FLOOR_LEVEL = 54;

    private ArmorStand chased = null;

    private long chasedSpawnTime = System.currentTimeMillis();

    public RogueChaser() {
        super(1);
    }

    @Override
    public void setUp() {
        world = managedWorld.get(ManagedWorldGetQuery.CITY);
        region = WorldGuardBridge.getManagerFor(world).getRegion("carpe-diem-district-rogue-guild-chased");
        tick = 1;
        listener = new RogueChaserListener(this);
        config = new RogueChaserConfig();

        CommandBook.registerEvents(new FlightBlockingListener(this::contains));
    }

    @Override
    public void disable() {
        removeChased();
    }

    private boolean isChasedDown() {
        if (chased == null) {
            return true;
        }

        if (!contains(chased)) {
            chased.remove();
        }

        return !chased.isValid();
    }

    private void spawnChased() {
        Validate.isTrue(isChasedDown());

        Location spawnPoint = LocationUtil.pickLocation(world, FLOOR_LEVEL, new NonSolidRegionChecker(region, world));

        chased = world.spawn(spawnPoint, ArmorStand.class);
        chased.getEquipment().setHelmet(new ItemStack(Material.DRAGON_HEAD));
        chased.setVisible(false);

        chasedSpawnTime = System.currentTimeMillis();
    }

    private void removeChased() {
        if (chased != null) {
            chased.remove();
        }
        chased = null;

        getContained(ArmorStand.class).forEach(Entity::remove);
    }

    private void moveChased() {
        chased.setVelocity(chased.getVelocity().add(new Vector(
                ChanceUtil.getRangedRandom(-config.chasedSpeed, config.chasedSpeed),
                (ChanceUtil.getChance(config.chanceOfJump) ? 2 : 0),
                ChanceUtil.getRangedRandom(-config.chasedSpeed, config.chasedSpeed)
        )));
        chased.setHeadPose(chased.getHeadPose().add(0, .1, 0));

        Location particleLoc = chased.getEyeLocation().add(0, -.5, 0);
        new ParticleBuilder(Particle.LAVA).count(3).location(particleLoc).allPlayers().spawn();
    }

    protected boolean isChased(Entity entity) {
        return entity.equals(chased);
    }

    protected void chasedHitBy(Player player) {
        removeChased();

        guild.getState(player).ifPresent(guildState -> {
            long timeTaken = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - chasedSpawnTime);

            long baseExp = config.baseXp + (getContainedParticipants().size() * config.playerCountXpModifier);
            long expGranted = Math.max(baseExp - timeTaken, config.minXp);
            if (guildState.grantExp(expGranted)) {
                ChatUtil.sendNotice(
                        getAudiblePlayers(),
                        player.getDisplayName() + " got the dragon for " +
                                ChatColor.WHITE + expGranted +
                                ChatColor.YELLOW + " experience!"
                );
            }
        });
    }

    @Override
    public void run() {
        if (isEmpty()) {
            if (!isChasedDown()) {
                removeChased();
            }

            return;
        }

        if (isChasedDown()) {
            spawnChased();
        }

        moveChased();
    }
}
