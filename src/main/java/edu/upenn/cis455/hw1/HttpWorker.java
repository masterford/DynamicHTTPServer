package edu.upenn.cis455.hw1;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import edu.upenn.cis455.hw1.interfaces.HaltException;
public class HttpWorker extends Thread{
	
	   private String relativeDirectory;
	   private int port;
	   private  BlockingQueue queue;
	   private String CLRF = "\r\n";
	   private volatile boolean isShutdown;
	   private Socket client;
	   private String shutdownCommand;
	   private String control;
	   private MyWebService service;
	   private String state;
	   private String date;
	   private String invalidHeader = "Not Found";
	   private String closeHeader = "Connection:  close\r\n";
	   private Map<String, ArrayList<MyRoute>> routeMap;
	   private Map<Integer, MySession> jSessionMap;
	   private MyRequest myRequest;
	   private boolean isDynamic = false;
	   private String acceptType; 
	   private OutputStream clientStream;
	   	   
	   private int id;
   
	   public HttpWorker(int id, MyWebService service) {
		   this.service = service;
		   this.id = id;
		   jSessionMap = service.getJSessionMap();
		 //  port = service.po;
		   relativeDirectory = service.getStaticFileLocation();
		   if (relativeDirectory == null) { //static file location not specified by user
			   relativeDirectory = "/home/cis455";
		   }
		   
		   routeMap = service.getRoutes();
		   isShutdown = false;
		   queue = service.getQueue();
		   shutdownCommand = "/shutdown";
		   control = "/control";
	   }
	   
	   public void run() {
		   while(!isShutdown) {
			    this.state = String.format("Thread %d : waiting", this.id); 
			    try {			    	
					client = queue.dequeue();
				} catch (InterruptedException e1) {
					System.out.println("Thread received shutdown Interrupt");
					service.sendShutdown(this);
					this.finish();
					break;
				}
			    this.state = String.format("Thread %d : Received Connection", this.id); 
					// Read HTTP request from the client socket
			try {
					InputStream inputStream = client.getInputStream();
					clientStream = client.getOutputStream();
					String line = ""; 
					byte[] requestBody = null;
					
					int b;
					StringBuilder requestBuilder = new StringBuilder();
					
					/*First Parse requestLine*/
					while((b = inputStream.read()) != -1) {
						char c = (char) b;
						requestBuilder.append(Character.toString(c));
						if ( c == '\n') { //end of line
							 line = requestBuilder.toString();
							 System.out.print(line);
							 requestBuilder.delete(0, line.length()); //flush 
							 break;
						}
					} 
					
					date = "Date: " + new Date().toString() + "\r\n";
					HttpRequest request = new HttpRequest(line, relativeDirectory);
					this.state = String.format("Thread %d : %s", this.id, request.getRequestUrl());
					
					/*Parse HttpHeaders from Client */
					while((b = inputStream.read()) != -1) {
						char c = (char) b;
						requestBuilder.append(Character.toString(c));											
						if ( c == '\n') { //end of line	
							line = requestBuilder.toString();							
							 request.parseHeader(line);
							 System.out.print(line);
							 requestBuilder.delete(0, line.length()); //flush
							 						 							  							
						}
						if(line.isBlank()) { //end of stream
							if(request.getContentLength() != null) { //body found								 
								 int size =  Integer.parseInt(request.getContentLength().trim());
								 requestBody = new byte[size];
								 inputStream.read(requestBody); //read body
							 }	
							break;
						}
					} 
					
					Method method = request.getRequestType();
					if(method == null) { //Invalid Request
						HttpResponse response = new HttpResponse(405);
						clientStream.write(response.getStatusline().getBytes());
						//clientStream.write(date.getBytes());
						//clientStream.write(closeHeader.getBytes());
						//clientStream.write(this.CLRF.getBytes());
						
						clientStream.close();
						client.close();
						inputStream.close();
						continue;								
					}  
					
					acceptType = request.getAccept();
					myRequest = new MyRequest();
					myRequest.setPort(client.getPort());
					myRequest.setIp(new String (client.getInetAddress().toString().split("/")[1]));					
					myRequest.setJSessionMap(jSessionMap); //Give request object access to global jObjectMap
					myRequest.setUrl(request.getRequestUrl());
					myRequest.setUri(request.getRequestUrl().split("\\?")[0]); //uri is the url without query string //source:cid=298
					myRequest.setProtocol(request.getProtocol());
					myRequest.setUserAgent(request.getUserAgent());
					myRequest.setPathInfo(request.getResource());
					myRequest.setHeaderMap((HashMap<String, String>) request.getHeaderMap());
					
					if(request.getCookie() != null) {
						String cookieString = request.getCookie().trim();
						myRequest.addCookie(cookieString);
					}
					
					if(request.getRequestType() == null) { //invalid request
						HttpResponse response = new HttpResponse(405);
						clientStream.write(response.getStatusline().getBytes());
						clientStream.write(date.getBytes());
						clientStream.write(closeHeader.getBytes());
						clientStream.write(this.CLRF.getBytes());
						
						clientStream.close();
						client.close();
						inputStream.close();
						continue;		
					}
					String methodString = request.getRequestType().toString();
					
					if(request.getContentType() != null) {
						myRequest.setContentType(request.getContentType());	
					}								
					if(request.getContentLength()!= null) {
						myRequest.setContentLength(Integer.parseInt(request.getContentLength().trim()));
					}	
					if(requestBody != null) {
						myRequest.setBody(new String(requestBody, "UTF-8"));
					}
					
					/* Populate requestObject with current information */
					myRequest.setRequestMethod(methodString);
					if(methodString.equals("POST") || methodString.equals("PUT")) { //parse request body
						if(request.getContentType() != null && request.getContentType().trim().equals("application/x-www-form-urlencoded")) { //form data
							String [] queries = myRequest.body().trim().split("&");
							for(int j = 0; j < queries.length; j++) { //e.g. id=a&b=fordo
								String [] pair = queries[j].split("="); 
								ArrayList<String> queryValues = (ArrayList<String>) myRequest.queryParamsValues(pair[0]);
								if(queryValues == null) {
									queryValues = new ArrayList<String>();
								}
								queryValues.add(pair[1]);
								myRequest.getQueryParamsMap().put(pair[0], queryValues);
							}
						}						
					}
										
					if(request.getHeaderMap().containsKey("Cookie")) { /*Check if we have a session for this cookie Id already and if we do request returns same session object  */
						String headerVal = request.getHeaderMap().get("Cookie").trim();
						String [] cookiePair = headerVal.split("=");
						String cookieid = cookiePair[1]; //i.e. Cookie: JSESSIONID=xxx means id will be xxx
						//myRequest						
						if(cookiePair[0].equals("JSESSIONID")) {
							try {
								int id = Integer.parseInt(cookieid);
								MySession result = jSessionMap.getOrDefault(id, null);
								if(result != null) {
									myRequest.setSession(result);
								}
							}catch(NumberFormatException e) {
								e.printStackTrace();
							}
						}										
					}
																																		
					ArrayList<MyRoute> routes = routeMap.getOrDefault(methodString, new ArrayList<MyRoute>());										
					
					for (MyRoute route : routes) {
						if(route.matches(myRequest, request.getResource())) {
							System.out.println("FOUND MATCHING ROUTE");
							handleDynamic(route, request.getResource(), clientStream);
							inputStream.close();
							isDynamic = true;
							break;
						}
					}
					
					if(isDynamic) { //restart the loop
						inputStream.close();
						continue;
					}
					
					if(request.getResource().equals(shutdownCommand)) { //shutdown
						inputStream.close();					   
						isShutdown = true;
						this.shutdown(service.getPool(), clientStream);						
						break;
					}
					
					if(request.getResource().equals(control)) { //control
						String response = "HTTP/1.1 200 OK\r\n"; 
						clientStream.write(response.getBytes());
						clientStream.write(date.getBytes());
						String body = getControlPanel();
						String length =  "Content-Length: " + Integer.toString(body.length()) + "\r\n";
						String type = "Content-type: text/html \r\n";
						clientStream.write(type.getBytes());
						clientStream.write(length.getBytes());
						clientStream.write(closeHeader.getBytes());
						clientStream.write(this.CLRF.getBytes());
						clientStream.write(body.getBytes());
							
						inputStream.close();
						clientStream.close();
						client.close();
						continue;
					}
															                                                    
					String resource = relativeDirectory + request.getResource();
					System.out.println("Resource is " + resource);
					File file = new File(resource);
					
					
					if(!file.exists()) {	
						if(!client.isClosed()) {
							System.out.println("file not found");
							HttpResponse response = new HttpResponse(404);
							clientStream.write(response.getStatusline().getBytes());
							clientStream.write(date.getBytes());
							clientStream.write(closeHeader.getBytes());
							clientStream.write(this.CLRF.getBytes());
							
							clientStream.close();
							client.close();
							inputStream.close();
							continue;
						}										
					} 
					if(!request.getIfModified().equals("Not Found")) { //check for presence of If Modified Header
						DateFormat format = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.ENGLISH);
						try {
							Date dateIf = format.parse(request.getIfModified());
							//String comp = new Date().toString();
							Date ref = format.parse("00:00:00 GMT, January 1, 1970");
							
							long last_modified = file.lastModified();
							long difference = dateIf.getTime() - ref.getTime();
							
							if(last_modified <= difference) { //File has not been modified since given date
								HttpResponse response = new HttpResponse(304);
								clientStream.write(response.getStatusline().getBytes());
								clientStream.write(date.getBytes());
								clientStream.write(closeHeader.getBytes());
								clientStream.write(this.CLRF.getBytes());
								
								client.close();
								clientStream.close();
								inputStream.close();
								continue;
							}
						} catch (ParseException e) {
							e.printStackTrace();
							HttpResponse response = new HttpResponse(400); //couldn't parse date.
							clientStream.write(response.getStatusline().getBytes());
							clientStream.write(date.getBytes());
							clientStream.write(closeHeader.getBytes());
							clientStream.write(this.CLRF.getBytes());
							
							client.close();
							inputStream.close();
							continue;							
						}
					}
					
					if(request.getSpecification() == 1 && request.getHost().equals(invalidHeader)) { //No Host Header found in this http1.1 request
						HttpResponse response = new HttpResponse(404);
						clientStream.write(response.getStatusline().getBytes());
						clientStream.write(date.getBytes());
						clientStream.write(closeHeader.getBytes());
						clientStream.write(this.CLRF.getBytes());
						
						client.close();
						inputStream.close();
						clientStream.close();
						continue;
					}

						HttpResponse response = new HttpResponse(file, resource, relativeDirectory, port);
						
						if(response.getResult() < 0) { //error occurred
							clientStream.write(response.getStatusline().getBytes());
							clientStream.write(date.getBytes());
							clientStream.write(closeHeader.getBytes());
							clientStream.write(this.CLRF.getBytes());
						}
						else if(response.hasMessage()) { //reading a file
							clientStream.write(response.getStatusline().getBytes());
							clientStream.write(date.getBytes());
							clientStream.write(response.getType().getBytes());
							clientStream.write(response.getContentLength().getBytes());
							clientStream.write(closeHeader.getBytes());
							clientStream.write(this.CLRF.getBytes());
							
							if(method == Method.GET) {
							clientStream.write(response.getBody());
							}
						} else {
							clientStream.write(response.getStatusline().getBytes());
							clientStream.write(date.getBytes());
							clientStream.write(this.CLRF.getBytes());
							if(method == Method.GET) {
								clientStream.write(response.getBody());
							}
						}									
						client.close();
						inputStream.close();
						clientStream.close();
															  
			   } catch (SocketException s) {
				   if(this.isShutdown) {
					   System.out.println("Shutdown");
					   break;
				   } else {
					   s.printStackTrace();
				   }
			   } catch (SocketTimeoutException s) {					
					System.out.println("Socket Timed Out");	
			   } catch (IOException e) {
				   e.printStackTrace();
				    this.finish();
				    break;					
				}
		   }  
		   return;
	   }
	   
	   /*Invoked when matching route is found */
	   public void handleDynamic(MyRoute route, String requestString, OutputStream clientStream ) {		
		   MyResponse myResponse = new MyResponse();
		   
		   /*Check before filters */
		   beforeFilter(myRequest,myResponse,  clientStream);
		   /*Fill default Values */
		   myResponse.type("text/html"); //default type		   
		   myResponse.header("Date: ", new Date().toString() + "\r\n"); //set date header //TODO: print all headers
		   
		   String cLength = "";
		   try {
			Object body = route.getRoute().handle(myRequest, myResponse);
			if(body != null) {
				myResponse.body(body.toString());
				cLength = "Content-Length: " + body.toString().length() + "\r\n";
			}else {
				if(myResponse.bodyRaw() != null) {
					cLength = "Content-Length: " + myResponse.bodyRaw().length + "\r\n"; 
				}			
			}
			String cType = "Content-Type: " + myResponse.type() + "\r\n";
			//String responseDate = myResponse.
			if(myResponse.hasRedirect && myResponse.status() == 200) { //client did not set status code
				myResponse.status(302);				
			}
			
			/*Check After Filter  */
			afterFilter(myRequest, myResponse, clientStream);
	
			/*Send Headers */
			clientStream.write(myResponse.statusLine(myResponse.status()).getBytes());
			clientStream.write(date.getBytes());
			clientStream.write(cType.getBytes());
			clientStream.write(cLength.getBytes());
			clientStream.write(closeHeader.getBytes());
			
			if(myRequest.newSession()) { //Send JSessionId Cookie
				System.out.println("set cookie: " + myRequest.session().id());
				String setCookie = String.format("Set-Cookie: JSESSIONID=%s\r\n", myRequest.session().id());
				clientStream.write(setCookie.getBytes());
			}
			
			for(MyCookie cookie : myResponse.removedCookies()) { //removed cookies
				String path, name, value;			
				path = cookie.getPath();
				name = cookie.getName();
				value = cookie.getValue();
				
				String setCookie = String.format("Set-Cookie: %s=%s", name,value);
				if(path != null) {
					setCookie += " Path=" + path;
				}	
				Date past = new Date(new Date().getTime() - (1000000)); //set date in the past
				setCookie += " Expires=" + past.toString() + "\r\n";
				clientStream.write(setCookie.getBytes());
			}
			
			for(MyCookie cookie : myResponse.getCookies().values()) { //write Out cookies sent by clients
				String path, name, value;
				int maxAge;				
				path = cookie.getPath();
				name = cookie.getName();
				value = cookie.getValue();
				maxAge = cookie.getMaxAge();
				String setCookie = String.format("Set-Cookie: %s=%s", name,value);
				if(path != null) {
					setCookie += " Path=" + path;
				}
				if(maxAge != 0) {
					setCookie += " Max-Age=" + maxAge;
				}
				setCookie += "\r\n";
				clientStream.write(setCookie.getBytes());
			}
			if(myResponse.hasRedirect) {
				String location = "Location: " + myResponse.getRedirect() + "\r\n";
				clientStream.write(location.getBytes());
			}
			clientStream.write(this.CLRF.getBytes());
			
			if(!myRequest.requestMethod().equals("HEAD")) { //don't output body if it's a head request
				if(body != null) {
					clientStream.write(body.toString().getBytes());
				} else if(myResponse.bodyRaw() != null) {
					clientStream.write(myResponse.bodyRaw());
				}		
			}
			clientStream.close();			
		} catch (HaltException halt) {
			String statusLine = myResponse.statusLine(halt.statusCode());
			String cType = null;
			
			if(halt.body() != null) {
				 cType = "Content-Type: text/html\r\n";
				 cLength = "Content-Length: " + halt.body().length() + "\r\n";
			}
			/*Send Headers */
			try {
				clientStream.write(statusLine.getBytes());
				clientStream.write(date.getBytes());
				if(cType != null) {
					clientStream.write(cType.getBytes());
					clientStream.write(cLength.getBytes());
				}				
				clientStream.write(closeHeader.getBytes());
				if(halt.body() != null) {
					clientStream.write(halt.body().getBytes());
				}
			} catch (IOException e) {
				e.printStackTrace();
			}						
		} catch(SocketException s) {
			if(this.isShutdown) {
				System.out.println("server shutdown");
			} else {
				s.printStackTrace();
			}
		} catch (Exception e) {
			e.printStackTrace();
			String statusLine = myResponse.statusLine(500); //internal server error
			try {
				clientStream.write(statusLine.getBytes());
				clientStream.write(date.getBytes());
				clientStream.write(closeHeader.getBytes());
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			
		}		   
	  }
	   
	   public void beforeFilter(MyRequest myRequest, MyResponse myResponse, OutputStream clientStream) {
		   /*Check before filters */
		   ArrayList<MyFilter> beforeFilters = this.service.getBeforeFilters();
		   for(MyFilter myFilter : beforeFilters) {
			   try {
				   if(myFilter.getPath() == null && myFilter.getAcceptType() ==null) {
					   
						myFilter.getFilter().handle(myRequest, myResponse);
					} else {
						if(myFilter.matches(myRequest, myRequest.pathInfo())) {
							if(acceptType != null && acceptType.contains(myFilter.getAcceptType())) {
								myFilter.getFilter().handle(myRequest, myResponse);
							}							
						}
					}
			   }
			   catch (HaltException halt) {
				   String statusLine = myResponse.statusLine(halt.statusCode());
					String cType = null;
					String cLength = null;
					if(halt.body() != null) {
						 cType = "Content-Type: text/html\r\n";
						 cLength = "Content-Length: " + halt.body().length() + "\r\n";
					}
					/*Send Headers */
					try {
						clientStream.write(statusLine.getBytes());
						clientStream.write(date.getBytes());
						if(cType != null && cLength != null) {
							clientStream.write(cType.getBytes());
							clientStream.write(cLength.getBytes());
						}				
						clientStream.write(closeHeader.getBytes());
						if(halt.body() != null) {
							clientStream.write(halt.body().getBytes());
						}
					} catch (IOException e) {
						e.printStackTrace();
					}	
				} catch (Exception e) {
					e.printStackTrace();
					String statusLine = myResponse.statusLine(500); //internal server error
					try {
						clientStream.write(statusLine.getBytes());
						clientStream.write(date.getBytes());
						clientStream.write(closeHeader.getBytes());
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
		   }
	   }
	   
	   public void afterFilter(MyRequest myRequest, MyResponse myResponse, OutputStream clientStream) {
		   /*Check before filters */
		   ArrayList<MyFilter> afterFilters = this.service.getAfterFilters();
		   for(MyFilter myFilter : afterFilters) {
			   try {
				   if(myFilter.getPath() == null && myFilter.getAcceptType() ==null) {
					   
						myFilter.getFilter().handle(myRequest, myResponse);
					} else {
						if(myFilter.matches(myRequest, myRequest.pathInfo())) {
							if(acceptType != null && acceptType.contains(myFilter.getAcceptType())) {
								myFilter.getFilter().handle(myRequest, myResponse);
							}							
						}
					}
			   }
			   catch (HaltException halt) {
				   String statusLine = myResponse.statusLine(halt.statusCode());
					String cType = null;
					String cLength = null;
					if(halt.body() != null) {
						 cType = "Content-Type: text/html\r\n";
						 cLength = "Content-Length: " + halt.body().length() + "\r\n";
					}
					/*Send Headers */
					try {
						clientStream.write(statusLine.getBytes());
						clientStream.write(date.getBytes());
						if(cType != null && cLength != null) {
							clientStream.write(cType.getBytes());
							clientStream.write(cLength.getBytes());
						}				
						clientStream.write(closeHeader.getBytes());
						if(halt.body() != null) {
							clientStream.write(halt.body().getBytes());
						}
					} catch (IOException e) {
						e.printStackTrace();
					}	
				} catch (Exception e) {
					e.printStackTrace();
					String statusLine = myResponse.statusLine(500); //internal server error
					try {
						clientStream.write(statusLine.getBytes());
						clientStream.write(date.getBytes());
						clientStream.write(closeHeader.getBytes());
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
		   }
	   }
	   	   
	   /*Special URL to display control panel : Worker Threads + Thread State */
	   public String getControlPanel() {
			  StringBuilder builder = new StringBuilder();
			  builder.append("<!DOCTYPE html>");
			  builder.append("<html>");
			  builder.append("<head>");
			  builder.append("</head>");
			  builder.append("<body>");
			  builder.append("<h1>Control Panel</h1>");
			  builder.append("<h2> *** Author: Ransford Antwi (ransford) ***</h2>");
			 
				for(HttpWorker worker: service.getPool()) {
				  builder.append(String.format("<ul><li>%s: %s</li></ul>", worker.getThreadState(), worker.getState()));
			  }
			  builder.append(String.format("<button onclick=\"window.location.href = 'http://localhost:%d/shutdown';\">Shutdown</button>", service.getPort()));
			  builder.append("</body>");
			  builder.append("</html>");
				
			  return builder.toString();
		   }
	   
	   public void sendShutdown() { //receive shutdown signal from the server
		   shutdown(service.getPool(), clientStream);
	   }
	   /*Special URL to shutdown the server. Exits all worker threads before finally closing the server socket */
	   public synchronized void shutdown(ArrayList<HttpWorker> threadPool, OutputStream clientStream) {
			System.out.println("Thread received shutdown Command");
			this.isShutdown = true;
			String response = "HTTP/1.1 200 OK\r\n"; //FILE FOUND
			try {
				clientStream.write(response.getBytes());
				clientStream.write(date.getBytes());
				String body = "<HTML>" + "<HEAD><h1 style=\"color: #5e9ca0;\"><span style=\"color: #2b2301;\">Shutting Down service- Goodbye :)</span></h1></HEAD></HTML>";
				String length =  "Content-Length: " + Integer.toString(body.length()) + "\r\n";
				String type = "Content-type: text/html \r\n";
				clientStream.write(type.getBytes());
				clientStream.write(length.getBytes());
				clientStream.write(closeHeader.getBytes());
				clientStream.write(this.CLRF.getBytes());
				clientStream.write(body.getBytes());
				clientStream.close();
				client.close();	
			} catch (SocketException s) {
				System.out.println("socket closed");
			}
			catch (IOException e1) {
				e1.printStackTrace();
			}			
		   for(HttpWorker worker : threadPool) {
			   
			   if(this.equals(worker)) {
				   System.out.println("WHATTA DOO");
				   continue;
			   }
			   worker.finish(); //Stop All threads
			  if(!worker.isInterrupted()) {
				  worker.interrupt();
			  }
		   }
		  service.getDispatcher().interrupt();
		   //service.
		   try {
			service.getSocket().close();
		} catch (IOException e) {
			System.out.println("Failed to close socket");
			e.printStackTrace();
		}
		   service.sendShutdown(this);
		   this.finish();
	   }
	   
	   public String getThreadState() {
		   return this.state;
	   }
	   
	   public int getStateId() {
		   return this.id;
	   }
	   
	   /*Closes active resources such as client socket */
	   public void finish() {
		   this.isShutdown = true;
		   try {
			   if(client != null && !client.isClosed()) {
				   client.close();
			   }			
		} catch (IOException e) {
			System.out.println("Error Closing client");
			e.printStackTrace();
		}	   		   
		   if(!this.isInterrupted()) {
			   this.interrupt();
		   }		   
	   }
}
			  
