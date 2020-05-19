package edu.upenn.cis455.hw1;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.upenn.cis455.hw1.interfaces.Response;

public class MyResponse extends Response {
			
	private HashMap<String, String> responseHeaders = new HashMap<String, String>();
	private Map<String, MyCookie> cookieMap = new HashMap<String, MyCookie>();
	private List<MyCookie> removedCookies = new ArrayList<MyCookie>();
	private String redirectLocation;
	public boolean hasRedirect = false;
		
    public String getHeaders() {
      return responseHeaders.toString();
    }

    public void header(String header, String value) {
    	responseHeaders.put(header, value);
    }
    
    public void redirect(String location) {
    	this.redirectLocation = location;
    	this.hasRedirect = true;
    }
    
    public String getRedirect() {
    	return this.redirectLocation;
    }
    
    public void redirect(String location, int httpStatusCode) {
    	this.redirectLocation = location;
    	super.status(httpStatusCode);
    	this.hasRedirect = true;
    }
    
    public void cookie(String name, String value) {
    	MyCookie cookie = new MyCookie();
    	cookie.setCookie(name, value);
    	cookieMap.put(name, cookie);
    }
    
    public void cookie(String name, String value, int maxAge) {
    	MyCookie cookie = new MyCookie();
    	cookie.setAge(maxAge);
    	cookie.setCookie(name, value);
    	cookieMap.put(name, cookie);
    }

    public void cookie(String path, String name, String value) {
    	MyCookie cookie = new MyCookie();
    	cookie.setPath(path);
    	cookie.setCookie(name, value);
    	cookieMap.put(name, cookie);
    }
    
    public void cookie(String path, String name, String value, int maxAge) {
    	MyCookie cookie = new MyCookie();
    	cookie.setPath(path);
    	cookie.setCookie(name, value);
    	cookieMap.put(name, cookie);
    }

    public void removeCookie(String name) {
    	MyCookie remove = cookieMap.remove(name);
    	if(remove != null) {
    		removedCookies.add(remove);
    	}
    }
    
    public void removeCookie(String path, String name) {
    	MyCookie remove = cookieMap.remove(name);
    	if(remove != null) {
    		remove.setPath(path);
    		removedCookies.add(remove);
    	}    	
    }
    
    public  Map<String, MyCookie> getCookies() {
    	return this.cookieMap;
    }
    public ArrayList<MyCookie> removedCookies() { //returns list of all removed cookie objects
    	return (ArrayList<MyCookie>) removedCookies;
    }
    
    public String statusLine(int status) {
    	String s = null;
    	switch(status) {
    		case 200:
    			s = "HTTP/1.1 200 OK\r\n";
    			break;
    		case 404:
    			s = "HTTP/1.1 404 NOT FOUND\r\n";
    			break;
    		case 302:
    			s = "HTTP/1.1 302 FOUND\r\n";
    			break;
    		case 303:
    			s = "HTTP/1.1 303 See Other\r\n";
    			break;
    		case 307:
    			s = "HTTP/1.1 307 TemporaryRedirect\r\n";
    			break;
    		case 304:
    			s = "HTTP/1.1 304 NotModified\r\n";
    			break;
    		case 401:
    			s = "HTTP/1.1 401 Unauthorized\r\n";
    			break;
    		case 500:
    			s = "HTTP/1.1 500 Internal server Error\r\n";
    			break;
    		default:
    			s = "HTTP/1.1 " + status + " \r\n";
    			break;
    	}   	
    	return s;
    }
}
