package opentools.ILib;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.security.KeyManagementException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HandshakeCompletedEvent;
import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class FakeAsyncTCPClient 
{
	public static int MaxThreadCount = 5;
	private ThreadPoolExecutor tp;
	private ILibThreadFactory factory = new ILibThreadFactory("FakeAsyncTCPClient");
	
	public FakeAsyncTCPClient()
	{
		this(MaxThreadCount);
	}
		
	public FakeAsyncTCPClient(int maxThreads)
	{
		tp = new ThreadPoolExecutor(1, MaxThreadCount, 30, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());
		tp.setThreadFactory(factory);
	}
	
	public void ResolveDns(String hostname, DnsResolverHandler callback)
	{
		tp.execute(new RunnableEx<Object[]>(new Object[]{hostname,callback})
				{
					@Override
					public void run(Object[] param) 
					{
						String h = (String)param[0];
						DnsResolverHandler d = (DnsResolverHandler)param[1];
						
						try 
						{
							InetAddress a = InetAddress.getByName(h);
							d.OnDnsResolve(a, true);
						} catch (UnknownHostException e) 
						{
							e.printStackTrace();
							d.OnDnsResolve(null, false);
						}
					}
				});
	}
	
	public void TLSConnect(InetAddress remoteAddress, int remotePort, String serverCertHash, Object stateObject, TCPClientHandler handler) throws NoSuchAlgorithmException, KeyManagementException
	{
		SSLContext ssl = SSLContext.getInstance("TLS");
		
		if(serverCertHash!=null)
		{
			ssl.init(null, new TrustManager[]{new SimpleTrustManager(serverCertHash)}, null);
		}
		else
		{
			ssl.init(null, null, null);
		}
		
		tp.execute(new TCPClientTask(ssl, stateObject, factory, remoteAddress, remotePort, handler));
	}
	
	public void Connect(InetAddress remoteAddress, int remotePort, TCPClientHandler handler)
	{
		tp.execute(new TCPClientTask(factory, remoteAddress, remotePort, handler));
	}
	public void Stop()
	{
		try
		{
			tp.shutdownNow();
		}
		catch(Exception e)
		{
			
		}
	}
	public interface TCPClientHandler
	{
		public void OnConnect(TCPClient sender);
		public void OnDisconnect(TCPClient sender);
		public void OnReceive(TCPClient sender, byte[] buffer, int bufferLength);
	}
	public interface TCPClientReadStreamHandler
	{
		public void OnReceive(TCPClient sender, InputStream s);
	}
	
	public interface TCPClient
	{
		public void Close();
		public void Send(byte[] buffer, int offset, int length);
		public void SendAsync(byte[] buffer, int offset, int length);
		public Object getStateObject();
		public String getTLSCipher();
		public void SetUseInputStream(TCPClientReadStreamHandler handler, int size) throws IOException;
		public void SetUseManualStreams(RefParameter<InputStream> inputStream, RefParameter<OutputStream> outputStream);
	}
	
	private class SimpleTrustManager implements X509TrustManager
	{
		private String mServerCertHash;
		
		public SimpleTrustManager(String serverCertHash)
		{
			mServerCertHash = serverCertHash;
		}
		@Override
		public void checkClientTrusted(X509Certificate[] chain, String authType)
				throws CertificateException 
		{
			throw new CertificateException("SimpleTrustManager does not provide client authentication");
		}

		@Override
		public void checkServerTrusted(X509Certificate[] chain, String authType)
				throws CertificateException 
		{
			try 
			{
				String hash = getThumbPrint(chain[0]);
				if(hash.compareToIgnoreCase(mServerCertHash)!=0)
				{
					throw new CertificateException("Certificate not trusted");
				}
			} catch (NoSuchAlgorithmException e) 
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
				throw new CertificateException("Certificate not trusted");
			}
		}

		@Override
		public X509Certificate[] getAcceptedIssuers() 
		{
			return(null);
		}
		private String getThumbPrint(X509Certificate cert) 
        	throws NoSuchAlgorithmException, CertificateEncodingException 
        {
	        MessageDigest md = MessageDigest.getInstance("SHA-1");
	        byte[] der = cert.getEncoded();
	        md.update(der);
	        byte[] digest = md.digest();
	        return hexify(digest);
        }

    	private String hexify (byte bytes[]) 
    	{
	        char[] hexDigits = {'0', '1', '2', '3', '4', '5', '6', '7', 
	                        '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
	
	        StringBuffer buf = new StringBuffer(bytes.length * 2);
	
	        for (int i = 0; i < bytes.length; ++i) 
	        {
	            buf.append(hexDigits[(bytes[i] & 0xf0) >> 4]);
	            buf.append(hexDigits[bytes[i] & 0x0f]);
	        }
	
	        return(buf.toString());
    	}
		
	}
	
	public class TCPClientTask implements TCPClient, Runnable, ForciblyInterruptible
	{
		private ILibCircularBuffer mCircularBuffer = null;
		private TCPClientReadStreamHandler mStreamHandler = null;
		private boolean isUsingManualStreams = false;
		
		private InetAddress destAddress;
		private int destPort;
		private TCPClientHandler callback;
		private OutputStream WriteStream = null;
		private InputStream ReadStream = null;
		private Socket mSock = null;
		private Socket mSock2 = null;
		
		private ILibThreadFactory mFactory;
		private SSLContext mSSL;
		private Object mStateObject;
		
		private String mTLSCipher = null;
		
		public TCPClientTask(SSLContext ssl, Object stateObject, ILibThreadFactory factory, InetAddress remoteAddress, int remotePort, TCPClientHandler handler)
		{
			mStateObject = stateObject;
			mSSL = ssl;
			mFactory = factory;
			destAddress = remoteAddress;
			destPort = remotePort;
			callback = handler;
		}
		public TCPClientTask(ILibThreadFactory factory, InetAddress remoteAddress, int remotePort, TCPClientHandler handler)
		{
			this(null,null,factory,remoteAddress,remotePort,handler);
		}
		
		@Override
		public Object getStateObject()
		{
			return(mStateObject);
		}
		@Override
		public String getTLSCipher() 
		{
			return(mTLSCipher);
		}
		@Override
		public void run() 
		{
			mFactory.CheckIn(this);

			try 
			{
				if(mSSL==null)
				{	
					mSock = SocketChannel.open().socket();
					mSock.connect(new InetSocketAddress(destAddress, destPort));
				}
				else
				{
					mSock2 = SocketChannel.open().socket();
					mSock2.connect(new InetSocketAddress(destAddress, destPort));
					mSock = mSSL.getSocketFactory().createSocket(mSock2, destAddress.toString(), destPort, true);
					((SSLSocket)mSock).addHandshakeCompletedListener(new HandshakeCompletedListener()
					{
						@Override
						public void handshakeCompleted(
								HandshakeCompletedEvent event) 
						{
							mTLSCipher = event.getCipherSuite();
						}		
					});
					((SSLSocket)mSock).startHandshake();
				}
				WriteStream = mSock.getOutputStream();
				ReadStream = mSock.getInputStream();
				if(callback!=null)
				{
					callback.OnConnect(this);
				}

				if(isUsingManualStreams)
				{
					mFactory.CheckOut();
					return;
				}
				
				byte[] readBuffer = new byte[4096];
				int bytesRead = 0;
				
				do
				{
					bytesRead = ReadStream.read(readBuffer);
					if(bytesRead>0)
					{
						if(mStreamHandler!=null)
						{
							mCircularBuffer.getOutputStream().write(readBuffer, 0, bytesRead);
							mStreamHandler.OnReceive(this, mCircularBuffer.getInputStream());
						}
						else if(callback!=null)
						{
							callback.OnReceive(this, readBuffer, bytesRead);
						}

					}
				}while(bytesRead > 0);
				if(callback!=null)
				{
					callback.OnDisconnect(this);
				}

				
				
			} catch (IOException e) 
			{
				if(callback!=null)
				{
					callback.OnDisconnect(this);
				}
			}
			mFactory.CheckOut();
		}

		@Override
		public void Send(byte[] buffer, int offset, int length) 
		{
			if(WriteStream!=null)
			{
				try 
				{
					WriteStream.write(buffer, offset, length);
				} catch (IOException e) 
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		@Override
		public void Close() 
		{
			Thread temp = new Thread(new Runnable()
			{
				@Override
				public void run() 
				{
					if(mSock!=null)
					{
						try 
						{
							mSock.shutdownInput();
							mSock.shutdownOutput();
						}
						catch(UnsupportedOperationException e)
						{
							e.printStackTrace();
						} 
						catch (IOException e) 
						{
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						try 
						{
							mSock.close();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			});
			temp.start();
		}
		@Override
		public void ForceInterrupt() 
		{
			Close();
		}
		@Override
		public void SetUseInputStream(TCPClientReadStreamHandler handler, int size) throws IOException 
		{
			mCircularBuffer = new ILibCircularBuffer(size);
			mStreamHandler = handler;
		}
		@Override
		public void SendAsync(byte[] buffer, int offset, int length) 
		{
			ByteBuffer b = ByteBuffer.allocate(length-offset);
			b.put(buffer, offset, length);
			
			tp.execute(new RunnableEx<ByteBuffer>(b)
					{
						@Override
						public void run(ByteBuffer param) 
						{
							byte[] buffer = param.array();
							if(WriteStream!=null)
							{
								try 
								{
									WriteStream.write(buffer, 0, buffer.length);
								} catch (IOException e) 
								{
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
						}
					});
			
		}
		@Override
		public void SetUseManualStreams(RefParameter<InputStream> inputStream,
				RefParameter<OutputStream> outputStream) 
		{
			isUsingManualStreams = true;
			inputStream.value = ReadStream;
			outputStream.value = WriteStream;
		}
		
	}
	
	public interface DnsResolverHandler
	{
		public void OnDnsResolve(InetAddress addr, boolean success);
	}
}
