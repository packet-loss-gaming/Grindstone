/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.blocks.ItemID;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;
import gg.packetloss.grindstone.util.ChatUtil;
import org.bukkit.ChatColor;
import org.bukkit.EntityEffect;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

@ComponentInformation(friendlyName = "Profanity", desc = "Kill Profanity.")
public class ProfanityComponent extends BukkitComponent implements Listener {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = CommandBook.logger();
    private final Server server = CommandBook.server();

    private LocalConfiguration config;

    @Override
    public void enable() {

        this.config = configure(new LocalConfiguration());
        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);
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
        public Set<String> blackListedWords = new HashSet<>(Arrays.asList(
                "shit", "fuck", "penis", "bitch", "piss", "retard", "bastard", "likes dick", "kunt", "cunt", "slut",
                "pussy",
                "pussies", "gay", "whore", "wanker", "bloody hell", "rape", "strip club", "stripper club", "twat",
                "douche",
                "doosh", "handjob", "hand job", "blowjob", "blow job", "fuc", "rimming", "cum", "dildo", "ball sack",
                "ballsack", "hardon", "hard on", "fag", "faggot", "sexual", "jizz", "jackass", "jack ass", "jackoff",
                "jack off", "niger", "nigger", "nutsack", "prick", "queef", "queer", "titty", "tit", "testicle",
                "hooker"));
    }

    // Sign Censor
    @EventHandler(ignoreCancelled = true)
    public void onSignChange(SignChangeEvent event) {

        Player player = event.getPlayer();
        Block block = event.getBlock();
        Location blockLoc = block.getLocation();
        int blockTypeId = event.getBlock().getTypeId();

        // Basic Checks
        if (!config.enableSignCensor)
            return;

        // Check Block Type
        if ((blockTypeId == BlockID.SIGN_POST) || (blockTypeId == BlockID.WALL_SIGN)) {
            // Get Lines
            String[] signLine = event.getLines();

            // Check for profanity and explode if needed
            if (inBlackListedWord(signLine)) {

                // Get rid of that sign!
                block.getWorld().createExplosion(blockLoc, 0);
                blockLoc.getBlock().breakNaturally(new ItemStack(ItemID.SIGN, 1));

                // Mess with the player
                player.setHealth(0);
                player.playEffect(EntityEffect.DEATH);
                ChatUtil.sendWarning(player, "Let that be a lesson to you to not use profanity.");

                // Public Embarrassment? :P
                for (final Player otherPlayer : server.getOnlinePlayers()) {
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

    public boolean inBlackListedWord(String[] lines) {

        // Check for cuss words
        Object[] bLW = config.blackListedWords.toArray();

        for (int i = 0; i < config.blackListedWords.size(); i++) {
            for (String line : lines) {
                if (line.toLowerCase().contains(bLW[i].toString())) {
                    return true;
                }
            }
        }

        return false;
    }

    public String filterString(String string) {

        return filterString(string, true);
    }

    public String filterString(String string, boolean useColor) {

        Object[] bLW = config.blackListedWords.toArray();
        String[] words = string.split(" ");
        StringBuilder out = new StringBuilder();

        for (String word : words) {

            if (useColor) {
                for (Object aBLW : bLW) {

                    if (word.toLowerCase().contains(aBLW.toString().toLowerCase())) {
                        word = loonizeWord(word, true) + ChatColor.WHITE;
                    }
                }
            } else {
                for (Object aBLW : bLW) {

                    if (word.toLowerCase().contains(aBLW.toString().toLowerCase())) {
                        word = loonizeWord(word, false);
                    }
                }
            }
            out.append(word).append(" ");
        }
        return out.toString();
    }

    public String loonizeWord(String word) {

        return loonizeWord(word, true);
    }

    public String loonizeWord(String word, boolean useColor) {

        StringBuilder loonyFilteredString = new StringBuilder();
        if (useColor) {
            for (int f = 0; f < word.length(); f++) {
                loonyFilteredString.append(ChatUtil.loonyColor()).append(ChatUtil.loonyCharacter());
            }
        } else {
            for (int f = 0; f < word.length(); f++) {
                loonyFilteredString.append(ChatUtil.loonyCharacter());
            }
        }

        return loonyFilteredString.toString();
    }
}