package bp.tool;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;

import bp.BPGUICore;
import bp.locale.BPLocaleConstCC;
import bp.locale.BPLocaleConstCoreDict;
import bp.locale.BPLocaleHelpers;
import bp.os.BPOSHandlers_Win;
import bp.os.process.BPProcessHandler.ProcessInfo;
import bp.os.process.BPProcessHandler_Win;
import bp.res.BPResource;
import bp.res.BPResourceFileSystem;
import bp.ui.actions.BPAction;
import bp.ui.actions.BPActionConstCommon;
import bp.ui.actions.BPActionConstOSManagement;
import bp.ui.actions.BPActionHelpers;
import bp.ui.container.BPToolBarSQ;
import bp.ui.dialog.BPDialogSelectResource2;
import bp.ui.dialog.BPDialogSelectResource2.SELECTSCOPE;
import bp.ui.res.icon.BPIconResV;
import bp.ui.scomp.BPTable;
import bp.ui.scomp.BPTable.BPTableRendererFileSize;
import bp.ui.scomp.BPTextField;
import bp.ui.table.BPTableFuncsBase;
import bp.ui.util.UIStd;
import bp.ui.util.UIUtil;
import bp.util.ClassUtil;
import bp.util.SystemUtil;

public class BPToolGUIProcessManagerWin extends BPToolGUIBase<BPToolGUIProcessManagerWin.BPToolGUIContextPMW>
{
	public String getName()
	{
		return BPActionHelpers.getValue(BPActionConstOSManagement.TNAME_PROCESSMAN) + "(Windows)";
	}

	protected boolean checkRequirement()
	{
		if (ClassUtil.getTClass("com.sun.jna.Native", ClassUtil.getExtensionClassLoader()) != null)
			return true;
		UIStd.err(new RuntimeException("Need JNA in class path"));
		return false;
	}

	protected BPToolGUIContextPMW createToolContext()
	{
		return new BPToolGUIContextPMW();
	}

	protected static class BPToolGUIContextPMW implements BPToolGUIBase.BPToolGUIContext
	{
		protected JScrollPane m_scroll;
		protected BPTable<ProcessInfo> m_tbps;
		protected Timer m_timer;
		protected Action m_actautorefresh;
		protected WeakReference<Container> m_parref;

		public void initUI(Container par, Object... params)
		{
			m_parref = new WeakReference<Container>(par);
			m_scroll = new JScrollPane();
			m_tbps = new BPTable<ProcessInfo>(new BPTableFuncsProcessInfo());
			{
				BPTextField tf = new BPTextField();
				tf.setMonoFont();
				TableCellEditor editor = new BPTable.BPCellEditorReadonly(tf);
				m_tbps.getColumnModel().getColumn(0).setMaxWidth(UIUtil.scale(50));
				m_tbps.getColumnModel().getColumn(0).setCellEditor(editor);
				m_tbps.getColumnModel().getColumn(1).setCellEditor(editor);
				m_tbps.getColumnModel().getColumn(2).setCellRenderer(new BPTableRendererFileSize());
			}
			((DefaultTableCellRenderer) m_tbps.getDefaultRenderer(Integer.class)).setHorizontalAlignment(JLabel.LEFT);
			m_tbps.initRowSorter();

			BPToolBarSQ toolbar = new BPToolBarSQ(true);
			Action actrefresh = BPActionHelpers.getActionWithAlias(BPActionConstCommon.ACT_BTNREFRESH, BPActionConstCommon.ACT_BTNREFRESH_ACC, this::onRefresh);
			Action actkill = BPActionHelpers.getAction(BPActionConstOSManagement.ACT_BTNKILL, this::onKill);
			Action actrun = BPActionHelpers.getAction(BPActionConstCommon.ACT_BTNRUN, this::onRun, a -> a.acceleratorKey(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK)).vIcon(BPIconResV.ADD()));
			Action actruncmd = BPActionHelpers.getAction(BPActionConstOSManagement.ACT_BTNRUNCMD, this::onRunCommand);
			Action actshellexec = BPActionHelpers.getAction(BPActionConstOSManagement.ACT_BTNSHELLEXEC, this::onShellExecute);
			Action actautorefresh = BPActionHelpers.getAction(BPActionConstOSManagement.ACT_BTNAUTOREFRESH, this::onToggleTimer);
			m_actautorefresh = actautorefresh;
			toolbar.setBorderVertical(1);
			toolbar.setActions(new Action[] { actrefresh, BPAction.separator(), actkill, BPAction.separator(), actrun, actruncmd, actshellexec, BPAction.separator(), actautorefresh });

			m_scroll.setViewportView(m_tbps);
			m_scroll.setBorder(new EmptyBorder(0, 0, 0, 0));

			m_tbps.setMonoFont();

			par.add(toolbar, BorderLayout.WEST);
			par.add(m_scroll, BorderLayout.CENTER);
		}

		protected void onRefresh(ActionEvent e)
		{
			doRefresh();
		}

		protected void doRefresh()
		{
			List<ProcessInfo> ps = BPProcessHandler_Win.getProcessInfos();
			int[] selis = m_tbps.getSelectedRows();
			m_tbps.getBPTableModel().setDatas(ps);
			m_tbps.refreshData();
			int s = ps.size();
			if (selis != null && selis.length > 0)
			{
				for (int i : selis)
				{
					if (i < s)
						m_tbps.getSelectionModel().addSelectionInterval(i, i);
				}
			}
		}

		protected void onTimer(ActionEvent e)
		{
			if (checkStop())
				return;
			doRefresh();
		}

		protected boolean checkStop()
		{
			WeakReference<Container> parref = m_parref;
			if (parref != null)
			{
				Container par = m_parref.get();
				if (par == null)
					return true;
				if (par.getParent() == null)
					return true;
				Component c = par.getFocusCycleRootAncestor();
				if (c == null || !c.isVisible())
					return true;
			}

			return false;
		}

		protected void onRun(ActionEvent e)
		{
			BPDialogSelectResource2 dlg = new BPDialogSelectResource2();
			dlg.setScope(SELECTSCOPE.COMPUTER);
			dlg.showOpen();
			BPResource[] ress = dlg.getSelectedResources();
			if (ress != null && ress.length > 0)
			{
				List<String> filenames = new ArrayList<String>();
				for (BPResource res : ress)
				{
					if (res.isFileSystem())
					{
						BPResourceFileSystem resf = (BPResourceFileSystem) res;
						filenames.add(resf.getFileFullName());
					}
				}
				for (String filename : filenames)
				{
					String workdir = null;
					{
						File f = new File(filename);
						workdir = f.getParent();
					}
					BPOSHandlers_Win.run(null, filename, workdir, null, null, false);
				}
			}
		}

		protected void onRunCommand(ActionEvent e)
		{
			String cmd = UIStd.input("", ">", BPGUICore.S_BP_TITLE + " - Run Command");
			BPOSHandlers_Win.run(null, cmd, null, null, null, false);
		}

		protected void onShellExecute(ActionEvent e)
		{
			String filename = UIStd.input("", ">", BPGUICore.S_BP_TITLE + " - ShellExec");
			File f = new File(filename);
			Desktop d = Desktop.getDesktop();
			UIStd.wrapSegE(() -> d.edit(f));
		}

		protected void onKill(ActionEvent e)
		{
			List<ProcessInfo> ps = m_tbps.getSelectedDatas();
			for (ProcessInfo p : ps)
			{
				SystemUtil.kill((long) p.pid, false, true);
			}
			doRefresh();
		}

		public void initDatas(Object... params)
		{
			doRefresh();
		}

		protected void onToggleTimer(ActionEvent e)
		{
			Timer t = m_timer;
			if (t == null)
			{
				t = new Timer(2000, this::onRefresh);
				m_timer = t;
				t.start();
				m_actautorefresh.putValue(Action.SELECTED_KEY, true);
			}
			else
			{
				stopTimer();
			}
		}

		protected void stopTimer()
		{
			Timer t = m_timer;
			m_timer = null;
			if (t != null)
			{
				m_actautorefresh.putValue(Action.SELECTED_KEY, false);
				m_timer = null;
				t.stop();
			}
		}

		public void clearResource()
		{
			stopTimer();
		}
	}

	protected static class BPTableFuncsProcessInfo extends BPTableFuncsBase<ProcessInfo>
	{
		public BPTableFuncsProcessInfo()
		{
			m_colnames = new String[] { "PID", "Filename", "Memory" };
			m_collabels = new String[] { "PID", BPLocaleHelpers.getValue(BPLocaleConstCC.FILENAME), BPLocaleHelpers.translate(BPLocaleConstCoreDict.S, "Memory") };
			m_cols = new Class[] { Integer.class, String.class, Long.class };
		}

		public Object getValue(ProcessInfo o, int row, int col)
		{
			switch (col)
			{
				case 0:
				{
					return o.pid;
				}
				case 1:
				{
					return o.filename == null ? "" : o.filename;
				}
				case 2:
				{
					return o.mem_used;
				}
			}
			return "";
		}

		public boolean isEditable(ProcessInfo o, int row, int col)
		{
			return true;
		}
	}
}