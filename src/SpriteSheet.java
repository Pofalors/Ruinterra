import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class SpriteSheet {
    private BufferedImage sheet;
    private int frameWidth;
    private int frameHeight;
    
    public SpriteSheet(String path, int frameWidth, int frameHeight) {
        try {
            System.out.println("Loading sprite sheet: " + path);
            this.sheet = ImageIO.read(new File(path));
            this.frameWidth = frameWidth;
            this.frameHeight = frameHeight;
            System.out.println("Loaded: " + sheet.getWidth() + "x" + sheet.getHeight() + 
                            ", frames: " + (sheet.getWidth() / frameWidth));
        } catch (Exception e) {
            System.out.println("ERROR loading sprite sheet: " + path);
            e.printStackTrace();
        }
    }
    
    public BufferedImage getFrame(int index) {
        return sheet.getSubimage(index * frameWidth, 0, frameWidth, frameHeight);
    }
    
    public BufferedImage[] getAllFrames() {
        int cols = sheet.getWidth() / frameWidth;
        System.out.println("getAllFrames: cols=" + cols);
        BufferedImage[] frames = new BufferedImage[cols];
        for (int i = 0; i < cols; i++) {
            frames[i] = getFrame(i);
        }
        return frames;
    }
}
