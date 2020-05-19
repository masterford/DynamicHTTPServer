# DynamicHTTPServer

This code extends the multithreaded http server to support dynamic content. It is based on CIS555 assignment 1.
This API  mimics the one used by Spark Framework, a popular web service framework that has been gaining significant traction within the Web community.  (http://sparkjava.com/documentation):

Using this API, developers should be able to:
Register route handlers - e.g., using post(path, handler) for POST requests. The path can
include a pattern with wildcards (see the description in the Spark Framework documentation)
• Use the Request and Response objects to get information about the request, and to influence
aspects of the response. 
• Access query parameters, e.g., via queryParams(). If the request is a GET,  theserver should
get these from the request URI - for instance, if the request is for /foo?bar=123,
queryParams("bar") should return "123". If the request is a POST and the Content-Type is
application/x-www-form-urlencoded, the query parameters should be extracted from
the body instead. See https://www.w3.org/MarkUp/html-spec/html-spec_8.html#SEC8.2.1 for
more information.
• Register filters. A filter is called before (or after) the route handler, and is typically used to prevent
certain requests from being handled (by throwing a HaltException from the filter) or to add
parameters to the HTTP request (to give more detail to the route handler). For instance, a filter
could be used to validate that a request is authorized (e.g., belongs to an active session) before the
route handler is called. 
• Create cookies and sessions. A session is basically a key-value store that can be used to remember
information about a particular user. See Section 4.4 for more information.
• Redirect requests by calling the redirect() method in the response. The server sends a 301/302 HTTP response to forward the browser to a new location.
• Use the port() method to control the port on which your server runs, the threadPool()
method to control the size of the thread pool, and the staticFileLocation() method to
control where the web server looks for static files. 
