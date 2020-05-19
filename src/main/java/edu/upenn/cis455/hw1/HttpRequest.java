package edu.upenn.cis455.hw1;
import java.util.*;

enum Method
{
	GET,
	POST,
	PUT,
	DELETE,
	OPTIONS,
	HEAD;	
}

public class HttpRequest {
	
	private Map<String, String> headerMap; 
	private Set<String> headers;
	private Method method;
	private String invalidHeader = "Not Found";
	private String resource;
	private int specification; //HTTP specification, e.g. 1.0 or 1.1
	private String requestUrl;
	private String protocol;
	
	public HttpRequest(String requestLine, String relativeDirectory) {
		
		headerMap = new HashMap<String, String>();
		headers = new HashSet<String>();
		
		String [] requests = requestLine.split(" ");
		if (requests[0].equals("GET")) {
			this.method = Method.GET;
		} else if(requests[0].equals("HEAD")) {
			this.method = Method.HEAD;
		}else if (requests[0].equals("POST")){			
			this.method = Method.POST;
		} else if(requests[0].equals("DELETE")) {
			this.method = Method.DELETE;
		} else if(requests[0].equals("OPTIONS")) {
			this.method = Method.OPTIONS;
		} else if(requests[0].equals("PUT")) {
			this.method = Method.PUT;
		}else{
			this.method = null;
		}
		
		if (requests.length == 3) {
			this.requestUrl = requests[1];
			resource = requests[1].startsWith(relativeDirectory) ? requests[1].substring(relativeDirectory.length(), requests[1].length()) : requests[1];
			specification = (requests[2].equals("HTTP/1.1")) ? 1 : 0;		
			protocol = requests[2];
		} else {
			this.method = null;
		}
		
	}
	
	public boolean isValidRequestMethod() {
		return this.method != null;
	}
	
	public void parseHeader(String line) {
		String[] split = line.split(":", 2); //split once
		if(split.length == 2) {
			headers.add(split[0]); //header name
			headerMap.put(split[0], split[1]);
		}
	}
	
	public Method getRequestType() {
		return this.method;
	}
	
	public String getRequestUrl() {
		return this.requestUrl;
	}
	public String getHost() {
		return headerMap.getOrDefault("Host", invalidHeader);
	}
	
	public String getDate() {
		return headerMap.getOrDefault("Date", invalidHeader);
	}
	
	public String getIfModified() {
		return headerMap.getOrDefault("If-Modified-Since", invalidHeader);
	}
	
	public String getUserAgent() {
		return headerMap.getOrDefault("User-Agent", invalidHeader);
	}
	
	public String getIfUnModified() {
		return headerMap.getOrDefault("If-Unmodified-Since", invalidHeader);
	}
	
	public String getContentLength() {
		return headerMap.getOrDefault("Content-Length", null);
	}
	
	public String getContentType() {
		return headerMap.getOrDefault("Content-Type", invalidHeader);
	}
	public String getConnection() {
		return headerMap.getOrDefault("Connection", invalidHeader);
	}
	
	public String getAccept() {
		return headerMap.getOrDefault("Accept", null);
	}
	
	public String getCookie() {
		return headerMap.getOrDefault("Cookie", null);
	}
	public String getResource() {
		
		if(this.resource.startsWith("http")) {
			int index = resource.indexOf('/', 7);
			return this.resource.substring(index, this.resource.length());
		}
		return this.resource;
	}
	
	public Map<String, String> getHeaderMap(){
		return this.headerMap;
	}
	
	public Set<String> getHeaders(){
		return this.headers;
	}
	
	public String getProtocol(){
		return this.protocol;
	}
	
	public int getSpecification() {
		return this.specification;
	}
	
	public String printHeaders() {
		return headerMap.keySet().toString();
	}
	
	public String printAll() {
		StringBuffer result = new StringBuffer();
		for (String key : headerMap.keySet()) {
			result.append(key + ": " + headerMap.get(key) + "\n");		
		}
		return result.toString();
	}
	
	
}
