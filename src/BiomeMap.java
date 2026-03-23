public class BiomeMap {
    public int cols;
    public int rows;
    public BiomeType[][] data;

    public BiomeMap(int cols, int rows) {
        this.cols = cols;
        this.rows = rows;
        this.data = new BiomeType[rows][cols];

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                data[row][col] = BiomeType.VOID;
            }
        }
    }

    public boolean isValid(int col, int row) {
        return col >= 0 && col < cols && row >= 0 && row < rows;
    }

    public BiomeType get(int col, int row) {
        if (!isValid(col, row)) return BiomeType.VOID;
        return data[row][col];
    }

    public void set(int col, int row, BiomeType biome) {
        if (!isValid(col, row)) return;
        data[row][col] = biome;
    }

    public void fill(BiomeType biome) {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                data[row][col] = biome;
            }
        }
    }

    public void fillRect(int startCol, int startRow, int width, int height, BiomeType biome) {
        for (int row = startRow; row < startRow + height; row++) {
            for (int col = startCol; col < startCol + width; col++) {
                set(col, row, biome);
            }
        }
    }
}
