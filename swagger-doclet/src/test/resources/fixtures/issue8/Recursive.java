package fixtures.issue8;

import com.fasterxml.jackson.annotation.JsonSubTypes;

@SuppressWarnings("javadoc")
public interface Recursive {
    @JsonSubTypes({
            @JsonSubTypes.Type(value = AImpl.class)
    })
    abstract class A {
    }

    class AImpl extends A {
        public B b;
    }

    @JsonSubTypes({
            @JsonSubTypes.Type(value = BImpl.class)
    })
    abstract class B {
    }

    class BImpl extends B {
        public A a;
    }
}