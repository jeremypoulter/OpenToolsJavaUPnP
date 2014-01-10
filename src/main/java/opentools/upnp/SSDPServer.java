package opentools.upnp;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;

import opentools.ILib.AsyncUDPSocketHandler;
import opentools.ILib.FakeAsyncMulticastSocket;
import opentools.ILib.HTTPMessage;

public class SSDPServer 
{
	private FakeAsyncMulticastSocket S;
	private SSDPServerHandler userCallback;
	private static int instanceNumber = 0;
	
	public SSDPServer(SSDPServerHandler callback)
	{
		++instanceNumber;
		userCallback = callback;
		S = new FakeAsyncMulticastSocket(1900, new AsyncUDPSocketHandler()
		{
			@Override
			public void OnReceiveFrom(SocketAddress local, InetAddress remoteAddress, int remotePort,
					byte[] data, int dataLength) 
			{
				if(userCallback!=null)
				{
					HTTPMessage msg = HTTPMessage.Parse(data, 0, dataLength);
					userCallback.OnSSDPMessage(msg, ((InetSocketAddress)local).getAddress(), remoteAddress, remotePort);
				}
			}
		});
		S.SetOwnerName(String.format("SSDPServer[%d]",instanceNumber));
		try 
		{
			S.JoinMulticastGroup(new InetSocketAddress(InetAddress.getByName("239.255.255.250"),1900));
			S.SetMulticastTTL(2);
			S.Start();
		} catch (UnknownHostException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public void Stop()
	{
		S.Stop();
	}
}
