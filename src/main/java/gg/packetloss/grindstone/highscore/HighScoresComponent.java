/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.highscore;

import com.google.common.collect.Lists;
import com.google.gson.reflect.TypeToken;
import com.sk89q.commandbook.CommandBook;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import gg.packetloss.bukkittext.Text;
import gg.packetloss.bukkittext.TextAction;
import gg.packetloss.grindstone.admin.AdminComponent;
import gg.packetloss.grindstone.chatbridge.ChatBridgeComponent;
import gg.packetloss.grindstone.data.DataBaseComponent;
import gg.packetloss.grindstone.highscore.mysql.MySQLHighScoresDatabase;
import gg.packetloss.grindstone.highscore.scoretype.GobletScoreType;
import gg.packetloss.grindstone.highscore.scoretype.ScoreType;
import gg.packetloss.grindstone.highscore.scoretype.ScoreTypes;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.CollectionUtil;
import gg.packetloss.grindstone.util.StringUtil;
import gg.packetloss.grindstone.util.TimeUtil;
import gg.packetloss.grindstone.util.chat.ChatConstants;
import gg.packetloss.grindstone.util.chat.TextComponentChatPaginator;
import gg.packetloss.grindstone.util.persistence.SingleFileFilesystemStateHelper;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

import static gg.packetloss.grindstone.util.StringUtil.toTitleCase;
import static gg.packetloss.grindstone.util.StringUtil.toUppercaseTitle;

@ComponentInformation(friendlyName = "High Scores Component", desc = "High Scores")
@Depend(components = {ChatBridgeComponent.class, DataBaseComponent.class})
public class HighScoresComponent extends BukkitComponent {

    private static final CommandBook inst = CommandBook.inst();
    private static final Logger log = inst.getLogger();
    private static final Server server = CommandBook.server();

    private static final Map<String, ScoreType> nameToScoreType = new HashMap<>();
    private static final Map<Integer, ScoreType> idToScoreType = new HashMap<>();
    private static final Map<Integer, String> idToName = new HashMap<>();

    static {
        for (Field field : ScoreTypes.class.getFields()) {
            try {
                Object result = field.get(null);
                if (result instanceof ScoreType) {
                    String processedFieldName = field.getName().toLowerCase();
                    nameToScoreType.put(processedFieldName, (ScoreType) result);
                    idToScoreType.put(((ScoreType) result).getId(), (ScoreType) result);
                    idToName.put(((ScoreType) result).getId(), processedFieldName);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @InjectComponent
    private AdminComponent adminComponent;
    @InjectComponent
    private ChatBridgeComponent chatBridge;

    private Lock highScoreLock = new ReentrantLock();
    private List<HighScoreUpdate> highScoreUpdates = new ArrayList<>();

    private HighScoreDatabase database = new MySQLHighScoresDatabase();

    private GobletScoreType gobletScoreType;
    private GobletState gobletState = new GobletState();
    private SingleFileFilesystemStateHelper<GobletState> stateHelper;

    @Override
    public void enable() {
        try {
            stateHelper = new SingleFileFilesystemStateHelper<>("goblet-high-score.json", new TypeToken<>() { });
            stateHelper.load().ifPresent(loadedState -> gobletState = loadedState);
        } catch (IOException e) {
            e.printStackTrace();
        }

        checkGobletForUpdate();

        registerCommands(Commands.class);

        server.getScheduler().runTaskTimerAsynchronously(inst, () -> {
            highScoreLock.lock();

            List<HighScoreUpdate> scoresToUpdate;
            try {
                scoresToUpdate = highScoreUpdates;
                highScoreUpdates = new ArrayList<>();
            } finally {
                highScoreLock.unlock();
            }

            database.batchProcess(scoresToUpdate);
        }, 25, 20);
    }

    private ScoreType getNewGobletScoreType() {
        do {
            ScoreType scoreType = CollectionUtil.getElement(idToScoreType.values());

            // Don't allow the same thing twice in a row
            if (gobletScoreType != null && gobletScoreType.isGobletEquivalent(scoreType)) {
                continue;
            }

            if (scoreType.isEnabledForGoblet()) {
                return scoreType;
            }
        } while (true);
    }

    private void loadGobletScoreType() {
        gobletScoreType = gobletState.loadScoreType(idToScoreType);
    }

    private void addGobletWinner(OfflinePlayer player) {
        gobletState.addWinner(player.getUniqueId());

        String friendlyGobletName = StringUtil.toTitleCase(getGobletName());
        String winMessage = ChatColor.YELLOW + player.getName() + " has won the " + friendlyGobletName + "!";
        Bukkit.broadcastMessage(winMessage);
        chatBridge.broadcast(ChatColor.stripColor(winMessage));
    }

    private void refreshStaleGoblet() {
        try {

            if (gobletState.wasActive()) {
                // Initialize the goblet score type in case we just started the server
                loadGobletScoreType();

                // Get the winner if the goblet if there was one
                Optional<ScoreEntry> optWinner = getBest(gobletScoreType);

                // Clear the goblet table, stop processing if we fail to clear the scores
                if (!deleteAllScores(gobletScoreType)) {
                    return;
                }

                // Scores are cleared, award a winner, start a new score type, and save.
                // It's important that this is called before the score type is reloaded.
                optWinner.ifPresent((scoreEntry -> {
                    addGobletWinner(scoreEntry.getPlayer());
                }));
            }

            gobletState.setScoreType(getNewGobletScoreType());
            stateHelper.save(gobletState);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void checkGobletForUpdate() {
        if (gobletState.isStale()) {
            refreshStaleGoblet();
        }

        loadGobletScoreType();

        server.getScheduler().runTaskLater(
            CommandBook.inst(),
            this::checkGobletForUpdate,
            TimeUtil.getTicksTillNextMonth()
        );
    }

    private void queueUpdate(HighScoreUpdate update) {
        highScoreLock.lock();
        try {
            highScoreUpdates.add(update);
        } finally {
            highScoreLock.unlock();
        }
    }

    private boolean isCurrentGoblet(ScoreType scoreType) {
        return scoreType.isGobletEquivalent(gobletScoreType);
    }

    public void update(Player player, ScoreType scoreType, long value) {
        // Disqualify any high score gains in admin mode
        if (adminComponent.isAdmin(player)) {
            return;
        }

        queueUpdate(new HighScoreUpdate(player.getUniqueId(), scoreType, value));
        if (isCurrentGoblet(scoreType)) {
            queueUpdate(new HighScoreUpdate(player.getUniqueId(), gobletScoreType, value));
        }
    }

    private boolean deleteAllScores(ScoreType scoreType) {
        return database.deleteAllScores(scoreType);
    }

    public Optional<ScoreEntry> getBest(ScoreType scoreType) {
        List<ScoreEntry> scores = database.getTop(scoreType, 1).orElse(List.of());
        if (scores.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(scores.get(0));
    }

    public List<ScoreEntry> getTop(ScoreType scoreType, int amt) {
        return database.getTop(scoreType, amt).get();
    }

    public List<ScoreEntry> getTop(ScoreType scoreType) {
        return getTop(scoreType, 5);
    }

    public Optional<Integer> getAverage(ScoreType scoreType) {
        return database.getAverageScore(scoreType);
    }

    private Text createScoreLine(int rank, ScoreEntry entry, ScoreType scoreType) {
        String playerName = entry.getPlayer().getName();

        return Text.of(ChatColor.YELLOW, '#', rank, ' ',
                ChatColor.BLUE, StringUtils.rightPad(playerName, ChatConstants.MAX_PLAYER_NAME_LENGTH), "   ",
                (rank == 1 ? ChatColor.GOLD : ChatColor.WHITE), scoreType.format(entry.getScore()));
    }

    private Text createScoreTypeLine(String scoreType) {
        return Text.of(
                ChatColor.BLUE, toUppercaseTitle(scoreType),
                TextAction.Click.runCommand("/highscores " + scoreType),
                TextAction.Hover.showText(Text.of("View high scores for " + toTitleCase(scoreType)))
        );
    }

    private String getGobletName() {
        String baseName = gobletScoreType.getGobletName().orElseGet(() -> {
            return idToName.get(gobletScoreType.getBaseScoreType().getId());
        });
        return baseName + "_goblet";
    }

    private ScoreType getScoreTypeFromString(String scoreTypeName) {
        if (scoreTypeName.equals(getGobletName())) {
            return gobletScoreType;
        }

        return nameToScoreType.get(scoreTypeName);
    }

    public class Commands {
        @Command(aliases = {"highscores", "highscore"},
                usage = "<scope type>", desc = "View high scores",
                flags = "p:")
        public void highscoresCmd(CommandContext args, CommandSender sender) throws CommandException {
            String scoreTypeString = args.argsLength() == 0 ? ""
                    : args.getJoinedStrings(0).toLowerCase().replaceAll(" ", "_");

            ScoreType scoreType = getScoreTypeFromString(scoreTypeString);
            if (scoreType != null) {
                ChatUtil.sendNotice(sender, ChatColor.GOLD + toTitleCase(scoreTypeString));

                List<ScoreEntry> scores = getTop(scoreType);
                for (int i = 0; i < scores.size(); ++i) {
                    sender.sendMessage(createScoreLine(i + 1, scores.get(i), scoreType).build());
                }
            } else {
                List<String> tables = Lists.newArrayList(nameToScoreType.keySet());
                tables.add(getGobletName());
                tables.sort(Comparator.naturalOrder());

                new TextComponentChatPaginator<String>(ChatColor.GOLD, "High Score Tables") {
                    @Override
                    public Optional<String> getPagerCommand(int page) {
                        return Optional.of("/highscores -p " + page);
                    }

                    @Override
                    public Text format(String scoreType) {
                        return createScoreTypeLine(scoreType);
                    }
                }.display(sender, tables, args.getFlagInteger('p', 1));
            }
        }
    }
}
