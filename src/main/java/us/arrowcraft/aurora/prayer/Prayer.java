package us.arrowcraft.aurora.prayer;
import org.bukkit.entity.Player;
import us.arrowcraft.aurora.prayer.PrayerFX.AbstractPrayer;
import us.arrowcraft.aurora.prayer.PrayerFX.AbstractTriggeredPrayer;

/**
 * Author: Turtle9598
 */
public class Prayer implements Comparable {

    private final Player player;
    private final AbstractPrayer abstractPrayer;
    private final long startTime;
    private long maxDuration;


    public Prayer(Player player, AbstractPrayer abstractPrayer, long maxDuration) {

        this.player = player;
        this.abstractPrayer = abstractPrayer;
        this.startTime = System.currentTimeMillis();
        this.maxDuration = maxDuration;
    }

    public Player getPlayer() {

        return player;
    }

    public AbstractPrayer getEffect() {

        return abstractPrayer;
    }

    public long getStartTime() {

        return startTime;
    }

    public long getMaxDuration() {

        return maxDuration;
    }

    public void setMaxDuration(long maxDuration) {

        this.maxDuration = maxDuration;
    }

    public boolean hasTrigger() {

        return abstractPrayer instanceof AbstractTriggeredPrayer;
    }

    public Class getTriggerClass() {

        return hasTrigger() ? ((AbstractTriggeredPrayer) abstractPrayer).getTriggerClass() : null;
    }

    @Override
    public int compareTo(Object o) {

        if (o == null || !(o instanceof Prayer)) return 0;

        Prayer prayer = (Prayer) o;
        if (this.getEffect().getType().getValue() == prayer.getEffect().getType().getValue()) return 0;
        if (this.getEffect().getType().getValue() > prayer.getEffect().getType().getValue()) return 1;
        return -1;
    }
}
