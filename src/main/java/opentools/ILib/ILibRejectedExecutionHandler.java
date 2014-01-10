package opentools.ILib;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

public class ILibRejectedExecutionHandler implements RejectedExecutionHandler
{
	private SmartTimer internalTimer;
	
	public ILibRejectedExecutionHandler()
	{
		this(new SmartTimer());
	}
	public ILibRejectedExecutionHandler(SmartTimer t)
	{
		internalTimer = t;
	}
	@Override
	public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) 
	{
		internalTimer.AddObject(new Object[]{r,executor}, 250, new SmartTimerHandler()
		{
			@Override
			public void OnSmartTimerExpired(Object obj) 
			{
				Runnable r = (Runnable)((Object[])obj)[0];
				ThreadPoolExecutor executor = (ThreadPoolExecutor)((Object[])obj)[1];
				
				executor.execute(r);
			}
		});
	}

}
