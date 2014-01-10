package opentools.ILib;

public class RefParameter<T>
{
	public T value;
	
	public RefParameter()
	{
		value = null;
	}
	public RefParameter(T init)
	{
		value = init;
	}
}
