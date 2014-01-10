package opentools.ILib;

public class SmartTimerObject 
{
	protected Object mUserObject;
	protected int mTimeout;
	protected SmartTimerHandler userCallback;
	
	public SmartTimerObject(Object userObject, int timeout, SmartTimerHandler callback)
	{
		mUserObject = userObject;
		mTimeout = timeout;
		userCallback = callback;
	}
	
	@Override
	public int hashCode()
	{
		return(mUserObject.hashCode());
	}
}
