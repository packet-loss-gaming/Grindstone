package gg.packetloss.grindstone.prettyfier;

import com.sk89q.commandbook.CommandBook;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;

import java.util.ArrayList;
import java.util.List;

@ComponentInformation(friendlyName = "Prettyfier", desc = "Keeping things neat and orderly")
public class PrettyfierComponent extends BukkitComponent {
    private List<Prettyfier> prettyfiers = new ArrayList<>();

    @Override
    public void enable() {
        AutoCloser autoCloser = new AutoCloser();
        CommandBook.registerEvents(autoCloser);
        prettyfiers.add(autoCloser);
    }
}
