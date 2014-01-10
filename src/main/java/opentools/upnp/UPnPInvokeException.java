package opentools.upnp;

public class UPnPInvokeException extends Exception
{
	protected int UPnPErrorCode;
	protected String UPnPErrorDescription;
	
	public UPnPInvokeException(int errorCode, String errorDescription)
	{
		UPnPErrorCode = errorCode;
		UPnPErrorDescription = errorDescription;
	}
}
