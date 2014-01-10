package opentools.ILib;

import java.net.InetAddress;
import java.net.SocketAddress;

public interface AsyncUDPSocketHandler 
{
	public void OnReceiveFrom(SocketAddress local, InetAddress remoteAddress, int remotePort, byte[] data, int dataLength);
}
