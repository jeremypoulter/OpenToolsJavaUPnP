package opentools.upnp;

public class GenericInvoke_StateObject 
{
	public GenericInvokeHandler userCallback;
	public Object userStateObject1;
	public Object userStateObject2;
	public String methodName;
	
	public GenericInvoke_StateObject(String MethodName, GenericInvokeHandler callback, Object userState1, Object userState2)
	{
		methodName = MethodName;
		userCallback = callback;
		userStateObject1 = userState1;
		userStateObject2 = userState2;
	}
}
