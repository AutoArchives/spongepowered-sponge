/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.common.world.generation.feature;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.data.persistence.DataQuery;
import org.spongepowered.api.data.persistence.DataView;
import org.spongepowered.api.world.generation.feature.Feature;
import org.spongepowered.api.world.generation.feature.FeatureType;

import java.util.Objects;

public final class SpongeFeatureBuilder implements Feature.Builder {

    private static final DataQuery TYPE = DataQuery.of("type");

    private @Nullable FeatureType type;
    private @Nullable Feature prototype;

    public SpongeFeatureBuilder() {
        this.reset();
    }

    @Override
    public Feature.Builder type(final FeatureType type) {
        this.type = type;
        return this;
    }

    @Override
    public Feature.Builder reset() {
        this.type = null;
        this.prototype = null;
        return this;
    }

    @Override
    public Feature.Builder from(final Feature feature) {
        this.prototype = feature;
        this.type = null;
        return this;
    }

    @Override
    public Feature build() {
        final Feature source = Objects.requireNonNull(this.prototype, "feature");
        // Round-tripping produces a distinct instance, which registries require: they reject
        // a value that is already registered under another key by identity.
        final DataView config = source.toContainer();
        config.remove(SpongeFeatureBuilder.TYPE);
        return (this.type == null ? source.type() : this.type).configure(config);
    }
}
