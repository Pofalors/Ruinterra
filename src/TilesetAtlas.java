import java.awt.image.BufferedImage;

public class TilesetAtlas {
    public String name;
    public BufferedImage image;
    public int tileSize;
    public int columns;
    public int rows;

    public TilesetAtlas(String name, BufferedImage image, int tileSize) {
        this.name = name;
        this.image = image;
        this.tileSize = tileSize;
        this.columns = image.getWidth() / tileSize;
        this.rows = image.getHeight() / tileSize;
    }

    public BufferedImage getTileImage(int tileIndex) {
        if (tileIndex < 0) return null;

        int col = tileIndex % columns;
        int row = tileIndex / columns;

        if (col < 0 || col >= columns || row < 0 || row >= rows) {
            return null;
        }

        return image.getSubimage(col * tileSize, row * tileSize, tileSize, tileSize);
    }
}
