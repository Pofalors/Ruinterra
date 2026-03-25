import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Color;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.io.File;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.BasicStroke;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.AudioInputStream;
import java.net.URL;
import java.awt.Toolkit;
import java.awt.RadialGradientPaint;
import java.awt.AlphaComposite;              
import java.awt.GraphicsEnvironment;  
import java.awt.GraphicsDevice;  
import java.awt.Rectangle;
import java.awt.RenderingHints;

public class GamePanel extends JPanel implements Runnable {
    // Ρυθμίσεις οθόνης
    public final int originalTileSize = 16; // 16x16 pixels το κάθε πλακάκι (στο πρωτότυπο)
    final int scale = 3; // Για να τα βλέπουμε, τα μεγαλώνουμε 3 φορές
    // Για fullscreen
    private JFrame window;
    private GraphicsDevice device;
    public double scaleX = 1.0;
    public double scaleY = 1.0;
    public boolean isFullScreen = false;

    final int tileSize = originalTileSize * scale; // 48x48 pixels
    final int maxScreenCol = 16; // 16 στήλες από πλακάκια στην οθόνη
    final int maxScreenRow = 12; // 12 σειρές

    final int screenWidth = tileSize * maxScreenCol; // 768 pixels
    final int screenHeight = tileSize * maxScreenRow; // 576 pixels

    // ΝΕΟ: Ρυθμίσεις κόσμου
    public int maxWorldCol;  // 50 στήλες (πολύ μεγαλύτερος κόσμος)
    public int maxWorldRow;  // 50 σειρές
    int worldWidth;
    int worldHeight;

    // Πολλαπλοί χάρτες
    public int currentMap = 0;
    public int maxMaps = 0;

    // Θέσεις spawn για κάθε χάρτη
    public int[][] spawnPoints = {
        {23 * tileSize, 21 * tileSize}, // Overworld spawn
        {10 * tileSize, 41 * tileSize}, // Dungeon spawn
        {12 * tileSize, 12 * tileSize}, // Merchant spawn
        {4 * tileSize, 22 * tileSize},  // Main town
        {12 * tileSize, 12 * tileSize}, // Big house interior
        {12 * tileSize, 12 * tileSize}   // Small house interior  // Main town
    };

    // SPAWN POINT (ξεκινάμε στη μέση του κόσμου)
    public int worldX = 23 * tileSize;  // Ξεκίνα στη μέση περίπου
    public int worldY = 21 * tileSize;

    // Game loop
    Thread gameThread;

    // tile manager
    TileManager tileM; // Διαχειριστής χάρτη

    // NPC
    public NPC_OldMan oldMan; // Ο NPC μας
    public Entity player; // Ο ήρωας ως Entity
    public ArrayList<ArrayList<Entity>> npcs = new ArrayList<>();

    // New classes
    public ArrayList<PartyMember> partyMembers = new ArrayList<>();
    public ArrayList<BattlePartyMember> battlePartyMembers = new ArrayList<>();
    ArrayList<Point> playerPositions = new ArrayList<>();

    // Inventory
    public Inventory inventory = new Inventory();
    public boolean showInventory = false; // Να εμφανίζεται το inventory
    public final int inventoryState = 2; // Νέο game state
    //public ArrayList<ItemOnGround> itemsOnGround = new ArrayList<>();
    public ArrayList<ArrayList<ItemOnGround>> itemsOnGround = new ArrayList<>();
    public String itemTooltip = "";
    public int tooltipTimer = 0;

    // ========== ΗΧΟΣ ==========
    public Sound sound = new Sound();

    // ========== ΔΙΑΛΟΓΟΣ ΜΕ "ΓΡΑΨΙΜΟ" ==========
    public String fullDialogue = "";        // Το πλήρες κείμενο
    public String displayedDialogue = "";   // Όσο έχει εμφανιστεί μέχρι τώρα
    public int dialogueCharCounter = 0;     // Μετρητής χαρακτήρων
    public int dialogueSpeed = 1;           // Πόσοι χαρακτήρες ανά frame
    public int dialogueFrameCounter = 0;     // μετρητής frames
    public boolean dialogueTyping = false;  // Αν γράφεται ακόμα

    // === CUSTOM FONTS ===
    public Font maruMonica;
    public Font maruMonicaBold;
    public Font maruMonicaSmall;
    public Font maruMonicaLarge;

    // Σύστημα μέρας/νύχτας
    public int dayTime = 0; // 0=μέρα, 1=βράδυ, 2=νύχτα
    public int timeCounter = 0;
    public final int dayDuration = 9000; // Πόσα frames διαρκεί η μέρα (2.5 λεπτά στα 60fps)
    public final int transitionDuration = 3000; // ΜΙΣΟ λεπτό για τη μετάβαση (αντί για eveningDuration)
    public final int nightDuration = 9000; // 2.5 λεπτά
    // Μουσική μέρας/νύχτας
    public String currentDayMusic = "overworld_day";   // Μουσική μέρας
    public String currentNightMusic = "overworld_night"; // Μουσική νύχτας
    public float targetMusicVolume = 0.3f;  // Στόχος έντασης (0-1)
    public float currentMusicVolume = 0.3f; // Τρέχουσα ένταση
    public boolean isCrossfading = false;   // Αν γίνεται crossfade
    public String nextMusic = "";           // Επόμενη μουσική για crossfade
    public final float CROSSFADE_SPEED = 0.01f;
    // ΜΕΡΑ/ΝΥΧΤΑ
    public float targetDarkness = 0.0f; // Στόχος σκοταδιού
    public float currentDarkness = 0.0f; // Τρέχον σκοτάδι (για smooth transition)
    public float darknessAlpha = 0.0f; // Διαφάνεια σκοταδιού (0-255)
    public boolean hasLantern = false; // Αν ο παίκτης έχει φανάρι
    public Item lantern; // Το αντικείμενο φανάρι
    public int lanternRadius = 5; // Ακτίνα φωτισμού σε tiles

    // MERCHANT
    public ArrayList<Item> merchantItems = new ArrayList<>();
    public int shopOption = 0; // 0=BUY, 1=SELL, 2=EXIT
    public int shopMode = 0; // 0=BUY mode, 1=SELL mode
    public int selectedShopItem = 0;
    public int selectedSellItem = 0;
    public boolean talkingToMerchant = false;

    // DECORATIONS
    public ArrayList<ArrayList<Decoration>> decorations = new ArrayList<>();
    public ArrayList<House> houses = new ArrayList<>();


    // Game states
    public int gameState = 0;
    public final int playState = 0;
    public final int dialogueState = 1;
    public final int dialogueOptionsState = 3;
    public final int mapState = 5;
    public final int titleState = 6;  
    public final int controlsState = 7;
    public final int pauseState = 8;
    public final int quitConfirmState = 9;
    public final int pauseControlsState = 10;
    public final int shopState = 11;

    // επιλογές διαλόγου
    public String[] dialogueOptions = new String[3];
    public int selectedOption = 0;
    // Διάλογος
    public String currentDialogue = "";

    // ========== ΜΑΧΗ ==========
    public ArrayList<ArrayList<Enemy>> enemies = new ArrayList<>();
    public final int battleState = 4;
    public final int battleVictoryState = 12;
    public Enemy currentEnemy;
    // Random Encounters
    public int encounterStepCounter = 0;
    public int encounterRate = 20; // Κάθε 20 βήματα
    public String currentArea = "overworld"; // "overworld" ή "dungeon"
    // Transition Battle
    public boolean battleStarting = false;
    public int battleFadeAlpha = 0; // 0 = διάφανο, 255 = μαύρο
    public boolean battleFadeIn = false; // Αν γίνεται fade in
    public boolean battleFadeOut = false; // Αν γίνεται fade out
    public final int FADE_SPEED = 3; // Ταχύτητα fade
    public int battleWalkTimer = 0; // Μετρητής για animation περπατήματος
    public int battleWalkFrame = 0; // Frame animation
    public int battleTransitionTimer = 0;
    public final int BATTLE_TRANSITION_TIME = 60; // 1 δευτερόλεπτο στα 60fps
    public boolean battleEntering = false;
    public BufferedImage battleBackground;
    public ArrayList<BattleEnemy> battleEnemies = new ArrayList<>();
    public ArrayList<BattlePlayer> battlePlayers = new ArrayList<>();
    public int groundY = 0; // Το ύψος του εδάφους
    public int groundX = 0; // Η θέση Χ του εδάφους (για κλίση)
    public double groundAngle = 0.17; // 10 μοίρες σε radians (10 * π/180)createGroundGradient
    public int groundWidth = screenWidth; // Πλάτος εδάφους
    public int groundHeight = tileSize * 2; // Ύψος εδάφους
    BufferedImage groundImage;
    BufferedImage groundGrass; // Εικόνα για εξωτερικούς χώρους
    BufferedImage groundDungeon; // Εικόνα για dungeon
    // ========== ΝΕΟ ΣΥΣΤΗΜΑ ΜΑΧΗΣ ==========
    public BattleParty battleParty = new BattleParty();
    public int battleMenuOption = 0; // 0=Attack, 1=Class Skills, 2=Items, 3=Defend, 4=Flee
    public String[] battleMenuOptions = {"Attack", "Class Skills", "Items", "Defend", "Flee"};
    public int selectedTarget = 0;
    public boolean selectingTarget = false;
    public int selectedBoost = 0;
    public boolean selectingBoost = false;
    public boolean actionInProgress = false;
    public String lastKillerName = "";
    public boolean waitingForBattleMusic = false;
    public int battleAppearTimer = 0;
    public int battleAppearDurationFrames = 0;
    public int boostLoopLevel = 0; // 0 = κανένα loop, 1/2/3 = BOOSTLVL1S/2S/3S
    public int boostLoopTimer = 0;
    public final int BOOST_LOOP_INTERVAL = 45;
    public boolean battleEnterLatch = false;
    public boolean battleBLatch = false;
    public boolean battleCLatch = false;
    public boolean commandLocked = false;
    public long boostVisualStartTime = 0;
    public int boostBurstTimer = 0;
    public int boostBurstDuration = 0;
    public int boostBurstLevel = 0;
    public int boostBurstX = 0;
    public int boostBurstY = 0;
    public int hitFlashTimer = 0;
    public int hitFlashDuration = 0;
    public int hitFlashLevel = 0;
    public double boostSlashAngle = -0.65; // βασική διαγώνια φορά

    public int screenShakeTimer = 0;
    public int screenShakeDuration = 0;
    public int screenShakeStrength = 0;
    // camera zoom
    public int zoomPopTimer = 0;
    public int zoomPopDuration = 0;
    public float zoomPopScale = 1.0f;
    public float zoomPopMaxScale = 1.0f;
    // lunge
    public int lungeTimer = 0;
    public int lungeDuration = 0;
    public int lungeDistance = 0;
    public BattleEntity lungingActor = null;
    public int afterimageCount = 0;
    // swing attack trail
    public BattleEntity swingTrailActor = null;
    public int swingTrailTimer = 0;
    public int swingTrailDuration = 0;
    public int swingTrailLevel = 0;

    public int hitPauseTimer = 0;
    public final int HIT_PAUSE_DURATION = 8;
    public BufferedImage[] weaponIcons; // Για τα εικονίδια όπλων
    public int pendingExp = 0;
    public int pendingGold = 0;
    public String battleMessage = "";
    public int battleMessageTimer = 0;
    public String lastAction = "";        // ΝΕΟ: τελευταία ενέργεια που έγινε
    public int actionMessageTimer = 0;    // ΝΕΟ: πόσο θα εμφανίζεται
    public final int ACTION_MESSAGE_DURATION = 90; // ΝΕΟ: 1.5 sec στα 60fps
    public final int BATTLE_ANIMATION_SPEED = 8; // Πιο αργό animation (από 8)
    public final int BATTLE_DEATH_DELAY = 90; // 1.5 δευτερόλεπτο για death animation (60fps)
    public final int BATTLE_VICTORY_DELAY = 120; // 2 δευτερόλεπτα για rewards
    public int battleTurnDelay = 0;
    public final int BATTLE_TURN_DELAY_TIME = 150; // 1 δευτερόλεπτο delay
    public boolean waitingForNextTurn = false;
    public int battlePhase = 0; // 0=player turn start, 1=player attack, 2=player hurt, 3=enemy turn start, 4=enemy attack, 5=enemy hurt, 6=waiting
    public int battlePhaseTimer = 0;
    public final int BATTLE_PHASE_DELAY = 180; // 3 δευτερόλεπτα στα 60fps (180 = 3sec)
    public BattleEntity currentAttacker = null;
    public BattleEntity currentTarget = null;
    public BattleEnemy currentBattleEnemy = null;
    public int pendingDamage = 0;
    public PlayerAnimation playerBattleAnim;

    // variables για victory
    public int victoryTimer = 0;
    public int victoryExp = 0;
    public int victoryGold = 0;
    public boolean victoryRewardsShown = false;

    // ΠΟΖΕΣ
    BufferedImage playerDown1, playerDown2;
    BufferedImage playerUp1, playerUp2;
    BufferedImage playerLeft1, playerLeft2;
    BufferedImage playerRight1, playerRight2;

    BufferedImage currentPlayerImage; // Η τωρινή εικόνα που θα εμφανίζεται

    // TITLE SCREEN
    public int titleCommandNum = 0; // 0=START, 1=SETTINGS, 2=CONTROLS
    public BufferedImage titleLogo; // Το logo του παιχνιδιού
    // SETTINGS
    public int pauseCommandNum = 0; // 0=FULLSCREEN, 1=MUSIC, 2=SOUND, 3=CONTROLS, 4=QUIT
    public String[] pauseOptions = {"FULLSCREEN", "MUSIC VOLUME", "SOUND VOLUME", "CONTROLS", "QUIT GAME"};

    // Για volume control
    public int musicVolume = 30; // 0-100
    public int soundVolume = 70; // 0-100
    public int volumeSettingMode = 0; // 0=κανένα, 1=music, 2=sound
    public int quitConfirmOption = 0; // 0=NO, 1=YES

    int direction = 2; // 0=κάτω, 1=αριστερά, 2=δεξιά, 3=πάνω
    int frame = 0; // 0 ή 1 (δύο καρέ)
    int counter = 0; // Μετρητής για animation

    // Movement
    KeyHandler keyH = new KeyHandler();
    int playerSpeed = 4;

    // Ορίζουμε το μέγεθος του παραθύρου
    public GamePanel(JFrame window) {
        this.setPreferredSize(new Dimension(screenWidth, screenHeight));
        this.setBackground(Color.black);
        this.setDoubleBuffered(true); // Για καλύτερο ζωγράφισμα

        this.addKeyListener(keyH);
        this.setFocusable(true);

        this.window = window;
        this.device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();

        tileM = new TileManager(this); // Δώσε του access στο GamePanel  
        
        // npc
        for (int i = 0; i < maxMaps; i++) {
            npcs.add(new ArrayList<>());
            itemsOnGround.add(new ArrayList<>());
        }

        // Δημιούργησε τον player ως Entity
        player = new Entity(this);
        // player.worldX = 23 * tileSize;  // ή όποιο spawn point θες
        // player.worldY = 21 * tileSize;
        currentMap = 0;
        tileM.applyMapSizeToGamePanel(currentMap);

        TiledObjectData spawn = tileM.findMapObjectByName(currentMap, "player_spawn");
        if (spawn != null) {
            int spawnCol = (int)(spawn.x / originalTileSize);
            int spawnRow = (int)(spawn.y / originalTileSize) - 1;

            player.worldX = spawnCol * tileSize;
            player.worldY = spawnRow * tileSize;
        } else {
            player.worldX = 5 * tileSize;
            player.worldY = 5 * tileSize;
        }
        player.speed = 4;
        player.direction = "down";

        // NPCS NEW
        spawnTiledNPCs(currentMap);
        // ITEMS ON GROUND NEW
        spawnTiledChestLoot(currentMap);

        // ========== Δημιουργία των άλλων μελών της ομάδας ==========
        PartyMember assassin = new PartyMember(this, "assassin", "Assassin");
        assassin.worldX = player.worldX - tileSize; // Δίπλα στον κεντρικό ήρωα
        assassin.worldY = player.worldY;

        PartyMember mage = new PartyMember(this, "mage", "Mage");
        mage.worldX = player.worldX - tileSize * 2; // Πιο αριστερά
        mage.worldY = player.worldY;

        // Πρόσθεσέ τα στη λίστα
        partyMembers.add(assassin);
        partyMembers.add(mage);
        
        // ========== Δημιουργία λίστας εχθρών ==========
        for (int i = 0; i < maxMaps; i++) {
            enemies.add(new ArrayList<>());
        }
        // ========== Δημιουργία DECORATIONS ==========
        for (int i = 0; i < maxMaps; i++) {
            decorations.add(new ArrayList<>());
        }

        // Φόρτωση της εικόνας του ήρωα
        try {
            // Φόρτωση όλων των εικόνων
            playerDown1 = ImageIO.read(new File("res/player/player_down_1.png"));
            playerDown2 = ImageIO.read(new File("res/player/player_down_2.png"));
            playerUp1 = ImageIO.read(new File("res/player/player_up_1.png"));
            playerUp2 = ImageIO.read(new File("res/player/player_up_2.png"));
            playerLeft1 = ImageIO.read(new File("res/player/player_left_1.png"));
            playerLeft2 = ImageIO.read(new File("res/player/player_left_2.png"));
            playerRight1 = ImageIO.read(new File("res/player/player_right_1.png"));
            playerRight2 = ImageIO.read(new File("res/player/player_right_2.png"));
            
            // Ξεκίνα με την πρώτη εικόνα (κάτω)
            currentPlayerImage = playerDown1;
            
            System.out.println("Όλες οι εικόνες φορτώθηκαν επιτυχώς!");
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            titleLogo = ImageIO.read(new File("res/title/logo.png")); // Δημιούργησε αυτό το μονοπάτι
            System.out.println("Logo loaded successfully!");
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Using text fallback for logo");
        }

        // ========== ΦΟΡΤΩΣΗ BATTLE ANIMATIONS ΓΙΑ ΤΟΝ ΠΑΙΚΤΗ ΜΕ SPRITESHEET ==========
        try {
            // Φόρτωσε idle (3 frames)
            SpriteSheet idleSheet = new SpriteSheet("res/player/battle/idle.png", 64, 64);
            BufferedImage[] idleFrames = idleSheet.getAllFrames(); // 3 frames
            
            // Φόρτωσε hurt (2 frames)
            SpriteSheet hurtSheet = new SpriteSheet("res/player/battle/hurt.png", 64, 64);
            BufferedImage[] hurtFrames = hurtSheet.getAllFrames(); // 2 frames
            
            // Φόρτωσε death (3 frames)
            SpriteSheet deathSheet = new SpriteSheet("res/player/battle/death.png", 64, 64);
            BufferedImage[] deathFrames = deathSheet.getAllFrames(); // 3 frames
            
            // Φόρτωσε attack1 (π.χ. 6 frames)
            SpriteSheet attack1Sheet = new SpriteSheet("res/player/battle/attack.png", 64, 64);
            BufferedImage[] attack1Frames = attack1Sheet.getAllFrames(); // 6 frames
            
            // Δημιούργησε το animation object
            playerBattleAnim = new PlayerAnimation(idleFrames, hurtFrames, deathFrames, attack1Frames);
            
            System.out.println("Player battle animations loaded successfully with SpriteSheet!");
            
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Failed to load player battle animations");
        }

        // === FONTS ===
        try {
            // Φόρτωσε το κανονικό font
            maruMonica = Font.createFont(Font.TRUETYPE_FONT, new File("res/font/MaruMonica.ttf")).deriveFont(20f);
            maruMonicaBold = maruMonica.deriveFont(Font.BOLD, 24f);
            maruMonicaSmall = maruMonica.deriveFont(16f);
            maruMonicaLarge = maruMonica.deriveFont(Font.BOLD, 40f);
            
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(maruMonica);
            
            System.out.println("Custom font loaded successfully!");
        } catch (Exception e) {
            e.printStackTrace();
            // Fallback σε Arial
            maruMonica = new Font("Arial", Font.PLAIN, 20);
            maruMonicaBold = new Font("Arial", Font.BOLD, 24);
            maruMonicaSmall = new Font("Arial", Font.PLAIN, 16);
            maruMonicaLarge = new Font("Arial", Font.BOLD, 40);
        }

        // ========== ΔΗΜΙΟΥΡΓΙΑ ΦΑΝΑΡΙΟΥ ==========
        try {
            lantern = new Item("Lantern");
            lantern.isKeyItem = true;
            lantern.loadImage("res/items/lantern.png");
            inventory.addItem(lantern); // Αυτόματα θα πάει στα key items
            
            System.out.println("Lantern created successfully!");
        } catch (Exception e) {
            e.printStackTrace();
        }

        // ========== ΠΡΟΦΟΡΤΩΣΗ ΗΧΩΝ ==========
        // Μουσικές
        sound.preloadMusic("overworld_day", "overworld_day.wav");
        sound.preloadMusic("overworld_night", "overworld_night.wav");
        sound.preloadMusic("town_day", "town_day.wav");
        sound.preloadMusic("town_night", "town_night.wav");
        sound.preloadMusic("dungeon", "dungeon_music.wav");
        sound.preloadMusic("battle", "battle_music.wav");
        sound.preloadMusic("merchant_village", "Merchant.wav");
        
        // Ηχητικά εφέ
        sound.preloadSound("type", "type.wav");
        sound.preloadSound("dialogue_start", "dialogue_start.wav");
        sound.preloadSound("enemy_hit", "enemy_hit.wav");
        sound.preloadSound("hitmonster", "hitmonster.wav");
        sound.preloadSound("player_hit", "player_hit.wav");
        sound.preloadSound("level_up", "levelup.wav");
        sound.preloadSound("death", "death.wav");
        sound.preloadSound("run_away", "run_away.wav");
        sound.preloadSound("portal", "portal.wav");
        sound.preloadSound("stairs", "stairs.wav");
        sound.preloadSound("menu_open", "menu_open.wav");
        sound.preloadSound("menu_close", "menu_close.wav");
        sound.preloadSound("menu_select", "menu_select.wav");
        sound.preloadSound("equip", "equip.wav");
        sound.preloadSound("unequip", "unequip.wav");
        sound.preloadSound("enemy_appear", "enemy_appear.wav");
        sound.preloadSound("coin", "coin.wav");
        // ===== BATTLE SOUND EFFECTS =====
        sound.preloadBattleSound("ATTACKMISS", "attackMiss.wav");
        sound.preloadBattleSound("BOOSTASSASSIN", "boostAssassin.wav");
        sound.preloadBattleSound("BOOSTMAGE", "boostMage.wav");
        sound.preloadBattleSound("DEATHASSASSIN", "deathAssassin.wav");
        sound.preloadBattleSound("DEATHENEMY", "deathEnemy.wav");
        sound.preloadBattleSound("DEATHMAGE", "deathMage.wav");
        sound.preloadBattleSound("ENEMY_APPEAR", "enemy_appear.wav");
        sound.preloadBattleSound("GOBLIN_SLASH", "goblin_slash.wav");
        sound.preloadBattleSound("GUARD", "guard.wav");
        sound.preloadBattleSound("HEAL", "heal.wav");
        sound.preloadBattleSound("ITEM", "item.wav");
        sound.preloadBattleSound("LOWHPASSASSIN", "lowHpAssassin.wav");
        sound.preloadBattleSound("LOWHPMAGE", "LowHpMage.wav");
        sound.preloadBattleSound("MUSHROOM_ATTACK", "mushroom_attack.wav");
        sound.preloadBattleSound("SKELETON_ATTACK", "skeleton_attack.wav");
        sound.preloadBattleSound("SPEAR", "spear.wav");
        sound.preloadBattleSound("SPEAR2", "spear2.wav");
        sound.preloadBattleSound("STAFF", "staff.wav");
        sound.preloadBattleSound("STAFF2", "staff2.wav");
        sound.preloadBattleSound("SWORD", "sword.wav");
        sound.preloadBattleSound("SWORD2", "sword2.wav");
        sound.preloadBattleSound("THANKSASSASSIN", "thanksAssassin.wav");
        sound.preloadBattleSound("THANKSMAGE", "ThanksMage.wav");
        sound.preloadBattleSound("TURNASSASSIN", "turnAssassin.wav");
        sound.preloadBattleSound("TURNMAGE", "turnMage.wav");
        sound.preloadBattleSound("USEITEMASSASSIN", "useItemAssassin.wav");
        sound.preloadBattleSound("VICTORYASSASSIN", "victoryAssassin.wav");
        sound.preloadBattleSound("VICTORYMAGE", "victoryMage.wav");
        sound.preloadBattleSound("BOOSTCANCEL", "boostCancel.wav");
        sound.preloadBattleSound("BOOSTLVL1", "boostLvl1.wav");
        sound.preloadBattleSound("BOOSTLVL1S", "boostLvl1s.wav");
        sound.preloadBattleSound("BOOSTLVL2", "boostLvl2.wav");
        sound.preloadBattleSound("BOOSTLVL2S", "boostLvl2s.wav");
        sound.preloadBattleSound("BOOSTLVL3", "boostLvl3.wav");
        sound.preloadBattleSound("BOOSTLVL3S", "boostLvl3s.wav");
        
        // Ξεκίνα με την μουσική ΤΙΤΛΟΥ
        sound.preloadMusic("title", "title_music.wav");
        sound.playMusic("title");

        // MERCHANT ITEMS FOR SALE
        try {
            Item healthPotion = new Item("Health Potion");
            healthPotion.stackable = true;
            healthPotion.healAmount = 20;
            healthPotion.price = 50;
            healthPotion.loadImage("res/items/health_potion.png");
            merchantItems.add(healthPotion);
            
            Item manaPotion = new Item("Mana Potion");
            manaPotion.stackable = true;
            manaPotion.mpBonus = 20;
            manaPotion.price = 40;
            manaPotion.loadImage("res/items/potion_blue.png");
            merchantItems.add(manaPotion);
            
            Item ironSword = new Item("Iron Sword");
            ironSword.attackBonus = 5;
            ironSword.price = 200;
            ironSword.loadImage("res/items/iron_sword.png");
            merchantItems.add(ironSword);
            
            Item leatherArmor = new Item("Leather Armor");
            leatherArmor.defenseBonus = 3;
            leatherArmor.price = 150;
            leatherArmor.loadImage("res/items/leather_armor.png");
            merchantItems.add(leatherArmor);
            
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Προσθήκη δειγματικών decorations (στον overworld - χάρτης 0)
        // loadHouses();
        // try {
        //     // Statue - θέλουμε 2 tiles ύψος (96 pixels)
        //     BufferedImage originalStatue = ImageIO.read(new File("res/decorations/statue.png"));
        //     BufferedImage scaledStatue = scaleDecoration(originalStatue, tileSize * 2); // 96px
            
        //     decorations.get(3).add(new Decoration("Statue", scaledStatue, 
        //                                         9 * tileSize, 19 * tileSize, 
        //                                         scaledStatue.getWidth(), scaledStatue.getHeight(), true));
            
        //     // Stand - 1.5 tiles ύψος (72 pixels)
        //     BufferedImage originalStand = ImageIO.read(new File("res/decorations/stand.png"));
        //     BufferedImage scaledStand = scaleDecoration(originalStand, (int)(tileSize * 1.5));
            
        //     decorations.get(3).add(new Decoration("Stand", scaledStand, 
        //                                         18 * tileSize, 19 * tileSize,
        //                                         scaledStand.getWidth(), scaledStand.getHeight(), true));

        //     // house - 1.5 tiles ύψος (72 pixels)
        //     //BufferedImage originalHouse = ImageIO.read(new File("res/decorations/house_1.png"));
        //     //BufferedImage scaledHouse = scaleDecoration(originalHouse, (int)(tileSize * 6));
            
        //     //decorations.get(3).add(new Decoration("House", scaledHouse, 
        //                                         //10 * tileSize, 9 * tileSize,
        //                                         //scaledHouse.getWidth(), scaledHouse.getHeight(), true));
            
        // } catch (Exception e) {
        //     e.printStackTrace();
        // }

        Item mapItem = new Item("World Map");
        mapItem.isKeyItem = true;
        mapItem.loadImage("res/items/map.png");
        inventory.addItem(mapItem);  // Αυτόματα θα πάει στα key items

        // Δημιούργησε portals
        // portals.add(new Portal(0, 12 * tileSize, 8 * tileSize, 1, 10 * tileSize, 41 * tileSize)); // Overworld -> Dungeon
        // portals.add(new Portal(1, 9 * tileSize, 41 * tileSize, 0, 11 * tileSize, 9 * tileSize)); // Dungeon -> Overworld
        // portals.add(new Portal(0, 10 * tileSize, 39 * tileSize, 2, 12 * tileSize, 12 * tileSize)); // Overworld -> Merchant House
        // portals.add(new Portal(2, 12 * tileSize, 13 * tileSize, 0, 10 * tileSize, 40 * tileSize)); // Merchant House -> Overworld
        // portals.add(new Portal(0, 41 * tileSize, 7 * tileSize, 3, 4 * tileSize, 22 * tileSize)); // Overworld -> Town
        // portals.add(new Portal(3, 3 * tileSize, 22 * tileSize, 0, 41 * tileSize, 8 * tileSize)); // Town -> Overworld

        // //portals.add(new Portal(3, 40 * tileSize, 30 * tileSize, 6, 8 * tileSize, 8 * tileSize)); // Town -> region
        // portals.add(new Portal(3, 40 * tileSize, 28 * tileSize, 6, 20 * tileSize, 10 * tileSize)); // Town -> testmaps

        //ΞΕΚΙΝΑΩ ΜΕ TITLE
        gameState = titleState;
    }

    public void startGameThread() {
        gameThread = new Thread(this);
        gameThread.start();
    }

    @Override
    public void run() {
        double drawInterval = 1000000000 / 60;
        double nextDrawTime = System.nanoTime() + drawInterval;
        
        while (gameThread != null) {
            // ========== ΝΕΟ: Λήψη λιστών για τον τρέχοντα χάρτη ==========
            ArrayList<Entity> currentMapNPCs = npcs.get(currentMap);
            ArrayList<ItemOnGround> currentMapItems = itemsOnGround.get(currentMap);

            // Έλεγξε αν ο παίκτης πατάει Enter μπροστά από NPC
            if (keyH.enterPressed) {
                if (gameState == playState) {
                    // Πρώτα έλεγξε για NPCs (όπως πριν)
                    boolean enteredHouse = false;
        
                    // Υπολόγισε μπροστά από τον παίκτη
                    int checkX = player.worldX;
                    int checkY = player.worldY;
                    
                    switch(direction) {
                        case 0: checkY += tileSize; break; // down
                        case 1: checkX -= tileSize; break; // left
                        case 2: checkX += tileSize; break; // right
                        case 3: checkY -= tileSize; break; // up
                    }
                    
                    // ========== ΠΡΩΤΑ ΕΛΕΓΞΕ ΓΙΑ EXIT ΑΠΟ ΤΟ ΣΠΙΤΙ ==========
                    // Αν είμαστε μέσα σε σπίτι (χάρτης 4 ή 5)
                    if (currentMap == 4 || currentMap == 5) {
                        // Βρες το σπίτι στο οποίο βρισκόμαστε
                        for (House house : houses) {
                            if (house.interiorMap == currentMap) {
                                // Έλεγξε αν ο παίκτης είναι μπροστά από την πόρτα εξόδου
                                // Η έξοδος είναι στο σημείο που μπήκαμε (spawn point)
                                int doorX = house.spawnX;
                                int doorY = house.spawnY;
                                
                                int distanceX = Math.abs(player.worldX - doorX);
                                int distanceY = Math.abs(player.worldY - doorY);
                                
                                if (distanceX < tileSize && distanceY < tileSize) {
                                    System.out.println("Πάτησες Enter για έξοδο από το σπίτι!");
                                    
                                    if (house.hasAnimatedDoor) {
                                        // Ξεκίνα το animation κλεισίματος
                                        house.exitHouse();
                                        playSound("door_close");
                                        
                                        // Δημιούργησε νέο thread για να περιμένει να τελειώσει το animation
                                        Thread animationThread = new Thread(() -> {
                                            try {
                                                System.out.println("Exit thread started, waiting " + house.door.getAnimationDuration() + "ms");
                                                Thread.sleep(house.door.getAnimationDuration());
                                                System.out.println("Exit thread finished waiting, teleporting outside...");
                                                
                                                javax.swing.SwingUtilities.invokeLater(() -> {
                                                    currentMap = house.exteriorMap;
                                                    player.worldX = house.exitX;
                                                    player.worldY = house.exitY;
                                                    System.out.println("Teleported outside to map " + currentMap + " at (" + player.worldX/tileSize + "," + player.worldY/tileSize + ")");
                                                    
                                                    // Επανάφερε την εξωτερική μουσική
                                                    if (currentMap == 3) { // town
                                                        if (dayTime == 0 || dayTime == 3) {
                                                            sound.playMusic("town_day");
                                                        } else {
                                                            sound.playMusic("town_night");
                                                        }
                                                    }
                                                });
                                            } catch (InterruptedException e) {
                                                e.printStackTrace();
                                            }
                                        });
                                        animationThread.start();
                                    } else {
                                        // Αν δεν έχει animation, απλή έξοδος
                                        currentMap = house.exteriorMap;
                                        player.worldX = house.exitX;
                                        player.worldY = house.exitY;
                                        playSound("door_close");
                                        
                                        // Επανάφερε την εξωτερική μουσική
                                        if (currentMap == 3) { // town
                                            if (dayTime == 0 || dayTime == 3) {
                                                sound.playMusic("town_day");
                                            } else {
                                                sound.playMusic("town_night");
                                            }
                                        }
                                    }
                                    
                                    enteredHouse = true;
                                    keyH.enterPressed = false;
                                    break;
                                }
                            }
                        }
                    }
                    
                    // ΜΟΝΟ ΑΝ ΔΕΝ ΕΙΜΑΣΤΕ ΣΕ EXIT, ΕΛΕΓΞΕ ΓΙΑ ΕΙΣΟΔΟ ΣΕ ΣΠΙΤΙ
                    if (!enteredHouse) {
                        // Έλεγξε όλα τα houses
                        for (House house : houses) {
                            // Μόνο αν είμαστε στον σωστό εξωτερικό χάρτη
                            if (currentMap == house.exteriorMap) {
                                // Έλεγξε αν το μπροστινό tile είναι στην περιοχή της πόρτας
                                if (house.doorArea.contains(checkX, checkY)) {
                                    System.out.println("Πάτησες Enter μπροστά από την πόρτα!");
                                    
                                    if (house.hasAnimatedDoor) {
                                        // Ξεκίνα το animation
                                        System.out.println("Ξεκινάω animation πόρτας...");
                                        house.enterHouse();
                                        playSound("door_open");
                                        sound.playMusic("merchant_village");
                                        currentMusicVolume = musicVolume / 100.0f;
                                        sound.setMusicVolume(currentMusicVolume);
                                        
                                        // Δημιούργησε νέο thread για να περιμένει να τελειώσει το animation
                                        Thread animationThread = new Thread(() -> {
                                            try {
                                                System.out.println("Thread started, waiting " + house.door.getAnimationDuration() + "ms");
                                                Thread.sleep(house.door.getAnimationDuration());
                                                System.out.println("Thread finished waiting, teleporting...");
                                                
                                                javax.swing.SwingUtilities.invokeLater(() -> {
                                                    currentMap = house.interiorMap;
                                                    player.worldX = house.spawnX;
                                                    player.worldY = house.spawnY;
                                                    System.out.println("Teleported to map " + currentMap + " at (" + player.worldX/tileSize + "," + player.worldY/tileSize + ")");
                                                });
                                            } catch (InterruptedException e) {
                                                e.printStackTrace();
                                            }
                                        });
                                        animationThread.start();
                                    } else {
                                        // Αν δεν έχει animation, απλή είσοδος
                                        currentMap = house.interiorMap;
                                        player.worldX = house.spawnX;
                                        player.worldY = house.spawnY;
                                        playSound("door_open");
                                        sound.playMusic("merchant_village");
                                        currentMusicVolume = musicVolume / 100.0f;
                                        sound.setMusicVolume(currentMusicVolume);
                                        System.out.println("Μπήκες στο σπίτι (χωρίς animation)");
                                    }
                                    
                                    enteredHouse = true;
                                    break;
                                }
                            }
                        }
                    }
                    if (!enteredHouse) {
                        handleNPCInteraction(currentMapNPCs);
                    }
                    keyH.enterPressed = false;
                } 
                else if (gameState == dialogueState || gameState == dialogueOptionsState) {
                    if (keyH.enterPressed) {
                        if (gameState == dialogueOptionsState) {
                            if (!dialogueTyping) { // Μόνο αν έχει τελειώσει το γράψιμο
                                handleDialogueOption(selectedOption);
                            } else {
                                // Αν γράφεται ακόμα, τελείωσέ το αμέσως
                                displayedDialogue = fullDialogue;
                                dialogueTyping = false;
                                playSound("type");
                            }
                        } else {
                            if (dialogueTyping) {
                                displayedDialogue = fullDialogue;
                                dialogueTyping = false;
                                playSound("type");
                            } else {
                                if (talkingToMerchant) {
                                    gameState = shopState;
                                    talkingToMerchant = false;
                                } else {
                                    gameState = playState;
                                }
                            }
                        }
                        keyH.enterPressed = false;
                    }
                }
            }

            // ========== Μάζεμα items (χρησιμοποιεί currentMapItems) ==========
            if (keyH.ePressed) {
                for (int i = 0; i < currentMapItems.size(); i++) {
                    ItemOnGround item = currentMapItems.get(i);
                    
                    int distanceX = Math.abs(player.worldX - item.worldX);
                    int distanceY = Math.abs(player.worldY - item.worldY);
                    
                    if (distanceX < tileSize && distanceY < tileSize) {
                        if (inventory.addItem(item.item)) {
                            currentMapItems.remove(i);
                            startDialogue("Πήρες: " + item.name + "!");
                            gameState = dialogueState;
                        }
                        break;
                    }
                }
                
                try {
                    Thread.sleep(200);
                    keyH.ePressed = false;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            if (keyH.iPressed) {
                if (gameState == playState) {
                    gameState = inventoryState;
                    showInventory = true;
                    playSound("menu_open");
                } else if (gameState == inventoryState) {
                    gameState = playState;
                    showInventory = false;
                    playSound("menu_close");
                }
                
                try {
                    Thread.sleep(200);
                    keyH.iPressed = false;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            if (keyH.qPressed) {
                player.quests.clear();
                System.out.println("All quests cleared!");
                try { Thread.sleep(200); } catch (Exception e) {}
                keyH.qPressed = false;
            }

            // ========== ΠΛΟΗΓΗΣΗ ΣΤΟΝ ΔΙΑΛΟΓΟ (εκτός από το enterPressed) ==========
            if (gameState == dialogueOptionsState) {
                if (keyH.upPressed) {
                    selectedOption--;
                    if (selectedOption < 0) selectedOption = 0;
                    try { Thread.sleep(150); } catch (Exception e) {}
                    keyH.upPressed = false;
                }
                if (keyH.downPressed) {
                    selectedOption++;
                    if (selectedOption >= 3) selectedOption = 2;
                    try { Thread.sleep(150); } catch (Exception e) {}
                    keyH.downPressed = false;
                }
            }
            // ========== ΠΛΗΚΤΡΟ Μ (ΧΑΡΤΗΣ) ==========
            if (keyH.mPressed) {
                if (gameState == playState) {
                    // Έλεγξε αν έχει το χάρτη
                    boolean hasMap = false;
                    for (int i = 0; i < inventory.keyItems.length; i++) {
                        if (inventory.keyItems[i] != null && inventory.keyItems[i].name.equals("World Map")) {
                            hasMap = true;
                            break;
                        }
                    }
                    
                    if (hasMap) {
                        gameState = mapState;
                    } else {
                        startDialogue("Δεν έχεις χάρτη ακόμα...");
                        gameState = dialogueState;
                    }
                    keyH.mPressed = false;
                    try { Thread.sleep(200); } catch (Exception e) {}
                } else if (gameState == mapState) {
                    gameState = playState;
                    keyH.mPressed = false;
                    try { Thread.sleep(200); } catch (Exception e) {}
                }
            }

            // ========== SHOP STATE ==========
            if (gameState == shopState) {
                if (keyH.escapePressed) {
                    gameState = playState;
                    talkingToMerchant = false;
                    shopOption = 0; // Reset to BUY
                    selectedShopItem = 0;
                    selectedSellItem = 0;
                    keyH.escapePressed = false;
                    try { Thread.sleep(200); } catch (Exception e) {}
                }

                // Πλοήγηση μεταξύ BUY/SELL/EXIT με πάνω/κάτω
                if (keyH.upPressed) {
                    shopOption--;
                    if (shopOption < 0) shopOption = 2;
                    playSound("menu_select");
                    // Ενημέρωσε το shopMode ανάλογα
                    shopMode = (shopOption == 0) ? 0 : (shopOption == 1) ? 1 : 0;
                    repaint();
                    try { Thread.sleep(150); } catch (Exception e) {}
                    keyH.upPressed = false;
                }
                if (keyH.downPressed) {
                    shopOption++;
                    if (shopOption > 2) shopOption = 0;
                    playSound("menu_select");
                    // Ενημέρωσε το shopMode ανάλογα
                    shopMode = (shopOption == 0) ? 0 : (shopOption == 1) ? 1 : 0;
                    repaint();
                    try { Thread.sleep(150); } catch (Exception e) {}
                    keyH.downPressed = false;
                }
                
                // Μόνο αν ΔΕΝ είμαστε στο EXIT (shopOption != 2), επιτρέπεται πλοήγηση στα items
                if (shopOption != 2) {
                    if (shopOption == 0) { // BUY mode
                        if (keyH.leftPressed) {
                            selectedShopItem--;
                            if (selectedShopItem < 0) selectedShopItem = 0;
                            playSound("menu_select");
                            repaint();
                            try { Thread.sleep(150); } catch (Exception e) {}
                            keyH.leftPressed = false;
                        }
                        if (keyH.rightPressed) {
                            selectedShopItem++;
                            if (selectedShopItem >= merchantItems.size()) selectedShopItem = merchantItems.size() - 1;
                            playSound("menu_select");
                            repaint();
                            try { Thread.sleep(150); } catch (Exception e) {}
                            keyH.rightPressed = false;
                        }
                    } else if (shopOption == 1) { // SELL mode
                        // Υπολόγισε τα προς πώληση items
                        ArrayList<Item> sellableItems = new ArrayList<>();
                        for (int i = 0; i < inventory.storage.length; i++) {
                            if (inventory.storage[i] != null && !inventory.storage[i].isKeyItem) {
                                sellableItems.add(inventory.storage[i]);
                            }
                        }
                        
                        if (sellableItems.size() > 0) {
                            if (keyH.leftPressed) {
                                selectedSellItem--;
                                if (selectedSellItem < 0) selectedSellItem = 0;
                                playSound("menu_select");
                                repaint();
                                try { Thread.sleep(150); } catch (Exception e) {}
                                keyH.leftPressed = false;
                            }
                            if (keyH.rightPressed) {
                                selectedSellItem++;
                                if (selectedSellItem >= sellableItems.size()) selectedSellItem = sellableItems.size() - 1;
                                playSound("menu_select");
                                repaint();
                                try { Thread.sleep(150); } catch (Exception e) {}
                                keyH.rightPressed = false;
                            }
                        }
                    }
                }
                
                // Χειρισμός ENTER - διαφορετικός για κάθε option
                if (keyH.enterPressed) {
                    playSound("menu_select");
                    
                    if (shopOption == 0) { // BUY
                        if (selectedShopItem >= 0 && selectedShopItem < merchantItems.size()) {
                            Item selected = merchantItems.get(selectedShopItem);
                            if (player.gold >= selected.price) {
                                player.gold -= selected.price;
                                // Δημιούργησε αντίγραφο του item
                                Item boughtItem = new Item(selected.name);
                                boughtItem.stackable = selected.stackable;
                                boughtItem.healAmount = selected.healAmount;
                                boughtItem.manaAmount = selected.manaAmount;
                                boughtItem.attackBonus = selected.attackBonus;
                                boughtItem.defenseBonus = selected.defenseBonus;
                                boughtItem.magicBonus = selected.magicBonus;
                                boughtItem.hpBonus = selected.hpBonus;
                                boughtItem.mpBonus = selected.mpBonus;
                                boughtItem.speedBonus = selected.speedBonus;
                                boughtItem.price = selected.price;
                                boughtItem.isKeyItem = selected.isKeyItem;
                                boughtItem.loadImage("res/items/" + selected.name.toLowerCase().replace(" ", "_") + ".png");
                                
                                inventory.addItem(boughtItem);
                                playSound("coin");
                                startDialogue("Αγόρασες " + selected.name + "!");
                                gameState = dialogueState;
                            } else {
                                startDialogue("Δεν έχεις αρκετό χρυσό!");
                                gameState = dialogueState;
                            }
                        }
                    } else if (shopOption == 1) { // SELL
                        ArrayList<Item> sellableItems = new ArrayList<>();
                        for (int i = 0; i < inventory.storage.length; i++) {
                            if (inventory.storage[i] != null && !inventory.storage[i].isKeyItem) {
                                sellableItems.add(inventory.storage[i]);
                            }
                        }
                        
                        if (sellableItems.size() > 0 && selectedSellItem < sellableItems.size()) {
                            Item selected = sellableItems.get(selectedSellItem);
                            int sellPrice = selected.price / 2;
                            
                            // Αφαίρεσε το item από το inventory
                            for (int i = 0; i < inventory.storage.length; i++) {
                                if (inventory.storage[i] == selected) {
                                    if (selected.stackable && selected.amount > 1) {
                                        selected.amount--;
                                    } else {
                                        inventory.storage[i] = null;
                                    }
                                    break;
                                }
                            }
                            
                            player.gold += sellPrice;
                            playSound("coin");
                            startDialogue("Πούλησες " + selected.name + " για " + sellPrice + " χρυσά!");
                            gameState = dialogueState;
                            
                            // Επαναφορά επιλογής
                            selectedSellItem = 0;
                        }
                    } else if (shopOption == 2) { // EXIT
                        gameState = playState;
                        talkingToMerchant = false;
                        shopOption = 0; // Reset to BUY
                        selectedShopItem = 0;
                        selectedSellItem = 0;
                    }
                    
                    keyH.enterPressed = false;
                }
            }

            // ----- INVENTORY NAVIGATION -----
            if (gameState == inventoryState) {
                // Αλλαγή mode με Shift 
                if (keyH.shiftPressed) {
                    inventory.inventoryMode = (inventory.inventoryMode == 0) ? 1 : 0;
                    playSound("menu_select");
                    
                    // Αν πάμε σε equipment mode, επέλεξε το πρώτο slot
                    if (inventory.inventoryMode == 1) {
                        inventory.selectedEquipSlot = 0;
                    } else {
                        inventory.selectedStorageSlot = 0;
                    }
                    
                    try { Thread.sleep(200); } catch (Exception e) {}
                    keyH.shiftPressed = false;
                }               
                if (inventory.inventoryMode == 0) { // Storage mode
                    // Πλοήγηση στο 4x2 grid
                    if (keyH.upPressed) {
                        inventory.selectedStorageSlot -= 4;
                        if (inventory.selectedStorageSlot < 0) inventory.selectedStorageSlot = 0;
                        playSound("menu_select");
                        try { Thread.sleep(150); } catch (Exception e) {}
                        keyH.upPressed = false;
                    }
                    if (keyH.downPressed) {
                        inventory.selectedStorageSlot += 4;
                        if (inventory.selectedStorageSlot >= 8) inventory.selectedStorageSlot = 7;
                        playSound("menu_select");
                        try { Thread.sleep(150); } catch (Exception e) {}
                        keyH.downPressed = false;
                    }
                    if (keyH.leftPressed) {
                        inventory.selectedStorageSlot--;
                        if (inventory.selectedStorageSlot < 0) inventory.selectedStorageSlot = 0;
                        playSound("menu_select");
                        try { Thread.sleep(150); } catch (Exception e) {}
                        keyH.leftPressed = false;
                    }
                    if (keyH.rightPressed) {
                        inventory.selectedStorageSlot++;
                        if (inventory.selectedStorageSlot >= 8) inventory.selectedStorageSlot = 7;
                        playSound("menu_select");
                        try { Thread.sleep(150); } catch (Exception e) {}
                        keyH.rightPressed = false;
                    }
                    
                    // Χρήση item με Enter
                    if (keyH.enterPressed) {
                        Item selected = inventory.storage[inventory.selectedStorageSlot];
                        if (selected != null) {
                            if (selected.healAmount > 0) { // Potion
                                playSound("use_items");
                                player.heal(selected.healAmount);
                                inventory.removeFromStorage(inventory.selectedStorageSlot);
                                startDialogue("Ήπιες " + selected.name + "!");
                                gameState = dialogueState;
                            } else { // Equippable item
                                int targetSlot = -1;
                                
                                if (selected.name.contains("Sword") || selected.name.contains("Blade")) {
                                    targetSlot = 3; // SWORD
                                } else if (selected.name.contains("Shield")) {
                                    targetSlot = 5; // SHIELD
                                } else if (selected.name.contains("Helmet") || selected.name.contains("Hat")) {
                                    targetSlot = 1; // HELMET
                                } else if (selected.name.contains("Chest") || selected.name.contains("Armor")) {
                                    targetSlot = 4; // CHEST
                                } else if (selected.name.contains("Gloves") || selected.name.contains("Gauntlets")) {
                                    targetSlot = 6; // GLOVES
                                } else if (selected.name.contains("Belt")) {
                                    targetSlot = 7; // BELT
                                } else if (selected.name.contains("Boots") || selected.name.contains("Shoes")) {
                                    targetSlot = 8; // BOOTS
                                } else if (selected.name.contains("Ring")) {
                                    targetSlot = 0; // RING
                                } else if (selected.name.contains("Necklace") || selected.name.contains("Amulet")) {
                                    targetSlot = 2; // NECKLACE
                                }
                                
                                if (targetSlot != -1) {
                                    // Πριν εξοπλίσεις, αφαίρεσε τα μπόνους του παλιού item (αν υπάρχει)
                                    Item oldItem = inventory.getEquipSlot(targetSlot);
                                    player.recalcStats();
                                    if (oldItem != null) {
                                        player.attack -= oldItem.attackBonus;
                                        player.defense -= oldItem.defenseBonus;
                                        player.magicAttack -= oldItem.magicBonus;
                                        player.maxHp -= oldItem.hpBonus;
                                        player.maxMp -= oldItem.mpBonus;
                                        player.speed_stat -= oldItem.speedBonus;
                                        
                                        // Αν το maxHp μειώθηκε, προσάρμοσε και το τρέχον hp
                                        if (player.hp > player.maxHp) {
                                            player.hp = player.maxHp;
                                        }
                                        if (player.mp > player.maxMp) {
                                            player.mp = player.maxMp;
                                        }
                                    }
                                    
                                    // Εξόπλισε το νέο item
                                    playSound("equip");
                                    inventory.equipItem(inventory.selectedStorageSlot, targetSlot);
                                    
                                    // Πρόσθεσε τα μπόνους του νέου item
                                    player.attack += selected.attackBonus;
                                    player.defense += selected.defenseBonus;
                                    player.magicAttack += selected.magicBonus;
                                    player.maxHp += selected.hpBonus;
                                    player.maxMp += selected.mpBonus;
                                    player.speed_stat += selected.speedBonus;
                                    
                                    // Αύξησε hp/mp αν χρειάζεται
                                    player.hp += selected.hpBonus;
                                    player.mp += selected.mpBonus;
                                
                                } else {
                                    inventory.inventoryMode = 1;
                                    inventory.selectedEquipSlot = 0;
                                }
                            }
                        }
                        try { Thread.sleep(200); } catch (Exception e) {}
                        keyH.enterPressed = false;
                    }
                }
                else { // Equipment mode
                    // Πλοήγηση στο 3x3 grid
                    if (keyH.upPressed) {
                        inventory.selectedEquipSlot -= 3;
                        if (inventory.selectedEquipSlot < 0) inventory.selectedEquipSlot = 0;
                        playSound("menu_select");
                        try { Thread.sleep(150); } catch (Exception e) {}
                        keyH.upPressed = false;
                    }
                    if (keyH.downPressed) {
                        inventory.selectedEquipSlot += 3;
                        if (inventory.selectedEquipSlot >= 9) inventory.selectedEquipSlot = 8;
                        playSound("menu_select");
                        try { Thread.sleep(150); } catch (Exception e) {}
                        keyH.downPressed = false;
                    }
                    if (keyH.leftPressed) {
                        inventory.selectedEquipSlot--;
                        if (inventory.selectedEquipSlot < 0) inventory.selectedEquipSlot = 0;
                        playSound("menu_select");
                        try { Thread.sleep(150); } catch (Exception e) {}
                        keyH.leftPressed = false;
                    }
                    if (keyH.rightPressed) {
                        inventory.selectedEquipSlot++;
                        if (inventory.selectedEquipSlot >= 9) inventory.selectedEquipSlot = 8;
                        playSound("menu_select");
                        try { Thread.sleep(150); } catch (Exception e) {}
                        keyH.rightPressed = false;
                    }
                    
                    if (keyH.enterPressed) {
                        Item selected = inventory.getEquipSlot(inventory.selectedEquipSlot);
                        if (selected != null) {
                            playSound("unequip");
                            // Αφαίρεσε τα μπόνους
                            player.attack -= selected.attackBonus;
                            player.defense -= selected.defenseBonus;
                            player.magicAttack -= selected.magicBonus;
                            player.maxHp -= selected.hpBonus;
                            player.maxMp -= selected.mpBonus;
                            player.speed_stat -= selected.speedBonus;
                            
                            if (player.hp > player.maxHp) player.hp = player.maxHp;
                            if (player.mp > player.maxMp) player.mp = player.maxMp;
                            
                            inventory.unequipItem(inventory.selectedEquipSlot);
                            player.recalcStats();
                        }
                        try { Thread.sleep(200); } catch (Exception e) {}
                        keyH.enterPressed = false;
                    }
                }
            }
            // ========== BATTLE STATE ==========
            else if (gameState == battleState) {
                if (battleEntering) {
                    battleTransitionTimer++;
                    battleWalkTimer++;
                    
                    // Animation περπατήματος (εναλλαγή καρέ κάθε 10 frames)
                    if (battleWalkTimer > 10) {
                        battleWalkTimer = 0;
                        battleWalkFrame = (battleWalkFrame == 0) ? 1 : 0;
                    }
                    
                    // Κίνηση εχθρών προς τα μέσα
                    for (BattleEnemy be : battleEnemies) {
                        be.x += (be.targetX - be.x) / 10;
                        if (Math.abs(be.x - be.targetX) < 1) be.x = be.targetX;
                    }
                    
                    // Κίνηση παικτών προς τα μέσα ΜΕ ANIMATION
                    for (BattlePlayer bp : battlePlayers) {
                        bp.x -= (bp.x - bp.targetX) / 10;
                        if (Math.abs(bp.x - bp.targetX) < 1) bp.x = bp.targetX;
                        
                        // ΑΛΛΑΞΕ ΕΙΚΟΝΑ ΓΙΑ ANIMATION
                        if (battleWalkFrame == 0) {
                            bp.image = playerLeft1;
                        } else {
                            bp.image = playerLeft2;
                        }
                        
                        // Ενημέρωσε και την εικόνα στο BattleEntity
                        if (!battleParty.party.isEmpty()) {
                            battleParty.party.get(0).image = bp.image;
                        }
                    }
                    
                    // Όταν τελειώσει το transition
                    if (battleTransitionTimer >= BATTLE_TRANSITION_TIME) {
                        battleEntering = false;
                        // Βεβαιώσου ότι είναι στις σωστές θέσεις
                        for (BattleEnemy be : battleEnemies) be.x = be.targetX;
                        for (BattlePlayer bp : battlePlayers) {
                            bp.x = bp.targetX;
                            bp.image = playerLeft1; // Στατική εικόνα όταν σταματήσει
                        }
                        
                        // Ενημέρωσε τα BattleEntities
                        if (!battleParty.party.isEmpty()) {
                            battleParty.party.get(0).image = playerLeft1;
                        }
                    }
                    
                    repaint();
                } else {
                    // ========== ΚΑΝΟΝΙΚΗ ΛΟΓΙΚΗ ΜΑΧΗΣ ==========
                    boolean battleEnterJustPressed = keyH.enterPressed && !battleEnterLatch;
                    boolean battleBJustPressed = keyH.bPressed && !battleBLatch;
                    boolean battleCJustPressed = keyH.cPressed && !battleCLatch;

                    battleEnterLatch = keyH.enterPressed;
                    battleBLatch = keyH.bPressed;
                    battleCLatch = keyH.cPressed;

                    updateBattleVisuals();
                    updateBattleAction();
                    updateBoostBurstEffect();
                    updateHitReactions();
                    updateZoomPop();
                    updateLunge();
                    updateSwingTrail();
                    // Έλεγξε αν τελείωσε η μάχη
                    if (battleParty.battleEnded) {
                        if (battleParty.party.isEmpty()) {
                            // Ηττα - επαναφορά
                            player.worldX = spawnPoints[0][0];
                            player.worldY = spawnPoints[0][1];
                            player.hp = player.maxHp;
                            player.mp = player.maxMp;
                            gameState = playState;
                            
                            // Επιστροφή στη μουσική του χάρτη
                            returnToMapMusic();
                            
                            // Επαναφορά του battleParty για επόμενη μάχη
                            battleParty = new BattleParty();
                        } 
                        continue;
                    }

                    if (waitingForNextTurn) {
                        commandLocked = true;
                        battleTurnDelay++;
                        if (battleTurnDelay >= BATTLE_TURN_DELAY_TIME) {
                            waitingForNextTurn = false;
                            battleTurnDelay = 0;

                            selectedBoost = 0;
                            boostLoopLevel = 0;
                            boostLoopTimer = 0;
                            sound.stopBattleLoop();

                            battleParty.nextTurn();
                            commandLocked = false;
                        }
                        repaint();
                    }                   
                    // Αν είναι σειρά του εχθρού
                    else if (!battleParty.isPlayerTurn()) {
                        if (!actionInProgress) {
                            BattleEntity currentEnemy = battleParty.getCurrentTurn();
                            if (currentEnemy != null && currentEnemy.canAct()) {
                                actionInProgress = true;
                                currentEnemy.enterState(CombatState.WINDUP);
                            }
                        }
                        repaint();
                    }
                    if (waitingForBattleMusic) {
                        battleAppearTimer++;
                        if (battleAppearTimer >= battleAppearDurationFrames) {
                            sound.playMusic("battle");
                            currentMusicVolume = musicVolume / 100.0f;
                            sound.setMusicVolume(currentMusicVolume);
                            waitingForBattleMusic = false;
                        }
                    }
                    // Αν είναι σειρά του παίκτη
                    else if (battleParty.isPlayerTurn() && !selectingTarget && !selectingBoost && !actionInProgress && !commandLocked) {
                        BattleEntity currentPlayer = battleParty.getCurrentTurn();
                        int maxSelectableBoost = Math.min(3, currentPlayer.bp);

                        if (battleBJustPressed) {
                            if (selectedBoost < maxSelectableBoost) {
                                selectedBoost++;

                                if (selectedBoost == 1) {
                                    boostVisualStartTime = System.currentTimeMillis();
                                    sound.playBattleSE("BOOSTLVL1");
                                } else if (selectedBoost == 2) {
                                    boostVisualStartTime = System.currentTimeMillis();
                                    sound.playBattleSE("BOOSTLVL2");
                                } else if (selectedBoost == 3) {
                                    boostVisualStartTime = System.currentTimeMillis();
                                    sound.playBattleSE("BOOSTLVL3");
                                }

                                if (currentPlayer.name.equals("Assassin")) {
                                    sound.playBattleSE("BOOSTASSASSIN");
                                } else if (currentPlayer.name.equals("Mage")) {
                                    sound.playBattleSE("BOOSTMAGE");
                                }

                                boostLoopLevel = selectedBoost;
                                battleMessage = "Boost: " + selectedBoost + "   Press B/C";
                            }
                            keyH.bPressed = false;
                            repaint();
                        }

                        if (battleCJustPressed) {
                            if (selectedBoost > 0) {
                                sound.stopBattleLoop();
                                selectedBoost--;
                                boostVisualStartTime = System.currentTimeMillis();
                                sound.playBattleSE("BOOSTCANCEL");
                                boostLoopLevel = selectedBoost;
                                battleMessage = "Boost: " + selectedBoost + "   Press B/C";
                            }
                            keyH.cPressed = false;
                            repaint();
                        }

                        if (boostLoopLevel == 1) {
                            if (!sound.isBattleLoopPlaying("BOOSTLVL1S")) {
                                sound.playBattleLoopFromMs("BOOSTLVL1S", 100);
                            }
                        } else if (boostLoopLevel == 2) {
                            if (!sound.isBattleLoopPlaying("BOOSTLVL2S")) {
                                sound.playBattleLoopFromMs("BOOSTLVL2S", 850);
                            }
                        } else if (boostLoopLevel == 3) {
                            if (!sound.isBattleLoopPlaying("BOOSTLVL3S")) {
                                sound.playBattleLoopFromMs("BOOSTLVL3S", 450);
                            }
                        } else {
                            sound.stopBattleLoop();
                        }
                        // Πλοήγηση στο μενού
                        if (keyH.leftPressed) {
                            battleMenuOption--;
                            if (battleMenuOption < 0) battleMenuOption = battleMenuOptions.length - 1;
                            playSound("menu_select");
                            try { Thread.sleep(150); } catch (Exception e) {}
                            keyH.leftPressed = false;
                            repaint();
                        }
                        if (keyH.rightPressed) {
                            battleMenuOption++;
                            if (battleMenuOption >= battleMenuOptions.length) battleMenuOption = 0;
                            playSound("menu_select");
                            try { Thread.sleep(150); } catch (Exception e) {}
                            keyH.rightPressed = false;
                            repaint();
                        }
                        
                        if (battleEnterJustPressed) {
                            playSound("menu_select");
                            
                            currentPlayer = battleParty.getCurrentTurn();
                            
                            switch(battleMenuOption) {
                                case 0: // ATTACK
                                    commandLocked = true;
                                    selectingTarget = true;
                                    selectedTarget = 0;
                                    boostLoopLevel = selectedBoost;
                                    battleMessage = "Επίλεξε στόχο!  ENTER=Confirm  ESC=Back";
                                    break;
                                    
                                case 1: // CLASS SKILLS
                                    try { Thread.sleep(1000); } catch (Exception e) {}
                                    break;
                                    
                                case 2: // ITEMS
                                    try { Thread.sleep(1000); } catch (Exception e) {}
                                    break;
                                    
                                case 3: // DEFEND
                                    sound.stopBattleLoop();
                                    selectedBoost = 0;
                                    boostLoopLevel = 0;
                                    boostLoopTimer = 0;

                                    commandLocked = true;
                                    currentPlayer.defending = true;
                                    currentPlayer.enterState(CombatState.DEFENDING);
                                    playActorAnimation(currentPlayer, "defend");
                                    battleMessage = currentPlayer.name + " guards!";
                                    sound.playBattleSE("GUARD");
                                    actionInProgress = true;
                                    waitingForNextTurn = false;
                                    battleTurnDelay = 0;
                                    break;
                                    
                                case 4: // FLEE
                                    commandLocked = true;
                                    battleMessage = "Απέδρασες!";
                                    repaint();
                                    try { Thread.sleep(1000); } catch (Exception e) {}
                                    gameState = playState;
                                    returnToMapMusic();

                                    break;
                            }
                            keyH.enterPressed = false;
                            repaint();
                        }
                    }
                    // Αν είμαστε σε κατάσταση επιλογής στόχου
                    else if (selectingTarget) {
                        BattleEntity currentPlayer = battleParty.getCurrentTurn();
                        // Πλοήγηση στους εχθρούς
                        if (keyH.leftPressed) {
                            selectedTarget--;
                            if (selectedTarget < 0) selectedTarget = 0;
                            playSound("menu_select");
                            try { Thread.sleep(150); } catch (Exception e) {}
                            keyH.leftPressed = false;
                            repaint();
                        }
                        if (keyH.rightPressed) {
                            selectedTarget++;
                            if (selectedTarget >= battleParty.enemies.size()) 
                                selectedTarget = battleParty.enemies.size() - 1;
                            playSound("menu_select");
                            try { Thread.sleep(150); } catch (Exception e) {}
                            keyH.rightPressed = false;
                            repaint();
                        }
                        
                        if (battleEnterJustPressed) {
                            if (selectedTarget >= 0 && selectedTarget < battleParty.enemies.size()) {
                                // ΣΗΜΑΝΤΙΚΟ: Το selectedTarget αναφέρεται στο battleParty.enemies
                                BattleEntity target = battleParty.enemies.get(selectedTarget);
                                currentPlayer.queuedAction = "attack";
                                currentPlayer.queuedTarget = target;
                                currentPlayer.boostUsed = selectedBoost;
                                currentPlayer.spendBP(selectedBoost);
                                sound.stopBattleLoop();
                                boostLoopLevel = 0;
                                boostLoopTimer = 0;
                                boostVisualStartTime = 0;
                                currentPlayer.enterState(CombatState.WINDUP);

                                actionInProgress = true;
                                selectingTarget = false;
                                commandLocked = true;
                                battleMessage = "";
                                keyH.enterPressed = false;
                                repaint();
                                continue;
                            }
                        }
                        
                        // Ακύρωση με ESC
                        if (keyH.escapePressed) {
                            sound.stopBattleLoop();
                            selectingTarget = false;
                            selectingBoost = false;
                            selectedBoost = 0;
                            boostLoopLevel = 0;
                            boostLoopTimer = 0;
                            commandLocked = false;
                            battleMessage = "";
                            keyH.escapePressed = false;
                            repaint();
                        }
                    }
                }
            }
            // ========== BATTLE VICTORY STATE ==========   
            else if (gameState == battleVictoryState) {
                victoryTimer++;
                // Πρόσθεσε τα rewards μόνο μία φορά
                if (!victoryRewardsShown) {
                    battleMessage = "Νίκη! +" + victoryExp + " EXP, +" + victoryGold + " Gold!";
                    player.addExp(victoryExp);
                    player.gold += victoryGold;
                    victoryRewardsShown = true;
                    sound.stopMusic();
                    playSound("levelup");
                }
                
                // Αν πατήσει Enter, ξεκίνα fade out
                if (keyH.enterPressed && !battleFadeOut) {
                    battleFadeOut = true;
                    battleFadeAlpha = 0;
                    keyH.enterPressed = false;
                }
                
                repaint();
            }
            // ========== TITLE STATE ==========
            if (gameState == titleState) {
                if (keyH.upPressed) {
                    titleCommandNum--;
                    if (titleCommandNum < 0) {
                        titleCommandNum = 2;
                    }
                    playSound("menu_select");
                    try { Thread.sleep(150); } catch (Exception e) {}
                    keyH.upPressed = false;
                }
                if (keyH.downPressed) {
                    titleCommandNum++;
                    if (titleCommandNum > 2) {
                        titleCommandNum = 0;
                    }
                    playSound("menu_select");
                    try { Thread.sleep(150); } catch (Exception e) {}
                    keyH.downPressed = false;
                }
                if (keyH.enterPressed) {
                    playSound("menu_select");
                    if (titleCommandNum == 0) { // START GAME
                        gameState = playState;
                        sound.stopMusic();
                        // Ξεκίνα με τη σωστή μουσική ανάλογα με την ώρα
                        if (dayTime == 0 || dayTime == 3) {
                            sound.playMusic("overworld_day");
                        } else {
                            sound.playMusic("overworld_night");
                        }
                        currentMusicVolume = musicVolume / 100.0f;
                        sound.setMusicVolume(currentMusicVolume);
                    } else if (titleCommandNum == 1) { // SETTINGS
                        startDialogue("Settings will be available in a future update!");
                        gameState = dialogueState;
                    } else if (titleCommandNum == 2) { // CONTROLS
                        gameState = controlsState;
                    }
                    keyH.enterPressed = false;
                    try { Thread.sleep(200); } catch (Exception e) {}
                }
            }

            // ΜΕΣΑ στο σύστημα μέρας/νύχτας
            if (gameState == playState) {
                timeCounter++;
                
                // Ενημέρωσε την ώρα και το targetDarkness
                if (dayTime == 0) { // Μέρα
                    targetDarkness = 0.0f;
                    if (timeCounter > dayDuration) {
                        dayTime = 1; // Πήγαινε σε μετάβαση προς νύχτα
                        timeCounter = 0;
                        
                        // Ξεκίνα crossfade προς νυχτερινή μουσική
                        if (currentMap == 0) { // Μόνο για overworld
                            startMusicCrossfade("overworld_night");
                        } else if (currentMap == 3) { // Μόνο για overworld
                            startMusicCrossfade("town_night");
                        }
                    }
                } else if (dayTime == 1) { // Μετάβαση από μέρα σε νύχτα
                    targetDarkness = 150.0f;
                    if (timeCounter > transitionDuration) {
                        dayTime = 2; // Πήγαινε σε νύχτα
                        timeCounter = 0;
                    }
                } else if (dayTime == 2) { // Νύχτα
                    targetDarkness = 150.0f;
                    if (timeCounter > nightDuration) {
                        dayTime = 3; // Πήγαινε σε μετάβαση προς μέρα
                        timeCounter = 0;
                        
                        // Ξεκίνα crossfade προς μεσημεριανή μουσική
                        if (currentMap == 0) { // Μόνο για overworld
                            startMusicCrossfade("overworld_day");
                        } else if (currentMap == 3) { // Μόνο για overworld
                            startMusicCrossfade("town_day");
                        }
                    }
                } else if (dayTime == 3) { // Μετάβαση από νύχτα σε μέρα
                    targetDarkness = 0.0f;
                    if (timeCounter > transitionDuration) {
                        dayTime = 0; // Πήγαινε σε μέρα
                        timeCounter = 0;
                    }
                }
                
                // Smooth transition - σταδιακή προσαρμογή του currentDarkness προς το targetDarkness
                float speed = 0.5f;
                if (currentDarkness < targetDarkness) {
                    currentDarkness = Math.min(targetDarkness, currentDarkness + speed);
                } else if (currentDarkness > targetDarkness) {
                    currentDarkness = Math.max(targetDarkness, currentDarkness - speed);
                }
                
                // Crossfade μουσικής - σταδιακή αλλαγή έντασης
                if (isCrossfading) {
                    // Μείωσε σταδιακά την τρέχουσα μουσική
                    currentMusicVolume = Math.max(0, currentMusicVolume - CROSSFADE_SPEED);
                    sound.setMusicVolume(currentMusicVolume);
                    
                    // Όταν φτάσει στο 0, άλλαξε μουσική και ξεκίνα να την ανεβάζεις
                    if (currentMusicVolume <= 0) {
                        sound.playMusic(nextMusic);
                        // Συνέχισε με την επιθυμητή ένταση (από το musicVolume)
                        targetMusicVolume = musicVolume / 100.0f;
                        isCrossfading = false;
                    }
                } else if (sound.getCurrentMusic().equals(nextMusic) && !isCrossfading) {
                    // Σταδιακή αύξηση της νέας μουσικής μέχρι το target
                    if (currentMusicVolume < targetMusicVolume) {
                        currentMusicVolume = Math.min(targetMusicVolume, currentMusicVolume + CROSSFADE_SPEED);
                        sound.setMusicVolume(currentMusicVolume);
                    }
                }
                
                // Έλεγξε αν ο παίκτης έχει φανάρι
                hasLantern = false;
                for (int i = 0; i < inventory.keyItems.length; i++) {
                    if (inventory.keyItems[i] != null && inventory.keyItems[i].name.equals("Lantern")) {
                        hasLantern = true;
                        break;
                    }
                }
            }

            // ========== CONTROLS STATE ==========
            if (gameState == controlsState) {
                if (keyH.enterPressed || keyH.mPressed || keyH.iPressed) {
                    gameState = titleState;
                    keyH.enterPressed = false;
                    keyH.mPressed = false;
                    keyH.iPressed = false;
                    try { Thread.sleep(200); } catch (Exception e) {}
                }
            }

            // ========== ESCAPE BUTTON ==========
            if (keyH.escapePressed) {
                if (gameState == playState) {
                    gameState = pauseState; // Πήγαινε στο pause menu
                    pauseCommandNum = 0;
                    volumeSettingMode = 0;
                    playSound("menu_open");
                } else if (gameState == pauseState) {
                    gameState = playState; // Γύρνα πίσω στο παιχνίδι
                    playSound("menu_close");
                } else if (gameState == quitConfirmState) {
                    gameState = pauseState; // Γύρνα πίσω στο pause menu
                    playSound("menu_select");
                }
                keyH.escapePressed = false;
                try { Thread.sleep(200); } catch (Exception e) {}
            }

            // ========== PAUSE STATE ==========
            if (gameState == pauseState) {
                if (volumeSettingMode == 0) { // Κανονική πλοήγηση μενού
                    if (keyH.upPressed) {
                        pauseCommandNum--;
                        if (pauseCommandNum < 0) pauseCommandNum = pauseOptions.length - 1;
                        playSound("menu_select");
                        try { Thread.sleep(150); } catch (Exception e) {}
                        keyH.upPressed = false;
                    }
                    if (keyH.downPressed) {
                        pauseCommandNum++;
                        if (pauseCommandNum >= pauseOptions.length) pauseCommandNum = 0;
                        playSound("menu_select");
                        try { Thread.sleep(150); } catch (Exception e) {}
                        keyH.downPressed = false;
                    }
                    if (keyH.enterPressed) {
                        playSound("menu_select");
                        
                        if (pauseCommandNum == 0) { // FULLSCREEN
                            toggleFullScreen();
                        } else if (pauseCommandNum == 1) { // MUSIC VOLUME
                            volumeSettingMode = 1;
                        } else if (pauseCommandNum == 2) { // SOUND VOLUME
                            volumeSettingMode = 2;
                        } else if (pauseCommandNum == 3) { // CONTROLS
                            gameState = pauseControlsState;;
                        } else if (pauseCommandNum == 4) { // QUIT GAME
                            gameState = quitConfirmState;
                            quitConfirmOption = 0; // Default: NO
                        }
                        keyH.enterPressed = false;
                    }
                } else { // Ρύθμιση volume
                    if (keyH.leftPressed) {
                        if (volumeSettingMode == 1) { // Music
                            musicVolume = Math.max(0, musicVolume - 10);
                            currentMusicVolume = musicVolume / 100.0f;
                            sound.setMusicVolume(musicVolume); // Θα το φτιάξουμε
                        } else { // Sound
                            soundVolume = Math.max(0, soundVolume - 10);
                        }
                        playSound("menu_select");
                        try { Thread.sleep(100); } catch (Exception e) {}
                        keyH.leftPressed = false;
                    }
                    if (keyH.rightPressed) {
                        if (volumeSettingMode == 1) { // Music
                            musicVolume = Math.min(100, musicVolume + 10);
                            currentMusicVolume = musicVolume / 100.0f;
                            sound.setMusicVolume(musicVolume);
                        } else { // Sound
                            soundVolume = Math.min(100, soundVolume + 10);
                        }
                        playSound("menu_select");
                        try { Thread.sleep(100); } catch (Exception e) {}
                        keyH.rightPressed = false;
                    }
                    if (keyH.enterPressed || keyH.escapePressed) {
                        volumeSettingMode = 0; // Γύρνα πίσω στο μενού
                        playSound("menu_select");
                        keyH.enterPressed = false;
                        keyH.escapePressed = false;
                    }
                }
            }
            // ========== PAUSE CONTROLS STATE ==========
            if (gameState == pauseControlsState) {
                if (keyH.enterPressed || keyH.escapePressed) {
                    gameState = pauseState; // Γύρνα πίσω στο pause menu
                    keyH.enterPressed = false;
                    keyH.escapePressed = false;
                    try { Thread.sleep(200); } catch (Exception e) {}
                }
            }

            // ========== QUIT CONFIRM STATE ==========
            if (gameState == quitConfirmState) {
                if (keyH.leftPressed || keyH.rightPressed) {
                    quitConfirmOption = (quitConfirmOption == 0) ? 1 : 0;
                    playSound("menu_select");
                    try { Thread.sleep(150); } catch (Exception e) {}
                    keyH.leftPressed = false;
                    keyH.rightPressed = false;
                }
                if (keyH.enterPressed) {
                    playSound("menu_select");
                    if (quitConfirmOption == 0) { // YES
                        System.exit(0); // Κλείσε το παιχνίδι
                    } else { // NO
                        gameState = pauseState; // Γύρνα πίσω
                    }
                    keyH.enterPressed = false;
                }
                if (keyH.escapePressed) {
                    gameState = pauseState; // Γύρνα πίσω
                    keyH.escapePressed = false;
                }
            }
            
            // ========== Ενημέρωση όλων των NPCs του τρέχοντος χάρτη ==========
            if (gameState == playState) {
                for (Entity npc : currentMapNPCs) {
                    if (npc instanceof NPC_OldMan) {
                        ((NPC_OldMan) npc).update();
                    }
                }
            }

            // ========== Κίνηση ήρωα (εκτός από μικρή αλλαγή στα portals) ==========
            // 1. ΕΝΗΜΕΡΩΣΗ: Κουνάμε τον ήρωα και αλλάζουμε κατεύθυνση
            boolean moving = false;

            if (gameState == playState && !battleStarting) {
                for (House house : houses) {
                    house.updateDoor();
                }
                // Δοκιμάζουμε πρώτα την οριζόντια κίνηση (X)
                if (keyH.leftPressed) {
                    direction = 1; // Αριστερά
                    
                    // Δοκίμασε να κουνηθείς αριστερά
                    int nextWorldX = player.worldX - playerSpeed;
                    
                    // Έλεγξε πρώτα με tiles
                    boolean tileCollision = hasWorldCollision(nextWorldX, player.worldY);

                    // Έλεγξε με decorations
                    boolean decorationCollision = false;
                    ArrayList<Decoration> currentMapDecorations = decorations.get(currentMap);
                    for (Decoration dec : currentMapDecorations) {
                        if (dec.solid) {
                            if (checkDecorationCollision(player, dec, nextWorldX, player.worldY)) {
                                decorationCollision = true;
                                break;
                            }
                        }
                    }
                    
                    // Έλεγξε με τον NPC (αν είναι στερεός)
                    boolean npcCollision = false;
                    // Έλεγξε collision ΜΟΝΟ αν το NPC είναι στατικό (solid && !moving)
                    for (Entity npc : currentMapNPCs) {
                        if (npc.solid && !npc.moving) {
                            if (checkEntityCollision(player, npc, nextWorldX, player.worldY)) {
                                npcCollision = true;
                                break;
                            }
                        }
                    }
                    
                    if (!tileCollision && !npcCollision && !decorationCollision) {
                        player.worldX = nextWorldX;
                        moving = true;

                        // ========== ΕΛΕΓΧΟΣ ΠΕΡΙΟΧΗΣ ΜΕ ΒΑΣΗ ΤΟΝ ΧΑΡΤΗ ==========
                        TiledObjectData encounterZone = getCurrentEncounterZone();

                        if (encounterZone != null) {
                            currentArea = encounterZone.getProperty("areaType");
                        } else {
                            currentArea = "safe";
                        }
                        
                        // ========== RANDOM ENCOUNTER CHECK ==========
                        encounterStepCounter++;

                        if (encounterStepCounter >= encounterRate) {
                            encounterStepCounter = 0;

                            double chance = Math.random();
                            double encounterChance = 0;

                            if (encounterZone != null) {
                                try {
                                    encounterChance = Double.parseDouble(encounterZone.getProperty("rate"));
                                } catch (Exception e) {
                                    encounterChance = 0.2;
                                }
                            }

                            if (chance < encounterChance) {
                                startRandomEncounter();
                            }
                        }
                    }
                }
                else if (keyH.rightPressed) {
                    direction = 2; // Δεξιά
                    
                    // Δοκίμασε να κουνηθείς δεξιά
                    int nextWorldX = player.worldX + playerSpeed;
                    
                    // Έλεγξε πρώτα με tiles
                    boolean tileCollision = hasWorldCollision(nextWorldX, player.worldY);

                    // Έλεγξε με decorations
                    boolean decorationCollision = false;
                    ArrayList<Decoration> currentMapDecorations = decorations.get(currentMap);
                    for (Decoration dec : currentMapDecorations) {
                        if (dec.solid) {
                            if (checkDecorationCollision(player, dec, nextWorldX, player.worldY)) {
                                decorationCollision = true;
                                break;
                            }
                        }
                    }
                    
                    // Έλεγξε με τον NPC (αν είναι στερεός)
                    boolean npcCollision = false;
                    // Έλεγξε collision ΜΟΝΟ αν το NPC είναι στατικό (solid && !moving)
                    for (Entity npc : currentMapNPCs) {
                        if (npc.solid && !npc.moving) {
                            if (checkEntityCollision(player, npc, nextWorldX, player.worldY)) {
                                npcCollision = true;
                                break;
                            }
                        }
                    }
                    
                    if (!tileCollision && !npcCollision && !decorationCollision) {
                        player.worldX = nextWorldX;
                        moving = true;

                        // ========== ΕΛΕΓΧΟΣ ΠΕΡΙΟΧΗΣ ΜΕ ΒΑΣΗ ΤΟΝ ΧΑΡΤΗ ==========
                        TiledObjectData encounterZone = getCurrentEncounterZone();

                        if (encounterZone != null) {
                            currentArea = encounterZone.getProperty("areaType");
                        } else {
                            currentArea = "safe";
                        }
                        
                        // ========== RANDOM ENCOUNTER CHECK ==========
                        encounterStepCounter++;

                        if (encounterStepCounter >= encounterRate) {
                            encounterStepCounter = 0;

                            double chance = Math.random();
                            double encounterChance = 0;

                            if (encounterZone != null) {
                                try {
                                    encounterChance = Double.parseDouble(encounterZone.getProperty("rate"));
                                } catch (Exception e) {
                                    encounterChance = 0.2;
                                }
                            }

                            if (chance < encounterChance) {
                                startRandomEncounter();
                            }
                        }
                    }
                }

                // Μετά δοκιμάζουμε την κάθετη κίνηση (Y)
                if (keyH.upPressed) {
                    direction = 3; // Πάνω
                    
                    // Δοκίμασε να κουνηθείς πάνω
                    int nextWorldY = player.worldY - playerSpeed;
                    
                    // Έλεγξε πρώτα με tiles
                    boolean tileCollision = hasWorldCollision(player.worldX, nextWorldY);

                    // Έλεγξε με decorations
                    boolean decorationCollision = false;
                    ArrayList<Decoration> currentMapDecorations = decorations.get(currentMap);
                    for (Decoration dec : currentMapDecorations) {
                        if (dec.solid) {
                            if (checkDecorationCollision(player, dec, player.worldX, nextWorldY)) {
                                decorationCollision = true;
                                break;
                            }
                        }
                    }
                    
                    // Έλεγξε με τον NPC (αν είναι στερεός)
                    boolean npcCollision = false;
                    // Έλεγξε collision ΜΟΝΟ αν το NPC είναι στατικό (solid && !moving)
                    for (Entity npc : currentMapNPCs) {
                        if (npc.solid && !npc.moving) {
                            if (checkEntityCollision(player, npc, player.worldX, nextWorldY)) {
                                npcCollision = true;
                                break;
                            }
                        }
                    }
                    
                    if (!tileCollision && !npcCollision && !decorationCollision) {
                        player.worldY = nextWorldY;
                        moving = true;

                        // ========== ΕΛΕΓΧΟΣ ΠΕΡΙΟΧΗΣ ΜΕ ΒΑΣΗ ΤΟΝ ΧΑΡΤΗ ==========
                        TiledObjectData encounterZone = getCurrentEncounterZone();

                        if (encounterZone != null) {
                            currentArea = encounterZone.getProperty("areaType");
                        } else {
                            currentArea = "safe";
                        }
                        
                        // ========== RANDOM ENCOUNTER CHECK ==========
                        encounterStepCounter++;

                        if (encounterStepCounter >= encounterRate) {
                            encounterStepCounter = 0;

                            double chance = Math.random();
                            double encounterChance = 0;

                            if (encounterZone != null) {
                                try {
                                    encounterChance = Double.parseDouble(encounterZone.getProperty("rate"));
                                } catch (Exception e) {
                                    encounterChance = 0.2;
                                }
                            }

                            if (chance < encounterChance) {
                                startRandomEncounter();
                            }
                        }
                    }
                }
                else if (keyH.downPressed) {
                    direction = 0; // Κάτω
                    
                    // Δοκίμασε να κουνηθείς κάτω
                    int nextWorldY = player.worldY + playerSpeed;
                    
                    // Έλεγξε πρώτα με tiles
                    boolean tileCollision = hasWorldCollision(player.worldX, nextWorldY);

                    // Έλεγξε με decorations
                    boolean decorationCollision = false;
                    ArrayList<Decoration> currentMapDecorations = decorations.get(currentMap);
                    for (Decoration dec : currentMapDecorations) {
                        if (dec.solid) {
                            if (checkDecorationCollision(player, dec, player.worldX, nextWorldY)) {
                                decorationCollision = true;
                                break;
                            }
                        }
                    }
                    
                    // Έλεγξε με τον NPC (αν είναι στερεός)
                    boolean npcCollision = false;
                    // Έλεγξε collision ΜΟΝΟ αν το NPC είναι στατικό (solid && !moving)
                    for (Entity npc : currentMapNPCs) {
                        if (npc.solid && !npc.moving) {
                            if (checkEntityCollision(player, npc, player.worldX, nextWorldY)) {
                                npcCollision = true;
                                break;
                            }
                        }
                    }
                    
                    
                    if (!tileCollision && !npcCollision && !decorationCollision) {
                        player.worldY = nextWorldY;
                        moving = true;

                        // ========== ΕΛΕΓΧΟΣ ΠΕΡΙΟΧΗΣ ΜΕ ΒΑΣΗ ΤΟΝ ΧΑΡΤΗ ==========
                        TiledObjectData encounterZone = getCurrentEncounterZone();

                        if (encounterZone != null) {
                            currentArea = encounterZone.getProperty("areaType");
                        } else {
                            currentArea = "safe";
                        }
                        
                        // ========== RANDOM ENCOUNTER CHECK ==========
                        encounterStepCounter++;

                        if (encounterStepCounter >= encounterRate) {
                            encounterStepCounter = 0;

                            double chance = Math.random();
                            double encounterChance = 0;

                            if (encounterZone != null) {
                                try {
                                    encounterChance = Double.parseDouble(encounterZone.getProperty("rate"));
                                } catch (Exception e) {
                                    encounterChance = 0.2;
                                }
                            }

                            if (chance < encounterChance) {
                                startRandomEncounter();
                            }
                        }
                    }
                }

                // 🚧 ΟΡΙΑ (τα αφήνουμε για εξτρά ασφάλεια)
                if (player.worldX < 0) player.worldX = 0;
                if (player.worldX > worldWidth - tileSize) player.worldX = worldWidth - tileSize;
                if (player.worldY < 0) player.worldY = 0;
                if (player.worldY > worldHeight - tileSize) player.worldY = worldHeight - tileSize;

                // Έλεγξε για portals
                ArrayList<TiledObjectData> portalObjects = tileM.getMapObjectsByLayer(currentMap, "portals");

                for (TiledObjectData portalObj : portalObjects) {
                    int portalX = (int)(portalObj.x / originalTileSize) * tileSize;
                    int portalY = (int)(portalObj.y / originalTileSize) * tileSize;

                    int portalWidthTiles = Math.max(1, portalObj.width / originalTileSize);
                    int portalHeightTiles = Math.max(1, portalObj.height / originalTileSize);

                    int portalWidthWorld = portalWidthTiles * tileSize;
                    int portalHeightWorld = portalHeightTiles * tileSize;

                    boolean touchingPortal =
                            player.worldX + tileSize > portalX &&
                            player.worldX < portalX + portalWidthWorld &&
                            player.worldY + tileSize > portalY &&
                            player.worldY < portalY + portalHeightWorld;

                    if (touchingPortal) {
                        int targetMap = portalObj.getPropertyInt("targetMap", currentMap);
                        int targetCol = portalObj.getPropertyInt("targetX", 5);
                        int targetRow = portalObj.getPropertyInt("targetY", 5);

                        currentMap = targetMap;
                        tileM.applyMapSizeToGamePanel(currentMap);

                        player.worldX = targetCol * tileSize;
                        player.worldY = targetRow * tileSize;

                        // μουσική
                        if (currentMap == 0) { // Overworld
                            if (dayTime == 0 || dayTime == 3) {
                                sound.playMusic("overworld_day");
                            } else {
                                sound.playMusic("overworld_night");
                            }
                        } else if (currentMap == 1) { // Dungeon
                            sound.playMusic("dungeon");
                        } else if (currentMap == 3) { // Town
                            if (dayTime == 0 || dayTime == 3) {
                                sound.playMusic("town_day");
                            } else {
                                sound.playMusic("town_night");
                            }
                        } else {
                            sound.playMusic("merchant_village");
                        }

                        currentMusicVolume = musicVolume / 100.0f;
                        sound.setMusicVolume(currentMusicVolume);

                        playSound("portal");

                        String portalMessage = portalObj.getProperty("message");
                        if (portalMessage == null || portalMessage.isEmpty()) {
                            portalMessage = "Ταξίδεψες σε άλλη περιοχή!";
                        }

                        startDialogue(portalMessage);
                        gameState = dialogueState;
                        break;
                    }
                }

                // ========== ΝΕΟ: Έλεγξε αν ο παίκτης ακουμπάει εχθρό ==========
                ArrayList<Enemy> currentMapEnemies = enemies.get(currentMap);
                for (int i = 0; i < currentMapEnemies.size(); i++) {
                    Enemy enemy = currentMapEnemies.get(i);
                    int distanceX = Math.abs(player.worldX - enemy.worldX);
                    int distanceY = Math.abs(player.worldY - enemy.worldY);
                    
                    if (distanceX < tileSize && distanceY < tileSize) {
                        currentEnemy = enemy;
                        gameState = battleState;
                        break;
                    }
                }
            }

            // Ενημέρωσε την κάμερα (worldX, worldY) να ακολουθεί τον παίκτη
            worldX = player.worldX - screenWidth/2 + tileSize/2;
            worldY = player.worldY - screenHeight/2 + tileSize/2;

            // Μην αφήνεις την κάμερα να βγει έξω από τον κόσμο
            if (worldX < 0) worldX = 0;
            if (worldX > worldWidth - screenWidth) worldX = worldWidth - screenWidth;
            if (worldY < 0) worldY = 0;
            if (worldY > worldHeight - screenHeight) worldY = worldHeight - screenHeight;
            
            // 🔄 ANIMATION: Αλλάζουμε εικόνα ανάλογα με την κατεύθυνση
            if (moving) {
                // Animation του βασικού ήρωα
                counter++;
                if (counter > 10) {
                    if (frame == 0) {
                        frame = 1;
                    } else {
                        frame = 0;
                    }
                    counter = 0;
                }
            } else {
                frame = 0;
                counter = 0;
            }

            // ========== ΚΙΝΗΣΗ ΤΩΝ ΥΠΟΛΟΙΠΩΝ ΜΕΛΩΝ (ΠΑΝΤΑ, ΑΚΟΜΑ ΚΑΙ ΟΤΑΝ Ο ΠΑΙΚΤΗΣ ΕΙΝΑΙ ΑΚΙΝΗΤΟΣ) ==========
            // Αποθήκευση θέσεων παίκτη (για follow system)
            boolean playerMoving = keyH.leftPressed || keyH.rightPressed || keyH.upPressed || keyH.downPressed;

            if (playerMoving) {
                playerPositions.add(0, new Point(player.worldX, player.worldY));

                if (playerPositions.size() > 200) {
                    playerPositions.remove(playerPositions.size() - 1);
                }
            }

            // Κράτα λίγες θέσεις για performance
            if (playerPositions.size() > 200) {
                playerPositions.remove(playerPositions.size() - 1);
            }
            updatePartyMembers();
            
            // Αποφάσισε ποια εικόνα θα δείξεις με βάση direction και frame
            if (direction == 0) { // Κάτω
                if (frame == 0) {
                    currentPlayerImage = playerDown1;
                } else {
                    currentPlayerImage = playerDown2;
                }
            } else if (direction == 1) { // Αριστερά
                if (frame == 0) {
                    currentPlayerImage = playerLeft1;
                } else {
                    currentPlayerImage = playerLeft2;
                }
            } else if (direction == 2) { // Δεξιά
                if (frame == 0) {
                    currentPlayerImage = playerRight1;
                } else {
                    currentPlayerImage = playerRight2;
                }
            } else if (direction == 3) { // Πάνω
                if (frame == 0) {
                    currentPlayerImage = playerUp1;
                } else {
                    currentPlayerImage = playerUp2;
                }
            }

            // ========== ΔΙΑΛΟΓΟΣ ΜΕ "ΓΡΑΨΙΜΟ" ==========
            if (gameState == dialogueState || gameState == dialogueOptionsState) {
                if (dialogueTyping) {
                    dialogueFrameCounter++;
                    // Γράψε 1 χαρακτήρα κάθε 2 frames (πιο αργά)
                    if (dialogueFrameCounter >= 2) {
                        if (dialogueCharCounter < fullDialogue.length()) {
                            displayedDialogue += fullDialogue.charAt(dialogueCharCounter);
                            dialogueCharCounter++;
                            
                            // Παίξε ήχο γραψίματος (κάθε 2 χαρακτήρες)
                            if (dialogueCharCounter % 2 == 0) {
                                playSound("type");
                            }
                        } else {
                            dialogueTyping = false;
                        }
                        dialogueFrameCounter = 0;
                    }
                }
            }

            // ========== BATTLE FADE EFFECT ==========
            if (battleFadeOut) {
                battleFadeAlpha += FADE_SPEED;
                if (battleFadeAlpha >= 255) {
                    battleFadeAlpha = 255;
                    
                    // Αν είμαστε σε victory state, γύρνα στο playState
                    if (gameState == battleVictoryState) {
                        gameState = playState;
                        returnToMapMusic();
                        battleParty = new BattleParty();
                        pendingExp = 0;
                        pendingGold = 0;
                        victoryRewardsShown = false;
                    } else {
                        // Αλλιώς ξεκίνα μάχη (για το κανονικό fade in)
                        gameState = battleState;
                        battleEntering = true;
                        battleTransitionTimer = 0;
                        battleWalkTimer = 0;
                        battleWalkFrame = 0;
                        battleStarting = false;
                        
                        setupBattleEntities();
                        battleFadeIn = true;
                    }
                    battleFadeOut = false;
                }
            }

            if (battleFadeIn) {
                battleFadeAlpha -= FADE_SPEED;
                if (battleFadeAlpha <= 0) {
                    battleFadeAlpha = 0;
                    battleFadeIn = false;
                }
            }
            
            // 2. ΣΧΕΔΙΑΣΗ
            repaint();
            
            // 3. ΣΥΓΧΡΟΝΙΣΜΟΣ (ίδιο όπως πριν)
            try {
                double remainingTime = nextDrawTime - System.nanoTime();
                remainingTime = remainingTime / 1000000;
                if (remainingTime < 0) remainingTime = 0;
                Thread.sleep((long) remainingTime);
                nextDrawTime += drawInterval;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void handleDialogueOption(int option) {
        if (fullDialogue.contains("Φύλακας")) {  // ΑΛΛΑΓΗ: currentDialogue -> fullDialogue
            if (fullDialogue.contains("Χρειάζομαι βοήθεια")) {  // ΑΛΛΑΓΗ
                if (option == 0) {
                    player.quests.removeIf(q -> q.name.equals("Καθάρισμα τεράτων"));
                    
                    Quest newQuest = new Quest("Καθάρισμα τεράτων", "Σκότωσε 5 goblins");
                    newQuest.targetItem = "Goblin Ear";
                    newQuest.requiredAmount = 5;
                    newQuest.active = true;
                    player.quests.add(newQuest);
                    
                    startDialogue("Σε ευχαριστώ! Γύρνα όταν έχεις 5 αυτιά goblin!");
                    gameState = dialogueState;
                } else if (option == 1) {
                    startDialogue("Κρίμα... Αν αλλάξεις γνώμη, είμαι εδώ.");
                    gameState = dialogueState;
                }
            }
            else if (fullDialogue.contains("Πώς πάει η αποστολή")) {  // ΑΛΛΑΓΗ
                if (option == 0) {
                    boolean hasItem = false;
                    for (int i = 0; i < inventory.storage.length; i++) {
                        Item item = inventory.storage[i];
                        if (item != null && item.name.equals("Goblin Ear") && item.amount >= 5) {
                            hasItem = true;
                            item.amount -= 5;
                            if (item.amount == 0) {
                                inventory.storage[i] = null;
                            }
                            break;
                        }
                    }
                    
                    if (hasItem) {
                        player.gold += 100;
                        startDialogue("Μπράβο! Ορίστε 100 χρυσά ως ανταμοιβή!");
                        for (Quest q : player.quests) {
                            if (q.name.equals("Καθάρισμα τεράτων")) {
                                q.completed = true;
                                break;
                            }
                        }
                    } else {
                        startDialogue("Δεν έχεις ακόμα 5 αυτιά goblin... Γύρνα όταν τα μαζέψεις!");
                    }
                    gameState = dialogueState;
                } else {
                    startDialogue("Καλή τύχη! Γύρνα όταν τα μαζέψεις.");
                    gameState = dialogueState;
                }
            }
        }
    }

    public void startDialogue(String text) {
        fullDialogue = text;
        displayedDialogue = "";
        dialogueCharCounter = 0;
        dialogueTyping = true;
        
        // Παίξε ήχο έναρξης διαλόγου (προαιρετικά)
        playSound("dialogue_start");
    }

    public void showActionMessage(String message) {
        lastAction = message;
        actionMessageTimer = ACTION_MESSAGE_DURATION;
    }

    public void playSound(String soundName) {
        try {
            // Προσπάθησε πρώτα να παίξει ως sound effect
            sound.playSE(soundName);
        } catch (Exception e) {
            // Αν αποτύχει, δοκίμασε με την παλιά μέθοδο setFile/play
            try {
                sound.setFile(soundName);
                sound.play();
            } catch (Exception ex) {
                // Αγνόησε αν δεν υπάρχει
            }
        }
    }

    public void startMusicCrossfade(String newMusic) {
        if (currentMap != 0 && currentMap != 3) return;
        
        nextMusic = newMusic;
        isCrossfading = true;
    }

    public void returnToMapMusic() {
        sound.stopMusic();
        if (currentMap == 0) { // Overworld
            if (dayTime == 0 || dayTime == 3) {
                sound.playMusic("overworld_day");
            } else {
                sound.playMusic("overworld_night");
            }
        } else if (currentMap == 1) { // Dungeon
            sound.playMusic("dungeon");
        } else if (currentMap == 3) { // Town
            if (dayTime == 0 || dayTime == 3) {
                sound.playMusic("town_day");
            } else {
                sound.playMusic("town_night");
            }
        } else {
            sound.playMusic("merchant_village");
        }
        currentMusicVolume = musicVolume / 100.0f;
        sound.setMusicVolume(currentMusicVolume);
    }

    public void toggleFullScreen() {
        try {
            if (isFullScreen) {
                // Έξοδος από fullscreen
                window.dispose();
                window.setUndecorated(false);
                window.setVisible(true);
                device.setFullScreenWindow(null);
                
                // Επανάφερε στο κανονικό μέγεθος
                window.setSize(screenWidth, screenHeight);
                scaleX = 1.0;
                scaleY = 1.0;
                isFullScreen = false;
            } else {
                // Είσοδος σε fullscreen
                window.dispose();
                window.setUndecorated(true);
                window.setVisible(true);
                device.setFullScreenWindow(window);
                
                // Υπολόγισε τα scale factors
                Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                scaleX = screenSize.getWidth() / screenWidth;
                scaleY = screenSize.getHeight() / screenHeight;
                isFullScreen = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ============================
    //  HELPERS FOR TILED
    // ============================
    private void spawnTiledNPCs(int mapIndex) {
        ArrayList<TiledObjectData> npcObjects = tileM.getMapObjectsByLayer(mapIndex, "npcs");

        for (TiledObjectData obj : npcObjects) {
            int col = (int)(obj.x / originalTileSize);
            int row = (int)(obj.y / originalTileSize) - 1;

            int worldX = col * tileSize;
            int worldY = row * tileSize;

            String npcId = obj.getProperty("npcId");
            String direction = obj.getProperty("direction");
            String dialogueId = obj.getProperty("dialogue");
            String npcType = obj.getProperty("type");

            Entity npc = null;

            if (npcId.equalsIgnoreCase("old_man")) {
                npc = new NPC_OldMan(this);
            } else if (npcId.equalsIgnoreCase("guard")) {
                npc = new NPC_Guard(this);
            } else if (npcId.equalsIgnoreCase("merchant")) {
                npc = new NPC_Merchant(this);
            }

            if (npc != null) {
                npc.worldX = worldX;
                npc.worldY = worldY;

                if (direction != null && !direction.isEmpty()) {
                    npc.direction = direction;
                }

                npc.dialogueId = dialogueId;
                npc.npcType = npcType;

                npcs.get(mapIndex).add(npc);
            }
        }
    }

    public String[] getDialogue(String id) {

        if (id == null) return new String[]{"..."};

        switch (id) {

            case "old_man_intro":
                return new String[]{
                    "Καλώς ήρθες ταξιδιώτη...",
                    "Ο κόσμος έξω είναι επικίνδυνος."
                };

            case "guard_warning":
                return new String[]{
                    "Μην πλησιάζεις το dungeon!",
                    "Είναι γεμάτο τέρατα."
                };

            case "merchant_shop":
                return new String[]{
                    "Καλώς ήρθες στο μαγαζί μου!",
                    "Θες να αγοράσεις κάτι;"
                };

            default:
                return new String[]{
                    "..."
                };
        }
    }

    private void spawnTiledChestLoot(int mapIndex) {
        ArrayList<TiledObjectData> chestObjects = tileM.getMapObjectsByLayer(mapIndex, "chests");

        for (TiledObjectData obj : chestObjects) {
            int col = (int)(obj.x / originalTileSize);
            int row = (int)(obj.y / originalTileSize) - 1;

            int worldX = col * tileSize;
            int worldY = row * tileSize;

            String itemId = obj.getProperty("item");
            int amount = obj.getPropertyInt("amount", 1);

            try {
                Item item = createItemFromId(itemId);
                if (item == null) continue;

                item.amount = amount;

                BufferedImage itemImage = item.image;

                itemsOnGround.get(mapIndex).add(
                        new ItemOnGround(item.name, itemImage, worldX, worldY, item)
                );

                System.out.println("Spawned chest loot " + itemId + " x" + amount +
                        " at map " + mapIndex + " (" + worldX + "," + worldY + ")");
            } catch (Exception e) {
                System.out.println("Failed to spawn chest loot: " + itemId);
                e.printStackTrace();
            }
        }
    }

    private Item createItemFromId(String itemId) throws Exception {
        if (itemId == null || itemId.isEmpty()) return null;

        if (itemId.equalsIgnoreCase("health_potion")) {
            Item item = new Item("Health Potion");
            item.stackable = true;
            item.healAmount = 20;
            item.price = 50;
            item.loadImage("res/items/health_potion.png");
            return item;
        }

        if (itemId.equalsIgnoreCase("mana_potion")) {
            Item item = new Item("Mana Potion");
            item.stackable = true;
            item.mpBonus = 20;
            item.price = 40;
            item.loadImage("res/items/potion_blue.png");
            return item;
        }

        if (itemId.equalsIgnoreCase("iron_sword")) {
            Item item = new Item("Iron Sword");
            item.attackBonus = 5;
            item.price = 200;
            item.loadImage("res/items/iron_sword.png");
            return item;
        }

        if (itemId.equalsIgnoreCase("goblin_ear")) {
            Item item = new Item("Goblin Ear");
            item.stackable = true;
            item.loadImage("res/items/goblin_ear.png");
            return item;
        }

        if (itemId.equalsIgnoreCase("lantern")) {
            Item item = new Item("Lantern");
            item.isKeyItem = true;
            item.loadImage("res/items/lantern.png");
            return item;
        }

        return null;
    }

    private void handleNPCInteraction(ArrayList<Entity> currentMapNPCs) {
        for (Entity npc : currentMapNPCs) {
            int distanceX = Math.abs(player.worldX - npc.worldX);
            int distanceY = Math.abs(player.worldY - npc.worldY);

            if (distanceX <= tileSize && distanceY <= tileSize) {

                faceNPCToPlayer(npc);

                // ΝΕΟΣ ΤΡΟΠΟΣ: αν έχει dialogueId από Tiled, χρησιμοποίησέ το
                if (npc.dialogueId != null && !npc.dialogueId.isEmpty()) {

                    if ("shop".equalsIgnoreCase(npc.npcType)) {
                        gameState = dialogueState;
                        String[] dialogueLines = getDialogue(npc.dialogueId);
                        startDialogue(String.join("\n", dialogueLines));
                        talkingToMerchant = true;
                        return;
                    }

                    if ("guard".equalsIgnoreCase(npc.npcType)) {
                        gameState = dialogueState;
                        String[] dialogueLines = getDialogue(npc.dialogueId);
                        startDialogue(String.join("\n", dialogueLines));
                        return;
                    }

                    gameState = dialogueState;
                    String[] dialogueLines = getDialogue(npc.dialogueId);
                    startDialogue(String.join("\n", dialogueLines));
                    return;
                }

                // FALLBACK για τα παλιά hardcoded NPCs
                if (npc instanceof NPC_OldMan) {
                    gameState = dialogueState;
                    startDialogue("Γεια σου, ταξιδιώτη!...\nΚαλώς ήρθες στο πρώτο μου RPG!");
                    return;
                }

                if (npc instanceof NPC_Guard) {
                    boolean hasQuest = false;
                    boolean questCompleted = false;

                    for (Quest q : player.quests) {
                        if (q.name.equals("Καθάρισμα τεράτων")) {
                            hasQuest = true;
                            questCompleted = q.completed;
                            break;
                        }
                    }

                    if (!hasQuest) {
                        gameState = dialogueOptionsState;
                        startDialogue("Φύλακας: Χρειάζομαι βοήθεια! Τα goblin έχουν γίνει μάστιγα!\nΤι λες;");
                        dialogueOptions[0] = "(1) Αναλαμβάνω την αποστολή";
                        dialogueOptions[1] = "(2) Δεν έχω χρόνο τώρα";
                        dialogueOptions[2] = null;
                        selectedOption = 0;
                    } 
                    else if (!questCompleted) {
                        gameState = dialogueOptionsState;
                        startDialogue("Φύλακας: Πώς πάει η αποστολή; Έφερες τα 5 αυτιά;");
                        dialogueOptions[0] = "(1) Ναι, ορίστε!";
                        dialogueOptions[1] = "(2) Όχι ακόμα...";
                        dialogueOptions[2] = null;
                        selectedOption = 0;
                    } 
                    else {
                        gameState = dialogueState;
                        startDialogue("Φύλακας: Σε ευχαριστώ για τη βοήθεια! Οι χωρικοί είναι ασφαλείς.");
                    }
                    return;
                }

                if (npc instanceof NPC_Merchant) {
                    gameState = dialogueState;
                    ((NPC_Merchant) npc).setDirectionTowardsPlayer();
                    ((NPC_Merchant) npc).speak();
                    talkingToMerchant = true;
                    return;
                }
            }
        }
    }

    private void faceNPCToPlayer(Entity npc) {
        if (player.worldX < npc.worldX) {
            npc.direction = "left";
            if (npc instanceof NPC_OldMan) ((NPC_OldMan) npc).currentImage = ((NPC_OldMan) npc).left1;
            if (npc instanceof NPC_Guard) ((NPC_Guard) npc).currentImage = ((NPC_Guard) npc).left1;
        }
        else if (player.worldX > npc.worldX) {
            npc.direction = "right";
            if (npc instanceof NPC_OldMan) ((NPC_OldMan) npc).currentImage = ((NPC_OldMan) npc).right1;
            if (npc instanceof NPC_Guard) ((NPC_Guard) npc).currentImage = ((NPC_Guard) npc).right1;
        }
        else if (player.worldY < npc.worldY) {
            npc.direction = "up";
            if (npc instanceof NPC_OldMan) ((NPC_OldMan) npc).currentImage = ((NPC_OldMan) npc).up1;
            if (npc instanceof NPC_Guard) ((NPC_Guard) npc).currentImage = ((NPC_Guard) npc).up1;
        }
        else if (player.worldY > npc.worldY) {
            npc.direction = "down";
            if (npc instanceof NPC_OldMan) ((NPC_OldMan) npc).currentImage = ((NPC_OldMan) npc).down1;
            if (npc instanceof NPC_Guard) ((NPC_Guard) npc).currentImage = ((NPC_Guard) npc).down1;
        }
    }

    private TiledObjectData getCurrentEncounterZone() {
        ArrayList<TiledObjectData> encounterObjects = tileM.getMapObjectsByLayer(currentMap, "encounters");

        for (TiledObjectData obj : encounterObjects) {
            int zoneX = (int)(obj.x / originalTileSize) * tileSize;
            int zoneY = (int)(obj.y / originalTileSize) * tileSize;

            int zoneWidthTiles = Math.max(1, obj.width / originalTileSize);
            int zoneHeightTiles = Math.max(1, obj.height / originalTileSize);

            int zoneWidthWorld = zoneWidthTiles * tileSize;
            int zoneHeightWorld = zoneHeightTiles * tileSize;

            boolean insideZone =
                    player.worldX + tileSize > zoneX &&
                    player.worldX < zoneX + zoneWidthWorld &&
                    player.worldY + tileSize > zoneY &&
                    player.worldY < zoneY + zoneHeightWorld;

            if (insideZone) {
                return obj;
            }
        }

        return null;
    }
    // =============================
    //  END OF HELPERS FOR TILED
    // ============================

    public void startRandomEncounter() {
        // Επέλεξε τυχαίο τέρας ανάλογα με την περιοχή
        String[] possibleEnemies;
        
        if (currentArea.equals("overworld")) {
            possibleEnemies = new String[]{"Goblin", "Mushroom"};
        } else { // dungeon
            possibleEnemies = new String[]{"Goblin", "Skeleton"};
        }
        
        String enemyType = possibleEnemies[(int)(Math.random() * possibleEnemies.length)];
        
        Enemy newEnemy = null;
        
        // Δημιούργησε το κατάλληλο τέρας
        if (enemyType.equals("Goblin")) {
            newEnemy = new Enemy_Goblin(this);
        } else if (enemyType.equals("Mushroom")) {
            newEnemy = new Enemy_Mushroom(this);
        } else if (enemyType.equals("Skeleton")) {
            newEnemy = new Enemy_Skeleton(this);
        }
        
        if (newEnemy != null) {
            // ΞΕΚΙΝΑΕΙ ΜΑΧΗ
            battleStarting = true;
            startBattleWithTransition(newEnemy);
            battleMessage = "Εμφανίστηκε " + enemyType + "!";
            sound.stopMusic();
            sound.playBattleSE("ENEMY_APPEAR");
            waitingForBattleMusic = true;
            battleAppearTimer = 0;
            battleAppearDurationFrames = (int)(sound.getBattleSoundLengthMs("ENEMY_APPEAR") / 16.67);

            // ξεκίνα τη μουσική ~20 frames πριν τελειώσει ο ήχος
            battleAppearDurationFrames -= 200;

            if (battleAppearDurationFrames <= 0) battleAppearDurationFrames = 40;
        }
    }

    // Νέα μέθοδος για έναρξη μάχης με transition
    public void startBattleWithTransition(Enemy enemy) {
        currentEnemy = enemy;

        // Ξεκίνα fade out
        battleFadeOut = true;
        battleFadeAlpha = 0;
        battleFadeIn = false;

        try {
            groundGrass = ImageIO.read(new File("res/battle/ground_grass.png"));
            groundDungeon = ImageIO.read(new File("res/battle/ground_dungeon.png"));
            System.out.println("Ground images loaded successfully!");
        } catch (IOException e) {
            e.printStackTrace();
            // Fallback: δημιούργησε gradient images
            groundGrass = createGroundGradient(new Color(34, 139, 34), new Color(144, 238, 144));
            groundDungeon = createGroundGradient(new Color(70, 70, 70), new Color(120, 70, 70));
        }

        
        // Δημιούργησε τυχαίο background για τη μάχη
        createBattleBackground();
        
        // Ρύθμισε τους εχθρούς και τους παίκτες για οριζόντια μάχη
        //setupBattleEntities();
    }

    public BufferedImage createGroundGradient(Color topColor, Color bottomColor) {
        BufferedImage ground = new BufferedImage(groundWidth, groundHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = ground.createGraphics();
        
        GradientPaint gradient = new GradientPaint(
            0, 0, topColor,
            0, groundHeight, bottomColor
        );
        
        g2d.setPaint(gradient);
        g2d.fillRect(0, 0, groundWidth, groundHeight);
        g2d.dispose();
        
        return ground;
    }

    public void createBattleBackground() {
        // Διάλεξε τυχαίο background ανάλογα με τον χάρτη
        String[] possibleBackgrounds;
        
        if (currentMap == 0) { // Overworld
            possibleBackgrounds = new String[]{"field1"};
        } else if (currentMap == 1) { // Dungeon
            possibleBackgrounds = new String[]{"dungeon1", "dungeon2"};
        } else {
            possibleBackgrounds = new String[]{"field1"};
        }
        
        String bgName = possibleBackgrounds[(int)(Math.random() * possibleBackgrounds.length)];
        
        try {
            battleBackground = ImageIO.read(new File("res/battle/bg_" + bgName + ".png"));
        } catch (Exception e) {
            // Αν δεν υπάρχει εικόνα, δημιούργησε gradient background
            battleBackground = createGradientBackground();
        }
    }

    public BufferedImage createGradientBackground() {
        BufferedImage bg = new BufferedImage(screenWidth, screenHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = bg.createGraphics();
        
        // Gradient ανάλογα με τον χάρτη
        Color topColor, bottomColor;
        if (currentMap == 0) {
            topColor = new Color(100, 150, 200); // Μπλε ουρανός
            bottomColor = new Color(50, 100, 50); // Πράσινο έδαφος
        } else {
            topColor = new Color(50, 50, 50); // Σκοτεινό
            bottomColor = new Color(100, 50, 50); // Κοκκινωπό
        }
        
        GradientPaint gradient = new GradientPaint(0, 0, topColor, 
                                                0, screenHeight, bottomColor);
        g2d.setPaint(gradient);
        g2d.fillRect(0, 0, screenWidth, screenHeight);
        g2d.dispose();
        
        return bg;
    }

    public void setupBattleEntities() {
        waitingForNextTurn = false;
        battleTurnDelay = 0;
        battlePhase = 0;
        battlePhaseTimer = 0;
        selectingTarget = false;
        battleMenuOption = 0;
        actionMessageTimer = 0;
        pendingExp = 0;
        pendingGold = 0;
        victoryExp = 0;
        victoryGold = 0;
        victoryRewardsShown = false;
        lastAction = "";
        battleEnemies.clear();
        battlePlayers.clear();
        battlePartyMembers.clear();  // ΝΕΟ: Καθάρισε τη λίστα
        battleParty.party.clear();
        battleParty.enemies.clear();

        selectingBoost = false;
        selectedBoost = 0;
        actionInProgress = false;
        battleMessage = "";
        battleParty.battleEnded = false;
        battleEnterLatch = false;
        battleBLatch = false;
        battleCLatch = false;
        commandLocked = false;

        lastKillerName = "";
        boostLoopLevel = 0;
        boostLoopTimer = 0;
        boostVisualStartTime = System.currentTimeMillis();

        // ========== ΕΠΙΛΕΞΕ ΤΗΝ ΚΑΤΑΛΛΗΛΗ ΕΙΚΟΝΑ ΕΔΑΦΟΥΣ ==========
        if (currentMap == 0) {
            groundImage = groundGrass;
        } else if (currentMap == 1) {
            groundImage = groundDungeon;
        } else {
            groundImage = groundGrass;
        }
        
        // Υπολόγισε τη θέση του εδάφους
        groundY = screenHeight - groundHeight - tileSize - 100;
        groundX = 0;
        
        // ========== ΔΗΜΙΟΥΡΓΙΑ ΕΧΘΡΩΝ (2-3) ==========
        int numEnemies = 1 + (int)(Math.random() * 2);
        
        for (int i = 0; i < numEnemies; i++) {
            Enemy enemy;
            String[] possibleEnemies;
            if (currentArea.equals("overworld")) {
                possibleEnemies = new String[]{"Goblin", "Mushroom"};
            } else {
                possibleEnemies = new String[]{"Goblin", "Skeleton"};
            }
            String enemyType = possibleEnemies[(int)(Math.random() * possibleEnemies.length)];
            
            if (enemyType.equals("Goblin")) {
                enemy = new Enemy_Goblin(this);
            } else if (enemyType.equals("Mushroom")) {
                enemy = new Enemy_Mushroom(this);
            } else {
                enemy = new Enemy_Skeleton(this);
            }
            
            BattleEnemy be = new BattleEnemy(enemy);
            
            // Στοίχιση κατακόρυφα
            be.x = -tileSize * 4;
            be.y = groundY - tileSize - (i * 120);
            be.targetX = tileSize / 2 - 60;
            be.targetY = groundY - tileSize - (i * 120);
            
            be.playAnimation("idle");
            battleEnemies.add(be);
            
            BattleEntity enemyEntity = new BattleEntity(enemy, enemy.currentImage);
            battleParty.enemies.add(enemyEntity);
        }
        
        // ========== ΔΗΜΙΟΥΡΓΙΑ ΠΑΙΚΤΗ (ΚΕΝΤΡΙΚΟΣ) ==========
        BattlePlayer bp = new BattlePlayer();
        bp.player = player;
        bp.hp = player.hp;
        bp.maxHp = player.maxHp;
        bp.mp = player.mp;
        bp.maxMp = player.maxMp;
        bp.anim = playerBattleAnim;
        bp.image = playerDown1;
        if (bp.anim != null) bp.playAnimation("idle");
        
        bp.x = screenWidth + tileSize * 4;
        bp.y = groundY - tileSize * 4 + 40;
        bp.targetX = screenWidth - tileSize * 5;
        bp.targetY = groundY - tileSize * 4 + 40;
        
        battlePlayers.add(bp);
        
        // ========== ΔΗΜΙΟΥΡΓΙΑ ΤΩΝ ΑΛΛΩΝ ΜΕΛΩΝ ==========
        for (int i = 0; i < partyMembers.size(); i++) {
            PartyMember member = partyMembers.get(i);
            BattlePartyMember bpm = new BattlePartyMember(member);

            bpm.playAnimation("idle");
            
            // Τοποθέτηση σε κατακόρυφη σειρά (πιο πάνω από τον κεντρικό)
            bpm.x = screenWidth + tileSize * 4;
            bpm.y = groundY - tileSize * 4 + 40 - ((i + 1) * 100);
            bpm.targetX = screenWidth - tileSize * 5;
            bpm.targetY = groundY - tileSize * 4 + 40 - ((i + 1) * 100);
            
            battlePartyMembers.add(bpm);
        }
        
        // ========== ΔΗΜΙΟΥΡΓΙΑ BATTLE ENTITIES ==========
        // Κεντρικός
        battleParty.party.clear(); // Σιγουρέψου ότι είναι άδειο

        // ΠΡΩΤΑ ο κεντρικός ήρωας
        BattleEntity playerEntity = new BattleEntity(player, playerDown1);
        playerEntity.name = "Hero"; // Δώσε του όνομα
        battleParty.party.add(playerEntity);

        // ΜΕΤΑ τα άλλα μέλη (με διαφορετικά ονόματα)
        for (int i = 0; i < partyMembers.size(); i++) {
            PartyMember member = partyMembers.get(i);
            BattleEntity memberEntity = new BattleEntity(member, member.down1);
            
            // Δώσε τους διαφορετικά ονόματα!
            if (member.className.equals("Assassin")) {
                memberEntity.name = "Assassin";
            } else if (member.className.equals("Mage")) {
                memberEntity.name = "Mage";
            }
            
            battleParty.party.add(memberEntity);
        }

        // Εκτύπωσε για επιβεβαίωση
        System.out.println("BattleParty party size: " + battleParty.party.size());
        for (BattleEntity be : battleParty.party) {
            System.out.println("  - " + be.name + " (HP: " + be.hp + ")");
        }
        
        // Υπολόγισε τη σειρά σειράς
        battleParty.calculateRandomTurnOrder();
        battleParty.startNewRoundBP();
        
        // Αρχικοποίησε μεταβλητές μάχης
        battleMenuOption = 0;
        selectingTarget = false;
        selectingBoost = false;
        selectedBoost = 0;
        actionInProgress = false;
        battleMessage = "";
        battleParty.battleEnded = false;

        pendingExp = 0;
        pendingGold = 0;
        victoryExp = 0;
        victoryGold = 0;
        victoryRewardsShown = false;
    }

    public BufferedImage getWeaponImage() {
        // Έλεγξε αν ο παίκτης έχει όπλο εξοπλισμένο
        Item weapon = inventory.getEquipSlot(3); // Slot 3 = sword
        
        if (weapon != null && weapon.image != null) {
            return weapon.image;
        }
        
        // Default εικόνα αν δεν έχει όπλο
        try {
            return ImageIO.read(new File("res/items/weapon_default.png"));
        } catch (Exception e) {
            return playerDown1; // Fallback
        }
    }

    public void updateBattleVisuals() {
        for (BattlePlayer bp : battlePlayers) {
            bp.update();
        }

        for (BattlePartyMember bpm : battlePartyMembers) {
            bpm.update();
        }

        for (BattleEnemy be : battleEnemies) {
            be.update();
        }
    }

    public void updateBattleAction() {
        if (!actionInProgress) return;

        BattleEntity actor = battleParty.getCurrentTurn();
        if (actor == null) return;

        actor.updateStateTimer();

        if (actor.isPlayer) {
            updatePlayerAction(actor);
        } else {
            updateEnemyAction(actor);
        }
    }

    public void updatePlayerAction(BattleEntity actor) {
        if (actor.state == CombatState.DEFENDING) {
            if (isActorAnimationFinished(actor)) {
                actor.resetTurnFlags();
                actor.enterState(CombatState.IDLE);

                selectedBoost = 0;
                boostLoopLevel = 0;
                boostLoopTimer = 0;
                sound.stopBattleLoop();

                actionInProgress = false;
                waitingForNextTurn = true;
                commandLocked = false;
                battleTurnDelay = 0;
            }
            return;
        }
        
        if (actor.state == CombatState.WINDUP) {
            playActorAnimation(actor, getAttackAnimationName(actor));
            actor.enterState(CombatState.ATTACKING);
            return;
        }

        if (actor.state == CombatState.ATTACKING) {
            if (!actor.strikeTriggered && actor.queuedTarget != null && isActorOnStrikeFrame(actor)) {
                actor.strikeTriggered = true;

                playPlayerAttackSound(actor);
                triggerLunge(actor, actor.boostUsed);
                triggerSwingTrail(actor, actor.boostUsed);

                if (actor.boostUsed > 0) {
                    triggerBoostBurst(actor.queuedTarget, actor.boostUsed);
                    triggerHitFlash(actor.boostUsed);
                    triggerZoomPop(actor.boostUsed);
                }

                int reactStrength = (actor.boostUsed > 0) ? actor.boostUsed : 1;
                actor.queuedTarget.triggerHitReact(reactStrength);

                int damage = calculateAttackDamage(actor, actor.queuedTarget, actor.boostUsed);
                actor.queuedTarget.takeDamage(damage);

                playTargetHurt(actor.queuedTarget);
                syncVisualHp(actor.queuedTarget);

                showActionMessage(actor.name + " attacks " + actor.queuedTarget.name + " for " + damage + " damage!");

                if (!actor.queuedTarget.isAlive()) {
                    lastKillerName = actor.name;
                    playTargetDeath(actor.queuedTarget);
                }

                if (actor.boostUsed == 1) {
                    hitPauseTimer = HIT_PAUSE_DURATION + 3;
                } else if (actor.boostUsed == 2) {
                    hitPauseTimer = HIT_PAUSE_DURATION + 5;
                } else if (actor.boostUsed >= 3) {
                    hitPauseTimer = HIT_PAUSE_DURATION + 7;
                } else {
                    hitPauseTimer = HIT_PAUSE_DURATION;
                }

                actor.enterState(CombatState.HIT_PAUSE);
            }
            return;
        }

        if (actor.state == CombatState.HIT_PAUSE) {
            hitPauseTimer--;
            if (hitPauseTimer <= 0) {
                actor.enterState(CombatState.RECOVERY);
            }
            return;
        }

        if (actor.state == CombatState.RECOVERY) {
            if (isActorAnimationFinished(actor)) {
                finalizeDeathsAndRewards();
                actor.resetTurnFlags();
                actor.enterState(CombatState.IDLE);

                selectedBoost = 0;
                boostLoopLevel = 0;
                boostLoopTimer = 0;
                sound.stopBattleLoop();

                actionInProgress = false;
                waitingForNextTurn = true;
                commandLocked = false;
                battleTurnDelay = 0;
            }
        }
    }

    public void updateEnemyAction(BattleEntity actor) {
        if (actor.queuedTarget == null) {
            ArrayList<BattleEntity> alivePlayers = new ArrayList<>();
            for (BattleEntity playerEntity : battleParty.party) {
                if (playerEntity.isAlive()) {
                    alivePlayers.add(playerEntity);
                }
            }

            if (alivePlayers.isEmpty()) return;

            int randomIndex = (int)(Math.random() * alivePlayers.size());
            actor.queuedTarget = alivePlayers.get(randomIndex);
            actor.queuedAction = "attack";
            actor.boostUsed = 0;
            actor.enterState(CombatState.WINDUP);
        }

        if (actor.state == CombatState.WINDUP) {
            System.out.println("ENEMY WINDUP -> PLAY ATTACK");
            playActorAnimation(actor, "attack");
            actor.enterState(CombatState.ATTACKING);
            return;
        }

        if (actor.state == CombatState.ATTACKING) {
            if (!actor.strikeTriggered && actor.queuedTarget != null && isActorOnStrikeFrame(actor)) {
                actor.strikeTriggered = true;
                actor.queuedTarget.triggerHitReact(1);

                playEnemyAttackSound(actor);

                int damage = calculateAttackDamage(actor, actor.queuedTarget, 0);
                actor.queuedTarget.takeDamage(damage);

                playTargetHurt(actor.queuedTarget);
                syncVisualHp(actor.queuedTarget);

                showActionMessage(actor.name + " attacks " + actor.queuedTarget.name + " for " + damage + " damage!");

                if (!actor.queuedTarget.isAlive()) {
                    playTargetDeath(actor.queuedTarget);
                }

                hitPauseTimer = HIT_PAUSE_DURATION;
                actor.enterState(CombatState.HIT_PAUSE);
            }
            return;
        }

        if (actor.state == CombatState.HIT_PAUSE) {
            hitPauseTimer--;
            if (hitPauseTimer <= 0) {
                actor.enterState(CombatState.RECOVERY);
            }
            return;
        }

        if (actor.state == CombatState.RECOVERY) {
            if (isActorAnimationFinished(actor)) {
                battleParty.removeDeadEntities();
                battleParty.syncPlayerHealth();

                actor.resetTurnFlags();
                actor.enterState(CombatState.IDLE);
                actionInProgress = false;
                waitingForNextTurn = true;
                battleTurnDelay = 0;
                commandLocked = false;
            }
        }
    }

    public void playPlayerAttackSound(BattleEntity actor) {
        if (actor.name.equals("Hero")) {
            if (actor.boostUsed > 0) sound.playBattleSE("SPEAR2");
            else sound.playBattleSE("SPEAR");
        } else if (actor.name.equals("Assassin")) {
            if (actor.boostUsed > 0) sound.playBattleSE("SWORD2");
            else sound.playBattleSE("SWORD");
        } else if (actor.name.equals("Mage")) {
            if (actor.boostUsed > 0) sound.playBattleSE("STAFF2");
            else sound.playBattleSE("STAFF");
        }
    }

    public void playEnemyAttackSound(BattleEntity actor) {
        if (actor.enemyRef == null) return;

        String enemyName = actor.enemyRef.getClass().getSimpleName().toLowerCase();

        if (enemyName.contains("goblin")) {
            sound.playBattleSE("GOBLIN_SLASH");
        } else if (enemyName.contains("mushroom")) {
            sound.playBattleSE("MUSHROOM_ATTACK");
        } else if (enemyName.contains("skeleton")) {
            sound.playBattleSE("SKELETON_ATTACK");
        }
    }

    public void drawImpactSlashLines(Graphics2D g2) {
        if (boostBurstTimer >= boostBurstDuration || boostBurstLevel <= 0) return;

        Graphics2D g = (Graphics2D) g2.create();

        float progress = (float) boostBurstTimer / boostBurstDuration;
        float inv = 1.0f - progress;

        // ίδιο offset με το burst
        int impactX = boostBurstX - 15;
        int impactY = boostBurstY + 120;

        // χρώματα
        Color mainSlash = new Color(255, 245, 220, Math.max(0, (int)(210 * inv)));
        Color glowSlash = new Color(255, 190, 120, Math.max(0, (int)(130 * inv)));

        // μήκος/πάχος ανά boost
        int mainLength;
        float mainThickness;

        if (boostBurstLevel == 1) {
            mainLength = 55;
            mainThickness = 3f;
        } else if (boostBurstLevel == 2) {
            mainLength = 75;
            mainThickness = 5f;
        } else {
            mainLength = 95;
            mainThickness = 7f;
        }

        double angle = boostSlashAngle;

        int dx = (int)(Math.cos(angle) * mainLength);
        int dy = (int)(Math.sin(angle) * mainLength);

        int x1 = impactX - dx / 2;
        int y1 = impactY - dy / 2;
        int x2 = impactX + dx / 2;
        int y2 = impactY + dy / 2;

        // ===== glow κάτω από το main slash =====
        g.setStroke(new BasicStroke(mainThickness + 4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(glowSlash);
        g.drawLine(x1, y1, x2, y2);

        // ===== main slash =====
        g.setStroke(new BasicStroke(mainThickness, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(mainSlash);
        g.drawLine(x1, y1, x2, y2);

        // ===== δευτερεύουσες streaks =====
        int streakCount = 1 + boostBurstLevel; // 2,3,4 συνολικά περίπου
        for (int i = 0; i < streakCount; i++) {
            double offsetFactor = (i - streakCount / 2.0) * 12.0;
            double perpAngle = angle + Math.PI / 2.0;

            int ox = (int)(Math.cos(perpAngle) * offsetFactor);
            int oy = (int)(Math.sin(perpAngle) * offsetFactor);

            int streakLength = (int)(mainLength * (0.45 + i * 0.08));
            int sdx = (int)(Math.cos(angle) * streakLength);
            int sdy = (int)(Math.sin(angle) * streakLength);

            int sx1 = impactX + ox - sdx / 2;
            int sy1 = impactY + oy - sdy / 2;
            int sx2 = impactX + ox + sdx / 2;
            int sy2 = impactY + oy + sdy / 2;

            g.setStroke(new BasicStroke(Math.max(1.5f, mainThickness - 2f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(255, 255, 255, Math.max(0, (int)(140 * inv))));
            g.drawLine(sx1, sy1, sx2, sy2);
        }

        g.dispose();
    }

    public int getHitRecoilDrawOffsetX(BattleEntity entity, boolean targetIsEnemy) {
        if (entity == null || entity.hitRecoilTimer <= 0) return 0;

        float progress = (float) entity.hitRecoilTimer / entity.hitRecoilDuration;
        int amount = (int)(entity.hitRecoilOffsetX * progress);

        // Αν είναι enemy, τίναγμα προς τα δεξιά.
        // Αν είναι player, τίναγμα προς τα αριστερά.
        return targetIsEnemy ? amount : -amount;
    }

    public int getHitRecoilDrawOffsetY(BattleEntity entity) {
        if (entity == null || entity.hitRecoilTimer <= 0) return 0;

        float progress = (float) entity.hitRecoilTimer / entity.hitRecoilDuration;
        return (int)(entity.hitRecoilOffsetY * progress);
    }

    public void updateHitReactions() {
        // enemies
        for (BattleEntity entity : battleParty.enemies) {
            if (entity.hitRecoilTimer > 0) entity.hitRecoilTimer--;
            if (entity.hitOutlineTimer > 0) entity.hitOutlineTimer--;
            if (entity.dustTimer > 0) entity.dustTimer--;
        }
        // party members
        for (BattleEntity entity : battleParty.party) {
            if (entity.hitRecoilTimer > 0) entity.hitRecoilTimer--;
            if (entity.hitOutlineTimer > 0) entity.hitOutlineTimer--;
            if (entity.dustTimer > 0) entity.dustTimer--;
        }
    }

    public void drawImpactDust(Graphics2D g2, BattleEntity entity, int centerX, int footY, int strength) {
        if (entity == null || entity.dustTimer <= 0) return;

        Graphics2D g = (Graphics2D) g2.create();

        float progress = (float) entity.dustTimer / entity.dustDuration;
        float inv = 1.0f - progress;

        // ===== ΠΕΡΙΣΣΟΤΕΡΑ PARTICLES =====
        int puffCount = 10 + strength * 6; // ΠΡΙΝ: 4-10 → ΤΩΡΑ: 16-28+

        for (int i = 0; i < puffCount; i++) {

            // ===== SPREAD (πιο wide explosion) =====
            double angle = Math.PI + ((Math.random() - 0.5) * 1.4); // πιο chaotic
            int dist = 10 + (int)(inv * (40 + strength * 10)) + (int)(Math.random() * 10);

            int px = centerX + (int)(Math.cos(angle) * dist);
            int py = footY - 5 + (int)(Math.sin(angle) * 15);

            // ===== SIZE VARIATION =====
            int size = 6 + (int)(inv * (18 + strength * 4)) + (int)(Math.random() * 6);

            // ===== ALPHA =====
            int alpha = Math.max(0, (int)(140 * progress));

            // ===== BASE DUST (σκούρο) =====
            g.setColor(new Color(60, 50, 40, alpha));
            g.fillOval(px - size / 2, py - size / 2, size, size);

            // ===== LIGHT DUST OVERLAY =====
            g.setColor(new Color(170, 150, 120, Math.max(0, alpha - 40)));
            g.fillOval(px - size / 3, py - size / 3, size - 4, size - 4);
        }

        // ===== EXTRA HEAVY PUFFS (βάση explosion) =====
        int heavyCount = 4 + strength * 2;

        for (int i = 0; i < heavyCount; i++) {
            int offset = (i - heavyCount / 2) * (10 + strength * 2);

            int px = centerX + offset;
            int py = footY + 2;

            int size = 14 + (int)(inv * (20 + strength * 6));

            int alpha = Math.max(0, (int)(160 * progress));

            // σκούρα βάση
            g.setColor(new Color(50, 45, 40, alpha));
            g.fillOval(px - size / 2, py - size / 2, size, size);

            // πιο ανοιχτή πάνω
            g.setColor(new Color(180, 160, 130, Math.max(0, alpha - 50)));
            g.fillOval(px - size / 3, py - size / 3, size - 5, size - 5);
        }

        g.dispose();
    }

    public void triggerZoomPop(int boostLevel) {
        if (boostLevel <= 0) return;

        if (boostLevel == 1) {
            zoomPopDuration = 6;
            zoomPopMaxScale = 1.03f;
        } else if (boostLevel == 2) {
            zoomPopDuration = 8;
            zoomPopMaxScale = 1.05f;
        } else {
            zoomPopDuration = 10;
            zoomPopMaxScale = 1.07f;
        }

        zoomPopTimer = zoomPopDuration;
    }

    public void updateZoomPop() {
        if (zoomPopTimer > 0) {
            float progress = (float) zoomPopTimer / zoomPopDuration;

            // snap-in γρήγορα, μετά ομαλή επιστροφή
            zoomPopScale = 1.0f + ((zoomPopMaxScale - 1.0f) * progress);

            zoomPopTimer--;
            if (zoomPopTimer <= 0) {
                zoomPopScale = 1.0f;
            }
        } else {
            zoomPopScale = 1.0f;
        }
    }

    public void triggerLunge(BattleEntity actor, int boostLevel) {
        if (actor == null) return;

        lungingActor = actor;

        if (boostLevel <= 0) {
            lungeDuration = 6;
            lungeDistance = 18;
            afterimageCount = 0;
        } else if (boostLevel == 1) {
            lungeDuration = 8;
            lungeDistance = 28;
            afterimageCount = 2;
        } else if (boostLevel == 2) {
            lungeDuration = 10;
            lungeDistance = 40;
            afterimageCount = 3;
        } else {
            lungeDuration = 12;
            lungeDistance = 54;
            afterimageCount = 4;
        }

        lungeTimer = lungeDuration;
    }

    public void updateLunge() {
        if (lungeTimer > 0) {
            lungeTimer--;
            if (lungeTimer <= 0) {
                lungingActor = null;
                afterimageCount = 0;
            }
        }
    }

    public int getLungeOffsetX(BattleEntity actor) {
        if (actor == null || lungingActor == null || actor != lungingActor || lungeTimer <= 0) {
            return 0;
        }

        float progress = (float) lungeTimer / lungeDuration;

        // snap forward και επιστροφή
        float curve = (float)Math.sin(progress * Math.PI);

        int amount = (int)(lungeDistance * curve);

        // Players πάνε δεξιά προς τους εχθρούς
        if (actor.isPlayer) {
            return amount;
        }

        // Enemies πάνε αριστερά προς τους players
        return -amount;
    }

    public int getLungeOffsetY(BattleEntity actor) {
        if (actor == null || lungingActor == null || actor != lungingActor || lungeTimer <= 0) {
            return 0;
        }

        float progress = (float) lungeTimer / lungeDuration;
        float curve = (float)Math.sin(progress * Math.PI);

        return -(int)(6 * curve);
    }

    public void triggerSwingTrail(BattleEntity actor, int boostLevel) {
        if (actor == null) return;

        swingTrailActor = actor;
        swingTrailLevel = boostLevel;

        if (boostLevel <= 0) {
            swingTrailDuration = 5;
        } else if (boostLevel == 1) {
            swingTrailDuration = 6;
        } else if (boostLevel == 2) {
            swingTrailDuration = 8;
        } else {
            swingTrailDuration = 10;
        }

        swingTrailTimer = swingTrailDuration;
    }

    public void updateSwingTrail() {
        if (swingTrailTimer > 0) {
            swingTrailTimer--;
            if (swingTrailTimer <= 0) {
                swingTrailActor = null;
                swingTrailLevel = 0;
            }
        }
    }

    public void drawSwingTrail(Graphics2D g2, BattleEntity actor, int centerX, int centerY) {
        if (actor == null || swingTrailActor == null || actor != swingTrailActor) return;
        if (swingTrailTimer <= 0) return;

        Graphics2D g = (Graphics2D) g2.create();

        float progress = (float) swingTrailTimer / swingTrailDuration;
        float inv = 1.0f - progress;

        Color trailColor;
        int arcSize;
        float strokeSize;

        if (swingTrailLevel <= 0) {
            trailColor = new Color(255, 255, 255, Math.max(0, (int)(120 * progress)));
            arcSize = 46;
            strokeSize = 3f;
        } else if (swingTrailLevel == 1) {
            trailColor = new Color(255, 140, 100, Math.max(0, (int)(150 * progress)));
            arcSize = 56;
            strokeSize = 4f;
        } else if (swingTrailLevel == 2) {
            trailColor = new Color(255, 220, 120, Math.max(0, (int)(180 * progress)));
            arcSize = 68;
            strokeSize = 5f;
        } else {
            trailColor = new Color(120, 200, 255, Math.max(0, (int)(210 * progress)));
            arcSize = 82;
            strokeSize = 6f;
        }

        int x = centerX - arcSize / 2;
        int y = centerY - arcSize / 2;

        // glow layer
        g.setStroke(new BasicStroke(strokeSize + 3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(trailColor.getRed(), trailColor.getGreen(), trailColor.getBlue(),
                Math.max(0, (int)(70 * progress))));
        g.drawArc(x, y, arcSize, arcSize, -40, 140);

        // main arc
        g.setStroke(new BasicStroke(strokeSize, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(trailColor);
        g.drawArc(x, y, arcSize, arcSize, -40, 140);

        // secondary inner arc για boosted hits
        if (swingTrailLevel >= 2) {
            int inner = arcSize - 14;
            g.setColor(new Color(255, 255, 255, Math.max(0, (int)(130 * progress))));
            g.setStroke(new BasicStroke(Math.max(2f, strokeSize - 2f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawArc(centerX - inner / 2, centerY - inner / 2, inner, inner, -20, 115);
        }

        // spark points πάνω στο arc
        int sparkCount = 3 + Math.max(0, swingTrailLevel);
        for (int i = 0; i < sparkCount; i++) {
            double ang = Math.toRadians(-40 + (140.0 / Math.max(1, sparkCount - 1)) * i);
            int radius = arcSize / 2;
            int sx = centerX + (int)(Math.cos(ang) * radius);
            int sy = centerY + (int)(Math.sin(ang) * radius);

            int size = (swingTrailLevel >= 3) ? 4 : 3;
            g.setColor(new Color(255, 255, 255, Math.max(0, (int)(150 * progress))));
            g.fillOval(sx - size / 2, sy - size / 2, size, size);
        }

        g.dispose();
    }

    public void drawAfterimageTrail(Graphics2D g2, BufferedImage img, BattleEntity actor,
                                    int drawX, int drawY, int width, int height) {
        if (img == null || actor == null || lungingActor == null || actor != lungingActor) return;
        if (lungeTimer <= 0 || afterimageCount <= 0) return;

        Graphics2D g = (Graphics2D) g2.create();

        int direction = actor.isPlayer ? -1 : 1; // afterimages πίσω από την κίνηση
        int baseSpacing = 10;

        for (int i = 1; i <= afterimageCount; i++) {
            int alpha = Math.max(20, 110 - (i * 22));

            int offsetX = direction * i * baseSpacing;
            int offsetY = i * 1;

            g.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, alpha / 255f));

            // ελαφρύ color tint
            g.drawImage(img, drawX + offsetX, drawY + offsetY, width, height, null);
        }

        g.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, 1f));
        g.dispose();
    }

    public void drawBPDots(Graphics2D g2, BattleEntity entity, int startX, int y) {
        int dotSpacing = 14;
        int dotSize = 8;

        // Pulse animation
        double time = System.currentTimeMillis() * 0.006;
        float pulse = (float)(Math.sin(time) * 0.5 + 0.5); // 0 -> 1

        for (int i = 0; i < entity.maxBp; i++) {
            int x = startX + (i * dotSpacing);

            if (i < entity.bp) {
                int glowSize = dotSize + 4 + (int)(pulse * 3);
                int glowOffset = (glowSize - dotSize) / 2;

                // Glow
                g2.setColor(new Color(255, 80, 80, 70));
                g2.fillOval(x - glowOffset, y - glowOffset, glowSize, glowSize);

                // Main dot
                g2.setColor(new Color(220, 40, 40));
                g2.fillOval(x, y, dotSize, dotSize);

                // Highlight
                g2.setColor(new Color(255, 190, 190));
                g2.drawOval(x, y, dotSize, dotSize);

                // Tiny shine
                g2.setColor(new Color(255, 220, 220, 180));
                g2.fillOval(x + 2, y + 1, 2, 2);

            } else {
                // Empty slot
                g2.setColor(new Color(75, 75, 75));
                g2.fillOval(x, y, dotSize, dotSize);

                g2.setColor(new Color(130, 130, 130));
                g2.drawOval(x, y, dotSize, dotSize);
            }
        }
    }

    public Point getBattleEntityScreenCenter(BattleEntity entity) {
        int enemySpriteSize = tileSize * 8;
        int playerSpriteSize = tileSize * 4;

        // ===== ENEMIES =====
        for (int i = 0; i < battleParty.enemies.size(); i++) {
            BattleEntity enemyEntity = battleParty.enemies.get(i);
            if (enemyEntity == entity) {
                int numEnemies = battleEnemies.size();
                int drawX;
                int drawY;

                if (numEnemies == 1) {
                    drawX = screenWidth / 2 - enemySpriteSize - 40;
                    drawY = groundY - enemySpriteSize + (tileSize * 2) - 50;
                } else if (numEnemies == 2) {
                    if (i == 0) {
                        drawX = screenWidth / 2 - enemySpriteSize - 40;
                        drawY = groundY - enemySpriteSize + (tileSize * 2) - 70;
                    } else {
                        drawX = screenWidth / 2 - enemySpriteSize + 60;
                        drawY = groundY - enemySpriteSize + (tileSize * 2) - 30;
                    }
                } else {
                    double angle = (i - (numEnemies - 1) / 2.0) * 0.3;
                    drawX = screenWidth / 2 - enemySpriteSize + (int)(Math.sin(angle) * 200);
                    drawY = groundY - enemySpriteSize + (tileSize * 2) - (int)(Math.abs(angle) * 50) - 50;
                }

                return new Point(drawX + enemySpriteSize / 2, drawY + enemySpriteSize / 2);
            }
        }

        // ===== HERO =====
        if (!battlePlayers.isEmpty() && entity.name.equals("Hero")) {
            int basePlayerX = 600;
            int basePlayerY = groundY - playerSpriteSize + 100;
            return new Point(basePlayerX + playerSpriteSize / 2, basePlayerY + playerSpriteSize / 2);
        }

        // ===== PARTY MEMBERS =====
        for (int i = 0; i < battlePartyMembers.size(); i++) {
            BattlePartyMember bpm = battlePartyMembers.get(i);
            if (entity.name.equals(bpm.member.className)) {
                int basePlayerX = 600;
                int basePlayerY = groundY - playerSpriteSize + 100;

                int offsetX = -80 - (i * 60);
                int offsetY = -40 - (i * 40);

                int drawX = basePlayerX + offsetX;
                int drawY = basePlayerY + offsetY;

                return new Point(drawX + playerSpriteSize / 2, drawY + playerSpriteSize / 2);
            }
        }

        return new Point(screenWidth / 2, screenHeight / 2);
    }

    public void triggerBoostBurst(BattleEntity target, int boostLevel) {
        if (target == null || boostLevel <= 0) return;

        Point p = getBattleEntityScreenCenter(target);

        boostBurstLevel = boostLevel;
        boostBurstX = p.x;
        boostBurstY = p.y;
        boostBurstTimer = 0;

        if (boostLevel == 1) {
            boostBurstDuration = 12;
            screenShakeStrength = 3;
            screenShakeDuration = 6;
        } else if (boostLevel == 2) {
            boostBurstDuration = 16;
            screenShakeStrength = 5;
            screenShakeDuration = 8;
        } else {
            boostBurstDuration = 20;
            screenShakeStrength = 7;
            screenShakeDuration = 10;
        }

        screenShakeTimer = screenShakeDuration;
    }

    public void drawBoostBurstEffect(Graphics2D g2) {
        if (boostBurstTimer >= boostBurstDuration || boostBurstLevel <= 0) return;

        Graphics2D g = (Graphics2D) g2.create();

        float progress = (float) boostBurstTimer / boostBurstDuration;
        float inv = 1.0f - progress;

        // ===== OFFSET για να είναι πιο αριστερά και πιο χαμηλά =====
        int impactX = boostBurstX - 15;
        int impactY = boostBurstY + 120;

        // ===== ΙΔΙΟ ΧΡΩΜΑ ΓΙΑ ΟΛΑ ΤΑ BOOST =====
        Color flashColor = new Color(255, 180, 100, Math.max(0, (int)(200 * inv)));

        // ===== IMPACT FLASH =====
        int flashRadius;
        if (boostBurstLevel == 1) {
            flashRadius = 24 + (int)(progress * 30);
        } else if (boostBurstLevel == 2) {
            flashRadius = 34 + (int)(progress * 40);
        } else {
            flashRadius = 46 + (int)(progress * 52);
        }

        g.setColor(flashColor);
        g.fillOval(impactX - flashRadius, impactY - flashRadius, flashRadius * 2, flashRadius * 2);

        // ===== RING EXPLOSION =====
        g.setStroke(new BasicStroke(3f));
        int ringRadius;
        if (boostBurstLevel == 1) {
            ringRadius = 28 + (int)(progress * 38);
        } else if (boostBurstLevel == 2) {
            ringRadius = 40 + (int)(progress * 50);
        } else {
            ringRadius = 54 + (int)(progress * 64);
        }

        g.setColor(new Color(255, 210, 150, Math.max(0, (int)(170 * inv))));
        g.drawOval(impactX - ringRadius, impactY - ringRadius, ringRadius * 2, ringRadius * 2);

        // ===== SECOND RING για boost 2/3 =====
        if (boostBurstLevel >= 2) {
            int ringRadius2;
            if (boostBurstLevel == 2) {
                ringRadius2 = 18 + (int)(progress * 32);
            } else {
                ringRadius2 = 28 + (int)(progress * 44);
            }

            g.setColor(new Color(255, 245, 220, Math.max(0, (int)(120 * inv))));
            g.drawOval(impactX - ringRadius2, impactY - ringRadius2, ringRadius2 * 2, ringRadius2 * 2);
        }

        // ===== SPARKS =====
        int sparkCount = 6 + boostBurstLevel * 3;
        for (int i = 0; i < sparkCount; i++) {
            double angle = (Math.PI * 2 / sparkCount) * i + (boostBurstTimer * 0.15);
            int dist = 10 + (int)(progress * 50) + (i % 3) * 4;

            int sx = impactX + (int)(Math.cos(angle) * dist);
            int sy = impactY + (int)(Math.sin(angle) * dist);

            int size = (boostBurstLevel == 3) ? 5 : 4;
            g.setColor(new Color(255, 255, 255, Math.max(0, (int)(180 * inv))));
            g.fillOval(sx, sy, size, size);
        }

        g.dispose();
    }

    public void updateBoostBurstEffect() {
        if (boostBurstTimer < boostBurstDuration) {
            boostBurstTimer++;
        }

        if (screenShakeTimer > 0) {
            screenShakeTimer--;
        }

        if (hitFlashTimer > 0) {
            hitFlashTimer--;
        }
    }

    public void drawMiniBoostIndicator(Graphics2D g2, int x, int y, int boostLevel) {
        for (int i = 0; i < 3; i++) {
            if (i < boostLevel) {
                g2.setColor(new Color(255, 80, 40));
                g2.fillOval(x + i * 14, y, 10, 10);
                g2.setColor(new Color(255, 200, 160));
                g2.drawOval(x + i * 14, y, 10, 10);
            } else {
                g2.setColor(new Color(70, 70, 70));
                g2.fillOval(x + i * 14, y, 10, 10);
                g2.setColor(new Color(120, 120, 120));
                g2.drawOval(x + i * 14, y, 10, 10);
            }
        }
    }

    public void drawBoostAura(Graphics2D g2, int centerX, int footY, int boostLevel) {
        if (boostLevel <= 0) return;

        Graphics2D g = (Graphics2D) g2.create();

        double time = System.currentTimeMillis() * 0.006;
        double spin = System.currentTimeMillis() * 0.0042;
        float pulse = (float)(Math.sin(time) * 0.5 + 0.5f);

        Color mainColor;
        Color glowColor;
        Color particleColor;
        int baseRadius;
        int coneHeight;
        int ringCount;
        int particleCount;

        if (boostLevel == 1) {
            mainColor = new Color(255, 80, 80, 150);
            glowColor = new Color(255, 140, 140, 85);
            particleColor = new Color(255, 220, 220, 170);
            baseRadius = 86;
            coneHeight = 88;
            ringCount = 4;
            particleCount = 10;
        } else if (boostLevel == 2) {
            mainColor = new Color(255, 220, 90, 165);
            glowColor = new Color(255, 245, 170, 95);
            particleColor = new Color(255, 248, 220, 180);
            baseRadius = 108;
            coneHeight = 122;
            ringCount = 5;
            particleCount = 14;
        } else {
            mainColor = new Color(95, 175, 255, 185);
            glowColor = new Color(170, 225, 255, 110);
            particleColor = new Color(235, 245, 255, 190);
            baseRadius = 132;
            coneHeight = 162;
            ringCount = 6;
            particleCount = 18;
        }

        // ===== soft ground glow =====
        int groundW = baseRadius * 2 + (int)(pulse * 14);
        int groundH = baseRadius / 2 + 12;
        g.setColor(new Color(glowColor.getRed(), glowColor.getGreen(), glowColor.getBlue(), 60));
        g.fillOval(centerX - groundW / 2, footY - groundH / 2, groundW, groundH);

        // ===== dense cyclone layers =====
        g.setStroke(new BasicStroke(2.4f));

        for (int i = 0; i < ringCount; i++) {
            float layer = i / (float)Math.max(1, ringCount - 1);

            int width = baseRadius - (int)(layer * (baseRadius * 0.48f)) + (int)(pulse * 6);
            int height = Math.max(18, width / 3);

            int y = footY - (int)(layer * coneHeight);

            double phase = spin + i * 0.72;
            int sway = (int)(Math.sin(phase) * (10 + i * 2));

            int x = centerX - width / 2 + sway;

            int alpha = 135 - i * 15;
            if (alpha < 38) alpha = 38;

            // main band
            g.setColor(new Color(mainColor.getRed(), mainColor.getGreen(), mainColor.getBlue(), alpha));
            g.drawArc(x, y - 10, width, height, 0, 310);

            // outer glow band
            g.setColor(new Color(glowColor.getRed(), glowColor.getGreen(), glowColor.getBlue(), alpha / 2));
            g.drawArc(x - 5, y - 12, width + 10, height + 6, 16, 275);

            // inner bright band στα μεγαλύτερα boost
            if (boostLevel >= 2 && i < ringCount - 1) {
                g.setColor(new Color(255, 255, 255, Math.max(20, alpha / 3)));
                g.drawArc(x + 4, y - 8, width - 8, Math.max(10, height - 4), 24, 240);
            }
        }

        // ===== upward inner wisps =====
        for (int i = 0; i < boostLevel + 4; i++) {
            double phase = spin * 1.6 + i * 1.15;
            int sway = (int)(Math.sin(phase) * (10 + boostLevel * 3));

            int wx = centerX + sway;
            int wy = footY - 18 - i * 16;
            int wh = 26 + i * 11;

            g.setColor(new Color(glowColor.getRed(), glowColor.getGreen(), glowColor.getBlue(), 75));
            g.drawArc(wx - 10, wy - wh / 2, 20, wh, 95, 170);
        }

        // ===== circular energy particles instead of lightning =====
        for (int i = 0; i < particleCount; i++) {
            double ang = spin * 1.8 + i * (Math.PI * 2 / particleCount);
            int radius = 18 + (i % 5) * 9 + (int)(pulse * 4);

            int px = centerX + (int)(Math.cos(ang) * radius);
            int py = footY - 12 - (int)(Math.abs(Math.sin(ang * 1.15)) * (coneHeight * 0.78));

            int size = 3 + (i % 3);
            if (boostLevel == 3 && i % 4 == 0) size += 1;

            g.setColor(new Color(particleColor.getRed(), particleColor.getGreen(), particleColor.getBlue(), 150));
            g.fillOval(px - size / 2, py - size / 2, size, size);

            g.setColor(new Color(255, 255, 255, 110));
            g.fillOval(px, py, Math.max(1, size - 2), Math.max(1, size - 2));
        }

        // ===== top mist / release cloud =====
        for (int i = 0; i < 5 + boostLevel; i++) {
            int size = 14 + i * 4 + (int)(pulse * 3);
            int px = centerX - baseRadius / 4 + i * (baseRadius / 7);
            int py = footY - coneHeight + 10 - i * 5;

            g.setColor(new Color(glowColor.getRed(), glowColor.getGreen(), glowColor.getBlue(), 35));
            g.fillOval(px - size / 2, py - size / 2, size, size);
        }

        g.dispose();
    }

    public void triggerHitFlash(int boostLevel) {
        if (boostLevel <= 0) return;

        hitFlashLevel = boostLevel;

        if (boostLevel == 1) {
            hitFlashDuration = 4;
        } else if (boostLevel == 2) {
            hitFlashDuration = 6;
        } else {
            hitFlashDuration = 8;
        }

        hitFlashTimer = hitFlashDuration;
    }

    public void drawHitFlashOverlay(Graphics2D g2) {
        if (hitFlashTimer <= 0 || hitFlashLevel <= 0) return;

        float progress = (float) hitFlashTimer / hitFlashDuration;

        int alpha;
        if (hitFlashLevel == 1) {
            alpha = (int)(70 * progress);
        } else if (hitFlashLevel == 2) {
            alpha = (int)(110 * progress);
        } else {
            alpha = (int)(150 * progress);
        }

        if (alpha < 0) alpha = 0;
        if (alpha > 255) alpha = 255;

        g2.setColor(new Color(255, 255, 255, alpha));
        g2.fillRect(0, 0, screenWidth, screenHeight);
    }

    public int calculateAttackDamage(BattleEntity attacker, BattleEntity target, int boostUsed) {
        double multiplier = 1.0 + (boostUsed * 0.5);
        int baseDamage = attacker.attack - target.defense;
        if (baseDamage < 1) baseDamage = 1;
        return Math.max(1, (int)(baseDamage * multiplier));
    }

    public String getAttackAnimationName(BattleEntity actor) {
        switch (actor.boostUsed) {
            case 0: return "attack1";
            case 1: return "attack1";
            case 2: return "attack2";
            case 3: return "attack3";
            default: return "attack1";
        }
    }

    public void playActorAnimation(BattleEntity actor, String animName) {
        if (actor.isPlayer) {
            if (actor.name.equals("Hero")) {
                if (!battlePlayers.isEmpty()) battlePlayers.get(0).playAnimation(animName);
            } else {
                for (BattlePartyMember bpm : battlePartyMembers) {
                    if (bpm.member.className.equals(actor.name)) {
                        bpm.playAnimation(animName);
                        break;
                    }
                }
            }
        } else {
            for (BattleEnemy be : battleEnemies) {
                if (be.enemy == actor.enemyRef) {
                    be.playAnimation(animName);
                    break;
                }
            }
        }
    }

    public boolean isActorOnStrikeFrame(BattleEntity actor) {
        if (actor.isPlayer) {
            if (actor.name.equals("Hero")) {
                return !battlePlayers.isEmpty() && battlePlayers.get(0).isOnStrikeFrame();
            } else {
                for (BattlePartyMember bpm : battlePartyMembers) {
                    if (bpm.member.className.equals(actor.name)) {
                        return bpm.isOnStrikeFrame();
                    }
                }
            }
        } else {
            for (BattleEnemy be : battleEnemies) {
                if (be.enemy == actor.enemyRef) {
                    return be.isOnStrikeFrame();
                }
            }
        }
        return false;
    }

    public boolean isActorAnimationFinished(BattleEntity actor) {
        if (actor.isPlayer) {
            if (actor.name.equals("Hero")) {
                return battlePlayers.isEmpty() || battlePlayers.get(0).isAnimationFinished();
            } else {
                for (BattlePartyMember bpm : battlePartyMembers) {
                    if (bpm.member.className.equals(actor.name)) {
                        return bpm.isAnimationFinished();
                    }
                }
            }
        } else {
            for (BattleEnemy be : battleEnemies) {
                if (be.enemy == actor.enemyRef) {
                    return be.isAnimationFinished();
                }
            }
        }
        return true;
    }

    public void playTargetHurt(BattleEntity target) {
        if (target.isPlayer) {
            if (target.name.equals("Hero")) {
                if (!battlePlayers.isEmpty()) battlePlayers.get(0).playAnimation("hurt");
            } else {
                for (BattlePartyMember bpm : battlePartyMembers) {
                    if (bpm.member.className.equals(target.name)) {
                        bpm.playAnimation("hurt");
                        break;
                    }
                }
            }
        } else {
            for (BattleEnemy be : battleEnemies) {
                if (be.enemy == target.enemyRef) {
                    be.playAnimation("hurt");
                    break;
                }
            }
        }
    }

    public void playTargetDeath(BattleEntity target) {
        if (target.isPlayer) {
            if (target.name.equals("Hero")) {
                if (!battlePlayers.isEmpty()) battlePlayers.get(0).playAnimation("death");
            } else {
                for (BattlePartyMember bpm : battlePartyMembers) {
                    if (bpm.member.className.equals(target.name)) {
                        bpm.playAnimation("death");

                        if (target.name.equals("Assassin")) {
                            sound.playBattleSE("DEATHASSASSIN");
                        } else if (target.name.equals("Mage")) {
                            sound.playBattleSE("DEATHMAGE");
                        }

                        break;
                    }
                }
            }
        } else {
            for (BattleEnemy be : battleEnemies) {
                if (be.enemy == target.enemyRef) {
                    be.playAnimation("death");
                    sound.playBattleSE("DEATHENEMY");
                    break;
                }
            }
        }
    }

    public void syncVisualHp(BattleEntity target) {
        if (target.isPlayer) {
            if (target.name.equals("Hero")) {
                if (!battlePlayers.isEmpty()) battlePlayers.get(0).hp = target.hp;
            } else {
                for (BattlePartyMember bpm : battlePartyMembers) {
                    if (bpm.member.className.equals(target.name)) {
                        bpm.hp = target.hp;
                        bpm.mp = target.mp;
                        break;
                    }
                }
            }
        } else {
            for (BattleEnemy be : battleEnemies) {
                if (be.enemy == target.enemyRef) {
                    be.hp = target.hp;
                    break;
                }
            }
        }
    }

    public void finalizeDeathsAndRewards() {
        for (BattleEntity enemyEntity : new ArrayList<>(battleParty.enemies)) {
            if (!enemyEntity.isAlive() && enemyEntity.enemyRef != null) {
                int[] rewards = enemyEntity.enemyRef.giveRewards(player);
                pendingExp += rewards[0];
                pendingGold += rewards[1];
            }
        }

        battleParty.removeDeadEntities();
        battleParty.syncPlayerHealth();

        battleEnemies.removeIf(be -> {
            for (BattleEntity enemyEntity : battleParty.enemies) {
                if (enemyEntity.enemyRef == be.enemy) return false;
            }
            return true;
        });

        if (battleParty.battleEnded) {
            victoryExp = pendingExp;
            victoryGold = pendingGold;
            victoryRewardsShown = false;
            gameState = battleVictoryState;

            if (lastKillerName.equals("Assassin")) {
                sound.playBattleSE("VICTORYASSASSIN");
            } else if (lastKillerName.equals("Mage")) {
                sound.playBattleSE("VICTORYMAGE");
            }
        }
    }

    public void updatePartyMembers() {

        if (gameState != playState || battleStarting) return;

        boolean playerMoving = keyH.leftPressed || keyH.rightPressed || keyH.upPressed || keyH.downPressed;

        for (int i = 0; i < partyMembers.size(); i++) {

            PartyMember member = partyMembers.get(i);

            // Delay = πόσο πίσω ακολουθεί
            int delay = (i + 1) * 15;

            if (playerPositions.size() > delay) {

                Point target = playerPositions.get(delay);

                int targetX = target.x;
                int targetY = target.y;

                int dx = targetX - member.worldX;
                int dy = targetY - member.worldY;

                // Κίνηση στον άξονα X
                if (Math.abs(dx) > 2) {
                    member.worldX += (int) Math.signum(dx) * member.speed;
                    member.direction = (dx > 0) ? "right" : "left";
                }

                // Κίνηση στον άξονα Y
                if (Math.abs(dy) > 2) {
                    member.worldY += (int) Math.signum(dy) * member.speed;
                    member.direction = (dy > 0) ? "down" : "up";
                }

                // Animation μόνο όταν κινείται ο player
                if (playerMoving) {
                    member.counter++;
                    if (member.counter > 10) {
                        member.frame = (member.frame == 0) ? 1 : 0;
                        member.counter = 0;
                    }
                } else {
                    member.frame = 0;
                    member.counter = 0;
                }

            }

            member.updateImage();
        }
    }

    // Ελέγχει αν η επόμενη θέση του ήρωα έχει collision - ΒΕΛΤΙΩΜΕΝΗ ΕΚΔΟΣΗ
    public boolean hasWorldCollision(int nextWorldX, int nextWorldY) {
        int leftX = nextWorldX;
        int rightX = nextWorldX + tileSize - 1;
        int topY = nextWorldY;
        int bottomY = nextWorldY + tileSize - 1;

        int topLeftCol = leftX / tileSize;
        int topLeftRow = topY / tileSize;
        int topRightCol = rightX / tileSize;
        int topRightRow = topY / tileSize;
        int bottomLeftCol = leftX / tileSize;
        int bottomLeftRow = bottomY / tileSize;
        int bottomRightCol = rightX / tileSize;
        int bottomRightRow = bottomY / tileSize;

        boolean collision = false;

        if (isValidWorldTile(topLeftRow, topLeftCol)) {
            collision = tileM.isTileCollision(currentMap, topLeftRow, topLeftCol);
        }
        if (!collision && isValidWorldTile(topRightRow, topRightCol)) {
            collision = tileM.isTileCollision(currentMap, topRightRow, topRightCol);
        }
        if (!collision && isValidWorldTile(bottomLeftRow, bottomLeftCol)) {
            collision = tileM.isTileCollision(currentMap, bottomLeftRow, bottomLeftCol);
        }
        if (!collision && isValidWorldTile(bottomRightRow, bottomRightCol)) {
            collision = tileM.isTileCollision(currentMap, bottomRightRow, bottomRightCol);
        }

        return collision;
    }

    // Για NPC και άλλες οντότητες (με Entity)
    public boolean checkCollision(Entity entity, int nextX, int nextY) {
        // Ίδιος κώδικας με το hasWorldCollision, αλλά μπορείς απλά να καλέσεις:
        return hasWorldCollision(nextX, nextY); // Αφού προς το παρόν κάνουν το ίδιο
    }

    // Βοηθητική μέθοδος για έλεγχο ορίων
    public boolean isValidWorldTile(int row, int col) {
        return tileM.isValidTile(currentMap, row, col);
    }

    // Έλεγχος σύγκρουσης μεταξύ δύο οντοτήτων (π.χ. ήρωας και NPC)
    public boolean checkEntityCollision(Entity e1, Entity e2, int nextWorldX, int nextWorldY) {
        if (!e2.solid) return false;
        
        // Όρια της e1 στην επόμενη θέση (world coordinates)
        int e1Left = nextWorldX;
        int e1Right = nextWorldX + tileSize;
        int e1Top = nextWorldY;
        int e1Bottom = nextWorldY + tileSize;
        
        // Όρια της e2 στην τωρινή θέση (world coordinates)
        int e2Left = e2.worldX;
        int e2Right = e2.worldX + tileSize;
        int e2Top = e2.worldY;
        int e2Bottom = e2.worldY + tileSize;
        
        if (e1Left < e2Right && e1Right > e2Left && 
            e1Top < e2Bottom && e1Bottom > e2Top) {
            return true;
        }
        
        return false;
    }

    public boolean checkDecorationCollision(Entity e, Decoration d, int nextWorldX, int nextWorldY) {
        if (!d.solid) return false;
        
        // Όρια παίκτη
        Rectangle playerRect = new Rectangle(nextWorldX, nextWorldY, tileSize, tileSize);
        
        // Αν έχει custom collision rectangles, χρησιμοποίησέ τα
        if (d.collisionRects != null && !d.collisionRects.isEmpty()) {
            for (Rectangle rect : d.collisionRects) {
                if (playerRect.intersects(rect)) {
                    return true;
                }
            }
            return false;
        }
        
        // Αλλιώς χρησιμοποίησε ολόκληρο το decoration
        Rectangle decRect = new Rectangle(d.worldX, d.worldY, d.width, d.height);
        return playerRect.intersects(decRect);
    }

    public BufferedImage scaleDecoration(BufferedImage original, int targetHeight) {
        // Υπολόγισε το πλάτος διατηρώντας αναλογία
        int originalWidth = original.getWidth();
        int originalHeight = original.getHeight();
        int targetWidth = (originalWidth * targetHeight) / originalHeight;
        
        BufferedImage scaled = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = scaled.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, 
                            RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2d.drawImage(original, 0, 0, targetWidth, targetHeight, null);
        g2d.dispose();
        
        return scaled;
    }

    // public void loadHouses() {
    //     try {
    //         // ========== ΜΕΓΑΛΟ ΣΠΙΤΙ (6x6) ==========
    //         BufferedImage originalHouse = ImageIO.read(new File("res/decorations/house_1.png"));
    //         BufferedImage scaledHouse = scaleDecoration(originalHouse, tileSize * 6);
            
    //         // Δημιούργησε τον πίνακα collision 6x6
    //         // true = δεν περνάς, false = περνάς
    //         boolean[][] bigHouseCollision = {
    //             {true,  true,  true,  true,  true,  true,  true,  true,  true},  // Πάνω σειρά
    //             {true,  true,  true,  true,  true,  true,  true,  true,  true},
    //             {true,  true,  true,  true,  true,  true,  true,  true,  true},
    //             {true,  true,  true,  true,  true,  true,  true,  true,  true},
    //             {false,  false,  false,  true,  true,  true,  true,  false,  false}  // Κάτω σειρά - η πόρτα στη μέση
    //         };
            
    //         // Δημιούργησε το decoration (χωρίς collision ακόμα)
    //         Decoration houseExt = new Decoration("Big House", scaledHouse,
    //                                             10 * tileSize, 9 * tileSize,
    //                                             scaledHouse.getWidth(), scaledHouse.getHeight(),
    //                                             new ArrayList<Rectangle>()); // Κενό collision
            
    //         decorations.get(3).add(houseExt);
            
    //         // Δημιούργησε το house object
    //         Rectangle doorArea = new Rectangle(
    //             14 * tileSize - tileSize/2,      // 14*48 - 24 = 648 pixels (λίγο πιο αριστερά)
    //             13 * tileSize - tileSize/2,      // 13*48 - 24 = 600 pixels (λίγο πιο πάνω)
    //             tileSize * 2,                    // 96 pixels πλάτος (2 tiles)
    //             tileSize * 2                      // 96 pixels ύψος (2 tiles)
    //         );

    //         House bigHouse = new House("Big House", 3, houseExt, doorArea,
    //                             4,                       // interiorMap = 4
    //                             12 * tileSize,            // spawnX = 672
    //                             13 * tileSize,            // spawnY = 624
    //                             14 * tileSize,            // exitX = 1200
    //                             14 * tileSize,            // exitY = 1200
    //                             this);
            
    //         // Πέρασε τον πίνακα collision
    //         bigHouse.setCollisionMap(bigHouseCollision);
            
    //         // ========== Φόρτωσε animated door για το μεγάλο σπίτι ==========
    //         AnimatedDoor bigHouseDoor = new AnimatedDoor();
    //         bigHouseDoor.loadFrames("res/decorations/door_big/", "house", 6); // 5 frames
    //         bigHouse.setAnimatedDoor(bigHouseDoor, 2, 6); // Η πόρτα είναι στο col 2, row 5 (0-based)
            
    //         houses.add(bigHouse);
            
    //         // ========== ΜΙΚΡΟ ΣΠΙΤΙ (4x4) ==========
    //         BufferedImage originalSmallHouse = ImageIO.read(new File("res/decorations/house_2.png"));
    //         BufferedImage scaledSmallHouse = scaleDecoration(originalSmallHouse, tileSize * 5);
            
    //         // Πίνακας collision 4x4 - όλα true εκτός από την πόρτα
    //         boolean[][] smallHouseCollision = {
    //             {true,  true,  true,  true,  true,  true},  // Πάνω σειρά
    //             {true,  true,  true,  true,  true,  true},
    //             {true,  true,  true,  true,  true,  true},
    //             {true,  true,  true,  true,  true,  true},
    //             {true,  true,  true,  true,  true,  true},
    //             {true,  true,  true, true,  true,  true}   // Κάτω σειρά - η πόρτα στη μέση
    //         };
            
    //         Decoration smallHouseExt = new Decoration("Small House", scaledSmallHouse,
    //                                                 29 * tileSize, 9 * tileSize,
    //                                                 scaledSmallHouse.getWidth(), scaledSmallHouse.getHeight(),
    //                                                 new ArrayList<Rectangle>());
            
    //         decorations.get(3).add(smallHouseExt);
            
    //         Rectangle smallDoorArea = new Rectangle(
    //             29 * tileSize - tileSize,      // 31*48 - 24 = 1464 pixels
    //             14 * tileSize - tileSize,      // 14*48 - 24 = 648 pixels
    //             tileSize * 3,                    // 144 pixels πλάτος
    //             tileSize * 3                      // 144 pixels ύψος
    //         );
            
    //         House smallHouse = new House("Small House", 3, smallHouseExt, smallDoorArea,
    //                             5,                       // interiorMap = 5
    //                             12 * tileSize,            // spawnX = 672
    //                             13 * tileSize,            // spawnY = 624
    //                             31 * tileSize,            // exitX = 1200
    //                             14 * tileSize,            // exitY = 1200
    //                             this);
            
    //         smallHouse.setCollisionMap(smallHouseCollision);
            
    //         // Προαιρετικά animated door για το μικρό σπίτι
    //         AnimatedDoor smallHouseDoor = new AnimatedDoor();
    //         smallHouseDoor.loadFrames("res/decorations/door_small/", "door", 6);
    //         smallHouse.setAnimatedDoor(smallHouseDoor, 1, 3);
            
    //         houses.add(smallHouse);
            
    //         System.out.println("Houses loaded successfully!");
            
    //     } catch (Exception e) {
    //         e.printStackTrace();
    //     }
    // }

    // // Μέθοδος για έξοδο από το σπίτι (π.χ. όταν πατάς Enter μπροστά από την πόρτα από μέσα)
    //     public void exitHouse(House house) {
    //         currentMap = house.exteriorMap;
    //         player.worldX = house.exitX;
    //         player.worldY = house.exitY;
            
    //         // Επανάφερε την εξωτερική μουσική
    //         if (currentMap == 3) { // town
    //             if (dayTime == 0 || dayTime == 3) {
    //                 sound.playMusic("town_day");
    //             } else {
    //                 sound.playMusic("town_night");
    //             }
    //         }
            
    //         playSound("door_close");
    //     }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        // Μετατροπή Graphics σε Graphics2D (πιο δυνατό)
        Graphics2D g2 = (Graphics2D) g;

        if (isFullScreen) {
            g2.scale(scaleX, scaleY);
        }
        

        if (gameState == titleState) {
            drawTitleScreen(g2);
        } else if (gameState == controlsState) {
            drawControlsScreen(g2, "TITLE");
        }else if (gameState == pauseControlsState) {
            drawControlsScreen(g2, "PAUSE"); 
        } else if (gameState == pauseState) {
            // Πρώτα ζωγράφισε το κανονικό παιχνίδι
            drawGame(g2);
            drawLighting(g2);
            // Μετά ζωγράφισε το pause menu από πάνω
            drawPauseScreen(g2);
        } else if (gameState == quitConfirmState) {
            // Πρώτα ζωγράφισε το κανονικό παιχνίδι
            drawGame(g2);
            drawLighting(g2);
            // Μετά ζωγράφισε το quit confirm
            drawQuitConfirmScreen(g2);
        } else {
            // Κανονικό παιχνίδι
            drawGame(g2);
            drawLighting(g2);

            // ========== Battle Screen ==========
            if (gameState == battleState) {
                drawBattleScreen(g2);
            } else if (gameState == battleVictoryState) { // ΝΕΟ
                drawBattleScreen(g2); // Ζωγράφισε πρώτα τη μάχη
                // Το victory message θα ζωγραφιστεί μέσα στο drawBattleScreen
            }

            // Αν είμαστε σε κατάσταση διαλόγου, ζωγράφισε την οθόνη διαλόγου
            if (gameState == dialogueState || gameState == dialogueOptionsState) {
                drawDialogueScreen(g2);
            }

            if (gameState == inventoryState) {
                drawInventory(g2);
            }
            if (gameState == mapState) {
                drawMapScreen(g2);
            }
            if (gameState == shopState) {
                drawShopScreen(g2);
            }
        }
        // ========== ΠΡΟΣΘΕΣΕ ΤΟ FADE EFFECT ΓΙΑ ΟΛΕΣ ΤΙΣ ΚΑΤΑΣΤΑΣΕΙΣ ==========
        // Αυτό ζωγραφίζεται ΠΑΝΤΑ από πάνω, ανεξάρτητα από το gameState
        if (battleFadeOut || battleFadeIn) {
            g2.setColor(new Color(0, 0, 0, battleFadeAlpha));
            g2.fillRect(0, 0, screenWidth, screenHeight);
        }
        
        g2.dispose(); // Καθάρισε (καλή πρακτική)
    }

    // ==== ζωγραφίζεις το παιχνίδι ===== 
    public void drawGame(Graphics2D g2) {
        // 🗺️ ΖΩΓΡΑΦΙΣΕ ΠΡΩΤΑ ΤΟΝ ΧΑΡΤΗ (από κάτω)
        tileM.draw(g2);
        
        // ========== DECORATIONS (πίσω από τον παίκτη) ==========
        ArrayList<Decoration> currentMapDecorations = decorations.get(currentMap);
        for (Decoration dec : currentMapDecorations) {
            int screenX = dec.worldX - worldX;
            int screenY = dec.worldY - worldY;
            
            // Έλεγξε αν είναι ορατό στην οθόνη
            if (screenX + dec.width > 0 && screenX < screenWidth &&
                screenY + dec.height > 0 && screenY < screenHeight) {
                
                // Ζωγράφισε στο ΚΑΝΟΝΙΚΟ του μέγεθος
                g2.drawImage(dec.image, screenX, screenY, dec.width, dec.height, null);
            }
        }
        // Ζωγράφισε items στο έδαφος
        ArrayList<ItemOnGround> currentMapItems = itemsOnGround.get(currentMap);
        for (ItemOnGround item : currentMapItems) {
            int screenX = item.worldX - worldX;
            int screenY = item.worldY - worldY;
            
            if (screenX + tileSize > 0 && screenX < screenWidth &&
                screenY + tileSize > 0 && screenY < screenHeight) {
                g2.drawImage(item.image, screenX, screenY, tileSize, tileSize, null);
            }
        }

        // ========== ΖΩΓΡΑΦΙΣΕ ΤΑ ΜΕΛΗ ΤΗΣ ΟΜΑΔΑΣ (ΠΙΣΩ ΑΠΟ ΤΟΝ ΠΑΙΚΤΗ) ==========
        for (PartyMember member : partyMembers) {
            int screenX = member.worldX - worldX;
            int screenY = member.worldY - worldY;
            if (screenX + tileSize > 0 && screenX < screenWidth &&
                screenY + tileSize > 0 && screenY < screenHeight) {
                g2.drawImage(member.currentImage, screenX, screenY, tileSize, tileSize, null);
            }
        }
        
        // 🧙 ΖΩΓΡΑΦΙΣΕ ΜΕΤΑ ΤΟΝ ΗΡΩΑ (από πάνω)
        if (currentPlayerImage != null) {
            int screenX = player.worldX - worldX;
            int screenY = player.worldY - worldY;
            g2.drawImage(currentPlayerImage, screenX, screenY, tileSize, tileSize, null);
        }
        
        // 👴 NPC
        ArrayList<Entity> currentMapNPCs = npcs.get(currentMap);
        for (Entity npc : currentMapNPCs) {
            int npcScreenX = npc.worldX - worldX;
            int npcScreenY = npc.worldY - worldY;
            g2.drawImage(npc.currentImage, npcScreenX, npcScreenY, tileSize, tileSize, null);
        }
    }

    public void drawPanel(Graphics2D g2, int x, int y, int width, int height, Color bgColor, Color borderColor) {
        // Σκούρο ημιδιάφανο φόντο
        g2.setColor(bgColor);
        g2.fillRoundRect(x, y, width, height, 35, 35);
        
        // Λευκό περίγραμμα
        g2.setColor(borderColor);
        g2.setStroke(new BasicStroke(3));
        g2.drawRoundRect(x, y, width, height, 35, 35);
    }

    // Overload για default χρώματα (όπως στα dialogue boxes)
    public void drawPanel(Graphics2D g2, int x, int y, int width, int height) {
        drawPanel(g2, x, y, width, height, new Color(0, 0, 0, 220), Color.white);
    }

    public void drawTitleScreen(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0));
        g2.fillRect(0, 0, screenWidth, screenHeight);
        
        if (titleLogo != null) {
            int logoWidth = screenWidth - 200;
            int logoHeight = logoWidth / 2;
            int logoX = screenWidth/2 - logoWidth/2;
            int logoY = 20;
            g2.drawImage(titleLogo, logoX, logoY, logoWidth, logoHeight, null);
        } else {
            g2.setFont(maruMonicaLarge);
            String text = "My RPG";
            int x = getXforCenteredText(text, g2);
            int y = 150;
            
            g2.setColor(Color.gray);
            g2.drawString(text, x + 5, y + 5);
            g2.setColor(Color.white);
            g2.drawString(text, x, y);
        }
        
        g2.setFont(maruMonicaBold);
        String[] options = {"START GAME", "SETTINGS", "CONTROLS"};
        int startY = 300;
        int spacing = 60;
        
        for (int i = 0; i < options.length; i++) {
            int x = getXforCenteredText(options[i], g2) - 10;
            int y = startY + i * spacing;
            
            if (i == titleCommandNum) {
                g2.setColor(Color.yellow);
                g2.drawString(">", x - 40, y);
                g2.drawString(options[i], x, y);
            } else {
                g2.setColor(Color.white);
                g2.drawString(options[i], x, y);
            }
        }
        
        g2.setFont(maruMonicaSmall);
        g2.setColor(Color.gray);
        String controls = "Use ↑/↓ to navigate, Enter to select";
        int x = getXforCenteredText(controls, g2);
        g2.drawString(controls, x, screenHeight - 50);
    }

    public void drawControlsScreen(Graphics2D g2, String source) {
        g2.setColor(new Color(0, 0, 0, 220));
        g2.fillRect(0, 0, screenWidth, screenHeight);
        
        // Τίτλος
        g2.setFont(maruMonicaLarge);
        g2.setColor(Color.yellow);
        String title = "CONTROLS";
        int x = getXforCenteredText(title, g2);
        g2.drawString(title, x, 80);
        
        // Πλαίσιο για τα controls
        drawPanel(g2, 100, 120, screenWidth - 200, screenHeight - 240);
        
        // Controls
        g2.setFont(maruMonica);
        g2.setColor(Color.white);
        
        String[][] controls = {
            {"ARROW KEYS", "Move character"},
            {"ENTER", "Interact with NPCs / Confirm"},
            {"I", "Open/Close Inventory"},
            {"E", "Pick up items"},
            {"M", "Open Map (requires World Map)"},
            {"SHIFT", "Switch between Storage/Equipment"},
            {"ESC", "Open Pause Menu"},
            {"Q", "Clear quests (debug)"}
        };
        
        int startY = 180;
        int spacing = 40;
        
        for (int i = 0; i < controls.length; i++) {
            g2.setColor(Color.yellow);
            g2.drawString(controls[i][0], 150, startY + i * spacing);
            g2.setColor(Color.lightGray);
            g2.drawString(controls[i][1], 350, startY + i * spacing);
        }
        
        // Μάχη controls
        g2.setColor(Color.cyan);
        g2.setFont(maruMonicaBold);
        String battleTitle = "BATTLE CONTROLS";
        x = getXforCenteredText(battleTitle, g2);
        g2.drawString(battleTitle, x, 500);
        
        g2.setFont(maruMonica);
        g2.setColor(Color.white);
        String[] battleControls = {
            "←/→ : Select action",
            "ENTER : Confirm action"
        };
        
        for (int i = 0; i < battleControls.length; i++) {
            x = getXforCenteredText(battleControls[i], g2);
            g2.drawString(battleControls[i], x, 540 + i * 30);
        }
        
        // Οδηγία εξόδου
        g2.setFont(maruMonicaSmall);
        g2.setColor(Color.gray);
        String exit;
        if (source.equals("TITLE")) {
            exit = "Press ENTER or M or I to return to title screen";
        } else {
            exit = "Press ENTER or ESC to return to pause menu";
        }
        x = getXforCenteredText(exit, g2);
        g2.drawString(exit, x, screenHeight - 50);
    }

    // Βοηθητική μέθοδος για κεντράρισμα κειμένου
    public int getXforCenteredText(String text, Graphics2D g2) {
        int length = (int)g2.getFontMetrics().getStringBounds(text, g2).getWidth();
        return screenWidth/2 - length/2;
    }

    // ========== Μέθοδος για μάχη ==========
    public void drawBattleScreen(Graphics2D g2) {
        Graphics2D g = (Graphics2D) g2.create();

        int shakeX = 0;
        int shakeY = 0;
        if (screenShakeTimer > 0) {
            shakeX = (int)((Math.random() * (screenShakeStrength * 2 + 1)) - screenShakeStrength);
            shakeY = (int)((Math.random() * (screenShakeStrength * 2 + 1)) - screenShakeStrength);
        }

        // Zoom από το κέντρο της οθόνης
        g.translate(screenWidth / 2, screenHeight / 2);
        g.scale(zoomPopScale, zoomPopScale);
        g.translate(-screenWidth / 2, -screenHeight / 2);

        // Μετά πρόσθεσε shake
        g.translate(shakeX, shakeY);

        // Σκούρο φόντο
        g.setColor(new Color(0, 0, 0, 200));
        g.fillRect(0, 0, screenWidth, screenHeight);

        // Ζωγράφισε το background
        if (battleBackground != null) {
            g.drawImage(battleBackground, 0, 0, screenWidth, screenHeight, null);
        }

        // ========== ΠΛΑΤΦΟΡΜΑ ==========
        if (groundImage != null) {
            int groundScreenX = groundX;
            int groundScreenY = groundY;
            int gw = groundWidth;
            int gh = groundHeight;
            int topInset = -200;
            int leftExtension = 500;
            int rightExtension = 500;

            int[] xPoints = {
                groundScreenX + topInset,
                groundScreenX + gw - topInset,
                groundScreenX + gw + rightExtension,
                groundScreenX - leftExtension
            };
            int[] yPoints = {
                groundScreenY,
                groundScreenY,
                groundScreenY + gh,
                groundScreenY + gh
            };

            java.awt.Shape oldClip = g.getClip();
            java.awt.Polygon groundPolygon = new java.awt.Polygon(xPoints, yPoints, 4);
            g.setClip(groundPolygon);

            g.drawImage(groundImage,
                groundScreenX - leftExtension - 100,
                groundScreenY - 50,
                gw + leftExtension + rightExtension + 200,
                gh + 100,
                null);

            g.setClip(oldClip);

            // Σκιά
            g.setColor(new Color(0, 0, 0, 70));
            int[] shadowX = {
                groundScreenX - leftExtension + 20,
                groundScreenX + gw + rightExtension - 20,
                groundScreenX + gw + rightExtension,
                groundScreenX - leftExtension
            };
            int[] shadowY = {
                groundScreenY + gh,
                groundScreenY + gh,
                groundScreenY + gh + 15,
                groundScreenY + gh + 15
            };
            g.fillPolygon(shadowX, shadowY, 4);
        }

        // ========== ΕΧΘΡΟΙ (ΣΕ ΣΧΗΜΑ ΚΩΝΟΥ) ==========
        int enemySpriteSize = tileSize * 8;
        int numEnemies = battleEnemies.size();

        for (int i = 0; i < numEnemies; i++) {
            BattleEnemy be = battleEnemies.get(i);
            BattleEntity enemyEntity = (i < battleParty.enemies.size()) ? battleParty.enemies.get(i) : null;

            if (enemyEntity != null && !enemyEntity.isAlive() && gameState == battleVictoryState) {
                continue;
            }

            int drawX;
            int drawY;

            if (numEnemies == 1) {
                drawX = screenWidth / 2 - enemySpriteSize - 40;
                drawY = groundY - enemySpriteSize + (tileSize * 2) - 50;
            } else if (numEnemies == 2) {
                if (i == 0) {
                    drawX = screenWidth / 2 - enemySpriteSize - 40;
                    drawY = groundY - enemySpriteSize + (tileSize * 2) - 70;
                } else {
                    drawX = screenWidth / 2 - enemySpriteSize + 60;
                    drawY = groundY - enemySpriteSize + (tileSize * 2) - 30;
                }
            } else {
                double angle = (i - (numEnemies - 1) / 2.0) * 0.3;
                drawX = screenWidth / 2 - enemySpriteSize + (int)(Math.sin(angle) * 200);
                drawY = groundY - enemySpriteSize + (tileSize * 2) - (int)(Math.abs(angle) * 50) - 50;
            }

            // Σκιά εχθρού
            g.setColor(new Color(0, 0, 0, 100));
            g.fillOval(drawX + enemySpriteSize / 3, drawY + enemySpriteSize - 10,
                    enemySpriteSize / 4, enemySpriteSize / 8);

            // Εχθρός
            int recoilX = getHitRecoilDrawOffsetX(enemyEntity, true);
            int recoilY = getHitRecoilDrawOffsetY(enemyEntity);

            // sprite
            g.drawImage(be.getCurrentImage(), drawX + recoilX, drawY + recoilY, enemySpriteSize, enemySpriteSize, null);

            // dust στα πόδια
            drawImpactDust(g, enemyEntity,
                    drawX + recoilX + enemySpriteSize / 2,
                    drawY + recoilY + enemySpriteSize - 10,
                    boostBurstLevel > 0 ? boostBurstLevel : 1);

            // HP Bar πάνω από τον εχθρό
            if (enemyEntity != null) {
                int barWidth = 120;
                int barHeight = 10;
                int barX = drawX + enemySpriteSize / 2 - barWidth / 2;
                int barY = drawY - 20;

                g.setColor(Color.black);
                g.fillRect(barX, barY, barWidth, barHeight);
                g.setColor(Color.red);
                double hpPercentage = (double) enemyEntity.hp / enemyEntity.maxHp;
                int hpWidth = (int) (barWidth * hpPercentage);
                g.fillRect(barX, barY, hpWidth, barHeight);
            }

            // Animation ζημιάς
            if (enemyEntity != null && enemyEntity.isTakingDamage) {
                enemyEntity.damageTimer--;
                if (enemyEntity.damageTimer <= 0) {
                    enemyEntity.isTakingDamage = false;
                }

                g.setFont(new Font("Arial", Font.BOLD, 24));
                g.setColor(Color.red);
                int offsetY = -(enemyEntity.DAMAGE_DURATION - enemyEntity.damageTimer) / 2;
                String damageText = "-" + enemyEntity.damageNumber;
                int textWidth = g.getFontMetrics().stringWidth(damageText);
                int textX = drawX + enemySpriteSize / 2 - textWidth / 2;
                int textY = drawY - 40 - offsetY;
                g.drawString(damageText, textX, textY);
            }
        }

        // ========== ΠΑΙΚΤΕΣ (ΣΕ ΔΙΑΓΩΝΙΑ ΣΤΟΙΧΙΣΗ) ==========
        int playerSpriteSize = tileSize * 4;
        int basePlayerX = 600;
        int basePlayerY = groundY - playerSpriteSize + 100;

        BattleEntity currentTurn = battleParty.getCurrentTurn();

        // 1. HERO
        if (!battlePlayers.isEmpty()) {
            BattlePlayer bp = battlePlayers.get(0);
            int drawX = basePlayerX;
            int drawY = basePlayerY;

            // Σκιά ήρωα
            g.setColor(new Color(0, 0, 0, 100));
            g.fillOval(drawX + playerSpriteSize / 4, drawY + playerSpriteSize - 10,
                    playerSpriteSize / 2, playerSpriteSize / 8);

            if (currentTurn != null && currentTurn.isPlayer &&
                currentTurn.name.equals("Hero") &&
                selectedBoost > 0 &&
                !actionInProgress) {

                int auraCenterX = drawX + playerSpriteSize / 2;
                int auraFootY = drawY + playerSpriteSize - 10;
                drawBoostAura(g, auraCenterX, auraFootY, selectedBoost);
            }

            BattleEntity heroEntity = null;
            for (BattleEntity entity : battleParty.party) {
                if (entity.name.equals("Hero")) {
                    heroEntity = entity;
                    break;
                }
            }

            int recoilX = getHitRecoilDrawOffsetX(heroEntity, false);
            int recoilY = getHitRecoilDrawOffsetY(heroEntity);
            int lungeX = getLungeOffsetX(heroEntity);
            int lungeY = getLungeOffsetY(heroEntity);

            BufferedImage playerImg = bp.getCurrentImage();
            if (playerImg != null) {
                drawAfterimageTrail(g, playerImg, heroEntity,
                        drawX + recoilX + lungeX,
                        drawY + recoilY + lungeY,
                        playerSpriteSize, playerSpriteSize);
                drawSwingTrail(g, heroEntity,
                        drawX + recoilX + lungeX + playerSpriteSize / 2,
                        drawY + recoilY + lungeY + playerSpriteSize / 2);
                g.drawImage(playerImg, drawX + recoilX + lungeX, drawY + recoilY + lungeY, playerSpriteSize, playerSpriteSize, null);
                drawImpactDust(g, heroEntity, drawX + recoilX + playerSpriteSize / 2, drawY + recoilY + playerSpriteSize - 10,1);
            }
        }

        // 2. PARTY MEMBERS
        for (int i = 0; i < battlePartyMembers.size(); i++) {
            BattlePartyMember bpm = battlePartyMembers.get(i);

            int offsetX = -80 - (i * 60);
            int offsetY = -40 - (i * 40);

            int drawX = basePlayerX + offsetX;
            int drawY = basePlayerY + offsetY;

            // Σκιά
            g.setColor(new Color(0, 0, 0, 100));
            g.fillOval(drawX + playerSpriteSize / 4, drawY + playerSpriteSize - 10,
                    playerSpriteSize / 2, playerSpriteSize / 8);

            if (currentTurn != null && currentTurn.isPlayer &&
                currentTurn.name.equals(bpm.member.className) &&
                selectedBoost > 0 &&
                !actionInProgress) {

                int auraCenterX = drawX + playerSpriteSize / 2;
                int auraFootY = drawY + playerSpriteSize - 10;
                drawBoostAura(g, auraCenterX, auraFootY, selectedBoost);
            }

            BattleEntity memberEntity = null;
            for (BattleEntity entity : battleParty.party) {
                if (entity.name.equals(bpm.member.className)) {
                    memberEntity = entity;
                    break;
                }
            }

            int recoilX = getHitRecoilDrawOffsetX(memberEntity, false);
            int recoilY = getHitRecoilDrawOffsetY(memberEntity);
            int lungeX = getLungeOffsetX(memberEntity);
            int lungeY = getLungeOffsetY(memberEntity);

            BufferedImage memberImg = bpm.getCurrentImage();
            if (memberImg != null) {
                drawAfterimageTrail(g, memberImg, memberEntity,
                        drawX + recoilX + lungeX,
                        drawY + recoilY + lungeY,
                        playerSpriteSize, playerSpriteSize);
                drawSwingTrail(g, memberEntity,
                        drawX + recoilX + lungeX + playerSpriteSize / 2,
                        drawY + recoilY + lungeY + playerSpriteSize / 2);
                g.drawImage(memberImg, drawX + recoilX + lungeX, drawY + recoilY + lungeY, playerSpriteSize, playerSpriteSize, null);
                drawImpactDust(g, memberEntity, drawX + recoilX + playerSpriteSize / 2, drawY + recoilY + playerSpriteSize - 10,1);
            }
        }

        // ===== BOOST BURST EFFECT ΠΑΝΩ ΑΠΟ ΤΑ SPRITES, ΠΡΙΝ ΤΟ UI =====
        drawBoostBurstEffect(g);
        drawImpactSlashLines(g);
        drawHitFlashOverlay(g);

        // ========== ΜΕΝΟΥ ΜΑΧΗΣ ==========
        if (!battleEntering) {
            // Status party
            drawPartyStatus(g);

            // Μήνυμα ενέργειας
            if (actionMessageTimer > 0) {
                actionMessageTimer--;

                int msgWidth = 250;
                int msgHeight = 30;
                int msgX = screenWidth / 2 - msgWidth / 2;
                int msgY = 80;

                g.setColor(new Color(0, 0, 0, 180));
                g.fillRoundRect(msgX, msgY, msgWidth, msgHeight, 10, 10);
                g.setColor(Color.white);
                g.setStroke(new BasicStroke(1));
                g.drawRoundRect(msgX, msgY, msgWidth, msgHeight, 10, 10);

                g.setFont(maruMonicaSmall.deriveFont(12f));
                g.setColor(Color.white);
                int textX = getXforCenteredText(lastAction, g);
                g.drawString(lastAction, textX, msgY + 20);
            }

            // Μήνυμα νίκης
            if (gameState == battleVictoryState) {
                g.setColor(new Color(0, 0, 0, 150));
                g.fillRect(0, 0, screenWidth, screenHeight);

                g.setFont(maruMonicaLarge);
                g.setColor(Color.yellow);
                String victory = "VICTORY!";
                int x = getXforCenteredText(victory, g);
                g.drawString(victory, x, screenHeight / 2 - 50);

                g.setFont(maruMonicaBold);
                g.setColor(Color.white);
                String expText = "+" + victoryExp + " EXP";
                String goldText = "+" + victoryGold + " Gold";

                x = getXforCenteredText(expText, g);
                g.drawString(expText, x, screenHeight / 2);

                x = getXforCenteredText(goldText, g);
                g.drawString(goldText, x, screenHeight / 2 + 40);

                victoryTimer++;
                if ((victoryTimer / 30) % 2 == 0) {
                    g.setFont(maruMonicaSmall);
                    g.setColor(Color.lightGray);
                    String cont = "Press ENTER to continue";
                    x = getXforCenteredText(cont, g);
                    g.drawString(cont, x, screenHeight / 2 + 70);
                }
            }

            // Σειρά σειράς
            drawBattleTurnOrder(g);

            // Κύριο μενού
            int menuX = 0;
            int menuY = screenHeight - tileSize * 3;
            int menuWidth = screenWidth;
            int menuHeight = tileSize * 3;

            drawPanel(g, menuX, menuY, menuWidth, menuHeight, new Color(0, 0, 0, 220), Color.white);

            // Τίτλος σειράς
            g.setFont(maruMonicaBold);
            g.setColor(Color.yellow);
            BattleEntity current = battleParty.getCurrentTurn();
            String turnText = (current != null && current.isPlayer) ? "YOUR TURN" : "ENEMY TURN";
            int turnX = getXforCenteredText(turnText, g);
            g.drawString(turnText, turnX, menuY + 25);

            // Μενού επιλογών (μόνο για παίκτη)
            if (battleParty.isPlayerTurn() && !selectingTarget && !actionInProgress && !commandLocked) {
                g.setFont(maruMonica);
                int optionSpacing = menuWidth / battleMenuOptions.length;

                for (int i = 0; i < battleMenuOptions.length; i++) {
                    int optionX = 20 + (i * optionSpacing);
                    int optionY = menuY + 70;

                    String optionText = battleMenuOptions[i];
                    if (i == 0 && selectedBoost > 0) {
                        optionText = "Attack +" + selectedBoost;
                    }

                    if (i == battleMenuOption) {
                        g.setFont(maruMonicaBold);
                        g.setColor(Color.yellow);
                        g.drawString("▶", optionX - 15, optionY);

                        if (i == 0) {
                            BufferedImage weaponImg = getWeaponImage();
                            if (weaponImg != null) {
                                g.drawImage(weaponImg, optionX + 85, optionY - 20, 20, 20, null);
                            }
                        }

                        g.drawString(optionText, optionX, optionY);

                        g.setFont(maruMonicaSmall);
                        g.setColor(Color.lightGray);
                        String desc = getBattleOptionDescription(i);
                        int descX = getXforCenteredText(desc, g);
                        g.drawString(desc, descX, menuY + 100);

                        g.setColor(Color.white);
                        g.drawString("B: Boost Up   C: Boost Down", menuX + 25, menuY + 120);
                        g.drawString("Current Boost: " + selectedBoost, menuX + 25, menuY + 138);
                        drawMiniBoostIndicator(g, menuX + 145, menuY + 128, selectedBoost);

                        g.setFont(maruMonica);
                    } else {
                        g.setColor(Color.white);
                        g.drawString(optionText, optionX, optionY);
                    }
                }

            } else if (selectingTarget) {
                // Μενού επιλογής στόχου
                g.setFont(maruMonicaBold);
                g.setColor(Color.yellow);
                g.drawString("Select target:", menuX + 50, menuY + 70);

                g.setFont(maruMonicaSmall);
                g.setColor(Color.lightGray);
                g.drawString("ENTER=Confirm   ESC=Back", menuX + 50, menuY + 92);
                g.drawString("Boost: " + selectedBoost, menuX + 260, menuY + 92);

                int targetY = menuY + 110;
                for (int i = 0; i < battleParty.enemies.size(); i++) {
                    BattleEntity enemy = battleParty.enemies.get(i);
                    int x = menuX + 100 + (i * 150);

                    if (i == selectedTarget) {
                        g.setColor(Color.yellow);
                        g.drawString("▶ " + enemy.name, x, targetY);
                    } else {
                        g.setColor(Color.white);
                        g.drawString(enemy.name, x + 15, targetY);
                    }

                    int barWidth = 100;
                    int barHeight = 8;
                    int barX = x;
                    int barY = targetY + 10;

                    g.setColor(Color.black);
                    g.fillRect(barX, barY, barWidth, barHeight);
                    g.setColor(Color.green);
                    double hpPercentage = (double) enemy.hp / enemy.maxHp;
                    int hpWidth = (int) (barWidth * hpPercentage);
                    g.fillRect(barX, barY, hpWidth, barHeight);
                }
            }
        }

        g.dispose();
    }

    public void drawPartyStatus(Graphics2D g2) {
        int startX = screenWidth - 200;
        int startY = 20;
        int slotHeight = 50;
        int spacing = 8;

        int panelWidth = 190;
        int slotWidth = 180;

        // Φόντο για όλο το party panel
        g2.setColor(new Color(0, 0, 0, 180));
        g2.fillRoundRect(startX - 8, startY - 8, panelWidth,
                (battleParty.party.size() * (slotHeight + spacing)) + 8, 10, 10);
        g2.setColor(Color.white);
        g2.setStroke(new BasicStroke(1));
        g2.drawRoundRect(startX - 8, startY - 8, panelWidth,
                (battleParty.party.size() * (slotHeight + spacing)) + 8, 10, 10);

        for (int i = 0; i < battleParty.party.size(); i++) {
            BattleEntity entity = battleParty.party.get(i);
            int y = startY + i * (slotHeight + spacing);

            // Φόντο slot
            g2.setColor(new Color(30, 30, 30, 200));
            g2.fillRoundRect(startX - 5, y - 5, slotWidth, slotHeight, 8, 8);

            // ===== NAME =====
            g2.setFont(maruMonicaSmall);
            g2.setColor(Color.yellow);

            String name = entity.name;
            if (name.length() > 8) name = name.substring(0, 8);

            int nameX = startX;
            int nameY = y + 8;
            g2.drawString(name, nameX, nameY);

            // ===== BP DOTS =====
            // Δίπλα από το όνομα, στην ίδια οπτική γραμμή
            int bpX = startX + 78;
            int bpY = y + 1;
            drawBPDots(g2, entity, bpX, bpY);

            // ===== HP BAR =====
            int barWidth = 150;
            int barHeight = 10;
            int hpBarY = y + 15;

            g2.setColor(Color.darkGray);
            g2.fillRect(startX, hpBarY, barWidth, barHeight);

            g2.setColor(Color.red);
            double hpPercentage = (double) entity.hp / entity.maxHp;
            int hpWidth = (int) (barWidth * hpPercentage);
            g2.fillRect(startX, hpBarY, hpWidth, barHeight);

            g2.setFont(new Font("Arial", Font.BOLD, 8));
            g2.setColor(Color.white);
            String hpText = entity.hp + "/" + entity.maxHp;
            int textX = startX + (barWidth / 2) - 15;
            g2.drawString(hpText, textX, hpBarY + 8);

            // ===== MP BAR =====
            if (entity.isPlayer) {
                int mpBarY = hpBarY + barHeight + 2;

                g2.setColor(Color.darkGray);
                g2.fillRect(startX, mpBarY, barWidth, barHeight);

                g2.setColor(Color.blue);
                double mpPercentage = (double) entity.mp / entity.maxMp;
                int mpWidth = (int) (barWidth * mpPercentage);
                g2.fillRect(startX, mpBarY, mpWidth, barHeight);

                g2.setFont(new Font("Arial", Font.BOLD, 8));
                g2.setColor(Color.white);
                String mpText = entity.mp + "/" + entity.maxMp;
                textX = startX + (barWidth / 2) - 15;
                g2.drawString(mpText, textX, mpBarY + 8);
            }
        }
    }

    public void drawBattleTurnOrder(Graphics2D g2) {
        int startX = 20;  // Πάνω αριστερά
        int startY = 10;
        int slotWidth = 48;  // Λίγο μεγαλύτερο (ίδιο με tileSize)
        int slotHeight = 48;
        int spacing = 5;     // Μικρή απόσταση μεταξύ slots
        
        // Τρέχων γύρος
        int currentIndex = battleParty.currentTurnIndex;
        
        for (int i = 0; i < battleParty.turnOrder.size(); i++) {
            BattleEntity entity = battleParty.turnOrder.get(i);
            
            // Υπολόγισε θέση οριζόντια
            int x = startX + i * (slotWidth + spacing);
            int y = startY;
            
            // Διάλεξε χρώμα πλαισίου ανάλογα με το ποιος είναι
            if (entity.isPlayer) {
                // Δικοί μας - Μπλε
                if (i == currentIndex) {
                    g2.setColor(new Color(0, 100, 255, 180)); // Μπλε με διαφάνεια
                    g2.fillRoundRect(x, y, slotWidth, slotHeight, 10, 10);
                    g2.setColor(new Color(0, 150, 255)); // Πιο φωτεινό μπλε για περίγραμμα
                    g2.setStroke(new BasicStroke(3));
                    g2.drawRoundRect(x, y, slotWidth, slotHeight, 10, 10);
                } else {
                    g2.setColor(new Color(0, 70, 150, 150)); // Σκούρο μπλε με διαφάνεια
                    g2.fillRoundRect(x, y, slotWidth, slotHeight, 10, 10);
                    g2.setColor(new Color(100, 150, 255)); // Ανοιχτό μπλε για περίγραμμα
                    g2.setStroke(new BasicStroke(2));
                    g2.drawRoundRect(x, y, slotWidth, slotHeight, 10, 10);
                }
            } else {
                // Αντίπαλοι - Κόκκινο
                if (i == currentIndex) {
                    g2.setColor(new Color(255, 0, 0, 180)); // Κόκκινο με διαφάνεια
                    g2.fillRoundRect(x, y, slotWidth, slotHeight, 10, 10);
                    g2.setColor(new Color(255, 100, 100)); // Πιο φωτεινό κόκκινο για περίγραμμα
                    g2.setStroke(new BasicStroke(3));
                    g2.drawRoundRect(x, y, slotWidth, slotHeight, 10, 10);
                } else {
                    g2.setColor(new Color(150, 0, 0, 150)); // Σκούρο κόκκινο με διαφάνεια
                    g2.fillRoundRect(x, y, slotWidth, slotHeight, 10, 10);
                    g2.setColor(new Color(255, 100, 100)); // Ανοιχτό κόκκινο για περίγραμμα
                    g2.setStroke(new BasicStroke(2));
                    g2.drawRoundRect(x, y, slotWidth, slotHeight, 10, 10);
                }
            }
            
            // Εικόνα οντότητας
            BufferedImage icon = null;

            if (entity.isPlayer) {
                if (entity.name.equals("Hero")) {
                    icon = playerDown1;
                } else if (entity.name.equals("Assassin")) {
                    // Βρες τον assassin στα partyMembers
                    for (PartyMember member : partyMembers) {
                        if (member.className.equals("Assassin")) {
                            icon = member.down1;
                            break;
                        }
                    }
                } else if (entity.name.equals("Mage")) {
                    for (PartyMember member : partyMembers) {
                        if (member.className.equals("Mage")) {
                            icon = member.down1;
                            break;
                        }
                    }
                }
            } else {
                // Για εχθρό, χρησιμοποίησε την εικόνα του εχθρού
                if (entity.enemyRef != null && entity.enemyRef.currentImage != null) {
                    icon = entity.enemyRef.currentImage;
                }
            }
            
            // Ζωγράφισε την εικόνα (ελαφρώς μικρότερη για να φαίνεται το πλαίσιο)
            if (icon != null) {
                g2.drawImage(icon, x + 4, y + 4, slotWidth - 8, slotHeight - 8, null);
            }
        }
    }

    public String getBattleOptionDescription(int option) {
        switch(option) {
            case 0: return "Attack with your equipped weapon";
            case 1: return "Use class-specific skills";
            case 2: return "Use an item from your inventory";
            case 3: return "Defend and reduce damage";
            case 4: return "Attempt to flee from battle";
            default: return "";
        }
    }
    
    public void drawDialogueScreen(Graphics2D g2) {
        int x = tileSize * 2;
        int y = tileSize * 5;
        int width = screenWidth - (tileSize * 4);
        int height = (gameState == dialogueOptionsState) ? tileSize * 7 : tileSize * 4;
        
        drawPanel(g2, x, y, width, height);
        
        g2.setFont(maruMonica);
        g2.setColor(Color.white);
        
        String[] lines = displayedDialogue.split("\n");
        int textY = y + 40;
        for (int i = 0; i < lines.length; i++) {
            g2.drawString(lines[i], x + 20, textY);
            textY += 30;
        }
        
        // Αν τελείωσε το γράψιμο, δείξε το μήνυμα για Enter
        if (!dialogueTyping && gameState == dialogueState) {
            g2.setFont(new Font("Arial", Font.ITALIC, 16));
            g2.drawString("Πάτα Enter για συνέχεια...", x + width - 220, y + height - 20);
        } else if (gameState == dialogueOptionsState) {
            int optionY = textY + 10;
            for (int i = 0; i < dialogueOptions.length; i++) {
                if (dialogueOptions[i] != null) {
                    if (i == selectedOption) {
                        g2.setColor(Color.yellow);
                        g2.fillRect(x + 15, optionY - 18, width - 30, 28);
                        g2.setColor(Color.black);
                    } else {
                        g2.setColor(Color.white);
                    }
                    g2.setFont(new Font("Arial", Font.PLAIN, 18));
                    g2.drawString(dialogueOptions[i], x + 25, optionY);
                    optionY += 30;
                }
            }
            // ========== ΠΡΟΣΘΕΣΕ ΤΟ ΜΗΝΥΜΑ Enter ΚΑΙ ΕΔΩ ==========
            if (!dialogueTyping) {
                g2.setFont(new Font("Arial", Font.ITALIC, 16));
                g2.drawString("Πάτα Enter για συνέχεια...", x + width - 220, y + height - 20);
            }
        } else {
            if (!dialogueTyping) {
            g2.setFont(new Font("Arial", Font.ITALIC, 16));
            g2.drawString("Πάτα Enter για συνέχεια...", x + width - 220, y + height - 20);
        }
        }
    }

    public void drawInventory(Graphics2D g2) {
        // Σκούρο φόντο
        g2.setColor(new Color(0, 0, 0, 220));
        g2.fillRect(0, 0, screenWidth, screenHeight);
        
        int slotSize = 60;
        int slotSpacingX = 7;  // Οριζόντιο spacing = 8
        int slotSpacingY = 20; // Κάθετο spacing = 15
        
        // ========== ΑΡΙΣΤΕΡΑ: STORAGE (4x2) ΜΕ ΕΙΚΟΝΑ ==========
        int storageX = 200;
        int storageY = 50;
        
        // Φόρτωσε και ζωγράφισε την εικόνα για storage
        try {
            BufferedImage storageBg = ImageIO.read(new File("res/gui/storage_bg.png"));
            g2.drawImage(storageBg, storageX - 20, storageY - 30, 300, 200, null);
        } catch (Exception e) {
            // Αν δεν υπάρχει εικόνα, ζωγράφισε απλό πλαίσιο
            g2.setColor(new Color(50, 50, 50, 200));
            g2.fillRoundRect(storageX - 10, storageY - 10, 4 * (slotSize + slotSpacingX) - slotSpacingX + 20, 
                            2 * (slotSize + slotSpacingY) - slotSpacingY + 20, 15, 15);
        }
        
        // Τίτλος
        g2.setColor(Color.white);
        g2.setFont(new Font("Arial", Font.BOLD, 18));
        g2.drawString("STORAGE", storageX + 20, storageY - 15);
        
        // Ζωγράφισε τα 8 slots (4x2)
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 4; col++) {
                int index = row * 4 + col;
                int slotX = storageX + col * (slotSize + slotSpacingX);
                int slotY = storageY + row * (slotSize + slotSpacingY);

                
                // Αν υπάρχει item
                if (index < inventory.storage.length && inventory.storage[index] != null) {
                    Item item = inventory.storage[index];
                    
                    // Ζωγράφισε εικόνα item
                    if (item.image != null) {
                        g2.drawImage(item.image, slotX + 5, slotY + 5, slotSize - 10, slotSize - 10, null);
                    }
                    
                    // Ποσότητα (αν stackable)
                    if (item.stackable && item.amount > 1) {
                        g2.setColor(Color.white);
                        g2.setFont(new Font("Arial", Font.BOLD, 10));
                        g2.drawString("x" + item.amount, slotX + 40, slotY + 50);
                    }
                }
                
                // ========== ΝΕΟ HIGHLIGHT: Κίτρινο περίγραμμα ==========
                if (inventory.inventoryMode == 0 && index == inventory.selectedStorageSlot) {
                    g2.setColor(Color.yellow);
                    g2.setStroke(new BasicStroke(4)); // Πιο χοντρό περίγραμμα
                    g2.drawRoundRect(slotX - 2, slotY - 2, slotSize + 2, slotSize +2, 10, 10);
                }

                // ========== TOOLTIP ΓΙΑ ΤΟ ΕΠΙΛΕΓΜΕΝΟ ITEM ==========
                Item hoveredItem = null;

                if (inventory.inventoryMode == 0) { // Storage mode
                    if (inventory.selectedStorageSlot >= 0 && 
                        inventory.selectedStorageSlot < inventory.storage.length) {
                        hoveredItem = inventory.storage[inventory.selectedStorageSlot];
                    }
                } else { // Equipment mode
                    hoveredItem = inventory.getEquipSlot(inventory.selectedEquipSlot);
                }

                if (hoveredItem != null) {
                    drawItemTooltip(g2, hoveredItem);
                }
            }
        }
        
        // ========== ΔΕΞΙΑ: EQUIPMENT (3x3) ΜΕ ΕΙΚΟΝΑ ==========
        int equipX = 500;
        int equipY = 50;
        int slotSizeEquip = 55;
        int slotSpacingEquipX = 17;  // Οριζόντιο spacing = 8
        int slotSpacingEquipY = 17; // Κάθετο spacing = 15
        
        // Φόρτωσε και ζωγράφισε την εικόνα για equipment
        try {
            BufferedImage equipBg = ImageIO.read(new File("res/gui/equipment_bg.png"));
            g2.drawImage(equipBg, equipX - 20, equipY - 30, 250, 250, null);
        } catch (Exception e) {
            // Αν δεν υπάρχει εικόνα, ζωγράφισε απλό πλαίσιο
            g2.setColor(new Color(50, 50, 50, 200));
            g2.fillRoundRect(equipX - 10, equipY - 10, 3 * (slotSizeEquip + slotSpacingEquipX) + 10, 
                            3 * (slotSizeEquip + slotSpacingEquipY) + 10, 15, 15);
        }
        
        // Τίτλος
        g2.setColor(Color.white);
        g2.setFont(new Font("Arial", Font.BOLD, 18));
        g2.drawString("EQUIPMENT", equipX + 20, equipY - 15);
        
        // Ζωγράφισε τα 9 slots (3x3)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int index = row * 3 + col;
                int slotX = equipX + col * (slotSizeEquip + slotSpacingEquipX) + 6; // +2 δεξιά
                int slotY = equipY + row * (slotSizeEquip + slotSpacingEquipY) - 3; // -2 πάνω
                
                // Αν υπάρχει item
                Item item = inventory.getEquipSlot(index);
                if (item != null && item.image != null) {
                    g2.drawImage(item.image, slotX + 5, slotY + 5, slotSizeEquip - 10, slotSizeEquip - 10, null);
                }
                
                // ========== ΝΕΟ HIGHLIGHT: Κίτρινο περίγραμμα ==========
                if (inventory.inventoryMode == 1 && index == inventory.selectedEquipSlot) {
                    g2.setColor(Color.yellow);
                    g2.setStroke(new BasicStroke(4));
                    g2.drawRoundRect(slotX - 2, slotY - 2, slotSizeEquip + 2, slotSizeEquip + 2, 10, 10);
                }
            }
        }

        // ========== ΚΕΝΤΡΟ: KEY ITEMS (4x2) ==========
        int keyX = 450;  // Ανάμεσα στα stats και equipment
        int keyY = 350;

        // Φόρτωσε και ζωγράφισε την εικόνα για key items
        try {
            BufferedImage keyBg = ImageIO.read(new File("res/gui/storage_bg.png"));
            g2.drawImage(keyBg, keyX - 20, keyY - 30, 300, 200, null);
        } catch (Exception e) {
            // Fallback
            g2.setColor(new Color(100, 50, 50, 200));
            g2.fillRoundRect(keyX - 10, keyY - 10, 4 * (slotSize + slotSpacingX) - slotSpacingX + 20, 
                            2 * (slotSize + slotSpacingY) - slotSpacingY + 20, 15, 15);
        }

        // Τίτλος
        g2.setColor(Color.white);
        g2.setFont(new Font("Arial", Font.BOLD, 18));
        g2.drawString("KEY ITEMS", keyX + 20, keyY - 15);

        // Ζωγράφισε τα 8 slots (4x2)
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 4; col++) {
                int index = row * 4 + col;
                int slotX = keyX + col * (slotSize + slotSpacingX);
                int slotY = keyY + row * (slotSize + slotSpacingY);
                
                // Αν υπάρχει item
                if (index < inventory.keyItems.length && inventory.keyItems[index] != null) {
                    Item item = inventory.keyItems[index];
                    if (item.image != null) {
                        g2.drawImage(item.image, slotX + 5, slotY + 5, slotSize - 10, slotSize - 10, null);
                    }
                }
                
                // Highlight
                if (inventory.inventoryMode == 2 && index == inventory.selectedKeyItemSlot) {
                    g2.setColor(Color.yellow);
                    g2.setStroke(new BasicStroke(4));
                    g2.drawRoundRect(slotX - 2, slotY - 2, slotSize + 2, slotSize +2, 10, 10);
                }
            }
        }
        
        // ========== ΗΡΩΑΣ ΜΕ ΚΑΡΔΙΕΣ ==========
        int heroX = 50;
        int heroY = 50;
        
        // Εικόνα ήρωα
        g2.drawImage(playerDown1, heroX, heroY, tileSize*2, tileSize*2, null);
        
        // 3 καρδιές για HP
        int heartX = heroX - 10;
        int heartY = heroY + 150;
        int heartSize = 25;
        
        try {
            BufferedImage fullHeart = ImageIO.read(new File("res/gui/heart_full.png"));
            BufferedImage emptyHeart = ImageIO.read(new File("res/gui/heart_empty.png"));
            BufferedImage halfHeart = ImageIO.read(new File("res/gui/heart_half.png"));
            
            // Υπολόγισε πόσες καρδιές (κάθε καρδιά = 10 HP)
            int totalHearts = 3;
            int hpPerHeart = player.maxHp / totalHearts;
            
            for (int i = 0; i < totalHearts; i++) {
                int heartHp = player.hp - (i * hpPerHeart);
                if (heartHp >= hpPerHeart) {
                    g2.drawImage(fullHeart, heartX + i * (heartSize + 5), heartY, heartSize, heartSize, null);
                } else if (heartHp > 0) {
                    g2.drawImage(halfHeart, heartX + i * (heartSize + 5), heartY, heartSize, heartSize, null);
                } else {
                    g2.drawImage(emptyHeart, heartX + i * (heartSize + 5), heartY, heartSize, heartSize, null);
                }
            }
        } catch (Exception e) {
            // Αν δεν υπάρχουν εικόνες, ζωγράφισε απλά κυκλάκια
            g2.setColor(Color.red);
            for (int i = 0; i < 3; i++) {
                g2.fillOval(heartX + i * 30, heartY, 20, 20);
            }
        }
        
        // 3 ασπίδες για DEFENCE (πάνω από τις καρδιές)
        int shieldX = heroX - 10;
        int shieldY = heartY - 40;
        
        try {
            BufferedImage shieldFull = ImageIO.read(new File("res/gui/shield_full.png"));
            BufferedImage shieldEmpty = ImageIO.read(new File("res/gui/shield_empty.png"));
            
            // Υπολόγισε πόσες ασπίδες (κάθε ασπίδα = 5 defense)
            int totalShields = 3;
            int defPerShield = 5;
            int playerDefense = player.defense;
            
            for (int i = 0; i < totalShields; i++) {
                if (playerDefense >= (i + 1) * defPerShield) {
                    g2.drawImage(shieldFull, shieldX + i * (heartSize + 5), shieldY, heartSize, heartSize, null);
                } else {
                    g2.drawImage(shieldEmpty, shieldX + i * (heartSize + 5), shieldY, heartSize, heartSize, null);
                }
            }
        } catch (Exception e) {
            // Αν δεν υπάρχουν εικόνες, ζωγράφισε απλά τετράγωνα
            g2.setColor(Color.blue);
            for (int i = 0; i < 3; i++) {
                g2.fillRect(shieldX + i * 30, shieldY, 20, 20);
            }
        }
        
        // ========== STATS ΜΕ ΧΡΩΜΑΤΑ ==========
        int statsX = heroX - 10;
        int statsY = heartY + 60;

        g2.setColor(new Color(50, 50, 50, 200));
        g2.fillRoundRect(statsX - 20, statsY - 20, 150, 250, 15, 15);
        g2.setColor(Color.white);
        g2.setStroke(new BasicStroke(2));
        g2.drawRoundRect(statsX - 20, statsY - 20, 150, 250, 15, 15);

        g2.setFont(new Font("Arial", Font.BOLD, 16));
        g2.drawString("STATS", statsX + 25, statsY);

        g2.setFont(new Font("Arial", Font.PLAIN, 14));
        int statLineY = statsY + 25;

        // Level (πάντα λευκό)
        g2.setColor(Color.white);
        g2.drawString("Level: " + player.level, statsX, statLineY);

        // Attack - Πράσινο αν αυξημένο
        int attackBonus = player.attack - player.baseAttack;
        if (attackBonus > 0) {
            g2.setColor(Color.green);
            g2.drawString("Attack: " + player.attack + " (+" + attackBonus + ")", statsX, statLineY + 20);
        } else {
            g2.setColor(Color.white);
            g2.drawString("Attack: " + player.attack, statsX, statLineY + 20);
        }

        // Defense - Πράσινο αν αυξημένο
        int defenseBonus = player.defense - player.baseDefense;
        if (defenseBonus > 0) {
            g2.setColor(Color.green);
            g2.drawString("Defense: " + player.defense + " (+" + defenseBonus + ")", statsX, statLineY + 40);
        } else {
            g2.setColor(Color.white);
            g2.drawString("Defense: " + player.defense, statsX, statLineY + 40);
        }

        // HP - Κόκκινο (πάντα)
        g2.setColor(Color.red);
        g2.drawString("HP: " + player.hp + "/" + player.maxHp, statsX, statLineY + 60);

        // MP - Μπλε (πάντα)
        g2.setColor(Color.cyan);
        g2.drawString("MP: " + player.mp + "/" + player.maxMp, statsX, statLineY + 80);

        // Magic Attack - Πράσινο αν αυξημένο
        int magicBonus = player.magicAttack - player.baseMagicAttack;
        if (magicBonus > 0) {
            g2.setColor(Color.green);
            g2.drawString("Magic: " + player.magicAttack + " (+" + magicBonus + ")", statsX, statLineY + 100);
        } else {
            g2.setColor(Color.white);
            g2.drawString("Magic: " + player.magicAttack, statsX, statLineY + 100);
        }

        // Speed - Πράσινο αν αυξημένο
        int speedBonus = player.speed_stat - player.baseSpeed;
        if (speedBonus > 0) {
            g2.setColor(Color.green);
            g2.drawString("Speed: " + player.speed_stat + " (+" + speedBonus + ")", statsX, statLineY + 120);
        } else {
            g2.setColor(Color.white);
            g2.drawString("Speed: " + player.speed_stat, statsX, statLineY + 120);
        }

        // EXP - Κίτρινο
        g2.setColor(Color.yellow);
        g2.drawString("EXP: " + player.exp + "/" + player.expToNextLevel, statsX, statLineY + 140);

        // Gold - Χρυσό
        g2.setColor(new Color(255, 215, 0)); // Gold color
        g2.drawString("Gold: " + player.gold, statsX, statLineY + 160);
        
        // ========== ΟΔΗΓΙΕΣ ==========
        g2.setColor(Color.gray);
        g2.setFont(new Font("Arial", Font.ITALIC, 12));
        g2.drawString("SHIFT: Switch between Storage/Equipment", 50, screenHeight - 30);
        g2.drawString("Arrows: Move | Enter: Use/Equip | I: Close", 50, screenHeight - 15);
    }

    public void drawItemTooltip(Graphics2D g2, Item item) {
        int tooltipX = 200;
        int tooltipY = 250;
        int tooltipWidth = 200;
        int tooltipHeight = 120;
        
        drawPanel(g2, tooltipX, tooltipY, tooltipWidth, tooltipHeight);
        
        // ονομα item
        g2.setFont(maruMonicaBold);
        g2.setColor(Color.yellow);
        g2.drawString(item.name, tooltipX + 15, tooltipY + 25);
        
        // Γραμμή διαχωρισμού
        g2.setColor(Color.gray);
        g2.drawLine(tooltipX + 10, tooltipY + 35, tooltipX + tooltipWidth - 10, tooltipY + 35);
        
        // Stats
        g2.setFont(new Font("Arial", Font.PLAIN, 14));
        int statsY = tooltipY + 55;
        int lineHeight = 20;
        
        if (item.healAmount > 0) {
            g2.setColor(Color.green);
            g2.drawString("Heals: " + item.healAmount + " HP", tooltipX + 15, statsY);
            statsY += lineHeight;
        }
        
        if (item.attackBonus > 0) {
            g2.setColor(Color.red);
            g2.drawString("Attack +" + item.attackBonus, tooltipX + 15, statsY);
            statsY += lineHeight;
        }
        
        if (item.defenseBonus > 0) {
            g2.setColor(Color.blue);
            g2.drawString("Defense +" + item.defenseBonus, tooltipX + 15, statsY);
            statsY += lineHeight;
        }
        
        if (item.magicBonus > 0) {
            g2.setColor(Color.cyan);
            g2.drawString("Magic +" + item.magicBonus, tooltipX + 15, statsY);
            statsY += lineHeight;
        }
        
        if (item.hpBonus > 0) {
            g2.setColor(Color.pink);
            g2.drawString("Max HP +" + item.hpBonus, tooltipX + 15, statsY);
            statsY += lineHeight;
        }
        
        if (item.mpBonus > 0) {
            g2.setColor(Color.magenta);
            g2.drawString("Max MP +" + item.mpBonus, tooltipX + 15, statsY);
            statsY += lineHeight;
        }
        
        if (item.speedBonus > 0) {
            g2.setColor(Color.orange);
            g2.drawString("Speed +" + item.speedBonus, tooltipX + 15, statsY);
            statsY += lineHeight;
        }
        
        // Αν δεν έχει stats
        if (item.healAmount == 0 && item.attackBonus == 0 && item.defenseBonus == 0 &&
            item.magicBonus == 0 && item.hpBonus == 0 && item.mpBonus == 0 && item.speedBonus == 0) {
            g2.setColor(Color.lightGray);
            g2.drawString("No special effects", tooltipX + 15, statsY);
        }
    }

    public void drawMapScreen(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 220));
        g2.fillRect(0, 0, screenWidth, screenHeight);
        
        // ========== ΧΑΡΤΗΣ ==========
        // Υπολόγισε το μέγεθος κάθε tile στο minimap
        int mapWidth = screenWidth - 50;
        int mapHeight = screenHeight - 50;
        
        int tileSizeMap = Math.min(mapWidth / maxWorldCol, mapHeight / maxWorldRow);
        tileSizeMap = Math.max(tileSizeMap, 4); // Μίνιμουμ 4 pixels
        
        // Κεντράρισμα του χάρτη
        int totalMapWidth = maxWorldCol * tileSizeMap;
        int totalMapHeight = maxWorldRow * tileSizeMap;
        int mapX = (screenWidth - totalMapWidth) / 2;
        int mapY = (screenHeight - totalMapHeight - 50) / 2;
        
        // Ζωγράφισε τον κόσμο (tiles) - ΧΡΗΣΙΜΟΠΟΙΩΝΤΑΣ ΤΙΣ ΠΡΑΓΜΑΤΙΚΕΣ ΕΙΚΟΝΕΣ
        for (int row = 0; row < maxWorldRow; row++) {
            for (int col = 0; col < maxWorldCol; col++) {
                int tileNum = tileM.getTileNum(currentMap, row, col);
                
                // Πάρε την εικόνα του tile από τον TileManager
                BufferedImage tileImage = tileM.tile[tileNum].image;
                
                if (tileImage != null) {
                    int x = mapX + col * tileSizeMap;
                    int y = mapY + row * tileSizeMap;
                    
                    // Ζωγράφισε το tile με scale στο μέγεθος του minimap
                    g2.drawImage(tileImage, x, y, tileSizeMap, tileSizeMap, null);
                } else {
                    // Fallback αν δεν υπάρχει εικόνα
                    g2.setColor(Color.darkGray);
                    g2.fillRect(mapX + col * tileSizeMap, mapY + row * tileSizeMap, tileSizeMap, tileSizeMap);
                }
            }
        }
        
        // ========== ΠΑΙΚΤΗΣ (με εικόνα) ==========
        int playerCol = player.worldX / tileSize;
        int playerRow = player.worldY / tileSize;
        int playerMapX = mapX + playerCol * tileSizeMap;
        int playerMapY = mapY + playerRow * tileSizeMap;
        
        // Ζωγράφισε την εικόνα του παίκτη
        if (playerDown1 != null) {
            g2.drawImage(playerDown1, playerMapX, playerMapY, tileSizeMap, tileSizeMap, null);
        } else {
            // Fallback
            g2.setColor(Color.red);
            g2.fillOval(playerMapX, playerMapY, tileSizeMap, tileSizeMap);
        }

        // ========== NPCs (προαιρετικά) ==========
        ArrayList<Entity> currentMapNPCs = npcs.get(currentMap);
        for (Entity npc : currentMapNPCs) {
            int npcCol = npc.worldX / tileSize;
            int npcRow = npc.worldY / tileSize;
            int npcMapX = mapX + npcCol * tileSizeMap;
            int npcMapY = mapY + npcRow * tileSizeMap;
            

            if (npc.currentImage != null) {
                g2.drawImage(npc.down1, npcMapX, npcMapY, tileSizeMap, tileSizeMap, null);
            } else {
                // Fallback
                g2.setColor(Color.cyan);
                g2.fillRect(npcMapX, npcMapY, tileSizeMap, tileSizeMap);
            }
        }
        
        // ========== ΣΥΝΤΕΤΑΓΜΕΝΕΣ ==========
        int coordX = 10;
        int coordY = screenHeight - 80;
        
        g2.setFont(maruMonicaBold);
        g2.setColor(Color.yellow);
        g2.drawString("Συντεταγμένες:", coordX, coordY);
        
        g2.setFont(maruMonica);
        g2.setColor(Color.white);
        g2.drawString("Tile: (" + playerCol + ", " + playerRow + ")", 
                    coordX + 20, coordY + 25);
        g2.drawString("Pixel: (" + player.worldX + ", " + player.worldY + ")", 
                    coordX + 20, coordY + 45);
        
        // ========== ΠΛΗΡΟΦΟΡΙΕΣ ΧΑΡΤΗ ==========
        g2.setFont(maruMonicaSmall);
        g2.drawString("World: " + (currentMap == 0 ? "Overworld" : "Dungeon"), 
                    coordX, coordY + 70);
        
        // ========== ΟΔΗΓΙΕΣ ==========
        g2.setColor(Color.gray);
        g2.setFont(maruMonicaSmall);
        String exit = "Πάτα Μ για να κλείσεις";
        int x = getXforCenteredText(exit, g2);
        g2.drawString(exit, x, screenHeight - 30);
    }

    public void drawPauseScreen(Graphics2D g2) {
        // Ημιδιάφανο μαύρο φόντο
        g2.setColor(new Color(0, 0, 0, 200));
        g2.fillRect(0, 0, screenWidth, screenHeight);
        
        // Πλαίσιο μενού (ΧΡΗΣΙΜΟΠΟΙΩΝΤΑΣ ΤΗ ΝΕΑ ΜΕΘΟΔΟ)
        int frameWidth = 500;
        int frameHeight = 450;
        int frameX = screenWidth/2 - frameWidth/2;
        int frameY = screenHeight/2 - frameHeight/2;
        
        drawPanel(g2, frameX, frameY, frameWidth, frameHeight);
        
        // Τίτλος με custom font
        g2.setFont(maruMonicaLarge);
        g2.setColor(Color.yellow);
        String title = "PAUSE MENU";
        int x = getXforCenteredText(title, g2);
        g2.drawString(title, x, frameY + 60);
        
        if (volumeSettingMode == 0) {
            // Κανονικές επιλογές μενού
            g2.setFont(maruMonicaBold);
            int startY = frameY + 130;
            int spacing = 45;
            
            for (int i = 0; i < pauseOptions.length; i++) {
                String option = pauseOptions[i];
                x = frameX + 80; // Σταθερή θέση για τις επιλογές
                
                if (i == 0) { // FULLSCREEN - με checkbox
                    if (isFullScreen) {
                        g2.drawString("[✓]", x + 120, startY + i * spacing);
                    } else {
                        g2.drawString("[ ]", x + 120, startY + i * spacing);
                    }
                } else if (i == 1 || i == 2) { // MUSIC/SOUND - με ποσοστό
                    int volume = (i == 1) ? musicVolume : soundVolume;
                    option += ": " + volume + "%";
                }
                
                if (i == pauseCommandNum) {
                    g2.setColor(Color.yellow);
                    g2.drawString(">", x - 50, startY + i * spacing);
                    g2.drawString(option, x, startY + i * spacing);
                } else {
                    g2.setColor(Color.white);
                    g2.drawString(option, x, startY + i * spacing);
                }
            }
        } else {
            // Volume controls
            g2.setFont(new Font("Arial", Font.BOLD, 32));
            g2.setColor(Color.cyan);
            String volumeType = (volumeSettingMode == 1) ? "MUSIC VOLUME" : "SOUND VOLUME";
            x = getXforCenteredText(volumeType, g2);
            g2.drawString(volumeType, x, frameY + 150);
            
            // Volume bar
            int barWidth = 300;
            int barHeight = 30;
            int barX = screenWidth/2 - barWidth/2;
            int barY = frameY + 220;
            
            // Background bar
            g2.setColor(Color.darkGray);
            g2.fillRect(barX, barY, barWidth, barHeight);
            
            // Volume level
            g2.setColor(Color.green);
            int volume = (volumeSettingMode == 1) ? musicVolume : soundVolume;
            int fillWidth = (int)(barWidth * (volume / 100.0));
            g2.fillRect(barX, barY, fillWidth, barHeight);
            
            // Border
            g2.setColor(Color.white);
            g2.setStroke(new BasicStroke(2));
            g2.drawRect(barX, barY, barWidth, barHeight);
            
            // Percentage text
            g2.setFont(new Font("Arial", Font.BOLD, 24));
            g2.setColor(Color.white);
            String percentText = volume + "%";
            x = getXforCenteredText(percentText, g2);
            g2.drawString(percentText, x, barY + 50);
            
            // Instructions
            g2.setFont(new Font("Arial", Font.ITALIC, 18));
            g2.setColor(Color.gray);
            String instr = "←/→ to adjust, Enter/Esc to confirm";
            x = getXforCenteredText(instr, g2);
            g2.drawString(instr, x, frameY + 350);
        }
        
        // Οδηγία στο κάτω μέρος
        g2.setFont(maruMonicaSmall);
        g2.setColor(Color.gray);
        String exit = "Press ESC to return to game";
        x = getXforCenteredText(exit, g2);
        g2.drawString(exit, x, frameY + frameHeight - 30);
    }

    public void drawQuitConfirmScreen(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 200));
        g2.fillRect(0, 0, screenWidth, screenHeight);
        
        int frameWidth = 400;
        int frameHeight = 250;
        int frameX = screenWidth/2 - frameWidth/2;
        int frameY = screenHeight/2 - frameHeight/2;
        
        drawPanel(g2, frameX, frameY, frameWidth, frameHeight);
        
        g2.setFont(maruMonicaBold);
        g2.setColor(Color.yellow);
        String msg = "QUIT GAME?";
        int x = getXforCenteredText(msg, g2);
        g2.drawString(msg, x, frameY + 80);
        
        g2.setFont(maruMonica);
        g2.setColor(Color.white);
        String msg2 = "Are you sure you want to quit?";
        x = getXforCenteredText(msg2, g2);
        g2.drawString(msg2, x, frameY + 130);
        
        String[] options = {"YES", "NO"};
        int startX = frameX + 100;
        int optionY = frameY + 190;
        
        for (int i = 0; i < options.length; i++) {
            if (i == quitConfirmOption) {
                g2.setColor(Color.yellow);
                g2.drawString("> " + options[i], startX + i * 120, optionY);
            } else {
                g2.setColor(Color.white);
                g2.drawString(options[i], startX + i * 120 + 15, optionY);
            }
        }
    }

    public void drawShopScreen(Graphics2D g2) {
        int margin = tileSize; // Μόνο 48 pixels περιθώριο από κάθε πλευρά
        int frameX = margin;
        int frameY = margin;
        int frameWidth = screenWidth - (margin * 2);
        int frameHeight = screenHeight - (margin * 2);
        
        drawPanel(g2, frameX, frameY, frameWidth, frameHeight);
        
        // Τίτλος
        g2.setFont(maruMonicaLarge);
        g2.setColor(Color.yellow);
        String title = "MERCHANT SHOP";
        int x = getXforCenteredText(title, g2);
        g2.drawString(title, x, frameY + 60);
        
        // Χρυσά
        g2.setFont(maruMonicaBold);
        g2.setColor(new Color(255, 215, 0));
        String goldText = "Gold: " + player.gold;
        g2.drawString(goldText, frameX + frameWidth - 150, frameY + 40);
        
        // Πίνακας επιλογών (Buy/Sell/Exit) - VERTICAL now
        String[] shopOptions = {"BUY", "SELL", "EXIT"};
        int optionX = frameX + 50;
        int startY = frameY + 120;
        int spacing = 40;
        
        for (int i = 0; i < shopOptions.length; i++) {
            g2.setFont(maruMonicaBold);
            if (i == shopOption) {
                g2.setColor(Color.yellow);
                g2.drawString("> " + shopOptions[i], optionX, startY + i * spacing);
            } else {
                g2.setColor(Color.white);
                g2.drawString(shopOptions[i], optionX + 15, startY + i * spacing);
            }
        }
        
        // Υπόλοιπο περιεχόμενο ανάλογα με τη λειτουργία
        if (shopOption != 2) {
            if (shopMode == 0) { // BUY mode
                drawBuyScreen(g2, frameX, frameY, frameWidth, frameHeight);
            } else if (shopMode == 1) { // SELL mode
                drawSellScreen(g2, frameX, frameY, frameWidth, frameHeight);
            }
        } else {
            // Μήνυμα επιβεβαίωσης εξόδου (προαιρετικά)
            g2.setFont(maruMonica);
            g2.setColor(Color.white);
            String exitMsg = "Πάτα ENTER για έξοδο!";
            int exitX = getXforCenteredText(exitMsg, g2);
            g2.drawString(exitMsg, exitX, frameY + frameHeight/2);
        }
    }

    public void drawBuyScreen(Graphics2D g2, int frameX, int frameY, int frameWidth, int frameHeight) {
        int slotSize = 60;
        int startX = frameX + 150; // Μετακίνησε δεξιά για να αφήσεις χώρο για το μενού
        int startY = frameY + 150;
        int spacingX = 70;
        int spacingY = 80;
        
        for (int i = 0; i < merchantItems.size() && i < 8; i++) {
            int row = i / 4;
            int col = i % 4;
            int x = startX + col * spacingX;
            int y = startY + row * spacingY;
            
            Item item = merchantItems.get(i);
            
            // Slot
            g2.setColor(new Color(50, 50, 50, 200));
            g2.fillRoundRect(x, y, slotSize, slotSize, 10, 10);
            g2.setColor(Color.white);
            g2.setStroke(new BasicStroke(2));
            g2.drawRoundRect(x, y, slotSize, slotSize, 10, 10);
            
            // Item image
            if (item.image != null) {
                g2.drawImage(item.image, x + 5, y + 5, slotSize - 10, slotSize - 10, null);
            }
            
            // Τιμή
            g2.setFont(maruMonicaSmall);
            g2.setColor(new Color(255, 215, 0));
            g2.drawString(item.price + "G", x + 5, y + slotSize + 15);
            
            // Highlight επιλεγμένου
            if (i == selectedShopItem && shopOption == 0) {
                g2.setColor(Color.yellow);
                g2.setStroke(new BasicStroke(4));
                g2.drawRoundRect(x - 2, y - 2, slotSize + 4, slotSize + 4, 12, 12);
            }
        }
        
        // Item details
        if (selectedShopItem >= 0 && selectedShopItem < merchantItems.size() && shopOption == 0) {
            Item selected = merchantItems.get(selectedShopItem);
            drawItemTooltip(g2, selected, frameX + frameWidth - 200, frameY + 150);
        }
        
        // Instructions
        g2.setFont(maruMonicaSmall);
        g2.setColor(Color.gray);
        g2.drawString("↑/↓: Menu | ←/→: Items | Enter: Buy | ESC: Back", 
                    frameX + 50, frameY + frameHeight - 30);
    }

    public void drawSellScreen(Graphics2D g2, int frameX, int frameY, int frameWidth, int frameHeight) {
        int slotSize = 60;
        int startX = frameX + 150; // Μετακίνησε δεξιά για να αφήσεις χώρο για το μενού
        int startY = frameY + 150;
        int spacingX = 70;
        int spacingY = 80;
        
        // Φιλτράρισμα items που μπορούν να πουληθούν (όχι key items)
        ArrayList<Item> sellableItems = new ArrayList<>();
        for (int i = 0; i < inventory.storage.length; i++) {
            if (inventory.storage[i] != null && !inventory.storage[i].isKeyItem) {
                sellableItems.add(inventory.storage[i]);
            }
        }
        
        for (int i = 0; i < sellableItems.size() && i < 8; i++) {
            int row = i / 4;
            int col = i % 4;
            int x = startX + col * spacingX;
            int y = startY + row * spacingY;
            
            Item item = sellableItems.get(i);
            
            // Slot
            g2.setColor(new Color(50, 50, 50, 200));
            g2.fillRoundRect(x, y, slotSize, slotSize, 10, 10);
            g2.setColor(Color.white);
            g2.setStroke(new BasicStroke(2));
            g2.drawRoundRect(x, y, slotSize, slotSize, 10, 10);
            
            // Item image
            if (item.image != null) {
                g2.drawImage(item.image, x + 5, y + 5, slotSize - 10, slotSize - 10, null);
            }
            
            // Τιμή πώλησης (μισή τιμή)
            int sellPrice = item.price / 2;
            g2.setFont(maruMonicaSmall);
            g2.setColor(new Color(255, 215, 0));
            g2.drawString(sellPrice + "G", x + 5, y + slotSize + 15);
            
            // Ποσότητα αν stackable
            if (item.stackable && item.amount > 1) {
                g2.setColor(Color.white);
                g2.setFont(maruMonicaSmall);
                g2.drawString("x" + item.amount, x + slotSize - 20, y + slotSize - 5);
            }
            
            // Highlight επιλεγμένου
            if (i == selectedSellItem && shopOption == 1) {
                g2.setColor(Color.yellow);
                g2.setStroke(new BasicStroke(4));
                g2.drawRoundRect(x - 2, y - 2, slotSize + 4, slotSize + 4, 12, 12);
            }
        }
        
        if (selectedSellItem >= 0 && selectedSellItem < sellableItems.size() && shopOption == 1) {
            Item selected = sellableItems.get(selectedSellItem);
            drawItemTooltip(g2, selected, frameX + frameWidth - 200, frameY + 150);
        }
        
        g2.setFont(maruMonicaSmall);
        g2.setColor(Color.gray);
        g2.drawString("↑/↓: Menu | ←/→: Items | Enter: Sell | ESC: Back", 
                    frameX + 50, frameY + frameHeight - 30);
    }

    public void drawItemTooltip(Graphics2D g2, Item item, int x, int y) {
        int width = 200;
        int height = 150;
        
        drawPanel(g2, x, y, width, height);
        
        g2.setFont(maruMonicaBold);
        g2.setColor(Color.yellow);
        g2.drawString(item.name, x + 15, y + 25);
        
        g2.setFont(maruMonicaSmall);
        g2.setColor(Color.white);
        int textY = y + 50;
        
        if (item.healAmount > 0) {
            g2.drawString("Heals: " + item.healAmount + " HP", x + 15, textY);
            textY += 20;
        }
        if (item.attackBonus > 0) {
            g2.drawString("Attack +" + item.attackBonus, x + 15, textY);
            textY += 20;
        }
        if (item.defenseBonus > 0) {
            g2.drawString("Defense +" + item.defenseBonus, x + 15, textY);
            textY += 20;
        }
        if (item.mpBonus > 0) {
            g2.drawString("Mana +" + item.mpBonus, x + 15, textY);
            textY += 20;
        }
        
        g2.setColor(new Color(255, 215, 0));
        g2.drawString("Price: " + item.price + "G", x + 15, y + height - 20);
    }

    public void drawLighting(Graphics2D g2) {
        // ΑΝ ΕΙΜΑΣΤΕ ΣΕ INTERIOR (χάρτες 4,5) ΜΗΝ ΕΦΑΡΜΟΣΕΙΣ ΣΚΟΤΑΔΙ
        if (currentMap == 2 || currentMap == 4 || currentMap == 5) {
            return; // Βγες αμέσως, κανένα εφέ
        }
        // Αν είναι μέρα και δεν έχει αρχίσει ακόμα να σκοτεινιάζει
        if (currentMap == 1) {
            currentDarkness = 150.0f; // Μόνιμο σκοτάδι
            // Το φανάρι λειτουργεί κανονικά
        } else {
            // Στον overworld, κανονικό σύστημα μέρας/νύχτας
            if (currentDarkness <= 0.5f) return;
        }
        
        // Δημιούργησε ένα προσωρινό BufferedImage για το εφέ φωτισμού
        BufferedImage lightBuffer = new BufferedImage(screenWidth, screenHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D lightG2 = (Graphics2D) lightBuffer.getGraphics();
        
        // Σχεδίασε μαύρο φόντο με την τρέχουσα διαφάνεια
        lightG2.setColor(new Color(0, 0, 0, (int)currentDarkness));
        lightG2.fillRect(0, 0, screenWidth, screenHeight);
        
        // Αν έχει φανάρι, δημιούργησε φωτεινή περιοχή γύρω από τον παίκτη
        if (hasLantern && currentDarkness > 30) {
            int playerScreenX = player.worldX - worldX;
            int playerScreenY = player.worldY - worldY;
            int centerX = playerScreenX + tileSize/2;
            int centerY = playerScreenY + tileSize/2;
            
            // Η ακτίνα εξαρτάται από το πόσο σκοτάδι έχει
            int radius = lanternRadius * tileSize;
            if (currentDarkness > 100) {
                radius = (int)(radius * 1.3f); // Πιο δυνατό φως σε πλήρες σκοτάδι
            }
            
            // Απαλό gradient - το κέντρο είναι εντελώς διάφανο
            float[] dist = {0.0f, 0.3f, 0.7f, 1.0f};
            Color[] colors = {
                new Color(255, 255, 255, 0),      // Εντελώς διάφανο στο κέντρο
                new Color(0, 0, 0, 20),            // Ελάχιστο σκοτάδι
                new Color(0, 0, 0, (int)(currentDarkness * 0.5f)), // Μέτριο σκοτάδι
                new Color(0, 0, 0, (int)currentDarkness) // Κανονικό σκοτάδι στην άκρη
            };
            
            RadialGradientPaint gradient = new RadialGradientPaint(
                centerX, centerY, radius,
                dist, colors
            );
            
            lightG2.setPaint(gradient);
            lightG2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
            lightG2.fillRect(0, 0, screenWidth, screenHeight);
        }
        
        lightG2.dispose();
        
        // Σχεδίασε το εφέ πάνω στο παιχνίδι
        g2.drawImage(lightBuffer, 0, 0, null);
    }

    class KeyHandler implements KeyListener {
        public boolean upPressed, downPressed, leftPressed, rightPressed;
        public boolean enterPressed = false; // Νέο πλήκτρο για αλληλεπίδραση (π.χ. με NPC)
        public boolean iPressed = false; // Για inventory
        public boolean ePressed = false; // Για items
        public boolean qPressed = false;
        public boolean shiftPressed = false;
        public boolean mPressed = false;
        public boolean escapePressed = false;
        public boolean bPressed = false;
        public boolean cPressed = false;

        @Override
        public void keyTyped(KeyEvent e) {}

        @Override
        public void keyPressed(KeyEvent e) {
            int code = e.getKeyCode();
            
            if (code == KeyEvent.VK_ESCAPE) {
                escapePressed = true;
            }
            if (code == KeyEvent.VK_UP) {
                upPressed = true;
            }
            if (code == KeyEvent.VK_DOWN) {
                downPressed = true;
            }
            if (code == KeyEvent.VK_LEFT) {
                leftPressed = true;
            }
            if (code == KeyEvent.VK_RIGHT) {
                rightPressed = true;
            }
            if (code == KeyEvent.VK_ENTER) {
                enterPressed = true;
            }
            if (code == KeyEvent.VK_I) {
                iPressed = true;
            }
            if (code == KeyEvent.VK_E) {
                ePressed = true;
            }
            if (code == KeyEvent.VK_Q) {
                qPressed = true;
            }
            if (code == KeyEvent.VK_SHIFT) {
                shiftPressed = true;
            }
            if (code == KeyEvent.VK_M) {
                mPressed = true;
            }
            if (code == KeyEvent.VK_B) {
                bPressed = true;
            }
            if (code == KeyEvent.VK_C) {
                cPressed = true;
            }   
        }

        @Override
        public void keyReleased(KeyEvent e) {
            int code = e.getKeyCode();
            
            if (code == KeyEvent.VK_ESCAPE) {
                escapePressed = false;
            }
            if (code == KeyEvent.VK_UP) {
                upPressed = false;
            }
            if (code == KeyEvent.VK_DOWN) {
                downPressed = false;
            }
            if (code == KeyEvent.VK_LEFT) {
                leftPressed = false;
            }
            if (code == KeyEvent.VK_RIGHT) {
                rightPressed = false;
            }
            if (code == KeyEvent.VK_ENTER) {
                enterPressed = false;
            }
            if (code == KeyEvent.VK_I) {
                iPressed = false;
            } 
            if (code == KeyEvent.VK_E) {
                ePressed = false;
            }
            if (code == KeyEvent.VK_Q) {
                qPressed = false;
            }
            if (code == KeyEvent.VK_SHIFT) {
                shiftPressed = false;
            }
            if (code == KeyEvent.VK_M) {
                mPressed = false;
            }
            if (code == KeyEvent.VK_B) {
                bPressed = false;
            }
            if (code == KeyEvent.VK_C) {
                cPressed = false;
            }
        }
    }
}