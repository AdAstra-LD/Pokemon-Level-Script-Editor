package main;

import binaryutils.*;
import binaryutils.trifindo.BinaryReader;
import binaryutils.trifindo.BinaryWriter;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.beans.value.ObservableBooleanValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import levelscript.*;
import levelscript.InvalidFieldsException;

import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.prefs.Preferences;


public class Controller implements Initializable {
    public static Preferences prefs = Preferences.userNodeForPackage(Controller.class);
    private Stage stage;
    private String defaultTitle;

    private File currentFile = null;
    private LSTrigger selected = null;
    private BooleanProperty editMode = new SimpleBooleanProperty();
    private DoubleProperty opacity = new SimpleDoubleProperty();

    ObservableList<LSTrigger> olist;
    private ObservableSet<LSTrigger> lsobsSet;


    public void setStage(Stage s) {
        this.stage = s;
        defaultTitle = s.getTitle();
    }

    private void startOver() {
        editMode.set(false);
        opacity.set(1.0);

        lsobsSet.clear();
        variableRBTN.setSelected(true);
        paddingCHK.setSelected(true);

        idFLD.clear();
        valueFLD.clear();
        varFLD.clear();
    }

    private void clearInputFields() {
        idFLD.clear();
        valueFLD.clear();
        varFLD.clear();
    }

    public int getSelectedButtonID (ToggleGroup btns) {
        int scriptType = 0;
        while (!btns.getToggles().get(scriptType).isSelected()) {
            scriptType++;
        }
        return scriptType+1;
    }

    public void saveToFile (File toWrite) {
        int bytesWritten;
        try (BinaryWriter bw = new BinaryWriter(toWrite)){

            TreeSet<MapScreenLoadTrigger> tsMapScreenLoad = new TreeSet<>();
            TreeSet<VariableValueTrigger> tsVariable = new TreeSet<>();

            for (LSTrigger lst : lsobsSet) {
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
            if (paddingCHK.isSelected()) {
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
        stage.setTitle(toWrite.getName() + " - " + defaultTitle);
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
        lsobsSet.addAll(bufferSet);
        stage.setTitle(toparse.getName() + " - " + defaultTitle);
    }

    @FXML
    private MenuItem editMNU;

    @FXML
    private MenuItem removeMNU;

    @FXML
    private Button newBTN;

    @FXML
    private Button openBTN;

    @FXML
    private Button saveBTN;

    @FXML
    private HBox editModeBOX;

    @FXML
    private HBox normalbtnsBOX;

    @FXML
    private RadioButton variableRBTN;

    @FXML
    private CheckBox paddingCHK;

    @FXML
    private ToggleGroup levelscriptTypeGroup;

    @FXML
    private ListView<LSTrigger> list;

    @FXML
    private TextField idFLD;

    @FXML
    private Label varTXT;

    @FXML
    private TextField varFLD;

    @FXML
    private Label valueTXT;

    @FXML
    private TextField valueFLD;

    @FXML
    private Button addBTN;

    @FXML
    private Button removeBTN;

    @FXML
    private MenuItem saveMENUOPT;

    @FXML
    private MenuItem saveasMENUOPT;


    @FXML
    void quit(ActionEvent event) {
        Alert a = new Alert(Alert.AlertType.WARNING, "All unsaved changes will be lost.", ButtonType.OK, ButtonType.CANCEL);
        a.setHeaderText("Are you sure you want to quit?");
        a.setTitle("Quit?");
        Optional<ButtonType> bt = a.showAndWait();

        if (bt.isPresent() && bt.get() == ButtonType.OK) {
            Platform.exit();
        }

    }

    @FXML
    void about(ActionEvent event) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, defaultTitle + " by AdAstra/LD3005. [2020]", ButtonType.CLOSE);
        a.setHeaderText(null);
        a.setTitle("About...");
        a.setGraphic(new ImageView(new Image(getClass().getResource("/resources/logo.png").toExternalForm())));
        a.show();
    }


    @FXML
    void newLS(ActionEvent event) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Do you want to start over?");
        confirm.setHeaderText("Discard all and Reset GUI?");
        confirm.setTitle("Start over");
        Optional<ButtonType> bt = confirm.showAndWait();

        if (bt.isPresent() && bt.get() == ButtonType.OK) {
            startOver();
            currentFile = null;
            stage.setTitle("Unsaved Level Script" + " - " + defaultTitle);
        }
    }

    @FXML
    void openLS(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Level Script files (*.scr, *.bin)", "*.scr", "*.bin"));

        String prefPath = prefs.get("LastPath", null);
        File lastFile = new File(prefPath).getParentFile();
        if (prefPath != null && lastFile.exists()) {
            fileChooser.setInitialDirectory(lastFile);
        }
        currentFile = fileChooser.showOpenDialog(stage);

        if (currentFile == null) {
            System.out.println("Open file cancelled");
        } else {
            prefs.put("LastPath", currentFile.getAbsolutePath());
            parseFile(currentFile);
        }
    }

    @FXML
    void saveAsLS(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save to File");

        String prefPath = prefs.get("LastPath", null);
        if (prefPath != null)
            fileChooser.setInitialDirectory(new File(prefPath).getParentFile());
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Level Script files (*.scr, *.bin)", "*.scr", "*.bin")
                //new FileChooser.ExtensionFilter("Binary files (*.bin)", "*.bin")
        );
        currentFile = fileChooser.showSaveDialog(stage);

        if (currentFile == null) {
            System.out.println("Save file cancelled");
        } else {
            prefs.put("LastPath", currentFile.getAbsolutePath());
            saveToFile(currentFile);
        }
    }

    @FXML
    void saveLS(ActionEvent event) {
        if (currentFile == null) {
            saveAsLS(event);
        } else {
            saveToFile(currentFile);
        }
    }


    @FXML
    LSTrigger add(ActionEvent event) throws InvalidFieldsException, DuplicateTriggerException {
        LSTrigger built = buildTriggerFromFields();

        if (!lsobsSet.add(built)) {
            if(!editMode.get()) {
                throw new DuplicateTriggerException();
            } else {
                built = null;
            }
        }

        clearInputFields();
        return built;
    }

    private LSTrigger buildTriggerFromFields() throws InvalidFieldsException {
        int triggerType = getSelectedButtonID(levelscriptTypeGroup);
        Integer scriptID = null, variableID = null, varExpectedValue = null;
        String errorFields = "";

        try {
            BinaryInt.checkU16(scriptID = Integer.parseInt(idFLD.getText()));
        } catch (NumberFormatException | BinaryIntOutOfRangeException e) {
            errorFields += "\n- Script to trigger";
        }

        if (triggerType == MapScreenLoadTrigger.VARIABLEVALUE) {
            try {
                BinaryInt.checkU16(variableID = Integer.parseInt(varFLD.getText()));
            } catch (NumberFormatException | BinaryIntOutOfRangeException e) {
                errorFields += "\n- Variable to watch";
            }

            try {
                BinaryInt.checkU16(varExpectedValue = Integer.parseInt(valueFLD.getText()));
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

    @FXML
    void remove(ActionEvent event) {
        LSTrigger ls = list.getSelectionModel().getSelectedItem();
        lsobsSet.remove(ls);
    }

    @FXML
    void startEditing(ActionEvent event) {
        editMode.set(true);
        opacity.set(0.85);
        selected = list.getSelectionModel().getSelectedItem();
        idFLD.requestFocus();

        idFLD.setText(String.valueOf(selected.getScriptTriggered()));
        levelscriptTypeGroup.getToggles().get(selected.getTriggerType()-1).setSelected(true);
        if (selected.getTriggerType() == LSTrigger.VARIABLEVALUE) {
            VariableValueTrigger varLStrig = (VariableValueTrigger) selected;

            varFLD.setText(String.valueOf(varLStrig.getVariableToWatch()));
            valueFLD.setText(String.valueOf(varLStrig.getExpectedValue()));
        }
    }

    @FXML
    void confirm(ActionEvent event) throws InvalidFieldsException, DuplicateTriggerException {
        LSTrigger built = add(event);

        if (built != null)
            lsobsSet.remove(selected);

        list.getSelectionModel().select(built);
        editMode.set(false);
        opacity.set(1.0);
    }

    @FXML
    void discard(ActionEvent event) {
        clearInputFields();
        editMode.set(false);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        editMode.set(false);
        opacity.set(1.0);


        lsobsSet = FXCollections.observableSet();
        lsobsSet.addListener((SetChangeListener.Change<? extends LSTrigger> c) -> {
            if (c.wasAdded()) {
                list.getItems().add(c.getElementAdded());
            }
            if (c.wasRemoved()) {
                list.getItems().remove(c.getElementRemoved());
            }
        });


        olist = FXCollections.observableArrayList(lsobsSet);
        list.setItems(olist);


        setDefaultButtonStyle(newBTN);
        setDefaultButtonStyle(openBTN);
        setDefaultButtonStyle(saveBTN);

        varFLD.visibleProperty().bind(variableRBTN.selectedProperty());
        varTXT.visibleProperty().bind(variableRBTN.selectedProperty());

        valueFLD.visibleProperty().bind(variableRBTN.selectedProperty());
        valueTXT.visibleProperty().bind(variableRBTN.selectedProperty());


        ObservableBooleanValue a = variableRBTN.selectedProperty();
        ObservableBooleanValue b = idFLD.textProperty().isEmpty();
        ObservableBooleanValue c = varFLD.textProperty().isEmpty();
        ObservableBooleanValue d = valueFLD.textProperty().isEmpty();
        ObservableBooleanValue cORd = Bindings.or(c,d);

        //Add button disabled when:
        // - Script ID is empty
        // - but also when VariableScript is selected and any other text field is empty
        addBTN.disableProperty().bind(Bindings.and(a,cORd).or(b));

        // Remove button disabled when:
        // - List is empty
        SimpleListProperty<LSTrigger> slpTrigger = new SimpleListProperty<>(olist);
        removeBTN.disableProperty().bind(slpTrigger.emptyProperty());
        removeMNU.disableProperty().bind(slpTrigger.emptyProperty());
        editMNU.disableProperty().bind(slpTrigger.emptyProperty());
        //saveBTN.disableProperty().bind(Bindings.or(slpTrigger.emptyProperty(), editMode));
        saveasMENUOPT.disableProperty().bind(Bindings.or(slpTrigger.emptyProperty(), editMode));
        saveMENUOPT.disableProperty().bind(Bindings.or(slpTrigger.emptyProperty(), editMode));

        editModeBOX.visibleProperty().bind(editMode);
        normalbtnsBOX.disableProperty().bind(editMode);
        paddingCHK.disableProperty().bind(editMode);
        list.mouseTransparentProperty().bind(editMode);
        list.opacityProperty().bind(opacity);
    }

    private void setDefaultButtonStyle(Button btn) {
        btn.setOnMouseEntered(event -> btn.setStyle(null));
        btn.setOnMouseExited(event -> btn.setStyle("-fx-background-color: transparent"));
    }
}
