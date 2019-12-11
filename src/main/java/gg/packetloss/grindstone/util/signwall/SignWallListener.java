package gg.packetloss.grindstone.util.signwall;

import gg.packetloss.grindstone.util.EnvironmentUtil;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Optional;

public class SignWallListener implements Listener {
    private final SignWall<?> signWall;

    public SignWallListener(SignWall<?> signWall) {
        this.signWall = signWall;
    }

    private void handleSignClick(Player player, Block block, boolean leftClick) {
        Optional<Integer> optSignIndex = signWall.getSignIndexAt(block.getLocation());
        if (optSignIndex.isEmpty()) {
            return;
        }

        int signIndex = optSignIndex.get();

        signWall.handleClick(player, signIndex, leftClick);
        signWall.paint();
    }

    public boolean isClick(PlayerInteractEvent event) {
        switch (event.getAction()) {
            case LEFT_CLICK_AIR:
            case LEFT_CLICK_BLOCK:
            case RIGHT_CLICK_BLOCK:
            case RIGHT_CLICK_AIR:
                return true;
            default:
                return false;
        }
    }

    public boolean isLeftClick(PlayerInteractEvent event) {
        switch (event.getAction()) {
            case LEFT_CLICK_AIR:
            case LEFT_CLICK_BLOCK:
                return true;
            case RIGHT_CLICK_BLOCK:
            case RIGHT_CLICK_AIR:
                return false;
            default:
                throw new IllegalStateException();
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onClick(PlayerInteractEvent event) {
        if (!isClick(event)) {
           return;
        }

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) {
            return;
        }

        if (!EnvironmentUtil.isSign(clickedBlock)) {
            return;
        }

        handleSignClick(event.getPlayer(), clickedBlock, isLeftClick(event));
    }
}
