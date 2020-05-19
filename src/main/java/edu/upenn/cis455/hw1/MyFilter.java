package edu.upenn.cis455.hw1;

import java.util.ArrayList;

import edu.upenn.cis455.hw1.interfaces.Filter;

public class MyFilter {

	private String path;
	private String acceptType;
	private Filter filter;
	
	public MyFilter(Filter filter) {
		this.filter = filter;
				
	}
	
	public String getPath() {
		return this.path;
	}
	
	public String getAcceptType() {
		return this.acceptType;
	}
	
	public Filter getFilter() {
		return this.filter;
	}
	
	public void setPath(String path) {
		this.path = path;
	}
	
	public void setAcceptType(String type) {
		this.acceptType = type;
	}
	
	public boolean matches(MyRequest myRequest, String request) {
		
		if(request.equals(path)) {
			return true;
		}
		String[] requestSplit = request.split("/");
		String[] routeSplit = path.split("/");
		
		if(requestSplit.length != routeSplit.length) {
			return false;
		}
		int length = requestSplit.length;
		
		for(int i = 0; i < length; i++) {
			String routeDef = routeSplit[i];
			String reqDef = requestSplit[i];
			
			if(routeDef.equals(reqDef) || routeDef.equals("*")) {
				continue;
			} else if(routeDef.matches(":.*")) { //param
				String param = routeDef.substring(1);
				String paramValue = reqDef; // e.g. route is /user/:name. and request is user/Alice.  Param is name and value is Alice
				if(reqDef.contains("\\?")) {
					String []query = reqDef.split("\\?");
					paramValue = query[0]; //GET user/Alice?pet=rabbit. ParamValue is ALice
					String [] queries = query[1].split("&");
					
					for(int j = 0; j < queries.length; j++) { //e.g. id=a&b=fordo
						String [] pair = queries[j].split("="); 
						//HashMap<String, List<String>> queryMap = myRequest.getQueryParamsMap();
						ArrayList<String> queryValues = (ArrayList<String>) myRequest.queryParamsValues(pair[0]);
						if(queryValues == null) {
							queryValues = new ArrayList<String>();
						}
						queryValues.add(pair[1]);
						myRequest.getQueryParamsMap().put(pair[0], queryValues);
					}					
				}
				myRequest.params().put(param, paramValue); //add param to request object without leading colons
				continue;
			}else if (reqDef.matches(".*?.*")) { //e.g. /foo/bar?num=5&id=b&name=Fordo route is /foo/bar
				String [] query = reqDef.split("\\?");
				String resource = query[0];
				if(!resource.equals(routeDef)) {
					return false;
				}
				String [] queries = query[1].split("&");
				for(int j = 0; j < queries.length; j++) { //e.g. id=a&b=fordo
					String [] pair = queries[j].split("="); 
					ArrayList<String> queryValues = (ArrayList<String>) myRequest.queryParamsValues(pair[0]);
					if(queryValues == null) {
						queryValues = new ArrayList<String>();
					}
					queryValues.add(pair[1]);
					myRequest.getQueryParamsMap().put(pair[0], queryValues);
				}
			} else {
				return false;
			}
		}
		
		return true; 
	}

}
