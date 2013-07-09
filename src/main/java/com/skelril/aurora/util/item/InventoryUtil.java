package com.skelril.aurora.util.item;

import org.bukkit.event.inventory.InventoryAction;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class InventoryUtil {

    private static Set<InventoryAction> acceptedActions = new HashSet<>();

    static {
        acceptedActions.add(InventoryAction.SWAP_WITH_CURSOR);
        acceptedActions.add(InventoryAction.CLONE_STACK);

        acceptedActions.add(InventoryAction.PLACE_ALL);
        acceptedActions.add(InventoryAction.PLACE_SOME);
        acceptedActions.add(InventoryAction.PLACE_ONE);

        acceptedActions.add(InventoryAction.PICKUP_ALL);
        acceptedActions.add(InventoryAction.PICKUP_HALF);
        acceptedActions.add(InventoryAction.PICKUP_SOME);
        acceptedActions.add(InventoryAction.PICKUP_ONE);
    }

    public static Set<InventoryAction> getAcceptedActions() {

        return Collections.unmodifiableSet(acceptedActions);
    }

    private static Set<InventoryAction> duplicationActions = new HashSet<>();

    static {
        duplicationActions.add(InventoryAction.CLONE_STACK);
    }

    public static Set<InventoryAction> getDuplicationActions() {

        return Collections.unmodifiableSet(duplicationActions);
    }

    private static Set<InventoryAction> placeActions = new HashSet<>();

    static {
        placeActions.add(InventoryAction.SWAP_WITH_CURSOR);

        placeActions.add(InventoryAction.PLACE_ALL);
        placeActions.add(InventoryAction.PLACE_SOME);
        placeActions.add(InventoryAction.PLACE_ONE);
    }

    public static Set<InventoryAction> getPlaceActions() {

        return Collections.unmodifiableSet(placeActions);
    }

    private static Set<InventoryAction> pickUpActions = new HashSet<>();

    static {
        pickUpActions.add(InventoryAction.SWAP_WITH_CURSOR);

        pickUpActions.add(InventoryAction.PICKUP_ALL);
        pickUpActions.add(InventoryAction.PICKUP_HALF);
        pickUpActions.add(InventoryAction.PICKUP_ONE);
        pickUpActions.add(InventoryAction.PICKUP_SOME);
    }

    public static Set<InventoryAction> getPickUpActions() {

        return Collections.unmodifiableSet(pickUpActions);
    }
}
