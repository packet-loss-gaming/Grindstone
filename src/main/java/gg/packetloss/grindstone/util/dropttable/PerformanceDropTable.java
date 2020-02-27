package gg.packetloss.grindstone.util.dropttable;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class PerformanceDropTable implements DropTable<PerformanceKillInfo> {
    private List<ChanceEntry<PerformanceKillInfo>> takeAllDrops = new ArrayList<>();
    private List<SlicedValueEntry<PerformanceKillInfo, PerformanceSlicedDropInfo>> slicedDrops = new ArrayList<>();

    public void registerTakeAllDrop(Supplier<ItemStack> supplier) {
        registerTakeAllDrop((info) -> 1, supplier);
    }

    public void registerTakeAllDrop(int chance, Supplier<ItemStack> supplier) {
        registerTakeAllDrop((info) -> chance, supplier);
    }

    public void registerTakeAllDrop(Function<PerformanceKillInfo, Integer> chance, Supplier<ItemStack> supplier) {
        takeAllDrops.add(new ChanceEntry<>(chance, supplier));
    }

    public void registerSlicedDrop(Function<PerformanceKillInfo, Integer> points,
                                   BiConsumer<PerformanceSlicedDropInfo, Consumer<ItemStack>> consumer) {
        slicedDrops.add(new SlicedValueEntry<>(points, consumer));
    }

    @Override
    public void getDrops(PerformanceKillInfo info, Consumer<Drop> drops) {
        Optional<Player> optTopDamager = info.getTopDamager();
        if (optTopDamager.isEmpty()) {
            return;
        }

        Player topDamager = optTopDamager.get();
        for (var takeAllDrop : takeAllDrops) {
            takeAllDrop.get(info).ifPresent((stack) -> {
                drops.accept(new SimpleDrop(topDamager, stack));
            });
        }

        for (var sliceEntry : slicedDrops) {
            SliceInfo sliceInfo = new SliceInfo(info, sliceEntry.getValue(info));

            for (Player player : info.getDamagers()) {
                sliceEntry.accept(new PerformanceSlicedDropInfo(sliceInfo, player), (itemStack) -> {
                    drops.accept(new SimpleDrop(player, itemStack));
                });
            }
        }
    }

    protected static class SliceInfo {
        public final PerformanceKillInfo killInfo;
        public final int points;

        public SliceInfo(PerformanceKillInfo killInfo, int points) {
            this.killInfo = killInfo;
            this.points = points;
        }
    }

    private static class SlicedValueEntry<T, C> {
        private final Function<T, Integer> pointSupplier;
        private final BiConsumer<C, Consumer<ItemStack>> consumer;

        SlicedValueEntry(Function<T, Integer> pointSupplier,
                         BiConsumer<C, Consumer<ItemStack>> consumer) {
            this.pointSupplier = pointSupplier;
            this.consumer = consumer;
        }

        public int getValue(T info) {
            return pointSupplier.apply(info);
        }

        public void accept(C dropInfo, Consumer<ItemStack> consumer) {
            this.consumer.accept(dropInfo, consumer);
        }
    }
}
