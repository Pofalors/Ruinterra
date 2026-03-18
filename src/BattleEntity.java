import java.awt.image.BufferedImage;

public class BattleEntity {
    public String name;
    public int hp, maxHp;
    public int mp, maxMp;
    public int attack, defense, magicAttack, magicDefense, speed;
    public BufferedImage image;
    public boolean isPlayer;
    public Entity playerRef;
    public Enemy enemyRef;

    // damage popup
    public int damageNumber = 0;
    public int damageTimer = 0;
    public final int DAMAGE_DURATION = 30;
    public boolean isTakingDamage = false;

    // NEW: combat state machine
    public CombatState state = CombatState.IDLE;
    public int stateTimer = 0;

    // NEW: BP system
    public int bp = 0;
    public final int maxBp = 5;
    public int boostUsed = 0;

    // NEW: queued action data
    public BattleEntity queuedTarget = null;
    public String queuedAction = "";
    public boolean strikeTriggered = false;
    public boolean actionFinished = false;
    public boolean defending = false;

    // Enemy constructor
    public BattleEntity(Enemy enemy, BufferedImage img) {
        this.name = enemy.getClass().getSimpleName();
        this.hp = enemy.hp;
        this.maxHp = enemy.maxHp;
        this.mp = enemy.mp;
        this.maxMp = enemy.maxMp;
        this.attack = enemy.attack;
        this.defense = enemy.defense;
        this.magicAttack = enemy.magicAttack;
        this.magicDefense = enemy.magicDefense;
        this.speed = enemy.speed_stat;
        this.image = img;
        this.isPlayer = false;
        this.enemyRef = enemy;
    }

    // Player / party constructor
    public BattleEntity(Entity player, BufferedImage img) {
        if (player instanceof PartyMember) {
            this.name = ((PartyMember) player).className;
        } else {
            this.name = "Hero";
        }

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
        if (defending) {
            damage = Math.max(1, damage / 2);
        }

        hp -= damage;
        if (hp < 0) hp = 0;

        this.damageNumber = damage;
        this.damageTimer = DAMAGE_DURATION;
        this.isTakingDamage = true;

        if (!isAlive()) {
            enterState(CombatState.DEAD);
        } else {
            enterState(CombatState.HURT);
        }
    }

    public void heal(int amount) {
        hp += amount;
        if (hp > maxHp) hp = maxHp;
    }

    public void useMp(int amount) {
        mp -= amount;
        if (mp < 0) mp = 0;
    }

    public void gainBP() {
        if (bp < maxBp) bp++;
    }

    public void spendBP(int amount) {
        bp -= amount;
        if (bp < 0) bp = 0;
    }

    public void enterState(CombatState newState) {
        this.state = newState;
        this.stateTimer = 0;

        if (newState != CombatState.ATTACKING) {
            this.strikeTriggered = false;
        }
    }

    public void updateStateTimer() {
        stateTimer++;
    }

    public void resetTurnFlags() {
        queuedTarget = null;
        queuedAction = "";
        boostUsed = 0;
        strikeTriggered = false;
        actionFinished = false;
        defending = false;
    }

    public boolean canAct() {
        return isAlive() && state != CombatState.DEAD;
    }
}