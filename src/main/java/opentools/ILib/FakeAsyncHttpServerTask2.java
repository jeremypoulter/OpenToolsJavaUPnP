package opentools.ILib;

import java.io.IOException;
import java.net.InetAddress;

import org.apache.http.HttpException;
import org.apache.http.HttpServerConnection;
import org.apache.http.impl.DefaultHttpServerConnection;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpService;

public class FakeAsyncHttpServerTask2 implements Runnable
{
	private HttpService mService;
	private HttpServerConnection mConnection;
	private HttpContext mContext;
	
	public FakeAsyncHttpServerTask2(HttpService service, HttpServerConnection connection, HttpContext context)
	{
		mService = service;
		mConnection = connection;
		mContext = context;
	}
	
	@Override
	public void run() 
	{
		synchronized(FakeAsyncHttpServer.SyncObject)
		{
			++FakeAsyncHttpServer.threadNumber;
			Thread.currentThread().setName(String.format("FakeAsyncHttpServer/Processor[%d] - Thread #%d", FakeAsyncHttpServer.instanceNumber,FakeAsyncHttpServer.threadNumber));
		}
		
		if(mConnection instanceof DefaultHttpServerConnection)
		{
			InetAddress local = ((DefaultHttpServerConnection)mConnection).getLocalAddress();
			InetAddress remote = ((DefaultHttpServerConnection)mConnection).getRemoteAddress();
			
			Integer localPort = ((DefaultHttpServerConnection)mConnection).getLocalPort();
			Integer remotePort = ((DefaultHttpServerConnection)mConnection).getRemotePort();
			
			mContext.setAttribute(Integer.valueOf(Thread.currentThread().hashCode()).toString(), new Object[]{local,localPort,remote,remotePort});
		}
		
		try 
		{
			mService.handleRequest(mConnection, mContext);
			mConnection.shutdown();
		} catch (IOException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (HttpException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
