package com.skelril.aurora.economic.lottery;

import com.skelril.aurora.util.player.GenericWealthStore;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Author: Turtle9598
 */
public interface LotteryTicketDatabase extends Iterable<GenericWealthStore> {

    public boolean load();

    public boolean save();

    public boolean ticketExist(Player player);

    public boolean ticketExist(String playerName);

    public void addTicket(Player player, int count);

    public void addTicket(String playerName, int count);

    public void removeTicket(String playerName);

    public void clearTickets();

    public GenericWealthStore getLotteryTicket(Player player);

    public GenericWealthStore getLotteryTicket(String playerName);

    public List<GenericWealthStore> getTickets();
}
