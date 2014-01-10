package opentools.ILib;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TimerTask;
import java.util.TreeSet;

public class SmartTimer_Task extends TimerTask
{
	private SmartTimer mParent;
	
	public SmartTimer_Task(SmartTimer Parent)
	{
		mParent = Parent;
	}
	@Override
	public void run() 
	{
		synchronized(mParent)
		{
			SmartTimerObject first;
			TreeSet<SmartTimerObject> newSet = new TreeSet<SmartTimerObject>(new SmartTimerComparator());
			int elapsed = (int)(new Date().getTime() - mParent.timeAtStart.getTime());
			
			List<SmartTimerObject> callbackList = new ArrayList<SmartTimerObject>();
			
			Iterator<SmartTimerObject> i = mParent.timeoutSet.iterator();
			while(i.hasNext())
			{
				first = i.next();
				if(first.mTimeout <= elapsed)
				{
					if(first.userCallback!=null)
					{
						callbackList.add(first);
					}
				}
				else
				{
					first.mTimeout -= elapsed;
					newSet.add(first);
				}
			}
			
			mParent.timeoutSet.clear();
			mParent.timeoutSet = newSet;
			
			if(mParent.timeoutSet.size()>0)
			{
				mParent.timeAtStart = new Date();
				mParent.t.schedule(new SmartTimer_Task(mParent), mParent.timeoutSet.first().mTimeout);
			}
			
			i = callbackList.iterator();
			while(i.hasNext())
			{
				first = i.next();
				first.userCallback.OnSmartTimerExpired(first.mUserObject);
			}
		}
	}
}
