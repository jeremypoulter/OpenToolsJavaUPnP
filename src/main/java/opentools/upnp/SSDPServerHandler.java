package opentools.upnp;

import java.net.InetAddress;
import java.net.SocketAddress;

import opentools.ILib.HTTPMessage;

public interface SSDPServerHandler 
{
	public void OnSSDPMessage(HTTPMessage message, InetAddress recievedOn, InetAddress receivedFromAddress, int receivedFromPort);
}
