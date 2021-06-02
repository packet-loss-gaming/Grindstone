/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util.invite;

import java.util.*;

public class InviteList {
    private final Map<UUID, UUID> receiverToSender = new HashMap<>();
    private final Set<UUID> unacceptedInvites = new HashSet<>();
    private final Map<UUID, Set<UUID>> senderToReceivers = new HashMap<>();

    /**
     * Checks to see a player has been invited, and accepted.
     *
     * @param receiver the player who received the invite
     * @return true if the player was invited and accepted
     */
    public boolean containsAccepted(UUID receiver) {
        return receiverToSender.containsKey(receiver) && !unacceptedInvites.contains(receiver);
    }

    /**
     * Accepts an invite for a player.
     *
     * @param receiver the player who received the invite
     * @return true if the invite was still pending, and is now accepted.
     */
    public boolean accept(UUID receiver) {
        return unacceptedInvites.remove(receiver);
    }

    /**
     * Get the player who issued an invite.
     *
     * @param receiver the invited player
     * @return the UUID of the player who sent the invite
     */
    public UUID getIssuer(UUID receiver) {
        return receiverToSender.get(receiver);
    }

    /**
     * Invite a player.
     *
     * @param issuer the player sending the invite
     * @param invitee the player receiving the invite
     */
    public void invite(UUID issuer, UUID invitee) {
        if (receiverToSender.containsKey(invitee)) {
            revoke(invitee);
        }

        receiverToSender.put(invitee, issuer);
        senderToReceivers.putIfAbsent(issuer, new HashSet<>());
        senderToReceivers.get(issuer).add(invitee);

        unacceptedInvites.add(invitee);
    }

    /**
     * Revoke an invite.
     *
     * @param receiver the player who received an invite
     */
    public void revoke(UUID receiver) {
        UUID senderID = receiverToSender.remove(receiver);
        if (senderID == null) {
            return;
        }

        // Remove any unaccepted invites
        unacceptedInvites.remove(receiver);

        senderToReceivers.get(senderID).remove(receiver);
    }

    /**
     * Revoke all invites issued by a particular player.
     *
     * @param issuer the player who sent the invites
     * @return the list of players who were invited and accepted their invites
     */
    public List<UUID> revokeAllInvitesBy(UUID issuer) {
        Set<UUID> senderInvited = senderToReceivers.remove(issuer);
        if (senderInvited == null) {
            return List.of();
        }

        List<UUID> accepted = new ArrayList<>();
        for (UUID receiver : senderInvited) {
            // Remove any unaccepted invites, if the invite was already accepted
            // add the player to the list of players that accepted their invites.
            if (!accept(receiver)) {
                accepted.add(receiver);
            }

            receiverToSender.remove(receiver);
        }

        return accepted;
    }
}
