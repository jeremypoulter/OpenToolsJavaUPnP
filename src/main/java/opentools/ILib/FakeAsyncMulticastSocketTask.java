package opentools.ILib;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.net.SocketException;


public class FakeAsyncMulticastSocketTask implements Runnable, ForciblyInterruptible
{
	private InetAddress localAddress;
	private NetworkInterface localInterface;
	private MulticastSocket mSocket;
	private AsyncUDPSocketHandler userCallback;
	private byte[] buffer = new byte[4096];
	private FakeAsyncMulticastSocket mParent;
	

	public InetAddress getLocalAddress()
	{
		return(localAddress);
	}
	public void setRecvTimeout(int msTimeout)
	{
		try 
		{
			mSocket.setSoTimeout(msTimeout);
		} catch (SocketException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public FakeAsyncMulticastSocketTask(FakeAsyncMulticastSocket parent, InetAddress local, int portNumber, NetworkInterface itfc, AsyncUDPSocketHandler callback)
	{
		mParent = parent;
		userCallback = callback;
		localAddress = local;
		localInterface = itfc;
		try 
		{
			mSocket = new MulticastSocket(portNumber);
			mSocket.setInterface(localAddress);
		} catch (IOException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public void SetBufferSize(int size)
	{
		buffer = new byte[size];
	}
	public void JoinMulticastGroup(SocketAddress mcastAddress)
	{
		try 
		{
			mSocket.joinGroup(mcastAddress, localInterface);
		} catch (IOException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public void SetMulticastTTL(int ttl)
	{
		try 
		{
			mSocket.setTimeToLive(ttl);
		} catch (IOException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public void Send(DatagramPacket p) 
	{
		try 
		{
			mSocket.send(p);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void run() 
	{
		mParent.factory.CheckIn(this);
		SocketAddress local = new InetSocketAddress(localAddress, mSocket.getLocalPort());
		while(!Thread.interrupted())
		{
			try 
			{
				DatagramPacket p = new DatagramPacket(buffer, buffer.length);
				
				mSocket.receive(p);
				InetAddress fromAddr = p.getAddress();
				int fromPort = p.getPort();
				
				byte[] data = p.getData();
				int len = data.length;
				
				userCallback.OnReceiveFrom(local, fromAddr, fromPort, data, len);
			} catch (Exception e) 
			{
				break;
			}
		}
		mParent.factory.CheckOut();
	}
	@Override
	public void ForceInterrupt() 
	{
		mSocket.close();
	}
}
