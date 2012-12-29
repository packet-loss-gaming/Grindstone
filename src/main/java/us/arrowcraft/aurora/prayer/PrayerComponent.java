package us.arrowcraft.aurora.prayer;
import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.session.PersistentSession;
import com.sk89q.commandbook.session.SessionComponent;
import com.sk89q.commandbook.util.PlayerUtil;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import us.arrowcraft.aurora.admin.AdminComponent;
import us.arrowcraft.aurora.events.PrayerApplicationEvent;
import us.arrowcraft.aurora.prayer.PrayerFX.*;
import us.arrowcraft.aurora.prayer.PrayerFX.Throwable;
import us.arrowcraft.aurora.util.ChatUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * @author Turtle9598
 */
@ComponentInformation(friendlyName = "Prayers", desc = "Let the light (or darkness) be unleashed on thy!")
@Depend(components = {AdminComponent.class})
public class PrayerComponent extends BukkitComponent implements Listener, Runnable {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    @InjectComponent
    private SessionComponent sessions;
    @InjectComponent
    private AdminComponent adminComponent;

    private LocalConfiguration config;

    // Player Management
    public boolean influencePlayer(Player player, Prayer... prayer) {

        for (Prayer aPrayer : prayer) {
            if (isValidPrayer(aPrayer.getPrayerType())) {
                InfluenceState session = sessions.getSession(InfluenceState.class, player);
                session.influence(aPrayer);
            } else return false;
        }
        return true;
    }

    public boolean isInfluenced(Player player) {

        return sessions.getSession(InfluenceState.class, player).isInfluenced();
    }

    public Prayer[] getInfluences(Player player) {

        return sessions.getSession(InfluenceState.class, player).getInfluences();
    }

    public void uninfluencePlayer(Player player) {

        InfluenceState session = sessions.getSession(InfluenceState.class, player);
        session.uninfluence();
    }

    public void uninfluencePlayer(Player player, Prayer prayer) {

        InfluenceState session = sessions.getSession(InfluenceState.class, player);
        session.uninfluence(prayer);
    }

    public PrayerType getPrayerByString(String prayer) {

        try {
            return PrayerType.valueOf(prayer.trim().toUpperCase());
        } catch (Exception e) {
            return null;
        }
    }

    public PrayerType getPrayerByInteger(int prayer) {

        try {
            return PrayerType.getId(prayer);
        } catch (Exception e) {
            return null;
        }
    }

    public Prayer constructPrayer(Player player, PrayerType prayerType, long maxDuration) {

        AbstractPrayer prayerEffects;
        Class triggerClass = null;

        switch (prayerType) {

            case ALONZO:
                prayerEffects = new AlonzoFX();
                break;
            case ANTIFIRE:
                prayerEffects = new AntifireFX();
                break;
            case ARROW:
                prayerEffects = new ArrowFX();
                break;
            case BLINDNESS:
                prayerEffects = new BlindnessFX();
                break;
            case BUTTERFINGERS:
                prayerEffects = new ButterFingersFX();
                break;
            case CANNON:
                prayerEffects = new CannonFX();
                break;
            case DEADLYDEFENSE:
                prayerEffects = new DeadlyDefenseFX();
                break;
            case DOOM:
                prayerEffects = new DoomFX();
                break;
            case FIRE:
                prayerEffects = new FireFX();
                break;
            case FLASH:
                prayerEffects = new FlashFX();
                break;
            case SLAP:
                prayerEffects = new SlapFX();
                break;
            case GOD:
                prayerEffects = new GodFX();
                break;
            case HEALTH:
                prayerEffects = new HealthFX();
                break;
            case INVENTORY:
                prayerEffects = new InventoryFX();
                break;
            case INVISIBILITY:
                prayerEffects = new InvisibilityFX();
                break;
            case MUSHROOM:
                prayerEffects = new MushroomFX();
                break;
            case NIGHTVISION:
                prayerEffects = new NightVisionFX();
                break;
            case POISON:
                prayerEffects = new PoisonFX();
                break;
            case POWER:
                prayerEffects = new PowerFX();
                break;
            case RACKET:
                prayerEffects = new RacketFX();
                break;
            case ROCKET:
                prayerEffects = new RocketFX();
                break;
            case SMOKE:
                prayerEffects = new SmokeFX();
                break;
            case SPEED:
                prayerEffects = new SpeedFX();
                break;
            case STARVATION:
                prayerEffects = new StarvationFX();
                break;
            case FIREBALL:
                prayerEffects = new ThrownFireballFX();
                triggerClass = PlayerInteractEvent.class;
                break;
            case TNT:
                prayerEffects = new TNTFX();
                break;
            case WALK:
                prayerEffects = new WalkFX();
                break;
            case ZOMBIE:
                prayerEffects = new ZombieFX();
                break;
            default:
                return null;
        }

        return new Prayer(player, prayerType, prayerEffects, maxDuration, triggerClass);
    }

    @Override
    public void enable() {

        config = configure(new LocalConfiguration());
        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);
        registerCommands(Commands.class);
        server.getScheduler().scheduleSyncRepeatingTask(inst, this, 20 * 2, 11);
    }

    @Override
    public void reload() {

        super.reload();
        configure(config);
    }

    private static class LocalConfiguration extends ConfigurationBase {

        @Setting("enable-unholy-nameType")
        public boolean enableUnholy = true;
        @Setting("enable-holy-nameType")
        public boolean enableHoly = true;
        @Setting("enable-god-prayer")
        public boolean enableGodPrayer = false;
        @Setting("enable-mushroom-prayer")
        public boolean enableMushroomPrayer = false;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {

        Player player = event.getEntity().getPlayer();
        if (isInfluenced(player)) {

            short count = 0;
            for (Prayer prayer : getInfluences(player)) {
                if (prayer.getPrayerType().isUnholy()) {
                    count++;
                }
            }

            if (count > 1) {
                ChatUtil.sendNotice(player, ChatColor.GOLD, "The Curses have been lifted!");
            } else if (count > 0) {
                ChatUtil.sendNotice(player, ChatColor.GOLD, "The Curse has been lifted!");
            }
            uninfluencePlayer(player);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {

        Player player = event.getPlayer();

        if (!event.getAction().equals(Action.LEFT_CLICK_AIR)) return;
        for (Prayer prayer : getInfluences(player)) {
            if (!integrityTest(player, prayer) || !(prayer.getEffect() instanceof Throwable)) continue;

            if (!prayer.getTriggerClass().equals(PlayerInteractEvent.class)) continue;

            ((Throwable) prayer.getEffect()).trigger(player);
        }
    }

    @Override
    public void run() {

        for (InfluenceState influenceState : sessions.getSessions(InfluenceState.class).values()) {
            executeInfluence(influenceState);
        }
    }


    public class Commands {

        @Command(aliases = {"pray", "pr"},
                usage = "<player> <prayer>", desc = "Pray for something to happen to the player",
                flags = "cs", min = 2, max = 2)
        public void prayerCmd(CommandContext args, CommandSender sender) throws CommandException {

            String playerString = args.getString(0);
            String prayerString = args.getString(1);
            Player player = PlayerUtil.matchSinglePlayer(sender, playerString);

            // Check for valid nameType
            if (isValidPrayer(prayerString)) {
                if (player.getName().equals(sender.getName())) {
                    adminComponent.standardizePlayer(player); // Remove Admin & Guild
                    uninfluencePlayer(player);                // Remove any other Prayers
                    player.getWorld().strikeLightningEffect(player.getLocation());
                    player.setFireTicks(20 * 60);
                    throw new CommandException("The gods don't take kindly to using their power on yourself.");
                }

                if (getPrayerByString(prayerString).isUnholy()) {
                    inst.checkPermission(sender, "aurora.pray.unholy." + prayerString);
                    if (!config.enableUnholy) throw new CommandException("Unholy prayers are not currently enabled.");
                    ChatUtil.sendNotice(sender, ChatColor.DARK_RED + "The player: " + player.getDisplayName()
                            + " has been smited!");
                } else {
                    inst.checkPermission(sender, "aurora.pray.holy." + prayerString);
                    if (!config.enableHoly) throw new CommandException("Holy prayers are not currently enabled.");
                    ChatUtil.sendNotice(sender, ChatColor.GOLD + "The player: " + player.getDisplayName()
                            + " has been blessed!");
                }

                if (args.hasFlag('c')) uninfluencePlayer(player);
                Prayer prayer = constructPrayer(player, getPrayerByString(prayerString), TimeUnit.MINUTES.toMillis(30));
                influencePlayer(player, prayer);

                if (!args.hasFlag('s')) return;
                if (prayer.getPrayerType().equals(PrayerType.GOD)) {
                    Bukkit.broadcastMessage(ChatColor.GOLD + "A god has taken the form of " + player.getName() + "!");
                } else if (prayer.getPrayerType().equals(PrayerType.POWER)) {
                    Bukkit.broadcastMessage(ChatColor.GOLD + "The true power of " + player.getName() + " has been " +
                            "awakened!");
                }

            } else {
                throw new CommandException("That is not a valid prayer!");
            }
        }
    }

    private boolean isValidPrayer(PrayerType prayerType) {

        return prayerType != null && !(!config.enableGodPrayer && prayerType.equals(PrayerType.GOD)
                || !config.enableMushroomPrayer && prayerType.equals(PrayerType.MUSHROOM));
    }

    private boolean isValidPrayer(int prayerNumber) {

        return isValidPrayer(getPrayerByInteger(prayerNumber));
    }

    private boolean isValidPrayer(String prayerString) {

        return isValidPrayer(getPrayerByString(prayerString));
    }

    private boolean integrityTest(Player player, Prayer prayer) {
        // Continue if the prayer is out of date
        if (System.currentTimeMillis() - prayer.getStartTime() >= prayer.getMaxDuration()) {

            uninfluencePlayer(player, prayer);
            return false;
        } else if ((prayer.getPrayerType().isUnholy() && !config.enableUnholy)
                || (prayer.getPrayerType().isHoly() && !config.enableHoly)) {

            uninfluencePlayer(player, prayer);
            return false;
        } else if (prayer.getPrayerType().isUnholy()) {

            adminComponent.standardizePlayer(player);
        }

        return true;
    }

    private void executeInfluence(InfluenceState influenceState) {

        try {
            if (!influenceState.isInfluenced()) return;

            Player player = influenceState.getPlayer();

            // Stop this from breaking if the player isn't here
            if (player == null || !player.isOnline() || player.isDead() || player.isInsideVehicle()) return;

            for (Prayer prayer : getInfluences(player)) {

                if (!integrityTest(player, prayer)) continue;

                prayer.getEffect().clean(player);

                // Run our event
                PrayerApplicationEvent event = new PrayerApplicationEvent(player, prayer);
                server.getPluginManager().callEvent(event);

                if (event.isCancelled()) continue;

                prayer.getEffect().add(player);
            }

        } catch (Exception e) {
            log.warning("The influence state of: " + influenceState.getPlayer().getName()
                    + " was not executed by the: " + this.getInformation().friendlyName() + " component.");
            e.printStackTrace();
        }
    }

    // Prayer Session
    private static class InfluenceState extends PersistentSession {

        public static final long MAX_AGE = TimeUnit.MINUTES.toMillis(30);

        private List<Prayer> prayers = new ArrayList<>();

        protected InfluenceState() {

            super(MAX_AGE);
        }

        public boolean isInfluenced() {

            return getInfluences().length > 0;
        }

        public final Prayer[] getInfluences() {

            return prayers.toArray(new Prayer[prayers.size()]);
        }

        public void influence(Prayer prayer) {

            prayers.add(prayer);
        }

        public void uninfluence(Prayer prayer) {

            if (prayers.contains(prayer)) prayers.remove(prayer);
            prayer.getEffect().clean(getPlayer());
        }

        public void uninfluence() {

            for (Prayer prayer : prayers) {
                prayer.getEffect().clean(getPlayer());
            }

            prayers.clear();
        }

        public Player getPlayer() {

            CommandSender sender = super.getOwner();
            return sender instanceof Player ? (Player) sender : null;
        }
    }
}