package edu.upenn.cis455.hw1;

import edu.upenn.cis455.hw1.interfaces.WebService;
import edu.upenn.cis455.hw1.interfaces.Route;
import edu.upenn.cis455.hw1.interfaces.Session;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import edu.upenn.cis455.hw1.interfaces.Filter;

public class MyWebService extends WebService implements Runnable { 
	private ServerSocket serverSocket;
	private String staticFileLocation;
	private int port = 1235; //default
	private String ipAddress = "127.0.0.1"; //default is localhost
	private Thread dispatcher;
	private Map<String, ArrayList<MyRoute>> routes;
	private ArrayList<MyFilter> beforeFilters;
	private ArrayList<MyFilter> afterFilters;
	private Map<Integer, MySession> jSessionMap; //Maps jsessionid to session

	private BlockingQueue queue;
	ArrayList<HttpWorker> pool;
	private int poolSize = 10;
	private volatile boolean shutdown;
	   
  public MyWebService()
  {
			pool = new ArrayList<HttpWorker>();
			routes = new HashMap<String, ArrayList<MyRoute>>();
			this.shutdown = false;
			jSessionMap = new HashMap<Integer, MySession>();
			beforeFilters = new ArrayList<MyFilter>();
			afterFilters = new ArrayList<MyFilter>();			
  }

  public void run() {
	  this.dispatcher = Thread.currentThread();
	  try {
		  InetAddress ip = InetAddress.getByName(this.ipAddress);
		serverSocket = new ServerSocket(this.port, this.poolSize, ip);
	} catch (IOException e1) {
		System.out.println("Unable to open server socket");
		e1.printStackTrace();
	}
	  queue = new BlockingQueue(poolSize);
	  
	  /*initialize Workers  */
	  	for(int i = 0; i < this.poolSize; i++) {
	  		HttpWorker worker = new HttpWorker(i, this);
	  		pool.add(worker);
	  		worker.start();
	  	}
	  	
	  	while(!this.getShutdown()) {
	  		try {
				Socket client = this.serverSocket.accept();
				this.queue.enqueue(client);			
			} catch (IOException e) {
				if(this.getShutdown()) {
					System.out.println("caught socket close exception");
					break;
				}else {
					e.printStackTrace();
					continue;
				}
				
			}
	  	}
  }
  
  public Map<String, ArrayList<MyRoute>> getRoutes(){
	
	  return this.routes;
  }
  
  public Thread getDispatcher() {
	  return this.dispatcher; //TODO: Remove
   }
  
  public BlockingQueue getQueue() {
	  return this.queue;
   }

  public boolean getShutdown() {
	  return this.shutdown;
  }
  
  public Map<Integer, MySession> getJSessionMap(){
	  return this.jSessionMap;
  }
  
  public String getStaticFileLocation() {
	  return this.staticFileLocation;
  }
  
  public int getPort() {
	  return this.port;
  }
  
  public ArrayList<HttpWorker> getPool() {
	   return this.pool;
  }
    
  public synchronized void  sendShutdown(HttpWorker worker) {
	   this.shutdown = true;
	   System.out.println("RECEIVED FROM: " + worker.getStateId());
  }
   
  public ServerSocket getSocket() {
	   return this.serverSocket;
  }
  
  public void start()
  {
	  //IGNORE. Source : piazza, cid=276
  }
    
  public void stop()
  {
	  this.shutdown = true;
	  HttpWorker worker = this.getPool().get(0); //one thread shutsdown
	  worker.sendShutdown();
  }

  public void staticFileLocation(String directory)
  {
	  this.staticFileLocation = directory;
  }

  public void get(String path, Route route)
  {
	//Register route
	  MyRoute myRoute = new MyRoute(path, route);
	  ArrayList<MyRoute> getRoutes = routes.getOrDefault("GET", new ArrayList<MyRoute>());
	  
	  getRoutes.add(myRoute);  
	  routes.put("GET", getRoutes);
  }

  public void ipAddress(String ipAddress)
  {
	  this.ipAddress = ipAddress;
  }
    
  public void port(int port)
  {
	  this.port = port;
	  
  }
    
  public void threadPool(int threads)
  {
	  
	  this.poolSize = threads;
  }
    
  public void post(String path, Route route)
  {
	  MyRoute myRoute = new MyRoute(path, route);
	  ArrayList<MyRoute> getRoutes = routes.getOrDefault("POST", new ArrayList<MyRoute>());
	  
	  getRoutes.add(myRoute);  
	  routes.put("POST", getRoutes);
  }

  public void put(String path, Route route)
  {
	  MyRoute myRoute = new MyRoute(path, route);
	  ArrayList<MyRoute> getRoutes = routes.getOrDefault("PUT", new ArrayList<MyRoute>());
	  
	  getRoutes.add(myRoute);  
	  routes.put("PUT", getRoutes);
  }

  public void delete(String path, Route route)
  {
	  MyRoute myRoute = new MyRoute(path, route);
	  ArrayList<MyRoute> getRoutes = routes.getOrDefault("DELETE", new ArrayList<MyRoute>());
	  
	  getRoutes.add(myRoute);  
	  routes.put("DELETE", getRoutes);
  }

  public void head(String path, Route route)
  {
	  MyRoute myRoute = new MyRoute(path, route);
	  ArrayList<MyRoute> getRoutes = routes.getOrDefault("HEAD", new ArrayList<MyRoute>());
	  
	  getRoutes.add(myRoute);  
	  routes.put("HEAD", getRoutes);
  }

  public void options(String path, Route route)
  {
	  MyRoute myRoute = new MyRoute(path, route);
	  ArrayList<MyRoute> getRoutes = routes.getOrDefault("OPTIONS", new ArrayList<MyRoute>());
	  
	  getRoutes.add(myRoute);  
	  routes.put("OPTIONS", getRoutes);
  }
  
  public ArrayList<MyFilter> getBeforeFilters(){
	  return this.beforeFilters;
  }
  
  public ArrayList<MyFilter> getAfterFilters(){
	  return this.afterFilters;
  }
  public void before(Filter filter)
  {
	  MyFilter myFilter = new MyFilter(filter);
	 this.beforeFilters.add(myFilter);
  }

  public void after(Filter filter)
  {
	  MyFilter myFilter = new MyFilter(filter);
		 this.afterFilters.add(myFilter);
  }
    
  public void before(String path, String acceptType, Filter filter)
  {
	  MyFilter myFilter = new MyFilter(filter);
	  myFilter.setAcceptType(acceptType);
	  myFilter.setPath(path);
	  this.beforeFilters.add(myFilter);
  }
    
  public void after(String path, String acceptType, Filter filter)
  {
	  MyFilter myFilter = new MyFilter(filter);
	  myFilter.setAcceptType(acceptType);
	  myFilter.setPath(path);
	  this.afterFilters.add(myFilter);
  }

}
