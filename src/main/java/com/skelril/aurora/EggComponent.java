package com.skelril.aurora;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.session.PersistentSession;
import com.sk89q.commandbook.session.SessionComponent;
import com.sk89q.minecraft.util.commands.*;
import com.sk89q.worldedit.blocks.ItemID;
import com.sk89q.worldedit.bukkit.BukkitUtil;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.skelril.aurora.events.egg.EggDropEvent;
import com.skelril.aurora.events.egg.EggHatchEvent;
import com.skelril.aurora.util.ChanceUtil;
import com.skelril.aurora.util.ChatUtil;
import com.skelril.aurora.util.EnvironmentUtil;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@ComponentInformation(friendlyName = "Eggs", desc = "Mob Eggs")
@Depend(components = {SessionComponent.class})
public class EggComponent extends BukkitComponent implements Listener, Runnable {

    private final CommandBook inst = CommandBook.inst();
    private final Server server = CommandBook.server();

    @InjectComponent
    SessionComponent sessions;

    private WorldGuardPlugin worldGuard;
    private LocalConfiguration config;

    @Override
    public void enable() {

        config = configure(new LocalConfiguration());
        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);
        registerCommands(Commands.class);

        setUpWorldGuard();

        server.getScheduler().scheduleSyncRepeatingTask(inst, this, 20 * 2, 120);
    }

    @Override
    public void reload() {

        super.reload();
        configure(config);
    }

    private static class LocalConfiguration extends ConfigurationBase {

        @Setting("enable-easter-eggs")
        public boolean enableEasterEggs = true;
        @Setting("enable-halloween-eggs")
        public boolean enableHalloweenEggs = true;
        @Setting("strict-mode")
        public boolean strictMode = true;
    }

    @Override
    public void run() {

        for (World world : server.getWorlds()) {
            for (Item item : world.getEntitiesByClass(Item.class)) {

                ItemStack itemStack = item.getItemStack();
                if (itemStack.getTypeId() != ItemID.SPAWN_EGG || item.getTicksLived() < 300) continue;

                // Attempt to hatch the egg
                hatchEgg(item);
            }
        }
    }

    private void setUpWorldGuard() {

        Plugin plugin = server.getPluginManager().getPlugin("WorldGuard");

        // WorldGuard may not be loaded
        if (plugin == null || !(plugin instanceof WorldGuardPlugin)) {
            worldGuard = null;
        }

        //noinspection ConstantConditions
        worldGuard = (WorldGuardPlugin) plugin;
    }

    public void eggOn(Player player) {

        for (EggType eggType : EggType.values()) {
            if (eggType == EggType.INVALID) continue;
            sessions.getSession(EggState.class, player).setEggDrop(eggType, true);
        }
    }

    public void eggOn(Player player, EggType eggType) {

        if (eggType.equals(EggType.INVALID)) eggOn(player);
        else sessions.getSession(EggState.class, player).setEggDrop(eggType, true);
    }

    public boolean allowedEggs(Player player, EggType eggType) {

        return sessions.getSession(EggState.class, player).getEggDropAllowed(eggType);
    }

    public void eggOff(Player player) {

        for (EggType eggType : EggType.values()) {
            if (eggType == EggType.INVALID) continue;
            sessions.getSession(EggState.class, player).setEggDrop(eggType, false);
        }
    }

    public void eggOff(Player player, EggType eggType) {

        if (eggType.equals(EggType.INVALID)) eggOff(player);
        else sessions.getSession(EggState.class, player).setEggDrop(eggType, false);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {

        Player player = event.getPlayer();

        Block block = event.getBlock();
        World world = block.getWorld();
        int blockType = block.getTypeId();

        if (EnvironmentUtil.isShrubBlock(blockType)) {
            if (inst.hasPermission(player, "aurora.egg.easter") && allowedEggs(player, EggType.EASTER)
                    && config.enableEasterEggs && !ChanceUtil.getChance(5, 6)) {

                for (short c = 0; c < ChanceUtil.getRangedRandom(7, 17); c++) {
                    EggEntity attemptedEgg;
                    switch (ChanceUtil.getRandom(8)) {
                        case 1:
                            attemptedEgg = EggEntity.BAT;
                            break;
                        case 2:
                            attemptedEgg = EggEntity.CHICKEN;
                            break;
                        case 3:
                            attemptedEgg = EggEntity.COW;
                            break;
                        case 4:
                            attemptedEgg = EggEntity.MUSHROOM_COW;
                            break;
                        case 5:
                            attemptedEgg = EggEntity.OCELOT;
                            break;
                        case 6:
                            attemptedEgg = EggEntity.WOLF;
                            break;
                        case 7:
                            attemptedEgg = EggEntity.SHEEP;
                            break;
                        case 8:
                        default:
                            attemptedEgg = EggEntity.PIG;
                            break;
                    }
                    EggDropEvent eggDropEvent = new EggDropEvent(attemptedEgg, block.getLocation());
                    server.getPluginManager().callEvent(eggDropEvent);
                    if (!eggDropEvent.isCancelled()) {
                        world.dropItemNaturally(eggDropEvent.getLocation(), eggDropEvent.getEggType().toSpawnEgg());
                    }
                }
            }

            if (inst.hasPermission(event.getPlayer(), "aurora.egg.halloween") && allowedEggs(player, EggType.HALLOWEEN)
                    && config.enableHalloweenEggs && !ChanceUtil.getChance(7, 8)) {

                for (short c = 0; c < ChanceUtil.getRangedRandom(7, 13); c++) {
                    EggEntity attemptedEgg;
                    switch (ChanceUtil.getRandom(8)) {
                        case 1:
                            attemptedEgg = EggEntity.ENDERMAN;
                            break;
                        case 2:
                            attemptedEgg = EggEntity.SPIDER;
                            break;
                        case 3:
                            attemptedEgg = EggEntity.CAVE_SPIDER;
                            break;
                        case 4:
                            attemptedEgg = EggEntity.SLIME;
                            break;
                        case 5:
                            attemptedEgg = EggEntity.MAGMA_CUBE;
                            break;
                        case 6:
                            attemptedEgg = EggEntity.WITCH;
                            break;
                        case 7:
                            attemptedEgg = EggEntity.SKELETON;
                            break;
                        case 8:
                        default:
                            attemptedEgg = EggEntity.ZOMBIE;
                            break;
                    }
                    EggDropEvent eggDropEvent = new EggDropEvent(attemptedEgg, block.getLocation());
                    server.getPluginManager().callEvent(eggDropEvent);
                    if (!eggDropEvent.isCancelled()) {
                        world.dropItemNaturally(eggDropEvent.getLocation(), eggDropEvent.getEggType().toSpawnEgg());
                    }
                }
            }
        }
    }

    public class Commands {

        @Command(aliases = {"egg"}, desc = "Toggle Egg Drop")
        @NestedCommand(ToggleCommands.class)
        public void eggCommand(CommandContext args, CommandSender sender) throws CommandException {

        }
    }

    public class ToggleCommands {

        @Command(aliases = {"all"},
                usage = "<on/off>", desc = "Toggle Egg Drop",
                flags = "", min = 1, max = 1)
        @CommandPermissions({"aurora.egg.easter", "aurora.egg.halloween"})
        public void eggAllCommand(CommandContext args, CommandSender sender) throws CommandException {

            if (!(sender instanceof Player)) throw new CommandException("You must be a player to use this command.");

            Player player = (Player) sender;

            switch (args.getString(0).toLowerCase()) {
                case "on":
                    eggOn(player, EggType.INVALID);
                    ChatUtil.sendNotice(sender, "You are filled with holiday spirit.");
                    break;
                case "off":
                    eggOff(player, EggType.INVALID);
                    ChatUtil.sendNotice(sender, "I guess the holidays aren't for everyone...");
                    break;
                default:
                    throw new CommandUsageException("Invalid egg dropping state.", "[on/off]");
            }
        }

        @Command(aliases = {"easter"},
                usage = "<on/off>", desc = "Toggle Egg Drop",
                flags = "", min = 1, max = 1)
        @CommandPermissions({"aurora.egg.easter"})
        public void eggEasterCommand(CommandContext args, CommandSender sender) throws CommandException {

            if (!(sender instanceof Player)) throw new CommandException("You must be a player to use this command.");

            Player player = (Player) sender;

            switch (args.getString(0).toLowerCase()) {
                case "on":
                    eggOn(player, EggType.EASTER);
                    break;
                case "off":
                    eggOff(player, EggType.EASTER);
                    break;
                default:
                    throw new CommandUsageException("Invalid egg dropping state.", "[on/off]");
            }
        }

        @Command(aliases = {"halloween"},
                usage = "<on/off>", desc = "Toggle Egg Drop",
                flags = "", min = 1, max = 1)
        @CommandPermissions({"aurora.egg.halloween"})
        public void eggHalloweenCommand(CommandContext args, CommandSender sender) throws CommandException {

            if (!(sender instanceof Player)) throw new CommandException("You must be a player to use this command.");

            Player player = (Player) sender;

            switch (args.getString(0).toLowerCase()) {
                case "on":
                    eggOn(player, EggType.HALLOWEEN);
                    break;
                case "off":
                    eggOff(player, EggType.HALLOWEEN);
                    break;
                default:
                    throw new CommandUsageException("Invalid egg dropping state.", "[on/off]");
            }
        }
    }

    private boolean hatchEgg(Item egg) {

        EggHatchEvent event = new EggHatchEvent(egg, EntityType.fromId(egg.getItemStack().getData().getData()),
                egg.getLocation());
        server.getPluginManager().callEvent(event);

        if (!event.isCancelled()) {

            if (config.strictMode && worldGuard != null) {

                RegionManager mgr = worldGuard.getGlobalRegionManager().get(egg.getWorld());
                ApplicableRegionSet set = mgr.getApplicableRegions(BukkitUtil.toVector(event.getLocation()));

                if (!set.allows(DefaultFlag.MOB_SPAWNING)) {
                    event.setCancelled(true);
                    return false;
                }

                Set<EntityType> entityTypes = set.getFlag(DefaultFlag.DENY_SPAWN);
                if (entityTypes != null && entityTypes.contains(event.getEggType())) {
                    event.setCancelled(true);
                    return false;
                }
            }

            int hatchCount = 0;
            int c;
            for (c = 0; c < egg.getItemStack().getAmount(); c++) {
                if (ChanceUtil.getChance(200)) {
                    hatchCount++;
                    event.getLocation().getWorld().spawn(event.getLocation(), event.getEggType().getEntityClass());
                }
            }

            if (c - hatchCount > 0) {
                egg.getItemStack().setAmount(c - hatchCount);
            } else {
                egg.remove();
            }
            return true;
        }
        return false;
    }

    private enum EggType {
        EASTER,
        HALLOWEEN,
        INVALID
    }

    public enum EggEntity {

        // FRIENDLY - 7
        BAT(EntityType.BAT, 65),
        CHICKEN(EntityType.CHICKEN, 93),
        COW(EntityType.COW, 92),
        MUSHROOM_COW(EntityType.MUSHROOM_COW, 96),
        OCELOT(EntityType.OCELOT, 98),
        PIG(EntityType.PIG, 90),
        SHEEP(EntityType.SHEEP, 91),

        // NEUTRAL - 1
        WOLF(EntityType.WOLF, 95),

        // MEAN - 8
        ENDERMAN(EntityType.ENDERMAN, 58),
        SPIDER(EntityType.SPIDER, 52),
        CAVE_SPIDER(EntityType.CAVE_SPIDER, 59),
        SLIME(EntityType.SLIME, 55),
        MAGMA_CUBE(EntityType.MAGMA_CUBE, 62),
        WITCH(EntityType.WITCH, 66),
        SKELETON(EntityType.SKELETON, 51),
        ZOMBIE(EntityType.ZOMBIE, 54);


        private final EntityType entityType;
        private final byte networkId;

        EggEntity(EntityType entityType, int networkId) {

            this.entityType = entityType;
            this.networkId = (byte) networkId;
        }

        public EntityType getType() {

            return entityType;
        }

        public short getNetworkId() {

            return networkId;
        }

        public ItemStack toSpawnEgg(int amt) {

            return new ItemStack(ItemID.SPAWN_EGG, amt, getNetworkId());
        }

        public ItemStack toSpawnEgg() {

            return toSpawnEgg(1);
        }
    }

    private static class EggState extends PersistentSession {

        public static final long MAX_AGE = TimeUnit.DAYS.toMillis(1);

        @Setting("easter-egg")
        private boolean easterEgg = true;
        @Setting("halloween-egg")
        private boolean halloweenEgg = true;

        protected EggState() {

            super(MAX_AGE);
        }

        public boolean getEggDropAllowed(EggType eggType) {

            switch (eggType) {
                case EASTER:
                    return easterEgg;
                case HALLOWEEN:
                    return halloweenEgg;
                default:
                    return false;
            }
        }

        public void setEggDrop(EggType eggType, boolean eggDrop) {

            StringBuilder sb = new StringBuilder();

            switch (eggType) {
                case EASTER:
                    this.easterEgg = eggDrop;
                    sb.append("Easter ");
                    break;
                case HALLOWEEN:
                    this.halloweenEgg = eggDrop;
                    sb.append("Halloween ");
                    break;
            }
            sb.append("eggs are now ");
            if (eggDrop) sb.append("enabled.");
            else sb.append("disabled.");
            if (getPlayer().isOnline()) ChatUtil.sendNotice(getPlayer(), sb.toString());
        }

        public Player getPlayer() {

            CommandSender sender = super.getOwner();
            return sender instanceof Player ? (Player) sender : null;
        }
    }
}
