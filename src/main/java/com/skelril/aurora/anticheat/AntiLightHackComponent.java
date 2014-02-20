/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.anticheat;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldedit.blocks.BlockID;
import com.skelril.aurora.admin.AdminComponent;
import com.skelril.aurora.events.environment.DarkAreaInjuryEvent;
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
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Author: Turtle9598
 */
@ComponentInformation(friendlyName = "Light Hack", desc = "Stop the light hackers")
@Depend(components = AdminComponent.class)
public class AntiLightHackComponent extends BukkitComponent implements Listener {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    @InjectComponent
    AdminComponent adminComponent;

    LocalConfiguration config;

    @Override
    public void enable() {

        config = configure(new LocalConfiguration());
        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);
    }

    @Override
    public void reload() {

        super.reload();
        configure(config);
    }

    private static class LocalConfiguration extends ConfigurationBase {

        @Setting("chance-of-failure")
        public int failRate = 3;
        @Setting("max-damage")
        public int maxDamage = 5;
        @Setting("max-light")
        public double maxLight = 0;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {

        Player player = event.getPlayer();
        Block block = event.getBlock();
        Block pBlock = player.getLocation().getBlock();
        Block pEyeBlock = pBlock.getRelative(BlockFace.UP);

        if (!adminComponent.isAdmin(player) && isProtectedBlock(block.getTypeId())) {

            // Light
            int light = pBlock.getLightLevel();
            light += pEyeBlock.getLightLevel();
            if (light > config.maxLight) return;

            // Environment
            if (!isValidEnvironment(pBlock)) return;

            // Chanced
            if (!ChanceUtil.getChance(config.failRate)) return;

            // Potions
            if (player.hasPotionEffect(PotionEffectType.NIGHT_VISION)) return;

            // Damage
            DarkAreaInjuryEvent aEvent = new DarkAreaInjuryEvent(player,
                    ChanceUtil.getRandom(config.maxDamage), textOps[ChanceUtil.getRandom(textOps.length) - 1]);
            server.getPluginManager().callEvent(aEvent);

            if (aEvent.isCancelled()) return;

            ChatUtil.sendWarning(player, aEvent.getMessage());
            player.damage(aEvent.getDamage());
        }
    }

    private static String[] textOps = new String[4];

    static {
        textOps[0] = "You swing your pickaxe at your foot sometimes yelling... ouch!";
        textOps[1] = "Your pickaxe suddenly came in contact with your eye.";
        textOps[2] = "You stumble and trip over a rock.";
        textOps[3] = "You suddenly find yourself falling from the... Oh wait, you just bashed your head off a rock.";
    }

    private static List<Biome> biomeBlackList = new ArrayList<>();

    static {
        biomeBlackList.add(Biome.HELL);
    }

    private boolean isValidEnvironment(Block block) {

        return !biomeBlackList.contains(block.getBiome()) && !EnvironmentUtil.isWater(block.getTypeId());
    }

    private boolean isProtectedBlock(int id) {

        return EnvironmentUtil.isValuableOre(id) || id == BlockID.STONE;
    }
}
