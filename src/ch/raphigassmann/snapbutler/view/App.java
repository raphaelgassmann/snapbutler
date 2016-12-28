package ch.raphigassmann.snapbutler.view;

import ch.raphigassmann.snapbutler.control.IManager;
import ch.raphigassmann.snapbutler.control.Manager;
import ch.raphigassmann.snapbutler.control.Snapshot;
import ch.raphigassmann.snapbutler.control.ValidatedVm;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Created by raphaelgassmann on 12.09.16.
 */
public class App extends Application {

    private IManager manager;
    private static VBox vBoxProgressPaneInner = new VBox(); //In other methods of this class needed.
    TableView<Snapshot> tableScanSystemForSnapshots = new TableView<Snapshot>(); //Method GetSnapshotsScanSystem needs knowledge of this
    TableView tableValidateVMs = new TableView<>(); //Method ValidateVm needs knowledge of this
    final ToggleGroup tglGrpRbOptions = new ToggleGroup(); //Method deleteSnapshot needs knowledge of this - Create Toggle Group - only one Radio Button as active

    //Snapshot creation option
    final ToggleGroup tglGrpRbCreateSnapshotOptionsActionOption = new ToggleGroup(); // Create Toggle Group - only one Raido Button per Group active
    //Checkbox Scan with User Information
    private CheckBox cbTakeSnapshotFromVirtualMemory = new CheckBox();
    private CheckBox cbQuiesceFilesystem = new CheckBox();

    private ObservableList<Snapshot> snapshots = FXCollections.observableArrayList();

    public App() {

    }

    public void startup(Stage primaryStage, Manager manager){
        System.out.println("App Scene is starting up.");
        this.manager = manager;

        try {
            start(primaryStage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setTitle("SnapButler");
        primaryStage.setResizable(false);

        int tabPaneMinWidth = 644;

        // =========== Components for Tab Snapshot Information ===========

        //Table View Snapshots
        tableScanSystemForSnapshots.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE); //make multiple selection in TableView possible

        //Initialize Columns
        TableColumn<Snapshot, String> colVmName = new TableColumn<>("VM Name");
        TableColumn<Snapshot, String> colSnapshotName = new TableColumn<>("Snapshot Name");
        TableColumn<Snapshot, String> colSnapshotDescription = new TableColumn<>("Description");
        TableColumn<Snapshot, String> colCreatedTime = new TableColumn<>("created Time");
        TableColumn<Snapshot, String> colCreatedBy = new TableColumn<>("created by");
        colVmName.setPrefWidth(142);
        colSnapshotName.setPrefWidth(150);
        colSnapshotDescription.setPrefWidth(80);
        colCreatedTime.setPrefWidth(106);
        colCreatedBy.setPrefWidth(156);


        //Initialize ValueFactory - name has to be the same as in Class: Snapshot
        colVmName.setCellValueFactory(new PropertyValueFactory<Snapshot, String>("vmName"));
        colSnapshotName.setCellValueFactory(new PropertyValueFactory<Snapshot, String>("snapshotName"));
        colSnapshotDescription.setCellValueFactory(new PropertyValueFactory<Snapshot, String>("description"));
        colCreatedTime.setCellValueFactory(new PropertyValueFactory<Snapshot, String>("createdTime"));
        colCreatedBy.setCellValueFactory(new PropertyValueFactory<Snapshot, String>("createdBy"));


        //TextField Filter
        TextField txtFilter = new TextField();
        txtFilter.setPrefWidth(180);
        txtFilter.setPromptText("Filter");
        txtFilter.setId("TextFieldFilter");
        txtFilter.textProperty().addListener(((observable, oldValue, newValue) -> {
            FilteredList<Snapshot> filteredListSnapshots = new FilteredList<>(snapshots, snapshot -> true);
            filteredListSnapshots.setPredicate(s -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                String lowerCaseFilter = newValue.toLowerCase(); //Comparator
                if (s.getVmName().toLowerCase().contains(lowerCaseFilter)) {return true;} //Filter matches VM Name
                if (s.getSnapshotName().toLowerCase().contains(lowerCaseFilter)) {return true;} //Filter matches Snapshot Name
                if (s.getCreatedBy().toLowerCase().contains(lowerCaseFilter)) {return true;} //Filter matches CreatedBy
                if (s.getCreatedTime().toLowerCase().contains(lowerCaseFilter)) {return true;} //Filter matches CreatedTime
                if (s.getDescription().toLowerCase().contains(lowerCaseFilter)) {return true;} //Filter matches Description
                return false; // Filter does not match for any item and column
            });

            SortedList<Snapshot> sortedData = new SortedList<Snapshot>(filteredListSnapshots);
            sortedData.comparatorProperty().bind(tableScanSystemForSnapshots.comparatorProperty());
            tableScanSystemForSnapshots.setItems(sortedData);
        }));

        //Checkbox Scan with User Information
        CheckBox cbScanWithoutUserInformation = new CheckBox();
        cbScanWithoutUserInformation.setText("without user-information");
        cbScanWithoutUserInformation.setId("CheckboxUserInformation");
        cbScanWithoutUserInformation.setSelected(false);

        //Button Scan System for Snapshots
        Button btnScanSystemForSnapshots;
        btnScanSystemForSnapshots = new Button();
        btnScanSystemForSnapshots.setText("Scan System for Snapshots");
        btnScanSystemForSnapshots.setPrefSize(260, 25);
        btnScanSystemForSnapshots.setOnAction(event -> ScanSystemForSnapshots(cbScanWithoutUserInformation.isSelected()));

        Separator separator = new Separator();
        separator.setOrientation(Orientation.VERTICAL);
        separator.setPadding(new Insets(0, 0, 0, 29));

        tableScanSystemForSnapshots.getColumns().addAll(colVmName, colSnapshotName, colSnapshotDescription, colCreatedTime, colCreatedBy);
        tableScanSystemForSnapshots.setMinWidth(tabPaneMinWidth - 8);

        //Options
        Label lblOptions = new Label();
        lblOptions.setText("Options");

        RadioButton rbDeleteMarkedSnapshotsSequential = new RadioButton();
        rbDeleteMarkedSnapshotsSequential.setToggleGroup(tglGrpRbOptions);
        rbDeleteMarkedSnapshotsSequential.setSelected(true); // Set as default option
        rbDeleteMarkedSnapshotsSequential.setText("Delete marked Snapshots sequential");

        RadioButton rbDeleteMarkedSnapshotsInGroup = new RadioButton();
        rbDeleteMarkedSnapshotsInGroup.setToggleGroup(tglGrpRbOptions);
        rbDeleteMarkedSnapshotsInGroup.setSelected(false);
        rbDeleteMarkedSnapshotsInGroup.setText("Delete marked Snapshots in Groups");

        //Slider for group size
        Label lblSliderGroupSize = new Label();
        lblSliderGroupSize.setText("group size: 5");
        Slider sliderDeleteMarkedSnapshotsInGroup = new Slider(2, 15, 5); //Create new Slider with values from 2 to 15 with default value 5
        sliderDeleteMarkedSnapshotsInGroup.setId("SliderDeleteMarkedSnapshotsInGroup");
        sliderDeleteMarkedSnapshotsInGroup.valueProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                lblSliderGroupSize.textProperty().setValue("group size: "+
                        String.valueOf((int) sliderDeleteMarkedSnapshotsInGroup.getValue()));
            }
        });

        RadioButton rbDeleteMarkedSnapshotsAllTogether = new RadioButton();
        rbDeleteMarkedSnapshotsAllTogether.setToggleGroup(tglGrpRbOptions);
        rbDeleteMarkedSnapshotsAllTogether.setSelected(false);
        rbDeleteMarkedSnapshotsAllTogether.setText("Delete marked Snapshots all together");

        //Button DeleteMarkedSnapshots
        Button btnDeleteMarkedSnapshots = new Button();
        btnDeleteMarkedSnapshots.setText("Delete marked Snapshots");
        btnDeleteMarkedSnapshots.setPrefSize(300, 25);
        btnDeleteMarkedSnapshots.setPrefWidth(tabPaneMinWidth - 8);
        btnDeleteMarkedSnapshots.setOnAction(event -> DeleteMarkedSnapshots((int) sliderDeleteMarkedSnapshotsInGroup.getValue(), cbScanWithoutUserInformation.isSelected()));

        //HBox for Option "Delete marked Snapshots in Group"
        HBox hBoxScanSystemForSnapshotsRow3OptionDeleteInGroup = new HBox();
        hBoxScanSystemForSnapshotsRow3OptionDeleteInGroup.getChildren().addAll(rbDeleteMarkedSnapshotsInGroup, sliderDeleteMarkedSnapshotsInGroup, lblSliderGroupSize);

        //HBox ScanSystemForSnapshotsRow1 - Buttton Scan and Filter
        HBox hBoxScanSystemForSnapshotsRow1 = new HBox(4); //VBox with Margin Size
        hBoxScanSystemForSnapshotsRow1.getStyleClass().add("TabHBoxRow");
        hBoxScanSystemForSnapshotsRow1.setId("hBoxScanSystemForSnapshotsRow1");
        hBoxScanSystemForSnapshotsRow1.setMinWidth(tabPaneMinWidth);
        hBoxScanSystemForSnapshotsRow1.getChildren().addAll(btnScanSystemForSnapshots, cbScanWithoutUserInformation, separator, txtFilter);

        //HBox ScanSystemForSnapshotsRow2 - TableView
        HBox hBoxScanSystemForSnapshotsRow2 = new HBox();
        hBoxScanSystemForSnapshotsRow2.getStyleClass().add("TabHBoxRow");
        hBoxScanSystemForSnapshotsRow2.getChildren().addAll(tableScanSystemForSnapshots);

        //VBox & HBox ScanSystemForSnapshotsRow3 Options
        VBox vBoxScanSystemForSnapshotsRow3 = new VBox(2); //VBox with Padding Size
        vBoxScanSystemForSnapshotsRow3.getChildren().addAll(lblOptions, rbDeleteMarkedSnapshotsSequential, rbDeleteMarkedSnapshotsAllTogether, hBoxScanSystemForSnapshotsRow3OptionDeleteInGroup);
        HBox hBoxScanSystemForSnapshotsRow3 = new HBox();
        hBoxScanSystemForSnapshotsRow3.getStyleClass().add("TabHBoxRow");
        hBoxScanSystemForSnapshotsRow3.getChildren().addAll(vBoxScanSystemForSnapshotsRow3);

        //HBox ScanSystemForSnapshotsRow4 Delete Action
        HBox hBoxScanSystemForSnapshotsRow4 = new HBox();
        hBoxScanSystemForSnapshotsRow4.getStyleClass().add("TabHBoxRow");
        hBoxScanSystemForSnapshotsRow4.getChildren().addAll(btnDeleteMarkedSnapshots);

        //Initialize Separators
        Separator separator1 = new Separator();
        Separator separator2 = new Separator();

        //Main VBox ScanSystemForSnapshots
        VBox vBoxScanSystemForSnapshots = new VBox(4); //VBox with Padding Size
        vBoxScanSystemForSnapshots.getChildren().addAll(hBoxScanSystemForSnapshotsRow1, hBoxScanSystemForSnapshotsRow2, separator1, hBoxScanSystemForSnapshotsRow3, separator2, hBoxScanSystemForSnapshotsRow4);

        // =========== Components for Tab Create Snapshot ===========
        TextArea taInsertVMs = new TextArea();
        taInsertVMs.setPromptText("Insert VM names (comma or semicolon separated)");
        taInsertVMs.setMaxHeight(80);

        Button btnValidateVMs = new Button();
        btnValidateVMs.setText("Validate VM's");
        btnValidateVMs.setPrefWidth(tabPaneMinWidth - 8);
        btnValidateVMs.setOnAction(event1 -> ValidateVMs(taInsertVMs.getText()));

        //Table View Snapshots
        tableValidateVMs.setEditable(true);
        //Initialize Columns
        TableColumn<ValidatedVm, String> colVmValidateVMs = new TableColumn("VM Name");
        TableColumn<ValidatedVm, String> colPowerState = new TableColumn("VM Power State");
        TableColumn<ValidatedVm, String> colNumberOfSnapshots = new TableColumn("Number of Snapshots");
        colVmValidateVMs.setPrefWidth(265);
        colPowerState.setPrefWidth(142);
        colNumberOfSnapshots.setPrefWidth(200);
        //Initialize ValueFactory - name has to be the same as in Class: ValidatedVm
        colVmValidateVMs.setCellValueFactory(new PropertyValueFactory<ValidatedVm, String>("vmName"));
        colPowerState.setCellValueFactory(new PropertyValueFactory<ValidatedVm, String>("powerState"));
        colNumberOfSnapshots.setCellValueFactory(new PropertyValueFactory<ValidatedVm, String>("snapshotsOnSystem"));

        tableValidateVMs.getColumns().addAll(colVmValidateVMs, colPowerState, colNumberOfSnapshots);
        tableValidateVMs.setMinWidth(tabPaneMinWidth - 8);
        tableValidateVMs.setMaxHeight(289);

        VBox vBoxCreateSnapshotRow1 = new VBox(4);
        vBoxCreateSnapshotRow1.getChildren().addAll(taInsertVMs, btnValidateVMs, tableValidateVMs);

        HBox hBoxCreateSnapshotRow1 = new HBox();
        hBoxCreateSnapshotRow1.getChildren().add(vBoxCreateSnapshotRow1);
        hBoxCreateSnapshotRow1.getStyleClass().add("TabHBoxRow");

        //Tab Create Snapshot Row2
        //Snapshot Information
        TextField txtSnapshotName = new TextField();
        txtSnapshotName.setPromptText("Snapshot Name");
        txtSnapshotName.setMaxWidth(300);

        TextArea taSnapshotDescription = new TextArea();
        taSnapshotDescription.setPromptText("Description");
        taSnapshotDescription.setMaxHeight(80);
        taSnapshotDescription.setMaxWidth(300);

        VBox vBoxCreateSnapshotRow2SnapshotInformation = new VBox(2);
        vBoxCreateSnapshotRow2SnapshotInformation.getChildren().addAll(txtSnapshotName, taSnapshotDescription);


        //Checkbox Take Snapshot from virtual Memory
        cbTakeSnapshotFromVirtualMemory.setText("Take Snapshot form virtual memory");
        cbTakeSnapshotFromVirtualMemory.setSelected(false);
        cbTakeSnapshotFromVirtualMemory.selectedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                if (cbQuiesceFilesystem.isSelected()==true) {
                    cbQuiesceFilesystem.setSelected(!newValue);
                }
            }
        });

        //Checkbox Quiesce filesystem
        cbQuiesceFilesystem.setText("Quiesce filesystem");
        cbQuiesceFilesystem.setSelected(true);
        cbQuiesceFilesystem.selectedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                if (cbTakeSnapshotFromVirtualMemory.isSelected() == true) {
                    cbTakeSnapshotFromVirtualMemory.setSelected(!newValue);
                }
            }
        });

        Separator separator5 = new Separator();

        RadioButton rbTakeSnapshotsSequentially = new RadioButton();
        rbTakeSnapshotsSequentially.setToggleGroup(tglGrpRbCreateSnapshotOptionsActionOption);
        rbTakeSnapshotsSequentially.setSelected(true);
        rbTakeSnapshotsSequentially.setText("Take Snapshots sequentially");

        RadioButton rbTakeAllSnapshotsTogether = new RadioButton();
        rbTakeAllSnapshotsTogether.setToggleGroup(tglGrpRbCreateSnapshotOptionsActionOption);
        rbTakeAllSnapshotsTogether.setSelected(true);
        rbTakeAllSnapshotsTogether.setText("Take all Snapshots together");
        final Tooltip ttRbTakeAllSnapshotsTogether = new Tooltip();
        ttRbTakeAllSnapshotsTogether.setText("Max group size allowed to create Snapshots togehter is 15.\nIf you choose this option with more than 15 VMs selected,\nthe Snapshots would be taken in groups of the size 15.");
        rbTakeAllSnapshotsTogether.setTooltip(ttRbTakeAllSnapshotsTogether);

        RadioButton rbTakeSnapshotsInGroup = new RadioButton();
        rbTakeSnapshotsInGroup.setToggleGroup(tglGrpRbCreateSnapshotOptionsActionOption);
        rbTakeSnapshotsInGroup.setSelected(true);
        rbTakeSnapshotsInGroup.setText("Take Snapshots in Group");

        //Slider + Label for Take Snapshots in Groups
        Label lblSliderGroupSizeTakeSnapshots = new Label();
        lblSliderGroupSizeTakeSnapshots.setText("group size: 5");
        Slider sliderTakeSnapshotsInGroup = new Slider(2, 15, 5);
        sliderTakeSnapshotsInGroup.setId("SliderTakeSnapshotsInGroup");
        sliderTakeSnapshotsInGroup.valueProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                lblSliderGroupSizeTakeSnapshots.textProperty().setValue("group size: " +
                        String.valueOf((int) sliderTakeSnapshotsInGroup.getValue()));
            }
        });

        HBox hBoxCreateSnapshotRow2TakeSnapshotsinGroup = new HBox();
        hBoxCreateSnapshotRow2TakeSnapshotsinGroup.getChildren().addAll(sliderTakeSnapshotsInGroup, lblSliderGroupSizeTakeSnapshots);

        VBox vBoxCreateSnapshotRow2SnapshotOption = new VBox(2);
        vBoxCreateSnapshotRow2SnapshotOption.getChildren().addAll(cbTakeSnapshotFromVirtualMemory, cbQuiesceFilesystem, separator5, rbTakeSnapshotsSequentially, rbTakeAllSnapshotsTogether, rbTakeSnapshotsInGroup, hBoxCreateSnapshotRow2TakeSnapshotsinGroup);

        HBox hBoxCreateSnapshotRow2 = new HBox(4);
        hBoxCreateSnapshotRow2.getChildren().addAll(vBoxCreateSnapshotRow2SnapshotInformation, vBoxCreateSnapshotRow2SnapshotOption);
        hBoxCreateSnapshotRow2.getStyleClass().add("TabHBoxRow");

        //Tab Create Snapshot Row3
        Button btnCreateSnapshots = new Button();
        btnCreateSnapshots.setText("Create Snapshots");
        btnCreateSnapshots.setPrefWidth(tabPaneMinWidth - 8);
        btnCreateSnapshots.setOnAction(event -> CreateSnapshots(txtSnapshotName.getText(), taSnapshotDescription.getText(), sliderTakeSnapshotsInGroup.getValue()));

        HBox hBoxCreateSnapshotRow3 = new HBox(2);
        hBoxCreateSnapshotRow3.getChildren().addAll(btnCreateSnapshots);
        hBoxCreateSnapshotRow3.getStyleClass().add("TabHBoxRow");

        //Main VBox CreateSnapshot
        VBox vBoxCreateSnapshot = new VBox(4);
        vBoxCreateSnapshot.getChildren().addAll(hBoxCreateSnapshotRow1, hBoxCreateSnapshotRow2, hBoxCreateSnapshotRow3);

        // =========== FlowPane for Main Tabs ===========
        FlowPane fpTabSnapshotInformation = new FlowPane();
        fpTabSnapshotInformation.getChildren().add(vBoxScanSystemForSnapshots);
        FlowPane fpTabCreateSnapshot = new FlowPane();
        fpTabCreateSnapshot.getChildren().add(vBoxCreateSnapshot);

        // =========== TabPane ===========
        TabPane tabPane = new TabPane();
        //Tab Snapshot Information
        Tab tabSnapshotInformation = new Tab();
        tabSnapshotInformation.setText("Snapshot Information");
        tabSnapshotInformation.setClosable(false);
        tabSnapshotInformation.setContent(fpTabSnapshotInformation);

        //Tab Create Snapshot
        Tab tabCreateSnapshot = new Tab();
        tabCreateSnapshot.setText("Create Snapshot");
        tabCreateSnapshot.setClosable(false);
        tabCreateSnapshot.setContent(fpTabCreateSnapshot);

        tabPane.setPrefWidth(tabPaneMinWidth); //less than is available (Puffer)
        //tabPane.setMaxWidth(Double.MAX_VALUE);
        tabPane.getTabs().addAll(tabSnapshotInformation, tabCreateSnapshot);

        // =========== Progress Pane ===========
        // Components
        Label lblTasks = new Label();
        lblTasks.setText("recent tasks:");
        lblTasks.getStyleClass().add("TaskMainTitle");

        Separator separator3 = new Separator();

        VBox vBoxProgressPane = new VBox();
        vBoxProgressPane.getChildren().addAll(lblTasks, separator3, vBoxProgressPaneInner);

        FlowPane fpProgressPane = new FlowPane();
        fpProgressPane.setPrefWidth(250);
        fpProgressPane.setId("FlowPaneProgressPane");
        fpProgressPane.getChildren().add(vBoxProgressPane);

        //ScrollPane Progress
        ScrollPane spvBoxProgressPane = new ScrollPane(fpProgressPane);
        spvBoxProgressPane.setFitToHeight(true);
        spvBoxProgressPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        spvBoxProgressPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        spvBoxProgressPane.setContent(fpProgressPane);
        spvBoxProgressPane.setPrefViewportHeight(600);

        // =========== FlowPane for TabPane and Progress Pane (with HBox and Separators) ===========
        Separator separator4 = new Separator();
        separator4.setOrientation(Orientation.VERTICAL);
        HBox hBoxFlowTabPaneProgressPane = new HBox();
        hBoxFlowTabPaneProgressPane.getChildren().addAll(tabPane, separator4, spvBoxProgressPane);

        FlowPane fpTabPaneProgressPane = new FlowPane();
        fpTabPaneProgressPane.getChildren().addAll(hBoxFlowTabPaneProgressPane);

        StackPane layout = new StackPane();
        layout.getChildren().add(fpTabPaneProgressPane);
        layout.setId("AppLayout"); // Set ID for CSS Stylesheet

        Scene scene = new Scene(layout, 900, 608);
        String css =this.getClass().getResource("../view/App.css").toExternalForm();
        scene.getStylesheets().add(css); // Add CSS to JavaFX Class

        primaryStage.setScene(scene);
        primaryStage.setResizable(true);
        primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("../view/ico.png")));
        primaryStage.show();

        //Setting min width and height to its actual size
        primaryStage.setMinWidth(primaryStage.getWidth());
        primaryStage.setMinHeight(primaryStage.getHeight());
    }



    /**
     * Add's Task under recent Tasks
     * @param taskText
     * @param progress
     * @return
     */
    public static String AddTask(String taskText, Double progress) {
        long longTaskId = System.currentTimeMillis(); //Define TaskID
        String taskId = ""+longTaskId;

        Platform.runLater(() -> {
            Label lblTaskEntry = new Label();
            lblTaskEntry.setText(taskText);
            lblTaskEntry.getStyleClass().add("TaskEntryLabel");
            lblTaskEntry.setId(taskId + "Label");

            Separator separatorTasks = new Separator();
            separatorTasks.setPrefWidth(230);

            ProgressIndicator piTaskEntry = new ProgressIndicator();
            piTaskEntry.getStyleClass().add("TaskEntryProgressIndicator");
            piTaskEntry.setId(taskId + "ProgressIndicator");

            if (progress != 0) {
                piTaskEntry.setProgress(progress);
                piTaskEntry.setMaxSize(55, 55);//set maxsize of Progress Indicator if he has progress > 0 and Determinate
            } else {
                //piTaskEntry.setMaxSize(55, 55);//set maxsize for Progress Indicator if he has state of Indeterminate
                piTaskEntry.setMaxSize(35, 35);//set maxsize for Progress Indicator if he has state of Indeterminate
            }

            HBox hBoxProgressPaneInnerTaskEntry = new HBox();
            hBoxProgressPaneInnerTaskEntry.setAlignment(Pos.CENTER_LEFT);
            hBoxProgressPaneInnerTaskEntry.setId(taskId + "Hbox");
            hBoxProgressPaneInnerTaskEntry.getChildren().addAll(piTaskEntry, lblTaskEntry);
            if(taskText=="System Scan has started!"||taskText=="Validate VM's!"){hBoxProgressPaneInnerTaskEntry.setPrefHeight(55);}
            vBoxProgressPaneInner.getChildren().add(0, separatorTasks);
            vBoxProgressPaneInner.getChildren().add(1, hBoxProgressPaneInnerTaskEntry);
        });
        return taskId;
    }

    /**
     * Update an existing Task's Message and Progress
     * @param taskId
     * @param taskText
     * @param progress
     */
    public static void UpdateTask(String taskId, String taskText, Double progress){
        Platform.runLater(() -> {
            Node nodeLookupHbox = vBoxProgressPaneInner.lookup("#" + taskId + "Hbox");
            HBox hBoxProgressPaneInnerTaskEntry = (HBox) nodeLookupHbox;

            //do not update label taskText if it is empty or not set
            if (taskText != null || taskText != "") {
                Node nodeLookupLabel = hBoxProgressPaneInnerTaskEntry.lookup("#" + taskId + "Label");
                Label lblTaskOnUpdate = (Label) nodeLookupLabel;
                lblTaskOnUpdate.setText(taskText);
            }
            Node nodeLookupProgressIndicator = hBoxProgressPaneInnerTaskEntry.lookup("#" + taskId + "ProgressIndicator");
            ProgressIndicator piTaskEntryOnUpdate = (ProgressIndicator) nodeLookupProgressIndicator;
            piTaskEntryOnUpdate.setProgress(progress);
            piTaskEntryOnUpdate.setMaxSize(55, 55);
        });
    }

    public void ScanSystemForSnapshots(Boolean scanForCreator) {
        new Thread(() -> {
            double startScanTime = System.currentTimeMillis();
            //Preparing Event
            String startupTaskText = "System Scan has started!";
            double startUpProgress = 0;
            String taskID = App.AddTask(startupTaskText, startUpProgress);

            String [][] snapshotArray = manager.getSystemSnapshotsScanSystem(scanForCreator, taskID); //Get snapshots from Manager in Array
            tableScanSystemForSnapshots.setItems(getSnapshot(snapshotArray)); //push values to observableList
            snapshots = tableScanSystemForSnapshots.getItems();
            double endScanTime = System.currentTimeMillis();
            String updateStartupTaskText = "System Scan finished in "+ ((endScanTime-startScanTime)/1000) + " Seconds";
            startUpProgress = 1;
            UpdateTask(taskID, updateStartupTaskText, startUpProgress);
        }).start();
    }

    public ObservableList<Snapshot> getSnapshot(String [][] snapshotArray){
        ObservableList<Snapshot> snapshots = FXCollections.observableArrayList();
        snapshots.clear();
        for (int i = 0; i< snapshotArray.length; i++) {
            if (snapshotArray[i][0] != null){ //Check if this row in array is empty
                snapshots.add(new Snapshot(snapshotArray[i][1], snapshotArray[i][2], snapshotArray[i][3], snapshotArray[i][4], snapshotArray[i][5]/*snapshotArray[i][5]*/)); //Create object Snapshot - vmName, snapshotName, description, createdTime, createdBy
            }
        }
        return snapshots;
    }

    private void DeleteMarkedSnapshots(int groupSize, Boolean scanForCreator) {
        Set<Snapshot> selection = new HashSet<Snapshot>(tableScanSystemForSnapshots.getSelectionModel().getSelectedItems()); //Get selected items from tableview and fill it to HashSet
        String confirmationText = "VM Name\t\t\t\tSnapshot Name\n";
        for(Snapshot s:selection){
            System.out.println(s.getSnapshotName()); //Loop through and get Snapshot Names
            confirmationText = confirmationText + s.getVmName() +"\t\t\t\t" + s.getSnapshotName() + "\n";
        }

        //Question - Ask for Delete
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation of Deletion");
        alert.setHeaderText("Please confirm to delete following Snapshots:");
        alert.setContentText(confirmationText);
        Optional<ButtonType> answerConformation  = alert.showAndWait();
        if(answerConformation.get() == ButtonType.CANCEL){return;}

        //Preparing Event
        double startScanTime = System.currentTimeMillis();
        String startupTaskText = "Deleting selected snapshots";
        double startUpProgress = 0;
        String taskID = App.AddTask(startupTaskText, startUpProgress);

        Thread worker = new Thread(new Runnable() {
            @Override
            public void run() {
                RadioButton rbDeleteOption = (RadioButton) tglGrpRbOptions.getSelectedToggle();
                manager.deleteSnapshots(selection, rbDeleteOption.getText(), groupSize, taskID);
            }
        });
        worker.start();

        //Question - Ask for reload list --> if yes, start new thread and join thread for deletion
        Alert alert2 = new Alert(Alert.AlertType.CONFIRMATION);
        alert2.setTitle("Reload Snapshotlist after Deletion");
        alert2.setHeaderText("Rescan snapshotlist?");
        alert2.setContentText("Do you wish to reload the list of snapshots in your enviroment after deletion is done?\nAction: Scan System for Snapshots");
        Optional<ButtonType> answerConformation2  = alert2.showAndWait();
        if(answerConformation2.get() == ButtonType.CANCEL){return;}
        if(answerConformation2.get() == ButtonType.OK){
            Thread worker2 = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        worker.join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    try{
                        Thread.sleep(2000);
                    } catch (InterruptedException ex){Thread.currentThread().interrupt();}
                    ScanSystemForSnapshots(scanForCreator);
                }
            });
            worker2.start();
        }
    }

    private void ValidateVMs(String vmToValidate) {
        System.out.println("Button ValidateVMs pressed!");

        if (vmToValidate.isEmpty()){
            System.out.println("is empty");
            //Alert
            //Question - Ask for Delete
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("No VMs to validate!");
            alert.setHeaderText("No VMs to Validate!");
            alert.setContentText("Please provide some input text with VM names to validate.");
            alert.showAndWait();
            return;
        }else{

            int SemicolonIndexVmToValidate = vmToValidate.indexOf(';');
            int CommaIndexVmToValidate = vmToValidate.indexOf(',');
            //Check if input text has two delimiters
            if (SemicolonIndexVmToValidate>=0&&CommaIndexVmToValidate>=0 || SemicolonIndexVmToValidate==0 || CommaIndexVmToValidate == 0){
                //Alert
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Input Error");
                alert.setHeaderText("Inserted text could not be used for search!");
                alert.setContentText("Please check input VM-Names. Two delimeters are included or first character contains a delimeter.");
                alert.showAndWait();
                return;
            }

            new Thread(() -> {
                double startScanTime = System.currentTimeMillis();
                //Preparing Event
                String startupTaskText = "Validate VM's!";
                double startUpProgress = 0;
                String taskID = App.AddTask(startupTaskText, startUpProgress);
                //manager.validateVMs(vmToValidate, taskID);
                String [][] validatedVMs = manager.validateVMs(vmToValidate, taskID); //Get snapshots from Manager in Array
                tableValidateVMs.setItems(getValidatedVm(validatedVMs)); //push values to observableList
                double endScanTime = System.currentTimeMillis();
                String updateStartupTaskText = "Validate VM's finished in "+ ((endScanTime-startScanTime)/1000) + " Seconds";
                startUpProgress = 1;
                UpdateTask(taskID, updateStartupTaskText, startUpProgress);
            }).start();

        }
    }

    public ObservableList<ValidatedVm> getValidatedVm(String[][] validatedVmArray){
        ObservableList<ValidatedVm> validatedVms = FXCollections.observableArrayList();
        validatedVms.clear();
        for (int i = 0; i < validatedVmArray.length; i++) {
            validatedVms.add(new ValidatedVm(validatedVmArray[i][0],validatedVmArray[i][1],validatedVmArray[i][2]));//Create object ValidatedVm - vmName, powerState, numberOfSnapshotsOnSystem
        }
        return validatedVms;
    }

    private void CreateSnapshots(String snapshotName, String snapshotDescription, double doubleGroupSize) {
        int groupSize = (int) doubleGroupSize;
        System.out.println("CreateSnapshotsButtonPressed");
        Set<ValidatedVm> validatedVms = new HashSet<ValidatedVm>(tableValidateVMs.getItems()); //Get all items of tableview and fill to HashSet
        String confirmationText = "VM Name\n";
        for(ValidatedVm s:validatedVms){
            if(s.getVmName()!=null){
            System.out.println(s.getVmName()); //Loop through and get VM-Names
            confirmationText = confirmationText + s.getVmName() + "\n";
            }
        }
        //Question - Ask for Creation
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation of Creation");
        alert.setHeaderText("Please confirm to create snapshots of following systems:");
        alert.setContentText(confirmationText);
        Optional<ButtonType> answerConformation  = alert.showAndWait();
        if(answerConformation.get() == ButtonType.CANCEL){return;}

        //Preparing Event
        double startTime = System.currentTimeMillis();
        String startupTaskText = "Creating Snapshots";
        double startUpProgress = 0;
        String taskID = App.AddTask(startupTaskText, startUpProgress);

        new Thread(() -> {
            RadioButton rbActionOption = (RadioButton) tglGrpRbCreateSnapshotOptionsActionOption.getSelectedToggle();
            manager.createSnapshots(validatedVms, snapshotName, snapshotDescription, cbTakeSnapshotFromVirtualMemory.isSelected(),cbQuiesceFilesystem.isSelected(), rbActionOption.getText(), groupSize, taskID);
        }).start();

        double endScanTime = System.currentTimeMillis();
        //String updateStartupTaskText = "Snapshots created.";
        //startUpProgress = 1;
        //UpdateTask(taskID, "", 1);
    }
}
