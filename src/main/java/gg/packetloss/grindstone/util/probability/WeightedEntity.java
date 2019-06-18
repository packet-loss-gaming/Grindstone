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
