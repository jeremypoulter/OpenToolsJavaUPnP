package opentools.ILib;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;

public interface AsyncHttpClientHandler 
{
	public void OnResponse(HttpResponse response, HttpRequest request, Object State);
}
