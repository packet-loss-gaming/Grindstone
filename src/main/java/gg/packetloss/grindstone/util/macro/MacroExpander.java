package gg.packetloss.grindstone.util.macro;

import java.util.List;

public class MacroExpander {
    private List<AbstractMacro> macros;

    public MacroExpander(List<AbstractMacro> macros) {
        this.macros = macros;
    }

    public List<String> expand(String input) {
        for (AbstractMacro macro : macros) {
            if (macro.matches(input)) {
                return macro.expand(input);
            }
        }
        return List.of(input);
    }
}
