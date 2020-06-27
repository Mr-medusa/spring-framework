/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.beans.factory.support;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.tests.sample.beans.DerivedTestBean;
import org.springframework.tests.sample.beans.TestBean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 04.07.2006
 */
public class DefaultSingletonBeanRegistryTests {

	@Test
	public void testSingletons() {
		DefaultSingletonBeanRegistry beanRegistry = new DefaultSingletonBeanRegistry();

		TestBean tb = new TestBean();
		beanRegistry.registerSingleton("tb", tb);
		assertThat(beanRegistry.getSingleton("tb")).isSameAs(tb);

		TestBean tb2 = (TestBean) beanRegistry.getSingleton("tb2", new ObjectFactory<Object>() {
			@Override
			public Object getObject() throws BeansException {
				return new TestBean();
			}
		});
		assertThat(beanRegistry.getSingleton("tb2")).isSameAs(tb2);

		assertThat(beanRegistry.getSingleton("tb")).isSameAs(tb);
		assertThat(beanRegistry.getSingleton("tb2")).isSameAs(tb2);
		assertThat(beanRegistry.getSingletonCount()).isEqualTo(2);
		String[] names = beanRegistry.getSingletonNames();
		assertThat(names.length).isEqualTo(2);
		assertThat(names[0]).isEqualTo("tb");
		assertThat(names[1]).isEqualTo("tb2");

		beanRegistry.destroySingletons();
		assertThat(beanRegistry.getSingletonCount()).isEqualTo(0);
		assertThat(beanRegistry.getSingletonNames().length).isEqualTo(0);
	}

	@Test
	public void testDisposableBean() {
		DefaultSingletonBeanRegistry beanRegistry = new DefaultSingletonBeanRegistry();

		DerivedTestBean tb = new DerivedTestBean();
		beanRegistry.registerSingleton("tb", tb);
		beanRegistry.registerDisposableBean("tb", tb);
		assertThat(beanRegistry.getSingleton("tb")).isSameAs(tb);

		assertThat(beanRegistry.getSingleton("tb")).isSameAs(tb);
		assertThat(beanRegistry.getSingletonCount()).isEqualTo(1);
		String[] names = beanRegistry.getSingletonNames();
		assertThat(names.length).isEqualTo(1);
		assertThat(names[0]).isEqualTo("tb");
		assertThat(tb.wasDestroyed()).isFalse();

		beanRegistry.destroySingletons();
		assertThat(beanRegistry.getSingletonCount()).isEqualTo(0);
		assertThat(beanRegistry.getSingletonNames().length).isEqualTo(0);
		assertThat(tb.wasDestroyed()).isTrue();
	}

	@Test
	public void testDependentRegistration() {
		DefaultSingletonBeanRegistry beanRegistry = new DefaultSingletonBeanRegistry();

		beanRegistry.registerDependentBean("a", "b");
		beanRegistry.registerDependentBean("b", "c");
		beanRegistry.registerDependentBean("c", "b");
		assertThat(beanRegistry.isDependent("a", "b")).isTrue();
		assertThat(beanRegistry.isDependent("b", "c")).isTrue();
		assertThat(beanRegistry.isDependent("c", "b")).isTrue();
		assertThat(beanRegistry.isDependent("a", "c")).isTrue();
		assertThat(beanRegistry.isDependent("c", "a")).isFalse();
		assertThat(beanRegistry.isDependent("b", "a")).isFalse();
		assertThat(beanRegistry.isDependent("a", "a")).isFalse();
		assertThat(beanRegistry.isDependent("b", "b")).isTrue();
		assertThat(beanRegistry.isDependent("c", "c")).isTrue();
	}

	@Test
	public void destroyDependentBean() {
		DefaultSingletonBeanRegistry beanRegistry = new DefaultSingletonBeanRegistry();
		beanRegistry.registerDependentBean("A", "B");
		beanRegistry.registerDependentBean("B", "C");
		beanRegistry.registerDependentBean("C", "D");

		beanRegistry.registerDependentBean("C", "F");
		beanRegistry.registerDependentBean("C", "G");
		beanRegistry.registerDependentBean("E", "G");            // D->F->G|C无依赖|->C->A->B->E

		// dependenciesForBeanMap
		Assertions.assertThat(beanRegistry.getDependenciesForBean("C")).isEqualTo(new String[]{"B"});
		Assertions.assertThat(beanRegistry.getDependenciesForBean("D")).isEqualTo(new String[]{"C"});

		// dependentBeanMap
		Assertions.assertThat(beanRegistry.getDependentBeans("C")).isEqualTo(new String[]{"D", "F", "G"});


		addDisposableBean(beanRegistry, "G", "F", "E", "D", "C", "B", "A");
		beanRegistry.destroySingletons();
	}

	@Test
	public void destroyCircleDependentBean() {
		DefaultSingletonBeanRegistry beanRegistry = new DefaultSingletonBeanRegistry();
		beanRegistry.registerDependentBean("A", "B");
		beanRegistry.registerDependentBean("B", "C");
		beanRegistry.registerDependentBean("C", "D");
		beanRegistry.registerDependentBean("D", "A");


		addDisposableBean(beanRegistry, "D", "C", "B", "A");
		System.out.println("containedBeanMap = " + beanRegistry.getContainedBeanMap());
		System.out.println("dependentBeanMap = " + beanRegistry.getDependentBeanMap());
		System.out.println("dependenciesForBeanMap = " + beanRegistry.getDependenciesForBeanMap());
		beanRegistry.destroySingletons();
	}

	@Test
	public void destroyDependentBeanWithContained() {
		DefaultSingletonBeanRegistry beanRegistry = new DefaultSingletonBeanRegistry();
		beanRegistry.registerContainedBean("A", "B");
		beanRegistry.registerContainedBean("B", "C");
		beanRegistry.registerContainedBean("C", "D");
		beanRegistry.registerContainedBean("D", "E");
		beanRegistry.registerContainedBean("F", "E");


		addDisposableBean(beanRegistry, "F", "E", "D", "C", "B", "A");
		System.out.println("containedBeanMap = " + beanRegistry.getContainedBeanMap());
		System.out.println("dependentBeanMap = " + beanRegistry.getDependentBeanMap());
		System.out.println("dependenciesForBeanMap = " + beanRegistry.getDependenciesForBeanMap());
		beanRegistry.destroySingletons();
	}

	public void addDisposableBean(DefaultSingletonBeanRegistry beanRegistry, String... beanName) {
		for (String name : beanName) {
			beanRegistry.registerDisposableBean(name, new DisposableBeanImpl(name, beanRegistry));
		}
	}

	private static class DisposableBeanImpl implements DisposableBean {
		private String name;
		private DefaultSingletonBeanRegistry registry;

		public DisposableBeanImpl(String name, DefaultSingletonBeanRegistry registry) {
			this.name = name;
			this.registry = registry;
		}

		@Override
		public void destroy() throws Exception {
			System.out.println("----------" + name + "----------");
			System.out.println("containedBeanMap = " + this.registry.getContainedBeanMap());
			System.out.println("dependentBeanMap = " + this.registry.getDependentBeanMap());
			System.out.println("dependenciesForBeanMap = " + this.registry.getDependenciesForBeanMap());
		}
	}

}









































