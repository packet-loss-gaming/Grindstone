package com.skelril.aurora.prayer;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.session.PersistentSession;
import com.sk89q.commandbook.session.SessionComponent;
import com.sk89q.commandbook.util.PlayerUtil;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandUsageException;
import com.skelril.aurora.admin.AdminComponent;
import com.skelril.aurora.events.PrayerApplicationEvent;
import com.skelril.aurora.exceptions.InvalidPrayerException;
import com.skelril.aurora.exceptions.UnsupportedPrayerException;
import com.skelril.aurora.prayer.PrayerFX.*;
import com.skelril.aurora.util.ChatUtil;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.*;
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
    private List<PrayerType> disabledPrayers = new ArrayList<>();

    // Player Management
    public boolean influencePlayer(Player player, Prayer... prayer) {

        List<PrayerType> disabled = Collections.unmodifiableList(disabledPrayers);
        for (Prayer aPrayer : prayer) {
            if (!disabled.contains(aPrayer.getEffect().getType())) {
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

    public PrayerType getPrayerByString(String prayer) throws InvalidPrayerException {

        try {
            return PrayerType.valueOf(prayer.trim().toUpperCase());
        } catch (Exception e) {
            throw new InvalidPrayerException();
        }
    }

    public PrayerType getPrayerByInteger(int prayer) throws InvalidPrayerException {

        try {
            return PrayerType.getId(prayer);
        } catch (Exception e) {
            throw new InvalidPrayerException();
        }
    }

    public Prayer constructPrayer(Player player, PrayerType type, long maxDuration) throws UnsupportedPrayerException {

        Validate.notNull(player);
        Validate.notNull(type);
        Validate.notNull(maxDuration);

        AbstractPrayer prayerEffects;

        switch (type) {

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
            case DIGGYDIGGY:
                prayerEffects = new DiggyDiggyFX();
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
                throw new UnsupportedPrayerException();
        }

        return new Prayer(player, prayerEffects, maxDuration);
    }

    @Override
    public void enable() {

        config = configure(new LocalConfiguration());
        refreshDisabled();
        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);
        registerCommands(Commands.class);
        server.getScheduler().scheduleSyncRepeatingTask(inst, this, 20 * 2, 11);
    }

    @Override
    public void reload() {

        super.reload();
        configure(config);
        refreshDisabled();
    }

    private static class LocalConfiguration extends ConfigurationBase {

        @Setting("enable-unholy-nameType")
        public boolean enableUnholy = true;
        @Setting("enable-holy-nameType")
        public boolean enableHoly = true;
        @Setting("disabled-prayers")
        public Set<String> disabled = new HashSet<>(Arrays.asList(
                "god", "mushroom"
        ));
    }

    private void refreshDisabled() {

        disabledPrayers.clear();
        disabledPrayers.add(PrayerType.UNASSIGNED);
        for (String string : config.disabled) {
            try {
                disabledPrayers.add(getPrayerByString(string));
            } catch (InvalidPrayerException ex) {
                log.warning("The prayer: " + string + " is not valid.");
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {

        Player player = event.getEntity().getPlayer();
        if (isInfluenced(player)) {

            short count = 0;
            for (Prayer prayer : getInfluences(player)) {
                if (prayer.getEffect().getType().isUnholy()) {
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
            if (!integrityTest(player, prayer) || !(prayer.getEffect() instanceof AbstractTriggeredPrayer)) continue;

            if (!prayer.getTriggerClass().equals(PlayerInteractEvent.class)) continue;

            ((AbstractTriggeredPrayer) prayer.getEffect()).trigger(player);
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
                flags = "csl", min = 0, max = 2)
        public void prayerCmd(CommandContext args, CommandSender sender) throws CommandException {

            String playerString;
            String prayerString;
            Player player;

            if (args.argsLength() < 2) {
                if (args.hasFlag('l')) {
                    int quantity = 0;
                    StringBuilder sb = new StringBuilder();
                    sb.append(ChatColor.YELLOW + "Valid prayers: ");
                    for (PrayerType prayer : PrayerType.values()) {
                        if (disabledPrayers.contains(prayer)) continue;
                        if (prayer.isHoly()) {
                            if (!inst.hasPermission(sender, "aurora.pray.holy." + prayer.toString().toLowerCase())) {
                                continue;
                            }
                        } else {
                            if (!inst.hasPermission(sender, "aurora.pray.unholy." + prayer.toString().toLowerCase())) {
                                continue;
                            }
                        }
                        if (quantity > 0) sb.append(ChatColor.YELLOW + ", ");
                        sb.append(prayer.isHoly() ? ChatColor.BLUE
                                                  : ChatColor.RED).append(prayer.toString().toLowerCase());
                        quantity++;
                    }
                    sb.append(ChatColor.YELLOW + ".");
                    ChatUtil.sendNotice(sender, sb.toString());
                    return;
                } else {
                    throw new CommandUsageException("Too few arguments.", "/pray [csl] <player> <prayer>");
                }
            } else {
                playerString = args.getString(0);
                prayerString = args.getString(1).toLowerCase();
                player = PlayerUtil.matchSinglePlayer(sender, playerString);
            }

            // Check for valid nameType
            try {

                if (sender instanceof BlockCommandSender) {
                    commandBlock(player, prayerString);
                    return;
                }

                if (player.getName().equals(sender.getName()) && adminComponent.standardizePlayer(player)) {
                    uninfluencePlayer(player);
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

                if (args.hasFlag('s')) return;
                if (prayer.getEffect().getType().equals(PrayerType.GOD)) {
                    Bukkit.broadcastMessage(ChatColor.GOLD + "A god has taken the form of " + player.getName() + "!");
                } else if (prayer.getEffect().getType().equals(PrayerType.POWER)) {
                    Bukkit.broadcastMessage(ChatColor.GOLD + "The true power of " + player.getName() + " has been " +
                            "awakened!");
                }
            } catch (InvalidPrayerException | UnsupportedPrayerException ex) {
                throw new CommandException("That is not a valid prayer!");
            }
        }
    }

    private void commandBlock(Player player, String prayerString) throws InvalidPrayerException, UnsupportedPrayerException {

        Prayer prayer = constructPrayer(player, getPrayerByString(prayerString), TimeUnit.MINUTES.toMillis(1));
        influencePlayer(player, prayer);
    }

    private boolean integrityTest(Player player, Prayer prayer) {
        // Continue if the prayer is out of date
        if (System.currentTimeMillis() - prayer.getStartTime() >= prayer.getMaxDuration()) {

            uninfluencePlayer(player, prayer);
            return false;
        } else if ((prayer.getEffect().getType().isUnholy() && !config.enableUnholy)
                || (prayer.getEffect().getType().isHoly() && !config.enableHoly)) {

            uninfluencePlayer(player, prayer);
            return false;
        } else if (prayer.getEffect().getType().isUnholy()) {

            if (!adminComponent.standardizePlayer(player)) uninfluencePlayer(player);
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

            return prayers.size() > 0;
        }

        public Prayer[] getInfluences() {

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