package bp.tool;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.lang.ref.WeakReference;
import java.util.List;

import javax.swing.Action;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;

import bp.os.window.BPWindowHandler_Win;
import bp.os.window.BPWindowHandler_Win.WindowInfo;
import bp.ui.actions.BPAction;
import bp.ui.actions.BPActionConstOSManagement;
import bp.ui.actions.BPActionHelpers;
import bp.ui.container.BPToolBarSQ;
import bp.ui.frame.BPFrame;
import bp.ui.res.icon.BPIconResV;
import bp.ui.scomp.BPTable;
import bp.ui.scomp.BPTextField;
import bp.ui.table.BPTableFuncsBase;
import bp.ui.util.UIStd;
import bp.ui.util.UIUtil;
import bp.util.ClassUtil;

public class BPToolGUIWindowManagerWin extends BPToolGUIBase<BPToolGUIWindowManagerWin.BPToolGUIContextWMW>
{
	public String getName()
	{
		return BPActionHelpers.getValue(BPActionConstOSManagement.TNAME_WINDOWMAN) + "(Windows)";
	}

	protected boolean checkRequirement()
	{
		if (ClassUtil.getTClass("com.sun.jna.Native", ClassUtil.getExtensionClassLoader()) != null)
			return true;
		UIStd.err(new RuntimeException("Need JNA in class path"));
		return false;
	}

	protected BPToolGUIContextWMW createToolContext()
	{
		return new BPToolGUIContextWMW();
	}

	protected void setFramePrefers(BPFrame f)
	{
		f.setPreferredSize(UIUtil.getPercentDimension(0.8f, 0.8f));
		f.pack();
		if (!f.isLocationByPlatform())
			f.setLocationRelativeTo(null);
	}

	protected static class BPToolGUIContextWMW implements BPToolGUIBase.BPToolGUIContext
	{
		protected JScrollPane m_scroll;
		protected BPTable<WindowInfo> m_tbwininfos;
		protected Action m_actautorefresh;
		protected Timer m_timer;
		protected WeakReference<Container> m_parref;

		public void initUI(Container par, Object... params)
		{
			m_parref = new WeakReference<Container>(par);
			m_scroll = new JScrollPane();
			m_tbwininfos = new BPTable<WindowInfo>(new BPTableFuncsWindowInfo());

			BPToolBarSQ toolbar = new BPToolBarSQ(true);
			Action actrefresh = BPAction.build("Refresh").tooltip("Refresh(F5)").callback(this::onRefresh).acceleratorKey(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0)).vIcon(BPIconResV.REFRESH()).getAction();
			Action actclosewin = BPAction.build("Close").tooltip("Close(F3)").callback(this::onClose).acceleratorKey(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0)).vIcon(BPIconResV.KILL()).getAction();
			Action actautorefresh = BPAction.build("AR").tooltip("Auto Refresh").callback(this::onToggleTimer).vIcon(BPIconResV.REFRESH()).getAction();
			m_actautorefresh = actautorefresh;
			toolbar.setActions(new Action[] { actrefresh, BPAction.separator(), actclosewin, BPAction.separator(), actautorefresh });

			m_scroll.setViewportView(m_tbwininfos);
			m_scroll.setBorder(new EmptyBorder(0, 0, 0, 0));
			toolbar.setBorderVertical(1);

			m_tbwininfos.setMonoFont();
			m_tbwininfos.initRowSorter();
			{
				BPTextField tf = new BPTextField();
				tf.setMonoFont();
				TableCellEditor editor = new BPTable.BPCellEditorReadonly(tf);
				m_tbwininfos.getBPColumnModel().getColumnBuilder(0).setCellRenderer(new BPTableRendererHex()).setMaxWidth(UIUtil.scale(120)).setCellEditor(editor);
				m_tbwininfos.getColumnModel().getColumn(1).setCellEditor(editor);
			}

			par.add(toolbar, BorderLayout.WEST);
			par.add(m_scroll, BorderLayout.CENTER);
		}

		public void initDatas(Object... params)
		{
			onRefresh(null);
		}

		protected void onClose(ActionEvent e)
		{
			WindowInfo w = m_tbwininfos.getSelectedData();
			if (w != null && w.pid != 0)
				if (BPWindowHandler_Win.closeWindow(w.hwnd))
					UIUtil.laterUI(this::doRefresh);
		}

		public void onRefresh(ActionEvent e)
		{
			doRefresh();
		}

		public void doRefresh()
		{
			int[] selis = m_tbwininfos.getSelectedRows();
			List<WindowInfo> wins = BPWindowHandler_Win.getWindowInfos();
			m_tbwininfos.getBPTableModel().setDatas(wins);
			m_tbwininfos.refreshData();
			int s = wins.size();
			if (selis != null && selis.length > 0)
			{
				for (int i : selis)
				{
					if (i < s)
						m_tbwininfos.getSelectionModel().addSelectionInterval(i, i);
				}
			}
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

	protected static class BPTableFuncsWindowInfo extends BPTableFuncsBase<WindowInfo>
	{
		public BPTableFuncsWindowInfo()
		{
			m_colnames = new String[] { "HWND", "Title", "Classname", "Process" };
			m_cols = new Class[] { Long.class, String.class, String.class, String.class };
		}

		public boolean isEditable(WindowInfo o, int row, int col)
		{
			return true;
		}

		public Object getValue(WindowInfo o, int row, int col)
		{
			switch (col)
			{
				case 0:
				{
					return o.hwnd;
				}
				case 1:
				{
					return nvl(o.title);
				}
				case 2:
				{
					return nvl(o.classname);
				}
				case 3:
				{
					return (o.pid == -1 ? "" : (o.pid + ":" + (o.pinfo == null ? "" : nvl(o.pinfo.filename))));
				}
			}
			return "";
		}
	}

	protected static class BPTableRendererHex extends DefaultTableCellRenderer
	{
		/**
		 * 
		 */
		private static final long serialVersionUID = 5743458936100824244L;

		public BPTableRendererHex()
		{
			super();
		}

		public void setValue(Object value)
		{
			if (value == null)
				setText("");
			else if (value instanceof Long)
				setText(Long.toHexString((long) value).toUpperCase());
		}
	}
}