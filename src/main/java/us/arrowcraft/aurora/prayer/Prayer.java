package us.arrowcraft.aurora.prayer;
import org.bukkit.entity.Player;
import us.arrowcraft.aurora.prayer.PrayerFX.AbstractPrayer;

/**
 * Author: Turtle9598
 */
public class Prayer {

    private final Player player;
    private final PrayerType prayerType;
    private final AbstractPrayer abstractPrayer;
    private final long startTime;
    private long maxDuration;
    private Class triggerClass;


    public Prayer(Player player, PrayerType prayerType, AbstractPrayer abstractPrayer, long maxDuration,
                  Class triggerClass) {

        this.player = player;
        this.prayerType = prayerType;
        this.abstractPrayer = abstractPrayer;
        this.startTime = System.currentTimeMillis();
        this.maxDuration = maxDuration;
        this.triggerClass = triggerClass;
    }

    public Player getPlayer() {

        return player;
    }

    public PrayerType getPrayerType() {

        return prayerType;
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

        return triggerClass != null;
    }

    public Class getTriggerClass() {

        return triggerClass;
    }

    public void setTriggerClass(Class triggerClass) {

        this.triggerClass = triggerClass;
    }
}
