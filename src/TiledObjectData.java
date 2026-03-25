public class TiledObjectData {
    public String layerName;
    public String name;
    public String type;

    public int x;
    public int y;
    public int width;
    public int height;

    public java.util.HashMap<String, String> properties = new java.util.HashMap<>();

    public TiledObjectData() {
    }

    public String getProperty(String key) {
        return properties.getOrDefault(key, "");
    }

    public int getPropertyInt(String key, int fallback) {
        try {
            return Integer.parseInt(properties.get(key));
        } catch (Exception e) {
            return fallback;
        }
    }
}
