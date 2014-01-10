package opentools.ILib;

import java.net.DatagramSocket;
import java.util.Hashtable;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class FakeAsyncUDPSocket 
{
	private ThreadPoolExecutor tp;
	private Hashtable<String,DatagramSocket> socketTable;
	
	public FakeAsyncUDPSocket()
	{
		tp = new ThreadPoolExecutor(1, 5, 30, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());
		socketTable = new Hashtable<String,DatagramSocket>();
	}
	public AsyncUDPSocketTask Create(DatagramSocket s, AsyncUDPSocketHandler callback) throws Exception
	{
		AsyncUDPSocketTask retVal = new AsyncUDPSocketTask(s, callback);
		return(retVal);
	}

	public void Start(AsyncUDPSocketTask task)
	{
		tp.execute(task);
	}
	public void Stop()
	{
		tp.shutdownNow();
	}
}
