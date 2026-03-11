import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class BattleScene {
    public BufferedImage background; // Το τυχαίο terrain
    public ArrayList<BattleEnemy> enemies = new ArrayList<>();
    public ArrayList<BattlePlayer> players = new ArrayList<>();
    public int state = 0; // 0=entering, 1=battle, 2=victory, 3=defeat
    public int timer = 0;
}
