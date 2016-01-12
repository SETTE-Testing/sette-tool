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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.Validate;

  

abstract class HeapElement {
    private String num;

    public HeapElement() {
        this(null);
    }

    public HeapElement(String num) {
        this.num = num;
    }

    public String getNum() {
        return num;
    }

    public void setNum(String num) {
        this.num = num;
    }

    public final boolean isArray() {
        return (this instanceof HeapArray);
    }

    public final boolean isObject() {
        return (this instanceof HeapObject);
    }

    public final HeapArray asHeapArray() {
        Validate.isTrue(isArray(), "This object must be a heap array");
        return (HeapArray) this;
    }

    public final HeapObject asHeapObject() {
        Validate.isTrue(isObject(), "This object must be a heap object");
        return (HeapObject) this;
    }

    final static class HeapArray extends HeapElement {
        private String type;
        private String numElems;
        private final List<DataOrRef> args;

        public HeapArray() {
            this(null);
        }

        public HeapArray(String num) {
            super(num);
            args = new ArrayList<>();
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getNumElems() {
            return numElems;
        }

        public void setNumElems(String numElems) {
            this.numElems = numElems;
        }

        public List<DataOrRef> args() {
            return args;
        }
    }

    final static class HeapObject extends HeapElement {
        private String className;
        private final List<HeapObjectField> fields;

        public HeapObject() {
            this(null);
        }

        public HeapObject(String num) {
            super(num);
            fields = new ArrayList<>();
        }

        public String getClassName() {
            return className;
        }

        public void setClassName(String className) {
            this.className = className;
        }

        public List<HeapObjectField> fields() {
            return fields;
        }

        final static class HeapObjectField {
            private String fieldName;
            private DataOrRef dataOrRef;

            public HeapObjectField() {
            }

            public String getFieldName() {
                return fieldName;
            }

            public void setFieldName(String fieldName) {
                this.fieldName = fieldName;
            }

            public DataOrRef getDataOrRef() {
                return dataOrRef;
            }

            public void setDataOrRef(DataOrRef dataOrRef) {
                this.dataOrRef = dataOrRef;
            }
        }
    }
}
