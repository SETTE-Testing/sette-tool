/*
 * SETTE - Symbolic Execution based Test Tool Evaluator
 *
 * SETTE is a tool to help the evaluation and comparison of symbolic execution based test input 
 * generator tools.
 *
 * Budapest University of Technology and Economics (BME)
 *
 * Authors: Lajos Cseppentő <lajos.cseppento@inf.mit.bme.hu>, Zoltán Micskei <micskeiz@mit.bme.hu>
 *
 * Copyright 2014-2016
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except 
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the 
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either 
 * express or implied. See the License for the specific language governing permissions and 
 * limitations under the License.
 */
package hu.bme.mit.sette.core.util;

import static com.google.common.base.Preconditions.checkState;

import java.util.Objects;

import lombok.NonNull;

/**
 * This class provides functionality for immutable whose values can be set later, but only once.
 * Until the value is not set, it is <code>null</code>. This class is useful when a filed can be
 * only set once, but declaring it <code>final</code> would be painful or the value is not even
 * known when constructing the instance.
 * <p>
 * Note: Although the class called immutable, it is only immutable after the value is set. This
 * class only ensures that the value can be set maximum one time, but overall its state (including
 * {@link #hashCode()}) will probably change when setting the value.
 * 
 * @param <T>
 *            the wrapped type
 */
public final class LazyImmutable<T> {
    /**
     * @param <T>
     *            the wrapped type
     * @return a new {@link LazyImmutable} instance whose value is not set
     */
    public static <T> LazyImmutable<T> of() {
        return new LazyImmutable<T>();
    }

    private volatile T value;

    private LazyImmutable() {
        this.value = null;
    }

    /**
     * @return <code>true</code> if the value has been already set, otherwise <code>false</code>
     */
    public synchronized boolean isSet() {
        return value != null;
    }

    /**
     * @return the stored value (<code>null</code> if not set yet)
     */
    public synchronized T get() {
        return value;
    }

    /**
     * Sets the value of the instance if possible, otherwise throws {@link IllegalStateException}.
     * 
     * @param value
     *            the value to set (must not be <code>null</code>)
     * @throws IllegalStateException
     *             if the value has been already set
     */
    public synchronized void set(@NonNull T value) {
        checkState(!isSet(), "The value has been already set");
        this.value = value;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (getClass() != obj.getClass()) {
            return false;
        }

        LazyImmutable<?> other = (LazyImmutable<?>) obj;
        return Objects.equals(value, other.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }

    @Override
    public String toString() {
        return String.format("LazyImmutable [%s]", value);
    }
}
