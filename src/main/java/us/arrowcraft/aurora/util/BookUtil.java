package us.arrowcraft.aurora.util;

import com.sk89q.worldedit.blocks.ItemID;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.util.ArrayList;
import java.util.List;

public class BookUtil {

    public static class Rules {

        public static class BuildingCode {

            public static ItemStack carpeDiem() {

                List<String> rules = new ArrayList<>();
                rules.add("There are no rules!");

                return generateRuleBook("Carpe Diem", rules);
            }

            public static ItemStack glaciesMare() {

                List<String> rules = new ArrayList<>();

                return generateRuleBook("Glacies Mare", rules);
            }

            public static ItemStack obiluts() {

                List<String> rules = new ArrayList<>();
                rules.add("Building height cannot exceed layer 96");
                rules.add("Admin have the right to remove unsightly blemishes from houses");
                rules.add("Alteration of the building's exterior will be subject of approval by admin");
                rules.add("Infringement upon the street by the player or his/her belongings is forbidden");
                rules.add("Any redstone or craftbook devices are not to expand over the players plot boundaries");
                rules.add("Gardens/farms should be kept out of sight unless it is permitted by admin");
                rules.add("No single column towers");
                rules.add("Symbols should not be visible from the external locations unless it is permitted by admin");

                return generateRuleBook("Oblitus", rules);
            }

            public static ItemStack vineam() {

                List<String> rules = new ArrayList<>();
                rules.add("Building height cannot exceed layer 96");
                rules.add("Admin have the right to remove unsightly blemishes from houses");
                rules.add("Alteration of the building's exterior will be subject of approval by admin");
                rules.add("Infringement upon the street by the player or his/her belongings is forbidden");
                rules.add("House interiors should be up kept and nice");
                rules.add("Houses cannot be combined unless approved by admin");
                rules.add("Shops are not to be kept within the housing district");
                rules.add("Any redstone or craftbook devices are not to expand over the players plot boundaries");
                rules.add("Gardens/farms should be kept out of sight unless it is permitted by admin");
                rules.add("No single column towers");
                rules.add("Symbols should not be visible from the external locations unless it is permitted by admin");

                return generateRuleBook("Vineam", rules);
            }

            private static ItemStack generateRuleBook(String district, List<String> text) {

                ItemStack ruleBook = new ItemStack(ItemID.WRITTEN_BOOK);
                BookMeta ruleBookMeta = (BookMeta) ruleBook.getItemMeta();

                ruleBookMeta.setTitle(district + " Building Code");
                ruleBookMeta.setAuthor("Admin Council");

                for (int i = 0; i < text.size(); i++) {

                    ruleBookMeta.addPage("Rule #" + (i + 1) + "\n\n" + text.get(i));
                }

                ruleBook.setItemMeta(ruleBookMeta);

                return ruleBook;
            }
        }
    }
}
