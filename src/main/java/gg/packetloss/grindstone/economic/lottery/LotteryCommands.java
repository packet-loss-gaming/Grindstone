package gg.packetloss.grindstone.economic.lottery;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.worldedit.command.util.CommandPermissions;
import com.sk89q.worldedit.command.util.CommandPermissionsConditionGenerator;
import gg.packetloss.grindstone.util.ChatUtil;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.enginehub.piston.annotation.Command;
import org.enginehub.piston.annotation.CommandContainer;
import org.enginehub.piston.annotation.param.Arg;
import org.enginehub.piston.annotation.param.Switch;

import java.util.ArrayList;
import java.util.List;

@CommandContainer(superTypes = CommandPermissionsConditionGenerator.Registration.class)
public class LotteryCommands {
    private LotteryComponent component;
    private Economy economy;

    public LotteryCommands(LotteryComponent component, Economy economy) {
        this.component = component;
        this.economy = economy;
    }

    @Command(name = "buy", desc = "Buy Lottery Tickets.")
    @CommandPermissions({"aurora.lottery.ticket.buy.command"})
    public void lotteryBuyCmd(Player player, @Arg(desc = "amount to buy", def = "1") int amount) throws CommandException {
        component.buyTickets(player, amount);
    }

    @Command(name = "draw", desc = "Trigger the lottery draw.")
    @CommandPermissions({"aurora.lottery.draw"})
    public void lotteryDrawCmd(CommandSender sender) {
        ChatUtil.sendNotice(sender, "Lottery draw in progress.");
        component.completeLottery();
    }

    @Command(name = "pot", desc = "View the lottery pot size.")
    @CommandPermissions({"aurora.lottery.pot"})
    public void lotteryPotCmd(CommandSender sender, @Switch(name = 'b', desc = "broadcast to all players") boolean broadcast) {
        List<CommandSender> que = new ArrayList<>();

        que.add(sender);
        if (broadcast && sender.hasPermission("aurora.lottery.pot.broadcast")) {
            que.addAll(CommandBook.server().getOnlinePlayers());
        }

        component.broadcastLottery(que);
    }

    @Command(name = "recent", desc = "View the last lottery winner.")
    @CommandPermissions({"aurora.lottery.last"})
    public void lotteryLastCmd(CommandSender sender) {
        ChatUtil.sendNotice(sender, ChatColor.GRAY, "Lottery - Recent winners:");

        short number = 0;
        for (LotteryWinner winner : component.getRecentWinner()) {
            number++;
            ChatUtil.sendNotice(sender, "  " + ChatColor.GOLD + number + ". " + ChatColor.YELLOW
                    + winner.getName() + ChatColor.GOLD + " - " + ChatColor.WHITE + economy.format(winner.getAmt()));
        }
    }
}
