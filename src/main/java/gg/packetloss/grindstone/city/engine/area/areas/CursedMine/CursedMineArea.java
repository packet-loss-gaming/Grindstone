package gg.packetloss.grindstone.city.engine.area.areas.CursedMine;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import gg.packetloss.grindstone.admin.AdminComponent;
import gg.packetloss.grindstone.city.engine.area.AreaComponent;
import gg.packetloss.grindstone.exceptions.UnsupportedPrayerException;
import gg.packetloss.grindstone.highscore.HighScoresComponent;
import gg.packetloss.grindstone.prayer.PrayerComponent;
import gg.packetloss.grindstone.prayer.PrayerFX.InventoryFX;
import gg.packetloss.grindstone.prayer.PrayerType;
import gg.packetloss.grindstone.spectator.SpectatorComponent;
import gg.packetloss.grindstone.state.block.BlockStateComponent;
import gg.packetloss.grindstone.state.block.BlockStateKind;
import gg.packetloss.grindstone.state.player.PlayerStateKind;
import gg.packetloss.grindstone.util.ChanceUtil;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.LocationUtil;
import gg.packetloss.grindstone.util.SkullPlacer;
import gg.packetloss.grindstone.util.bridge.WorldEditBridge;
import gg.packetloss.grindstone.util.bridge.WorldGuardBridge;
import gg.packetloss.grindstone.util.explosion.ExplosionStateFactory;
import gg.packetloss.grindstone.util.item.ItemUtil;
import gg.packetloss.grindstone.util.listener.FlightBlockingListener;
import gg.packetloss.grindstone.util.listener.InventorySlotBlockingListener;
import gg.packetloss.grindstone.util.region.RegionWalker;
import gg.packetloss.grindstone.util.restoration.RestorationUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.*;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static gg.packetloss.grindstone.util.bridge.WorldEditBridge.toBlockVec3;

@ComponentInformation(friendlyName = "Cursed Mine", desc = "Dave says hi")
@Depend(components = {
        AdminComponent.class, BlockStateComponent.class, HighScoresComponent.class,
        PrayerComponent.class, RestorationUtil.class, SpectatorComponent.class
})
public class CursedMineArea extends AreaComponent<CursedMineConfig> {

    @InjectComponent
    protected AdminComponent admin;
    @InjectComponent
    protected BlockStateComponent blockState;
    @InjectComponent
    protected HighScoresComponent highScores;
    @InjectComponent
    protected PrayerComponent prayer;
    @InjectComponent
    protected RestorationUtil restorationUtil;
    @InjectComponent
    protected SpectatorComponent spectator;

    protected ProtectedRegion floodGate_RG;
    protected ProtectedRegion skullsEast_RG;
    protected ProtectedRegion skullsNorth_RG;
    protected ProtectedRegion skullsWest_RG;

    protected final long lastActivationTime = 18000;
    protected long lastActivation = 0;
    protected Map<UUID, Long> daveHitList = new HashMap<>();

    protected static Set<Material> AFFECTED_MATERIALS = new HashSet<>();

    static {
        // Automatically compute the list of affected materials based on drain rate
        for (Material type : Material.values()) {
            if (type.isLegacy()) {
                continue;
            }

            if (getMaxStackDrain(type) > 0) {
                AFFECTED_MATERIALS.add(type);
            }
        }
    }

    @Override
    public void setUp() {
        spectator.registerSpectatorKind(PlayerStateKind.CURSED_MINE_SPECTATOR);

        world = server.getWorlds().get(0);

        RegionManager manager = WorldGuardBridge.getManagerFor(world);
        String base = "oblitus-district-cursed-mine";
        region = manager.getRegion(base);
        floodGate_RG = manager.getRegion(base + "-flood-gate");
        skullsEast_RG = manager.getRegion(base + "-deaths-east");
        skullsNorth_RG = manager.getRegion(base + "-deaths-north");
        skullsWest_RG = manager.getRegion(base + "-deaths-west");
        tick = 4 * 20;
        listener = new CursedMineListener(this);
        config = new CursedMineConfig();

        CommandBook.registerEvents(new FlightBlockingListener(admin, this::contains));
        CommandBook.registerEvents(new InventorySlotBlockingListener(
                this::isParticipant,
                (st) -> st == InventoryType.SlotType.CRAFTING
        ));

        spectator.registerSpectatedRegion(PlayerStateKind.CURSED_MINE_SPECTATOR, region);
        spectator.registerSpectatorSkull(
                PlayerStateKind.CURSED_MINE_SPECTATOR,
                new Location(world, 353, 66, -529),
                () -> getContainedParticipants().size() > 1
        );
    }

    @Override
    public void run() {
        removeExpiredHitlistEntries();

        drain();
        trySpawnBlazes();
        restoreReadyBlocks();

        if (!isEmpty()) {
            changeWater();
        }
    }

    private void removeExpiredHitlistEntries() {
        daveHitList.entrySet().removeIf(stringLongEntry -> stringLongEntry.getValue() >= System.currentTimeMillis());
    }

    public void addToHitList(Player player) {
        daveHitList.put(player.getUniqueId(), System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(10));
    }

    public void removeFromHitList(Player player) {
        daveHitList.remove(player.getUniqueId());
    }

    public boolean isOnHitList(Player player) {
        return daveHitList.containsKey(player.getUniqueId());
    }

    public void addSkull(Player player) {
        ProtectedRegion r;
        BlockFace direction;

        switch (ChanceUtil.getRandom(3)) {
            case 1:
                r = skullsEast_RG;
                direction = BlockFace.WEST;
                break;
            case 2:
                r = skullsNorth_RG;
                direction = BlockFace.SOUTH;
                break;
            case 3:
                r = skullsWest_RG;
                direction = BlockFace.EAST;
                break;
            default:
                return;
        }

        if (r != null) {
            Location l = LocationUtil.pickLocation(getWorld(), r.getMinimumPoint().getY(), r.getMinimumPoint(), r.getMaximumPoint());
            SkullPlacer.placePlayerSkullOnGround(l, direction, player);
        }
    }

    private static int getMaxStackDrain(Material type) {
        switch (type) { // grouped by drain category, ordered by mine depth
            case IRON_BLOCK:
            case GOLD_BLOCK:
            case REDSTONE_BLOCK:
            case LAPIS_BLOCK:
            case DIAMOND_BLOCK:
            case EMERALD_BLOCK:
                return 2;

            case IRON_ORE:
            case GOLD_ORE:
            case REDSTONE_ORE:
            case LAPIS_ORE:
            case DIAMOND_ORE:
            case EMERALD_ORE:
                return 4;

            case IRON_INGOT:
            case GOLD_INGOT:
            case DIAMOND:
            case EMERALD:
                return 8;

            case REDSTONE:
            case LAPIS_LAZULI:
                return 34;

            case GOLD_NUGGET:
            case IRON_NUGGET:
                return 80;

            default:
                return 0;
        }
    }

    public int getMaxStackDrain(ItemStack stack) {
        if (stack == null || ItemUtil.isNamed(stack)) {
            return 0;
        }

        return getMaxStackDrain(stack.getType());
    }

    private boolean checkInventory(Player player, ItemStack[] itemStacks) {
        if (!inst.hasPermission(player, "aurora.tome.divinity")) return false;

        for (ItemStack stack : itemStacks) {
            if (getMaxStackDrain(stack) > 0) {
                return true;
            }
        }

        return false;
    }

    private void drainPlayers() {
        for (Entity e : getContained(InventoryHolder.class)) {
            Inventory eInventory = ((InventoryHolder) e).getInventory();

            if (e instanceof Player) {
                if (!isParticipant((Player) e)) {
                    continue;
                }

                boolean hasItems = checkInventory((Player) e, eInventory.getContents());
                if (hasItems && ChanceUtil.getChance(15)) {
                    ChatUtil.sendNotice(e, "Divine intervention protects some of your items.");
                    continue;
                }
            }

            for (int i = 0; i < eInventory.getSize(); ++i) {
                ItemStack stack = eInventory.getItem(i);

                int maxDrainAmount = getMaxStackDrain(stack);
                if (maxDrainAmount == 0) {
                    continue;
                }

                assert stack != null;

                int drainAmount = ChanceUtil.getRandom(maxDrainAmount);
                int newAmt = stack.getAmount() - drainAmount;
                if (newAmt < 1) {
                    eInventory.setItem(i, null);
                } else {
                    stack.setAmount(newAmt);
                }
            }
        }
    }

    private void drainFloor() {
        for (Item item : getContained(Item.class)) {
            if (!contains(item)) continue;

            ItemStack stack = item.getItemStack();

            int maxDrainAmount = getMaxStackDrain(stack);
            if (maxDrainAmount == 0) {
                continue;
            }

            int drainAmount = ChanceUtil.getRandom(maxDrainAmount);
            int newAmt = stack.getAmount() - drainAmount;
            if (newAmt < 1) {
                item.remove();
            } else {
                item.getItemStack().setAmount(newAmt);
            }
        }
    }

    public void drain() {
        drainPlayers();
        drainFloor();
    }

    private boolean isInEmeraldMine(Entity entity) {
        return entity.getLocation().getY() < 30;
    }

    private boolean shouldSpawnBlazes() {
        long diff = System.currentTimeMillis() - lastActivation;
        return lastActivation == 0 || diff <= lastActivationTime * .35 || diff >= lastActivationTime * 5;
    }

    private void trySpawnBlazes() {
        if (!shouldSpawnBlazes()) {
            return;
        }

        for (Player player : getContainedParticipants()) {
            if (!isInEmeraldMine(player)) {
                continue;
            }

            Location playerLoc = player.getLocation();
            for (int i = 0; i < ChanceUtil.getRangedRandom(2, 5); i++) {
                Location spawnLoc = LocationUtil.findRandomLoc(playerLoc, 5, true, false);

                if (spawnLoc.getBlock().getType() != Material.AIR) {
                    spawnLoc = playerLoc;
                }

                getWorld().spawn(spawnLoc, Blaze.class);
            }
        }
    }

    public void restoreReadyBlocks() {
        blockState.popBlocksOlderThan(
                BlockStateKind.CURSED_MINE,
                TimeUnit.SECONDS.toMillis(30)
        );
    }

    public void revertPlayerBlocks(Player player) {
        blockState.popBlocksCreatedBy(BlockStateKind.CURSED_MINE, player);
    }

    private static final Set<Material> REPLACEABLE_TYPES = Set.of(
            Material.WATER, Material.OAK_PLANKS, Material.AIR
    );

    private Material getNewFloodGateType() {
        if (lastActivation == 0 || System.currentTimeMillis() - lastActivation >= lastActivationTime) {
            return Material.OAK_PLANKS;
        } else {
            return Material.AIR;
        }
    }

    private void changeWater() {
        Material newType = getNewFloodGateType();

        RegionWalker.walk(floodGate_RG, (x, y, z) -> {
            Block block = getWorld().getBlockAt(x, y, z);
            if (REPLACEABLE_TYPES.contains(block.getType())) {
                block.setType(newType);
            }
        });
    }

    protected void eatFood(Player player) {
        if (!ChanceUtil.getChance(10)) {
            return;
        }

        if (player.getSaturation() - 1 >= 0) {
            player.setSaturation(player.getSaturation() - 1);
        } else if (player.getFoodLevel() - 1 >= 0) {
            player.setFoodLevel(player.getFoodLevel() - 1);
        } else if (player.getHealth() - 1 >= 0) {
            player.setHealth(player.getHealth() - 1);
        }
    }

    protected void poison(Player player, int duration) {
        if (ChanceUtil.getChance(player.getLocation().getBlockY() / 2)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 20 * duration, 2));
            ChatUtil.sendWarning(player, "The ore releases a toxic gas poisoning you!");
        }
    }

    protected void ghost(final Player player, Material blockType) {

        try {
            if (ChanceUtil.getChance(player.getLocation().getBlockY())) {
                if (ChanceUtil.getChance(2)) {
                    switch (ChanceUtil.getRandom(6)) {
                        case 1:
                            ChatUtil.sendNotice(player, "Caspher the friendly ghost drops some bread.");
                            player.getWorld().dropItemNaturally(player.getLocation(),
                                    new ItemStack(Material.BREAD, ChanceUtil.getRandom(16)));
                            break;
                        case 2:
                            ChatUtil.sendNotice(player, "COOKIE gives you a cookie.");
                            player.getWorld().dropItemNaturally(player.getLocation(), new ItemStack(Material.COOKIE));
                            break;
                        case 3:
                            ChatUtil.sendNotice(player, "Caspher the friendly ghost appears.");
                            for (int i = 0; i < 8; i++) {
                                player.getWorld().dropItemNaturally(player.getLocation(),
                                        new ItemStack(Material.IRON_INGOT, ChanceUtil.getRandom(64)));
                                player.getWorld().dropItemNaturally(player.getLocation(),
                                        new ItemStack(Material.GOLD_INGOT, ChanceUtil.getRandom(64)));
                                player.getWorld().dropItemNaturally(player.getLocation(),
                                        new ItemStack(Material.DIAMOND, ChanceUtil.getRandom(64)));
                            }
                            break;
                        case 4:
                            ChatUtil.sendNotice(player, "John gives you a new jacket.");
                            player.getWorld().dropItemNaturally(player.getLocation(), new ItemStack(Material.LEATHER_CHESTPLATE));
                            break;
                        case 5:
                            ChatUtil.sendNotice(player, "Tim teleports items to you.");
                            getContained(Item.class).forEach(i -> i.teleport(player));

                            // Add in some extra drops just in case the loot wasn't very juicy
                            player.getWorld().dropItem(player.getLocation(), new ItemStack(Material.IRON_INGOT, ChanceUtil.getRandom(64)));
                            player.getWorld().dropItem(player.getLocation(), new ItemStack(Material.GOLD_INGOT, ChanceUtil.getRandom(64)));
                            player.getWorld().dropItem(player.getLocation(), new ItemStack(Material.DIAMOND, ChanceUtil.getRandom(64)));
                            break;
                        case 6:
                            ChatUtil.sendNotice(player, "Dan gives you a sparkling touch.");

                            Material type;
                            switch (ChanceUtil.getRandom(3)) {
                                case 1:
                                    type = Material.IRON_INGOT;
                                    break;
                                case 2:
                                    type = Material.GOLD_INGOT;
                                    break;
                                case 3:
                                    type = Material.DIAMOND;
                                    break;
                                default:
                                    type = Material.REDSTONE;
                                    break;
                            }

                            prayer.influencePlayer(player,
                                    PrayerComponent.constructPrayer(player, new InventoryFX(type, 64), 5000));
                            break;
                        default:
                            break;
                    }
                } else {
                    if (ItemUtil.hasAncientArmour(player) && ChanceUtil.getChance(2)) {
                        ChatUtil.sendNotice(player, ChatColor.AQUA, "Your armour blocks an incoming ghost attack.");
                        return;
                    }

                    Location modifiedLoc = null;

                    switch (ChanceUtil.getRandom(11)) {
                        case 1:
                            if (ChanceUtil.getChance(4)) {
                                if (blockType == Material.DIAMOND_ORE) {
                                    addToHitList(player);
                                    ChatUtil.sendWarning(player, "You ignite fumes in the air!");
                                    EditSession ess = WorldEditBridge.getSystemEditSessionFor(getWorld());
                                    try {
                                        ess.fillXZ(toBlockVec3(player.getLocation()), BlockTypes.FIRE.getDefaultState(), 20, 20, true);
                                        ess.flushSession();
                                    } catch (MaxChangedBlocksException ignored) {

                                    }
                                    for (int i = ChanceUtil.getRandom(24) + 20; i > 0; --i) {
                                        final boolean untele = i == 11;
                                        server.getScheduler().runTaskLater(inst, () -> {
                                            if (untele) {
                                                revertPlayerBlocks(player);
                                                removeFromHitList(player);
                                            }

                                            if (!contains(player)) return;

                                            Location l = LocationUtil.findRandomLoc(player.getLocation().getBlock(), 3, true, false);
                                            ExplosionStateFactory.createExplosion(l, 3, true, false);
                                        }, 12 * i);
                                    }
                                } else {
                                    addToHitList(player);
                                    player.chat("Who's a good ghost?!?!");
                                    server.getScheduler().runTaskLater(inst, () -> {
                                        player.chat("Don't hurt me!!!");
                                        server.getScheduler().runTaskLater(inst, () -> {
                                            player.chat("Nooooooooooo!!!");

                                            try {
                                                prayer.influencePlayer(player, PrayerComponent.constructPrayer(player,
                                                        PrayerType.CANNON, TimeUnit.MINUTES.toMillis(2)));
                                            } catch (UnsupportedPrayerException ex) {
                                                ex.printStackTrace();
                                            }
                                        }, 20);
                                    }, 20);
                                }
                                break;
                            }
                        case 2:
                            ChatUtil.sendWarning(player, "You find yourself falling from the sky...");
                            addToHitList(player);
                            modifiedLoc = new Location(player.getWorld(), player.getLocation().getX(), 350, player.getLocation().getZ());
                            break;
                        case 3:
                            ChatUtil.sendWarning(player, "George plays with fire, sadly too close to you.");
                            prayer.influencePlayer(player, PrayerComponent.constructPrayer(player,
                                    PrayerType.FIRE, TimeUnit.SECONDS.toMillis(45)));
                            break;
                        case 4:
                            ChatUtil.sendWarning(player, "Simon says pick up sticks.");
                            for (int i = 0; i < player.getInventory().getContents().length * 1.5; i++) {
                                player.getWorld().dropItem(player.getLocation(), new ItemStack(Material.STICK, 64));
                            }
                            break;
                        case 5:
                            ChatUtil.sendWarning(player, "Ben dumps out your backpack.");
                            prayer.influencePlayer(player, PrayerComponent.constructPrayer(player,
                                    PrayerType.BUTTERFINGERS, TimeUnit.SECONDS.toMillis(10)));
                            break;
                        case 6:
                            ChatUtil.sendWarning(player, "Merlin attacks with a mighty rage!");
                            prayer.influencePlayer(player, PrayerComponent.constructPrayer(player,
                                    PrayerType.MERLIN, TimeUnit.SECONDS.toMillis(20)));
                            break;
                        case 7:
                            ChatUtil.sendWarning(player, "Dave tells everyone that your mining!");
                            Bukkit.broadcastMessage(ChatColor.GOLD + "The player: "
                                    + player.getDisplayName() + " is mining in the cursed mine!!!");
                            break;
                        case 8:
                            ChatUtil.sendWarning(player, "Dave likes your food.");
                            addToHitList(player);
                            prayer.influencePlayer(player, PrayerComponent.constructPrayer(player,
                                    PrayerType.STARVATION, TimeUnit.MINUTES.toMillis(15)));
                            break;
                        case 9:
                            ChatUtil.sendWarning(player, "Hallow declares war on YOU!");
                            for (int i = 0; i < ChanceUtil.getRangedRandom(10, 30); i++) {
                                Blaze blaze = getWorld().spawn(player.getLocation(), Blaze.class);
                                blaze.setTarget(player);
                                blaze.setRemoveWhenFarAway(true);
                            }
                            break;
                        case 10:
                            ChatUtil.sendWarning(player, "A legion of hell hounds appears!");
                            for (int i = 0; i < ChanceUtil.getRangedRandom(10, 30); i++) {
                                Wolf wolf = getWorld().spawn(player.getLocation(), Wolf.class);
                                wolf.setTarget(player);
                                wolf.setRemoveWhenFarAway(true);
                            }
                            break;
                        case 11:
                            if (blockType == Material.EMERALD_ORE) {
                                ChatUtil.sendNotice(player, "Dave got a chemistry set!");
                                addToHitList(player);
                                prayer.influencePlayer(player, PrayerComponent.constructPrayer(player,
                                        PrayerType.DEADLYPOTION, TimeUnit.MINUTES.toMillis(30)));
                            } else {
                                ChatUtil.sendWarning(player, "Dave says hi, that's not good.");
                                addToHitList(player);
                                prayer.influencePlayer(player, PrayerComponent.constructPrayer(player,
                                        PrayerType.SLAP, TimeUnit.MINUTES.toMillis(30)));
                                prayer.influencePlayer(player, PrayerComponent.constructPrayer(player,
                                        PrayerType.BUTTERFINGERS, TimeUnit.MINUTES.toMillis(30)));
                                prayer.influencePlayer(player, PrayerComponent.constructPrayer(player,
                                        PrayerType.FIRE, TimeUnit.MINUTES.toMillis(30)));
                            }
                            break;
                        default:
                            break;
                    }

                    if (modifiedLoc != null) player.teleport(modifiedLoc, PlayerTeleportEvent.TeleportCause.UNKNOWN);
                }
            }
        } catch (UnsupportedPrayerException ex) {
            ex.printStackTrace();
        }
    }
}
