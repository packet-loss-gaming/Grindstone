package gg.packetloss.grindstone.util.dropttable;

import org.apache.logging.log4j.util.TriConsumer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class PerformanceDropTable implements DropTable<PerformanceKillInfo> {
    private List<ChanceEntry> takeAllDrops = new ArrayList<>();
    private List<SliceEntry> slicedDrops = new ArrayList<>();

    public void registerTakeAllDrop(Supplier<ItemStack> supplier) {
        registerTakeAllDrop(1, supplier);
    }

    public void registerTakeAllDrop(int chance, Supplier<ItemStack> supplier) {
        takeAllDrops.add(new ChanceEntry(chance, supplier));
    }

    public void registerSlicedDrop(Function<PerformanceKillInfo, Integer> points, TriConsumer<SliceInfo, Player, Consumer<ItemStack>> consumer) {
        slicedDrops.add(new SliceEntry(points, consumer));
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

        for (SliceEntry sliceEntry : slicedDrops) {
            SliceInfo sliceInfo = new SliceInfo(info, sliceEntry.getPointSupplier().apply(info));

            for (Player player : info.getDamagers()) {
                sliceEntry.getConsumer().accept(sliceInfo, player, (itemStack) -> {
                    drops.accept(new SimpleDrop(player, itemStack));
                });
            }
        }
    }

    public static class SliceInfo {
        private final PerformanceKillInfo killInfo;
        private final int points;

        public SliceInfo(PerformanceKillInfo killInfo, int points) {
            this.killInfo = killInfo;
            this.points = points;
        }

        public PerformanceKillInfo getKillInfo() {
            return killInfo;
        }

        public int getPoints() {
            return points;
        }
    }

    private static class SliceEntry {
        private final Function<PerformanceKillInfo, Integer> pointSupplier;
        private final TriConsumer<SliceInfo, Player, Consumer<ItemStack>> consumer;

        private SliceEntry(Function<PerformanceKillInfo, Integer> pointSupplier,
                           TriConsumer<SliceInfo, Player, Consumer<ItemStack>> consumer) {
            this.pointSupplier = pointSupplier;
            this.consumer = consumer;
        }

        public Function<PerformanceKillInfo, Integer> getPointSupplier() {
            return pointSupplier;
        }

        public TriConsumer<SliceInfo, Player, Consumer<ItemStack>> getConsumer() {
            return consumer;
        }
    }
}
