/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util.dropttable;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class MassBossDropTable implements DropTable<MassBossKillInfo> {
    private List<ChanceEntry<MassBossKillInfo>> entries = new ArrayList<>();
    private List<BiConsumer<MassBossKillInfo, Consumer<ItemStack>>> customEntries = new ArrayList<>();
    private List<ChanceEntry<MassBossPlayerKillInfo>> playerEntries = new ArrayList<>();
    private List<BiConsumer<MassBossPlayerKillInfo, Consumer<ItemStack>>> customPlayerEntries = new ArrayList<>();

    public void registerDrop(Supplier<ItemStack> supplier) {
        registerDrop((info) -> 1, supplier);
    }

    public void registerDrop(int chance, Supplier<ItemStack> supplier) {
        registerDrop((info) -> chance, supplier);
    }

    public void registerDrop(Function<MassBossKillInfo, Integer> pipeline, Supplier<ItemStack> supplier) {
        entries.add(new ChanceEntry<>(pipeline, supplier));
    }

    public void registerCustomDrop(BiConsumer<MassBossKillInfo, Consumer<ItemStack>> consumer) {
        customEntries.add(consumer);
    }

    public void registerPlayerDrop(Supplier<ItemStack> supplier) {
        registerPlayerDrop((info) -> 1, supplier);
    }

    public void registerPlayerDrop(int chance, Supplier<ItemStack> supplier) {
        registerPlayerDrop((info) -> chance, supplier);
    }

    public void registerPlayerDrop(Function<MassBossPlayerKillInfo, Integer> chance, Supplier<ItemStack> supplier) {
        playerEntries.add(new ChanceEntry<>(chance, supplier));
    }

    public void registerCustomPlayerDrop(BiConsumer<MassBossPlayerKillInfo, Consumer<ItemStack>> consumer) {
        customPlayerEntries.add(consumer);
    }

    @Override
    public void getDrops(MassBossKillInfo info, Consumer<Drop> drops) {
        for (var entry : entries) {
            entry.get(info).ifPresent((stack) -> {
                drops.accept(new SimpleDrop(stack));
            });
        }

        for (var entry : customEntries) {
            entry.accept(info, itemStack -> {
                drops.accept(new SimpleDrop(itemStack));
            });
        }

        for (Player player : info.getPlayers()) {
            var killInfo = new MassBossPlayerKillInfo(info, player);

            for (var entry : playerEntries) {
                entry.get(killInfo).ifPresent((stack) -> {
                    drops.accept(new SimpleDrop(player, stack));
                });
            }

            for (var entry : customPlayerEntries) {
                entry.accept(killInfo, (stack) -> {
                    drops.accept(new SimpleDrop(player, stack));
                });
            }
        }
    }
}
