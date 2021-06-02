/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util.signwall;

public interface SignWallDataBackend<T> {
    public T get(int index);
    public void set(int index, T value);
    public int size();
}
