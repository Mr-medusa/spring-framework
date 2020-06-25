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

package org.springframework.core.convert.support;

import org.junit.Test;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalConverter;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.core.io.DescriptiveResource;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.tests.Assume;
import org.springframework.tests.TestGroup;
import org.springframework.util.NumberUtils;
import org.springframework.util.StopWatch;
import org.springframework.util.StringUtils;

import java.awt.*;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.*;

import static java.util.Comparator.naturalOrder;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for {@link GenericConversionService}.
 *
 * <p>In this package for access to package-local converter implementations.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @author David Haraburda
 * @author Sam Brannen
 */
public class GenericConversionServiceTests {

	private final GenericConversionService conversionService = new GenericConversionService();


	@Test
	public void canConvert() {
		assertThat(conversionService.canConvert(String.class, Integer.class)).isFalse();
		conversionService.addConverterFactory(new StringToNumberConverterFactory());
		assertThat(conversionService.canConvert(String.class, Integer.class)).isTrue();
	}

	@Test
	public void canConvertAssignable() {
		assertThat(conversionService.canConvert(String.class, String.class)).isTrue();
		assertThat(conversionService.canConvert(Integer.class, Number.class)).isTrue();
		assertThat(conversionService.canConvert(boolean.class, boolean.class)).isTrue();
		assertThat(conversionService.canConvert(boolean.class, Boolean.class)).isTrue();
	}

	@Test
	public void canConvertFromClassSourceTypeToNullTargetType() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				conversionService.canConvert(String.class, null));
	}

	@Test
	public void canConvertFromTypeDescriptorSourceTypeToNullTargetType() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				conversionService.canConvert(TypeDescriptor.valueOf(String.class), null));
	}

	@Test
	public void canConvertNullSourceType() {
		assertThat(conversionService.canConvert(null, Integer.class)).isTrue();
		assertThat(conversionService.canConvert(null, TypeDescriptor.valueOf(Integer.class))).isTrue();
	}

	@Test
	public void convert() {
		conversionService.addConverter(new Converter<String, Number>() {

			@Override
			public Number convert(String source) {
				return NumberUtils.parseNumber(source, Number.class);
			}
		});
		conversionService.addConverterFactory(new StringToNumberConverterFactory());
		assertThat(conversionService.convert("3", Integer.class)).isEqualTo((int) Integer.valueOf(3));
	}

	@Test
	public void convertNullSource() {
		assertThat(conversionService.convert(null, Integer.class)).isEqualTo(null);
	}

	@Test
	public void convertNullSourcePrimitiveTarget() {
		assertThatExceptionOfType(ConversionFailedException.class).isThrownBy(() ->
				conversionService.convert(null, int.class));
	}

	@Test
	public void convertNullSourcePrimitiveTargetTypeDescriptor() {
		assertThatExceptionOfType(ConversionFailedException.class).isThrownBy(() ->
				conversionService.convert(null, TypeDescriptor.valueOf(String.class), TypeDescriptor.valueOf(int.class)));
	}

	@Test
	public void convertNotNullSourceNullSourceTypeDescriptor() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				conversionService.convert("3", null, TypeDescriptor.valueOf(int.class)));
	}

	@Test
	public void convertAssignableSource() {
		assertThat(conversionService.convert(false, boolean.class)).isEqualTo(Boolean.FALSE);
		assertThat(conversionService.convert(false, Boolean.class)).isEqualTo(Boolean.FALSE);
	}

	@Test
	public void converterNotFound() {
		assertThatExceptionOfType(ConverterNotFoundException.class).isThrownBy(() ->
				conversionService.convert("3", Integer.class));
	}

	@Test
	public void addConverterNoSourceTargetClassInfoAvailable() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				conversionService.addConverter(new UntypedConverter()));
	}

	@Test
	public void sourceTypeIsVoid() {
		assertThat(conversionService.canConvert(void.class, String.class)).isFalse();
	}

	@Test
	public void targetTypeIsVoid() {
		assertThat(conversionService.canConvert(String.class, void.class)).isFalse();
	}

	@Test
	public void convertNull() {
		assertThat(conversionService.convert(null, Integer.class)).isNull();
	}

	@Test
	public void convertToNullTargetClass() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				conversionService.convert("3", (Class<?>) null));
	}

	@Test
	public void convertToNullTargetTypeDescriptor() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				conversionService.convert("3", TypeDescriptor.valueOf(String.class), null));
	}

	@Test
	public void convertWrongSourceTypeDescriptor() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				conversionService.convert("3", TypeDescriptor.valueOf(Integer.class), TypeDescriptor.valueOf(Long.class)));
	}

	@Test
	public void convertWrongTypeArgument() {
		conversionService.addConverterFactory(new StringToNumberConverterFactory());
		assertThatExceptionOfType(ConversionFailedException.class).isThrownBy(() ->
				conversionService.convert("BOGUS", Integer.class));
	}

	@Test
	public void convertSuperSourceType() {
		conversionService.addConverter(new Converter<CharSequence, Integer>() {
			@Override
			public Integer convert(CharSequence source) {
				return Integer.valueOf(source.toString());
			}
		});
		Integer result = conversionService.convert("3", Integer.class);
		assertThat((int) result).isEqualTo((int) Integer.valueOf(3));
	}

	// SPR-8718
	@Test
	public void convertSuperTarget() {
		conversionService.addConverter(new ColorConverter());
		assertThatExceptionOfType(ConverterNotFoundException.class).isThrownBy(() ->
				conversionService.convert("#000000", SystemColor.class));
	}

	@Test
	public void convertObjectToPrimitive() {
		assertThat(conversionService.canConvert(String.class, boolean.class)).isFalse();
		conversionService.addConverter(new StringToBooleanConverter());
		assertThat(conversionService.canConvert(String.class, boolean.class)).isTrue();
		Boolean b = conversionService.convert("true", boolean.class);
		assertThat(b).isTrue();
		assertThat(conversionService.canConvert(TypeDescriptor.valueOf(String.class), TypeDescriptor.valueOf(boolean.class))).isTrue();
		b = (Boolean) conversionService.convert("true", TypeDescriptor.valueOf(String.class), TypeDescriptor.valueOf(boolean.class));
		assertThat(b).isTrue();
	}

	@Test
	public void convertObjectToPrimitiveViaConverterFactory() {
		assertThat(conversionService.canConvert(String.class, int.class)).isFalse();
		conversionService.addConverterFactory(new StringToNumberConverterFactory());
		// int.class查询转换器的时候当作包装类型处理
		assertThat(conversionService.canConvert(String.class, int.class)).isTrue();
		Integer three = conversionService.convert("3", int.class);
		assertThat(three.intValue()).isEqualTo(3);
	}

	@Test
	public void genericConverterDelegatingBackToConversionServiceConverterNotFound() {
		conversionService.addConverter(new ObjectToArrayConverter(conversionService));
		assertThat(conversionService.canConvert(String.class, Integer[].class)).isFalse();

		assertThatExceptionOfType(ConverterNotFoundException.class).isThrownBy(() ->
				conversionService.convert("3,4,5", Integer[].class));
	}

	//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	//+++++++++++++++++++++++++++++++++++++      直接转换            +++++++++++++++++++++++++++++++++++++++++++++
	//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

	@Test
	public void testListToIterableConversion() {
		List<Object> raw = new ArrayList<>();
		raw.add("one");
		raw.add("two");
		Object converted = conversionService.convert(raw, Iterable.class);
		assertThat(converted).isSameAs(raw);
	}

	@Test
	public void testListToObjectConversion() {
		List<Object> raw = new ArrayList<>();
		raw.add("one");
		raw.add("two");
		Object converted = conversionService.convert(raw, Object.class);
		assertThat(converted).isSameAs(raw);
	}

	@Test
	public void testMapToObjectConversion() {
		Map<Object, Object> raw = new HashMap<>();
		raw.put("key", "value");
		Object converted = conversionService.convert(raw, Object.class);
		assertThat(converted).isSameAs(raw);
	}

	@Test
	public void testWildcardMap() throws Exception {
		Map<String, String> input = new LinkedHashMap<>();
		input.put("key", "value");
		Object converted = conversionService.convert(input, TypeDescriptor.forObject(input), new TypeDescriptor(getClass().getField("wildcardMap")));
		assertThat(converted).isEqualTo(input);
	}

	@Test
	public void testStringToString() {
		String value = "myValue";
		String result = conversionService.convert(value, String.class);
		assertThat(result).isSameAs(value);
	}

	@Test
	public void testStringToObject() {
		String value = "myValue";
		Object result = conversionService.convert(value, Object.class);
		assertThat(result).isSameAs(value);
	}


	//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	//+++++++++++++++++++++++++++++++++++++      添加一些转换器测试            +++++++++++++++++++++++++++++++++++
	//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

	/**
	 * 这就是为什么转换器Converter<? super S,...>的原因了,我们想要实现了该接口的类也能得到转换。
	 */
	@Test
	public void testInterfaceToString() {
		conversionService.addConverter(new MyBaseInterfaceToStringConverter());
		conversionService.addConverter(new ObjectToStringConverter());
		Object converted = conversionService.convert(new MyInterfaceImplementer(), String.class);
		assertThat(converted).isEqualTo("RESULT");
	}


	//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	//+++++++++++++++++++++++++++++++++++++      数组接口综合使用            ++++++++++++++++++++++++++++++++++++++
	//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

	/**
	 * 接口数组转字符串数组
	 */
	@Test
	public void testInterfaceArrayToStringArray() {
		conversionService.addConverter(new MyBaseInterfaceToStringConverter());
		conversionService.addConverter(new ArrayToArrayConverter(conversionService));
		String[] converted = conversionService.convert(new MyInterface[]{new MyInterfaceImplementer()}, String[].class);
		assertThat(converted[0]).isEqualTo("RESULT");
	}


	/**
	 * 对象数组转字符串数组
	 * 本质同上 - 参考GenericConversionService.find(...)方法
	 */
	@Test
	public void testObjectArrayToStringArray() {
		conversionService.addConverter(new MyBaseInterfaceToStringConverter());
		conversionService.addConverter(new ArrayToArrayConverter(conversionService));
		String[] converted = conversionService.convert(new MyInterfaceImplementer[]{new MyInterfaceImplementer()}, String[].class);
		assertThat(converted[0]).isEqualTo("RESULT");
	}

	/**
	 * 字符串数组转资源数组
	 */
	@Test
	public void testStringArrayToResourceArray() {
		conversionService.addConverter(new MyStringArrayToResourceArrayConverter());
		Resource[] converted = conversionService.convert(new String[]{"x1", "z3"}, Resource[].class);
		List<String> descriptions = Arrays.stream(converted).map(Resource::getDescription).sorted(naturalOrder()).collect(toList());
		assertThat(descriptions).isEqualTo(Arrays.asList("1", "3"));
	}

	/**
	 * 字符串数组转整数数组
	 */
	@Test
	public void testStringArrayToIntegerArray() {
		conversionService.addConverter(new MyStringArrayToIntegerArrayConverter());
		Integer[] converted = conversionService.convert(new String[]{"x1", "z3"}, Integer[].class);
		assertThat(converted).isEqualTo(new Integer[]{1, 3});
	}
	/**
	 * 字符串转整数数组
	 */
	@Test
	public void testStringToIntegerArray() {
		conversionService.addConverter(new MyStringToIntegerArrayConverter());
		Integer[] converted = conversionService.convert("x1,z3", Integer[].class);
		assertThat(converted).isEqualTo(new Integer[]{1, 3});
	}


	@Test
	public void testIgnoreCopyConstructor() {
		WithCopyConstructor value = new WithCopyConstructor();
		Object result = conversionService.convert(value, WithCopyConstructor.class);
		assertThat(result).isSameAs(value);
	}

	@Test
	public void testPerformance2() throws Exception {
		Assume.group(TestGroup.PERFORMANCE);
		StopWatch watch = new StopWatch("list<string> -> list<integer> conversionPerformance");
		watch.start("convert 4,000,000 with conversion service");
		List<String> source = new LinkedList<>();
		source.add("1");
		source.add("2");
		source.add("3");
		TypeDescriptor td = new TypeDescriptor(getClass().getField("list"));
		for (int i = 0; i < 1000000; i++) {
			conversionService.convert(source, TypeDescriptor.forObject(source), td);
		}
		watch.stop();
		watch.start("convert 4,000,000 manually");
		for (int i = 0; i < 4000000; i++) {
			List<Integer> target = new ArrayList<>(source.size());
			for (String element : source) {
				target.add(Integer.valueOf(element));
			}
		}
		watch.stop();
//		 System.out.println(watch.prettyPrint());
	}

	@Test
	public void testPerformance3() throws Exception {
		Assume.group(TestGroup.PERFORMANCE);
		StopWatch watch = new StopWatch("map<string, string> -> map<string, integer> conversionPerformance");
		watch.start("convert 4,000,000 with conversion service");
		Map<String, String> source = new HashMap<>();
		source.put("1", "1");
		source.put("2", "2");
		source.put("3", "3");
		TypeDescriptor td = new TypeDescriptor(getClass().getField("map"));
		for (int i = 0; i < 1000000; i++) {
			conversionService.convert(source, TypeDescriptor.forObject(source), td);
		}
		watch.stop();
		watch.start("convert 4,000,000 manually");
		for (int i = 0; i < 4000000; i++) {
			Map<String, Integer> target = new HashMap<>(source.size());
			source.forEach((k, v) -> target.put(k, Integer.valueOf(v)));
		}
		watch.stop();
		// System.out.println(watch.prettyPrint());
	}

	/**
	 * 空集合转数组
	 */
	@Test
	public void emptyListToArray() {
		conversionService.addConverter(new CollectionToArrayConverter(conversionService));
		conversionService.addConverterFactory(new StringToNumberConverterFactory());
		List<String> list = new ArrayList<>();
		TypeDescriptor sourceType = TypeDescriptor.forObject(list);
		TypeDescriptor targetType = TypeDescriptor.valueOf(String[].class);
		assertThat(conversionService.canConvert(sourceType, targetType)).isTrue();
		assertThat(((String[]) conversionService.convert(list, sourceType, targetType)).length).isEqualTo(0);
	}

	@Test
	public void emptyListToObject() {
		conversionService.addConverter(new CollectionToObjectConverter(conversionService));
		conversionService.addConverterFactory(new StringToNumberConverterFactory());
		List<String> list = new ArrayList<>();
		TypeDescriptor sourceType = TypeDescriptor.forObject(list);
		TypeDescriptor targetType = TypeDescriptor.valueOf(Integer.class);
		assertThat(conversionService.canConvert(sourceType, targetType)).isTrue();
		assertThat(conversionService.convert(list, sourceType, targetType)).isNull();
	}

	@Test
	public void stringToArrayCanConvert() {
		conversionService.addConverter(new StringToArrayConverter(conversionService));
		assertThat(conversionService.canConvert(String.class, Integer[].class)).isFalse();
		conversionService.addConverterFactory(new StringToNumberConverterFactory());
		assertThat(conversionService.canConvert(String.class, Integer[].class)).isTrue();
	}

	/**
	 * String 转集合
	 * @throws Exception
	 */
	@Test
	public void stringToCollectionCanConvert() throws Exception {
		conversionService.addConverter(new StringToCollectionConverter(conversionService));
		assertThat(conversionService.canConvert(String.class, Collection.class)).isTrue();
		TypeDescriptor targetType = new TypeDescriptor(getClass().getField("integerCollection"));
		assertThat(conversionService.canConvert(TypeDescriptor.valueOf(String.class), targetType)).isFalse();
		conversionService.addConverterFactory(new StringToNumberConverterFactory());
		assertThat(conversionService.canConvert(TypeDescriptor.valueOf(String.class), targetType)).isTrue();
	}

	//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	//+++++++++++++++++++++++++++++++++++++      无转换器测试            +++++++++++++++++++++++++++++++++++++++++
	//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	@Test
	public void testConvertiblePairsInSet() {
		Set<GenericConverter.ConvertiblePair> set = new HashSet<>();
		set.add(new GenericConverter.ConvertiblePair(Number.class, String.class));
		assert set.contains(new GenericConverter.ConvertiblePair(Number.class, String.class));
	}

	@Test
	public void testConvertiblePairEqualsAndHash() {
		GenericConverter.ConvertiblePair pair = new GenericConverter.ConvertiblePair(Number.class, String.class);
		GenericConverter.ConvertiblePair pairEqual = new GenericConverter.ConvertiblePair(Number.class, String.class);
		assertThat(pairEqual).isEqualTo(pair);
		assertThat(pairEqual.hashCode()).isEqualTo(pair.hashCode());
	}

	@Test
	public void testConvertiblePairDifferentEqualsAndHash() {
		GenericConverter.ConvertiblePair pair = new GenericConverter.ConvertiblePair(Number.class, String.class);
		GenericConverter.ConvertiblePair pairOpposite = new GenericConverter.ConvertiblePair(String.class, Number.class);
		assertThat(pair.equals(pairOpposite)).isFalse();
		assertThat(pair.hashCode() == pairOpposite.hashCode()).isFalse();
	}

	@Test
	public void canConvertIllegalArgumentNullTargetTypeFromClass() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				conversionService.canConvert(String.class, null));
	}

	@Test
	public void canConvertIllegalArgumentNullTargetTypeFromTypeDescriptor() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				conversionService.canConvert(TypeDescriptor.valueOf(String.class), null));
	}

	//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	//+++++++++++++++++++++++++++++++++++++      无转换器测试            +++++++++++++++++++++++++++++++++++++++++
	//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++


	//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	//+++++++++++++++++++++++++++++++++++++      条件转换器测试            +++++++++++++++++++++++++++++++++++++++
	//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

	/**
	 * 若同一ConvertiblePair有多个转换器,可以使用条件转换器更精确的控制是否可转化
	 */
	@Test
	public void removeConvertible() {
		conversionService.addConverter(new ColorConverter());
		assertThat(conversionService.canConvert(String.class, Color.class)).isTrue();
		conversionService.removeConvertible(String.class, Color.class);
		assertThat(conversionService.canConvert(String.class, Color.class)).isFalse();
	}

	@Test
	public void conditionalConverter() {
		MyConditionalConverter converter = new MyConditionalConverter();
		conversionService.addConverter(new ColorConverter());
		conversionService.addConverter(converter);
		assertThat(conversionService.convert("#000000", Color.class)).isEqualTo(Color.BLACK);
		assertThat(converter.getMatchAttempts() > 0).isTrue();
	}

	@Test
	public void conditionalConverterFactory() {
		MyConditionalConverterFactory converter = new MyConditionalConverterFactory();
		conversionService.addConverter(new ColorConverter());
		conversionService.addConverterFactory(converter);
		assertThat(conversionService.convert("#000000", Color.class)).isEqualTo(Color.BLACK);
		assertThat(converter.getMatchAttempts() > 0).isTrue();
		assertThat(converter.getNestedMatchAttempts() > 0).isTrue();
	}

	@Test
	public void conditionalConverterCachingForDifferentAnnotationAttributes() throws Exception {
		conversionService.addConverter(new ColorConverter());
		conversionService.addConverter(new MyConditionalColorConverter());

		assertThat(conversionService.convert("000000xxxx",
				new TypeDescriptor(getClass().getField("activeColor")))).isEqualTo(Color.BLACK);
		assertThat(conversionService.convert(" #000000 ",
				new TypeDescriptor(getClass().getField("inactiveColor")))).isEqualTo(Color.BLACK);
		assertThat(conversionService.convert("000000yyyy",
				new TypeDescriptor(getClass().getField("activeColor")))).isEqualTo(Color.BLACK);
		assertThat(conversionService.convert("  #000000  ",
				new TypeDescriptor(getClass().getField("inactiveColor")))).isEqualTo(Color.BLACK);
	}

	@Test
	public void shouldNotSupportNullConvertibleTypesFromNonConditionalGenericConverter() {
		GenericConverter converter = new NonConditionalGenericConverter();
		assertThatIllegalStateException().isThrownBy(() ->
				conversionService.addConverter(converter))
				.withMessage("Only conditional converters may return null convertible types");
	}

	@Test
	public void conditionalConversionForAllTypes() {
		MyConditionalGenericConverter converter = new MyConditionalGenericConverter();
		conversionService.addConverter(converter);
		assertThat(conversionService.convert(3, Integer.class)).isEqualTo(3);
		assertThat(converter.getSourceTypes().size()).isGreaterThan(2);
		assertThat(converter.getSourceTypes().stream().allMatch(td -> Integer.class.equals(td.getType()))).isTrue();
	}

	@Test
	public void convertOptimizeArray() {
		// SPR-9566
		byte[] byteArray = new byte[]{1, 2, 3};
		byte[] converted = conversionService.convert(byteArray, byte[].class);
		assertThat(converted).isSameAs(byteArray);
	}

	//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	//+++++++++++++++++++++++++++++++++++++      枚举转换器            +++++++++++++++++++++++++++++++++++++++++++
	//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	@Test
	public void testEnumToStringConversion() {
		conversionService.addConverter(new EnumToStringConverter(conversionService));
		assertThat(conversionService.convert(MyEnum.A, String.class)).isEqualTo("A");
	}

	@Test
	public void testSubclassOfEnumToString() throws Exception {
		conversionService.addConverter(new EnumToStringConverter(conversionService));
		assertThat(conversionService.convert(EnumWithSubclass.FIRST, String.class)).isEqualTo("FIRST");
	}

	@Test
	public void testEnumWithInterfaceToStringConversion() {
		// SPR-9692
		conversionService.addConverter(new EnumToStringConverter(conversionService));
		conversionService.addConverter(new MyEnumInterfaceToStringConverter<MyEnum>());
		assertThat(conversionService.convert(MyEnum.A, String.class)).isEqualTo("1");
	}

	//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	//+++++++++++++++++++++++++++++++++++++      字符串转枚举            +++++++++++++++++++++++++++++++++++++++++
	//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	@Test
	public void testStringToEnumWithInterfaceConversion() {
//		conversionService.addConverterFactory(new StringToEnumConverterFactory());
		conversionService.addConverterFactory(new StringToMyEnumInterfaceConverterFactory());
		assertThat(conversionService.convert("1", MyEnum.class)).isEqualTo(MyEnum.A);
	}

	@Test
	public void testStringToEnumWithBaseInterfaceConversion() {
//		conversionService.addConverterFactory(new StringToEnumConverterFactory());
		conversionService.addConverterFactory(new StringToMyEnumBaseInterfaceConverterFactory());
		assertThat(conversionService.convert("base1", MyEnum.class)).isEqualTo(MyEnum.A);
	}

	@Test
	public void convertNullAnnotatedStringToString() throws Exception {
		String source = null;
		TypeDescriptor sourceType = new TypeDescriptor(getClass().getField("annotatedString"));
		TypeDescriptor targetType = TypeDescriptor.valueOf(String.class);
		conversionService.convert(source, sourceType, targetType);
	}


	//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	//+++++++++++++++++++++++++++++++++++++      协变            ++++++++++++++++++++++++++++++++++++++++++++++++
	//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	@Test
	public void multipleCollectionTypesFromSameSourceType() throws Exception {
		conversionService.addConverter(new MyStringToRawCollectionConverter());			// 匹配无泛型集合
		conversionService.addConverter(new MyStringToGenericCollectionConverter());		// 匹配泛型集合
		conversionService.addConverter(new MyStringToStringCollectionConverter());		// 匹配String集合
		conversionService.addConverter(new MyStringToIntegerCollectionConverter());		// 匹配整数集合

		assertThat(conversionService.convert("test", TypeDescriptor.valueOf(String.class), new TypeDescriptor(getClass().getField("stringCollection")))).isEqualTo(Collections.singleton("testX"));
		assertThat(conversionService.convert("test", TypeDescriptor.valueOf(String.class), new TypeDescriptor(getClass().getField("integerCollection")))).isEqualTo(Collections.singleton(4));
		assertThat(conversionService.convert("test", TypeDescriptor.valueOf(String.class), new TypeDescriptor(getClass().getField("rawCollection")))).isEqualTo(Collections.singleton(4));
		assertThat(conversionService.convert("test", TypeDescriptor.valueOf(String.class), new TypeDescriptor(getClass().getField("genericCollection")))).isEqualTo(Collections.singleton(4));
		assertThat(conversionService.convert("test", TypeDescriptor.valueOf(String.class), new TypeDescriptor(getClass().getField("rawCollection")))).isEqualTo(Collections.singleton(4));
		assertThat(conversionService.convert("test", TypeDescriptor.valueOf(String.class), new TypeDescriptor(getClass().getField("stringCollection")))).isEqualTo(Collections.singleton("testX"));
	}

	/**
	 * Converter<String, Collection<String>>
	 *
	 * ? 		可以协变为 String
	 * 无泛型   可以协变为 String
	 */
	@Test
	public void adaptedCollectionTypesFromSameSourceType() throws Exception {
		conversionService.addConverter(new MyStringToStringCollectionConverter());
		assertThat(conversionService.convert("test", TypeDescriptor.valueOf(String.class), new TypeDescriptor(getClass().getField("stringCollection")))).isEqualTo(Collections.singleton("testX"));
		assertThat(conversionService.convert("test", TypeDescriptor.valueOf(String.class), new TypeDescriptor(getClass().getField("genericCollection")))).isEqualTo(Collections.singleton("testX"));
		assertThat(conversionService.convert("test", TypeDescriptor.valueOf(String.class), new TypeDescriptor(getClass().getField("rawCollection")))).isEqualTo(Collections.singleton("testX"));
		assertThat(conversionService.convert("test", TypeDescriptor.valueOf(String.class), new TypeDescriptor(getClass().getField("genericCollection")))).isEqualTo(Collections.singleton("testX"));
		assertThat(conversionService.convert("test", TypeDescriptor.valueOf(String.class), new TypeDescriptor(getClass().getField("stringCollection")))).isEqualTo(Collections.singleton("testX"));
		assertThat(conversionService.convert("test", TypeDescriptor.valueOf(String.class), new TypeDescriptor(getClass().getField("rawCollection")))).isEqualTo(Collections.singleton("testX"));

		assertThatExceptionOfType(ConverterNotFoundException.class).isThrownBy(() ->
				conversionService.convert("test", TypeDescriptor.valueOf(String.class), new TypeDescriptor(getClass().getField("integerCollection"))));
	}

	/**
	 * Converter<String, Collection<?>>
	 *     都可以协变
	 */
	@Test
	public void genericCollectionAsSource() throws Exception {
		conversionService.addConverter(new MyStringToGenericCollectionConverter());

		assertThat(conversionService.convert("test", TypeDescriptor.valueOf(String.class), new TypeDescriptor(getClass().getField("stringCollection")))).isEqualTo(Collections.singleton("testX"));
		assertThat(conversionService.convert("test", TypeDescriptor.valueOf(String.class), new TypeDescriptor(getClass().getField("genericCollection")))).isEqualTo(Collections.singleton("testX"));
		assertThat(conversionService.convert("test", TypeDescriptor.valueOf(String.class), new TypeDescriptor(getClass().getField("rawCollection")))).isEqualTo(Collections.singleton("testX"));

		// The following is unpleasant but a consequence of the generic collection converter above...
		assertThat(conversionService.convert("test", TypeDescriptor.valueOf(String.class),
				new TypeDescriptor(getClass().getField("integerCollection")))).isEqualTo(Collections.singleton("testX"));
	}

	/**
	 * Converter<String, Collection>
	 *     都可以协变
	 */
	@Test
	public void rawCollectionAsSource() throws Exception {
		conversionService.addConverter(new MyStringToRawCollectionConverter());

		assertThat(conversionService.convert("test", TypeDescriptor.valueOf(String.class), new TypeDescriptor(getClass().getField("stringCollection")))).isEqualTo(Collections.singleton("testX"));
		assertThat(conversionService.convert("test", TypeDescriptor.valueOf(String.class), new TypeDescriptor(getClass().getField("genericCollection")))).isEqualTo(Collections.singleton("testX"));
		assertThat(conversionService.convert("test", TypeDescriptor.valueOf(String.class), new TypeDescriptor(getClass().getField("rawCollection")))).isEqualTo(Collections.singleton("testX"));

		// The following is unpleasant but a consequence of the raw collection converter above...
		assertThat(conversionService.convert("test", TypeDescriptor.valueOf(String.class), new TypeDescriptor(getClass().getField("integerCollection")))).isEqualTo(Collections.singleton("testX"));
	}


	//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	//+++++++++++++++++++++++++++++++++++++      内建的转换器测试            +++++++++++++++++++++++++++++++++++++
	//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	static class Foo{
//		public Bar toBar(){
//			System.out.println("toBar");
//			return new Bar();
//		}
		public static Bar findBar(MyEntity myEntity){
			System.out.println("findBar(Class<Bar> bar)");
			return new Bar();
		}

	}
	static class Bar{
		//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
		//+++++++++++++++++++++++++++++++++++++      测试Obj to Obj          +++++++++++++++++++++++++++++++++++++++++++
		//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
		public Bar(){}
//		public Bar(Foo foo) {
//			System.out.println("Bar(Foo foo)");
//		}
		//		public static Bar valueOf(Foo foo){
//			System.out.println("valueOf(Foo foo)");
//			return new Bar();
//		}
//		public static Bar of(Foo foo){
//			System.out.println("of(Foo foo)");
//			return new Bar();
//		}
//		public static Bar from(Foo foo){
//			System.out.println("from(Foo foo)");
//			return new Bar();
//		}

		//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
		//+++++++++++++++++++++++++++++++++++++      测试IdToEntity            +++++++++++++++++++++++++++++++++++++
		//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
		public static Bar findBar(MyEntity myEntity){
			System.out.println("findBar(Class<Bar> bar)");
			return new Bar();
		}

	}
	static class MyEntity{
		public String id;
		public static MyEntity of(Foo foo){
			System.out.println("MyEntity of(Foo foo)");
			return new MyEntity();
		}
	}

	@Test
	public void buildInConversions(){
		conversionService.addConverter(new ObjectToObjectConverter());
		conversionService.addConverter(new IdToEntityConverter(conversionService));
		Object convert =  conversionService.convert(new Foo(), Bar.class);
		System.out.println(convert);
	}

	@ExampleAnnotation(active = true)
	public String annotatedString;

	@ExampleAnnotation(active = true)
	public Color activeColor;

	@ExampleAnnotation(active = false)
	public Color inactiveColor;

	public List<Integer> list;

	public Map<String, Integer> map;

	public Map<String, ?> wildcardMap;

	@SuppressWarnings("rawtypes")
	public Collection rawCollection;

	public Collection<?> genericCollection;

	public Collection<String> stringCollection;

	public Collection<Integer> integerCollection;


	@Retention(RetentionPolicy.RUNTIME)
	private @interface ExampleAnnotation {

		boolean active();
	}


	private interface MyBaseInterface {
	}


	private interface MyInterface extends MyBaseInterface {
	}


	private static class MyInterfaceImplementer implements MyInterface {
	}


	private static class MyBaseInterfaceToStringConverter implements Converter<MyBaseInterface, String> {

		@Override
		public String convert(MyBaseInterface source) {
			return "RESULT";
		}
	}


	private static class MyStringArrayToResourceArrayConverter implements Converter<String[], Resource[]> {

		@Override
		public Resource[] convert(String[] source) {
			return Arrays.stream(source).map(s -> s.substring(1)).map(DescriptiveResource::new).toArray(Resource[]::new);
		}
	}


	private static class MyStringArrayToIntegerArrayConverter implements Converter<String[], Integer[]> {

		@Override
		public Integer[] convert(String[] source) {
			return Arrays.stream(source).map(s -> s.substring(1)).map(Integer::valueOf).toArray(Integer[]::new);
		}
	}


	private static class MyStringToIntegerArrayConverter implements Converter<String, Integer[]> {

		@Override
		public Integer[] convert(String source) {
			String[] srcArray = StringUtils.commaDelimitedListToStringArray(source);
			return Arrays.stream(srcArray).map(s -> s.substring(1)).map(Integer::valueOf).toArray(Integer[]::new);
		}
	}


	private static class WithCopyConstructor {

		WithCopyConstructor() {
		}

		@SuppressWarnings("unused")
		WithCopyConstructor(WithCopyConstructor value) {
		}
	}


	private static class MyConditionalConverter implements Converter<String, Color>, ConditionalConverter {

		private int matchAttempts = 0;

		@Override
		public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
			matchAttempts++;
			return false;
		}

		@Override
		public Color convert(String source) {
			throw new IllegalStateException();
		}

		public int getMatchAttempts() {
			return matchAttempts;
		}
	}


	private static class NonConditionalGenericConverter implements GenericConverter {

		@Override
		public Set<ConvertiblePair> getConvertibleTypes() {
			return null;
		}

		@Override
		@Nullable
		public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
			return null;
		}
	}


	private static class MyConditionalGenericConverter implements GenericConverter, ConditionalConverter {

		private final List<TypeDescriptor> sourceTypes = new ArrayList<>();

		@Override
		public Set<ConvertiblePair> getConvertibleTypes() {
			return null;
		}

		@Override
		public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
			sourceTypes.add(sourceType);
			return false;
		}

		@Override
		@Nullable
		public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
			return null;
		}

		public List<TypeDescriptor> getSourceTypes() {
			return sourceTypes;
		}
	}


	private static class MyConditionalConverterFactory implements ConverterFactory<String, Color>, ConditionalConverter {

		private MyConditionalConverter converter = new MyConditionalConverter();

		private int matchAttempts = 0;

		@Override
		public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
			matchAttempts++;
			return true;
		}

		@Override
		@SuppressWarnings("unchecked")
		public <T extends Color> Converter<String, T> getConverter(Class<T> targetType) {
			return (Converter<String, T>) converter;
		}

		public int getMatchAttempts() {
			return matchAttempts;
		}

		public int getNestedMatchAttempts() {
			return converter.getMatchAttempts();
		}
	}

	private static interface MyEnumBaseInterface {
		String getBaseCode();
	}


	private interface MyEnumInterface extends MyEnumBaseInterface {
		String getCode();
	}


	private enum MyEnum implements MyEnumInterface {

		A("1"),
		B("2"),
		C("3");

		private final String code;

		MyEnum(String code) {
			this.code = code;
		}

		@Override
		public String getCode() {
			return code;
		}

		@Override
		public String getBaseCode() {
			return "base" + code;
		}
	}


	private enum EnumWithSubclass {

		FIRST {
			@Override
			public String toString() {
				return "1st";
			}
		}
	}


	@SuppressWarnings("rawtypes")
	private static class MyStringToRawCollectionConverter implements Converter<String, Collection> {

		@Override
		public Collection convert(String source) {
			return Collections.singleton(source + "X");
		}
	}


	private static class MyStringToGenericCollectionConverter implements Converter<String, Collection<?>> {

		@Override
		public Collection<?> convert(String source) {
			return Collections.singleton(source + "X");
		}
	}


	private static class MyEnumInterfaceToStringConverter<T extends MyEnumInterface> implements Converter<T, String> {

		@Override
		public String convert(T source) {
			return source.getCode();
		}
	}


	private static class StringToMyEnumInterfaceConverterFactory implements ConverterFactory<String, MyEnumInterface> {

		@SuppressWarnings({"unchecked", "rawtypes"})
		public <T extends MyEnumInterface> Converter<String, T> getConverter(Class<T> targetType) {
			return new StringToMyEnumInterfaceConverter(targetType);
		}

		private static class StringToMyEnumInterfaceConverter<T extends Enum<?> & MyEnumInterface> implements Converter<String, T> {

			private final Class<T> enumType;

			public StringToMyEnumInterfaceConverter(Class<T> enumType) {
				this.enumType = enumType;
			}

			public T convert(String source) {
				for (T value : enumType.getEnumConstants()) {
					if (value.getCode().equals(source)) {
						return value;
					}
				}
				return null;
			}
		}
	}


	private static class StringToMyEnumBaseInterfaceConverterFactory implements ConverterFactory<String, MyEnumBaseInterface> {

		@SuppressWarnings({"unchecked", "rawtypes"})
		public <T extends MyEnumBaseInterface> Converter<String, T> getConverter(Class<T> targetType) {
			return new StringToMyEnumBaseInterfaceConverter(targetType);
		}

		private static class StringToMyEnumBaseInterfaceConverter<T extends Enum<?> & MyEnumBaseInterface> implements Converter<String, T> {

			private final Class<T> enumType;

			public StringToMyEnumBaseInterfaceConverter(Class<T> enumType) {
				this.enumType = enumType;
			}

			public T convert(String source) {
				for (T value : enumType.getEnumConstants()) {
					if (value.getBaseCode().equals(source)) {
						return value;
					}
				}
				return null;
			}
		}
	}


	private static class MyStringToStringCollectionConverter implements Converter<String, Collection<String>> {

		@Override
		public Collection<String> convert(String source) {
			return Collections.singleton(source + "X");
		}
	}


	private static class MyStringToIntegerCollectionConverter implements Converter<String, Collection<Integer>> {

		@Override
		public Collection<Integer> convert(String source) {
			return Collections.singleton(source.length());
		}
	}


	@SuppressWarnings("rawtypes")
	private static class UntypedConverter implements Converter {

		@Override
		public Object convert(Object source) {
			return source;
		}
	}


	private static class ColorConverter implements Converter<String, Color> {

		@Override
		public Color convert(String source) {
			return Color.decode(source.trim());
		}
	}


	private static class MyConditionalColorConverter implements Converter<String, Color>, ConditionalConverter {

		@Override
		public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
			ExampleAnnotation ann = targetType.getAnnotation(ExampleAnnotation.class);
			return (ann != null && ann.active());
		}

		@Override
		public Color convert(String source) {
			return Color.decode(source.substring(0, 6));
		}
	}
}
