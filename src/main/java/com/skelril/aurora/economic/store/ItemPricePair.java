package com.skelril.aurora.economic.store;

public class ItemPricePair implements Comparable<ItemPricePair> {

    private String name;
    private double price;

    public ItemPricePair(String name, double price) {

        this.name = name;
        this.price = price;
    }

    public String getName() {

        return name;
    }

    public double getPrice() {

        return price;
    }

    public double getSellPrice() {

        return price > 100000 ? price * .92 : price * .80;
    }

    @Override
    public int compareTo(ItemPricePair record) {

        if (record == null) return -1;

        if (this.getPrice() == record.getPrice()) {
            int c = String.CASE_INSENSITIVE_ORDER.compare(this.getName(), record.getName());
            return c == 0 ? 1 : c;
        }

        return this.getPrice() > record.getPrice() ? 1 : -1;
    }
}
