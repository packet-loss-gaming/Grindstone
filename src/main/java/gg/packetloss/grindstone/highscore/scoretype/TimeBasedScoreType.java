/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.highscore.scoretype;

import com.google.common.base.Joiner;

import java.text.DecimalFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

class TimeBasedScoreType extends BasicScoreType {
    private static final DecimalFormat FINE_TIME_FORMATTER = new DecimalFormat("0.000");

    protected TimeBasedScoreType(int id, boolean gobletEnabled, UpdateMethod updateMethod, Order order) {
        super(id, gobletEnabled, updateMethod, order);
    }

    @Override
    public String format(long score) {
        Duration duration = Duration.ofMillis(score);

        List<String> components = new ArrayList<>();

        if (duration.toHours() > 0) {
            components.add(duration.toHours() + " hours");
            duration = duration.minusHours(duration.toHours());
        }
        if (duration.toMinutes() > 0) {
            components.add(duration.toMinutes() + " minutes");
            duration = duration.minusMinutes(duration.toMinutes());
        }
        if (duration.getSeconds() > 0 || duration.getNano() > 0) {
            double fractionalSeconds = duration.getSeconds() + ((double) duration.getNano() / TimeUnit.SECONDS.toNanos(1));
            String formattedSeconds = FINE_TIME_FORMATTER.format(fractionalSeconds);
            components.add(formattedSeconds + " seconds");
        }

        return Joiner.on(' ').join(components);
    }
}
