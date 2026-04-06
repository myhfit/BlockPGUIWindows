package bp.tool;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.FlowLayout;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

import bp.nativehelper.BPNativeHelpers;
import bp.nativehelper.windows.Kernel32Helper;
import bp.ui.actions.BPActionConstCommon;
import bp.ui.actions.BPActionConstOSManagement;
import bp.ui.actions.BPActionHelpers;
import bp.ui.scomp.BPButton;
import bp.ui.scomp.BPCheckBox;
import bp.ui.util.UIStd;

public class BPToolGUIPowerManagerWin extends BPToolGUIBase<BPToolGUIPowerManagerWin.BPToolGUIContextPMW>
{
	public String getName()
	{
		return BPActionHelpers.getValue(BPActionConstOSManagement.TNAME_POWERMAN) + "(Windows)";
	}

	protected boolean checkRequirement()
	{
		Kernel32Helper helper = BPNativeHelpers.getInterface(Kernel32Helper.HELPER_NAME_K32);
		if (helper != null)
			return true;
		UIStd.err(new RuntimeException("Need kernel32 helper"));
		return false;
	}

	protected BPToolGUIContextPMW createToolContext()
	{
		return new BPToolGUIContextPMW();
	}

	protected static class BPToolGUIContextPMW implements BPToolGUIBase.BPToolGUIContext
	{
		protected JScrollPane m_scroll;

		public void initUI(Container par, Object... params)
		{
			m_scroll = new JScrollPane();
			{
				JPanel pmain = new JPanel();
				pmain.setLayout(new BoxLayout(pmain, BoxLayout.Y_AXIS));
				{
					JPanel pste = new JPanel();
					pste.setBorder(new TitledBorder("ThreadExecutionState"));
					pste.setLayout(new BoxLayout(pste, BoxLayout.Y_AXIS));
					JPanel pchk = new JPanel();
					pchk.setLayout(new FlowLayout());
					BPCheckBox chk0 = new BPCheckBox("ES_CONTINUOUS");
					BPCheckBox chk1 = new BPCheckBox("ES_SYSTEM_REQUIRED");
					BPCheckBox chk2 = new BPCheckBox("ES_DISPLAY_REQUIRED");
					BPCheckBox chk3 = new BPCheckBox("ES_AWAYMODE_REQUIRED");
					chk0.setMonoFont();
					chk1.setMonoFont();
					chk2.setMonoFont();
					chk3.setMonoFont();
					BPButton btn = new BPButton();
					btn.setText(BPActionHelpers.getValue(BPActionConstCommon.ACT_BTNSET));
					btn.setMonoFont();
					btn.addActionListener(e ->
					{
						int f = 0;
						f |= chk0.isSelected() ? Kernel32Helper.ES_CONTINUOUS : 0;
						f |= chk1.isSelected() ? Kernel32Helper.ES_SYSTEM_REQUIRED : 0;
						f |= chk2.isSelected() ? Kernel32Helper.ES_DISPLAY_REQUIRED : 0;
						f |= chk3.isSelected() ? Kernel32Helper.ES_AWAYMODE_REQUIRED : 0;
						setThreadExecutionState(f);
					});
					pchk.add(chk0);
					pchk.add(chk1);
					pchk.add(chk2);
					pchk.add(chk3);
					pste.add(pchk);
					pste.add(btn);
					pmain.add(pste);
				}

				pmain.add(Box.createVerticalGlue());
				m_scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
				m_scroll.setViewportView(pmain);
			}
			m_scroll.setBorder(new EmptyBorder(0, 0, 0, 0));
			par.add(m_scroll, BorderLayout.CENTER);
		}

		protected void setThreadExecutionState(int f)
		{
			Kernel32Helper helper = BPNativeHelpers.getInterface(Kernel32Helper.HELPER_NAME_K32);
			helper.setThreadExecutionState(f);
		}

		public void initDatas(Object... params)
		{
		}

		public void clearResource()
		{
		}
	}
}