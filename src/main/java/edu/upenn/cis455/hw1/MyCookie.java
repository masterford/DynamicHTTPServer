package edu.upenn.cis455.hw1;

/*Simple Data Structure to store Cookie Information  */
public class MyCookie {

	private String name;
	private String value;
	private String path;
	private int maxAge;
	
	public MyCookie() {
		
	}
	
	public String getName() {	
		return this.name;
	}
	
	public String getValue() {
		return this.value;
	}
	
	public String getPath() {
		return this.path;
	}
	
	public void setCookie(String name, String value) {
		this.name = name;
		this.value = value;
	}
	
	public void setAge(int maxAge) {
		this.maxAge = maxAge;
	}
	
	public int getMaxAge() {
		return this.maxAge;
	}
	
	public void setPath(String path) {
		this.path = path;
	}
	
}
