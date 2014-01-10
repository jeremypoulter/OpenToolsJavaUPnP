package opentools.ILib;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import android.os.Handler;
import android.os.Looper;

public class AsyncInputOutputReaderWriter 
{
	public interface AsyncInputOutputReaderWriterHandler
	{
		public void OnRead(AsyncInputOutputReaderWriter sender, InputStream iStream, byte[] buffer, int length);
		public void OnEndOfStream(AsyncInputOutputReaderWriter sender);
	}

	private Thread readerThread,writerThread;
	private InputStream mReadStream;
	private OutputStream mWriteStream;
	
	private Handler writerHandle = null;	
	private AsyncInputOutputReaderWriterHandler mUser = null;
	
	public AsyncInputOutputReaderWriter(InputStream iStream, OutputStream wStream, AsyncInputOutputReaderWriterHandler callback)
	{
		mReadStream = iStream;
		mWriteStream = wStream;
		mUser = callback;
	}
	public void Stop()
	{
		if(writerHandle!=null)
		{
			writerHandle.getLooper().quit();
		}	
		readerThread.interrupt();
	}
	public void Start(int bufferSize, boolean useCircularBuffer)
	{
		readerThread = new Thread(new RunnableEx<Object[]>(new Object[]{Integer.valueOf(bufferSize), Boolean.valueOf(useCircularBuffer)})
				{
					@Override
					public void run(Object[] param) 
					{
						int bufferSize = ((Integer)param[0]).intValue();
						boolean useCircularBuffer = ((Boolean)param[1]).booleanValue();
						int bytesRead;
						byte[] readBuffer = new byte[bufferSize];
	
						try 
						{
							ILibCircularBuffer circleBuffer=null;
							if(useCircularBuffer)
							{
								circleBuffer = new ILibCircularBuffer(bufferSize);
							}
							
							while(true)
							{
								bytesRead = mReadStream.read(readBuffer);
								if(bytesRead>0)
								{
									if(useCircularBuffer)
									{
										circleBuffer.getOutputStream().write(readBuffer, 0, bytesRead);
										if(mUser!=null)
										{
											mUser.OnRead(AsyncInputOutputReaderWriter.this, circleBuffer.getInputStream(), null, bytesRead);
										}
									}
									else
									{
										if(mUser!=null)
										{
											mUser.OnRead(AsyncInputOutputReaderWriter.this, null, readBuffer, bytesRead);
										}
									}
								}
								else
								{
									if(mUser!=null)
									{
										mUser.OnEndOfStream(AsyncInputOutputReaderWriter.this);
									}
									if(useCircularBuffer)
									{
										circleBuffer.close();
									}
									mReadStream.close();
									mWriteStream.close();
								}
							}
						} catch (IOException e) 
						{
							// TODO Auto-generated catch block
							e.printStackTrace();
							if(mUser!=null)
							{
								mUser.OnEndOfStream(AsyncInputOutputReaderWriter.this);
							}
						}
					}
				});
		readerThread.start();
		
		writerThread = new Thread(new Runnable()
		{
			@Override
			public void run() 
			{
				Looper.prepare();
				writerHandle = new Handler();
				Looper.loop();
			}	
		});
		writerThread.start();
	}
	public void Write(byte[] buffer, int offset, int length)
	{
		ByteBuffer b = ByteBuffer.allocate(length);
		b.put(buffer, offset, length);
		
		writerHandle.post(new RunnableEx<ByteBuffer>(b)
				{
					@Override
					public void run(ByteBuffer param)
					{
						try 
						{
							mWriteStream.write(param.array());
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				});
	}
	public void WriteEx(byte[] buffer, int offset, int length) throws IOException
	{
		mWriteStream.write(buffer, offset, length);
	}
}
