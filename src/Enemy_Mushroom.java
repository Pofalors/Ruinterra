import java.io.File;
import javax.imageio.ImageIO;

public class Enemy_Mushroom extends Enemy {
    public Enemy_Mushroom(GamePanel gp) {
        super(gp);
        try {
            down1 = ImageIO.read(new File("res/enemy/mushroom.png"));
            down2 = ImageIO.read(new File("res/enemy/mushroom.png"));
            currentImage = down1;
            
            // Stats
            level = 1;
            maxHp = 18;
            hp = maxHp;
            attack = 5;
            defense = 1;
            magicAttack = 3;
            magicDefense = 2;
            speed_stat = 3;
            exp = 12;
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
