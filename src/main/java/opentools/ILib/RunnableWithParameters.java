package opentools.ILib;

public class RunnableWithParameters implements Runnable
{
	private RunnableWithParametersHandler R;
	private Object[] userParams;
	
	public RunnableWithParameters(Object[] Params, RunnableWithParametersHandler userCallback)
	{
		R = userCallback;
		userParams = Params;
	}

	@Override
	public void run() 
	{
		R.run(userParams);
	}
}
