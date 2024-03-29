/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.highscore;

import com.google.gson.reflect.TypeToken;
import com.sk89q.commandbook.CommandBook;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import gg.packetloss.grindstone.admin.AdminComponent;
import gg.packetloss.grindstone.chatbridge.ChatBridgeComponent;
import gg.packetloss.grindstone.data.DatabaseComponent;
import gg.packetloss.grindstone.highscore.scoretype.GobletScoreType;
import gg.packetloss.grindstone.highscore.scoretype.ScoreType;
import gg.packetloss.grindstone.highscore.scoretype.ScoreTypes;
import gg.packetloss.grindstone.highscore.sql.SQLHighScoresDatabase;
import gg.packetloss.grindstone.util.CollectionUtil;
import gg.packetloss.grindstone.util.PluginTaskExecutor;
import gg.packetloss.grindstone.util.TimeUtil;
import gg.packetloss.grindstone.util.persistence.SingleFileFilesystemStateHelper;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

@ComponentInformation(friendlyName = "High Scores Component", desc = "High Scores")
@Depend(components = {ChatBridgeComponent.class, DatabaseComponent.class})
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
                    String processedFieldName = field.getName().toUpperCase();
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

    private HighScoreDatabase database = new SQLHighScoresDatabase();

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

        // Register user facing commands
        CommandBook.getComponentRegistrar().registerTopLevelCommands((registrar) -> {
            ScoreTypeConverter.register(registrar, this);
            registrar.register(HighScoreCommandsRegistration.builder(), new HighScoreCommands(this));
        });

        CommandBook.registerEvents(new GobletLoginListener(this));

        Bukkit.getScheduler().runTaskTimerAsynchronously(CommandBook.inst(), this::processUpdateQueue, 25, 20);
    }

    @Override
    public void disable() {
        processUpdateQueue();
    }

    private void processUpdateQueue() {
        highScoreLock.lock();

        List<HighScoreUpdate> scoresToUpdate;
        try {
            scoresToUpdate = highScoreUpdates;
            highScoreUpdates = new ArrayList<>();
        } finally {
            highScoreLock.unlock();
        }

        database.batchProcess(scoresToUpdate);
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

        String gobletName = getGobletScoreType().getDisplayNameNoColor();
        String winMessage = ChatColor.YELLOW + player.getName() + " has won the " + gobletName + "!";
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

        Bukkit.getScheduler().runTaskLater(
            CommandBook.inst(),
            this::checkGobletForUpdate,
            TimeUtil.getTicksTillNextMonth()
        );
    }

    private boolean isCurrentGoblet(ScoreType scoreType) {
        return scoreType.isGobletEquivalent(gobletScoreType);
    }

    private void update(UUID playerID, ScoreType scoreType, BigInteger value) {
        highScoreLock.lock();
        try {
            highScoreUpdates.add(new HighScoreUpdate(playerID, scoreType, value));
            if (isCurrentGoblet(scoreType)) {
                highScoreUpdates.add(new HighScoreUpdate(playerID, gobletScoreType, value));
            }
        } finally {
            highScoreLock.unlock();
        }
    }

    public void update(OfflinePlayer player, ScoreType scoreType, BigInteger value) {
        // FIXME: Ideally this wouldn't be necessary, need to make sure the PluginTaskExecutor
        // isn't shutdown before the high scores, or any dependent component.
        try {
            PluginTaskExecutor.submitAsync(() -> {
                // Disqualify any high score gains in admin mode
                if (adminComponent.isAdmin(player)) {
                    return;
                }

                update(player.getUniqueId(), scoreType, value);
            });
        } catch (RejectedExecutionException ex) {
            if (!(player instanceof Player onlinePlayer)) {
                return;
            }

            if (adminComponent.isAdmin(onlinePlayer)) {
                return;
            }

            update(onlinePlayer.getUniqueId(), scoreType, value);
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

    private String getGobletName() {
        String baseName = gobletScoreType.getGobletName().orElseGet(() -> {
            return idToName.get(gobletScoreType.getBaseScoreType().getId());
        });
        return baseName + "_GOBLET";
    }

    public AnnotatedScoreType getGobletScoreType() {
        return new AnnotatedScoreType(getGobletName(), gobletScoreType);
    }

    public List<AnnotatedScoreType> getScoreTypes() {
        List<AnnotatedScoreType> scoreTypes = new ArrayList<>();
        for (Map.Entry<String, ScoreType> entry : nameToScoreType.entrySet()) {
            scoreTypes.add(new AnnotatedScoreType(entry.getKey(), entry.getValue()));
        }
        scoreTypes.add(getGobletScoreType());
        return scoreTypes;
    }
}
