package gg.packetloss.grindstone.util.macro;

import java.util.List;
import java.util.Set;

public class MacroExpander {
    private List<AbstractMacro> macros;

    public MacroExpander(List<AbstractMacro> macros) {
        this.macros = macros;
    }

    public Set<String> expand(String input) {
        for (AbstractMacro macro : macros) {
            if (macro.matches(input)) {
                return macro.expand(input);
            }
        }
        return Set.of(input);
    }
}
