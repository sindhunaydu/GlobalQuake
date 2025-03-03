package globalquake.ui.database.action.source;

import globalquake.database.StationDatabaseManager;
import globalquake.database.StationSource;
import globalquake.ui.database.table.FilterableTableModel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

public class RemoveStationSourceAction extends AbstractAction {

    private final StationDatabaseManager databaseManager;
    private final Component parent;
    private FilterableTableModel<StationSource> tableModel;

    private JTable table;

    public RemoveStationSourceAction(StationDatabaseManager databaseManager, Component parent){
        super("Remove");
        this.parent = parent;
        this.databaseManager = databaseManager;

        putValue(SHORT_DESCRIPTION, "Remove Station Sources");
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        int[] selectedRows = table.getSelectedRows();
        if (selectedRows.length < 1) {
            throw new IllegalStateException("Invalid selected rows count (must be > 0): " + selectedRows.length);
        }
        if (table.isEditing()) {
            table.getCellEditor().cancelCellEditing();
        }

        int option = JOptionPane.showConfirmDialog(parent,
                "Are you sure you want to delete those items?",
                "Confirmation",
                JOptionPane.YES_NO_OPTION);

        if (option != JOptionPane.YES_OPTION) {
            return;
        }

        databaseManager.getStationDatabase().getDatabaseWriteLock().lock();
        try{
            List<StationSource> toBeRemoved = new ArrayList<>();
            for(int i:selectedRows){
                StationSource stationSource = tableModel.getEntity(table.getRowSorter().convertRowIndexToModel(i));
                toBeRemoved.add(stationSource);
            }
            databaseManager.removeAllStationSources(toBeRemoved);
        }finally {
            databaseManager.getStationDatabase().getDatabaseWriteLock().unlock();
        }

        tableModel.applyFilter();
    }

    public void setTableModel(FilterableTableModel<StationSource> tableModel) {
        this.tableModel = tableModel;
    }

    public void setTable(JTable table) {
        this.table = table;
    }
}
