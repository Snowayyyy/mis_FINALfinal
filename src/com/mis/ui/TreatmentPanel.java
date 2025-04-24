package com.mis.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
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
import javax.swing.table.DefaultTableModel;

import com.mis.api.AnimalController;
import com.mis.model.Animal;
import com.mis.model.Treatment;
import com.mis.model.TreatmentType;

/**
 * Panel for managing treatments
 */
public class TreatmentPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    
    private MainWindow mainWindow;
    private AnimalController animalController;
    
    private JTable treatmentTable;
    private DefaultTableModel tableModel;
    private JButton addButton;
    private JButton administerButton;
    private JButton filterButton;
    private JButton clearFilterButton;
    
    private Animal filteredAnimal = null;
    
    public TreatmentPanel(MainWindow mainWindow, AnimalController animalController) {
        this.mainWindow = mainWindow;
        this.animalController = animalController;
        
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
        tableModel.addColumn("Animal Name");
        tableModel.addColumn("Treatment Type");
        tableModel.addColumn("Name");
        tableModel.addColumn("Description");
        tableModel.addColumn("Administration Date");
        tableModel.addColumn("Next Due Date");
        tableModel.addColumn("Administered");
        tableModel.addColumn("Status");
        
        // Create table
        treatmentTable = new JTable(tableModel);
        treatmentTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        treatmentTable.getTableHeader().setReorderingAllowed(false);
        
        // Add table to scroll pane
        JScrollPane scrollPane = new JScrollPane(treatmentTable);
        add(scrollPane, BorderLayout.CENTER);
        
        // Create button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        addButton = new JButton("Add Treatment");
        administerButton = new JButton("Administer Treatment");
        filterButton = new JButton("Filter by Animal");
        clearFilterButton = new JButton("Clear Filter");
        
        buttonPanel.add(addButton);
        buttonPanel.add(administerButton);
        buttonPanel.add(filterButton);
        buttonPanel.add(clearFilterButton);
        
        add(buttonPanel, BorderLayout.SOUTH);
        
        // Add button listeners
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showAddTreatmentDialog();
            }
        });
        
        administerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedRow = treatmentTable.getSelectedRow();
                if (selectedRow >= 0) {
                    int treatmentId = (int) tableModel.getValueAt(selectedRow, 0);
                    showAdministerDialog(treatmentId);
                } else {
                    JOptionPane.showMessageDialog(TreatmentPanel.this,
                            "Please select a treatment to administer.",
                            "Selection Required", JOptionPane.WARNING_MESSAGE);
                }
            }
        });
        
        filterButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showFilterDialog();
            }
        });
        
        clearFilterButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                filteredAnimal = null;
                refreshData();
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
            List<Animal> animals;
            
            if (filteredAnimal != null) {
                animals = List.of(animalController.getAnimalById(filteredAnimal.getId()));
            } else {
                animals = animalController.getAllAnimals();
            }
            
            for (Animal animal : animals) {
                for (Treatment treatment : animal.getTreatments()) {
                    Object[] rowData = new Object[9];
                    rowData[0] = treatment.getId();
                    rowData[1] = animal.getName();
                    rowData[2] = treatment.getType().toString();
                    rowData[3] = treatment.getName();
                    rowData[4] = treatment.getDescription();
                    rowData[5] = treatment.getAdministrationDate() != null ? treatment.getAdministrationDate().toString() : "";
                    rowData[6] = treatment.getNextDueDate() != null ? treatment.getNextDueDate().toString() : "";
                    rowData[7] = treatment.isAdministered() ? "Yes" : "No";
                    
                    // Status (Overdue, Due Soon, OK)
                    String status = "OK";
                    if (treatment.isOverdue()) {
                        status = "OVERDUE";
                    } else if (treatment.getNextDueDate() != null) {
                        LocalDate now = LocalDate.now();
                        LocalDate oneWeekFromNow = now.plusDays(7);
                        if (treatment.getNextDueDate().isBefore(oneWeekFromNow)) {
                            status = "DUE SOON";
                        }
                    }
                    rowData[8] = status;
                    
                    tableModel.addRow(rowData);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Error loading treatments: " + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Show dialog to add a new treatment
     */
    private void showAddTreatmentDialog() {
        try {
            // Get all animals for selection
            List<Animal> animals = animalController.getAllAnimals();
            if (animals.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "No animals available. Please add an animal first.",
                        "No Animals", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            JPanel panel = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(5, 5, 5, 5);
            
            // Animal selection
            gbc.gridx = 0;
            gbc.gridy = 0;
            panel.add(new JLabel("Animal:"), gbc);
            
            gbc.gridx = 1;
            JComboBox<Animal> animalCombo = new JComboBox<>();
            for (Animal animal : animals) {
                animalCombo.addItem(animal);
            }
            // If we have a filtered animal, preselect it
            if (filteredAnimal != null) {
                for (int i = 0; i < animalCombo.getItemCount(); i++) {
                    if (animalCombo.getItemAt(i).getId() == filteredAnimal.getId()) {
                        animalCombo.setSelectedIndex(i);
                        break;
                    }
                }
            }
            animalCombo.setRenderer(new AnimalListCellRenderer());
            panel.add(animalCombo, gbc);
            
            // Treatment Type
            gbc.gridx = 0;
            gbc.gridy = 1;
            panel.add(new JLabel("Treatment Type:"), gbc);
            
            gbc.gridx = 1;
            JComboBox<TreatmentType> typeCombo = new JComboBox<>(TreatmentType.values());
            panel.add(typeCombo, gbc);
            
            // Name
            gbc.gridx = 0;
            gbc.gridy = 2;
            panel.add(new JLabel("Name:"), gbc);
            
            gbc.gridx = 1;
            JTextField nameField = new JTextField(20);
            panel.add(nameField, gbc);
            
            // Description
            gbc.gridx = 0;
            gbc.gridy = 3;
            panel.add(new JLabel("Description:"), gbc);
            
            gbc.gridx = 1;
            JTextField descriptionField = new JTextField(20);
            panel.add(descriptionField, gbc);
            
            // Next due date
            gbc.gridx = 0;
            gbc.gridy = 4;
            panel.add(new JLabel("Next Due Date (YYYY-MM-DD):"), gbc);
            
            gbc.gridx = 1;
            JTextField dueDateField = new JTextField(20);
            panel.add(dueDateField, gbc);
            
            int result = JOptionPane.showConfirmDialog(this, panel,
                    "Add Treatment", JOptionPane.OK_CANCEL_OPTION);
            
            if (result == JOptionPane.OK_OPTION) {
                try {
                    Animal selectedAnimal = (Animal) animalCombo.getSelectedItem();
                    TreatmentType selectedType = (TreatmentType) typeCombo.getSelectedItem();
                    String name = nameField.getText().trim();
                    String description = descriptionField.getText().trim();
                    String dueDateStr = dueDateField.getText().trim();
                    
                    if (name.isEmpty()) {
                        JOptionPane.showMessageDialog(this,
                                "Treatment name is a required field.",
                                "Validation Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    
                    LocalDate dueDate = null;
                    if (!dueDateStr.isEmpty()) {
                        try {
                            dueDate = LocalDate.parse(dueDateStr);
                        } catch (DateTimeParseException e) {
                            JOptionPane.showMessageDialog(this,
                                    "Invalid due date format. Please use YYYY-MM-DD.",
                                    "Validation Error", JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                    }
                    
                    // Add the treatment
                    Treatment treatment = animalController.addTreatment(
                            selectedAnimal.getId(), selectedType, name, description, dueDate);
                    
                    if (treatment != null) {
                        refreshData();
                    } else {
                        JOptionPane.showMessageDialog(this,
                                "Failed to add treatment.",
                                "Add Error", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(this,
                            "Error adding treatment: " + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Error loading animals: " + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Show dialog to administer a treatment
     */
    private void showAdministerDialog(int treatmentId) {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // Next due date for the next administration
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Next Due Date (YYYY-MM-DD):"), gbc);
        
        gbc.gridx = 1;
        JTextField dueDateField = new JTextField(20);
        panel.add(dueDateField, gbc);
        
        int result = JOptionPane.showConfirmDialog(this, panel,
                "Administer Treatment", JOptionPane.OK_CANCEL_OPTION);
        
        if (result == JOptionPane.OK_OPTION) {
            try {
                String dueDateStr = dueDateField.getText().trim();
                
                LocalDate dueDate = null;
                if (!dueDateStr.isEmpty()) {
                    try {
                        dueDate = LocalDate.parse(dueDateStr);
                    } catch (DateTimeParseException e) {
                        JOptionPane.showMessageDialog(this,
                                "Invalid due date format. Please use YYYY-MM-DD.",
                                "Validation Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }
                
                // Administer the treatment
                boolean success = animalController.administerTreatment(treatmentId, dueDate);
                
                if (success) {
                    refreshData();
                } else {
                    JOptionPane.showMessageDialog(this,
                            "Failed to administer treatment.",
                            "Administration Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this,
                        "Error administering treatment: " + e.getMessage(),
                        "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    /**
     * Show dialog to filter treatments by animal
     */
    private void showFilterDialog() {
        try {
            // Get all animals for selection
            List<Animal> animals = animalController.getAllAnimals();
            if (animals.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "No animals available for filtering.",
                        "No Animals", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            JComboBox<Animal> animalCombo = new JComboBox<>();
            for (Animal animal : animals) {
                animalCombo.addItem(animal);
            }
            animalCombo.setRenderer(new AnimalListCellRenderer());
            
            int result = JOptionPane.showConfirmDialog(this, animalCombo,
                    "Select Animal for Filtering", JOptionPane.OK_CANCEL_OPTION);
            
            if (result == JOptionPane.OK_OPTION) {
                Animal selectedAnimal = (Animal) animalCombo.getSelectedItem();
                filteredAnimal = selectedAnimal;
                refreshData();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Error loading animals: " + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Custom renderer for Animal objects in JComboBox
     */
    private class AnimalListCellRenderer extends javax.swing.DefaultListCellRenderer {
        private static final long serialVersionUID = 1L;
        
        @Override
        public java.awt.Component getListCellRendererComponent(
                javax.swing.JList<?> list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {
            
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            
            if (value instanceof Animal) {
                Animal animal = (Animal) value;
                setText(animal.getName() + " (" + animal.getSpecies() + ")");
            }
            
            return this;
        }
    }
} 