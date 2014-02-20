/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.util.item;

public class BaseItem {

    private int id;
    private int data;

    /**
     * Construct the object.
     *
     * @param id ID of the item
     */
    public BaseItem(int id) {

        this.id = id;
        this.data = 0;
    }

    /**
     * Construct the object.
     *
     * @param id   ID of the item
     * @param data data value of the item
     */
    public BaseItem(int id, int data) {

        this.id = id;
        this.data = data;
    }

    /**
     * Get the type of item.
     *
     * @return the id
     */
    public int getType() {

        return id;
    }

    /**
     * Get the type of item.
     *
     * @param id the id to set
     */
    public void setType(int id) {

        this.id = id;
    }

    /**
     * Gets the raw data value
     *
     * @return the raw data
     */
    public int getRawData() {

        return data;
    }

    /**
     * Get the data value.
     *
     * @return the data
     */
    public int getData() {

        return data < 0 ? 0 : data;
    }

    /**
     * Set the data value.
     *
     * @param data the damage to set
     */
    public void setData(int data) {

        this.data = data;
    }

    /**
     * Checks whether the type ID and data value are equal.
     */
    public boolean equalsExact(BaseItem o) {

        return o != null && getType() == o.getType() && getData() == o.getData();
    }

    /**
     * Checks if the type is the same, and if data is the same if only data != -1.
     *
     * @param o other block
     * @return true if equal
     */
    @Override
    public boolean equals(Object o) {

        return o instanceof BaseItem && (getType() == ((BaseItem) o).getType())
                && (getRawData() == ((BaseItem) o).getRawData() || getRawData() == -1 || ((BaseItem) o).getRawData() == -1);
    }
}