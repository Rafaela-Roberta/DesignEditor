/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.designeditor;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.function.BiConsumer;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class MainApp extends Application {

    private static final String GEMINI_API_KEY = "AIzaSyA-8gLbDG8Qv1Tqc7vu0ASbm5lckhrOX-8";

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

    // API
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    // Gallery images
    private final String[] galleryImages = {
            "coding.png",
            "computer.png",
            "demo.png",
            "happyperson.png",
            "house.png"
    };

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

        Button btnText    = new Button("Text");
        Button btnGallery = new Button("Gallery");
        Button btnUpload  = new Button("Upload");
        Button btnDL      = new Button("Download PNG");

        styleButton(btnText);
        styleButton(btnGallery);
        styleButton(btnUpload);
        styleButton(btnDL);

        // Text options
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

        // AI box
        VBox aiBox = buildAiBox();
        aiBox.setMaxWidth(Double.MAX_VALUE);

        // Put all sidebar items
        leftBar.getChildren().addAll(
                lbTemplates, templatesMenu,
                new Separator(),
                lbTools, btnText, btnGallery, btnUpload, btnDL,
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
            if (e.getTarget() == canvas) {
                clearSelection();
                updateHandlesVisibility(null);
            }
        });

        // Wrap canvas
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

        // ACTIONS
        miBlank.setOnAction(e -> resetCanvas());
        miPost.setOnAction(e -> resizeCanvasAndGoTop(1080, 1080));
        miStory.setOnAction(e -> resizeCanvasAndGoTop(1080, 1920));
        miFlyer.setOnAction(e -> resizeCanvasAndGoTop(850, 1100));

        btnText.setOnAction(e -> addTextToCanvas("Edit me"));
        btnGallery.setOnAction(e -> openGalleryWindow());
        btnUpload.setOnAction(e -> openImageFileChooser());
        btnDL.setOnAction(e -> exportCanvasToPNG());

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
        updateHandlesVisibility(null);
        scrollPane.setHvalue(0);
        scrollPane.setVvalue(0);
        canvasBgPicker.setValue(Color.WHITE);
        setCanvasBackground(Color.WHITE);
    }

    private void resizeCanvasAndGoTop(double w, double h) {
        setCanvasSize(w, h);
        clearSelection();
        updateHandlesVisibility(null);
        scrollPane.setHvalue(0);
        scrollPane.setVvalue(0);
    }

    // TEXT
    private void addTextToCanvas(String content) {
        Text t = new Text(content);
        t.setLayoutX(120);
        t.setLayoutY(160);
        t.setWrappingWidth(300);

        t.setFill(textColorPicker.getValue());
        t.setFont(Font.font(Math.min(fontSizeSpinner.getValue(), 28)));

        enableDragSelectAndEdit(t);

        // scroll over text changes font size
        t.addEventFilter(ScrollEvent.SCROLL, e -> {
            if (selectedNode == t) {
                int size = (int) t.getFont().getSize();
                int newSize = (int) Math.max(8, Math.min(200, size + (e.getDeltaY() > 0 ? 2 : -2)));
                fontSizeSpinner.getValueFactory().setValue(newSize);
                t.setFont(Font.font(newSize));
                updateSelectionBox();
                updateHandlesVisibility(selectedNode);
                e.consume();
            }
        });

        canvas.getChildren().add(t);
        selectNode(t);
    }

    // UPLOAD IMAGE
    private void openImageFileChooser() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select an Image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );
        File file = fileChooser.showOpenDialog(canvas.getScene().getWindow());
        if (file != null) {
            try {
                Image img = new Image(file.toURI().toString());
                if (img.isError()) throw img.getException();
                addImageToCanvas(img);
            } catch (Exception ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Cannot load selected image.");
                alert.showAndWait();
            }
        }
    }

    // GALLERY
      private void openGalleryWindow() {
    Stage galleryStage = new Stage();
    galleryStage.setTitle("Gallery");

    TilePane tilePane = new TilePane();
    tilePane.setPadding(new Insets(15));
    tilePane.setHgap(12);
    tilePane.setVgap(12);
    tilePane.setPrefColumns(2);

    for (String fileName : galleryImages) {
        try {
            Path imagePath = Paths.get("images", fileName);
            File file = imagePath.toFile();

            System.out.println("Trying: " + file.getAbsolutePath());
            System.out.println("Exists: " + file.exists());

            if (!file.exists()) {
                continue;
            }

            Image img = new Image(file.toURI().toString());

            ImageView preview = new ImageView(img);
            preview.setFitWidth(120);
            preview.setFitHeight(120);
            preview.setPreserveRatio(true);

            Button imgButton = new Button();
            imgButton.setGraphic(preview);
            imgButton.setStyle(
                    "-fx-background-color: white;" +
                    "-fx-border-color: #d0d0d0;" +
                    "-fx-border-radius: 10;" +
                    "-fx-background-radius: 10;" +
                    "-fx-padding: 8;"
            );

            imgButton.setOnAction(e -> {
                Image fullImg = new Image(file.toURI().toString());
                addImageToCanvas(fullImg);
                galleryStage.close();
            });

            VBox itemBox = new VBox(5);
            itemBox.setAlignment(Pos.CENTER);
            itemBox.getChildren().addAll(imgButton, new Label(fileName));
            tilePane.getChildren().add(itemBox);

        } catch (Exception ex) {
            System.out.println("Error loading image: " + fileName);
            ex.printStackTrace();
        }
    }

    if (tilePane.getChildren().isEmpty()) {
        Alert alert = new Alert(Alert.AlertType.ERROR,
                "No gallery images were loaded.\nCheck if the images folder is in the project root.");
        alert.showAndWait();
        return;
    }

    ScrollPane sp = new ScrollPane(tilePane);
    sp.setFitToWidth(true);
    sp.setStyle("-fx-background-color: white;");

    Scene galleryScene = new Scene(sp, 350, 400);
    galleryStage.setScene(galleryScene);
    galleryStage.show();
}

    // ADD IMAGE TO CANVAS
    private void addImageToCanvas(Image img) {
        if (img == null) return;

        ImageView iv = new ImageView(img);
        iv.setPreserveRatio(true);
        double initWidth = Math.min(400, Math.max(100, img.getWidth()));
        iv.setFitWidth(initWidth);

        Group g = new Group(iv);
        g.setLayoutX((canvas.getPrefWidth() - iv.getFitWidth()) / 2);
        g.setLayoutY((canvas.getPrefHeight() - iv.getBoundsInParent().getHeight()) / 2);

        enableResize(iv, g);
        enableDragAndSelect(g);

        canvas.getChildren().add(g);
        g.toFront();
        selectNode(g);
    }

    // RESIZE IMAGE WITH INDICATOR
    private void enableResize(ImageView iv, Group g) {
        final double handleSize = 10;

        Rectangle topLeft     = new Rectangle(handleSize, handleSize, Color.rgb(0,120,215,0.6));
        Rectangle topRight    = new Rectangle(handleSize, handleSize, Color.rgb(0,120,215,0.6));
        Rectangle bottomLeft  = new Rectangle(handleSize, handleSize, Color.rgb(0,120,215,0.6));
        Rectangle bottomRight = new Rectangle(handleSize, handleSize, Color.rgb(0,120,215,0.6));

        g.getChildren().addAll(topLeft, topRight, bottomLeft, bottomRight);

        Runnable updateHandles = () -> {
            Bounds b = iv.getBoundsInParent();
            topLeft.setX(b.getMinX() - handleSize / 2);
            topLeft.setY(b.getMinY() - handleSize / 2);

            topRight.setX(b.getMaxX() - handleSize / 2);
            topRight.setY(b.getMinY() - handleSize / 2);

            bottomLeft.setX(b.getMinX() - handleSize / 2);
            bottomLeft.setY(b.getMaxY() - handleSize / 2);

            bottomRight.setX(b.getMaxX() - handleSize / 2);
            bottomRight.setY(b.getMaxY() - handleSize / 2);
        };

        updateHandles.run();

        BiConsumer<Rectangle, String> setupDrag = (handle, corner) -> {
            final double[] initWidth = new double[1];
            final double[] initHeight = new double[1];
            final double[] mouseStartX = new double[1];
            final double[] mouseStartY = new double[1];

            handle.setOnMousePressed(e -> {
                initWidth[0] = iv.getFitWidth();
                initHeight[0] = iv.getFitHeight();
                mouseStartX[0] = e.getSceneX();
                mouseStartY[0] = e.getSceneY();
                selectNode(g);
                e.consume();
            });

            handle.setOnMouseDragged(e -> {
                double deltaX = e.getSceneX() - mouseStartX[0];
                double deltaY = e.getSceneY() - mouseStartY[0];

                double newWidth = initWidth[0];
                double newHeight = initHeight[0];

                switch(corner) {
                    case "topLeft":
                        newWidth  = Math.max(20, initWidth[0] - deltaX);
                        newHeight = Math.max(20, initHeight[0] - deltaY);
                        break;
                    case "topRight":
                        newWidth  = Math.max(20, initWidth[0] + deltaX);
                        newHeight = Math.max(20, initHeight[0] - deltaY);
                        break;
                    case "bottomLeft":
                        newWidth  = Math.max(20, initWidth[0] - deltaX);
                        newHeight = Math.max(20, initHeight[0] + deltaY);
                        break;
                    case "bottomRight":
                        newWidth  = Math.max(20, initWidth[0] + deltaX);
                        newHeight = Math.max(20, initHeight[0] + deltaY);
                        break;
                }

                iv.setFitWidth(newWidth);
                iv.setFitHeight(newHeight);

                updateHandles.run();
                updateSelectionBox();
                e.consume();
            });
        };

        setupDrag.accept(topLeft, "topLeft");
        setupDrag.accept(topRight, "topRight");
        setupDrag.accept(bottomLeft, "bottomLeft");
        setupDrag.accept(bottomRight, "bottomRight");
    }

    private void updateHandlesVisibility(Node n) {
        if (n instanceof Group g) {
            for (Node child : g.getChildren()) {
                if (child instanceof Rectangle r && r.getWidth() == 10 && r.getHeight() == 10) {
                    r.setVisible(true);
                }
            }
        }

        for (Node node : canvas.getChildren()) {
            if (node instanceof Group otherGroup && otherGroup != n) {
                for (Node child : otherGroup.getChildren()) {
                    if (child instanceof Rectangle r && r.getWidth() == 10 && r.getHeight() == 10) {
                        r.setVisible(false);
                    }
                }
            }
        }
    }

    // EXPORT TO PNG
    private void exportCanvasToPNG() {
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        WritableImage snapshot = canvas.snapshot(params, null);
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Canvas as PNG");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PNG Image", "*.png")
        );
        File file = fileChooser.showSaveDialog(canvas.getScene().getWindow());
        if (file != null) {
            try {
                ImageIO.write(SwingFXUtils.fromFXImage(snapshot, null), "png", file);
                System.out.println("Canvas saved to: " + file.getAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // DOUBLE-CLICK EDIT THE TEXT
    private void startInlineEdit(Text t) {
        TextField editor = new TextField(t.getText());
        editor.setStyle(
                "-fx-background-color: white;" +
                "-fx-border-color: #cfcfcf;" +
                "-fx-border-radius: 8;" +
                "-fx-background-radius: 8;"
        );

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
            updateHandlesVisibility(selectedNode);
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
            updateHandlesVisibility(selectedNode);
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

        Label title = new Label("AI Prompt");
        title.setStyle("-fx-font-size: 13; -fx-font-weight: bold;");

        TextArea prompt = new TextArea();
        prompt.setPromptText("Write your prompt here...");
        prompt.setWrapText(true);
        prompt.setPrefRowCount(4);

        Label status = new Label("");
        status.setStyle("-fx-text-fill: #666; -fx-font-size: 11;");

        Button generate = new Button("Generate Text");
        generate.setMaxWidth(Double.MAX_VALUE);
        styleButtonGraphite(generate);

        generate.setOnAction(e -> generateAiText(prompt.getText(), generate, status));

        box.getChildren().addAll(title, prompt, status, generate);
        return box;
    }

    // AI TEXT GENERATION
    private void generateAiText(String promptText, Button generateButton, Label statusLabel) {
        if (promptText == null || promptText.trim().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Please enter a prompt first.");
            alert.showAndWait();
            return;
        }

        generateButton.setDisable(true);
        generateButton.setText("Generating...");
        statusLabel.setText("Generating text...");

        new Thread(() -> {
            try {
                String url =
                        "https://generativelanguage.googleapis.com/v1beta/models/" +
                        "gemini-3-flash-preview:generateContent";

                String fullPrompt =
                        "Generate only the text the user asked for. " +
                        "Do not add titles, bullet points, labels, quotation marks, or explanations unless the user asks for them. " +
                        "Keep the response clean and ready to place in a design editor. " +
                        "User request: " + promptText.trim();

                String requestBody = """
                {
                  "contents": [{
                    "parts": [
                      { "text": %s }
                    ]
                  }]
                }
                """.formatted(mapper.writeValueAsString(fullPrompt));

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("x-goog-api-key", GEMINI_API_KEY)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build();

                HttpResponse<String> response =
                        httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    throw new RuntimeException("HTTP " + response.statusCode() + ":\n" + response.body());
                }

                JsonNode root = mapper.readTree(response.body());
                JsonNode candidates = root.path("candidates");

                if (!candidates.isArray() || candidates.isEmpty()) {
                    throw new RuntimeException("No response returned:\n" + response.body());
                }

                JsonNode parts = candidates.get(0).path("content").path("parts");

                String aiText = null;
                if (parts.isArray()) {
                    for (JsonNode part : parts) {
                        JsonNode textNode = part.path("text");
                        if (!textNode.isMissingNode() && !textNode.isNull()) {
                            aiText = textNode.asText();
                            if (aiText != null && !aiText.isBlank()) {
                                break;
                            }
                        }
                    }
                }

                if (aiText == null || aiText.isBlank()) {
                    throw new RuntimeException("No text returned:\n" + response.body());
                }

                String finalText = aiText.trim()
                        .replace("*", "")
                        .replace("#", "")
                        .replace("\"", "");

                Platform.runLater(() -> {
                    addTextToCanvas(finalText);
                    statusLabel.setText("Text added to canvas.");
                    generateButton.setDisable(false);
                    generateButton.setText("Generate Text");
                });

            } catch (Exception ex) {
                Platform.runLater(() -> {
                    generateButton.setDisable(false);
                    generateButton.setText("Generate Text");
                    statusLabel.setText("Generation failed.");

                    Alert alert = new Alert(
                            Alert.AlertType.ERROR,
                            "Text generation failed:\n" + ex.getMessage()
                    );
                    alert.showAndWait();
                });
            }
        }).start();
    }

    // SELECTION + DELETE
    private void selectNode(Node n) {
        selectedNode = n;

        if (n instanceof Text t) {
            textColorPicker.setValue((Color) t.getFill());
            fontSizeSpinner.getValueFactory().setValue((int) t.getFont().getSize());
        }

        updateSelectionBox();
        updateHandlesVisibility(selectedNode);
    }

    private void updateSelectionBox() {
        if (selectionBox != null) {
            canvas.getChildren().remove(selectionBox);
            selectionBox = null;
        }
        if (selectedNode == null) return;

        Bounds b = null;

        if (selectedNode instanceof Group g) {
            for (Node child : g.getChildren()) {
                if (child instanceof ImageView iv) {
                    Bounds sceneBounds = iv.localToScene(iv.getBoundsInLocal());
                    Bounds canvasBounds = canvas.sceneToLocal(sceneBounds);

                    b = new BoundingBox(
                            canvasBounds.getMinX(),
                            canvasBounds.getMinY(),
                            canvasBounds.getWidth(),
                            canvasBounds.getHeight()
                    );
                    break;
                }
            }
        }

        if (b == null) {
            b = selectedNode.getBoundsInParent();
        }

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
        updateHandlesVisibility(null);
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
            updateHandlesVisibility(selectedNode);
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
            updateHandlesVisibility(selectedNode);
            e.consume();
        });
    }

    private static class Delta {
        double x, y;
    }

    // STYLES
    private void styleButton(Button b) {
        b.setMaxWidth(Double.MAX_VALUE);
        styleButtonGraphite(b);
    }

    private void styleButtonGraphite(Button b) {
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
        
        b.lookup(".label");
        Platform.runLater(() -> {
            Label label = (Label) b.lookup(".label");
            if(label != null){
                label.setTextFill(Color.WHITE);
            }
        });
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