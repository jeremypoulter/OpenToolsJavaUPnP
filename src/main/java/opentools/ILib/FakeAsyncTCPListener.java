package opentools.ILib;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Semaphore;

public class FakeAsyncTCPListener 
{
	public TCPSessionHandler SessionCallback = null;
	protected ThreadPoolExecutor tp;
	protected ILibThreadFactory mFactory = new ILibThreadFactory("FakeAsyncTCPListener");
	private int mLocalPort;	
	public static int MaxThreadCount = 5;
	
	protected class TCPSessionImpl implements TCPSession, Runnable, ForciblyInterruptible
	{
		FakeAsyncTCPListener mParent;
		private TCPSessionReceiveHandler receiveCallback = null;
		private Socket mSocket;
		private byte[] readBuffer;
		private Object userObj = null;
		private InputStream readStream = null;
		private OutputStream writeStream = null;
		
		private SocketAddress remoteAddr = null;
		
		protected TCPSessionImpl(byte[] buffer, Socket s, FakeAsyncTCPListener parent)
		{
			mParent = parent;
			mSocket = s;
			readBuffer = buffer;
			remoteAddr = s.getRemoteSocketAddress();

			try 
			{
				readStream = mSocket.getInputStream();
				writeStream = mSocket.getOutputStream();
			} catch (IOException e) 
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if(mParent.SessionCallback != null)
			{
				mParent.SessionCallback.OnTCPSession(this);
			}
		}

		@Override
		public void Send(byte[] buffer, int offset, int length) 
		{
			if(mSocket!=null)
			{
				try 
				{
					writeStream.write(buffer, offset, length);
				} catch (IOException e) 
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		@Override
		public void SetReceiveListener(TCPSessionReceiveHandler handler) 
		{
			receiveCallback = handler;	
		}

		@Override
		public void run() 
		{
			mParent.mFactory.CheckIn(this);
			/*
			try 
			{
				startWait.acquire();
			} catch (InterruptedException e1) 
			{
				return;
			}
			*/
			while(!Thread.interrupted())
			{
				try 
				{
					int bytesRead = readStream.read(readBuffer);
					if(bytesRead > 0)
					{
						if(receiveCallback!=null)
						{		
							receiveCallback.OnReceive(this, readBuffer, bytesRead);
						}
					}
					else
					{
						if(mParent.SessionCallback!=null)
						{
							mParent.SessionCallback.OnTCPSessionClosed(this);
						}
						break;
					}
				} catch (IOException e) 
				{
					if(mParent.SessionCallback!=null)
					{
						mParent.SessionCallback.OnTCPSessionClosed(this);
					}
					break;
				}
			}
			mParent.mFactory.CheckOut();
		}

		@Override
		public void Close() 
		{
			synchronized(this)
			{
				if(mSocket!=null)
				{
					try 
					{
						mSocket.shutdownInput();
						mSocket.shutdownOutput();
						mSocket.close();
					} catch (IOException e) 
					{
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					mSocket = null;
				}
			}
		}

		@Override
		public void SetUserObject(Object user) 
		{
			userObj = user;
		}

		@Override
		public Object GetUserObject() 
		{
			return(userObj);
		}

		@Override
		public void ForceInterrupt() 
		{
			Close();
		}

		@Override
		public void StartReceiving() 
		{
			mParent.tp.execute(this);		
		}

		@Override
		public SocketAddress GetRemoteSocketAddress() 
		{
			return(this.remoteAddr);
		}
	}
	public interface TCPSession
	{
		public void StartReceiving();
		public void Close();
		public void Send(byte[] buffer, int offset, int length);
		public void SetReceiveListener(TCPSessionReceiveHandler handler);
		public void SetUserObject(Object user);
		public SocketAddress GetRemoteSocketAddress();
		public Object GetUserObject();
	}
	public interface TCPSessionReceiveHandler
	{
		public void OnReceive(TCPSession sender, byte[] buffer, int bufferLength);
	}
	public interface TCPSessionHandler
	{
		public void OnTCPSession(TCPSession sender);
		public void OnTCPSessionClosed(TCPSession sender);
	}
	private class TCPTask implements Runnable
	{
		private ServerSocket mSocket;
		private FakeAsyncTCPListener mParent;
		
		public TCPTask(FakeAsyncTCPListener parent, ServerSocket mSock)
		{
			mParent = parent;
			mSocket = mSock;
		}

		@Override
		public void run() 
		{
			while(!Thread.interrupted())
			{
				try 
				{
					Socket s = mSocket.getChannel().accept().socket();
					
					TCPSessionImpl t = new TCPSessionImpl(new byte[4096], s, mParent);
					
					//mParent.tp.execute(t);
				} catch (IOException e) 
				{
					break;
				}
			}
		}
	}
	
	public FakeAsyncTCPListener(int localPort)
	{
		tp = new ThreadPoolExecutor(1, MaxThreadCount, 30, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());
		tp.setThreadFactory(mFactory);
		mLocalPort = localPort;
	}
	public boolean Start()
	{
		ServerSocket mSocket;
		try 
		{
			mSocket = ServerSocketChannel.open().socket();
			mSocket.bind(new InetSocketAddress(mLocalPort));
			tp.execute(new TCPTask(this, mSocket));
		} catch (IOException e) 
		{
			return(false);
		}
		return(true);
	}
	public void Stop()
	{
		tp.shutdownNow();
	}
}
