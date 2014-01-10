package opentools.upnp;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.jar.Attributes;

import org.apache.http.message.BasicNameValuePair;

import opentools.ILib.HTTPMessage;

public class UPnPAction 
{
	protected UPnPService mParent;
	protected GenericRemoteInvokeHandler mHandler;
	public String Name;
	public List<UPnPArgument> argList;
	public UPnPAction(String actionName, List<UPnPArgument> parameters, GenericRemoteInvokeHandler invokeHandler)
	{
		Name = actionName;
		mHandler = invokeHandler;
		argList = new ArrayList<UPnPArgument>();
		
		Iterator<UPnPArgument> i = parameters.iterator();
		while(i.hasNext())
		{
			argList.add(i.next());
		}
	}
	protected void ProcessRemoteInvocation(Attributes inParams, HTTPMessage response)
	{
		//
		// Validate Parameters
		//
		Iterator<UPnPArgument> argI = argList.iterator();

		while(argI.hasNext())
		{
			UPnPArgument arg = argI.next();
			if(arg.Direction == ArgumentDirection.IN)
			{
				if(!inParams.containsKey(new java.util.jar.Attributes.Name(arg.Name)))
				{
					response.StatusCode = 500;
					response.StatusData = "Internal";
					response.SetStringBuffer(mParent.GetSOAPFault(402, String.format("[%s] Missing", arg.Name)));
					return;
				}
			}
		}
		
		List<BasicNameValuePair> outParams = new ArrayList<BasicNameValuePair>();
		try
		{
			mHandler.OnGenericRemoteInvoke(this, inParams, outParams);
		}
		catch(UPnPInvokeException ie)
		{
			response.StatusCode = 500;
			response.StatusData = "Internal";
			response.SetStringBuffer(mParent.GetSOAPFault(ie.UPnPErrorCode, ie.UPnPErrorDescription));
			return;
		}
		
		StringBuilder sb = new StringBuilder();
		sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\r\n");
		sb.append("<s:Envelope s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\" xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\">");
		sb.append("<s:Body>");
		sb.append("<u:");
		sb.append(Name);
		sb.append("Response");
		sb.append(" xmlns:u=\"");
		sb.append(mParent.ServiceType);
		sb.append("\" >");
		
		for(BasicNameValuePair nvp:outParams)
		{
			sb.append(String.format("<%s>%s</%s>",nvp.getName(), nvp.getValue(),nvp.getName()));
		}

		sb.append("</u:");
		sb.append(Name);
		sb.append("Response>");
		sb.append("</s:Body>");
		sb.append("</s:Envelope>");
		
		response.StatusCode = 200;
		response.StatusData = "OK";
		response.SetStringBuffer(sb.toString());
	}
}
