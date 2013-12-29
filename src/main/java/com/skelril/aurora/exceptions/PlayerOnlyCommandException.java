package com.skelril.aurora.exceptions;

import com.sk89q.minecraft.util.commands.CommandException;

/**
 * Created by wyatt on 12/28/13.
 */
public class PlayerOnlyCommandException extends CommandException {

    public PlayerOnlyCommandException() {
        super("You must be a player to use this command.");
    }
}
