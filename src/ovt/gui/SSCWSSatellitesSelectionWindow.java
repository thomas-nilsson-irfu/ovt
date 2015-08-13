/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ovt.gui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import ovt.OVTCore;
import static ovt.XYZWindow.SETTING_VISUALIZATION_PANEL_WIDTH;

import ovt.datatype.Time;
import ovt.event.ChildrenEvent;
import ovt.interfaces.ChildrenListener;
import ovt.object.SSCWSSat;
import ovt.util.SSCWSLibrary;
import ovt.util.SSCWSLibraryTestEmulator;

/**
 * GUI window for listing and selecting satellite offered online using NASA's
 * Satellite Situation Center's (SSC) Web Services.
 *
 * NOTE: The class stores "bookmarked" SSCWS satellites. This should maybe be
 * moved to OVTCore (or XYZWindow)?! (Bad for testing?) Would make storing
 * properties when quitting more natural (using getCore().saveSettings())?! .
 *
 * @author Erik P G Johansson, erik.johansson@irfu.se
 */
// --
// PROPOSAL: Rename to xxxCustomizer in analogy with other window classes.
// PROPOSAL: Save window location to Settings/properties (config file).
// PROPOSAL: Initial window location at center of screen.
// TODO: Automatically appropriate column widths.
public class SSCWSSatellitesSelectionWindow extends JFrame {

    private static final int DEBUG = 1;
    private static final String WINDOW_TITLE = "Satellite data offered online by NASA SSC";
    private static final String INFO_TEXT = "Satellite data offered online by NASA's Satellite Situation Center (SSC)"
            + " and available through OVT. Note that some of these \"satellites\" may be located at Lagrange points "
            + "(e.g. ACE) or be balloons (e.g. BARREL-*).";
    private static final String[] COLUMN_GUI_TITLES = {"Bookmarked", "Added", "Name", "Data begins", "Data ends"};
    //private final String[] COLUMN_GUI_TITLES = {".", ".", ".", "."};
    private static final int COLUMN_INDEX_BOOKMARK = 0;
    private static final int COLUMN_INDEX_GUI_TREE_ADDED = 1;
    private static final int COLUMN_INDEX_SATELLITE_NAME = 2;
    private static final int COLUMN_INDEX_DATA_AVAILABLE_BEGIN = 3;
    private static final int COLUMN_INDEX_DATA_AVAILABLE_END = 4;

    private final SSCWSLibrary sscwsLib;
    private final JTable table;
    private final LocalTableModel tableModel;


    /**
     * NOTE: xyzWin == null can be used for test code.
     */
    public SSCWSSatellitesSelectionWindow(SSCWSLibrary mSSCWSLib, OVTCore core, SSCWSSatellitesBookmarks bookmarks) throws IOException {
        sscwsLib = mSSCWSLib;
        setTitle(WINDOW_TITLE);

        //this.setLayout(new BoxLayout(this.getContentPane(), BoxLayout.Y_AXIS));
        this.setLayout(new GridBagLayout());

        /*================
         Create text area
         ===============*/
        final JTextArea textArea = new JTextArea();
        {
            final String infoText = INFO_TEXT
                    + "\n\nSSC Acknowledgements: " + mSSCWSLib.getAcknowledgements().get(0)
                    + "\nSSC Privacy and Important Notices: " + mSSCWSLib.getPrivacyAndImportantNotices().get(0);
            textArea.setText(infoText);
            textArea.setWrapStyleWord(true);
            textArea.setLineWrap(true);
            textArea.setEditable(false);
            textArea.setOpaque(false);  // Use the background color of the window by making it transparent.
            textArea.setFont(Style.getLabelFont());
            textArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            final GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;
            c.weightx = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            this.getContentPane().add(textArea, c);
        }


        /*============
         Create Table
         ===========*/
        {
            tableModel = new LocalTableModel(core, bookmarks);
            table = new JTable(tableModel);
            table.setAutoCreateRowSorter(true);   // To enable sorting by the user when clicking on column headers (default sorting).
            table.getRowSorter().toggleSortOrder(COLUMN_INDEX_SATELLITE_NAME); // Sort column once. Does not seem to reverse sorting of already sorted SSC satellites.

            initColumnWidths();

            // Center the contents of the two data availability columns.
            final DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
            centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
            table.getColumnModel().getColumn(COLUMN_INDEX_DATA_AVAILABLE_BEGIN).setCellRenderer(centerRenderer);
            table.getColumnModel().getColumn(COLUMN_INDEX_DATA_AVAILABLE_END).setCellRenderer(centerRenderer);

            table.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);

            //table.setFillsViewportHeight(true);        
            final JScrollPane tableScrollPane = new JScrollPane(table,
                    ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                    //ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                    //ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
                    ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

            final GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 1;
            c.weighty = 1;
            c.weightx = 1;
            c.fill = GridBagConstraints.BOTH;
            this.getContentPane().add(tableScrollPane, c);
        }
        /* Needed to get convenient initial width, roughly the width needed to
         display all data in all columns. Unknown why this has to be done manually.
         Unknown why extra small margin is needed even without scroll pane
         (the command changes the content pane, not the window; an otherwie common mistake).
         Added additional extra arbitrary width margin to cover vertical scroll pane.
         This is probably not how one is supposed to, but lacking something better, it will have to do.
         */
        this.getContentPane().setPreferredSize(
                new Dimension(table.getPreferredSize().width + 10 + 50,
                        this.getPreferredSize().height));

        pack();

        if (core != null) {
            core.getSats().getChildren().addChildrenListener(tableModel);
        }

        //System.out.println("frame.size = " + getWidth() + ", " + getHeight());   // TEMP
        // Set location at center of screen.
        final Dimension scrnSize = Toolkit.getDefaultToolkit().getScreenSize();
        final Dimension frameSize = getSize();
        setLocation(scrnSize.width / 2 - frameSize.width / 2, scrnSize.height / 2 - frameSize.height / 2);

    }


    /*@Override
     public Dimension getPreferredSize() {
     return new Dimension(table.getPreferredSize().width + 10, super.getPreferredSize().height);
     }*/
    /**
     * Should in theory set the (preferred) column widths to the width needed to
     * fit the widest cell content in each column. In practice, this does not
     * translate to the JFrame size (via pack()). The relative column sizes seem
     * to be accurate but the initial JFrame size is slightly too small.<BR>
     * /Erik P G Johansson 2015-08-12
     */
    private void initColumnWidths() {

        int totColumnWidth = 0;
        final TableCellRenderer headerRenderer = table.getTableHeader().getDefaultRenderer();
        for (int col = 0; col < COLUMN_GUI_TITLES.length; col++) {
            final TableColumn column = table.getColumnModel().getColumn(col);

            final Component headerComp = headerRenderer.getTableCellRendererComponent(
                    null, column.getHeaderValue(),
                    false, false, 0, 0);
            final int headerWidth = headerComp.getPreferredSize().width;

            int cellWidth = 0;
            final Object[] columnWidestValues = tableModel.getWidestValues(col);
            for (Object columnWidestValue : columnWidestValues) {
                final Component cellComp = table.getDefaultRenderer(
                        tableModel.getColumnClass(col)).getTableCellRendererComponent(
                                table, columnWidestValue, false, false, 0, col);
                cellWidth = Math.max(cellWidth, cellComp.getPreferredSize().width);
            }

            final int newColumnWidth = (int) (Math.max(headerWidth, cellWidth));   // Margin helps but not sure why it is needed.
            column.setPreferredWidth(newColumnWidth);
            totColumnWidth = totColumnWidth + newColumnWidth;
        }
        //System.out.println("totColumnWidth = " + totColumnWidth);

        //this.setPreferredSize(new Dimension(totColumnWidth, table.getPreferredSize().height));   // TEST
        //this.getContentPane().setPreferredSize(new Dimension(totColumnWidth+10, this.getPreferredSize().height));   // TEST
    }

    //##########################################################################

    /**
     * NOTE: The row indices still refer to the same row of data even when the
     * table is sorted by a column (because that is how JTable uses TableModel).
     */
    private class LocalTableModel extends AbstractTableModel implements ChildrenListener {

        private final List<SSCWSLibrary.SSCWSSatelliteInfo> localSatInfoList;
        private final OVTCore core;
        private final SSCWSSatellitesBookmarks bookmarks;  

        /**
         * NOTE: mCore == null can be used for test code.
         */
        public LocalTableModel(OVTCore mCore, SSCWSSatellitesBookmarks mBookmarks) {
            List<SSCWSLibrary.SSCWSSatelliteInfo> tempSatInfoList;
            core = mCore;
            try {
                tempSatInfoList = SSCWSSatellitesSelectionWindow.this.sscwsLib.getAllSatelliteInfo();
            } catch (IOException e) {
                core.sendErrorMessage("Can not initialize table model since can not obtain list of SSC-based satellites.", e);
                tempSatInfoList = new ArrayList<>();  // Empty list
            }
            localSatInfoList = tempSatInfoList;
            bookmarks = mBookmarks;
        }


        @Override
        public int getRowCount() {
            return localSatInfoList.size();
        }


        @Override
        public int getColumnCount() {
            return COLUMN_GUI_TITLES.length;
        }


        @Override
        public Object getValueAt(int row, int col) {
            //Log.log(this.getClass().getSimpleName() + ".getValueAt(rowIndex=" + row + " , columnIndex=" + col + ")", DEBUG);
            final SSCWSLibrary.SSCWSSatelliteInfo satInfo = localSatInfoList.get(row);

            if (col == COLUMN_INDEX_BOOKMARK) {
                return bookmarks.isBookmark(localSatInfoList.get(row).ID);
            } else if (col == COLUMN_INDEX_GUI_TREE_ADDED) {

                if (core != null) {
                    try {
                        return core.getXYZWin().sscwsSatAlreadyAdded(satInfo.ID);
                    } catch (IOException e) {
                        /* It is very, very unlikely that this exception will be
                         thrown since it can only happen if (1) the list of satellites
                         has not already been downloaded, or (2) the satellite ID
                         does not match any satellite which can only happen due to bugs. */
                        core.sendErrorMessage("Error", e);
                        return false;
                    }
                } else {
                    return false;
                }
            } else if (col == COLUMN_INDEX_SATELLITE_NAME) {
                //Log.log("satInfo.name = " + satInfo.name, DEBUG);
                return satInfo.name;
            } else if (col == COLUMN_INDEX_DATA_AVAILABLE_BEGIN) {
                return Time.toString(satInfo.availableBeginTimeMjd);
            } else if (col == COLUMN_INDEX_DATA_AVAILABLE_END) {
                return Time.toString(satInfo.availableEndTimeMjd);
            } else {
                throw new IllegalArgumentException("No value at col=" + col + ", row=" + row + ".");
                //return "(" + row + ", " + col + ")";
            }
            //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }


        @Override
        public void setValueAt(Object value, int row, int col) {
            //Log.log(this.getClass().getSimpleName() + ".getValueAt(row=" + row + " , col=" + col + ")", DEBUG);
            final SSCWSLibrary.SSCWSSatelliteInfo satInfo = localSatInfoList.get(row);

            if (col == COLUMN_INDEX_BOOKMARK) {
                final boolean toBookmark = (Boolean) value;
                bookmarks.setBookmark(localSatInfoList.get(row).ID, toBookmark);

            } else if (col == COLUMN_INDEX_GUI_TREE_ADDED) {

                if (core != null) {
                    final boolean addSat = (Boolean) value;
                    if (addSat) {
                        core.getXYZWin().addSSCWSSatAction(satInfo.ID);
                    } else {
                        core.getXYZWin().removeSSCWSSatAction(satInfo.ID);
                    }

                } else {
                    // Do nothing.
                }

            } else {
                throw new IllegalArgumentException("Function not defined for this column.");
            }
        }


        @Override
        public boolean isCellEditable(int row, int col) {
            return ((col == COLUMN_INDEX_BOOKMARK) || (col == COLUMN_INDEX_GUI_TREE_ADDED));
        }


        @Override
        public String getColumnName(int col) {
            return COLUMN_GUI_TITLES[col];
        }


        @Override
        public Class getColumnClass(int col) {
            if ((col == COLUMN_INDEX_BOOKMARK) || (col == COLUMN_INDEX_GUI_TREE_ADDED)) {
                // Needed to make the column values "interpreted" as a checkbox.
                return Boolean.class;
            } else {
                return Object.class;
            }
        }


        // interface ChildrenListener
        @Override
        public void childAdded(ChildrenEvent evt) {
            if (evt.getChild() instanceof SSCWSSat) {
                SSCWSSat sat = (SSCWSSat) evt.getChild();

                final int row = getSatIndex(sat.dataSource.satInfo.ID);
                fireTableCellUpdated(row, COLUMN_INDEX_GUI_TREE_ADDED);
            }
        }


        // interface ChildrenListener
        @Override
        public void childRemoved(ChildrenEvent evt) {
            if (evt.getChild() instanceof SSCWSSat) {
                SSCWSSat sat = (SSCWSSat) evt.getChild();

                final int row = getSatIndex(sat.dataSource.satInfo.ID);
                fireTableCellUpdated(row, COLUMN_INDEX_GUI_TREE_ADDED);
            }
            //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }


        // interface ChildrenListener
        @Override
        public void childrenChanged(ChildrenEvent evt) {
            // Do nothing.
        }


        // PROPSAL: Move to SSCWSLibrary?
        //    NOTE: Will make it need to throw IOException.
        //    CON: Such a method will not act on localSatInfoList.
        private int getSatIndex(String satId) {
            for (int i = 0; i < localSatInfoList.size(); i++) {
                if (localSatInfoList.get(i).ID.equals(satId)) {
                    return i;
                }
            }
            throw new IllegalArgumentException("There is no such satellite ID (SSC based): \"" + satId + "\".");
        }


        private Object[] getWidestValues(int col) {
            // IMPLEMENTATION NOTE: Should be able to handle zero rows. ==> No call to getValueAt(...).
            if (col == COLUMN_INDEX_BOOKMARK) {
                return new Object[]{Boolean.FALSE};
            } else if (col == COLUMN_INDEX_GUI_TREE_ADDED) {
                return new Object[]{Boolean.FALSE};
            } else if (col == COLUMN_INDEX_SATELLITE_NAME) {
                final Object[] values = new Object[localSatInfoList.size()];
                for (int row = 0; row < getRowCount(); row++) {
                    values[row] = getValueAt(row, col);
                }
                return values;
            } else if ((col == COLUMN_INDEX_DATA_AVAILABLE_BEGIN)
                    || (col == COLUMN_INDEX_DATA_AVAILABLE_END)) {
                return new Object[]{Time.toString(0)};   // TEMP
            }
            throw new RuntimeException("Method not able to handle this column. This indicates a pure code bug.");
        }

    }

    //##########################################################################

    /**
     * Informal test code.
     */
    public static void test() throws IOException {
        //final SSCWSLibrary lib = SSCWSLibraryImpl.DEFAULT_INSTANCE;        
        final SSCWSLibrary lib = SSCWSLibraryTestEmulator.DEFAULT_INSTANCE;

        final JFrame frame = new SSCWSSatellitesSelectionWindow(lib, null, new SSCWSSatellitesBookmarks());
        frame.setVisible(true);
    }

}