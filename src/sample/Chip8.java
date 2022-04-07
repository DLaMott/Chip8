package sample;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.io.*;

public class Chip8 extends Application {
    private static final int SCREEN_WIDTH = 800;
    private static final int SCREEN_HEIGHT = 450;
    private Stage mainStage;
    Timeline gameLoop;
    private Memory memory;
    private Screen screen;
    private Keyboard keyboard;

    private void initialize() {
        mainStage.setTitle("CHIP-8-Emulator");
        screen = new Screen();
        keyboard = new Keyboard();

        // Initialize menu that contains buttons for exiting and switching applications to run.
        MenuBar menuBar = new MenuBar();
        Menu menuFile = new Menu("File");

        MenuItem loadRomItem = new MenuItem("Load ROM");
        loadRomItem.setOnAction(e -> {
            // Open file choose to let the user select a ROM.
            FileChooser f = new FileChooser();
            f.setTitle("Open ROM File");
            File file = f.showOpenDialog(mainStage);

            if (file != null) {
                loadProgram(file.getPath());
            }
        });
        MenuItem exitItem = new MenuItem("Exit");
        exitItem.setOnAction(e -> {
            System.exit(0);
        });

        menuFile.getItems().add(loadRomItem);
        menuFile.getItems().add(exitItem);
        menuBar.getMenus().add(menuFile);

        // Initial render of the screen.
        screen.render();

        // Place all elements into the main window.
        VBox root = new VBox();
        root.getChildren().add(menuBar);
        root.getChildren().add(screen);

        Scene mainScene = new Scene(root);

        // Handle key presses.
        mainScene.setOnKeyPressed(e -> keyboard.setKeyDown(e.getCode()));

        // Handle key releases.
        mainScene.setOnKeyReleased(e -> keyboard.setKeyUp(e.getCode()));

        // Set up the main window for show.
        mainStage.setScene(mainScene);
        mainStage.setMaxWidth(SCREEN_WIDTH);
        mainStage.setMaxHeight(SCREEN_HEIGHT);
        mainStage.setMinWidth(SCREEN_WIDTH);
        mainStage.setMinHeight(SCREEN_HEIGHT);
        mainStage.setResizable(false);

        gameLoop = new Timeline();
        gameLoop.setCycleCount(Timeline.INDEFINITE);

        // Construct the keyframe telling the application what to happen inside the game loop.
        KeyFrame kf = new KeyFrame(
                Duration.seconds(0.003),
                actionEvent -> {
                    try {
                        // Fetch opcode
                        memory.fetchOpcode();
                        // Decode & Execute opcode
                        memory.decodeOpcode();
                    } catch (RuntimeException e) {
                        gameLoop.stop();
                    }

                    // Render
                    if (memory.isDrawFlag()) {
                        screen.render();
                        memory.setDrawFlag(false);
                    }

                    // Update Timers
                    if (memory.getDelayTimer() > 0) {
                        memory.setDelayTimer(memory.getDelayTimer() - 1);
                    }

                    if (memory.getSoundTimer() > 0) {
                        if (memory.getSoundTimer() == 1) {
                            System.out.println("Make Sound!");
                        }
                        memory.setSoundTimer(memory.getSoundTimer() - 1);
                    }
                });

        gameLoop.getKeyFrames().add(kf);
        loadProgram("roms/PONG");
        mainStage.show();
    }

    /**
     * Copy the program to run into the memory
     *
     * @param program - The program to copy into memory.
     */
    private void loadProgram(String program) {
        gameLoop.stop();
        screen.clear();
        memory = new Memory(screen, keyboard);

        // Load binary and pass it to memory
        try {

            File file = new File(program);
            DataInputStream inputStream = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
            byte[] fileLength = new byte[(int) file.length()];
            inputStream.read(fileLength);

            memory.loadProgram(fileLength);

            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        gameLoop.play();
    }

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        mainStage = primaryStage;
        initialize();
    }
}