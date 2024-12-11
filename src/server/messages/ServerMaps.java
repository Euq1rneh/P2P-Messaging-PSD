package server.messages;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServerMaps {
	
	private static Map<String, Map<String, List<String>>> userMaps = new HashMap<>();
	
	public static String search(String user, String keywords) {
		Map<String, List<String>> userMap = userMaps.get(user);
		
		List<String> results = userMap.get(keywords);
		
		if (results == null || results.isEmpty()) {
			return "Keyword search yielded no results";
		}
		
		StringBuilder sb = new StringBuilder();
		for (String result : results) {
			sb.append("Keyword found in ");
			sb.append(result);
			sb.append("\n");
		}
		return sb.toString();
	}
}
