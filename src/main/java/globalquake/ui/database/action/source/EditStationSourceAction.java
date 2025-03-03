package globalquake.ui.database.action.source;

import globalquake.database.StationDatabaseManager;
import globalquake.database.StationSource;
import globalquake.ui.database.EditStationSourceDialog;
import globalquake.ui.database.table.FilterableTableModel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class EditStationSourceAction extends AbstractAction {

    private final StationDatabaseManager databaseManager;
    private final Window parent;
    private FilterableTableModel<StationSource> tableModel;

    private JTable table;

    public EditStationSourceAction(Window parent, StationDatabaseManager databaseManager){
        super("Edit");
        this.databaseManager = databaseManager;
        this.parent = parent;

        putValue(SHORT_DESCRIPTION, "Edit Station Source");
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        int[] selectedRows = table.getSelectedRows();
        if (selectedRows.length != 1) {
            throw new IllegalStateException("Invalid selected rows count (must be 1): " + selectedRows.length);
        }
        if (table.isEditing()) {
            table.getCellEditor().cancelCellEditing();
        }
        int modelRow = table.convertRowIndexToModel(selectedRows[0]);
        StationSource stationSource = tableModel.getEntity(modelRow);
        new EditStationSourceDialog(parent, databaseManager, tableModel, stationSource);
    }

    public void setTableModel(FilterableTableModel<StationSource> tableModel) {
        this.tableModel = tableModel;
    }

    public void setTable(JTable table) {
        this.table = table;
    }
}
