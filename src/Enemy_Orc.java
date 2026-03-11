import java.io.File;
import javax.imageio.ImageIO;

public class Enemy_Orc extends Enemy {
    public Enemy_Orc(GamePanel gp) {
        super(gp);
        try {
            down1 = ImageIO.read(new File("res/enemy/orc.png"));
            down2 = ImageIO.read(new File("res/enemy/orc.png"));
            currentImage = down1;
            
            // Stats
            level = 3;
            maxHp = 40;
            hp = maxHp;
            attack = 10;
            defense = 4;
            magicAttack = 2;
            magicDefense = 3;
            speed_stat = 5;
            exp = 25;
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
