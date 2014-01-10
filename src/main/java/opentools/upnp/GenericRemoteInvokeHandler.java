package opentools.upnp;

import java.util.List;
import java.util.jar.Attributes;

import org.apache.http.message.BasicNameValuePair;

public interface GenericRemoteInvokeHandler 
{
	public void OnGenericRemoteInvoke(UPnPAction sender, Attributes inParams, List<BasicNameValuePair> outParams)  throws UPnPInvokeException;
}
