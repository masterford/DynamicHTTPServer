package edu.upenn.cis455.hw1;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import edu.upenn.cis455.hw1.interfaces.Session;

public class MySession extends Session {
	
	private HashMap<String, Object> attributeMap = new HashMap<String, Object>();
	
	private long creationTime = 0;
	private long lastAccessedTime = 0;
	private int interval;
	private String id = "";
	private Map<Integer, MySession> jSessionMap;
	
    public String id() {
      return id;
    }
    
    public long creationTime() {
      return this.creationTime;
    }
    
    public long lastAccessedTime() {
      return this.lastAccessedTime;
    }
    
    public void setCreationTime(long time) {
    	this.creationTime = time;
    }
    
    public void setLastAccessedTime(long time) {
    	this.lastAccessedTime = time;
    }
    
    public void invalidate() {
    	if(jSessionMap != null && jSessionMap.containsKey(Integer.parseInt(this.id))) {
    		this.jSessionMap.remove(Integer.parseInt(this.id));
    	}
    }
    
    public void setJSessionMap(Map<Integer, MySession> jSessionMap) {
    	access();
    	this.jSessionMap = jSessionMap;
    }
    public int maxInactiveInterval() {
    	access();
    	return this.interval;
    }
    
    public void maxInactiveInterval(int interval) {
    	access();
    	this.interval = interval;
    }
    
    public void access() {
    	this.lastAccessedTime = new Date().getTime();
    }
    
    public void attribute(String name, Object value) {
    	access();
    	attributeMap.put(name, value);
    }
    
    public Object attribute(String name) {
    	access();
      return attributeMap.getOrDefault(name, null);
    }
    
    public Set<String> attributes() {
      access();
      return attributeMap.keySet();
    }
    
    public void setId(String id) {
    	this.id = id;
    }
    
    public void removeAttribute(String name) {
    	access();
    	if(attributeMap.containsKey(name)) {
    		attributeMap.remove(name);
    	}
    }
}
