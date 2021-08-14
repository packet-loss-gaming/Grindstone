/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.world.type.city.area.areas.CursedMine;

import java.util.function.Predicate;

public class Ghost {
    private final String name;
    private final Predicate<SummoningContext> summonPredicate;

    public Ghost(String name, Predicate<SummoningContext> summonPredicate) {
        this.name = name;
        this.summonPredicate = summonPredicate;
    }

    public String getName() {
        return name;
    }

    public boolean shouldBeSummoned(SummoningContext context) {
        return summonPredicate.test(context);
    }
}
