package opentools.upnp;

import java.util.ArrayList;
import java.util.List;

public class UPnPStateVariable 
{
	public String Name;
	public boolean isEvented;
	public String varType;
	protected String value;
	protected UPnPService mParent;
	
	protected String min, max, step;
	protected String defaultValue;
	protected List<String> allowedValues;
	
	public UPnPStateVariable(String variableName, String variableType, boolean evented)
	{
		Name = variableName;
		varType = variableType;
		isEvented = evented;
	}
	public void SetValue(String newValue)
	{
		value = newValue;
		if(mParent!=null)
		{
			mParent.StateVariableUpdate(this);
		}
	}
	public void SetRange(String minVal, String maxVal, String stepVal)
	{
		min = minVal;
		max = maxVal;
		step = stepVal;
	}
	public void SetDefaultValue(String defValue)
	{
		value = defValue;
		defaultValue = defValue;
	}
	public void SetAllowedValues(String[] values)
	{
		allowedValues = new ArrayList<String>();
		for(int i=0;i<values.length;++i)
		{
			allowedValues.add(values[i]);
		}
	}
	public String[] GetAllowedValues()
	{
		if(allowedValues!=null)
		{
			return(allowedValues.toArray(new String[0]));
		}
		else
		{
			return(null);
		}
	}
	public String getMinRange()
	{
		return(min);
	}
	public String getMaxRange()
	{
		return(max);
	}
	public String getStep()
	{
		return(step);
	}
	@Override
	public boolean equals(Object o)
	{
		return(Name.equals(((UPnPStateVariable)o).Name));
	}
}
