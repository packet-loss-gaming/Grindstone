/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.highscore;

import gg.packetloss.bukkittext.Text;
import gg.packetloss.bukkittext.TextAction;
import gg.packetloss.grindstone.highscore.scoretype.ScoreType;
import org.bukkit.ChatColor;

import java.util.Optional;

import static gg.packetloss.grindstone.util.StringUtil.toUppercaseTitle;

public class AnnotatedScoreType implements ScoreType {
    private final String name;
    private final ScoreType scoreType;

    protected AnnotatedScoreType(String name, ScoreType scoreType) {
        this.name = name;
        this.scoreType = scoreType;
    }

    public String getName() {
        return name;
    }

    public String getLookupName() {
        return name;
    }

    public String getDisplayNameNoColor() {
        return toUppercaseTitle(name);
    }

    public Text getDisplayName() {
        return Text.of(ChatColor.BLUE, getDisplayNameNoColor());
    }

    public Text getActiveDisplayName() {
        return Text.of(
            getDisplayName(),
            TextAction.Click.runCommand("/highscore " + getLookupName())
        );
    }

    @Override
    public int getId() {
        return scoreType.getId();
    }

    @Override
    public boolean isEnabledForGoblet() {
        return scoreType.isEnabledForGoblet();
    }

    @Override
    public Optional<String> getGobletName() {
        return scoreType.getGobletName();
    }

    @Override
    public boolean isGobletEquivalent(ScoreType scoreType) {
        return scoreType.isGobletEquivalent(scoreType);
    }

    @Override
    public UpdateMethod getUpdateMethod() {
        return scoreType.getUpdateMethod();
    }

    @Override
    public Order getOrder() {
        return scoreType.getOrder();
    }

    public String format(long score) {
        return scoreType.format(score);
    }
}
