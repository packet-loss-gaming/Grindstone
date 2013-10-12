package com.skelril.aurora.util.restoration;

public abstract class BlockRecordIndex {

    public abstract void revertByTime(long time);

    public abstract void revertAll();

    public abstract int size();
}
