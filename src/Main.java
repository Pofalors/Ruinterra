import javax.swing.JFrame;

public class Main {
    public static void main(String[] args) {
        JFrame window = new JFrame();
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setResizable(false); 
        window.setTitle("RPG");

        GamePanel gamePanel = new GamePanel(window);
        window.add(gamePanel);
        window.pack(); // Κάνει το παράθυρο να πάρει το μέγεθος του GamePanel

        window.setLocationRelativeTo(null); // Το παράθυρο να εμφανιστεί στο κέντρο
        window.setVisible(true);

        gamePanel.startGameThread();
    }
}