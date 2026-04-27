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
import java.awt.Toolkit;
import java.awt.RadialGradientPaint;
import java.awt.AlphaComposite;              
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.FontMetrics;
import java.util.HashMap;
import java.util.Map;

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
    public ArrayList<PartyMember> allPartyMembers = new ArrayList<>();
    public ArrayList<PartyMember> activePartyMembers = new ArrayList<>();
    public ArrayList<BattlePartyMember> battlePartyMembers = new ArrayList<>();
    ArrayList<Point> playerPositions = new ArrayList<>();

    // Inventory
    public Inventory inventory = new Inventory();
    public boolean showInventory = false; // Να εμφανίζεται το inventory
    public final int inventoryState = 2; // Νέο game state
    // ===== OCTOPATH-STYLE MAIN MENU =====
    public String[] mainMenuOptions = {
        "Items",
        "Status",
        "Equipment",
        "World Map",
        "Journal",
        "Save",
        "Options"
    };
    public String[] itemCategoryOptions = {
        "ALL",
        "POTIONS",
        "WEAPONS",
        "SHIELDS",
        "HELMETS",
        "BODY ARMOR",
        "NECKLACES",
        "CONSUMABLES",
        "SCROLLS"
    };
    // ===== MENU PORTRAITS =====
    public BufferedImage heroPortrait32;
    public BufferedImage assassinPortrait32;
    public BufferedImage magePortrait32;
    public BufferedImage equipmentGridBg;
    public BufferedImage menuPointer;
    public BufferedImage itemCatAllIcon;
    public BufferedImage itemCatPotionsIcon;
    public BufferedImage itemCatWeaponsIcon;
    public BufferedImage itemCatShieldsIcon;
    public BufferedImage itemCatHelmetsIcon;
    public BufferedImage itemCatBodyArmorIcon;
    public BufferedImage itemCatNecklacesIcon;
    public BufferedImage itemCatConsumablesIcon;
    public BufferedImage itemCatScrollsIcon;
    public BufferedImage worldMapBackground;
    public BufferedImage worldMapTownIcon;
    public BufferedImage worldMapPathIcon;
    public BufferedImage worldMapForestIcon;
    public BufferedImage worldMapCaveIcon;
    //public ArrayList<ItemOnGround> itemsOnGround = new ArrayList<>();
    public ArrayList<ArrayList<ItemOnGround>> itemsOnGround = new ArrayList<>();
    public String itemTooltip = "";
    public int tooltipTimer = 0;
    // ===== CHEST LOOT WINDOW =====
    public ArrayList<Item> chestLootItems = new ArrayList<>();
    public ArrayList<Integer> chestLootAmounts = new ArrayList<>();
    public int chestLootSelectedIndex = 0;
    // ===== WORLD MAP REGIONS =====
    public static class WorldMapRegion {
        public String id;
        public String displayName;
        public int x;
        public int y;
        public int width;
        public int height;
        public String type; // town, field, cave, forest, path, wild
        public boolean travelEnabled;
        public String targetMapName;

        public WorldMapRegion(String id, String displayName, int x, int y, int width, int height,
                            String type, boolean travelEnabled, String targetMapName) {
            this.id = id;
            this.displayName = displayName;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.type = type;
            this.travelEnabled = travelEnabled;
            this.targetMapName = targetMapName;
        }

        public boolean contains(int px, int py) {
            return px >= x && px <= x + width && py >= y && py <= y + height;
        }
    }
    public ArrayList<WorldMapRegion> worldMapRegions = new ArrayList<>();
    public int mapCursorX = 250;
    public int mapCursorY = 220;
    public String hoveredMapRegionName = "";
    // ==========================

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
    // NEW CHESTS
    public ArrayList<ArrayList<Chest>> chests = new ArrayList<>();
    public java.util.HashSet<String> openedChestIds = new java.util.HashSet<>();


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
    public final int chestLootState = 13;
    public final int cutsceneState = 14;

    public StoryManager storyManager = new StoryManager();
    public CutscenePlayer cutscenePlayer = new CutscenePlayer(this);

    // επιλογές διαλόγου
    public String[] dialogueOptions = new String[3];
    public int selectedOption = 0;
    // Διάλογος
    public String currentDialogue = "";
    // ===== OBJECTIVE POPUP =====
    public boolean showObjectivePopup = false;
    public String objectivePopupTitle = "";
    public String objectivePopupDescription = "";
    public int objectivePopupTimer = 0;
    public final int OBJECTIVE_POPUP_DURATION = 240; // 4 sec at 60fps

    // ========== ΜΑΧΗ ==========
    /*
    =====================================================
    BATTLE FLOW (CURRENT ACTIVE PATH)
    1. Player steps inside Tiled encounter zone
    2. getCurrentEncounterZone()
    3. buildEncounterFromZone(...)
    4. startRandomEncounter()
    5. startBattleWithTransition(ArrayList<Enemy>)
    6. pendingEncounterEnemies becomes the source for the next battle setup
    7. Existing battle render / turn / BP flow continues normally

    LEGACY / FALLBACK ONLY
    - startBattleWithTransition(Enemy enemy)
    - old hardcoded area-based enemy spawning
    - currentEnemy as full source of truth for all battle enemies

    NOTES
    - currentArea is now theme/background helper only
    - enemy composition now comes from Tiled encounter zone properties
    =====================================================
    */
    // LEGACY FIELD - πιθανό leftover από παλιότερο map/enemy storage.
    // Δεν είναι το active source των battle encounters στο Tiled flow.
    public ArrayList<ArrayList<Enemy>> enemies = new ArrayList<>(); 
    public final int battleState = 4;
    public final int battleVictoryState = 12;

    // FALLBACK / convenience pointer μόνο.
    // Στο Tiled battle flow, η πραγματική enemy ομάδα έρχεται από pendingEncounterEnemies.
    public Enemy currentEnemy;

    // Random Encounters
    public int encounterStepCounter = 0;
    public int encounterRate = 20; // Κάθε 20 βήματα
    // Theme / background helper only.
    // Δεν χρησιμοποιείται πλέον για hardcoded enemy selection στο active Tiled encounter flow.
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
    // ===== BOOST AURA SPRITE ANIMATION =====
    public BufferedImage[] boostAuraLv1FrontFrames;
    public BufferedImage[] boostAuraLv1BackFrames;
    public BufferedImage[] boostAuraLv2FrontFrames;
    public BufferedImage[] boostAuraLv2BackFrames;
    public BufferedImage[] boostAuraLv3FrontFrames;
    public BufferedImage[] boostAuraLv3BackFrames;

    public int boostAuraFrameDelay = 3; // πόσα game frames κρατά κάθε sprite frame
    public float boostAuraScaleLv1 = 1.4f;
    public float boostAuraScaleLv2 = 1.55f;
    public float boostAuraScaleLv3 = 1.7f;
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
    public ArrayList<BattleEffectInstance> activeBattleEffects = new ArrayList<>();
    // ===== BATTLE EFFECT SPRITE REGISTRY =====
    public Map<String, BufferedImage[]> battleEffectFrames = new HashMap<>();
    public Map<String, Integer> battleEffectFrameDelays = new HashMap<>();
    public Map<String, Float> battleEffectScales = new HashMap<>();
    public double boostSlashAngle = -0.65; // βασική διαγώνια φορά

    // === BREAK VARIABLES ====
    private BufferedImage swordIcon;
    private BufferedImage spearIcon;
    private BufferedImage staffIcon;
    private BufferedImage shieldIcon;
    // SLOW MOTION EFFECT
    public float battleTimeScale = 1.0f;
    public int slowMotionTimer = 0;
    public final int MULTI_HIT_DELAY = 16; // frames ανάμεσα στα hits

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
    public boolean battleFleeing = false;
    public final double BATTLE_FLEE_SPEED = 22.0;
    public boolean waitingForNextTurn = false;
    public int battlePhase = 0; // 0=player turn start, 1=player attack, 2=player hurt, 3=enemy turn start, 4=enemy attack, 5=enemy hurt, 6=waiting
    public int battlePhaseTimer = 0;
    public final int BATTLE_PHASE_DELAY = 180; // 3 δευτερόλεπτα στα 60fps (180 = 3sec)
    public BattleEntity currentAttacker = null;
    public BattleEntity currentTarget = null;
    public BattleEnemy currentBattleEnemy = null;
    public int pendingDamage = 0;
    public PlayerAnimation playerBattleAnim;
    public ArrayList<Enemy> pendingEncounterEnemies = new ArrayList<>();
    public ArrayList<BattleEntity> nextTurnPreview = new ArrayList<>();
    public int revealedNextTurnCount = 0;
    public final int INITIAL_NEXT_TURN_REVEAL = 3;
    // NEW FRONT MOVEMENT ANIMATIONS
    public final int BATTLE_PLAYER_REST_X = 600;
    public final int BATTLE_PLAYER_REST_Y_OFFSET = 100;
    public final int BATTLE_FRONT_ACTOR_X = 430;
    public final int BATTLE_FRONT_ACTOR_Y_OFFSET = 50;
    public final double BATTLE_FORMATION_LERP = 0.18;

    public String activeStoryBattleId = "";
    public String forcedBattleMusic = "";
    public BufferedImage groundRuins;

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

        if (this.window != null) {
            this.window.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    System.out.println("Window closed");
                }
            });
        }

        tileM = new TileManager(this); // Δώσε του access στο GamePanel  
        
        // npc
        for (int i = 0; i < maxMaps; i++) {
            npcs.add(new ArrayList<>());
            itemsOnGround.add(new ArrayList<>());
            chests.add(new ArrayList<>());
        }

        // Δημιούργησε τον player ως Entity
        player = new Entity(this);
        // player.worldX = 23 * tileSize;  // ή όποιο spawn point θες
        // player.worldY = 21 * tileSize;

        // LOAD OPENED CHESTS
        loadOpenedChests();

        currentMap = 0;
        tileM.applyMapSizeToGamePanel(currentMap);

        boolean loadedSave = loadPlayerState();
        loadInventoryAndGold();
        loadQuests();
        storyManager.load();

        if (!loadedSave) {
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
        }
        player.speed = 4;
        player.direction = "down";

        // ========== Δημιουργία των άλλων μελών της ομάδας ==========
        PartyMember assassin = new PartyMember(this, "assassin", "Assassin");
        assassin.worldX = player.worldX - tileSize;
        assassin.worldY = player.worldY;
        assassin.joinedParty = storyManager.hasFlag(StoryFlag.ASSASSIN_JOINED);

        PartyMember mage = new PartyMember(this, "mage", "Mage");
        mage.worldX = player.worldX - tileSize * 2;
        mage.worldY = player.worldY;
        mage.joinedParty = storyManager.hasFlag(StoryFlag.MAGE_JOINED);

        allPartyMembers.add(assassin);
        allPartyMembers.add(mage);

        if (assassin.joinedParty) {
            activePartyMembers.add(assassin);
        }
        if (mage.joinedParty) {
            activePartyMembers.add(mage);
        }

        // ΤΩΡΑ που υπάρχουν όλοι, φόρτωσε party stats
        loadPartyStats();
        loadEquipment();

        for (int i = 0; i < maxMaps; i++) {
            spawnTiledNPCs(i);
            spawnTiledChests(i);
        }
        
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
        
        // MENU PORTRAITS
        heroPortrait32 = loadImageSafe("res/menu/hero_portrait_32.png");
        assassinPortrait32 = loadImageSafe("res/menu/assassin_portrait_32.png");
        magePortrait32 = loadImageSafe("res/menu/mage_portrait_32.png");
        equipmentGridBg = loadImageSafe("res/gui/equipment_bg.png");
        menuPointer = loadImageSafe("res/gui/menu_pointer.png");
        worldMapBackground = loadImageSafe("res/gui/world_map_bg.png");
        worldMapTownIcon = loadImageSafe("res/gui/map_icon_town.png");
        worldMapPathIcon = loadImageSafe("res/gui/map_icon_path.png");
        worldMapForestIcon = loadImageSafe("res/gui/map_icon_forest.png");
        worldMapCaveIcon = loadImageSafe("res/gui/map_icon_cave.png");

        itemCatAllIcon = loadImageSafe("res/gui/item_cat_all.png");
        itemCatPotionsIcon = loadImageSafe("res/gui/item_cat_potions.png");
        itemCatWeaponsIcon = loadImageSafe("res/gui/item_cat_weapons.png");
        itemCatShieldsIcon = loadImageSafe("res/gui/item_cat_shields.png");
        itemCatHelmetsIcon = loadImageSafe("res/gui/item_cat_helmets.png");
        itemCatBodyArmorIcon = loadImageSafe("res/gui/item_cat_body_armor.png");
        itemCatNecklacesIcon = loadImageSafe("res/gui/item_cat_necklaces.png");
        itemCatConsumablesIcon = loadImageSafe("res/gui/item_cat_consumables.png");
        itemCatScrollsIcon = loadImageSafe("res/gui/item_cat_scrolls.png");

        swordIcon = loadImageSafe("res/gui/weapons/sword.png");
        spearIcon = loadImageSafe("res/gui/weapons/spear.png");
        staffIcon = loadImageSafe("res/gui/weapons/staff.png");
        shieldIcon = loadImageSafe("res/gui/weapons/break_shield.png");

        try {
            titleLogo = ImageIO.read(new File("res/title/logo.png")); // Δημιούργησε αυτό το μονοπάτι
            System.out.println("Logo loaded successfully!");
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Using text fallback for logo");
        }

        // ========== ΦΟΡΤΩΣΗ BATTLE ANIMATIONS ΓΙΑ ΤΟΝ ΠΑΙΚΤΗ ΜΕ SPRITESHEET ==========
        try {
            SpriteSheet idleSheet = new SpriteSheet("res/player/battle/idle.png", 64, 64);
            SpriteSheet hurtSheet = new SpriteSheet("res/player/battle/hurt.png", 64, 64);
            SpriteSheet deathSheet = new SpriteSheet("res/player/battle/death.png", 64, 64);
            SpriteSheet attack0Sheet = new SpriteSheet("res/player/battle/attack.png", 64, 64);
            SpriteSheet runLeftSheet = new SpriteSheet("res/player/battle/run_left.png", 64, 64);
            SpriteSheet runRightSheet = new SpriteSheet("res/player/battle/run_right.png", 64, 64);

            BufferedImage[] idleFrames = idleSheet.getAllFrames();
            BufferedImage[] hurtFrames = hurtSheet.getAllFrames();
            BufferedImage[] deathFrames = deathSheet.getAllFrames();
            BufferedImage[] attack0Frames = attack0Sheet.getAllFrames();
            BufferedImage[] runLeftFrames = runLeftSheet.getAllFrames();
            BufferedImage[] runRightFrames = runRightSheet.getAllFrames();
            BufferedImage[] attack1Frames = null;
            BufferedImage[] attack2Frames = null;
            BufferedImage[] attack3Frames = null;
            try {
                SpriteSheet attack1Sheet = new SpriteSheet("res/player/battle/attack2.png", 64, 64);
                attack1Frames = attack1Sheet.getAllFrames();
            } catch (Exception e) {
                System.out.println("attack2.png not found, using fallback");
                attack1Frames = attack0Frames;
            }

            try {
                SpriteSheet attack2Sheet = new SpriteSheet("res/player/battle/attack3.png", 64, 64);
                attack2Frames = attack2Sheet.getAllFrames();
            } catch (Exception e) {
                System.out.println("attack3.png not found, using fallback");
                attack2Frames = attack1Frames != null ? attack1Frames : attack0Frames;
            }

            try {
                SpriteSheet attack3Sheet = new SpriteSheet("res/player/battle/attack4.png", 64, 64);
                attack3Frames = attack3Sheet.getAllFrames();
            } catch (Exception e) {
                System.out.println("attack4.png not found, using fallback");
                attack3Frames = attack2Frames != null ? attack2Frames : 
                            (attack1Frames != null ? attack1Frames : attack0Frames);
            }

            playerBattleAnim = new PlayerAnimation(idleFrames, hurtFrames, deathFrames, attack0Frames);
            playerBattleAnim.attack1 = attack0Frames;
            playerBattleAnim.attack2 = attack1Frames;
            playerBattleAnim.attack3 = attack2Frames;
            playerBattleAnim.attack4 = attack3Frames;
            playerBattleAnim.runLeft = runLeftFrames;
            playerBattleAnim.runRight = runRightFrames;
            
            System.out.println("Player battle animations loaded successfully with SpriteSheet!");
            
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Failed to load player battle animations");
        }

        // ========== ΦΟΡΤΩΣΗ EFFECT SPRITE SHEETS ==========
        loadBattleEffectAssets();
        loadBoostAuraEffects();

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
            if (!inventorySaveExists()) {
                inventory.addItem(lantern);
            }
            
            System.out.println("Lantern created successfully!");
        } catch (Exception e) {
            e.printStackTrace();
        }

        // ========== ΠΡΟΦΟΡΤΩΣΗ ΗΧΩΝ ==========
        // Μουσικές
        sound.preloadMusic("overworld_day", "overworld_day.wav");
        sound.preloadMusic("monastery", "monastery.wav");
        sound.preloadMusic("fire_music", "fire_music.wav");
        sound.preloadMusic("overworld_night", "overworld_night.wav");
        sound.preloadMusic("town_day", "town_day.wav");
        sound.preloadMusic("town_night", "town_night.wav");
        sound.preloadMusic("dungeon", "dungeon_music.wav");
        sound.preloadMusic("battle", "battle_music.wav");
        sound.preloadMusic("merchant_village", "Merchant.wav");
        sound.preloadMusic("boss_battle", "boss_battle_music.wav");
        
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
        sound.preloadBattleSound("FLEE", "flee.wav");
        sound.preloadBattleSound("HEAL", "heal.wav");
        sound.preloadBattleSound("ITEM", "item.wav");
        sound.preloadBattleSound("LOWHPASSASSIN", "lowHpAssassin.wav");
        sound.preloadBattleSound("LOWHPMAGE", "LowHpMage.wav");
        sound.preloadBattleSound("MUSHROOM_ATTACK", "mushroom_attack.wav");
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
        sound.preloadBattleSound("NEXTTURN", "nextTurn.wav");
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
        sound.preloadBattleSound("BREAK", "break.wav");
        
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
        if (!inventorySaveExists()) {
            inventory.addItem(mapItem);
        }

        // Δημιούργησε portals
        // portals.add(new Portal(0, 12 * tileSize, 8 * tileSize, 1, 10 * tileSize, 41 * tileSize)); // Overworld -> Dungeon
        // portals.add(new Portal(1, 9 * tileSize, 41 * tileSize, 0, 11 * tileSize, 9 * tileSize)); // Dungeon -> Overworld
        // portals.add(new Portal(0, 10 * tileSize, 39 * tileSize, 2, 12 * tileSize, 12 * tileSize)); // Overworld -> Merchant House
        // portals.add(new Portal(2, 12 * tileSize, 13 * tileSize, 0, 10 * tileSize, 40 * tileSize)); // Merchant House -> Overworld
        // portals.add(new Portal(0, 41 * tileSize, 7 * tileSize, 3, 4 * tileSize, 22 * tileSize)); // Overworld -> Town
        // portals.add(new Portal(3, 3 * tileSize, 22 * tileSize, 0, 41 * tileSize, 8 * tileSize)); // Town -> Overworld

        // //portals.add(new Portal(3, 40 * tileSize, 30 * tileSize, 6, 8 * tileSize, 8 * tileSize)); // Town -> region
        // portals.add(new Portal(3, 40 * tileSize, 28 * tileSize, 6, 20 * tileSize, 10 * tileSize)); // Town -> testmaps

        initWorldMapRegions();
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
                        handleChestInteraction();
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

                    // ===== RESET MENU STATE =====
                    inventory.menuSection = 0;          // Items
                    inventory.menuFocus = 0;            // left command list
                    inventory.selectedItemCategory = 0; // Consumables
                    inventory.selectedPartyMember = 0;
                    inventory.hideDetails = false;

                    // κράτα προσωρινά και τα παλιά selections reset
                    inventory.selectedStorageSlot = 0;
                    inventory.selectedEquipSlot = 0;
                    inventory.selectedKeyItemSlot = 0;
                    inventory.inventoryMode = 0;
                    inventory.selectedItemsCategory = 0;
                    inventory.selectedItemsListIndex = 0;
                    inventory.itemUseTargetMode = false;
                    inventory.itemUseTargetIndex = 0;
                    inventory.worldMapOpenFromMenu = false;
                    inventory.worldMapTransitionAlpha = 0;
                    inventory.worldMapOpening = false;
                    inventory.worldMapClosing = false;

                    // καθάρισε latched menu/navigation keys για να μη μπει κατευθείαν στο center
                    keyH.enterPressed = false;
                    keyH.rightPressed = false;
                    keyH.leftPressed = false;
                    keyH.upPressed = false;
                    keyH.downPressed = false;
                    keyH.escapePressed = false;

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
                keyH.mPressed = false;
            }

            // ========== WORLD MAP POINTER ==========
            // if (gameState == mapState) {

            //     if (keyH.escapePressed) {
            //         gameState = playState;
            //         hoveredMapRegionName = "";
            //         keyH.escapePressed = false;
            //         try { Thread.sleep(180); } catch (Exception e) {}
            //     }

            //     int cursorSpeed = 8;

            //     if (keyH.upPressed) {
            //         mapCursorY -= cursorSpeed;
            //         if (mapCursorY < 40) mapCursorY = 40;
            //         keyH.upPressed = false;
            //     }

            //     if (keyH.downPressed) {
            //         mapCursorY += cursorSpeed;
            //         if (mapCursorY > screenHeight - 40) mapCursorY = screenHeight - 40;
            //         keyH.downPressed = false;
            //     }

            //     if (keyH.leftPressed) {
            //         mapCursorX -= cursorSpeed;
            //         if (mapCursorX < 40) mapCursorX = 40;
            //         keyH.leftPressed = false;
            //     }

            //     if (keyH.rightPressed) {
            //         mapCursorX += cursorSpeed;
            //         if (mapCursorX > screenWidth - 40) mapCursorX = screenWidth - 40;
            //         keyH.rightPressed = false;
            //     }

            //     WorldMapRegion hovered = getHoveredWorldMapRegion();
            //     if (hovered != null) {
            //         hoveredMapRegionName = hovered.displayName;
            //     } else {
            //         hoveredMapRegionName = "";
            //     }
            // }

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

            // ========== CHEST LOOT WINDOW ==========
            if (gameState == chestLootState) {
                if (keyH.enterPressed || keyH.escapePressed) {
                    gameState = playState;
                    chestLootItems.clear();
                    chestLootAmounts.clear();
                    chestLootSelectedIndex = 0;

                    playSound("menu_close");

                    keyH.enterPressed = false;
                    keyH.escapePressed = false;

                    try { Thread.sleep(180); } catch (Exception e) {}
                }
            }

            // ----- INVENTORY NAVIGATION -----
            if (gameState == inventoryState) {
                // ===== JOURNAL FULLSCREEN SUBMODE =====
                if (inventory.journalOpenFromMenu) {

                    if (keyH.escapePressed || keyH.iPressed || keyH.enterPressed) {
                        inventory.journalOpenFromMenu = false;
                        inventory.menuFocus = 0;

                        playSound("menu_close");

                        keyH.escapePressed = false;
                        keyH.iPressed = false;
                        keyH.enterPressed = false;

                        try { Thread.sleep(120); } catch (Exception e) {}
                    }

                    repaint();
                    try { Thread.sleep(16); } catch (Exception e) {}

                    continue;
                }
                // ===== WORLD MAP FULLSCREEN SUBMODE =====
                    if (inventory.worldMapOpenFromMenu) {

                        // opening fade
                        if (inventory.worldMapOpening) {
                            inventory.worldMapTransitionAlpha += 20;
                            if (inventory.worldMapTransitionAlpha >= 255) {
                                inventory.worldMapTransitionAlpha = 255;
                                inventory.worldMapOpening = false;
                            }
                        }

                        // closing fade
                        if (inventory.worldMapClosing) {
                            inventory.worldMapTransitionAlpha -= 20;
                            if (inventory.worldMapTransitionAlpha <= 0) {
                                inventory.worldMapTransitionAlpha = 0;
                                inventory.worldMapClosing = false;
                                inventory.worldMapOpenFromMenu = false;
                                inventory.menuFocus = 0;
                            }
                        }

                        // input μόνο όταν fully open
                        if (!inventory.worldMapOpening && !inventory.worldMapClosing) {

                            int cursorSpeed = 8;

                            if (keyH.upPressed) {
                                mapCursorY -= cursorSpeed;
                                if (mapCursorY < 40) mapCursorY = 40;
                                keyH.upPressed = false;
                            }

                            if (keyH.downPressed) {
                                mapCursorY += cursorSpeed;
                                if (mapCursorY > screenHeight - 40) mapCursorY = screenHeight - 40;
                                keyH.downPressed = false;
                            }

                            if (keyH.leftPressed) {
                                mapCursorX -= cursorSpeed;
                                if (mapCursorX < 40) mapCursorX = 40;
                                keyH.leftPressed = false;
                            }

                            if (keyH.rightPressed) {
                                mapCursorX += cursorSpeed;
                                if (mapCursorX > screenWidth - 40) mapCursorX = screenWidth - 40;
                                keyH.rightPressed = false;
                            }

                            WorldMapRegion hovered = getHoveredWorldMapRegion();
                            hoveredMapRegionName = (hovered != null) ? hovered.displayName : "";

                            if (keyH.enterPressed) {
                                if (hovered != null && hovered.travelEnabled) {
                                    fastTravelToRegion(hovered);

                                    // Άμεσο refresh μουσικής σύμφωνα με το νέο map
                                    refreshMusicForCurrentMap(true);

                                    // Κλείσιμο world map / επιστροφή στο gameplay
                                    inventory.worldMapOpenFromMenu = false;
                                    inventory.worldMapOpening = false;
                                    inventory.worldMapClosing = false;
                                    inventory.worldMapTransitionAlpha = 0;

                                    showInventory = false;
                                    gameState = playState;
                                    hoveredMapRegionName = "";
                                    inventory.menuFocus = 0;

                                    playSound("portal");
                                }

                                keyH.enterPressed = false;
                                try { Thread.sleep(180); } catch (Exception e) {}
                            }

                            if (keyH.escapePressed) {
                                inventory.worldMapClosing = true;
                                playSound("menu_close");
                                keyH.escapePressed = false;
                                try { Thread.sleep(120); } catch (Exception e) {}
                            }
                        }

                        repaint();
                        try { Thread.sleep(16); } catch (Exception e) {}

                        continue;
                    }

                if (inventory.statusDetailOpen) {

                    // =========================
                    // POPUP MODE
                    // =========================
                    if (inventory.statusEquipPopupOpen) {
                        ArrayList<Item> allEquipItems = getStatusEquipmentDisplayList();
                        Item popupSelectedItem = null;

                        if (!allEquipItems.isEmpty() &&
                            inventory.selectedEquipmentListIndex >= 0 &&
                            inventory.selectedEquipmentListIndex < allEquipItems.size()) {
                            popupSelectedItem = allEquipItems.get(inventory.selectedEquipmentListIndex);
                        }

                        Entity popupSelectedCharacter = getSelectedStatusCharacter();
                        boolean popupCanEquip = canCharacterEquipItem(popupSelectedCharacter, popupSelectedItem);

                        if (popupCanEquip && (keyH.leftPressed || keyH.rightPressed)) {
                            inventory.statusEquipPopupOption = (inventory.statusEquipPopupOption == 0) ? 1 : 0;
                            playSound("menu_select");
                            try { Thread.sleep(150); } catch (Exception e) {}
                            keyH.leftPressed = false;
                            keyH.rightPressed = false;
                        }

                        if (keyH.escapePressed) {
                            inventory.statusEquipPopupOpen = false;
                            inventory.statusEquipPopupOption = 0;
                            playSound("menu_close");
                            try { Thread.sleep(180); } catch (Exception e) {}
                            keyH.escapePressed = false;
                        }

                        if (keyH.enterPressed) {
                            if (popupCanEquip) {
                                if (inventory.statusEquipPopupOption == 0) {
                                    handleStatusEquipAction();
                                }
                            }

                            inventory.statusEquipPopupOpen = false;
                            inventory.statusEquipPopupOption = 0;

                            playSound("menu_select");
                            try { Thread.sleep(180); } catch (Exception e) {}
                            keyH.enterPressed = false;
                        }
                    }

                    // =========================
                    // STATUS DETAIL NORMAL MODE
                    // =========================
                    else {
                        if (keyH.escapePressed) {
                            inventory.statusDetailOpen = false;
                            playSound("menu_close");
                            try { Thread.sleep(180); } catch (Exception e) {}
                            keyH.escapePressed = false;
                        }

                        ArrayList<Item> allEquipItems = getStatusEquipmentDisplayList();
                        int maxIndex = allEquipItems.size() - 1;
                        if (maxIndex < 0) maxIndex = 0;

                        if (keyH.upPressed) {
                            inventory.selectedEquipmentListIndex--;
                            if (inventory.selectedEquipmentListIndex < 0) inventory.selectedEquipmentListIndex = 0;
                            playSound("menu_select");
                            try { Thread.sleep(150); } catch (Exception e) {}
                            keyH.upPressed = false;
                        }

                        if (keyH.downPressed) {
                            inventory.selectedEquipmentListIndex++;
                            if (inventory.selectedEquipmentListIndex > maxIndex) {
                                inventory.selectedEquipmentListIndex = maxIndex;
                            }
                            playSound("menu_select");
                            try { Thread.sleep(150); } catch (Exception e) {}
                            keyH.downPressed = false;
                        }

                        if (keyH.enterPressed && !allEquipItems.isEmpty()) {
                            inventory.statusEquipPopupOpen = true;
                            inventory.statusEquipPopupOption = 0;
                            playSound("menu_select");
                            try { Thread.sleep(150); } catch (Exception e) {}
                            keyH.enterPressed = false;
                        }
                    }
                }

                // =========================================
                // ONLY when status detail is NOT open
                // =========================================
                else {

                    if (inventory.menuFocus == 0) {

                        if (keyH.upPressed) {
                            inventory.menuSection--;
                            if (inventory.menuSection < 0) {
                                inventory.menuSection = mainMenuOptions.length - 1;
                            }
                            playSound("menu_select");
                            try { Thread.sleep(150); } catch (Exception e) {}
                            keyH.upPressed = false;
                        }

                        if (keyH.downPressed) {
                            inventory.menuSection++;
                            if (inventory.menuSection >= mainMenuOptions.length) {
                                inventory.menuSection = 0;
                            }
                            playSound("menu_select");
                            try { Thread.sleep(150); } catch (Exception e) {}
                            keyH.downPressed = false;
                        }

                        if (keyH.enterPressed) {

                            // =========================
                            // ITEMS
                            // =========================
                            if (inventory.menuSection == 0) {
                                inventory.menuFocus = 1;
                                inventory.selectedItemsCategory = 0;
                                inventory.selectedItemsListIndex = 0;
                                inventory.itemUseTargetMode = false;
                                inventory.itemUseTargetIndex = 0;
                            }

                            // =========================
                            // STATUS
                            // =========================
                            else if (inventory.menuSection == 1) {
                                inventory.menuFocus = 1;
                                inventory.selectedEquipSlot = 0;
                                inventory.inventoryMode = 1;
                            }

                            // =========================
                            // EQUIPMENT
                            // =========================
                            else if (inventory.menuSection == 2) {
                                inventory.menuFocus = 1;
                                inventory.selectedPartyMember = 0;
                            }

                            // =========================
                            // WORLD MAP
                            // =========================
                            else if (inventory.menuSection == 3) {

                                boolean hasMap = false;

                                for (int i = 0; i < inventory.keyItems.length; i++) {
                                    if (inventory.keyItems[i] != null &&
                                        inventory.keyItems[i].name.equals("World Map")) {
                                        hasMap = true;
                                        break;
                                    }
                                }

                                if (hasMap) {
                                    inventory.worldMapOpenFromMenu = true;
                                    inventory.worldMapOpening = true;
                                    inventory.worldMapClosing = false;
                                    inventory.worldMapTransitionAlpha = 0;

                                    mapCursorX = screenWidth / 2;
                                    mapCursorY = screenHeight / 2;
                                    hoveredMapRegionName = "";

                                    playSound("menu_select");
                                } else {
                                    startDialogue("Δεν έχεις χάρτη ακόμα...");
                                    gameState = dialogueState;
                                }

                                keyH.enterPressed = false;
                                try { Thread.sleep(180); } catch (Exception e) {}

                                continue;
                            }

                            // =========================
                            // JOURNAL
                            // =========================
                            else if (inventory.menuSection == 4) {
                                inventory.journalOpenFromMenu = true;

                                playSound("menu_select");

                                keyH.enterPressed = false;
                                try { Thread.sleep(180); } catch (Exception e) {}

                                continue;
                            }

                            // =========================
                            // SAVE
                            // =========================
                            else if (inventory.menuSection == 5) {
                                saveGame();
                                startDialogue("Το παιχνίδι αποθηκεύτηκε!");
                                gameState = dialogueState;
                                keyH.enterPressed = false;
                            }

                            // =========================
                            // OPTIONS
                            // =========================
                            else if (inventory.menuSection == 6) {
                                inventory.menuFocus = 1;
                            }

                            playSound("menu_select");
                            try { Thread.sleep(180); } catch (Exception e) {}
                            keyH.enterPressed = false;
                        }

                        if (keyH.escapePressed) {
                            gameState = playState;
                            showInventory = false;
                            playSound("menu_close");
                            try { Thread.sleep(180); } catch (Exception e) {}
                            keyH.escapePressed = false;
                        }
                    }
                    
                    else if (inventory.menuFocus == 1) {

                        // =========================
                        // ITEMS SECTION
                        // =========================
                        if (inventory.menuSection == 0) {

                            ArrayList<Item> filteredItems = getFilteredItemsList();
                            int maxIndex = filteredItems.size() - 1;
                            if (maxIndex < 0) maxIndex = 0;

                            // =====================================
                            // TARGET SELECT MODE
                            // =====================================
                            if (inventory.itemUseTargetMode) {

                                if (keyH.upPressed) {
                                    inventory.itemUseTargetIndex--;
                                    if (inventory.itemUseTargetIndex < 0) {
                                        inventory.itemUseTargetIndex = 0;
                                    }

                                    playSound("menu_select");
                                    try { Thread.sleep(150); } catch (Exception e) {}
                                    keyH.upPressed = false;
                                }

                                if (keyH.downPressed) {
                                    inventory.itemUseTargetIndex++;
                                    int maxTarget = getMaxItemUseTargetIndex();
                                    if (inventory.itemUseTargetIndex > maxTarget) {
                                        inventory.itemUseTargetIndex = maxTarget;
                                    }

                                    playSound("menu_select");
                                    try { Thread.sleep(150); } catch (Exception e) {}
                                    keyH.downPressed = false;
                                }

                                if (keyH.escapePressed) {
                                    inventory.itemUseTargetMode = false;
                                    inventory.itemUseTargetIndex = 0;

                                    playSound("menu_close");
                                    try { Thread.sleep(180); } catch (Exception e) {}
                                    keyH.escapePressed = false;
                                }

                                if (keyH.enterPressed) {
                                    if (!filteredItems.isEmpty() &&
                                        inventory.selectedItemsListIndex >= 0 &&
                                        inventory.selectedItemsListIndex < filteredItems.size()) {

                                        Item selected = filteredItems.get(inventory.selectedItemsListIndex);

                                        if (isUsableItemFromItemsMenu(selected)) {
                                            Entity target = getSelectedItemUseTarget();

                                            applyItemToTarget(selected, target);
                                            consumeOneItemFromInventory(selected);

                                            inventory.itemUseTargetMode = false;
                                            inventory.itemUseTargetIndex = 0;

                                            sound.playBattleSE("item");
                                        }
                                    }

                                    try { Thread.sleep(180); } catch (Exception e) {}
                                    keyH.enterPressed = false;
                                }
                            }

                            // =====================================
                            // NORMAL ITEMS MODE
                            // =====================================
                            else {

                                // category αλλαγή
                                if (keyH.leftPressed) {
                                    inventory.selectedItemsCategory--;
                                    if (inventory.selectedItemsCategory < 0) {
                                        inventory.selectedItemsCategory = 0;
                                    }

                                    inventory.selectedItemsListIndex = 0;

                                    playSound("menu_select");
                                    try { Thread.sleep(150); } catch (Exception e) {}
                                    keyH.leftPressed = false;
                                }

                                if (keyH.rightPressed) {
                                    inventory.selectedItemsCategory++;
                                    if (inventory.selectedItemsCategory > itemCategoryOptions.length - 1) {
                                        inventory.selectedItemsCategory = itemCategoryOptions.length - 1;
                                    }

                                    inventory.selectedItemsListIndex = 0;

                                    playSound("menu_select");
                                    try { Thread.sleep(150); } catch (Exception e) {}
                                    keyH.rightPressed = false;
                                }

                                // item list navigation
                                if (keyH.upPressed) {
                                    if (!filteredItems.isEmpty()) {
                                        inventory.selectedItemsListIndex--;
                                        if (inventory.selectedItemsListIndex < 0) {
                                            inventory.selectedItemsListIndex = 0;
                                        }
                                    } else {
                                        inventory.selectedItemsListIndex = 0;
                                    }

                                    playSound("menu_select");
                                    try { Thread.sleep(150); } catch (Exception e) {}
                                    keyH.upPressed = false;
                                }

                                if (keyH.downPressed) {
                                    if (!filteredItems.isEmpty()) {
                                        inventory.selectedItemsListIndex++;
                                        if (inventory.selectedItemsListIndex > maxIndex) {
                                            inventory.selectedItemsListIndex = maxIndex;
                                        }
                                    } else {
                                        inventory.selectedItemsListIndex = 0;
                                    }

                                    playSound("menu_select");
                                    try { Thread.sleep(150); } catch (Exception e) {}
                                    keyH.downPressed = false;
                                }

                                if (keyH.enterPressed) {
                                    if (!filteredItems.isEmpty() &&
                                        inventory.selectedItemsListIndex >= 0 &&
                                        inventory.selectedItemsListIndex < filteredItems.size()) {

                                        Item selected = filteredItems.get(inventory.selectedItemsListIndex);

                                        if (isUsableItemFromItemsMenu(selected)) {
                                            inventory.itemUseTargetMode = true;
                                            inventory.itemUseTargetIndex = 0;
                                            playSound("menu_select");
                                        } else {
                                            playSound("menu_select");
                                        }
                                    }

                                    try { Thread.sleep(150); } catch (Exception e) {}
                                    keyH.enterPressed = false;
                                }
                            }
                        }

                        // =========================
                        // EQUIPMENT SECTION
                        // =========================
                        else if (inventory.menuSection == 1) {

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
                                try { Thread.sleep(180); } catch (Exception e) {}
                                keyH.enterPressed = false;
                            }
                        }

                        // =========================
                        // STATUS SECTION
                        // =========================
                        else if (inventory.menuSection == 2) {
                            if (keyH.upPressed) {
                                inventory.selectedPartyMember--;
                                if (inventory.selectedPartyMember < 0) inventory.selectedPartyMember = 0;
                                playSound("menu_select");
                                try { Thread.sleep(150); } catch (Exception e) {}
                                keyH.upPressed = false;
                            }
                            if (keyH.downPressed) {
                                inventory.selectedPartyMember++;
                                int maxPartyIndex = activePartyMembers.size(); // 0 = hero, 1.. = party members
                                if (inventory.selectedPartyMember > maxPartyIndex) {
                                    inventory.selectedPartyMember = maxPartyIndex;
                                }
                                playSound("menu_select");
                                try { Thread.sleep(150); } catch (Exception e) {}
                                keyH.downPressed = false;
                            }
                            if (keyH.enterPressed) {
                                inventory.statusDetailOpen = true;
                                inventory.selectedEquipmentListIndex = 0;
                                playSound("menu_select");
                                try { Thread.sleep(180); } catch (Exception e) {}
                                keyH.enterPressed = false;
                            }
                        }

                        // Back to left menu
                        if (keyH.escapePressed) {
                            inventory.menuFocus = 0;
                            playSound("menu_close");
                            try { Thread.sleep(180); } catch (Exception e) {}
                            keyH.escapePressed = false;
                        }
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
                        
                        if (!battleParty.party.isEmpty()) {
                            BattleEntity heroEntity = battleParty.party.get(0);
                            updateBattlePlayerMovementAnimation(heroEntity, bp.x, bp.targetX);
                        }
                    }

                    // Κίνηση party members προς τα μέσα ΜΕ ANIMATION
                    for (BattlePartyMember bpm : battlePartyMembers) {
                        bpm.x -= (bpm.x - bpm.targetX) / 10;
                        if (Math.abs(bpm.x - bpm.targetX) < 1) bpm.x = bpm.targetX;

                        for (BattleEntity entity : battleParty.party) {
                            if (entity.name.equals(bpm.member.className)) {
                                updateBattlePlayerMovementAnimation(entity, bpm.x, bpm.targetX);
                                break;
                            }
                        }
                    }
                    
                    // Όταν τελειώσει το transition
                    if (battleTransitionTimer >= BATTLE_TRANSITION_TIME) {
                        battleEntering = false;
                        // Βεβαιώσου ότι είναι στις σωστές θέσεις
                        for (BattleEnemy be : battleEnemies) be.x = be.targetX;
                        for (BattlePlayer bp : battlePlayers) {
                            bp.x = bp.targetX;
                        }
                        for (BattlePartyMember bpm : battlePartyMembers) {
                            bpm.x = bpm.targetX;
                        }
                        
                        for (BattleEntity entity : battleParty.party) {
                            setActorIdleAnimation(entity);
                        }
                    }
                    
                    repaint();
                } else if (battleFleeing) {
                    for (BattlePlayer bp : battlePlayers) {
                        bp.x += BATTLE_FLEE_SPEED;
                        updateBattlePlayerMovementAnimation(
                                !battleParty.party.isEmpty() ? battleParty.party.get(0) : null,
                                bp.x,
                                bp.targetX
                        );
                    }

                    for (BattlePartyMember bpm : battlePartyMembers) {
                        bpm.x += BATTLE_FLEE_SPEED;

                        for (BattleEntity entity : battleParty.party) {
                            if (entity.name.equals(bpm.member.className)) {
                                updateBattlePlayerMovementAnimation(entity, bpm.x, bpm.targetX);
                                break;
                            }
                        }
                    }

                    if (areAllBattlePlayersOffscreenRight()) {
                        battleFleeing = false;
                        actionInProgress = false;
                        commandLocked = false;
                        gameState = playState;
                        returnToMapMusic();

                        for (BattleEntity entity : battleParty.party) {
                            setActorIdleAnimation(entity);
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

                    // SLOW MOTION TIMER
                    if (slowMotionTimer > 0) {
                        slowMotionTimer--;
                        if (slowMotionTimer <= 0) {
                            battleTimeScale = 1.0f;
                        }
                    }

                    // BATTLE UPDATES με time scale
                    if (battleTimeScale >= 1.0f || slowMotionTimer % Math.max(1, (int)(1.0f / battleTimeScale)) == 0) {
                        updateBattleVisuals();
                        updateBattleAction();
                    }

                    // Αυτά τρέχουν πάντα
                    updateHitReactions();
                    updateBoostBurstEffect();
                    updateZoomPop();
                    updateLunge();
                    updateSwingTrail();
                    updateBattleEffects();
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

                            boolean wasLastActorInRound =
                                    !battleParty.turnOrder.isEmpty() &&
                                    battleParty.currentTurnIndex == battleParty.turnOrder.size() - 1;

                            battleParty.nextTurn();

                            if (wasLastActorInRound) {
                                // Μπήκαμε σε νέο round, ξαναχτίζουμε το NEXT TURN
                                rebuildNextTurnPreview();
                            } else {
                                // Reveal ακόμα ένα slot από το επόμενο round
                                revealOneMoreNextTurnSlot();
                            }

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
                            String battleTrack = (forcedBattleMusic != null && !forcedBattleMusic.isEmpty())
                                    ? forcedBattleMusic
                                    : "battle";

                            sound.playMusic(battleTrack);
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
                                    if (currentPlayer.name.equals("Assassin")) {
                                        sound.playBattleSE("BOOSTASSASSIN");
                                    } else if (currentPlayer.name.equals("Mage")) {
                                        sound.playBattleSE("BOOSTMAGE");
                                    }
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
                                    sound.playBattleSE("FLEE");
                                    commandLocked = true;
                                    actionInProgress = true;
                                    battleFleeing = true;

                                    sound.stopBattleLoop();
                                    selectedBoost = 0;
                                    boostLoopLevel = 0;
                                    boostLoopTimer = 0;

                                    battleMessage = "Απέδρασες!";

                                    // Στείλε όλους τους παίκτες έξω δεξιά
                                    for (BattlePlayer bp : battlePlayers) {
                                        bp.targetX = screenWidth + 220;
                                    }

                                    for (BattlePartyMember bpm : battlePartyMembers) {
                                        bpm.targetX = screenWidth + 220;
                                    }

                                    for (BattleEntity entity : battleParty.party) {
                                        setActorRunAnimation(entity, "run_right");
                                    }

                                    keyH.enterPressed = false;
                                    repaint();
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

                    // Ο Hero παίρνει το ίδιο EXP
                    player.addExp(victoryExp);

                    // Όλα τα party members παίρνουν το ίδιο EXP
                    for (PartyMember member : activePartyMembers) {
                        member.addExp(victoryExp);
                    }

                    // Τα gold είναι κοινά, μπαίνουν μία φορά μόνο
                    player.gold += victoryGold;

                    victoryRewardsShown = true;
                    sound.stopMusic();
                    playSound("levelup");
                }
                
                // Αν πατήσει Enter, ξεκίνα fade out
                if (keyH.enterPressed && !battleFadeOut) {
                    if (storyManager.hasFlag(StoryFlag.DEMO_BOSS_DEFEATED) &&
                        !storyManager.hasFlag(StoryFlag.DEMO_COMPLETE) &&
                        "ashen_guardian".equals(activeStoryBattleId)) {

                        activeStoryBattleId = "";
                        startStoryEvent("demo_ending");
                    }
                    battleFadeOut = true;
                    battleFadeAlpha = 0;
                    keyH.enterPressed = false;
                }
                
                repaint();
            }
            // ========== CUTSCENE STATE ==========
            if (cutscenePlayer.isRunning()) {
                cutscenePlayer.update();
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
                        refreshMusicForCurrentMap(true);
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
                        
                        // Ξεκίνα crossfade προς τη σωστή νυχτερινή μουσική για το current map
                        currentDayMusic = getDayMusicForMap(currentMap);
                        currentNightMusic = getNightMusicForMap(currentMap);
                        startMusicCrossfade(currentNightMusic);
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
                        
                        // Ξεκίνα crossfade προς τη σωστή ημερήσια μουσική για το current map
                        currentDayMusic = getDayMusicForMap(currentMap);
                        currentNightMusic = getNightMusicForMap(currentMap);
                        startMusicCrossfade(currentDayMusic);
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
            // IMPORTANT:
            // Μην καταναλώνεις το Escape εδώ όταν είμαστε στο inventory/menu,
            // γιατί το status detail window έχει δικό του back logic.
            if (keyH.escapePressed && gameState != inventoryState) {
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
                checkStoryTriggers();
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
                        resetPartyFollowAfterTeleport();

                        // μουσική
                        if (currentMap == 0) { // Town
                            if (dayTime == 0 || dayTime == 3) {
                                sound.playMusic("monastery");
                            } else {
                                sound.playMusic("monastery");
                            }
                        } else if (currentMap == 1) { // Fields
                            if (dayTime == 0 || dayTime == 3) {
                                sound.playMusic("town_day");
                            } else {
                                sound.playMusic("town_night");
                            }
                        } else if (currentMap == 2) { // Cave
                            sound.playMusic("dungeon");
                        } else {
                            sound.playMusic("overworld_day");
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
                        boolean wasAshenGuardianStoryBattle =
                                "ashen_guardian".equals(activeStoryBattleId);

                        if (wasAshenGuardianStoryBattle) {
                            storyManager.setFlag(StoryFlag.DEMO_BOSS_DEFEATED);
                        }

                        forcedBattleMusic = "";
                        returnToMapMusic();

                        gameState = playState;

                        if (wasAshenGuardianStoryBattle &&
                            !storyManager.hasFlag(StoryFlag.DEMO_COMPLETE)) {

                            activeStoryBattleId = "";
                            startStoryEvent("demo_ending");
                        } else {
                            activeStoryBattleId = "";
                        }
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

            if (showObjectivePopup) {
                objectivePopupTimer--;
                if (objectivePopupTimer <= 0) {
                    objectivePopupTimer = 0;
                    showObjectivePopup = false;
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

    private String getDayMusicForMap(int mapIndex) {
        AdvancedMapData map = tileM.getMap(mapIndex);
        if (map == null || map.name == null) {
            return "overworld_day";
        }

        String mapName = map.name.toLowerCase();

        if (mapName.contains("town")) {
            return "town_day";
        }
        if (mapName.contains("monastery")) {
            return "monastery";
        }
        if (mapName.contains("cave") || mapName.contains("mountain")) {
            return "dungeon";
        }
        if (mapName.contains("field")) {
            return "overworld_day";
        }

        return "overworld_day";
    }

    private String getNightMusicForMap(int mapIndex) {
        AdvancedMapData map = tileM.getMap(mapIndex);
        if (map == null || map.name == null) {
            return "overworld_night";
        }

        String mapName = map.name.toLowerCase();

        if (mapName.contains("town")) {
            return "town_night";
        }
        if (mapName.contains("cave")) {
            return "dungeon";
        }
        if (mapName.contains("field")) {
            return "overworld_night";
        }

        return "overworld_night";
    }

    private void refreshMusicForCurrentMap(boolean immediate) {
        currentDayMusic = getDayMusicForMap(currentMap);
        currentNightMusic = getNightMusicForMap(currentMap);

        String targetMusic = (dayTime == 0 || dayTime == 3) ? currentDayMusic : currentNightMusic;

        if (immediate) {
            isCrossfading = false;
            nextMusic = "";
            sound.stopMusic();
            sound.playMusic(targetMusic);

            currentMusicVolume = musicVolume / 100.0f;
            sound.setMusicVolume(currentMusicVolume);
        } else {
            startMusicCrossfade(targetMusic);
        }
    }

    public void startMusicCrossfade(String newMusic) {
        if (currentMap != 0 && currentMap != 3) return;
        
        nextMusic = newMusic;
        isCrossfading = true;
    }

    public void returnToMapMusic() {
        sound.stopMusic();
        if (currentMap == 0) { // Monastery
            if (dayTime == 0 || dayTime == 3) {
                sound.playMusic("monastery");
            } else {
                sound.playMusic("monastery");
            }
        } else if (currentMap == 1) { // Town
            if (dayTime == 0 || dayTime == 3) {
                sound.playMusic("town_day");
            } else {
                sound.playMusic("town_night");
            }
        } else if (currentMap == 2) { // Cave
            sound.playMusic("dungeon");
        } else {
            sound.playMusic("overworld_day");
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
            String storyRole = obj.getProperty("storyRole");
            String requiredStoryFlag = obj.getProperty("requiredStoryFlag");
            String forbiddenStoryFlag = obj.getProperty("forbiddenStoryFlag");
            String hiddenProp = obj.getProperty("hidden");

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
                npc.storyRole = storyRole;
                npc.requiredStoryFlag = requiredStoryFlag;
                npc.forbiddenStoryFlag = forbiddenStoryFlag;
                npc.hidden = "true".equalsIgnoreCase(hiddenProp);

                npcs.get(mapIndex).add(npc);
            }
        }
    }

    public String[] getDialogue(String id) {
        if (id == null) return new String[]{"..."};

        switch (id) {
            case "master_ren_idle":
                if (!storyManager.hasFlag(StoryFlag.MONK_ESCAPE_DONE)) {
                    return new String[]{
                        "Go, Kael.",
                        "Do not let fear chain your feet."
                    };
                }
                return new String[]{
                    "..."
                };

            case "wounded_monk_1":
                return new String[]{
                    "They came out of the smoke...",
                    "Not bandits. Not soldiers. Something worse."
                };

            case "wounded_monk_2":
                return new String[]{
                    "Master Ren is still inside...",
                    "Please... you have to keep moving."
                };

            case "town_woman_1":
                if (!storyManager.hasFlag(StoryFlag.TOWN_REACHED)) {
                    return new String[]{
                        "The market closes early these days."
                    };
                }
                return new String[]{
                    "You're from the mountain, aren't you?",
                    "People saw fire on the ridge before dawn."
                };

            case "old_scholar_1":
                if (!storyManager.hasFlag(StoryFlag.TOWN_REACHED)) {
                    return new String[]{
                        "Hm..."
                    };
                }
                return new String[]{
                    "Monasteries keep records older than kingdoms.",
                    "If someone burned yours, they were not hunting gold."
                };

            case "merchant_intro":
                return new String[]{
                    "You look exhausted, traveler.",
                    "Supplies cost coin, but information is sometimes cheaper."
                };

            case "assassin_scene_npc":
                return new String[]{
                    "A woman in dark clothes was asking about monks.",
                    "She headed toward the east quarter."
                };
                
            case "town_guard":
                if (!storyManager.hasFlag(StoryFlag.TOWN_REACHED)) {
                    return new String[]{
                        "The roads are dangerous lately."
                    };
                } else if (!storyManager.hasFlag(StoryFlag.ASSASSIN_JOINED)) {
                    return new String[]{
                        "You're from the mountain?",
                        "A hooded woman came through earlier asking about fires and dead monks."
                    };
                } else {
                    return new String[]{
                        "The east quarter has been tense all night.",
                        "Keep your hand near your weapon."
                    };
                }

            case "inn_keeper":
                if (!storyManager.hasFlag(StoryFlag.TOWN_REACHED)) {
                    return new String[]{
                        "Welcome, traveler."
                    };
                } else {
                    return new String[]{
                        "People are whispering about the monastery.",
                        "Whatever happened up there, it has everyone on edge."
                    };
                }
            case "east_quarter_guard":
                if (!storyManager.hasFlag(StoryFlag.ASSASSIN_INTRO_PLAYED)) {
                    return new String[]{
                        "A hooded woman slipped through here not long ago.",
                        "Did not look like trouble. That is what worries me."
                    };
                }
                return new String[]{
                    "If you're chasing her, be careful.",
                    "People like that don't walk in straight lines."
                };

            case "alley_witness":
                if (!storyManager.hasFlag(StoryFlag.ASSASSIN_INTRO_PLAYED)) {
                    return new String[]{
                        "I heard voices deeper in the alley.",
                        "One of them sounded calm. Too calm."
                    };
                }
                return new String[]{
                    "She vanished before I could even blink.",
                    "I swear she was standing right there."
                };

            case "academy_guard_1":
                if (!storyManager.hasFlag(StoryFlag.ASSASSIN_JOINED)) {
                    return new String[]{
                        "The academy is closed to outsiders."
                    };
                }
                if (!storyManager.hasFlag(StoryFlag.MAGE_INTRO_PLAYED)) {
                    return new String[]{
                        "A young scholar rushed inside not long ago.",
                        "He looked terrified... and angry."
                    };
                }
                return new String[]{
                    "Whatever happened inside, it has the whole district on edge."
                };

            case "worried_student_1":
                if (!storyManager.hasFlag(StoryFlag.MAGE_INTRO_PLAYED)) {
                    return new String[]{
                        "Someone broke into the old archive.",
                        "I heard shouting from the library hall."
                    };
                }
                return new String[]{
                    "That scholar said the records were stolen.",
                    "He kept talking about ancient monastery texts."
                };
        }

        return new String[]{"..."};
    }

    private void checkStoryTriggers() {
        ArrayList<TiledObjectData> triggers = tileM.getMapObjectsByLayer(currentMap, "story_triggers");
        if (triggers == null || triggers.isEmpty()) return;
        if (cutscenePlayer.isRunning()) return;
        if (gameState != playState) return;

        int px = player.worldX;
        int py = player.worldY;

        for (TiledObjectData obj : triggers) {
            int ox = (int)(obj.x / originalTileSize) * tileSize;
            int oy = (int)(obj.y / originalTileSize) * tileSize;
            int ow = Math.max(tileSize, (int)(obj.width / originalTileSize) * tileSize);
            int oh = Math.max(tileSize, (int)(obj.height / originalTileSize) * tileSize);

            boolean inside =
                    px + tileSize > ox &&
                    px < ox + ow &&
                    py + tileSize > oy &&
                    py < oy + oh;

            if (!inside) continue;

            String eventId = obj.getProperty("eventId");
            String requiredFlag = obj.getProperty("requiredFlag");
            String forbiddenFlag = obj.getProperty("forbiddenFlag");
            System.out.println("TRIGGER HIT: " + eventId + " on map " + currentMap);

            if (eventId == null || eventId.isEmpty()) continue;

            if (requiredFlag != null && !requiredFlag.isEmpty() && !storyManager.hasFlag(requiredFlag)) {
                continue;
            }

            if (forbiddenFlag != null && !forbiddenFlag.isEmpty() && storyManager.hasFlag(forbiddenFlag)) {
                continue;
            }

            startStoryEvent(eventId);
            return;
        }
        
    }

    public void startStoryEvent(String eventId) {
        if (eventId == null || eventId.isEmpty()) return;

        ArrayList<CutsceneAction> actions = new ArrayList<>();

        switch (eventId) {
            case "monk_intro_start":
                if (storyManager.hasFlag(StoryFlag.DEMO_INTRO_PLAYED)) return;

                actions.add(CutsceneAction.setFlag(StoryFlag.DEMO_INTRO_PLAYED));
                actions.add(CutsceneAction.setObjective(
                        "escape_monastery",
                        "Escape the Monastery",
                        "Reach the outer gate before the attackers overrun the sanctuary."
                ));
                actions.add(CutsceneAction.dialogue(
                        "Master Ren: Kael... listen carefully.\n" +
                        "The sanctuary has fallen.\n" +
                        "You must take the First Breath and leave at once."
                ));
                actions.add(CutsceneAction.dialogue(
                        "Kael: I cannot abandon everyone!\n" +
                        "Master Ren: If you fall here, the flame of our order dies with you."
                ));
                actions.add(CutsceneAction.setFlag(StoryFlag.MONK_ESCAPE_STARTED));
                actions.add(CutsceneAction.endCutscene());
                cutscenePlayer.start(actions);
                return;

            case "monk_exit_gate":
                if (!storyManager.hasFlag(StoryFlag.MONK_ESCAPE_STARTED)) return;
                if (storyManager.hasFlag(StoryFlag.MONK_ESCAPE_DONE)) return;

                actions.add(CutsceneAction.dialogue(
                        "Kael: The outer path is still open...\n" +
                        "I have no choice now."
                ));
                actions.add(CutsceneAction.setFlag(StoryFlag.MONK_ESCAPE_DONE));
                actions.add(CutsceneAction.setObjective(
                        "reach_town",
                        "Reach the Town",
                        "Descend from the mountain and seek answers in the settlement below."
                ));
                actions.add(CutsceneAction.endCutscene());
                cutscenePlayer.start(actions);
                return;

            case "road_reflection_1":
                if (!storyManager.hasFlag(StoryFlag.MONK_ESCAPE_DONE)) return;
                if (storyManager.hasFlag(StoryFlag.TOWN_REACHED)) return;
                if (storyManager.hasFlag(StoryFlag.ROAD_REFLECTION_1_DONE)) return;

                actions.add(CutsceneAction.dialogue(
                        "Kael: The smoke still rises behind me...\n" +
                        "If they destroyed the monastery for the First Breath, they will not stop there."
                ));
                actions.add(CutsceneAction.dialogue(
                        "Kael: I need answers.\n" +
                        "And I need them before whoever did this vanishes into the dark."
                ));
                actions.add(CutsceneAction.setFlag(StoryFlag.ROAD_REFLECTION_1_DONE));
                actions.add(CutsceneAction.endCutscene());
                cutscenePlayer.start(actions);
                return;
            
            case "town_arrival":
                if (storyManager.hasFlag(StoryFlag.TOWN_REACHED)) return;

                actions.add(CutsceneAction.setFlag(StoryFlag.TOWN_REACHED));
                actions.add(CutsceneAction.dialogue(
                        "Kael: So this is the town below the mountain...\n" +
                        "If the attackers passed through here, someone must know something."
                ));
                actions.add(CutsceneAction.setObjective(
                        "ask_in_town",
                        "Search for Answers",
                        "Talk to the townspeople and learn who attacked the monastery."
                ));
                actions.add(CutsceneAction.endCutscene());
                cutscenePlayer.start(actions);
                return;
            
            case "assassin_intro_start":
                if (!storyManager.hasFlag(StoryFlag.TOWN_REACHED)) return;
                if (storyManager.hasFlag(StoryFlag.ASSASSIN_INTRO_PLAYED)) return;

                actions.add(CutsceneAction.setFlag(StoryFlag.ASSASSIN_INTRO_PLAYED));
                actions.add(CutsceneAction.dialogue(
                        "Voice: Too late.\n" +
                        "Whoever was here is already gone."
                ));
                actions.add(CutsceneAction.dialogue(
                        "Kael: Show yourself."
                ));
                actions.add(CutsceneAction.dialogue(
                        "Mysterious Woman: Calm. If I wanted you dead, you would not have \n" + 
                        "heard my voice first."
                ));
                actions.add(CutsceneAction.dialogue(
                        "Kael: Were you at the monastery?"
                ));
                actions.add(CutsceneAction.dialogue(
                        "Mysterious Woman: I was near it.\n" +
                        "Close enough to smell the ash. Close enough to know \n" + 
                        "you are carrying something others would kill for."
                ));
                actions.add(CutsceneAction.dialogue(
                        "Kael: Then answer me.\n" +
                        "Who attacked us?"
                ));
                actions.add(CutsceneAction.dialogue(
                        "Mysterious Woman: Ask better questions.\n" +
                        "The dead monastery is not the beginning of your trouble.\n" + 
                        "Only the echo."
                ));
                actions.add(CutsceneAction.setObjective(
                        "track_assassin",
                        "Track the Hooded Woman",
                        "Search the eastern quarter and learn why the mysterious \n" + 
                        "assassin was investigating the monastery."
                ));
                actions.add(CutsceneAction.endCutscene());
                cutscenePlayer.start(actions);
                return;
                
            case "assassin_join_start":
                if (!storyManager.hasFlag(StoryFlag.ASSASSIN_INTRO_PLAYED)) return;
                if (storyManager.hasFlag(StoryFlag.ASSASSIN_JOIN_SCENE_DONE)) return;

                actions.add(CutsceneAction.setFlag(StoryFlag.ASSASSIN_JOIN_SCENE_DONE));
                actions.add(CutsceneAction.dialogue(
                        "Kael: Stop running."
                ));
                actions.add(CutsceneAction.dialogue(
                        "Mysterious Woman: I was waiting.\n" +
                        "You are slower than I expected."
                ));
                actions.add(CutsceneAction.dialogue(
                        "Kael: Then speak plainly."
                ));
                actions.add(CutsceneAction.dialogue(
                        "Mysterious Woman: Fine.\n" +
                        "Men connected to the attack are moving through this town.\n" +
                        "I am hunting them for my own reasons."
                ));
                actions.add(CutsceneAction.dialogue(
                        "Kael: And you expect me to trust you?"
                ));
                actions.add(CutsceneAction.dialogue(
                        "Mysterious Woman: No.\n" +
                        "I expect you to understand that we want the same answers."
                ));
                actions.add(CutsceneAction.dialogue(
                        "Kael: ...Then until I have those answers, we travel together."
                ));
                actions.add(CutsceneAction.dialogue(
                        "Seren: Hm.\n" +
                        "Very well, monk. Try not to die before you become useful."
                ));
                actions.add(CutsceneAction.setFlag(StoryFlag.ASSASSIN_JOINED));
                actions.add(CutsceneAction.unlockPartyMember("Assassin"));
                actions.add(CutsceneAction.setObjective(
                        "seek_scholar",
                        "Find a Scholar",
                        "Now that Seren has joined you, investigate who in town might know more about the relic and the monastery records."
                ));
                actions.add(CutsceneAction.endCutscene());
                cutscenePlayer.start(actions);
                return;
                
            case "mage_intro_start":
                if (!storyManager.hasFlag(StoryFlag.ASSASSIN_JOINED)) return;
                if (storyManager.hasFlag(StoryFlag.MAGE_INTRO_PLAYED)) return;

                actions.add(CutsceneAction.setFlag(StoryFlag.MAGE_INTRO_PLAYED));
                actions.add(CutsceneAction.dialogue(
                        "Eldrin: No, no, no...\n" +
                        "If the archive pages were taken, then someone knew exactly what to look for."
                ));
                actions.add(CutsceneAction.dialogue(
                        "Kael: You there. What was stolen?"
                ));
                actions.add(CutsceneAction.dialogue(
                        "Eldrin: Records.\n" +
                        "Monastic records, pre-royal records, references to a relic sealed by ascetics in the northern heights."
                ));
                actions.add(CutsceneAction.dialogue(
                        "Seren: Convenient.\n" +
                        "That sounds very close to our monastery problem."
                ));
                actions.add(CutsceneAction.dialogue(
                        "Eldrin: Your monastery problem?\n" +
                        "Then you are already involved whether you understand it or not."
                ));
                actions.add(CutsceneAction.dialogue(
                        "Kael: Then explain."
                ));
                actions.add(CutsceneAction.dialogue(
                        "Eldrin: Not here.\n" +
                        "If the thieves are still in the district, the remaining records are in danger."
                ));
                actions.add(CutsceneAction.setObjective(
                        "follow_scholar",
                        "Follow the Scholar",
                        "Investigate the academy district and learn what was stolen from the archive."
                ));
                actions.add(CutsceneAction.endCutscene());
                cutscenePlayer.start(actions);
                return;

            case "mage_join_start":
                if (!storyManager.hasFlag(StoryFlag.MAGE_INTRO_PLAYED)) return;
                if (storyManager.hasFlag(StoryFlag.MAGE_JOIN_SCENE_DONE)) return;

                actions.add(CutsceneAction.setFlag(StoryFlag.MAGE_JOIN_SCENE_DONE));
                actions.add(CutsceneAction.dialogue(
                        "Eldrin: The missing pages mentioned a name.\n" +
                        "\"First Breath.\""
                ));
                actions.add(CutsceneAction.dialogue(
                        "Kael: ..."
                ));
                actions.add(CutsceneAction.dialogue(
                        "Seren: That reaction tells me enough.\n" +
                        "So the scholar stays."
                ));
                actions.add(CutsceneAction.dialogue(
                        "Eldrin: I beg your pardon?"
                ));
                actions.add(CutsceneAction.dialogue(
                        "Seren: If people are killing over monastery records, then your research is no longer academic."
                ));
                actions.add(CutsceneAction.dialogue(
                        "Kael: You know what was taken.\n" +
                        "We know why it matters. We move together."
                ));
                actions.add(CutsceneAction.dialogue(
                        "Eldrin: Hm.\n" +
                        "Very well. But understand this: if I am correct, then what was stolen is only one part of something far older."
                ));
                actions.add(CutsceneAction.setFlag(StoryFlag.MAGE_JOINED));
                actions.add(CutsceneAction.unlockPartyMember("Mage"));
                actions.add(CutsceneAction.setObjective(
                        "prepare_for_ruins",
                        "Search the Ashen Chapel Ruins",
                        "Follow the trail described in the stolen records and discover who is searching for the First Breath."
                ));
                actions.add(CutsceneAction.endCutscene());
                cutscenePlayer.start(actions);
                return;
            
            case "post_mage_regroup":
                if (!storyManager.hasFlag(StoryFlag.MAGE_JOINED)) return;
                if (storyManager.hasFlag(StoryFlag.RUINS_OBJECTIVE_SET)) return;

                actions.add(CutsceneAction.setFlag(StoryFlag.RUINS_OBJECTIVE_SET));
                actions.add(CutsceneAction.dialogue(
                        "Eldrin: The stolen pages mentioned a sealed site beyond the town.\n" +
                        "A ruined chapel used long ago to store forbidden records."
                ));
                actions.add(CutsceneAction.dialogue(
                        "Seren: Then that is where your thieves went next."
                ));
                actions.add(CutsceneAction.dialogue(
                        "Kael: If they seek the First Breath, we cannot let them reach it first."
                ));
                actions.add(CutsceneAction.dialogue(
                        "Eldrin: The site is known in the old texts as the Ashen Chapel Ruins."
                ));
                actions.add(CutsceneAction.setObjective(
                        "go_to_ruins",
                        "Search the Ashen Chapel Ruins",
                        "Leave town and investigate the ruined chapel mentioned in the stolen academy records."
                ));
                actions.add(CutsceneAction.endCutscene());
                cutscenePlayer.start(actions);
                return;

            case "ruins_path_scene":
                if (!storyManager.hasFlag(StoryFlag.RUINS_OBJECTIVE_SET)) return;
                if (storyManager.hasFlag(StoryFlag.RUINS_PATH_SCENE_DONE)) return;

                actions.add(CutsceneAction.setFlag(StoryFlag.RUINS_PATH_SCENE_DONE));
                actions.add(CutsceneAction.dialogue(
                        "Seren: Someone passed through here recently.\n" +
                        "Three, maybe four people. Light steps. Organized."
                ));
                actions.add(CutsceneAction.dialogue(
                        "Eldrin: Then we are close."
                ));
                actions.add(CutsceneAction.dialogue(
                        "Kael: Stay alert.\n" +
                        "If they came for the same relic, they may still be inside."
                ));
                actions.add(CutsceneAction.endCutscene());
                cutscenePlayer.start(actions);
                return;

            case "ruins_entered":
                if (!storyManager.hasFlag(StoryFlag.RUINS_OBJECTIVE_SET)) return;
                if (storyManager.hasFlag(StoryFlag.RUINS_ENTERED)) return;

                actions.add(CutsceneAction.setFlag(StoryFlag.RUINS_ENTERED));
                actions.add(CutsceneAction.dialogue(
                        "Eldrin: These markings...\n" +
                        "They were deliberately broken."
                ));
                actions.add(CutsceneAction.dialogue(
                        "Kael: A seal?"
                ));
                actions.add(CutsceneAction.dialogue(
                        "Eldrin: Yes.\n" +
                        "And not a minor one."
                ));
                actions.add(CutsceneAction.dialogue(
                        "Seren: Then whoever came here was not merely stealing records.\n" +
                        "They were opening something."
                ));
                actions.add(CutsceneAction.setObjective(
                        "search_inner_ruins",
                        "Search the Inner Ruins",
                        "Go deeper into the Ashen Chapel and find the source of the broken seal."
                ));
                actions.add(CutsceneAction.endCutscene());
                cutscenePlayer.start(actions);
                return;

            case "demo_boss_intro":
                if (!storyManager.hasFlag(StoryFlag.RUINS_ENTERED)) return;
                if (storyManager.hasFlag(StoryFlag.DEMO_BOSS_INTRO_PLAYED)) return;

                actions.add(CutsceneAction.setFlag(StoryFlag.DEMO_BOSS_INTRO_PLAYED));
                actions.add(CutsceneAction.dialogue(
                        "Kael: The chamber... it was already opened."
                ));
                actions.add(CutsceneAction.dialogue(
                        "Eldrin: No.\n" +
                        "Not opened. Disturbed."
                ));
                actions.add(CutsceneAction.dialogue(
                        "Seren: Move."
                ));
                actions.add(CutsceneAction.dialogue(
                        "???: ...Unworthy..."
                ));
                actions.add(CutsceneAction.dialogue(
                        "Eldrin: The guardian is waking!"
                ));
                actions.add(CutsceneAction.startBattle("ashen_guardian"));
                actions.add(CutsceneAction.endCutscene());
                cutscenePlayer.start(actions);
                return;

            case "demo_ending":
                if (!storyManager.hasFlag(StoryFlag.DEMO_BOSS_DEFEATED)) return;
                if (storyManager.hasFlag(StoryFlag.DEMO_COMPLETE)) return;

                actions.add(CutsceneAction.setFlag(StoryFlag.DEMO_COMPLETE));
                actions.add(CutsceneAction.dialogue(
                        "Eldrin: Look...\n" +
                        "That mark on the altar. It matches the stolen pages."
                ));
                actions.add(CutsceneAction.dialogue(
                        "Seren: So the town was only a stop on their route."
                ));
                actions.add(CutsceneAction.dialogue(
                        "Kael: Then this is larger than the monastery."
                ));
                actions.add(CutsceneAction.dialogue(
                        "Eldrin: Much larger.\n" +
                        "If the First Breath is only one fragment, then others may already be in danger."
                ));
                actions.add(CutsceneAction.dialogue(
                        "Kael: Then we move before they do."
                ));
                actions.add(CutsceneAction.dialogue(
                        "Demo Complete\n" +
                        "The hunt for the remaining fragments will continue..."
                ));
                actions.add(CutsceneAction.endCutscene());
                cutscenePlayer.start(actions);
                return;
        }
    }

    public void startStoryBattle(String battleId) {
        ArrayList<Enemy> enemiesForBattle = new ArrayList<>();

        if (battleId == null || battleId.isEmpty()) return;

        switch (battleId) {
            case "ashen_guardian":
                Enemy boss = EnemyFactory.createEnemy(this, "ashen_guardian");
                if (boss != null) {
                    enemiesForBattle.add(boss);
                    forcedBattleMusic = "boss_battle";
                }
                break;
        }

        if (enemiesForBattle.isEmpty()) return;

        activeStoryBattleId = battleId;
        pendingEncounterEnemies.clear();
        pendingEncounterEnemies.addAll(enemiesForBattle);

        battleStarting = true;
        startBattleWithTransition(enemiesForBattle);

        if (enemiesForBattle.size() == 1) {
            battleMessage = "Εμφανίστηκε " + enemiesForBattle.get(0).name + "!";
        } else {
            battleMessage = "Εμφανίστηκαν εχθροί!";
        }

        sound.stopMusic();
        sound.playBattleSE("ENEMY_APPEAR");
        waitingForBattleMusic = true;
        battleAppearTimer = 0;
        battleAppearDurationFrames = (int)(sound.getBattleSoundLengthMs("ENEMY_APPEAR") / 16.67);
        battleAppearDurationFrames -= 200;

        if (battleAppearDurationFrames <= 0) battleAppearDurationFrames = 40;
    }

    public PartyMember findPartyMemberByClassName(String className) {
        for (PartyMember member : allPartyMembers) {
            if (member.className.equalsIgnoreCase(className)) {
                return member;
            }
        }
        return null;
    }

    public void unlockPartyMember(String className) {
        PartyMember member = findPartyMemberByClassName(className);
        if (member == null) return;

        if (!member.joinedParty) {
            member.joinedParty = true;

            if (!activePartyMembers.contains(member)) {
                activePartyMembers.add(member);
            }

            // βάλ’ τον πίσω από τον player όταν μπαίνει
            if (activePartyMembers.size() == 1) {
                member.worldX = player.worldX - tileSize;
                member.worldY = player.worldY;
            } else {
                member.worldX = player.worldX - tileSize * activePartyMembers.size();
                member.worldY = player.worldY;
            }

            System.out.println(className + " joined the party.");
        }
    }
    
    private BufferedImage getPartyPortraitForMember(PartyMember pm) {
        if (pm == null) return null;

        if (pm.className.equalsIgnoreCase("Assassin")) {
            return assassinPortrait32;
        }
        if (pm.className.equalsIgnoreCase("Mage")) {
            return magePortrait32;
        }

        return null;
    }

    private void spawnTiledChests(int mapIndex) {
        ArrayList<TiledObjectData> chestObjects = tileM.getMapObjectsByLayer(mapIndex, "chests");

        for (TiledObjectData obj : chestObjects) {

            int col = (int)(obj.x / originalTileSize);
            int row = (int)(obj.y / originalTileSize) - 1;

            int worldX = col * tileSize;
            int worldY = row * tileSize;

            String chestId = obj.getProperty("chestId");

            // νέο multi-item format
            String itemsCsv = obj.getProperty("items");
            String amountsCsv = obj.getProperty("amounts");

            // παλιό single-item fallback
            String singleItemId = obj.getProperty("item");
            int singleAmount = obj.getPropertyInt("amount", 1);

            ArrayList<String> rewardItemIds;
            ArrayList<Integer> rewardAmounts;

            // Αν υπάρχει νέο multi-item property, χρησιμοποίησέ το
            if (itemsCsv != null && !itemsCsv.trim().isEmpty()) {
                rewardItemIds = parseChestItemIds(itemsCsv);
                rewardAmounts = parseChestAmounts(amountsCsv, rewardItemIds.size());
            }
            // Αλλιώς χρησιμοποίησε το παλιό single-item format
            else {
                rewardItemIds = new ArrayList<>();
                rewardAmounts = new ArrayList<>();

                if (singleItemId != null && !singleItemId.trim().isEmpty()) {
                    rewardItemIds.add(singleItemId.trim());
                    rewardAmounts.add(singleAmount);
                }
            }

            Chest chest = new Chest(chestId, worldX, worldY, rewardItemIds, rewardAmounts);

            if (chestId != null && !chestId.isEmpty() && openedChestIds.contains(chestId)) {
                chest.opened = true;
            }

            chests.get(mapIndex).add(chest);

            System.out.println("Chest spawned with rewards: " + rewardItemIds);
        }
    }

    private ArrayList<String> parseChestItemIds(String csv) {
        ArrayList<String> result = new ArrayList<>();

        if (csv == null || csv.trim().isEmpty()) return result;

        String[] parts = csv.split(",");
        for (String part : parts) {
            String value = part.trim();
            if (!value.isEmpty()) {
                result.add(value);
            }
        }

        return result;
    }

    private ArrayList<Integer> parseChestAmounts(String csv, int expectedSize) {
        ArrayList<Integer> result = new ArrayList<>();

        if (csv != null && !csv.trim().isEmpty()) {
            String[] parts = csv.split(",");
            for (String part : parts) {
                try {
                    result.add(Integer.parseInt(part.trim()));
                } catch (Exception e) {
                    result.add(1);
                }
            }
        }

        while (result.size() < expectedSize) {
            result.add(1);
        }

        return result;
    }

    private Item createItemFromId(String itemId) throws Exception {
        return ItemFactory.createById(itemId);
    }

    private void handleNPCInteraction(ArrayList<Entity> currentMapNPCs) {
        for (Entity npc : currentMapNPCs) {
            int distanceX = Math.abs(player.worldX - npc.worldX);
            int distanceY = Math.abs(player.worldY - npc.worldY);

            if (npc.hidden) continue;

            if (!npc.requiredStoryFlag.isEmpty() && !storyManager.hasFlag(npc.requiredStoryFlag)) {
                continue;
            }

            if (!npc.forbiddenStoryFlag.isEmpty() && storyManager.hasFlag(npc.forbiddenStoryFlag)) {
                continue;
            }

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

    //============================
    //     CHEST HELPERS
    // ===========================

    private void handleChestInteraction() {
        ArrayList<Chest> currentChests = chests.get(currentMap);

        for (Chest chest : currentChests) {

            int distanceX = Math.abs(player.worldX - chest.worldX);
            int distanceY = Math.abs(player.worldY - chest.worldY);

            if (distanceX <= tileSize && distanceY <= tileSize) {

                if (!chest.opened) {

                chest.opened = true;

                try {
                    ArrayList<Item> rewards = new ArrayList<>();
                    ArrayList<Integer> rewardAmounts = new ArrayList<>();

                    for (int i = 0; i < chest.itemIds.size(); i++) {
                        String rewardItemId = chest.itemIds.get(i);

                        int rewardAmount = 1;
                        if (i < chest.amounts.size()) {
                            rewardAmount = chest.amounts.get(i);
                        }

                        Item item = createItemFromId(rewardItemId);
                        if (item == null) continue;

                        item.amount = rewardAmount;
                        inventory.addItem(item);

                        rewards.add(item);
                        rewardAmounts.add(rewardAmount);
                    }

                    if (chest.chestId != null && !chest.chestId.isEmpty()) {
                        openedChestIds.add(chest.chestId);
                    }
                    

                    sound.playBattleSE("item");
                    //playSound("item");

                    openChestLootWindow(rewards, rewardAmounts);

                } catch (Exception e) {
                    e.printStackTrace();
                }

                return;
            }
            }
        }
    }

    private void saveOpenedChests() {
        try {
            java.io.PrintWriter writer = new java.io.PrintWriter(new java.io.FileWriter("res/save/opened_chests.txt"));

            for (String chestId : openedChestIds) {
                writer.println(chestId);
            }

            writer.close();
            System.out.println("Opened chests saved.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadOpenedChests() {
        openedChestIds.clear();

        java.io.File file = new java.io.File("res/save/opened_chests.txt");
        if (!file.exists()) return;

        try {
            java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(file));
            String line;

            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    openedChestIds.add(line);
                }
            }

            br.close();
            System.out.println("Opened chests loaded.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void openChestLootWindow(ArrayList<Item> rewards, ArrayList<Integer> amounts) {
        chestLootItems.clear();
        chestLootAmounts.clear();

        if (rewards != null) {
            chestLootItems.addAll(rewards);
        }

        if (amounts != null) {
            chestLootAmounts.addAll(amounts);
        }

        chestLootSelectedIndex = 0;
        gameState = chestLootState;
    }

    //============================
    //    END OF CHEST HELPERS
    // ===========================

    // ===========================
    //     SAVE/LOAD HELPERS
    // ===========================

    private void saveGame() {
        savePlayerState();
        saveInventoryAndGold();
        saveQuests();
        savePartyStats();
        saveOpenedChests();
        saveEquipment();
        storyManager.save();
        System.out.println("Manual save completed.");
    }

    private void savePlayerState() {
        try {
            java.io.File saveDir = new java.io.File("res/save");
            if (!saveDir.exists()) {
                saveDir.mkdirs();
            }

            java.io.PrintWriter writer = new java.io.PrintWriter(
                    new java.io.FileWriter("res/save/player_state.txt")
            );

            writer.println("currentMap=" + currentMap);
            writer.println("worldX=" + player.worldX);
            writer.println("worldY=" + player.worldY);

            writer.close();
            System.out.println("Player state saved.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean loadPlayerState() {
        java.io.File file = new java.io.File("res/save/player_state.txt");
        if (!file.exists()) return false;

        int loadedMap = 0;
        int loadedWorldX = -1;
        int loadedWorldY = -1;

        try {
            java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(file));
            String line;

            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || !line.contains("=")) continue;

                String[] parts = line.split("=", 2);
                String key = parts[0].trim();
                String value = parts[1].trim();

                switch (key) {
                    case "currentMap":
                        loadedMap = Integer.parseInt(value);
                        break;
                    case "worldX":
                        loadedWorldX = Integer.parseInt(value);
                        break;
                    case "worldY":
                        loadedWorldY = Integer.parseInt(value);
                        break;
                }
            }

            br.close();

            if (loadedMap >= 0 && loadedMap < maxMaps) {
                currentMap = loadedMap;
                tileM.applyMapSizeToGamePanel(currentMap);
            } else {
                currentMap = 0;
                tileM.applyMapSizeToGamePanel(currentMap);
            }

            if (loadedWorldX >= 0 && loadedWorldY >= 0) {
                player.worldX = loadedWorldX;
                player.worldY = loadedWorldY;
            } else {
                return false;
            }

            System.out.println("Player state loaded.");
            return true;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    private void saveInventoryAndGold() {
        try {
            java.io.File saveDir = new java.io.File("res/save");
            if (!saveDir.exists()) {
                saveDir.mkdirs();
            }

            java.io.PrintWriter writer = new java.io.PrintWriter(
                    new java.io.FileWriter("res/save/inventory.txt")
            );

            writer.println("gold=" + player.gold);

            // =========================
            // STORAGE ITEMS
            // =========================
            for (int i = 0; i < inventory.storage.length; i++) {
                Item item = inventory.storage[i];
                if (item != null) {
                    int amount = item.stackable ? item.amount : 1;
                    writer.println("STORAGE|" + item.name + "|" + amount);
                }
            }

            // =========================
            // KEY ITEMS
            // =========================
            for (int i = 0; i < inventory.keyItems.length; i++) {
                Item item = inventory.keyItems[i];
                if (item != null) {
                    writer.println("KEY|" + item.name + "|1");
                }
            }

            writer.close();
            System.out.println("Inventory and gold saved.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadInventoryAndGold() {
        java.io.File file = new java.io.File("res/save/inventory.txt");
        if (!file.exists()) return;

        // καθάρισε inventory πριν το ξαναγεμίσεις
        for (int i = 0; i < inventory.storage.length; i++) {
            inventory.storage[i] = null;
        }

        for (int i = 0; i < inventory.keyItems.length; i++) {
            inventory.keyItems[i] = null;
        }

        try {
            java.io.BufferedReader br = new java.io.BufferedReader(
                    new java.io.FileReader(file)
            );

            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                if (line.startsWith("gold=")) {
                    player.gold = Integer.parseInt(line.substring("gold=".length()));
                    continue;
                }

                String[] parts = line.split("\\|");
                if (parts.length < 3) continue;

                String itemType = parts[0].trim();   // STORAGE ή KEY
                String itemName = parts[1].trim();
                int amount = Integer.parseInt(parts[2].trim());

                Item item = createItemFromSaveName(itemName);
                if (item == null) continue;

                if (item.stackable) {
                    item.amount = amount;
                }

                if (itemType.equalsIgnoreCase("KEY")) {
                    item.isKeyItem = true;
                }

                inventory.addItem(item);
            }

            br.close();
            System.out.println("Inventory and gold loaded.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Item createItemFromSaveName(String itemName) throws Exception {
        return ItemFactory.createBySaveName(itemName);
    }

    private void saveQuests() {
        try {
            java.io.File saveDir = new java.io.File("res/save");
            if (!saveDir.exists()) {
                saveDir.mkdirs();
            }

            java.io.PrintWriter writer = new java.io.PrintWriter(
                    new java.io.FileWriter("res/save/quests.txt")
            );

            for (Quest q : player.quests) {
                writer.println(q.name + "|" + q.completed);
            }

            writer.close();
            System.out.println("Quests saved.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadQuests() {
        java.io.File file = new java.io.File("res/save/quests.txt");
        if (!file.exists()) return;

        player.quests.clear();

        try {
            java.io.BufferedReader br = new java.io.BufferedReader(
                    new java.io.FileReader(file)
            );

            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split("\\|");
                if (parts.length < 2) continue;

                String questName = parts[0].trim();
                boolean completed = Boolean.parseBoolean(parts[1].trim());

                Quest q = new Quest(questName, "");
                q.completed = completed;
                player.quests.add(q);
            }

            br.close();
            System.out.println("Quests loaded.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean inventorySaveExists() {
        return new java.io.File("res/save/inventory.txt").exists();
    }

    private void writeEntityStats(java.io.PrintWriter writer, String prefix, Entity e) {
        int attackBonus = getTotalEquippedAttackBonus(e);
        int defenseBonus = getTotalEquippedDefenseBonus(e);
        int magicBonus = getTotalEquippedMagicBonus(e);
        int hpBonus = getTotalEquippedHpBonus(e);
        int mpBonus = getTotalEquippedMpBonus(e);
        int speedBonus = getTotalEquippedSpeedBonus(e);

        int baseHp = e.hp - hpBonus;
        int baseMp = e.mp - mpBonus;
        int baseMaxHp = e.maxHp - hpBonus;
        int baseMaxMp = e.maxMp - mpBonus;
        int baseAttack = e.attack - attackBonus;
        int baseDefense = e.defense - defenseBonus;
        int baseMagicAttack = e.magicAttack - magicBonus;
        int baseSpeed = e.speed_stat - speedBonus;

        if (baseHp < 0) baseHp = 0;
        if (baseMp < 0) baseMp = 0;
        if (baseMaxHp < 1) baseMaxHp = 1;
        if (baseMaxMp < 0) baseMaxMp = 0;

        writer.println(prefix + ".hp=" + baseHp);
        writer.println(prefix + ".mp=" + baseMp);
        writer.println(prefix + ".maxHp=" + baseMaxHp);
        writer.println(prefix + ".maxMp=" + baseMaxMp);
        writer.println(prefix + ".attack=" + baseAttack);
        writer.println(prefix + ".defense=" + baseDefense);
        writer.println(prefix + ".magicAttack=" + baseMagicAttack);
        writer.println(prefix + ".speed=" + baseSpeed);
        writer.println(prefix + ".level=" + e.level);
        writer.println(prefix + ".exp=" + e.exp);
    }

    private void savePartyStats() {
        try {
            java.io.File saveDir = new java.io.File("res/save");
            if (!saveDir.exists()) {
                saveDir.mkdirs();
            }

            java.io.PrintWriter writer = new java.io.PrintWriter(
                    new java.io.FileWriter("res/save/party_stats.txt")
            );

            // main player
            writeEntityStats(writer, "player", player);

            // party members
            writer.println("party.count=" + activePartyMembers.size());

            for (int i = 0; i < activePartyMembers.size(); i++) {
                PartyMember member = activePartyMembers.get(i);
                writer.println("party." + i + ".className=" + member.className);
                writeEntityStats(writer, "party." + i, member);
            }

            writer.close();
            System.out.println("Party stats saved.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadPartyStats() {
        java.io.File file = new java.io.File("res/save/party_stats.txt");
        if (!file.exists()) return;

        java.util.HashMap<String, String> data = new java.util.HashMap<>();

        try {
            java.io.BufferedReader br = new java.io.BufferedReader(
                    new java.io.FileReader(file)
            );

            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || !line.contains("=")) continue;

                String[] parts = line.split("=", 2);
                data.put(parts[0].trim(), parts[1].trim());
            }

            br.close();

            // main player
            if (data.containsKey("player.hp")) player.hp = Integer.parseInt(data.get("player.hp"));
            if (data.containsKey("player.mp")) player.mp = Integer.parseInt(data.get("player.mp"));
            if (data.containsKey("player.maxHp")) player.maxHp = Integer.parseInt(data.get("player.maxHp"));
            if (data.containsKey("player.maxMp")) player.maxMp = Integer.parseInt(data.get("player.maxMp"));
            if (data.containsKey("player.attack")) player.attack = Integer.parseInt(data.get("player.attack"));
            if (data.containsKey("player.defense")) player.defense = Integer.parseInt(data.get("player.defense"));
            if (data.containsKey("player.magicAttack")) player.magicAttack = Integer.parseInt(data.get("player.magicAttack"));
            if (data.containsKey("player.speed")) player.speed_stat = Integer.parseInt(data.get("player.speed"));
            if (data.containsKey("player.level")) player.level = Integer.parseInt(data.get("player.level"));
            if (data.containsKey("player.exp")) player.exp = Integer.parseInt(data.get("player.exp"));

            // party members
            for (int i = 0; i < activePartyMembers.size(); i++) {
                PartyMember member = activePartyMembers.get(i);

                String base = "party." + i;

                if (data.containsKey(base + ".hp")) member.hp = Integer.parseInt(data.get(base + ".hp"));
                if (data.containsKey(base + ".mp")) member.mp = Integer.parseInt(data.get(base + ".mp"));
                if (data.containsKey(base + ".maxHp")) member.maxHp = Integer.parseInt(data.get(base + ".maxHp"));
                if (data.containsKey(base + ".maxMp")) member.maxMp = Integer.parseInt(data.get(base + ".maxMp"));
                if (data.containsKey(base + ".attack")) member.attack = Integer.parseInt(data.get(base + ".attack"));
                if (data.containsKey(base + ".defense")) member.defense = Integer.parseInt(data.get(base + ".defense"));
                if (data.containsKey(base + ".magicAttack")) member.magicAttack = Integer.parseInt(data.get(base + ".magicAttack"));
                if (data.containsKey(base + ".speed")) member.speed_stat = Integer.parseInt(data.get(base + ".speed"));
                if (data.containsKey(base + ".level")) member.level = Integer.parseInt(data.get(base + ".level"));
                if (data.containsKey(base + ".exp")) member.exp = Integer.parseInt(data.get(base + ".exp"));
            }

            System.out.println("Party stats loaded.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveEquipment() {
        try {
            java.io.File saveDir = new java.io.File("res/save");
            if (!saveDir.exists()) {
                saveDir.mkdirs();
            }

            java.io.PrintWriter writer = new java.io.PrintWriter(
                    new java.io.FileWriter("res/save/equipment.txt")
            );

            // 0 = Hero
            for (int slot = 0; slot < 9; slot++) {
                Item item = getCharacterEquipSlot(player, slot);
                if (item != null) {
                    writer.println("0|" + slot + "|" + item.name);
                }
            }

            // 1.. = party members
            for (int p = 0; p < activePartyMembers.size(); p++) {
                PartyMember member = activePartyMembers.get(p);

                for (int slot = 0; slot < 9; slot++) {
                    Item item = getCharacterEquipSlot(member, slot);
                    if (item != null) {
                        writer.println((p + 1) + "|" + slot + "|" + item.name);
                    }
                }
            }

            writer.close();
            System.out.println("Equipment saved.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadEquipment() {
        java.io.File file = new java.io.File("res/save/equipment.txt");
        if (!file.exists()) return;

        try {
            java.io.BufferedReader br = new java.io.BufferedReader(
                    new java.io.FileReader(file)
            );

            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || !line.contains("|")) continue;

                String[] parts = line.split("\\|");
                if (parts.length < 3) continue;

                int ownerIndex = Integer.parseInt(parts[0]);
                int slot = Integer.parseInt(parts[1]);
                String itemName = parts[2];

                Item item = createItemFromSaveName(itemName);
                if (item == null) continue;

                Entity targetCharacter;

                if (ownerIndex == 0) {
                    targetCharacter = player;
                } else {
                    int partyIndex = ownerIndex - 1;
                    if (partyIndex < 0 || partyIndex >= activePartyMembers.size()) continue;
                    targetCharacter = activePartyMembers.get(partyIndex);
                }

                setCharacterEquipSlot(targetCharacter, slot, item);
                applyEquipBonuses(targetCharacter, item);
            }

            br.close();
            System.out.println("Equipment loaded.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private BufferedImage loadImageSafe(String path) {
        try {
            BufferedImage img = ImageIO.read(new File(path));
            if (img == null) {
                System.out.println("WARNING: Image not found or unsupported format: " + path);
            }
            return img;
        } catch (IOException e) {
            System.out.println("ERROR loading image: " + path + " - " + e.getMessage());
            return null;
        }
    }

    // ====================================
    //     END OF SAVE/LOAD HELPERS
    // ====================================

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
    // TILED ENCOUNTER HELPERS
    // =============================

    private String[] parseCsv(String csv) {
        if (csv == null || csv.trim().isEmpty()) return new String[0];

        String[] parts = csv.split(",");
        for (int i = 0; i < parts.length; i++) {
            parts[i] = parts[i].trim();
        }
        return parts;
    }

    private int[] parseIntCsv(String csv) {
        String[] parts = parseCsv(csv);
        int[] result = new int[parts.length];

        for (int i = 0; i < parts.length; i++) {
            try {
                result[i] = Integer.parseInt(parts[i]);
            } catch (Exception e) {
                result[i] = 1;
            }
        }

        return result;
    }

    private String getObjectPropertyOrDefault(TiledObjectData obj, String key, String defaultValue) {
        if (obj == null) return defaultValue;
        String value = obj.getProperty(key);
        return (value == null || value.trim().isEmpty()) ? defaultValue : value.trim();
    }

    private int getObjectIntPropertyOrDefault(TiledObjectData obj, String key, int defaultValue) {
        try {
            return Integer.parseInt(getObjectPropertyOrDefault(obj, key, String.valueOf(defaultValue)));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private String chooseWeightedEnemy(String[] enemyIds, int[] weights) {
        if (enemyIds.length == 0) return null;

        if (weights == null || weights.length != enemyIds.length) {
            return enemyIds[(int)(Math.random() * enemyIds.length)];
        }

        int total = 0;
        for (int w : weights) {
            total += Math.max(0, w);
        }

        if (total <= 0) {
            return enemyIds[(int)(Math.random() * enemyIds.length)];
        }

        int roll = (int)(Math.random() * total);
        int cumulative = 0;

        for (int i = 0; i < enemyIds.length; i++) {
            cumulative += Math.max(0, weights[i]);
            if (roll < cumulative) {
                return enemyIds[i];
            }
        }

        return enemyIds[enemyIds.length - 1];
    }

    private ArrayList<Enemy> buildEncounterFromZone(TiledObjectData zone) {
        ArrayList<Enemy> result = new ArrayList<>();
        if (zone == null) return result;

        String enemiesCsv = getObjectPropertyOrDefault(zone, "enemies", "");
        String[] enemyIds = parseCsv(enemiesCsv);

        if (enemyIds.length == 0) {
            System.out.println("Encounter zone has no enemies property.");
            return result;
        }

        int minGroup = getObjectIntPropertyOrDefault(zone, "minGroup", 1);
        int maxGroup = getObjectIntPropertyOrDefault(zone, "maxGroup", minGroup);

        if (maxGroup < minGroup) maxGroup = minGroup;

        int groupSize = minGroup;
        if (maxGroup > minGroup) {
            groupSize = minGroup + (int)(Math.random() * (maxGroup - minGroup + 1));
        }

        int[] weights = parseIntCsv(getObjectPropertyOrDefault(zone, "weights", ""));

        for (int i = 0; i < groupSize; i++) {
            String chosenId = chooseWeightedEnemy(enemyIds, weights);
            Enemy enemy = EnemyFactory.createEnemy(this, chosenId);

            if (enemy != null) {
                result.add(enemy);
            }
        }

        return result;
    }

    // =============================
    //  END OF HELPERS FOR TILED
    // ============================

    public void startRandomEncounter() {
        TiledObjectData encounterZone = getCurrentEncounterZone();
        if (encounterZone == null) return;

        ArrayList<Enemy> encounterEnemies = buildEncounterFromZone(encounterZone);
        if (encounterEnemies.isEmpty()) return;

        battleStarting = true;
        startBattleWithTransition(encounterEnemies);

        if (encounterEnemies.size() == 1) {
            battleMessage = "Εμφανίστηκε " + encounterEnemies.get(0).name + "!";
        } else {
            battleMessage = "Εμφανίστηκαν εχθροί!";
        }

        sound.stopMusic();
        sound.playBattleSE("ENEMY_APPEAR");
        waitingForBattleMusic = true;
        battleAppearTimer = 0;
        battleAppearDurationFrames = (int)(sound.getBattleSoundLengthMs("ENEMY_APPEAR") / 16.67);
        battleAppearDurationFrames -= 200;

        if (battleAppearDurationFrames <= 0) battleAppearDurationFrames = 40;
    }

    public void startBattleWithTransition(ArrayList<Enemy> enemiesToBattle) {
        if (enemiesToBattle == null || enemiesToBattle.isEmpty()) return;

        currentEnemy = enemiesToBattle.get(0);
        pendingEncounterEnemies = new ArrayList<>(enemiesToBattle);

        battleFadeOut = true;
        battleFadeAlpha = 0;
        battleFadeIn = false;

        try {
            groundGrass = ImageIO.read(new File("res/battle/ground_grass.png"));
            groundDungeon = ImageIO.read(new File("res/battle/ground_dungeon.png"));
            groundRuins = ImageIO.read(new File("res/battle/ground_dungeon.png"));
            System.out.println("Ground images loaded successfully!");
        } catch (IOException e) {
            e.printStackTrace();
            groundGrass = createGroundGradient(new Color(34, 139, 34), new Color(144, 238, 144));
            groundDungeon = createGroundGradient(new Color(70, 70, 70), new Color(120, 70, 70));
            groundRuins = createGroundGradient(new Color(90, 80, 70), new Color(140, 120, 100));
        }

        createBattleBackground();
    }

    // ===================================
    //    LEGACY METHOD
    // ===================================
    // Νέα μέθοδος για έναρξη μάχης με transition
    // public void startBattleWithTransition(Enemy enemy) {
    //     currentEnemy = enemy;

    //     // Ξεκίνα fade out
    //     battleFadeOut = true;
    //     battleFadeAlpha = 0;
    //     battleFadeIn = false;

    //     try {
    //         groundGrass = ImageIO.read(new File("res/battle/ground_grass.png"));
    //         groundDungeon = ImageIO.read(new File("res/battle/ground_dungeon.png"));
    //         System.out.println("Ground images loaded successfully!");
    //     } catch (IOException e) {
    //         e.printStackTrace();
    //         // Fallback: δημιούργησε gradient images
    //         groundGrass = createGroundGradient(new Color(34, 139, 34), new Color(144, 238, 144));
    //         groundDungeon = createGroundGradient(new Color(70, 70, 70), new Color(120, 70, 70));
    //     }

        
    //     // Δημιούργησε τυχαίο background για τη μάχη
    //     createBattleBackground();
        
    //     // Ρύθμισε τους εχθρούς και τους παίκτες για οριζόντια μάχη
    //     //setupBattleEntities();
    // }

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
        String battleTheme = "";
        if (currentEnemy != null && currentEnemy.battleBg != null) {
            battleTheme = currentEnemy.battleBg.toLowerCase();
        }

        String bgName;

        if (battleTheme.equals("ruins")) {
            bgName = "ruins1";
        } else if (battleTheme.equals("dungeon")) {
            String[] possibleBackgrounds = {"dungeon1", "dungeon2"};
            bgName = possibleBackgrounds[(int)(Math.random() * possibleBackgrounds.length)];
        } else {
            String[] possibleBackgrounds = {"field1"};
            bgName = possibleBackgrounds[(int)(Math.random() * possibleBackgrounds.length)];
        }

        try {
            battleBackground = ImageIO.read(new File("res/battle/" + bgName + ".png"));
            System.out.println("Battle background loaded: " + bgName);
        } catch (IOException e) {
            e.printStackTrace();
            createGradientBackground();
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
        if (currentEnemy != null && currentEnemy.groundType != null) {
            String groundTheme = currentEnemy.groundType.toLowerCase();

            if (groundTheme.equals("ruins")) {
                groundImage = groundRuins;
            } else if (groundTheme.equals("dungeon")) {
                groundImage = groundDungeon;
            } else {
                groundImage = groundGrass;
            }
        } else {
            groundImage = groundGrass;
        }
        
        // Υπολόγισε τη θέση του εδάφους
        groundY = screenHeight - groundHeight - tileSize - 100;
        groundX = 0;
        
        // ========== ΔΗΜΙΟΥΡΓΙΑ ΕΧΘΡΩΝ (2-3) ==========
        if (pendingEncounterEnemies == null || pendingEncounterEnemies.isEmpty()) return;

        for (int i = 0; i < pendingEncounterEnemies.size(); i++) {
            Enemy enemy = pendingEncounterEnemies.get(i);

            BattleEnemy be = new BattleEnemy(enemy);

            be.x = -tileSize * 4;
            be.y = groundY - tileSize - (i * 120);
            be.targetX = tileSize / 2 - 60;
            be.targetY = groundY - tileSize - (i * 120);

            be.playAnimation("idle");
            battleEnemies.add(be);

            BattleEntity enemyEntity = new BattleEntity(enemy, enemy.currentImage);
            setupEnemyBreakData(enemyEntity);
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
        
        Point heroRest = getHeroRestBattlePosition();

        bp.x = screenWidth + tileSize * 4;
        bp.y = heroRest.y;
        bp.targetX = heroRest.x;
        bp.targetY = heroRest.y;
        
        battlePlayers.add(bp);
        
        // ========== ΔΗΜΙΟΥΡΓΙΑ ΤΩΝ ΑΛΛΩΝ ΜΕΛΩΝ ==========
        for (int i = 0; i < activePartyMembers.size(); i++) {
            PartyMember member = activePartyMembers.get(i);
            BattlePartyMember bpm = new BattlePartyMember(member);

            bpm.playAnimation("idle");
            
            // Τοποθέτηση σε κατακόρυφη σειρά (πιο πάνω από τον κεντρικό)
            Point memberRest = getPartyMemberRestBattlePosition(i);

            bpm.x = screenWidth + tileSize * 4;
            bpm.y = memberRest.y;
            bpm.targetX = memberRest.x;
            bpm.targetY = memberRest.y;
            
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
        for (int i = 0; i < activePartyMembers.size(); i++) {
            PartyMember member = activePartyMembers.get(i);
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
        
        // Υπολόγισε τη σειρά σειράς με βάση το speed
        battleParty.calculateTurnOrder();
        battleParty.startNewRoundBP();

        // Χτίσε το πρώτο NEXT TURN preview
        rebuildNextTurnPreview();
        
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

    private Point getHeroRestBattlePosition() {
        int playerSpriteSize = tileSize * 4;
        int x = BATTLE_PLAYER_REST_X;
        int y = groundY - playerSpriteSize + BATTLE_PLAYER_REST_Y_OFFSET;
        return new Point(x, y);
    }

    private Point getPartyMemberRestBattlePosition(int index) {
        int playerSpriteSize = tileSize * 4;

        int baseX = BATTLE_PLAYER_REST_X;
        int baseY = groundY - playerSpriteSize + BATTLE_PLAYER_REST_Y_OFFSET;

        int offsetX = -60 - (index * 20);
        int offsetY = -30 - (index * 40);

        return new Point(baseX + offsetX, baseY + offsetY);
    }

    private Point getActiveFrontBattlePosition() {
        int playerSpriteSize = tileSize * 4;
        int x = BATTLE_FRONT_ACTOR_X;
        int y = groundY - playerSpriteSize + BATTLE_FRONT_ACTOR_Y_OFFSET;
        return new Point(x, y);
    }

    private void updateBattleFormationTargets() {
        BattleEntity currentTurn = battleParty.getCurrentTurn();
        Point activePos = getActiveFrontBattlePosition();

        // ===== HERO =====
        if (!battlePlayers.isEmpty()) {
            BattlePlayer bp = battlePlayers.get(0);
            Point heroRest = getHeroRestBattlePosition();

            bp.targetX = heroRest.x;
            bp.targetY = heroRest.y;

            if (currentTurn != null && currentTurn.isPlayer && currentTurn.name.equals("Hero")) {
                bp.targetX = activePos.x;
                bp.targetY = activePos.y;
            }
        }

        // ===== PARTY MEMBERS =====
        for (int i = 0; i < battlePartyMembers.size(); i++) {
            BattlePartyMember bpm = battlePartyMembers.get(i);
            Point restPos = getPartyMemberRestBattlePosition(i);

            bpm.targetX = restPos.x;
            bpm.targetY = restPos.y;

            if (currentTurn != null && currentTurn.isPlayer &&
                currentTurn.name.equals(bpm.member.className)) {
                bpm.targetX = activePos.x;
                bpm.targetY = activePos.y;
            }
        }
    }

    private void animateBattleFormation() {
        // HERO
        if (!battlePlayers.isEmpty()) {
            BattlePlayer bp = battlePlayers.get(0);
            BattleEntity heroEntity = null;

            for (BattleEntity entity : battleParty.party) {
                if (entity.name.equals("Hero")) {
                    heroEntity = entity;
                    break;
                }
            }

            bp.x += (bp.targetX - bp.x) * BATTLE_FORMATION_LERP;
            bp.y += (bp.targetY - bp.y) * BATTLE_FORMATION_LERP;

            if (Math.abs(bp.x - bp.targetX) < 1.0) bp.x = bp.targetX;
            if (Math.abs(bp.y - bp.targetY) < 1.0) bp.y = bp.targetY;

            updateBattlePlayerMovementAnimation(heroEntity, bp.x, bp.targetX);
        }

        // PARTY MEMBERS
        for (BattlePartyMember bpm : battlePartyMembers) {
            BattleEntity memberEntity = null;

            for (BattleEntity entity : battleParty.party) {
                if (entity.name.equals(bpm.member.className)) {
                    memberEntity = entity;
                    break;
                }
            }

            bpm.x += (bpm.targetX - bpm.x) * BATTLE_FORMATION_LERP;
            bpm.y += (bpm.targetY - bpm.y) * BATTLE_FORMATION_LERP;

            if (Math.abs(bpm.x - bpm.targetX) < 1.0) bpm.x = bpm.targetX;
            if (Math.abs(bpm.y - bpm.targetY) < 1.0) bpm.y = bpm.targetY;

            updateBattlePlayerMovementAnimation(memberEntity, bpm.x, bpm.targetX);
        }
    }

    public void updateBattleVisuals() {
        updateBattleFormationTargets();
        animateBattleFormation();

        for (BattlePlayer bp : battlePlayers) {
            bp.update();
        }

        for (BattlePartyMember bpm : battlePartyMembers) {
            bpm.update();
        }

        for (BattleEnemy be : battleEnemies) {
            be.update();
        }

        updateBattleEffects();
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
                
                actor.multiHitCount = 1 + actor.boostUsed;
                actor.currentHitIndex = 0;
                
                // Πρώτο hit
                executeSingleHit(actor, actor.queuedTarget);
                actor.currentHitIndex = 1;
                
                playPlayerAttackSound(actor);
                triggerLunge(actor, actor.boostUsed);
                triggerSwingTrail(actor, actor.boostUsed);
            }
            
            // Έλεγχος για επόμενα hits μέσα στο ΙΔΙΟ animation (νέα spritesheets)
            if (actor.strikeTriggered && actor.currentHitIndex < actor.multiHitCount) {
                int hitFrame = getHitFrameForIndex(actor, actor.currentHitIndex);
                if (hitFrame >= 0 && isActorOnFrame(actor, hitFrame)) {
                    executeSingleHit(actor, actor.queuedTarget);
                    actor.currentHitIndex++;
                    playPlayerAttackSound(actor);
                    
                    // Μικρό shake/flash για κάθε hit
                    screenShakeStrength = 2;
                    screenShakeDuration = 3;
                    screenShakeTimer = screenShakeDuration;
                    spawnBattleEffect("hit_flash", screenWidth/2, screenHeight/2, 3, 1);
                }
            }
            
            // Τέλος animation → RECOVERY
            if (isActorAnimationFinished(actor)) {
                actor.enterState(CombatState.RECOVERY);
            }
            return;
        }

        if (actor.state == CombatState.RECOVERY) {
            // Μικρό delay για να φανεί το τελευταίο frame
            actor.stateTimer++;
            if (actor.stateTimer >= 10) {
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
        if (actor.shouldSkipTurnBecauseBroken()) {
            showActionMessage(actor.name + " is broken and cannot act!");
            actor.consumeBrokenTurn();
            actor.resetTurnFlags();
            actor.enterState(CombatState.IDLE);

            actionInProgress = false;
            waitingForNextTurn = true;
            commandLocked = false;
            battleTurnDelay = 0;
            return;
        }
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
                removeDeadFromNextTurnPreview();

                actor.resetTurnFlags();
                actor.enterState(CombatState.IDLE);
                actionInProgress = false;
                waitingForNextTurn = true;
                battleTurnDelay = 0;
                commandLocked = false;
            }
        }
    }

    public String getAttackAnimationName(BattleEntity actor) {
        switch (actor.boostUsed) {
            case 0: return "attack1";
            case 1: return "attack2";
            case 2: return "attack3";
            case 3: return "attack4";
            default: return "attack1";
        }
    }

    private int getHitFrameForIndex(BattleEntity actor, int hitIndex) {
        if (actor == null) return -1;
        
        boolean isHero = actor.name.equals("Hero");
        
        if (isHero) {
            // HERO: 5 frames total, 4 ανά hit + 1 pause, 1 recovery
            switch (actor.boostUsed) {
                case 0: return -1;
                case 1: return (hitIndex == 1) ? 5 : -1;     // 2ο hit στο frame 5
                case 2:
                    if (hitIndex == 1) return 5;              // 2ο hit
                    if (hitIndex == 2) return 10;             // 3ο hit
                    return -1;
                case 3:
                    if (hitIndex == 1) return 5;              // 2ο hit
                    if (hitIndex == 2) return 10;             // 3ο hit
                    if (hitIndex == 3) return 15;             // 4ο hit
                    return -1;
                default: return -1;
            }
        } else {
            // ASSASSIN/MAGE: 9 frames total, 7 ανά hit + 1 pause, 2 recovery
            switch (actor.boostUsed) {
                case 0: return -1;
                case 1: return (hitIndex == 1) ? 8 : -1;     // 2ο hit στο frame 8
                case 2:
                    if (hitIndex == 1) return 8;              // 2ο hit
                    if (hitIndex == 2) return 16;             // 3ο hit
                    return -1;
                case 3:
                    if (hitIndex == 1) return 8;              // 2ο hit
                    if (hitIndex == 2) return 16;             // 3ο hit
                    if (hitIndex == 3) return 24;             // 4ο hit
                    return -1;
                default: return -1;
            }
        }
    }

    private boolean isActorOnFrame(BattleEntity actor, int targetFrame) {
        if (actor == null || targetFrame < 0) return false;
        
        if (actor.isPlayer) {
            if (actor.name.equals("Hero")) {
                if (!battlePlayers.isEmpty()) {
                    return battlePlayers.get(0).getCurrentFrameIndex() == targetFrame;
                }
            } else {
                for (BattlePartyMember bpm : battlePartyMembers) {
                    if (bpm.member.className.equals(actor.name)) {
                        return bpm.getCurrentFrameIndex() == targetFrame;
                    }
                }
            }
        }
        return false;
    }

    private void executeSingleHit(BattleEntity attacker, BattleEntity target) {
        if (target == null || !target.isAlive()) return;
        
        String attackType = getBasicAttackType(attacker);
        
        boolean targetWasAlreadyBroken = target.broken;
        boolean justBroken = processBreakHit(attacker, target, attackType);
        
        int damage = calculateAttackDamage(attacker, target, attacker.boostUsed);
        
        // Broken damage multiplier
        if (targetWasAlreadyBroken || justBroken) {
            damage = target.applyBrokenDamageMultiplier(damage);
        }
        
        target.takeDamage(damage);
        
        playTargetHurt(target);
        syncVisualHp(target);
        
        // Trigger hit react μόνο στο πρώτο hit
        if (attacker.currentHitIndex == 0) {
            int reactStrength = (attacker.boostUsed > 0) ? attacker.boostUsed : 1;
            target.triggerHitReact(reactStrength);
        }
        
        // Trigger slash effect
        triggerSlashImpact(target, 1);
        
        showActionMessage(attacker.name + " hits " + target.name + " for " + damage + " damage!" +
                        (attacker.multiHitCount > 1 ? " (" + (attacker.currentHitIndex + 1) + "/" + attacker.multiHitCount + ")" : ""));
        
        if (!target.isAlive()) {
            lastKillerName = attacker.name;
            playTargetDeath(target);
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
            sound.playBattleSE("GOBLIN_SLASH");
        }
    }

    // public void drawImpactSlashLines(Graphics2D g2) {
    //     if (boostBurstTimer >= boostBurstDuration || boostBurstLevel <= 0) return;

    //     Graphics2D g = (Graphics2D) g2.create();

    //     float progress = (float) boostBurstTimer / boostBurstDuration;
    //     float inv = 1.0f - progress;

    //     // ίδιο offset με το burst
    //     int impactX = boostBurstX - 15;
    //     int impactY = boostBurstY + 120;

    //     // χρώματα
    //     Color mainSlash = new Color(255, 245, 220, Math.max(0, (int)(210 * inv)));
    //     Color glowSlash = new Color(255, 190, 120, Math.max(0, (int)(130 * inv)));

    //     // μήκος/πάχος ανά boost
    //     int mainLength;
    //     float mainThickness;

    //     if (boostBurstLevel == 1) {
    //         mainLength = 55;
    //         mainThickness = 3f;
    //     } else if (boostBurstLevel == 2) {
    //         mainLength = 75;
    //         mainThickness = 5f;
    //     } else {
    //         mainLength = 95;
    //         mainThickness = 7f;
    //     }

    //     double angle = boostSlashAngle;

    //     int dx = (int)(Math.cos(angle) * mainLength);
    //     int dy = (int)(Math.sin(angle) * mainLength);

    //     int x1 = impactX - dx / 2;
    //     int y1 = impactY - dy / 2;
    //     int x2 = impactX + dx / 2;
    //     int y2 = impactY + dy / 2;

    //     // ===== glow κάτω από το main slash =====
    //     g.setStroke(new BasicStroke(mainThickness + 4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    //     g.setColor(glowSlash);
    //     g.drawLine(x1, y1, x2, y2);

    //     // ===== main slash =====
    //     g.setStroke(new BasicStroke(mainThickness, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    //     g.setColor(mainSlash);
    //     g.drawLine(x1, y1, x2, y2);

    //     // ===== δευτερεύουσες streaks =====
    //     int streakCount = 1 + boostBurstLevel; // 2,3,4 συνολικά περίπου
    //     for (int i = 0; i < streakCount; i++) {
    //         double offsetFactor = (i - streakCount / 2.0) * 12.0;
    //         double perpAngle = angle + Math.PI / 2.0;

    //         int ox = (int)(Math.cos(perpAngle) * offsetFactor);
    //         int oy = (int)(Math.sin(perpAngle) * offsetFactor);

    //         int streakLength = (int)(mainLength * (0.45 + i * 0.08));
    //         int sdx = (int)(Math.cos(angle) * streakLength);
    //         int sdy = (int)(Math.sin(angle) * streakLength);

    //         int sx1 = impactX + ox - sdx / 2;
    //         int sy1 = impactY + oy - sdy / 2;
    //         int sx2 = impactX + ox + sdx / 2;
    //         int sy2 = impactY + oy + sdy / 2;

    //         g.setStroke(new BasicStroke(Math.max(1.5f, mainThickness - 2f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    //         g.setColor(new Color(255, 255, 255, Math.max(0, (int)(140 * inv))));
    //         g.drawLine(sx1, sy1, sx2, sy2);
    //     }

    //     g.dispose();
    // }

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
            BattlePlayer bp = battlePlayers.get(0);
            int drawX = (int)Math.round(bp.x);
            int drawY = (int)Math.round(bp.y);
            return new Point(drawX + playerSpriteSize / 2, drawY + playerSpriteSize / 2);
        }

        // ===== PARTY MEMBERS =====
        for (int i = 0; i < battlePartyMembers.size(); i++) {
            BattlePartyMember bpm = battlePartyMembers.get(i);
            if (entity.name.equals(bpm.member.className)) {
                int drawX = (int)Math.round(bpm.x);
                int drawY = (int)Math.round(bpm.y);
                return new Point(drawX + playerSpriteSize / 2, drawY + playerSpriteSize / 2);
            }
        }

        return new Point(screenWidth / 2, screenHeight / 2);
    }

    private void loadBattleEffectAssets() {
        try {
            registerBattleEffect("break", "res/effects/break.png", 64, 64, 3, 2.0f);
            registerBattleEffect("slash", "res/effects/slash.png", 48, 48, 2, 2.5f);

            registerBattleEffect("boost_lv1", "res/effects/boost_lv1.png", 128, 128, 2, 1.4f);
            registerBattleEffect("boost_lv2", "res/effects/boost_lv2.png", 128, 128, 2, 1.4f);
            registerBattleEffect("boost_lv3", "res/effects/boost_lv3.png", 128, 128, 2, 1.4f);

            System.out.println("Battle effect sprite sheets loaded successfully.");
        } catch (Exception e) {
            System.out.println("Failed to load battle effect sprite sheets.");
            e.printStackTrace();
        }
    }

    private void loadBoostAuraEffects() {
        try {
            SpriteSheet boostLv1FrontSheet = new SpriteSheet("res/effects/boost_lv1_front.png", 128, 128);
            SpriteSheet boostLv1BackSheet = new SpriteSheet("res/effects/boost_lv1_back.png", 128, 128);
            SpriteSheet boostLv2FrontSheet = new SpriteSheet("res/effects/boost_lv2_front.png", 128, 128);
            SpriteSheet boostLv2BackSheet = new SpriteSheet("res/effects/boost_lv2_back.png", 128, 128);
            SpriteSheet boostLv3FrontSheet = new SpriteSheet("res/effects/boost_lv3_front.png", 128, 128);
            SpriteSheet boostLv3BackSheet = new SpriteSheet("res/effects/boost_lv3_back.png", 128, 128);

            boostAuraLv1FrontFrames = boostLv1FrontSheet.getAllFrames();
            boostAuraLv1BackFrames = boostLv1BackSheet.getAllFrames();
            boostAuraLv2FrontFrames = boostLv2FrontSheet.getAllFrames();
            boostAuraLv2BackFrames = boostLv2BackSheet.getAllFrames();
            boostAuraLv3FrontFrames = boostLv3FrontSheet.getAllFrames();
            boostAuraLv3BackFrames = boostLv3BackSheet.getAllFrames();

            System.out.println("Boost aura sprite sheets loaded successfully (front/back).");
        } catch (Exception e) {
            System.out.println("Failed to load boost aura sprite sheets.");
            e.printStackTrace();
        }
    }

    private void registerBattleEffect(String effectType, String path,
                                    int frameWidth, int frameHeight,
                                    int frameDelay, float scale) {
        SpriteSheet sheet = new SpriteSheet(path, frameWidth, frameHeight);
        BufferedImage[] frames = sheet.getAllFrames();

        battleEffectFrames.put(effectType, frames);
        battleEffectFrameDelays.put(effectType, frameDelay);
        battleEffectScales.put(effectType, scale);
    }

    public BufferedImage[] getBattleEffectFrames(String type) {
        return battleEffectFrames.get(type);
    }

    public int getBattleEffectFrameDelay(String type) {
        Integer value = battleEffectFrameDelays.get(type);
        return value != null ? value : 2;
    }

    public float getBattleEffectScale(String type) {
        Float value = battleEffectScales.get(type);
        return value != null ? value : 1.0f;
    }

    public void spawnBattleEffect(String type, int x, int y, int duration, int level) {
        String resolvedType = resolveBattleEffectType(type, level);

        BufferedImage[] frames = getBattleEffectFrames(resolvedType);

        if (frames != null && frames.length > 0) {
            int frameDelay = getBattleEffectFrameDelay(resolvedType);
            float scale = getBattleEffectScale(resolvedType);

            BattleEffectInstance fx = new BattleEffectInstance(
                    resolvedType,
                    x,
                    y,
                    duration,
                    level,
                    frames,
                    frameDelay,
                    scale
            );

            configureBattleEffectOffsets(fx, resolvedType, level);
            activeBattleEffects.add(fx);
            return;
        }

        BattleEffectInstance fx = new BattleEffectInstance(type, x, y, duration, level);
        configureBattleEffectOffsets(fx, type, level);
        activeBattleEffects.add(fx);
    }

    private String getBasicAttackType(BattleEntity actor) {
        if (actor == null) return "sword";

        String n = actor.name.toLowerCase();

        // Προσωρινό mapping με βάση τα ονόματα / classes
        if (n.contains("mage")) return "staff";
        if (n.contains("assassin")) return "sword";
        if (n.contains("hero")) return "spear";

        // enemies / default
        return "sword";
    }

    private boolean processBreakHit(BattleEntity attacker, BattleEntity target, String attackType) {
        if (attacker == null || target == null) return false;
        if (!target.isAlive()) return false;
        if (!target.hasBreakSystem()) return false;
        if (target.broken) return false;

        if (!target.isWeakTo(attackType)) {
            return false;
        }

        target.revealWeakness(attackType);

        Point p = getBattleEntityScreenCenter(target);
        spawnBattleEffect("hit_slash", p.x, p.y - 40, 10, 1);

        boolean justBroken = target.applyShieldDamage(1);

        if (justBroken) {
            spawnBattleEffect("break", p.x, p.y, 18, 1);
            
            // Ήχος break
            sound.playBattleSE("BREAK");
            
            // SLOW MOTION αντί για freeze
            battleTimeScale = 0.5f;  // 1/4 ταχύτητα
            slowMotionTimer = 80;     // ~0.67 δευτερόλεπτα στους 60fps
            
            screenShakeStrength = Math.max(screenShakeStrength, 8);
            screenShakeDuration = Math.max(screenShakeDuration, 15);
            screenShakeTimer = screenShakeDuration;
            
            showActionMessage(target.name + " is broken!");
        } else {
            // μικρό shield hit effect
            spawnBattleEffect("hit_slash", p.x, p.y - 20, 6, 0); // μικρό, γρήγορο
        }

        return justBroken;
    }

    private BufferedImage getWeaknessIcon(String type) {
        if (type == null) return null;
        
        switch (type.toLowerCase()) {
            case "sword": return swordIcon;
            case "spear": return spearIcon;
            case "staff": return staffIcon;
            default: return null;
        }
    }

    private void setupEnemyBreakData(BattleEntity enemy) {
        if (enemy == null || enemy.isPlayer) return;

        String n = enemy.name.toLowerCase();

        if (n.contains("goblin")) {
            enemy.setupBreakData(2, "sword", "spear");
        } else if (n.contains("skeleton")) {
            enemy.setupBreakData(3, "staff", "spear");
        } else if (n.contains("mushroom")) {
            enemy.setupBreakData(2, "sword", "staff");
        } else {
            // default προσωρινό setup
            enemy.setupBreakData(2, "sword");
        }
    }

    private void drawEnemyBreakUI(Graphics2D g2, BattleEntity enemy, int enemyX, int enemyY, int enemyW, int enemyH) {
        if (enemy == null || !enemy.isAlive() || !enemy.hasBreakSystem()) return;

        Graphics2D g = (Graphics2D) g2.create();

        g.setFont(maruMonicaSmall);

        int weaknessCount = 0;
        for (int i = 0; i < enemy.weaknessTypes.length; i++) {
            if (enemy.weaknessTypes[i] != null) {
                weaknessCount++;
            }
        }

        int shieldBoxW = 42;
        int shieldBoxH = 42;
        int slotW = 26;
        int slotH = 26;
        int slotGap = 6;
        int gapAfterShield = 10;

        int totalWidth = shieldBoxW + gapAfterShield;
        if (weaknessCount > 0) {
            totalWidth += weaknessCount * slotW + (weaknessCount - 1) * slotGap;
        }

        int uiX = enemyX + enemyW / 2 - totalWidth / 2;
        int uiY = enemyY + enemyH + 8;

        // shield icon με αριθμό από πάνω
        int shieldIconSize = 42;

        if (shieldIcon != null) {
            g.drawImage(shieldIcon, uiX - 3, uiY - 3, shieldIconSize, shieldIconSize, null);
            
            g.setFont(maruMonicaSmall.deriveFont(Font.BOLD, 17f));
            
            String shieldText;
            if (enemy.broken) {
                g.setColor(new Color(255, 210, 120));
                shieldText = "0";
            } else {
                g.setColor(Color.white);
                shieldText = String.valueOf(enemy.shieldPoints);
            }
            
            FontMetrics fm = g.getFontMetrics();
            int textWidth = fm.stringWidth(shieldText);
            int textHeight = fm.getAscent();
            
            // 2 pixels πιο αριστερά, 2 pixels πιο πάνω από την προηγούμενη θέση
            int textX = uiX + shieldIconSize / 2 - textWidth / 2 - 5;
            int textY = uiY + shieldIconSize / 2 + textHeight / 3 - 2;
            
            // Σκιά
            g.setColor(new Color(0, 0, 0, 200));
            g.drawString(shieldText, textX + 1, textY + 1);
            
            // Αριθμός
            if (enemy.broken) {
                g.setColor(new Color(255, 210, 120));
            } else {
                g.setColor(Color.white);
            }
            g.drawString(shieldText, textX, textY);
        } else {
            // FALLBACK
            g.setColor(new Color(20, 20, 30, 190));
            g.fillRoundRect(uiX, uiY, shieldBoxW, shieldBoxH, 8, 8);
            g.setColor(new Color(220, 230, 255));
            g.drawRoundRect(uiX, uiY, shieldBoxW, shieldBoxH, 8, 8);
            
            if (enemy.broken) {
                g.setColor(new Color(255, 210, 120));
                g.drawString("0", uiX + 16, uiY + 27);
            } else {
                g.setColor(Color.white);
                g.drawString(String.valueOf(enemy.shieldPoints), uiX + 16, uiY + 27);
            }
        }

        // weakness slots
        int slotX = uiX + shieldIconSize + gapAfterShield - 4;

        for (int i = 0; i < enemy.weaknessTypes.length; i++) {
            if (enemy.weaknessTypes[i] == null) continue;

            g.setColor(new Color(20, 20, 30, 190));
            g.fillRoundRect(slotX, uiY + 6, slotW, slotH, 8, 8);

            g.setColor(new Color(220, 230, 255));
            g.drawRoundRect(slotX, uiY + 6, slotW, slotH, 8, 8);

            if (enemy.weaknessRevealed[i]) {
                BufferedImage icon = getWeaknessIcon(enemy.weaknessTypes[i]);
                if (icon != null) {
                    int iconPadding = 2;
                    g.drawImage(icon, slotX + iconPadding, uiY + 6 + iconPadding, 
                            slotW - iconPadding*2, slotH - iconPadding*2, null);
                } else {
                    g.setColor(new Color(255, 245, 180));
                    g.setFont(g.getFont().deriveFont(Font.BOLD, 12f));
                    g.drawString("?", slotX + 6, uiY + 24);
                }
            } else {
                g.setColor(Color.white);
                g.setFont(g.getFont().deriveFont(Font.BOLD, 12f));
                g.drawString("?", slotX + 6, uiY + 24);
            }
            
            slotX += slotW + slotGap;
        }

        if (enemy.broken) {
            g.setColor(new Color(255, 220, 140));
            g.drawString("BREAK", uiX + 8, uiY - 6);
        }

        g.dispose();
    }

    private String resolveBattleEffectType(String type, int level) {
        switch (type) {
            case "boost_burst":
                if (level == 1) return "boost_lv1";
                if (level == 2) return "boost_lv2";
                return "boost_lv3";

            case "hit_slash":
            case "slash":
            case "impact":
                return "slash";

            case "break":
                return "break";

            default:
                return type;
        }
    }

    private void configureBattleEffectOffsets(BattleEffectInstance fx, String resolvedType, int level) {
        fx.centered = true;

        switch (resolvedType) {
            case "slash":
                fx.offsetX = -10;
                fx.offsetY = 120;
                break;

            case "break":
                fx.offsetX = -10;
                fx.offsetY = 120;
                break;

            case "boost_lv1":
            case "boost_lv2":
            case "boost_lv3":
                fx.offsetX = 0;
                fx.offsetY = 20;
                break;

            default:
                fx.offsetX = 0;
                fx.offsetY = 0;
                break;
        }
    }

    public void spawnBattleEffectAtEntity(String type, BattleEntity target, int duration, int level) {
        if (target == null) return;

        Point p = getBattleEntityScreenCenter(target);
        spawnBattleEffect(type, p.x, p.y, duration, level);
    }

    public void updateBattleEffects() {
        for (int i = activeBattleEffects.size() - 1; i >= 0; i--) {
            BattleEffectInstance fx = activeBattleEffects.get(i);
            fx.update();

            if (fx.finished) {
                activeBattleEffects.remove(i);
            }
        }
    }

    public void drawBattleEffects(Graphics2D g2) {
        for (BattleEffectInstance fx : activeBattleEffects) {
            fx.draw(g2, this);
        }
    }

    // public void triggerBoostBurst(BattleEntity target, int boostLevel) {
    //     if (target == null || boostLevel <= 0) return;

    //     Point p = getBattleEntityScreenCenter(target);

    //     int duration;
    //     if (boostLevel == 1) {
    //         duration = 12;
    //         screenShakeStrength = 3;
    //         screenShakeDuration = 6;
    //     } else if (boostLevel == 2) {
    //         duration = 16;
    //         screenShakeStrength = 5;
    //         screenShakeDuration = 8;
    //     } else {
    //         duration = 20;
    //         screenShakeStrength = 7;
    //         screenShakeDuration = 10;
    //     }

    //     screenShakeTimer = screenShakeDuration;

    //     spawnBattleEffect("hit_slash", p.x, p.y, duration, boostLevel);
    // }

    public void triggerSlashImpact(BattleEntity target, int level) {
        if (target == null) return;

        Point p = getBattleEntityScreenCenter(target);

        int duration = 12;
        screenShakeStrength = 3;
        screenShakeDuration = 6;
        screenShakeTimer = screenShakeDuration;

        spawnBattleEffect("hit_slash", p.x, p.y, duration, 1);
    }

    // public void drawBoostBurstEffect(Graphics2D g2) {
    //     if (boostBurstTimer >= boostBurstDuration || boostBurstLevel <= 0) return;

    //     Graphics2D g = (Graphics2D) g2.create();

    //     float progress = (float) boostBurstTimer / boostBurstDuration;
    //     float inv = 1.0f - progress;

    //     // ===== OFFSET για να είναι πιο αριστερά και πιο χαμηλά =====
    //     int impactX = boostBurstX - 15;
    //     int impactY = boostBurstY + 120;

    //     // ===== ΙΔΙΟ ΧΡΩΜΑ ΓΙΑ ΟΛΑ ΤΑ BOOST =====
    //     Color flashColor = new Color(255, 180, 100, Math.max(0, (int)(200 * inv)));

    //     // ===== IMPACT FLASH =====
    //     int flashRadius;
    //     if (boostBurstLevel == 1) {
    //         flashRadius = 24 + (int)(progress * 30);
    //     } else if (boostBurstLevel == 2) {
    //         flashRadius = 34 + (int)(progress * 40);
    //     } else {
    //         flashRadius = 46 + (int)(progress * 52);
    //     }

    //     g.setColor(flashColor);
    //     g.fillOval(impactX - flashRadius, impactY - flashRadius, flashRadius * 2, flashRadius * 2);

    //     // ===== RING EXPLOSION =====
    //     g.setStroke(new BasicStroke(3f));
    //     int ringRadius;
    //     if (boostBurstLevel == 1) {
    //         ringRadius = 28 + (int)(progress * 38);
    //     } else if (boostBurstLevel == 2) {
    //         ringRadius = 40 + (int)(progress * 50);
    //     } else {
    //         ringRadius = 54 + (int)(progress * 64);
    //     }

    //     g.setColor(new Color(255, 210, 150, Math.max(0, (int)(170 * inv))));
    //     g.drawOval(impactX - ringRadius, impactY - ringRadius, ringRadius * 2, ringRadius * 2);

    //     // ===== SECOND RING για boost 2/3 =====
    //     if (boostBurstLevel >= 2) {
    //         int ringRadius2;
    //         if (boostBurstLevel == 2) {
    //             ringRadius2 = 18 + (int)(progress * 32);
    //         } else {
    //             ringRadius2 = 28 + (int)(progress * 44);
    //         }

    //         g.setColor(new Color(255, 245, 220, Math.max(0, (int)(120 * inv))));
    //         g.drawOval(impactX - ringRadius2, impactY - ringRadius2, ringRadius2 * 2, ringRadius2 * 2);
    //     }

    //     // ===== SPARKS =====
    //     int sparkCount = 6 + boostBurstLevel * 3;
    //     for (int i = 0; i < sparkCount; i++) {
    //         double angle = (Math.PI * 2 / sparkCount) * i + (boostBurstTimer * 0.15);
    //         int dist = 10 + (int)(progress * 50) + (i % 3) * 4;

    //         int sx = impactX + (int)(Math.cos(angle) * dist);
    //         int sy = impactY + (int)(Math.sin(angle) * dist);

    //         int size = (boostBurstLevel == 3) ? 5 : 4;
    //         g.setColor(new Color(255, 255, 255, Math.max(0, (int)(180 * inv))));
    //         g.fillOval(sx, sy, size, size);
    //     }

    //     g.dispose();
    // }

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

    public void drawBoostAuraBack(Graphics2D g2, int centerX, int footY, int boostLevel) {
        if (boostLevel <= 0) return;

        BufferedImage[] frames = null;
        float scale = 1.0f;

        if (boostLevel == 1) {
            frames = boostAuraLv1BackFrames;
            scale = boostAuraScaleLv1;
        } else if (boostLevel == 2) {
            frames = boostAuraLv2BackFrames;
            scale = boostAuraScaleLv2;
        } else {
            frames = boostAuraLv3BackFrames;
            scale = boostAuraScaleLv3;
        }

        drawAuraLayer(g2, centerX, footY, frames, scale);
    }

    public void drawBoostAuraFront(Graphics2D g2, int centerX, int footY, int boostLevel) {
        if (boostLevel <= 0) return;

        BufferedImage[] frames = null;
        float scale = 1.0f;

        if (boostLevel == 1) {
            frames = boostAuraLv1FrontFrames;
            scale = boostAuraScaleLv1;
        } else if (boostLevel == 2) {
            frames = boostAuraLv2FrontFrames;
            scale = boostAuraScaleLv2;
        } else {
            frames = boostAuraLv3FrontFrames;
            scale = boostAuraScaleLv3;
        }

        drawAuraLayer(g2, centerX, footY, frames, scale);
    }

    private void drawAuraLayer(Graphics2D g2, int centerX, int footY, 
                            BufferedImage[] frames, float scale) {
        if (frames == null || frames.length == 0) return;

        int frameIndex = getLoopingBoostAuraFrameIndex(frames.length);
        BufferedImage frame = frames[frameIndex];
        if (frame == null) return;

        int drawWidth = Math.round(frame.getWidth() * scale);
        int drawHeight = Math.round(frame.getHeight() * scale);

        int drawX = centerX - drawWidth / 2;
        int drawY = footY - drawHeight + 36;

        g2.drawImage(frame, drawX, drawY, drawWidth, drawHeight, null);
    }

    private int getLoopingBoostAuraFrameIndex(int totalFrames) {
        if (totalFrames <= 0) return 0;

        long ticks = System.currentTimeMillis() / 16L; // περίπου 60fps reference
        return (int)((ticks / boostAuraFrameDelay) % totalFrames);
    }

    public void triggerHitFlash(int boostLevel) {
        if (boostLevel <= 0) return;

        int duration;
        if (boostLevel == 1) {
            duration = 4;
        } else if (boostLevel == 2) {
            duration = 6;
        } else {
            duration = 8;
        }

        spawnBattleEffect("hit_flash", screenWidth / 2, screenHeight / 2, duration, boostLevel);
    }

    // public void drawHitFlashOverlay(Graphics2D g2) {
    //     if (hitFlashTimer <= 0 || hitFlashLevel <= 0) return;

    //     float progress = (float) hitFlashTimer / hitFlashDuration;

    //     int alpha;
    //     if (hitFlashLevel == 1) {
    //         alpha = (int)(70 * progress);
    //     } else if (hitFlashLevel == 2) {
    //         alpha = (int)(110 * progress);
    //     } else {
    //         alpha = (int)(150 * progress);
    //     }

    //     if (alpha < 0) alpha = 0;
    //     if (alpha > 255) alpha = 255;

    //     g2.setColor(new Color(255, 255, 255, alpha));
    //     g2.fillRect(0, 0, screenWidth, screenHeight);
    // }

    public int calculateAttackDamage(BattleEntity attacker, BattleEntity target, int boostUsed) {
        int baseDamage = attacker.attack - target.defense;
        if (baseDamage < 1) baseDamage = 1;
        return baseDamage;
    }

    private void setActorRunAnimation(BattleEntity actor, String animName) {
        if (actor == null || !actor.isPlayer) return;
        playActorAnimation(actor, animName);
    }

    private void setActorIdleAnimation(BattleEntity actor) {
        if (actor == null || !actor.isPlayer) return;
        playActorAnimation(actor, "idle");
    }

    private void updateBattlePlayerMovementAnimation(BattleEntity actor, double currentX, double targetX) {
        if (actor == null || !actor.isPlayer) return;

        // ΜΗΝ πειράζεις τα πραγματικά combat animations
        // εκτός αν είμαστε σε battle entry ή flee
        if (!battleEntering && !battleFleeing) {
            if (actor.state == CombatState.WINDUP ||
                actor.state == CombatState.ATTACKING ||
                actor.state == CombatState.HIT_PAUSE ||
                actor.state == CombatState.RECOVERY ||
                actor.state == CombatState.DEFENDING ||
                actor.state == CombatState.HURT ||
                actor.state == CombatState.DEAD) {
                return;
            }
        }

        double diff = targetX - currentX;

        if (Math.abs(diff) <= 2.0) {
            setActorIdleAnimation(actor);
        } else if (diff < 0) {
            setActorRunAnimation(actor, "run_left");
        } else {
            setActorRunAnimation(actor, "run_right");
        }
    }

    private boolean areAllBattlePlayersOffscreenRight() {
        for (BattlePlayer bp : battlePlayers) {
            if (bp.x < screenWidth + 120) return false;
        }

        for (BattlePartyMember bpm : battlePartyMembers) {
            if (bpm.x < screenWidth + 120) return false;
        }

        return true;
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
                if (!battlePlayers.isEmpty()) {
                    return battlePlayers.get(0).getCurrentFrameIndex() == 1; // Hero: frame 1
                }
            } else {
                for (BattlePartyMember bpm : battlePartyMembers) {
                    if (bpm.member.className.equals(actor.name)) {
                        return bpm.getCurrentFrameIndex() == 2; // Assassin/Mage: frame 2
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
        removeDeadFromNextTurnPreview();

        battleEnemies.removeIf(be -> {
            for (BattleEntity enemyEntity : battleParty.enemies) {
                if (enemyEntity.enemyRef == be.enemy) return false;
            }
            return true;
        });

        // Ενημέρωσε το next turn preview μετά από deaths
        rebuildNextTurnPreview();       

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

            if ("ashen_guardian".equals(activeStoryBattleId)) {
                storyManager.setFlag(StoryFlag.DEMO_BOSS_DEFEATED);
            }
        }
    }

    public void updatePartyMembers() {

        if (gameState != playState || battleStarting) return;

        boolean playerMoving = keyH.leftPressed || keyH.rightPressed || keyH.upPressed || keyH.downPressed;

        for (int i = 0; i < activePartyMembers.size(); i++) {

            PartyMember member = activePartyMembers.get(i);

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
                drawOctopathMenu(g2);

                if (inventory.statusDetailOpen) {
                    drawStatusDetailWindow(g2);
                }

                if (inventory.statusDetailOpen && inventory.statusEquipPopupOpen) {
                    drawStatusEquipPopup(g2);
                }
            }
            // if (gameState == mapState) {
            //     drawMapScreen(g2);
            // }
            if (gameState == shopState) {
                drawShopScreen(g2);
            }
            if (gameState == chestLootState) {
                drawChestLootWindow(g2);
            }
            drawObjectivePopup(g2);
        }
        // ========== ΠΡΟΣΘΕΣΕ ΤΟ FADE EFFECT ΓΙΑ ΟΛΕΣ ΤΙΣ ΚΑΤΑΣΤΑΣΕΙΣ ==========
        // Αυτό ζωγραφίζεται ΠΑΝΤΑ από πάνω, ανεξάρτητα από το gameState
        if (battleFadeOut || battleFadeIn) {
            g2.setColor(new Color(0, 0, 0, battleFadeAlpha));
            g2.fillRect(0, 0, screenWidth, screenHeight);
        }
        
        g2.dispose(); // Καθάρισε (καλή πρακτική)
    }
    
    // ==================================
    //   DRAW METHODS AND THEIR HELPERS
    // ==================================

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

        // ζωγραφισε chests
        ArrayList<Chest> currentChests = chests.get(currentMap);
        for (Chest chest : currentChests) {
            chest.draw(g2, this);
        }

        // ========== ΖΩΓΡΑΦΙΣΕ ΤΑ ΜΕΛΗ ΤΗΣ ΟΜΑΔΑΣ (ΠΙΣΩ ΑΠΟ ΤΟΝ ΠΑΙΚΤΗ) ==========
        for (PartyMember member : activePartyMembers) {
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
            if (selectingTarget && i == selectedTarget && enemyEntity != null && enemyEntity.isAlive()) {
                drawEnemySelectionGlow(g, drawX + recoilX, drawY + recoilY, enemySpriteSize, enemySpriteSize);
            }
            g.drawImage(be.getCurrentImage(), drawX + recoilX, drawY + recoilY, enemySpriteSize, enemySpriteSize, null);
            drawEnemyBreakUI(g, enemyEntity, drawX + recoilX, drawY + recoilY, enemySpriteSize, enemySpriteSize);

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

        BattleEntity currentTurn = battleParty.getCurrentTurn();

        // 1. PARTY MEMBERS
        for (int i = battlePartyMembers.size() - 1; i >= 0; i--) {
            BattlePartyMember bpm = battlePartyMembers.get(i);

            int drawX = (int)Math.round(bpm.x);
            int drawY = (int)Math.round(bpm.y);

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
                drawBoostAuraBack(g, auraCenterX, auraFootY, selectedBoost);
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

            if (currentTurn != null && currentTurn.isPlayer &&
                currentTurn.name.equals(bpm.member.className) &&
                selectedBoost > 0 &&
                !actionInProgress) {

                int auraCenterX = drawX + playerSpriteSize / 2;
                int auraFootY = drawY + playerSpriteSize - 10;
                drawBoostAuraFront(g, auraCenterX, auraFootY, selectedBoost);
            }
        }

        // 2. HERO
        if (!battlePlayers.isEmpty()) {
            BattlePlayer bp = battlePlayers.get(0);
            int drawX = (int)Math.round(bp.x);
            int drawY = (int)Math.round(bp.y);

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
                drawBoostAuraBack(g, auraCenterX, auraFootY, selectedBoost);  // BACK ΠΡΩΤΑ
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

            if (currentTurn != null && currentTurn.isPlayer &&
                currentTurn.name.equals("Hero") &&
                selectedBoost > 0 &&
                !actionInProgress) {

                int auraCenterX = drawX + playerSpriteSize / 2;
                int auraFootY = drawY + playerSpriteSize - 10;
                drawBoostAuraFront(g, auraCenterX, auraFootY, selectedBoost);  // FRONT ΜΕΤΑ
            }
        }

        // ===== BATTLE EFFECT LAYER ΠΑΝΩ ΑΠΟ ΤΑ SPRITES, ΠΡΙΝ ΤΟ UI =====
        drawBattleEffects(g);

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
                g.setFont(maruMonicaBold);
                g.setColor(Color.yellow);
                g.drawString("Select target:", menuX + 50, menuY + 30);

                g.setFont(maruMonicaSmall);
                g.setColor(Color.lightGray);
                g.drawString("ENTER=Confirm   ESC=Back", menuX + 50, menuY + 52);
                g.drawString("Boost: " + selectedBoost, menuX + 260, menuY + 52);

                int targetY = menuY + 110;

                for (int i = 0; i < battleParty.enemies.size(); i++) {
                    BattleEntity enemy = battleParty.enemies.get(i);
                    int x = menuX + 100 + (i * 150);

                    if (i == selectedTarget) {
                        g.setColor(new Color(255, 245, 180));

                        // POINTER
                        if (menuPointer != null) {
                            g.drawImage(menuPointer, x - 40, targetY - 20, 32, 32, null);
                        }

                        g.setFont(maruMonicaBold);
                        g.drawString(enemy.name, x, targetY);

                    } else {
                        g.setColor(Color.white);
                        g.drawString(enemy.name, x + 15, targetY);
                    }
                }
            }
        }

        g.dispose();
    }

    private void drawEnemySelectionGlow(Graphics2D g2, int enemyX, int enemyY, int enemyW, int enemyH) {
        Graphics2D g = (Graphics2D) g2.create();

        long t = System.currentTimeMillis();
        float pulse = 0.65f + 0.35f * (float)Math.sin(t * 0.01);

        int centerX = enemyX + enemyW / 2;
        int footY = enemyY + enemyH;

        int ovalWidth = (int)(enemyW * 0.45);
        int ovalHeight = (int)(enemyH * 0.12);

        int x = centerX - ovalWidth / 2 - 12;
        int y = footY - ovalHeight / 2 + 6;

        // soft glow
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.18f * pulse));
        g.setColor(Color.white);
        g.fillOval(x - 6, y - 4, ovalWidth + 12, ovalHeight + 8);

        // main glow
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.28f * pulse));
        g.fillOval(x, y, ovalWidth, ovalHeight);

        // outline
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.9f));
        g.setStroke(new BasicStroke(2.5f));
        g.setColor(new Color(255, 255, 255, 200));
        g.drawOval(x, y, ovalWidth, ovalHeight);

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
        int startX = 20;
        int startY = 10;
        int slotWidth = 48;
        int slotHeight = 48;
        int spacing = 5;
        int blockGap = 18;

        // =========================
        // CURRENT TURN QUEUE
        // =========================
        int currentIndex = battleParty.currentTurnIndex;
        int currentCount = battleParty.turnOrder.size() - currentIndex;

        int slotIndex = 0;
        for (int i = 0; i < currentCount; i++) {
            BattleEntity entity = battleParty.turnOrder.get(currentIndex + i);
            
            // ΠΑΡΑΛΕΙΨΕ broken enemies στο current turn
            if (entity.broken && !entity.isPlayer) {
                continue;
            }
            
            int x = startX + slotIndex * (slotWidth + spacing);
            int y = startY;
            drawTurnSlot(g2, entity, x, y, slotWidth, slotHeight, slotIndex == 0);
            slotIndex++;
        }

        int currentLabelX = startX;
        int labelY = startY + slotHeight + 14;

        g2.setFont(maruMonicaSmall.deriveFont(Font.BOLD, 10f));
        g2.setColor(new Color(255, 245, 180));
        g2.drawString("CURRENT TURN", currentLabelX, labelY);

        // =========================
        // NEXT TURN QUEUE
        // =========================
        int nextStartX = startX + slotIndex * (slotWidth + spacing) + blockGap;

        removeDeadFromNextTurnPreview();

        int nextSlotIndex = 0;
        for (int i = 0; i < revealedNextTurnCount && i < nextTurnPreview.size(); i++) {
            BattleEntity entity = nextTurnPreview.get(i);
            
            // ΠΑΡΑΛΕΙΨΕ broken enemies
            if (entity.broken && !entity.isPlayer) {
                continue;
            }
            
            int x = nextStartX + nextSlotIndex * (slotWidth + spacing);
            int y = startY;
            drawTurnSlot(g2, entity, x, y, slotWidth, slotHeight, false);
            nextSlotIndex++;
        }

        g2.setFont(maruMonicaSmall.deriveFont(Font.BOLD, 10f));
        g2.setColor(new Color(210, 210, 210));
        g2.drawString("NEXT TURN", nextStartX, labelY);
    }

    private void drawTurnSlot(Graphics2D g2, BattleEntity entity, int x, int y, int slotWidth, int slotHeight, boolean isCurrentActor) {
        if (entity == null) return;

        if (entity.isPlayer) {
            if (isCurrentActor) {
                g2.setColor(new Color(0, 100, 255, 180));
                g2.fillRoundRect(x, y, slotWidth, slotHeight, 10, 10);
                g2.setColor(new Color(0, 150, 255));
                g2.setStroke(new BasicStroke(3));
            } else {
                g2.setColor(new Color(0, 70, 150, 150));
                g2.fillRoundRect(x, y, slotWidth, slotHeight, 10, 10);
                g2.setColor(new Color(100, 150, 255));
                g2.setStroke(new BasicStroke(2));
            }
        } else {
            if (isCurrentActor) {
                g2.setColor(new Color(180, 60, 60, 180));
                g2.fillRoundRect(x, y, slotWidth, slotHeight, 10, 10);
                g2.setColor(new Color(255, 120, 120));
                g2.setStroke(new BasicStroke(3));
            } else {
                g2.setColor(new Color(120, 50, 50, 150));
                g2.fillRoundRect(x, y, slotWidth, slotHeight, 10, 10);
                g2.setColor(new Color(220, 120, 120));
                g2.setStroke(new BasicStroke(2));
            }
        }

        g2.drawRoundRect(x, y, slotWidth, slotHeight, 10, 10);

        BufferedImage icon = getBattleTurnPortrait(entity);
        if (icon != null) {
            int iconX = x + 4;
            int iconY = y + 4;
            int iconW = slotWidth - 8;
            int iconH = slotHeight - 8;

            g2.drawImage(icon, iconX, iconY, iconW, iconH, null);

            // ===== BROKEN ENEMY - SLOT ΕΞΑΦΑΝΙΖΕΤΑΙ =====
            if (entity.broken && !entity.isPlayer) {
                Graphics2D g = (Graphics2D) g2.create();
                
                // Σχεδίασε ΚΑΝΟΝΙΚΑ το slot αλλά με opacity
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.0f));
                
                // Το αφήνουμε κενό - ο χώρος παραμένει αλλά άδειος
                // (Αν θες να φαίνεται και ο κενός χώρος, μπορείς να βάλεις 0.1f opacity)
                
                g.dispose();
                return; // ΒΓΕΣ ΑΜΕΣΩΣ - μη ζωγραφίσεις τίποτα άλλο
            }
        }
    }

    private BufferedImage getBattleTurnPortrait(BattleEntity entity) {
        if (entity == null) return null;

        if (entity.isPlayer) {
            if (entity.playerRef != null && !(entity.playerRef instanceof PartyMember)) {
                return heroPortrait32;
            }

            if (entity.name.equalsIgnoreCase("Assassin")) {
                return assassinPortrait32;
            }

            if (entity.name.equalsIgnoreCase("Mage")) {
                return magePortrait32;
            }

            return heroPortrait32;
        }

        if (entity.enemyRef != null && entity.enemyRef.currentImage != null) {
            return entity.enemyRef.currentImage;
        }

        return entity.image;
    }

    private void rebuildNextTurnPreview() {
        ArrayList<BattleEntity> fullOrder = battleParty.buildSpeedOrderSnapshot();
        nextTurnPreview.clear();
        
        // ΦΙΛΤΡΑΡΕ - ΑΦΑΙΡΕΣΕ broken enemies από το preview
        for (BattleEntity entity : fullOrder) {
            if (entity.broken && !entity.isPlayer) {
                continue; // ΠΑΡΑΛΕΙΨΕ broken enemies
            }
            nextTurnPreview.add(entity);
        }
        
        revealedNextTurnCount = Math.min(INITIAL_NEXT_TURN_REVEAL, nextTurnPreview.size());
    }

    private void revealOneMoreNextTurnSlot() {
        if (revealedNextTurnCount < nextTurnPreview.size()) {
            revealedNextTurnCount++;
        }
    }

    private void removeDeadFromNextTurnPreview() {
        nextTurnPreview.removeIf(entity -> entity == null || !entity.isAlive());

        if (revealedNextTurnCount > nextTurnPreview.size()) {
            revealedNextTurnCount = nextTurnPreview.size();
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

    public void drawChestLootWindow(Graphics2D g2) {
        int screenW = screenWidth;
        int screenH = screenHeight;

        Color bg = new Color(0, 0, 0, 210);
        Color panelDark = new Color(20, 20, 20, 235);
        Color panelMid = new Color(40, 35, 30, 235);
        Color border = new Color(180, 150, 90, 180);
        Color textMain = new Color(240, 230, 210);
        Color textDim = new Color(180, 170, 150);
        Color highlight = new Color(180, 130, 60, 180);

        g2.setColor(bg);
        g2.fillRect(0, 0, screenW, screenH);

        int panelW = 520;
        int panelH = 340;
        int panelX = (screenW - panelW) / 2;
        int panelY = (screenH - panelH) / 2;

        g2.setColor(panelDark);
        g2.fillRoundRect(panelX, panelY, panelW, panelH, 24, 24);
        g2.setColor(border);
        g2.drawRoundRect(panelX, panelY, panelW, panelH, 24, 24);

        // title
        g2.setColor(textMain);
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 28f));
        g2.drawString("Chest Rewards", panelX + 30, panelY + 42);

        // divider
        g2.setColor(new Color(180, 150, 90, 100));
        g2.drawLine(panelX + 20, panelY + 60, panelX + panelW - 20, panelY + 60);

        // loot entries
        int startY = panelY + 100;
        int rowHeight = 54;

        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 20f));

        for (int i = 0; i < chestLootItems.size(); i++) {
            Item item = chestLootItems.get(i);

            int amount = 1;
            if (i < chestLootAmounts.size()) {
                amount = chestLootAmounts.get(i);
            }

            int rowY = startY + (i * rowHeight);

            if (i == chestLootSelectedIndex) {
                g2.setColor(highlight);
                g2.fillRoundRect(panelX + 20, rowY - 24, panelW - 40, 38, 12, 12);
            }

            // item icon
            if (item != null && item.image != null) {
                g2.drawImage(item.image, panelX + 30, rowY - 18, 32, 32, null);
            }

            // item text
            g2.setColor(textMain);
            if (item != null) {
                g2.drawString(item.name, panelX + 80, rowY);
                g2.setColor(textDim);
                g2.drawString("x" + amount, panelX + panelW - 80, rowY);
            }
        }

        // help bar
        g2.setColor(panelMid);
        g2.fillRoundRect(panelX + 20, panelY + panelH - 60, panelW - 40, 36, 14, 14);
        g2.setColor(border);
        g2.drawRoundRect(panelX + 20, panelY + panelH - 60, panelW - 40, 36, 14, 14);

        g2.setColor(textDim);
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 18f));
        g2.drawString("Press Enter or Esc to continue", panelX + 110, panelY + panelH - 35);
    }

    // ====================================
    //      DRAW MENU HELPERS
    // =================================== 

    public void drawOctopathMenu(Graphics2D g2) {
        int screenW = screenWidth;
        int screenH = screenHeight;
        if (inventory.worldMapOpenFromMenu) {
            drawMapScreen(g2);

            if (inventory.worldMapTransitionAlpha > 0 && inventory.worldMapTransitionAlpha < 255) {
                int alpha = 255 - inventory.worldMapTransitionAlpha;
                if (alpha < 0) alpha = 0;
                if (alpha > 255) alpha = 255;

                g2.setColor(new Color(0, 0, 0, alpha));
                g2.fillRect(0, 0, screenWidth, screenHeight);
            }

            return;
        }
        if (inventory.journalOpenFromMenu) {
            drawJournalScreen(g2);
            return;
        }

        // =========================
        // BACKDROP
        // =========================
        if (worldMapBackground != null) {
            g2.drawImage(worldMapBackground, 0, 0, screenW, screenH, null);
        } else {
            g2.setColor(new Color(0, 0, 0, 170));
            g2.fillRect(0, 0, screenW, screenH);
        }

        // ελαφρύ dark tint για να δένει το UI
        g2.setColor(new Color(0, 0, 0, 55));
        g2.fillRect(0, 0, screenW, screenH);

        // =========================
        // PANEL SIZES
        // =========================
        int bottomX = 0;
        int bottomY = screenH - 70;
        int bottomW = screenW;
        int bottomH = 70;

        // left panel full side
        int leftX = 0;
        int leftY = 0;
        int leftW = 220;
        int leftH = screenH - bottomH;

        // right panel full side
        int rightW = 250;
        int rightX = screenW - rightW;
        int rightY = 0;
        int rightH = screenH - bottomH;

        // center panel stays inset between them
        int centerX = leftW + 18;
        int centerY = 20;
        int centerW = rightX - centerX - 18;
        int centerH = screenH - 110;

        // =========================
        // PANEL BACKGROUNDS
        // =========================
        Color panelDark = new Color(0, 0, 0, 145);
        Color panelMid = new Color(0, 0, 0, 135);
        Color border = new Color(180, 150, 90, 140);
        Color highlight = new Color(210, 180, 90, 220);
        Color textMain = new Color(240, 230, 210);
        Color textDim = new Color(180, 170, 150);

        // Left panel - full side
        g2.setColor(panelDark);
        g2.fillRect(leftX, leftY, leftW, leftH);
        g2.setColor(border);
        g2.drawLine(leftW - 1, 0, leftW - 1, leftH);

        // Center panel - still framed
        g2.setColor(panelMid);
        g2.fillRoundRect(centerX, centerY, centerW, centerH, 20, 20);
        g2.setColor(border);
        g2.drawRoundRect(centerX, centerY, centerW, centerH, 20, 20);

        // Right panel - full side
        g2.setColor(panelDark);
        g2.fillRect(rightX, rightY, rightW, rightH);
        g2.setColor(border);
        g2.drawLine(rightX, 0, rightX, rightH);

        // center sub-areas
        int contentTopY = centerY + 50;
        int contentTopH = 260;

        int contentBottomY = contentTopY + contentTopH + 15;

        // divider line
        g2.setColor(new Color(180, 150, 90, 100));
        g2.drawLine(centerX + 15, contentBottomY - 8, centerX + centerW - 15, contentBottomY - 8);

        // Bottom help bar
        g2.setColor(new Color(0, 0, 0, 230));
        g2.fillRect(bottomX, bottomY, bottomW, bottomH);

        // =========================
        // LEFT ROOT MENU
        // =========================
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 28f));
        g2.setColor(textMain);
        g2.drawString("Menu", leftX + 24, leftY + 42);

        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 22f));
        int optionY = leftY + 90;

        for (int i = 0; i < mainMenuOptions.length; i++) {
            boolean selected = (inventory.menuFocus == 0 && inventory.menuSection == i);

            if (selected) {
                g2.setColor(new Color(180, 130, 60, 180));
                g2.fillRoundRect(leftX + 10, optionY - 24, leftW - 20, 36, 14, 14);
                g2.setColor(highlight);
            } else {
                g2.setColor(textMain);
            }

            g2.drawString(mainMenuOptions[i], leftX + 28, optionY);
            optionY += 52;
        }

        // =========================
        // CENTER PANEL HEADER
        // =========================
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 24f));
        g2.setColor(textMain);

        String centerHeader = mainMenuOptions[inventory.menuSection];
        if (inventory.menuSection == 0) {
            centerHeader = itemCategoryOptions[inventory.selectedItemsCategory];
        }

        g2.drawString(centerHeader, centerX + 20, centerY + 35);

        // =========================
        // CENTER PANEL CONTENT
        // =========================
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 18f));

        if (inventory.menuSection == 0) {
            drawItemCategoryIcons(g2, leftX, centerX, centerY);

            ArrayList<Item> filteredItems = getFilteredItemsList();
            int listY = centerY + 78;

            g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 18f));

            for (int i = 0; i < filteredItems.size(); i++) {
                Item item = filteredItems.get(i);
                boolean selected = (inventory.selectedItemsListIndex == i);

                if (selected) {
                    drawPointer(g2, centerX + 34, listY - 18);
                }

                g2.setColor(textMain);
                g2.drawString(item.name, centerX + 70, listY);

                g2.setColor(textDim);
                g2.drawString("x" + item.amount, centerX + centerW - 55, listY);

                listY += 32;
            }

            if (filteredItems.isEmpty()) {
                g2.setColor(textDim);
                g2.drawString("No items in this category.", centerX + 70, centerY + 110);
            }

            if (inventory.itemUseTargetMode) {
                int targetBoxW = 220;
                int targetBoxH = 160;
                int targetBoxX = centerX + centerW - targetBoxW - 18;
                int targetBoxY = centerY + 68;

                // shadow
                g2.setColor(new Color(0, 0, 0, 120));
                g2.fillRoundRect(targetBoxX + 4, targetBoxY + 4, targetBoxW, targetBoxH, 14, 14);

                // panel
                g2.setColor(new Color(22, 22, 22, 235));
                g2.fillRoundRect(targetBoxX, targetBoxY, targetBoxW, targetBoxH, 14, 14);

                // border
                g2.setColor(new Color(180, 150, 90, 190));
                g2.drawRoundRect(targetBoxX, targetBoxY, targetBoxW, targetBoxH, 14, 14);

                // title
                g2.setColor(textMain);
                g2.setFont(g2.getFont().deriveFont(Font.BOLD, 16f));
                g2.drawString("Use Item On", targetBoxX + 16, targetBoxY + 24);

                // divider
                g2.setColor(new Color(180, 150, 90, 90));
                g2.drawLine(targetBoxX + 12, targetBoxY + 34, targetBoxX + targetBoxW - 12, targetBoxY + 34);

                int targetY = targetBoxY + 58;
                g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 16f));

                for (int i = 0; i <= activePartyMembers.size(); i++) {
                    String targetName;

                    if (i == 0) {
                        targetName = player.name;
                    } else {
                        targetName = activePartyMembers.get(i - 1).name;
                    }

                    if (inventory.itemUseTargetIndex == i) {
                        drawPointer(g2, targetBoxX + 12, targetY - 18);
                    }

                    g2.setColor(textMain);
                    g2.drawString(targetName, targetBoxX + 42, targetY);
                    targetY += 28;
                }
            }
        }

        else if (inventory.menuSection == 1) {
            // STATUS
            g2.setColor(textDim);
            g2.drawString("Status", centerX + 20, centerY + 90);
            g2.drawString("Coming soon...", centerX + 20, centerY + 130);
        }

        else if (inventory.menuSection == 2) {
            // EQUIPMENT
            int listY = contentTopY + 50;

            g2.setColor(textMain);
            g2.drawString("Party Members", centerX + 20, listY);
            listY += 40;

            // hero
            boolean heroSelected = (inventory.menuFocus == 1 && inventory.selectedPartyMember == 0);
            if (heroSelected) {
                g2.setColor(new Color(180, 130, 60, 160));
                g2.fillRoundRect(centerX + 15, listY - 20, centerW - 30, 28, 10, 10);
            }
            g2.setColor(textMain);
            g2.drawString(player.name + " (Leader)", centerX + 25, listY);
            listY += 32;

            for (int i = 0; i < activePartyMembers.size(); i++) {
                boolean selected = (inventory.menuFocus == 1 && inventory.selectedPartyMember == i + 1);
                if (selected) {
                    g2.setColor(new Color(180, 130, 60, 160));
                    g2.fillRoundRect(centerX + 15, listY - 20, centerW - 30, 28, 10, 10);
                }

                g2.setColor(textMain);
                g2.drawString(activePartyMembers.get(i).name, centerX + 25, listY);
                listY += 32;
            }
        }

        else if (inventory.menuSection == 3) {
            g2.setColor(textDim);
            g2.drawString("World Map", centerX + 20, centerY + 90);
            g2.drawString("Coming soon...", centerX + 20, centerY + 130);
        }

        else if (inventory.menuSection == 4) {
            g2.setColor(textDim);
            g2.drawString("Journal", centerX + 20, centerY + 90);
            g2.drawString("Coming soon...", centerX + 20, centerY + 130);
        }

        else if (inventory.menuSection == 5) {
            g2.setColor(textMain);
            g2.drawString("Save Game", centerX + 20, centerY + 90);
            g2.setColor(textDim);
            g2.drawString("Press Enter to save.", centerX + 20, centerY + 130);
        }

        else if (inventory.menuSection == 6) {
            g2.setColor(textDim);
            g2.drawString("Options", centerX + 20, centerY + 90);
            g2.drawString("Coming soon...", centerX + 20, centerY + 130);
        }

        // =========================
        // DETAILS INSIDE CENTER PANEL
        // =========================
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 22f));
        g2.setColor(textMain);
        g2.drawString("Details", centerX + 40, contentBottomY + 25);

        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 18f));

        if (inventory.menuSection == 0) {
            ArrayList<Item> filteredItems = getFilteredItemsList();
            Item selected = null;

            if (!filteredItems.isEmpty() &&
                inventory.selectedItemsListIndex >= 0 &&
                inventory.selectedItemsListIndex < filteredItems.size()) {
                selected = filteredItems.get(inventory.selectedItemsListIndex);
            }

            // details panel background
            int detailX = centerX + 12;
            int detailY = contentBottomY + 18;
            int detailW = centerW - 24;

            if (selected != null) {
                // item image area
                int iconBoxX = detailX + 26;
                int iconBoxY = detailY + 16;
                int iconBoxSize = 64;

                g2.setColor(new Color(35, 35, 35, 220));
                g2.fillRoundRect(iconBoxX, iconBoxY, iconBoxSize, iconBoxSize, 10, 10);

                g2.setColor(new Color(180, 150, 90, 140));
                g2.drawRoundRect(iconBoxX, iconBoxY, iconBoxSize, iconBoxSize, 10, 10);

                if (selected.image != null) {
                    g2.drawImage(selected.image, iconBoxX + 8, iconBoxY + 8, 56, 56, null);
                }

                // item name
                g2.setColor(textMain);
                g2.setFont(g2.getFont().deriveFont(Font.BOLD, 18f));
                g2.drawString(selected.name, detailX + 105, detailY + 32);

                // description
                g2.setColor(textMain);
                g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 15f));

                String description = getItemDescription(selected);
                drawWrappedText(g2, description, detailX + 105, detailY + 58, detailW - 125, 20);
            } else {
                g2.setColor(textDim);
                g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 18f));
                g2.drawString("No item selected.", detailX + 18, detailY + 32);
            }
        }

        else if (inventory.menuSection == 1) {
            Item selected = inventory.getEquipSlot(inventory.selectedEquipSlot);

            if (selected != null) {
                g2.setColor(textMain);
                g2.drawString(selected.name, centerX + 20, contentBottomY + 70);

                g2.setColor(textDim);
                g2.drawString("ATK: +" + selected.attackBonus, centerX + 20, contentBottomY + 110);
                g2.drawString("DEF: +" + selected.defenseBonus, centerX + 20, contentBottomY + 140);
                g2.drawString("MAG: +" + selected.magicBonus, centerX + 20, contentBottomY + 170);
                g2.drawString("HP: +" + selected.hpBonus, centerX + 20, contentBottomY + 200);
                g2.drawString("MP: +" + selected.mpBonus, centerX + 20, contentBottomY + 230);
                g2.drawString("SPD: +" + selected.speedBonus, centerX + 20, contentBottomY + 260);
            } else {
                g2.setColor(textDim);
                g2.drawString("Empty slot.", centerX + 20, contentBottomY + 70);
            }
        }

        else if (inventory.menuSection == 2) {
            // nothing for now
        }

        else if (inventory.menuSection == 5) {
            g2.setColor(textDim);
            g2.drawString("Save your current progress.", centerX + 20, contentBottomY + 70);
        }

        // =========================
        // RIGHT PARTY PANEL
        // =========================
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 22f));
        g2.setColor(textMain);
        g2.drawString("Party", rightX + 20, rightY + 35);

        int partyY = rightY + 80;
        // hero
        drawPartyPanelEntry(
            g2,
            rightX + 20,
            partyY,
            heroPortrait32,
            player.name,
            player.level,
            player.hp,
            player.maxHp,
            player.mp,
            player.maxMp
        );
        partyY += 120;

        // party members
        for (int i = 0; i < activePartyMembers.size(); i++) {
            PartyMember pm = activePartyMembers.get(i);

            BufferedImage portrait = getPartyPortraitForMember(pm);

            drawPartyPanelEntry(
                g2,
                rightX + 20,
                partyY,
                portrait,
                pm.name,
                pm.level,
                pm.hp,
                pm.maxHp,
                pm.mp,
                pm.maxMp
            );

            partyY += 120;
        }

        // =========================
        // BOTTOM HELP BAR
        // =========================
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 20f));
        g2.setColor(textMain);

        if (inventory.menuSection == 0 && inventory.itemUseTargetMode) {
            g2.drawString("↑↓ Target   Enter Use Item   Esc Cancel   I Close Menu", 25, bottomY + 42);
        }
        else if (inventory.menuSection == 0) {
            g2.drawString("←→ Category   ↑↓ Select Item   Enter Use   Esc Back   I Close Menu", 25, bottomY + 42);
        }
        else {
            g2.drawString("↑↓ Select   → / Enter Confirm   Esc Back   I Close Menu", 25, bottomY + 42);
        }
    }

    private void drawPartyPanelEntry(Graphics2D g2, int x, int y, BufferedImage portrait, String name, int level, int hp, int maxHp, int mp, int maxMp) {
        Color textMain = new Color(240, 230, 210);
        Color textDim = new Color(180, 170, 150);
        Color hpColor = new Color(70, 180, 90);
        Color mpColor = new Color(70, 140, 220);
        Color barBack = new Color(50, 50, 50, 220);

        // portrait frame
        if (portrait != null) {
            g2.drawImage(portrait, x - 10, y + 46, 64, 64, null);
        } else {
            g2.setColor(textDim);
            g2.drawString("?", x + 16, y + 30);
        }

        int textX = x + 62;

        g2.setColor(textMain);
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 18f));
        g2.drawString(name, textX, y + 18);

        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 14f));
        g2.setColor(textDim);
        g2.drawString("Lv. " + level, textX, y + 40);

        // HP label
        g2.drawString("HP", textX, y + 64);
        g2.setColor(textMain);
        g2.drawString(hp + "/" + maxHp, textX + 35, y + 64);

        // HP bar
        int barW = 120;
        int barH = 10;
        int hpBarX = textX;
        int hpBarY = y + 72;

        g2.setColor(barBack);
        g2.fillRoundRect(hpBarX, hpBarY, barW, barH, 8, 8);

        int hpFill = (maxHp <= 0) ? 0 : (int)((hp / (double)maxHp) * barW);
        g2.setColor(hpColor);
        g2.fillRoundRect(hpBarX, hpBarY, hpFill, barH, 8, 8);

        // MP label
        g2.setColor(textDim);
        g2.drawString("SP", textX, y + 98);
        g2.setColor(textMain);
        g2.drawString(mp + "/" + maxMp, textX + 35, y + 98);

        // MP bar
        int mpBarY = y + 106;

        g2.setColor(barBack);
        g2.fillRoundRect(hpBarX, mpBarY, barW, barH, 8, 8);

        int mpFill = (maxMp <= 0) ? 0 : (int)((mp / (double)maxMp) * barW);
        g2.setColor(mpColor);
        g2.fillRoundRect(hpBarX, mpBarY, mpFill, barH, 8, 8);
    }

    public void drawStatusDetailWindow(Graphics2D g2) {
        int screenW = screenWidth;
        int screenH = screenHeight;

        Color bg = new Color(0, 0, 0, 220);
        Color panelDark = new Color(20, 20, 20, 230);
        Color panelMid = new Color(40, 35, 30, 230);
        Color border = new Color(180, 150, 90, 180);
        Color textMain = new Color(240, 230, 210);
        Color textDim = new Color(180, 170, 150);

        g2.setColor(bg);
        g2.fillRect(0, 0, screenW, screenH);

        int mainX = 20;
        int mainY = 20;
        int mainW = screenW - 40;
        int mainH = screenH - 90;

        g2.setColor(panelDark);
        g2.fillRoundRect(mainX, mainY, mainW, mainH, 20, 20);
        g2.setColor(border);
        g2.drawRoundRect(mainX, mainY, mainW, mainH, 20, 20);

        int leftX = mainX + 20;
        int leftY = mainY + 20;
        int leftW = 180;
        int leftH = mainH - 40;

        int centerX = leftX + leftW + 15;
        int centerY = mainY + 20;
        int centerW = 210;
        int centerH = mainH - 40;

        int rightX = centerX + centerW + 15;
        int rightY = mainY + 20;
        int rightW = mainX + mainW - rightX - 20;
        int rightH = mainH - 40;

        // left panel
        g2.setColor(panelMid);
        g2.fillRoundRect(leftX, leftY, leftW, leftH, 18, 18);
        g2.setColor(border);
        g2.drawRoundRect(leftX, leftY, leftW, leftH, 18, 18);

        // center panel
        g2.setColor(panelMid);
        g2.fillRoundRect(centerX, centerY, centerW, centerH, 18, 18);
        g2.setColor(border);
        g2.drawRoundRect(centerX, centerY, centerW, centerH, 18, 18);

        // right panel
        g2.setColor(panelMid);
        g2.fillRoundRect(rightX, rightY, rightW, rightH, 18, 18);
        g2.setColor(border);
        g2.drawRoundRect(rightX, rightY, rightW, rightH, 18, 18);

        // current selected character
        String charName;
        int level;
        int hp;
        int maxHp;
        int mp;
        int maxMp;
        int atk;
        int def;
        int mag;
        int spd;
        BufferedImage portrait;

        if (inventory.selectedPartyMember == 0) {
            charName = player.name;
            level = player.level;
            hp = player.hp;
            maxHp = player.maxHp;
            mp = player.mp;
            maxMp = player.maxMp;
            atk = player.attack;
            def = player.defense;
            mag = player.magicAttack;
            spd = player.speed_stat;
            portrait = heroPortrait32;
        } else {
            PartyMember pm = activePartyMembers.get(inventory.selectedPartyMember - 1);
            charName = pm.name;
            level = pm.level;
            hp = pm.hp;
            maxHp = pm.maxHp;
            mp = pm.mp;
            maxMp = pm.maxMp;
            atk = pm.attack;
            def = pm.defense;
            mag = pm.magicAttack;
            spd = pm.speed_stat;

            if (inventory.selectedPartyMember == 1) {
                portrait = assassinPortrait32;
            } else {
                portrait = magePortrait32;
            }
        }

        // ===== LEFT PANEL =====
        g2.setColor(textMain);
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 22f));
        g2.drawString(charName, leftX + 20, leftY + 35);

        if (portrait != null) {
            g2.drawImage(portrait, leftX + 16, leftY + 58, 84, 100, null);
        }

        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 18f));
        g2.setColor(textDim);
        g2.drawString("Lv. " + level, leftX + 16, leftY + 180);
        g2.drawString("HP: " + hp + "/" + maxHp, leftX + 16, leftY + 210);
        g2.drawString("SP: " + mp + "/" + maxMp, leftX + 16, leftY + 238);
        g2.drawString("ATK: " + atk, leftX + 16, leftY + 270);
        g2.drawString("DEF: " + def, leftX + 16, leftY + 298);
        g2.drawString("MAG: " + mag, leftX + 16, leftY + 326);
        g2.drawString("SPD: " + spd, leftX + 16, leftY + 354);

        // ===== CENTER PANEL =====
        g2.setColor(textMain);
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 20f));
        g2.drawString("Equipment List", centerX + 20, centerY + 35);

        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 16f));

        ArrayList<Item> allEquipItems = getStatusEquipmentDisplayList();

        int listStartY = centerY + 80;
        int visibleRows = 10;
        int scrollOffset = 0;

        if (inventory.selectedEquipmentListIndex >= visibleRows) {
            scrollOffset = inventory.selectedEquipmentListIndex - visibleRows + 1;
        }

        for (int i = 0; i < visibleRows; i++) {
            int itemIndex = i + scrollOffset;
            int drawY = listStartY + (i * 32);

            if (itemIndex >= allEquipItems.size()) break;

            Item item = allEquipItems.get(itemIndex);
            boolean selected = (itemIndex == inventory.selectedEquipmentListIndex);
            String ownerMarker = getEquipOwnerMarker(item);

            if (selected) {
                drawPointer(g2, centerX + 14, drawY - 18);
            }

            // πάντα το item name πρώτο
            g2.setColor(textMain);
            g2.drawString(item.name, centerX + 45, drawY);

            // και αν υπάρχει owner marker, ζωγράφισέ το δεξιά
            if (!ownerMarker.isEmpty()) {
                g2.setColor(new Color(220, 190, 100));
                g2.drawString(ownerMarker, centerX + centerW - 55, drawY);
            }
        }

        Item selectedEquipListItem = null;
        if (!allEquipItems.isEmpty() &&
            inventory.selectedEquipmentListIndex >= 0 &&
            inventory.selectedEquipmentListIndex < allEquipItems.size()) {
            selectedEquipListItem = allEquipItems.get(inventory.selectedEquipmentListIndex);
        }

        // ===== RIGHT PANEL =====
        g2.setColor(textMain);
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 20f));
        g2.drawString("Equipped", rightX + 20, rightY + 35);

        // Θέση του custom 3x3 background
        int bgX = rightX + 10;
        int bgY = rightY + 55;
        int bgW = 240;
        int bgH = 240;

        // Ζωγράφισε το custom background image
        if (equipmentGridBg != null) {
            g2.drawImage(equipmentGridBg, bgX, bgY, bgW, bgH, null);
        } else {
            // fallback αν λείπει η εικόνα
            g2.setColor(new Color(20, 20, 20, 220));
            g2.fillRoundRect(bgX, bgY, bgW, bgH, 16, 16);
            g2.setColor(border);
            g2.drawRoundRect(bgX, bgY, bgW, bgH, 16, 16);
        }

        // Slot positions ΠΑΝΩ στο custom image
        int gridX = bgX + 22;
        int gridY = bgY + 24;
        int slotSize = 56;
        int gap = 12;

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int slotIndex = row * 3 + col;
                int slotX = gridX + col * (slotSize + gap);
                int slotY = gridY + row * (slotSize + gap);

                Entity selectedCharacter = getSelectedStatusCharacter();
                Item equipped = getCharacterEquipSlot(selectedCharacter, slotIndex);

                if (equipped != null && equipped.image != null) {
                    g2.drawImage(equipped.image, slotX + 8, slotY + 8, 48, 48, null);
                }
            }
        }

        // help bar
        g2.setColor(new Color(0, 0, 0, 230));
        g2.fillRect(0, screenH - 70, screenW, 70);
        g2.setColor(textMain);
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 20f));
        g2.drawString("↑↓ Select Item   Enter Action   Esc Back", 25, screenH - 28);
    }

    private void drawWrappedText(Graphics2D g2, String text, int x, int y, int maxWidth, int lineHeight) {
        if (text == null || text.isEmpty()) return;

        FontMetrics fm = g2.getFontMetrics();
        String[] paragraphs = text.split("\n");
        int currentY = y;

        for (String paragraph : paragraphs) {
            String[] words = paragraph.split(" ");
            String line = "";

            for (String word : words) {
                String testLine = line.isEmpty() ? word : line + " " + word;

                if (fm.stringWidth(testLine) > maxWidth) {
                    g2.drawString(line, x, currentY);
                    currentY += lineHeight;
                    line = word;
                } else {
                    line = testLine;
                }
            }

            if (!line.isEmpty()) {
                g2.drawString(line, x, currentY);
                currentY += lineHeight;
            }
        }
    }

    private void drawPointer(Graphics2D g2, int x, int y) {
        if (menuPointer != null) {
            g2.drawImage(menuPointer, x, y, 24, 24, null);
        } else {
            // fallback αν λείπει το sprite
            g2.setColor(new Color(255, 220, 140));
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 20f));
            g2.drawString(">", x + 4, y + 18);
        }
    }

    private void drawStatusEquipPopup(Graphics2D g2) {
        ArrayList<Item> allEquipItems = getStatusEquipmentDisplayList();
        if (allEquipItems.isEmpty()) return;
        if (inventory.selectedEquipmentListIndex < 0) return;
        if (inventory.selectedEquipmentListIndex >= allEquipItems.size()) return;

        Item selected = allEquipItems.get(inventory.selectedEquipmentListIndex);
        if (selected == null) return;

        Entity selectedCharacter = getSelectedStatusCharacter();
        boolean canEquipSelected = canCharacterEquipItem(selectedCharacter, selected);
        String selectedCharacterName = getCharacterDisplayName(selectedCharacter);

        Color border = new Color(180, 150, 90, 180);
        Color textMain = new Color(240, 230, 210);
        Color textDim = new Color(180, 170, 150);

        int popW = 320;
        int popH = 140;
        int popX = (screenWidth - popW) / 2;
        int popY = (screenHeight - popH) / 2;

        // backdrop behind popup
        g2.setColor(new Color(0, 0, 0, 120));
        g2.fillRect(0, 0, screenWidth, screenHeight);

        // popup body
        g2.setColor(new Color(15, 15, 15, 240));
        g2.fillRoundRect(popX, popY, popW, popH, 18, 18);

        g2.setColor(border);
        g2.drawRoundRect(popX, popY, popW, popH, 18, 18);

        g2.setColor(textMain);
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 20f));

        int ownerIndex = getItemEquippedOwnerIndex(selected);
        int selectedCharacterIndex = getSelectedStatusCharacterIndex();

        if (!canEquipSelected) {
            g2.drawString(selectedCharacterName + " cannot equip", popX + 74, popY + 34);
            g2.drawString("this weapon.", popX + 110, popY + 58);
        }
        else if (ownerIndex == -1) {
            g2.drawString("Equip this item?", popX + 88, popY + 38);
        }
        else if (ownerIndex == selectedCharacterIndex) {
            g2.drawString("Unequip this item?", popX + 70, popY + 38);
        }
        else {
            String ownerName = getEquipOwnerName(selected);
            g2.drawString("Take this item from " + ownerName + "?", popX + 18, popY + 38);
        }

        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 18f));
        g2.setColor(textDim);
        g2.drawString(selected.name, popX + 130, popY + 78);

        boolean yesSelected = (inventory.statusEquipPopupOption == 0);
        boolean noSelected = (inventory.statusEquipPopupOption == 1);

        if (!canEquipSelected) {
            drawPointer(g2, popX + 120, popY + 92);

            g2.setColor(textMain);
            g2.drawString("OK", popX + 155, popY + 112);
        }
        else {
            if (yesSelected) {
                drawPointer(g2, popX + 46, popY + 92);
            }
            if (noSelected) {
                drawPointer(g2, popX + 176, popY + 92);
            }

            g2.setColor(textMain);
            g2.drawString("Yes", popX + 74, popY + 112);
            g2.drawString("No", popX + 220, popY + 112);
        }
    }

    private boolean isItemEquipped(Item item) {
        if (item == null || item.name == null) return false;

        // Hero
        for (int i = 0; i < 9; i++) {
            Item equipped = getCharacterEquipSlot(player, i);
            if (equipped != null && equipped.name != null && equipped.name.equals(item.name)) {
                return true;
            }
        }

        // Party members
        for (int p = 0; p < activePartyMembers.size(); p++) {
            PartyMember member = activePartyMembers.get(p);

            for (int i = 0; i < 9; i++) {
                Item equipped = getCharacterEquipSlot(member, i);
                if (equipped != null && equipped.name != null && equipped.name.equals(item.name)) {
                    return true;
                }
            }
        }

        return false;
    }

    private ArrayList<Item> getStatusEquipmentDisplayList() {
        ArrayList<Item> result = new ArrayList<>();

        // 1. items από storage
        for (int i = 0; i < inventory.storage.length; i++) {
            Item item = inventory.storage[i];
            if (item != null && isEquippableItem(item)) {
                if (!containsItemByName(result, item)) {
                    result.add(item);
                }
            }
        }

        // 2. items από Hero equips
        for (int i = 0; i < 9; i++) {
            Item equipped = getCharacterEquipSlot(player, i);
            if (equipped != null && isEquippableItem(equipped)) {
                if (!containsItemByName(result, equipped)) {
                    result.add(equipped);
                }
            }
        }

        // 3. items από τα equips όλων των party members
        for (int p = 0; p < activePartyMembers.size(); p++) {
            PartyMember member = activePartyMembers.get(p);

            for (int i = 0; i < 9; i++) {
                Item equipped = getCharacterEquipSlot(member, i);
                if (equipped != null && isEquippableItem(equipped)) {
                    if (!containsItemByName(result, equipped)) {
                        result.add(equipped);
                    }
                }
            }
        }

        return result;
    }

    private boolean isEquippableItem(Item item) {
        if (item == null) return false;

        return item.attackBonus != 0 ||
            item.defenseBonus != 0 ||
            item.magicBonus != 0 ||
            item.hpBonus != 0 ||
            item.mpBonus != 0 ||
            item.speedBonus != 0;
    }

    private boolean containsItemByName(ArrayList<Item> list, Item item) {
        if (item == null || item.name == null) return false;

        for (Item existing : list) {
            if (existing != null && existing.name != null && existing.name.equals(item.name)) {
                return true;
            }
        }

        return false;
    }

    private int getEquipSlotForItem(Item item) {
        if (item == null || item.name == null) return -1;

        String name = item.name;

        if (name.contains("Sword") || name.contains("Staff") || name.contains("Dagger") || name.contains("Spear")) {
            return 3; // WEAPON
        } else if (name.contains("Shield")) {
            return 5; // SHIELD
        } else if (name.contains("Helmet") || name.contains("Hat")) {
            return 1; // HELMET
        } else if (name.contains("Chest") || name.contains("Armor")) {
            return 4; // CHEST
        } else if (name.contains("Gloves") || name.contains("Gauntlets")) {
            return 6; // GLOVES
        } else if (name.contains("Belt")) {
            return 7; // BELT
        } else if (name.contains("Boots") || name.contains("Shoes")) {
            return 8; // BOOTS
        } else if (name.contains("Ring")) {
            return 0; // RING
        } else if (name.contains("Necklace") || name.contains("Amulet")) {
            return 2; // NECKLACE
        }

        return -1;
    }

    private String getWeaponType(Item item) {
        if (item == null || item.name == null) return "";

        String name = item.name.toLowerCase();

        if (name.contains("sword") || name.contains("blade")) return "sword";
        if (name.contains("spear")) return "spear";
        if (name.contains("dagger") || name.contains("knife")) return "dagger";
        if (name.contains("bow")) return "bow";
        if (name.contains("staff") || name.contains("rod") || name.contains("wand")) return "staff";

        return "";
    }

    private boolean isWeaponItem(Item item) {
        return getEquipSlotForItem(item) == 3;
    }

    private boolean canCharacterEquipItem(Entity character, Item item) {
        if (character == null || item == null) return false;

        // Για ΟΛΑ τα non-weapon items: επιτρέπονται σε όλους προς το παρόν
        if (!isWeaponItem(item)) {
            return true;
        }

        String weaponType = getWeaponType(item);

        // Αν δεν αναγνωρίζουμε weapon type, το μπλοκάρουμε για ασφάλεια
        if (weaponType.isEmpty()) {
            return false;
        }

        // Hero
        if (character == player) {
            return weaponType.equals("spear");
        }

        // Assassin
        if (activePartyMembers.size() > 0 && character == activePartyMembers.get(0)) {
            return weaponType.equals("dagger") || weaponType.equals("sword");
        }

        // Mage
        if (activePartyMembers.size() > 1 && character == activePartyMembers.get(1)) {
            return weaponType.equals("staff");
        }

        return false;
    }

    private String getCharacterDisplayName(Entity character) {
        if (character == null) return "Character";

        if (character == player) return player.name;

        if (activePartyMembers.size() > 0 && character == activePartyMembers.get(0)) {
            return activePartyMembers.get(0).name;
        }

        if (activePartyMembers.size() > 1 && character == activePartyMembers.get(1)) {
            return activePartyMembers.get(1).name;
        }

        return character.name;
    }

    private Entity getSelectedStatusCharacter() {
        if (inventory.selectedPartyMember == 0) {
            return player;
        }

        int idx = inventory.selectedPartyMember - 1;
        if (idx >= 0 && idx < activePartyMembers.size()) {
            return activePartyMembers.get(idx);
        }

        return player;
    }

    private int getSelectedStatusCharacterIndex() {
        return inventory.selectedPartyMember;
    }

    private Item getCharacterEquipSlot(Entity character, int slot) {
        if (character == null) return null;
        if (slot < 0 || slot >= character.equipped.size()) return null;
        return character.equipped.get(slot);
    }

    private void setCharacterEquipSlot(Entity character, int slot, Item item) {
        if (character == null) return;
        if (slot < 0 || slot >= character.equipped.size()) return;
        character.equipped.set(slot, item);
    }

    private void applyEquipBonuses(Entity character, Item item) {
        if (character == null || item == null) return;

        character.attack += item.attackBonus;
        character.defense += item.defenseBonus;
        character.magicAttack += item.magicBonus;
        character.maxHp += item.hpBonus;
        character.maxMp += item.mpBonus;
        character.speed_stat += item.speedBonus;

        character.hp += item.hpBonus;
        character.mp += item.mpBonus;
    }

    private void removeEquipBonuses(Entity character, Item item) {
        if (character == null || item == null) return;

        character.attack -= item.attackBonus;
        character.defense -= item.defenseBonus;
        character.magicAttack -= item.magicBonus;
        character.maxHp -= item.hpBonus;
        character.maxMp -= item.mpBonus;
        character.speed_stat -= item.speedBonus;

        if (character.hp > character.maxHp) character.hp = character.maxHp;
        if (character.mp > character.maxMp) character.mp = character.maxMp;
    }

    private int getTotalEquippedAttackBonus(Entity e) {
        int total = 0;
        if (e == null || e.equipped == null) return total;

        for (int i = 0; i < e.equipped.size(); i++) {
            Item item = e.equipped.get(i);
            if (item != null) total += item.attackBonus;
        }

        return total;
    }

    private int getTotalEquippedDefenseBonus(Entity e) {
        int total = 0;
        if (e == null || e.equipped == null) return total;

        for (int i = 0; i < e.equipped.size(); i++) {
            Item item = e.equipped.get(i);
            if (item != null) total += item.defenseBonus;
        }

        return total;
    }

    private int getTotalEquippedMagicBonus(Entity e) {
        int total = 0;
        if (e == null || e.equipped == null) return total;

        for (int i = 0; i < e.equipped.size(); i++) {
            Item item = e.equipped.get(i);
            if (item != null) total += item.magicBonus;
        }

        return total;
    }

    private int getTotalEquippedHpBonus(Entity e) {
        int total = 0;
        if (e == null || e.equipped == null) return total;

        for (int i = 0; i < e.equipped.size(); i++) {
            Item item = e.equipped.get(i);
            if (item != null) total += item.hpBonus;
        }

        return total;
    }

    private int getTotalEquippedMpBonus(Entity e) {
        int total = 0;
        if (e == null || e.equipped == null) return total;

        for (int i = 0; i < e.equipped.size(); i++) {
            Item item = e.equipped.get(i);
            if (item != null) total += item.mpBonus;
        }

        return total;
    }

    private int getTotalEquippedSpeedBonus(Entity e) {
        int total = 0;
        if (e == null || e.equipped == null) return total;

        for (int i = 0; i < e.equipped.size(); i++) {
            Item item = e.equipped.get(i);
            if (item != null) total += item.speedBonus;
        }

        return total;
    }

    private int getItemEquippedOwnerIndex(Item item) {
        if (item == null || item.name == null) return -1;

        // 0 = Hero
        for (int i = 0; i < 9; i++) {
            Item equipped = getCharacterEquipSlot(player, i);
            if (equipped != null && equipped.name != null && equipped.name.equals(item.name)) {
                return 0;
            }
        }

        // 1.. = party members
        for (int p = 0; p < activePartyMembers.size(); p++) {
            PartyMember member = activePartyMembers.get(p);

            for (int i = 0; i < 9; i++) {
                Item equipped = getCharacterEquipSlot(member, i);
                if (equipped != null && equipped.name != null && equipped.name.equals(item.name)) {
                    return p + 1;
                }
            }
        }

        return -1;
    }

    private String getEquipOwnerMarker(Item item) {
        int ownerIndex = getItemEquippedOwnerIndex(item);

        if (ownerIndex == 0) return "[H]";
        if (ownerIndex == 1) return "[A]";
        if (ownerIndex == 2) return "[M]";

        return "";
    }

    private String getEquipOwnerName(Item item) {
        int ownerIndex = getItemEquippedOwnerIndex(item);

        if (ownerIndex == 0) return player.name;

        int partyIndex = ownerIndex - 1;
        if (partyIndex >= 0 && partyIndex < activePartyMembers.size()) {
            return activePartyMembers.get(partyIndex).name;
        }

        return "";
    }

    private void handleStatusEquipAction() {
        ArrayList<Item> allEquipItems = getStatusEquipmentDisplayList();

        if (allEquipItems.isEmpty()) return;
        if (inventory.selectedEquipmentListIndex < 0) inventory.selectedEquipmentListIndex = 0;
        if (inventory.selectedEquipmentListIndex >= allEquipItems.size()) {
            inventory.selectedEquipmentListIndex = allEquipItems.size() - 1;
        }

        Entity selectedCharacter = getSelectedStatusCharacter();
        int selectedCharacterIndex = getSelectedStatusCharacterIndex();

        Item selected = allEquipItems.get(inventory.selectedEquipmentListIndex);
        if (selectedCharacter == null || selected == null) return;

        int targetSlot = getEquipSlotForItem(selected);
        if (targetSlot == -1) return;

        if (!canCharacterEquipItem(selectedCharacter, selected)) {
            return;
        }

        int ownerIndex = getItemEquippedOwnerIndex(selected);

        // =========================================
        // CASE 1: item not equipped by anyone
        // =========================================
        if (ownerIndex == -1) {
            Item currentItemInSlot = getCharacterEquipSlot(selectedCharacter, targetSlot);

            // βγάλε το current item από το slot και γύρνα το inventory
            if (currentItemInSlot != null) {
                removeEquipBonuses(selectedCharacter, currentItemInSlot);
                inventory.addItem(currentItemInSlot);
                setCharacterEquipSlot(selectedCharacter, targetSlot, null);
            }

            // βρες το selected item στο storage
            int storageIndex = -1;
            for (int i = 0; i < inventory.storage.length; i++) {
                Item storageItem = inventory.storage[i];
                if (storageItem != null && storageItem.name != null &&
                    storageItem.name.equals(selected.name)) {
                    storageIndex = i;
                    break;
                }
            }

            if (storageIndex != -1) {
                Item itemToEquip = inventory.storage[storageIndex];
                inventory.storage[storageIndex] = null;

                setCharacterEquipSlot(selectedCharacter, targetSlot, itemToEquip);
                applyEquipBonuses(selectedCharacter, itemToEquip);
                playSound("equip");
            }
        }

        // =========================================
        // CASE 2: item equipped by selected character -> unequip
        // =========================================
        else if (ownerIndex == selectedCharacterIndex) {
            Item currentItem = getCharacterEquipSlot(selectedCharacter, targetSlot);

            if (currentItem != null) {
                removeEquipBonuses(selectedCharacter, currentItem);

                if (inventory.addItem(currentItem)) {
                    setCharacterEquipSlot(selectedCharacter, targetSlot, null);
                    playSound("unequip");
                }
            }
        }

        // =========================================
        // CASE 3: item equipped by another character -> transfer
        // =========================================
        else {
            Entity oldOwner;

            if (ownerIndex == 0) {
                oldOwner = player;
            } else {
                int partyIndex = ownerIndex - 1;
                if (partyIndex < 0 || partyIndex >= activePartyMembers.size()) return;
                oldOwner = activePartyMembers.get(partyIndex);
            }

            Item itemFromOldOwner = getCharacterEquipSlot(oldOwner, targetSlot);
            Item currentItemInSelectedSlot = getCharacterEquipSlot(selectedCharacter, targetSlot);

            if (itemFromOldOwner == null) return;

            // 1. βγάλε το item από τον old owner
            removeEquipBonuses(oldOwner, itemFromOldOwner);
            setCharacterEquipSlot(oldOwner, targetSlot, null);

            // 2. αν ο selected character είχε item στο ίδιο slot, γύρνα το inventory
            if (currentItemInSelectedSlot != null) {
                removeEquipBonuses(selectedCharacter, currentItemInSelectedSlot);
                inventory.addItem(currentItemInSelectedSlot);
                setCharacterEquipSlot(selectedCharacter, targetSlot, null);
            }

            // 3. βάλε το transferred item στον selected character
            setCharacterEquipSlot(selectedCharacter, targetSlot, itemFromOldOwner);
            applyEquipBonuses(selectedCharacter, itemFromOldOwner);

            playSound("equip");
        }
    }

    private boolean isPotionItem(Item item) {
        if (item == null || item.name == null) return false;

        String name = item.name.toLowerCase();
        return name.contains("potion");
    }

    private boolean isWeaponCategoryItem(Item item) {
        if (item == null) return false;
        return getEquipSlotForItem(item) == 3;
    }

    private boolean isShieldItem(Item item) {
        if (item == null || item.name == null) return false;

        String name = item.name.toLowerCase();
        return name.contains("shield");
    }

    private boolean isHelmetItem(Item item) {
        if (item == null || item.name == null) return false;

        String name = item.name.toLowerCase();
        return name.contains("helmet") || name.contains("hat");
    }

    private boolean isBodyArmorItem(Item item) {
        if (item == null || item.name == null) return false;

        String name = item.name.toLowerCase();
        return name.contains("armor") || name.contains("chest");
    }

    private boolean isNecklaceItem(Item item) {
        if (item == null || item.name == null) return false;

        String name = item.name.toLowerCase();
        return name.contains("necklace") || name.contains("amulet");
    }

    private boolean isConsumableCategoryItem(Item item) {
        if (item == null) return false;

        // προς το παρόν consumables = potions / heal items
        return item.healAmount > 0 || item.mpBonus > 0;
    }

    private boolean isScrollItem(Item item) {
        if (item == null || item.name == null) return false;

        String name = item.name.toLowerCase();
        return name.contains("scroll");
    }

    private String getItemDescription(Item item) {
        if (item == null) return "";

        if (isPotionItem(item)) {
            if (item.healAmount > 0 && item.mpBonus > 0) {
                return "HP +" + item.healAmount + " / MP +" + item.mpBonus;
            }
            if (item.healAmount > 0) {
                return "HP +" + item.healAmount;
            }
            if (item.mpBonus > 0) {
                return "MP +" + item.mpBonus;
            }
            return "Potion item.";
        }

        if (isWeaponCategoryItem(item)) {
            StringBuilder sb = new StringBuilder("Weapon.\n");

            if (item.attackBonus != 0) {
                sb.append("ATK +").append(item.attackBonus);
            }
            if (item.magicBonus != 0) {
                if (sb.length() > 8) sb.append("\n");
                sb.append("MAG +").append(item.magicBonus);
            }

            return sb.toString();
        }

        if (isShieldItem(item)) {
            return "Shield.\nDEF +" + item.defenseBonus;
        }

        if (isHelmetItem(item)) {
            return "Helmet.\nDEF +" + item.defenseBonus;
        }

        if (isBodyArmorItem(item)) {
            StringBuilder sb = new StringBuilder("Body armor.\n");

            if (item.defenseBonus != 0) {
                sb.append("DEF +").append(item.defenseBonus);
            }
            if (item.hpBonus != 0) {
                if (sb.length() > 12) sb.append("\n");
                sb.append("HP +").append(item.hpBonus);
            }

            return sb.toString();
        }

        if (isNecklaceItem(item)) {
            StringBuilder sb = new StringBuilder("Accessory.\n");

            if (item.magicBonus != 0) {
                sb.append("MAG +").append(item.magicBonus);
            }
            if (item.mpBonus != 0) {
                if (sb.length() > 11) sb.append("\n");
                sb.append("MP +").append(item.mpBonus);
            }

            return sb.toString();
        }

        if (isScrollItem(item)) {
            return "A skill scroll.\nUsed to learn abilities later.";
        }

        if (isConsumableCategoryItem(item)) {
            if (item.healAmount > 0) {
                return "HP +" + item.healAmount;
            }
            if (item.mpBonus > 0) {
                return "MP +" + item.mpBonus;
            }
            return "Consumable item.";
        }

        return "Item.";
    }

    private ArrayList<Item> getAllInventoryItemsList() {
        ArrayList<Item> result = new ArrayList<>();

        for (int i = 0; i < inventory.storage.length; i++) {
            Item item = inventory.storage[i];
            if (item != null) {
                result.add(item);
            }
        }

        return result;
    }

    private ArrayList<Item> getFilteredItemsList() {
        ArrayList<Item> allItems = getAllInventoryItemsList();
        ArrayList<Item> result = new ArrayList<>();

        int cat = inventory.selectedItemsCategory;

        for (int i = 0; i < allItems.size(); i++) {
            Item item = allItems.get(i);

            if (cat == 0) { // ALL
                result.add(item);
            }
            else if (cat == 1 && isPotionItem(item)) {
                result.add(item);
            }
            else if (cat == 2 && isWeaponCategoryItem(item)) {
                result.add(item);
            }
            else if (cat == 3 && isShieldItem(item)) {
                result.add(item);
            }
            else if (cat == 4 && isHelmetItem(item)) {
                result.add(item);
            }
            else if (cat == 5 && isBodyArmorItem(item)) {
                result.add(item);
            }
            else if (cat == 6 && isNecklaceItem(item)) {
                result.add(item);
            }
            else if (cat == 7 && isConsumableCategoryItem(item)) {
                result.add(item);
            }
            else if (cat == 8 && isScrollItem(item)) {
                result.add(item);
            }
        }

        return result;
    }

    private BufferedImage getItemCategoryIcon(int categoryIndex) {
        switch (categoryIndex) {
            case 0: return itemCatAllIcon;
            case 1: return itemCatPotionsIcon;
            case 2: return itemCatWeaponsIcon;
            case 3: return itemCatShieldsIcon;
            case 4: return itemCatHelmetsIcon;
            case 5: return itemCatBodyArmorIcon;
            case 6: return itemCatNecklacesIcon;
            case 7: return itemCatConsumablesIcon;
            case 8: return itemCatScrollsIcon;
        }
        return null;
    }

    private void drawItemCategoryIcons(Graphics2D g2, int leftX, int centerX, int centerY) {
        int iconSize = 30;
        int boxSize = 40;
        int gap = 8;

        // πιο αριστερά, στο κενό ανάμεσα στο left panel και στο center panel
        int startX = leftX + 200;
        int startY = centerY + 52;

        for (int i = 0; i < itemCategoryOptions.length; i++) {
            int drawX = startX;
            int drawY = startY + i * (boxSize + gap);

            boolean selected = (inventory.selectedItemsCategory == i);

            // square box
            g2.setColor(new Color(25, 25, 25, 210));
            g2.fillRoundRect(drawX, drawY, boxSize, boxSize, 8, 8);

            if (selected) {
                g2.setColor(new Color(180, 130, 60, 200));
                g2.fillRoundRect(drawX, drawY, boxSize, boxSize, 8, 8);
            }

            g2.setColor(new Color(180, 150, 90, 180));
            g2.drawRoundRect(drawX, drawY, boxSize, boxSize, 8, 8);

            BufferedImage icon = getItemCategoryIcon(i);
            if (icon != null) {
                g2.drawImage(icon, drawX + 4, drawY + 4, iconSize, iconSize, null);
            } else {
                g2.setColor(new Color(240, 230, 210));
                g2.setFont(g2.getFont().deriveFont(Font.BOLD, 12f));
                g2.drawString("" + i, drawX + 14, drawY + 24);
            }
        }
    }

    private Entity getSelectedItemUseTarget() {
        if (inventory.itemUseTargetIndex == 0) {
            return player;
        }

        int idx = inventory.itemUseTargetIndex - 1;
        if (idx >= 0 && idx < activePartyMembers.size()) {
            return activePartyMembers.get(idx);
        }

        return player;
    }

    private int getMaxItemUseTargetIndex() {
        return activePartyMembers.size(); // 0 = hero, 1.. = party members
    }

    private boolean isUsableItemFromItemsMenu(Item item) {
        if (item == null) return false;

        return item.healAmount > 0 || item.mpBonus > 0;
    }

    private void applyItemToTarget(Item item, Entity target) {
        if (item == null || target == null) return;

        if (item.healAmount > 0) {
            target.hp += item.healAmount;
            if (target.hp > target.maxHp) target.hp = target.maxHp;
        }

        if (item.mpBonus > 0) {
            target.mp += item.mpBonus;
            if (target.mp > target.maxMp) target.mp = target.maxMp;
        }
    }

    private void consumeOneItemFromInventory(Item selected) {
        if (selected == null || selected.name == null) return;

        for (int i = 0; i < inventory.storage.length; i++) {
            Item storageItem = inventory.storage[i];
            if (storageItem != null && storageItem.name != null &&
                storageItem.name.equals(selected.name)) {
                inventory.removeFromStorage(i);
                return;
            }
        }
    }

    private void initWorldMapRegions() {
        worldMapRegions.clear();

        worldMapRegions.add(new WorldMapRegion(
            "town_01",
            "Town",
            400, 300, 40, 40,
            "town",
            true,
            "TOWN_01"
        ));

        worldMapRegions.add(new WorldMapRegion(
            "fields_01",
            "Southern Fields",
            400, 350, 40, 40,
            "field",
            false,
            "FIELDS_01"
        ));

        worldMapRegions.add(new WorldMapRegion(
            "cave_01",
            "Northern Cave",
            360, 240, 40, 40,
            "cave",
            false,
            "CAVE_01"
        ));

        worldMapRegions.add(new WorldMapRegion(
            "pass_01",
            "Mountain Pass",
            420, 90, 40, 40,
            "path",
            false,
            "PASS_01"
        ));
    }

    private int findMapIndexByName(String mapName) {
        if (mapName == null || mapName.trim().isEmpty()) return -1;

        for (int i = 0; i < 100; i++) {
            AdvancedMapData map = tileM.getMap(i);
            if (map == null) break;

            if (map.name != null && map.name.equalsIgnoreCase(mapName)) {
                return i;
            }
        }

        return -1;
    }

    private void resetPartyFollowAfterTeleport() {
        // Καθάρισε όλο το παλιό history του player,
        // αλλιώς οι followers θα προσπαθούν να πάνε στις παλιές θέσεις
        playerPositions.clear();

        // Βάλε ξανά τα party members ακριβώς πίσω από τον player
        for (int i = 0; i < activePartyMembers.size(); i++) {
            PartyMember pm = activePartyMembers.get(i);

            pm.worldX = player.worldX - ((i + 1) * tileSize);
            pm.worldY = player.worldY;
            pm.direction = player.direction;
            pm.frame = 0;
            pm.counter = 0;
            pm.updateImage();
        }

        // Ξαναγέμισε το history με τη ΝΕΑ θέση του player,
        // ώστε το follow system να θεωρεί ότι πάντα ήταν εδώ
        for (int i = 0; i < 200; i++) {
            playerPositions.add(new Point(player.worldX, player.worldY));
        }
    }

    private void fastTravelToRegion(WorldMapRegion region) {
        if (region == null) return;
        if (!region.travelEnabled) return;
        if (region.targetMapName == null || region.targetMapName.isEmpty()) return;

        int targetMapIndex = findMapIndexByName(region.targetMapName);
        if (targetMapIndex == -1) return;

        currentMap = targetMapIndex;
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

        // βάλε τα party members πίσω από τον player
        resetPartyFollowAfterTeleport();
    }

    private BufferedImage getWorldMapRegionIcon(WorldMapRegion region) {
        if (region == null || region.type == null) return null;

        switch (region.type.toLowerCase()) {
            case "town":
                return worldMapTownIcon;
            case "path":
                return worldMapPathIcon;
            case "forest":
            case "field":
            case "wild":
                return worldMapForestIcon;
            case "cave":
                return worldMapCaveIcon;
        }

        return null;
    }

    private WorldMapRegion getHoveredWorldMapRegion() {
        for (int i = 0; i < worldMapRegions.size(); i++) {
            WorldMapRegion region = worldMapRegions.get(i);
            if (region.contains(mapCursorX, mapCursorY)) {
                return region;
            }
        }
        return null;
    }

    public void drawJournalScreen(Graphics2D g2) {
        int screenW = screenWidth;
        int screenH = screenHeight;

        // backdrop
        if (worldMapBackground != null) {
            g2.drawImage(worldMapBackground, 0, 0, screenW, screenH, null);
        } else {
            g2.setColor(new Color(0, 0, 0, 170));
            g2.fillRect(0, 0, screenW, screenH);
        }

        g2.setColor(new Color(0, 0, 0, 70));
        g2.fillRect(0, 0, screenW, screenH);

        int leftX = 0;
        int leftY = 0;
        int leftW = 220;
        int leftH = screenH - 70;

        int centerX = leftW + 18;
        int centerY = 20;
        int centerW = screenW - centerX - 18;
        int centerH = screenH - 90;

        Color panelDark = new Color(0, 0, 0, 145);
        Color panelMid = new Color(0, 0, 0, 135);
        Color border = new Color(180, 150, 90, 140);
        Color textMain = new Color(240, 230, 210);
        Color textDim = new Color(180, 170, 150);

        // left panel
        g2.setColor(panelDark);
        g2.fillRect(leftX, leftY, leftW, leftH);
        g2.setColor(border);
        g2.drawLine(leftW - 1, 0, leftW - 1, leftH);

        // center panel
        g2.setColor(panelMid);
        g2.fillRoundRect(centerX, centerY, centerW, centerH, 20, 20);
        g2.setColor(border);
        g2.drawRoundRect(centerX, centerY, centerW, centerH, 20, 20);

        // left menu title
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 28f));
        g2.setColor(textMain);
        g2.drawString("Menu", leftX + 24, leftY + 42);

        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 22f));
        int optionY = leftY + 90;

        for (int i = 0; i < mainMenuOptions.length; i++) {
            boolean selected = (inventory.menuFocus == 0 && inventory.menuSection == i);

            if (selected) {
                g2.setColor(new Color(180, 130, 60, 180));
                g2.fillRoundRect(leftX + 10, optionY - 24, leftW - 20, 36, 14, 14);
                g2.setColor(new Color(210, 180, 90, 220));
            } else {
                g2.setColor(textMain);
            }

            g2.drawString(mainMenuOptions[i], leftX + 28, optionY);
            optionY += 52;
        }

        // header
        g2.setFont(maruMonicaLarge);
        g2.setColor(textMain);
        g2.drawString("Journal", centerX + 20, centerY + 40);

        int y = centerY + 90;

        g2.setFont(maruMonicaBold);
        g2.setColor(new Color(255, 220, 140));
        g2.drawString("Current Chapter", centerX + 25, y);

        y += 28;
        g2.setFont(maruMonica);
        g2.setColor(Color.WHITE);
        g2.drawString(storyManager.currentChapter != null ? storyManager.currentChapter : "Unknown", centerX + 25, y);

        y += 50;
        g2.setFont(maruMonicaBold);
        g2.setColor(new Color(255, 220, 140));
        g2.drawString("Current Objective", centerX + 25, y);

        y += 28;
        g2.setFont(maruMonicaBold);
        g2.setColor(Color.WHITE);

        String objTitle = "No active objective";
        String objDesc = "";

        if (storyManager.currentObjective != null) {
            objTitle = storyManager.currentObjective.title;
            objDesc = storyManager.currentObjective.description;
        }

        g2.drawString(objTitle, centerX + 25, y);

        y += 34;
        g2.setFont(maruMonica);
        g2.setColor(textMain);

        String[] lines = wrapText(objDesc, 58);
        for (String line : lines) {
            g2.drawString(line, centerX + 25, y);
            y += 24;
        }

        y += 24;
        g2.setFont(maruMonicaSmall);
        g2.setColor(textDim);
        g2.drawString("Press X / ESC / I to return", centerX + 25, centerY + centerH - 20);
    }

    public void drawObjectivePopup(Graphics2D g2) {
        if (!showObjectivePopup) return;

        int width = 420;
        int height = 110;
        int x = screenWidth - width - 20;
        int y = 20;

        float alpha = 1.0f;
        if (objectivePopupTimer < 40) {
            alpha = objectivePopupTimer / 40.0f;
        }

        java.awt.Composite oldComposite = g2.getComposite();
        g2.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, alpha));

        // background
        g2.setColor(new Color(20, 20, 30, 220));
        g2.fillRoundRect(x, y, width, height, 20, 20);

        // border
        g2.setColor(new Color(180, 180, 220));
        g2.setStroke(new BasicStroke(2f));
        g2.drawRoundRect(x, y, width, height, 20, 20);

        // title label
        g2.setFont(maruMonicaBold);
        g2.setColor(new Color(255, 220, 140));
        g2.drawString("New Objective", x + 20, y + 28);

        // objective title
        g2.setFont(maruMonicaBold.deriveFont(20f));
        g2.setColor(Color.WHITE);
        g2.drawString(objectivePopupTitle, x + 20, y + 55);

        // description
        g2.setFont(maruMonicaSmall);
        g2.setColor(new Color(210, 210, 210));

        String[] lines = wrapText(objectivePopupDescription, 42);
        int lineY = y + 78;
        for (int i = 0; i < lines.length && i < 2; i++) {
            g2.drawString(lines[i], x + 20, lineY);
            lineY += 18;
        }

        g2.setComposite(oldComposite);
    }

    public String[] wrapText(String text, int maxCharsPerLine) {
        if (text == null || text.isEmpty()) return new String[]{""};

        java.util.ArrayList<String> lines = new java.util.ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder current = new StringBuilder();

        for (String word : words) {
            if (current.length() == 0) {
                current.append(word);
            } else if (current.length() + 1 + word.length() <= maxCharsPerLine) {
                current.append(" ").append(word);
            } else {
                lines.add(current.toString());
                current = new StringBuilder(word);
            }
        }

        if (current.length() > 0) {
            lines.add(current.toString());
        }

        return lines.toArray(new String[0]);
    }

    public void showObjectivePopup(String title, String description) {
        showObjectivePopup = true;
        objectivePopupTitle = title != null ? title : "";
        objectivePopupDescription = description != null ? description : "";
        objectivePopupTimer = OBJECTIVE_POPUP_DURATION;
    }


    // ====================================
    //      END OF DRAW MENU HELPERS
    // =================================== 

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
        // ===== FULLSCREEN MAP BACKGROUND =====
        if (worldMapBackground != null) {
            g2.drawImage(worldMapBackground, 0, 0, screenWidth, screenHeight, null);
        } else {
            GradientPaint gpMap = new GradientPaint(
                0, 0, new Color(72, 63, 50),
                0, screenHeight, new Color(42, 37, 30)
            );
            g2.setPaint(gpMap);
            g2.fillRect(0, 0, screenWidth, screenHeight);
        }

        // ελαφρύ dark overlay για να δένουν τα icons/pointer
        g2.setColor(new Color(0, 0, 0, 40));
        g2.fillRect(0, 0, screenWidth, screenHeight);

        // ===== REGION ICONS =====
        for (int i = 0; i < worldMapRegions.size(); i++) {
            WorldMapRegion region = worldMapRegions.get(i);

            BufferedImage icon = getWorldMapRegionIcon(region);
            int iconX = region.x;
            int iconY = region.y;
            int iconSize = 26;

            if (icon != null) {
                g2.drawImage(icon, iconX, iconY, iconSize, iconSize, null);
            } else {
                g2.setColor(new Color(230, 220, 180));
                g2.fillOval(iconX, iconY, iconSize, iconSize);
            }
        }

        // ===== CURSOR POINTER =====
        if (menuPointer != null) {
            g2.drawImage(menuPointer, mapCursorX - 12, mapCursorY - 12, 24, 24, null);
        } else {
            g2.setColor(new Color(255, 220, 140));
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 20f));
            g2.drawString("X", mapCursorX - 6, mapCursorY + 6);
        }

        // ===== HOVER POPUP =====
        if (hoveredMapRegionName != null && !hoveredMapRegionName.isEmpty()) {
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 18f));
            FontMetrics fm = g2.getFontMetrics();

            int popupTextW = fm.stringWidth(hoveredMapRegionName);
            int popupW = popupTextW + 26;
            int popupH = 34;

            int popupX = mapCursorX + 18;
            int popupY = mapCursorY - 10;

            // να μη βγαίνει εκτός οθόνης δεξιά
            if (popupX + popupW > screenWidth - 10) {
                popupX = mapCursorX - popupW - 18;
            }

            // να μη βγαίνει εκτός οθόνης πάνω
            if (popupY < 10) {
                popupY = 10;
            }

            // crystal black popup shadow
            g2.setColor(new Color(0, 0, 0, 90));
            g2.fillRoundRect(popupX + 3, popupY + 3, popupW, popupH, 12, 12);

            // crystal black body
            g2.setColor(new Color(10, 10, 10, 185));
            g2.fillRoundRect(popupX, popupY, popupW, popupH, 12, 12);

            // border
            g2.setColor(new Color(220, 220, 220, 70));
            g2.drawRoundRect(popupX, popupY, popupW, popupH, 12, 12);

            // text
            g2.setColor(new Color(245, 235, 220));
            g2.drawString(hoveredMapRegionName, popupX + 13, popupY + 22);
        }

        // ===== FOOTER =====
        int footerH = 42;
        int footerY = screenHeight - footerH;

        g2.setColor(new Color(0, 0, 0, 150));
        g2.fillRect(0, footerY, screenWidth, footerH);

        g2.setColor(new Color(240, 230, 210));
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 16f));
        g2.drawString("Arrow Keys Move Cursor   Esc Back", 20, footerY + 26);
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

    // =================================
    //      END OF DRAW METHODS
    // =================================

    public void drawLighting(Graphics2D g2) {
        // ΑΝ ΕΙΜΑΣΤΕ ΣΕ INTERIOR (χάρτες 4,5) ΜΗΝ ΕΦΑΡΜΟΣΕΙΣ ΣΚΟΤΑΔΙ
        if (currentMap == 3 || currentMap == 4 || currentMap == 5) {
            return; // Βγες αμέσως, κανένα εφέ
        }
        // Αν είναι μέρα και δεν έχει αρχίσει ακόμα να σκοτεινιάζει
        if (currentMap == 2) {
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