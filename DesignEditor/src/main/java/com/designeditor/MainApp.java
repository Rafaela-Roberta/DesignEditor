/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.designeditor;

import javafx.application.Application;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class MainApp extends Application {

    private Pane canvas;
    private ScrollPane scrollPane;

    // Selection
    private Node selectedNode = null;
    private Rectangle selectionBox = null;

    // Text controls 
    private ColorPicker textColorPicker;
    private Spinner<Integer> fontSizeSpinner;

    // Canvas background control
    private ColorPicker canvasBgPicker;

    @Override
    public void start(Stage stage) {

        BorderPane layout = new BorderPane();
        layout.setStyle("-fx-background-color: white;");

        // LEFT SIDEBAR
        VBox leftBar = new VBox(12);
        leftBar.setPadding(new Insets(14));
        leftBar.setPrefWidth(220);
        leftBar.setStyle(
                "-fx-background-color: white;" +
                "-fx-border-color: #e6e6e6;" +
                "-fx-border-width: 0 1 0 0;"
        );

        // Templates
        Label lbTemplates = labelTitle("Templates");

        MenuButton templatesMenu = new MenuButton("Choose size");
        MenuItem miBlank = new MenuItem("Blank (1000x700)");
        MenuItem miPost  = new MenuItem("Instagram Post (1080x1080)");
        MenuItem miStory = new MenuItem("Instagram Story (1080x1920)");
        MenuItem miFlyer = new MenuItem("Flyer (850x1100)");
        templatesMenu.getItems().addAll(miBlank, new SeparatorMenuItem(), miPost, miStory, miFlyer);

        styleMenuButton(templatesMenu);

        // Tools
        Label lbTools = labelTitle("Tools");

        Button btnText   = new Button("Text");
        Button btnUpload = new Button("Upload");
        Button btnDL     = new Button("Download PNG");

        styleButton(btnText);
        styleButton(btnUpload);
        styleButton(btnDL);

        // Text options under Text button 
        Label lbTextOptions = labelSmall("Text options");

        textColorPicker = new ColorPicker(Color.BLACK);
        textColorPicker.setMaxWidth(Double.MAX_VALUE);
        stylePicker(textColorPicker);

        fontSizeSpinner = new Spinner<>(8, 200, 36);
        fontSizeSpinner.setEditable(true);
        fontSizeSpinner.setMaxWidth(Double.MAX_VALUE);
        styleSpinner(fontSizeSpinner);

        // Canvas background option
        Label lbCanvasOptions = labelSmall("Canvas");
        canvasBgPicker = new ColorPicker(Color.WHITE);
        canvasBgPicker.setMaxWidth(Double.MAX_VALUE);
        stylePicker(canvasBgPicker);

        // AI box inside sidebar 
        VBox aiBox = buildAiBox();
        aiBox.setMaxWidth(Double.MAX_VALUE);

        // Put all sidebar items
        leftBar.getChildren().addAll(
                lbTemplates, templatesMenu,
                new Separator(),
                lbTools, btnText, btnUpload, btnDL,
                lbTextOptions, new Label("Pick your color"), textColorPicker, new Label("Font size"), fontSizeSpinner,
                lbCanvasOptions, new Label("Background color"), canvasBgPicker,
                new Separator(),
                aiBox
        );

        // CANVAS + SCROLL 
        canvas = new Pane();
        setCanvasSize(1000, 700);
        setCanvasBackground(canvasBgPicker.getValue());
        setCanvasBorder();

        // clear selection
        canvas.setOnMousePressed(e -> {
            if (e.getTarget() == canvas) clearSelection();
        });

        // Wrap canvas (padding around)
        StackPane canvasWrapper = new StackPane(canvas);
        canvasWrapper.setPadding(new Insets(20));
        canvasWrapper.setStyle("-fx-background-color: white;");
        StackPane.setAlignment(canvas, Pos.CENTER);

        scrollPane = new ScrollPane(canvasWrapper);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setPannable(true);
        scrollPane.setStyle("-fx-background: white; -fx-background-color: white;");
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        //ACTIONS 
        miBlank.setOnAction(e -> resetCanvas());
        miPost.setOnAction(e -> resizeCanvasAndGoTop(1080, 1080));
        miStory.setOnAction(e -> resizeCanvasAndGoTop(1080, 1920));
        miFlyer.setOnAction(e -> resizeCanvasAndGoTop(850, 1100));

        btnText.setOnAction(e -> addTextToCanvas("Edit me"));
        btnUpload.setOnAction(e -> System.out.println("Upload later"));
        btnDL.setOnAction(e -> System.out.println("Download later"));

        // Apply text controls when changed
        textColorPicker.setOnAction(e -> applyTextStyleToSelectedIfText());
        fontSizeSpinner.valueProperty().addListener((obs, oldV, newV) -> applyTextStyleToSelectedIfText());

        // Change canvas background
        canvasBgPicker.setOnAction(e -> setCanvasBackground(canvasBgPicker.getValue()));

        layout.setLeft(leftBar);
        layout.setCenter(scrollPane);

        Scene scene = new Scene(layout, 1300, 850);

        // Delete selected node
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.DELETE || e.getCode() == KeyCode.BACK_SPACE) {
                deleteSelected();
            }
        });

        stage.setTitle("Design Editor - MVP");
        stage.setScene(scene);
        stage.show();
    }

    // CANVAS HELPERS 
    private void setCanvasSize(double w, double h) {
        canvas.setPrefSize(w, h);
        canvas.setMinSize(w, h);
        canvas.setMaxSize(w, h);
    }

    private void setCanvasBorder() {
        // border only; background handled separately
        canvas.setStyle("-fx-border-color: #d0d0d0; -fx-border-width: 2;");
    }

    private void setCanvasBackground(Color c) {
        String hex = toHex(c);
        
        canvas.setStyle("-fx-background-color: " + hex + ";" +
                "-fx-border-color: #d0d0d0;" +
                "-fx-border-width: 2;");
    }

    private void resetCanvas() {
        setCanvasSize(1000, 700);
        canvas.getChildren().clear();
        clearSelection();
        scrollPane.setHvalue(0);
        scrollPane.setVvalue(0);
        canvasBgPicker.setValue(Color.WHITE);
        setCanvasBackground(Color.WHITE);
    }

    private void resizeCanvasAndGoTop(double w, double h) {
        setCanvasSize(w, h);
        clearSelection();
        scrollPane.setHvalue(0);
        scrollPane.setVvalue(0);
    }

    // TEXT 
    private void addTextToCanvas(String content) {
        Text t = new Text(content);
        t.setLayoutX(120);
        t.setLayoutY(160);

        // Use current controls as defaults
        t.setFill(textColorPicker.getValue());
        t.setFont(Font.font(fontSizeSpinner.getValue()));

        enableDragSelectAndEdit(t);

        // scroll over text changes font size
        t.addEventFilter(ScrollEvent.SCROLL, e -> {
            if (selectedNode == t) {
                int size = (int) t.getFont().getSize();
                int newSize = (int) Math.max(8, Math.min(200, size + (e.getDeltaY() > 0 ? 2 : -2)));
                fontSizeSpinner.getValueFactory().setValue(newSize); // keeps UI synced
                t.setFont(Font.font(newSize));
                updateSelectionBox();
                e.consume();
            }
        });

        canvas.getChildren().add(t);
        selectNode(t);
    }

    // Double-click edit the text edit 
    private void startInlineEdit(Text t) {
        TextField editor = new TextField(t.getText());
        editor.setStyle(
                "-fx-background-color: white;" +
                "-fx-border-color: #cfcfcf;" +
                "-fx-border-radius: 8;" +
                "-fx-background-radius: 8;"
        );

        // Position editor over the text
        Bounds b = t.getBoundsInParent();
        editor.setLayoutX(b.getMinX());
        editor.setLayoutY(b.getMinY() - 18);
        editor.setPrefWidth(Math.max(160, b.getWidth() + 40));

        
        editor.setFont(t.getFont());

        canvas.getChildren().add(editor);
        editor.toFront();
        editor.requestFocus();
        editor.selectAll();

        Runnable finish = () -> {
            t.setText(editor.getText());
            canvas.getChildren().remove(editor);
            updateSelectionBox();
        };

        editor.setOnAction(e -> finish.run());
        editor.focusedProperty().addListener((obs, was, is) -> {
            if (!is) finish.run();
        });
    }

    private void applyTextStyleToSelectedIfText() {
        if (selectedNode instanceof Text t) {
            t.setFill(textColorPicker.getValue());
            t.setFont(Font.font(fontSizeSpinner.getValue()));
            updateSelectionBox();
        }
    }

    // AI BOX
    private VBox buildAiBox() {
        VBox box = new VBox(8);
        box.setPadding(new Insets(12));

        box.setStyle(
                "-fx-background-color: white;" +
                "-fx-border-color: #cfcfcf;" +
                "-fx-border-radius: 16;" +
                "-fx-background-radius: 16;"
        );

        Label title = new Label("AI Image");
        title.setStyle("-fx-font-size: 13; -fx-font-weight: bold;");

        TextArea prompt = new TextArea();
        prompt.setPromptText("Write your prompt here...");
        prompt.setWrapText(true);
        prompt.setPrefRowCount(4);

        Button generate = new Button("Generate");
        generate.setMaxWidth(Double.MAX_VALUE);
        styleButtonGraphite(generate);

        // For now: insert placeholder rectangle (later I will replace with real ImageView when I connect the API 
        generate.setOnAction(e -> insertAiPlaceholder(prompt.getText()));

        box.getChildren().addAll(title, prompt, generate);
        return box;
    }

    private void insertAiPlaceholder(String promptText) {
        Rectangle r = new Rectangle(320, 220);
        r.setArcWidth(18);
        r.setArcHeight(18);
        r.setFill(Color.web("#f2f2f2"));
        r.setStroke(Color.web("#cfcfcf"));
        r.setStrokeWidth(2);

        double x = (canvas.getPrefWidth() - r.getWidth()) / 2.0;
        double y = (canvas.getPrefHeight() - r.getHeight()) / 2.0;
        r.setLayoutX(Math.max(10, x));
        r.setLayoutY(Math.max(10, y));

        enableDragAndSelect(r);
        canvas.getChildren().add(r);
        selectNode(r);

        // I will add subtitles later (Rafa) 
        System.out.println("AI prompt: " + (promptText == null ? "" : promptText.trim()));
    }

    //SELECTION + DELETE 
    private void selectNode(Node n) {
        selectedNode = n;

        // If selecting Text, sync controls to the selected text
        if (n instanceof Text t) {
            textColorPicker.setValue((Color) t.getFill());
            fontSizeSpinner.getValueFactory().setValue((int) t.getFont().getSize());
        }

        updateSelectionBox();
    }

    private void updateSelectionBox() {
        if (selectionBox != null) {
            canvas.getChildren().remove(selectionBox);
            selectionBox = null;
        }
        if (selectedNode == null) return;

        Bounds b = selectedNode.getBoundsInParent();
        selectionBox = new Rectangle(b.getMinX(), b.getMinY(), b.getWidth(), b.getHeight());
        selectionBox.setFill(Color.TRANSPARENT);
        selectionBox.setStroke(Color.web("#4a90e2"));
        selectionBox.setStrokeWidth(2);
        selectionBox.getStrokeDashArray().setAll(6.0, 6.0);
        selectionBox.setMouseTransparent(true);

        canvas.getChildren().add(selectionBox);
        selectionBox.toFront();
    }

    private void clearSelection() {
        selectedNode = null;
        if (selectionBox != null) {
            canvas.getChildren().remove(selectionBox);
            selectionBox = null;
        }
    }

    private void deleteSelected() {
        if (selectedNode == null) return;
        canvas.getChildren().remove(selectedNode);
        clearSelection();
    }

    // DRAG + SELECT + DOUBLE CLICK EDIT 
    private void enableDragSelectAndEdit(Text t) {
        final Delta d = new Delta();

        t.setOnMousePressed(e -> {
            selectNode(t);
            d.x = e.getSceneX() - t.getLayoutX();
            d.y = e.getSceneY() - t.getLayoutY();
            e.consume();
        });

        t.setOnMouseDragged(e -> {
            t.setLayoutX(e.getSceneX() - d.x);
            t.setLayoutY(e.getSceneY() - d.y);
            updateSelectionBox();
            e.consume();
        });

        t.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                startInlineEdit(t);
                e.consume();
            }
        });
    }

    private void enableDragAndSelect(Node n) {
        final Delta d = new Delta();

        n.setOnMousePressed(e -> {
            selectNode(n);
            d.x = e.getSceneX() - n.getLayoutX();
            d.y = e.getSceneY() - n.getLayoutY();
            e.consume();
        });

        n.setOnMouseDragged(e -> {
            n.setLayoutX(e.getSceneX() - d.x);
            n.setLayoutY(e.getSceneY() - d.y);
            updateSelectionBox();
            e.consume();
        });
    }

    private static class Delta { double x, y; }

    // STYLES 
    private void styleButton(Button b) {

        b.setMaxWidth(Double.MAX_VALUE);
        styleButtonGraphite(b);
    }

    private void styleButtonGraphite(Button b) {
        // graphite button
        b.setStyle(
                "-fx-background-color: #5C5C5C;" +
                "-fx-text-fill: white;" +
                "-fx-border-color: #5C5C5C;" +
                "-fx-border-radius: 12;" +
                "-fx-background-radius: 12;" +
                "-fx-padding: 10;" +
                "-fx-font-size: 13;"
        );
    }

    private void styleMenuButton(MenuButton b) {
        b.setMaxWidth(Double.MAX_VALUE);
        b.setStyle(
                "-fx-background-color: #5C5C5C;" +
                "-fx-text-fill: white;" +
                "-fx-border-color: #5C5C5C;" +
                "-fx-border-radius: 12;" +
                "-fx-background-radius: 12;" +
                "-fx-padding: 6 10;" +
                "-fx-font-size: 13;"
        );
    }

    private void stylePicker(ColorPicker p) {
        p.setStyle(
                "-fx-background-color: white;" +
                "-fx-border-color: #cfcfcf;" +
                "-fx-border-radius: 12;" +
                "-fx-background-radius: 12;"
        );
    }

    private void styleSpinner(Spinner<Integer> sp) {
        sp.setStyle(
                "-fx-background-color: white;" +
                "-fx-border-color: #cfcfcf;" +
                "-fx-border-radius: 12;" +
                "-fx-background-radius: 12;"
        );
    }

    private Label labelTitle(String s) {
        Label lb = new Label(s);
        lb.setStyle("-fx-font-weight: bold; -fx-text-fill: #333;");
        return lb;
    }

    private Label labelSmall(String s) {
        Label lb = new Label(s);
        lb.setStyle("-fx-font-weight: bold; -fx-text-fill: #333; -fx-padding: 6 0 0 0;");
        return lb;
    }

    private String toHex(Color c) {
        int r = (int) Math.round(c.getRed() * 255);
        int g = (int) Math.round(c.getGreen() * 255);
        int b = (int) Math.round(c.getBlue() * 255);
        return String.format("#%02X%02X%02X", r, g, b);
    }

    public static void main(String[] args) {
        launch(args);
    }
}