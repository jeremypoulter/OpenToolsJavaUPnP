package opentools.ILib;

import org.apache.http.HttpRequest;
import org.apache.http.MethodNotSupportedException;
import org.apache.http.RequestLine;
import org.apache.http.impl.DefaultHttpRequestFactory;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.message.BasicHttpRequest;

public class ILibHttpRequestFactory extends DefaultHttpRequestFactory
{
	public ILibHttpRequestFactory()
	{
		super();
	}
	@Override
    public HttpRequest newHttpRequest(final RequestLine requestline) throws MethodNotSupportedException 
    {
    	if(requestline.getMethod().equalsIgnoreCase("notify"))
		{
    		return(new BasicHttpEntityEnclosingRequest(requestline));
		}
    	else if(requestline.getMethod().equalsIgnoreCase("subscribe"))
    	{
    		return(new BasicHttpRequest(requestline));
    	}
    	else if(requestline.getMethod().equalsIgnoreCase("unsubscribe"))
    	{
    		return(new BasicHttpRequest(requestline));
    	}
    	else
    	{
    		return(super.newHttpRequest(requestline));
    	}
    }
	@Override
	public HttpRequest newHttpRequest(final String method, final String uri) throws MethodNotSupportedException
	{
		if(method.equalsIgnoreCase("notify"))
		{
			return(new BasicHttpEntityEnclosingRequest(method,uri));
		}
		else if(method.equalsIgnoreCase("subscribe"))
		{
			return(new BasicHttpRequest(method,uri));
		}
		else if(method.equalsIgnoreCase("unsubscribe"))
		{
			return(new BasicHttpRequest(method,uri));
		}
		else
		{
			return super.newHttpRequest(method, uri);
		}
	}
}
