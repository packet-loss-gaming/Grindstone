/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.warps;

import gg.packetloss.grindstone.util.NamespaceConstants;

import java.util.UUID;

public class WarpQualifiedName {
    private final UUID namespace;
    private final String name;

    public WarpQualifiedName(String name) {
        this(NamespaceConstants.GLOBAL, name);
    }

    public WarpQualifiedName(UUID namespace, String name) {
        this.namespace = namespace;
        this.name = name.toUpperCase();
    }

    public boolean isGlobal() {
        return namespace.equals(NamespaceConstants.GLOBAL);
    }

    public UUID getNamespace() {
        return namespace;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof WarpQualifiedName)) {
            return false;
        }

        WarpQualifiedName other = (WarpQualifiedName) o;
        return namespace.equals(other.namespace) && name.equalsIgnoreCase(other.name);
    }

    @Override
    public int hashCode() {
        return namespace.hashCode() + name.hashCode();
    }

    @Override
    public String toString() {
        return getNamespace() + ":" + getName();
    }
}
