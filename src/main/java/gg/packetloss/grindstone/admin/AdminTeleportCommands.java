package gg.packetloss.grindstone.admin;

import com.sk89q.commandbook.command.argument.SinglePlayerTarget;
import com.sk89q.worldedit.command.util.CommandPermissions;
import com.sk89q.worldedit.command.util.CommandPermissionsConditionGenerator;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.particle.SingleBlockParticleEffect;
import gg.packetloss.grindstone.util.task.TaskBuilder;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.enginehub.piston.annotation.Command;
import org.enginehub.piston.annotation.CommandContainer;
import org.enginehub.piston.annotation.param.Arg;
import org.enginehub.piston.annotation.param.ArgFlag;

@CommandContainer(superTypes = CommandPermissionsConditionGenerator.Registration.class)
public class AdminTeleportCommands {
    private void sendNotice(Player from, Player to, int timesRemaining) {
        String time = "" + ChatColor.BLUE + timesRemaining;
        ChatUtil.sendNotice(to, from.getDisplayName() + " joining you in... " + time);
        ChatUtil.sendNotice(from, "Joining in... " + time);

    }

    @Command(name = "atp", desc = "Announced teleport command")
    @CommandPermissions({"aurora.admin.teleport"})
    public void announcedTeleportCmd(Player sendingPlayer,
                                     @ArgFlag(name = 'd', desc = "delay", def = "3") int delay,
                                     @Arg(desc = "target") SinglePlayerTarget player) {
        TaskBuilder.Countdown taskBuilder = TaskBuilder.countdown();

        // 0 actually means 1 because of the smoke effect forcing an additional delay.
        taskBuilder.setNumberOfRuns(Math.max(0, delay - 1));
        taskBuilder.setInterval(20);

        Player targetPlayer = player.get();

        taskBuilder.setAction((times) -> {
            sendNotice(sendingPlayer, targetPlayer, times + 1);
            return true;
        });

        taskBuilder.setFinishAction(() -> {
            sendNotice(sendingPlayer, targetPlayer, 1);

            Location targetLoc = targetPlayer.getLocation();

            TaskBuilder.Countdown innerTask = TaskBuilder.countdown();

            innerTask.setNumberOfRuns(20);

            innerTask.setAction((times) -> {
                SingleBlockParticleEffect.puffOfSmoke(targetLoc);
                return true;
            });

            innerTask.setFinishAction(() -> {
                sendingPlayer.teleport(targetLoc);
            });

            innerTask.build();
        });

        taskBuilder.build();
    }
}
