package gg.packetloss.grindstone.guild.passive;

import com.sk89q.commandbook.CommandBook;
import gg.packetloss.grindstone.guild.powers.RoguePower;
import gg.packetloss.grindstone.guild.state.InternalGuildState;
import gg.packetloss.grindstone.guild.state.RogueState;
import gg.packetloss.grindstone.util.ChanceUtil;
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
        InternalGuildState internalState = internalStateLookup.apply(player);
        if (internalState instanceof RogueState && internalState.isEnabled()) {
            return Optional.of((RogueState) internalState);
        }
        return Optional.empty();
    }

    private void metabolizeBadPotions(Player player) {
        List<PotionEffectType> affectedTypes = Arrays.asList(
                PotionEffectType.SLOW_DIGGING, PotionEffectType.BLINDNESS, PotionEffectType.POISON,
                PotionEffectType.CONFUSION, PotionEffectType.WEAKNESS, PotionEffectType.WITHER
        );

        for (PotionEffect effect : player.getActivePotionEffects()) {
            if (!affectedTypes.contains(effect.getType())) {
                continue;
            }

            int newDuration = (int) (effect.getDuration() * ChanceUtil.getRangedRandom(.5, 1.0));
            if (newDuration == 0) {
                player.removePotionEffect(effect.getType());
                continue;
            }

            PotionEffect newEffect = new PotionEffect(
                    effect.getType(),
                    newDuration,
                    effect.getAmplifier(),
                    effect.isAmbient(),
                    effect.hasParticles()
            );
            player.addPotionEffect(newEffect, true);
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
