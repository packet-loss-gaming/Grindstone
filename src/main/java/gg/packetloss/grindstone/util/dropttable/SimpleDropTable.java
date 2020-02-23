package gg.packetloss.grindstone.util.dropttable;

import gg.packetloss.grindstone.util.ChanceUtil;
import org.apache.logging.log4j.util.TriConsumer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class SimpleDropTable implements DropTable {
    private List<ChanceEntry> entries = new ArrayList<>();
    private List<BiConsumer<Integer, Consumer<ItemStack>>> customEntries = new ArrayList<>();
    private List<ChanceEntry> playerEntries = new ArrayList<>();
    private List<TriConsumer<Player, Integer, Consumer<ItemStack>>> customPlayerEntries = new ArrayList<>();

    public void registerDrop(Supplier<ItemStack> supplier) {
        registerDrop(1, supplier);
    }

    public void registerDrop(int chance, Supplier<ItemStack> supplier) {
        entries.add(new ChanceEntry(chance, supplier));
    }

    public void registerCustomDrop(BiConsumer<Integer, Consumer<ItemStack>> consumer) {
        customEntries.add(consumer);
    }

    public void registerCustomDrop(Consumer<Consumer<ItemStack>> consumer) {
        registerCustomDrop((ignored, innerConsumer) -> consumer.accept(innerConsumer));
    }

    public void registerPlayerDrop(Supplier<ItemStack> supplier) {
        registerPlayerDrop(1, supplier);
    }

    public void registerPlayerDrop(int chance, Supplier<ItemStack> supplier) {
        playerEntries.add(new ChanceEntry(chance, supplier));
    }

    public void registerCustomPlayerDrop(TriConsumer<Player, Integer, Consumer<ItemStack>> consumer) {
        customPlayerEntries.add(consumer);
    }

    public void registerCustomPlayerDrop(BiConsumer<Player, Consumer<ItemStack>> consumer) {
        registerCustomPlayerDrop((player, ignored, innerConsumer) -> consumer.accept(player, innerConsumer));
    }

    @Override
    public void getDrops(KillInfo info, Consumer<Drop> drops) {
        int globalChanceModifier = info.getGlobalChanceModifier();

        for (ChanceEntry entry : entries) {
            if (ChanceUtil.getChance(entry.getChance() / globalChanceModifier)) {
                drops.accept(new SimpleDrop(entry.get()));
            }
        }

        for (var entry : customEntries) {
            entry.accept(globalChanceModifier, itemStack -> {
                drops.accept(new SimpleDrop(itemStack));
            });
        }

        for (Player player : info.getPlayers()) {
            int playerChanceModifier = info.getChanceModifier(player);
            for (ChanceEntry entry : playerEntries) {
                if (ChanceUtil.getChance(entry.getChance() / playerChanceModifier)) {
                    drops.accept(new SimpleDrop(player, entry.get()));
                }
            }

            for (var entry : customPlayerEntries) {
                entry.accept(player, playerChanceModifier, (stack) -> {
                    drops.accept(new SimpleDrop(player, stack));
                });
            }
        }
    }

    private static class ChanceEntry {
        private final int chance;
        private final Supplier<ItemStack> supplier;

        private ChanceEntry(int chance, Supplier<ItemStack> supplier) {
            this.chance = chance;
            this.supplier = supplier;
        }

        public int getChance() {
            return chance;
        }

        public ItemStack get() {
            return supplier.get();
        }
    }
}
