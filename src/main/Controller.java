package main;

import binaryutils.*;
import binaryutils.trifindo.BinaryReader;
import binaryutils.trifindo.BinaryWriter;
import guianimation.ShakeAnimation;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import levelscript.*;

import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.Observable;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.TreeSet;


public class Controller implements Initializable {
    private ObservableSet<LSTrigger> lsobsSet;
    private String filePath;

    public int getSelectedButtonID (ToggleGroup btns) {
        int scriptType = 0;
        while (!levelscriptTypeGroup.getToggles().get(scriptType).isSelected()) {
            scriptType++;
        }
        return scriptType+1;
    }

    public void saveToFile () {
        try {
            BinaryWriter bw = new BinaryWriter(filePath);
            TreeSet<LSTrigger> tsMapScreenLoad = new TreeSet<>();
            TreeSet<VariableValueTrigger> tsVariable = new TreeSet<>();

            for (LSTrigger lst : lsobsSet) {
                if (lst.getType() == LSTrigger.VARIABLEVALUE) {
                    tsVariable.add((VariableValueTrigger) lst);
                } else {
                    tsMapScreenLoad.add(lst);
                }
            }

            for (LSTrigger lstm : tsMapScreenLoad) {
                bw.writeUInt8(lstm.getType());
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

            bw.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Alert a = new Alert(Alert.AlertType.INFORMATION, "File successfully saved.");
        a.show();
    }

    private void parseFile() {
        try {
            BinaryReader br = new BinaryReader(filePath);
            int scriptType = 100;
            while (scriptType > 0) {
                scriptType = br.readUInt8();
                long scriptToTrigger;

                if (scriptType > 0 && scriptType != LSTrigger.VARIABLEVALUE) {
                    scriptToTrigger = br.readUInt32();
                    lsobsSet.add(new MapScreenLoadTrigger(scriptType, (int) scriptToTrigger));
                }
            }
        } catch (FileNotFoundException e) {
            LSTrigger.customAlert("The file couldn't be located.");
            return;
        } catch (EOFException eof) {
            System.out.println("End of File reached");
        } catch (IOException e) {
            LSTrigger.customAlert("Error reading file.");
            return;
        }
    }

    @FXML
    private Button newBTN;

    @FXML
    private Button openBTN;

    @FXML
    private Button saveBTN;

    @FXML
    private RadioButton variableRBTN;

    @FXML
    private CheckBox paddingCHK;

    @FXML
    private ToggleGroup levelscriptTypeGroup;

    @FXML
    private RadioButton mapRBTN;

    @FXML
    private RadioButton resetRBTN;

    @FXML
    private RadioButton loadgameRBTN;

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
    void newLS(ActionEvent event) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Do you want to start over?");
        Optional<ButtonType> bt = confirm.showAndWait();
            if (bt.get() == ButtonType.OK) {
                lsobsSet.clear();
                filePath = "";
                variableRBTN.setSelected(true);
                paddingCHK.setSelected(true);

                idFLD.clear();
                valueFLD.clear();
                varFLD.clear();
            } else {
                return;
            }
    }

    @FXML
    void openLS(ActionEvent event) {
        Stage s = new Stage();
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open File");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Level Script files (*.scr)", "*.scr"),
                new FileChooser.ExtensionFilter("Binary files (*.bin)", "*.bin")
        );
        File selectedFile = fileChooser.showOpenDialog(s);

        if (selectedFile == null) {
            System.out.println("Open file cancelled");
            return;
        } else {
            filePath = selectedFile.getAbsolutePath();
            parseFile();
        }
    }

    @FXML
    void saveAsLS(ActionEvent event) {
        Stage s = new Stage();
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save to File");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Level Script files (*.scr)", "*.scr"),
                new FileChooser.ExtensionFilter("Binary files (*.bin)", "*.bin")
        );
        File selectedFile = fileChooser.showSaveDialog(s);

        if (selectedFile == null) {
            System.out.println("Save file cancelled");
            return;
        } else {
            filePath = selectedFile.getAbsolutePath();
            saveToFile();
        }
    }

    @FXML
    void saveLS(ActionEvent event) {
        if (filePath == null || filePath == "") {
            saveAsLS(event);
        } else {
            saveToFile();
        }
    }


    @FXML
    void add(ActionEvent event) {
        int triggerType = getSelectedButtonID(levelscriptTypeGroup);

        Integer scriptID = null, variableID = null, varExpectedValue = null;
        String error = "";

        try {
            BinaryInt.checkU16(scriptID = Integer.parseInt(idFLD.getText()));
        } catch (NumberFormatException | BinaryIntOutOfRangeException e) {
            error += "\n- Script ID";
        }

        if (triggerType == MapScreenLoadTrigger.VARIABLEVALUE) {
            try {
                BinaryInt.checkU16(variableID = Integer.parseInt(varFLD.getText()));
            } catch (NumberFormatException | BinaryIntOutOfRangeException e) {
                error += "\n- Variable ID";
            }

            try {
                BinaryInt.checkU16(varExpectedValue = Integer.parseInt(valueFLD.getText()));
            } catch (NumberFormatException | BinaryIntOutOfRangeException e) {
                error += "\n- Variable Expected Value";
            }
        }

        if (error != "") {
            LSTrigger.customAlert("Only integers in the range 0-65535 are allowed.\nInvalid field(s):" + error);
            return;
        }

        if (triggerType == MapScreenLoadTrigger.VARIABLEVALUE) {
            if (!lsobsSet.add(new VariableValueTrigger(scriptID, variableID, varExpectedValue))) {
                LSTrigger.customAlert("The trigger you're trying to add already exists.\n" +
                        "Check the fields and try again.");
                return;
            }
        }
        else {
            if (!lsobsSet.add(new MapScreenLoadTrigger(triggerType, scriptID))) {
                LSTrigger.customAlert("The trigger you're trying to add already exists.\n" +
                        "Check the fields and try again.");
                return;
            }
        }


        idFLD.clear();
        valueFLD.clear();
        varFLD.clear();
    }

    @FXML
    void remove(ActionEvent event) {
        LSTrigger ls = list.getSelectionModel().getSelectedItem();
        lsobsSet.remove(ls);
    }


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        lsobsSet = FXCollections.observableSet();
        lsobsSet.addListener((SetChangeListener.Change<? extends LSTrigger> c) -> {
            if (c.wasAdded()) {
                list.getItems().add(c.getElementAdded());
            }
            if (c.wasRemoved()) {
                list.getItems().remove(c.getElementRemoved());
            }
        });

        newBTN.setOnMouseEntered(event -> {
            newBTN.setStyle(null);
        });
        newBTN.setOnMouseExited(event -> {
            newBTN.setStyle("-fx-background-color: transparent");
        });

        openBTN.setOnMouseEntered(event -> {
            openBTN.setStyle(null);
        });
        openBTN.setOnMouseExited(event -> {
            openBTN.setStyle("-fx-background-color: transparent");
        });

        saveBTN.setOnMouseEntered(event -> {
            saveBTN.setStyle(null);
        });
        saveBTN.setOnMouseExited(event -> {
            saveBTN.setStyle("-fx-background-color: transparent");
        });


        ObservableList olist = FXCollections.observableArrayList(lsobsSet);
        list.setItems(olist);

        varFLD.visibleProperty().bind(variableRBTN.selectedProperty());
        varTXT.visibleProperty().bind(variableRBTN.selectedProperty());

        valueFLD.visibleProperty().bind(variableRBTN.selectedProperty());
        valueTXT.visibleProperty().bind(variableRBTN.selectedProperty());

        ObservableBooleanValue a = variableRBTN.selectedProperty();
        ObservableBooleanValue b = idFLD.textProperty().isEmpty();
        ObservableBooleanValue c = Bindings.not(varFLD.textProperty().isEmpty());
        ObservableBooleanValue d = Bindings.not(valueFLD.textProperty().isEmpty());

        ObservableBooleanValue abcd = Bindings.and(a,Bindings.not(b)).and(c).and(d);
        ObservableBooleanValue aOrB = Bindings.or(a,b);

        SimpleListProperty<LSTrigger> slpTrigger = new SimpleListProperty<LSTrigger>(olist);

        addBTN.disableProperty().bind(Bindings.and(Bindings.not(abcd), aOrB));
        removeBTN.disableProperty().bind(slpTrigger.emptyProperty());
    }
}
