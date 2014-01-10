package opentools.ILib;

import org.apache.http.HttpRequestFactory;
import org.apache.http.impl.DefaultHttpServerConnection;

public class ILibHttpServerConnection extends DefaultHttpServerConnection
{
	public ILibHttpServerConnection()
	{
		super();
	}
	@Override
	protected HttpRequestFactory createHttpRequestFactory()
	{
		return(new ILibHttpRequestFactory());
	}
}
