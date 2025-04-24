package com.mis.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import com.mis.db.BoxDAO;
import com.mis.model.Box;
import com.mis.model.BoxStatus;

/**
 * Panel for managing boxes
 */
public class BoxPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    
    private MainWindow mainWindow;
    private BoxDAO boxDAO;
    
    private JTable boxTable;
    private DefaultTableModel tableModel;
    private JButton addButton;
    private JButton editButton;
    private JButton deleteButton;
    
    public BoxPanel(MainWindow mainWindow) {
        this.mainWindow = mainWindow;
        this.boxDAO = new BoxDAO();
        
        setLayout(new BorderLayout());
        
        // Create table model
        tableModel = new DefaultTableModel() {
            private static final long serialVersionUID = 1L;
            
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        tableModel.addColumn("ID");
        tableModel.addColumn("Name");
        tableModel.addColumn("Location");
        tableModel.addColumn("Status");
        tableModel.addColumn("Current Animal");
        
        // Create table
        boxTable = new JTable(tableModel);
        boxTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        boxTable.getTableHeader().setReorderingAllowed(false);
        
        // Set custom renderer for the status column
        boxTable.getColumnModel().getColumn(3).setCellRenderer(new StatusCellRenderer());
        
        // Add table to scroll pane
        JScrollPane scrollPane = new JScrollPane(boxTable);
        add(scrollPane, BorderLayout.CENTER);
        
        // Create button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        addButton = new JButton("Add Box");
        editButton = new JButton("Edit Box");
        deleteButton = new JButton("Delete Box");
        
        buttonPanel.add(addButton);
        buttonPanel.add(editButton);
        buttonPanel.add(deleteButton);
        
        add(buttonPanel, BorderLayout.SOUTH);
        
        // Add button listeners
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showAddEditDialog(null);
            }
        });
        
        editButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedRow = boxTable.getSelectedRow();
                if (selectedRow >= 0) {
                    int boxId = (int) tableModel.getValueAt(selectedRow, 0);
                    try {
                        Box box = boxDAO.getById(boxId);
                        showAddEditDialog(box);
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(BoxPanel.this,
                                "Error loading box: " + ex.getMessage(),
                                "Database Error", JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    JOptionPane.showMessageDialog(BoxPanel.this,
                            "Please select a box to edit.",
                            "Selection Required", JOptionPane.WARNING_MESSAGE);
                }
            }
        });
        
        deleteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedRow = boxTable.getSelectedRow();
                if (selectedRow >= 0) {
                    int boxId = (int) tableModel.getValueAt(selectedRow, 0);
                    String status = (String) tableModel.getValueAt(selectedRow, 3);
                    
                    if (status.equals(BoxStatus.OCCUPIED.toString())) {
                        JOptionPane.showMessageDialog(BoxPanel.this,
                                "Cannot delete an occupied box. Please release the animal first.",
                                "Box Occupied", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    
                    int confirm = JOptionPane.showConfirmDialog(BoxPanel.this,
                            "Are you sure you want to delete this box?",
                            "Confirm Delete", JOptionPane.YES_NO_OPTION);
                    
                    if (confirm == JOptionPane.YES_OPTION) {
                        try {
                            boolean success = boxDAO.delete(boxId);
                            if (success) {
                                refreshData();
                            } else {
                                JOptionPane.showMessageDialog(BoxPanel.this,
                                        "Failed to delete box.",
                                        "Delete Error", JOptionPane.ERROR_MESSAGE);
                            }
                        } catch (SQLException ex) {
                            ex.printStackTrace();
                            JOptionPane.showMessageDialog(BoxPanel.this,
                                    "Error deleting box: " + ex.getMessage(),
                                    "Database Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                } else {
                    JOptionPane.showMessageDialog(BoxPanel.this,
                            "Please select a box to delete.",
                            "Selection Required", JOptionPane.WARNING_MESSAGE);
                }
            }
        });
        
        // Load data
        refreshData();
    }
    
    /**
     * Refresh data in the table
     */
    public void refreshData() {
        tableModel.setRowCount(0);
        
        try {
            List<Box> boxes = boxDAO.getAll();
            
            for (Box box : boxes) {
                Object[] rowData = new Object[5];
                rowData[0] = box.getId();
                rowData[1] = box.getName();
                rowData[2] = box.getLocation();
                rowData[3] = box.getStatus().toString();
                
                // Current animal info
                if (box.getCurrentAnimal() != null) {
                    rowData[4] = box.getCurrentAnimal().getName() + " (" + 
                                 box.getCurrentAnimal().getSpecies() + ")";
                } else {
                    rowData[4] = "";
                }
                
                tableModel.addRow(rowData);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Error loading boxes: " + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Show the add/edit box dialog
     */
    private void showAddEditDialog(Box box) {
        boolean isEdit = box != null;
        
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // Name
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Name:"), gbc);
        
        gbc.gridx = 1;
        JTextField nameField = new JTextField(20);
        if (isEdit) {
            nameField.setText(box.getName());
        }
        panel.add(nameField, gbc);
        
        // Location
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("Location:"), gbc);
        
        gbc.gridx = 1;
        JTextField locationField = new JTextField(20);
        if (isEdit && box.getLocation() != null) {
            locationField.setText(box.getLocation());
        }
        panel.add(locationField, gbc);
        
        // Status
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(new JLabel("Status:"), gbc);
        
        gbc.gridx = 1;
        JComboBox<BoxStatus> statusCombo = new JComboBox<>(BoxStatus.values());
        if (isEdit) {
            statusCombo.setSelectedItem(box.getStatus());
            
            // Disable status change if box is occupied
            if (box.getStatus() == BoxStatus.OCCUPIED && box.getCurrentAnimal() != null) {
                statusCombo.setEnabled(false);
            }
        } else {
            statusCombo.setSelectedItem(BoxStatus.AVAILABLE);
        }
        panel.add(statusCombo, gbc);
        
        int result = JOptionPane.showConfirmDialog(this, panel,
                isEdit ? "Edit Box" : "Add Box", JOptionPane.OK_CANCEL_OPTION);
        
        if (result == JOptionPane.OK_OPTION) {
            try {
                String name = nameField.getText().trim();
                String location = locationField.getText().trim();
                BoxStatus status = (BoxStatus) statusCombo.getSelectedItem();
                
                if (name.isEmpty()) {
                    JOptionPane.showMessageDialog(this,
                            "Box name is a required field.",
                            "Validation Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                if (isEdit) {
                    // Update existing box
                    box.setName(name);
                    box.setLocation(location.isEmpty() ? null : location);
                    box.setStatus(status);
                    
                    boolean success = boxDAO.update(box);
                    if (success) {
                        refreshData();
                    } else {
                        JOptionPane.showMessageDialog(this,
                                "Failed to update box.",
                                "Update Error", JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    // Create new box
                    Box newBox = new Box();
                    newBox.setName(name);
                    newBox.setLocation(location.isEmpty() ? null : location);
                    newBox.setStatus(status);
                    
                    int boxId = boxDAO.save(newBox);
                    if (boxId > 0) {
                        refreshData();
                    } else {
                        JOptionPane.showMessageDialog(this,
                                "Failed to add box.",
                                "Add Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this,
                        "Database error: " + e.getMessage(),
                        "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    /**
     * Custom renderer for the status column
     */
    private class StatusCellRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;
        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            
            Component c = super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);
            
            if (value != null) {
                String status = value.toString();
                
                if (status.equals(BoxStatus.AVAILABLE.toString())) {
                    c.setForeground(new Color(0, 128, 0)); // Green
                } else if (status.equals(BoxStatus.OCCUPIED.toString())) {
                    c.setForeground(Color.RED);
                } else if (status.equals(BoxStatus.MAINTENANCE.toString())) {
                    c.setForeground(Color.ORANGE);
                } else if (status.equals(BoxStatus.CLEANING.toString())) {
                    c.setForeground(Color.BLUE);
                }
            }
            
            return c;
        }
    }
} 