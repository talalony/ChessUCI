import org.json.simple.parser.ParseException;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;

import static java.util.function.UnaryOperator.identity;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.Border;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class GamePanel extends JPanel implements ActionListener, MouseListener {

    public static String startFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
    //    	public static String startFen = "5k2/6p1/8/4NPPP/4n2K/8/8/8 w - - 1 76 "; 8/7P/4B2K/8/1p3k2/8/P7/7r w - - 1 52
//public static String startFen = "8/k7/3p4/p2P1p2/P2P1P2/8/8/K7 w - - 0 8";
    public static String lastMoveFen = startFen;

    // display
    Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
    static final int screenWidth = 600;
    static final int screenHeight = 600;
    int w = dim.width / 2 - screenWidth / 2;
    int h = dim.height / 2 - screenHeight / 2;
    static final int unitSize = 75;
    static final int delay = 1000 / 120; // 120 fps
    Color white = new Color(238, 238, 210);
    Color black = new Color(118, 150, 86);

    // computer algorithm vars
    static boolean isCompDone = true;
    static boolean enginePlay = false;

    static int limitedDepth = 0;
    static int moveTime = 1;
    static int timeBound = 5;
    static int times = 1000;
    static boolean computersColor = false;
    public static boolean tableBase = true;
    public static HashMap<Integer, String> colMapping = new HashMap<>();
    public static HashMap<Character, Integer> RcolMapping = new HashMap<>();
    public static Client engine1;
    public static Client engine2;

    // game vars
    public static int[] enPassant = new int[2];
    public static int[] lastEnPassant = new int[2];
    static boolean isGameEnded = false;
    static boolean turn;
    static boolean perspective = false;
    static boolean isInCheck = false;
    static boolean tmpIsInCheck = false;
    static boolean stalemate = false;
    static int halfMoves = 0;
    static int fullMoves = 0;
    static boolean wcks;
    static boolean wcqs;
    static boolean bcks;
    static boolean bcqs;

    // main window
    boolean inMainWindow;
    private double chooseTime = 10;
    private boolean isZeroToOne = false;
    double displayTime = 5;
    boolean notAdded = true;

    // buttons

    JSlider timeRunning = new JSlider(0, 1, 1) {
        @Override
        public void updateUI() {
            setUI(new CustomSliderUI(this, new Color(200, 200 ,200)));
        }
    };
    JSlider minSlider = new JSlider(1, 65, 10) {
        @Override
        public void updateUI() {
            setUI(new CustomSliderUI(this, new Color(200, 200 ,200)));
        }
    };
    JSlider incrementSlider = new JSlider(0, 60, 0) {
        @Override
        public void updateUI() {
            setUI(new CustomSliderUI(this, new Color(200, 200 ,200)));
        }
    };

    static boolean isTimeRunning = true;
    static MyButton resignButton;

    static MyButton engineStartButton = new MyButton("Make Move");
    String enginePath = "";
    String enginePath2 = "";

    String firstEngineName;
    String secondEngineName;

    static MyButton changePersButton;
    MyButton resignYesButton;
    MyButton resignNoButton;

    // menu
    JMenuBar menuBar;
    JMenuItem loadEngine1;
    JMenuItem loadEngine2;

    JMenuItem removeEngine1;
    JMenuItem removeEngine2;
    JMenuItem showEngineLog;
    JCheckBoxMenuItem engineVsEngine;
    JMenuItem engineSwitch;

    JMenuItem fenFromClipBoard;
    JMenuItem fenToClipBoard;
    JMenuItem setPos;
    PositionFrame positionFrame;

    LevelFrame levelFrame;
    public static boolean isPositionFrameAlive = false;
    public static boolean isLevelFrameAlive = false;

    JCheckBoxMenuItem playWithEngine;
    JCheckBoxMenuItem showMoves;
    JCheckBoxMenuItem openingBook;
    JCheckBoxMenuItem endGameTableBase;

    JMenuItem playLevel;

    engineLog log;

    JMenu optionMenu;
    JMenu engineMenu;
    JMenu positionMenu;

    ChoosePieceFrame choosePiece;
    public static boolean isChoosePieceAlive = false;

    // animation
    JLabel movesBoard;
    JScrollPane scrollPane;
    public static int toScroll = 2;
    static JScrollBar vertical;
    boolean resignLabel = false;
    static boolean animation = false;
    static boolean dragging;
    static int vel = 0;
    static List<Piece> takenBlackPieces = new ArrayList<>();
    static List<Piece> takenWhitePieces = new ArrayList<>();
    static boolean drawGameOver = true;
    private static final int[] startArrow = new int[2];
    private static final List<Integer[]> redSquares = new ArrayList<>();
    public static List<Arrow> arrows = new ArrayList<>();

    // time management
    static int increment = 0;
    static int whiteTime = 0;
    static int blackTime = 0;
    static int whiteSeconds = 0;
    static int whiteMinutes = 0;
    static int blackSeconds = 0;
    static int blackMinutes = 0;
    Timer countDown;

    // operetion vars
    boolean running = false;
    public static Piece currPiece;
    public static Piece castlingRook;
    List<Integer[]> moves;
    public static Integer[] movedFrom;
    public static Integer[] movedTo;
    public static int[] clickedSpot = null;
    public static String chessMoveList;
    public static List<Long> positions = new ArrayList<>();
    static List<Piece> blackPieces = new ArrayList<>();
    static List<Piece> whitePieces = new ArrayList<>();
    public static Clip clip;
    public static long threatMap = 0L;
    Timer timer;
    Random random;
    static Spot[][] board = positionFromFen(startFen);
    static List<Object[]> playerMoveList = new ArrayList<>();
    static int moveListCounter = 0;


    GamePanel(GameFrame frame) {
        random = new Random();

        if (!frame.fullScreen) {
            dim.width = 1280;
            dim.height = 720;
            w = dim.width / 2 - screenWidth / 2;
            h = dim.height / 2 - screenHeight / 2;
        }

        this.setPreferredSize(dim);
        this.setBackground(new Color(32, 32, 32));
        this.setFocusable(true);
        this.addKeyListener(new myKeyAdapter(frame, this));
        this.setLayout(null);

        timeRunning.setBackground(new Color(32, 32, 32));
        timeRunning.setBounds(dim.width - dim.width / 4, dim.height / 4 + dim.height / 7, 50, 20);

        ChangeListener l = arg0 -> {
            boolean s = timeRunning.getValue() == 1;
            isTimeRunning = s;
            minSlider.setEnabled(s);
            incrementSlider.setEnabled(s);
            if (!s) {
                minSlider.setUI(new CustomSliderUI(minSlider, Color.gray));
                incrementSlider.setUI(new CustomSliderUI(incrementSlider, Color.gray));
            }
            else {
                minSlider.setUI(new CustomSliderUI(minSlider,new Color(200, 200 ,200)));
                incrementSlider.setUI(new CustomSliderUI(incrementSlider, new Color(200, 200 ,200)));
            }
        };

        minSlider.setBounds(dim.width - dim.width / 5, dim.height / 2 + dim.height / 19, 180, 20);
        minSlider.setBackground(new Color(32, 32, 32));
        ChangeListener l1 = arg0 -> {
            chooseTime = minSlider.getValue();
            isZeroToOne = chooseTime < 6;
            if (isZeroToOne)
                displayTime = chooseTime / 10;
            else
                displayTime = chooseTime - 5;
            increment = incrementSlider.getValue();

        };

        incrementSlider.setBounds(dim.width - dim.width / 5, dim.height / 2 + dim.height / 5 + dim.height / 30, 180, 20);
        incrementSlider.setBackground(new Color(32, 32, 32));
        incrementSlider.setMajorTickSpacing(5);


        incrementSlider.addChangeListener(l1);
        minSlider.addChangeListener(l1);
        timeRunning.addChangeListener(l);

        this.add(minSlider);
        this.add(incrementSlider);
        this.add(timeRunning);

        log = new engineLog();

        loadEngine1 = new JMenuItem("Load Engine1");
        loadEngine1.addActionListener(this);

        loadEngine2 = new JMenuItem("Load Engine2");
        loadEngine2.addActionListener(this);

        removeEngine1 = new JMenuItem("Remove Engine 1");
        removeEngine1.addActionListener(this);
        removeEngine1.setEnabled(false);

        removeEngine2 = new JMenuItem("Remove Engine 2");
        removeEngine2.addActionListener(this);
        removeEngine2.setEnabled(false);

        engineVsEngine = new JCheckBoxMenuItem("Engine Vs Engine");
        engineVsEngine.addActionListener(this);

        playWithEngine = new JCheckBoxMenuItem("Play With Engine");
        playWithEngine.addActionListener(this);

        showEngineLog = new JMenuItem("Engine Log");
        showEngineLog.addActionListener(this);

        engineSwitch = new JMenuItem("Switch Places");
        engineSwitch.addActionListener(this);

        showMoves = new JCheckBoxMenuItem("Show Moves");
        showMoves.setState(true);

        openingBook = new JCheckBoxMenuItem("Use Opening Book");
        openingBook.setState(true);

        endGameTableBase = new JCheckBoxMenuItem("Use Table Base");
        endGameTableBase.setState(false);
        if (!ChessGame.isPython)
            endGameTableBase.setEnabled(false);

        playLevel = new JMenuItem("Adjust Level");
        playLevel.addActionListener(this);

        optionMenu = new JMenu("Options");
        optionMenu.add(showMoves);
        optionMenu.add(openingBook);
        optionMenu.add(endGameTableBase);
        optionMenu.add(playLevel);
        optionMenu.add(engineSwitch);

        engineMenu = new JMenu("Engine");
        engineMenu.add(playWithEngine);
        engineMenu.add(engineVsEngine);
        engineMenu.add(loadEngine1);
        engineMenu.add(loadEngine2);
        engineMenu.add(removeEngine1);
        engineMenu.add(removeEngine2);
        engineMenu.add(showEngineLog);

        fenFromClipBoard = new JMenuItem("Get FEN From Clipboard");
        fenFromClipBoard.addActionListener(this);

        fenToClipBoard = new JMenuItem("Copy FEN To Clipboard");
        fenToClipBoard.addActionListener(this);

        setPos = new JMenuItem("Set position");
        setPos.addActionListener(this);

        positionMenu = new JMenu("Position");
        positionMenu.add(setPos);
        positionMenu.add(fenFromClipBoard);
        positionMenu.add(fenToClipBoard);

        menuBar = new JMenuBar();
        menuBar.setBounds(0, 0, dim.width, 20);
        menuBar.add(optionMenu);
        menuBar.add(engineMenu);
        menuBar.add(positionMenu);

        this.add(menuBar);
        String path = readLineFromFile("activeEngine.txt");
        if (!path.equals("")) {
            if (isFileExists(path)) {
                enginePath = path;
                firstEngineName = enginePath.split("\\\\")[enginePath.split("\\\\").length - 1];
                loadEngine(1);
            }
        }
        String path2 = readLineFromFile("activeEngine2.txt");
        if (!path2.equals("")) {
            if (isFileExists(path2)) {
                enginePath2 = path2;
                secondEngineName = enginePath2.split("\\\\")[enginePath2.split("\\\\").length - 1];
                loadEngine(2);
            }
        }

        addMouseListener(this);
        startGame();
    }

    public void startGame() {
        chessMoveList = "";
        movedFrom = null;
        movedTo = null;

        colMapping.put(0, "a");
        colMapping.put(1, "b");
        colMapping.put(2, "c");
        colMapping.put(3, "d");
        colMapping.put(4, "e");
        colMapping.put(5, "f");
        colMapping.put(6, "g");
        colMapping.put(7, "h");

        RcolMapping.put('a', 0);
        RcolMapping.put('b', 1);
        RcolMapping.put('c', 2);
        RcolMapping.put('d', 3);
        RcolMapping.put('e', 4);
        RcolMapping.put('f', 5);
        RcolMapping.put('g', 6);
        RcolMapping.put('h', 7);

        ChessGame.updateThreats(turn);
        ChessGame.updateThreats(!turn);
        isInCheck = ChessGame.isInCheck(turn);
        inMainWindow = true;
        ActionListener l = arg0 -> {
            if (isTimeRunning) {
                if (!turn)
                    blackTime--;
                else
                    whiteTime--;
            }
            whiteSeconds = whiteTime % 60;
            whiteMinutes = (whiteTime - whiteSeconds) / 60;
            blackSeconds = blackTime % 60;
            blackMinutes = (blackTime - blackSeconds) / 60;
        };
        countDown = new Timer(1000, l);
        timer = new Timer(delay, this);
        timer.start();
        Zobrist.fillArray();
        ChessGame.updateBoards(turn);
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        draw(g);
    }

    public void draw(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics;
        RenderingHints rh = new RenderingHints(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHints(rh);
        if (inMainWindow) {
            drawBoard(g);
            drawPositions(g);
            drawPieces(g);
            drawMainWindow(g);
            drawEnginesNames(g, perspective);

        } else if (running) {
            handCursor();
            drawBoard(g);

            if (movedFrom != null && movedTo != null) {
                g.setColor(new Color(255, 254, 123, 180));
                if (!perspective) {
                    g.fillRect(movedFrom[1] * unitSize + w, movedFrom[0] * unitSize + h, unitSize, unitSize);
                    g.fillRect(movedTo[1] * unitSize + w, movedTo[0] * unitSize + h, unitSize, unitSize);
                } else {
                    g.fillRect((7 - movedFrom[1]) * unitSize + w, (7 - movedFrom[0]) * unitSize + h, unitSize, unitSize);
                    g.fillRect((7 - movedTo[1]) * unitSize + w, (7 - movedTo[0]) * unitSize + h, unitSize, unitSize);
                }
            }
            if (whiteMinutes < 1 && increment < 10) {
                if (whiteSeconds < 20 && increment == 0)
                    if (times > 500) times = 500;
                if (whiteSeconds < 10 && increment == 0)
                    if (times > 250) times = 250;
                if (whiteSeconds < 5 && increment == 0)
                    if (times > 100) times = 100;
            } else
                timeBound = 10;
            if (blackMinutes < 1 && increment < 10) {
                if (blackSeconds < 20 && increment == 0)
                    if (times > 500) times = 500;
                if (blackSeconds < 10 && increment == 0)
                    if (times > 250) times = 250;
                if (blackSeconds < 5 && increment == 0)
                    if (times > 100) times = 100;
            } else
                timeBound = 10;

            if (moves != null) {
                drawMoves(g, showMoves.isSelected());
            }

            drawPositions(g);

            drawMovesBoard();
            g.setColor(new Color(188, 20, 20, 200));
            for (Integer[] square : redSquares)
                g.fillRect(w + square[0] * unitSize, h + square[1] * unitSize, unitSize, unitSize);

            drawPieces(g);

            drawEnginesNames(g, perspective);

            for (Arrow arrow : arrows)
                arrow.drawArrow(g, 15);

            if (animation) {
                animate(g);
            }

            if (dragging) {
                drawDraggedPiece(g);
            }
            if (resignLabel) {
                if (isGameEnded)
                    drawResignLabel(g, "Exit");
                else
                    drawResignLabel(g, "Resign");
            }

            if (!isGameEnded) {
                drawTimers(g);
                drawTakenPieces(g);
            }

            if (isGameEnded && !animation) {
                if (isInCheck) {
                    gameOver(g, this);
                } else {
                    tie(g, this);
                }
            }
            if (isTimeOver(turn)) {
                if (!isGameEnded) {
                    if (currPiece == null)
                        currPiece = whitePieces.get(0);
                    clip = currPiece.sound("chessStalemateSound.wav");
                    resignButton.setText("Exit");
                    if (clip != null) {
                        clip.setFramePosition(0);
                        clip.start();
                    }
                }
                isGameEnded = true;
                isInCheck = true;
                moves = null;
                gameOver(g, this);
            }
            if (halfMoves >= 50 && !animation) {
                tie(g, this);
            }
        }
    }

    public void drawBoard(Graphics g) {
        for (int i = 0; i < screenHeight / unitSize; i++) {
            for (int j = 0; j < screenHeight / unitSize; j++) {
                if ((i + j) % 2 == 0)
                    g.setColor(white);
                else
                    g.setColor(black);
                g.fillRect(i * unitSize + w, j * unitSize + h, unitSize, unitSize);
            }
        }
    }

    private void drawPieces(Graphics g) {
        drawPiecesInList(g, whitePieces);
        drawPiecesInList(g, blackPieces);
    }
    private void drawPiecesInList(Graphics g, List<Piece> pieces) {
        for (Piece p : pieces) {
            if (p.equals(currPiece) && (dragging || animation)) {
                continue;
            }
            if (p.equals(castlingRook) && animation) {
                int row = castlingRook.lastPos[0];
                int col = castlingRook.lastPos[1];
                if (perspective) {
                    row = 7-row;
                    col = 7-col;
                }
                g.drawImage(p.getImg(), col * unitSize + 2 + w, row * unitSize + 2 + h, null);
            } else {
                int row = p.getRow();
                int col = p.getCol();
                if (perspective) {
                    row = 7-row;
                    col = 7-col;
                }
                g.drawImage(p.getImg(), col * unitSize + 2 + w, row * unitSize + 2 + h, null);
            }
        }
    }

    private void handCursor() {
        PointerInfo a = MouseInfo.getPointerInfo();
        Point b = a.getLocation();
        int globalX = (int) b.getX() - this.getLocationOnScreen().x;
        int globalY = (int) b.getY() - this.getLocationOnScreen().y;
        int y = (globalX - w) / unitSize;
        int x = (globalY - h) / unitSize;
        if (perspective) {
            y = 7 - (globalX - w) / unitSize;
            x = 7 - (globalY - h) / unitSize;
        }

        if (globalX < w || globalX > screenWidth + w - 1 || globalY < h || globalY > screenHeight + h - 1) {
            this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            return;
        }
        List<Piece> pieces = (!turn) ? blackPieces : whitePieces;
        boolean hand = false;
        for (Piece p : pieces) {
            if (p.getRow() == x && p.getCol() == y) {
                hand = true;
                break;
            }
        }
        if (dragging || hand && !resignLabel && !isGameEnded) {
            this.setCursor(new Cursor(Cursor.HAND_CURSOR));
        } else
            this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
    }

    public void drawPositions(Graphics2D g) {
        g.setFont(new Font("Times New Roman", Font.PLAIN, 12));
        if (perspective) {
            String[] chars = {"h", "g", "f", "e", "d", "c", "b", "a"};
            for (int i = 0; i < 8; i++) {
                if (i % 2 == 0)
                    g.setColor(black);
                else
                    g.setColor(white);
                g.drawString("" + (i + 1), 5 + w, i * unitSize + 15 + h);
                if (i % 2 == 0)
                    g.setColor(white);
                else
                    g.setColor(black);
                g.drawString(chars[i], i * unitSize + 60 + w, 595 + h);

            }
        } else {
            String[] chars = {"a", "b", "c", "d", "e", "f", "g", "h"};
            for (int i = 0; i < 8; i++) {
                if (i % 2 == 0)
                    g.setColor(black);
                else
                    g.setColor(white);
                g.drawString("" + (8 - i), 5 + w, i * unitSize + 15 + h);
                if (i % 2 == 0)
                    g.setColor(white);
                else
                    g.setColor(black);
                g.drawString(chars[i], i * unitSize + 60 + w, 595 + h);

            }
        }
    }

    private void drawMoves(Graphics2D g, boolean draw) {
        if (!perspective) {
            g.setColor(new Color(255, 254, 123, 180));
            g.fillRect(currPiece.getCol() * unitSize + w, currPiece.getRow() * unitSize + h, unitSize, unitSize);
            if (draw) {
                for (Integer[] move : moves) {
                    g.setStroke(new BasicStroke(2));
                    g.setColor(new Color(50, 50, 50, 100));
                    if (!board[move[0]][move[1]].isFull())
                        g.fillOval(move[1] * unitSize + 25 + w, move[0] * unitSize + 25 + h, unitSize - 50, unitSize - 50);
                    else if (move[0] == enPassant[0] && move[1] == enPassant[1]) {
                        g.setColor(new Color(50, 50, 50, 100));
                        g.setStroke(new BasicStroke(4));
                        g.drawOval(move[1] * unitSize + 3 + w, move[0] * unitSize + 3 + h, unitSize - 6, unitSize - 6);
                    } else {
                        g.setColor(new Color(50, 50, 50, 100));
                        g.setStroke(new BasicStroke(4));
                        g.drawOval(move[1] * unitSize + 3 + w, move[0] * unitSize + 3 + h, unitSize - 6, unitSize - 6);
                    }
                }
            }
        } else {
            g.setColor(new Color(255, 254, 123, 180));
            g.fillRect((7 - currPiece.getCol()) * unitSize + w, (7 - currPiece.getRow()) * unitSize + h, unitSize, unitSize);
            if (draw) {
                for (Integer[] move : moves) {
                    g.setStroke(new BasicStroke(2));
                    g.setColor(new Color(50, 50, 50, 100));
                    if (!board[move[0]][move[1]].isFull())
                        g.fillOval((7 - move[1]) * unitSize + 25 + w, (7 - move[0]) * unitSize + 25 + h, unitSize - 50, unitSize - 50);
                    else if ((7 - move[0]) == enPassant[0] && (7 - move[1]) == enPassant[1]) {
                        g.setColor(new Color(50, 50, 50, 100));
                        g.setStroke(new BasicStroke(4));
                        g.drawOval((7 - move[1]) * unitSize + 3 + w, (7 - move[0]) * unitSize + 3 + h, unitSize - 6, unitSize - 6);
                    } else {
                        g.setColor(new Color(50, 50, 50, 100));
                        g.setStroke(new BasicStroke(4));
                        g.drawOval((7 - move[1]) * unitSize + 3 + w, (7 - move[0]) * unitSize + 3 + h, unitSize - 6, unitSize - 6);
                    }
                }
            }
        }
    }

    private void drawDraggedPiece(Graphics2D g) {
        if (isGameEnded) {
            dragging = false;
            return;
        }
        PointerInfo a = MouseInfo.getPointerInfo();
        Point b = a.getLocation();
        int globalX = (int) b.getX() - this.getLocationOnScreen().x;
        int globalY = (int) b.getY() - this.getLocationOnScreen().y;
        if (globalX < w + 20)
            globalX = w + 20;

        if (globalX > screenWidth - 20 + w)
            globalX = screenWidth - 20 + w;

        if (globalY < 20 + h)
            globalY = 20 + h;

        if (globalY > screenHeight - 25 + h)
            globalY = screenHeight - 25 + h;

        int x = (globalX - w) / unitSize;
        int y = (globalY - h) / unitSize;

        g.setColor(new Color(200, 200, 200, 200));
        g.setStroke(new BasicStroke(3));
        g.drawRect(w + x * unitSize, h + y * unitSize, unitSize, unitSize);
        g.drawImage(currPiece.getImg(), globalX - 30, globalY - 30, null);
    }

    private void drawTimers(Graphics2D g) {
        g.setFont(new Font("Arial Black", Font.PLAIN, 40));
        int w = dim.width / 2 + screenWidth / 2;
        FontMetrics fm = g.getFontMetrics();
        Rectangle2D rect = fm.getStringBounds(String.format("%02d:%02d", blackMinutes, blackSeconds), g);
        int x = w + w / 15;
        int y = h / 2 + dim.height / 3;
        if (perspective)
            y = h / 2 + (dim.height / 3) * 2;

        g.setColor(Color.black);
        g.fillRect(x,
                y - fm.getAscent(),
                (int) rect.getWidth() + 20,
                (int) rect.getHeight());

        if (!turn && isTimeRunning) {
            g.setColor(Color.white);
            if (blackMinutes == 0 && blackSeconds < 30)
                g.setColor(Color.RED);
        } else
            g.setColor(Color.gray);

        g.drawString(String.format("%02d:%02d", blackMinutes, blackSeconds), x + 10, y);
        y = h / 2 + (dim.height / 3) * 2;
        if (perspective)
            y = h / 2 + dim.height / 3;
        g.setColor(Color.black);
        g.fillRect(x,
                y - fm.getAscent(),
                (int) rect.getWidth() + 20,
                (int) rect.getHeight());

        if (turn && isTimeRunning) {
            g.setColor(Color.white);
            if (whiteMinutes == 0 && whiteSeconds < 30)
                g.setColor(Color.RED);
        } else
            g.setColor(Color.gray);
        g.drawString(String.format("%02d:%02d", whiteMinutes, whiteSeconds), x + 10, y);
    }

    private void drawEnginesNames(Graphics2D g, boolean perspective) {
        g.setColor(Color.white.darker());
        g.setFont(new Font("Arial Black", Font.PLAIN, 20));
        if (!perspective) {
            g.drawString("engine 1: " + firstEngineName, w, h + screenHeight + h / 2);
            g.drawString("engine 2: " + secondEngineName, w, h - h / 4);
        } else {
            g.drawString("engine 2: " + secondEngineName, w, h + screenHeight + h / 2);
            g.drawString("engine 1: " + firstEngineName, w, h - h / 4);
        }
    }

    public void drawResignLabel(Graphics2D g, String text) {
        g.setColor(new Color(125, 125, 125, 60));
        g.fillRect(w, h, screenWidth, screenHeight);
        g.setColor(Color.white);
        g.fillRoundRect(150 + w, 175 + h, 300, 250, 30, 35);
        g.setColor(new Color(40, 40, 40));
        g.setFont(new Font("Times New Roman", Font.BOLD, 25));
        FontMetrics metrics = getFontMetrics(g.getFont());
        g.drawString("Do You Want To " + text, (screenWidth - metrics.stringWidth("Do You Want To " + text)) / 2 + w, g.getFont().getSize() + 250);
    }

    private void drawMainWindow(Graphics2D g) {
        int height = (int) dim.getHeight();
        int width = (int) dim.getWidth();
        // choose piece color
        g.setColor(Color.white.darker());
        g.setFont(new Font("Arial Black", Font.PLAIN, 40));
        g.drawString("Play As", width / 15, height / 6);
        g.drawString("Board Color", width / 29, height / 2);
        Image whiteKing = null;
        Image blackKing = null;
        try {
            whiteKing = ImageIO.read(Objects.requireNonNull(this.getClass().getResource("images/white_king.png")));
            whiteKing = whiteKing.getScaledInstance(40, 40, Image.SCALE_SMOOTH);
            blackKing = ImageIO.read(Objects.requireNonNull(this.getClass().getResource("images/black_king.png")));
            blackKing = blackKing.getScaledInstance(40, 40, Image.SCALE_SMOOTH);
        } catch (IOException e) {
            e.printStackTrace();
        }
        g.setColor(new Color(45, 45, 45));
        g.fillRect(w - (w / 5) * 2, height / 5, 50, 50);
        g.fillRect(w - (w / 5) * 4, height / 5, 50, 50);
        g.drawImage(whiteKing, w - (w / 5) * 2 + 5, height / 5 + 3, 40, 40, null);
        g.drawImage(blackKing, w - (w / 5) * 4 + 5, height / 5 + 3, 40, 40, null);

        g.setStroke(new BasicStroke(3));
        g.setColor(new Color(10, 160, 10));
        if (computersColor) {
            g.drawRect(w - (w / 5) * 4 - 3, height / 5 - 3, 55, 55);
        } else {
            g.drawRect(w - (w / 5) * 2 - 3, height / 5 - 3, 55, 55);
        }

        // choose board color
        if (white.equals(new Color(238, 238, 210))) {
            g.drawRect(w - (w / 10) * 9 - 3, height / 2 + height / 19 - 3, 105, 105);
        } else if (white.equals(new Color(240, 217, 181))) {
            g.drawRect(w - (w / 10) * 4 - 3, height / 2 + height / 19 - 3, 105, 105);
        } else if (white.equals(new Color(222, 227, 230))) {
            g.drawRect(w - (w / 10) * 9 - 3, height / 2 + height / 4 - 3, 105, 105);
        } else if (white.equals(new Color(220, 220, 220))) {
            g.drawRect(w - (w / 10) * 4 - 3, height / 2 + height / 4 - 3, 105, 105);
        }
        // green
        g.setColor(new Color(238, 238, 210));
        g.fillRect(w - (w / 10) * 9, height / 2 + height / 19, 50, 50);
        g.fillRect(w - (w / 10) * 9 + 50, height / 2 + height / 19 + 50, 50, 50);
        g.setColor(new Color(118, 150, 86));
        g.fillRect(w - (w / 10) * 9 + 50, height / 2 + height / 19, 50, 50);
        g.fillRect(w - (w / 10) * 9, height / 2 + height / 19 + 50, 50, 50);
        // brown
        g.setColor(new Color(240, 217, 181));
        g.fillRect(w - (w / 10) * 4, height / 2 + height / 19, 50, 50);
        g.fillRect(w - (w / 10) * 4 + 50, height / 2 + height / 19 + 50, 50, 50);
        g.setColor(new Color(181, 136, 99));
        g.fillRect(w - (w / 10) * 4 + 50, height / 2 + height / 19, 50, 50);
        g.fillRect(w - (w / 10) * 4, height / 2 + height / 19 + 50, 50, 50);
        // blue
        g.setColor(new Color(222, 227, 230));
        g.fillRect(w - (w / 10) * 9, height / 2 + height / 4, 50, 50);
        g.fillRect(w - (w / 10) * 9 + 50, height / 2 + height / 4 + 50, 50, 50);
        g.setColor(new Color(140, 162, 173));
        g.fillRect(w - (w / 10) * 9 + 50, height / 2 + height / 4, 50, 50);
        g.fillRect(w - (w / 10) * 9, height / 2 + height / 4 + 50, 50, 50);
        // grey
        g.setColor(new Color(220, 220, 220));
        g.fillRect(w - (w / 10) * 4, height / 2 + height / 4, 50, 50);
        g.fillRect(w - (w / 10) * 4 + 50, height / 2 + height / 4 + 50, 50, 50);
        g.setColor(new Color(171, 171, 171));
        g.fillRect(w - (w / 10) * 4 + 50, height / 2 + height / 4, 50, 50);
        g.fillRect(w - (w / 10) * 4, height / 2 + height / 4 + 50, 50, 50);

        // choose time control

        g.setColor(Color.white.darker());
        if (!isTimeRunning) {
            g.setColor(Color.gray);
        }
        g.drawString("Time Control", width / 2 + width / 4, height / 2);
        String TR = String.valueOf(timeRunning.getValue());
//        dim.width - dim.width / 4, dim.height / 2 + dim.height / 19, 50, 20
        g.setFont(new Font("Arial Black", Font.PLAIN, 20));

        g.drawString(String.format("%.1f : minutes", displayTime), width - width / 5 + width / 50, height / 2 + height / 9);
//        g.drawString(TR, width - width / 4 + width / 50, height / 2 + height / 9);

        g.setFont(new Font("Arial Black", 0, 40));
        g.drawString("Increment", width - width / 4 + width / 40, height / 2 + height / 5);

        g.setFont(new Font("Arial Black", Font.PLAIN, 20));
        g.drawString(String.format("%02d : seconds", increment), width - width / 5 + width / 50, height / 2 + height / 4 + height / 20);
        JLabel play = new JLabel("Play", JLabel.CENTER);
        play.setForeground(Color.white.darker());
        play.setVerticalAlignment(JLabel.BOTTOM);
        play.setBounds(width - w / 2 - w / 4, height / 7, 165, 80);
        play.setBackground(new Color(45, 45, 45));
        play.setOpaque(true);
        play.setBorder(BorderFactory.createLineBorder(Color.white.darker(), 3));
        play.setFont(new Font("Arial Black", Font.PLAIN, 60));
        if (notAdded) {
            this.add(play);
            notAdded = false;
        }
    }

    public void drawMovesBoard() {
        String[] arr = chessMoveList.split(" ");
        if (chessMoveList == "")
            arr = new String[0];
        String drawMoveList = "<html>";
        for (int i = 0; i < arr.length; i++) {
            if ((i + 1) % 2 == 1) {
                String space = "&nbsp;";
                String num = (i / 2 + 1) + "";
                String s = new String(new char[3 - num.length()]).replace("\0", "*");
                String word = num + ":" + s + arr[i];
                space = new String(new char[12 - word.length()]).replace("\0", space);
                word = word.replace("*", "&nbsp;");
                drawMoveList += "&nbsp;" + word + space;
            } else
                drawMoveList += arr[i] + "<br/>";
        }
        movesBoard.setText(drawMoveList + "</html>");
        if (toScroll > 0) {
            vertical.setValue(vertical.getMaximum());
            toScroll -= 1;
        }
    }

    private void drawTakenPieces(Graphics2D g) {
        int w = dim.width / 2 + screenWidth / 2;
        int x = w + w / 35;
        int y = dim.height / 2 + h + h / 5;
        if (perspective)
            y = dim.height / 2 - h - h / 5;
        int whitePoints = 0;
        Type lastType = Type.PAWN;
        for (Piece p : takenBlackPieces) {
            if (p == takenBlackPieces.get(0))
                lastType = p.type;
            if (p.type != lastType)
                x += 12;
            g.drawImage(p.getSmallImg(), x, y, null);
            x += 12;
            lastType = p.type;
            whitePoints += p.evalPiece(true, false) / 100;
        }
        int sumOfBlackX = x;
        x = w + w / 35;
        y = dim.height / 2 - h - h / 5;
        if (perspective)
            y = dim.height / 2 + h + h / 5;
        int blackPoints = 0;
        for (Piece p : takenWhitePieces) {
            if (p == takenWhitePieces.get(0))
                lastType = p.type;
            if (p.type != lastType)
                x += 12;
            g.drawImage(p.getSmallImg(), x, y, null);
            x += 12;
            lastType = p.type;
            blackPoints += p.evalPiece(true, false) / 100;
        }
        int sumOfWhiteX = x;
        g.setFont(new Font("Arial Black", Font.PLAIN, 18));
        g.setColor(Color.white);
        int points = whitePoints - blackPoints;
        if (points > 0) {
            x = sumOfBlackX + 20;
            y = dim.height / 2 + h + h / 5 + 14 + 8;
            if (perspective)
                y = dim.height / 2 - h - h / 5 + 14 + 8;
            g.drawString("+" + Math.abs(points), x, y);
        } else if (points < 0) {
            x = sumOfWhiteX + 20;
            y = dim.height / 2 - h - h / 5 + 14 + 8;
            if (perspective)
                y = dim.height / 2 + h + h / 5 + 14 + 8;
            g.drawString("+ " + Math.abs(points), x, y);
        }
    }

    public void gameOver(Graphics2D g, GamePanel panel) {
        if (!drawGameOver) return;
        g.setColor(new Color(125, 125, 125, 60));
        g.fillRect(w, h, screenWidth, screenHeight);
        g.setColor(Color.white);
        g.fillRoundRect(150 + w, 175 + h, 300, 250, 30, 35);
        g.setColor(new Color(40, 40, 40));
        g.setFont(new Font("Times New Roman", Font.BOLD, 30));
        FontMetrics metrics = getFontMetrics(g.getFont());
        g.drawString("Game Over", (screenWidth - metrics.stringWidth("Game Over")) / 2 + w, g.getFont().getSize() + 250);
        String won = (!turn) ? "White" : "Black";
        List<Component> componentList = Arrays.asList(panel.getComponents());
        if (!componentList.contains(resignYesButton)) {
            if (won.equals("White")) {
                chessMoveList += "1-0";
            } else {
                chessMoveList += "0-1";
            }
            createYesNoButton("NO", false, w + 170, h + 375, panel, true);
            createYesNoButton("YES", true, w + 390, h + 375, panel, true);
            resignButton.setEnabled(false);
            changePersButton.setEnabled(false);
        }
        g.drawString(won + " won", (screenWidth - metrics.stringWidth(won + " won")) / 2 + w, g.getFont().getSize() + 300);


    }

    public void tie(Graphics2D g, GamePanel panel) {
        if (!drawGameOver) return;
        if (!chessMoveList.contains("1/2-1/2")) {
            chessMoveList += "1/2-1/2";
            createYesNoButton("NO", false, w + 170, h + 375, panel, true);
            createYesNoButton("YES", true, w + 390, h + 375, panel, true);
            resignButton.setEnabled(false);
            changePersButton.setEnabled(false);
        }
        g.setColor(new Color(125, 125, 125, 60));
        g.fillRect(w, h, screenWidth, screenHeight);
        g.setColor(Color.white);
        g.fillRoundRect(150 + w, 175 + h, 300, 250, 30, 35);
        g.setColor(new Color(40, 40, 40));
        g.setFont(new Font("Times New Roman", Font.BOLD, 30));
        FontMetrics metrics = getFontMetrics(g.getFont());
        g.drawString("Game Over", (screenWidth - metrics.stringWidth("Game Over")) / 2 + w, g.getFont().getSize() + 250);
        g.drawString("It's a tie!", (screenWidth - metrics.stringWidth("It's a tie!")) / 2 + w, g.getFont().getSize() + 300);

    }

    public static boolean isTimeOver(boolean color) {
        if (color) {
            return whiteMinutes <= 0 && whiteSeconds <= 0;
        } else {
            return blackMinutes <= 0 && blackSeconds <= 0;
        }
    }

    private void backGroundAnimate() {
        if (currPiece.type == Type.KING) {
            if (currPiece.getColor()) {
                if (currPiece.lastPos[1] - currPiece.getCol() == 2) {
                    castlingRook = board[7][3].getPiece();
                } else if (currPiece.lastPos[1] - currPiece.getCol() == -2) {
                    castlingRook = board[7][5].getPiece();
                } else
                    castlingRook = null;
            } else {
                if (currPiece.lastPos[1] - currPiece.getCol() == 2) {
                    castlingRook = board[0][3].getPiece();
                } else if (currPiece.lastPos[1] - currPiece.getCol() == -2) {
                    castlingRook = board[0][5].getPiece();
                } else
                    castlingRook = null;
            }
        } else
            castlingRook = null;
        int frameCount = 10;
        if (vel < frameCount)
            vel++;
        else {
            vel = 0;
            animation = false;
            if (currPiece.type == Type.KING) {
                if (currPiece.getColor()) {
                    if (currPiece.lastPos[1] - currPiece.getCol() == 2) {
                        currPiece = board[7][3].getPiece();
                        movedFrom = new Integer[]{7, 0};
                        movedTo = new Integer[]{7, 3};
                        animation = true;
                    }
                    if (currPiece.lastPos[1] - currPiece.getCol() == -2) {
                        currPiece = board[7][5].getPiece();
                        movedFrom = new Integer[]{7, 7};
                        movedTo = new Integer[]{7, 5};
                        animation = true;
                    }
                } else {
                    if (currPiece.lastPos[1] - currPiece.getCol() == 2) {
                        currPiece = board[0][3].getPiece();
                        movedFrom = new Integer[]{0, 0};
                        movedTo = new Integer[]{0, 3};
                        animation = true;
                    }
                    if (currPiece.lastPos[1] - currPiece.getCol() == -2) {
                        currPiece = board[0][5].getPiece();
                        movedFrom = new Integer[]{0, 7};
                        movedTo = new Integer[]{0, 5};
                        animation = true;
                    }
                }
            }
        }
    }

    public void draggingCastleAnimation() {
        if (currPiece.type == Type.KING) {
            if (currPiece.getColor()) {
                if (currPiece.lastPos[1] - currPiece.getCol() == 2) {
                    currPiece = board[7][3].getPiece();
                    movedFrom = new Integer[]{7, 0};
                    movedTo = new Integer[]{7, 3};
                    animation = true;
                }
                if (currPiece.lastPos[1] - currPiece.getCol() == -2) {
                    currPiece = board[7][5].getPiece();
                    movedFrom = new Integer[]{7, 7};
                    movedTo = new Integer[]{7, 5};
                    animation = true;
                }
            } else {
                if (currPiece.lastPos[1] - currPiece.getCol() == 2) {
                    currPiece = board[0][3].getPiece();
                    movedFrom = new Integer[]{0, 0};
                    movedTo = new Integer[]{0, 3};
                    animation = true;
                }
                if (currPiece.lastPos[1] - currPiece.getCol() == -2) {
                    currPiece = board[0][5].getPiece();
                    movedFrom = new Integer[]{0, 7};
                    movedTo = new Integer[]{0, 5};
                    animation = true;
                }
            }
        }
    }

    public void animate(Graphics2D g) {
        int dr;
        int dc;
        int anRow;
        int anCol;
        int frameCount = 10;
        if (perspective) {
            dr = (7 - movedTo[0]) * unitSize - (7 - movedFrom[0]) * unitSize;
            dc = (7 - movedTo[1]) * unitSize - (7 - movedFrom[1]) * unitSize;
            anCol = (7 - movedFrom[1]) * unitSize + (dc * vel / frameCount);
            anRow = (7 - movedFrom[0]) * unitSize + (dr * vel / frameCount);
        } else {
            dr = movedTo[0] * unitSize - movedFrom[0] * unitSize;
            dc = movedTo[1] * unitSize - movedFrom[1] * unitSize;
            anCol = movedFrom[1] * unitSize + (dc * vel / frameCount);
            anRow = movedFrom[0] * unitSize + (dr * vel / frameCount);
        }
        g.drawImage(currPiece.getImg(), anCol + 2 + w, anRow + 2 + h, null);
    }


    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == loadEngine1) {
            JFileChooser fileChooser = new JFileChooser();
            FileNameExtensionFilter filter = new FileNameExtensionFilter("EXE File", "exe");
            fileChooser.setFileFilter(filter);
            File workingDirectory = new File(System.getProperty("user.dir"));
            fileChooser.setCurrentDirectory(workingDirectory);
            int i = fileChooser.showOpenDialog(this);

            if (i == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                enginePath = file.getPath();
                firstEngineName = enginePath.split("\\\\")[enginePath.split("\\\\").length - 1];
                if (engine1 != null)
                    engine1.close();
                loadEngine(1);
            }
        }
        if (e.getSource() == loadEngine2) {
            JFileChooser fileChooser = new JFileChooser();
            FileNameExtensionFilter filter = new FileNameExtensionFilter("EXE File", "exe");
            fileChooser.setFileFilter(filter);
            int i = fileChooser.showOpenDialog(this);

            if (i == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                enginePath2 = file.getPath();
                secondEngineName = enginePath2.split("\\\\")[enginePath2.split("\\\\").length - 1];
                if (engine2 != null)
                    engine2.close();
                loadEngine(2);
            }
        }
        if (e.getSource() == removeEngine1) {
            engine1.close();
            engine1 = null;
            clearFile("activeEngine.txt");
            removeEngine1.setEnabled(false);
            enginePath = "";
            firstEngineName = "";
            if (engine2 == null)
                engineStartButton.setEnabled(false);
        }
        if (e.getSource() == removeEngine2) {
            engine2.close();
            engine2 = null;
            clearFile("activeEngine2.txt");
            removeEngine2.setEnabled(false);
            secondEngineName = "";
            enginePath2 = "";
            if (engine2 == null)
                engineStartButton.setEnabled(false);
        }
        if (engine1 == null || engine2 == null) {
            engineVsEngine.setEnabled(false);
            engineVsEngine.setState(false);
        } else
            engineVsEngine.setEnabled(true);
        if (e.getSource() == playWithEngine) engineVsEngine.setState(false);
        if (e.getSource() == engineVsEngine) playWithEngine.setState(false);
        if (engineVsEngine.isSelected()) engineStartButton.setEnabled(false);

        if (e.getSource() == showEngineLog) {
            log.setVisible(true);
        }

        if (e.getSource() == engineSwitch) {
            switchEngines();
        }

        if (e.getSource() == fenFromClipBoard) {
            try {
                String data = (String) Toolkit.getDefaultToolkit()
                        .getSystemClipboard().getData(DataFlavor.stringFlavor);
                if (isValidFen(data)) {
                    board = positionFromFen(data);
                    Piece.castlingRights();
                    ChessGame.updateBoards(turn);
                    ChessGame.updateThreats(!turn);
                    lastMoveFen = data;
                }
            } catch (UnsupportedFlavorException | IOException ex) {
                throw new RuntimeException(ex);
            }
        }
        if (e.getSource() == fenToClipBoard) {
            turn = !turn;
            StringSelection stringSelection = new StringSelection(CurrentFen());
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(stringSelection, null);
            turn = !turn;
        }
        if (e.getSource() == setPos) {
            try {
                positionFrame = new PositionFrame();
                board = positionFromFen("8/8/8/8/8/8/8/8 w - - 0 1");
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
        if (positionFrame != null) {
            if (!isPositionFrameAlive) {
                if (positionFrame.isConfirmed) {
                    turn = !positionFrame.toMove;
                    wcks = positionFrame.whiteCastleKS;
                    wcqs = positionFrame.whiteCastleQS;
                    bcks = positionFrame.blackCastleKS;
                    bcqs = positionFrame.blackCastleKS;
                    halfMoves = 0;
                    fullMoves = 10;
                    String f = CurrentFen();
                    if (isValidFen(f)) {
                        turn = !turn;
                        board = positionFromFen(f);
                        Piece.castlingRights();
                        ChessGame.updateBoards(turn);
                        ChessGame.updateThreats(!turn);
                        lastMoveFen = f;
                        if (!positionFrame.enPassant.getText().equals("")) {
                            setEnPassantSquare(positionFrame.enPassant.getText());
                        }
                    } else {
                        board = positionFromFen(startFen);
                        turn = true;
                        ChessGame.updateBoards(turn);
                        ChessGame.updateThreats(!turn);
                    }
                } else {
                    board = positionFromFen(startFen);
                    turn = true;
                    Piece.castlingRights();
                    ChessGame.updateBoards(turn);
                    ChessGame.updateThreats(!turn);
                    lastMoveFen = startFen;
                }
                positionFrame = null;
            }
        }
        if (e.getSource() == playLevel) {
            levelFrame = new LevelFrame();
        }
        if (levelFrame != null) {
            if (!isLevelFrameAlive) {
                if (levelFrame.isConfirmed) {
                    if (levelFrame.depth.isSelected()) {
                        limitedDepth = (int) levelFrame.spinner.getValue();
                    } else if (levelFrame.time.isSelected()) {
                        if (levelFrame.isInt()) {
                            moveTime = (int) levelFrame.spinner.getValue();
                            times = 1000;
                        }
                        else {
                            moveTime = 1;
                            times =  (int)((double) levelFrame.spinnerS.getValue()*1000);
                        }
                        limitedDepth = 0;
                    }
                }
                levelFrame = null;
            }
        }
        if (running) {
            if (stalemate) {
                if (!isGameEnded) {
                    if (!chessMoveList.contains("#")) {
                        chessMoveList = chessMoveList.substring(0, chessMoveList.length() - 2) + "# ";
                    }
                }
                isGameEnded = true;
                resignButton.setText("Exit");
            }
            if (halfMoves == 50) {
                if (!isGameEnded) {
                    if (!chessMoveList.contains("#")) {
                        chessMoveList = chessMoveList.substring(0, chessMoveList.length() - 2) + "# ";
                        vertical.setValue(vertical.getMaximum());
                    }
                }
                isGameEnded = true;
                resignButton.setText("Exit");
            }
        }

        if (threefoldRepetition()) {
            isGameEnded = true;
            isInCheck = false;
        }

        if (animation) {
            backGroundAnimate();
        }
//        else if (currPiece != null) {
//            draggingCastleAnimation();
//        }

        if (engineVsEngine.isSelected()) enginePlay = true;
        if (isCompDone && !animation && enginePlay && !isChoosePieceAlive) {
            computerMove();
        }
        repaint();
    }

    private void setEnPassantSquare(String text) {
        String[] enPossibleSquares = {"a3", "b3", "c3", "d3", "e3", "f3", "g3", "h3", "a6", "b6", "c6", "d6", "e6", "f6", "g6", "h6"};
        for (int i = 0; i < enPossibleSquares.length; i++) {
            if (enPossibleSquares[i].equals(text)) {
                // white just moved
                if (i < 8) {
                    if (board[4][i].getPiece() != null && board[4][i].getPiece().getColor() && board[4][i].getPiece().type == Type.PAWN) {
                        enPassant = new int[]{5, i};
                        break;
                    }
                }
                // black just moved
                else {
                    if (board[3][i % 8].getPiece() != null && !board[3][i % 8].getPiece().getColor() && board[3][i % 8].getPiece().type == Type.PAWN) {
                        enPassant = new int[]{2, i % 8};
                        break;
                    }
                }
            }
        }
    }

    private void computerMove() {
        enginePlay = false;
        if (!running) return;

        if (moveListCounter < playerMoveList.size())
            shortenLists();
        List<String> moves = new ArrayList<>();
        if (openingBook.isSelected()) {
            int moveNum = Integer.parseInt(lastMoveFen.split(" ")[5]);
            if (moveNum == 1 && turn) {
                int res = moveFromChessNotation("e4", true, 0);
                if (res == 1)
                    return;
            }
            if (moveNum < 7) {
                try {
                    InputStream resource = GamePanel.class.getResourceAsStream("GMGames.txt");
                    if (resource == null)
                        throw new FileNotFoundException();
                    BufferedReader  myObj = new BufferedReader (new InputStreamReader(resource));
                    Scanner myReader = new Scanner(myObj);
                    while (myReader.hasNextLine()) {
                        String data = myReader.nextLine();
                        if (chessMoveList.length() == 0) {
                            String m = data.split(" ")[0];
                            if (!moves.contains(m))
                                moves.add(m);
                            continue;
                        }
                        String m = data.split(" ")[chessMoveList.split(" ").length];
                        if (!moves.contains(m) && data.startsWith(chessMoveList.substring(0, chessMoveList.length() - 1))) {
                            moves.add(m);
                        }
                    }
                    myReader.close();
                } catch (FileNotFoundException e) {
                    System.out.println("File Not Found!");
                }
                Random rand = new Random();
                if (!moves.isEmpty()) {
                    String randomMove = moves.get(rand.nextInt(moves.size()));
                    int pro = 0;
                    if (randomMove.contains("=")) {
                        String p = randomMove.split("=")[1];
                        switch (Character.toLowerCase(p.charAt(0))) {
                            case 'n' -> pro = 1;
                            case 'b' -> pro = 2;
                            case 'r' -> pro = 3;
                            case 'q' -> pro = 4;
                        }
                    }
                    int res = moveFromChessNotation(randomMove, turn, pro);
                    if (res == 0) moves.clear();
                }
            }
        }
        int compMin = (turn) ? whiteMinutes : blackMinutes;
        int compSec = (turn) ? whiteSeconds : blackSeconds;
        if (whitePieces.size() + blackPieces.size() <= 7 && !(compMin == 0 && compSec <= 30) && tableBase && endGameTableBase.isSelected()) {
            isCompDone = false;
            if (!isGameEnded)
                playTableBase();
        } else if (moves.isEmpty()) {
            isCompDone = false;
            if (!isGameEnded)
                startThinking();
        }
    }

    public void playTableBase() {
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {

            @Override
            protected Void doInBackground() {
                try {
                    runPython(lastMoveFen, turn);
                } catch (IOException | URISyntaxException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void done() {
                isCompDone = true;
                ReadJson sample = new ReadJson();
                String[] res;
                try {
                    res = sample.getJson();
                } catch (IOException | ParseException e) {
                    tableBase = false;
                    computerMove();
                    return;
                }
                if (res[0].equals("losing")) {
                    tableBase = false;
                    computerMove();
                    return;
                }
                String m[] = res[1].split("=");
                int pro = 0;
                if (m.length > 1) {
                    switch (Character.toLowerCase(m[1].charAt(0))) {
                        case 'n' -> pro = 1;
                        case 'b' -> pro = 2;
                        case 'r' -> pro = 3;
                        case 'q' -> pro = 4;
                    }
                }
                int r = moveFromChessNotation(m[0], turn, pro);
                if (r == 0) {
                    tableBase = false;
                    computerMove();
                    return;
                }
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
                LocalDateTime now = LocalDateTime.now();
                System.out.println(dtf.format(now) + " ---> play with table base");
                System.out.println(dtf.format(now) + " <--- " + m[0] + " " + res[0]);
            }
        };
        worker.execute();
    }

    public void startThinking() {
        SwingWorker<Integer[], Void> worker = new SwingWorker<>() {

            @Override
            protected Integer[] doInBackground() throws ExecutionException, InterruptedException, TimeoutException {
                Integer[] result = new Integer[6];
                Client engine = engine1;
                if (enginePath.equals("") && !enginePath2.equals("")) engine = engine2;
                if (engineVsEngine.isSelected())
                    if (!enginePath.equals("") && !enginePath2.equals(""))
                        engine = (turn) ? engine1 : engine2;
                engine.command("position fen " + playerMoveList.get(moveListCounter - 1)[0], identity(), s -> s.startsWith("readyok"), 3000L);
                String bestMove;
                if (limitedDepth > 0) {
                    bestMove = engine.command(
                            "go depth " + limitedDepth,
                            lines -> lines.stream().filter(s -> s.startsWith("bestmove")).findFirst().get(),
                            line -> line.startsWith("bestmove"),
                            100000L);
                } else {
                    bestMove = engine.command(
                            "go movetime " + moveTime * times,
                            lines -> lines.stream().filter(s -> s.startsWith("bestmove")).findFirst().get(),
                            line -> line.startsWith("bestmove"),
                            moveTime * 3000L);
                }
                bestMove = bestMove.split(" ")[1];
                result[0] = 6;
                result[1] = 8 - Character.getNumericValue(bestMove.charAt(1));
                result[2] = RcolMapping.get(bestMove.charAt(0));
                result[3] = 8 - Character.getNumericValue(bestMove.charAt(3));
                result[4] = RcolMapping.get(bestMove.charAt(2));
                HashMap<Character, Integer> promotionMap = new HashMap<>();
                promotionMap.put('n', 1);
                promotionMap.put('b', 2);
                promotionMap.put('r', 3);
                promotionMap.put('q', 4);
                result[5] = 0;
                if (bestMove.length() >= 5)
                    result[5] = promotionMap.get(bestMove.charAt(4));
                return result;
            }

            @Override
            protected void done() {
                if (isTimeOver(turn))
                    return;
                Integer[] arr = null;
                try {
                    arr = get();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
                Piece p = null;
                Integer[] compMove = null;
                int promote = 0;
                if (arr != null) {
                    p = board[arr[1]][arr[2]].getPiece();
                    int points = arr[0];
                    compMove = new Integer[]{arr[3], arr[4]};
                    promote = arr[5];
                }
                if (p != null) {
                    movedFrom = new Integer[]{p.getRow(), p.getCol()};
                    movedTo = compMove;
                    currPiece = p;

                    if (board[compMove[0]][compMove[1]].isFull() || currPiece.type == Type.PAWN) {
                        halfMoves = 0;
                    }
                    chessMoveList += moveToChessNotation(compMove, currPiece, promote) + " ";
                    p.move(board, compMove[0], compMove[1], true, promote);
                    if (turn)
                        whiteTime += increment;
                    else
                        blackTime += increment;
                    whiteSeconds = whiteTime % 60;
                    whiteMinutes = (whiteTime - whiteSeconds) / 60;
                    blackSeconds = blackTime % 60;
                    blackMinutes = (blackTime - blackSeconds) / 60;

                    lastMoveFen = CurrentFen();
                    turn = !turn;
                    positions.add(Zobrist.getZobristHash(turn, wcks, wcqs, bcks, bcqs));
                    isInCheck = ChessGame.isInCheck(!p.getColor());
                    stalemate = ChessGame.getAllMoves(!p.getColor()).isEmpty();
                    isGameEnded = ChessGame.isStaleMate();
                    if (isGameEnded)
                        resignButton.setText("Exit");
                    GamePanel.animation = true;
                    if (isInCheck) {
                        clip = p.sound("chessCheckSound.wav");
                        chessMoveList = chessMoveList.substring(0, chessMoveList.length() - 1) + "+ ";
                    }
                    playerMoveList.add(new Object[]{lastMoveFen, movedFrom, movedTo, whiteTime, blackTime, turn, stalemate});
                    moveListCounter++;
                    if (isGameEnded || stalemate) {
                        if (isInCheck)
                            clip = p.sound("chessCheckmateSound.wav");
                        else
                            clip = p.sound("chessStalemateSound.wav");
                    }
                    clip.setFramePosition(0);
                    clip.start();
                }
                isCompDone = true;
                ChessGame.updateThreats(!turn);
                ChessGame.countMoves = 0;
                tableBase = true;
            }
        };
        worker.execute();
    }

    public static class myKeyAdapter extends KeyAdapter {

        JFrame frame;
        GamePanel panel;
        boolean full = true;

        myKeyAdapter(JFrame frame, GamePanel panel) {
            this.frame = frame;
            this.panel = panel;
        }

        @Override
        public void keyPressed(KeyEvent e) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_LEFT:
                    if (!animation && resignButton.isEnabled()) {
                        if (playerMoveList.size() > 1 && moveListCounter > 1) {
                            moveListCounter--;
                            Object[] last = playerMoveList.get(moveListCounter - 1);
                            movedFrom = (Integer[]) last[1];
                            movedTo = (Integer[]) last[2];
                            board = positionFromFen((String) last[0]);
                            ChessGame.updateBoards(turn);
                            ChessGame.updateThreats(!turn);
                            isInCheck = ChessGame.isInCheck(turn);
                            whiteTime = (int) last[3];
                            blackTime = (int) last[4];
                            panel.moves = null;
                            isGameEnded = false;
                            isCompDone = true;
                            stalemate = false;
                            drawGameOver = true;
                            resignButton.setText("Resign");
                        }
                        break;
                    }

                case KeyEvent.VK_RIGHT:
                    if (!animation) {
                        if (moveListCounter < playerMoveList.size()) {
                            Object[] last = playerMoveList.get(moveListCounter);
                            moveListCounter++;
                            movedFrom = (Integer[]) last[1];
                            movedTo = (Integer[]) last[2];
                            board = positionFromFen((String) last[0]);
                            ChessGame.updateBoards(turn);
                            ChessGame.updateThreats(!turn);
                            isInCheck = ChessGame.isInCheck(turn);
                            whiteTime = (int) last[3];
                            blackTime = (int) last[4];
                            panel.moves = null;
                            if ((boolean)last[6]) {
                                stalemate = true;
                                isGameEnded = true;
                            }
                        }
                        break;
                    }
                case KeyEvent.VK_ENTER:
                    if (e.isAltDown()) {
                        frame.dispose();
                        full = !full;
                        frame.setUndecorated(full);
                        frame.setAlwaysOnTop(full);
                        frame.pack();
                        frame.setVisible(true);
                        frame.setLocationRelativeTo(null);
                    }
                    break;
                case KeyEvent.VK_ESCAPE:
                    frame.dispose();
                    full = !full;
                    frame.setUndecorated(full);
                    frame.setAlwaysOnTop(full);
                    frame.pack();
                    frame.setVisible(true);
                    frame.setLocationRelativeTo(null);
                    break;
            }
        }
    }

    private void startOver() {
        running = false;
        isGameEnded = false;
        stalemate = false;
        dragging = false;
        drawGameOver = true;
        isCompDone = true;
        moves = null;
        board = positionFromFen(startFen);
        notAdded = true;
        takenBlackPieces = new ArrayList<>();
        takenWhitePieces = new ArrayList<>();
        positions.clear();
        playerMoveList.clear();
        moveListCounter = 0;
        chooseTime = 10;
        displayTime = 5;
        lastMoveFen = startFen;
        chessMoveList = "";
        movedFrom = null;
        movedTo = null;
        enPassant = new int[2];
        ChessGame.updateThreats(turn);
        ChessGame.updateThreats(!turn);
        isInCheck = ChessGame.isInCheck(turn);
        inMainWindow = true;
        Zobrist.fillArray();
        ChessGame.updateBoards(turn);
        this.add(menuBar);
        setPos.setEnabled(true);
        times = 1000;
        this.add(minSlider);
        this.add(incrementSlider);
        this.add(timeRunning);
    }

    private void createYesNoButton(String str, boolean yes, int x, int y, JPanel panel, boolean over) {
        MyButton button = new MyButton(str);
        if (yes)
            resignYesButton = button;
        else
            resignNoButton = button;
        ActionListener l = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (e.getSource() == button) {
                    panel.remove(resignYesButton);
                    panel.remove(resignNoButton);
                    resignButton.setEnabled(true);
                    changePersButton.setEnabled(true);
                    if (yes) {
                        panel.removeAll();
                        startOver();
                    } else {
                        if (over) {
                            drawGameOver = false;
                            resignButton.setEnabled(true);
                            changePersButton.setEnabled(true);
                        }
                    }
                    resignLabel = false;
                }
            }
        };
        button.addActionListener(l);
        button.setFocusable(false);
        button.setHoverBackgroundColor(Color.white);
        button.setPressedBackgroundColor(Color.white);
        button.setForeground(Color.black);
        button.setBackground(Color.white);
        button.setBorder(BorderFactory.createEmptyBorder());
        button.setFont(new Font("Arial Black", Font.PLAIN, 15));
        button.setBounds(x, y, 35, 20);
        this.add(button);
    }

    private void createPersButton(JPanel panel) {
        changePersButton = new MyButton();
        ActionListener l = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (e.getSource() == changePersButton) {
                    perspective = !perspective;
                }
            }
        };
        changePersButton.addActionListener(l);
        changePersButton.setFocusable(false);
        changePersButton.setBackground(new Color(45, 45, 45));
        changePersButton.setHoverBackgroundColor(new Color(60, 60, 60));
        changePersButton.setPressedBackgroundColor(new Color(100, 100, 100));
        changePersButton.setBorder(BorderFactory.createLineBorder(Color.white.darker(), 2));
        changePersButton.setForeground(Color.white);
        changePersButton.setBounds(w / 2 - 50, h + screenHeight - 50, 100, 50);
        try {
            Image img = ImageIO.read(Objects.requireNonNull(this.getClass().getResource("images/switch.png")));
            img = img.getScaledInstance(70, 40, Image.SCALE_SMOOTH);
            changePersButton.setIcon(new ImageIcon(img));
        } catch (Exception ex) {
            System.out.println(ex);
        }
        this.add(changePersButton);
    }

    private void createResignButton(JPanel panel) {
        resignButton = new MyButton("Resign");
        ActionListener l = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (e.getSource() == resignButton) {
                    createYesNoButton("NO", false, w + 170, h + 375, panel, false);
                    createYesNoButton("YES", true, w + 390, h + 375, panel, false);
                    resignButton.setEnabled(false);
                    changePersButton.setEnabled(false);
                    resignLabel = true;
                }
            }
        };
        resignButton.addActionListener(l);
        resignButton.setFocusable(false);
        resignButton.setBackground(new Color(45, 45, 45));
        resignButton.setHoverBackgroundColor(new Color(60, 60, 60));
        resignButton.setPressedBackgroundColor(new Color(100, 100, 100));
        resignButton.setBorder(BorderFactory.createLineBorder(Color.white.darker(), 2));
        resignButton.setForeground(Color.white);
        resignButton.setFont(new Font("Arial Black", Font.PLAIN, 15));
        resignButton.setBounds(w / 2 - 50, h, 100, 50);
        this.add(resignButton);
    }

    private void createEngineStartButton() {
        engineStartButton = new MyButton("Make Move");
        ActionListener l = e -> {
            if (e.getSource() == engineStartButton) {
                if (isCompDone) {
                    currPiece = null;
                    moves = null;
                    computerMove();
                }
            }
        };
        engineStartButton.addActionListener(l);
        engineStartButton.setFocusable(false);
        engineStartButton.setBackground(new Color(45, 45, 45));
        engineStartButton.setHoverBackgroundColor(new Color(60, 60, 60));
        engineStartButton.setPressedBackgroundColor(new Color(100, 100, 100));
        engineStartButton.setBorder(BorderFactory.createLineBorder(Color.white.darker(), 2));
        engineStartButton.setForeground(Color.white);
        engineStartButton.setFont(new Font("Arial Black", Font.PLAIN, 15));
        engineStartButton.setBounds(w + screenWidth + screenWidth / 7, h + screenHeight - 50, 100, 50);
        if (enginePath.equals("") && enginePath2.equals(""))
            engineStartButton.setEnabled(false);

        this.add(engineStartButton);
    }
    private void createMovesBoard() {
        movesBoard = new JLabel();
        movesBoard.setForeground(Color.white);
        movesBoard.setBackground(new Color(45, 45, 45));
        movesBoard.setVerticalAlignment(JLabel.TOP);
        movesBoard.setOpaque(true);
        movesBoard.setFont(new Font(Font.MONOSPACED, Font.BOLD, 18));
        Border boardBorder = BorderFactory.createLineBorder(Color.white.darker(), 1);
        movesBoard.setBorder(boardBorder);
        scrollPane = new JScrollPane(movesBoard);
        vertical = scrollPane.getVerticalScrollBar();
        scrollPane.setPreferredSize(new Dimension(w * 2 / 3, unitSize * 6));
        scrollPane.setBounds(w / 6, h + unitSize, w * 2 / 3, unitSize * 6);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(0, 0));
        this.add(scrollPane);
    }

    // Handles all the buttons on the main screen
    private void getClickedButtonOnMainWindow(MouseEvent e) {
        int height = (int) dim.getHeight();
        int width = (int) dim.getWidth();
        // piece color buttons
        if (e.getX() > w - (w / 5) * 2 && e.getX() < w - (w / 5) * 2 + 50 && e.getY() > height / 5 && e.getY() < height / 5 + 50) {
            computersColor = false;
            perspective = false;
        }
        if (e.getX() > w - (w / 5) * 4 && e.getX() < w - (w / 5) * 4 + 50 && e.getY() > height / 5 && e.getY() < height / 5 + 50) {
            computersColor = true;
            perspective = true;
        }

        // board color buttons
        // green
        if (e.getX() > w - (w / 10) * 9 && e.getX() < w - (w / 10) * 9 + 100 && e.getY() > height / 2 + height / 19 && e.getY() < height / 2 + height / 19 + 100) {
            white = new Color(238, 238, 210);
            black = new Color(118, 150, 86);
        }
        // brown
        if (e.getX() > w - (w / 10) * 4 && e.getX() < w - (w / 10) * 4 + 100 && e.getY() > height / 2 + height / 19 && e.getY() < height / 2 + height / 19 + 100) {
            white = new Color(240, 217, 181);
            black = new Color(181, 136, 99);
        }
        // blue
        if (e.getX() > w - (w / 10) * 9 && e.getX() < w - (w / 10) * 9 + 100 && e.getY() > height / 2 + height / 4 && e.getY() < height / 2 + height / 4 + 100) {
            white = new Color(222, 227, 230);
            black = new Color(140, 162, 173);
        }
        // grey
        if (e.getX() > w - (w / 10) * 4 && e.getX() < w - (w / 10) * 4 + 100 && e.getY() > height / 2 + height / 4 && e.getY() < height / 2 + height / 4 + 100) {
            white = new Color(220, 220, 220);
            black = new Color(171, 171, 171);
        }

        // play button
        if (e.getX() > width - w / 2 - w / 4 && e.getX() < width - w / 2 - w / 4 + 165 && e.getY() > height / 7 && e.getY() < height / 7 + 80) {
            running = true;
            inMainWindow = false;

            if (chooseTime < 6) {
                whiteTime = (int)chooseTime * 10;
                blackTime = (int)chooseTime * 10;
            }
            else {
                chooseTime -= 5;
                whiteTime = (int)chooseTime * 60;
                blackTime = (int)chooseTime * 60;
            }
            whiteSeconds = whiteTime % 60;
            whiteMinutes = (whiteTime - whiteSeconds) / 60;
            blackSeconds = blackTime % 60;
            blackMinutes = (blackTime - blackSeconds) / 60;

            playerMoveList.add(new Object[]{lastMoveFen, movedFrom, movedTo, whiteTime, blackTime, true, stalemate});
            moveListCounter = 1;
            this.removeAll();
            this.add(menuBar);
            setPos.setEnabled(false);
            createMovesBoard();
            createResignButton(this);
            createEngineStartButton();
            createPersButton(this);
            countDown.start();
            Clip clip = null;
            try {
                URL url = this.getClass().getResource(("chessOpeningSound.wav"));
                AudioInputStream audioStream = AudioSystem.getAudioInputStream(url);
                clip = AudioSystem.getClip();
                clip.open(audioStream);
                FloatControl gainControl =
                        (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                gainControl.setValue(-12.0f);
                clip.start();
                clip.setFramePosition(0);
            } catch (Exception e1) {
                System.out.println("sound not found!");
            }
        }
    }

    // This method shortens the player's move list and the string representation of all moves in the game
    public void shortenLists() {
        while (playerMoveList.size() > moveListCounter) {
            playerMoveList.remove(playerMoveList.size() - 1);
        }
        String tempMoves = "";
        String[] arr = chessMoveList.split(" ");
        for (int i = 0; i < moveListCounter - 1; i++) {
            tempMoves += arr[i] + " ";
        }
        chessMoveList = tempMoves;
    }

    // Puts the pieces on the board when setting a new position
    private void putPieces(MouseEvent e) throws IOException {
        int y = (e.getX() - w) / unitSize;
        int x = (e.getY() - h) / unitSize;
        if (perspective) {
            y = 7 - (e.getX() - w) / unitSize;
            x = 7 - (e.getY() - h) / unitSize;
        }

        if (e.getButton() == MouseEvent.BUTTON1 || e.getButton() == MouseEvent.BUTTON3) {
            boolean color = e.getButton() == MouseEvent.BUTTON1;
            List<Piece> pieces = (e.getButton() == MouseEvent.BUTTON1) ? whitePieces : blackPieces;

            if (x < 8 && x > -1 && y < 8 && y > -1) {
                if (board[x][y].isFull()) {
                    Piece p = board[x][y].getPiece();
                    board[x][y].makeEmpty();
                    if (p.getColor()) whitePieces.remove(p);
                    else blackPieces.remove(p);
                }
                if (positionFrame.isPawnPressed) {
                    Pawn p = new Pawn(x, y, color, 100);
                    board[x][y].assignPiece(p);
                    pieces.add(p);
                }
                if (positionFrame.isKnightPressed) {
                    Knight p = new Knight(x, y, color, 320);
                    board[x][y].assignPiece(p);
                    pieces.add(p);
                }
                if (positionFrame.isBishopPressed) {
                    Bishop p = new Bishop(x, y, color, 330);
                    board[x][y].assignPiece(p);
                    pieces.add(p);
                }
                if (positionFrame.isRookPressed) {
                    Rook p = new Rook(x, y, color, 500);
                    board[x][y].assignPiece(p);
                    pieces.add(p);
                }
                if (positionFrame.isQueenPressed) {
                    Queen p = new Queen(x, y, color, 950);
                    board[x][y].assignPiece(p);
                    pieces.add(p);
                }
                if (positionFrame.isKingPressed) {
                    King p = new King(x, y, color, 20000);
                    board[x][y].assignPiece(p);
                    pieces.add(p);
                }
            }
        } else if (e.getButton() == MouseEvent.BUTTON2) {
            if (board[x][y].isFull()) {
                Piece p = board[x][y].getPiece();
                board[x][y].makeEmpty();
                if (p.getColor()) whitePieces.remove(p);
                else blackPieces.remove(p);
            }
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (positionFrame != null) {
            if (isPositionFrameAlive) {
                try {
                    putPieces(e);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
        dragging = false;
        if (!isCompDone || isGameEnded || !running || resignLabel || e.getButton() != MouseEvent.BUTTON1) return;
        int y = (e.getX() - w) / unitSize;
        int x = (e.getY() - h) / unitSize;
        if (perspective) {
            y = 7 - (e.getX() - w) / unitSize;
            x = 7 - (e.getY() - h) / unitSize;
        }

        if (x < 8 && x > -1 && y < 8 && y > -1) {
            if (isCompDone && board[x][y].isFull() && board[x][y].getPiece().getColor() == turn && !animation) {
                currPiece = board[x][y].getPiece();
                this.moves = currPiece.possibleMoves();

            } else
                moves = null;
        }
    }

    @Override
    public void mouseEntered(MouseEvent arg0) {

    }

    @Override
    public void mouseExited(MouseEvent arg0) {

    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (inMainWindow && e.getButton() == MouseEvent.BUTTON1) {
            getClickedButtonOnMainWindow(e);
        }
        int y = (e.getX() - w) / unitSize;
        int x = (e.getY() - h) / unitSize;
        if (e.getButton() == MouseEvent.BUTTON3) {
            startArrow[0] = w + (unitSize / 2) + unitSize * y;
            startArrow[1] = h + (unitSize / 2) + unitSize * x;
            return;
        }
        if (!isCompDone || isGameEnded || !running || resignLabel || e.getButton() != MouseEvent.BUTTON1) return;
        if (perspective) {
            y = 7 - (e.getX() - w) / unitSize;
            x = 7 - (e.getY() - h) / unitSize;
        }
        arrows.clear();
        redSquares.clear();
        clickedSpot = new int[]{x, y};
        if (x < 8 && x > -1 && y < 8 && y > -1)
            if (isCompDone && board[x][y].isFull() && board[x][y].getPiece().getColor() == turn && !animation) {
                currPiece = board[x][y].getPiece();
                dragging = true;
                this.moves = currPiece.possibleMoves();
            }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        int y = (e.getX() - w) / unitSize;
        int x = (e.getY() - h) / unitSize;
        if (e.getButton() == MouseEvent.BUTTON3 && x < 8 && x >= 0 && y < 8 && y >= 0) {
            int endx = w + (unitSize / 2) + (unitSize * y);
            int endy = h + (unitSize / 2) + (unitSize * x);
            Arrow a = new Arrow(startArrow[0], startArrow[1], endx, endy);
            for (Arrow arrow : arrows)
                if (arrow.equals(a)) {
                    arrows.remove(arrow);
                    return;
                }
            if (startArrow[0] != endx || startArrow[1] != endy) {
                arrows.add(a);
            } else {
                Integer[] arr = new Integer[]{y, x};
                for (Integer[] square : redSquares) {
                    if (square[0] == arr[0] && square[1] == arr[1]) {
                        redSquares.remove(square);
                        return;
                    }
                }
                redSquares.add(new Integer[]{y, x});
            }
            return;
        }
        arrows.clear();
        redSquares.clear();
        if (!isCompDone || isGameEnded || !running || resignLabel || e.getButton() != MouseEvent.BUTTON1) return;
        if (perspective) {
            y = 7 - (e.getX() - w) / unitSize;
            x = 7 - (e.getY() - h) / unitSize;
        }
        dragging = false;
        if (moves != null && turn == currPiece.getColor()) {
            Integer[] m = {x, y};
            for (Integer[] move : moves) {
                if (m[0].equals(move[0]) && m[1].equals(move[1])) {
                    if (moveListCounter < playerMoveList.size())
                        shortenLists();
                    movedFrom = new Integer[]{currPiece.getRow(), currPiece.getCol()};
                    movedTo = move;
//					if (computersColor) {
//						fullMoves++;
//						halfMoves++;
//					}
                    if (board[x][y].isFull() || currPiece.type == Type.PAWN) {
                        halfMoves = 0;
                    }
                    String lastMove = moveToChessNotation(move, currPiece, 0) + " ";
                    chessMoveList += lastMove;
                    currPiece.move(board, x, y, true, 0);
                    if (turn)
                        whiteTime += increment;
                    else
                        blackTime += increment;
                    whiteSeconds = whiteTime % 60;
                    whiteMinutes = (whiteTime - whiteSeconds) / 60;
                    blackSeconds = blackTime % 60;
                    blackMinutes = (blackTime - blackSeconds) / 60;

                    lastMoveFen = CurrentFen();
                    turn = !turn;
                    positions.add(Zobrist.getZobristHash(turn, wcks, wcqs, bcks, bcqs));
                    isInCheck = ChessGame.isInCheck(!currPiece.getColor());
                    stalemate = ChessGame.getAllMoves(!currPiece.getColor()).isEmpty();
                    isGameEnded = ChessGame.isStaleMate();
                    if (playWithEngine.isSelected() && (engine1 != null || engine2 != null))
                        enginePlay = true;
                    if (m[0] == clickedSpot[0] && m[1] == clickedSpot[1])
                        animation = true;
                    moves = null;
                    if (currPiece.type == Type.PAWN && (m[0] == 0 || m[0] == 7)) {
                        try {
                            turn = !turn;
                            choosePiece = new ChoosePieceFrame(turn);
                            Pawn p = (Pawn) currPiece;
                            p.promote(board, choosePiece.pressed);
                            isInCheck = ChessGame.isInCheck(!currPiece.getColor());
                            stalemate = ChessGame.getAllMoves(!currPiece.getColor()).isEmpty();
                            isGameEnded = ChessGame.isStaleMate();
                            turn = !turn;
                            lastMoveFen = CurrentFen();
                            turn = !turn;
                            int pr = (choosePiece != null) ? choosePiece.pressed : 0;
                            chessMoveList = chessMoveList.substring(0, chessMoveList.length() - 1);
                            switch (pr) {
                                case 1 -> lastMove = "=N ";
                                case 2 -> lastMove = "=B ";
                                case 3 -> lastMove = "=R ";
                                case 4 -> lastMove = "=Q ";
                            }
                            chessMoveList += lastMove;
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                    if (isGameEnded)
                        resignButton.setText("Exit");
                    if (isInCheck) {
                        clip = currPiece.sound("chessCheckSound.wav");
                        chessMoveList = chessMoveList.substring(0, chessMoveList.length() - 1) + "+ ";
                    }
                    playerMoveList.add(new Object[]{lastMoveFen, movedFrom, movedTo, whiteTime, blackTime, !computersColor, stalemate});
                    moveListCounter++;
                    if (isGameEnded || stalemate) {
                        if (isInCheck)
                            clip = currPiece.sound("chessCheckmateSound.wav");
                        else
                            clip = currPiece.sound("chessStalemateSound.wav");
                    }
                    if (clip != null) {
                        clip.setFramePosition(0);
                        clip.start();
                    }
                    ChessGame.updateThreats(!turn);
                    break;
                }
            }
        }
        if (clickedSpot[0] != x || clickedSpot[1] != y)
            moves = null;
    }

    // Load position from fen string
    public static Spot[][] positionFromFen(String fen) {
        whitePieces = new ArrayList<>();
        blackPieces = new ArrayList<>();
        board = ChessGame.generate_Board(8);
        String[] sections = fen.split(" ");
        ChessGame.WP = 0L;
        ChessGame.WN = 0L;
        ChessGame.WB = 0L;
        ChessGame.WR = 0L;
        ChessGame.WQ = 0L;
        ChessGame.WK = 0L;
        ChessGame.BP = 0L;
        ChessGame.BN = 0L;
        ChessGame.BB = 0L;
        ChessGame.BR = 0L;
        ChessGame.BQ = 0L;
        ChessGame.BK = 0L;

        int whitePawns = 0, whiteKnights = 0, whiteBishops = 0, whiteRooks = 0, whiteQueens = 0;
        int blackPawns = 0, blackKnights = 0, blackBishops = 0, blackRooks = 0, blackQueens = 0;

        int file = 0;
        int rank = 0;

        int errCount = 0;
        for (int i = 0; i < sections[0].length(); i++) {
            char symbol = sections[0].charAt(i);
            if (symbol == '/') {
                file = 0;
                rank++;
            } else {
                if (Character.isDigit(symbol)) {
                    file += Character.getNumericValue(symbol);
                } else {
                    String Binary = "0000000000000000000000000000000000000000000000000000000000000000";
                    Binary = Binary.substring(rank * 8 + file + 1) + "1" + Binary.substring(0, rank * 8 + file);
                    boolean pieceColor = Character.isUpperCase(symbol);
                    Piece piece = null;
                    try {
                        switch (Character.toLowerCase(symbol)) {
                            case 'p' -> {
                                piece = new Pawn(rank, file, pieceColor, 100);
                                if (pieceColor) {
                                    ChessGame.WP += convertStringToBitboard(Binary);
                                    whitePawns++;
                                } else {
                                    ChessGame.BP += convertStringToBitboard(Binary);
                                    blackPawns++;

                                }
                            }
                            case 'k' -> {
                                piece = new King(rank, file, pieceColor, 20000);
                                if (pieceColor)
                                    ChessGame.WK += convertStringToBitboard(Binary);
                                else
                                    ChessGame.BK += convertStringToBitboard(Binary);
                            }
                            case 'n' -> {
                                piece = new Knight(rank, file, pieceColor, 320);
                                if (pieceColor) {
                                    ChessGame.WN += convertStringToBitboard(Binary);
                                    whiteKnights++;
                                } else {
                                    ChessGame.BN += convertStringToBitboard(Binary);
                                    blackKnights++;
                                }
                            }
                            case 'b' -> {
                                piece = new Bishop(rank, file, pieceColor, 330);
                                if (pieceColor) {
                                    ChessGame.WB += convertStringToBitboard(Binary);
                                    whiteBishops++;
                                } else {
                                    ChessGame.BB += convertStringToBitboard(Binary);
                                    blackBishops++;
                                }
                            }
                            case 'r' -> {
                                piece = new Rook(rank, file, pieceColor, 500);
                                if (pieceColor) {
                                    ChessGame.WR += convertStringToBitboard(Binary);
                                    whiteRooks++;
                                } else {
                                    ChessGame.BR += convertStringToBitboard(Binary);
                                    blackRooks++;
                                }
                            }
                            case 'q' -> {
                                piece = new Queen(rank, file, pieceColor, 950);
                                if (pieceColor) {
                                    ChessGame.WQ += convertStringToBitboard(Binary);
                                    whiteQueens++;
                                } else {
                                    ChessGame.BQ += convertStringToBitboard(Binary);
                                    blackQueens++;
                                }
                            }
                        }
                    } catch (Exception e) {
                        System.out.println("something went wrong!" + errCount);
                        errCount++;
                    }
                    if (piece != null) {
                        board[rank][file].assignPiece(piece);
                        if (pieceColor)
                            whitePieces.add(piece);
                        else
                            blackPieces.add(piece);
                    }
                    file++;
                }
            }
        }
        takenWhitePieces = new ArrayList<>();
        takenBlackPieces = new ArrayList<>();

        for (int i = 8 - whitePawns; i > 0; i--) {
            try {
                takenWhitePieces.add(new Pawn(0, 0, true, 100));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        for (int i = 2 - whiteKnights; i > 0; i--) {
            try {
                takenWhitePieces.add(new Knight(0, 0, true, 320));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        for (int i = 2 - whiteBishops; i > 0; i--) {
            try {
                takenWhitePieces.add(new Bishop(0, 0, true, 330));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        for (int i = 2 - whiteRooks; i > 0; i--) {
            try {
                takenWhitePieces.add(new Rook(0, 0, true, 500));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        for (int i = 1 - whiteQueens; i > 0; i--) {
            try {
                takenWhitePieces.add(new Queen(0, 0, true, 900));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        for (int i = 8 - blackPawns; i > 0; i--) {
            try {
                takenBlackPieces.add(new Pawn(0, 0, false, 100));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        for (int i = 2 - blackKnights; i > 0; i--) {
            try {
                takenBlackPieces.add(new Knight(0, 0, false, 320));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        for (int i = 2 - blackBishops; i > 0; i--) {
            try {
                takenBlackPieces.add(new Bishop(0, 0, false, 330));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        for (int i = 2 - blackRooks; i > 0; i--) {
            try {
                takenBlackPieces.add(new Rook(0, 0, false, 500));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        for (int i = 1 - blackQueens; i > 0; i--) {
            try {
                takenBlackPieces.add(new Queen(0, 0, false, 900));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }


        if (sections.length > 1)
            turn = sections[1].equalsIgnoreCase("w");
        else
            turn = true;

        String castlingRights = (sections.length > 2) ? sections[2] : "";

        wcks = castlingRights.contains("K");

        wcqs = castlingRights.contains("Q");

        bcks = castlingRights.contains("k");

        bcqs = castlingRights.contains("q");
        if (sections.length > 3) {
            if (!sections[3].equals("-")) {
                enPassant[1] = RcolMapping.get(sections[3].charAt(0));
                enPassant[0] = 8 - Character.getNumericValue(sections[3].charAt(1));

            }
        }

        halfMoves = Integer.parseInt(sections[4]);
        fullMoves = Integer.parseInt(sections[5]);

        boolean c = true;
        Type[] types = {Type.PAWN, Type.KNIGHT, Type.BISHOP, Type.ROOK, Type.QUEEN, Type.KING};
        long[] tables = {ChessGame.WP, ChessGame.WN, ChessGame.WB, ChessGame.WR, ChessGame.WQ, ChessGame.WK, ChessGame.BP, ChessGame.BN, ChessGame.BB, ChessGame.BR, ChessGame.BQ, ChessGame.BK};
        for (int i = 0; i < 12; i++) {
            ChessGame.pieceTables.put("" + c + types[i % 6], tables[i]);
            if (i == 5)
                c = false;
        }
        return board;
    }

    // Get the fen string of the current position
    public static String CurrentFen() {
        StringBuilder fen = new StringBuilder();

        int whitePawns = 0, whiteKnights = 0, whiteBishops = 0, whiteRooks = 0, whiteQueens = 0;
        int blackPawns = 0, blackKnights = 0, blackBishops = 0, blackRooks = 0, blackQueens = 0;

        for (int rank = 0; rank < 8; rank++) {
            int numEmptyFiles = 0;
            for (int file = 0; file < 8; file++) {
                Piece piece = board[rank][file].getPiece();
                if (piece != null) {
                    if (numEmptyFiles != 0) {
                        fen.append(numEmptyFiles);
                        numEmptyFiles = 0;
                    }
                    boolean isBlack = !piece.getColor();
                    Type pieceType = piece.type;
                    char pieceChar = ' ';
                    switch (pieceType) {
                        case ROOK -> {
                            pieceChar = 'R';
                            if (isBlack) blackRooks++;
                            else whiteRooks++;
                        }
                        case KNIGHT -> {
                            pieceChar = 'N';
                            if (isBlack) blackKnights++;
                            else whiteKnights++;
                        }
                        case BISHOP -> {
                            pieceChar = 'B';
                            if (isBlack) blackBishops++;
                            else whiteBishops++;
                        }
                        case QUEEN -> {
                            pieceChar = 'Q';
                            if (isBlack) blackQueens++;
                            else whiteQueens++;
                        }
                        case KING -> pieceChar = 'K';
                        case PAWN -> {
                            pieceChar = 'P';
                            if (isBlack) blackPawns++;
                            else whitePawns++;
                        }
                    }
                    fen.append((isBlack) ? Character.toLowerCase(pieceChar) : pieceChar);
                } else {
                    numEmptyFiles++;
                }

            }
            if (numEmptyFiles != 0) {
                fen.append(numEmptyFiles);
            }
            if (rank != 7) {
                fen.append('/');
            }
        }

        // Side to move
        fen.append(' ');
        fen.append((turn) ? 'b' : 'w');

        // Castling
        fen.append(' ');
        fen.append((wcks) ? "K" : "");
        fen.append((wcqs) ? "Q" : "");
        fen.append((bcks) ? "k" : "");
        fen.append((bcqs) ? "q" : "");
        if (!wcks && !wcqs && !bcks && !bcqs)
            fen.append("-");

        // En-passant
        fen.append(' ');
        HashMap<Integer, String> colMapping = new HashMap<>();
        colMapping.put(0, "a");
        colMapping.put(1, "b");
        colMapping.put(2, "c");
        colMapping.put(3, "d");
        colMapping.put(4, "e");
        colMapping.put(5, "f");
        colMapping.put(6, "g");
        colMapping.put(7, "h");
        boolean added = false;
        if (turn && currPiece != null) {
            if (currPiece.type == Type.PAWN && movedFrom != null && movedTo != null)
                if (movedFrom[0] == 6 && movedTo[0] == 4 && currPiece.lastPos[0] == 6 && currPiece.getRow() == 4) {
                    added = true;
                    Pawn pa = (Pawn) currPiece;
                    enPassant[0] = pa.getRow() + 1;
                    enPassant[1] = pa.getCol();
                    fen.append(colMapping.get(pa.getCol()));
                    fen.append(8 - pa.getRow() - 1);
                }
        } else if (currPiece != null) {
            if (currPiece.type == Type.PAWN && movedFrom != null && movedTo != null)
                if (movedFrom[0] == 1 && movedTo[0] == 3 && currPiece.lastPos[0] == 1 && currPiece.getRow() == 3) {
                    added = true;
                    Pawn pa = (Pawn) currPiece;
                    enPassant[0] = pa.getRow() - 1;
                    enPassant[1] = pa.getCol();
                    fen.append(colMapping.get(pa.getCol()));
                    fen.append(8 - pa.getRow() + 1);
                }
        }
        if (!added) {
            fen.append("-");
            enPassant[0] = 0;
            enPassant[1] = 0;
        }
        lastEnPassant[0] = enPassant[0];
        lastEnPassant[1] = enPassant[1];

//		// 50 move counter
        fen.append(' ');
        fen.append(halfMoves);

        // Full-move count (should be one at start, and increase after each move by black)
        fen.append(' ');
        fen.append(fullMoves);
        takenWhitePieces = new ArrayList<>();
        takenBlackPieces = new ArrayList<>();

        for (int i = 8 - whitePawns; i > 0; i--) {
            try {
                takenWhitePieces.add(new Pawn(0, 0, true, 100));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        for (int i = Math.max(2, blackKnights) - whiteKnights; i > 0; i--) {
            try {
                takenWhitePieces.add(new Knight(0, 0, true, 320));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        for (int i = Math.max(2, blackBishops) - whiteBishops; i > 0; i--) {
            try {
                takenWhitePieces.add(new Bishop(0, 0, true, 330));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        for (int i = Math.max(2, blackRooks) - whiteRooks; i > 0; i--) {
            try {
                takenWhitePieces.add(new Rook(0, 0, true, 500));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        for (int i = Math.max(1, blackQueens) - whiteQueens; i > 0; i--) {
            try {
                takenWhitePieces.add(new Queen(0, 0, true, 900));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        for (int i = 8 - blackPawns; i > 0; i--) {
            try {
                takenBlackPieces.add(new Pawn(0, 0, false, 100));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        for (int i = Math.max(2, whiteKnights) - blackKnights; i > 0; i--) {
            try {
                takenBlackPieces.add(new Knight(0, 0, false, 320));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        for (int i = Math.max(2, whiteBishops) - blackBishops; i > 0; i--) {
            try {
                takenBlackPieces.add(new Bishop(0, 0, false, 330));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        for (int i = Math.max(2, whiteRooks) - blackRooks; i > 0; i--) {
            try {
                takenBlackPieces.add(new Rook(0, 0, false, 500));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        for (int i = Math.max(1, whiteQueens) - blackQueens; i > 0; i--) {
            try {
                takenBlackPieces.add(new Queen(0, 0, false, 900));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return fen.toString();
    }

    // Converts a move from the format "piece, [row, col]" to Chess Notation
    public static String moveToChessNotation(Integer[] move, Piece p, int promote) {
        if (move == null) return "";
        if (p.type == Type.KING) {
            if (p.getColor()) {
                if (p.getRow() == 7 && p.getCol() == 4) {
                    if (move[0] == 7 && move[1] == 6)
                        return "O-O";
                    if (move[0] == 7 && move[1] == 2)
                        return "O-O-O";
                }
            } else {
                if (p.getRow() == 0 && p.getCol() == 4) {
                    if (move[0] == 0 && move[1] == 6)
                        return "O-O";
                    if (move[0] == 0 && move[1] == 2)
                        return "O-O-O";
                }
            }
        }
        StringBuilder result = new StringBuilder();
        HashMap<Type, Character> pieceMapping = new HashMap<>();
        pieceMapping.put(Type.KNIGHT, 'N');
        pieceMapping.put(Type.BISHOP, 'B');
        pieceMapping.put(Type.ROOK, 'R');
        pieceMapping.put(Type.QUEEN, 'Q');
        pieceMapping.put(Type.KING, 'K');
        HashMap<Integer, String> colMapping = new HashMap<>();
        colMapping.put(0, "a");
        colMapping.put(1, "b");
        colMapping.put(2, "c");
        colMapping.put(3, "d");
        colMapping.put(4, "e");
        colMapping.put(5, "f");
        colMapping.put(6, "g");
        colMapping.put(7, "h");
        String row = "" + (8 - move[0]);
        String col = colMapping.get(move[1]);
        if (p.type != Type.PAWN) {
            result.append(pieceMapping.get(p.type));
            long moves = p.getMoves();
            List<Piece> pieces = (p.getColor()) ? whitePieces : blackPieces;
            for (Piece piece : pieces) {
                if (p.type == piece.type && p != piece) {
                    long m = piece.getMoves();
                    long merged = moves & m;
                    String s = Long.toBinaryString(merged);
                    String zeros = "0000000000000000000000000000000000000000000000000000000000000000";
                    s = zeros.substring(s.length()) + s;
                    int index = 63 - (move[0] * 8 + move[1]);
                    if (s.charAt(index) == '1') {
                        result.append(colMapping.get(p.getCol()));
                    }
                }
            }
        }
        if (GamePanel.board[move[0]][move[1]].isFull()) {
            if (p.type == Type.PAWN) {
                result.append(colMapping.get(p.getCol()));
            }
            result.append("x");
        }
        result.append(col).append(row);
        if (p.type == Type.PAWN)
            if (p.getColor() && row.equals("8") || !p.getColor() && row.equals("1")) {
                if (promote == 1)
                    result.append("=N");
                else if (promote == 2)
                    result.append("=B");
                else if (promote == 3)
                    result.append("=R");
                else if (promote == 4)
                    result.append("=Q");
            }

        return result.toString();
    }

    // This method checks if the current board state has occurred three times in the game's move history, which would result in a draw by repetition
    public static boolean threefoldRepetition() {
        int count = 0;
        for (Object[] o : playerMoveList) {
            count = 0;
            String fenString = (String) o[0];
            String[] a = fenString.split(" ");
            fenString = a[0] + " " + a[1];
            for (Object[] o1 : playerMoveList) {
                String fenString1 = (String) o1[0];
                String[] arr = fenString1.split(" ");
                fenString1 = arr[0] + " " + arr[1];
                if (fenString.equals(fenString1)) count++;
            }
        }
        return count > 2;
    }

    // Make a move according to the Chess Notation move that is specified
    public static int moveFromChessNotation(String move, boolean color, int promote) {
        HashMap<Character, Integer> TcolMapping = new HashMap<>();
        TcolMapping.put('a', 0);
        TcolMapping.put('b', 1);
        TcolMapping.put('c', 2);
        TcolMapping.put('d', 3);
        TcolMapping.put('e', 4);
        TcolMapping.put('f', 5);
        TcolMapping.put('g', 6);
        TcolMapping.put('h', 7);

        HashMap<Character, Type> pieceMapping = new HashMap<>();
        pieceMapping.put('N', Type.KNIGHT);
        pieceMapping.put('B', Type.BISHOP);
        pieceMapping.put('R', Type.ROOK);
        pieceMapping.put('Q', Type.QUEEN);
        pieceMapping.put('K', Type.KING);

        Type pieceType = Type.PAWN;
        int row = 0;
        int col = 0;
        int specificCol = -1;
        int specificRow = -1;
        if (move.equals("O-O")) {
            row = (color) ? 7 : 0;
            col = 6;
            pieceType = Type.KING;
        } else if (move.equals("O-O-O")) {
            row = (computersColor) ? 7 : 0;
            col = 2;
            pieceType = Type.KING;
        } else if (move.length() == 2) {
            row = 8 - Character.getNumericValue(move.charAt(1));
            col = TcolMapping.get(move.charAt(0));
        } else if (move.length() == 3) {
            if (move.charAt(2) == '+') {
                row = 8 - Character.getNumericValue(move.charAt(1));
                col = TcolMapping.get(move.charAt(0));
            } else {
                pieceType = pieceMapping.get(move.charAt(0));
                row = 8 - Character.getNumericValue(move.charAt(2));
                col = TcolMapping.get(move.charAt(1));
            }
        } else if (move.length() == 4) {
            if (move.charAt(3) == '+' | move.charAt(3) == '#') {
                pieceType = pieceMapping.get(move.charAt(0));
                row = 8 - Character.getNumericValue(move.charAt(2));
                col = TcolMapping.get(move.charAt(1));
            } else {
                row = 8 - Character.getNumericValue(move.charAt(3));
                col = TcolMapping.get(move.charAt(2));
                if (Character.isUpperCase(move.charAt(0)))
                    pieceType = pieceMapping.get(move.charAt(0));
                if (Character.isDigit(move.charAt(1))) {
                    specificRow = 8 - Character.getNumericValue(move.charAt(1));
                } else if (move.charAt(1) != 'x') {
                    specificCol = TcolMapping.get(move.charAt(1));
                }
            }
        } else if (move.length() == 5) {
            if (move.charAt(4) == '+' | move.charAt(4) == '#') {
                row = 8 - Character.getNumericValue(move.charAt(3));
                col = TcolMapping.get(move.charAt(2));
            } else {
                row = 8 - Character.getNumericValue(move.charAt(4));
                col = TcolMapping.get(move.charAt(3));
            }
            if (Character.isUpperCase(move.charAt(0)))
                pieceType = pieceMapping.get(move.charAt(0));
            if (Character.isDigit(move.charAt(1))) {
                specificRow = 8 - Character.getNumericValue(move.charAt(1));
            } else if (move.charAt(1) != 'x') {
                specificCol = TcolMapping.get(move.charAt(1));
            }
        }
        List<Piece> pieces = (color) ? whitePieces : blackPieces;
        for (Piece p : pieces) {
            if (p.type == pieceType) {
                List<Integer[]> moves = p.possibleMoves();
                for (Integer[] m : moves) {
                    if (m[0] == row && m[1] == col) {
                        if ((specificCol == -1 && specificRow == -1) || (specificCol != -1 && p.getCol() == specificCol) || (specificRow != -1 && p.getRow() == specificRow)) {
                            movedFrom = new Integer[]{p.getRow(), p.getCol()};
                            movedTo = new Integer[]{row, col};
                            currPiece = p;
//							if (!computersColor) {
//								fullMoves++;
//								halfMoves++;
//							}
                            if (board[row][col].isFull() || currPiece.type == Type.PAWN) {
                                halfMoves = 0;
                            }
                            String lastMove = move + " ";
                            chessMoveList += lastMove;
                            p.move(board, row, col, true, promote);
                            if (computersColor)
                                whiteTime += increment;
                            else
                                blackTime += increment;
                            lastMoveFen = CurrentFen();
                            turn = !turn;
                            GamePanel.animation = true;
                            positions.add(Zobrist.getZobristHash(turn, wcks, wcqs, bcks, bcqs));
                            isInCheck = ChessGame.isInCheck(!p.getColor());
                            stalemate = ChessGame.getAllMoves(!p.getColor()).isEmpty();
                            isGameEnded = ChessGame.isStaleMate();
                            if (isGameEnded)
                                resignButton.setText("Exit");
                            if (isInCheck) {
                                clip = p.sound("chessCheckSound.wav");
                            }
                            playerMoveList.add(new Object[]{lastMoveFen, movedFrom, movedTo, whiteTime, blackTime, computersColor, stalemate});
                            moveListCounter++;
                            if (isGameEnded || stalemate) {
                                if (isInCheck)
                                    clip = p.sound("chessCheckmateSound.wav");
                                else
                                    clip = p.sound("chessStalemateSound.wav");
                            }
                            clip.setFramePosition(0);
                            clip.start();
                            isCompDone = true;
                            ChessGame.countMoves = 0;
                            return 1;
                        }
                    }
                }
            }
        }
        return 0;
    }

    // Converts a binary string representation of a bitboard to a long integer value
    public static long convertStringToBitboard(String Binary) {
        if (Binary.charAt(0) == '0') {
            return Long.parseLong(Binary, 2);
        } else {
            return Long.parseLong("1" + Binary.substring(2), 2) * 2;
        }
    }

    // Executes a Python script that scrapes the web for a table-base evaluation of the fen specified
    public void runPython(String fen, boolean color) throws IOException, URISyntaxException {
        String c = color + "";
        String name = "tableBase.py";
        String[] cmd = {
                "python",
                name,
                c,
                fen
        };
        Process p = Runtime.getRuntime().exec(cmd);
        try {
            p.waitFor(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // Loads and starts a chess engine, checks its compatibility with UCI protocol, and updates UI components
    public void loadEngine(int engineNum) {
        Client engine;
        JMenuItem remove;
        if (engineNum == 1) {
            engine1 = new Client();
            engine1.start(enginePath);
            engine = engine1;
            remove = removeEngine1;
        } else {
            engine2 = new Client();
            engine2.start(enginePath2);
            engine = engine2;
            remove = removeEngine2;
        }
        String ret = "";
        try {
            ret = engine.command(
                    "uci",
                    lines -> lines.stream().filter(s -> s.startsWith("uciok")).findFirst().get(),
                    line -> line.startsWith("uciok"),
                    11000L).split(" ")[0];
            remove.setEnabled(true);
        } catch (InterruptedException | ExecutionException | TimeoutException ex) {
            System.err.println("file is not UCI compatible");
            engine = null;
        }
        if (ret != "") {
            engineStartButton.setEnabled(true);
            if (!engineVsEngine.isSelected())
                playWithEngine.setState(true);
            createAndWriteToEnginesFile(engineNum);
        } else engineStartButton.setEnabled(false);
    }

    // This method creates a new file and writes the engine path to it,
    // depending on the engine number passed as the "engineNum" parameter
    void createAndWriteToEnginesFile(int engineNum) {
        String path = enginePath;
        String to = "activeEngine.txt";
        if (engineNum == 2) {
            to = "activeEngine2.txt";
            path = enginePath2;
        }
        try {
            FileWriter myWriter = new FileWriter(System.getProperty("user.dir") + "\\" + to);
            // Writes this content into the specified file
            myWriter.write(path);

            // Closing is necessary to retrieve the resources allocated
            myWriter.close();
            System.out.println("Successfully wrote to the file.");
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

    // This method clears the content of a file specified by the "fileName" parameter
    void clearFile(String fileName) {
        try {
            FileWriter myWriter = new FileWriter(System.getProperty("user.dir") + "\\" + fileName);
            // Writes this content into the specified file
            myWriter.write("");

            // Closing is necessary to retrieve the resources allocated
            myWriter.close();
            System.out.println("Successfully wrote to the file.");
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

    // This method reads the first line of a file and returns it as a string
    String readLineFromFile(String fileName) {
        String res = "";
        try {
            File myObj = new File(System.getProperty("user.dir") + "\\" + fileName);
            Scanner myReader = new Scanner(myObj);
            if (myReader.hasNextLine()) {
                res = myReader.nextLine();
            }
            myReader.close();
        } catch (FileNotFoundException e) {
            System.out.println("File Not Found");
        }
        return res;
    }

    boolean isFileExists(String filePath) {
        File f = new File(filePath);
        return f.exists() && !f.isDirectory();
    }

    // Returns true if the rank contains 8 elements (that is, a combination of
    // pieces and empty spaces). Assumes string contains valid chess pieces and
    // the digits 1..8.
    private static boolean verifyRank(String rank) {
        int count = 0;
        for (int i = 0; i < rank.length(); i++) {
            if (rank.charAt(i) >= '1' && rank.charAt(i) <= '8') {
                count += (rank.charAt(i) - '0');
            } else {
                count++;
            }
        }
        return count != 8;
    }

    // This method is used to switch between the two Chess engines
    public void switchEngines() {
        String temp = enginePath;
        enginePath = enginePath2;
        enginePath2 = temp;
        temp = firstEngineName;
        firstEngineName = secondEngineName;
        secondEngineName = temp;
        createAndWriteToEnginesFile(1);
        createAndWriteToEnginesFile(2);
        engine1.close();
        engine2.close();
        loadEngine(1);
        loadEngine(2);
    }

    public static boolean isValidFen(String fen) {
        Pattern pattern = Pattern.compile("((([prnbqkPRNBQK12345678]*/){7})([prnbqkPRNBQK12345678]*)) (w|b) ((K?Q?k?q?)|\\-) (([abcdefgh][36])|\\-) (\\d*) (\\d*)");
        Matcher matcher = pattern.matcher(fen);
        if (!matcher.matches()) {
            return false;
        }

        // Check each rank.
        String[] ranks = matcher.group(2).split("/");
        for (String rank : ranks) {
            if (verifyRank(rank)) {
                return false;
            }
        }
        if (verifyRank(matcher.group(4))) {
            return false;
        }

        // Check for more than one 'k' or more than one 'K'.
        long countK = fen.chars().filter(ch -> ch == 'k').count();
        if (countK > 1) {
            return false;
        }
        long countCapitalK = fen.chars().filter(ch -> ch == 'K').count();
        if (countCapitalK > 1) {
            return false;
        }
        // Check if kings are adjacent
        int whiteKingIndex = getKingIndex(fen, 'K');
        int blackKingIndex = getKingIndex(fen, 'k');

        int whiteKingRank = 8 - whiteKingIndex / 8;
        int whiteKingFile = whiteKingIndex % 8;

        int blackKingRank = 8 - blackKingIndex / 8;
        int blackKingFile = blackKingIndex % 8;

        if (Math.abs(whiteKingRank - blackKingRank) <= 1 && Math.abs(whiteKingFile - blackKingFile) <= 1)
            return false;

        // Check draw by insufficient material
        if (ChessGame.isStaleMate())
            return false;

        // Check two kings.
        return matcher.group(1).contains("k") && matcher.group(1).contains("K");
    }

    private static int getKingIndex(String fen, char king) {
        String[] sections = fen.split(" ");
        int file = 0;
        int rank = 0;

        for (int i = 0; i < sections[0].length(); i++) {
            char symbol = sections[0].charAt(i);
            if (symbol == '/') {
                file = 0;
                rank++;
            } else {
                if (Character.isDigit(symbol)) {
                    file += Character.getNumericValue(symbol);
                } else {
                    if (symbol == king) return rank*8+file;
                }
            }
        }
        return -1;
    }

}

