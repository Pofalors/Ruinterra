import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;

import javax.imageio.ImageIO;

public class Chest {

    public int worldX, worldY;
    public boolean opened = false;

    public String chestId;
    public ArrayList<String> itemIds = new ArrayList<>();
    public ArrayList<Integer> amounts = new ArrayList<>();

    public BufferedImage closedImage;
    public BufferedImage openedImage;

    public Chest(String chestId, int worldX, int worldY, ArrayList<String> itemIds, ArrayList<Integer> amounts) {
        this.chestId = chestId;
        this.worldX = worldX;
        this.worldY = worldY;

        if (itemIds != null) {
            this.itemIds.addAll(itemIds);
        }

        if (amounts != null) {
            this.amounts.addAll(amounts);
        }

        try {
            closedImage = ImageIO.read(new File("res/items/chest_closed.png"));
            openedImage = ImageIO.read(new File("res/items/chest_opened.png"));
        } catch (Exception e) {
            System.out.println("Failed to load chest images.");
            e.printStackTrace();
        }
    }

    public void draw(Graphics2D g2, GamePanel gp) {
        int screenX = worldX - gp.worldX;
        int screenY = worldY - gp.worldY;

        BufferedImage img = opened ? openedImage : closedImage;

        if (img != null) {
            g2.drawImage(img, screenX, screenY, gp.tileSize, gp.tileSize, null);
        }
    }
}
