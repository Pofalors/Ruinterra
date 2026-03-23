public class MapLayer {
    public String name;
    public String atlasName;
    public int rows;
    public int cols;
    public int[][] tiles;
    public boolean visible = true;

    public MapLayer(String name, int rows, int cols) {
        this.name = name;
        this.rows = rows;
        this.cols = cols;
        this.tiles = new int[rows][cols];
        this.atlasName = "";
    }
}
