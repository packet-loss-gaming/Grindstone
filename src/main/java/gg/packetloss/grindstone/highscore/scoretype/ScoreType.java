/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.highscore.scoretype;

import java.util.Optional;

public interface ScoreType {
    public int getId();

    public boolean isEnabledForGoblet();

    public Optional<String> getGobletName();

    public boolean isGobletEquivalent(ScoreType scoreType);

    public boolean isIncremental();

    public Order getOrder();

    public String format(long score);

    public enum Order {
        ASC, DESC
    }
}
