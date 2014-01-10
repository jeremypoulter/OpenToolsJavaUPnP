package opentools.ILib;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import android.util.Log;

public class FakeAsyncHttpClientTask implements Runnable
{
	private HttpRequest req;
	private SocketAddress localAddress;;
	private Object UserStateObject;
	private AsyncHttpClientHandler UserCallbackObject;
	//private HttpClient MasterClient;
	
	public FakeAsyncHttpClientTask(HttpRequest r, SocketAddress local, Object state, AsyncHttpClientHandler callback)
	{
		req = r;
		localAddress = local;
		UserStateObject = state;
		UserCallbackObject = callback;
		//MasterClient = client;
	}
	public void run()
	{
		try
		{
			run2();
		}
		catch(Exception e)
		{
			Log.w("FakeAsyncHttpClient", e.toString());
		}
	}
	private void run2() 
	{
		synchronized(FakeAsyncHttpClient.SyncObject)
		{
			++FakeAsyncHttpClient.threadNumber;
			Thread.currentThread().setName(String.format("FakeAsyncHttpClient[%d] - Thread #%d", FakeAsyncHttpClient.idNumber, FakeAsyncHttpClient.threadNumber));
		}
		HttpClient MasterClient;
		
		if(FakeAsyncHttpClient.ConnectionTimeoutInMilliseconds!=0)
		{
			HttpParams httpParameters = new BasicHttpParams();
			int timeoutConnection = FakeAsyncHttpClient.ConnectionTimeoutInMilliseconds;
			HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
			MasterClient = new DefaultHttpClient(httpParameters);
		}
		else
		{
			MasterClient = new DefaultHttpClient();
		}
		
		String uri = req.getRequestLine().getUri();
        String scheme = uri.substring(0,uri.indexOf("://"));
    	int len = uri.indexOf("/", scheme.length() + 3);
    	len = len>=0?len:uri.length();
    	String host = uri.substring(scheme.length() + 3, len);
    	String port = scheme.compareToIgnoreCase("https")==0?"443":"80";
    	if(host.indexOf(":")>0)
    	{
    		port = host.substring(host.indexOf(":")+1);
    		host = host.substring(0, host.indexOf(":"));
    	}
    	int portNumber = Integer.parseInt(port);
    	HttpHost hHost = null;

    	hHost = new HttpHost(host, portNumber, scheme);
    		 
		try 
		{
			HttpResponse resp = MasterClient.execute(hHost, req);
			if(UserCallbackObject!=null)
			{			
				UserCallbackObject.OnResponse(resp, req, UserStateObject);
			}
			return;
		} 
		catch (ClientProtocolException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//
		// An Error occured... 
		// Let's try one more time
		//
		try 
		{
			HttpResponse resp = MasterClient.execute(hHost, req);
			if(UserCallbackObject!=null)
			{			
				UserCallbackObject.OnResponse(resp, req, UserStateObject);
			}
			return;
		} 
		catch (ClientProtocolException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//
		// Ok, let's give up
		//
		if(UserCallbackObject!=null)
		{	
			UserCallbackObject.OnResponse(null, req, UserStateObject);
		}
	}
}
