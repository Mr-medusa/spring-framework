package org.springframework.hgh;

import java.util.HashMap;
import java.util.Map;

public class SimpleAliasRegistryTest {
	private static Map<String, String> aliasMap = new HashMap<>();

	static {
		aliasMap.put("A", "B");
		aliasMap.put("B", "Cd");
		aliasMap.put("Cd", "K");
		aliasMap.put("C", "K");
		aliasMap.put("K", "Q");

	}

	public static void main(String[] args) {
//		SimpleAliasRegistry s = new SimpleAliasRegistry();
//		s.registerAlias("A","B");
//		s.registerAlias("B","C");
//		s.registerAlias("C","D");
//
//		s.registerAlias("E","A"); // for->AE找到B for->BE找到C for->CE找到D for->DE说明没找到或循环结束了
//
//		s.registerAlias("D","A");		// C A
		System.out.println(checkForAliasCircle("A", "C"));
	}


	public static boolean checkForAliasCircle(String key, String value) {
		for (Map.Entry<String, String> entry : aliasMap.entrySet()) {
			if (entry.getKey().equals(key)) {	// 把entry.getValue()替换为key仍能在keys中查询得到,说明链接未断,则继续替换该key的value,直到查出个结果为止
				return (entry.getValue().equals(value) || checkForAliasCircle(entry.getValue(), value));
			}
		}
		return false;
	}
}



















