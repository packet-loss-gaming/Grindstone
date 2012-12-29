package us.arrowcraft.aurora.jail;
/**
 * Author: Turtle9598
 */
public class Inmate {

    private final String name;
    private final String prisonName;
    private final String reason;
    private final long start;
    private final long end;

    public Inmate(String name, String prisonName, String reason, long start, long end) {

        this.name = name;
        this.prisonName = prisonName;
        this.reason = reason;
        this.start = start;
        this.end = end;
    }

    public String getName() {

        return name;
    }

    public String getPrisonName() {

        return prisonName;
    }

    public String getReason() {

        return reason;
    }

    public long getStart() {

        return start;
    }

    public long getEnd() {

        return end;
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
