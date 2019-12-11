package gg.packetloss.grindstone.util.signwall;

public interface SignWallDataBackend<T> {
    public T get(int index);
    public void set(int index, T value);
    public int size();
}
