package gg.packetloss.grindstone.ic.ics;

import com.sk89q.craftbook.ChangedSign;
import com.sk89q.craftbook.bukkit.util.CraftBookBukkitUtil;
import com.sk89q.craftbook.mechanics.ic.*;
import com.sk89q.craftbook.util.SearchArea;
import gg.packetloss.grindstone.guild.GuildComponent;
import gg.packetloss.grindstone.guild.GuildType;
import org.bukkit.Server;
import org.bukkit.entity.Player;

import java.util.Optional;

public class GuildDetector extends AbstractSelfTriggeredIC {
    public GuildDetector(Server server, ChangedSign block, ICFactory factory) {
        super(server, block, factory);
    }

    @Override
    public String getTitle() {
        return "Guild Detection";
    }

    @Override
    public String getSignTitle() {
        return "G-DETECTION";
    }

    @Override
    public void trigger(ChipState chip) {
        if (chip.getInput(0)) {
            chip.setOutput(0, invertOutput != isDetected());
        }
    }

    @Override
    public void think(ChipState state) {
        state.setOutput(0, invertOutput != isDetected());
    }

    private SearchArea area;

    private GuildType guildType;
    private boolean invertOutput = false;
    private boolean invertDetection = false;

    private static Optional<GuildType> tryToParseGuild(String text) {
        try {
            return Optional.of(GuildType.valueOf(text.replaceAll("[!^]", "")));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    @Override
    public void load() {
        guildType = tryToParseGuild(getLine(3)).orElseThrow();

        invertOutput = getLine(3).contains("!");
        invertDetection = getLine(3).contains("^");

        area = SearchArea.createArea(CraftBookBukkitUtil.toSign(getSign()).getBlock(), getLine(2));
    }

    private boolean doesGuildMatch(Player player) {
        GuildComponent guildComponent = ((GuildDetector.Factory) getFactory()).getGuildComponent();
        return guildComponent.getState(player)
                .filter((gs) -> gs.getType() == guildType)
                .isPresent();
    }

    private boolean isDetected() {
        for (Player player : area.getPlayersInArea()) {
            if (player == null || !player.isValid()) {
                continue;
            }

            if (invertDetection != doesGuildMatch(player)) {
                return true;
            }
        }

        return false;
    }

    public static class Factory extends AbstractICFactory implements RestrictedIC {
        private GuildComponent guild;

        public Factory(Server server, GuildComponent guild) {
            super(server);
            this.guild = guild;
        }

        public GuildComponent getGuildComponent() {
            return guild;
        }

        @Override
        public IC create(ChangedSign sign) {
            return new GuildDetector(getServer(), sign, this);
        }

        @Override
        public void verify(ChangedSign sign) throws ICVerificationException {
            if (!SearchArea.createArea(CraftBookBukkitUtil.toSign(sign).getBlock(), sign.getLine(2)).isValid()) {
                throw new ICVerificationException("Invalid SearchArea on line 3!");
            }

            if (tryToParseGuild(sign.getLine(3)).isEmpty()) {
                throw new ICVerificationException("Invalid GuildType on line 4!");
            }
        }

        @Override
        public String getShortDescription() {
            return "Detects players in a guild within a radius.";
        }

        @Override
        public String[] getLineHelp() {
            return new String[] {
                    "SearchArea",
                    "GuildType"
            };
        }
    }
}