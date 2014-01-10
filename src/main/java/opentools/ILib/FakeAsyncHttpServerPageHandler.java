package opentools.ILib;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Enumeration;

import org.apache.http.Header;
import org.apache.http.HeaderIterator;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.RequestLine;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

public class FakeAsyncHttpServerPageHandler implements HttpRequestHandler
{
	private String VirtualDirectory;
	private AsyncHttpServerHandler userCallback;
	
	public FakeAsyncHttpServerPageHandler(String vDir, AsyncHttpServerHandler callback)
	{
		VirtualDirectory = vDir;
		userCallback = callback;
	}
	
	@Override
	public void handle(HttpRequest request, HttpResponse response,
			HttpContext context) throws HttpException, IOException 
	{
		HTTPMessage userRequest = new HTTPMessage();
		
		Object[] endpoints = (Object[])context.getAttribute(Integer.valueOf(Thread.currentThread().hashCode()).toString());
		if(endpoints!=null)
		{
			userRequest.localAddress = (InetAddress)endpoints[0];
			userRequest.localPort = ((Integer)endpoints[1]).intValue();
			
			userRequest.remoteAddress = (InetAddress)endpoints[2];
			userRequest.remotePort = ((Integer)endpoints[3]).intValue();
		}
		
		
		RequestLine r = request.getRequestLine();
		userRequest.Directive = r.getMethod();
		userRequest.DirectiveObj = r.getUri();
		HeaderIterator i = request.headerIterator();
		
		while(i.hasNext())
		{
			Header h = i.nextHeader();
			userRequest.AddTag(h.getName(), h.getValue());
		}
		if(request instanceof org.apache.http.HttpEntityEnclosingRequest)
		{
			HttpEntity e = ((HttpEntityEnclosingRequest)request).getEntity();
			InputStream s = e.getContent();
			
			ArrayList<Byte> b = new ArrayList<Byte>();
			int data;
			do
			{
				data = s.read();
				if(data>=0)
				{
					b.add((byte)data);
				}
			}while(data>=0);
			byte[] buffer = new byte[b.size()];
			for(int x=0;x<buffer.length;++x)
			{
				buffer[x] = b.get(x).byteValue();
			}
			userRequest.SetBodyBuffer(buffer);
		}
		
		HTTPMessage userResponse = new HTTPMessage();
		userResponse.StatusCode = 200;
		userResponse.StatusData = "OK";
		
		if(userCallback!=null)
		{
			userCallback.OnRequest(userRequest, userResponse);
		}
		response.setStatusLine(HttpVersion.HTTP_1_1, userResponse.StatusCode, userResponse.StatusData);
		
		Enumeration<String> keys = userResponse.Headers.keys();
		while(keys.hasMoreElements())
		{
			String key = keys.nextElement();
			String val = userResponse.GetTag(key);
			
			response.addHeader(key, val);
		}
		HttpEntity e = userResponse.getEntity();
		if(e!=null)
		{
			response.setEntity(e);
		}
	}

}
