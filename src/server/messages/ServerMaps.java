package server.messages;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ServerMaps {
	
	private Map<String, Map<String, List<String>>> userMaps;
	
	public ServerMaps() {
		this.userMaps = new ConcurrentHashMap<>();
	}
	
	public String search(String user, String keywords) {
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
	
	public void addKeyword(String username, String keyword, String file) {
		Map<String, List<String>> searchMap = userMaps.computeIfAbsent(username, k -> new ConcurrentHashMap<>());
        List<String> fileList = searchMap.computeIfAbsent(keyword, k -> Collections.synchronizedList(new ArrayList<>()));
        synchronized (fileList) {
            if (!fileList.contains(file)) {
                fileList.add(file);
            }
        }
	}
}
