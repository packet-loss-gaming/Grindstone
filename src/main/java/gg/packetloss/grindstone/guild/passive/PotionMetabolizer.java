/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.guild.passive;

import com.sk89q.commandbook.CommandBook;
import gg.packetloss.grindstone.guild.powers.RoguePower;
import gg.packetloss.grindstone.guild.state.InternalGuildState;
import gg.packetloss.grindstone.guild.state.RogueState;
import gg.packetloss.grindstone.util.ChanceUtil;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class PotionMetabolizer implements Runnable {
    private Function<Player, InternalGuildState> internalStateLookup;

    public PotionMetabolizer(Function<Player, InternalGuildState> internalStateLookup) {
        this.internalStateLookup = internalStateLookup;
    }

    private Optional<RogueState> getState(Player player) {
        if (player.getGameMode() == GameMode.SPECTATOR) {
            return Optional.empty();
        }

        InternalGuildState internalState = internalStateLookup.apply(player);
        if (internalState instanceof RogueState && internalState.isEnabled()) {
            return Optional.of((RogueState) internalState);
        }
        return Optional.empty();
    }

    private void metabolizeBadPotions(Player player) {
        List<PotionEffectType> affectedTypes = Arrays.asList(
                PotionEffectType.MINING_FATIGUE, PotionEffectType.BLINDNESS, PotionEffectType.POISON,
                PotionEffectType.NAUSEA, PotionEffectType.WEAKNESS, PotionEffectType.WITHER
        );

        for (PotionEffect effect : player.getActivePotionEffects()) {
            if (!affectedTypes.contains(effect.getType())) {
                continue;
            }

            /* Remove the effect, we'll readd it if it's still relevant below. */
            player.removePotionEffect(effect.getType());

            int newDuration = (int) (effect.getDuration() * ChanceUtil.getRangedRandom(.5, 1.0));
            if (newDuration == 0) {
                continue;
            }

            PotionEffect newEffect = new PotionEffect(
                    effect.getType(),
                    newDuration,
                    effect.getAmplifier(),
                    effect.isAmbient(),
                    effect.hasParticles()
            );
            player.addPotionEffect(newEffect);
        }
    }

    @Override
    public void run() {
        for (Player player : CommandBook.server().getOnlinePlayers()) {
            Optional<RogueState> optState = getState(player);
            if (optState.isEmpty()) {
                continue;
            }

            RogueState state = optState.get();
            if (!state.hasPower(RoguePower.POTION_METABOLIZATION)) {
                continue;
            }

            if (ChanceUtil.getChance(10)) {
                metabolizeBadPotions(player);
            }
        }
    }
}
