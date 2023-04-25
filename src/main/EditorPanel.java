/*
 * Created by JFormDesigner on Sun Apr 23 17:43:07 CDT 2023
 */

package main;

import binaryutils.BinaryInt;
import binaryutils.BinaryIntOutOfRangeException;
import binaryutils.trifindo.BinaryReader;
import binaryutils.trifindo.BinaryWriter;
import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatLightLaf;
import levelscript.*;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

public class EditorPanel extends JFrame {
    public static Preferences prefs = Preferences.userNodeForPackage(EditorPanel.class);
    private String defaultTitle;

    private JRadioButton[] triggerTypeButtons;

    private File currentFile = null;
    private LSTrigger selected = null;
    private boolean editMode;
//    private BooleanProperty editMode = new SimpleBooleanProperty();
//    private DoubleProperty opacity = new SimpleDoubleProperty();

    DefaultListModel<LSTrigger> listModel = new DefaultListModel();

    DocumentListener documentListener = new DocumentListener() {
        @Override
        public void insertUpdate(DocumentEvent e) {
            paramFieldTextChange();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            paramFieldTextChange();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            paramFieldTextChange();
        }
    };

    public EditorPanel() throws InvalidFieldsException, DuplicateTriggerException {
        editMode = false;
        initComponents();
        defaultTitle = getTitle();
        varValueButton.setSelected(true);
        paddingCheckbox.setSelected(true);

        levelScriptList.setModel(listModel);
        levelScriptList.setSelectedIndex(-1);
        levelScriptListValueChanged(null);
        triggerTypeButtons = new JRadioButton[]{varValueButton, mapChangeButton, screenResetButton, loadGameButton};
        startOver();

        scriptNoField.getDocument().addDocumentListener(documentListener);
        variableField.getDocument().addDocumentListener(documentListener);
        valueField.getDocument().addDocumentListener(documentListener);

        String prefLaF = prefs.get("LaF", null);

        if (prefLaF == null) {
            prefLaF = "dark";
        }

        if (prefLaF.equals("dark")) {
            darkMenuItemActionPerformed(null);
        } else {
            lightMenuItemActionPerformed(null);
        }

        removeButton.setEnabled(false);
        SwingUtilities.updateComponentTreeUI(this);
    }

    public void saveToFile(File toWrite) {
        int bytesWritten;
        try (BinaryWriter bw = new BinaryWriter(toWrite)) {

            TreeSet<MapScreenLoadTrigger> tsMapScreenLoad = new TreeSet<>();
            TreeSet<VariableValueTrigger> tsVariable = new TreeSet<>();

            for (LSTrigger lst : Arrays.stream(listModel.toArray()).map(s -> (LSTrigger) s).collect(Collectors.toList())) {
                if (lst.getTriggerType() == LSTrigger.VARIABLEVALUE) {
                    tsVariable.add((VariableValueTrigger) lst);
                } else {
                    tsMapScreenLoad.add((MapScreenLoadTrigger) lst);
                }
            }

            for (LSTrigger lstm : tsMapScreenLoad) {
                bw.writeUInt8(lstm.getTriggerType());
                bw.writeUInt32(lstm.getScriptTriggered());
            }

            if (tsVariable.size() != 0) {
                bw.writeUInt8(LSTrigger.VARIABLEVALUE);
                bw.writeUInt32(1);
                bw.writeUInt8(0);
                for (VariableValueTrigger lstv : tsVariable) {
                    bw.writeUInt16(lstv.getVariableToWatch());
                    bw.writeUInt16(lstv.getExpectedValue());
                    bw.writeUInt16(lstv.getScriptTriggered());
                }
            }

            bw.writeUInt16(0);
            if (paddingCheckbox.isSelected()) {
                int missingBytes = bw.getBytesWritten() % 4;
                if (missingBytes != 0) {
                    for (int i = 0; i < 4 - missingBytes; i++) {
                        bw.writeUInt8(0);
                    }
                }
            }

            bytesWritten = bw.getBytesWritten();
        } catch (FileNotFoundException e) {
            LSTrigger.customAlert("The file couldn't be located.");
            return;
        } catch (IOException e) {
            LSTrigger.customAlert("Error writing file.");
            return;
        }

        if (bytesWritten <= 4) {
            LSTrigger.customInfo("Empty level script file was correctly saved.", "Success!");
        } else {
            LSTrigger.customInfo("File was correctly saved.", "Success!");
        }

        setTitle(toWrite.getName() + " - " + defaultTitle);
    }

    private void parseFile(File toparse) {
        Set<LSTrigger> bufferSet = new HashSet<>();

        try {
            BinaryReader br = new BinaryReader(toparse);

            int scriptType;
            boolean hasConditionalStructure = false;
            int conditionalStructureOffset = -1;

            while ((scriptType = br.readUInt8()) >= LSTrigger.VARIABLEVALUE
                    && scriptType <= LSTrigger.LOADGAME) {
                long scriptToTrigger;

                if (hasConditionalStructure) conditionalStructureOffset--;
                if (scriptType != LSTrigger.VARIABLEVALUE) {
                    scriptToTrigger = br.readUInt32();
                    if (hasConditionalStructure) conditionalStructureOffset -= 4;
                    bufferSet.add(new MapScreenLoadTrigger(scriptType, (int) scriptToTrigger));
                } else {
                    hasConditionalStructure = true;
                    conditionalStructureOffset = (int) br.readUInt32();
                }
            }

            if (br.getBytesRead() == 1) {
                if (br.readUInt16() == 0 && toparse.length() < LSTrigger.SMALLEST_TRIGGER_SIZE) {
                    LSTrigger.customInfo("This level script does nothing.", "Interesting...");
                    return;
                }
            }

            if (br.getBytesRead() < LSTrigger.SMALLEST_TRIGGER_SIZE) {
                LSTrigger.customAlert("Parser failure: The input file you attempted to load is either malformed or not a Level Script file. ");
                return;
            }

            if (hasConditionalStructure) {
                if (conditionalStructureOffset != 1) {
                    LSTrigger.customAlert("Field error: The Level Script file you attempted to load is broken. " + conditionalStructureOffset);
                    return;
                } else {
                    int variableID;
                    while ((variableID = br.readUInt16()) > 0) {
                        int varExpectedValue = br.readUInt16();
                        int scriptToTrigger = br.readUInt16();
                        bufferSet.add(new VariableValueTrigger(scriptToTrigger, variableID, varExpectedValue));
                    }
                }
            }
        } catch (FileNotFoundException e) {
            LSTrigger.customAlert("The file couldn't be located.");
        } catch (EOFException eof) {
            System.out.println("End of File reached");
        } catch (IOException e) {
            LSTrigger.customAlert("Error reading file.");
        }

        startOver();
        for (LSTrigger trigger : bufferSet) {
            listModel.addElement(trigger);
        }

        setTitle(toparse.getName() + " - " + defaultTitle);
    }

    private void startOver() {
        SwingUtilities.updateComponentTreeUI(this);
        editMode = false;
        toggleEditModeStates();
        levelScriptList.clearSelection();
        listModel.removeAllElements();
        levelScriptList.clearSelection();
        varValueButton.setSelected(true);
        paddingCheckbox.setSelected(true);
        confirmButton.setEnabled(false);
        discardButton.setEnabled(false);
        confirmButton.setVisible(false);
        discardButton.setVisible(false);
        addButton.setEnabled(false);
        removeButton.setEnabled(false);
        clearInputFields();
        SwingUtilities.updateComponentTreeUI(this);
    }

    private void clearInputFields() {
        valueField.setText("");
        variableField.setText("");
        scriptNoField.setText("");
    }

    public int getSelectedButtonID() {
        int scriptType = 0;
        while (!triggerTypeButtons[scriptType].isSelected()) {
            scriptType++;
        }
        return scriptType + 1;
    }

    private LSTrigger buildTriggerFromFields() throws InvalidFieldsException {
        int triggerType = getSelectedButtonID();
        Integer scriptID = null, variableID = null, varExpectedValue = null;
        String errorFields = "";

        try {
            BinaryInt.checkU16(scriptID = Integer.parseInt(scriptNoField.getText()));
        } catch (NumberFormatException | BinaryIntOutOfRangeException e) {
            errorFields += "\n- Script to trigger";
        }

        if (triggerType == MapScreenLoadTrigger.VARIABLEVALUE) {
            try {
                BinaryInt.checkU16(variableID = Integer.parseInt(variableField.getText()));
            } catch (NumberFormatException | BinaryIntOutOfRangeException e) {
                errorFields += "\n- Variable to watch";
            }

            try {
                BinaryInt.checkU16(varExpectedValue = Integer.parseInt(valueField.getText()));
            } catch (NumberFormatException | BinaryIntOutOfRangeException e) {
                errorFields += "\n- Expected Value of the variable";
            }
        }

        if (!errorFields.equals("")) {
            throw new InvalidFieldsException(errorFields);
        }

        if (triggerType == MapScreenLoadTrigger.VARIABLEVALUE) {
            return new VariableValueTrigger(scriptID, variableID, varExpectedValue);
        } else {
            return new MapScreenLoadTrigger(triggerType, scriptID);
        }
    }

    private void newButtonActionPerformed(ActionEvent e) {
        int result = JOptionPane.showConfirmDialog(this, "Discard all and Reset GUI?", "Start over", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (result == JOptionPane.YES_OPTION) {
            startOver();
            currentFile = null;
            setTitle("Unsaved Level Script" + " - " + defaultTitle);
        }
    }

    private void openButtonActionPerformed(ActionEvent e) {
        String prefPath = prefs.get("LastPath", null);
        if (prefPath == null) {
            prefPath = System.getProperty("user.dir");
        }

        JFileChooser fc = new JFileChooser(prefPath);
        fc.setMultiSelectionEnabled(false);
        fc.addChoosableFileFilter(new MyFilter("Level Script files", ".scr", ".bin"));
        fc.setAcceptAllFileFilterUsed(true);

        int returnVal = fc.showOpenDialog(this);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            currentFile = fc.getSelectedFile();
            prefs.put("LastPath", currentFile.getAbsolutePath());
            parseFile(currentFile);
        } else {
            System.out.println("Open file cancelled");
        }
    }

    private void saveButtonActionPerformed(ActionEvent e) {
        if (currentFile == null) {
            saveAs();
        } else {
            saveToFile(currentFile);
        }
    }

    private void varValueButtonActionPerformed(ActionEvent e) {
        changeFieldVisibility(true);
        if (isAnyFieldEmpty()) {
            addButton.setEnabled(false);
        }
    }

    private void mapChangeButtonActionPerformed(ActionEvent e) {
        changeFieldVisibility(false);
        if (!scriptNoField.getText().equals("")) {
            addButton.setEnabled(true);
        }
    }

    private void screenResetButtonActionPerformed(ActionEvent e) {
        changeFieldVisibility(false);
        if (!scriptNoField.getText().equals("")) {
            addButton.setEnabled(true);
        }
    }

    private void loadGameButtonActionPerformed(ActionEvent e) {
        changeFieldVisibility(false);
        if (!scriptNoField.getText().equals("")) {
            addButton.setEnabled(true);
        }
    }

    private LSTrigger addTriggerToList() {
        try {
            LSTrigger built = buildTriggerFromFields();

            if (listModel.contains(built)) {
                if (!editMode) {
                    throw new DuplicateTriggerException();
                } else {
                    built = null;
                }
            } else {
                listModel.addElement(built);
            }

            clearInputFields();
            return built;
        } catch (InvalidFieldsException | DuplicateTriggerException ex) {
            ex.printStackTrace();
        }

        return null;
    }

    private void addButtonActionPerformed(ActionEvent e) {
        addTriggerToList();
    }

    private void removeButtonActionPerformed(ActionEvent e) {
        int selection = levelScriptList.getSelectedIndex();
        if (selection != -1) {
            listModel.remove(selection);
        }
    }

    void changeFieldVisibility(boolean setting) {
        if (setting) {
            variableLabel.setVisible(true);
            variableField.setVisible(true);
            valueLabel.setVisible(true);
            valueField.setVisible(true);
        } else {
            variableLabel.setVisible(false);
            variableField.setVisible(false);
            valueLabel.setVisible(false);
            valueField.setVisible(false);
        }
    }

    void saveAs() {
        String prefPath = prefs.get("LastPath", null);
        if (prefPath == null) {
            prefPath = System.getProperty("user.dir");
        }

        JFileChooser fc = new JFileChooser(prefPath);
        fc.setMultiSelectionEnabled(false);
        fc.addChoosableFileFilter(new MyFilter("Level Script files", ".scr"));
        fc.setAcceptAllFileFilterUsed(true);

        int returnVal = fc.showOpenDialog(this);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            currentFile = fc.getSelectedFile();
            if (!currentFile.getAbsolutePath().endsWith(".scr") && !currentFile.exists()) {
                currentFile = new File(currentFile.getAbsolutePath() + ".scr");
            }
            prefs.put("LastPath", currentFile.getAbsolutePath());
            saveToFile(currentFile);
        } else {
            System.out.println("Save file cancelled");
        }
    }

    private void newMenuItemActionPerformed(ActionEvent e) {
        newButtonActionPerformed(e);
    }

    private void openMenuItemActionPerformed(ActionEvent e) {
        openButtonActionPerformed(e);
    }

    private void saveMenuItemActionPerformed(ActionEvent e) {
        saveButtonActionPerformed(e);
    }

    private void saveAsMenuItemActionPerformed(ActionEvent e) {
        saveAs();
    }

    private void quitMenuItemActionPerformed(ActionEvent e) {
        thisWindowClosing(null);
    }

    private void aboutMenuItemActionPerformed(ActionEvent e) {
        JOptionPane.showMessageDialog(this, defaultTitle + " by AdAstra/LD3005. [2023]\n\nJava11 port by turtleisaac.", "About...", JOptionPane.INFORMATION_MESSAGE, new ImageIcon(EditorPanel.class.getResource("/resources/logo.png")));
    }

    private void thisWindowClosing(WindowEvent e) {
        int result = JOptionPane.showConfirmDialog(this, "All unsaved changes will be lost.", "Quit?", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (result == JOptionPane.YES_OPTION) {
            dispose();
            System.exit(0);
        }
    }

    private void startEditing() {
        if (levelScriptList.getSelectedIndex() != -1) {
            editMode = true;
            toggleEditModeStates();

            selected = levelScriptList.getSelectedValue();
            scriptNoField.requestFocus();

            scriptNoField.setText(String.valueOf(selected.getScriptTriggered()));
            buttonGroup1.clearSelection();
            buttonGroup1.setSelected(triggerTypeButtons[selected.getTriggerType() - 1].getModel(), true);
            if (selected.getTriggerType() == LSTrigger.VARIABLEVALUE) {
                VariableValueTrigger varLStrig = (VariableValueTrigger) selected;

                variableField.setText(String.valueOf(varLStrig.getVariableToWatch()));
                valueField.setText(String.valueOf(varLStrig.getExpectedValue()));
            }
        } else {
            editMode = false;
            toggleEditModeStates();
        }
        SwingUtilities.updateComponentTreeUI(this);
    }

    private void levelScriptListValueChanged(ListSelectionEvent e) {
        removeButton.setEnabled(!levelScriptList.isSelectionEmpty());
    }

    private void createUIComponents() {
        // TODO: add custom component creation code here
    }

    private void darkMenuItemActionPerformed(ActionEvent e) {
        try {
            UIManager.setLookAndFeel(new FlatDarculaLaf());
        } catch (UnsupportedLookAndFeelException ex) {
            ex.printStackTrace();
        }

        SwingUtilities.updateComponentTreeUI(this);
        pack();
    }

    private void lightMenuItemActionPerformed(ActionEvent e) {
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (UnsupportedLookAndFeelException ex) {
            ex.printStackTrace();
        }
        SwingUtilities.updateComponentTreeUI(this);
        pack();
    }

    private boolean isNoFieldNull() {
        return scriptNoField.getText() != null && variableField.getText() != null && valueField.getText() != null;
    }

    private boolean isAnyFieldEmpty() {
        return scriptNoField.getText().equals("") || variableField.getText().equals("") || valueField.getText().equals("");
    }

    private void paramFieldTextChange() {
        if (isNoFieldNull()) {
            if (!scriptNoField.getText().equals("") && !varValueButton.isSelected()) {
                if (!addButton.isEnabled() && !editMode) {
                    addButton.setEnabled(true);
                }
            } else if (isAnyFieldEmpty()) {
                if (addButton.isEnabled()) {
                    addButton.setEnabled(false);
                }
            } else { //all are filled
                if (!addButton.isEnabled() && !editMode) {
                    addButton.setEnabled(true);
                }
            }
            SwingUtilities.updateComponentTreeUI(this);
        }

    }

    private void levelScriptListMousePressed(MouseEvent e) {
        if (SwingUtilities.isRightMouseButton(e)) {
            JPopupMenu menu = new JPopupMenu();
            JMenuItem editItem = new JMenuItem("Edit selected trigger");
            JMenuItem removeItem = new JMenuItem("Remove selected trigger");
            editItem.addActionListener(e1 -> startEditing());
            removeItem.addActionListener(e12 -> removeButtonActionPerformed(e12));

            if (listModel.isEmpty()) {
                menu.setEnabled(false);
                editItem.setEnabled(false);
                removeItem.setEnabled(false);
            }

            menu.add(editItem);
            menu.add(removeItem);
            menu.show(levelScriptList, e.getX(), e.getY());
        }
    }

    private void confirmButtonActionPerformed(ActionEvent e) {
        LSTrigger built = addTriggerToList();

        if (built != null)
            listModel.remove(levelScriptList.getSelectedIndex());

        int count = -1;
        for (LSTrigger lst : Arrays.stream(listModel.toArray()).map(s -> (LSTrigger) s).collect(Collectors.toList())) {
            if (!lst.equals(built)) {
                count++;
            }
        }

        levelScriptList.setSelectedIndex(count + 1);
        editMode = false;
        toggleEditModeStates();
    }

    private void discardButtonActionPerformed(ActionEvent e) {
        clearInputFields();
        editMode = false;
        toggleEditModeStates();
    }


    private void toggleEditModeStates() {
        if (editMode) {
            removeButton.setEnabled(false);
            addButton.setEnabled(false);
            confirmButton.setEnabled(true);
            discardButton.setEnabled(true);
            confirmButton.setVisible(true);
            discardButton.setVisible(true);
            paddingCheckbox.setEnabled(false);
        } else {
            addButton.setEnabled(true);
            removeButton.setEnabled(true);
            confirmButton.setEnabled(false);
            discardButton.setEnabled(false);
            confirmButton.setVisible(false);
            discardButton.setVisible(false);
            paddingCheckbox.setEnabled(true);
        }
    }


    private void initComponents() throws InvalidFieldsException, DuplicateTriggerException {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        ResourceBundle bundle = ResourceBundle.getBundle("resources.gui");
        triggerTypeLabel = new JLabel();
        newButton = new JButton();
        openButton = new JButton();
        saveButton = new JButton();
        varValueButton = new JRadioButton();
        mapChangeButton = new JRadioButton();
        screenResetButton = new JRadioButton();
        loadGameButton = new JRadioButton();
        separator1 = new JSeparator();
        panel1 = new JPanel();
        scrollPane1 = new JScrollPane();
        levelScriptList = new JList<>();
        configLabel = new JLabel();
        separator2 = new JSeparator();
        scriptLabel = new JLabel();
        scriptNoField = new JTextField();
        variableLabel = new JLabel();
        variableField = new JTextField();
        valueLabel = new JLabel();
        valueField = new JTextField();
        confirmButton = new JButton();
        discardButton = new JButton();
        paddingCheckbox = new JCheckBox();
        addButton = new JButton();
        removeButton = new JButton();
        menuBar1 = new JMenuBar();
        fileMenu = new JMenu();
        newMenuItem = new JMenuItem();
        openMenuItem = new JMenuItem();
        saveMenuItem = new JMenuItem();
        saveAsMenuItem = new JMenuItem();
        quitMenuItem = new JMenuItem();
        windowMenu = new JMenu();
        darkMenuItem = new JMenuItem();
        lightMenuItem = new JMenuItem();
        helpMenu = new JMenu();
        aboutMenuItem = new JMenuItem();
        buttonGroup1 = new ButtonGroup();

        //======== this ========
        setMinimumSize(new Dimension(590, 600));
        setName("Pok\u00e9mon Level Script Editor v1.2 [Swing Edition]");
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setResizable(false);
        setTitle("Pok\u00e9mon Level Script Editor v1.2 [Swing Edition]");
        setIconImage(new ImageIcon(getClass().getResource("/resources/LSE.png")).getImage());
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                thisWindowClosing(e);
            }
        });
        var contentPane = getContentPane();
        contentPane.setLayout(new MigLayout(
            "insets 5,hidemode 3",
            // columns
            "[127,left]" +
            "[462,fill]",
            // rows
            "[]" +
            "[]" +
            "[fill]" +
            "[grow]" +
            "[]" +
            "[]"));

        //---- triggerTypeLabel ----
        triggerTypeLabel.setText(bundle.getString("EditorPanel.triggerTypeLabel.text"));
        triggerTypeLabel.setFont(new Font(".AppleSystemUIFont", Font.PLAIN, 16));
        contentPane.add(triggerTypeLabel, "cell 1 0");

        //---- newButton ----
        newButton.setIcon(new ImageIcon(getClass().getResource("/resources/new.png")));
        newButton.setMaximumSize(new Dimension(38, 38));
        newButton.setMinimumSize(new Dimension(38, 38));
        newButton.addActionListener(e -> newButtonActionPerformed(e));
        contentPane.add(newButton, "cell 0 0 1 2,grow");

        //---- openButton ----
        openButton.setIcon(new ImageIcon(getClass().getResource("/resources/open.png")));
        openButton.setMaximumSize(new Dimension(38, 38));
        openButton.setMinimumSize(new Dimension(38, 38));
        openButton.addActionListener(e -> openButtonActionPerformed(e));
        contentPane.add(openButton, "cell 0 0 1 2,grow");

        //---- saveButton ----
        saveButton.setIcon(new ImageIcon(getClass().getResource("/resources/save.png")));
        saveButton.setMaximumSize(new Dimension(38, 38));
        saveButton.setMinimumSize(new Dimension(38, 38));
        saveButton.addActionListener(e -> saveButtonActionPerformed(e));
        contentPane.add(saveButton, "cell 0 0 1 2,grow");

        //---- varValueButton ----
        varValueButton.setText(bundle.getString("EditorPanel.varValueButton.text"));
        varValueButton.addActionListener(e -> varValueButtonActionPerformed(e));
        contentPane.add(varValueButton, "cell 1 1");

        //---- mapChangeButton ----
        mapChangeButton.setText(bundle.getString("EditorPanel.mapChangeButton.text"));
        mapChangeButton.addActionListener(e -> mapChangeButtonActionPerformed(e));
        contentPane.add(mapChangeButton, "cell 1 1");

        //---- screenResetButton ----
        screenResetButton.setText(bundle.getString("EditorPanel.screenResetButton.text"));
        screenResetButton.addActionListener(e -> screenResetButtonActionPerformed(e));
        contentPane.add(screenResetButton, "cell 1 1");

        //---- loadGameButton ----
        loadGameButton.setText("Load game");
        loadGameButton.addActionListener(e -> loadGameButtonActionPerformed(e));
        contentPane.add(loadGameButton, "cell 1 1");
        contentPane.add(separator1, "cell 0 2 2 1,grow");

        //======== panel1 ========
        {
            panel1.setLayout(new MigLayout(
                "hidemode 3",
                // columns
                "[359,fill]" +
                "[grow,fill]",
                // rows
                "[]" +
                "[]" +
                "[]" +
                "[]" +
                "[]" +
                "[]" +
                "[]" +
                "[grow]" +
                "[]" +
                "[]" +
                "[]"));

            //======== scrollPane1 ========
            {

                //---- levelScriptList ----
                levelScriptList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                levelScriptList.addListSelectionListener(e -> levelScriptListValueChanged(e));
                levelScriptList.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) {
                        levelScriptListMousePressed(e);
                    }
                });
                scrollPane1.setViewportView(levelScriptList);
            }
            panel1.add(scrollPane1, "cell 0 0 1 11,grow");

            //---- configLabel ----
            configLabel.setText(bundle.getString("EditorPanel.configLabel.text"));
            configLabel.setFont(new Font(".AppleSystemUIFont", Font.PLAIN, 16));
            panel1.add(configLabel, "cell 1 0");
            panel1.add(separator2, "cell 1 1");

            //---- scriptLabel ----
            scriptLabel.setText(bundle.getString("EditorPanel.scriptLabel.text"));
            panel1.add(scriptLabel, "cell 1 2");
            panel1.add(scriptNoField, "cell 1 3,aligny top,grow 100 0");

            //---- variableLabel ----
            variableLabel.setText(bundle.getString("EditorPanel.variableLabel.text"));
            panel1.add(variableLabel, "cell 1 4,aligny top,growy 0");
            panel1.add(variableField, "cell 1 5,aligny top,grow 100 0");

            //---- valueLabel ----
            valueLabel.setText(bundle.getString("EditorPanel.valueLabel.text"));
            panel1.add(valueLabel, "cell 1 6,aligny top,growy 0");
            panel1.add(valueField, "cell 1 7,aligny top,grow 100 0");

            //---- confirmButton ----
            confirmButton.setText(bundle.getString("EditorPanel.confirmButton.text"));
            confirmButton.setIcon(new ImageIcon(getClass().getResource("/resources/confirm.png")));
            confirmButton.setEnabled(false);
            confirmButton.addActionListener(e -> confirmButtonActionPerformed(e));
            panel1.add(confirmButton, "cell 1 8");

            //---- discardButton ----
            discardButton.setText(bundle.getString("EditorPanel.discardButton.text"));
            discardButton.setIcon(new ImageIcon(getClass().getResource("/resources/discard.png")));
            discardButton.setEnabled(false);
            discardButton.addActionListener(e -> discardButtonActionPerformed(e));
            panel1.add(discardButton, "cell 1 8");

            //---- paddingCheckbox ----
            paddingCheckbox.setText(bundle.getString("EditorPanel.paddingCheckbox.text"));
            panel1.add(paddingCheckbox, "cell 1 9");

            //---- addButton ----
            addButton.setText(bundle.getString("EditorPanel.addButton.text"));
            addButton.setIcon(new ImageIcon(getClass().getResource("/resources/add.png")));
            addButton.setEnabled(false);
            addButton.addActionListener(e -> addButtonActionPerformed(e));
            panel1.add(addButton, "cell 1 10");

            //---- removeButton ----
            removeButton.setText(bundle.getString("EditorPanel.removeButton.text"));
            removeButton.setIcon(new ImageIcon(getClass().getResource("/resources/remove.png")));
            removeButton.setEnabled(false);
            removeButton.addActionListener(e -> removeButtonActionPerformed(e));
            panel1.add(removeButton, "cell 1 10");
        }
        contentPane.add(panel1, "cell 0 3 2 3,grow");

        //======== menuBar1 ========
        {

            //======== fileMenu ========
            {
                fileMenu.setText("File");

                //---- newMenuItem ----
                newMenuItem.setText(bundle.getString("EditorPanel.newMenuItem.text"));
                newMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
                newMenuItem.addActionListener(e -> newMenuItemActionPerformed(e));
                fileMenu.add(newMenuItem);

                //---- openMenuItem ----
                openMenuItem.setText(bundle.getString("EditorPanel.openMenuItem.text"));
                openMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
                openMenuItem.addActionListener(e -> openMenuItemActionPerformed(e));
                fileMenu.add(openMenuItem);

                //---- saveMenuItem ----
                saveMenuItem.setText(bundle.getString("EditorPanel.saveMenuItem.text"));
                saveMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
                saveMenuItem.addActionListener(e -> saveMenuItemActionPerformed(e));
                fileMenu.add(saveMenuItem);

                //---- saveAsMenuItem ----
                saveAsMenuItem.setText(bundle.getString("EditorPanel.saveAsMenuItem.text"));
                saveAsMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()|KeyEvent.SHIFT_DOWN_MASK));
                saveAsMenuItem.addActionListener(e -> saveAsMenuItemActionPerformed(e));
                fileMenu.add(saveAsMenuItem);

                //---- quitMenuItem ----
                quitMenuItem.setText(bundle.getString("EditorPanel.quitMenuItem.text"));
                quitMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
                quitMenuItem.addActionListener(e -> quitMenuItemActionPerformed(e));
                fileMenu.add(quitMenuItem);
            }
            menuBar1.add(fileMenu);

            //======== windowMenu ========
            {
                windowMenu.setText(bundle.getString("EditorPanel.windowMenu.text"));

                //---- darkMenuItem ----
                darkMenuItem.setText(bundle.getString("EditorPanel.darkMenuItem.text"));
                darkMenuItem.addActionListener(e -> darkMenuItemActionPerformed(e));
                windowMenu.add(darkMenuItem);

                //---- lightMenuItem ----
                lightMenuItem.setText(bundle.getString("EditorPanel.lightMenuItem.text"));
                lightMenuItem.addActionListener(e -> lightMenuItemActionPerformed(e));
                windowMenu.add(lightMenuItem);
            }
            menuBar1.add(windowMenu);

            //======== helpMenu ========
            {
                helpMenu.setText("Help");

                //---- aboutMenuItem ----
                aboutMenuItem.setText(bundle.getString("EditorPanel.aboutMenuItem.text"));
                aboutMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_B, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
                aboutMenuItem.addActionListener(e -> aboutMenuItemActionPerformed(e));
                helpMenu.add(aboutMenuItem);
            }
            menuBar1.add(helpMenu);
        }
        contentPane.add(menuBar1, "north,aligny top,grow 100 0");
        pack();
        setLocationRelativeTo(getOwner());

        //---- buttonGroup1 ----
        buttonGroup1.add(varValueButton);
        buttonGroup1.add(mapChangeButton);
        buttonGroup1.add(screenResetButton);
        buttonGroup1.add(loadGameButton);
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JLabel triggerTypeLabel;
    private JButton newButton;
    private JButton openButton;
    private JButton saveButton;
    private JRadioButton varValueButton;
    private JRadioButton mapChangeButton;
    private JRadioButton screenResetButton;
    private JRadioButton loadGameButton;
    private JSeparator separator1;
    private JPanel panel1;
    private JScrollPane scrollPane1;
    private JList<LSTrigger> levelScriptList;
    private JLabel configLabel;
    private JSeparator separator2;
    private JLabel scriptLabel;
    private JTextField scriptNoField;
    private JLabel variableLabel;
    private JTextField variableField;
    private JLabel valueLabel;
    private JTextField valueField;
    private JButton confirmButton;
    private JButton discardButton;
    private JCheckBox paddingCheckbox;
    private JButton addButton;
    private JButton removeButton;
    private JMenuBar menuBar1;
    private JMenu fileMenu;
    private JMenuItem newMenuItem;
    private JMenuItem openMenuItem;
    private JMenuItem saveMenuItem;
    private JMenuItem saveAsMenuItem;
    private JMenuItem quitMenuItem;
    private JMenu windowMenu;
    private JMenuItem darkMenuItem;
    private JMenuItem lightMenuItem;
    private JMenu helpMenu;
    private JMenuItem aboutMenuItem;
    private ButtonGroup buttonGroup1;
    // JFormDesigner - End of variables declaration  //GEN-END:variables

    public class MyFilter extends FileFilter {
        private String[] extensions;
        private String description;

        public MyFilter(String description, String... extensions) {
            this.extensions = extensions;
            this.description = description;
        }

        public boolean accept(File f) {
            for (String str : extensions) {
                if (f.getName().endsWith(str))
                    return true;
                else if (f.isDirectory())
                    return true;
            }
            return false;
        }

        @Override
        public String getDescription() {
            StringBuilder extensions = new StringBuilder(" (");
            String extension;

            for (int i = 0; i < this.extensions.length; i++) {
                extension = this.extensions[i];
                extensions.append("*").append(extension);
                if (i != this.extensions.length - 1)
                    extensions.append(", ");
            }
            extensions.append(")");

            return description + extensions.toString();
        }
    }

}
