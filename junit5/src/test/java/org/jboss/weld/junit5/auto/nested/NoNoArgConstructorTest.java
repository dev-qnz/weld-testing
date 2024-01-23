package org.jboss.weld.junit5.auto.nested;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Objects;

import org.jboss.weld.junit5.ExplicitParamInjection;
import org.jboss.weld.junit5.auto.EnableAutoWeld;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

@EnableAutoWeld
@ExplicitParamInjection
class NoNoArgConstructorTest {

    {
        System.out.println("(no-cdi) new NoNoArgConstructorTest() = " + this);
    }

    interface ExplicitlyProducedBean {
        default String identity() {
            // use hashCode to verify same instance in assertSame thereby ignoring proxies
            return "identity" + Objects.hashCode(this);
        }
    }

    static ExplicitlyProducedBean theExplicitlyProducedBean = new ExplicitlyProducedBean() {{
        System.out.println("(no-cdi) new ExplicitlyProducedBean() = " + this);
    }};

//    NoNoArgConstructorTest(TestInfo testInfo) {
//        System.out.println("testinfo = " + testInfo);
//    }

    @Produces
    ExplicitlyProducedBean produceExplicitlyProducedBean() {
        System.out.println("(cdi) produceExplicitlyProducedBean = " + theExplicitlyProducedBean);
        return theExplicitlyProducedBean;
    }

    @Inject
    Provider<ExplicitlyProducedBean> injectedExplicitlyProducedBeanInstance;
    
    void disposeBean(@Disposes ExplicitlyProducedBean explicitlyProducedBean) {
        assertEquals(theExplicitlyProducedBean.identity(), explicitlyProducedBean.identity());
    }
    
    @Dependent
    static class AutoBean {
//        @Inject ExplicitlyProducedBean bean;
//        Provider<ExplicitlyProducedBean> bean;
//        
        @Inject
        Provider<NoNoArgConstructorTest> testInstanceProvider;
        
        String identity() {
            // use hashCode to verify same instance in assertSame thereby ignoring proxies
            return "identity" + Objects.hashCode(this);
        }
    }

    @Inject
    AutoBean injectedAutoBean;
    
//    @Inject
//    void setInjectedBeanSetter(Bean injectedBean) {
//        setInjectedBean = injectedBean;
//    }

    @Test
    void testFieldInstanceInjected() {
        assertEquals(theExplicitlyProducedBean.identity(), injectedExplicitlyProducedBeanInstance.get().identity());
        assertNotNull(injectedAutoBean);
    }

    @Test
    void testTestInstanceInjected() {
        NoNoArgConstructorTest x = injectedAutoBean.testInstanceProvider.get();
        System.out.println("injectedAutoBean.testInstanceProvider.get() = " + x);
        assertEquals(NoNoArgConstructorTest.this, x);
    }

//    @Test
//    void testFieldInjected() {
//        assertEquals(theBean.identity(), injectedBean.identity());
//    }

//    @Test
//    void testInjectedBeanSetter() {
//        assertEquals(theBean.identity(), setInjectedBean.identity());
//    }

//    @Test
//    void testParameterInjected(@Default Bean injectedBean) {
//        assertEquals(theBean.identity(), injectedBean.identity());
//    }

    interface ExplicitlyProducedNestedBean {
        default String identity() {
            // use hashCode to verify same instance in assertSame thereby ignoring proxies
            return "identity" + Objects.hashCode(this);
        }
    }

    static ExplicitlyProducedNestedBean theExplicitlyProducedNestedBean = new ExplicitlyProducedNestedBean() {{
        System.out.println("(no-cdi) new ExplicitlyProducedNestedBean() = " + this);
    }};

    @Nested
    class NestedTest {

        {
            System.out.println("(no-cdi) new NoNoArgConstructorTest.NestedTest() = " + this);
        }
        
        
//        @Test
//        void test() {
//            
//        }
        
        
        

        @Produces
        ExplicitlyProducedNestedBean produceExplicitlyProducedNestedBean() {
            System.out.println("(cdi) produceExplicitlyProducedNestedBean = " + theExplicitlyProducedNestedBean);
            return theExplicitlyProducedNestedBean;
        }

        @Inject
        Provider<ExplicitlyProducedNestedBean> injectedExplicitlyProducedNestedBeanInstance;
        
        void disposeBean(@Disposes ExplicitlyProducedNestedBean explicitlyProducedNestedBean) {
            assertEquals(theExplicitlyProducedNestedBean.identity(), explicitlyProducedNestedBean.identity());
        }

        @Inject
        AutoBean nestedInjectedAutoBean;
        
        @Test
        void testFieldInstanceInjected() {
            assertEquals(theExplicitlyProducedBean.identity(), injectedExplicitlyProducedBeanInstance.get().identity());
            assertNotNull(injectedAutoBean);
            assertNotNull(nestedInjectedAutoBean);
            assertNotEquals(injectedAutoBean, nestedInjectedAutoBean);
        }
        
        @Test
        void testFieldInstanceInjectedProducedByNestedProducer() {
            System.out.println("injectedExplicitlyProducedNestedBeanInstance = " + injectedExplicitlyProducedNestedBeanInstance);
            assertEquals(theExplicitlyProducedNestedBean.identity(), injectedExplicitlyProducedNestedBeanInstance.get().identity());
        }

    }

}
