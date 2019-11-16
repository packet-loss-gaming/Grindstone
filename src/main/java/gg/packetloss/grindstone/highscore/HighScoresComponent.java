package gg.packetloss.grindstone.highscore;

import com.google.common.base.Joiner;
import com.sk89q.commandbook.CommandBook;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import gg.packetloss.grindstone.admin.AdminComponent;
import gg.packetloss.grindstone.highscore.mysql.MySQLHighScoresDatabase;
import gg.packetloss.grindstone.util.ChatUtil;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@ComponentInformation(friendlyName = "High Scores Component", desc = "High Scores")
public class HighScoresComponent extends BukkitComponent {

    private static final CommandBook inst = CommandBook.inst();
    private static final Logger log = inst.getLogger();
    private static final Server server = CommandBook.server();

    private static final Map<String, ScoreType> nameToScoreType = new HashMap<>();

    static {
        for (Field field : ScoreTypes.class.getFields()) {
            try {
                Object result = field.get(null);
                if (result instanceof ScoreType) {
                    String processedFieldName = field.getName().toLowerCase();
                    nameToScoreType.put(processedFieldName, (ScoreType) result);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @InjectComponent
    private AdminComponent adminComponent;

    private Lock highScoreLock = new ReentrantLock();
    private List<HighScoreUpdate> highScoreUpdates = new ArrayList<>();

    private HighScoreDatabase database = new MySQLHighScoresDatabase();

    @Override
    public void enable() {
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

    private void queueUpdate(HighScoreUpdate update) {
        highScoreLock.lock();
        try {
            highScoreUpdates.add(update);
        } finally {
            highScoreLock.unlock();
        }
    }

    public void update(Player player, ScoreType scoreType, int value) {
        // Disqualify any high score gains in admin mode
        if (adminComponent.isAdmin(player)) {
            return;
        }

        queueUpdate(new HighScoreUpdate(player.getUniqueId(), scoreType, value));
    }

    public List<ScoreEntry> getTop(ScoreType scoreType, int amt) {
        return database.getTop(scoreType, amt).get();
    }

    public List<ScoreEntry> getTop(ScoreType scoreType) {
        return getTop(scoreType, 5);
    }

    private String getFriendlyName(String unfriendlyName) {
        List<String> words = Arrays.stream(unfriendlyName.split("_")).map(
                str -> StringUtils.capitalize(str.toLowerCase())
        ).collect(Collectors.toList());

        return Joiner.on(" ").join(words);
    }

    private String createScoreLine(int rank, ScoreEntry entry, ScoreType scoreType) {
        String playerName = entry.getPlayer().getName();

        return "" + ChatColor.YELLOW + '#' + rank + ' ' +
                ChatColor.BLUE + playerName + "   " +
                (rank == 1 ? ChatColor.GOLD : ChatColor.WHITE) + scoreType.format(entry.getScore());
    }

    private String createScoreTypeLine(String scoreType) {
        return "" + ChatColor.BLUE + getFriendlyName(scoreType);
    }

    public class Commands {
        @Command(aliases = {"highscores", "highscore"},
                usage = "<scope type>", desc = "View high scores",
                flags = "")
        public void highscoresCmd(CommandContext args, CommandSender sender) throws CommandException {
            String scoreTypeString = args.argsLength() == 0 ? ""
                    : args.getJoinedStrings(0).toLowerCase().replaceAll(" ", "_");

            ScoreType scoreType = nameToScoreType.get(scoreTypeString);
            if (scoreType != null) {
                ChatUtil.sendNotice(sender, ChatColor.GOLD + getFriendlyName(scoreTypeString));

                List<ScoreEntry> scores = getTop(scoreType);
                for (int i = 0; i < scores.size(); ++i) {
                    ChatUtil.sendNotice(sender, createScoreLine(i + 1, scores.get(i), scoreType));
                }
            } else {
                ChatUtil.sendNotice(sender, ChatColor.GOLD + "High Score Tables");
                nameToScoreType.keySet().stream()
                        .map(HighScoresComponent.this::createScoreTypeLine)
                        .forEachOrdered((line) -> {
                            ChatUtil.sendNotice(sender, line);
                        });
            }
        }
    }
}