package opentools.ILib;

import java.util.Comparator;

public class SmartTimerComparator implements Comparator<SmartTimerObject>
{
	@Override
	public int compare(SmartTimerObject object1, SmartTimerObject object2) 
	{
		if(object1.hashCode()==object2.hashCode())
		{
			return(0);
		}
		else
		{
			if(object1.mTimeout == object2.mTimeout)
			{
				//
				// Even tho the timeouts are equal, we must only return that they are equal
				// if it really is the same object.
				//
				return(-1);
			}
			else
			{
				return(new Integer(object1.mTimeout).compareTo(new Integer(object2.mTimeout)));
			}
		}
	}
}
