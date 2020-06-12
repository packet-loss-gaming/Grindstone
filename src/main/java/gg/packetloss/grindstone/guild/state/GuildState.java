package gg.packetloss.grindstone.guild.state;

import com.sk89q.commandbook.CommandBook;
import gg.packetloss.bukkittext.Text;
import gg.packetloss.bukkittext.TextAction;
import gg.packetloss.grindstone.events.guild.GuildGrantExpEvent;
import gg.packetloss.grindstone.events.guild.GuildPowersDisableEvent;
import gg.packetloss.grindstone.events.guild.GuildPowersEnableEvent;
import gg.packetloss.grindstone.guild.GuildLevel;
import gg.packetloss.grindstone.guild.GuildType;
import gg.packetloss.grindstone.guild.base.GuildBase;
import gg.packetloss.grindstone.guild.powers.GuildPower;
import gg.packetloss.grindstone.guild.setting.GuildSetting;
import gg.packetloss.grindstone.guild.setting.GuildSettingUpdate;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.StringUtil;
import gg.packetloss.grindstone.util.chat.TextComponentChatPaginator;
import gg.packetloss.grindstone.util.task.DebounceHandle;
import gg.packetloss.grindstone.util.task.TaskBuilder;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class GuildState {
    private final Player player;
    private final InternalGuildState state;
    private final GuildBase base;

    private DebounceHandle<Double> gainedExpDebounce;

    public GuildState(Player player, InternalGuildState state, GuildBase base) {
        this.player = player;
        this.state = state;
        this.base = base;
    }

    public boolean isEnabled() {
        return state.isEnabled();
    }

    public boolean isDisabled() {
        return !isEnabled();
    }

    public GuildType getType() {
        return state.getType();
    }

    public int getLevel() {
        return GuildLevel.getLevel(state.getExperience());
    }

    public void setVirtualLevel(int level) {
        state.setVirtualLevel(level);
    }

    public void clearVirtualLevel() {
        state.clearVirtualLevel();
    }

    public boolean enablePowers() {
        if (isEnabled()) {
            return true;
        }

        GuildPowersEnableEvent event = new GuildPowersEnableEvent(player, state.getType());
        CommandBook.server().getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            return false;
        }

        state.setEnabled(true);
        return true;
    }

    public boolean disablePowers() {
        if (isDisabled()) {
            return true;
        }

        GuildPowersDisableEvent event = new GuildPowersDisableEvent(player, state.getType());
        CommandBook.server().getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            return false;
        }

        state.setEnabled(false);
        return true;
    }

    public void sendSettings(Player player) {
        List<GuildSetting> settings = state.getSettings().getAllSorted();
        for (GuildSetting setting : settings) {
            Text text = Text.of(
                    ChatColor.YELLOW,
                    setting.getName(),
                    ": ",
                    setting.getValueAsText()
            );

            String defaultUpdateString = setting.getDefaultUpdateString();
            if (!defaultUpdateString.isEmpty()) {
                text = Text.of(
                        text,
                        TextAction.Hover.showText(Text.of("Update")),
                        TextAction.Click.runCommand("/guild settings " + defaultUpdateString)
                );
            }

            player.sendMessage(text.build());
        }
    }

    public boolean updateSetting(GuildSettingUpdate setting) {
        return state.getSettings().updateSetting(setting);
    }

    public void sendLevelChart(Player player, int page) {
        int currentLevel = getLevel();
        GuildPower[] powers = state.getType().getPowers();

        List<GuildLevel> levels = GuildLevel.getLevels();
        new TextComponentChatPaginator<GuildLevel>(ChatColor.GOLD, "Levels") {
            @Override
            public Optional<String> getPagerCommand(int page) {
                return Optional.of("/guild level -p " + page);
            }

            @Override
            public Text format(GuildLevel level) {
                boolean belowLevel = currentLevel < level.getLevel();

                ChatColor levelColor = (belowLevel ? ChatColor.RED : ChatColor.DARK_GREEN);

                List<GuildPower> unlocks = new ArrayList<>();
                for (GuildPower power : powers) {
                    if (power.getUnlockLevel() == level.getLevel()) {
                        unlocks.add(power);
                    }
                }

                // Create level text
                Text levelText = Text.of(
                        Text.of(
                                levelColor,
                                level.getLevel()
                        ),
                        ChatColor.YELLOW,
                        " (",
                        ChatUtil.WHOLE_NUMBER_FORMATTER.format(level.getExperience()),
                        " exp)",
                        (unlocks.isEmpty() ? Text.of("") : Text.of(" - ", Text.of(ChatColor.GOLD, unlocks.size()), " unlock(s)"))
                );

                // Create hover text
                Text hoverText;
                if (belowLevel) {
                    hoverText = Text.of(
                            ChatUtil.WHOLE_NUMBER_FORMATTER.format(
                                    level.getExperience() - state.getExperience()
                            ),
                            " experience remaining"
                    );
                } else {
                    hoverText = Text.of("Unlocked:");
                }

                for (GuildPower unlock : unlocks) {
                    hoverText = Text.of(
                            hoverText,
                            "\n",
                            ChatColor.YELLOW,
                            " - ",
                            levelColor,
                            StringUtil.toTitleCase(unlock.name())
                    );
                }

                // Apply hover effect if relevant
                if (belowLevel || unlocks.size() > 0) {
                    levelText = Text.of(
                            levelText,
                            TextAction.Hover.showText(hoverText)
                    );
                }

                return levelText;
            }
        }.display(player, levels, page);
    }

    private void lazySetupExpDebounce() {
        if (gainedExpDebounce != null) {
            return;
        }

        TaskBuilder.Debounce<Double> builder = TaskBuilder.debounce();
        builder.setWaitTime(2); // 2 because delaying by 1 tick for the same action isn't that uncommon
        builder.setInitialValue(0D);
        builder.setUpdateFunction(Double::sum);
        builder.setBounceAction((exp) -> {
            player.sendMessage(Text.of(
                    ChatColor.GOLD, "Guild Experience: +",
                    Text.of(ChatColor.WHITE, new DecimalFormat("#.##").format(exp))
            ).build());
        });
        builder.setExistingState(state.getExpNoticeDebounce());

        gainedExpDebounce = builder.build();

        // Update or set the shared state object
        state.setExpNoticeDebounce(gainedExpDebounce.getState());
    }

    public boolean grantExp(double amount) {
        if (isDisabled()) {
            return false;
        }

        GuildGrantExpEvent event = new GuildGrantExpEvent(player, getType(), amount);
        CommandBook.callEvent(event);

        boolean success = !event.isCancelled();
        if (success && state.getSettings().shouldPrintExpVerbose()) {
            lazySetupExpDebounce();
            gainedExpDebounce.accept(event.getGrantedExp());
        }

        return success;
    }

    public CompletableFuture<Boolean> teleportToGuild() {
        return player.teleportAsync(base.getLocation());
    }
}
