package gg.packetloss.grindstone.highscore;

import java.util.List;
import java.util.Optional;

public interface HighScoreDatabase {
    void batchProcess(List<HighScoreUpdate> scoresToUpdate);

    Optional<List<ScoreEntry>> getTop(ScoreType scoreType, int amt);
}
