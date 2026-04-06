package bp.tool;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.lang.ref.WeakReference;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;

import bp.config.UIConfigs;
import bp.os.monitor.BPOSMonitor;
import bp.os.monitor.BPOSMonitor.BPOSMonitor_CPU;
import bp.os.monitor.BPOSMonitor.BPOSMonitor_Memory;
import bp.os.monitor.BPOSMonitors;
import bp.ui.actions.BPActionConstOSManagement;
import bp.ui.actions.BPActionHelpers;
import bp.ui.container.BPToolBarSQ;
import bp.ui.frame.BPFrame;
import bp.ui.scomp.BPLabel;
import bp.ui.scomp.BPMonitorUINumber;
import bp.ui.scomp.BPMonitorUINumber.MonitorFormatMode;
import bp.ui.scomp.BPMonitorUINumber.MonitorRenderMode;
import bp.ui.util.UIStd;
import bp.ui.util.UIUtil;
import bp.util.ClassUtil;
import bp.util.NumberUtil;

public class BPToolGUISystemMonitorWin extends BPToolGUIBase<BPToolGUISystemMonitorWin.BPToolGUIContextSMW>
{
	public String getName()
	{
		return BPActionHelpers.getValue(BPActionConstOSManagement.TNAME_SYSTEMMON) + "(Windows)";
	}

	protected BPToolGUIContextSMW createToolContext()
	{
		return new BPToolGUIContextSMW();
	}

	protected boolean checkRequirement()
	{
		if (ClassUtil.getTClass("com.sun.jna.Native", ClassUtil.getExtensionClassLoader()) != null)
			return true;
		UIStd.err(new RuntimeException("Need JNA in class path"));
		return false;
	}

	protected void setFramePrefers(BPFrame f)
	{
		f.setPreferredSize(UIUtil.getPercentDimension(0.4f, 0.4f));
		f.pack();
		if (!f.isLocationByPlatform())
			f.setLocationRelativeTo(null);
	}

	protected static class BPToolGUIContextSMW implements BPToolGUIBase.BPToolGUIContext
	{
		protected Timer m_timer;
		protected WeakReference<Container> m_parref;
		protected BPOSMonitor_CPU m_moncpu;
		protected BPOSMonitor_Memory m_monmem;
		protected BPMonitorUINumber m_muicpu;
		protected BPMonitorUINumber m_muimem;

		public void initUI(Container par, Object... params)
		{
			m_parref = new WeakReference<Container>(par);

			BPToolBarSQ toolbar = new BPToolBarSQ(true);
			toolbar.setBorderVertical(1);
			toolbar.setActions(new Action[] {});

			m_muicpu = new BPMonitorUINumber();
			m_muicpu.setMonoFont();
			m_muicpu.setRenderMode(MonitorRenderMode.STACK_HORIZONTAL);
			m_muicpu.setFormatMode(MonitorFormatMode.PERCENT);
			m_muimem = new BPMonitorUINumber();
			m_muimem.setMonoFont();
			m_muimem.setRenderMode(MonitorRenderMode.STACK_HORIZONTAL);
			m_muimem.setFormatMode(MonitorFormatMode.PERCENT);

			JPanel panms = new JPanel();
			GridLayout l = new GridLayout(1, 2, 0, 0);
			panms.setLayout(l);
			panms.add(borderPanel(m_muicpu, "CPU"));
			panms.add(borderPanel(m_muimem, "Memory"));

			// par.add(toolbar, BorderLayout.WEST);
			par.add(panms, BorderLayout.CENTER);
		}

		protected JComponent borderPanel(JComponent c, String label)
		{
			JPanel rc = new JPanel();
			BPLabel lbl = new BPLabel(label, BPLabel.CENTER);
			lbl.setLabelFont();
			lbl.setBorder(new MatteBorder(0, 0, 1, 0, UIConfigs.COLOR_WEAKBORDER()));
			rc.setBackground(UIConfigs.COLOR_TEXTBG());
			rc.setLayout(new BorderLayout());
			rc.setBorder(new CompoundBorder(new EmptyBorder(2, 2, 2, 2), new MatteBorder(1, 1, 1, 1, UIConfigs.COLOR_TEXTQUARTER())));
			rc.add(c, BorderLayout.CENTER);
			rc.add(lbl, BorderLayout.NORTH);
			return rc;
		}

		protected void onRefresh(ActionEvent e)
		{
			doRefresh();
		}

		protected void doRefresh()
		{
			BPOSMonitor_CPU moncpu = m_moncpu;
			if (moncpu != null)
			{
				moncpu.tick();
				double r = moncpu.getCPUUsage();
				m_muicpu.appendValue(r);
			}

			BPOSMonitor_Memory monmem = m_monmem;
			if (monmem != null)
			{
				monmem.tick();
				long[] r = monmem.getMemoryStatus();
				m_muimem.appendValue(1 - ((double) (r[0]) / (double) (r[1])));
				m_muimem.setText(NumberUtil.formatByteCount(r[1] - r[0], 1024) + "/" + NumberUtil.formatByteCount(r[1], 1024) + "  " + NumberUtil.formatPercent((double) (r[1] - r[0]) / r[1]));
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

		public void initDatas(Object... params)
		{
			m_moncpu = BPOSMonitors.createMonitor(BPOSMonitors.MONITOR_KEY_CPU);
			m_monmem = BPOSMonitors.createMonitor(BPOSMonitors.MONITOR_KEY_MEMORY);
			if (m_moncpu != null)
				m_moncpu.start();
			if (m_monmem != null)
				m_monmem.start();
			onToggleTimer(null);
			doRefresh();
		}

		protected void onToggleTimer(ActionEvent e)
		{
			Timer t = m_timer;
			if (t == null)
			{
				t = new Timer(1000, this::onRefresh);
				m_timer = t;
				t.start();
				// m_actautorefresh.putValue(Action.SELECTED_KEY, true);
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
				// m_actautorefresh.putValue(Action.SELECTED_KEY, false);
				m_timer = null;
				t.stop();
			}
		}

		public void clearResource()
		{
			stopTimer();
			BPOSMonitor m = m_moncpu;
			m_moncpu = null;
			if (m != null)
				m.stop();
			m = m_monmem;
			m_monmem = null;
			if (m != null)
				m.stop();
		}
	}

}
