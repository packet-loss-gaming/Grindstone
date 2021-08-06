/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.highscore;

import com.google.common.base.Joiner;
import gg.packetloss.bukkittext.Text;
import gg.packetloss.bukkittext.TextAction;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.chat.ChatConstants;
import gg.packetloss.grindstone.util.chat.TextComponentChatPaginator;
import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.enginehub.piston.annotation.Command;
import org.enginehub.piston.annotation.CommandContainer;
import org.enginehub.piston.annotation.param.Arg;
import org.enginehub.piston.annotation.param.ArgFlag;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@CommandContainer
public class HighScoreCommands {
    private final HighScoresComponent component;

    public HighScoreCommands(HighScoresComponent component) {
        this.component = component;
    }

    private Text createScoreLine(int rank, ScoreEntry entry, AnnotatedScoreType scoreType) {
        String playerName = entry.getPlayer().getName();

        return Text.of(ChatColor.YELLOW, '#', rank, ' ',
            ChatColor.BLUE, StringUtils.rightPad(playerName, ChatConstants.MAX_PLAYER_NAME_LENGTH), "   ",
            (rank == 1 ? ChatColor.GOLD : ChatColor.WHITE), scoreType.format(entry.getScore()));
    }

    @Command(name = "highscore", desc = "View a highscore table.")
    public void highScoreCmd(CommandSender sender, @Arg(desc = "score type") AnnotatedScoreType scoreType) {
        ChatUtil.sendNotice(sender, ChatColor.GOLD + scoreType.getDisplayName());

        List<ScoreEntry> scores = component.getTop(scoreType);
        for (int i = 0; i < scores.size(); ++i) {
            sender.sendMessage(createScoreLine(i + 1, scores.get(i), scoreType).build());
        }
    }

    private Text createScoreTypeLine(AnnotatedScoreType scoreType) {
        return Text.of(
            ChatColor.BLUE, scoreType.getDisplayName(),
            TextAction.Click.runCommand("/highscore " + scoreType.getLookupName())
        );
    }

    @Command(name = "highscores", desc = "View a list of highscore tables.")
    public void highScoresCmd(CommandSender sender,
                              @ArgFlag(name = 'p', desc = "page", def = "1") int page,
                              @Arg(name = "tableFilter", desc = "table filter", def = "", variable = true) List<String> tableFilterArgs) {
        String tableFilter = Joiner.on(' ').join(tableFilterArgs).toUpperCase();

        List<AnnotatedScoreType> scoreTypes = component.getScoreTypes().stream().filter(
            (st) -> st.getDisplayName().contains(tableFilter)
        ).sorted(Comparator.comparing(AnnotatedScoreType::getLookupName)).collect(Collectors.toList());

        new TextComponentChatPaginator<AnnotatedScoreType>(ChatColor.GOLD, "High Score Tables") {
            @Override
            public Optional<String> getPagerCommand(int page) {
                return Optional.of("/highscores -p " + page + " " + tableFilter);
            }

            @Override
            public Text format(AnnotatedScoreType scoreType) {
                return createScoreTypeLine(scoreType);
            }
        }.display(sender, scoreTypes, page);
    }

}
