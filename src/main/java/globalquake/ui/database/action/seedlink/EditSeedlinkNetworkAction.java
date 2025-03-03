package globalquake.ui.database.action.seedlink;

import globalquake.database.SeedlinkNetwork;
import globalquake.database.StationDatabaseManager;
import globalquake.ui.database.EditSeedlinkNetworkDialog;
import globalquake.ui.database.table.FilterableTableModel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class EditSeedlinkNetworkAction extends AbstractAction {

    private final StationDatabaseManager databaseManager;
    private final Window parent;
    private FilterableTableModel<SeedlinkNetwork> tableModel;

    private JTable table;

    public EditSeedlinkNetworkAction(Window parent, StationDatabaseManager databaseManager){
        super("Edit");
        this.databaseManager = databaseManager;
        this.parent = parent;

        putValue(SHORT_DESCRIPTION, "Edit Seedlink Network");
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
        SeedlinkNetwork seedlinkNetwork = tableModel.getEntity(modelRow);
        new EditSeedlinkNetworkDialog(parent, databaseManager, tableModel, seedlinkNetwork);
    }

    public void setTableModel(FilterableTableModel<SeedlinkNetwork> tableModel) {
        this.tableModel = tableModel;
    }

    public void setTable(JTable table) {
        this.table = table;
    }
}
