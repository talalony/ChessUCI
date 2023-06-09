import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.IOException;

public class ChoosePieceFrame extends JDialog {
    Image knightImage;
    Image bishopImage;
    Image rookImage;
    Image queenImage;
    int pressed = 0;
    MyButton knightButton;
    MyButton bishopButton;
    MyButton rookButton;
    MyButton queenButton;

    Color blue = new Color(50, 139, 168);

    Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();

    public ChoosePieceFrame(boolean color, int xloc, int yloc, Integer[] square) throws IOException {
        this.setUndecorated(true);
        this.setSize(GamePanel.unitSize, 300);
        this.setFocusable(true);
        int height = (square[0] == 7) ? 3*GamePanel.unitSize : GamePanel.unitSize;
        int width = square[1] * GamePanel.unitSize;
        this.setLocation(xloc + width, yloc+height);
        this.setResizable(false);
        this.setLayout(null);
        this.setAlwaysOnTop(true);
        this.setModal(true);
        Color defaultBackground = getBackground();
        setBackground(new Color(defaultBackground.getRed(), defaultBackground.getGreen(), defaultBackground.getBlue(), 240));
        setOpacity(0.95f);

        GamePanel.isChoosePieceAlive = true;

        knightImage = new Knight(0,0,color, 0).getImg();
        bishopImage = new Bishop(0,0,color, 0).getImg();
        rookImage = new Rook(0,0,color, 0).getImg();
        queenImage = new Queen(0,0,color, 0).getImg();

        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        ActionListener pieceAction = e -> {
            if (e.getSource() == knightButton) {
                pressed = 1;
                dispose();
            }
            if (e.getSource() == bishopButton) {
                pressed = 2;
                dispose();
            }
            if (e.getSource() == rookButton) {
                pressed = 3;
                dispose();
            }
            if (e.getSource() == queenButton) {
                pressed = 4;
                dispose();
            }
            GamePanel.isChoosePieceAlive = false;
            GamePanel.turn = !GamePanel.turn;
        };

        int[] xplacments = (color) ?  new int[] {4,78,152,226} : new int[] {226,152,78,4};
        knightButton = makeButton(2,xplacments[3], 70, 70, pieceAction, knightImage);

        bishopButton = makeButton(2,xplacments[2], 70, 70, pieceAction, bishopImage);

        rookButton = makeButton(2,xplacments[1], 70, 70, pieceAction, rookImage);

        queenButton = makeButton(2,xplacments[0], 70, 70, pieceAction, queenImage);


        this.add(knightButton);
        this.add(bishopButton);
        this.add(rookButton);
        this.add(queenButton);

        this.setVisible(true);

    }

    public void paint(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Color.white);
//        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
        super.paint(g);
    }


    MyButton makeButton(int x, int y, int width, int height, ActionListener l, Image img) {
        MyButton button;
        button = new MyButton("",true);
        button.setBackground(new Color(0, 0, 0, 0));
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setBounds(x, y, width, height);
        button.setFocusable(false);
        if (img != null)
            button.setIcon(new ImageIcon(img));
        button.setHoverBackgroundColor(blue);
        button.setPressedBackgroundColor(blue);
        button.addActionListener(l);
        return button;
    }
}
