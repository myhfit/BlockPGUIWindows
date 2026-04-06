package bp.ui.actions;

import javax.swing.Action;

import bp.ui.frame.BPMainFrameIFC;

public class BPMainFrameActionsExtWin
{
	protected Action syshotkeys;

	protected BPMainFrameIFC m_mf;

	public BPMainFrameActionsExtWin(BPMainFrameIFC mf)
	{
		m_mf = mf;
		syshotkeys = BPAction.build("Hotkeys...").getAction();
	}

	public Action[] getSystemActions()
	{
		return new Action[] { syshotkeys };
	}
}
