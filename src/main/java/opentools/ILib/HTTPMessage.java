package opentools.ILib;

/**
 * Title:        <p>
 * Description:  <p>
 * Copyright:    Copyright (c) Bryan Roe<p>
 * Company:      <p>
 * @author Bryan Roe
 * @version 1.0
 */

import java.io.*;
import java.net.InetAddress;
import java.util.*;

import org.apache.http.HttpEntity;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.InputStreamEntity;

public class HTTPMessage
{
  protected Hashtable<String, String> Headers = new Hashtable<String,String>();
  protected byte[] Body;
  protected HttpEntity mEntity = null;

  public String Directive;
  public String DirectiveObj;

  public int StatusCode;
  public String StatusData;

  public Object StateObject = null;
  
  public InetAddress localAddress = null;
  public InetAddress remoteAddress = null;
  public int localPort = 0;
  public int remotePort = 0;
  
  public HTTPMessage clone()
  {
	  HTTPMessage retVal = new HTTPMessage();
	  retVal.Directive = Directive;
	  retVal.DirectiveObj = DirectiveObj;
	  retVal.StatusCode = StatusCode;
	  retVal.StatusData = StatusData;
	  retVal.Body = Body.clone();
	  
	  Enumeration<String> e = Headers.keys();
	  while(e.hasMoreElements())
	  {
		  String key = e.nextElement();
		  retVal.AddTag(key, Headers.get(key));
	  }
	  
	  return(retVal);
  }

  public HTTPMessage()
  {
    Headers = new Hashtable<String,String>();
    Directive = "";
    DirectiveObj = "";
    StatusCode = -1;
    StatusData = "";
    Body = new byte[0];
  }

  public Enumeration<String> getHeaderTags()
  {
	  return(Headers.keys());
  }
  public void AddTag(String TagName, String TagData)
  {
    Headers.put(TagName.toUpperCase(),TagData);
  }
  public void RemoveTag(String TagName)
  {
	  Headers.remove(TagName.toUpperCase());
  }
  public String GetTag(String TagName)
  {
    String val = (String)Headers.get(TagName.toUpperCase());
    if(val==null)
    {
      return("");
    }
    else
    {
      return(val);
    }
  }
  public String GetStringPacket() throws Exception
  {
    return(new String(GetRawPacket()));
  }
  public byte[] GetRawPacket()
  {
    Enumeration en = Headers.keys();
    ByteArrayOutputStream bostream = new ByteArrayOutputStream();
    String Tag;
    String TagData;

    try
    {
      if(StatusCode!=-1)
      {
        bostream.write(("HTTP/1.1 " + (new Integer(StatusCode)).toString() + " " + StatusData + "\r\n").getBytes());
      }
      else
      {
        bostream.write((Directive + " " + DirectiveObj + " HTTP/1.1\r\n").getBytes());
      }
      while(en.hasMoreElements())
      {
        Tag = (String)en.nextElement();
        TagData = (String)Headers.get(Tag);
        bostream.write((Tag + ":" + TagData + "\r\n").getBytes());
      }
      bostream.write(("Content-Length:" + Body.length + "\r\n").getBytes());
      bostream.write(("\r\n").getBytes());
      bostream.write(Body);
      byte[] buffer = bostream.toByteArray();
      bostream.close();
      return(buffer);
    }
    catch(Exception e)
    {
      return(new byte[0]);
    }
  }
  public byte[] GetBodyBuffer()
  {
    return(Body);
  }
  public String GetStringBuffer()
  {
    if(Body.length==0)
    {
      return("");
    }
    else
    {
      return(new String(Body));
    }
  }
  public void SetBodyBuffer(byte[] buffer)
  {
    Body = buffer;
  }

  public void setEntity(HttpEntity e)
  {
	  mEntity = e;
  }
  
  public HttpEntity getEntity()
  {
	  HttpEntity retVal = null;
	  
	  if(Body!=null && Body.length > 0)
	  {
		  retVal = new ByteArrayEntity(Body);
	  }
	  else if(mEntity!=null)
	  {
		  retVal = mEntity;
	  }
	  return(retVal);
  }
  public void SetStringBuffer(String buffer)
  {
    Body = buffer.getBytes();
  }
  static public HTTPMessage Parse(byte[] buffer)
  {
    return(HTTPMessage.Parse(buffer,0,buffer.length));
  }
  static public HTTPMessage Parse(byte[] buffer, int offset, int length)
  {
    String temp = new String(buffer,offset,length);
    HTTPMessage RetVal = new HTTPMessage();
    int ContentLength = 0;
    int BodyStart = temp.indexOf("\r\n\r\n")+4;
    if(BodyStart==3)
    {
      System.out.println(temp + "*");
    }
    temp = temp.substring(0,temp.indexOf("\r\n\r\n")+2);

    int StartIDX = 0;
    int EndIDX = temp.indexOf("\r\n");
    String Line;
    String Tag;
    String TagData;
    do
    {
      Line = temp.substring(StartIDX,EndIDX);
      if(StartIDX==0)
      {
        if(Line.toUpperCase().startsWith("HTTP/")==true)
        {
          Tag = Line.substring(Line.indexOf(" ")+1);
          TagData = Tag.substring(0,Tag.indexOf(" "));
          RetVal.StatusCode = Integer.parseInt(TagData);
          RetVal.StatusData = Tag.substring(Tag.indexOf(" ")+1);
        }
        else
        {
          RetVal.Directive = Line.substring(0,Line.indexOf(" ")).toUpperCase();
          RetVal.DirectiveObj = java.net.URLDecoder.decode(
            Line.substring(Line.indexOf(" ")+1,Line.indexOf(" ",Line.indexOf(" ")+1))
            );
        }
      }
      else
      {
        Tag = Line.substring(0,Line.indexOf(":")).toUpperCase().trim();
        if(Tag.indexOf(":") == Line.length()-1)
        {
          TagData = "";
        }
        else
        {
          TagData = Line.substring(Line.indexOf(":")+1).trim();
        }
        if(Tag.compareTo("CONTENT-LENGTH")!=0)
        {
          RetVal.AddTag(Tag,TagData);
        }
        else
        {
          ContentLength = Integer.parseInt(TagData);
        }
      }
      StartIDX += Line.length()+2;
      EndIDX = temp.indexOf("\r\n",StartIDX);
    }while(StartIDX<temp.length());

    byte[] body;
    if(ContentLength>0)
    {
      RetVal.Body = new byte[ContentLength];
      java.lang.System.arraycopy(buffer,BodyStart,RetVal.Body,0,RetVal.Body.length);
    }
    else
    {
      RetVal.Body = new byte[0];
    }
    return(RetVal);
  }
  static public int SizeToRead(byte[] buffer)
  {
    return(SizeToRead(buffer,0,buffer.length));
  }
  static public int SizeToRead(byte[] buffer,int start, int length)
  {
    String temp = new String(buffer,0,length);
    int x = temp.indexOf("\r\n\r\n")+4;
    temp = temp.substring(0,temp.indexOf("\r\n\r\n")+2).toUpperCase();
    if(temp.indexOf("CONTENT-LENGTH")==-1)
    {
      return(0);
    }
    temp = temp.substring(temp.indexOf("CONTENT-LENGTH"));
    temp = temp.substring(0,temp.indexOf("\r\n"));
    temp = temp.substring(temp.indexOf(":")+1).trim();
    x += Integer.parseInt(temp);
    return(x);
  }
}
