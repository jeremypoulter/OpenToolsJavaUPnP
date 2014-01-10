package opentools.upnp;

import java.util.Date;
import java.util.UUID;

public class UPnPEventSubscriber 
{
	protected String SubscriptionID;
	protected int SequenceID;
	protected String GenaURL;
	
	protected long SubscriptionPeriod;
	
	private long LastRenewal;
	
	
	protected UPnPEventSubscriber(int periodSeconds, String callbackURL)
	{
		SubscriptionPeriod = (long)(periodSeconds * 1000);
		LastRenewal = new Date().getTime();
		GenaURL = callbackURL;
		SequenceID = 0;
		SubscriptionID = String.format("uuid:%s", UUID.randomUUID().toString());
	}
	protected int GetNextSequenceID()
	{
		synchronized(this)
		{
			return(SequenceID++);
		}
	}
	protected void RenewSubscription(int periodSeconds)
	{
		synchronized(this)
		{
			if(periodSeconds>0)
			{
				SubscriptionPeriod = (long)(periodSeconds * 1000);
			}
			LastRenewal = new Date().getTime();
		}
	}
	protected boolean isValidSubscription()
	{
		synchronized(this)
		{
			long elapsed = new Date().getTime() - LastRenewal;
			return(elapsed < SubscriptionPeriod);
		}
	}	
}
