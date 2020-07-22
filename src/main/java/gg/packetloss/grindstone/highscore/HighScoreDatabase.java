/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.highscore;

import java.util.List;
import java.util.Optional;

public interface HighScoreDatabase {
    void batchProcess(List<HighScoreUpdate> scoresToUpdate);

    Optional<List<ScoreEntry>> getTop(ScoreType scoreType, int amt);

    public Optional<Integer> getAverageScore(ScoreType scoreType);
}
