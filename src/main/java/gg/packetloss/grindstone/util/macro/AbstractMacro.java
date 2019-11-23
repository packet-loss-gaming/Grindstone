package gg.packetloss.grindstone.util.macro;

import java.util.List;
import java.util.regex.Pattern;

public abstract class AbstractMacro {
    protected final Pattern pattern;

    public AbstractMacro(String macroRegex) {
        pattern = Pattern.compile(macroRegex, Pattern.CASE_INSENSITIVE);
    }

    public boolean matches(String input) {
        return pattern.matcher(input).find();
    }

    public abstract List<String> expand(String input);
}
