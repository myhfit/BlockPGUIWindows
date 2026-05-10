package bp.tool;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumnModel;

import bp.nativehelper.BPNativeHelpers;
import bp.nativehelper.windows.IphlpapiHelper;
import bp.nativehelper.windows.IphlpapiHelper.IPForwardRecord;
import bp.nativehelper.windows.IphlpapiHelper.SockAddr_INet;
import bp.ui.actions.BPActionConstCommon;
import bp.ui.actions.BPActionHelpers;
import bp.ui.container.BPToolBarSQ;
import bp.ui.frame.BPFrame;
import bp.ui.scomp.BPTable;
import bp.ui.scomp.BPTextField;
import bp.ui.table.BPTableFuncsBase;
import bp.ui.util.UIStd;
import bp.ui.util.UIUtil;

public class BPToolGUIRouteTableWin extends BPToolGUIBase<BPToolGUIRouteTableWin.BPToolGUIContextRTW>
{
	public String getName()
	{
		return "Route Table(Windows)";
	}

	protected boolean checkRequirement()
	{
		if (BPNativeHelpers.hasJNASupport())
			return true;
		UIStd.err(new RuntimeException("Need JNA in class path"));
		return false;
	}

	protected BPToolGUIContextRTW createToolContext()
	{
		return new BPToolGUIContextRTW();
	}

	protected void setFramePrefers(BPFrame f)
	{
		f.setPreferredSize(UIUtil.getPercentDimension(0.8f, 0.8f));
		f.pack();
		if (!f.isLocationByPlatform())
			f.setLocationRelativeTo(null);
	}

	protected static class BPToolGUIContextRTW implements BPToolGUIBase.BPToolGUIContext
	{
		protected JScrollPane m_scroll;
		protected BPTable<IPForwardRecord> m_tbps;

		public void initUI(Container par, Object... params)
		{
			m_scroll = new JScrollPane();
			m_tbps = new BPTable<IPForwardRecord>(new BPTableFuncsRouteTable());
			{
				BPTextField tf = new BPTextField();
				tf.setMonoFont();
				TableCellEditor editor = new BPTable.BPCellEditorReadonly(tf);
				{
					TableColumnModel tcm = m_tbps.getColumnModel();
					tcm.getColumn(0).setCellEditor(editor);
					tcm.getColumn(1).setCellEditor(editor);
					tcm.getColumn(2).setCellEditor(editor);
					tcm.getColumn(3).setCellEditor(editor);
					tcm.getColumn(4).setCellEditor(editor);
					tcm.getColumn(3).setMaxWidth(80);
					tcm.getColumn(4).setMaxWidth(80);
				}
			}
			((DefaultTableCellRenderer) m_tbps.getDefaultRenderer(Integer.class)).setHorizontalAlignment(JLabel.LEFT);
			m_tbps.initRowSorter();

			BPToolBarSQ toolbar = new BPToolBarSQ(true);
			Action actrefresh = BPActionHelpers.getActionWithAlias(BPActionConstCommon.ACT_BTNREFRESH, BPActionConstCommon.ACT_BTNREFRESH_ACC, this::onRefresh);
			toolbar.setBorderVertical(1);
			toolbar.setActions(new Action[] { actrefresh });

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
			IphlpapiHelper helper = BPNativeHelpers.getInterface(IphlpapiHelper.HELPER_NAME_IPHLPAPI);
			List<IPForwardRecord> datas = new ArrayList<IPForwardRecord>();
			helper.getIpForwardTable2((short) 0, datas);
			int[] selis = m_tbps.getSelectedRows();
			m_tbps.getBPTableModel().setDatas(datas);
			m_tbps.refreshData();
			int s = datas.size();
			if (selis != null && selis.length > 0)
			{
				for (int i : selis)
				{
					if (i < s)
						m_tbps.getSelectionModel().addSelectionInterval(i, i);
				}
			}
		}

		public void initDatas(Object... params)
		{
			doRefresh();
		}

		public void clearResource()
		{
		}
	}

	protected static class BPTableFuncsRouteTable extends BPTableFuncsBase<IPForwardRecord>
	{
		public BPTableFuncsRouteTable()
		{
			m_colnames = new String[] { "Prefix", "Mask", "NextHop", "Metric", "Interface" };
			m_collabels = new String[] { "Prefix", "Mask", "NextHop", "Metric", "Interface" };
			m_cols = new Class[] { String.class, Integer.class, String.class, Integer.class, Long.class };
		}

		public Object getValue(IPForwardRecord o, int row, int col)
		{
			switch (col)
			{
				case 0:
				{
					return parseAddress(o.DestinationPrefix.Prefix);
				}
				case 1:
				{
					return getMask(o.DestinationPrefix.PrefixLength, o.DestinationPrefix.Prefix.si_family);
				}
				case 2:
				{
					return parseAddress(o.NextHop);
				}
				case 3:
				{
					return o.Metric;
				}
				case 4:
				{
					return o.InterfaceIndex;
				}
			}
			return "";
		}

		public String getMask(byte prefixlen, short sifamily)
		{
			int plen = (int) prefixlen & 0xFF;
			switch (sifamily)
			{
				case IphlpapiHelper.AF_INET:
					return toIPv4Mask(plen);
				case IphlpapiHelper.AF_INET6:
					return plen + "";
			}
			return null;
		}

		protected String toIPv4Mask(int len)
		{
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < 4; i++)
			{
				if (i > 0)
					sb.append(".");
				if (len >= 8)
				{
					sb.append("255");
				}
				else if (len > 0)
				{
					int c = 0;
					for (int j = 0; j < len; j++)
					{
						c = c | (2 << 6 - j);
					}
					sb.append(Integer.toString(c));
				}
				else
				{
					sb.append("0");
				}
				len -= 8;
			}
			return sb.toString();
		}

		public boolean isEditable(IPForwardRecord o, int row, int col)
		{
			return true;
		}

		private static String parseAddress(SockAddr_INet addr)
		{
			if (addr.si_family == IphlpapiHelper.AF_INET)
			{
				int ip = addr.Ipv4.sin_addr;
				return ((ip >> 0) & 0xFF) + "." + ((ip >> 8) & 0xFF) + "." + ((ip >> 16) & 0xFF) + "." + ((ip >> 24) & 0xFF);
			}
			else if (addr.si_family == IphlpapiHelper.AF_INET6)
				return bytesToHexString(addr.Ipv6.sin6_addr);
			return null;
		}

		private static String bytesToHexString(byte[] bytes)
		{
			StringBuilder sb = new StringBuilder();
			int c = 0;
			for (byte b : bytes)
			{
				int i = (int) b & 0xFF;
				String s = Integer.toHexString(i).toUpperCase();
				if (s.length() < 2)
					sb.append('0');
				sb.append(s);
				c += 2;
				if (c < 32)
				{
					if (c % 4 == 0)
						sb.append(":");
				}
			}
			return sb.toString();
		}
	}
}