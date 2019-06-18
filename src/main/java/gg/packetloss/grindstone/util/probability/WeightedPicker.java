package gg.packetloss.grindstone.util.probability;

import gg.packetloss.grindstone.util.ChanceUtil;

import java.util.ArrayList;
import java.util.List;

public class WeightedPicker<T> {
    private List<WeightedEntity<T>> entities = new ArrayList<>();
    private int totalWeight = 0;

    public void add(T entity, int weight) {
        entities.add(new WeightedEntity<>(entity, weight));
        totalWeight += weight;
    }

    public T pick() {
        int randomValue = ChanceUtil.getRandom(totalWeight);

        for (WeightedEntity<T> weightedTicket : entities) {
            randomValue -= weightedTicket.getWeight();
            if (randomValue <= 0) {
                return weightedTicket.getEntity();
            }
        }

        throw new RuntimeException("Somehow ran out of options while trying to pick entity.");
    }
}
