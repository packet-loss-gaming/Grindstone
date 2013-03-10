package com.skelril.aurora.prayer.PrayerFX;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;

/**
 * Author: Turtle9598
 */
public abstract class AbstractTriggeredPrayer extends AbstractPrayer {

    private final Class triggerClass;

    public AbstractTriggeredPrayer(Class triggerClass) {

        this.triggerClass = triggerClass;
    }

    public AbstractTriggeredPrayer(Class triggerClass, AbstractPrayer[] subFX) {

        super(subFX);
        this.triggerClass = triggerClass;
    }

    public AbstractTriggeredPrayer(Class triggerClass, AbstractPrayer[] subFX, PotionEffect... effects) {

        super(subFX, effects);
        this.triggerClass = triggerClass;
    }

    public Class getTriggerClass() {

        return triggerClass;
    }

    public abstract void trigger(Player player);

}
