package sample;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class Screen extends Canvas {
    private static final int WIDTH = 64;
    private static final int HEIGHT = 32;

    private int scale = 12;

    private GraphicsContext gc;

    public int[][] graphic = new int[WIDTH][HEIGHT]; // Graphics in Chip 8 is a black
    // and white screen of 2048
    // pixels (62*32).

    public Screen() {
        super(800, 400);
        setFocusTraversable(true);

        gc = this.getGraphicsContext2D();
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, 800, 400);
        clear();
    }

    /**
     * Clears the display setting all the pixels to zero.
     */
    public void clear() {
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                graphic[x][y] = 0;
            }
        }
    }


    //Renders the display.

    public void render() {
        for (int x = 0; x < graphic.length; x++) {
            for (int y = 0; y < graphic[y].length; y++) {
                if (graphic[x][y] == 1) {
                    gc.setFill(Color.WHITE);
                } else {
                    gc.setFill(Color.BLACK);
                }

                gc.fillRect(x * scale, (y * scale), scale, scale);
            }
        }
    }

    /**
     * Gets the content of target pixel.
     *
     * @return The pixel at target x and y coordinate, 1 for white 0 for black.
     */
    public int getPixel(int x, int y) {
        return graphic[x][y];
    }

    //Sets the pixel at target location.
    public void setPixel(int x, int y) {
        graphic[x][y] ^= 1;
    }
}
