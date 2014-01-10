package opentools.ILib;

public abstract class RunnableEx<T> implements Runnable
{
	private T mParam;
	
	public RunnableEx(T inParam)
	{
		mParam = inParam;
	}

	@Override
	public void run() 
	{
		run(mParam);
	}
		
	public abstract void run(T param);
}

