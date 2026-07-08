package com.norbert.app;

import com.norbert.gui.MainFrame;

import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args){
        SwingUtilities.invokeLater(() -> {
            new MainFrame();
        });
    }
}
