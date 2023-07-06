/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone;

import com.sk89q.commandbook.CommandBook;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;
import gg.packetloss.grindstone.util.ChatUtil;
import gg.packetloss.grindstone.util.EnvironmentUtil;
import gg.packetloss.grindstone.util.explosion.ExplosionStateFactory;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.List;

import static gg.packetloss.grindstone.util.ChatUtil.loonizeWord;

@ComponentInformation(friendlyName = "Profanity", desc = "Kill Profanity.")
public class ProfanityComponent extends BukkitComponent implements Listener {
    private LocalConfiguration config;

    @Override
    public void enable() {

        this.config = configure(new LocalConfiguration());
        CommandBook.registerEvents(this);
    }

    @Override
    public void reload() {

        super.reload();
        configure(config);
    }

    private static class LocalConfiguration extends ConfigurationBase {

        @Setting("enable-sign-censor")
        public boolean enableSignCensor = true;
        @Setting("enable-chat-censor")
        public boolean enableChatCensor = false;
        @Setting("censored-words")
        public List<String> censoredWords = List.of(
                "shit", "fuck", "penis", "bitch", "piss", "retard", "bastard", "likes dick", "kunt", "cunt", "slut",
                "pussy",
                "pussies", "gay", "whore", "wanker", "bloody hell", "rape", "strip club", "stripper club", "twat",
                "douche",
                "doosh", "handjob", "hand job", "blowjob", "blow job", "fuc", "rimming", "cum", "dildo", "ball sack",
                "ballsack", "hardon", "hard on", "fag", "faggot", "sexual", "jizz", "jackass", "jack ass", "jackoff",
                "jack off", "niger", "nigger", "nutsack", "prick", "queef", "queer", "titty", "tit", "testicle",
                "hooker");
    }

    // Sign Censor
    @EventHandler(ignoreCancelled = true)
    public void onSignChange(SignChangeEvent event) {

        Player player = event.getPlayer();
        Block block = event.getBlock();
        Location blockLoc = block.getLocation();

        // Basic Checks
        if (!config.enableSignCensor)
            return;

        // Check Block Type
        if (EnvironmentUtil.isSign(block)) {
            // Get Lines
            String[] signLine = event.getLines();

            // Check for profanity and explode if needed
            if (inBlackListedWord(signLine)) {
                // Get rid of that sign!
                ExplosionStateFactory.createFakeExplosion(blockLoc);
                blockLoc.getBlock().breakNaturally();

                // Mess with the player
                player.setHealth(0);
                ChatUtil.sendWarning(player, "Let that be a lesson to you to not use profanity.");

                // Public Embarrassment? :P
                for (final Player otherPlayer : Bukkit.getOnlinePlayers()) {
                    // Don't tell the player we are sending this message
                    if (otherPlayer != player) {
                        ChatUtil.sendNotice(otherPlayer, "The player: "
                                + player.getDisplayName() + " attempted "
                                + "to place a sign containing one or more blacklisted word(s).");
                    }
                }
            }
        }
    }

    // Chat Censor
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        // Basic Checks
        if (!config.enableChatCensor) return;

        event.setMessage(filterString(ChatUtil.runeizeString(event.getMessage()), true));
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onCommandPreProcess(PlayerCommandPreprocessEvent event) {
        // Basic Checks
        if (!config.enableChatCensor) return;

        event.setMessage(filterString(event.getMessage(), false));
    }

    public boolean containsCensoredWord(String string) {
        for (String word : config.censoredWords) {
            if (string.toLowerCase().contains(word)) {
                return true;
            }
        }
        return false;
    }

    public boolean inBlackListedWord(String[] lines) {
        for (String line : lines) {
            if (containsCensoredWord(line)) {
                return true;
            }
        }

        return false;
    }

    public String filterString(String string) {
        return filterString(string, true);
    }

    public String filterString(String string, boolean useColor) {
        StringBuilder out = new StringBuilder();

        for (String word : string.split(" ")) {
            if (containsCensoredWord(word)) {
                out.append(loonizeWord(word, useColor));
                if (useColor) {
                    out.append(ChatColor.WHITE);
                }
            } else {
                out.append(word);
            }
            out.append(" ");
        }
        return out.toString();
    }

}