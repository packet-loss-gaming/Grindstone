package com.skelril.aurora.jail;

/**
 * Author: Turtle9598
 */
public class Inmate {

    private final String name;
    private final String prisonName;
    private final String reason;
    private final long start;
    private final long end;
    private final boolean isMuted;

    public Inmate(String name, String prisonName, String reason, long start, long end, boolean isMuted) {

        this.name = name.trim();
        this.prisonName = prisonName.trim();
        this.reason = reason.trim();
        this.start = start;
        this.end = end;
        this.isMuted = isMuted;
    }

    public String getName() {

        return name;
    }

    public String getPrisonName() {

        return prisonName;
    }

    public String getReason() {

        return reason.isEmpty() ? null : reason;
    }

    public long getStart() {

        return start;
    }

    public long getEnd() {

        return end;
    }

    public boolean isMuted() {

        return isMuted;
    }

    @Override
    public boolean equals(Object other) {

        if (!(other instanceof Inmate)) {
            return false;
        }
        Inmate ban = (Inmate) other;
        return potentialNullEquals(name, ban.name);
    }

    public static boolean potentialNullEquals(Object a, Object b) {

        return (a == null && b == null)
                || a != null && b != null
                && a.equals(b);
    }

    @Override
    public int hashCode() {

        int result = name != null ? name.hashCode() : 0;
        result = 32 * result;
        return result;
    }
}
