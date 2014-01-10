package opentools.ILib;

import java.util.Hashtable;
import java.util.concurrent.ThreadFactory;

public class ILibThreadFactory implements ThreadFactory
{
	protected String mName;
	private int threadNumber;
	private Hashtable<Thread, Runnable> checkInTable;
	
	protected void ForcelyInterruptMe(Thread t)
	{
		Runnable r;
		
		synchronized(this)
		{
			r = checkInTable.get(t);
			checkInTable.remove(t);
		}
		
		if(r!=null && r instanceof ForciblyInterruptible)
		{
			((ForciblyInterruptible)r).ForceInterrupt();
		}
	}
	
	public ILibThreadFactory(String name)
	{
		mName = name;
		threadNumber = 1;
		checkInTable = new Hashtable<Thread,Runnable>();
	}
	
	public void CheckIn(Runnable task)
	{
		synchronized(this)
		{
			checkInTable.put(Thread.currentThread(), task);
		}
	}
	public void CheckOut()
	{
		synchronized(this)
		{
			checkInTable.remove(Thread.currentThread());
		}
	}
	
	@Override
	public Thread newThread(Runnable r) 
	{
		synchronized(this)
		{
			ILibThread t = new ILibThread(this,r);
			t.setName(String.format("%s - Thread #%d", mName,threadNumber++));
			return(t);
		}
	}

}
