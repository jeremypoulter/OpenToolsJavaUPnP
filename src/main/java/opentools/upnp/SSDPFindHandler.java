package opentools.upnp;

import java.net.InetAddress;

public interface SSDPFindHandler 
{
	public void OnFind(InetAddress receivedOn, String URN, String UDN, int MaxAge, String LocationUri);
}
