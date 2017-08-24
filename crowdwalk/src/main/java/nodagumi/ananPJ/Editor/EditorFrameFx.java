package nodagumi.ananPJ.Editor;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.lang.Thread;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Optional;
import java.util.regex.Matcher;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.RadioButton;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.Separator;
import javafx.scene.control.Spinner;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import com.sun.javafx.scene.control.behavior.ButtonBehavior;
import com.sun.javafx.scene.control.behavior.KeyBinding;

import nodagumi.ananPJ.CrowdWalkLauncher;
import nodagumi.ananPJ.Editor.EditCommand.*;
import nodagumi.ananPJ.Gui.MapViewFrame;
import nodagumi.ananPJ.GuiSimulationLauncher;
import nodagumi.ananPJ.Editor.MapEditor.TextPosition;
import nodagumi.ananPJ.Editor.Panel.AreaPanelFx;
import nodagumi.ananPJ.Editor.Panel.GroupPanel;
import nodagumi.ananPJ.Editor.Panel.LinkPanelFx;
import nodagumi.ananPJ.Editor.Panel.NodePanelFx;
import nodagumi.ananPJ.Editor.Panel.ScenarioPanelFx;
import nodagumi.ananPJ.Editor.Panel.TagSetupPane;
import nodagumi.ananPJ.NetworkMap.Area.MapArea;
import nodagumi.ananPJ.NetworkMap.Link.MapLink;
import nodagumi.ananPJ.NetworkMap.Link.MapLinkTable;
import nodagumi.ananPJ.NetworkMap.MapPartGroup;
import nodagumi.ananPJ.NetworkMap.Node.MapNode;
import nodagumi.ananPJ.NetworkMap.Node.MapNodeTable;
import nodagumi.ananPJ.NetworkMap.NetworkMap;
import nodagumi.ananPJ.NetworkMap.OBNode;
import nodagumi.ananPJ.misc.CrowdWalkPropertiesHandler;
import nodagumi.ananPJ.misc.MapChecker;
import nodagumi.ananPJ.misc.SetupFileInfo;
import nodagumi.ananPJ.Settings;
import nodagumi.ananPJ.Simulator.Obstructer.ObstructerBase;

/**
 * マップエディタのウィンドウ構築と GUI コントロール
 */
public class EditorFrameFx {
    // Enter キーを押下した時にフォーカスが当たっているボタンが押される様にする(デフォルトではそうなっていないため)
    public class EnableButtonEnterKey extends ButtonBehavior<Button> {
        public EnableButtonEnterKey() {
            super(new Button());
            BUTTON_BINDINGS.add(new KeyBinding(KeyCode.ENTER, KeyEvent.KEY_PRESSED, "Press"));
            BUTTON_BINDINGS.add(new KeyBinding(KeyCode.ENTER, KeyEvent.KEY_RELEASED, "Release"));
        }
    }

    /**
     * ヘルプ表示用コンテンツのアドレス
     * TODO: 定義ファイル化する
     */
    public static final String QUICK_REFERENCE = "/quick_reference.html";
    public static final String PROPERTIES_PATH = "./doc/javadoc/nodagumi/ananPJ/misc/CrowdWalkPropertiesHandler.html";
    public static final String TUTORIAL_PATH = "./doc/manual.html";
    public static final String ZONE_REFERENCE_URI = "http://www.gsi.go.jp/sokuchikijun/jpc.html";
    public static final String GITHUB_REPOSITORY_URI = "https://github.com/crest-cassia/CrowdWalk";

    /**
     * 編集モード
     */
    public static enum EditorMode {
        ADD_NODE, ADD_LINK, ADD_NODE_LINK, ADD_AREA, EDIT_NODE, EDIT_LINK, EDIT_AREA, BACKGROUND_IMAGE
    };
    private EditorMode mode = EditorMode.EDIT_NODE;

    /**
     * マップエディタ
     */
    private MapEditor editor;

    /**
     * ウィンドウフレーム
     */
    private Stage frame;

    /**
     * マップエディタのキャンバス
     */
    private EditorCanvas canvas;

    /**
     * 編集エリアとタブエリアの仕切り
     */
    private SplitPane splitPane;

    /**
     * GUI の設定情報
     */
    private Settings settings;

    /**
     * 属性を扱うハンドラ
     */
    private CrowdWalkPropertiesHandler properties = null;

    /**
     * 設定ファイルの取りまとめ。
     */
    private SetupFileInfo setupFileInfo = new SetupFileInfo();

    /**
     * コマンドラインで指定された fallback 設定
     */
    private ArrayList<String> commandLineFallbacks = null;

    /**
     * 背景グループ表示メニュー
     */
    private Menu menuShowBackgroundGroup = new Menu("Show background group");

    /**
     * グループ選択パネル
     */
    private FlowPane groupSelectionPane = new FlowPane();
    private ToggleGroup groupToggleGroup = new ToggleGroup();
    private HashMap<MapPartGroup, ToggleButton> groupButtonMap = new HashMap();

    /**
     * ステータスバー
     */
    private HBox statusPane = new HBox();
    private HBox linkAttributesPane;
    private TextField linkLengthField = new TextField("0.0");
    private Label linkScaleLabel = new Label("(scale: 1.0)");
    private TextField linkWidthField = new TextField("1.0");
    private Label statusLabel = new Label("Unedited");

    /**
     * コンテキスト・メニュー
     */
    private ContextMenu editNodeMenu = new ContextMenu();
    private ContextMenu editLinkMenu = new ContextMenu();
    private ContextMenu editAreaMenu = new ContextMenu();
    private ContextMenu bgImageMenu = new ContextMenu();
    private Menu menuAddSymbolicLinkOfNode = new Menu("Add symbolic link");
    private Menu menuAddSymbolicLinkOfLink = new Menu("Add symbolic link");
    private MenuItem miSetBackgroundImage;
    private MenuItem miSetBackgroundImageAttributes;
    private MenuItem miRemoveBackgroundImage;

    /**
     * タブパネル
     */
    private TabPane tabPane = new TabPane();
    private GroupPanel groupPanel;
    private NodePanelFx nodePanel;
    private LinkPanelFx linkPanel;
    private AreaPanelFx areaPanel;
    private ScenarioPanelFx scenarioPanel;

    /**
     * ヘルプ表示用
     */
    private Stage helpStage = new Stage();
    private WebView webView = new WebView();
    private double helpZoom = 1.0;

    /**
     * コンストラクタ
     */
    public EditorFrameFx() {}

    /**
     * コンストラクタ
     */
    public EditorFrameFx(MapEditor editor, String title, CrowdWalkPropertiesHandler properties, Settings settings) {
        frame = new Stage();
        frame.setTitle(title);
        this.editor = editor;
        this.properties = properties;
        this.settings = settings;
        new EnableButtonEnterKey();

        int x = settings.get("_editorPositionX", 0);
        int y = settings.get("_editorPositionY", 0);
        int width = settings.get("_editorWidth", 960);
        int height = settings.get("_editorHeight", 720);
        frame.setX(x);
        frame.setY(y);
        frame.setWidth(width);
        frame.setHeight(height);

        init();

        // メニューの構築

        Node menuBar = createMainMenu();
        updateShowBackgroundGroupMenu();
        createContextMenus();
        updateAddSymbolicLinkMenu();

        // editor canvas の構築

        Group root = new Group();
        canvas = new EditorCanvas(editor, this);
        root.getChildren().add(canvas);
        StackPane canvasPane = new StackPane();
        String image = getClass().getResource("/img/canvas_bg.png").toExternalForm();
        canvasPane.setStyle(
            "-fx-background-image: url('" + image + "'); " +
            "-fx-background-position: center center; " +
            "-fx-background-repeat: repeat;"
        );
        canvasPane.getChildren().add(root);
        canvasPane.setMinWidth(800);
        canvasPane.setMinHeight(600);
        canvas.widthProperty().bind(canvasPane.widthProperty());
        canvas.heightProperty().bind(canvasPane.heightProperty());

        // 左側ペインの構築

        BorderPane leftPane = new BorderPane();
        groupSelectionPane.getStyleClass().add("custom-color-pane");
        groupSelectionPane.setPadding(new Insets(4));
        groupSelectionPane.setHgap(8);
        groupSelectionPane.setAlignment(Pos.CENTER);
        leftPane.setCenter(canvasPane);     // これを先にセットしないと rotation した時に他の Pane に被ってしまう
        leftPane.setTop(updateGroupSelectionPane());
        leftPane.setBottom(createModeSelectionPane());

        // タブパネルの構築

        groupPanel = new GroupPanel(editor);
        Tab groupsTab = new Tab("Groups");
        groupsTab.setContent(groupPanel);
        groupsTab.setClosable(false);

        nodePanel = new NodePanelFx(editor, this);
        Tab nodesTab = new Tab("Nodes");
        nodesTab.setContent(nodePanel);
        nodesTab.setClosable(false);

        linkPanel = new LinkPanelFx(editor, this);
        Tab linksTab = new Tab("Links");
        linksTab.setContent(linkPanel);
        linksTab.setClosable(false);

        areaPanel = new AreaPanelFx(editor, this);
        Tab areasTab = new Tab("Areas");
        areasTab.setContent(areaPanel);
        areasTab.setClosable(false);

        scenarioPanel = new ScenarioPanelFx(editor, this);
        Tab scenarioTab = new Tab("Scenario");
        scenarioTab.setContent(scenarioPanel);
        scenarioTab.setClosable(false);

        tabPane.getTabs().addAll(groupsTab, nodesTab, linksTab, areasTab, scenarioTab);

        // 下側ペインの構築

        BorderPane.setAlignment(statusLabel, Pos.CENTER_LEFT);

        statusPane.setAlignment(Pos.CENTER_LEFT);
        statusPane.setSpacing(8);
        linkAttributesPane = createLinkAttributesPane();
        statusPane.getChildren().addAll(statusLabel);

        Button simulate2dButton = new Button("2D Simulate");
        Button simulate3dButton = new Button("3D Simulate");
        simulate2dButton.setOnAction(e -> simulate("GuiSimulationLauncher2D", simulate2dButton, simulate3dButton));
        simulate3dButton.setOnAction(e -> simulate("GuiSimulationLauncher3D", simulate2dButton, simulate3dButton));

        FlowPane simulationButtonPane = new FlowPane();
        simulationButtonPane.setHgap(8);
        simulationButtonPane.setMaxWidth(230);
        simulationButtonPane.setAlignment(Pos.CENTER);
        simulationButtonPane.getChildren().addAll(simulate2dButton, simulate3dButton);

        BorderPane bottomPane = new BorderPane();
        bottomPane.setPadding(new Insets(4));
        bottomPane.setCenter(statusPane);
        bottomPane.setRight(simulationButtonPane);

        // スプリットペインの構築
        splitPane = new SplitPane();
        splitPane.getItems().addAll(leftPane, tabPane);
        splitPane.getDividers().get(0).positionProperty().addListener((obs, oldVal, newVal) -> {
            double tabWidth = splitPane.getWidth() * (1.0 - newVal.doubleValue());
            areaPanel.widthChanged(tabWidth);
            scenarioPanel.widthChanged(tabWidth);
        });
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                String pos = settings.get("dividerPosition", "");
                if (! pos.isEmpty()) {
                    splitPane.setDividerPositions(Double.valueOf(pos));
                }
            }
        });

        BorderPane borderPane = new BorderPane();
        borderPane.setTop(menuBar);
        borderPane.setCenter(splitPane);
        borderPane.setBottom(bottomPane);

        Scene scene = new Scene(borderPane);
        scene.getStylesheets().add("stylesheet.css");
        frame.setScene(scene);

        // ウィンドウイベントのハンドリング
        frame.setOnShown(e -> {
            try {
                boolean simulationWindowOpen = properties.getBoolean("simulation_window_open", false);
                boolean autoSimulationStart = properties.getBoolean("auto_simulation_start", false);
                if (simulationWindowOpen || autoSimulationStart) {
                    if (editor.getNetworkMapFile() == null || editor.getGenerationFile() == null || editor.getScenarioFile() == null) {
                        // プロパティファイルの設定が足りないためシミュレーションを開始することが出来ません
                        Alert alert = new Alert(AlertType.WARNING, "The simulation can not be started because the property file setting is insufficient.", ButtonType.OK);
                        alert.showAndWait();
                        return;
                    }
                    if (CrowdWalkLauncher.use2dSimulator) {
                        simulate2dButton.fire();
                    } else {
                        simulate3dButton.fire();
                    }
                }
            } catch(Exception ex) {
                System.err.println(ex.getMessage());
                System.exit(1);
            }
            notice();
        });
        frame.setOnCloseRequest(e -> closing(e));

        // TODO: 旧エディタを廃止したら setImplicitExit(false) と以下を削除する
        frame.setOnHidden(e -> {
            notice();
            System.exit(0);
        });
    }

    /**
     * お知らせ表示
     */
    private void notice() {
        System.err.println("\n【お知らせ】");
        System.err.println("・GitHub の CrowdWalk ページ(Help > Browse GitHub repository で表示されます)の Issues にある「新マップエディタの問題点と追加予定機能」を一読してください。");
        System.err.println("・何か不具合が生じたら Issues に書き込んで報告してください。");
        System.err.println("・追加して欲しい機能等があれば Issues に書き込んでください。");
        System.err.println("・コマンドラインで -e オプションを使うと旧バージョンのエディタを起動することが出来ます。新エディタで問題が生じる場合にはこちらを使用してください。\n");
    }

    /**
     * 初期設定
     */
    private void init() {
        // ヘルプ画面の準備

        webView.setOnKeyPressed(event -> {
            if (event.isControlDown()) {
                if (event.getCode() == KeyCode.W) {
                    helpStage.close();
                } else if (event.getCode() == KeyCode.PLUS || event.getCode() == KeyCode.UP) {
                    helpZoom += 0.1;
                    webView.setZoom(helpZoom);
                } else if (event.getCode() == KeyCode.MINUS || event.getCode() == KeyCode.DOWN) {
                    helpZoom -= 0.1;
                    webView.setZoom(helpZoom);
                } else if (event.getCode() == KeyCode.DIGIT0) {
                    helpZoom = 1.0;
                    webView.setZoom(helpZoom);
                }
            }
        });

        Button okButton = new Button("  OK  ");
        okButton.setOnAction(event -> helpStage.close());
        BorderPane buttonPane = new BorderPane();
        buttonPane.setPadding(new Insets(4, 8, 4, 8));
        buttonPane.setRight(okButton);

        BorderPane borderPane = new BorderPane();
        borderPane.setCenter(webView);
        borderPane.setBottom(buttonPane);

        helpStage.setScene(new Scene(borderPane));
    }

    /**
     * 終了処理
     */
    public boolean closing(WindowEvent event) {
        if (editor.isModified()) {
            Alert alert = new Alert(AlertType.CONFIRMATION, "Warning:\n    Map data has been modified.\n    Do you want to quit anyway?", ButtonType.YES, ButtonType.NO);
            Optional<ButtonType> result = alert.showAndWait();
            if (! result.isPresent() || result.get() == ButtonType.NO) {
                if (event != null) {
                    event.consume();
                }
                return false;
            }
        }
        settings.put("_editorPositionX", (int)frame.getX());
        settings.put("_editorPositionY", (int)frame.getY());
        settings.put("_editorWidth", (int)frame.getWidth());
        settings.put("_editorHeight", (int)frame.getHeight());
        settings.put("dividerPosition", "" + splitPane.getDividerPositions()[0]);
        return true;
    }

    /**
     * メインメニューの生成
     */
    private Node createMainMenu() {
        MenuBar menuBar = new MenuBar();

        /* File menu */

        Menu fileMenu = new Menu("File");

        MenuItem miNew = new MenuItem("New");
        miNew.setOnAction(e -> clearMapData());
        miNew.setAccelerator(KeyCombination.valueOf("Ctrl+N"));

        MenuItem miOpenMap = new MenuItem("Open map");
        miOpenMap.setOnAction(e -> openMap());
        miOpenMap.setAccelerator(KeyCombination.valueOf("Ctrl+O"));

        MenuItem miSaveMap = new MenuItem("Save map");
        miSaveMap.setOnAction(e -> saveMap());
        miSaveMap.setAccelerator(KeyCombination.valueOf("Ctrl+S"));

        MenuItem miSaveMapAs = new MenuItem("Save map as");
        miSaveMapAs.setOnAction(e -> saveMapAs());

        // TODO:
        // MenuItem miOpenProperty = new MenuItem("Open property");
        // miOpenProperty.setOnAction(e -> {
        //     System.err.println("Open property: under construction");
        // });
        //
        // MenuItem miSaveProperty = new MenuItem("Save property");
        // miSaveProperty.setOnAction(e -> {
        //     System.err.println("Save property: under construction");
        // });

        MenuItem miQuit = new MenuItem("Quit");
        miQuit.setOnAction(e -> {
            if (closing(null)) {
                frame.close();
            }
        });
        miQuit.setAccelerator(KeyCombination.valueOf("Ctrl+Q"));

        fileMenu.getItems().addAll(miNew, miOpenMap, miSaveMap, miSaveMapAs, /* miOpenProperty, miSaveProperty, */ miQuit);

        /* Edit menu */

        Menu editMenu = new Menu("Edit");

        MenuItem miUndo = new MenuItem("Undo");
        miUndo.setOnAction(e -> editor.undo());
        miUndo.setAccelerator(KeyCombination.valueOf("Ctrl+Z"));

        MenuItem miRedo = new MenuItem("Redo");
        miRedo.setOnAction(e -> editor.redo());
        miRedo.setAccelerator(KeyCombination.valueOf("Ctrl+Y"));

        editMenu.getItems().addAll(miUndo, miRedo);

        /* View menu */

        Menu viewMenu = new Menu("View");

        MenuItem miShow3d = new MenuItem("Show 3D");
        miShow3d.setOnAction(e -> {
            MapViewFrame mapViewer = new MapViewFrame("3D preview of Structure", 800, 600, editor.getMap(), properties);
            mapViewer.show();
        });

        MenuItem miCentering = new MenuItem("Centering");
        miCentering.setOnAction(e -> {
            canvas.centering(false);
            canvas.repaintLater();
        });

        MenuItem miCenteringWithScaling = new MenuItem("Centering with scaling");
        miCenteringWithScaling.setOnAction(e -> {
            canvas.centering(true);
            canvas.repaintLater();
        });

        MenuItem miToTheOrigin = new MenuItem("To the origin");
        miToTheOrigin.setOnAction(e -> {
            canvas.setTranslate(0.0, 0.0);
            canvas.repaintLater();
        });

        MenuItem miSetRotation = new MenuItem("Set rotation");
        miSetRotation.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog("" + 0.0);
            dialog.setTitle("Set rotation");
            dialog.setHeaderText("Enter the rotation angle");
            String text = dialog.showAndWait().orElse("").trim();
            if (text.isEmpty()) {
                return;
            }
            Double angle = convertToDouble(text);
            if (angle != null) {
                canvas.rotate(angle);
            }
        });

        MenuItem miResetRotation = new MenuItem("Reset rotation");
        miResetRotation.setOnAction(e -> {
            canvas.rotate(0.0);
        });

        CheckMenuItem cmiShowNodes = new CheckMenuItem("Show nodes");
        cmiShowNodes.setSelected(true);
        cmiShowNodes.setOnAction(e -> {
            canvas.setNodesShowing(cmiShowNodes.isSelected());
            canvas.repaintLater();
        });

        CheckMenuItem cmiShowNodeLabels = new CheckMenuItem("Show node labels");
        cmiShowNodeLabels.setSelected(false);
        cmiShowNodeLabels.setOnAction(e -> {
            canvas.setNodeLabelsShowing(cmiShowNodeLabels.isSelected());
            canvas.repaintLater();
        });

        CheckMenuItem cmiShowLinks = new CheckMenuItem("Show links");
        CheckMenuItem cmiShowLinkLabels = new CheckMenuItem("Show link labels");
        cmiShowLinks.setSelected(true);
        cmiShowLinks.setOnAction(e -> {
            if (cmiShowLinks.isSelected()) {
                cmiShowLinkLabels.setDisable(false);
                canvas.setLinksShowing(true);
            } else {
                cmiShowLinkLabels.setDisable(true);
                canvas.setLinksShowing(false);
            }
            canvas.repaintLater();
        });

        cmiShowLinkLabels.setSelected(false);
        cmiShowLinkLabels.setOnAction(e -> {
            canvas.setLinkLabelsShowing(cmiShowLinkLabels.isSelected());
            canvas.repaintLater();
        });

        CheckMenuItem cmiShowAreas = new CheckMenuItem("Show areas");
        CheckMenuItem cmiShowAreaLabels = new CheckMenuItem("Show area labels");
        cmiShowAreas.setSelected(true);
        cmiShowAreas.setOnAction(e -> {
            if (cmiShowAreas.isSelected()) {
                cmiShowAreaLabels.setDisable(false);
                canvas.setAreasShowing(true);
            } else {
                cmiShowAreaLabels.setDisable(true);
                canvas.setAreasShowing(false);
            }
            canvas.repaintLater();
        });

        cmiShowAreaLabels.setSelected(false);
        cmiShowAreaLabels.setOnAction(e -> {
            canvas.setAreaLabelsShowing(cmiShowAreaLabels.isSelected());
            canvas.repaintLater();
        });

        CheckMenuItem cmiShowBackgroundImage = new CheckMenuItem("Show background image");
        cmiShowBackgroundImage.setSelected(true);
        cmiShowBackgroundImage.setOnAction(e -> {
            canvas.setBackgroundImageShowing(cmiShowBackgroundImage.isSelected());
            canvas.repaintLater();
        });

        CheckMenuItem cmiShowMapCoordinates = new CheckMenuItem("Show map coordinates on the cursor");
        cmiShowMapCoordinates.setOnAction(e -> {
            canvas.setMapCoordinatesShowing(cmiShowMapCoordinates.isSelected());
        });

        viewMenu.getItems().addAll(miShow3d, new SeparatorMenuItem(), miCentering, miCenteringWithScaling, miToTheOrigin, miSetRotation, miResetRotation, new SeparatorMenuItem(), cmiShowNodes, cmiShowNodeLabels, cmiShowLinks, cmiShowLinkLabels, cmiShowAreas, cmiShowAreaLabels, new SeparatorMenuItem(), cmiShowBackgroundImage, menuShowBackgroundGroup, cmiShowMapCoordinates);

        /* Validation menu */

        Menu actionMenu = new Menu("Validation");

        MenuItem miCalculateTagPaths = new MenuItem("Calculate tag paths");
        miCalculateTagPaths.setOnAction(e -> openCalculateTagPathsDialog());

        MenuItem miCheckForPiledNodes = new MenuItem("Check for node in same position");
        miCheckForPiledNodes.setOnAction(e -> checkForPiledNodes());

        MenuItem miCheckReachability = new MenuItem("Check reachability");
        miCheckReachability.setOnAction(e -> checkForReachability());

        actionMenu.getItems().addAll(miCalculateTagPaths, miCheckForPiledNodes, miCheckReachability);

        /* Help menu */

        Menu helpMenu = new Menu("Help");

        MenuItem miKeyboardShortcuts = new MenuItem("Quick reference");
        miKeyboardShortcuts.setOnAction(e -> {
            helpStage.setTitle("Help - Quick reference");
            webView.getEngine().loadContent(ObstructerBase.resourceToString(QUICK_REFERENCE));
            helpStage.show();
            helpStage.toFront();
        });

        MenuItem miPropertiesSetting = new MenuItem("Properties setting");
        miPropertiesSetting.setOnAction(e -> {
            File file = new File(PROPERTIES_PATH);
            if (file.exists()) {
                helpStage.setTitle("Help - Properties setting");
                helpStage.setWidth(900);
                helpStage.setHeight(Math.min(Screen.getPrimary().getVisualBounds().getHeight(), 1200));
                try {
                    webView.getEngine().load("file:///" + file.getCanonicalPath().replace('\\', '/'));
                    helpStage.show();
                    helpStage.toFront();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            } else {
                Alert alert = new Alert(AlertType.WARNING, "You need to execute \"sh make_javadoc.sh\" on the command line to generate javadoc files.", ButtonType.OK);
                alert.showAndWait();
            }
        });

        MenuItem miTutorialManual = new MenuItem("Tutorial manual(Old content)");
        miTutorialManual.setOnAction(e -> {
            File file = new File(TUTORIAL_PATH);
            if (file.exists()) {
                helpStage.setTitle("Help - Tutorial manual");
                helpStage.setWidth(1000);
                helpStage.setHeight(Math.min(Screen.getPrimary().getVisualBounds().getHeight(), 1200));
                try {
                    webView.getEngine().load("file:///" + file.getCanonicalPath().replace('\\', '/'));
                    helpStage.show();
                    helpStage.toFront();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        MenuItem miZoneReference = new MenuItem("Zone of plane rectangular coordinate system");
        miZoneReference.setOnAction(e -> {
            helpStage.setTitle("Help - Zone of plane rectangular coordinate system");
            helpStage.setWidth(900);
            helpStage.setHeight(Math.min(Screen.getPrimary().getVisualBounds().getHeight(), 1200));
            webView.getEngine().load(ZONE_REFERENCE_URI);
            helpStage.show();
            helpStage.toFront();
        });

        MenuItem miGitHub = new MenuItem("Browse GitHub repository");
        miGitHub.setOnAction(e -> {
            new Thread(() -> {
                try {
                    URI uri = new URI(GITHUB_REPOSITORY_URI);
                    Desktop.getDesktop().browse(uri);
                } catch (IOException | URISyntaxException ex) {
                    ex.printStackTrace();
                }
            }).start();
        });

        helpMenu.getItems().addAll(miKeyboardShortcuts, miPropertiesSetting, miTutorialManual, miZoneReference, miGitHub);

        menuBar.getMenus().addAll(fileMenu, editMenu, viewMenu, actionMenu, helpMenu);

        return menuBar;
    }

    /**
     * 背景グループ表示サブメニューを更新する
     */
    public void updateShowBackgroundGroupMenu() {
        menuShowBackgroundGroup.getItems().clear();
        ToggleGroup backgroundGroup = new ToggleGroup();
        if (canvas != null) {
            canvas.setBackgroundGroup(null);
        }

        for (MapPartGroup group : editor.getMap().getGroups()) {
            if (group == editor.getMap().getRoot() || group.getTags().size() == 0) {
                continue;
            }
            RadioMenuItem menuItem = new RadioMenuItem(group.getTagString());
            menuItem.setToggleGroup(backgroundGroup);
            menuItem.setOnAction(e -> {
                canvas.setBackgroundGroup(group);
                canvas.repaintLater();
            });
            menuShowBackgroundGroup.getItems().add(menuItem);
        }
        RadioMenuItem menuItem = new RadioMenuItem("Do not show");
        menuItem.setToggleGroup(backgroundGroup);
        menuItem.setSelected(true);
        menuItem.setOnAction(e -> {
            canvas.setBackgroundGroup(null);
            canvas.repaintLater();
        });
        menuShowBackgroundGroup.getItems().addAll(new SeparatorMenuItem(), menuItem);
    }

    /**
     * コンテキスト・メニューを生成する
     */
    private void createContextMenus() {
        // EDIT_NODE モード
        // ・Set node attributes
        // ・Align nodes horizontally
        // ・Align nodes vertically
        // ・Move
        // ・Duplicate and move
        // ・Make stairs
        // ・Rotate and scale
        // ・Add symbolic link
        // ・Clear symbolic link
        // ・Remove nodes

        MenuItem miSetNodeAttributes = new MenuItem("Set node attributes");
        miSetNodeAttributes.setOnAction(e -> openNodeAttributesDialog());

        MenuItem miHorizontally = new MenuItem("Align nodes horizontally");
        miHorizontally.setOnAction(e -> editor.alignNodesHorizontally());

        MenuItem miVertically = new MenuItem("Align nodes vertically");
        miVertically.setOnAction(e -> editor.alignNodesVertically());

        MenuItem miMove = new MenuItem("Move");
        miMove.setOnAction(e -> openMoveNodesDialog());

        MenuItem miDuplicateAndMove = new MenuItem("Duplicate and move");
        miDuplicateAndMove.setOnAction(e -> openDuplicateAndMoveDialog());

        MenuItem miMakeStairs = new MenuItem("Make stairs");
        miMakeStairs.setOnAction(e -> openMakeStairsDialog());

        MenuItem miRotateAndScale = new MenuItem("Rotate and scale");
        miRotateAndScale.setOnAction(e -> openRotateAndScaleNodesDialog());

        MenuItem miClearSymbolicLinkOfNode = new MenuItem("Clear symbolic link");
        miClearSymbolicLinkOfNode.setOnAction(e -> editor.removeSymbolicLink(editor.getSelectedNodes()));

        MenuItem miRemoveNode = new MenuItem("Remove nodes");
        miRemoveNode.setOnAction(e -> editor.removeNodes());

        editNodeMenu.getItems().addAll(miSetNodeAttributes, miHorizontally, miVertically, miMove, miDuplicateAndMove, miMakeStairs, miRotateAndScale, menuAddSymbolicLinkOfNode, miClearSymbolicLinkOfNode, miRemoveNode);

        // EDIT_LINK モード
        // ・Set link attributes
        // ・Set one-way
        // ・Set road closed
        // ・Reset one-way / road closed
        // ・Add symbolic link
        // ・Clear symbolic link
        // ・Scale calculation and link length recalculation
        // ・Remove links

        MenuItem miSetLinkAttributes = new MenuItem("Set link attributes");
        miSetLinkAttributes.setOnAction(e -> openLinkAttributesDialog());

        MenuItem miSetOneWay = new MenuItem("Set one-way");
        miSetOneWay.setOnAction(e -> setOneWay());

        MenuItem miSetRoadClosed = new MenuItem("Set Road closed");
        miSetRoadClosed.setOnAction(e -> editor.setRoadClosed());

        MenuItem miResetOneWayAndRoadClosed = new MenuItem("Reset one-way / road closed");
        miResetOneWayAndRoadClosed.setOnAction(e -> editor.resetOneWayRoadClosed());

        MenuItem miCalculateScale = new MenuItem("Calculate scale and recalculate link length");
        miCalculateScale.setOnAction(e -> openCalculateScaleDialog());

        MenuItem miClearSymbolicLinkOfLink = new MenuItem("Clear symbolic link");
        miClearSymbolicLinkOfLink.setOnAction(e -> editor.removeSymbolicLink(editor.getSelectedLinks()));

        MenuItem miRemoveLink = new MenuItem("Remove links");
        miRemoveLink.setOnAction(e -> editor.removeLinks());

        editLinkMenu.getItems().addAll(miSetLinkAttributes, miSetOneWay, miSetRoadClosed, miResetOneWayAndRoadClosed, menuAddSymbolicLinkOfLink, miClearSymbolicLinkOfLink, miCalculateScale, miRemoveLink);

        // EDIT_AREA モード
        // ・Set area attributes
        // ・Remove areas

        MenuItem miSetAreaAttributes = new MenuItem("Set area attributes");
        miSetAreaAttributes.setOnAction(e -> openAreaAttributesDialog());

        MenuItem miRemoveArea = new MenuItem("Remove areas");
        miRemoveArea.setOnAction(e -> editor.removeAreas());

        editAreaMenu.getItems().addAll(miSetAreaAttributes, miRemoveArea);

        // BACKGROUND_IMAGE モード
        // ・Set background image
        // ・Set background image attributes
        // ・Remove background image

        miSetBackgroundImage = new MenuItem("Set background image");
        miSetBackgroundImage.setOnAction(e -> setBackgroundImage());

        miSetBackgroundImageAttributes = new MenuItem("Set background image attributes");
        miSetBackgroundImageAttributes.setOnAction(e -> openBackgroundImageAttributesDialog());

        miRemoveBackgroundImage = new MenuItem("Remove background image");
        miRemoveBackgroundImage.setOnAction(e -> removeBackgroundImage());

        bgImageMenu.getItems().addAll(miSetBackgroundImage, miSetBackgroundImageAttributes, miRemoveBackgroundImage);
    }

    /**
     * シンボリックリンク追加サブメニューを更新する
     */
    public void updateAddSymbolicLinkMenu() {
        menuAddSymbolicLinkOfNode.getItems().clear();
        menuAddSymbolicLinkOfLink.getItems().clear();
        for (MapPartGroup group : editor.getMap().getGroups()) {
            if (group == editor.getMap().getRoot() || group.getTags().size() == 0) {
                continue;
            }
            MenuItem menuItem = new MenuItem("Symbolic link to " + group.getTagString());
            menuItem.setOnAction(e -> addSymbolicLinkOfNode(group));
            menuAddSymbolicLinkOfNode.getItems().add(menuItem);

            menuItem = new MenuItem("Symbolic link to " + group.getTagString());
            menuItem.setOnAction(e -> addSymbolicLinkOfLink(group));
            menuAddSymbolicLinkOfLink.getItems().add(menuItem);
        }
    }

    /**
     * グループ選択パネルを更新する
     */
    public FlowPane updateGroupSelectionPane() {
        NetworkMap networkMap = editor.getMap();
        groupButtonMap.clear();
        groupSelectionPane.getChildren().clear();
        groupToggleGroup.getToggles().clear();

        Label label = new Label("Group");
        label.setFont(Font.font("Arial", FontWeight.BOLD, label.getFont().getSize()));
        groupSelectionPane.getChildren().add(label);
        for (MapPartGroup group : networkMap.getGroups()) {
            if (group.getTags().size() == 0) {
                continue;
            }
            ToggleButton groupButton = new ToggleButton(group.getTagString());
            groupButton.setToggleGroup(groupToggleGroup);
            if (networkMap.getGroups().size() > 1 && group == networkMap.getRoot()) {
                groupButton.setDisable(true);
            } else {
                groupButton.setSelected(group == editor.getCurrentGroup());
                groupButton.setOnAction(e -> {
                    if (groupButton.isSelected()) {
                        editor.setCurrentGroup(group);
                        canvas.repaintLater();
                    }
                });
            }
            groupButtonMap.put(group, groupButton);
            groupSelectionPane.getChildren().add(groupButton);
        }
        return groupSelectionPane;
    }

    /**
     * 編集モード選択パネルを構築する
     */
    private FlowPane createModeSelectionPane() {
        FlowPane flowPane = new FlowPane();
        flowPane.getStyleClass().add("custom-color-pane");
        flowPane.setPadding(new Insets(4));
        flowPane.setHgap(8);
        ToggleGroup group = new ToggleGroup();

        Label label = new Label("Mode");
        label.setFont(Font.font("Arial", FontWeight.BOLD, label.getFont().getSize()));

        ToggleButton tbAddNode = new ToggleButton("Add Node");
        tbAddNode.setToggleGroup(group);
        tbAddNode.setOnAction(e -> {
            removeLinkAttributesPane();
            canvas.setMode(EditorMode.ADD_NODE);
        });

        ToggleButton tbAddLink = new ToggleButton("Add Link");
        tbAddLink.setToggleGroup(group);
        tbAddLink.setOnAction(e -> {
            addLinkAttributesPane();
            canvas.setMode(EditorMode.ADD_LINK);
        });

        ToggleButton tbAddNodeAndLink = new ToggleButton("Add Node & Link");
        tbAddNodeAndLink.setToggleGroup(group);
        tbAddNodeAndLink.setOnAction(e -> {
            addLinkAttributesPane();
            canvas.setMode(EditorMode.ADD_NODE_LINK);
        });

        ToggleButton tbEditNode = new ToggleButton("Edit Node");
        tbEditNode.setToggleGroup(group);
        tbEditNode.setOnAction(e -> {
            removeLinkAttributesPane();
            canvas.setMode(EditorMode.EDIT_NODE);
        });

        ToggleButton tbEditLink = new ToggleButton("Edit Link");
        tbEditLink.setToggleGroup(group);
        tbEditLink.setOnAction(e -> {
            removeLinkAttributesPane();
            canvas.setMode(EditorMode.EDIT_LINK);
        });

        ToggleButton tbEditArea = new ToggleButton("Edit Area");
        tbEditArea.setToggleGroup(group);
        tbEditArea.setOnAction(e -> {
            removeLinkAttributesPane();
            canvas.setMode(EditorMode.EDIT_AREA);
        });

        ToggleButton tbBgImage = new ToggleButton("Background Image");
        tbBgImage.setToggleGroup(group);
        tbBgImage.setOnAction(e -> {
            removeLinkAttributesPane();
            canvas.setMode(EditorMode.BACKGROUND_IMAGE);
        });

        flowPane.getChildren().addAll(label, tbAddNode, tbAddLink, tbAddNodeAndLink, tbEditNode, tbEditLink, tbEditArea, tbBgImage);
        tbEditNode.setSelected(true);

        return flowPane;
    }

    /**
     * リンク情報パネルを構築する
     */
    private HBox createLinkAttributesPane() {
        HBox hbox = new HBox();
        hbox.setAlignment(Pos.CENTER_LEFT);
        hbox.setSpacing(8);

        Label lengthLabel = new Label("length");
        linkLengthField.setDisable(true);
        linkLengthField.setPrefWidth(160);

        Label widthLabel = new Label("width");
        linkWidthField.setPrefWidth(160);

        hbox.getChildren().addAll(lengthLabel, linkLengthField, linkScaleLabel, widthLabel, linkWidthField);
        hbox.setMargin(linkScaleLabel, new Insets(0, 8, 0, 0));

        return hbox;
    }

    /**
     * ステータスバーにリンク情報パネルを追加する
     */
    private void addLinkAttributesPane() {
        linkLengthField.setText("0.0");
        linkScaleLabel.setText("(scale: " + editor.getCurrentGroup().getScale() + ")");
        if (statusPane.getChildren().size() == 1) {
            statusPane.getChildren().add(0, linkAttributesPane);
            statusPane.getChildren().add(1, new Separator(Orientation.VERTICAL));
        }
    }

    /**
     * ステータスバーからリンク情報パネルを削除する
     */
    private void removeLinkAttributesPane() {
        if (statusPane.getChildren().size() == 3) {
            statusPane.getChildren().remove(0, 2);
        }
    }

    /**
     * リンク長フィールドに値をセットする
     */
    public void setCurrentLinkLength(double length) {
        linkLengthField.setText("" + length);
    }

    /**
     * リンク幅フィールドの値を取得する
     */
    public double getCurrentLinkWidth() {
        Double width = convertToDouble(linkWidthField.getText());
        if (width == null) {
            return 0.0;
        }
        if (width <= 0.0) {
            alertInvalidInputValue("Incorrect width.");
        }
        return width;
    }

    /**
     * 現在のマップデータで GUI を再設定する
     */
    public void resetGui() {
        editor.initCurrentGroup();
        editor.updateHeight();
        groupPanel.clear();
        groupPanel.construct();
        nodePanel.reset();
        linkPanel.reset();
        areaPanel.reset();
        scenarioPanel.reset();
        updateShowBackgroundGroupMenu();
        updateAddSymbolicLinkMenu();
        updateGroupSelectionPane();
        canvas.repaintLater();
    }

    /**
     * マップデータを消去して新規状態にする
     */
    public void clearMapData() {
        if (editor.isModified()) {
            Alert alert = new Alert(AlertType.CONFIRMATION, "Warning:\n    Map data has been modified.\n    Do you wish to continue anyway?", ButtonType.YES, ButtonType.NO);
            Optional<ButtonType> result = alert.showAndWait();
            if (! result.isPresent() || result.get() == ButtonType.NO) {
                return;
            }
        }
        editor.initNetworkMap();
        canvas.clearEditingStates();
        resetGui();
    }

    /**
     * ファイル選択ダイアログを開いてマップファイルを読み込む
     */
    public void openMap() {
        if (editor.isModified()) {
            Alert alert = new Alert(AlertType.CONFIRMATION, "Warning:\n    Map data has been modified.\n    Do you wish to continue anyway?", ButtonType.YES, ButtonType.NO);
            Optional<ButtonType> result = alert.showAndWait();
            if (! result.isPresent() || result.get() == ButtonType.NO) {
                return;
            }
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open map");
        String fileName = editor.getNetworkMapFile();
        if (fileName == null || fileName.isEmpty()) {
            String dirName = settings.get("mapDir", "");
            if (dirName.isEmpty()) {
                dirName = "./";
            }
            fileChooser.setInitialDirectory(new File(dirName));
            fileChooser.setInitialFileName(settings.get("mapFile", ""));     // TODO: 現状では無効
        } else {
            setInitialPath(fileChooser, fileName);
        }
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("XML", "*.xml"),
            new FileChooser.ExtensionFilter("All", "*.*")
        );
        File file = fileChooser.showOpenDialog(frame);
        if (file == null) {
            return;
        }
        editor.setNetworkMapFile(editor.getRelativePath(file));
        if (! editor.loadNetworkMap()) {
            Alert alert = new Alert(AlertType.WARNING, "Map file open error.", ButtonType.OK);
            alert.showAndWait();
            editor.initNetworkMap();
        }
        canvas.clearEditingStates();
        resetGui();
    }

    /**
     * マップファイルを保存する
     */
    public void saveMap() {
        if (! editor.isModified()) {
            Alert alert = new Alert(AlertType.INFORMATION, "Map data is not modified.", ButtonType.OK);
            alert.showAndWait();
            return;
        }

        String fileName = editor.getNetworkMapFile();
        if (fileName == null || fileName.isEmpty()) {
            saveMapAs();
            return;
        }
        if (! editor.saveMap()) {
            Alert alert = new Alert(AlertType.ERROR, "Save map file failed: " + fileName, ButtonType.OK);
            alert.showAndWait();
        }
    }

    /**
     * マップファイルに名前を付けて保存する
     */
    public void saveMapAs() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save map as");
        String fileName = editor.getNetworkMapFile();
        if (fileName == null || fileName.isEmpty()) {
            String dirName = settings.get("mapDir", "");
            if (dirName.isEmpty()) {
                dirName = "./";
            }
            fileChooser.setInitialDirectory(new File(dirName));
            fileChooser.setInitialFileName(settings.get("mapFile", ""));     // TODO: 現状では無効
        } else {
            setInitialPath(fileChooser, fileName);
        }
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("XML", "*.xml"),
            new FileChooser.ExtensionFilter("All", "*.*")
        );
        File file = fileChooser.showSaveDialog(frame);
        if (file == null) {
            return;
        }

        try {
            fileName = file.getCanonicalPath();
        } catch (IOException e) {
            e.printStackTrace();
            fileName = file.getAbsolutePath();
        }
        editor.setNetworkMapFile(fileName);
        if (! editor.saveMap()) {
            Alert alert = new Alert(AlertType.ERROR, "Save map file failed: " + fileName, ButtonType.OK);
            alert.showAndWait();
        }
    }

    /**
     * GUI シミュレータを起動する
     */
    private void simulate(String simulator, Button simulate2dButton, Button simulate3dButton) {
        if (editor.getNetworkMapFile() == null || editor.getNetworkMapFile().isEmpty() || editor.getGenerationFile() == null || editor.getGenerationFile().isEmpty() || editor.getScenarioFile() == null || editor.getScenarioFile().isEmpty()) {
            Alert alert = new Alert(AlertType.INFORMATION, "There are not enough files for simulation.", ButtonType.OK);
            alert.showAndWait();
            return;
        }
        if (editor.isModified()) {
            Alert alert = new Alert(AlertType.CONFIRMATION, "Warning:\n    Map data may change when simulation is executed.\n    Would you like to start the simulator?", ButtonType.YES, ButtonType.NO);
            Optional<ButtonType> result = alert.showAndWait();
            if (! result.isPresent() || result.get() != ButtonType.YES) {
                return;
            }
        }
        simulate2dButton.setDisable(true);
        simulate3dButton.setDisable(true);
        GuiSimulationLauncher launcher = GuiSimulationLauncher.createInstance(simulator);
        launcher.init(editor.getRandom(), properties, editor.getSetupFileInfo(), editor.getMap(), settings);
        launcher.simulate();
    }

    /**
     * フロアの複製ダイアログを開く
     */
    public void openDuplicateFloorDialog() {
        MapPartGroup currentGroup = editor.getCurrentGroup();
        if (currentGroup == editor.getMap().getRoot()) {
            Alert alert = new Alert(AlertType.WARNING, "Root group can not be duplicated.", ButtonType.OK);
            alert.showAndWait();
            return;
        }
        Matcher match = currentGroup.matchTag("^(B?)(\\d+)F$");
        if (match == null) {
            Alert alert = new Alert(AlertType.WARNING, "No floor number given for this group.", ButtonType.OK);
            alert.showAndWait();
            return;
        }

        Dialog dialog = new Dialog();
        dialog.setTitle("Duplicate floor");
        VBox paramPane = new VBox();

        Label label = new Label("Duplicate " + currentGroup.getTagString());
        label.setFont(Font.font("Arial", FontWeight.BOLD, label.getFont().getSize()));
        label.setPadding(new Insets(0, 0, 8, 0));

        Label directionLabel = new Label("Direction");
        RadioButton upButton = new RadioButton("Up");
        RadioButton downButton = new RadioButton("Down");
        ToggleGroup toggleGroup = new ToggleGroup();
        upButton.setToggleGroup(toggleGroup);
        downButton.setToggleGroup(toggleGroup);
        upButton.setSelected(true);
        FlowPane flowPane = new FlowPane();
        flowPane.setHgap(8);
        flowPane.getChildren().addAll(upButton, downButton);

        Label numberLabel = new Label("Number of floors");
        Spinner<Integer> numberSpinner = new Spinner<>(1, 100, 1);

        Label heightDiffLabel = new Label("Height difference");
        TextField heightDiffField = new TextField("" + 0.0);
        heightDiffField.setMaxWidth(150);

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.add(directionLabel, 1, 1);
        grid.add(flowPane, 2, 1);
        grid.add(numberLabel, 1, 2);
        grid.add(numberSpinner, 2, 2);
        grid.add(heightDiffLabel, 1, 3);
        grid.add(heightDiffField, 2, 3);

        paramPane.getChildren().addAll(label, grid);

        dialog.getDialogPane().setContent(paramPane);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            Double value = convertToDouble(heightDiffField.getText());
            if (value != null) {
                double heightDiff = value;
                if (heightDiff < 1.0) {
                    Alert alert = new Alert(AlertType.WARNING, "Invalid height difference.", ButtonType.OK);
                    alert.showAndWait();
                    return;
                }
                int floor = Integer.parseInt(match.group(2));
                if (! match.group(1).isEmpty()) {
                    floor = -floor;
                }
                try {
                    editor.duplicateFloor(currentGroup, upButton.isSelected() ? 1 : -1, numberSpinner.getValue(), heightDiff);
                } catch(Exception e) {
                    Alert alert = new Alert(AlertType.WARNING, e.getMessage(), ButtonType.OK);
                    alert.showAndWait();
                }
            }
        }
    }

    /**
     * Calculate tag paths ダイアログを開く
     */
    public void openCalculateTagPathsDialog() {
        TextInputDialog dialog = new TextInputDialog("");
        dialog.setTitle("Calculate tag paths");
        dialog.setHeaderText("Enter a goal tag");
        String tag = dialog.showAndWait().orElse("").trim();
        if (! tag.isEmpty()) {
            Alert alert = new Alert(AlertType.CONFIRMATION, "Warning:\n    Tags are added to all nodes that can reach the goal.\n    Do you want to continue?", ButtonType.YES, ButtonType.NO);
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.YES) {
                if (editor.calcTagPaths(tag)) {
                    Alert _alert = new Alert(AlertType.NONE, "Calculation of paths finished.", ButtonType.OK);
                    _alert.showAndWait();
                } else {
                    Alert _alert = new Alert(AlertType.WARNING, "No goal with tag " + tag, ButtonType.OK);
                    _alert.showAndWait();
                }
            }
        }
    }

    /**
     * ノード座標の重複をチェックする
     */
    public void checkForPiledNodes() {
        for (MapNode node : editor.getMap().getNodes()) {
            if (node.selected) {
                Alert alert = new Alert(AlertType.WARNING, "Cancel all node selections before executing.", ButtonType.OK);
                alert.showAndWait();
                return;
            }
            if (! nodePanel.getFilteredSet().contains(node)) {
                Alert alert = new Alert(AlertType.WARNING, "Cancel node filter before executing.", ButtonType.OK);
                alert.showAndWait();
                return;
            }
        }

        ArrayList<MapNode> piledNodes = editor.getPiledNodes();
        if (piledNodes.isEmpty()) {
            Alert alert = new Alert(AlertType.INFORMATION, "No nodes with the same position.", ButtonType.OK);
            alert.showAndWait();
            return;
        }

        Alert alert = new Alert(AlertType.CONFIRMATION, "" + piledNodes.size() + " collisions were found.\nResolve them?", ButtonType.YES, ButtonType.NO);
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.YES) {
            nodePanel.select(piledNodes);
        }
    }

    /**
     * ターゲットに到達できないリンクがないかチェックする
     */
    public void checkForReachability() {
        NetworkMap networkMap = editor.getMap();
        for (MapLink link : networkMap.getLinks()) {
            if (link.selected) {
                Alert alert = new Alert(AlertType.WARNING, "Cancel all link selections before executing.", ButtonType.OK);
                alert.showAndWait();
                return;
            }
            if (! linkPanel.getFilteredSet().contains(link)) {
                Alert alert = new Alert(AlertType.WARNING, "Cancel link filter before executing.", ButtonType.OK);
                alert.showAndWait();
                return;
            }
        }

        TextInputDialog dialog = new TextInputDialog("");
        dialog.setTitle("Check reachability");
        dialog.setHeaderText("Enter a target tag");
        String tag = dialog.showAndWait().orElse("").trim();
        if (tag.isEmpty()) {
            return;
        }

        MapLinkTable reachableLinks = MapChecker.getReachableLinks(networkMap.getNodes(), tag);
        int notConnectedCount = networkMap.getLinks().size() - reachableLinks.size();
        if (notConnectedCount > 0) {
            Alert alert = new Alert(AlertType.CONFIRMATION, "There were " + notConnectedCount + " links not leading to target!\nShould select REACHABLE links?", ButtonType.YES, ButtonType.NO);
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.YES) {
                linkPanel.select(reachableLinks);
            }
        } else {
            Alert alert = new Alert(AlertType.INFORMATION, "Calculation of paths finished.", ButtonType.OK);
            alert.showAndWait();
        }
    }

    /**
     * 選択オブジェクトが複数グループにまたがっている場合の継続確認
     */
    public boolean multipleGroupConfirmation(ArrayList<? extends OBNode> obNodes, String message) {
        if (! editor.isSingleGroup(obNodes)) {
            Alert alert = new Alert(AlertType.CONFIRMATION, message, ButtonType.YES, ButtonType.NO);
            Optional<ButtonType> result = alert.showAndWait();
            return result.isPresent() && result.get() == ButtonType.YES;
        }
        return true;
    }

    /**
     * ノード設定ダイアログを開く
     */
    public void openNodeAttributesDialog() {
        ArrayList<MapNode> nodes = editor.getSelectedNodes();
        if (nodes.isEmpty()) {
            return;
        }
        if (! multipleGroupConfirmation(nodes, "Warning:\n    Nodes of multiple groups were selected.\n    Do you want to continue?")) {
            return;
        }

        double averageHeight = 0.0;
        for (MapNode node : nodes) {
            averageHeight += node.getHeight();
        }
        averageHeight /= nodes.size();

        Dialog dialog = new Dialog();
        dialog.setTitle("Set node attributes");
        VBox paramPane = new VBox();

        if (nodes.size() > 1) {
            Label label = new Label("" + nodes.size() + " nodes selected");
            label.setPadding(new Insets(0, 0, 12, 0));
            paramPane.getChildren().addAll(label);
        }

        Label label = new Label("Parameters");
        label.setFont(Font.font("Arial", FontWeight.BOLD, label.getFont().getSize()));
        label.setPadding(new Insets(0, 0, 8, 0));

        // height field
        Label itemInfo = new Label("height(" + averageHeight + ")");
        itemInfo.setPadding(new Insets(0, 0, 0, 4));
        TextField textField = new TextField("" + averageHeight);
        textField.setMinWidth(100);
        Button button = new Button("Set");
        EventHandler heightHandler = new EventHandler<ActionEvent>() {
            public void handle(ActionEvent event) {
                Double value = convertToDouble(textField.getText());
                if (value != null) {
                    double height = value;
                    editor.startOfCommandBlock();
                    for (MapNode node : nodes) {
                        if (! editor.invoke(new SetHeight(node, height))) {
                            break;
                        }
                    }
                    editor.endOfCommandBlock();
                    editor.updateHeight();
                    dialog.close();
                }
            }
        };
        textField.setOnAction(heightHandler);
        button.setOnAction(heightHandler);
        FlowPane flowPane = new FlowPane();
        flowPane.setHgap(8);
        flowPane.getChildren().addAll(itemInfo, textField, button);

        Separator separator = new Separator();
        separator.setPadding(new Insets(8, 0, 8, 0));
        TagSetupPane pane = new TagSetupPane(editor, nodes, dialog);
        paramPane.getChildren().addAll(label, flowPane, separator, pane);

        dialog.getDialogPane().setContent(paramPane);
        ButtonType cancel = new ButtonType("Cancel", ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().add(cancel);
        dialog.showAndWait();
    }

    /**
     * ノード移動ダイアログを開く
     */
    public void openMoveNodesDialog() {
        ArrayList<MapNode> nodes = editor.getSelectedNodes();
        if (nodes.isEmpty()) {
            return;
        }
        if (! multipleGroupConfirmation(nodes, "Warning:\n    Nodes of multiple groups were selected.\n    Do you want to continue?")) {
            return;
        }

        Dialog dialog = new Dialog();
        dialog.setTitle("Move nodes");
        dialog.getDialogPane().setPrefWidth(300);
        VBox paramPane = new VBox();

        if (nodes.size() > 1) {
            Label label = new Label("" + nodes.size() + " nodes selected");
            label.setPadding(new Insets(0, 0, 12, 0));
            paramPane.getChildren().addAll(label);
        }

        Label label = new Label("Moving distance (m)");
        label.setFont(Font.font("Arial", FontWeight.BOLD, label.getFont().getSize()));
        label.setPadding(new Insets(0, 0, 8, 0));

        // X
        Label xLabel = new Label("X");
        TextField xField = new TextField("" + 0.0);
        xField.setMinWidth(100);

        // Y
        Label yLabel = new Label("Y");
        TextField yField = new TextField("" + 0.0);
        yField.setMinWidth(100);

        // Z
        Label zLabel = new Label("Z");
        TextField zField = new TextField("" + 0.0);
        zField.setMinWidth(100);

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(0, 0, 0, 10));
        grid.setHgap(8);
        grid.setVgap(8);
        grid.add(xLabel, 1, 1);
        grid.add(xField, 2, 1);
        grid.add(yLabel, 1, 2);
        grid.add(yField, 2, 2);
        grid.add(zLabel, 1, 3);
        grid.add(zField, 2, 3);

        paramPane.getChildren().addAll(label, grid);

        dialog.getDialogPane().setContent(paramPane);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // TODO: 既存のノードと座標が重なる場合は失敗させる
            Double _x = convertToDouble(xField.getText());
            Double _y = convertToDouble(yField.getText());
            Double _z = convertToDouble(zField.getText());
            double x = 0.0;
            double y = 0.0;
            double z = 0.0;
            if (_x != null) {
                x = _x;
            }
            if (_y != null) {
                y = _y;
            }
            if (_z != null) {
                z = _z;
            }
            editor.moveNodes(nodes, x, y, z);
        }
    }

    /**
     * 複製して移動ダイアログを開く
     */
    public void openDuplicateAndMoveDialog() {
        ArrayList<MapNode> nodes = editor.getSelectedNodes();
        if (nodes.isEmpty()) {
            return;
        }
        if (! multipleGroupConfirmation(nodes, "Warning:\n    Nodes of multiple groups were selected.\n    Do you want to continue?")) {
            return;
        }

        Dialog dialog = new Dialog();
        dialog.setTitle("Duplicate and Move");
        dialog.getDialogPane().setPrefWidth(300);
        VBox paramPane = new VBox();

        if (nodes.size() > 1) {
            Label label = new Label("" + nodes.size() + " nodes selected");
            label.setPadding(new Insets(0, 0, 12, 0));
            paramPane.getChildren().addAll(label);
        }

        Label xyzLabel = new Label("Moving distance (m)");
        xyzLabel.setFont(Font.font("Arial", FontWeight.BOLD, xyzLabel.getFont().getSize()));
        xyzLabel.setPadding(new Insets(0, 0, 8, 0));

        // X
        Label xLabel = new Label("X");
        TextField xField = new TextField("" + 0.0);
        xField.setMinWidth(100);

        // Y
        Label yLabel = new Label("Y");
        TextField yField = new TextField("" + 0.0);
        yField.setMinWidth(100);

        // Z
        Label zLabel = new Label("Z");
        TextField zField = new TextField("" + 0.0);
        zField.setMinWidth(100);

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(0, 0, 8, 10));
        grid.setHgap(8);
        grid.setVgap(8);
        grid.add(xLabel, 1, 1);
        grid.add(xField, 2, 1);
        grid.add(yLabel, 1, 2);
        grid.add(yField, 2, 2);
        grid.add(zLabel, 1, 3);
        grid.add(zField, 2, 3);

        Label groupLabel = new Label("Destination group");
        groupLabel.setFont(Font.font("Arial", FontWeight.BOLD, groupLabel.getFont().getSize()));
        groupLabel.setPadding(new Insets(2, 0, 10, 0));

        HashMap<String, MapPartGroup> groups = new HashMap();
        ArrayList<String> groupNames = new ArrayList();
        for (MapPartGroup group : editor.getMap().getGroups()) {
            if (group == editor.getMap().getRoot() || group.getTags().size() == 0) {
                continue;
            }
            groups.put(group.getTagString(), group);
            groupNames.add(group.getTagString());
        }
        ChoiceBox groupChoiceBox = new ChoiceBox(FXCollections.observableArrayList(groupNames));
        groupChoiceBox.setValue(editor.getCurrentGroup().getTagString());
        FlowPane flowPane = new FlowPane();
        flowPane.setPadding(new Insets(0, 0, 14, 18));
        flowPane.getChildren().add(groupChoiceBox);

        CheckBox wlCheckBox = new CheckBox("Without links");
        wlCheckBox.setFont(Font.font("Arial", FontWeight.BOLD, wlCheckBox.getFont().getSize()));

        paramPane.getChildren().addAll(xyzLabel, grid, groupLabel, flowPane, wlCheckBox);

        dialog.getDialogPane().setContent(paramPane);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            Double _x = convertToDouble(xField.getText());
            Double _y = convertToDouble(yField.getText());
            Double _z = convertToDouble(zField.getText());
            double x = 0.0;
            double y = 0.0;
            double z = 0.0;
            if (_x != null) {
                x = _x;
            }
            if (_y != null) {
                y = _y;
            }
            if (_z != null) {
                z = _z;
            }
            MapPartGroup toGroup = groups.get(groupChoiceBox.getValue());
            ArrayList<MapNode> collisionNodes = editor.getCollisionNodes(nodes, x, y, z, toGroup);
            if (! collisionNodes.isEmpty()) {
                Alert alert = new Alert(AlertType.CONFIRMATION, "Warning:\n    " + collisionNodes.size() + " nodes are place on nodes already existing.\n    Do you want to continue?", ButtonType.YES, ButtonType.NO);
                result = alert.showAndWait();
                if (! result.isPresent() || result.get() != ButtonType.YES) {
                    return;
                }
            }
            editor.duplicateAndMoveNodes(nodes, x, y, z, toGroup, wlCheckBox.isSelected());
        }
    }

    /**
     * 階段を作成するダイアログを開く.
     *
     * ノードを二つ指定して、標高が高いノード側のグループにリンクを作る
     */
    public void openMakeStairsDialog() {
        ArrayList<MapNode> nodes = editor.getSelectedNodes();
        if (nodes.size() != 2) {
            Alert alert = new Alert(AlertType.WARNING, "Select only 2 nodes.", ButtonType.OK);
            alert.showAndWait();
            return;
        }
        if (nodes.get(0).getParent() == nodes.get(1).getParent()) {
            Alert alert = new Alert(AlertType.WARNING, "Can not make stairs in the same group.", ButtonType.OK);
            alert.showAndWait();
            return;
        }

        int fromIndex = 0;
        int toIndex = 1;
        if (nodes.get(0).getHeight() < nodes.get(1).getHeight()) {
            fromIndex = 1;
            toIndex = 0;
        }
        final MapNode fromNode = nodes.get(fromIndex);
        final MapNode toNode = nodes.get(toIndex);

        Dialog dialog = new Dialog();
        dialog.setTitle("Make stairs");
        VBox paramPane = new VBox();

        Label label = new Label("Make stairs between " + ((MapPartGroup)fromNode.getParent()).getTagString() + " and " + ((MapPartGroup)toNode.getParent()).getTagString());
        label.setFont(Font.font("Arial", FontWeight.BOLD, label.getFont().getSize()));
        label.setPadding(new Insets(0, 0, 12, 0));

        Label lengthLabel = new Label("Length");
        lengthLabel.setFont(Font.font("Arial", FontWeight.BOLD, lengthLabel.getFont().getSize()));
        TextField lengthField = new TextField("" + 1.0);
        lengthField.setPrefWidth(160);

        Button calcLengthButton = new Button("Calc length");
        CheckBox heightDiffCheckBox = new CheckBox("Reflect the height difference");
        calcLengthButton.setOnAction(e -> {
            double distance = fromNode.getAbsoluteCoordinates().distance(toNode.getAbsoluteCoordinates()) * editor.getCurrentGroup().getScale();
            if (heightDiffCheckBox.isSelected()) {
                Point3D point0 = new Point3D(fromNode.getX(), fromNode.getY(), fromNode.getHeight());
                Point3D point1 = new Point3D(toNode.getX(), toNode.getY(), toNode.getHeight());
                distance = point0.distance(point1) * editor.getCurrentGroup().getScale();
            }
            lengthField.setText("" + distance);
        });

        Label widthLabel = new Label("Width");
        widthLabel.setFont(Font.font("Arial", FontWeight.BOLD, widthLabel.getFont().getSize()));
        TextField widthField = new TextField("" + 1.0);
        widthField.setPrefWidth(160);

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.add(lengthLabel, 1, 1);
        grid.add(lengthField, 2, 1);
        grid.add(calcLengthButton, 3, 1);
        grid.add(heightDiffCheckBox, 4, 1);
        grid.add(widthLabel, 1, 2);
        grid.add(widthField, 2, 2);

        paramPane.getChildren().addAll(label, grid);

        dialog.getDialogPane().setContent(paramPane);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            Double length = convertToDouble(lengthField.getText());
            Double width = convertToDouble(widthField.getText());
            if (length != null && width != null) {
                if (length <= 0.0 || width <= 0.0) {
                    Alert alert = new Alert(AlertType.WARNING, "Invalid value.", ButtonType.OK);
                    alert.showAndWait();
                    return;
                }
                editor.startOfCommandBlock();
                if (editor.invoke(new AddLink(fromNode, toNode, length, width))) {
                    MapLink link = fromNode.connectedTo(toNode);
                    editor.invoke(new AddTag(link, "GENERATED_STAIR"));
                }
                editor.endOfCommandBlock();
            }
        }
    }

    /**
     * 複数ノードの拡大縮小と回転ダイアログを開く
     */
    public void openRotateAndScaleNodesDialog() {
        ArrayList<MapNode> nodes = editor.getSelectedNodes();
        if (nodes.size() < 2) {
            return;
        }
        if (! editor.isSingleGroup(nodes)) {
            Alert alert = new Alert(AlertType.WARNING, "Nodes of multiple groups were selected.", ButtonType.OK);
            alert.showAndWait();
            return;
        }

        Dialog dialog = new Dialog();
        dialog.setTitle("Rotate and Scale");
        dialog.getDialogPane().setPrefWidth(360);
        VBox paramPane = new VBox();

        if (nodes.size() > 1) {
            Label label = new Label("" + nodes.size() + " nodes selected");
            label.setPadding(new Insets(0, 0, 12, 0));
            paramPane.getChildren().addAll(label);
        }

        Label scaleXLabel = new Label("Scale X");
        scaleXLabel.setFont(Font.font("Arial", FontWeight.BOLD, scaleXLabel.getFont().getSize()));
        TextField scaleXField = new TextField("" + 1.0);
        scaleXField.setPrefWidth(100);

        Label scaleYLabel = new Label("Scale Y");
        scaleYLabel.setFont(Font.font("Arial", FontWeight.BOLD, scaleYLabel.getFont().getSize()));
        TextField scaleYField = new TextField("" + 1.0);
        scaleYField.setPrefWidth(100);

        Label angleLabel = new Label("Rotation angle (0.0 .. 360.0)");
        angleLabel.setFont(Font.font("Arial", FontWeight.BOLD, angleLabel.getFont().getSize()));
        TextField angleField = new TextField("" + 0.0);
        angleField.setPrefWidth(100);

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.add(scaleXLabel, 1, 1);
        grid.add(scaleXField, 2, 1);
        grid.add(scaleYLabel, 1, 2);
        grid.add(scaleYField, 2, 2);
        grid.add(angleLabel, 1, 3);
        grid.add(angleField, 2, 3);

        paramPane.getChildren().add(grid);

        dialog.getDialogPane().setContent(paramPane);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            Double _scaleX = convertToDouble(scaleXField.getText());
            Double _scaleY = convertToDouble(scaleYField.getText());
            Double _angle = convertToDouble(angleField.getText());
            double scaleX = 1.0;
            double scaleY = 1.0;
            double angle = 0.0;
            if (_scaleX != null) {
                scaleX = _scaleX;
            }
            if (_scaleY != null) {
                scaleY = _scaleY;
            }
            if (_angle != null) {
                angle = _angle;
            }

            StringBuilder buff = new StringBuilder();
            if (scaleX <= 0.0) {
                buff.append("Scale X : " + scaleX);
                buff.append("\n");
            }
            if (scaleY <= 0.0) {
                buff.append("Scale Y : " + scaleY);
                buff.append("\n");
            }
            if (angle < 0.0 || angle > 360.0) {
                buff.append("Rotation angle : " + angle);
                buff.append("\n");
            }
            if (buff.length() > 0) {
                alertInvalidInputValue(buff.toString());
                return;
            }

            if (scaleX != 1.0 || scaleY != 1.0 || (angle > 0.0 && angle < 360.0)) {
                editor.rotateAndScaleNodes(nodes, scaleX, scaleY, angle);
            }
        }
    }

    /**
     * ノードのシンボリックリンクを追加する
     */
    public void addSymbolicLinkOfNode(MapPartGroup group) {
        ArrayList<MapNode> nodes = editor.getSelectedNodes();
        if (nodes.isEmpty()) {
            return;
        }
        MapNodeTable groupNodes = group.getChildNodes();
        for (MapNode node : nodes) {
            if (groupNodes.contains(node)) {
                Alert alert = new Alert(AlertType.WARNING, "Can not symbolic link to own group.", ButtonType.OK);
                alert.showAndWait();
                return;
            }
        }
        editor.addSymbolicLink(nodes, group);
    }

    /**
     * リンクのシンボリックリンクを追加する
     */
    public void addSymbolicLinkOfLink(MapPartGroup group) {
        MapLinkTable links = editor.getSelectedLinks();
        if (links.isEmpty()) {
            return;
        }
        MapLinkTable groupLinks = group.getChildLinks();
        for (MapLink link : links) {
            if (groupLinks.contains(link)) {
                Alert alert = new Alert(AlertType.WARNING, "Can not symbolic link to own group.", ButtonType.OK);
                alert.showAndWait();
                return;
            }
        }
        editor.addSymbolicLink(links, group);
    }

    /**
     * リンク設定ダイアログを開く
     */
    public void openLinkAttributesDialog() {
        MapLinkTable links = editor.getSelectedLinks();
        if (links.isEmpty()) {
            return;
        }
        if (! multipleGroupConfirmation(links, "Warning:\n    Links of multiple groups were selected.\n    Do you want to continue?")) {
            return;
        }

        double averageLength = 0.0;
        double averageWidth = 0.0;
        for (MapLink link : links) {
            averageLength += link.getLength();
            averageWidth += link.getWidth();
        }
        averageLength /= links.size();
        averageWidth /= links.size();

        Dialog dialog = new Dialog();
        dialog.setTitle("Set link attributes");
        dialog.getDialogPane().setPrefWidth(512);
        VBox paramPane = new VBox();

        if (links.size() > 1) {
            Label label = new Label("" + links.size() + " links selected");
            label.setPadding(new Insets(0, 0, 12, 0));
            paramPane.getChildren().addAll(label);
        }

        Label label = new Label("Parameters");
        label.setFont(Font.font("Arial", FontWeight.BOLD, label.getFont().getSize()));
        label.setPadding(new Insets(0, 0, 8, 0));

        // length field
        Label lengthLabel = new Label("length");
        lengthLabel.setPadding(new Insets(0, 0, 0, 4));
        TextField lengthField = new TextField("" + averageLength);
        lengthField.setMinWidth(160);
        Button calcLengthButton = new Button("Calc length");
        CheckBox heightDiffCheckBox = new CheckBox("Reflect height");
        calcLengthButton.setDisable(links.size() != 1);
        calcLengthButton.setOnAction(e -> {
            MapLink link = links.get(0);
            MapNode fromNode = link.getFrom();
            MapNode toNode = link.getTo();
            double distance = fromNode.getAbsoluteCoordinates().distance(toNode.getAbsoluteCoordinates()) * editor.getCurrentGroup().getScale();
            if (heightDiffCheckBox.isSelected()) {
                Point3D point0 = new Point3D(fromNode.getX(), fromNode.getY(), fromNode.getHeight());
                Point3D point1 = new Point3D(toNode.getX(), toNode.getY(), toNode.getHeight());
                distance = point0.distance(point1) * editor.getCurrentGroup().getScale();
            }
            lengthField.setText("" + distance);
        });
        Button lengthButton = new Button("Set");
        EventHandler lengthHandler = new EventHandler<ActionEvent>() {
            public void handle(ActionEvent event) {
                Double value = convertToDouble(lengthField.getText());
                if (value != null) {
                    double length = value;
                    editor.startOfCommandBlock();
                    for (MapLink link : links) {
                        if (! editor.invoke(new SetLength(link, length))) {
                            break;
                        }
                    }
                    editor.endOfCommandBlock();
                    dialog.close();
                }
            }
        };
        lengthField.setOnAction(lengthHandler);
        lengthButton.setOnAction(lengthHandler);
        FlowPane lengthFlowPane = new FlowPane();
        lengthFlowPane.setHgap(8);
        lengthFlowPane.setPadding(new Insets(0, 0, 8, 0));
        lengthFlowPane.getChildren().addAll(lengthLabel, lengthField, calcLengthButton, heightDiffCheckBox, lengthButton);

        // width field
        Label widthLabel = new Label("width");
        widthLabel.setPadding(new Insets(0, 0, 0, 4));
        TextField widthField = new TextField("" + averageWidth);
        widthField.setMinWidth(160);
        Button widthButton = new Button("Set");
        EventHandler widthHandler = new EventHandler<ActionEvent>() {
            public void handle(ActionEvent event) {
                Double value = convertToDouble(widthField.getText());
                if (value != null) {
                    double width = value;
                    editor.startOfCommandBlock();
                    for (MapLink link : links) {
                        if (! editor.invoke(new SetWidth(link, width))) {
                            break;
                        }
                    }
                    editor.endOfCommandBlock();
                    dialog.close();
                }
            }
        };
        widthField.setOnAction(widthHandler);
        widthButton.setOnAction(widthHandler);
        FlowPane widthFlowPane = new FlowPane();
        widthFlowPane.setHgap(8);
        widthFlowPane.getChildren().addAll(widthLabel, widthField, widthButton);

        Separator separator = new Separator();
        separator.setPadding(new Insets(8, 0, 8, 0));
        TagSetupPane pane = new TagSetupPane(editor, links, dialog);
        paramPane.getChildren().addAll(label, lengthFlowPane, widthFlowPane, separator, pane);

        dialog.getDialogPane().setContent(paramPane);
        ButtonType cancel = new ButtonType("Cancel", ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().add(cancel);
        dialog.showAndWait();
    }

    /**
     * スケール計算とリンク長再計算ダイアログを開く
     */
    public void openCalculateScaleDialog() {
        if (editor.getCountOfSelectedLinks() != 1) {
            Alert alert = new Alert(AlertType.WARNING, "Please select only one link for calculation.", ButtonType.OK);
            alert.showAndWait();
            return;
        }

        MapPartGroup group = editor.getCurrentGroup();
        MapLink link = editor.getSelectedLinks().get(0);
        double actualDistance = link.getFrom().getAbsoluteCoordinates().distance(link.getTo().getAbsoluteCoordinates());

        Dialog dialog = new Dialog();
        dialog.setTitle("Calculate scale and recalculate link length");
        dialog.getDialogPane().setPrefWidth(440);
        VBox paramPane = new VBox();

        Label scaleLabel = new Label("Current scale of this group");
        Label scaleValue = new Label("" + group.getScale());
        Label distanceLabel = new Label("Actual distance between nodes");
        Label distanceValue = new Label("" + actualDistance);
        Label lengthLabel = new Label("Link length");
        Label lengthValue = new Label("" + link.getLength());

        Label requiredLengthLabel = new Label("Required link length");
        requiredLengthLabel.setFont(Font.font("Arial", FontWeight.BOLD, requiredLengthLabel.getFont().getSize()));
        TextField requiredLengthField = new TextField("" + link.getLength());
        requiredLengthField.setPrefWidth(170);

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(0, 0, 16, 0));
        grid.setHgap(8);
        grid.setVgap(8);
        grid.add(scaleLabel, 1, 1);
        grid.add(scaleValue, 2, 1);
        grid.add(distanceLabel, 1, 2);
        grid.add(distanceValue, 2, 2);
        grid.add(lengthLabel, 1, 3);
        grid.add(lengthValue, 2, 3);
        grid.add(requiredLengthLabel, 1, 4);
        grid.add(requiredLengthField, 2, 4);

        paramPane.getChildren().add(grid);

        CheckBox recalcLengthCheckBox = new CheckBox("Recalculate link lengths with new scale");
        recalcLengthCheckBox.setPadding(new Insets(8, 0, 0, 8));
        recalcLengthCheckBox.setFont(Font.font("Arial", FontWeight.BOLD, recalcLengthCheckBox.getFont().getSize()));
        recalcLengthCheckBox.setSelected(true);

        CheckBox updateGroupCheckBox = new CheckBox("Update all groups");
        updateGroupCheckBox.setPadding(new Insets(8, 0, 0, 8));
        updateGroupCheckBox.setFont(Font.font("Arial", FontWeight.BOLD, updateGroupCheckBox.getFont().getSize()));
        updateGroupCheckBox.setSelected(true);

        paramPane.getChildren().addAll(recalcLengthCheckBox, updateGroupCheckBox);

        dialog.getDialogPane().setContent(paramPane);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            Double _requiredLength = convertToDouble(requiredLengthField.getText());
            if (_requiredLength == null || _requiredLength <= 0.0) {
                return;
            }
            double scale = _requiredLength / actualDistance;

            StringBuilder buff = new StringBuilder("Scale : ");
            buff.append(scale);
            buff.append("\n");
            if (recalcLengthCheckBox.isSelected()) {
                buff.append("Recalculate link lengths.\n");
            } else {
                buff.append("Do not recalculate link lengths.\n");
            }
            if (updateGroupCheckBox.isSelected()) {
                buff.append("Update all groups.\n");
            } else {
                buff.append("Update current group.\n");
            }
            buff.append("\nDo you want to continue?");

            Alert alert = new Alert(AlertType.CONFIRMATION, buff.toString(), ButtonType.YES, ButtonType.NO);
            result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.YES) {
                editor.setScaleAndRecalculateLinkLength(group, scale, recalcLengthCheckBox.isSelected(), updateGroupCheckBox.isSelected());
            }
        }
    }

    /**
     * 選択中のリンクを一方通行に設定する。
     */
    public void setOneWay() {
        MapLinkTable seriesLinks = null;
        MapNode firstNode = null;
        MapNode lastNode = null;
        MapNode enteringNode = null;

        // 選択中のリンクを連続したリンクに変換する
        try {
            HashMap result = editor.getSeriesLinks(editor.getSelectedLinks());
            seriesLinks = (MapLinkTable)result.get("linkTable");
            firstNode = (MapNode)result.get("firstNode");
            lastNode = (MapNode)result.get("lastNode");
        } catch(Exception e) {
            Alert alert = new Alert(AlertType.WARNING, e.getMessage(), ButtonType.OK);
            alert.showAndWait();
            return;
        }

        // 両端のノードに "A", "B" ラベルを表示する
        TextPosition positionA = editor.getClearPosition(seriesLinks.get(0), firstNode);
        TextPosition positionB = editor.getClearPosition(seriesLinks.get(seriesLinks.size() - 1), lastNode);
        canvas.setOneWayIndicator(true, firstNode, positionA, lastNode, positionB);
        canvas.repaintLater();

        // 方向を選択する
        ArrayList<String> directions = new ArrayList();
        directions.add("A -> B");
        directions.add("B -> A");
        ChoiceDialog<String> dialog = new ChoiceDialog<String>(directions.get(0), directions);
        dialog.setTitle("One-way setting");
        dialog.setHeaderText("Please select one-way direction");
        String direction = dialog.showAndWait().orElse("");
        if (direction.equals("A -> B")) {
            enteringNode = firstNode;
        } else if (direction.equals("B -> A")) {
            Collections.reverse(seriesLinks);
            enteringNode = lastNode;
        } else {
            canvas.setOneWayIndicator(false, null, TextPosition.CENTER, null, TextPosition.CENTER);
            canvas.repaintLater();
            return;
        }

        // リンクに一方通行タグを振る
        editor.startOfCommandBlock();
        for (MapLink link : seriesLinks) {
            if (link.isForwardDirectionFrom(enteringNode)) {
                if (! editor.invoke(new SetTrafficRestriction(link, true, false, false))) {
                    break;
                }
            } else {
                if (! editor.invoke(new SetTrafficRestriction(link, false, true, false))) {
                    break;
                }
            }
            enteringNode = link.getOther(enteringNode);
        }

        canvas.setOneWayIndicator(false, null, TextPosition.CENTER, null, TextPosition.CENTER);
        editor.deselectLinks();
        editor.endOfCommandBlock();
    }

    /**
     * エリア設定ダイアログを開く
     */
    public void openAreaAttributesDialog() {
        ArrayList<MapArea> areas = editor.getSelectedAreas();
        if (areas.isEmpty()) {
            return;
        }
        if (! multipleGroupConfirmation(areas, "Warning:\n    Areas of multiple groups were selected.\n    Do you want to continue?")) {
            return;
        }

        Dialog dialog = new Dialog();
        dialog.setTitle("Set area attributes");
        VBox paramPane = new VBox();

        if (areas.size() > 1) {
            Label label = new Label("" + areas.size() + " areas selected");
            label.setPadding(new Insets(0, 0, 12, 0));
            paramPane.getChildren().add(label);
        }

        Separator separator = new Separator();
        separator.setPadding(new Insets(8, 0, 8, 0));
        TagSetupPane pane = new TagSetupPane(editor, areas, dialog);
        paramPane.getChildren().add(pane);

        dialog.getDialogPane().setContent(paramPane);
        ButtonType cancel = new ButtonType("Cancel", ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().add(cancel);
        dialog.showAndWait();
    }

    /**
     * 背景画像をセットする
     */
    public void setBackgroundImage() {
        Point2D point = canvas.getMapPointOnTheMouseCursor();
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open background image file");
        fileChooser.setInitialDirectory(editor.getDir());
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Image", "*.bmp", "*.gif", "*.jpg", "*.png"),
            new FileChooser.ExtensionFilter("All", "*.*")
        );
        File file = fileChooser.showOpenDialog(frame);
        if (file != null) {
            try {
                if (! file.getParentFile().getCanonicalPath().equals(editor.getPath())) {
                    Alert alert = new Alert(AlertType.WARNING, "Directory can not be changed.", ButtonType.OK);
                    alert.showAndWait();
                    return;
                }
            } catch (IOException ex) {
                ex.printStackTrace();
                return;
            }
            MapPartGroup group = editor.getCurrentGroup();
            editor.invokeSingleCommand(new SetBackgroundImage(group, file.getName(), point.getX(), point.getY()));
        }
    }

    /**
     * 背景画像設定ダイアログを開く
     */
    public void openBackgroundImageAttributesDialog() {
        MapPartGroup group = editor.getCurrentGroup();
        Dialog dialog = new Dialog();
        dialog.setTitle("Set background image attributes");
        VBox paramPane = new VBox();

        Label label = new Label("Parameters");
        label.setFont(Font.font("Arial", FontWeight.BOLD, label.getFont().getSize()));
        label.setPadding(new Insets(0, 0, 8, 0));

        // sx
        Label scaleXLabel = new Label("Scale X");
        scaleXLabel.setFont(Font.font("Arial", FontWeight.BOLD, scaleXLabel.getFont().getSize()));
        TextField scaleXField = new TextField("" + group.sx);
        scaleXField.setPrefWidth(128);
        Button scaleXButton = new Button("Set");
        EventHandler scaleXHandler = new EventHandler<ActionEvent>() {
            public void handle(ActionEvent event) {
                Double value = convertToDouble(scaleXField.getText());
                if (value != null) {
                    double scaleX = value;
                    if (scaleX <= 0.0) {
                        alertInvalidInputValue("Scale X : " + scaleX);
                        return;
                    }
                    if (scaleX != group.sx) {
                        editor.invokeSingleCommand(new ScaleTheBackgroundImage(group, scaleX, group.sy));
                    }
                    dialog.close();
                }
            }
        };
        scaleXField.setOnAction(scaleXHandler);
        scaleXButton.setOnAction(scaleXHandler);

        // sy
        Label scaleYLabel = new Label("Scale Y");
        scaleYLabel.setFont(Font.font("Arial", FontWeight.BOLD, scaleYLabel.getFont().getSize()));
        TextField scaleYField = new TextField("" + group.sy);
        scaleYField.setPrefWidth(128);
        Button scaleYButton = new Button("Set");
        EventHandler scaleYHandler = new EventHandler<ActionEvent>() {
            public void handle(ActionEvent event) {
                Double value = convertToDouble(scaleYField.getText());
                if (value != null) {
                    double scaleY = value;
                    if (scaleY <= 0.0) {
                        alertInvalidInputValue("Scale Y : " + scaleY);
                        return;
                    }
                    if (scaleY != group.sy) {
                        editor.invokeSingleCommand(new ScaleTheBackgroundImage(group, group.sx, scaleY));
                    }
                    dialog.close();
                }
            }
        };
        scaleYField.setOnAction(scaleYHandler);
        scaleYButton.setOnAction(scaleYHandler);

        // r
        Label angleLabel = new Label("Rotation angle");
        angleLabel.setFont(Font.font("Arial", FontWeight.BOLD, angleLabel.getFont().getSize()));
        TextField angleField = new TextField("" + group.r);
        angleField.setPrefWidth(128);
        Button angleButton = new Button("Set");
        EventHandler angleHandler = new EventHandler<ActionEvent>() {
            public void handle(ActionEvent event) {
                Double value = convertToDouble(angleField.getText());
                if (value != null) {
                    double angle = value;
                    if (angle < 0.0 || angle > 360.0) {
                        alertInvalidInputValue("Rotation angle : " + angle);
                        return;
                    }
                    if (angle == 360.0) {
                        angle = 0.0;
                    }
                    if (angle != group.r) {
                        editor.invokeSingleCommand(new RotateBackgroundImage(group, angle * Math.PI / 180.0));
                    }
                    dialog.close();
                }
            }
        };
        angleField.setOnAction(angleHandler);
        angleButton.setOnAction(angleHandler);

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(10);
        grid.add(scaleXLabel, 1, 1);
        grid.add(scaleXField, 2, 1);
        grid.add(scaleXButton, 3, 1);
        grid.add(scaleYLabel, 1, 2);
        grid.add(scaleYField, 2, 2);
        grid.add(scaleYButton, 3, 2);
        grid.add(angleLabel, 1, 3);
        grid.add(angleField, 2, 3);
        grid.add(angleButton, 3, 3);

        paramPane.getChildren().addAll(label, grid);

        dialog.getDialogPane().setContent(paramPane);
        ButtonType cancel = new ButtonType("Cancel", ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().add(cancel);
        dialog.showAndWait();
    }

    /**
     * 背景画像を削除する
     */
    public void removeBackgroundImage() {
        Alert alert = new Alert(AlertType.CONFIRMATION, "Warning:\n    Do you really want to delete?", ButtonType.YES, ButtonType.NO);
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.YES) {
            editor.invokeSingleCommand(new RemoveBackgroundImage(editor.getCurrentGroup()));
        }
    }

    /**
     * Invalid input value ダイアログを表示する
     */
    public void alertInvalidInputValue(String message) {
        Alert alert = new Alert(AlertType.WARNING, message, ButtonType.OK);
        alert.getDialogPane().setHeaderText("Invalid input value");
        alert.showAndWait();
    }

    /**
     * 文字列を検証して整数に変換する
     */
    public Integer convertToInteger(String str) {
        try {
            return Integer.valueOf(str);
        } catch(NumberFormatException e) {
            alertInvalidInputValue(e.getMessage());
        }
        return null;
    }

    /**
     * 文字列を検証して実数に変換する
     */
    public Double convertToDouble(String str) {
        try {
            return Double.valueOf(str);
        } catch(NumberFormatException e) {
            alertInvalidInputValue(e.getMessage());
        }
        return null;
    }
    
    /**
     * ファイル選択ダイアログの初期パスを設定する
     */
    public void setInitialPath(FileChooser fileChooser, String path) {
        if (path == null || path.isEmpty()) {
            fileChooser.setInitialDirectory(new File("./"));
        } else {
            File file = new File(path);
            fileChooser.setInitialDirectory(file.getParentFile());
            fileChooser.setInitialFileName(file.getName());     // TODO: 現状では無効
        }
    }

    /**
     * ウィンドウを表示する
     */
    public void show() {
        frame.show();
    }

    /**
     * ステータスラインにメッセージを表示する
     */
    public void setStatusText(String text) {
        statusLabel.setText(text);
    }

    /**
     * BACKGROUND_IMAGE モードのコンテキスト・メニューの有効状態を設定する
     */
    public void setBgImageMenuStatus() {
        boolean imageExisting = (editor.getBackgroundImage(editor.getCurrentGroup()) != null);
        miSetBackgroundImage.setDisable(imageExisting);
        miSetBackgroundImageAttributes.setDisable(! imageExisting);
        miRemoveBackgroundImage.setDisable(! imageExisting);
    }

    /**
     * グループボタンをクリックする
     */
    public void clickGroupButton(MapPartGroup group) {
        groupButtonMap.get(group).fire();
    }

    /**
     * グループ表示を更新する
     */
    public void updateGroupTag(MapPartGroup group) {
        ToggleButton groupButton = groupButtonMap.get(group);
        groupButton.setText(group.getTagString());
    }

    public Stage getStage() {
        return frame;
    }

    public EditorCanvas getCanvas() {
        return canvas;
    }

    public GroupPanel getGroupPanel() {
        return groupPanel;
    }

    public NodePanelFx getNodePanel() {
        return nodePanel;
    }

    public LinkPanelFx getLinkPanel() {
        return linkPanel;
    }

    public AreaPanelFx getAreaPanel() {
        return areaPanel;
    }

    public ScenarioPanelFx getScenarioPanel() {
        return scenarioPanel;
    }

    public ContextMenu getEditNodeMenu() {
        return editNodeMenu;
    }

    public ContextMenu getEditLinkMenu() {
        return editLinkMenu;
    }

    public ContextMenu getEditAreaMenu() {
        return editAreaMenu;
    }

    public ContextMenu getBgImageMenu() {
        return bgImageMenu;
    }
}