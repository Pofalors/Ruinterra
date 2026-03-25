public class AtlasLayer {
    public String name;
    public int rows;
    public int cols;
    public LayerTile[][] tiles;
    public boolean visible = true;

    public AtlasLayer(String name, int rows, int cols) {
        this.name = name;
        this.rows = rows;
        this.cols = cols;
        this.tiles = new LayerTile[rows][cols];

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                tiles[r][c] = new LayerTile();
            }
        }
    }
}
