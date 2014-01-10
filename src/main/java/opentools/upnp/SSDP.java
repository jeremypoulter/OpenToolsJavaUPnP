package opentools.upnp;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.apache.http.HttpRequest;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;

import opentools.ILib.AsyncUDPSocketHandler;
import opentools.ILib.AsyncUDPSocketTask;
import opentools.ILib.FakeAsyncMulticastSocket;
import opentools.ILib.FakeAsyncMulticastSocketTask;
import opentools.ILib.FakeAsyncUDPSocket;
import opentools.ILib.HTTPMessage;
import opentools.ILib.SmartTimer;
import opentools.ILib.SmartTimerHandler;

public class SSDP 
{
	public static int MX_TIMEOUT = 3;
	public SSDPFindHandler OnFind = null;
	private FakeAsyncMulticastSocket U;
	private SmartTimer stimer;
	private Random rGenerator;
	private UPnPDevice internalDevice;
	
	private static int instanceNumber = 0;
	
	private SmartTimerHandler mTimerHandler = new SmartTimerHandler()
	{
		@Override
		public void OnSmartTimerExpired(Object obj) 
		{
			HTTPMessage m = (HTTPMessage)((Object[])obj)[0];
			FakeAsyncMulticastSocketTask t = (FakeAsyncMulticastSocketTask)((Object[])obj)[1];
			Integer MaxAge = (Integer)((Object[])obj)[2];
			
			byte[] buffer = m.GetRawPacket();
			
			DatagramPacket p;
			try 
			{
				p = new DatagramPacket(buffer, buffer.length, InetAddress.getByName("239.255.255.250"), 1900);
				t.Send(p);
				
				stimer.AddObject(new Object[]{m,t,MaxAge}, rGenerator.nextInt(1000*MaxAge.intValue()/2), mTimerHandler);
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}	
		}	
	};
	
	public SSDP()
	{
		++instanceNumber;
		U = new FakeAsyncMulticastSocket(mSocketCallback);
		U.SetOwnerName(String.format("SSDP[%d]", instanceNumber));
		U.SetMulticastTTL(2);
		U.Start();
	}
	public SSDP(UPnPDevice device)
	{
		internalDevice = device;
		HTTPMessage m;
		U = new FakeAsyncMulticastSocket(null);
		U.SetMulticastTTL(2);
		stimer = new SmartTimer();
		rGenerator = new Random();
		
		List<HTTPMessage> notifyPackets = new ArrayList<HTTPMessage>();
		
		ProcessDevice(device,notifyPackets);
		
		Iterator<FakeAsyncMulticastSocketTask> i = U.getTaskIterator();
		while(i.hasNext())
		{
			FakeAsyncMulticastSocketTask t = i.next();
			Iterator<HTTPMessage> mI = notifyPackets.iterator();
			while(mI.hasNext())
			{
				m = mI.next().clone();
				m.AddTag("CACHE-CONTROL", String.format("max-age=%d", device.MaxAge));
				m.AddTag("Location", String.format("http://%s:%d/ddd.xml",t.getLocalAddress().getHostAddress(),device.WS.getLocalPort()));
				
				stimer.AddObject(new Object[]{m,t,Integer.valueOf(device.MaxAge)}, rGenerator.nextInt(1000), mTimerHandler);
			}			
		}
		U.Stop();
	}

	public void Stop()
	{
		if(internalDevice==null)
		{
			U.Stop();
			return;
		}
		if(stimer!=null)
		{
			stimer.Flush();
		}
		
		HTTPMessage m;
		
		List<HTTPMessage> notifyPackets = new ArrayList<HTTPMessage>();
		ProcessDevice(internalDevice, notifyPackets);
		
		Iterator<FakeAsyncMulticastSocketTask> i = U.getTaskIterator();
		
		while(i.hasNext())
		{
			FakeAsyncMulticastSocketTask t = i.next();
			Iterator<HTTPMessage> mI = notifyPackets.iterator();
			while(mI.hasNext())
			{
				m = mI.next().clone();
				m.AddTag("NTS", "ssdp:byebye");
				stimer.AddObject(new Object[]{m,t}, rGenerator.nextInt(500), new SmartTimerHandler()
				{
					@Override
					public void OnSmartTimerExpired(Object obj) 
					{
						HTTPMessage m = (HTTPMessage)((Object[])obj)[0];
						FakeAsyncMulticastSocketTask t = (FakeAsyncMulticastSocketTask)((Object[])obj)[1];
												
						byte[] buffer = m.GetRawPacket();
						
						DatagramPacket p;
						try 
						{
							p = new DatagramPacket(buffer, buffer.length, InetAddress.getByName("239.255.255.250"), 1900);
							t.Send(p);
						} catch (UnknownHostException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}	
					}
				});
			}			
		}
		stimer.AddObject(this, 1000, new SmartTimerHandler()
		{
			@Override
			public void OnSmartTimerExpired(Object obj) 
			{
				stimer.Flush();
			}
		});
	}
	private void ProcessDevice(UPnPDevice device, List<HTTPMessage> notifyPackets)
	{
		HTTPMessage m;
		
		if(device.Parent==null)
		{
			//
			// Root Device
			//
			m = new HTTPMessage();
			m.Directive = "NOTIFY";
			m.DirectiveObj = "*";
			m.AddTag("NT", "upnp:rootdevice");
			m.AddTag("Host","239.255.255.250:1900");
			m.AddTag("NTS", "ssdp:alive");
			m.AddTag("USN", String.format("%s::upnp:rootdevice", device.DeviceUDN));
			m.AddTag("Server", String.format("Android/%s, UPnP/1.0",android.os.Build.VERSION.RELEASE));
			notifyPackets.add(m);
		}
		
		//
		// Device Type
		//
		m = new HTTPMessage();
		m.Directive = "NOTIFY";
		m.DirectiveObj = "*";
		m.AddTag("NT", device.DeviceURN);
		m.AddTag("Host","239.255.255.250:1900");
		m.AddTag("NTS", "ssdp:alive");
		m.AddTag("USN", String.format("%s::%s", device.DeviceUDN,device.DeviceURN));
		m.AddTag("Server", String.format("Android/%s, UPnP/1.0",android.os.Build.VERSION.RELEASE));
		notifyPackets.add(m);
		
		//
		// Device UDN
		//
		m = new HTTPMessage();
		m.Directive = "NOTIFY";
		m.DirectiveObj = "*";
		m.AddTag("NT", device.DeviceUDN);
		m.AddTag("Host","239.255.255.250:1900");
		m.AddTag("NTS", "ssdp:alive");
		m.AddTag("USN", device.DeviceUDN);
		m.AddTag("Server", String.format("Android/%s, UPnP/1.0",android.os.Build.VERSION.RELEASE));
		notifyPackets.add(m);
		
		Iterator<UPnPService> sI = device.serviceList.iterator();
		while(sI.hasNext())
		{
			UPnPService S = sI.next();
			m = new HTTPMessage();
			m.Directive = "NOTIFY";
			m.DirectiveObj = "*";
			m.AddTag("NT", S.ServiceType);
			m.AddTag("Host","239.255.255.250:1900");
			m.AddTag("NTS", "ssdp:alive");
			m.AddTag("USN", String.format("%s::%s",device.DeviceUDN,S.ServiceType));
			m.AddTag("Server", String.format("Android/%s, UPnP/1.0",android.os.Build.VERSION.RELEASE));
			notifyPackets.add(m);
		}
		
		Iterator<UPnPDevice> dI = device.embeddedDeviceList.iterator();
		while(dI.hasNext())
		{
			UPnPDevice ed = dI.next();
			ProcessDevice(ed, notifyPackets);
		}
	}
	
	private AsyncUDPSocketHandler mSocketCallback = new AsyncUDPSocketHandler()
	{
		@Override
		public void OnReceiveFrom(SocketAddress local, InetAddress remoteAddress, int remotePort,
				byte[] data, int dataLength) 
		{
			HTTPMessage m = HTTPMessage.Parse(data, 0, dataLength);
			String uri = m.GetTag("Location");
			String urn = m.GetTag("ST");
			String udn = m.GetTag("USN");
			String MaxAge = m.GetTag("CACHE-CONTROL");
			MaxAge = MaxAge.substring(1+MaxAge.indexOf("=")).trim();
			int _MaxAge = 0;
			try
			{
				_MaxAge = Integer.valueOf(MaxAge);
			}
			catch(Exception x)
			{
				
			}
			
			if(udn.contains("::"))
			{
				udn = udn.substring(0, udn.indexOf("::"));
			}
			
			
			if(OnFind!=null)
			{
				OnFind.OnFind(((InetSocketAddress)local).getAddress(),urn, udn, _MaxAge,uri);
			}
		}
	};
	public void Find(String urn)
	{			
		HTTPMessage m = new HTTPMessage();
		m.Directive = "M-SEARCH";
		m.DirectiveObj = "*";
		m.AddTag("ST", urn);
		m.AddTag("MAN", "\"ssdp:discover\"");
		m.AddTag("Host", "239.255.255.250:1900");
		m.AddTag("MX", String.valueOf(MX_TIMEOUT));
		byte[] buffer = m.GetRawPacket();
		DatagramPacket p;
		try 
		{
			p = new DatagramPacket(buffer, 0, buffer.length, InetAddress.getByName("239.255.255.250"), 1900);
			U.Send(p);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public void SendRaw(DatagramPacket P, InetAddress localAddress)
	{
		U.Send(P, localAddress);
	}
}
