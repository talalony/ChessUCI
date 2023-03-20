import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.PrintStream;

public class PositionFrame extends JFrame {

    Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();

    Image pawnImage = new Pawn(0,0,true, 0).getImg();
    Image knightImage = new Knight(0,0,true, 0).getImg();
    Image bishopImage = new Bishop(0,0,true, 0).getImg();
    Image rookImage = new Rook(0,0,true, 0).getImg();
    Image queenImage = new Queen(0,0,true, 0).getImg();
    Image kingImage = new King(0,0,true, 0).getImg();

    public boolean isPawnPressed = false;
    public boolean isKnightPressed = false;
    public boolean isBishopPressed = false;
    public boolean isRookPressed = false;
    public boolean isQueenPressed = false;
    public boolean isKingPressed = true;
    MyButton pawnButton;
    MyButton knightButton;
    MyButton bishopButton;
    MyButton rookButton;
    MyButton queenButton;
    MyButton kingButton;


    MyButton whiteToMove;
    MyButton blackToMove;
    public boolean toMove = true;

    MyButton whiteCKS;
    MyButton whiteCQS;
    MyButton blackCKS;
    MyButton blackCQS;

    public boolean whiteCastleKS;
    public boolean whiteCastleQS;
    public boolean blackCastleKS;
    public boolean blackCastleQS;
    JTextArea enPassant;

    MyButton OK;
    MyButton can;

    public boolean isConfirmed = false;

    Color blue = new Color(50, 139, 168);


    public PositionFrame() throws IOException {
        this.setTitle("Set Position");
        this.setSize(500, 400);
//        this.setLocationRelativeTo(null);
        this.setLocation((dim.width/2)+(dim.width/12), (dim.height/2)-(dim.height/6));
        this.setResizable(false);
        this.setVisible(true);
        this.setLayout(null);
        this.setAlwaysOnTop(true);

        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        this.addWindowListener( new WindowAdapter()
        {
            public void windowClosing(WindowEvent e)
            {
                GamePanel.isPositionFrameAlive = false;
                e.getWindow().dispose();
            }
        });

        ActionListener pieceAction = e -> {
            isPawnPressed = false;
            isKnightPressed = false;
            isBishopPressed = false;
            isRookPressed = false;
            isQueenPressed = false;
            isKingPressed = false;

            pawnButton.setBorder(BorderFactory.createEmptyBorder());
            knightButton.setBorder(BorderFactory.createEmptyBorder());
            bishopButton.setBorder(BorderFactory.createEmptyBorder());
            rookButton.setBorder(BorderFactory.createEmptyBorder());
            queenButton.setBorder(BorderFactory.createEmptyBorder());
            kingButton.setBorder(BorderFactory.createEmptyBorder());
            if (e.getSource() == pawnButton) {
                isPawnPressed = true;
                pawnButton.setBorder(BorderFactory.createLineBorder(blue, 2));
            }
            if (e.getSource() == knightButton) {
                isKnightPressed = true;
                knightButton.setBorder(BorderFactory.createLineBorder(blue, 2));
            }
            if (e.getSource() == bishopButton) {
                isBishopPressed = true;
                bishopButton.setBorder(BorderFactory.createLineBorder(blue, 2));
            }
            if (e.getSource() == rookButton) {
                isRookPressed = true;
                rookButton.setBorder(BorderFactory.createLineBorder(blue, 2));
            }
            if (e.getSource() == queenButton) {
                isQueenPressed = true;
                queenButton.setBorder(BorderFactory.createLineBorder(blue, 2));
            }
            if (e.getSource() == kingButton) {
                isKingPressed = true;
                kingButton.setBorder(BorderFactory.createLineBorder(blue, 2));
            }
        };
        ActionListener sideAction = e -> {
            if (e.getSource() == whiteToMove) {
                whiteToMove.setBorder(BorderFactory.createLineBorder(blue, 2));
                blackToMove.setBorder(BorderFactory.createEmptyBorder());
                toMove = true;
            }
            if (e.getSource() == blackToMove) {
                blackToMove.setBorder(BorderFactory.createLineBorder(blue, 2));
                whiteToMove.setBorder(BorderFactory.createEmptyBorder());
                toMove = false;
            }
        };

        ActionListener castlingAction = e -> {
            if (e.getSource() == whiteCKS) {
                if (whiteCastleKS)
                    whiteCKS.setBorder(BorderFactory.createEmptyBorder());
                else
                    whiteCKS.setBorder(BorderFactory.createLineBorder(blue, 2));
                whiteCastleKS = !whiteCastleKS;
            }
            if (e.getSource() == whiteCQS) {
                if (whiteCastleQS)
                    whiteCQS.setBorder(BorderFactory.createEmptyBorder());
                else
                    whiteCQS.setBorder(BorderFactory.createLineBorder(blue, 2));
                whiteCastleQS = !whiteCastleQS;
            }

            if (e.getSource() == blackCKS) {
                if (blackCastleKS)
                    blackCKS.setBorder(BorderFactory.createEmptyBorder());
                else
                    blackCKS.setBorder(BorderFactory.createLineBorder(blue, 2));
                blackCastleKS = !blackCastleKS;
            }
            if (e.getSource() == blackCQS) {
                if (blackCastleQS)
                    blackCQS.setBorder(BorderFactory.createEmptyBorder());
                else
                    blackCQS.setBorder(BorderFactory.createLineBorder(blue, 2));
                blackCastleQS = !blackCastleQS;
            }

            if (e.getSource() == OK) {
                isConfirmed = true;
                GamePanel.isPositionFrameAlive = false;
                this.dispose();
            }
            if (e.getSource() == can) {
                GamePanel.isPositionFrameAlive = false;
                this.dispose();
            }
        };

        pawnButton = makeButton("", 2,2, 70, 70, pieceAction, pawnImage, false);

        knightButton = makeButton("", 74,2, 70, 70, pieceAction, knightImage, false);

        bishopButton = makeButton("", 2,74, 70, 70, pieceAction, bishopImage, false);

        rookButton = makeButton("", 74,74, 70, 70, pieceAction, rookImage, false);

        queenButton = makeButton("", 2,146, 70, 70, pieceAction, queenImage, false);

        kingButton = makeButton("", 74,146, 70, 70, pieceAction, kingImage, true);


        this.add(pawnButton);
        this.add(knightButton);
        this.add(bishopButton);
        this.add(rookButton);
        this.add(queenButton);
        this.add(kingButton);

        whiteToMove = makeButton("White To Move", 22,220, 100, 25, sideAction, null, true);

        blackToMove = makeButton("Black To Move", 22,250, 100, 25, sideAction, null, false);

        this.add(whiteToMove);
        this.add(blackToMove);

        whiteCKS = makeButton("White Castle Short", 160, 65, 130, 25, castlingAction, null, false);

        whiteCQS = makeButton("White Castle Long", 160, 135, 130, 25, castlingAction, null, false);

        blackCKS = makeButton("Black Castle Short", 300, 65, 130, 25, castlingAction, null, false);

        blackCQS = makeButton("Black Castle Long", 300, 135, 130, 25, castlingAction, null, false);

        this.add(whiteCKS);
        this.add(whiteCQS);
        this.add(blackCKS);
        this.add(blackCQS);

        enPassant = new JTextArea();
        enPassant.setLocation(300, 200);
        enPassant.setSize(70, 20);
        enPassant.setBorder(BorderFactory.createLineBorder(Color.darkGray, 1));

        JLabel label = new JLabel("En Passant:");
        label.setLocation(225, 200);
        label.setSize(70, 20);
        label.setBorder(BorderFactory.createEmptyBorder());

        this.add(enPassant);
        this.add(label);

        OK = new MyButton("Confirm");
        OK.setBounds(30, 320, 100, 30);
        OK.setFocusable(false);
        OK.setHoverBackgroundColor(Color.white.darker().darker());
        OK.setPressedBackgroundColor(Color.white.darker());
        OK.setForeground(Color.black);
        OK.setBackground(Color.gray.brighter());
        OK.setBorder(BorderFactory.createEmptyBorder());
        OK.addActionListener(castlingAction);

        can = new MyButton("Cancel");
        can.setBounds(350, 320, 100, 30);
        can.setFocusable(false);
        can.setHoverBackgroundColor(Color.white.darker().darker());
        can.setPressedBackgroundColor(Color.white.darker());
        can.setForeground(Color.black);
        can.setBackground(Color.gray.brighter());
        can.setBorder(BorderFactory.createEmptyBorder());
        can.addActionListener(castlingAction);

        this.add(OK);
        this.add(can);
        GamePanel.isPositionFrameAlive = true;
    }

    MyButton makeButton(String text, int x, int y, int width, int height, ActionListener l, Image img, boolean border) {
        MyButton button;
        if (text.equals(""))
            button = new MyButton(true);
        else
            button = new MyButton(text,true);
        if (border)
            button.setBorder(BorderFactory.createLineBorder(blue, 2));
        else
            button.setBorder(BorderFactory.createEmptyBorder());
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
