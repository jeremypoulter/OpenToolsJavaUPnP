package opentools.ILib;

import java.util.Date;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;

public class SmartTimer 
{
	protected Timer t = null;
	
	TreeSet<SmartTimerObject> timeoutSet;
	Date timeAtStart;
		
	public SmartTimer()
	{
		timeoutSet = new TreeSet<SmartTimerObject>(new SmartTimerComparator());
	}
	public void Flush()
	{
		synchronized(this)
		{
			if(t!=null)
			{
				t.cancel();
				timeoutSet.clear();
			}
		}
	}
	private void UpdateTime()
	{
		synchronized(this)
		{
			int elapsed = (int)(new Date().getTime() - timeAtStart.getTime());
			Iterator<SmartTimerObject> e = timeoutSet.iterator();
			while(e.hasNext())
			{
				SmartTimerObject j = e.next();
				//
				// We have to iterate through all the objects and subtract out the 
				// amount of time that elapsed since the timer was set until now
				//
				j.mTimeout -= elapsed;
				if(j.mTimeout<0)
				{
					j.mTimeout = 0;
				}
			}
		}
	}
	
	public void AddObject(Object obj, int timeoutInMilliseconds, SmartTimerHandler userCallback)
	{
		synchronized(this)
		{
			if(timeoutInMilliseconds<=0)
			{
				timeoutInMilliseconds = 1;
			}
			//
			// Let's try to remove the object first just in case we're just adjusting the callback time,
			// 
			RemoveObject(obj);			
			if(t!=null)
			{
				t.cancel();
				UpdateTime();	
			}
			
			SmartTimerObject st = new SmartTimerObject(obj, timeoutInMilliseconds, userCallback);
			timeoutSet.add(st);
			t = new Timer();
			timeAtStart = new Date();
			t.schedule(new SmartTimer_Task(this), timeoutSet.first().mTimeout);
		}
	}
	public void RemoveObject(Object obj)
	{
		synchronized(this)
		{
			if(timeoutSet.size()==0)
			{
				return;
			}
			if(timeoutSet.first().hashCode() == obj.hashCode())
			{
				//
				// We have to reset the timer, because we're removing the head.
				// 
				t.cancel();
				SmartTimerObject j = timeoutSet.first();
				timeoutSet.remove(j);
				
				UpdateTime();
				
				if(timeoutSet.size()>0)
				{
					t = new Timer();
					timeAtStart = new Date();
					t.schedule(new SmartTimer_Task(this), timeoutSet.first().mTimeout);
				}
				else
				{
					t = null;
				}
			}
			else
			{
				//
				// The timer is fine, we can just find our object and remove it from the list.
				//
				Iterator<SmartTimerObject> e = timeoutSet.iterator();
				while(e.hasNext())
				{
					SmartTimerObject j = e.next();
					if(j.hashCode() == obj.hashCode())
					{
						//
						// Found it!
						//
						timeoutSet.remove(j);
						break;
					}
				}
			}
		}
	}
}
