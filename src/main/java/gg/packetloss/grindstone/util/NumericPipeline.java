/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util;

import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public class NumericPipeline<T> implements Function<T, Integer> {
    private final List<BiFunction<T, Integer, Integer>> elements;
    private final Supplier<Integer> sourceValue;

    private NumericPipeline(List<BiFunction<T, Integer, Integer>> elements, Supplier<Integer> sourceValue) {
        this.elements = elements;
        this.sourceValue = sourceValue;
    }

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    @Override
    public Integer apply(T info) {
        int value = sourceValue.get();
        for (var element : elements) {
            value = element.apply(info, value);
        }
        return value;
    }

    public static class Builder<T> {
        private List<BiFunction<T, Integer, Integer>> elements = new ArrayList<>();

        private Builder() { }
        private Builder(List<BiFunction<T, Integer, Integer>> elements) {
            this.elements = Lists.newArrayList(elements);
        }

        public void accept(BiFunction < T, Integer, Integer > element) {
            elements.add(element);
        }

        public Builder<T> fork() {
             return new Builder<>(elements);
        }

        public NumericPipeline<T> build(Supplier<Integer> sourceValue) {
            return new NumericPipeline<>(elements, sourceValue);
        }

        public NumericPipeline<T> build(int sourceValue) {
            return build(() -> sourceValue);
        }
    }
}
