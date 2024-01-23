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
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jboss.weld.injection.ForwardingInjectionTarget;
import org.jboss.weld.util.bean.ForwardingBeanAttributes;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanAttributes;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.InjectionTarget;
import jakarta.enterprise.inject.spi.InjectionTargetFactory;
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

    public TestInstanceInjectionExtension(List<?> testInstances) {
        this.testInstances = testInstances;
    }

    @SuppressWarnings({"unchecked", "rawtypes"}) // TODO
    void afterBeanDiscovery(@Observes AfterBeanDiscovery afterBeanDiscovery, BeanManager beanManager) {
        testInstances.forEach(testInstance -> {
            Class<?> testClass = testInstance.getClass();
            
            
            AnnotatedType<?> annotatedType = beanManager.createAnnotatedType(testClass);
            BeanAttributes<?> beanAttributes = new ForwardingBeanAttributes<Object>() {
                @Override
                protected BeanAttributes<Object> attributes() {
                    return (BeanAttributes) beanManager.createBeanAttributes(annotatedType);
                }

                @Override
                public Set<Annotation> getQualifiers() {
                    return Stream.concat(attributes().getQualifiers().stream(), Stream.of(Default.Literal.INSTANCE)).collect(Collectors.toSet());
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
        });
    }
    
}
