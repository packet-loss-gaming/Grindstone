/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.util.item;

import com.sk89q.worldedit.blocks.ItemID;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.util.ArrayList;
import java.util.List;

public class BookUtil {

    public static class General {

        private static ItemStack generateBook(String title, String author, List<String> text) {

            ItemStack ruleBook = new ItemStack(ItemID.WRITTEN_BOOK);
            BookMeta ruleBookMeta = (BookMeta) ruleBook.getItemMeta();

            ruleBookMeta.setTitle(title);
            ruleBookMeta.setAuthor(author);
            ruleBookMeta.addPage(text.toArray(new String[text.size()]));

            ruleBook.setItemMeta(ruleBookMeta);

            return ruleBook;
        }
    }

    public static class Help {

        public static class Admin {

            public static ItemStack housing() {

                List<String> guide = new ArrayList<>();
                guide.add("Index\n\n1.) Creating\n2.) Moving\n3.) Removing");

                // Creating
                guide.add("Creating a House\n\nTo to create the house do the following." +
                        "\n1.) Select the region for the house" +
                        "\n2.) Run the command /home admin create [player] <district[-district]>" +
                        "\n    If a player is not specified it will be purchasable" +
                        "\n3.) Done!");

                // Moving
                guide.add("Moving a House\n\nTo to move the house do the following." +
                        "\n1.) Select the region where the house is going to be" +
                        "\n2.) Run the command /home admin move <player> <new district[-district]>" +
                        "\n3.) Done!");

                // Removing
                guide.add("Removing a House\n\nTo to remove the house do the following." +
                        "\n1.) Run the command /home admin remove <player>" +
                        "\n2.) Done!");

                return General.generateBook("Admin Housing Guide", "Dark_Arc", guide);
            }
        }
    }

    public static class Tutorial {

        public static ItemStack newbieBook() {

            List<String> guide = new ArrayList<>();
            guide.add(
                    "Registering Your Account" +
                            "\nTo take full advantage of the server you must register your account." +
                            " This is a fairly strait forward process, and requires a very simple application." +
                            " To complete the application, go to skelril.com, and click the creeper."
            );

            guide.add(
                    "Buying A House" +
                            "\nThe system has been made fully automatic and is very easy to use." +
                            " To purchase a house all you must do is find a house for sale, stand" +
                            " in its lawn, and then type /home buy. More instructions will be" +
                            " given after the command is used about how to confirm your purchase."
            );

            return General.generateBook("Newbie Book", "Admin Council", guide);
        }
    }

    public static class Rules {

        public static class BuildingCode {

            public static ItemStack server() {

                List<String> rules = new ArrayList<>();
                rules.add("You may not defile trees or landscaping in the City");
                rules.add("Admin have the right to remove unsightly blemishes from houses");

                return generateRuleBook("Global", rules);
            }

            public static ItemStack carpeDiem() {

                List<String> rules = new ArrayList<>();
                rules.add("There are no rules!");

                return generateRuleBook("Carpe Diem", rules);
            }

            public static ItemStack glaciesMare() {

                List<String> rules = new ArrayList<>();
                rules.add("Building height cannot exceed layer 96");
                rules.add("Alteration of the building's exterior will be subject of approval by admin");
                rules.add("Infringement upon the street by the player or his/her belongings is forbidden");
                rules.add("Any redstone or craftbook devices are not to expand over the players plot boundaries");
                rules.add("Gardens/farms should be kept out of sight unless it is permitted by admin");
                rules.add("No single column towers");
                rules.add("Symbols should not be visible from the external locations unless it is permitted by admin");

                return generateRuleBook("Glacies Mare", rules);
            }

            public static ItemStack obiluts() {

                List<String> rules = new ArrayList<>();
                rules.add("Building height cannot exceed layer 96");
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

    public static class Lore {

        public static class Monsters {

            public static ItemStack skelril() {

                List<String> pages = new ArrayList<>();
                pages.add("My brothers, after many days of observation, " +
                        "I feel we are nearly ready to fight this monsters. Dave suggested we push it back " +
                        "and have Merlin prevent its onslaught on the city with a blessed barrier and sleeping hex.");
                pages.add("As it turns out the plan worked! I however, feel the spell will not last forever. " +
                        "Due to my fear that the beast may once again awaken I will document its attacks to " +
                        "help any future generations should it awaken!");

                pages.add("Deviant Wrath\n\nThe monster appears to shout \"Taste my wrath\" " +
                        "prior to unleashing an attack which ignites and throws it enemies.");

                pages.add("Corrupting Toxin\n\nThe monster appears to shout \"Embrace my corruption!\" " +
                        "prior to using an extremely painful and corrupting attack. " +
                        "Perhaps it has relations to the legendary Wither?");

                pages.add("Blinding Fog\n\nThe monster appears to shout \"Are you BLIND? Mwhahahaha!\" " +
                        "prior to blinding all fighters.");

                pages.add("Dancing David (Your welcome Dave)\n\nThe monster appears to have a strange " +
                        "obsession with dancing. Dave seems quite fond of this attack, hiding behind " +
                        "the pillars in the arena should help.");

                pages.add("Unholy healing\n\nThe monster seems to occasionally embrace divine abilities " +
                        "and is healed by our weapons.");

                pages.add("Inferno\n\nI hope that if you are reading this you at least " +
                        "have the capacity to interpret this one...");

                pages.add("Ghastly Wrath\n\nAvoid this attack at all cost. This attack has cost us many lives. " +
                        "Hiding behind pillars seems to help with the survival rate however.");

                pages.add("Unholy Prayer\n\nThe monster appears to call out to it's own demonic lord " +
                        "prior to unleashing an attack, which is so unholy I cannot even begin to describe it. " +
                        "When faced with this attack, hiding behind the pillars may be the only way to survive.");

                pages.add("Also it may be of importance that you know of this monsters origins. " +
                        "It appears to have been created by the Dark King" + ChatColor.ITALIC +
                        " ...the rest of the pages have been torn out of the book and the " +
                        "binding glows a demonic red...");

                return General.generateBook("Tattered Book", "The Forge Knights", pages);
            }
        }

        public static class Areas {

            public static ItemStack theGreatMine() {

                List<String> pages = new ArrayList<>();
                pages.add("It was a dark and stormy night. The rain was falling down as Lord Milias " +
                        "ordered the construction of The Great Mine. Merlin the greatest wizard " +
                        "alive at the time and Dave a legendary smith were ordered to construct the mine.");
                pages.add("A plot was chosen next to the castle where the mine would be created. " +
                        "Dave dug out the mine with other miner's working with him. They found" +
                        " suitable deposits of ores and then had Merlin use a very powerful spell to" +
                        " make the ores replenish.");
                pages.add("Lord Milias thought the mine would bring great riches and influence to the " +
                        "kingdom and opened it without a second thought. For years The Great Mine brought " +
                        "many miner's and gave the district a huge sum of power.");
                pages.add("Then... One night a priest appeared from the fog warning Dave and Merlin that they " +
                        "must convince Lord Milias by the hour of midnight to decomission the mine or a great " +
                        "horror would fall over the land.");
                pages.add("King Milias ignored this warning and although at first nothing really happened " +
                        "within a short time everyone involved with the creation of the mine including" +
                        " Merlin and Dave died.");
                pages.add("Some say their very souls are trapped in it trying to find a way out. Lord Milias" +
                        " disappeared shortly after never to be seen again.");
                pages.add("Many great philosophers believe Milias was captured and killed by the Dark King. " +
                        "Whether that is the truth of not, well we may never know.");
                pages.add("The virus that now controls The Great Mine began to spread in the days " +
                        "after the incident.");
                pages.add("I leave you with a warning, under no circumstance must the seals put in" +
                        " place around the \"Cursed Mine\" as it is now known be broken.");
                pages.add(ChatColor.ITALIC + "...there appears to be a strange marking on this page...");

                return General.generateBook("The Great Mine", "The Forge Knights", pages);
            }
        }
    }
}
