package opentools.ILib;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.ServerSocketChannel;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpServerConnection;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.BasicHttpProcessor;
import org.apache.http.protocol.HttpRequestHandlerRegistry;
import org.apache.http.protocol.HttpService;
import org.apache.http.protocol.ResponseConnControl;
import org.apache.http.protocol.ResponseContent;
import org.apache.http.protocol.ResponseDate;
import org.apache.http.protocol.ResponseServer;

public class FakeAsyncHttpServer 
{
	private ThreadPoolExecutor tp;
	private HttpService hService;
	private BasicHttpProcessor httpproc;
	private BasicHttpContext httpcontext;
	private HttpRequestHandlerRegistry virDirRegistry;
	private ServerSocket serverSocket;
	
	protected static int instanceNumber = 0;
	protected static int threadNumber = 0;
	protected static Object SyncObject = new Object();
	
	public FakeAsyncHttpServer()
	{
		++instanceNumber;
		tp = new ThreadPoolExecutor(1, 10, 30, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());
		httpproc = new BasicHttpProcessor();
		httpcontext = new BasicHttpContext();
		
        httpproc.addInterceptor(new ResponseDate());
        httpproc.addInterceptor(new ResponseServer());
        httpproc.addInterceptor(new ResponseContent());
        httpproc.addInterceptor(new ResponseConnControl());
                
        virDirRegistry = new HttpRequestHandlerRegistry();
		
		hService = new HttpService(httpproc, 
                new DefaultConnectionReuseStrategy(),
                new DefaultHttpResponseFactory());
		hService.setHandlerResolver(virDirRegistry);
		
	}
	
	public int getLocalPort()
	{
		return(serverSocket.getLocalPort());
	}
	public int Start(int localPort)
	{
		try 
		{
			serverSocket = ServerSocketChannel.open().socket();
			serverSocket.bind(new InetSocketAddress(localPort));
			tp.execute(new FakeAsyncHttpServerTask(serverSocket, this));
			return(serverSocket.getLocalPort());
		} catch (IOException e) 
		{
			return(-1);
		}
	}
	public void Stop()
	{
		tp.shutdownNow();
	}
	protected void HandleRequest(HttpServerConnection connection)
	{
		tp.execute(new FakeAsyncHttpServerTask2(hService, connection, httpcontext));
	}
	public void RegisterVirtualDirectory(String virtualDirectory, AsyncHttpServerHandler callback)
	{
		virDirRegistry.register(virtualDirectory, new FakeAsyncHttpServerPageHandler(virtualDirectory,callback));
	}
	public void UnRegisterVirtualDirectory(String virtualDirectory)
	{
		virDirRegistry.unregister(virtualDirectory);
	}
}
