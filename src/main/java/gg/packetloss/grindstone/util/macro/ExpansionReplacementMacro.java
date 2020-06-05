package gg.packetloss.grindstone.util.macro;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;

public class ExpansionReplacementMacro extends AbstractMacro {
    private final List<String> substitutions;

    public ExpansionReplacementMacro(String macroRegex, List<String> substitutions) {
        super(macroRegex);
        this.substitutions = substitutions;
    }

    @Override
    public Set<String> expand(String input) {
        Set<String> expansions = new HashSet<>(substitutions.size());

        Matcher matcher = pattern.matcher(input);
        for (String substitution : substitutions) {
            expansions.add(matcher.replaceAll(substitution));
            matcher.reset();
        }

        return expansions;
    }
}
