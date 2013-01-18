package us.arrowcraft.aurora.economic.lottery;
import java.util.List;

/**
 * Author: Turtle9598
 */
public interface LotteryWinnerDatabase extends Iterable<String> {

    public boolean load();

    public boolean save();

    public void addWinner(String name);

    public void addWinner(String name, long time);

    public List<String> getRecentWinner();

    public List<String> getRecentWinner(int limit);
}
