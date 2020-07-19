package gg.packetloss.grindstone.world.type.city.area.areas.NinjaParkour;

public class NinjaParkourPlayerState {
    private long startTime = System.currentTimeMillis();

    public long getStartTime() {
        return startTime;
    }

    public long getElapsedTime(long fromTime) {
        return fromTime - getStartTime();
    }

    public long getElapsedTime() {
        return getElapsedTime(System.currentTimeMillis());
    }
}
