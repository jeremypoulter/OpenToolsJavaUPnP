package opentools.ILib;

import android.annotation.SuppressLint;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

@SuppressLint("NewApi")
public class ILibCircularBuffer 
{	
	private mOutputStream mOutput;
	private mInputStream mInput;
	
	private int available = 0;
	
	public ILibCircularBuffer(int size) throws IOException
	{
		mOutput = new mOutputStream(this);
		mInput = new mInputStream(mOutput,size,this);
	}
	protected void BytesWritten(int count)
	{
		synchronized(this)
		{
			available += count;
		}
	}
	protected void BytesRead(int count)
	{
		synchronized(this)
		{
			available -= count;
		}
	}
	protected int getBytesAvailable()
	{
		synchronized(this)
		{
			return(available);
		}
	}
	public InputStream getInputStream()
	{
		return(mInput);
	}
	public OutputStream getOutputStream()
	{
		return(mOutput);
	}
	public void close()
	{
		if(mInput!=null && mOutput!=null)
		{
			try 
			{
				mInput.close();
				mOutput.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			mInput = null;
			mOutput = null;
		}
	}
	private class mInputStream extends PipedInputStream
	{
		private ILibCircularBuffer mParent;
		
		public mInputStream(PipedOutputStream s, int size, ILibCircularBuffer parent) throws IOException
		{
			super(s,size);
			
			mParent = parent;
		}

		@Override
		public synchronized int read() throws IOException 
		{
			int v = super.read();
			mParent.BytesRead(1);
			return(v);
		}

		@Override
		public synchronized int read(byte[] bytes, int offset, int byteCount)
				throws IOException 
		{
			int v = super.read(bytes, offset, byteCount);
			mParent.BytesRead(v);
			return(v);
		}

		@Override
		public synchronized int available() throws IOException 
		{
			return(mParent.getBytesAvailable());
		}
		
		
	}
	private class mOutputStream extends PipedOutputStream
	{
		private ILibCircularBuffer mParent;
		
		public mOutputStream(ILibCircularBuffer parent)
		{
			mParent = parent;
		}
		

		@Override
		public void write(int oneByte) throws IOException 
		{
			mParent.BytesWritten(1);
			super.write(oneByte);
		}

		
	}

}
