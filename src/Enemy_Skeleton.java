import java.io.File;
import javax.imageio.ImageIO;

public class Enemy_Skeleton extends Enemy {
    public Enemy_Skeleton(GamePanel gp) {
        super(gp);
        try {
            down1 = ImageIO.read(new File("res/enemy/skeleton.png"));
            down2 = ImageIO.read(new File("res/enemy/skeleton.png"));
            currentImage = down1;
            
            // Stats
            level = 3;
            maxHp = 35;
            hp = maxHp;
            attack = 9;
            defense = 3;
            magicAttack = 4;
            magicDefense = 4;
            speed_stat = 7;
            exp = 22;
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
