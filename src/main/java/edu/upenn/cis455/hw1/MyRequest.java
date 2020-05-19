package edu.upenn.cis455.hw1;
	
import edu.upenn.cis455.hw1.interfaces.Request;
import edu.upenn.cis455.hw1.interfaces.Session;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
	
public class MyRequest extends Request {
	private String requestMethod;
	private String host;
	private String userAgent;
	private String url;
	private String uri;
	private String protocol;
	private String contentType;
	private String ip;
	private String body;
	private String pathInfo;
	private MySession session;
	
	private HashMap<String, String> headerMap;
	private HashMap<String, Object> attributeMap = new HashMap<String, Object>();
	private Map<Integer, MySession> sessionMap;
	private Map<String, List<String>> queryParamMap = new HashMap<String, List<String>>();
	private Map<String, String> paramMap;
	private Map<String, String> cookieMap = new HashMap<String, String>();
	
	private int contentLength;
	private int port;
	private boolean newJSession = false; //variable to check whether client requested a new session
	
    public String requestMethod() {
      return this.requestMethod;
    }

    public String host() {
      return this.host;
    }
    
    public String userAgent() {
      return this.userAgent;
    }

    public int port() {
      return this.port;
    }

    public String pathInfo() {
      return this.pathInfo;
    }
    
    public String url() {
      return this.url;
}

	public String uri() {
	  return this.uri;
	}
	
	public String protocol() {
	  return this.protocol;
	}
	
	public String contentType() {
	  return this.contentType;
	}
	
	public String ip() {
	  return this.ip;
	}
	
	public String body() {
	  return this.body;
	}
	
	public int contentLength() {
	  return this.contentLength;
	}
	
	public String headers(String name) {
	  return headerMap.get(name);
	}
	
	public Set<String> headers() {
	  return headerMap.keySet();
	}
	
	public boolean persistentConnection() {
	  return persistent;
	}
	
	public Session session() {
		if(session != null) {
			this.session.setJSessionMap(sessionMap);
		}
	  
	  return this.session == null ? session(true) : this.session;
	}
	
	public void setSession(MySession session) {
		this.session = session;
	}
	
	public boolean newSession() {
		return this.newJSession;
	}
	public Session session(boolean create) {
		
	  if(create && session == null) { //create new session
		  session = new MySession();		  
		  session.setCreationTime(new Date().getTime());
		  session.access(); //session was just accessed
		  int random = new Random().nextInt();
		  while(sessionMap.containsKey(random)) { //make sure we don't generate the same key
			  random = new Random().nextInt();
		  }
		  session.setId(Integer.toString(random));
		  sessionMap.put(random, session);
		  session.setJSessionMap(sessionMap);
		  newJSession = true;
	  }
	  return session; //could be null
	}
	
	public Map<String, String> params() {
	  return this.paramMap = this.paramMap == null ? new HashMap<String, String>() : this.paramMap;
	}
	
	public String params(String param) {
		if(param == null) {
			return null;
		}
	  return paramMap.getOrDefault(param, null);
	}
	
	public void setJSessionMap(Map<Integer, MySession> jSessionMap) {
		this.sessionMap = jSessionMap;
	}
	
	public String queryParams(String param) {
	
		ArrayList<String> values = (ArrayList<String>) this.queryParamMap.getOrDefault(param, new ArrayList<String>());
		if(!values.isEmpty()) {
			return values.get(0); //return first match
		}else {
			return null;
		}	  
	}
	
	public List<String> queryParamsValues(String param) {
	  return this.queryParamMap.getOrDefault(param, null);
	}
	
	public Set<String> queryParams() {
	  return this.queryParamMap.keySet();
	}
	
	public Map<String, List<String>> getQueryParamsMap(){
		return this.queryParamMap;
	}
	public String queryString() {	 
	  String [] urlSplit = this.url.split("//?");
	  if(urlSplit.length == 2) {
		  return urlSplit[1];
	  }
	  return null;
	}
	
	public Object attribute(String attrib) {
	  return attributeMap.getOrDefault(attrib, null);
	}
	
	public void attribute(String attrib, Object val) {
		attributeMap.put(attrib, val);
	}
	
	public Set<String> attributes() {
	  return attributeMap.keySet();
	}
	
	public Map<String, String> cookies() {
			
	  return this.cookieMap;
	}
	
	public void addCookie(String cookie) {
		String [] pair = cookie.split("=");		
		this.cookieMap.put(pair[0], pair[1]);
	}
	
	public void setRequestMethod(String requestMethod) {
	   this.requestMethod = requestMethod;
	}
	
	public void setHost(String host) {
	    this.host = host;
	}
	  
	public void setUserAgent(String agent) {
	   this.userAgent = agent;
	}
	
	public void setPort(int port) {
	    this.port = port;
	}
	
	public void setPathInfo(String pathInfo) {
	   this.pathInfo = pathInfo;
    }
	  
	public void setUrl(String url) {
	    this.url = url;
	}
	  
    public void setUri(String uri) {
	    this.uri = uri;
	}
	  
	public void setProtocol(String protocol) {
	    this.protocol = protocol;
	}
	
	public void setContentType(String contentType) {
	    this.contentType = contentType;
	}
	  
	public void setIp(String ip) {
	    this.ip = ip;
	}
	
	public void setBody(String body) {
	
	    this.body = body;
	}
	
	public void setContentLength(int contentLength) {
	    this.contentLength = contentLength;
	}
	  	  	
	public void setHeaderMap(HashMap<String, String> headerMap) {
		this.headerMap = headerMap;
	}
			  
}
