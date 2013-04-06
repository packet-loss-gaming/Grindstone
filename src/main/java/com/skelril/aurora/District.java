package com.skelril.aurora;
/**
 * Author: Turtle9598
 */
public enum District {

    GLOBAL("City", new String[]{"Dark_Arc", "RichRick18", "digilue"}),
    CARPE_DIEM("Carpe Diem", new String[]{"darkkrai13", "XxSerionAllorxX"}),
    GLACIES_MARE("Glacies Mare", new String[]{"1alonzo4"}),
    OBLITUS("Oblitus", new String[]{"Dark_Arc"}),
    VINEAM("Vineam", new String[]{"RichRick18"});

    private final String properName;
    private final String[] managers;

    District(String properName, String[] managers) {

        this.properName = properName;
        this.managers = managers;
    }

    public String toProperName() {

        return properName;
    }

    public String[] getManagers() {

        return managers;
    }

    public String getManagersFriendly() {

        String managersString = "";
        for (int i = 0; i < managers.length; i++) {
            if (i + 1 == managers.length) {
                managersString += managers[i];
            } else {
                managersString += managers[i] + ", ";
            }
        }
        return managersString;
    }
}
