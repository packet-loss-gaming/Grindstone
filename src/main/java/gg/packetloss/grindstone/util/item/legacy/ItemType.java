/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package gg.packetloss.grindstone.util.item.legacy;

import com.sk89q.util.StringUtil;
import gg.packetloss.grindstone.util.item.BaseItem;

import java.util.*;

/**
 * ItemType types w/ damage value support
 */
public enum ItemType {
    // Blocks
    AIR(BlockID.AIR, "Air", "air"),
    STONE(BlockID.STONE, "Stone", "stone", "rock"),
    GRASS(BlockID.GRASS, "Grass", "grass"),
    DIRT(BlockID.DIRT, 0, "Dirt", "dirt"),
    PODZOL(BlockID.DIRT, 2, "Podzol", "podzol"),
    COBBLESTONE(BlockID.COBBLESTONE, "Cobblestone", "cobblestone", "cobble"),
    WOOD(BlockID.WOOD, 0, "Oak Wood", "oakwood", "wood", "oakwoodplank", "oakwoodplanks", "oakplanks"),
    SPRUCE_WOOD(BlockID.WOOD, 1, "Spruce Wood", "sprucewood", "sprucewoodplank", "sprucewoodplanks", "spruceplanks"),
    BIRCH_WOOD(BlockID.WOOD, 2, "Birch Wood", "birchwood", "bitchwoodplank", "birchwoodplanks", "birchplanks"),
    JUNGLE_WOOD(BlockID.WOOD, 3, "Jungle Wood", "junglewood", "junglewoodplank", "junglewoodplanks", "jungleplanks"),
    ACACIA_WOOD(BlockID.WOOD, 4, "Acacia Wood", "acaciawood", "acaciawoodplank", "acaciawoodplanks", "acaciaplanks"),
    DARK_OAK_WOOD(BlockID.WOOD, 5, "Dark Oak Wood", "darkoakwood", "darkoakwoodplank", "darkoakwoodplanks", "darkoakplanks"),
    SAPLING(BlockID.SAPLING, 0, "Oak Sapling", "sapling", "oaksapling", "oakseedling"),
    SPRUCE_SAPLING(BlockID.SAPLING, 1, "Spruce Sapling", "sprucesapling", "spruceseedling"),
    BIRCH_SAPLING(BlockID.SAPLING, 2, "Birch Sapling", "birchsapling", "birchseedling"),
    JUNGLE_SAPLING(BlockID.SAPLING, 3, "Jungle Sapling", "junglesapling", "jungleseedling"),
    ACACIA_SAPLING(BlockID.SAPLING, 4, "Acacia Sapling", "acaciasapling", "acaciaseedling"),
    DARK_OAK_SAPLING(BlockID.SAPLING, 5, "Dark Oak Sapling", "darkoaksapling", "darkoakseedling"),
    BEDROCK(BlockID.BEDROCK, "Bedrock", "adminium", "bedrock"),
    WATER(BlockID.WATER, "Water", "watermoving", "movingwater", "flowingwater", "waterflowing"),
    STATIONARY_WATER(BlockID.STATIONARY_WATER, "Water (stationary)", "water", "waterstationary", "stationarywater", "stillwater"),
    LAVA(BlockID.LAVA, "Lava", "lavamoving", "movinglava", "flowinglava", "lavaflowing"),
    STATIONARY_LAVA(BlockID.STATIONARY_LAVA, "Lava (stationary)", "lava", "lavastationary", "stationarylava", "stilllava"),
    SAND(BlockID.SAND, 0, "Sand", "sand"),
    RED_SAND(BlockID.SAND, 1, "Red Sand", "redsand"),
    GRAVEL(BlockID.GRAVEL, "Gravel", "gravel"),
    GOLD_ORE(BlockID.GOLD_ORE, "Gold ore", "goldore"),
    IRON_ORE(BlockID.IRON_ORE, "Iron ore", "ironore"),
    COAL_ORE(BlockID.COAL_ORE, "Coal ore", "coalore"),
    LOG(BlockID.LOG, 0, "Oak Log", "log", "oaklog", "oaklogs"),
    SPRUCE_LOG(BlockID.LOG, 1, "Spruce Log", "sprucelog", "sprucelogs"),
    BIRCH_LOG(BlockID.LOG, 2, "Birch Log", "birchlog", "birchlogs"),
    JUNGLE_LOG(BlockID.LOG, 3, "Jungle Log", "junglelog", "junglelogs"),
    LEAVES(BlockID.LEAVES, 0, "Oak Leaves", "oakleaves"),
    SPRUCE_LEAVES(BlockID.LEAVES, 1, "Spruce Leaves", "spruceleaves"),
    BIRCH_LEAVES(BlockID.LEAVES, 2, "Birch Leaves", "birchleaves"),
    JUNGLE_LEAVES(BlockID.LEAVES, 3, "Jungle Leaves", "jungleleaves"),
    SPONGE(BlockID.SPONGE, "Sponge", "sponge"),
    GLASS(BlockID.GLASS, "Glass", "glass"),
    LAPIS_LAZULI_ORE(BlockID.LAPIS_LAZULI_ORE, "Lapis lazuli ore", "lapislazuliore", "blueore", "lapisore"),
    LAPIS_LAZULI_BLOCK(BlockID.LAPIS_LAZULI_BLOCK, "Lapis lazuli block", "lapislazuliblock", "bluerock"),
    DISPENSER(BlockID.DISPENSER, "Dispenser", "dispenser"),
    SANDSTONE(BlockID.SANDSTONE, 0, "Sandstone", "sandstone"),
    SANDSTONE_CHISELED(BlockID.SANDSTONE, 1, "Chiseled Sandstone", "chiseledsandstone"),
    SANDSTONE_SMOOTH(BlockID.SANDSTONE, 2, "Smooth Sandstone", "smoothsandstone"),
    NOTE_BLOCK(BlockID.NOTE_BLOCK, "Note block", "musicblock", "noteblock", "note", "music", "instrument"),
    BED(BlockID.BED, "Bed", "bed"),
    POWERED_RAIL(BlockID.POWERED_RAIL, "Powered Rail", "poweredrail", "boosterrail", "poweredtrack", "boostertrack", "booster"),
    DETECTOR_RAIL(BlockID.DETECTOR_RAIL, "Detector Rail", "detectorrail", "detector"),
    PISTON_STICKY_BASE(BlockID.PISTON_STICKY_BASE, "Sticky Piston", "stickypiston"),
    WEB(BlockID.WEB, "Web", "web", "spiderweb"),
    LONG_GRASS(BlockID.LONG_GRASS, 0, "Long grass", "longgrass", "tallgrass"),
    DEAD_BUSH(BlockID.DEAD_BUSH, "Shrub", "deadbush", "shrub", "deadshrub", "tumbleweed"),
    PISTON_BASE(BlockID.PISTON_BASE, "Piston", "piston"),
    PISTON_EXTENSION(BlockID.PISTON_EXTENSION, "Piston extension", "pistonextension", "pistonhead"),
    CLOTH(BlockID.CLOTH, 0, "Wool", "cloth", "wool"),
    PISTON_MOVING_PIECE(BlockID.PISTON_MOVING_PIECE, "Piston moving piece", "pistonmovingpiece", "movingpiston"),
    DANDELION(BlockID.YELLOW_FLOWER, "Dandelion", "dandelion"),
    POPPY(BlockID.RED_FLOWER, 0, "Poppy", "poppy"),
    BLUE_ORCHID(BlockID.RED_FLOWER, 1, "Blue Orchid", "blueorchid"),
    ALLIUM(BlockID.RED_FLOWER, 2, "Allium", "allium"),
    AZURE_BLUET(BlockID.RED_FLOWER, 3, "Azure Bluet", "azurebluet"),
    RED_TULIP(BlockID.RED_FLOWER, 4, "Red Tulip", "redtulip"),
    ORANGE_TULIP(BlockID.RED_FLOWER, 5, "Orange Tulip", "orangetulip"),
    WHITE_TULIP(BlockID.RED_FLOWER, 6, "White Tulip", "whitetulip"),
    PINK_TULIP(BlockID.RED_FLOWER, 7, "Pink Tulip", "pinktulip"),
    OXEYE_DAISY(BlockID.RED_FLOWER, 8, "Oxeye Daisy", "oxeyedaisy"),
    BROWN_MUSHROOM(BlockID.BROWN_MUSHROOM, "Brown mushroom", "brownmushroom", "mushroom"),
    RED_MUSHROOM(BlockID.RED_MUSHROOM, "Red mushroom", "redmushroom"),
    GOLD_BLOCK(BlockID.GOLD_BLOCK, "Gold block", "gold", "goldblock"),
    IRON_BLOCK(BlockID.IRON_BLOCK, "Iron block", "iron", "ironblock"),
    DOUBLE_STEP(BlockID.DOUBLE_STEP, 0, "Double step", "doubleslab", "doublestoneslab", "doublestep"),
    SLAB(BlockID.STEP, 0, "Stone slab", "slab", "stoneslab", "step", "halfstep"),
    SANDSTONE_SLAB(BlockID.STEP, 1, "Sandstone slab", "sandstoneslab", "sandstonestep"),
    // WOODEN_SLAB(BlockID.STEP, 2, "Wooden slab", "woodslab", "woodstep"),
    COBBLESTONE_SLAB(BlockID.STEP, 3, "Cobblestone slab", "cobblestoneslab", "cobbleslab", "cobblestep"),
    BRICK_SLAB(BlockID.STEP, 4, "Brick slab", "brickslab", "brickstep"),
    STONE_BRICK_SLAB(BlockID.STEP, 5, "Stone brick slab", "stonebrickslab", "stonebrickstep"),
    NETHER_BRICK_SLAB(BlockID.STEP, 6, "Nether brick slab", "netherbrickslab", "netherbrickstep"),
    QUARTZ_SLAB(BlockID.STEP, 7, "Quartz slab", "quartzslab", "quartzstep"),
    BRICK(BlockID.BRICK, "Brick block", "brickblock"),
    TNT(BlockID.TNT, "TNT", "tnt", "c4", "explosive"),
    BOOKCASE(BlockID.BOOKCASE, "Bookcase", "bookshelf", "bookshelves", "bookcase", "bookcases"),
    MOSSY_COBBLESTONE(BlockID.MOSSY_COBBLESTONE, "Mossy Cobblestone", "mossycobblestone", "mossstone", "mossystone", "mosscobble", "mossycobble", "moss", "mossy", "sossymobblecone"),
    OBSIDIAN(BlockID.OBSIDIAN, "Obsidian", "obsidian"),
    TORCH(BlockID.TORCH, "Torch", "torch", "light", "candle"),
    FIRE(BlockID.FIRE, "Fire", "fire", "flame", "flames"),
    MOB_SPAWNER(BlockID.MOB_SPAWNER, "Mob spawner", "mobspawner", "spawner"),
    WOODEN_STAIRS(BlockID.OAK_WOOD_STAIRS, "Oak Wood stairs", "oakstairs", "oakwoodstairs"),
    CHEST(BlockID.CHEST, "Chest", "chest", "storage", "storagechest"),
    REDSTONE_WIRE(BlockID.REDSTONE_WIRE, "Redstone wire", "redstonewire", "redstone", "redstoneblock"),
    DIAMOND_ORE(BlockID.DIAMOND_ORE, "Diamond ore", "diamondore"),
    DIAMOND_BLOCK(BlockID.DIAMOND_BLOCK, "Diamond block", "diamond", "diamondblock"),
    WORKBENCH(BlockID.WORKBENCH, "Workbench", "workbench", "table", "craftingtable", "crafting"),
    CROPS(BlockID.CROPS, "Crops", "crops", "crop", "plant", "plants"),
    SOIL(BlockID.SOIL, "Soil", "soil", "farmland"),
    FURNACE(BlockID.FURNACE, "Furnace", "furnace"),
    BURNING_FURNACE(BlockID.BURNING_FURNACE, "Furnace (burning)", "furnaceburning", "burningfurnace", "litfurnace"),
    SIGN_POST(BlockID.SIGN_POST, "Sign post", "signpost"),
    // WOODEN_DOOR(BlockID.WOODEN_DOOR, "Wooden door", "wooddoor", "woodendoor", "door"),
    LADDER(BlockID.LADDER, "Ladder", "ladder"),
    MINECART_TRACKS(BlockID.MINECART_TRACKS, "Minecart tracks", "track", "tracks", "minecrattrack", "minecarttracks", "rails", "rail"),
    COBBLESTONE_STAIRS(BlockID.COBBLESTONE_STAIRS, "Cobblestone stairs", "cobblestonestair", "cobblestonestairs", "cobblestair", "cobblestairs"),
    WALL_SIGN(BlockID.WALL_SIGN, "Wall sign", "wallsign"),
    LEVER(BlockID.LEVER, "Lever", "lever", "switch", "stonelever", "stoneswitch"),
    STONE_PRESSURE_PLATE(BlockID.STONE_PRESSURE_PLATE, "Stone pressure plate", "stonepressureplate", "stoneplate"),
    // IRON_DOOR(BlockID.IRON_DOOR, "Iron Door", "irondoor"),
    WOODEN_PRESSURE_PLATE(BlockID.WOODEN_PRESSURE_PLATE, "Wooden pressure plate", "woodpressureplate", "woodplate", "woodenpressureplate", "woodenplate", "plate", "pressureplate"),
    REDSTONE_ORE(BlockID.REDSTONE_ORE, "Redstone ore", "redstoneore"),
    GLOWING_REDSTONE_ORE(BlockID.GLOWING_REDSTONE_ORE, "Glowing redstone ore", "glowingredstoneore"),
    REDSTONE_TORCH_OFF(BlockID.REDSTONE_TORCH_OFF, "Redstone torch (off)", "redstonetorchoff", "rstorchoff"),
    REDSTONE_TORCH_ON(BlockID.REDSTONE_TORCH_ON, "Redstone torch", "redstonetorch", "redstonetorchon", "rstorchon", "redtorch"),
    STONE_BUTTON(BlockID.STONE_BUTTON, "Stone Button", "stonebutton", "button"),
    SNOW(BlockID.SNOW, "Snow", "snow"),
    ICE(BlockID.ICE, "Ice", "ice"),
    SNOW_BLOCK(BlockID.SNOW_BLOCK, "Snow block", "snowblock"),
    CACTUS(BlockID.CACTUS, "Cactus", "cactus", "cacti"),
    CLAY(BlockID.CLAY, "Clay block", "clayblock"),
    SUGAR_CANE(BlockID.REED, "Sugar Cane", "reed", "cane", "sugarcane", "sugarcanes", "vine", "vines"),
    JUKEBOX(BlockID.JUKEBOX, "Jukebox", "jukebox", "stereo", "recordplayer"),
    FENCE(BlockID.FENCE, "Fence", "fence"),
    PUMPKIN(BlockID.PUMPKIN, "Pumpkin", "pumpkin"),
    NETHERRACK(BlockID.NETHERRACK, "Netherrack", "redmossycobblestone", "redcobblestone", "redmosstone", "redcobble", "netherstone", "netherrack", "nether", "hellstone"),
    SOUL_SAND(BlockID.SLOW_SAND, "Soul sand", "slowmud", "mud", "soulsand", "hellmud"),
    GLOWSTONE(BlockID.LIGHTSTONE, "Glowstone", "brittlegold", "glowstone", "lightstone", "brimstone", "australium"),
    PORTAL(BlockID.PORTAL, "Portal", "portal"),
    JACK_O_LANTERN(BlockID.JACKOLANTERN, "Jack o' Lantern", "pumpkinlighted", "pumpkinon", "litpumpkin", "jackolantern"),
    CAKE(BlockID.CAKE_BLOCK, "Cake", "cake", "cakeblock"),
    REDSTONE_REPEATER_OFF(BlockID.REDSTONE_REPEATER_OFF, "Redstone repeater", "diodeoff", "redstonerepeater", "repeateroff", "delayeroff"),
    REDSTONE_REPEATER_ON(BlockID.REDSTONE_REPEATER_ON, "Redstone repeater (on)", "diodeon", "redstonerepeateron", "repeateron", "delayeron"),
    STAINED_GLASS(BlockID.STAINED_GLASS, 0, "White Stained Glass", "whitestainedglass"),
    TRAP_DOOR(BlockID.TRAP_DOOR, "Trap door", "trapdoor", "hatch", "floordoor"),
    SILVERFISH_BLOCK(BlockID.SILVERFISH_BLOCK, 0, "Silverfish block", "silverfishblock", "silverfish", "silver"),
    COBBLE_SILVERFISH_BLOCK(BlockID.SILVERFISH_BLOCK, 1, "Cobblestone Silverfish block", "cobblestonesilverfish", "cobblesilver"),
    STONE_BRICK_SILVER_FISH(BlockID.SILVERFISH_BLOCK, 1, "Stone brick Silverfish block", "stonebricksilverfish", "stonebricksilver"),
    STONE_BRICK(BlockID.STONE_BRICK, 0, "Stone brick", "stonebrick", "sbrick", "smoothstonebrick"),
    MOSSY_STONE_BRICK(BlockID.STONE_BRICK, 1, "Mossy Stone brick", "mossystonebrick", "msbrick"),
    CRACKED_STONE_BRICK(BlockID.STONE_BRICK, 2, "Cracked Stone brick", "crackedstonebrick", "csbrick"),
    RED_MUSHROOM_CAP(BlockID.RED_MUSHROOM_CAP, 0, "Red mushroom cap", "giantmushroomred", "redgiantmushroom", "redmushroomcap"),
    BROWN_MUSHROOM_CAP(BlockID.BROWN_MUSHROOM_CAP, 0, "Brown mushroom cap", "giantmushroombrown", "browngiantmushoom", "brownmushroomcap"),
    IRON_BARS(BlockID.IRON_BARS, "Iron bars", "ironbars", "ironfence"),
    GLASS_PANE(BlockID.GLASS_PANE, "Glass pane", "window", "glasspane", "glasswindow"),
    MELON_BLOCK(BlockID.MELON_BLOCK, "Melon (block)", "melonblock"),
    PUMPKIN_STEM(BlockID.PUMPKIN_STEM, "Pumpkin stem", "pumpkinstem"),
    MELON_STEM(BlockID.MELON_STEM, "Melon stem", "melonstem"),
    VINE(BlockID.VINE, "Vine", "vine", "vines", "creepers"),
    FENCE_GATE(BlockID.FENCE_GATE, "Fence gate", "fencegate", "gate"),
    BRICK_STAIRS(BlockID.BRICK_STAIRS, "Brick stairs", "brickstairs", "bricksteps"),
    STONE_BRICK_STAIRS(BlockID.STONE_BRICK_STAIRS, "Stone brick stairs", "stonebrickstairs", "smoothstonebrickstairs"),
    MYCELIUM(BlockID.MYCELIUM, "Mycelium", "mycelium", "fungus", "mycel"),
    LILY_PAD(BlockID.LILY_PAD, "Lily pad", "lilypad", "waterlily"),
    NETHER_BRICK(BlockID.NETHER_BRICK, "Nether brick block", "netherbrickblock"),
    NETHER_BRICK_FENCE(BlockID.NETHER_BRICK_FENCE, "Nether brick fence", "netherbrickfence", "netherfence"),
    NETHER_BRICK_STAIRS(BlockID.NETHER_BRICK_STAIRS, "Nether brick stairs", "netherbrickstairs", "netherbricksteps", "netherstairs", "nethersteps"),
    NETHER_WART(BlockID.NETHER_WART, "Nether wart", "netherwart", "netherstalk"),
    ENCHANTMENT_TABLE(BlockID.ENCHANTMENT_TABLE, "Enchantment table", "enchantmenttable", "enchanttable"),
    BREWING_STAND(BlockID.BREWING_STAND, "Brewing Stand", "brewingstand"),
    CAULDRON(BlockID.CAULDRON, "Cauldron", "cauldron"),
    END_PORTAL(BlockID.END_PORTAL, "End Portal", "endportal", "blackstuff", "airportal", "weirdblackstuff"),
    END_PORTAL_FRAME(BlockID.END_PORTAL_FRAME, "End Portal Frame", "endportalframe", "airportalframe", "crystalblock"),
    END_STONE(BlockID.END_STONE, "End Stone", "endstone", "enderstone", "endersand"),
    DRAGON_EGG(BlockID.DRAGON_EGG, "Dragon Egg", "dragonegg", "dragons"),
    REDSTONE_LAMP_OFF(BlockID.REDSTONE_LAMP_OFF, "Redstone lamp (off)", "redstonelampoff", "rslampoff", "rsglowoff"),
    REDSTONE_LAMP_ON(BlockID.REDSTONE_LAMP_ON, "Redstone lamp", "redstonelamp", "rslamp", "rslampon", "rsglow", "rsglowon"),
    DOUBLE_WOODEN_STEP(BlockID.DOUBLE_WOODEN_STEP, 0, "Double wood step", "doublewoodslab", "doublewoodstep"),
    WOODEN_STEP(BlockID.WOODEN_STEP, 0, "Oak Wood Slab", "oakwoodslab", "oakwoodstep"),
    SPRUCE_STEP(BlockID.WOODEN_STEP, 1, "Spruce Wood Slab", "sprucewoodslab", "sprucewoodstep"),
    BIRCH_STEP(BlockID.WOODEN_STEP, 2, "Birch Wood Slab", "birchwoodslab", "birchwoodstep"),
    JUNGLE_STEP(BlockID.WOODEN_STEP, 3, "Jungle Wood Slab", "junglewoodslab", "junglewoodstep"),
    COCOA_PLANT(BlockID.COCOA_PLANT, "Cocoa plant", "cocoplant", "cocoaplant"),
    SANDSTONE_STAIRS(BlockID.SANDSTONE_STAIRS, "Sandstone stairs", "sandstairs", "sandstonestairs"),
    EMERALD_ORE(BlockID.EMERALD_ORE, "Emerald ore", "emeraldore"),
    ENDER_CHEST(BlockID.ENDER_CHEST, "Ender chest", "enderchest"),
    TRIPWIRE_HOOK(BlockID.TRIPWIRE_HOOK, "Tripwire hook", "tripwirehook"),
    TRIPWIRE(BlockID.TRIPWIRE, "Tripwire", "tripwire", "string"),
    EMERALD_BLOCK(BlockID.EMERALD_BLOCK, "Emerald block", "emeraldblock", "emerald"),
    SPRUCE_WOOD_STAIRS(BlockID.SPRUCE_WOOD_STAIRS, "Spruce Wood stairs", "sprucestairs", "sprucewoodstairs"),
    BIRCH_WOOD_STAIRS(BlockID.BIRCH_WOOD_STAIRS, "Birch Wood stairs", "birchstairs", "birchwoodstairs"),
    JUNGLE_WOOD_STAIRS(BlockID.JUNGLE_WOOD_STAIRS, "Jungle Wood stairs", "junglestairs", "junglewoodstairs"),
    COMMAND_BLOCK(BlockID.COMMAND_BLOCK, "Command block", "commandblock", "cmdblock", "command", "cmd"),
    BEACON(BlockID.BEACON, "Beacon", "beacon", "beaconblock"),
    COBBLESTONE_WALL(BlockID.COBBLESTONE_WALL, 0, "Cobblestone wall", "cobblestonewall", "cobblewall"),
    MOSSY_COBBLESTONE_WALL(BlockID.COBBLESTONE_WALL, 1, "Mossy Cobblestone wall", "mossycobblestonewall", "mossycobblewall"),
    FLOWER_POT_BLOCK(BlockID.FLOWER_POT, "Flower pot", "flowerpot", "plantpot", "pot", "flowerpotblock"),
    CARROTS_BLOCK(BlockID.CARROTS, "Carrots", "carrots", "carrotsplant", "carrotsblock"),
    POTATOES_BLOCK(BlockID.POTATOES, "Potatoes", "patatoes", "potatoesblock"),
    WOODEN_BUTTON(BlockID.WOODEN_BUTTON, "Wooden button", "woodbutton", "woodenbutton"),
    HEAD_BLOCK(BlockID.HEAD, "Head", "head", "headmount", "mount", "headblock", "mountblock"),
    ANVIL(BlockID.ANVIL, 0, "Anvil", "anvil", "blacksmith"),
    TRAPPED_CHEST(BlockID.TRAPPED_CHEST, "Trapped Chest", "trappedchest", "redstonechest"),
    PRESSURE_PLATE_LIGHT(BlockID.PRESSURE_PLATE_LIGHT, "Weighted Pressure Plate (Light)", "weightedpressureplatelight", "lightpressureplate"),
    PRESSURE_PLATE_HEAVY(BlockID.PRESSURE_PLATE_HEAVY, "Weighted Pressure Plate (Heavy)", "weightedpressureplateheavy", "heavypressureplate"),
    COMPARATOR_OFF(BlockID.COMPARATOR_OFF, "Redstone Comparator (inactive)", "restonecomparatorinactive", "redstonecomparator", "comparator"),
    COMPARATOR_ON(BlockID.COMPARATOR_ON, "Redstone Comparator (active)", "redstonecomparatoractive", "redstonecomparatoron", "comparatoron"),
    DAYLIGHT_SENSOR(BlockID.DAYLIGHT_SENSOR, "Daylight Sensor", "daylightsensor","daylightsesnor", "lightsensor", "daylight sesnor"),
    REDSTONE_BLOCK(BlockID.REDSTONE_BLOCK, "Block of Redstone", "redstoneblock", "blockofredstone"),
    QUARTZ_ORE(BlockID.QUARTZ_ORE, "Nether Quartz Ore", "quartzore", "netherquartzore"),
    HOPPER(BlockID.HOPPER, "Hopper", "hopper"),
    QUARTZ_BLOCK(BlockID.QUARTZ_BLOCK, 0, "Block of Quartz", "blockofquartz", "quartzblock"),
    QUARTZ_STAIRS(BlockID.QUARTZ_STAIRS, "Quartz Stairs", "quartzstairs"),
    ACTIVATOR_RAIL(BlockID.ACTIVATOR_RAIL, "Activator Rail", "activatorrail", "tntrail", "activatortrack"),
    DROPPER(BlockID.DROPPER, "Dropper", "dropper"),
    STAINED_CLAY(BlockID.STAINED_CLAY, 0, "White Stained clay", "whitestainedclay"),
    STAINED_GLASS_PANE(BlockID.STAINED_GLASS_PANE, 0, "White Stained Glass Pane", "whitestainedglasspane"),
    ACACIA_LEAVES(BlockID.LEAVES2, 0, "Acacia Leaves", "acacialeaves"),
    DARK_OAK_LEAVES(BlockID.LEAVES2, 1, "Dark Oak Leaves", "darkoakleaves"),
    ACACIA_LOG(BlockID.LOG2, 0, "Acacia Log", "acacialog", "acacialogs"),
    DARK_OAK_LOG(BlockID.LOG2, 1, "Dark Oak Log", "darkoaklog", "darkoaklogs"),
    ACACIA_STAIRS(BlockID.ACACIA_STAIRS, "Acacia Wood Stairs", "acaciawoodstairs", "acaciastairs"),
    DARK_OAK_STAIRS(BlockID.DARK_OAK_STAIRS, "Dark Oak Wood Stairs", "darkoakwoodstairs", "darkoakstairs"),
    SLIME(BlockID.SLIME, "Slime Block", "slimeblock"),
    BARRIER(BlockID.BARRIER, "Barrier", "barrier", "wall", "worldborder", "edge"),
    IRON_TRAP_DOOR(BlockID.IRON_TRAP_DOOR, "Iron Trap Door", "irontrapdoor"),
    PRISMARINE(BlockID.PRISMARINE, "Prismarine", "prismarine"),
    SEA_LANTERN(BlockID.SEA_LANTERN, "Sea Lantern", "sealantern"),
    HAY_BLOCK(BlockID.HAY_BLOCK, "Hay Block", "hayblock", "haybale", "wheatbale"),
    CARPET(BlockID.CARPET, 0, "Carpet", "carpet"),
    HARDENED_CLAY(BlockID.HARDENED_CLAY, "Hardened Clay", "hardenedclay", "hardclay"),
    COAL_BLOCK(BlockID.COAL_BLOCK, "Block of Coal", "coalblock", "blockofcoal"),
    PACKED_ICE(BlockID.PACKED_ICE, "Packed Ice", "packedice", "hardice"),
    SUN_FLOWER(BlockID.DOUBLE_PLANT, 0, "Sun Flower", "sunflower"),
    LILAC(BlockID.DOUBLE_PLANT, 1, "Lilac", "lilac"),
    DOUBLE_TALL_GRASS(BlockID.DOUBLE_PLANT, 2, "Double Tallgrass", "doubletallgrass"),
    LARGE_FERN(BlockID.DOUBLE_PLANT, 3, "Large Fern", "largefern"),
    ROSE_BUSH(BlockID.DOUBLE_PLANT, 4, "Rose Bush", "rosebush"),
    PEONY(BlockID.DOUBLE_PLANT, 5, "Peony", "peony"),
    STANDING_BANNER(BlockID.STANDING_BANNER, "Standing Banner", "standingbannear", "banner"),
    WALL_BANNER(BlockID.WALL_BANNER, "Wall Banner", "wallbanner"),
    DAYLIGHT_SENSOR_INVERTED(BlockID.DAYLIGHT_SENSOR_INVERTED, "Inverted Daylight Sensor", "inverteddaylight", "inverteddaylightsensor"),
    RED_SANDSTONE(BlockID.RED_SANDSTONE, "Red Sandstone", "redsandstone"),
    RED_SANDSTONE_STAIRS(BlockID.RED_SANDSTONE_STAIRS, "Red Sandstone Stairs", "redsandstonestairs"),
    DOUBLE_STEP2(BlockID.DOUBLE_STEP2, "Double Step 2", "doublestep2", "doubleslab2", "doublestoneslab2", "doublestonestep2"),
    STEP2(BlockID.STEP2, "Step 2", "step2", "slab2", "stonestep2", "stoneslab2"),
    SPRUCE_FENCE_GATE(BlockID.SPRUCE_FENCE_GATE, "Spruce Fence Gate", "spurcefencegate"),
    BIRCH_FENCE_GATE(BlockID.BIRCH_FENCE_GATE, "Birch Fence Gate", "birchfencegate"),
    JUNGLE_FENCE_GATE(BlockID.JUNGLE_FENCE_GATE, "Jungle Fence Gate", "junglefencegate"),
    DARK_OAK_FENCE_GATE(BlockID.DARK_OAK_FENCE_GATE, "Dark Oak Fence Gate", "darkoakfencegate"),
    ACACIA_FENCE_GATE(BlockID.ACACIA_FENCE_GATE, "Acacia Fence Gate", "acaciafencegate"),
    SPRUCE_FENCE(BlockID.SPRUCE_FENCE, "Spruce Fence", "sprucefence"),
    BIRCH_FENCE(BlockID.BIRCH_FENCE, "Birch Fence", "birchfence"),
    JUNGLE_FENCE(BlockID.JUNGLE_FENCE, "Jungle Fence", "junglefence"),
    DARK_OAK_FENCE(BlockID.DARK_OAK_FENCE, "Dark Oak Fence", "darkoakfence"),
    ACACIA_FENCE(BlockID.ACACIA_FENCE, "Acacia Fence", "acaciafence"),
    SPRUCE_DOOR(BlockID.SPRUCE_DOOR, "Spruce Door", "sprucedoor"),
    BIRCH_DOOR(BlockID.BIRCH_DOOR, "Birch Door", "birchdoor"),
    JUNGLE_DOOR(BlockID.JUNGLE_DOOR, "Jungle Door", "jungledoor"),
    ACACIA_DOOR(BlockID.ACACIA_DOOR, "Acacia Door", "acaciadoor"),
    DARK_OAK_DOOR(BlockID.DARK_OAK_DOOR, "Dark Oak Door", "darkoakdoor"),
    END_ROD(BlockID.END_ROD, "End Rod", "endrod", "endtorch"),
    CHORUS_PLANT(BlockID.CHORUS_PLANT, "Chorus Plant", "chorusplant", "chorusstem"),
    CHORUS_FLOWER(BlockID.CHORUS_FLOWER, "Chorus Flower", "chorusflower"),
    PURPUR_BLOCK(BlockID.PURPUR_BLOCK, "Purpur Block", "purpurblock", "blockpurpur"),
    PURPUR_PILLAR(BlockID.PURPUR_PILLAR, "Purpur Pillar", "purpurpillar"),
    PURPUR_STAIRS(BlockID.PURPUR_STAIRS, "Purpur Stairs", "purpurstairs"),
    PURPUR_DOUBLE_SLAB(BlockID.PURPUR_DOUBLE_SLAB, "Purpur Double Slab", "purpurdoubleslab", "doubleslabpurpur", "doublepurpurslab"),
    PURPUR_SLAB(BlockID.PURPUR_SLAB, "Purpur Slab", "purpurslab", "slabpurpur"),
    END_BRICKS(BlockID.END_BRICKS, "End Bricks", "endbricks"),
    BEETROOTS(BlockID.BEETROOTS, "Beetroots", "beetroots", "beetroot_plant"),
    GRASS_PATH(BlockID.GRASS_PATH, "Grass Path", "grasspath", "dirtpath"),
    END_GATEWAY(BlockID.END_GATEWAY, "End Gateway", "endgateway"),
    REPEATING_COMMAND_BLOCK(BlockID.REPEATING_COMMAND_BLOCK, "Repeating Command Block", "repeatingcommandblock", "commandblockrepeating"),
    CHAIN_COMMAND_BLOCK(BlockID.CHAIN_COMMAND_BLOCK, "Chain Command Block", "chaincommandblock", "commandblockchain"),
    FROSTED_ICE(BlockID.FROSTED_ICE, "Frosted Ice", "frostedice", "frostwalkerice"),
    MAGMA_BLOCK(BlockID.MAGMA_BLOCK, "Magma Block", "magmablock", "magma"),
    NETHER_WART_BLOCK(BlockID.NETHER_WART_BLOCK, "Nether Wart Block", "netherwartblock"),
    RED_NETHER_BRICK(BlockID.RED_NETHER_BRICK, "Red Nether Brick", "rednetherbrick", "netherbrickred"),
    BONE_BLOCK(BlockID.BONE_BLOCK, "Bone Block", "boneblock", "blockbone", "fossil", "fossilblock", "blockfossil"),
    STRUCTURE_VOID(BlockID.STRUCTURE_VOID, "Structure Void", "structurevoid", "structureair"),
    OBSERVER(BlockID.OBSERVER, "Observer", "observer", "blockupdatedetector"),
    SHULKER_BOX_WHITE(BlockID.SHULKER_BOX_WHITE, "White Shulker Box", "shulkerboxwhite"),
    SHULKER_BOX_ORANGE(BlockID.SHULKER_BOX_ORANGE, "Orange Shulker Box", "shulkerboxorange"),
    SHULKER_BOX_MAGENTA(BlockID.SHULKER_BOX_MAGENTA, "Magenta Shulker Box", "shulkerboxmagenta"),
    SHULKER_BOX_LIGHT_BLUE(BlockID.SHULKER_BOX_LIGHT_BLUE, "Light Blue Shulker Box", "shulkerboxlightblue"),
    SHULKER_BOX_YELLOW(BlockID.SHULKER_BOX_YELLOW, "Yellow Shulker Box", "shulkerboxyellow"),
    SHULKER_BOX_LIME(BlockID.SHULKER_BOX_LIME, "Lime Shulker Box", "shulkerboxlime"),
    SHULKER_BOX_PINK(BlockID.SHULKER_BOX_PINK, "Pink Shulker Box", "shulkerboxpink"),
    SHULKER_BOX_GRAY(BlockID.SHULKER_BOX_GRAY, "Gray Shulker Box", "shulkerboxgray"),
    SHULKER_BOX_LIGHT_GRAY(BlockID.SHULKER_BOX_LIGHT_GRAY, "Light Gray Shulker Box", "shulkerboxlightgray"),
    SHULKER_BOX_CYAN(BlockID.SHULKER_BOX_CYAN, "Cyan Shulker Box", "shulkerboxcyan"),
    SHULKER_BOX_PURPLE(BlockID.SHULKER_BOX_PURPLE, "Purple Shulker Box", "shulkerboxpurple"),
    SHULKER_BOX_BLUE(BlockID.SHULKER_BOX_BLUE, "Blue Shulker Box", "shulkerboxblue"),
    SHULKER_BOX_BROWN(BlockID.SHULKER_BOX_BROWN, "Brown Shulker Box", "shulkerboxbrown"),
    SHULKER_BOX_GREEN(BlockID.SHULKER_BOX_GREEN, "Green Shulker Box", "shulkerboxgreen"),
    SHULKER_BOX_RED(BlockID.SHULKER_BOX_RED, "Red Shulker Box", "shulkerboxred"),
    SHULKER_BOX_BLACK(BlockID.SHULKER_BOX_BLACK, "Black Shulker Box", "shulkerboxblack"),
    TERRACOTTA_WHITE(BlockID.TERRACOTTA_WHITE, "White Terracotta", "terracottawhite"),
    TERRACOTTA_ORANGE(BlockID.TERRACOTTA_ORANGE, "Orange Terracotta", "terracottaorange"),
    TERRACOTTA_MAGENTA(BlockID.TERRACOTTA_MAGENTA, "Magenta Terracotta", "terracottamagenta"),
    TERRACOTTA_LIGHT_BLUE(BlockID.TERRACOTTA_LIGHT_BLUE, "Light Blue Terracotta", "terracottalightblue"),
    TERRACOTTA_YELLOW(BlockID.TERRACOTTA_YELLOW, "Yellow Terracotta", "terracottayellow"),
    TERRACOTTA_LIME(BlockID.TERRACOTTA_LIME, "Lime Terracotta", "terracottalime"),
    TERRACOTTA_PINK(BlockID.TERRACOTTA_PINK, "Pink Terracotta", "terracottapink"),
    TERRACOTTA_GRAY(BlockID.TERRACOTTA_GRAY, "Gray Terracotta", "terracottagray"),
    TERRACOTTA_LIGHT_GRAY(BlockID.TERRACOTTA_LIGHT_GRAY, "Light Gray Terracotta", "terracottalightgray"),
    TERRACOTTA_CYAN(BlockID.TERRACOTTA_CYAN, "Cyan Terracotta", "terracottacyan"),
    TERRACOTTA_PURPLE(BlockID.TERRACOTTA_PURPLE, "Purple Terracotta", "terracottapurple"),
    TERRACOTTA_BLUE(BlockID.TERRACOTTA_BLUE, "Blue Terracotta", "terracottablue"),
    TERRACOTTA_BROWN(BlockID.TERRACOTTA_BROWN, "Brown Terracotta", "terracottabrown"),
    TERRACOTTA_GREEN(BlockID.TERRACOTTA_GREEN, "Green Terracotta", "terracottagreen"),
    TERRACOTTA_RED(BlockID.TERRACOTTA_RED, "Red Terracotta", "terracottared"),
    TERRACOTTA_BLACK(BlockID.TERRACOTTA_BLACK, "Black Terracotta", "terracottablack"),
    CONCRETE(BlockID.CONCRETE, "Concrete", "concrete"),
    CONCRETE_POWDER(BlockID.CONCRETE_POWDER, "Concrete Powder", "concretepowder"),
    STRUCTURE_BLOCK(BlockID.STRUCTURE_BLOCK, "Structure Block", "structureblock"),

    // Items
    IRON_SHOVEL(ItemID.IRON_SHOVEL, "Iron shovel", "ironshovel"),
    IRON_PICK(ItemID.IRON_PICK, "Iron pickaxe", "ironpick", "ironpickaxe"),
    IRON_AXE(ItemID.IRON_AXE, "Iron axe", "ironaxe"),
    FLINT_AND_TINDER(ItemID.FLINT_AND_TINDER, "Flint and tinder", "flintandtinder", "lighter", "flintandsteel", "flintsteel", "flintandiron", "flintnsteel", "flintniron", "flintntinder"),
    RED_APPLE(ItemID.RED_APPLE, "Red apple", "redapple", "apple"),
    BOW(ItemID.BOW, "Bow", "bow"),
    ARROW(ItemID.ARROW, "Arrow", "arrow"),
    COAL(ItemID.COAL, 0, "Coal", "coal"),
    CHARCOAL(ItemID.COAL, 1, "Charcoal", "charcoal"),
    DIAMOND(ItemID.DIAMOND, "Diamond", "diamond"),
    IRON_BAR(ItemID.IRON_BAR, "Iron bar", "ironbar", "iron"),
    GOLD_BAR(ItemID.GOLD_BAR, "Gold bar", "goldbar", "gold"),
    IRON_SWORD(ItemID.IRON_SWORD, "Iron sword", "ironsword"),
    WOOD_SWORD(ItemID.WOOD_SWORD, "Wooden sword", "woodensword", "woodsword"),
    WOOD_SHOVEL(ItemID.WOOD_SHOVEL, "Wooden shovel", "woodenshovel", "woodshovel"),
    WOOD_PICKAXE(ItemID.WOOD_PICKAXE, "Wooden pickaxe", "woodenpickaxe", "woodpick", "woodpickaxe"),
    WOOD_AXE(ItemID.WOOD_AXE, "Wooden axe", "woodenaxe", "woodaxe"),
    STONE_SWORD(ItemID.STONE_SWORD, "Stone sword", "stonesword"),
    STONE_SHOVEL(ItemID.STONE_SHOVEL, "Stone shovel", "stoneshovel"),
    STONE_PICKAXE(ItemID.STONE_PICKAXE, "Stone pickaxe", "stonepick", "stonepickaxe"),
    STONE_AXE(ItemID.STONE_AXE, "Stone pickaxe", "stoneaxe", "stonepickaxe"),
    DIAMOND_SWORD(ItemID.DIAMOND_SWORD, "Diamond sword", "diamondsword"),
    DIAMOND_SHOVEL(ItemID.DIAMOND_SHOVEL, "Diamond shovel", "diamondshovel"),
    DIAMOND_PICKAXE(ItemID.DIAMOND_PICKAXE, "Diamond pickaxe", "diamondpick", "diamondpickaxe"),
    DIAMOND_AXE(ItemID.DIAMOND_AXE, "Diamond axe", "diamondaxe"),
    STICK(ItemID.STICK, "Stick", "stick"),
    BOWL(ItemID.BOWL, "Bowl", "bowl"),
    MUSHROOM_SOUP(ItemID.MUSHROOM_SOUP, "Mushroom soup", "mushroomsoup", "soup", "brbsoup"),
    GOLD_SWORD(ItemID.GOLD_SWORD, "Golden sword", "goldsword", "goldensword"),
    GOLD_SHOVEL(ItemID.GOLD_SHOVEL, "Golden shovel", "goldshovel", "goldenshovel"),
    GOLD_PICKAXE(ItemID.GOLD_PICKAXE, "Golden pickaxe", "goldenpickaxe", "goldpick", "goldpickaxe"),
    GOLD_AXE(ItemID.GOLD_AXE, "Golden axe", "goldaxe", "goldenaxe"),
    STRING(ItemID.STRING, "String", "string"),
    FEATHER(ItemID.FEATHER, "Feather", "feather"),
    GUN_POWDER(ItemID.SULPHUR, "Gun Powder", "sulphur", "sulfur", "gunpowder"),
    WOOD_HOE(ItemID.WOOD_HOE, "Wooden hoe", "woodhoe", "woodenhoe"),
    STONE_HOE(ItemID.STONE_HOE, "Stone hoe", "stonehoe"),
    IRON_HOE(ItemID.IRON_HOE, "Iron hoe", "ironhoe"),
    DIAMOND_HOE(ItemID.DIAMOND_HOE, "Diamond hoe", "diamondhoe"),
    GOLD_HOE(ItemID.GOLD_HOE, "Golden hoe", "goldhoe", "goldenhoe"),
    SEEDS(ItemID.SEEDS, "Seeds", "seeds", "seed"),
    WHEAT(ItemID.WHEAT, "Wheat", "wheat"),
    BREAD(ItemID.BREAD, "Bread", "bread"),
    LEATHER_HELMET(ItemID.LEATHER_HELMET, "Leather helmet", "leatherhelmet", "leatherhat"),
    LEATHER_CHEST(ItemID.LEATHER_CHEST, "Leather chestplate", "leatherchest", "leatherchestplate", "leathervest", "leatherbreastplate", "leatherplate", "leathercplate", "leatherbody"),
    LEATHER_PANTS(ItemID.LEATHER_PANTS, "Leather pants", "leatherpants", "leathergreaves", "leatherlegs", "leatherleggings", "leatherstockings", "leatherbreeches"),
    LEATHER_BOOTS(ItemID.LEATHER_BOOTS, "Leather boots", "leatherboots", "leathershoes", "leatherfoot", "leatherfeet"),
    CHAINMAIL_HELMET(ItemID.CHAINMAIL_HELMET, "Chainmail helmet", "chainmailhelmet", "chainmailhat"),
    CHAINMAIL_CHEST(ItemID.CHAINMAIL_CHEST, "Chainmail chestplate", "chainmailchest", "chainmailchestplate", "chainmailvest", "chainmailbreastplate", "chainmailplate", "chainmailcplate", "chainmailbody"),
    CHAINMAIL_PANTS(ItemID.CHAINMAIL_PANTS, "Chainmail pants", "chainmailpants", "chainmailgreaves", "chainmaillegs", "chainmailleggings", "chainmailstockings", "chainmailbreeches"),
    CHAINMAIL_BOOTS(ItemID.CHAINMAIL_BOOTS, "Chainmail boots", "chainmailboots", "chainmailshoes", "chainmailfoot", "chainmailfeet"),
    IRON_HELMET(ItemID.IRON_HELMET, "Iron helmet", "ironhelmet", "ironhat"),
    IRON_CHEST(ItemID.IRON_CHEST, "Iron chestplate", "ironchest", "ironchestplate", "ironvest", "ironbreastplate", "ironplate", "ironcplate", "ironbody"),
    IRON_PANTS(ItemID.IRON_PANTS, "Iron pants", "ironpants", "irongreaves", "ironlegs", "ironleggings", "ironstockings", "ironbreeches"),
    IRON_BOOTS(ItemID.IRON_BOOTS, "Iron boots", "ironboots", "ironshoes", "ironfoot", "ironfeet"),
    DIAMOND_HELMET(ItemID.DIAMOND_HELMET, "Diamond helmet", "diamondhelmet", "diamondhat"),
    DIAMOND_CHEST(ItemID.DIAMOND_CHEST, "Diamond chestplate", "diamondchest", "diamondchestplate", "diamondvest", "diamondbreastplate", "diamondplate", "diamondcplate", "diamondbody"),
    DIAMOND_PANTS(ItemID.DIAMOND_PANTS, "Diamond pants", "diamondpants", "diamondgreaves", "diamondlegs", "diamondleggings", "diamondstockings", "diamondbreeches"),
    DIAMOND_BOOTS(ItemID.DIAMOND_BOOTS, "Diamond boots", "diamondboots", "diamondshoes", "diamondfoot", "diamondfeet"),
    GOLD_HELMET(ItemID.GOLD_HELMET, "Gold helmet", "goldhelmet", "goldhat"),
    GOLD_CHEST(ItemID.GOLD_CHEST, "Gold chestplate", "goldchest", "goldchestplate", "goldvest", "goldbreastplate", "goldplate", "goldcplate", "goldbody"),
    GOLD_PANTS(ItemID.GOLD_PANTS, "Gold pants", "goldpants", "goldgreaves", "goldlegs", "goldleggings", "goldstockings", "goldbreeches"),
    GOLD_BOOTS(ItemID.GOLD_BOOTS, "Gold boots", "goldboots", "goldshoes", "goldfoot", "goldfeet"),
    FLINT(ItemID.FLINT, "Flint", "flint"),
    RAW_PORKCHOP(ItemID.RAW_PORKCHOP, "Raw porkchop", "rawpork", "rawporkchop", "rawbacon", "baconstrips", "rawmeat"),
    COOKED_PORKCHOP(ItemID.COOKED_PORKCHOP, "Cooked porkchop", "pork", "cookedpork", "cookedporkchop", "cookedbacon", "bacon", "meat"),
    PAINTING(ItemID.PAINTING, "Painting", "painting"),
    GOLD_APPLE(ItemID.GOLD_APPLE, 0, "Golden apple", "goldapple", "goldenapple"),
    ENHANCED_GOLD_APPLE(ItemID.GOLD_APPLE, 1, "Enhanced Golden apple", "enhancedgoldenapple", "enhancedgoldapple", "notchapple"),
    SIGN(ItemID.SIGN, "Wooden sign", "sign", "woodensign"),
    WOODEN_DOOR_ITEM(ItemID.WOODEN_DOOR_ITEM, "Wooden door", "woodendoor", "wooddoor", "door"),
    BUCKET(ItemID.BUCKET, "Bucket", "bucket", "bukkit"),
    WATER_BUCKET(ItemID.WATER_BUCKET, "Water bucket", "waterbucket", "waterbukkit"),
    LAVA_BUCKET(ItemID.LAVA_BUCKET, "Lava bucket", "lavabucket", "lavabukkit"),
    MINECART(ItemID.MINECART, "Minecart", "minecart", "cart"),
    SADDLE(ItemID.SADDLE, "Saddle", "saddle"),
    IRON_DOOR_ITEM(ItemID.IRON_DOOR_ITEM, "Iron door", "irondoor"),
    REDSTONE_DUST(ItemID.REDSTONE_DUST, "Redstone dust", "redstonedust", "reddust", "redstone", "dust", "wire"),
    SNOWBALL(ItemID.SNOWBALL, "Snowball", "snowball"),
    WOOD_BOAT(ItemID.WOOD_BOAT, "Wooden boat", "woodboat", "woodenboat", "boat"),
    LEATHER(ItemID.LEATHER, "Leather", "leather", "cowhide"),
    MILK_BUCKET(ItemID.MILK_BUCKET, "Milk bucket", "milkbucket", "milk", "milkbukkit"),
    BRICK_BAR(ItemID.BRICK_BAR, "Brick", "brick", "brickbar"),
    CLAY_BALL(ItemID.CLAY_BALL, "Clay Ball", "clayball", "clay"),
    SUGAR_CANE_ITEM(ItemID.SUGAR_CANE_ITEM, "Sugar cane", "sugarcane", "reed", "reeds"),
    PAPER(ItemID.PAPER, "Paper", "paper"),
    BOOK(ItemID.BOOK, "Book", "book"),
    SLIME_BALL(ItemID.SLIME_BALL, "Slime ball", "slimeball", "slime"),
    STORAGE_MINECART(ItemID.STORAGE_MINECART, "Minecart with Chest", "storageminecart", "storagecart", "minecartwithchest", "minecartchest", "chestminecart"),
    POWERED_MINECART(ItemID.POWERED_MINECART, "Minecart with Furnace", "poweredminecart", "poweredcart", "minecartwithfurnace", "minecartfurnace", "furnaceminecart"),
    EGG(ItemID.EGG, "Egg", "egg"),
    COMPASS(ItemID.COMPASS, "Compass", "compass"),
    FISHING_ROD(ItemID.FISHING_ROD, "Fishing rod", "fishingrod", "fishingpole"),
    CLOCK(ItemID.WATCH, "Clock", "watch", "clock", "timer"),
    GLOWSTONE_DUST(ItemID.LIGHTSTONE_DUST, "Glowstone dust", "lightstonedust", "glowstonedone", "glowstonedust", "brightstonedust", "brittlegolddust", "brimstonedust"),
    RAW_FISH(ItemID.RAW_FISH, 0, "Cod", "cod", "rawfish"),
    RAW_SALMON(ItemID.RAW_FISH, 1, "Salmon", "salmon", "rawsalmon"),
    CLOWNFISH(ItemID.RAW_FISH, 2, "Clownfish", "clownfish"),
    PUFFERFISH(ItemID.RAW_FISH, 3, "Pufferfish", "pufferfish"),
    COOKED_FISH(ItemID.COOKED_FISH, 0, "Cooked Cod", "cookedcod", "cookedfish"),
    COOKED_SALMON(ItemID.COOKED_FISH, 1, "Cooked Salmon", "cookedsalmon"),
    INK_SACK(ItemID.INK_SACK, 0, "Ink sac", "inksac", "ink", "blackdye", "inksack"),
    LAPIS_LAZULI(ItemID.INK_SACK, 4, "Lapis lazuli", "lapislazuli", "bluedye"),
    BONE_MEAL(ItemID.INK_SACK, 15, "Bone Meal", "bonemeal", "whitedye"),
    BONE(ItemID.BONE, "Bone", "bone"),
    SUGAR(ItemID.SUGAR, "Sugar", "sugar"),
    CAKE_ITEM(ItemID.CAKE_ITEM, "Cake", "cake"),
    BED_ITEM(ItemID.BED_ITEM, "Bed", "bed"),
    REDSTONE_REPEATER(ItemID.REDSTONE_REPEATER, "Redstone repeater", "redstonerepeater", "diode", "delayer", "repeater"),
    COOKIE(ItemID.COOKIE, "Cookie", "cookie"),
    MAP(ItemID.MAP, 0, "City Map", "citymap", "map"),
    SHEARS(ItemID.SHEARS, "Shears", "shears", "scissors"),
    MELON(ItemID.MELON, "Melon Slice", "melon", "melonslice"),
    PUMPKIN_SEEDS(ItemID.PUMPKIN_SEEDS, "Pumpkin seeds", "pumpkinseed", "pumpkinseeds"),
    MELON_SEEDS(ItemID.MELON_SEEDS, "Melon seeds", "melonseed", "melonseeds"),
    RAW_BEEF(ItemID.RAW_BEEF, "Raw beef", "rawbeef", "rawcow", "beef"),
    COOKED_BEEF(ItemID.COOKED_BEEF, "Steak", "steak", "cookedbeef", "cookedcow"),
    RAW_CHICKEN(ItemID.RAW_CHICKEN, "Raw chicken", "rawchicken"),
    COOKED_CHICKEN(ItemID.COOKED_CHICKEN, "Cooked chicken", "cookedchicken", "chicken", "grilledchicken"),
    ROTTEN_FLESH(ItemID.ROTTEN_FLESH, "Rotten flesh", "rottenflesh", "zombiemeat", "flesh"),
    ENDER_PEARL(ItemID.ENDER_PEARL, "Ender pearl", "pearl", "enderpearl"),
    BLAZE_ROD(ItemID.BLAZE_ROD, "Blaze rod", "blazerod"),
    GHAST_TEAR(ItemID.GHAST_TEAR, "Ghast tear", "ghasttear"),
    GOLD_NUGGET(ItemID.GOLD_NUGGET, "Gold nugget", "goldnugget", "goldnuggest"),
    NETHER_WART_ITEM(ItemID.NETHER_WART_SEED, "Nether wart", "netherwart", "netherwartseed"),
    POTION(ItemID.POTION, 0, "Potion", "potion"),
    GLASS_BOTTLE(ItemID.GLASS_BOTTLE, "Glass bottle", "glassbottle"),
    SPIDER_EYE(ItemID.SPIDER_EYE, "Spider eye", "spidereye"),
    FERMENTED_SPIDER_EYE(ItemID.FERMENTED_SPIDER_EYE, "Fermented spider eye", "fermentedspidereye", "fermentedeye"),
    BLAZE_POWDER(ItemID.BLAZE_POWDER, "Blaze powder", "blazepowder"),
    MAGMA_CREAM(ItemID.MAGMA_CREAM, "Magma cream", "magmacream"),
    BREWING_STAND_ITEM(ItemID.BREWING_STAND, "Brewing stand", "brewingstand"),
    CAULDRON_ITEM(ItemID.CAULDRON, "Cauldron", "cauldron"),
    EYE_OF_ENDER(ItemID.EYE_OF_ENDER, "Eye of Ender", "eyeofender", "endereye"),
    GLISTERING_MELON(ItemID.GLISTERING_MELON, "Glistering Melon", "glisteringmelon", "goldmelon"),
    SPAWN_EGG(ItemID.SPAWN_EGG, 92, "Spawn Egg", "spawnegg", "spawn", "mobspawnegg"), // Should be 0
    BOTTLE_O_ENCHANTING(ItemID.BOTTLE_O_ENCHANTING, "Bottle o' Enchanting", "xpbottle", "expbottle", "bottleoenchanting", "experiencebottle"),
    FIRE_CHARGE(ItemID.FIRE_CHARGE, "Fire Charge", "firecharge", "firestarter", "firerock"),
    BOOK_AND_QUILL(ItemID.BOOK_AND_QUILL, "Book and Quill", "bookandquill", "quill", "writingbook"),
    WRITTEN_BOOK(ItemID.WRITTEN_BOOK, "Written Book", "writtenbook"),
    EMERALD(ItemID.EMERALD, "Emerald", "emeraldingot", "emerald"),
    ITEM_FRAME(ItemID.ITEM_FRAME, "Item frame", "itemframe", "frame", "itempainting"),
    FLOWER_POT(ItemID.FLOWER_POT, "Flower pot", "flowerpot", "plantpot", "pot"),
    CARROT(ItemID.CARROT, "Carrot", "carrot"),
    POTATO(ItemID.POTATO, "Potato", "potato"),
    BAKED_POTATO(ItemID.BAKED_POTATO, "Baked potato", "bakedpotato", "potatobaked"),
    POISONOUS_POTATO(ItemID.POISONOUS_POTATO, "Poisonous potato", "poisonpotato", "poisonouspotato"),
    BLANK_MAP(ItemID.BLANK_MAP, 0, "Blank map", "blankmap", "emptymap"),
    GOLDEN_CARROT(ItemID.GOLDEN_CARROT, "Golden carrot", "goldencarrot", "goldcarrot"),
    SKULL(ItemID.HEAD, 0, "Skull", "skull", "head", "headmount", "mount"),
    WITHER_SKULL(ItemID.HEAD, 1, "Wither Skeleton Skull", "witherskull", "witherskeletonskull", "witherskeletonhead", "witherhead"),
    ZOMBIE_SKULL(ItemID.HEAD, 2, "Zombie Skull", "zombieskull", "zombiehead"),
    PLAYER_SKULL(ItemID.HEAD, 3, "Player Skull", "playerskull", "playerhead"),
    CARROT_ON_A_STICK(ItemID.CARROT_ON_A_STICK, "Carrot on a stick", "carrotonastick", "carrotonstick", "stickcarrot", "carrotstick"),
    NETHER_STAR(ItemID.NETHER_STAR, "Nether star", "netherstar", "starnether"),
    PUMPKIN_PIE(ItemID.PUMPKIN_PIE, "Pumpkin pie", "pumpkinpie"),
    FIREWORK_ROCKET(ItemID.FIREWORK_ROCKET, "Firework rocket", "fireworkrocket", "firework", "rocket"),
    FIREWORK_STAR(ItemID.FIREWORK_STAR, "Firework star", "fireworkstar", "fireworkcharge"),
    ENCHANTED_BOOK(ItemID.ENCHANTED_BOOK, "Enchanted book", "enchantedbook", "spellbook", "enchantedtome", "tome"),
    COMPARATOR(ItemID.COMPARATOR, "Comparator", "comparator", "capacitor"),
    NETHER_BRICK_ITEM(ItemID.NETHER_BRICK, "Nether Brick", "netherbrick", "netherbrickbar"),
    NETHER_QUARTZ(ItemID.NETHER_QUARTZ, "Nether Quartz", "netherquartz", "quartz"),
    TNT_MINECART(ItemID.TNT_MINECART, "Minecart with TNT", "minecraftwithtnt", "tntminecart", "minecarttnt"),
    HOPPER_MINECART(ItemID.HOPPER_MINECART, "Minecart with Hopper", "minecraftwithhopper", "hopperminecart", "minecarthopper"),
    PRISMARINE_SHARD(ItemID.PRISMARINE_SHARD, "Prismarine Shard", "prismarineshard"),
    PRISMARINE_CRYSTALS(ItemID.PRISMARINE_CRYSTALS, "Prismarine Crystals", "prismarinecrystals", "prismarinecrystal"),
    RABBIT(ItemID.RABBIT, "Raw Rabbit", "rawrabbit", "rabbit"), // raw and wriggling
    COOKED_RABBIT(ItemID.COOKED_RABBIT, "Cooked Rabbit", "cookedrabbit"), // stupid fat hobbit, you ruins it
    RABBIT_STEW(ItemID.RABBIT_STEW, "Rabbit Stew", "rabbitstew"), // po-ta-toes
    RABBIT_FOOT(ItemID.RABBIT_FOOT, "Rabbit's Foot", "rabbitsfoot", "rabbitfoot"),
    RABBIT_HIDE(ItemID.RABBIT_HIDE, "Rabbit Hide", "rabbithide", "rabbitskin"),
    ARMOR_STAND(ItemID.ARMOR_STAND, "Armor Stand", "armorstand"),
    HORSE_ARMOR_IRON(ItemID.HORSE_ARMOR_IRON, "Iron Horse Armor", "ironhorsearmor", "ironbarding"),
    HORSE_ARMOR_GOLD(ItemID.HORSE_ARMOR_GOLD, "Gold Horse Armor", "goldhorsearmor", "goldbarding"),
    HORSE_ARMOR_DIAMOND(ItemID.HORSE_ARMOR_DIAMOND, "Diamond Horse Armor", "diamondhorsearmor", "diamondbarding"),
    LEAD(ItemID.LEAD, "Lead", "lead", "leash"),
    NAME_TAG(ItemID.NAME_TAG, "Name Tag", "nametag"),
    COMMAND_BLOCK_MINECART(ItemID.COMMAND_BLOCK_MINECART, "Minecart with Command Block"),
    MUTTON(ItemID.MUTTON, "Mutton", "mutton", "rawmutton"),
    COOKED_MUTTON(ItemID.COOKED_MUTTON, "Cooked Mutton", "cookedmutton"),
    BANNER(ItemID.BANNER, "Banner", "banner"),
    END_CRYSTAL(ItemID.END_CRYSTAL, "End Crystal", "endcrystal"),
    SPRUCE_DOOR_ITEM(ItemID.SPRUCE_DOOR, "Spruce Door", "sprucedoor"),
    BIRCH_DOOR_ITEM(ItemID.BIRCH_DOOR, "Birch Door", "birchdoor"),
    JUNGLE_DOOR_ITEM(ItemID.JUNGLE_DOOR, "Jungle Door", "jungledoor"),
    ACACIA_DOOR_ITEM(ItemID.ACACIA_DOOR, "Acacia Door", "acaciadoor"),
    DARK_OAK_DOOR_ITEM(ItemID.DARK_OAK_DOOR, "Dark Oak Door", "darkoakdoor"),
    CHORUS_FRUIT(ItemID.CHORUS_FRUIT, "Chorus Fruit", "chorusfruit"),
    CHORUS_FRUIT_POPPED(ItemID.CHORUS_FRUIT_POPPED, "Popped Chorus Fruit", "poppedchorusfruit", "chorusfruitpopped", "cookedchorusfruit"),
    BEETROOT(ItemID.BEETROOT, "Beetroot", "beetroot"),
    BEETROOT_SEEDS(ItemID.BEETROOT_SEEDS, "Beetroot Seeds", "beetrootseeds"),
    BEETROOT_SOUP(ItemID.BEETROOT_SOUP, "Beetroot Soup", "beetrootsoup"),
    DRAGON_BREATH(ItemID.DRAGON_BREATH, "Dragon Breath", "dragonbreath"),
    SPLASH_POTION(ItemID.SPLASH_POTION, "Splash Potion", "splashpotion", "potionsplash"),
    SPECTRAL_ARROW(ItemID.SPECTRAL_ARROW, "Spectral Arrow", "spectralarrow", "glowingarrow"),
    TIPPED_ARROW(ItemID.TIPPED_ARROW, "Tipped Arrow", "tippedarrow", "potionarrow"),
    LINGERING_POTION(ItemID.LINGERING_POTION, "Lingering Potion", "lingeringpotion", "potionlingering"),
    SHIELD(ItemID.SHIELD, "Shield", "shield"),
    ELYTRA(ItemID.ELYTRA, "Elytra", "elytra", "wings"),
    SPRUCE_BOAT(ItemID.SPRUCE_BOAT, "Spruce Boat", "spruceboat", "boatspruce"),
    BIRCH_BOAT(ItemID.BIRCH_BOAT, "Birch Boat", "birchboat", "boatbirch"),
    JUNGLE_BOAT(ItemID.JUNGLE_BOAT, "Jungle Boat", "jungleboat", "boatjungle"),
    ACACIA_BOAT(ItemID.ACACIA_BOAT, "Acacia Boat", "acaciaboat", "boatacacia"),
    DARK_OAK_BOAT(ItemID.DARK_OAK_BOAT, "Dark Oak Boat", "darkoakboat", "boatdarkoak"),
    TOTEM_OF_UNDYING(ItemID.TOTEM_OF_UNDYING, "Totem of Undying", "totemofundying", "undyingtotem"),
    SHULKER_SHELL(ItemID.SHULKER_SHELL, "Shulker Shell", "shulkershell"),
    IRON_NUGGET(ItemID.IRON_NUGGET, "Iron Nugget", "ironnugget"),
    KNOWLEDGE_BOOK(ItemID.KNOWLEDGE_BOOK, "Knowledge Book", "knowledgebook", "recipebook"),

    DISC_13(ItemID.DISC_13, "Music Disc - 13", "musicdisc13", "disc_13"),
    DISC_CAT(ItemID.DISC_CAT, "Music Disc - Cat", "musicdisccat", "disc_cat"),
    DISC_BLOCKS(ItemID.DISC_BLOCKS, "Music Disc - blocks", "musicdiscblocks", "disc_blocks"),
    DISC_CHIRP(ItemID.DISC_CHIRP, "Music Disc - chirp", "musicdiscchirp", "disc_chirp"),
    DISC_FAR(ItemID.DISC_FAR, "Music Disc - far", "musicdiscfar", "disc_far"),
    DISC_MALL(ItemID.DISC_MALL, "Music Disc - mall", "musicdiscmall", "disc_mall"),
    DISC_MELLOHI(ItemID.DISC_MELLOHI, "Music Disc - mellohi", "musicdiscmellohi", "disc_mellohi"),
    DISC_STAL(ItemID.DISC_STAL, "Music Disc - stal", "musicdiscstal", "disc_stal"),
    DISC_STRAD(ItemID.DISC_STRAD, "Music Disc - strad", "musicdiscstrad", "disc_strad"),
    DISC_WARD(ItemID.DISC_WARD, "Music Disc - ward", "musicdiscward", "disc_ward"),
    DISC_11(ItemID.DISC_11, "Music Disc - 11", "musicdisc11", "disc_11"),
    DISC_WAIT(ItemID.DISC_WAIT, "Music Disc - wait", "musicdiscwait", "disc_wait");

    /**
     * Stores a map of the IDs for fast access.
     */
    private static final Map<BaseItem, ItemType> ids = new HashMap<>();
    /**
     * Stores a map of the names for fast access.
     */
    private static final Map<String, ItemType> lookup = new LinkedHashMap<>();

    private final int id;
    private final int data;
    private final String name;
    private final String[] lookupKeys;

    static {
        for (ItemType type : EnumSet.allOf(ItemType.class)) {
            ids.put(new BaseItem(type.id, type.data), type);
            for (String key : type.lookupKeys) {
                lookup.put(key, type);
            }
        }
    }

    /**
     * Construct the type.
     *
     * @param id
     * @param name
     * @param lookupKey
     */
    ItemType(int id, String name, String lookupKey) {

        this.id = id;
        this.data = -1;
        this.name = name;
        this.lookupKeys = new String[]{lookupKey};
    }

    /**
     * Construct the type.
     *
     * @param id
     * @param data
     * @param name
     * @param lookupKey
     */
    ItemType(int id, int data, String name, String lookupKey) {

        this.id = id;
        this.data = data;
        this.name = name;
        this.lookupKeys = new String[]{lookupKey};
    }

    /**
     * Construct the type.
     *
     * @param id
     * @param name
     * @param lookupKeys
     */
    ItemType(int id, String name, String... lookupKeys) {

        this.id = id;
        this.data = -1;
        this.name = name;
        this.lookupKeys = lookupKeys;
    }

    /**
     * Construct the type.
     *
     * @param id
     * @param data
     * @param name
     * @param lookupKeys
     */
    ItemType(int id, int data, String name, String... lookupKeys) {

        this.id = id;
        this.data = data;
        this.name = name;
        this.lookupKeys = lookupKeys;
    }

    /**
     * Return type from ID. May return null.
     *
     * @param id
     * @return
     */
    public static ItemType fromNumberic(int id, int data) {

        BaseItem i = new BaseItem(id, data);
        for (Map.Entry<BaseItem, ItemType> entry : ids.entrySet()) {
            if (entry.getKey().equals(i)) return entry.getValue();
        }
        return null;
    }

    /**
     * Return type from name. May return null.
     *
     * @param name
     * @return
     */
    public static ItemType lookup(String name) {

        return lookup(name, false);
    }

    /**
     * Return type from name. May return null.
     *
     * @param name
     * @param fuzzy
     * @return
     */
    public static ItemType lookup(String name, boolean fuzzy) {

        try {
            String[] split = name.split(":");
            int id = Integer.parseInt(split[0]);
            short data = 0;
            if (split.length > 1) {
                data = Short.parseShort(split[1]);
            }
            return fromNumberic(id, data);
        } catch (NumberFormatException e) {
            return lookup(lookup, name, fuzzy);
        }
    }

    /**
     * Get item numeric ID.
     *
     * @return
     */
    public int getID() {

        return id;
    }

    /**
     * Get item numeric data
     *
     * @return
     */
    public int getData() {

        return data < 0 ? 0 : data;
    }

    /**
     * Get user-friendly item name.
     *
     * @return
     */
    public String getName() {

        return name;
    }

    /**
     * Get a list of aliases.
     *
     * @return
     */
    public String[] getAliases() {

        return lookupKeys;
    }

    private static final Set<Integer> usesDamageValue = new HashSet<>();

    static {
        usesDamageValue.add(BlockID.DIRT);
        usesDamageValue.add(BlockID.WOOD);
        usesDamageValue.add(BlockID.SAPLING);
        usesDamageValue.add(BlockID.SAND);
        usesDamageValue.add(BlockID.LOG);
        usesDamageValue.add(BlockID.LEAVES);
        usesDamageValue.add(BlockID.SANDSTONE);
        usesDamageValue.add(BlockID.LONG_GRASS);
        usesDamageValue.add(BlockID.CLOTH);
        usesDamageValue.add(BlockID.RED_FLOWER);
        usesDamageValue.add(BlockID.DOUBLE_STEP);
        usesDamageValue.add(BlockID.STEP);
        usesDamageValue.add(BlockID.SILVERFISH_BLOCK);
        usesDamageValue.add(BlockID.STONE_BRICK);
        usesDamageValue.add(BlockID.BROWN_MUSHROOM_CAP);
        usesDamageValue.add(BlockID.RED_MUSHROOM_CAP);
        usesDamageValue.add(BlockID.DOUBLE_WOODEN_STEP);
        usesDamageValue.add(BlockID.WOODEN_STEP);
        usesDamageValue.add(BlockID.COBBLESTONE_WALL);
        usesDamageValue.add(BlockID.ANVIL);
        usesDamageValue.add(BlockID.QUARTZ_BLOCK);
        usesDamageValue.add(BlockID.STAINED_CLAY);
        usesDamageValue.add(BlockID.CARPET);

        usesDamageValue.add(ItemID.COAL);
        usesDamageValue.add(ItemID.INK_SACK);
        usesDamageValue.add(ItemID.POTION);
        usesDamageValue.add(ItemID.SPAWN_EGG);
        usesDamageValue.add(ItemID.MAP);
        usesDamageValue.add(ItemID.HEAD);
        usesDamageValue.add(ItemID.GOLD_APPLE);
        usesDamageValue.add(ItemID.RAW_FISH);
        usesDamageValue.add(ItemID.COOKED_FISH);
    }

    /**
     * Returns true if an item uses its damage value for something
     * other than damage.
     *
     * @param id
     * @return
     */
    public static boolean usesDamageValue(int id) {

        return usesDamageValue.contains(id);
    }

    public static <T extends Enum<?>> T lookup(Map<String, T> lookup, String name, boolean fuzzy) {
        String testName = name.replaceAll("[^A-Za-z0-9]", "").toLowerCase();

        T type = lookup.get(testName);
        if (type != null) {
            return type;
        }

        if (!fuzzy) {
            return null;
        }

        int minDist = Integer.MAX_VALUE;

        for (Map.Entry<String, T> entry : lookup.entrySet()) {
            final String key = entry.getKey();
            if (key.charAt(0) != testName.charAt(0)) {
                continue;
            }

            int dist = StringUtil.getLevenshteinDistance(key, testName);

            if (dist >= minDist) {
                minDist = dist;
                type = entry.getValue();
            }
        }

        if (minDist > 1) {
            return null;
        }

        return type;
    }
}