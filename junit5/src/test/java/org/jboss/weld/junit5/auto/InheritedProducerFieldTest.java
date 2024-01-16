package org.jboss.weld.junit5.auto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * According to the specification, producer fields (annotated {@link Produces @Procudes}) are not inherited,
 * at least not with the effect of a producer by means of CDI.
 * <blockquote>[...] X declares a non-static producer field x then Y does not inherit this field.</blockquote>
 *
 * @see https://jakarta.ee/specifications/cdi/4.0/jakarta-cdi-spec-4.0#member_level_inheritance
 */
@EnableAutoWeld
class InheritedProducerFieldTest {

    interface Foo {
    }

    static class BaseClass {
        @Produces
        Foo baseFooProducer = new Foo() {
        };
    }

    // TODO: inner test classes are ignored anyway due to no no-args constructor
    @Nested
    class DontAddBeanClassesInheritedFromTestBaseClassNestedInnerTestClassTest extends BaseClass {
        @Test
        void test(BeanManager beanManager) {
            // base class might be found and instantiated if it had a bean defining annotation but it does not
            assertEquals(0, beanManager.getBeans(Foo.class).size());
        }
    }

    @EnableAutoWeld
    static class DontAddBeanClassesInheritedFromTestBaseClassStaticNestedTestClassTest extends BaseClass {
        @Test
        void test(BeanManager beanManager) {
            assertEquals(0, beanManager.getBeans(Foo.class).size());
        }
    }

    @Dependent
    static class SubClass extends BaseClass {
    }

    @Nested
    class DontAddBeanClassesInheritedFromBeanBaseClassTest {
        @Inject
        SubClass subClass;

        @Test
        void test(BeanManager beanManager) {
            assertEquals(0, beanManager.getBeans(Foo.class).size());
        }
    }

}
