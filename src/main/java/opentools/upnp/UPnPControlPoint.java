package opentools.upnp;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Hashtable;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;

import opentools.ILib.AsyncHttpClientHandler;
import opentools.ILib.FakeAsyncHttpClient;
import opentools.ILib.FakeAsyncHttpServer;
import opentools.ILib.HTTPMessage;
import opentools.ILib.SmartTimer;
import opentools.ILib.SmartTimerHandler;

public class UPnPControlPoint 
{
	private String ST;
	private SSDP _ssdp;
	private SSDPServer _ssdpd;
	
	private UPnPDeviceHandler userDeviceCallback;
	private FakeAsyncHttpClient H;
	protected FakeAsyncHttpServer WS;
	
	private final Hashtable<String, UPnPDevice> deviceTable = new Hashtable<String, UPnPDevice>();
	private Hashtable<String,HttpRequest> fetchingTable = new Hashtable<String,HttpRequest>();
	protected SmartTimer deviceMonitor;
	private UPnPControlPoint me = this;
	
	private byte[] GetBufferFromResponse(HttpResponse response)
	{
		byte[] buffer = null;
		
		try 
		{
			InputStream s = response.getEntity().getContent();
			ArrayList<Byte> b = new ArrayList<Byte>();
			int data;
			do
			{
				data = s.read();
				if(data>=0)
				{
					b.add((byte)data);
				}
			}while(data>=0);
			buffer = new byte[b.size()];
			for(int i=0;i<buffer.length;++i)
			{
				buffer[i] = b.get(i).byteValue();
			}
		} catch (IllegalStateException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
		return(buffer);
	}
	private SmartTimerHandler mDeviceTimeout = new SmartTimerHandler()
	{
		@Override
		public void OnSmartTimerExpired(Object obj) 
		{
			UPnPDevice dev = (UPnPDevice)obj;
			synchronized(deviceTable)
			{
				deviceTable.remove(dev.DeviceUDN);
			}
			if(userDeviceCallback!=null)
			{
				userDeviceCallback.OnRemovedDevice(dev);
			}
		}
	};
	
	private AsyncHttpClientHandler mDDDCallback = new AsyncHttpClientHandler()
	{
		@Override
		public void OnResponse(HttpResponse response, HttpRequest request,
				Object State) 
		{		
			if(response.getStatusLine().getStatusCode()==200)
			{
				InetAddress localHost = (InetAddress)((Object[])State)[1];
				int MaxAge = ((Integer)((Object[])State)[2]).intValue();
				byte[] buffer = GetBufferFromResponse(response);
				if(buffer!=null)
				{			
					UPnPDevice dev = new UPnPDevice(new String(buffer), request.getRequestLine().getUri(), H);
					dev.ParentCP = me;
					synchronized(deviceTable)
					{
						deviceTable.put(dev.DeviceUDN, dev);
					}
					dev.LocalHost = localHost.getHostAddress();
					dev.MaxAge = MaxAge;
					deviceMonitor.AddObject(dev, MaxAge*1000, mDeviceTimeout);
					
					if(userDeviceCallback!=null)
					{
						userDeviceCallback.OnAddedDevice(dev);
					}
				}
			}
			fetchingTable.remove(((Object[])State)[0]);
		}
	};
	private SSDPFindHandler mFindHandler = new SSDPFindHandler()
	{
		@Override
		public void OnFind(InetAddress receivedOn, String urn, String UDN, int maxAge, String LocationUri) 
		{
			if(deviceTable.containsKey(UDN))
			{
				//
				// Device Already Exists
				//
			}
			else
			{
				if(!fetchingTable.containsKey(UDN))
				{
					//
					// No outstanding requests, so we can go ahead and fetch the Description Document
					//
					HttpRequest r = new BasicHttpEntityEnclosingRequest("GET", LocationUri);
					fetchingTable.put(UDN, r);
					H.AddRequest(r,null, new Object[]{UDN,receivedOn,new Integer(maxAge)}, mDDDCallback);
				}
			}
		}
	};
	
	public UPnPControlPoint(String searchTarget, UPnPDeviceHandler DeviceCallback)
	{
		deviceMonitor = new SmartTimer();
		ST = searchTarget;
		_ssdp = new SSDP();
		_ssdpd = new SSDPServer(new SSDPServerHandler()
		{
			@Override
			public void OnSSDPMessage(HTTPMessage message,
					InetAddress recievedOn, InetAddress receivedFromAddress, int receivedFromPort) 
			{
				if(message.Directive.equalsIgnoreCase("NOTIFY"))
				{
					if(message.GetTag("NT").equals(ST))
					{
						//
						// Got a Notify Packet for a device we may be interested in. What is it's UDN?
						//
						String udn = message.GetTag("USN");
						if(udn.contains("::"))
						{
							udn = udn.substring(0, udn.indexOf("::"));
						}
						
						//
						// What kind of Packet is it?
						//
						String nts = message.GetTag("NTS");
						if(nts.equals("ssdp:alive"))
						{
							String MaxAge = message.GetTag("CACHE-CONTROL");
							MaxAge = MaxAge.substring(1+MaxAge.indexOf("=")).trim();
							
							//
							// Do we have this device already?
							// Note: There could be a race condition between the following two checks,
							// but it really doesn't matter, because worst case, is that we'll create a duplicate
							// device, but we'll quickly realize when we check the table before adding it, in which
							// case we can just toss the device.
							//
							if(deviceTable.containsKey(udn))
							{
								//
								// We already have this device, so just increment the expiration timeout
								//
								
								deviceMonitor.AddObject(deviceTable.get(udn), 1000*Integer.valueOf(MaxAge), mDeviceTimeout);
							}
							else
							{
								//
								// Are we in the process of creating this device?
								//
								if(!fetchingTable.containsKey(udn))
								{
									//
									// No? Then let's go get this device!
									//
																	
									HttpRequest r = new BasicHttpEntityEnclosingRequest("GET", message.GetTag("Location"));
									fetchingTable.put(udn, r);
									H.AddRequest(r,null, new Object[]{udn,recievedOn, Integer.parseInt(MaxAge)}, mDDDCallback);
								}
							}
						}
						else if(nts.equals("ssdp:byebye"))
						{
							//
							// Check to see if we have this device. If we do, we need to 
							// notify the user that this device is going away
							//
							synchronized(deviceTable)
							{
								if(deviceTable.containsKey(udn))
								{
									deviceMonitor.RemoveObject(deviceTable.get(udn));
									if(userDeviceCallback!=null)
									{
										userDeviceCallback.OnRemovedDevice(deviceTable.get(udn));
									}
									deviceTable.remove(udn);
								}
							}
						}
					}
				}
			}
		});
		H = new FakeAsyncHttpClient();
		userDeviceCallback = DeviceCallback;
		_ssdp.OnFind = mFindHandler;
		_ssdp.Find(searchTarget);
		_ssdp.Find(searchTarget);
	}
	public void Refresh()
	{
		_ssdp.Find(ST);
	}
	public void Stop()
	{
		_ssdp.Stop();
		_ssdpd.Stop();
		H.Shutdown();
		deviceMonitor.Flush();
		if(WS!=null)
		{
			WS.Stop();
		}
	}
}
