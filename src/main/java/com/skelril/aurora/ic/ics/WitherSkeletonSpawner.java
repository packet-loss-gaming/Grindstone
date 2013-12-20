package com.skelril.aurora.ic.ics;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.craftbook.ChangedSign;
import com.sk89q.craftbook.circuits.ic.*;
import com.sk89q.craftbook.util.LocationUtil;
import com.sk89q.worldedit.blocks.ItemID;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Skeleton;
import org.bukkit.inventory.ItemStack;

import java.util.logging.Logger;

/**
 * Created by Wyatt on 12/19/13.
 */
public class WitherSkeletonSpawner extends AbstractIC {

    private static final CommandBook inst = CommandBook.inst();
    private static final Logger log = inst.getLogger();
    private static final Server server = CommandBook.server();


    public WitherSkeletonSpawner(Server server, ChangedSign block, ICFactory factory) {

        super(server, block, factory);
    }

    @Override
    public String getTitle() {

        return "Wither Skel";
    }

    @Override
    public String getSignTitle() {

        return "WITHER SKEL";
    }

    @Override
    public void trigger(ChipState chip) {

        Location k = LocationUtil.getCenterOfBlock(LocationUtil.getNextFreeSpace(getBackBlock(), BlockFace.UP));
        Skeleton skeleton = (Skeleton) k.getWorld().spawnEntity(k, EntityType.SKELETON);
        skeleton.setSkeletonType(Skeleton.SkeletonType.WITHER);
        skeleton.getEquipment().setItemInHand(new ItemStack(ItemID.STONE_SWORD));
    }

    public static class Factory extends AbstractICFactory implements RestrictedIC {

        public Factory(Server server) {

            super(server);
        }

        @Override
        public IC create(ChangedSign sign) {

            return new WitherSkeletonSpawner(getServer(), sign, this);
        }

        @Override
        public String getShortDescription() {

            return "Spawns Wither Skeletons.";
        }

        @Override
        public String[] getLineHelp() {

            return new String[]{"", ""};
        }
    }
}
