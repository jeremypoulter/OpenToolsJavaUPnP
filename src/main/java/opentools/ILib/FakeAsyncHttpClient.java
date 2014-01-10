package opentools.ILib;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import android.net.Uri;

public class FakeAsyncHttpClient 
{
	public static int ConnectionTimeoutInMilliseconds = 0;
	private ThreadPoolExecutor tp;
	protected static int idNumber = 0;
	protected static int threadNumber = 0;
	protected static Object SyncObject = new Object();
	
	public FakeAsyncHttpClient()
	{
		++idNumber;
		tp = new ThreadPoolExecutor(1, 10, 30, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());
	}
	public void AddRequest(HttpRequest r, SocketAddress local, Object state, AsyncHttpClientHandler callback)
	{
		FakeAsyncHttpClientTask task = new FakeAsyncHttpClientTask(r, local, state, callback);
		tp.execute(task);
	}
	
	public static void AddRequest_SingleThreaded(HTTPMessage r, Uri destUri, Object state, AsyncHttpClientHandler callback)
	{
		HttpRequest request = new BasicHttpEntityEnclosingRequest(r.Directive, destUri.toString());
		ILibParsers.CopyPacketToRequest(r, request);
				
		HttpHost target = new HttpHost(destUri.getHost(), destUri.getPort(), destUri.getScheme());
		Thread t = new Thread(new RunnableEx<Object[]>(new Object[]{request, target, state, callback})
				{
					@Override
					public void run(Object[] param) 
					{
						HttpRequest request = (HttpRequest)param[0];
						HttpHost target = (HttpHost)param[1];
						Object userState = param[2];
						AsyncHttpClientHandler user = (AsyncHttpClientHandler)param[3];
						
						HttpParams httpParameters = new BasicHttpParams();
						int timeoutConnection = FakeAsyncHttpClient.ConnectionTimeoutInMilliseconds;
						HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
						DefaultHttpClient c = new DefaultHttpClient(httpParameters);
						
						HttpResponse response = null;
						try 
						{
							response = c.execute(target, request);
						} catch (Exception e) 
						{
							// TODO Auto-generated catch block
							e.printStackTrace();
						} 
						
						if(user!=null)
						{
							user.OnResponse(response, request, userState);
						}
					}
				});
		t.start();
	}
	
	public void AddRequest(HTTPMessage r, String destUri, Object state, AsyncHttpClientHandler callback)
	{
		HttpRequest req = new BasicHttpEntityEnclosingRequest(r.Directive, destUri);
		ILibParsers.CopyPacketToRequest(r, req);
		AddRequest(req,null,state,callback);
	}
	public void Shutdown()
	{
		tp.shutdownNow();
	}
	public static byte[] GetBufferFromResponse(HttpResponse response)
	{
		byte[] buffer = null;
		
		try 
		{
			InputStream s = response.getEntity().getContent();
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
			buffer = new byte[b.size()];
			for(int i=0;i<buffer.length;++i)
			{
				buffer[i] = b.get(i).byteValue();
			}
		} catch (IllegalStateException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
		return(buffer);
	}
}
