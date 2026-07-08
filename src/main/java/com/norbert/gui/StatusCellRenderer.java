package com.norbert.gui;

import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Color;
import java.awt.Component;

public class StatusCellRenderer extends DefaultTableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(
            JTable table,
            Object value,
            boolean isSelected,
            boolean hasFocus,
            int row,
            int column
    ) {
        Component component = super.getTableCellRendererComponent(
                table,
                value,
                isSelected,
                hasFocus,
                row,
                column
        );

        String status = String.valueOf(value);
        setHorizontalAlignment(SwingConstants.CENTER);
        setOpaque(true);

        if (!isSelected) {
            component.setForeground(new Color(35, 47, 43));
            component.setBackground(getStatusBackground(status));
        }

        return component;
    }

    private Color getStatusBackground(String status) {
        switch (status) {
            case "Up to date":
                return new Color(102, 255, 0);
            case "Update available":
                return new Color(255, 253, 55);
            case "Local version unknown":
                return new Color(255, 0, 0);
            case "Local version is newer":
                return new Color(65, 105, 225);
            case "API error":
            case "Cancelled":
            case "Not found on Modrinth":
                return new Color(255, 0, 0);
            default:
                return Color.WHITE;
        }
    }
}
