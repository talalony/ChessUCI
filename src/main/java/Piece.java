import java.awt.*;
import java.io.IOException;
import java.net.URL;

import javax.sound.sampled.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public abstract class Piece implements Comparable<Piece> {

    static long FILE_A = 72340172838076673L;
    static long FILE_H = -9187201950435737472L;
    static long FILE_AB = 217020518514230019L;
    static long FILE_GH = -4557430888798830400L;
    static long RANK_4 = 1095216660480L;
    static long RANK_5 = 4278190080L;
    static long KING_SPAN = 460039L;
    static long KNIGHT_SPAN = 43234889994L;
    static long NOT_MY_PIECES;
    static long ENEMY_PIECES;
    static long OCCUPIED;
    static long EMPTY;
    static long[] RankMasks8 =/*from rank1 to rank8*/
            {
                    0xFFL, 0xFF00L, 0xFF0000L, 0xFF000000L, 0xFF00000000L, 0xFF0000000000L, 0xFF000000000000L, 0xFF00000000000000L
            };
    static long[] FileMasks8 =/*from fileA to FileH*/
            {
                    0x101010101010101L, 0x202020202020202L, 0x404040404040404L, 0x808080808080808L,
                    0x1010101010101010L, 0x2020202020202020L, 0x4040404040404040L, 0x8080808080808080L
            };
    static long[] DiagonalMasks8 =/*from top left to bottom right*/
            {
                    0x1L, 0x102L, 0x10204L, 0x1020408L, 0x102040810L, 0x10204081020L, 0x1020408102040L,
                    0x102040810204080L, 0x204081020408000L, 0x408102040800000L, 0x810204080000000L,
                    0x1020408000000000L, 0x2040800000000000L, 0x4080000000000000L, 0x8000000000000000L
            };
    static long[] AntiDiagonalMasks8 =/*from top right to bottom left*/
            {
                    0x80L, 0x8040L, 0x804020L, 0x80402010L, 0x8040201008L, 0x804020100804L, 0x80402010080402L,
                    0x8040201008040201L, 0x4020100804020100L, 0x2010080402010000L, 0x1008040201000000L,
                    0x804020100000000L, 0x402010000000000L, 0x201000000000000L, 0x100000000000000L
            };

    static long HAndVMoves(int s) {
        //REMINDER: requires OCCUPIED to be up-to-date
        long binaryS = 1L << s;
        long possibilitiesHorizontal = (OCCUPIED - 2 * binaryS) ^ Long.reverse(Long.reverse(OCCUPIED) - 2 * Long.reverse(binaryS));
        long possibilitiesVertical = ((OCCUPIED & FileMasks8[s % 8]) - (2 * binaryS)) ^ Long.reverse(Long.reverse(OCCUPIED & FileMasks8[s % 8]) - (2 * Long.reverse(binaryS)));
        return (possibilitiesHorizontal & RankMasks8[s / 8]) | (possibilitiesVertical & FileMasks8[s % 8]);
    }

    static long DAndAntiDMoves(int s) {
        //REMINDER: requires OCCUPIED to be up-to-date
        long binaryS = 1L << s;
        long possibilitiesDiagonal = ((OCCUPIED & DiagonalMasks8[(s / 8) + (s % 8)]) - (2 * binaryS)) ^ Long.reverse(Long.reverse(OCCUPIED & DiagonalMasks8[(s / 8) + (s % 8)]) - (2 * Long.reverse(binaryS)));
        long possibilitiesAntiDiagonal = ((OCCUPIED & AntiDiagonalMasks8[(s / 8) + 7 - (s % 8)]) - (2 * binaryS)) ^ Long.reverse(Long.reverse(OCCUPIED & AntiDiagonalMasks8[(s / 8) + 7 - (s % 8)]) - (2 * Long.reverse(binaryS)));
        return (possibilitiesDiagonal & DiagonalMasks8[(s / 8) + (s % 8)]) | (possibilitiesAntiDiagonal & AntiDiagonalMasks8[(s / 8) + 7 - (s % 8)]);
    }


    public boolean isMoved = false;
    public boolean isPinned = false;
    public long pinnedMoves = 0L;
    public int[] lastPos;
    protected Stack<Boolean> lastMoveState = new Stack<>();
    protected int row;
    protected int col;
    protected int rawValue;
    protected boolean color;
    protected Type type = Type.PIECE;
    protected Long bitBoard;
    protected Image image;
    protected Image smallImage;

    Piece(int row, int col, boolean color, int value) {
        this.row = row;
        this.col = col;
        this.color = color;
        this.lastPos = new int[]{row, col};
        rawValue = value;
        lastMoveState.push(false);

    }

    public Image getImg() {
        return image;
    }


    public Image getSmallImg() {
        return smallImage;
    }

    protected abstract long getMoves();

    public static void castlingRights() {
        if (GamePanel.board[7][4].isFull() && GamePanel.board[7][4].getPiece().type == Type.KING
                && !GamePanel.board[7][4].getPiece().isMoved) {
            GamePanel.wcks = GamePanel.board[7][7].isFull() && GamePanel.board[7][7].getPiece().type == Type.ROOK
                    && !GamePanel.board[7][7].getPiece().isMoved;

            GamePanel.wcqs = GamePanel.board[7][0].isFull() && GamePanel.board[7][0].getPiece().type == Type.ROOK
                    && !GamePanel.board[7][0].getPiece().isMoved;
        } else {
            GamePanel.wcks = false;
            GamePanel.wcqs = false;
        }
        if (GamePanel.board[0][4].isFull() && GamePanel.board[0][4].getPiece().type == Type.KING
                && !GamePanel.board[0][4].getPiece().isMoved) {
            GamePanel.bcks = GamePanel.board[0][7].isFull() && GamePanel.board[0][7].getPiece().type == Type.ROOK
                    && !GamePanel.board[0][7].getPiece().isMoved;

            GamePanel.bcqs = GamePanel.board[0][0].isFull() && GamePanel.board[0][0].getPiece().type == Type.ROOK
                    && !GamePanel.board[0][0].getPiece().isMoved;
        } else {
            GamePanel.bcks = false;
            GamePanel.bcqs = false;
        }
    }

    protected abstract int evalPiece(boolean raw, boolean endGame);

//    public void undoMove(Spot[][] board, Piece rook, Piece toRevive, Piece queen, int[] lastPos, boolean color) {
//        GamePanel.positions.remove(GamePanel.positions.size() - 1);
//        if (lastPos != null)
//            this.lastPos = lastPos;
//        this.lastMoveState.pop();
//        this.isMoved = lastMoveState.lastElement();
//        board[this.row][this.col].makeEmpty();
//        long table = ChessGame.pieceTables.get("" + color + type);
//        table ^= ((1L << (row * 8 + col)) | (1L << (this.lastPos[0] * 8 + this.lastPos[1])));
//        ChessGame.pieceTables.put("" + color + type, table);
//        this.row = this.lastPos[0];
//        this.col = this.lastPos[1];
//        if (!color) {
//            GamePanel.fullMoves--;
//            GamePanel.halfMoves--;
//        }
//        board[this.lastPos[0]][this.lastPos[1]].assignPiece(this);
//
//        if (rook != null) {
//            if (this.type == Type.KING) {
//                King k = (King) this;
//                k.isCastled = false;
//            }
//            rook.undoMove(board, null, null, null, null, color);
//        }
//        castlingRights();
//        if (toRevive != null) {
//            long t = ChessGame.pieceTables.get("" + toRevive.color + toRevive.type);
//            t ^= (1L << (toRevive.row * 8 + toRevive.col));
//            ChessGame.pieceTables.put("" + toRevive.color + toRevive.type, t);
//            board[toRevive.getRow()][toRevive.getCol()].assignPiece(toRevive);
//            if (toRevive.color) {
//                GamePanel.whitePieces.add(toRevive);
//            } else
//                GamePanel.blackPieces.add(toRevive);
//        }
//
//        if (queen != null) {
//            table = ChessGame.pieceTables.get("" + color + "QUEEN");
//            table ^= (1L << (queen.row * 8 + queen.col));
//            ChessGame.pieceTables.put("" + color + "QUEEN", table);
//            table = ChessGame.pieceTables.get("" + color + type);
//            table ^= (1L << (queen.row * 8 + queen.col));
//            ChessGame.pieceTables.put("" + color + type, table);
//            board[this.row][this.col].assignPiece(this);
//            if (this.color) {
//                GamePanel.whitePieces.remove(queen);
//                GamePanel.whitePieces.add(this);
//            } else {
//                GamePanel.blackPieces.remove(queen);
//                GamePanel.blackPieces.add(this);
//            }
//        }
//    }

    public void move(Spot[][] grid, int row, int col, boolean animate, int promote) {
        grid[this.row][this.col].makeEmpty();
        this.lastPos = new int[]{this.row, this.col};
        String soundName = "chessMoveSound.wav";
        if (this.type == Type.PAWN) {
            Pawn pa = (Pawn) this;
            pa.enPassant = this.row == row + 2 || this.row == row - 2;
            if (grid[row][col].isEmpty() && col != this.col) {
                soundName = "chessCaptureSound.wav";
                if (this.color) {
                    long table = ChessGame.pieceTables.get("false" + type);
                    table ^= (1L << ((row + 1) * 8 + col));
                    ChessGame.pieceTables.put("false" + type, table);
                    Piece p = grid[row + 1][col].getPiece();
                    grid[row + 1][col].makeEmpty();
                    GamePanel.blackPieces.remove(p);
                } else {
                    long table = ChessGame.pieceTables.get("true" + type);
                    table ^= (1L << ((row - 1) * 8 + col));
                    ChessGame.pieceTables.put("true" + type, table);
                    Piece p = grid[row - 1][col].getPiece();
                    grid[row - 1][col].makeEmpty();
                    GamePanel.whitePieces.remove(p);
                }
            }
        }
        if (grid[row][col].isFull()) {
            soundName = "chessCaptureSound.wav";
            Piece p = grid[row][col].getPiece();
            long table = ChessGame.pieceTables.get("" + p.color + p.type);
            table ^= (1L << (p.row * 8 + p.col));
            ChessGame.pieceTables.put("" + p.color + p.type, table);
            if (p.color) {
                GamePanel.whitePieces.remove(p);
            } else {
                GamePanel.blackPieces.remove(p);
            }
        }

        if (this.type == Type.KING) {
            if (col == this.getCol() - 2) {
                King k = (King) this;
                k.isCastled = true;
                soundName = "chessCastleSound.wav";
                Piece r = grid[row][col - 2].getPiece();
                r.move(grid, row, r.getCol() + 3, false, 0);
            } else if (col == this.getCol() + 2) {
                King k = (King) this;
                k.isCastled = true;
                soundName = "chessCastleSound.wav";
                Piece r = grid[row][col + 1].getPiece();
                r.move(grid, row, r.getCol() - 2, false, 0);
            }
        }
        if (animate) {
            GamePanel.clip = sound(soundName);
            GamePanel.toScroll = 2;
        }
        this.lastMoveState.push(true);
        this.isMoved = true;
        long table = ChessGame.pieceTables.get("" + color + type);
        table ^= ((1L << (this.row * 8 + this.col)) | (1L << (row * 8 + col)));
        ChessGame.pieceTables.put("" + color + type, table);
        if (animate) {
            ChessGame.updateBoards(!color);
        }
        if (!color) {
            GamePanel.fullMoves++;
            GamePanel.halfMoves++;
        }
        this.row = row;
        this.col = col;
        grid[row][col].assignPiece(this);
        castlingRights();
        if (!animate)
            GamePanel.positions.add(Zobrist.getZobristHash(!color, GamePanel.wcks, GamePanel.wcqs, GamePanel.bcks, GamePanel.bcqs));
    }

    public Clip sound(String source) {
        Clip clip = null;
        try {
            URL url = this.getClass().getResource(source);
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(url);
            clip = AudioSystem.getClip();
            clip.open(audioStream);
            FloatControl gainControl =
                    (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            gainControl.setValue(-10.0f);
        } catch (Exception e1) {
            System.out.println("sound not found!");
        }
        return clip;
    }


    public List<Integer[]> possibleMoves() {
        List<Integer[]> result = new ArrayList<>();
        long moves = this.getMoves();
        long i = 0L;
        if (this.type == Type.KING) {
            long j = moves & -moves;
            while (j != 0) {
                int index = Long.numberOfTrailingZeros(j);
                result.add(new Integer[]{index / 8, index % 8});
                moves &= ~j;
                j = moves & -moves;
            }
            return result;
        }
        if (ChessGame.numOfAttackers == 2) {
            return result;
        }
        if (isPinned) {
            moves &= pinnedMoves;
        }
        if (ChessGame.numOfAttackers == 1) {
            i |= moves;
            moves &= ChessGame.pushMap;
            moves |= (i & ChessGame.captureMap);

            // check for enPassant capture to get out of check
            if (this.type == Type.PAWN) {
                if (this.color && this.row == 3 || !this.color && this.row == 4) {
                    List<Integer[]> l = new ArrayList<>();
                    long j = i & -i;
                    while (j != 0) {
                        int index = Long.numberOfTrailingZeros(j);
                        l.add(new Integer[]{index / 8, index % 8});
                        i &= ~j;
                        j = i & -i;
                    }

                    long map = ChessGame.captureMap;
                    List<Integer[]> captures = new ArrayList<>();
                    j = map & -map;
                    while (j != 0) {
                        int index = Long.numberOfTrailingZeros(j);
                        captures.add(new Integer[]{index / 8, index % 8});
                        map &= ~j;
                        j = map & -map;
                    }
                    for (Integer[] move : l) {
                        for (Integer[] capture : captures) {
                            if (this.color && this.row == capture[0] && this.col - 1 == capture[1]) {
                                if (move[0] == capture[0] - 1 && move[1] == capture[1] && move[0] == GamePanel.enPassant[0] && move[1] == GamePanel.enPassant[1])
                                    result.add(move);
                            } else if (this.color && this.row == capture[0] && this.col + 1 == capture[1]) {
                                if (move[0] == capture[0] - 1 && move[1] == capture[1] && move[0] == GamePanel.enPassant[0] && move[1] == GamePanel.enPassant[1])
                                    result.add(move);
                            } else if (!this.color && this.row == capture[0] && this.col - 1 == capture[1]) {
                                if (move[0] == capture[0] + 1 && move[1] == capture[1] && move[0] == GamePanel.enPassant[0] && move[1] == GamePanel.enPassant[1])
                                    result.add(move);
                            } else if (!this.color && this.row == capture[0] && this.col + 1 == capture[1]) {
                                if (move[0] == capture[0] + 1 && move[1] == capture[1] && move[0] == GamePanel.enPassant[0] && move[1] == GamePanel.enPassant[1])
                                    result.add(move);
                            }
                        }
                    }
                }
            }
        }
        long j = moves & -moves;
        while (j != 0) {
            int index = Long.numberOfTrailingZeros(j);
            result.add(new Integer[]{index / 8, index % 8});
            moves &= ~j;
            j = moves & -moves;
        }
        return result;
    }

    public static long unsafeForBlack() {
        long WP = ChessGame.pieceTables.get("truePAWN");
        long WN = ChessGame.pieceTables.get("trueKNIGHT");
        long WB = ChessGame.pieceTables.get("trueBISHOP");
        long WR = ChessGame.pieceTables.get("trueROOK");
        long WQ = ChessGame.pieceTables.get("trueQUEEN");
        long WK = ChessGame.pieceTables.get("trueKING");
        long BP = ChessGame.pieceTables.get("falsePAWN");
        long BN = ChessGame.pieceTables.get("falseKNIGHT");
        long BB = ChessGame.pieceTables.get("falseBISHOP");
        long BR = ChessGame.pieceTables.get("falseROOK");
        long BQ = ChessGame.pieceTables.get("falseQUEEN");
        long unsafe;
        long tempOcc = OCCUPIED;
        OCCUPIED = WP | WN | WB | WR | WQ | WK | BP | BN | BB | BR | BQ;
        //pawn
        unsafe = ((WP >>> 7) & ~FILE_A);//pawn capture right
        unsafe |= ((WP >>> 9) & ~FILE_H);//pawn capture left
        long possibility;
        //knight
        long i = WN & -WN;
        while (i != 0) {
            int iLocation = Long.numberOfTrailingZeros(i);
            if (iLocation > 18) {
                possibility = KNIGHT_SPAN << (iLocation - 18);
            } else {
                possibility = KNIGHT_SPAN >> (18 - iLocation);
            }
            if (iLocation % 8 < 4) {
                possibility &= ~FILE_GH;
            } else {
                possibility &= ~FILE_AB;
            }
            unsafe |= possibility;
            WN &= ~i;
            i = WN & -WN;
        }
        //bishop/queen
        long QB = WQ | WB;
        i = QB & -QB;
        while (i != 0) {
            int iLocation = Long.numberOfTrailingZeros(i);
            possibility = DAndAntiDMoves(iLocation);
            unsafe |= possibility;
            QB &= ~i;
            i = QB & -QB;
        }
        //rook/queen
        long QR = WQ | WR;
        i = QR & -QR;
        while (i != 0) {
            int iLocation = Long.numberOfTrailingZeros(i);
            possibility = HAndVMoves(iLocation);
            unsafe |= possibility;
            QR &= ~i;
            i = QR & -QR;
        }
        //king
        int iLocation = Long.numberOfTrailingZeros(WK);
        if (iLocation > 9) {
            possibility = KING_SPAN << (iLocation - 9);
        } else {
            possibility = KING_SPAN >> (9 - iLocation);
        }
        if (iLocation % 8 < 4) {
            possibility &= ~FILE_GH;
        } else {
            possibility &= ~FILE_AB;
        }
        unsafe |= possibility;
        OCCUPIED = tempOcc;
        return unsafe;
    }

    public static long unsafeForWhite() {
        long WP = ChessGame.pieceTables.get("truePAWN");
        long WN = ChessGame.pieceTables.get("trueKNIGHT");
        long WB = ChessGame.pieceTables.get("trueBISHOP");
        long WR = ChessGame.pieceTables.get("trueROOK");
        long WQ = ChessGame.pieceTables.get("trueQUEEN");
        long BP = ChessGame.pieceTables.get("falsePAWN");
        long BN = ChessGame.pieceTables.get("falseKNIGHT");
        long BB = ChessGame.pieceTables.get("falseBISHOP");
        long BR = ChessGame.pieceTables.get("falseROOK");
        long BQ = ChessGame.pieceTables.get("falseQUEEN");
        long BK = ChessGame.pieceTables.get("falseKING");
        long unsafe;
        long tempOcc = OCCUPIED;
        OCCUPIED = WP | WN | WB | WR | WQ | BP | BN | BB | BR | BQ | BK;
        //pawn
        unsafe = ((BP << 7) & ~FILE_H);//pawn capture right
        unsafe |= ((BP << 9) & ~FILE_A);//pawn capture left
        long possibility;
        //knight
        long i = BN & -BN;
        while (i != 0) {
            int iLocation = Long.numberOfTrailingZeros(i);
            if (iLocation > 18) {
                possibility = KNIGHT_SPAN << (iLocation - 18);
            } else {
                possibility = KNIGHT_SPAN >> (18 - iLocation);
            }
            if (iLocation % 8 < 4) {
                possibility &= ~FILE_GH;
            } else {
                possibility &= ~FILE_AB;
            }
            unsafe |= possibility;
            BN &= ~i;
            i = BN & -BN;
        }
        //bishop/queen
        long QB = BQ | BB;
        i = QB & -QB;
        while (i != 0) {
            int iLocation = Long.numberOfTrailingZeros(i);
            possibility = DAndAntiDMoves(iLocation);
            unsafe |= possibility;
            QB &= ~i;
            i = QB & -QB;
        }
        //rook/queen
        long QR = BQ | BR;
        i = QR & -QR;
        while (i != 0) {
            int iLocation = Long.numberOfTrailingZeros(i);
            possibility = HAndVMoves(iLocation);
            unsafe |= possibility;
            QR &= ~i;
            i = QR & -QR;
        }
        //king
        int iLocation = Long.numberOfTrailingZeros(BK);
        if (iLocation > 9) {
            possibility = KING_SPAN << (iLocation - 9);
        } else {
            possibility = KING_SPAN >> (9 - iLocation);
        }
        if (iLocation % 8 < 4) {
            possibility &= ~FILE_GH;
        } else {
            possibility &= ~FILE_AB;
        }
        unsafe |= possibility;
        OCCUPIED = tempOcc;
        return unsafe;
    }

    public int getRow() {
        return this.row;
    }

    public int getCol() {
        return this.col;
    }

    public boolean getColor() {
        return this.color;
    }

    public abstract Piece copy();

    public boolean equals(Piece p) {
        if (p == null) return false;
        if (p == this) return true;
        return p.type == this.type && p.color == this.color && p.row == this.row && p.col == this.col;
    }

    public String toString() {
        return this.type + ", " + this.color;
    }

    @Override
    public int compareTo(Piece p) {
        if (p == null) return -1;
        if (this.equals(p)) return 0;
        return Integer.compare(col, p.col);

    }

}





