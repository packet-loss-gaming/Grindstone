/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util.probability;

class WeightedEntity<T> {
    private T entity;
    private int weight;

    public WeightedEntity(T entity, int weight) {
        this.entity = entity;
        this.weight = weight;
    }

    public T getEntity() {
        return entity;
    }

    public int getWeight() {
        return weight;
    }
}
