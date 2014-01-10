package opentools.upnp;

import java.util.jar.Attributes;

public interface UPnPServiceEventHandler 
{
	public void OnUPnPEvent(UPnPService sender, Attributes eventedParameters);
}
