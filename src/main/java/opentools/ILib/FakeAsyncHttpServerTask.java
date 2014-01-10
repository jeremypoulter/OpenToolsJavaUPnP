package opentools.ILib;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.http.impl.DefaultHttpServerConnection;
import org.apache.http.params.BasicHttpParams;

public class FakeAsyncHttpServerTask  implements Runnable
{
	private FakeAsyncHttpServer Parent;
	private ServerSocket TheServerSocket;
	
	public FakeAsyncHttpServerTask(ServerSocket server, FakeAsyncHttpServer sender)
	{
		Parent = sender;
		TheServerSocket = server;
	}
	@Override
	public void run() 
	{
		synchronized(FakeAsyncHttpServer.SyncObject)
		{
			++FakeAsyncHttpServer.threadNumber;
			Thread.currentThread().setName(String.format("FakeAsyncHttpServer/Acceptor[%d] - Thread #%d", FakeAsyncHttpServer.instanceNumber,FakeAsyncHttpServer.threadNumber));
		}
		while(!Thread.interrupted())
		{
			try 
			{
				Socket s = TheServerSocket.accept();
                DefaultHttpServerConnection serverConnection = new ILibHttpServerConnection();
                
                serverConnection.bind(s, new BasicHttpParams());
                
                Parent.HandleRequest(serverConnection);
			} catch (IOException e) 
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
	}

}
