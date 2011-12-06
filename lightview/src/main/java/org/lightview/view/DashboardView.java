package org.lightview.view;

import javafx.collections.MapChangeListener;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.HBoxBuilder;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.lightview.presenter.ConnectionPoolBindings;
import org.lightview.presenter.DashboardPresenterBindings;

/**
 * User: blog.adam-bien.com
 * Date: 18.11.11
 * Time: 17:19
 */
public class DashboardView{

    DashboardPresenterBindings dashboardPresenter;
    Stage stage;
    private TextField txtUri;
    private BrowserView browserView;
    private SnapshotView heapView;
    private SnapshotView threadCountView;
    private SnapshotView busyThreadView;
    private SnapshotView queuedConnectionsView;
    private VBox vertical;
    private SnapshotView commitCountView;
    private SnapshotView rollbackCountView;
    private SnapshotView totalErrorsView;
    private GridView gridView;
    private TabPane tabPane;
    private SnapshotView peakThreadCount;
    private Node uriInputView;

    public DashboardView(Stage stage,DashboardPresenterBindings dashboardPresenter) {
        this.dashboardPresenter = dashboardPresenter;
        this.stage = stage;
        this.tabPane = new TabPane();
        this.createViews();
        this.open();
        this.bind();
    }

    private void bind() {
        this.dashboardPresenter.getUriProperty().bind(txtUri.textProperty());
        this.browserView.getUriProperty().bind(txtUri.textProperty());
        this.heapView.currentValue().bind(this.dashboardPresenter.getUsedHeapSizeInMB());

        this.threadCountView.currentValue().bind(this.dashboardPresenter.getThreadCount());
        this.busyThreadView.currentValue().bind(this.dashboardPresenter.getBusyThreads());
        this.peakThreadCount.currentValue().bind(this.dashboardPresenter.getPeakThreadCount());

        this.commitCountView.currentValue().bind(this.dashboardPresenter.getCommitCount());
        this.rollbackCountView.currentValue().bind(this.dashboardPresenter.getRollbackCount());

        this.queuedConnectionsView.currentValue().bind(this.dashboardPresenter.getQueuedConnections());
        this.totalErrorsView.currentValue().bind(this.dashboardPresenter.getTotalErrors());

        this.dashboardPresenter.getPools().addListener(new MapChangeListener<String, ConnectionPoolBindings>() {
            public void onChanged(Change<? extends String, ? extends ConnectionPoolBindings> change) {
                ConnectionPoolBindings valueAdded = change.getValueAdded();
                ConnectionPoolBindings valueRemoved = change.getValueRemoved();
                if(valueAdded != null)
                    createPoolTab(valueAdded);
            }
        });
    }

    private void createViews() {
        this.vertical = new VBox();
        HBox threadsAndMemory = new HBox();
        HBox paranormal = new HBox();
        HBox transactions = new HBox();

        String hBoxClass = "boxSpacing";
        this.vertical.getStyleClass().add(hBoxClass);
        threadsAndMemory.getStyleClass().add(hBoxClass);
        paranormal.getStyleClass().add(hBoxClass);
        transactions.getStyleClass().add(hBoxClass);

        instantiateViews();

        threadsAndMemory.getChildren().addAll(this.heapView.view(), this.threadCountView.view(), this.peakThreadCount.view());
        transactions.getChildren().addAll(this.commitCountView.view(), this.rollbackCountView.view());
        paranormal.getChildren().addAll(this.queuedConnectionsView.view(), this.totalErrorsView.view(), this.busyThreadView.view());

        Tab threadsAndMemoryTab = createTab(threadsAndMemory,"Threads And Memory");
        Tab transactionsTab = createTab(transactions, "Transactions");
        Tab paranormalTab = createTab(paranormal,"Paranormal Activity");
        this.tabPane.getTabs().addAll(threadsAndMemoryTab, transactionsTab, paranormalTab);

        this.vertical.getChildren().addAll(uriInputView, this.browserView.view(), this.tabPane, this.gridView.createTable());
    }

    private void instantiateViews() {
        this.uriInputView = createURIInputView();
        this.browserView = new BrowserView();

        this.heapView = new SnapshotView("Heap Size","Used Heap",null);
        this.threadCountView = new SnapshotView("Thread Count","Threads",null);
        this.peakThreadCount = new SnapshotView("Peak Thread Count", "Threads", null);
        this.busyThreadView = new SnapshotView("Busy Thread Count","Threads",null);
        this.commitCountView = new SnapshotView("TX Commit","#",null);
        this.rollbackCountView = new SnapshotView("TX Rollback","#",null);
        this.totalErrorsView = new SnapshotView("Errors","#",null);
        this.queuedConnectionsView = new SnapshotView("Queued Connections","Connections",null);
        this.gridView = new GridView(this.dashboardPresenter.getSnapshots());
    }


    private Tab createTab(Node content, String caption) {
        Tab tab = new Tab();
        tab.setContent(content);
        tab.setText(caption);
        return tab;
    }

    void createPoolTab(ConnectionPoolBindings valueAdded) {
        String jndiName = valueAdded.getJndiName().get();
        ConnectionPoolView connectionPoolView = new ConnectionPoolView(jndiName,valueAdded);
        Node view = connectionPoolView.view();
        Tab tab = createTab(view, "Resource: " + jndiName);
        this.tabPane.getTabs().add(tab);
    }

    private Node createURIInputView() {
        final Button button = new Button();
        button.setText("-");
        button.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent actionEvent) {
                toggleBrowserSize(button);
            }
        });
        HBox hBox = HBoxBuilder.create().spacing(10).build();
        this.txtUri = TextFieldBuilder.create().editable(true).text("http://localhost:8080/lightfish/live").prefColumnCount(40).minHeight(20).build();
        Label uri = LabelBuilder.create().labelFor(txtUri).text("LightFish location:").build();
        hBox.getChildren().addAll(uri, txtUri,button);
        return hBox;
    }

    private void toggleBrowserSize(Button button) {
        boolean minimized = this.browserView.toggleMinimize();
        if(minimized){
            button.setText("+");
        }else{
            button.setText("-");
        }
    }
    public void open(){
        Scene scene = new Scene(this.vertical);
        scene.getStylesheets().add(this.getClass().getResource("lightfish.css").toExternalForm());
        stage.setFullScreen(true);
        stage.setScene(scene);
        stage.show();
    }
}
