package opentools.ILib;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Random;
import java.util.Stack;

import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.entity.StringEntity;

public class ILibParsers 
{
	public static byte[] parseHexBinary(String s) 
	{
	    final int len = s.length();

	    // "111" is not a valid hex encoding.
	    if( len%2 != 0 )
	        throw new IllegalArgumentException("hexBinary needs to be even-length: "+s);

	    byte[] out = new byte[len/2];

	    for( int i=0; i<len; i+=2 ) 
	    {
	        int h = hexToBin(s.charAt(i  ));
	        int l = hexToBin(s.charAt(i+1));
	        if( h==-1 || l==-1 )
	            throw new IllegalArgumentException("contains illegal character for hexBinary: "+s);

	        out[i/2] = (byte)(h*16+l);
	    }

	    return out;
	}

	private static int hexToBin( char ch ) 
	{
	    if( '0'<=ch && ch<='9' )    return ch-'0';
	    if( 'A'<=ch && ch<='F' )    return ch-'A'+10;
	    if( 'a'<=ch && ch<='f' )    return ch-'a'+10;
	    return -1;
	}
	

	 public static Date dateFromString(String val)
	    {
	    	if(val.contains("T"))
	    	{	
	    		//
	    		// There is a time component
	    		//
	    		String datePart = val.substring(0, val.indexOf("T"));
	    		String timePart = val.substring(1+val.indexOf("T"));
				String TZ = "";


	    		//
	    		// Is a Timezone specified?
	    		//
	    		boolean hasTZ = false;
	    		if(timePart.endsWith("Z"))
	    		{
	    			hasTZ = true;
	    		}
	    		else
	    		{
	    			if(timePart.length()>=11)
	    			{
		                char c = timePart.charAt(timePart.length()-6);     
		                if(c=='-' || c=='+')
		                {
		                	hasTZ = true;
		                }
	    			}
	    		}
	    		
	    		//
	    		// Fix the time zone portion
	    		//
	    		if(hasTZ)
	    		{
	    			String baseTime;
	    			if(timePart.endsWith("Z"))
	    			{
	    				TZ = "GMT-00:00";
	    				baseTime = timePart.substring(0, timePart.length()-1);
	    			}
	    			else
	    			{
	    				baseTime = timePart.substring(0, timePart.length() - 6);
	    				TZ = "GMT" + timePart.substring(timePart.length() - 6);
	    			}
	    			timePart = baseTime;
	    		}
	    		else
	    		{
	    			TZ = "GMT-00:00";
	    		}
	    		
	    		//
	    		// Detect and fix fractional seconds
	    		//
	    		if(timePart.length()<6)
	    		{
	    			//
	    			// No Second Component, so lets add it
	    			//
	    			timePart = timePart + ":00:000";
	    		}
	    		else if(timePart.length()<9)
	    		{
	    			//
	    			// No Fractional component, so lets add it
	    			//
	    			timePart = timePart + ":000";
	    		}
	    		else if(timePart.charAt(8)=='.')
	    		{
	    			//
	    			// Convert fractional seconds to milliseconds
	    			//
		            String dec = timePart.substring(9);
		            timePart = timePart.substring(0,8);
		            int ms = (int)((Float.valueOf(dec).floatValue() / (float)100)*(float)1000);
		            timePart = String.format("%s:%d", timePart,ms);
	    		}
	    		try 
	    		{
					return(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss:SSSz").parse(String.format("%sT%s%s", datePart,timePart,TZ)));
				} catch (ParseException e) 
				{
					// TODO Auto-generated catch block
					return(null);
				}
	    	}
	    	else
	    	{
	    		//
	    		// No Time Component, so just use standard parsers
	    		//
	    		try 
	    		{
					return(new SimpleDateFormat().parse(val));
				} catch (ParseException e) 
				{
					// TODO Auto-generated catch block
					return(null);
				}
	    	}
	    }
	public static void CopyPacketToRequest(HTTPMessage packet, HttpRequest req)
	{
		Enumeration<String> keys = packet.Headers.keys();
		
		while(keys.hasMoreElements())
		{
			String key = keys.nextElement();
			String val = packet.GetTag(key);
			
			req.addHeader(key, val);
		}
		if(packet.Body != null)
		{
			if(req instanceof org.apache.http.HttpEntityEnclosingRequest)
			{
				HttpEntity entity;
				try 
				{
					entity = new StringEntity(packet.GetStringBuffer());
					((HttpEntityEnclosingRequest)req).setEntity(entity);
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
		}
	}
	public static char[] ILibGetSubArray(char[] inArray, int offset)
	{
		String val = String.copyValueOf(inArray, offset, inArray.length - offset);
		return(val.toCharArray());
	}
	public static String EscapeXmlString(String InString)
	{
		InString = InString.replace("&","&amp;");
		InString = InString.replace("<","&lt;");
		InString = InString.replace(">","&gt;");
		InString = InString.replace("\"","&quot;");
		InString = InString.replace("'","&apos;");
		return(InString);
	}
	public static String UnEscapeXmlString(String InString)
	{
		InString = InString.replace("&lt;","<");
		InString = InString.replace("&gt;",">");
		InString = InString.replace("&quot;","\"");
		InString = InString.replace("&apos;", "'");
		InString = InString.replace("&amp;","&");
		return(InString);
	}	
	public static int ILibString_IndexOfFirstWhiteSpace(char[] inString, int inStringLength)
	{
		//CR, LF, space, tab
		int i=0;
		for(i=0; i<inStringLength; ++i)
		{
			if (inString[i] == 13 || inString[i] == 10 || inString[i] == 9 || inString[i] == 32) return(i);
		}
		return(-1);
	}
	//
	// Determines if a buffer offset is a delimiter
	//
	public static boolean ILibIsDelimiter(char[] buffer, int offset, int buffersize, char[] Delimiter, int DelimiterLength)
	{
		//
		// For simplicity sake, we'll assume a match unless proven otherwise
		//
		int i=0;
		boolean RetVal = true;
		if (DelimiterLength>buffersize)
		{
			//
			// If the offset plus delimiter length is greater than the buffersize
			// There can't possible be a match, so don't bother looking
			//
			return(false);
		}

		for(i=0;i<DelimiterLength;++i)
		{
			if (buffer[offset+i]!=Delimiter[i])
			{
				//
				// Uh oh! Can't possibly be a match now!
				//
				RetVal = false;
				break;
			}
		}
		return(RetVal);
	}
	public static parser_result ILibParseString(char[] buffer, int offset, int length, char[] Delimiter, int DelimiterLength)
	{
		int OriginalOffset = offset;
		int i = 0;
		//char[] Token = null;
		String Token;
		int TokenLength = 0;
		parser_result RetVal;
		parser_result_field p_resultfield;

		RetVal = new parser_result();
		
		RetVal.FirstResult = null;
		RetVal.NumResults = 0;

		String oString = new String(buffer,offset,length);
		
		//
		// By default we will always return at least one token, which will be the
		// entire string if the delimiter is not found.
		//
		// Iterate through the string to find delimiters
		//
		//Token = ILibGetSubArray(buffer, offset);
		int tokenStart = 0;
		for(i = offset; i < length; ++i)
		{
			if (ILibIsDelimiter(buffer,i,length,Delimiter,DelimiterLength))
			{
				//
				// We found a delimiter in the string
				//
				p_resultfield = new parser_result_field();
				p_resultfield.data = new String(oString.substring(tokenStart, tokenStart+TokenLength)).toCharArray();
				p_resultfield.datalength = TokenLength;
				p_resultfield.NextResult = null;
				p_resultfield.OriginalData = buffer;
				p_resultfield.OffsetIntoOriginalData = OriginalOffset;
				if (RetVal.FirstResult != null)
				{
					RetVal.LastResult.NextResult = p_resultfield;
					RetVal.LastResult = p_resultfield;
				}
				else
				{
					RetVal.FirstResult = p_resultfield;
					RetVal.LastResult = p_resultfield;
				}

				//
				// After we populate the values, we advance the token to after the delimiter
				// to prep for the next token
				//
				++RetVal.NumResults;
				i = i + DelimiterLength -1;
				//Token = ILibGetSubArray(Token,TokenLength+DelimiterLength);
				tokenStart += (TokenLength+DelimiterLength);
				OriginalOffset += (TokenLength+DelimiterLength);
				TokenLength = 0;	
			}
			else
			{
				//
				// No match yet, so just increment this counter
				//
				++TokenLength;
			}
		}

		//
		// Create a result for the last token, since it won't be caught in the above loop
		// because if there are no more delimiters, than the entire last portion of the string since the 
		// last delimiter is the token
		//
		p_resultfield = new parser_result_field();
		p_resultfield.data = new String(oString.substring(tokenStart, tokenStart+TokenLength)).toCharArray();
		p_resultfield.datalength = TokenLength;
		p_resultfield.NextResult = null;
		p_resultfield.OriginalData = buffer;
		p_resultfield.OffsetIntoOriginalData = OriginalOffset;
		if (RetVal.FirstResult != null)
		{
			RetVal.LastResult.NextResult = p_resultfield;
			RetVal.LastResult = p_resultfield;
		}
		else
		{
			RetVal.FirstResult = p_resultfield;
			RetVal.LastResult = p_resultfield;
		}	
		++RetVal.NumResults;

		return(RetVal);
	}
	public static parser_result ILibParseStringAdv(char[] buffer, int offset, int length, char[] Delimiter, int DelimiterLength)
	{
		int OriginalOffset = offset;
		parser_result RetVal;
		int i=0;	
		char[] Token = null;
		int TokenLength = 0;
		parser_result_field p_resultfield;
		int Ignore = 0;
		char StringDelimiter=0;

		RetVal = new parser_result();
		RetVal.FirstResult = null;
		RetVal.NumResults = 0;

		//
		// By default we will always return at least one token, which will be the
		// entire string if the delimiter is not found.
		//
		// Iterate through the string to find delimiters
		//
		Token = ILibGetSubArray(buffer,offset);
		for(i = offset;i < (length+offset);++i)
		{
			if (StringDelimiter == 0)
			{
				if (buffer[i] == '"') 
				{
					//
					// Ignore everything inside double quotes
					//
					StringDelimiter = '"';
					Ignore = 1;
				}
				else
				{
					if (buffer[i] == '\'')
					{
						//
						// Ignore everything inside single quotes
						//
						StringDelimiter = '\'';
						Ignore = 1;
					}
				}
			}
			else
			{
				//
				// Once we isolated everything inside double or single quotes, we can get
				// on with the real parsing
				//
				if (buffer[i] == StringDelimiter)
				{
					Ignore = ((Ignore == 0)?1:0);
				}
			}
			if (Ignore == 0 && ILibIsDelimiter(buffer, i, length, Delimiter, DelimiterLength))
			{
				//
				// We found a delimiter in the string
				//
				p_resultfield = new parser_result_field();
				p_resultfield.data = Token;
				p_resultfield.datalength = TokenLength;
				p_resultfield.OriginalData = buffer;
				p_resultfield.OffsetIntoOriginalData = OriginalOffset;
				p_resultfield.NextResult = null;
				if (RetVal.FirstResult != null)
				{
					RetVal.LastResult.NextResult = p_resultfield;
					RetVal.LastResult = p_resultfield;
				}
				else
				{
					RetVal.FirstResult = p_resultfield;
					RetVal.LastResult = p_resultfield;
				}

				//
				// After we populate the values, we advance the token to after the delimiter
				// to prep for the next token
				//
				++RetVal.NumResults;
				i = i + DelimiterLength -1;
				OriginalOffset += (TokenLength + DelimiterLength);
				Token = ILibGetSubArray(Token,TokenLength+DelimiterLength);
				TokenLength = 0;	
			}
			else
			{
				//
				// No match yet, so just increment this counter
				//
				++TokenLength;
			}
		}

		//
		// Create a result for the last token, since it won't be caught in the above loop
		// because if there are no more delimiters, than the entire last portion of the string since the 
		// last delimiter is the token
		//
		p_resultfield = new parser_result_field();
		p_resultfield.data = Token;
		p_resultfield.datalength = TokenLength;
		p_resultfield.NextResult = null;
		p_resultfield.OriginalData = buffer;
		p_resultfield.OffsetIntoOriginalData = OriginalOffset;
		if (RetVal.FirstResult != null)
		{
			RetVal.LastResult.NextResult = p_resultfield;
			RetVal.LastResult = p_resultfield;
		}
		else
		{
			RetVal.FirstResult = p_resultfield;
			RetVal.LastResult = p_resultfield;
		}	
		++RetVal.NumResults;

		return(RetVal);
	}
	public static ILibXMLNode ILibParseXML(char[] buffer, int offset, int length)
	{
		parser_result xml;
		parser_result_field field;
		parser_result temp2;
		parser_result temp3;
		
		char[] TagName;
		int TagNameLength;
		int StartTag;
		int EmptyTag;
		int i;
		int wsi;

		ILibXMLNode RetVal = null;
		ILibXMLNode current = null;
		ILibXMLNode x = null;

		char[] NSTag;
		int NSTagLength;

		int CommentEnd = 0;
		int CommentIndex;

		//
		// Even though "technically" the first character of an XML document must be <
		// we're going to be nice, and not enforce that
		//
		while (buffer[offset]!='<' && length>0)
		{
			++offset;
			--length;
		}

		if (length==0)
		{
			// Garbage in Garbage out :)
			return(new ILibXMLNode());
		}

		//
		// All XML Elements start with a '<' character. If we delineate the string with 
		// this character, we can go from there.
		//
		xml = ILibParseString(buffer,offset,length,"<".toCharArray(),1);
		field = xml.FirstResult;
		while (field!=null)
		{
			//
			// Ignore the XML declarator
			// ToDo: Test this JAVA port
			//
			if (field.datalength !=0 && memcmp(field.data,"?",1)!=0 && (field.OffsetIntoOriginalData > CommentEnd))
			{
				if (field.datalength>3 && memcmp(field.data,"!--",3)==0)
				{
					//
					// XML Comment, find where it ends
					//
					CommentIndex = 3;
					String cmt = String.copyValueOf(field.data, 0, field.datalength);
					int cmti = cmt.indexOf("-->");
					if(cmti>0)
					{
						CommentIndex += cmti;
					}
					CommentEnd = field.OffsetIntoOriginalData + CommentIndex;
					field = field.NextResult;
					continue;
				}
				else
				{
					EmptyTag = 0;
					if (memcmp(field.data,"/",1)==0)
					{
						//
						// The first character after the '<' was a '/', so we know this is the
						// EndElement
						//
						StartTag = 0;
						field.data = ILibGetSubArray(field.data,1);
						field.datalength -= 1;
						field.OffsetIntoOriginalData += 1;
						//
						// If we look for the '>' we can find the end of this element
						//
						temp2 = ILibParseString(field.data,0,field.datalength,">".toCharArray(),1);
					}
					else
					{
						//
						// The first character after the '<' was not a '/' so we know this is a 
						// StartElement
						//
						StartTag = -1;
						//
						// If we look for the '>' we can find the end of this element
						//
						temp2 = ILibParseString(field.data,0,field.datalength,">".toCharArray(),1);
						if (temp2.FirstResult.datalength>0 && temp2.FirstResult.data[temp2.FirstResult.datalength-1]=='/')
						{
							//
							// If this element ended with a '/' this is an EmptyElement
							//
							EmptyTag = -1;
						}
					}
				}
				//
				// Parsing on the ' ', we can isolate the Element name from the attributes. 
				// The first token, being the element name
				//
				wsi = ILibString_IndexOfFirstWhiteSpace(temp2.FirstResult.data,temp2.FirstResult.datalength);
				//
				// Now that we have the token that contains the element name, we need to parse on the ":"
				// because we need to figure out what the namespace qualifiers are
				//
				temp3 = ILibParseString(temp2.FirstResult.data,0,wsi!=-1?wsi:temp2.FirstResult.datalength,":".toCharArray(),1);
				if (temp3.NumResults==1)
				{
					//
					// If there is only one token, there was no namespace prefix. 
					// The whole token is the attribute name
					//
					NSTag = null;
					NSTagLength = 0;
					TagName = temp3.FirstResult.data;
					TagNameLength = temp3.FirstResult.datalength;
				}
				else
				{
					//
					// The first token is the namespace prefix, the second is the attribute name
					//
					NSTag = temp3.FirstResult.data;
					NSTagLength = temp3.FirstResult.datalength;
					TagName = temp3.FirstResult.NextResult.data;
					TagNameLength = temp3.FirstResult.NextResult.datalength;
				}
				ILibDestructParserResults(temp3);

				//
				// Iterate through the tag name, to figure out what the exact length is, as
				// well as check to see if its an empty element
				//
				for(i=0;i<TagNameLength;++i)
				{
					if ( (TagName[i]==' ')||(TagName[i]=='/')||(TagName[i]=='>')||(TagName[i]=='\t')||(TagName[i]=='\r')||(TagName[i]=='\n') )
					{
						if (i!=0)
						{
							if (TagName[i]=='/')
							{
								EmptyTag = -1;
							}
							TagNameLength = i;
							break;
						}
					}
				}

				if (TagNameLength!=0)
				{
					//
					// Instantiate a new ILibXMLNode for this element
					//
					x = new ILibXMLNode();
					x.Name = String.copyValueOf(TagName, 0, TagNameLength);
					x.NameLength = TagNameLength;
					x.StartTag = StartTag;
					if(NSTag!=null)
					{
						x.NSTag = String.copyValueOf(NSTag, 0, NSTagLength);
						x.NSLength = NSTagLength;	
					}

					if (StartTag==0)
					{
						//
						// The Reserved field of StartElements point to te first character before
						// the '<'.
						//
						int StartingOffset = field.OffsetIntoOriginalData;
						do
						{
							--StartingOffset;
						}while (field.OriginalData[StartingOffset]=='<');
						//x.Reserved = ILibGetSubArray(field.OriginalData, StartingOffset);
						x.ReservedOffset = StartingOffset;
						x.ReservedEx = field.OriginalData;
					}
					else
					{
						//
						// The Reserved field of EndElements point to the end of the element
						//
						//x.Reserved = temp2.LastResult.data;
						x.ReservedOffset = temp2.LastResult.OffsetIntoOriginalData + field.OffsetIntoOriginalData;
						x.ReservedEx = field.OriginalData;
					}

					if (RetVal==null)
					{
						RetVal = x;
					}
					else
					{
						current.Next = x;
					}
					current = x;
					if (EmptyTag!=0)
					{
						//
						// If this was an empty element, we need to create a bogus EndElement, 
						// just so the tree is consistent. No point in introducing unnecessary complexity
						//
						x = new ILibXMLNode();
						x.Name = String.copyValueOf(TagName, 0, TagNameLength);
						x.NameLength = TagNameLength;
						if(NSTag!=null)
						{
							x.NSTag = String.copyValueOf(NSTag, 0, NSTagLength);
							x.NSLength = NSTagLength;
						}
						//x.Reserved = current.Reserved;
						x.ReservedOffset = current.ReservedOffset;
						current.EmptyTag = -1;
						current.Next = x;
						current = x;
					}
				}

				ILibDestructParserResults(temp2);
			}
			field = field.NextResult;
		}

		ILibDestructParserResults(xml);
		return(RetVal);
	}
	public static int ILibProcessXMLNodeList(ILibXMLNode nodeList)
	{
		int RetVal = 0;
		ILibXMLNode current = nodeList;
		ILibXMLNode temp;
		
		Stack<ILibXMLNode> TagStack = new Stack<ILibXMLNode>();

		//
		// Iterate through the node list, and setup all the pointers
		// such that all StartElements have pointers to EndElements,
		// And all StartElements have pointers to siblings and parents.
		//
		while (current!=null)
		{
			if ( current.Name == null ) return -1;
			if (memcmp(current.Name,"!",1)==0)
			{
				// Comment
				temp = current;
				current = TagStack.peek();
				if (current!=null)
				{
					current.Next = temp.Next;
				}
				else
				{
					current = temp;
				}
			}
			else if (current.StartTag!=0)
			{
				// Start Tag
				if(!TagStack.empty())
				{
					current.Parent = TagStack.peek();
				}
				else
				{
					current.Parent = null;
				}

				TagStack.push(current);
			}
			else
			{
				// Close Tag

				//
				// Check to see if there is supposed to be an EndElement
				//
				temp = TagStack.pop();
				if (temp!=null)
				{
					//
					// Checking to see if this EndElement is correct in scope
					//
					if (temp.NameLength==current.NameLength && memcmp(temp.Name,current.Name,current.NameLength)==0)
					{
						//
						// Now that we know this EndElement is correct, set the Peer
						// pointers of the previous sibling
						//
						if (current.Next!=null)
						{
							if (current.Next.StartTag!=0)
							{
								temp.Peer = current.Next;
							}
						}
						temp.ClosingTag = current;
						current.StartingTag = temp;
					}
					else
					{
						// Illegal Close Tag Order
						RetVal = -2;
						break;
					}
				}
				else
				{
					// Illegal Close Tag
					RetVal = -1;
					break;
				}
			}
			current = current.Next;
		}

		//
		// If there are still elements in the stack, that means not all the StartElements
		// have associated EndElements, which means this XML is not valid XML.
		//
		if (!TagStack.empty())
		{
			// Incomplete XML
			RetVal = -3;
			TagStack.clear();
		}

		return(RetVal);
	}
	public static ILibXMLAttribute ILibGetXMLAttributes(ILibXMLNode node)
	{
		ILibXMLAttribute RetVal = null;
		ILibXMLAttribute current = null;
		int cx;
		int EndReserved = (node.EmptyTag==0)?1:2;
		int i;
		
		parser_result xml;
		parser_result_field field,field2;
		parser_result temp2;
		parser_result temp3;


		//
		// The reserved field is used to show where the data segments start and stop. We
		// can also use them to figure out where the attributes start and stop
		//
		cx = node.ReservedOffset - 1;
		while (((char[])node.ReservedEx)[cx]!='<')
		{
			//
			// The Reserved field of the StartElement points to the first character after
			// the '>' of the StartElement. Just work our way backwards to find the start of
			// the StartElement
			//
			cx-=1;
		}
		cx += 1;

		String c = String.copyValueOf(ILibGetSubArray(((char[])node.ReservedEx),cx));
		//
		// Now that we isolated the string in between the '<' and the '>' we can parse the
		// string as delimited by ' ', because thats what delineates attributes. We need
		// to use ILibParseStringAdv because these attributes can be within quotation marks
		//
		//
		// But before we start, replace linefeeds and carriage-return-linefeeds to spaces
		//
		c = c.replace('\r', ' ');
		c = c.replace('\n', ' ');
		c = c.replace('\t', ' ');

		xml = ILibParseStringAdv(c.toCharArray(),0,node.ReservedOffset - cx - EndReserved," ".toCharArray(),1);
		field = xml.FirstResult;
		//
		// We skip the first token, because the first token, is the Element name
		//
		if (field != null) {field = field.NextResult;}

		//
		// Iterate through all the other tokens, as these are all attributes
		//
		while (field != null)
		{
			if (field.datalength > 0)
			{
				if (RetVal == null)
				{
					//
					// If we haven't already created an Attribute node, create it now
					//
					RetVal = new ILibXMLAttribute();
					RetVal.Next = null;
				}
				else
				{
					//
					// We already created an Attribute node, so simply create a new one, and
					// attach it on the beginning of the old one.
					//
					current = new ILibXMLAttribute();
					current.Next = RetVal;
					RetVal = current;
				}

				//
				// Parse each token by the ':'
				// If this results in more than one token, we can figure that the first token
				// is the namespace prefix
				//
				temp2 = ILibParseStringAdv(field.data,0,field.datalength,":".toCharArray(),1);
				if (temp2.NumResults == 1)
				{
					//
					// This attribute has no prefix, so just parse on the '='
					// The first token is the attribute name, the other is the value
					//
					RetVal.Prefix = null;
					RetVal.PrefixLength = 0;
					temp3 = ILibParseStringAdv(field.data,0,field.datalength,"=".toCharArray(),1);
					if (temp3.NumResults == 1)
					{
						//
						// There were whitespaces around the '='
						//
						field2 = field.NextResult;
						while (field2!=null)
						{
							if (!(field2.datalength==1 && memcmp(field2.data,"=",1)==0) && field2.datalength>0)
							{
								ILibDestructParserResults(temp3);
								field = field2;
								break;
							}
							field2 = field2.NextResult;
						}
					}
				}
				else
				{
					//
					// Since there is a namespace prefix, seperate that out, and parse the remainder
					// on the '=' to figure out what the attribute name and value are
					//
					RetVal.Prefix = String.copyValueOf(temp2.FirstResult.data, 0, temp2.FirstResult.datalength);
					RetVal.PrefixLength = temp2.FirstResult.datalength;
					temp3 = ILibParseStringAdv(field.data,RetVal.PrefixLength+1,field.datalength-RetVal.PrefixLength-1,"=".toCharArray(),1);
					if (temp3.NumResults==1)
					{
						//
						// There were whitespaces around the '='
						//
						field2 = field.NextResult;
						while (field2!=null)
						{
							if (!(field2.datalength==1 && memcmp(field2.data,"=",1)==0) && field2.datalength>0)
							{
								ILibDestructParserResults(temp3);
								field = field2;
								break;
							}
							field2 = field2.NextResult;
						}
					}			
				}
				//
				// After attaching the pointers, we can free the results, as all the data
				// is a pointer into the original string. We can think of the nodes we created
				// as templates. Once done, we don't need them anymore.
				//
				ILibDestructParserResults(temp2);
				RetVal.Parent = node;
				RetVal.Name = String.copyValueOf(temp3.FirstResult.data, 0, temp3.FirstResult.datalength);
				RetVal.Value = String.copyValueOf(temp3.LastResult.data, 0, temp3.LastResult.datalength);
				RetVal.NameLength = RetVal.Name.length();
				RetVal.ValueLength = RetVal.Value.length();
				// 
				// Remove the Quote or Apostraphe if it exists
				//
				if(RetVal.Value.startsWith("\"") || RetVal.Value.startsWith("'"))
				{
					RetVal.Value = RetVal.Value.substring(1, RetVal.Value.length()-1);
					RetVal.ValueLength -= 2;
				}
				ILibDestructParserResults(temp3);
			}
			field = field.NextResult;
		}

		ILibDestructParserResults(xml);
		return(RetVal);
	}
	@SuppressWarnings("unchecked")
	public static void ILibXML_BuildNamespaceLookupTable(ILibXMLNode node)
	{
		ILibXMLAttribute attr,currentAttr;
		ILibXMLNode current = node;

		//
		// Iterate through all the StartElements, and build a table of the declared namespaces
		//
		while (current!=null)
		{
			if (current.StartTag!=0)
			{
				//
				// Reserved2 is the HashTable containing the fully qualified namespace
				// keyed by the namespace prefix
				//
				current.Reserved2 = new Hashtable<String,String>();
				currentAttr = attr = ILibGetXMLAttributes(current);
				if (attr!=null)
				{
					//
					// Iterate through all the attributes to find namespace declarations
					//
					while (currentAttr!=null)
					{
						if (currentAttr.NameLength==5 && memcmp(currentAttr.Name,"xmlns",5)==0)
						{
							// Default Namespace Declaration
							//currentAttr.Value[currentAttr.ValueLength]=0;
							((Hashtable<String,String>)current.Reserved2).put("xmlns", currentAttr.Value);
						}
						else if (currentAttr.PrefixLength==5 && memcmp(currentAttr.Prefix,"xmlns",5)==0)
						{
							// Other Namespace Declaration
							//currentAttr->Value[currentAttr->ValueLength]=0;
							((Hashtable<String,String>)current.Reserved2).put(currentAttr.Name, currentAttr.Value);
						}
						currentAttr=currentAttr.Next;
					}
					ILibDestructXMLAttributeList(attr);
				}
			}
			current = current.Next;
		}
	}
	@SuppressWarnings("unchecked")
	public static String ILibXML_LookupNamespace(ILibXMLNode currentLocation, String prefix, int prefixLength)
	{
		ILibXMLNode temp = currentLocation;
		int done=0;
		String RetVal = "";

		//
		// If the specified Prefix is zero length, we interpret that to mean
		// they want to lookup the default namespace
		//
		if (prefixLength==0)
		{
			//
			// This is the default namespace prefix
			//
			prefix = "xmlns";
			prefixLength = 5;
		}


		//
		// From the current node, keep traversing up the parents, until we find a match.
		// Each step we go up, is a step wider in scope.
		//
		do
		{
			if (temp.Reserved2!=null)
			{
				if(((Hashtable<String,String>)temp.Reserved2).containsKey(prefix))
				{
					//
					// As soon as we find the namespace declaration, stop
					// iterating the tree, as it would be a waste of time
					//
					RetVal = ((Hashtable<String,String>)temp.Reserved2).get(prefix);
					done=1;
				}
			}
			temp = temp.Parent;
		}while (temp!=null && done==0);
		return(RetVal);
	}
	public static int ILibReadInnerXML(ILibXMLNode node, RefParameter<String> RetVal)
	{
		String[] val = new String[1];
		int rVal = ILibReadInnerXML(node,val);
		RetVal.value = val[0];
		return(rVal);
	}
	public static int ILibReadInnerXML(ILibXMLNode node, String[] RetVal)
	{
		ILibXMLNode x = node;
		int length = 0;
		Stack<ILibXMLNode> TagStack = new Stack<ILibXMLNode>();
		RetVal[0] = null;
		
		//
		// Starting with the current StartElement, we use this stack to find the matching
		// EndElement, so we can figure out what we need to return
		//

		do
		{
			if (x==null)
			{
				TagStack.clear();
				return(0);
			}
			if (x.StartTag!=0) {TagStack.push(x);}

			x = x.Next;

			if (x==null)
			{
				TagStack.clear();
				return(0);
			}
		}while (!(x.StartTag==0 && TagStack.pop()==node && x.NameLength==node.NameLength && memcmp(x.Name,node.Name,node.NameLength)==0));

		//
		// The Reserved fields of the StartElement and EndElement are used as pointers representing
		// the data segment of the XML
		//
		length = x.ReservedOffset - node.ReservedOffset - 1;
		if (length<0) {length=0;}
		RetVal[0] = new String(node.ReservedEx, node.ReservedOffset, length);
		//RetVal[0] = String.copyValueOf((char[])node.Reserved, 0, length);
		return(length);
	}
	public static void ILibDestructXMLAttributeList(ILibXMLAttribute attr) {
		// TODO Auto-generated method stub
		
	}
	public static int memcmp(String name, String name2, int nameLength) 
	{
		if(name.regionMatches(0, name2, 0, nameLength))
		{
			return(0);
		}
		else
		{
			return(1);
		}
	}
	public static int memcmp(char[] data, String string, int i) 
	{
		return(memcmp(String.copyValueOf(data), string, i));
	}
	public static void ILibDestructParserResults(parser_result pr) 
	{
		// TODO Auto-generated method stub
		
	}
	public static String ILibGenerateNonce(int numberOfBytes)
	{
		int i;
		String retVal = "";
		
		Random r;
		r = new Random();
		
		for(i=0;i<numberOfBytes;++i)
		{
			retVal += String.format("%02x", r.nextInt(256));
		}

		return(retVal);
	}
	public static String ILibMD5Hash(char[] inBuffer, int inBufferLength)
	{
		String retVal = "";
		String inVal = String.copyValueOf(inBuffer,0,inBufferLength);
		try 
		{
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(inVal.getBytes());
			byte[] hash = md.digest();
			for(int i=0;i<hash.length;++i)
			{
				retVal += String.format("%02x", hash[i]);
			}
		} catch (NoSuchAlgorithmException e) 
		{
			return("");
		}
		
		return(retVal);
	}
	public static String ILibCalculateHTTPDigest(String realm, String nonce, String cnonce, String qop, int counter, String method, String methodUri, String username, String password)
	{
		String R;
		String HA1,HA2,HR;

		String nc = String.format("%08d", counter);
		String A1 = String.format("%s:%s:%s", username,realm,password);
		String A2 = String.format("%s:%s", method,methodUri);

		HA1 = ILibMD5Hash(A1.toCharArray(),A1.length());
		HA2 = ILibMD5Hash(A2.toCharArray(),A2.length());

		R = String.format("%s:%s:%s:%s:%s:%s", HA1,nonce,nc,cnonce,qop,HA2);
		HR = ILibParsers.ILibMD5Hash(R.toCharArray(),R.length());

		return(HR);
	}
	public static void ILibDestructXMLNodeList(ILibXMLNode node)
	{
		
	}
}
