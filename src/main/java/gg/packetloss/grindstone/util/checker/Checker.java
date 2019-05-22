/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util.checker;

public abstract class Checker<T, K> implements Expression<K, Boolean> {

  private T main;

  public Checker(T main) {
    this.main = main;
  }

  public T get() {
    return main;
  }
}
