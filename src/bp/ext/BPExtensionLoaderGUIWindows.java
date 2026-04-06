package bp.ext;

import bp.ui.frame.BPMainFrameIFC;
import bp.util.SystemUtil;
import bp.util.SystemUtil.SystemOS;

public class BPExtensionLoaderGUIWindows implements BPExtensionLoaderGUISwing
{
	public String getName()
	{
		return "GUI Windows";
	}

	public String[] getParentExts()
	{
		return new String[] { "GUI-Swing", "Windows" };
	}

	public String[] getDependencies()
	{
		return null;
	}

	public boolean checkSystem()
	{
		return SystemUtil.getOS() == SystemOS.Windows;
	}

	public void setup(BPMainFrameIFC mf)
	{
	}
}
