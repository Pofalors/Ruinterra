import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import javax.imageio.ImageIO;

public class PartyMember extends Entity {
    public String className;
    public int exp;
    public int expToNextLevel;
    public int level;
    public int baseAttack, baseDefense, baseMagicAttack, baseSpeed;
    public boolean joinedParty = false;
    
    public PartyMember(GamePanel gp, String name, String className) {
        super(gp);
        this.name = name;
        this.className = className;
        this.solid = true;
        this.speed = 4;
        this.direction = "down";
        
        // Αρχικοποίηση stats ανάλογα με την κλάση
        if (className.equals("Assassin")) {
            this.baseAttack = 10;
            this.baseDefense = 6;
            this.baseMagicAttack = 2;
            this.baseSpeed = 12;
            this.maxHp = 30;
            this.maxMp = 15;
        } else if (className.equals("Mage")) {
            this.baseAttack = 5;
            this.baseDefense = 4;
            this.baseMagicAttack = 15;
            this.baseSpeed = 7;
            this.maxHp = 25;
            this.maxMp = 30;
        }
        
        this.attack = baseAttack;
        this.defense = baseDefense;
        this.magicAttack = baseMagicAttack;
        this.speed_stat = baseSpeed;
        this.hp = maxHp;
        this.mp = maxMp;
        this.level = 1;
        this.exp = 0;
        this.expToNextLevel = 100;
        
        loadImages();
    }
    
    public void loadImages() {
        try {
            down1 = ImageIO.read(new File("res/player/" + name.toLowerCase() + "_down_1.png"));
            down2 = ImageIO.read(new File("res/player/" + name.toLowerCase() + "_down_2.png"));
            up1 = ImageIO.read(new File("res/player/" + name.toLowerCase() + "_up_1.png"));
            up2 = ImageIO.read(new File("res/player/" + name.toLowerCase() + "_up_2.png"));
            left1 = ImageIO.read(new File("res/player/" + name.toLowerCase() + "_left_1.png"));
            left2 = ImageIO.read(new File("res/player/" + name.toLowerCase() + "_left_2.png"));
            right1 = ImageIO.read(new File("res/player/" + name.toLowerCase() + "_right_1.png"));
            right2 = ImageIO.read(new File("res/player/" + name.toLowerCase() + "_right_2.png"));
            
            currentImage = down1;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void addExp(int amount) {
        exp += amount;
        while (exp >= expToNextLevel) {
            levelUp();
        }
    }
    
    public void levelUp() {
        level++;
        exp -= expToNextLevel;
        expToNextLevel = (int)(expToNextLevel * 1.5);
        
        if (className.equals("Assassin")) {
            maxHp += 6;
            maxMp += 3;
            baseAttack += 2;
            baseDefense += 1;
            baseSpeed += 3;
        } else if (className.equals("Mage")) {
            maxHp += 4;
            maxMp += 8;
            baseMagicAttack += 4;
            baseAttack += 1;
            baseDefense += 1;
        }
        
        hp = maxHp;
        mp = maxMp;
        recalcStats();
    }
    
    public void recalcStats() {
        attack = baseAttack;
        defense = baseDefense;
        magicAttack = baseMagicAttack;
        speed_stat = baseSpeed;
        
        for (Item item : equipped) {
            if (item != null) {
                attack += item.attackBonus;
                defense += item.defenseBonus;
                magicAttack += item.magicBonus;
                speed_stat += item.speedBonus;
            }
        }
    }
    public void updateImage() {
        if (direction.equals("down")) {
            currentImage = (frame == 0) ? down1 : down2;
        } else if (direction.equals("up")) {
            currentImage = (frame == 0) ? up1 : up2;
        } else if (direction.equals("left")) {
            currentImage = (frame == 0) ? left1 : left2;
        } else if (direction.equals("right")) {
            currentImage = (frame == 0) ? right1 : right2;
        }
    }
}