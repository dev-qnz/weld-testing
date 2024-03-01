/*
 * JBoss, Home of Professional Open Source
 * Copyright 2018, Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.weld.junit5.auto;

import java.lang.annotation.Annotation;
import java.util.LinkedList;
import java.util.List;

import org.jboss.weld.bean.ForwardingBean;
import org.jboss.weld.bean.SyntheticProducerBean;
import org.jboss.weld.injection.ForwardingInjectionTarget;
import org.jboss.weld.util.bean.ForwardingBeanAttributes;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.AfterTypeDiscovery;
import jakarta.enterprise.inject.spi.AnnotatedField;
import jakarta.enterprise.inject.spi.AnnotatedMethod;
import jakarta.enterprise.inject.spi.AnnotatedParameter;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanAttributes;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.inject.spi.InjectionTarget;
import jakarta.enterprise.inject.spi.InjectionTargetFactory;
import jakarta.enterprise.inject.spi.PassivationCapable;
import jakarta.enterprise.inject.spi.ProducerFactory;
import jakarta.inject.Singleton;

/**
 * Extension that makes a test instance appear like a regular bean even though instantiated by JUnit.
 *
 * ???:
 * Injection into all test instances, also {@link org.junit.jupiter.api.Nested &#064;Nested} ones, is handled in
 * {@link org.jboss.weld.junit5.WeldInitiator#addObjectsToInjectInto} and related.
 */
public class TestInstanceInjectionExtension implements Extension {

    private final List<?> testInstances;
    private List<Class<?>> enabledAlternatives = new LinkedList<>();

    public TestInstanceInjectionExtension(List<?> testInstances) {
        this.testInstances = testInstances;
    }

    public void addEnabledAlternativeClass(Class<?> enabledAlternativeClass) {
        enabledAlternatives.add(enabledAlternativeClass);
    }
    
    void afterTypeDiscovery(@Observes AfterTypeDiscovery afterTypeDiscovery) {
        afterTypeDiscovery.getAlternatives().addAll(enabledAlternatives);
    }
    
    @SuppressWarnings({"unchecked", "rawtypes"}) // TODO
    void afterBeanDiscovery(@Observes AfterBeanDiscovery afterBeanDiscovery, BeanManager beanManagerToWorkAround) {
        
        BeanManager beanManager = beanManagerToWorkAround; // ((ManagedBean<?>) beanManagerToWorkAround.resolve(beanManagerToWorkAround.getBeans(BeanManagerWorkaroundBean.class))).getBeanManager();
        testInstances.forEach(testInstance -> {
            Class<?> testClass = testInstance.getClass();
            
            
            AnnotatedType<?> annotatedType = beanManager.createAnnotatedType(testClass);
            BeanAttributes<?> beanAttributes = new ForwardingBeanAttributes<Object>() {
                @Override
                protected BeanAttributes<Object> attributes() {
                    return (BeanAttributes) beanManager.createBeanAttributes(annotatedType);
                }

                @Override
                public Class<? extends Annotation> getScope() {
                    return Singleton.class;
                }
            };
            
            InjectionTargetFactory<?> injectionTargetFactory = new InjectionTargetFactory<Object>() {
                @Override
                public InjectionTarget<Object> createInjectionTarget(Bean<Object> bean) {
                    return new ForwardingInjectionTarget() {
                        @Override
                        protected InjectionTarget<Object> delegate() {
                            return beanManager.getInjectionTargetFactory(annotatedType).createInjectionTarget((Bean) bean);
                        }

                        @Override
                        public Object produce(CreationalContext creationalContext) {
                            return testInstance;
                        }
                    };
                }
            };
            
            Bean<?> bean = beanManager.createBean((BeanAttributes) beanAttributes, testClass, (InjectionTargetFactory) injectionTargetFactory);
            afterBeanDiscovery.addBean(bean);
            
            annotatedType.getFields().forEach(annotatedField -> {
                if (annotatedField.getAnnotation(Produces.class) != null && testClass.equals(annotatedField.getJavaMember().getDeclaringClass())) {
                    BeanAttributes<?> annotatedFieldBeanAttributes = beanManager.createBeanAttributes(annotatedField);
                    ProducerFactory<?> producerFactory = beanManager.getProducerFactory((AnnotatedField) annotatedField, (Bean) bean);
                    Bean<?> producerBean = beanManager.createBean((BeanAttributes) annotatedFieldBeanAttributes, testClass, (ProducerFactory) producerFactory);
                    PassivationCapableSyntheticProducerBean passivationCapableProducerBean = new PassivationCapableSyntheticProducerBean((SyntheticProducerBean<?, ?>) producerBean);
                    afterBeanDiscovery.addBean(passivationCapableProducerBean);
                }
            });
            
            annotatedType.getMethods().forEach(annotatedMethod -> {
                if (annotatedMethod.getAnnotation(Produces.class) != null && testClass.equals(annotatedMethod.getJavaMember().getDeclaringClass())) {
                    BeanAttributes<?> producerMethodBeanAttributes = beanManager.createBeanAttributes(annotatedMethod);
                    ProducerFactory<?> producerFactory = beanManager.getProducerFactory((AnnotatedMethod) annotatedMethod, (Bean) bean);
                    Bean<?> producerBean = beanManager.createBean((BeanAttributes) producerMethodBeanAttributes, testClass, (ProducerFactory) producerFactory);
                    PassivationCapableSyntheticProducerBean passivationCapableProducerBean = new PassivationCapableSyntheticProducerBean((SyntheticProducerBean<?, ?>) producerBean);
                    afterBeanDiscovery.addBean(passivationCapableProducerBean);
                }
            });
            
            annotatedType.getMethods().forEach(annotatedMethod -> {
                boolean isObserverMethod = annotatedMethod.getParameters().stream().filter(param -> {
                    return param.getAnnotation(Observes.class) != null || param.getAnnotation(ObservesAsync.class) != null;
                }).findAny().isPresent();
                if (isObserverMethod) {
                    afterBeanDiscovery.addObserverMethod()
                            .read(annotatedMethod)
                            .notifyWith(eventContext -> {
                                CreationalContext<?> creationalContext = beanManager.createCreationalContext(null);
                                try {
                                    Object[] args = annotatedMethod.getParameters().stream().map(param -> {
                                        boolean isObserverParam = param.getAnnotation(Observes.class) != null || param.getAnnotation(ObservesAsync.class) != null;
                                        if (isObserverParam) {
                                            return eventContext.getEvent();
                                        } else {
                                            InjectionPoint injectionPoint = beanManager.createInjectionPoint((AnnotatedParameter) param);
                                            Object arg = beanManager.getInjectableReference(injectionPoint, creationalContext);
                                            return arg;
                                        }
                                    }).toArray();
    
                                    System.out.println("notifying observer method with event = " + annotatedMethod + " " + eventContext.getEvent());
                                    annotatedMethod.getJavaMember().setAccessible(true);
                                    annotatedMethod.getJavaMember().invoke(testInstance, args);
                                } finally {
                                    creationalContext.release();
                                }
                            })
                            ;
                }
            });
            
            
            
        });
    }
    
    static class PassivationCapableSyntheticProducerBean<T, X> extends ForwardingBean<T> implements PassivationCapable {

        private final SyntheticProducerBean<T, X> delegate;
        
        PassivationCapableSyntheticProducerBean(SyntheticProducerBean<T, X> delegate) {
            this.delegate = delegate;
        }

        @Override
        public SyntheticProducerBean<T, X> delegate() {
            return delegate;
        }

        @Override
        public String getId() {
            return delegate().getId();
        }
    }

}
