/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package appayuda;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.concurrent.Worker.State;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.TextFormatter.Change;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.scene.web.*;
import javafx.scene.web.WebHistory.Entry;
import javafx.util.Callback;
import netscape.javascript.JSObject;

/**
 *
 * @author rafae
 */
public class AppAyuda extends Application {

    private Scene scene;

    @Override
    public void start(Stage stage) {
        stage.setTitle("Web View");
        scene = new Scene(new Browser(), 750, 500, Color.web("#666970"));
        stage.setScene(scene);
        stage.show();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

}

class Browser extends Region {

    private HBox toolBar;
    private static String[] imageFiles = new String[]{
        "facebook.png",
        "moodle.png",
        "documentacion.png",
        "twitter.png",
        "help.png"
    };
    private static String[] captions = new String[]{
        "Facebook",
        "Moodle",
        "Documentation",
        "Twitter",
        "Help"
    };
    private static String[] urls = new String[]{
        "http://www.facebook.com/",
        "http://www.ieslosmontecillos.es/moodle/my/",
        "http://docs.oracle.com/javase/index.html",
        "http://www.twitter.com/",
        AppAyuda.class.getResource("help.html").toExternalForm()
    };

    final ImageView selectedImage = new ImageView();
    final Hyperlink[] hpls = new Hyperlink[captions.length];
    final Image[] images = new Image[imageFiles.length];
    private boolean needDocumentationButton = false;

    final WebView browser = new WebView();
    final WebEngine webEngine = browser.getEngine();

    final Button showPrevDoc = new Button("Toggle Previous Docs");
    final WebView smallView = new WebView();
    
    

    public Browser() {
        //apply the styles
        getStyleClass().add("browser");
        for (int i = 0; i < captions.length; i++) {
            Hyperlink hpl = hpls[i] = new Hyperlink(captions[i]);
            Image image = images[i] = new Image(getClass().getResourceAsStream(imageFiles[i]));
            hpl.setGraphic(new ImageView(image));
            final String url = urls[i];
            final boolean addButton = (hpl.getText().equals("Documentacion"));
            //proccess event
            hpl.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent e) {
                    needDocumentationButton = addButton;
                    webEngine.load(url);
                }
            });
        }

        webEngine.load("http://docs.oracle.com/javase/index.html");
        

        // create the toolbar
        toolBar = new HBox();
        toolBar.setPadding(new Insets(30, 0, 0, 350));
        toolBar.setAlignment(Pos.CENTER);
        toolBar.getStyleClass().add("browser-toolbar");
        
        
        
        // habrá que definir la combo como propiedad de la clase Brower
        final ComboBox comboBox = new ComboBox();
        //En el constructor de la clase Browser damos formato al combobox y lo
        //incluimos en la toolbar
        comboBox.setPrefWidth(60);
        toolBar.getChildren().add(comboBox);
        toolBar.getChildren().addAll(hpls);
        toolBar.getChildren().add(createSpacer());
        //también el constructor de la clase Browser declaramos el manejador
        //del histórico
        final WebHistory history = webEngine.getHistory();
        history.getEntries().addListener(new ListChangeListener<WebHistory.Entry>() {
            @Override
            public void onChanged(Change<? extends Entry> c) {
                c.next();
                for (Entry e : c.getRemoved()) {
                    comboBox.getItems().remove(e.getUrl());
                }
                for (Entry e : c.getAddedSubList()) {
                    comboBox.getItems().add(e.getUrl());
                }
            }
        });
        //Se define el comportamiento del combobox
        comboBox.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent ev) {
                int offset
                        = comboBox.getSelectionModel().getSelectedIndex()
                        - history.getCurrentIndex();
                history.go(offset);
            }
        });

        //set action for the button
        showPrevDoc.setOnAction(new EventHandler() {
            @Override
            public void handle(Event event) {
                webEngine.executeScript("toggleDisplay('PrevRel')");
            }
        });

        smallView.setPrefSize(120, 80);
        //handle popup windows
        webEngine.setCreatePopupHandler(
                new Callback<PopupFeatures, WebEngine>() {
            @Override
            public WebEngine call(PopupFeatures config) {
                smallView.setFontScale(0.8);
                if (!toolBar.getChildren().contains(smallView)) {
                    toolBar.getChildren().add(smallView);
                }
                return smallView.getEngine();
            }
        }
        );

        // process page loading
        webEngine.getLoadWorker().stateProperty().addListener(new ChangeListener<State>() {
            @Override
            public void changed(ObservableValue<? extends State> ov, State oldState, State newState) {
                toolBar.getChildren().remove(showPrevDoc);
                if (newState == State.SUCCEEDED) {
                    JSObject win
                            = (JSObject) webEngine.executeScript("window");
                    win.setMember("app", new JavaApp());
                    if (needDocumentationButton) {
                        toolBar.getChildren().add(showPrevDoc);
                    }
                }
            }
        }
        );

        // load the web page
        webEngine.load("http://docs.oracle.com/javase/index.html");
       
        
        //add components
        getChildren().add(toolBar);
        //add the web view to the scene
        getChildren().add(browser); 
    }

    // JavaScript interface object
    public class JavaApp {

        public void exit() {
            Platform.exit();
        }
    }

    private Node createSpacer() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    @Override
    protected void layoutChildren() {
        double w = getWidth();
        double h = getHeight();
        layoutInArea(browser, 0, 50, w, h, 0, HPos.CENTER, VPos.CENTER);
    }

    @Override
    protected double computePrefWidth(double height) {
        return 750;
    }

    @Override
    protected double computePrefHeight(double width) {
        return 500;
    }
}
