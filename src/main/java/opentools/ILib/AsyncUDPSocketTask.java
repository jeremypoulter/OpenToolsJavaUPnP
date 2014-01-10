package opentools.ILib;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.net.SocketException;

public class AsyncUDPSocketTask implements Runnable
{
	private DatagramSocket internalSocket;
	private AsyncUDPSocketHandler userCallback;
	private byte[] buffer = new byte[4096];
	
	public AsyncUDPSocketTask(DatagramSocket s, AsyncUDPSocketHandler callback)
	{
		internalSocket = s;
		userCallback = callback;
	}
	public void setBufferSize(int size)
	{
		buffer = new byte[size];
	}
	public void joinMulticastGroup(SocketAddress mcastaddr, NetworkInterface netIf) 
	{
		try 
		{
			((MulticastSocket)internalSocket).joinGroup(mcastaddr, netIf);
		} catch (IOException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public void Bind(SocketAddress local) throws SocketException
	{
		internalSocket.bind(local);
	}
	public void SetMulticastInterface(InetAddress i) throws SocketException
	{
		((MulticastSocket)internalSocket).setInterface(i);
	}
	public void SetMulticastTTL(int TTL) throws Exception
	{
		((MulticastSocket)internalSocket).setTimeToLive(TTL);
	}
	public void connect(SocketAddress dest) throws SocketException
	{
		internalSocket.connect(dest);
	}
	public void send(DatagramPacket p) throws Exception
	{
		internalSocket.send(p);
	}
	public void run() 
	{
		DatagramPacket pack = new DatagramPacket(buffer, buffer.length);
		
		while(!Thread.interrupted())
		{	
			try 
			{
				internalSocket.receive(pack);
				userCallback.OnReceiveFrom(null, pack.getAddress(), pack.getPort(), pack.getData(), pack.getLength());
			} catch (IOException e) 
			{
				break;
			}
		}
	}
}
