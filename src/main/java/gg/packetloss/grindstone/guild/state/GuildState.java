package gg.packetloss.grindstone.guild.state;

import com.sk89q.commandbook.CommandBook;
import gg.packetloss.bukkittext.Text;
import gg.packetloss.bukkittext.TextAction;
import gg.packetloss.grindstone.events.guild.GuildPowersDisableEvent;
import gg.packetloss.grindstone.events.guild.GuildPowersEnableEvent;
import gg.packetloss.grindstone.guild.GuildLevel;
import gg.packetloss.grindstone.guild.GuildType;
import gg.packetloss.grindstone.guild.powers.GuildPower;
import gg.packetloss.grindstone.guild.setting.GuildSetting;
import gg.packetloss.grindstone.guild.setting.GuildSettingUpdate;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.StringUtil;
import gg.packetloss.grindstone.util.chat.TextComponentChatPaginator;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class GuildState {
    private Player player;
    private InternalGuildState state;

    public GuildState(Player player, InternalGuildState state) {
        this.player = player;
        this.state = state;
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
        List<GuildSetting> settings = state.getSettings().getAll();
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
}
