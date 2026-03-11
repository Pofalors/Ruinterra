import java.io.File;
import javax.imageio.ImageIO;

public class Enemy_Slime extends Enemy {
    public Enemy_Slime(GamePanel gp) {
        super(gp);
        try {
            down1 = ImageIO.read(new File("res/enemy/slime.png"));
            down2 = ImageIO.read(new File("res/enemy/slime.png"));
            currentImage = down1;
            
            // Stats
            level = 1;
            maxHp = 15;
            hp = maxHp;
            attack = 4;
            defense = 0;
            magicAttack = 2;
            magicDefense = 1;
            speed_stat = 3;
            exp = 10;
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
