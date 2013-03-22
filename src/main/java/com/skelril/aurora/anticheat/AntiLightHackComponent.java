package com.skelril.aurora.anticheat;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldedit.blocks.BlockID;
import com.skelril.aurora.admin.AdminComponent;
import com.skelril.aurora.util.ChanceUtil;
import com.skelril.aurora.util.ChatUtil;
import com.skelril.aurora.util.EnvironmentUtil;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
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

    @Override
    public void enable() {

        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);
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
            if (light > 0) return;

            // Environment
            if (!isValidEnvironment(pBlock)) return;

            // Chanced
            if (!ChanceUtil.getChance(3)) return;

            // Potions
            if (player.hasPotionEffect(PotionEffectType.NIGHT_VISION)) return;

            // Damage
            ChatUtil.sendWarning(player, textOps[ChanceUtil.getRandom(textOps.length) - 1]);
            player.damage(ChanceUtil.getRandom(5));
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
