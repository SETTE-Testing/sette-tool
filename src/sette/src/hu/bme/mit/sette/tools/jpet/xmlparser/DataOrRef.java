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
 * Copyright 2014-2015
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
// NOTE revise this file
package hu.bme.mit.sette.tools.jpet.xmlparser;

import com.google.common.base.Preconditions;

/**
 * Denotes a &lt;data&gt; or a &lt;ref&gt; element. Please note that &lt;arg&gt; elements have the
 * same meaning as &lt;data&gt; elements (the former is used in arrays, the latter in input
 * arguments and object fields), thus &lt;arg&gt; elements are handled as &lt;data&gt; elements.
 */
abstract class DataOrRef {
    private String text;

    public DataOrRef() {
        this(null);
    }

    public DataOrRef(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public final boolean isData() {
        return (this instanceof Data);
    }

    public final boolean isRef() {
        return (this instanceof Ref);
    }

    public final Data asData() {
        Preconditions.checkState(isData(), "This object must be a data");
        return (Data) this;
    }

    public final Ref asRef() {
        Preconditions.checkState(isRef(), "This object must be a ref");
        return (Ref) this;
    }

    final static class Data extends DataOrRef {
        public Data() {
            this(null);
        }

        public Data(String text) {
            super(text);
        }
    }

    final static class Ref extends DataOrRef {
        public Ref() {
            this(null);
        }

        public Ref(String text) {
            super(text);
        }
    }
}
