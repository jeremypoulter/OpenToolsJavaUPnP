package opentools.ILib;

public class ILibThread extends Thread
{
	private ILibThreadFactory mParent;
	
	public ILibThread(ILibThreadFactory parent, Runnable r)
	{
		super(r);
		mParent = parent;
	}
	@Override
	public void interrupt()
	{
		mParent.ForcelyInterruptMe(this);
		super.interrupt();
	}
}
