public class MapData {
    public String name;
    public String filePath;
    public int cols;
    public int rows;
    public int[][] tiles;

    public MapData(String name, String filePath, int cols, int rows) {
        this.name = name;
        this.filePath = filePath;
        this.cols = cols;
        this.rows = rows;
        this.tiles = new int[rows][cols];
    }
}
