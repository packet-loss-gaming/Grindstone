package gg.packetloss.grindstone.util.signwall;

import com.sk89q.commandbook.CommandBook;
import org.apache.commons.lang.Validate;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;

import java.util.Optional;

public class SignWall<T> {
    private final Location startPoint;
    private final BlockFace direction;
    private final int distance;
    private final SignWallPainter<T> painter;
    private final SignWallClickHandler<T> clickHandler;
    private final SignWallDataBackend<T> dataBackend;

    private int offset = 0;
    private boolean listenerEnabled = false;

    public SignWall(Location startPoint, BlockFace direction, int distance,
                    SignWallPainter<T> painter, SignWallClickHandler<T> clickHandler,
                    SignWallDataBackend<T> dataBackend) {
        this.startPoint = startPoint;
        this.direction = direction;
        this.distance = distance;
        this.painter = painter;
        this.clickHandler = clickHandler;
        this.dataBackend = dataBackend;
    }

    private void initListener() {
        Validate.isTrue(!listenerEnabled, "listener must only be registered once");

        CommandBook.registerEvents(new SignWallListener(this));
        listenerEnabled = true;
    }

    public void init() {
        initListener();
        paint();
    }

    private Location getLocationForIndex(int index) {
        Location loc = startPoint.clone();
        loc.add(
                direction.getModX() * index,
                direction.getModY() * index,
                direction.getModZ() * index
        );
        return loc;
    }

    private Optional<Sign> getSignForIndex(int index) {
        Location signLoc = getLocationForIndex(index);

        BlockState state = signLoc.getBlock().getState();
        if (state instanceof Sign) {
            return Optional.of((Sign) state);
        }

        return Optional.empty();
    }

    private int getValueIndex(int index) {
        return index - 1;
    }

    private boolean hasValue(int index) {
        return index + offset < dataBackend.size();
    }

    private T getValue(int index) {
        return dataBackend.get(index + offset);
    }

    private void setValue(int index, T value) {
        dataBackend.set(index + offset, value);
    }

    private int getFirstSignIndex() {
        return 0;
    }

    private int getLastSignIndex() {
        return distance - 1;
    }

    private int getNumContentSigns() {
        return distance - 2;
    }

    public void paint() {
        getSignForIndex(getFirstSignIndex()).ifPresent((sign) -> {
            painter.paintFirst(offset, sign);
        });

        for (int i = 0; i < getNumContentSigns(); ++i) {
            Optional<Sign> optSign = getSignForIndex(i + 1);
            if (optSign.isEmpty()) {
                continue;
            }

            if (hasValue(i)) {
                painter.paint(getValue(i), optSign.get());
            } else {
                painter.paintEmpty(optSign.get());
            }
        }

        getSignForIndex(getLastSignIndex()).ifPresent((sign) -> {
            painter.paintLast(Math.max(0, dataBackend.size() - (offset + getNumContentSigns())), sign);
        });
    }

    private boolean hasSignAt(Location location) {
        Location start = getLocationForIndex(0);
        Location end = getLocationForIndex(distance - 1);

        Location min = new Location(
                start.getWorld(),
                Math.min(start.getBlockX(), end.getBlockX()),
                Math.min(start.getBlockY(), end.getBlockY()),
                Math.min(start.getBlockZ(), end.getBlockZ())
        );
        Location max = new Location(
                start.getWorld(),
                Math.max(start.getBlockX(), end.getBlockX()),
                Math.max(start.getBlockY(), end.getBlockY()),
                Math.max(start.getBlockZ(), end.getBlockZ())
        );

        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();

        return min.getBlockX() <= x && x <= max.getBlockX() &&
               min.getBlockY() <= y && y <= max.getBlockY() &&
               min.getBlockZ() <= z && z <= max.getBlockZ();
    }

    private int getSignIndex(Location location) {
        Location adjusted = location.subtract(startPoint);

        switch (direction) {
            case NORTH:
            case SOUTH:
                return adjusted.getBlockZ();
            case EAST:
            case WEST:
                return adjusted.getBlockX();
            default:
                throw new IllegalStateException();
        }
    }

    public Optional<Integer> getSignIndexAt(Location location) {
        if (!hasSignAt(location)) {
            return Optional.empty();
        }

        return Optional.of(getSignIndex(location));
    }

    private void tryUpdateNavigation(Runnable op) {
        if (clickHandler.allowNavigation()) {
            op.run();
        }
    }

    public void handleClick(Player player, int index, boolean leftClick) {
        if (index == getFirstSignIndex()) {
            tryUpdateNavigation(() -> {
                offset = Math.max(0, offset - getNumContentSigns());
            });
        } else if (index == getLastSignIndex()) {
            tryUpdateNavigation(() -> {
                int adjustedDataSize = Math.max(getNumContentSigns(), dataBackend.size());
                offset = Math.min(adjustedDataSize - getNumContentSigns(), offset + getNumContentSigns());
            });
        } else {
            int valueIndex = getValueIndex(index);
            if (!hasValue(valueIndex)) {
                return;
            }

            if (leftClick) {
                setValue(valueIndex, clickHandler.handleLeftClick(player, getValue(valueIndex)));
            } else {
                setValue(valueIndex, clickHandler.handleRightClick(player, getValue(valueIndex)));
            }
        }
    }
}
