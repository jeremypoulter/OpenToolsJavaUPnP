package opentools.ILib;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class FakeAsyncMulticastSocket 
{
	private List<FakeAsyncMulticastSocketTask> sockets;
	private ThreadPoolExecutor tp;
	protected ILibThreadFactory factory = new ILibThreadFactory("FakeAsyncMulticastSocket");
	
	public void SetOwnerName(String name)
	{
		factory.mName = name;
	}
	public Iterator<FakeAsyncMulticastSocketTask> getTaskIterator()
	{
		return(sockets.iterator());
	}
	public FakeAsyncMulticastSocket(int portNumber, AsyncUDPSocketHandler callback)
	{
		sockets = new ArrayList<FakeAsyncMulticastSocketTask>();
		try 
		{
			Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces();
			while(e.hasMoreElements())
			{
				NetworkInterface n = e.nextElement();
				Enumeration<InetAddress> ae = n.getInetAddresses();
				while(ae.hasMoreElements())
				{
					InetAddress a = ae.nextElement();
					if(!a.isLoopbackAddress())
					{
						sockets.add(new FakeAsyncMulticastSocketTask(this, a, portNumber, n, callback));
					}
				}
			}
			if(callback!=null)
			{
				tp = new ThreadPoolExecutor(1, sockets.size()+1, 30, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());
				tp.setThreadFactory(factory);
			}
		} catch (SocketException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public FakeAsyncMulticastSocket(AsyncUDPSocketHandler callback)
	{
		this(0,callback);
	}

	public void JoinMulticastGroup(SocketAddress mcastAddress)
	{
		Iterator<FakeAsyncMulticastSocketTask> i = sockets.iterator();
		while(i.hasNext())
		{
			FakeAsyncMulticastSocketTask t = i.next();
			t.JoinMulticastGroup(mcastAddress);
		}
	}
	public void SetMulticastTTL(int ttl)
	{
		Iterator<FakeAsyncMulticastSocketTask> i = sockets.iterator();
		while(i.hasNext())
		{
			FakeAsyncMulticastSocketTask t = i.next();
			t.SetMulticastTTL(ttl);
		}	
	}
	public void SetBufferSize(int size)
	{
		Iterator<FakeAsyncMulticastSocketTask> i = sockets.iterator();
		while(i.hasNext())
		{
			FakeAsyncMulticastSocketTask t = i.next();
			t.SetBufferSize(size);
		}	
	}
	public void Send(DatagramPacket p, InetAddress OnThisAddress)
	{
		Iterator<FakeAsyncMulticastSocketTask> i = sockets.iterator();
		while(i.hasNext())
		{
			FakeAsyncMulticastSocketTask t = i.next();
			if(t.getLocalAddress().equals(OnThisAddress))
			{
				t.Send(p);
				break;
			}
		}
	}
	public void Send(DatagramPacket p) 
	{
		Iterator<FakeAsyncMulticastSocketTask> i = sockets.iterator();
		while(i.hasNext())
		{
			FakeAsyncMulticastSocketTask t = i.next();
			t.Send(p);
		}
	}
	public void setRecvTimeout(int msTimeout) 
	{
		Iterator<FakeAsyncMulticastSocketTask> i = sockets.iterator();
		while(i.hasNext())
		{
			FakeAsyncMulticastSocketTask t = i.next();
			t.setRecvTimeout(msTimeout);
		}
	}
	public void Start()
	{
		if(tp!=null)
		{
			Iterator<FakeAsyncMulticastSocketTask> i = sockets.iterator();
			while(i.hasNext())
			{
				FakeAsyncMulticastSocketTask t = i.next();
				tp.execute(t);
			}	
		}
	}
	public void Stop()
	{
		if(tp!=null)
		{
			tp.shutdownNow();
		}
	}
}
