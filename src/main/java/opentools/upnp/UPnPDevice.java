package opentools.upnp;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Stack;
import java.util.jar.Attributes;

import android.util.SparseArray;

import opentools.ILib.AsyncHttpServerHandler;
import opentools.ILib.FakeAsyncHttpClient;
import opentools.ILib.FakeAsyncHttpServer;
import opentools.ILib.FakeAsyncMulticastSocket;
import opentools.ILib.HTTPMessage;
import opentools.ILib.ILibParsers;
import opentools.ILib.ILibXMLNode;
import opentools.ILib.SmartTimer;
import opentools.ILib.SmartTimerHandler;

public class UPnPDevice 
{
	public String DeviceURN;
	public String DeviceUDN;
	public String FriendlyName;
	public String LocationURL;
	public String LocalHost;
	
	public String Manufacturer;
	public String ManufacturerURL;
	public String ModelDescription;
	public String ModelName;
	public String ModelNumber;
	public String ModelURL;
	public String SerialNumber;
	
	public List<UPnPService> serviceList;
	public List<UPnPDevice> embeddedDeviceList;
	public UPnPDevice Parent;
	private UPnPDevice me = this;
	
	protected FakeAsyncHttpClient H;
	protected FakeAsyncHttpServer WS;
	
	protected UPnPControlPoint ParentCP = null;
	protected int MaxAge;
	protected SmartTimer internalTimer;
	protected boolean isDevice = false;
	private SSDP SSDPSender;
	private SSDPServer SSDPListener;
	protected SparseArray<HTTPMessage> endpointTable = new SparseArray<HTTPMessage>();
	protected Hashtable<String,Attributes> customTagTable = new Hashtable<String,Attributes>();
	
	
	public InetAddress GetCallerAddress()
	{
		HTTPMessage m = endpointTable.get(Thread.currentThread().hashCode());
		return(m.remoteAddress);
	}
	public int GetCallerPort()
	{
		HTTPMessage m = endpointTable.get(Thread.currentThread().hashCode());
		return(m.remotePort);
	}
	public InetAddress GetReceiverAddress()
	{
		HTTPMessage m = endpointTable.get(Thread.currentThread().hashCode());
		return(m.localAddress);
	}
	public int GetReceiverPort()
	{
		HTTPMessage m = endpointTable.get(Thread.currentThread().hashCode());
		return(m.localPort);
	}
	
	
	public int GetLocalPort()
	{
		return(GetRootDevice().WS.getLocalPort());
	}
	public FakeAsyncHttpServer GetWebServer()
	{
		return(GetRootDevice().WS);
	}
	public UPnPDevice GetRootDevice()
	{
		UPnPDevice retVal = this;
		while(retVal.Parent != null)
		{
			retVal = retVal.Parent;
		}
		return(retVal);
	}
	
	@Override
	public int hashCode()
	{
		return(this.DeviceUDN.hashCode());
	}
	public void AddCustomTag(String namespace, String TagName, String TagValue)
	{
		if(!customTagTable.containsKey(namespace))
		{
			customTagTable.put(namespace, new Attributes());
		}
		customTagTable.get(namespace).putValue(TagName, TagValue);
	}
	public Attributes GetCustomTags(String namespace)
	{
		return(customTagTable.get(namespace));
	}
	public String GetCustomTag(String namespace, String tagName)
	{
		String retVal = null;
		Attributes a = customTagTable.get(namespace);
		if(a!=null)
		{
			retVal = a.getValue(tagName);
		}
		return(retVal);
	}
	private void Init(ILibXMLNode n)
	{
		String[] val = new String[1];
		Stack<ILibXMLNode> nodeStack = new Stack<ILibXMLNode>();
		
		while(n!=null)
		{
			if(n.Name.equals("device"))
			{
				nodeStack.push(n);
				n = n.Next;
				while(n!=null)
				{					
					if(n.Name.equals("deviceType"))
					{
						ILibParsers.ILibReadInnerXML(n,val);
						DeviceURN = val[0];
					}
					else if(n.Name.equals("friendlyName"))
					{
						ILibParsers.ILibReadInnerXML(n, val);
						FriendlyName = val[0];
					}
					else if(n.Name.equals("UDN"))
					{
						ILibParsers.ILibReadInnerXML(n, val);
						DeviceUDN = val[0];
					}
					else if(n.Name.equals("serviceList"))
					{
						nodeStack.push(n);
						n = n.Next;
						while(n!=null)
						{
							if(n.Name.equals("service"))
							{
								serviceList.add(new UPnPService(n,this));
							}
							n = n.Peer;
						}
						n = nodeStack.pop();
					}
					else if(n.Name.equals("deviceList"))
					{
						nodeStack.push(n);
						n = n.Next;
						while(n!=null)
						{
							if(n.Name.equals("device"))
							{
								embeddedDeviceList.add(new UPnPDevice(this, n, LocationURL, H));
							}
							n = n.Peer;
						}
						n = nodeStack.pop();
					}
					else if(n.Name.equals("manufacturer"))
					{
						ILibParsers.ILibReadInnerXML(n, val);
						Manufacturer = val[0];	
					}
					else if(n.Name.equals("manufacturerURL"))
					{
						ILibParsers.ILibReadInnerXML(n, val);
						ManufacturerURL = val[0];	
					}
					else if(n.Name.equals("modelDescription"))
					{
						ILibParsers.ILibReadInnerXML(n, val);
						ModelDescription = val[0];	
					}
					else if(n.Name.equals("modelName"))
					{
						ILibParsers.ILibReadInnerXML(n, val);
						ModelName = val[0];	
					}
					else if(n.Name.equals("modelNumber"))
					{
						ILibParsers.ILibReadInnerXML(n, val);
						ModelNumber = val[0];	
					}
					else if(n.Name.equals("serialNumber"))
					{
						ILibParsers.ILibReadInnerXML(n, val);
						SerialNumber = val[0];	
					}
					else
					{
						String NS = ILibParsers.ILibXML_LookupNamespace(n, n.NSTag, n.NSLength);
						ILibParsers.ILibReadInnerXML(n, val);
						
						if(!customTagTable.containsKey(NS))
						{
							customTagTable.put(NS, new Attributes());
						}
						customTagTable.get(NS).putValue(n.Name, val[0]);
					}
					n = n.Peer;
				}
				n = nodeStack.pop();
			}
				
			if(n.Name.equals("root"))
			{
				n = n.Next;
			}
			else
			{
				n = n.Peer;
			}
		}
	}

	public UPnPDevice(String UniqueIdentifier, String friendly, String URN)
	{
		isDevice = true;
		DeviceURN = URN;
		DeviceUDN = UniqueIdentifier;
		FriendlyName = friendly;
		LocationURL = "";
		LocalHost = "";
		
		serviceList = new ArrayList<UPnPService>();
		embeddedDeviceList = new ArrayList<UPnPDevice>();
		Parent = null;
		
		H = null;
		WS = null;
		MaxAge = 0;
		internalTimer = null;	
		
		if(!DeviceUDN.startsWith("uuid:"))
		{
			DeviceUDN = "uuid:" + DeviceUDN;
		}
	}
	private SSDPServerHandler mOnSSDPServerMessage = new SSDPServerHandler()
	{
		@Override
		public void OnSSDPMessage(HTTPMessage message, InetAddress recievedOn,
				InetAddress receivedFromAddress, int receivedFromPort) 
		{
			if(message.Directive.equalsIgnoreCase("M-SEARCH") &&
					message.DirectiveObj.equalsIgnoreCase("*"))
			{
				String st = message.GetTag("ST");
				int mx = Integer.valueOf(message.GetTag("MX")).intValue();
				Random rG = new Random();
				
				List<HTTPMessage> resList = new ArrayList<HTTPMessage>();
				STMatch(st,resList);
				
				Iterator<HTTPMessage> i = resList.iterator();
				while(i.hasNext())
				{
					HTTPMessage r = i.next();
					r.AddTag("Server", String.format("Android/%s UPnP/1.0",android.os.Build.VERSION.RELEASE));
					r.AddTag("EXT","");
					r.AddTag("Cache-Control", String.format("max-age=%d", me.MaxAge));
					r.AddTag("Location", String.format("http://%s:%d/ddd.xml",recievedOn.getHostAddress(),me.WS.getLocalPort()));
					
					if(internalTimer==null)
					{
						internalTimer = new SmartTimer();
					}
					
					internalTimer.AddObject(new Object[]{r,recievedOn, receivedFromAddress, Integer.valueOf(receivedFromPort) }, 1000*rG.nextInt(mx) , mHandleMSEARCH);
				}
			}
		}	
	};
	private SmartTimerHandler mHandleMSEARCH = new SmartTimerHandler()
	{
		@Override
		public void OnSmartTimerExpired(Object obj) 
		{
			HTTPMessage r = (HTTPMessage)((Object[])obj)[0];
			InetAddress local = (InetAddress)((Object[])obj)[1];
			InetAddress fAddr = (InetAddress)((Object[])obj)[2];
			int fPort = ((Integer)((Object[])obj)[3]).intValue();
			byte[] buffer = r.GetRawPacket();
			
			DatagramPacket p = new DatagramPacket(buffer, buffer.length, fAddr, fPort);
			SSDPSender.SendRaw(p, local);
		}	
	};
	private AsyncHttpServerHandler mOnHTTP = new AsyncHttpServerHandler()
	{
		@Override
		public void OnRequest(HTTPMessage request, HTTPMessage response) 
		{
			if(request.Directive.equalsIgnoreCase("get"))
			{
				response.AddTag("Content-Type", "text/xml");
				response.SetStringBuffer(GenerateDDD());
			}
		}
	};
	private void STMatch(String ST, List<HTTPMessage> resList)
	{
		if(ST.equals("ssdp:all"))
		{
			HTTPMessage r;
			if(Parent==null)
			{
				//
				// Root
				//
				r = new HTTPMessage();
				r.StatusCode = 200;
				r.StatusData = "OK";
				r.AddTag("USN", String.format("%s::%s", DeviceUDN,"upnp:rootdevice"));
				r.AddTag("ST", "upnp:rootdevice");
				resList.add(r);	
			}
			// Device Type
			r = new HTTPMessage();
			r.StatusCode = 200;
			r.StatusData = "OK";
			r.AddTag("USN", String.format("%s::%s", DeviceUDN,DeviceURN));
			r.AddTag("ST", DeviceURN);
			resList.add(r);	
			
			// Device UDN
			r = new HTTPMessage();
			r.StatusCode = 200;
			r.StatusData = "OK";
			r.AddTag("USN", DeviceUDN);
			r.AddTag("ST", DeviceUDN);
			resList.add(r);	
			
			// Enumerate Services
			Iterator<UPnPService> sI = serviceList.iterator();
			while(sI.hasNext())
			{
				UPnPService S = sI.next();
				r = new HTTPMessage();
				r.StatusCode = 200;
				r.StatusData = "OK";
				r.AddTag("USN", String.format("%s::%s", DeviceUDN,S.ServiceType));
				r.AddTag("ST", S.ServiceType);
				resList.add(r);	
			}
						
			// Enumerate Embedded Devices
			Iterator<UPnPDevice> dI = embeddedDeviceList.iterator();
			while(dI.hasNext())
			{
				UPnPDevice ed = dI.next();
				ed.STMatch(ST, resList);
			}
			
			return;
		}
			
		if(ST.equals("upnp:rootdevice") && Parent==null)
		{
			HTTPMessage r = new HTTPMessage();
			r.StatusCode = 200;
			r.StatusData = "OK";
			r.AddTag("USN", String.format("%s::%s", DeviceUDN,ST));
			r.AddTag("ST", ST);
			resList.add(r);
		}
		else if(ST.equals(DeviceURN) || ST.equals(DeviceUDN))
		{
			HTTPMessage r = new HTTPMessage();
			r.StatusCode = 200;
			r.StatusData = "OK";
			r.AddTag("USN", String.format("%s::%s", DeviceUDN,DeviceURN));
			r.AddTag("ST", ST);
			resList.add(r);
		}
		else
		{
			Iterator<UPnPService> sI = serviceList.iterator();
			while(sI.hasNext())
			{
				UPnPService S = sI.next();
				if(ST.equals(S.ServiceType))
				{
					HTTPMessage r = new HTTPMessage();
					r.StatusCode = 200;
					r.StatusData = "OK";
					r.AddTag("USN", String.format("%s::%s", DeviceUDN,S.ServiceType));
					r.AddTag("ST", ST);
					resList.add(r);
				}
			}
			
			Iterator<UPnPDevice> dI = embeddedDeviceList.iterator();
			while(dI.hasNext())
			{
				UPnPDevice ed = dI.next();
				ed.STMatch(ST, resList);
			}
		}
	}
	protected String GenerateDDD_Device()
	{
		StringBuilder sb = new StringBuilder();
		
		sb.append("<device>");
		
		Enumeration<String> customTagKeys = customTagTable.keys();
		while(customTagKeys.hasMoreElements())
		{
			String NS = customTagKeys.nextElement();
			Attributes tags = customTagTable.get(NS);
			Iterator<Entry<Object,Object>> i = tags.entrySet().iterator();
			while(i.hasNext())
			{
				Entry<Object,Object> e = i.next();
				sb.append(String.format("<%s xmlns=\"%s\">%s</%s>", e.getKey().toString(), NS, e.getValue().toString(), e.getKey().toString()));
			}
		}
		
		sb.append("<deviceType>");
		sb.append(DeviceURN);
		sb.append("</deviceType>");
		sb.append("<friendlyName>");
		sb.append(FriendlyName);
		sb.append("</friendlyName>");
		sb.append("<manufacturer>");
		sb.append(Manufacturer);
		sb.append("</manufacturer>");
		sb.append("<manufacturerURL>");
		sb.append(ManufacturerURL);
		sb.append("</manufacturerURL>");
		sb.append("<modelDescription>");
		sb.append(ModelDescription);
		sb.append("</modelDescription>");
		sb.append("<modelName>");
		sb.append(ModelName);
		sb.append("</modelName>");
		sb.append("<modelNumber>");
		sb.append(ModelNumber);
		sb.append("</modelNumber>");
		sb.append("<modelURL>");
		sb.append(ModelURL);
		sb.append("</modelURL>");
		sb.append("<serialNumber>");
		sb.append(SerialNumber);
		sb.append("</serialNumber>");
		sb.append("<UDN>");
		sb.append(DeviceUDN);
		sb.append("</UDN>");
		sb.append("<serviceList>");
		
		Iterator<UPnPService> si = serviceList.iterator();
		while(si.hasNext())
		{
			UPnPService S = si.next();
			sb.append("<service>");
			sb.append("<serviceType>");
			sb.append(S.ServiceType);
			sb.append("</serviceType>");
			sb.append("<serviceId>");
			sb.append(S.ServiceID);
			sb.append("</serviceId>");
			sb.append("<SCPDURL>/");
			sb.append(DeviceUDN);
			sb.append("/");
			sb.append(S.ServiceID);
			sb.append("/scpd.xml</SCPDURL>");
			sb.append("<controlURL>/");
			sb.append(DeviceUDN);
			sb.append("/");
			sb.append(S.ServiceID);
			sb.append("/control</controlURL>");
			sb.append("<eventSubURL>/");
			sb.append(DeviceUDN);
			sb.append("/");
			sb.append(S.ServiceID);
			sb.append("/event</eventSubURL>");
			sb.append("</service>");
		}

		sb.append("</serviceList>");
		if(embeddedDeviceList.size()>0)
		{
			sb.append("<deviceList>");
			Iterator<UPnPDevice> e = embeddedDeviceList.iterator();
			while(e.hasNext())
			{
				UPnPDevice ed = e.next();
				sb.append(ed.GenerateDDD_Device());
			}
			sb.append("</deviceList>");
		}
		sb.append("</device>");
		return(sb.toString());
	}
	protected String GenerateDDD()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\r\n");
		sb.append("<root xmlns=\"urn:schemas-upnp-org:device-1-0\">");
		sb.append("<specVersion>");
		sb.append("<major>1</major>");
		sb.append("<minor>0</minor>");
		sb.append("</specVersion>");
		sb.append(GenerateDDD_Device());
		sb.append("</root>");
		
		return(sb.toString());
	}
	public void StopServer()
	{
		UPnPDevice root = this.GetRootDevice();
		root.SSDPSender.Stop();
		root.SSDPListener.Stop();
		
		if(root.H!=null)
		{
			root.H.Shutdown();
		}
		if(root.WS!=null)
		{
			root.WS.Stop();
		}
		if(internalTimer!=null)
		{
			internalTimer.Flush();
		}
	}
	public void StartServer(int MaxAgeTimeout, int localPort)
	{
		UPnPDevice root = this.GetRootDevice();
		if(root.WS==null)
		{
			root.WS = new FakeAsyncHttpServer();
			root.WS.RegisterVirtualDirectory("/ddd.xml", root.mOnHTTP);
		}
		
		Iterator<UPnPService> i = serviceList.iterator();
		while(i.hasNext())
		{
			UPnPService s = i.next();
			root.WS.RegisterVirtualDirectory("/"+DeviceUDN+"/"+s.ServiceID+"/*", s.mOnServiceHTTP);
		}
		
		Iterator<UPnPDevice> di = embeddedDeviceList.iterator();
		while(di.hasNext())
		{
			UPnPDevice ed = di.next();
			ed.StartServer(MaxAgeTimeout, localPort);
		}
		
		if(root.DeviceUDN == DeviceUDN)
		{
			MaxAge = MaxAgeTimeout;
			WS.Start(localPort);
			SSDPSender = new SSDP(this);
			SSDPListener = new SSDPServer(mOnSSDPServerMessage);
		}
	}
	public void AddEmbeddedDevice(UPnPDevice eDevice)
	{
		if(isDevice)
		{
			eDevice.Parent = this;
			embeddedDeviceList.add(eDevice);
		}
	}
	public void AddService(UPnPService svc)
	{
		if(isDevice)
		{
			serviceList.add(svc);
			svc.Parent = this;
		}
	}
	protected UPnPDevice(UPnPDevice _Parent, ILibXMLNode n, String location, FakeAsyncHttpClient _client)
	{
		Parent = _Parent;
		H = _client;
		WS = null;
		LocationURL = location;
		serviceList = new ArrayList<UPnPService>();
		embeddedDeviceList = new ArrayList<UPnPDevice>();
		
		Init(n);	
	}
	
	protected UPnPDevice(String xml, String location, FakeAsyncHttpClient _client)
	{
		Parent = null;
		H = _client;
		WS = null;
		LocationURL = location;
		serviceList = new ArrayList<UPnPService>();
		embeddedDeviceList = new ArrayList<UPnPDevice>();
		

		ILibXMLNode n;
		n = ILibParsers.ILibParseXML(xml.toCharArray(), 0, xml.length());
		ILibParsers.ILibProcessXMLNodeList(n);
		ILibParsers.ILibXML_BuildNamespaceLookupTable(n);
		
		Init(n);
	}
	protected void GetDevice(String deviceType, List<UPnPDevice> inList)
	{
		ListIterator<UPnPDevice> e = embeddedDeviceList.listIterator();
		while(e.hasNext())
		{
			UPnPDevice d = e.next();
			if(d.DeviceURN.startsWith(deviceType))
			{
				inList.add(d);
			}
		}

		e = embeddedDeviceList.listIterator();
		while(e.hasNext())
		{
			e.next().GetDevice(deviceType, inList);
		}
	}
	public List<UPnPDevice> GetDevice(String deviceType)
	{
		List<UPnPDevice> retVal = new ArrayList<UPnPDevice>();
		
		GetDevice(deviceType,retVal);
		return(retVal);
	}
	public List<UPnPService> GetService(String serviceType)
	{	
		List<UPnPService> retVal = new ArrayList<UPnPService>();
		
		ListIterator<UPnPService> e = serviceList.listIterator();
		while(e.hasNext())
		{
			UPnPService s = e.next();
			if(s.ServiceType.startsWith(serviceType))
			{
				retVal.add(s);
			}
		}	
		return(retVal);
	}
	public UPnPService GetServiceByID(String serviceID)
	{
		ListIterator<UPnPService> e = serviceList.listIterator();
		while(e.hasNext())
		{
			UPnPService s = e.next();
			if(s.ServiceID.equals(serviceID))
			{
				return(s);
			}
		}	
		return(null);
	}

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UPnPDevice that = (UPnPDevice) o;

        if (!DeviceUDN.equals(that.DeviceUDN)) return false;

        return true;
    }
}
