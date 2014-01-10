package opentools.upnp;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.jar.Attributes;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.message.BasicNameValuePair;

import android.util.SparseArray;

import opentools.ILib.AsyncHttpClientHandler;
import opentools.ILib.AsyncHttpServerHandler;
import opentools.ILib.FakeAsyncHttpClient;
import opentools.ILib.FakeAsyncHttpServer;
import opentools.ILib.HTTPMessage;
import opentools.ILib.ILibParsers;
import opentools.ILib.ILibXMLAttribute;
import opentools.ILib.ILibXMLNode;
import opentools.ILib.SmartTimerHandler;

public class UPnPService 
{
	public static int EventCallbackPort = 0;
	public String ServiceType;
	public String ServiceID;
	public String ControlURL;
	public String EventURL;
	private String SCPDURL;
	
	public UPnPDevice Parent;
	private UPnPService me = this;;
	protected String SubscriptionSID;
	
	public List<UPnPAction> actionList = new ArrayList<UPnPAction>();
	public List<UPnPStateVariable> stateVariableList = new ArrayList<UPnPStateVariable>();
	public Object userObject;
	
	protected Hashtable<String, UPnPEventSubscriber> subscriptionTable;
	private boolean scpdLoadAttempted = false;
	
	public boolean isScpdLoadAttempted()
	{
		return(scpdLoadAttempted);
	}
	
	protected AsyncHttpServerHandler mOnServiceHTTP = new AsyncHttpServerHandler()
	{
		@Override
		public void OnRequest(HTTPMessage request, HTTPMessage response) 
		{
			if(request.DirectiveObj.endsWith("/scpd.xml"))
			{
				response.AddTag("Content-Type", "text/xml");
				response.SetStringBuffer(GetSCPD());
			}
			else if(request.DirectiveObj.endsWith("/control"))
			{
				//
				// Remote Invocation
				//
				Parent.GetRootDevice().endpointTable.put(Thread.currentThread().hashCode(), request);
				ProcessRemoteInvocation(request.GetTag("SOAPACTION"), request.GetStringBuffer(), response);
				Parent.GetRootDevice().endpointTable.remove(Thread.currentThread().hashCode());
			}
			else if(request.DirectiveObj.endsWith("event"))
			{
				//
				// Event Subscription
				//
				if(request.GetTag("SID")!="")
				{
					//
					// Renewal
					//
					String key = request.GetTag("SID");
					synchronized(me)
					{
						if(subscriptionTable.containsKey(key))
						{
							// Renew
							String timeout = request.GetTag("Timeout");
							int period = 300;
							if(timeout!="")
							{
								try
								{
									timeout = timeout.substring(1+timeout.indexOf("-"));
									period = Integer.valueOf(timeout);
								}
								catch(Exception x)
								{
									//
									// Just throw away the exception, because we want to enforce a maximum duration of 
									// 5 minutes. We do this, because if the subscriber disappears without removing the subscription,
									// it will stall the delivery of events due to the TCP timeout. This exception should only be thrown
									// if the TIMEOUT header is missing, or contains "INFINITE".
									//
								}
								if(period > 300)
								{
									period = 300;
								}
							}
							
							subscriptionTable.get(key).RenewSubscription(period);
							response.StatusCode = 200;
							response.StatusData = "OK";
							response.AddTag("Server", String.format("Android/%s UPnP/1.0",android.os.Build.VERSION.RELEASE));
							response.AddTag("Timeout", String.format("Second-%d", period));
							response.AddTag("SID", key);
						}
						else
						{
							response.StatusCode = 412;
							response.StatusData = "Invalid SID";
						}
					}
				}
				else
				{
					//
					// New Subscription
					//
					if(HasUPnPEvents())
					{
						String callbackURL = request.GetTag("Callback");
						callbackURL = callbackURL.substring(1, callbackURL.indexOf(">"));
						String timeout = request.GetTag("Timeout");
						int eventPeriod = 300;
						try
						{
							timeout = timeout.substring(1+timeout.indexOf("-"));
							eventPeriod = Integer.valueOf(timeout);
						}
						catch(Exception x)
						{
							//
							// Just throw away the exception, because we want to enforce a maximum duration of 
							// 5 minutes. We do this, because if the subscriber disappears without removing the subscription,
							// it will stall the delivery of events due to the TCP timeout. This exception should only be thrown
							// if the TIMEOUT header is missing, or contains "INFINITE".
							//
						}
						if(eventPeriod > 300)
						{
							eventPeriod = 300;
						}
						UPnPEventSubscriber sub = new UPnPEventSubscriber(eventPeriod, callbackURL);
						synchronized(me)
						{
							subscriptionTable.put(sub.SubscriptionID, sub);
						}
						
						response.StatusCode = 200;
						response.StatusData = "OK";
						response.AddTag("Server", String.format("Android/%s UPnP/1.0",android.os.Build.VERSION.RELEASE));
						response.AddTag("Timeout", String.format("Second-%d", eventPeriod));
						response.AddTag("SID", sub.SubscriptionID);
							
						SendEvent(sub);  //ToDo: Race condition between when Response is sent, and when initial event is sent
					}
					else
					{
						response.StatusCode = 500;
						response.StatusData = "No Evented Variables";
					}
				}
			}
			else
			{
				response.StatusCode = 404;
				response.StatusData = "Not Found";
			}
		}
	};
	private void SendEvent(UPnPEventSubscriber sub, UPnPStateVariable v)
	{
		HTTPMessage m = new HTTPMessage();
		m.Directive = "NOTIFY";
		m.AddTag("Content-Type", "text/xml");
		m.AddTag("NT", "upnp:event");
		m.AddTag("NTS", "upnp:propchange");
		m.AddTag("SID", sub.SubscriptionID);
		m.AddTag("SEQ", Integer.valueOf(sub.GetNextSequenceID()).toString());
		m.SetStringBuffer(getEventBody(v));
		
		if(Parent.GetRootDevice().H==null)
		{
			Parent.GetRootDevice().H = new FakeAsyncHttpClient();
		}
		Parent.GetRootDevice().H.AddRequest(m, sub.GenaURL, null, null);
	}
	private void SendEvent(UPnPEventSubscriber sub)
	{
		SendEvent(sub,null);
	}
	private boolean HasUPnPEvents()
	{
		boolean retVal = false;
		
		Iterator<UPnPStateVariable> vi = stateVariableList.iterator();
		while(vi.hasNext())
		{
			UPnPStateVariable v = vi.next();
			if(v.isEvented)
			{
				retVal = true;
			}
		}
		return(retVal);
	}
	private String getEventBody(UPnPStateVariable v)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("<?xml version=\"1.0\"?>\r\n");
		sb.append("<e:propertyset xmlns:e=\"urn:schemas-upnp-org:event-1-0\">");
		
		if(v==null)
		{
			Iterator<UPnPStateVariable> vi = stateVariableList.iterator();
			while(vi.hasNext())
			{
				v = vi.next();
				if(v.isEvented)
				{
					sb.append(String.format("<e:property><%s>%s</%s></e:property>",v.Name,v.value,v.Name));
				}
			}
		}
		else
		{
			sb.append(String.format("<e:property><%s>%s</%s></e:property>",v.Name,v.value,v.Name));
		}
		
		sb.append("</e:propertyset>");
		return(sb.toString());
	}
	private void ProcessRemoteInvocation(String SOAPAction, String body, HTTPMessage response)
	{
		response.AddTag("Content-Type", "text/xml; charset=\"utf-8\"");
		
		SOAPAction = SOAPAction.substring(1+SOAPAction.indexOf("#"));
		SOAPAction = SOAPAction.substring(0,SOAPAction.length()-1);
		
		ILibXMLNode root = ILibParsers.ILibParseXML(body.toCharArray(), 0, body.length());
		ILibParsers.ILibProcessXMLNodeList(root);
		
		while(root!=null)
		{
			if(root.Name.equals("Envelope"))
			{
				root = root.Next;
				while(root!=null)
				{
					if(root.Name.equals("Body"))
					{
						root = root.Next;
						while(root!=null)
						{
							if(root.Name.equals(SOAPAction))
							{
								root = root.Next.StartTag!=0?root.Next:null;
								Attributes inVals = new Attributes();
								String sVal[] = new String[1];
								while(root!=null)
								{
									ILibParsers.ILibReadInnerXML(root, sVal);
									inVals.putValue(root.Name, sVal[0]);
									root = root.Peer;
								}
								Iterator<UPnPAction> aI = actionList.iterator();
								while(aI.hasNext())
								{
									UPnPAction A = aI.next();
									if(A.Name.equals(SOAPAction))
									{
										A.ProcessRemoteInvocation(inVals, response);
										return;
									}
								}
								//
								// Invalid SOAP Action
								//
								response.StatusCode = 500;
								response.StatusData = "Internal Error";
								response.SetStringBuffer(GetSOAPFault(401, "Invalid Action: " + SOAPAction));
								return;
							}
							else
							{
								root = root.Peer;
							}
						}
					}
					else
					{
						root = root.Peer;
					}
				}
			}
			else
			{
				root = root.Peer;
			}
		}
		
		response.StatusCode = 500;
		response.StatusData = "Internal Error";
		response.SetStringBuffer(GetSOAPFault(501, "Unable to Process XML"));
	}
	protected String GetSOAPFault(int errorCode, String errorDescription)
	{
		StringBuilder sb = new StringBuilder();
		
		sb.append("<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">");
		sb.append("<s:Body>");
		sb.append("<s:Fault>");
		sb.append("<faultcode>s:Client</faultcode>");
		sb.append("<faultstring>UPnPError</faultstring>");
		sb.append("<detail>");
		sb.append("<UPnPError xmlns=\"urn:schemas-upnp-org:control-1-0\">");
		sb.append(String.format("<errorCode>%d</errorCode>",errorCode));
		sb.append(String.format("<errorDescription>%s</errorDescription>",errorDescription));
		sb.append("</UPnPError>");
		sb.append("</detail>");
		sb.append("</s:Fault>");
		sb.append("</s:Body>");
		sb.append("</s:Envelope>");
		
		return(sb.toString());
	}
	protected String GetSCPD()
	{
		StringBuilder sb = new StringBuilder();
		
		sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\r\n");
		sb.append("<scpd xmlns=\"urn:schemas-upnp-org:service-1-0\">");
		sb.append("<specVersion>");
		sb.append("<major>1</major>");
		sb.append("<minor>0</minor>");
		sb.append("</specVersion>");
		sb.append("<actionList>");
		
		Iterator<UPnPAction> actionIterator = actionList.iterator();
		while(actionIterator.hasNext())
		{
			UPnPAction A = actionIterator.next();
			sb.append("<action>");
			sb.append("<name>");
			sb.append(A.Name);
			sb.append("</name>");
			sb.append("<argumentList>");
			
			Iterator<UPnPArgument> argIterator = A.argList.iterator();
			while(argIterator.hasNext())
			{
				UPnPArgument arg = argIterator.next();
				if(arg.Direction == ArgumentDirection.IN)
				{
					sb.append("<argument>");
					sb.append("<name>");
					sb.append(arg.Name);
					sb.append("</name>");
					sb.append("<direction>in</direction>");
					sb.append("<relatedStateVariable>");
					sb.append(arg.associatedStateVariable.Name);
					sb.append("</relatedStateVariable>");
					sb.append("</argument>");
				}
			}
			
			argIterator = A.argList.iterator();
			while(argIterator.hasNext())
			{
				UPnPArgument arg = argIterator.next();
				if(arg.Direction == ArgumentDirection.RETURN)
				{
					sb.append("<argument>");
					sb.append("<name>");
					sb.append(arg.Name);
					sb.append("</name>");
					sb.append("<direction>out</direction>");
					sb.append("<retval />");
					sb.append("<relatedStateVariable>");
					sb.append(arg.associatedStateVariable.Name);
					sb.append("</relatedStateVariable>");
					sb.append("</argument>");
					break;
				}
			}
			
			argIterator = A.argList.iterator();
			while(argIterator.hasNext())
			{
				UPnPArgument arg = argIterator.next();
				if(arg.Direction == ArgumentDirection.OUT)
				{
					sb.append("<argument>");
					sb.append("<name>");
					sb.append(arg.Name);
					sb.append("</name>");
					sb.append("<direction>out</direction>");
					sb.append("<relatedStateVariable>");
					sb.append(arg.associatedStateVariable.Name);
					sb.append("</relatedStateVariable>");
					sb.append("</argument>");
				}
			}
			sb.append("</argumentList>");
			sb.append("</action>");	
		}
		sb.append("</actionList>");
		sb.append("<serviceStateTable>");
		
		Iterator<UPnPStateVariable> varIterator = stateVariableList.iterator();
		while(varIterator.hasNext())
		{
			UPnPStateVariable v = varIterator.next();
			sb.append("<stateVariable");
			if(v.isEvented)
			{
				sb.append(" sendEvents=\"yes\"");
			}
			sb.append(">");
			sb.append("<name>");
			sb.append(v.Name);
			sb.append("</name>");
			sb.append("<dataType>");
			sb.append(v.varType);
			sb.append("</dataType>");
			
			//
			// Has Default Value?
			//
			if(v.defaultValue != null)
			{
				sb.append(String.format("<defaultValue>%s</defaultValue>", v.defaultValue));
			}
			
			//
			// Has Allowed Value Range?
			//
			if(v.min!=null && v.max!=null)
			{
				sb.append(String.format("<allowedValueRange><minimum>%s</minimum><maximum>%s</maximum>", v.min, v.max));
				if(v.step != null)
				{
					sb.append(String.format("<step>%s</step>", v.step));
				}
				sb.append("</allowedValueRange>");
			}
			
			//
			// Has Allowed Values?
			//
			if(v.allowedValues!=null)
			{
				sb.append("<allowedValueList>");
				Iterator<String> i = v.allowedValues.iterator();
				while(i.hasNext())
				{
					sb.append(String.format("<allowedValue>%s</allowedValue>", i.next()));
				}
				sb.append("</allowedValueList>");
			}
			
			sb.append("</stateVariable>");
		}
		sb.append("</serviceStateTable></scpd>");
		
		return(sb.toString());
	}
	private String GetValueFromXNode(ILibXMLNode n)
	{
		String[] val = new String[1];
		ILibParsers.ILibReadInnerXML(n,val);
		return(ILibParsers.UnEscapeXmlString(val[0]));
	}
	private String UriToAbsoluteUri(String uri)
	{
		String RelativeBase = Parent.LocationURL;
		RelativeBase = RelativeBase.substring(0, 1+RelativeBase.lastIndexOf("/"));
		String Root = RelativeBase.substring(0, RelativeBase.indexOf("/", 7));
		
		if(uri.startsWith("http://"))
		{
			return(uri);
		}
		else
		{
			if(uri.startsWith("/"))
			{
				return(Root + uri);
			}
			else
			{
				return(RelativeBase + uri);
			}
		}
	}

	public UPnPServiceEventHandler EventedParametersCallback;
	
	public UPnPService(String serviceType, String serviceID)
	{
		ServiceType = serviceType;
		ServiceID = serviceID;
		subscriptionTable = new Hashtable<String, UPnPEventSubscriber>();
	}
	public void AddAction(UPnPAction action)
	{
		action.mParent = this;
		actionList.add(action);
	}
	public void AddStateVariable(UPnPStateVariable stateVar)
	{
		stateVariableList.add(stateVar);
		stateVar.mParent = this;
	}
	public UPnPStateVariable getStateVariable(String varName)
	{
		Iterator<UPnPStateVariable> i = stateVariableList.iterator();
		while(i.hasNext())
		{
			UPnPStateVariable v = i.next();
			if(v.Name.equals(varName))
			{
				return(v);
			}
		}
		return(null);
	}
	protected void StateVariableUpdate(UPnPStateVariable sender)
	{
		if(sender.isEvented)
		{
			synchronized(this)
			{
				Enumeration<String> e = this.subscriptionTable.keys();
				List<UPnPEventSubscriber> expired = new ArrayList<UPnPEventSubscriber>();
				
				while(e.hasMoreElements())
				{
					String SID = e.nextElement();
					UPnPEventSubscriber s = subscriptionTable.get(SID);
					if(s.isValidSubscription())
					{
						SendEvent(s, sender);
					}
					else
					{
						expired.add(s);
					}
				}
				
				Iterator<UPnPEventSubscriber> i = expired.iterator();
				while(i.hasNext())
				{
					UPnPEventSubscriber s = i.next();
					subscriptionTable.remove(s.SubscriptionID);
				}
			}
		}
	}
	protected UPnPService(ILibXMLNode n, UPnPDevice p)
	{	
		Parent = p;
		EventedParametersCallback = null;
		
		n = n.Next;
		while(n!=null)
		{
			if(n.Name.equals("serviceType"))
			{
				ServiceType = GetValueFromXNode(n);
			}
			if(n.Name.equals("serviceId"))
			{
				ServiceID = GetValueFromXNode(n);
			}
			if(n.Name.equals("SCPDURL"))
			{
				SCPDURL = UriToAbsoluteUri(GetValueFromXNode(n));
			}
			if(n.Name.equals("controlURL"))
			{
				ControlURL = UriToAbsoluteUri(GetValueFromXNode(n));
			}
			if(n.Name.equals("eventSubURL"))
			{
				EventURL = UriToAbsoluteUri(GetValueFromXNode(n));
			}
			n = n.Peer;
		}
	}

	private AsyncHttpClientHandler mGenericInvokeResponse = new AsyncHttpClientHandler()
	{
		@Override
		public void OnResponse(HttpResponse response, HttpRequest request,
				Object State) 
		{
			int errorCode = 0;
			Attributes Parameters = new Attributes();
			GenericInvoke_StateObject giso = (GenericInvoke_StateObject)State;
			byte[] buffer = GetBufferFromResponse(response);
			String sbuf = new String(buffer);
			
			errorCode = response.getStatusLine().getStatusCode();
			
			switch(errorCode)
			{
				case 200:
				case 500:
					
					ILibXMLNode n = ILibParsers.ILibParseXML(sbuf.toCharArray(), 0, sbuf.length());
					ILibParsers.ILibProcessXMLNodeList(n);

					while(n!=null)
					{
						if(n.Name.equals("Envelope"))
						{
							n = n.Next;
							while(n!=null)
							{
								if(n.Name.equals("Body"))
								{
									n = n.Next;
									while(n!=null)
									{
										if(n.Name.equals(giso.methodName + "Response"))
										{
											errorCode = 0;
											if(n.Next != n.ClosingTag)
											{
												n = n.Next;
												while(n!=null)
												{
													Parameters.putValue(n.Name, GetValueFromXNode(n));
													n = n.Peer;
												}
											}
											else
											{
												// No out values
												n = null;
											}
										}
										else if(n.Name.equals("Fault"))
										{
											n = n.Next;
											while(n!=null)
											{
												if(n.Name.equals("detail"))
												{
													n = n.Next;
													while(n!=null)
													{
														if(n.Name.equals("UPnPError"))
														{
															n = n.Next;
															while(n!=null)
															{
																if(n.Name.equals("errorCode"))
																{
																	errorCode = Integer.parseInt(GetValueFromXNode(n));
																}
																n = n.Peer;
															}
														}
														else
														{
															n = n.Peer;
														}
													}
												}
												else
												{
													n = n.Peer;
												}
											}
										}
										else
										{
											n = n.Peer;
										}
									}
								}
								else
								{
									n = n.Peer;
								}
							}
						}
						else
						{
							n = n.Peer;
						}
					}
					break;
				default:
					errorCode = -1;
					break;
			}
	
			if(giso.userCallback!=null)
			{
				giso.userCallback.OnGenericInvoke(giso.methodName, errorCode, Parameters, giso.userStateObject1, giso.userStateObject2);
			}
		}	
	};
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
	public void GenericInvoke(String methodName, BasicNameValuePair[] inputParameters, Object userState1, Object userState2, GenericInvokeHandler Callback)
	{
		StringBuilder parameterBody = new StringBuilder();
		String path = ControlURL.substring(ControlURL.indexOf("/",7));
		String host = ControlURL.substring(7,ControlURL.indexOf("/",7)-1);
		
		HTTPMessage m = new HTTPMessage();
		m.Directive = "POST";
		m.DirectiveObj = path;
		m.AddTag("Host", host);
		
		m.AddTag("SOAPACTION", String.format("\"%s#%s\"", ServiceType, methodName));
		m.AddTag("Content-Type", "text/xml; charset=\"utf-8\"");
		
		for(BasicNameValuePair nvp:inputParameters)
		{
			Object name = nvp.getName();
			Object value = nvp.getValue();
			parameterBody.append(String.format("<%s>%s</%s>", name.toString(),value.toString(),name.toString()));
		}
				
		String body = String.format("<?xml version=\"1.0\" encoding=\"utf-8\"?><s:Envelope s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\" xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\"><s:Body><u:%s xmlns:u=\"%s\">%s</u:%s></s:Body></s:Envelope>", 
				methodName, ServiceType, parameterBody.toString(), methodName);
		m.SetStringBuffer(body);
		
		HttpRequest r = new BasicHttpEntityEnclosingRequest("POST", ControlURL);
		ILibParsers.CopyPacketToRequest(m, r);
		
		Parent.H.AddRequest(r, null, new GenericInvoke_StateObject(methodName, Callback, userState1, userState2), mGenericInvokeResponse);
	}
	
	private AsyncHttpServerHandler mEventCallback = new AsyncHttpServerHandler()
	{
		@Override
		public void OnRequest(HTTPMessage request, HTTPMessage response) 
		{
			Attributes eventParameters = new Attributes();
			String e = request.GetStringBuffer();
			ILibXMLNode n;
			
			n = ILibParsers.ILibParseXML(e.toCharArray(), 0, e.length());
			if(ILibParsers.ILibProcessXMLNodeList(n)==0)
			{
				while(n!=null)
				{
					if(n.Name.equals("propertyset"))
					{
						n = n.Next;
						while(n!=null)
						{
							if(n.Name.equals("property"))
							{
								eventParameters.putValue(n.Next.Name, GetValueFromXNode(n.Next));
							}
							n = n.Peer;
						}
					}
					else
					{
						n = n.Peer;
					}
				}
			}
			if(!eventParameters.isEmpty())
			{
				if(EventedParametersCallback!=null)
				{
					EventedParametersCallback.OnUPnPEvent(me, eventParameters);
				}
			}
		}
	};
	private AsyncHttpClientHandler mSubscribeCallback = new AsyncHttpClientHandler()
	{
		@Override
		public void OnResponse(HttpResponse response, HttpRequest request,
				Object State) 
		{
			final UPnPDevice P = Parent.GetRootDevice();
			if(response.getStatusLine().getStatusCode()==200)
			{
				SubscriptionSID = response.getHeaders("SID")[0].getValue();
				String timeout = response.getHeaders("TIMEOUT")[0].getValue();
				timeout = timeout.substring(1+timeout.indexOf("-"));
				int subTimeout = (1000*Integer.valueOf(timeout))/2;
				
				P.ParentCP.deviceMonitor.AddObject(me, subTimeout, new SmartTimerHandler()
				{
					@Override
					public void OnSmartTimerExpired(Object obj) 
					{
						HTTPMessage msg = new HTTPMessage();
						msg.Directive = "SUBSCRIBE";
						msg.AddTag("SID", SubscriptionSID);
						msg.AddTag("TIMEOUT", "Second-300");
						P.H.AddRequest(msg, me.EventURL, null, mSubscribeCallback);
					}
				});				
			}
		}
	};
	public void SubscribeForEvents()
	{
		UPnPDevice P = Parent.GetRootDevice();
		
		if(P.ParentCP.WS == null)
		{
			P.ParentCP.WS = new FakeAsyncHttpServer();
			if(P.ParentCP.WS.Start(UPnPService.EventCallbackPort)<0)
			{
				P.ParentCP.WS = null;
			}
		}

		if(P.ParentCP.WS!=null)
		{
			String vDir = "/" + Parent.DeviceUDN + "/" + ServiceID;
			P.ParentCP.WS.RegisterVirtualDirectory(vDir, mEventCallback);
			
			String Callback = String.format("<http://%s:%d%s>", P.LocalHost, P.ParentCP.WS.getLocalPort(),vDir);
			
			HTTPMessage m = new HTTPMessage();
			m.Directive = "SUBSCRIBE";
			m.AddTag("NT", "upnp:event");
			m.AddTag("CALLBACK", Callback);
			m.AddTag("TIMEOUT", "Second-300");
			
			P.H.AddRequest(m, EventURL, null, mSubscribeCallback);
		}
	}
	public void CancelSubscriptionForEvents()
	{
		UPnPDevice P = Parent.GetRootDevice();
		if(SubscriptionSID!="")
		{
			String vDir = "/" + Parent.DeviceUDN + "/" + ServiceID;
			P.ParentCP.WS.UnRegisterVirtualDirectory(vDir);
			P.ParentCP.deviceMonitor.RemoveObject(this);
			
			HTTPMessage m = new HTTPMessage();
			m.Directive = "UNSUBSCRIBE";
			m.AddTag("SID", SubscriptionSID);			
			P.H.AddRequest(m, EventURL, null, null);	
			SubscriptionSID = "";
		}
	}

	public List<UPnPArgument> GetActionParameters(String actionName)
	{	
		Iterator<UPnPAction> i = actionList.iterator();
		while(i.hasNext())
		{
			UPnPAction action = i.next();
			if(action.Name.equals(actionName))
			{
				return(action.argList);
			}
		}
		return(null);
	}
	public UPnPStateVariable GetActionParameterStateVariable(String actionName, String argumentName)
	{
		List<UPnPArgument> argList = GetActionParameters(actionName);
		if(argList!=null)
		{
			Iterator<UPnPArgument> i = argList.iterator();
			while(i.hasNext())
			{
				UPnPArgument arg = i.next();
				if(arg.Name.equals(argumentName))
				{
					return(arg.associatedStateVariable);
				}
			}
		}
		return(null);
	}
	public boolean HasAction(String ActionName)
	{
		Iterator<UPnPAction> i = actionList.iterator();
		while(i.hasNext())
		{
			if(i.next().Name.equals(ActionName))
			{
				return(true);
			}
		}
		return(false);
	}
	public boolean HasStateVariable(String ActionName)
	{
		return(getStateVariable(ActionName)!=null?true:false);
	}
	
	public void LoadAndProcessSCPD(Object userState, UPnPService_FinishedParsingSCPD userCallback)
	{
		HttpRequest r = new BasicHttpRequest("GET", SCPDURL);
		Parent.GetRootDevice().H.AddRequest(r, null, new Object[]{userState,userCallback}, new AsyncHttpClientHandler()
		{
			@Override
			public void OnResponse(HttpResponse response, HttpRequest request,
					Object State) 
			{
				Object userState = ((Object[])State)[0];
				UPnPService_FinishedParsingSCPD userCallback = (UPnPService_FinishedParsingSCPD)((Object[])State)[1];
				if(response.getStatusLine().getStatusCode()==200)
				{
					byte[] buffer = GetBufferFromResponse(response);
					String xml = new String(buffer);
					scpdLoadAttempted = true;
					ParseSCPDXML(xml, userState, userCallback);
				}
				else
				{
					if(userCallback!=null)
					{
						userCallback.OnFinishedParsingSCPD(me, false, userState);
					}
				}
			}
		});
	}
	private UPnPArgument ParseSCPDXML_argument(ILibXMLNode n)
	{
		String name="";
		ArgumentDirection direction = ArgumentDirection.IN;
		UPnPStateVariable relatedVar = null;

		n = n.Next;
		while(n!=null)
		{
			if(n.Name.equals("name"))
			{
				name = GetValueFromXNode(n);
			}
			else if (n.Name.equals("direction"))
			{
				String tmp = GetValueFromXNode(n);
				if(tmp.equals("in"))
				{
					direction = ArgumentDirection.IN;
				}
				else if(tmp.equals("out"))
				{
					direction = ArgumentDirection.OUT;
				}
			}
			else if(n.Name.equals("retval"))
			{
				direction = ArgumentDirection.RETURN;
			}
			else if(n.Name.equals("relatedStateVariable"))
			{
				relatedVar = getStateVariable(GetValueFromXNode(n));
			}
			n = n.Peer;
		}
		
		UPnPArgument retVal = new UPnPArgument(name, direction, relatedVar);
		return(retVal);
	}
	private void ParseSCPDXML_action(ILibXMLNode n)
	{
		ILibXMLNode argListNode = null;
		String name=null;
		List<UPnPArgument> argList = new ArrayList<UPnPArgument>();
		
		n = n.Next;
		while(n!=null)
		{
			if(n.Name.equals("name"))
			{
				name = GetValueFromXNode(n);
			}
			else if(n.Name.equals("argumentList"))
			{
				argListNode = n;
				n = n.Next;
				while(n!=null)
				{
					if(n.Name.equals("argument"))
					{
						argList.add(ParseSCPDXML_argument(n));
					}
					n = n.Peer;
				}
				n = argListNode;
			}
			n = n.Peer;
		}
		
		if(name!=null)
		{
			UPnPAction action = new UPnPAction(name, argList, null);
			AddAction(action);
		}
	}
	private void ParseSCPDXML_actionList(ILibXMLNode n)
	{
		n = n.Next;
		while(n!=null)
		{
			if(n.Name.equals("action"))
			{
				ParseSCPDXML_action(n);
			}
			n = n.Peer;
		}
	}
	private UPnPStateVariable ParseSCPDXML_stateVariable(ILibXMLNode n)
	{
		boolean evented = false;
		String name = null;
		String dataType = null;
		String min = null;
		String max = null;
		String step = null;
		String defaultValue = null;
		List<String> allowedValues = null;
		
		ILibXMLAttribute a = ILibParsers.ILibGetXMLAttributes(n);
		while(a!=null)
		{
			if(a.Name.equals("sendEvents"))
			{
				evented = a.Value.equals("yes")?true:false;
				break;
			}
		}
		
		n = n.Next;
		while(n!=null)
		{
			if(n.Name.equals("name"))
			{
				name = GetValueFromXNode(n);
			}
			else if(n.Name.equals("dataType"))
			{
				dataType = GetValueFromXNode(n);
			}
			else if(n.Name.equals("defaultValue"))
			{
				defaultValue = GetValueFromXNode(n);
			}
			else if(n.Name.equals("allowedValueList"))
			{
				allowedValues = new ArrayList<String>();
				ILibXMLNode tmp = n;
				n = n.Next;
				while(n!=null)
				{
					if(n.Name.equals("allowedValue"))
					{
						allowedValues.add(GetValueFromXNode(n));
					}
					n = n.Peer;
				}
				n = tmp;
			}
			else if(n.Name.equals("allowedValueRange"))
			{
				ILibXMLNode tmp = n;
				n = n.Next;
				while(n!=null)
				{
					if(n.Name.equals("minimum"))
					{
						min = GetValueFromXNode(n);
					}
					else if(n.Name.equals("maximum"))
					{
						max = GetValueFromXNode(n);
					}
					else if(n.Name.equals("step"))
					{
						step = GetValueFromXNode(n);
					}
					n = n.Peer;
				}
				n = tmp;
			}
			n = n.Peer;	
		}
		
		if(name!=null && dataType!=null)
		{
			UPnPStateVariable var = new UPnPStateVariable(name, dataType, evented);
			var.defaultValue = defaultValue;
			if(min!=null && max!=null)
			{
				var.SetRange(min, max, step);
			}
			if(allowedValues != null)
			{
				var.SetAllowedValues(allowedValues.toArray(new String[0]));
			}
			return(var);
		}
		else
		{
			return(null);
		}
	}
	private void ParseSCPDXML_serviceStateTable(ILibXMLNode n)
	{
		UPnPStateVariable sVar;
		
		n = n.Next;
		while(n!=null)
		{
			if(n.Name.equals("stateVariable"))
			{
				sVar = ParseSCPDXML_stateVariable(n);
				if(sVar!=null)
				{
					AddStateVariable(sVar);
				}
			}
			n = n.Peer;
		}
	}
	private void ParseSCPDXML(String xml, Object userState, UPnPService_FinishedParsingSCPD userCallback)
	{
		boolean parsedServiceStateTable = false;
		
		ILibXMLNode actionListStart = null;
		ILibXMLNode n = ILibParsers.ILibParseXML(xml.toCharArray(), 0, xml.length());
		if(ILibParsers.ILibProcessXMLNodeList(n)!=0)
		{
			return; // Error in XML
		}
		
		while(n!=null)
		{
			if(n.Name.equals("scpd"))
			{
				n = n.Next;
				while(n!=null)
				{
					if(n.Name.equals("actionList"))
					{
						if(!parsedServiceStateTable)
						{
							//
							// Parse this after we parse the serviceStateTable
							//
							actionListStart = n;
						}
						else
						{
							ParseSCPDXML_actionList(n);
						}
					}
					else if(n.Name.equals("serviceStateTable"))
					{
						parsedServiceStateTable = true;
						ParseSCPDXML_serviceStateTable(n);
						if(actionListStart!=null)
						{
							ParseSCPDXML_actionList(actionListStart);
						}
					}
					n = n.Peer;
				}
			}
			else
			{
				n = n.Peer;
			}
		}
		
		//
		// Finished Parsing SCPD
		//
		if(userCallback!=null)
		{
			userCallback.OnFinishedParsingSCPD(this, true, userState);
		}
	}
}
