package gg.packetloss.grindstone.util.dropttable;

import org.apache.logging.log4j.util.TriConsumer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class PerformanceDropTable implements DropTable<PerformanceKillInfo> {
    private List<ChanceEntry> takeAllDrops = new ArrayList<>();
    private List<TriConsumer<Player, Double, Consumer<ItemStack>>> slicedDrops = new ArrayList<>();

    public void registerTakeAllDrop(Supplier<ItemStack> supplier) {
        registerTakeAllDrop(1, supplier);
    }

    public void registerTakeAllDrop(int chance, Supplier<ItemStack> supplier) {
        takeAllDrops.add(new ChanceEntry(chance, supplier));
    }

    public void registerSlicedDrop(TriConsumer<Player, Double, Consumer<ItemStack>> consumer) {
        slicedDrops.add(consumer);
    }

    @Override
    public void getDrops(PerformanceKillInfo info, Consumer<Drop> drops) {
        Optional<Player> optTopDamager = info.getTopDamager();
        if (optTopDamager.isEmpty()) {
            return;
        }

        int chanceModifier = info.getChanceModifier();

        Player topDamager = optTopDamager.get();
        for (var takeAllDrop : takeAllDrops) {
            takeAllDrop.get(chanceModifier).ifPresent((stack) -> {
                drops.accept(new SimpleDrop(topDamager, stack));
            });
        }

        for (var slicedDrop : slicedDrops) {
            for (Player player : info.getDamagers()) {
                double damage = info.getDamageDone(player).orElseThrow();
                slicedDrop.accept(player, damage, (itemStack) -> {
                    drops.accept(new SimpleDrop(player, itemStack));
                });
            }
        }
    }
}
