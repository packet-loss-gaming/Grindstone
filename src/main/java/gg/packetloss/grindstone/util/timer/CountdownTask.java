package gg.packetloss.grindstone.util.timer;

@Deprecated
public abstract class CountdownTask implements IntegratedRunnable {
    public abstract boolean matchesFilter(int times);
    public abstract void performStep(int times);
    public abstract void performFinal();
    public void performEvery(int times) {
    }

    @Override
    public boolean run(int times) {
        performEvery(times);

        if (matchesFilter(times)) {
            performStep(times);
        }
        return true;
    }

    @Override
    public void end() {
        performEvery(-1);
        performFinal();
    }
}
