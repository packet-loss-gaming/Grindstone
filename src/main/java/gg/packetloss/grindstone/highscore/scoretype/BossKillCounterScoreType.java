/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.highscore.scoretype;

import gg.packetloss.grindstone.util.BossKind;

import java.util.Optional;

public class BossKillCounterScoreType extends KillCounterScoreType {
    private final BossKind bossKind;

    protected BossKillCounterScoreType(int id, boolean gobletEnabled, BossKind bossKind) {
        super(id, gobletEnabled);
        this.bossKind = bossKind;
    }

    public BossKind getBossKind() {
        return bossKind;
    }

    @Override
    public Optional<String> getGobletName() {
        return Optional.of(bossKind.name().toLowerCase() + "_kills");
    }

    @Override
    public boolean isGobletEquivalentImpl(ScoreType scoreType) {
        if (!(scoreType instanceof BossKillCounterScoreType)) {
            return false;
        }

        return getBossKind() == ((BossKillCounterScoreType) scoreType).getBossKind();
    }
}
