package gg.packetloss.grindstone.items.migration;

import com.google.common.collect.Sets;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class MigrationManager {
    private List<Migration> migrations = new ArrayList<>();

    public void add(Migration migration) {
        migrations.add(migration);
    }

    public Set<String> getUpgradeOptions(ItemStack item) {
        for (Migration migration : migrations) {
            if (migration.test(item)) {
                return migration.getValidOptions();
            }
        }
        return Sets.newHashSet();
    }

    public Optional<ItemStack> upgradeItem(ItemStack item, String option) {
        for (Migration migration : migrations) {
            if (migration.test(item)) {
                if (migration.getValidOptions().contains(option.toLowerCase())) {
                    return Optional.of(migration.apply(item, option));
                }
                return Optional.empty();
            }
        }
        return Optional.empty();
    }
}
