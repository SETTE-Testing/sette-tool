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
