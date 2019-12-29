package gg.packetloss.grindstone.guild;

import org.apache.commons.lang3.Validate;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class GuildLevel {
    private static final int MAX_LEVEL = 100;
    private static final long[] LEVELS = new long[MAX_LEVEL];

    private static void computeLevel(int levelIndex) {
        LEVELS[levelIndex] = levelIndex * levelIndex * 600;
    }

    static {
        for (int i = 0; i < MAX_LEVEL; ++i) {
            computeLevel(i);
        }
    }

    private int level;

    private GuildLevel(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level + 1;
    }

    public long getExperience() {
        return LEVELS[level];
    }

    public static List<GuildLevel> getLevels() {
        return IntStream.range(0, MAX_LEVEL).mapToObj(GuildLevel::new).collect(Collectors.toList());
    }

    public static long getExperienceForLevel(int level) {
        Validate.isTrue(0 < level && level <= MAX_LEVEL);
        return LEVELS[Math.min(MAX_LEVEL, --level)];
    }

    public static int getLevel(long currentExp) {
        for (int i = 0; i < MAX_LEVEL; ++i) {
            if (LEVELS[i] <= currentExp) {
                continue;
            }

            return i;
        }

        throw new IllegalStateException();
    }

    public static Optional<Integer> getNewLevel(long currentExp, long newExp) {
        int currentLevel = getLevel(currentExp);
        if (currentLevel == MAX_LEVEL) {
            return Optional.empty();
        }

        // noinspection UnnecessaryLocalVariable
        int nextLevelIndex = currentLevel;
        if (currentExp + newExp >= LEVELS[nextLevelIndex]) {
            int nextLevel = nextLevelIndex + 1;
            return Optional.of(nextLevel);
        }

        return Optional.empty();
    }
}
