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

    // ===== BREAK SYSTEM =====
    public int shieldPoints = 0;
    public int maxShieldPoints = 0;

    public boolean broken = false;

    // Θέλουμε να χάνει την τωρινή σειρά και την επόμενη
    public int brokenTurnsRemaining = 0;

    // Weakness slots
    public String[] weaknessTypes = new String[6];
    public boolean[] weaknessRevealed = new boolean[6];

    // Damage multiplier όσο είναι broken
    public float brokenDamageMultiplier = 1.5f;
    // MULTI-HIT SYSTEM
    public int multiHitCount = 1;
    public int currentHitIndex = 0;
    public int multiHitTimer = 0;

    // Recoil etc.
    public int hitBlinkTimer = 0;
    public int hitBlinkDuration = 0;

    public int hitRecoilTimer = 0;
    public int hitRecoilDuration = 0;
    public int hitRecoilOffsetX = 0;
    public int hitRecoilOffsetY = 0;
    public int hitOutlineTimer = 0;
    public int hitOutlineDuration = 0;

    public int dustTimer = 0;
    public int dustDuration = 0;

    // Enemy constructor
    public BattleEntity(Enemy enemy, BufferedImage img) {
        this.name = enemy.name;
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

    public void triggerHitReact(int strength) {
        if (strength < 1) strength = 1;

        hitBlinkDuration = 0;
        hitBlinkTimer = 0;

        hitRecoilDuration = 6 + strength * 2;
        hitRecoilTimer = hitRecoilDuration;

        hitRecoilOffsetX = 6 + strength * 3;
        hitRecoilOffsetY = -2 - strength;

        hitOutlineDuration = 6 + strength * 2;
        hitOutlineTimer = hitOutlineDuration;

        dustDuration = 10 + strength * 2;
        dustTimer = dustDuration;
    }

    public void setupBreakData(int shields, String... weaknesses) {
        this.maxShieldPoints = Math.max(0, shields);
        this.shieldPoints = this.maxShieldPoints;

        this.broken = false;
        this.brokenTurnsRemaining = 0;

        for (int i = 0; i < weaknessTypes.length; i++) {
            weaknessTypes[i] = null;
            weaknessRevealed[i] = false;
        }

        if (weaknesses != null) {
            for (int i = 0; i < weaknesses.length && i < weaknessTypes.length; i++) {
                weaknessTypes[i] = weaknesses[i];
            }
        }
    }

    public boolean hasBreakSystem() {
        return maxShieldPoints > 0;
    }

    public boolean isWeakTo(String attackType) {
        if (attackType == null) return false;

        for (String weakness : weaknessTypes) {
            if (weakness != null && weakness.equalsIgnoreCase(attackType)) {
                return true;
            }
        }
        return false;
    }

    public void revealWeakness(String attackType) {
        if (attackType == null) return;

        for (int i = 0; i < weaknessTypes.length; i++) {
            if (weaknessTypes[i] != null && weaknessTypes[i].equalsIgnoreCase(attackType)) {
                weaknessRevealed[i] = true;
            }
        }
    }

    public boolean canLoseShield() {
        return hasBreakSystem() && !broken && shieldPoints > 0;
    }

    public boolean applyShieldDamage(int amount) {
        if (!canLoseShield()) return false;

        shieldPoints -= Math.max(1, amount);
        if (shieldPoints < 0) shieldPoints = 0;

        if (shieldPoints == 0) {
            broken = true;
            brokenTurnsRemaining = 2; // τρέχουσα + επόμενη σειρά του enemy
            return true;
        }

        return false;
    }

    public boolean shouldSkipTurnBecauseBroken() {
        return broken && brokenTurnsRemaining > 0;
    }

    public void consumeBrokenTurn() {
        if (brokenTurnsRemaining > 0) {
            brokenTurnsRemaining--;
        }

        if (broken && brokenTurnsRemaining <= 0) {
            recoverFromBreak();
        }
    }

    public void recoverFromBreak() {
        broken = false;
        brokenTurnsRemaining = 0;
        shieldPoints = maxShieldPoints;
    }

    public int applyBrokenDamageMultiplier(int baseDamage) {
        if (broken) {
            return Math.max(1, Math.round(baseDamage * brokenDamageMultiplier));
        }
        return baseDamage;
    }    
}