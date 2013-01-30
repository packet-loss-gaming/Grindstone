package us.arrowcraft.aurora.prayer.PrayerFX;
import us.arrowcraft.aurora.prayer.PrayerType;

/**
 * Author: Turtle9598
 */
public class AlonzoFX extends AbstractPrayer {

    private static final AbstractPrayer[] subFX = new AbstractPrayer[] {
            new FireFX(), new BlindnessFX(), new SmokeFX(), new MushroomFX(), new ButterFingersFX()
    };

    public AlonzoFX() {

        super(subFX);
    }

    @Override
    public PrayerType getType() {

        return PrayerType.ALONZO;
    }
}
