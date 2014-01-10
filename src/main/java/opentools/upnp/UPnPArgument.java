package opentools.upnp;

public class UPnPArgument 
{
	public String Name;
	public ArgumentDirection Direction;
	public UPnPStateVariable associatedStateVariable;
	
	public UPnPArgument(String argName, ArgumentDirection argDirection, UPnPStateVariable associatedVar)
	{
		Name = argName;
		associatedStateVariable = associatedVar;
		Direction = argDirection;
	}
}
