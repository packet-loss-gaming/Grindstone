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
import org.bukkit.EntityEffect;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.potion.PotionEffectType;

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

        if (!adminComponent.isAdmin(player)
                && !EnvironmentUtil.isWater(block)
                && player.getLocation().getBlock().getLightLevel() == 0
                && player.getEyeLocation().getBlock().getLightLevel() == 0
                && !player.hasPotionEffect(PotionEffectType.NIGHT_VISION)
                && ChanceUtil.getChance(3)
                && (EnvironmentUtil.isValuableOre(block) || block.getTypeId() == BlockID.STONE)) {
            int playerHealth = player.getHealth();
            if (playerHealth - 1 >= 0) {
                player.setHealth(playerHealth - 1);
                player.playEffect(EntityEffect.HURT);
                ChatUtil.sendWarning(player, "You hurt yourself attempting to mine in the dark.");
            } else {
                player.setHealth(0);
                player.playEffect(EntityEffect.DEATH);
                ChatUtil.sendWarning(player, "You kill yourself attempting to mine in the dark.");
            }
        }
    }
}
