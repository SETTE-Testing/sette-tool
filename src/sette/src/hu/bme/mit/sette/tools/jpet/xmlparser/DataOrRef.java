package hu.bme.mit.sette.tools.jpet.xmlparser;

import org.apache.commons.lang3.Validate;

/**
 * Denotes a &lt;data&gt; or a &lt;ref&gt; element. Please note that &lt;arg&gt;
 * elements have the same meaning as &lt;data&gt; elements (the former is used
 * in arrays, the latter in input arguments and object fields), thus &lt;arg&gt;
 * elements are handled as &lt;data&gt; elements.
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
        Validate.isTrue(isData(), "This object must be a data");
        return (Data) this;
    }

    public final Ref asRef() {
        Validate.isTrue(isRef(), "This object must be a ref");
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
