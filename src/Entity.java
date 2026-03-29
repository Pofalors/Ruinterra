import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class Entity {
    public GamePanel gp;

    public String name = "Entity";
    public String dialogueId = "";
    public String npcType = "normal";
    
    // Θέση και κίνηση
    public int worldX, worldY;
    public int speed;
    
    // Εικόνες
    public BufferedImage up1, up2, down1, down2, left1, left2, right1, right2;
    public BufferedImage currentImage;
    
    // Κατεύθυνση και animation
    public String direction = "down";
    public int frame = 0;
    public int counter = 0;
    public int animCounter = 0;
    
    // Collision
    public boolean collision = false;
    public boolean solid = true;
    public boolean moving = true;
    
    // ========== ΣΤΑΤΙΣΤΙΚΑ ΧΑΡΑΚΤΗΡΑ ==========
    // Βασικά stats (τρέχοντα - με εξοπλισμό)
    public int level = 1;
    public int exp = 0;
    public int expToNextLevel = 100;
    
    public int maxHp = 50;
    public int hp = 50;
    public int maxMp = 20;
    public int mp = 20;
    public int attack = 10;
    public int defense = 5;
    public int magicAttack = 8;
    public int magicDefense = 4;
    public int speed_stat = 8;
    
    public int gold = 0;
    
    // ========== ΒΑΣΙΚΑ STATS (χωρίς εξοπλισμό) ==========
    public int baseLevel = 1;
    public int baseMaxHp = 50;
    public int baseMaxMp = 20;
    public int baseAttack = 10;
    public int baseDefense = 5;
    public int baseMagicAttack = 8;
    public int baseMagicDefense = 4;
    public int baseSpeed = 8;
    
    public ArrayList<Quest> quests = new ArrayList<>();
    public ArrayList<Item> equipped = new ArrayList<>(9);
    
    public Entity(GamePanel gp) {
        this.gp = gp;

        for (int i = 0; i < 9; i++) {
            equipped.add(null);
        }
        
        // Αρχικοποίησε base stats (ίδια με τα αρχικά)
        baseLevel = level;
        baseMaxHp = maxHp;
        baseMaxMp = maxMp;
        baseAttack = attack;
        baseDefense = defense;
        baseMagicAttack = magicAttack;
        baseMagicDefense = magicDefense;
        baseSpeed = speed_stat;
    }
    
    public void addExp(int amount) {
        exp += amount;
        System.out.println("Πήρες " + amount + " EXP!");
        
        while (exp >= expToNextLevel) {
            levelUp();
            // Μήνυμα level up
            if (gp != null) {
                gp.startDialogue("Level Up! Έγινες level " + level + "!");
                // Προσοχή: μην αλλάξεις το gameState εδώ γιατί είσαι σε μάχη
            }
        }
    }
    
    public void levelUp() {
        level++;
        exp -= expToNextLevel;
        expToNextLevel = (int)(expToNextLevel * 1.5);
        
        // Αύξηση stats (ΠΡΩΤΑ τα base stats!)
        baseMaxHp += 10;
        baseMaxMp += 5;
        baseAttack += 2;
        baseDefense += 1;
        baseMagicAttack += 2;
        baseMagicDefense += 1;
        baseSpeed += 1;
        
        // Μετά ενημέρωσε τα τρέχοντα stats (base + bonuses από items)
        // ΥΠΟΛΟΓΙΣΕ ΞΑΝΑ ΤΑ ΣΥΝΟΛΙΚΑ STATS
        recalcStats();
        
        System.out.println("LEVEL UP! Έγινες level " + level + "!");
    }

    // ΝΕΑ ΜΕΘΟΔΟΣ: Υπολογίζει ξανά όλα τα stats
    public void recalcStats() {
        // Ξεκίνα από τα base stats
        maxHp = baseMaxHp;
        maxMp = baseMaxMp;
        attack = baseAttack;
        defense = baseDefense;
        magicAttack = baseMagicAttack;
        magicDefense = baseMagicDefense;
        speed_stat = baseSpeed;
        
        // Πρόσθεσε τα μπόνους από εξοπλισμό (αν υπάρχει inventory)
        if (gp != null && gp.inventory != null) {
            for (int i = 0; i < 9; i++) {
                Item item = gp.inventory.getEquipSlot(i);
                if (item != null) {
                    attack += item.attackBonus;
                    defense += item.defenseBonus;
                    magicAttack += item.magicBonus;
                    maxHp += item.hpBonus;
                    maxMp += item.mpBonus;
                    speed_stat += item.speedBonus;
                }
            }
        }
        
        // Βεβαιώσου ότι τα τρέχοντα hp/mp δεν ξεπερνούν τα νέα max
        if (hp > maxHp) hp = maxHp;
        if (mp > maxMp) mp = maxMp;
    }
    
    public void heal(int amount) {
        hp += amount;
        if (hp > maxHp) hp = maxHp;
    }
    
    public void takeDamage(int amount) {
        hp -= amount;
        if (hp < 0) hp = 0;
    }
    
    public boolean isAlive() {
        return hp > 0;
    }
    
    public void checkItemQuests(String itemName) {
        for (Quest quest : quests) {
            if (quest.active && !quest.completed && 
                quest.targetItem != null && quest.targetItem.equals(itemName)) {
                quest.currentAmount++;
                System.out.println("Quest progress: " + quest.currentAmount + "/" + quest.requiredAmount);
                
                if (quest.checkCompletion()) {
                    System.out.println("Quest completed: " + quest.name);
                }
            }
        }
    }
    
    public void checkTalkQuests(String npcName) {
        for (Quest quest : quests) {
            if (quest.active && !quest.completed && 
                quest.targetNPC != null && quest.targetNPC.equals(npcName)) {
                quest.talkedToNPC = true;
                
                if (quest.checkCompletion()) {
                    System.out.println("Quest completed: " + quest.name);
                }
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
