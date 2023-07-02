/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.type.city.area.areas.Factory;

import com.destroystokyo.paper.entity.ai.VanillaGoal;
import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import gg.packetloss.bukkittext.Text;
import gg.packetloss.grindstone.data.DatabaseComponent;
import gg.packetloss.grindstone.modifiers.ModifierComponent;
import gg.packetloss.grindstone.modifiers.ModifierType;
import gg.packetloss.grindstone.util.*;
import gg.packetloss.grindstone.util.bridge.WorldGuardBridge;
import gg.packetloss.grindstone.util.item.ItemNameCalculator;
import gg.packetloss.grindstone.util.task.promise.TaskFuture;
import gg.packetloss.grindstone.world.type.city.area.AreaComponent;
import gg.packetloss.grindstone.world.type.city.area.areas.Factory.component.LavaSupply;
import gg.packetloss.grindstone.world.type.city.area.areas.Factory.db.FactoryJobDatabase;
import gg.packetloss.grindstone.world.type.city.area.areas.Factory.db.MySQLFactoryJobDatabase;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Drowned;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

import static gg.packetloss.grindstone.util.ChatUtil.WHOLE_NUMBER_FORMATTER;

@ComponentInformation(friendlyName = "Factory", desc = "Mass production, now for the masses")
@Depend(plugins = {"WorldGuard"}, components = {DatabaseComponent.class})
public class FactoryArea extends AreaComponent<FactoryConfig> {

    private FactoryJobDatabase jobDatabase = new MySQLFactoryJobDatabase();

    // Smelter
    protected LavaSupply lavaSupply;
    protected ProtectedRegion[] smeltersOutputChannels;

    private List<FactoryJob> activeJobs = new ArrayList<>();
    private List<FactoryMachine> factoryMachines = new ArrayList<>();
    private FactoryProducer producer;

    @Override
    public void setUp() {
        world = server.getWorlds().get(0);

        RegionManager manager = WorldGuardBridge.getManagerFor(world);
        String base = "oblitus-district-old-factory";
        region = manager.getRegion(base);

        {
            // Setup smelting
            lavaSupply = new LavaSupply(
                world,
                new ProtectedRegion[]{
                    Objects.requireNonNull(manager.getRegion(base + "-lava-channel-1")),
                    Objects.requireNonNull(manager.getRegion(base + "-lava-channel-2"))
                },
                Objects.requireNonNull(manager.getRegion(base + "-lava"))
            );
            factoryMachines.add(new FactorySmelter(
                this, Objects.requireNonNull(manager.getRegion(base + "-hopper-1"))
            ));
            smeltersOutputChannels = new ProtectedRegion[] {
                Objects.requireNonNull(manager.getRegion(base + "-smelter-1-track")),
                Objects.requireNonNull(manager.getRegion(base + "-smelter-2-track"))
            };
        }

        {
            // Setup brewing
            factoryMachines.add(new FactoryBrewer(
                this, Objects.requireNonNull(manager.getRegion(base + "-vat-1"))
            ));
            factoryMachines.add(new FactoryBrewer(
                this, Objects.requireNonNull(manager.getRegion(base + "-vat-2"))
            ));
            factoryMachines.add(new FactoryBrewer(
                this, Objects.requireNonNull(manager.getRegion(base + "-vat-3"))
            ));
        }

        {
            // Setup output production
            Map<FactoryOutputKind, ProtectedRegion[]> outputMap = new EnumMap<>(FactoryOutputKind.class);
            outputMap.put(
                FactoryOutputKind.FOUNTAIN,
                new ProtectedRegion[]{Objects.requireNonNull(manager.getRegion(base + "-producer"))}
            );
            outputMap.put(
                FactoryOutputKind.SMELTING_CHAMBER,
                new ProtectedRegion[]{
                    Objects.requireNonNull(manager.getRegion(base + "-smelter-1")),
                    Objects.requireNonNull(manager.getRegion(base + "-smelter-2"))
                }
            );
            producer = new FactoryProducer(this, outputMap);
        }

        tick = 4 * 20;
        listener = new FactoryListener(this);
        config = new FactoryConfig();
    }

    @Override
    public void disable() {
        persistJobs(activeJobs);
    }

    private Optional<FactoryJob> getRandomActiveJob() {
        List<FactoryJob> incompleteJobs = activeJobs.stream().filter(
            FactoryJob::isIncomplete
        ).collect(Collectors.toList());
        if (incompleteJobs.isEmpty()) {
            return Optional.empty();
        }
        
        return Optional.of(CollectionUtil.getElement(incompleteJobs));
    }

    private void updateLavaSupply() {
        if (ChanceUtil.getChance(5)) {
            lavaSupply.checkDamage();
        }

        if (lavaSupply.tryAddLava()) {
            ChatUtil.sendNotice(getAudiblePlayers(), "Adding lava...");
        }
    }

    private ItemStack getDrownedWeaponIfAny() {
        return ChanceUtil.getChance(config.productionDrownedTridentChance) ? new ItemStack(Material.TRIDENT) : null;
    }

    private void spawnDrowned() {
        Drowned drowned = world.spawn(producer.getSpawnPoint(FactoryOutputKind.FOUNTAIN), Drowned.class);

        // Setup Equipment
        Objects.requireNonNull(drowned.getEquipment()).setItemInMainHand(getDrownedWeaponIfAny());
        drowned.setCanPickupItems(false);

        // Setup AI
        EntityUtil.setFollowRange(drowned, 75);
        Bukkit.getMobGoals().removeGoal(drowned, VanillaGoal.DROWNED_GO_TO_BEACH);
        Bukkit.getMobGoals().removeGoal(drowned, VanillaGoal.DROWNED_GO_TO_WATER);
    }

    protected void sendProductionMessage(Collection<Player> audiblePlayers, String step,
                                         int quantity, String itemName, int modifier) {
        List<Object> arguments = new ArrayList<>();
        arguments.add(step);
        arguments.add(": ");
        arguments.add(Text.of(ChatColor.WHITE, WHOLE_NUMBER_FORMATTER.format(quantity)));
        if (modifier != 1) {
            arguments.add(" -> ");
            arguments.add(Text.of(ChatColor.WHITE, WHOLE_NUMBER_FORMATTER.format(quantity * modifier)));
        }
        arguments.add(" ");
        arguments.add(ItemNameCalculator.getSystemDisplayName(itemName));
        if (modifier != 1) {
            arguments.add(" (x");
            arguments.add(Text.of(ChatColor.WHITE, WHOLE_NUMBER_FORMATTER.format(modifier)));
            arguments.add(")");
        }
        arguments.add(".");
        ChatUtil.sendNotice(audiblePlayers, arguments.toArray());
    }

    private void createJob(UUID playerID, String itemName, int amount) {
        TaskFuture.asyncTask(() -> jobDatabase.getJob(playerID, itemName)).thenAccept((optJob) -> {
            FactoryJob job = optJob.orElse(new FactoryJob(playerID, itemName, 0));
            int jobIndex = activeJobs.indexOf(job);
            // Add the job if it's not already present
            if (jobIndex == -1) {
                activeJobs.add(job);
                jobIndex = activeJobs.size() - 1;
            }
            // Retrieve the final job target
            FactoryJob targetJob = activeJobs.get(jobIndex);
            targetJob.increaseProduction(amount);
        });
    }

    private void detectNewJobs() {
        for (FactoryMachine machine : factoryMachines) {
            machine.detectNewJobs(this::createJob);
        }
    }

    private void persistJobs(List<FactoryJob> jobs) {
        if (jobs.isEmpty()) {
            return;
        }

        jobDatabase.updateJobs(jobs);
    }

    private void syncJobsWithPlayers(List<Player> players) {
        List<UUID> currentPlayers = players.stream().map(
            Entity::getUniqueId
        ).collect(Collectors.toList());

        TaskFuture.asyncTask(() -> jobDatabase.getJobs(currentPlayers)).thenAccept((newJobs) -> {
            List<FactoryJob> newActiveJobs = new ArrayList<>();
            List<FactoryJob> jobsGoingIdle = new ArrayList<>();

            Iterator<FactoryJob> activeIt = activeJobs.iterator();
            while (activeIt.hasNext()) {
                FactoryJob activeJob = activeIt.next();
                // Keep the job if it isn't completed, and it doesn't remain active
                if (!activeJob.isComplete() && newJobs.contains(activeJob)) {
                    newActiveJobs.add(activeJob);
                    continue;
                }

                // If the job was completed, remove it from the newJobs so it doesn't get re-added below
                if (activeJob.isComplete()) {
                    newJobs.remove(activeJob);
                }

                jobsGoingIdle.add(activeJob);
                activeIt.remove();
            }
            for (FactoryJob newJob : newJobs) {
                // If the job is already represented skip it.
                if (newActiveJobs.contains(newJob)) {
                    continue;
                }

                newActiveJobs.add(newJob);
            }

            activeJobs = newActiveJobs;
            PluginTaskExecutor.submitAsync(() -> persistJobs(jobsGoingIdle));
        });
    }

    @Override
    public void run() {
        List<Player> participants = getContainedParticipants();

        updateLavaSupply();

        boolean hexa = ModifierComponent.getModifierCenter().isActive(ModifierType.HEXA_FACTORY_SPEED);
        int max = participants.size() * (hexa ? 6 : 1) * config.productionBaseSpeed;

        for (int i = ChanceUtil.getRangedRandom(max / 3, max); i > 0; --i) {
            CommandBook.server().getScheduler().runTaskLater(CommandBook.inst(), () -> {
                getRandomActiveJob().ifPresent((job) -> {
                    producer.produce(job);

                    if (ChanceUtil.getChance(config.productionDrownedSpawnChance)) {
                        spawnDrowned();
                    }
                });
            }, i * 10L + ChanceUtil.getRandom(10));
        }

        detectNewJobs();
        syncJobsWithPlayers(participants);
    }
}
