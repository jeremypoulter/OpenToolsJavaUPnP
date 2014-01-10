package opentools.upnp;

import java.util.jar.Attributes;

public interface GenericInvokeHandler 
{
	public void OnGenericInvoke(String methodName, int errorCode, Attributes parameters, Object userState, Object userState2);
}
