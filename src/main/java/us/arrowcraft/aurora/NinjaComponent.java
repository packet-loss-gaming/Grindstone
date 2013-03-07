package us.arrowcraft.aurora;
import com.petrifiednightmares.pitfall.PitfallEvent;
import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.session.PersistentSession;
import com.sk89q.commandbook.session.SessionComponent;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.sk89q.worldedit.blocks.BlockID;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import us.arrowcraft.aurora.events.PrayerApplicationEvent;
import us.arrowcraft.aurora.util.ChatUtil;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

@ComponentInformation(friendlyName = "Ninja", desc = "Disappear into the night!")
@Depend(components = {SessionComponent.class, RogueComponent.class})
public class NinjaComponent extends BukkitComponent implements Listener, Runnable {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    @InjectComponent
    private SessionComponent sessions;
    @InjectComponent
    private RogueComponent rogueComponent;

    private final int WATCH_DISTANCE = 10;
    private final int WATCH_DISTANCE_SQ = WATCH_DISTANCE * WATCH_DISTANCE;
    private final int SNEAK_WATCH_DISTANCE = 3;
    private final int SNEAK_WATCH_DISTANCE_SQ = SNEAK_WATCH_DISTANCE * SNEAK_WATCH_DISTANCE;

    @Override
    public void enable() {

        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);
        registerCommands(Commands.class);
        server.getScheduler().scheduleSyncRepeatingTask(inst, this, 20 * 2, 11);
    }

    // Player Management
    public void ninjaPlayer(Player player) {

        sessions.getSession(NinjaState.class, player).setIsNinja(true);
    }

    public boolean isNinja(Player player) {

        return sessions.getSession(NinjaState.class, player).isNinja();
    }

    public void showToGuild(Player player) {

        sessions.getSession(NinjaState.class, player).showToGuild(true);
    }

    public boolean isShownToGuild(Player player) {

        return sessions.getSession(NinjaState.class, player).guildCanSee();
    }

    public void hideFromGuild(Player player) {

        sessions.getSession(NinjaState.class, player).showToGuild(false);
    }

    public void unninjaPlayer(Player player) {

        NinjaState session = sessions.getSession(NinjaState.class, player);
        session.setIsNinja(false);
        session.showToGuild(false);

        player.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
        player.removePotionEffect(PotionEffectType.WATER_BREATHING);
        player.removePotionEffect(PotionEffectType.FIRE_RESISTANCE);

        for (final Player otherPlayer : server.getOnlinePlayers()) {
            // Show Yourself!
            if (otherPlayer != player)
                otherPlayer.showPlayer(player);
        }
    }

    // Stop Mobs from targeting ninja
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityTarget(EntityTargetEvent event) {

        Entity entity = event.getEntity();
        Entity targetEntity = event.getTarget();

        if (entity instanceof Player || !(targetEntity instanceof Player)) return;

        Player player = (Player) targetEntity;

        if (isNinja(player) && player.isSneaking() && !player.getWorld().isThundering()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerRespawnEvent event) {

        Player player = event.getPlayer();

        for (final Player otherPlayer : server.getOnlinePlayers()) {
            if (otherPlayer != player) otherPlayer.showPlayer(player);
        }
    }

    @EventHandler
    public void onPitfallEvent(PitfallEvent event) {

        Entity entity = event.getCause();

        if (entity instanceof Player) {
            Player player = (Player) entity;

            if (isNinja(player) && player.isSneaking() && inst.hasPermission(player, "aurora.ninja.guild")
                    && event.getNewTypeIdB() == BlockID.AIR
                    && event.getNewTypeIdH() == BlockID.AIR) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPrayerApplication(PrayerApplicationEvent event) {

        if (isNinja(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @Override
    public void run() {

        for (NinjaState ninjaState : sessions.getSessions(NinjaState.class).values()) {
            if (!ninjaState.isNinja()) {
                continue;
            }

            Player player = ninjaState.getPlayer();

            // Stop this from breaking if the player isn't here
            if (player == null || !player.isOnline() || player.isDead()) continue;

            if (inst.hasPermission(player, "aurora.ninja.guild")) {
                player.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
                player.removePotionEffect(PotionEffectType.WATER_BREATHING);
                player.removePotionEffect(PotionEffectType.FIRE_RESISTANCE);
                player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20 * 600, 2));
                player.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, 20 * 600, 2));
                player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 20 * 600, 2));
            }

            Set<Player> invisibleNewCount = new HashSet<>();
            Set<Player> visibleNewCount = new HashSet<>();

            for (Player otherPlayer : server.getOnlinePlayers()) {
                if (otherPlayer != player) {
                    if (otherPlayer.getWorld().equals(player.getWorld())) {
                        if (player.getLocation().distanceSquared(otherPlayer.getLocation()) >= WATCH_DISTANCE_SQ
                                || (player.getLocation().distanceSquared(otherPlayer.getLocation()) >=
                                SNEAK_WATCH_DISTANCE_SQ
                                && player.isSneaking())) {
                            if (otherPlayer.canSee(player)
                                    && !(isShownToGuild(player) && otherPlayer.hasPermission("aurora.ninja.guild"))
                                    && !inst.hasPermission(otherPlayer, "aurora.ninja.guild.master")) {
                                otherPlayer.hidePlayer(player);
                                invisibleNewCount.add(otherPlayer);
                            }
                        } else {
                            if (!otherPlayer.canSee(player)) {
                                otherPlayer.showPlayer(player);
                                visibleNewCount.add(otherPlayer);
                            }
                        }
                    } else {
                        if (!otherPlayer.canSee(player)) {
                            otherPlayer.showPlayer(player);
                            visibleNewCount.add(otherPlayer);
                        }
                    }
                }
            }

            if (invisibleNewCount.size() > 0) {
                if (invisibleNewCount.size() > 3) {
                    ChatUtil.sendNotice(player, "You are now invisible to multiple players.");
                } else {
                    for (Player playerThatCanNotSeePlayer : invisibleNewCount) {
                        ChatUtil.sendNotice(player, "You are now invisible to "
                                + playerThatCanNotSeePlayer.getDisplayName() + ".");
                    }
                }
            }

            if (visibleNewCount.size() > 0) {
                if (visibleNewCount.size() > 3) {
                    ChatUtil.sendNotice(player, "You are now visible to multiple players.");
                } else {
                    for (Player playerThatCanSeePlayer : visibleNewCount) {
                        ChatUtil.sendNotice(player, "You are now visible to "
                                + playerThatCanSeePlayer.getDisplayName() + ".");
                    }
                }
            }
        }
    }

    public class Commands {

        @Command(aliases = {"ninja"}, desc = "Give a player the Ninja power",
                flags = "g", min = 0, max = 0)
        @CommandPermissions({"aurora.ninja"})
        public void ninja(CommandContext args, CommandSender sender) throws CommandException {

            if (!(sender instanceof Player)) throw new CommandException("You must be a player to use this command.");
            if (inst.hasPermission(sender, "aurora.rogue.guild") || rogueComponent.isRogue((Player) sender)) {
                throw new CommandException("You are a rogue not a ninja!");
            }

            ninjaPlayer((Player) sender);
            if (args.hasFlag('g') && inst.hasPermission(sender, "aurora.ninja.guild")) {
                showToGuild((Player) sender);
            } else if (!args.hasFlag('g')) {
                hideFromGuild((Player) sender);
            } else {
                ChatUtil.sendError(sender, "You must be a member of the ninja guild to use this flag.");
            }
            ChatUtil.sendNotice(sender, "You are inspired and become a ninja!");
        }

        @Command(aliases = {"unninja"}, desc = "Revoke a player's Ninja power",
                flags = "", min = 0, max = 0)
        @CommandPermissions({"aurora.ninja"})
        public void unninja(CommandContext args, CommandSender sender) throws CommandException {

            if (!(sender instanceof Player)) throw new CommandException("You must be a player to use this command.");
            if (inst.hasPermission(sender, "aurora.rogue.guild")) {
                throw new CommandException("You are a rogue not a ninja!");
            }

            unninjaPlayer((Player) sender);

            ChatUtil.sendNotice(sender, "You return to your previous boring existence.");
        }
    }

    // Ninja Session
    private static class NinjaState extends PersistentSession {

        public static final long MAX_AGE = TimeUnit.DAYS.toMillis(1);

        private boolean isNinja = false;
        private boolean showToGuild = false;

        protected NinjaState() {

            super(MAX_AGE);
        }

        public boolean isNinja() {

            return isNinja;
        }

        public void setIsNinja(boolean isNinja) {

            this.isNinja = isNinja;
        }

        public boolean guildCanSee() {

            return showToGuild;
        }

        public void showToGuild(boolean showToGuild) {

            this.showToGuild = showToGuild;
        }

        public Player getPlayer() {

            CommandSender sender = super.getOwner();
            return sender instanceof Player ? (Player) sender : null;
        }
    }
}