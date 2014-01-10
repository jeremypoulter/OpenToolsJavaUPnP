package opentools.upnp;

public interface UPnPDeviceHandler 
{
	public void OnAddedDevice(UPnPDevice device);
	public void OnRemovedDevice(UPnPDevice device);
}
