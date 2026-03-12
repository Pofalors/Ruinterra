import java.awt.image.BufferedImage;

public class BattleEntity {
    public String name;
    public int hp, maxHp;
    public int mp, maxMp;
    public int attack, defense, magicAttack, magicDefense, speed;
    public BufferedImage image;
    public boolean isPlayer;
    public Entity playerRef; // Αν είναι παίκτης, αναφορά στο Entity
    public Enemy enemyRef;   // Αν είναι εχθρός, αναφορά στο Enemy
    public int damageNumber = 0;           // Η ζημιά που έγινε
    public int damageTimer = 0;             // Μετρητής για το animation
    public final int DAMAGE_DURATION = 30;  // Πόσα frames θα φαίνεται (0.5 sec στα 60fps)
    public boolean isTakingDamage = false;  // Αν δείχνουμε animation ζημιάς
    
    // Για εχθρούς
    public BattleEntity(Enemy enemy, BufferedImage img) {
        this.name = enemy.getClass().getSimpleName();
        this.hp = enemy.hp;
        this.maxHp = enemy.maxHp;
        this.attack = enemy.attack;
        this.defense = enemy.defense;
        this.speed = 5; // Default speed για εχθρούς
        this.image = img;
        this.isPlayer = false;
        this.enemyRef = enemy;
    }
    
    // Για παίκτες
    public BattleEntity(Entity player, BufferedImage img) {
        this.name = "Hero"; // Θα το αλλάξουμε όταν έχουμε ονόματα
        this.hp = player.hp;
        this.maxHp = player.maxHp;
        this.mp = player.mp;
        this.maxMp = player.maxMp;
        this.attack = player.attack;
        this.defense = player.defense;
        this.magicAttack = player.magicAttack;
        this.magicDefense = player.magicDefense;
        this.speed = player.speed_stat;
        this.image = img;
        this.isPlayer = true;
        this.playerRef = player;
    }
    
    public boolean isAlive() {
        return hp > 0;
    }
    
    public void takeDamage(int damage) {
        hp -= damage;
        if (hp < 0) hp = 0;

        this.damageNumber = damage;
        this.damageTimer = DAMAGE_DURATION;
        this.isTakingDamage = true;
    }
    
    public void heal(int amount) {
        hp += amount;
        if (hp > maxHp) hp = maxHp;
    }
    
    public void useMp(int amount) {
        mp -= amount;
        if (mp < 0) mp = 0;
    }
}