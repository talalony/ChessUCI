import java.io.*;
import java.util.*;
import java.util.List;

public class ChessGame {
    static Long WP = 0L, WN = 0L, WB = 0L, WR = 0L, WQ = 0L, WK = 0L, BP = 0L, BN = 0L, BB = 0L, BR = 0L, BQ = 0L, BK = 0L;
    static Hashtable<String, Long> pieceTables = new Hashtable<>();
    static int countMoves = 0;
    public static long pushMap = Long.MAX_VALUE;
    public static long captureMap = Long.MAX_VALUE;
    public static int numOfAttackers = 0;
    public static int blackMobility = 0;
    public static int whiteMobility = 0;

    public static boolean isPython = false;


    public static Spot[][] generate_Board(int row) {
        Spot[][] bo = new Spot[row][row];
        for (int i = 0; i < row; i++) {
            for (int j = 0; j < row; j++) {
                bo[i][j] = new Spot(i, j, null);
            }
        }
        return bo;
    }


    public static boolean isStaleMate() {
        if (GamePanel.threefoldRepetition()) return true;
        if (GamePanel.whitePieces.size() == 1 && GamePanel.blackPieces.size() == 1)
            return true;
        if (GamePanel.whitePieces.size() == 1 && GamePanel.blackPieces.size() == 2) {
            if (GamePanel.blackPieces.get(0).type == Type.KNIGHT || GamePanel.blackPieces.get(1).type == Type.KNIGHT)
                return true;
            if (GamePanel.blackPieces.get(0).type == Type.BISHOP || GamePanel.blackPieces.get(1).type == Type.BISHOP)
                return true;
        }
        if (GamePanel.blackPieces.size() == 1 && GamePanel.whitePieces.size() == 2) {
            if (GamePanel.whitePieces.get(0).type == Type.KNIGHT || GamePanel.whitePieces.get(1).type == Type.KNIGHT)
                return true;
            if (GamePanel.whitePieces.get(0).type == Type.BISHOP || GamePanel.whitePieces.get(1).type == Type.BISHOP)
                return true;
        }
        if (GamePanel.blackPieces.size() == 2 && GamePanel.whitePieces.size() == 2) {
            if (GamePanel.whitePieces.get(0).type == Type.KNIGHT || GamePanel.whitePieces.get(0).type == Type.BISHOP ||
                    GamePanel.whitePieces.get(1).type == Type.KNIGHT || GamePanel.whitePieces.get(1).type == Type.BISHOP) {

                return GamePanel.blackPieces.get(0).type == Type.KNIGHT || GamePanel.blackPieces.get(0).type == Type.BISHOP ||
                        GamePanel.blackPieces.get(1).type == Type.KNIGHT || GamePanel.blackPieces.get(1).type == Type.BISHOP;
            }
        }
        return false;
    }

    public static List<Integer[]> getAllMoves(boolean color) {
        List<Integer[]> allMoves = new ArrayList<>();
        updateBoards(color);
        GamePanel.tmpIsInCheck = GamePanel.isInCheck;
        GamePanel.isInCheck = isInCheck(color);
        updateThreats(!color);
        if (color) {
            for (int i = 0; i < GamePanel.whitePieces.size(); i++) {
                Piece p = GamePanel.whitePieces.get(i);
                for (Integer[] move : p.possibleMoves()) {
                    Integer[] m = new Integer[4];
                    m[0] = p.getRow();
                    m[1] = p.getCol();
                    m[2] = move[0];
                    m[3] = move[1];
                    allMoves.add(m);
                }
            }
        } else {
            for (int i = 0; i < GamePanel.blackPieces.size(); i++) {
                Piece p = GamePanel.blackPieces.get(i);
                for (Integer[] move : p.possibleMoves()) {
                    Integer[] m = new Integer[4];
                    m[0] = p.getRow();
                    m[1] = p.getCol();
                    m[2] = move[0];
                    m[3] = move[1];
                    allMoves.add(m);
                }
            }
        }
        return allMoves;
    }

    public static void unPinAll() {
        for (Piece p : GamePanel.whitePieces) {
            p.isPinned = false;
            p.pinnedMoves = 0L;
        }
        for (Piece p : GamePanel.blackPieces) {
            p.isPinned = false;
            p.pinnedMoves = 0L;
        }

    }

    public static boolean isInCheck(boolean color) {
        unPinAll();
        pushMap = 0L;
        captureMap = 0L;
        King k = null;
        if (color) {
            for (Piece p : GamePanel.whitePieces) {
                if (p.type == Type.KING) {
                    k = (King) p;
                    break;
                }
            }
        } else {
            for (Piece p : GamePanel.blackPieces) {
                if (p.type == Type.KING) {
                    k = (King) p;
                    break;
                }
            }
        }
        List<Piece> attackers = new ArrayList<>();
        long tmpList = 0L;
        Spot[][] board = GamePanel.board;
        Piece pinned = null;
        boolean attFound = false;
        int counter = 0;
        long remove = 0L;
        // slider moves
        //Check right
        assert k != null;
        for (int i = k.getCol() + 1; i < 8; i++) {
            if (board[k.getRow()][i].isFull() && board[k.getRow()][i].getPiece().getColor() == k.getColor()) {
                if (counter == 0) {
                    pinned = board[k.getRow()][i].getPiece();
                    counter = 1;
                } else {
                    break;
                }
            } else if (board[k.getRow()][i].isEmpty()) {
                long add = (1L << (k.getRow() * 8 + i));
                remove |= add;
                pushMap |= add;
                tmpList |= add;
            } else if (board[k.getRow()][i].getPiece().getColor() != k.getColor()) {
                if ((board[k.getRow()][i].getPiece().type == Type.ROOK || board[k.getRow()][i].getPiece().type == Type.QUEEN)) {
                    long add = (1L << (k.getRow() * 8 + i));
                    if (pinned != null) {
                        pinned.isPinned = true;
                        tmpList |= add;
                        pinned.pinnedMoves = tmpList;
                    } else {
                        attFound = true;
                        attackers.add(board[k.getRow()][i].getPiece());
                        captureMap |= add;
                    }
                }
                break;
            }
        }
        if (!attFound)
            pushMap ^= remove;
        //Check left
        counter = 0;
        tmpList = 0L;
        remove = 0L;
        pinned = null;
        attFound = false;
        for (int i = k.getCol() - 1; i >= 0; i--) {
            if (board[k.getRow()][i].isFull() && board[k.getRow()][i].getPiece().getColor() == k.getColor()) {
                if (counter == 0) {
                    pinned = board[k.getRow()][i].getPiece();
                    counter = 1;
                } else {
                    break;
                }
            } else if (board[k.getRow()][i].getPiece() == null) {
                long add = (1L << (k.getRow() * 8 + i));
                remove |= add;
                pushMap |= add;
                tmpList |= add;
            } else if (board[k.getRow()][i].getPiece().getColor() != k.getColor()) {
                long add = (1L << (k.getRow() * 8 + i));
                if ((board[k.getRow()][i].getPiece().type == Type.ROOK || board[k.getRow()][i].getPiece().type == Type.QUEEN)) {
                    if (pinned != null) {
                        pinned.isPinned = true;
                        tmpList |= add;
                        pinned.pinnedMoves = tmpList;
                    } else {
                        attFound = true;
                        attackers.add(board[k.getRow()][i].getPiece());
                        captureMap |= add;
                    }
                }
                break;
            }
        }
        if (!attFound)
            pushMap ^= remove;

        if (attackers.size() >= 2) {
            ChessGame.numOfAttackers = 2;
            return true;
        }

        //Check down
        counter = 0;
        tmpList = 0L;
        remove = 0L;
        pinned = null;
        attFound = false;
        for (int i = k.getRow() + 1; i < 8; i++) {
            if (board[i][k.getCol()].isFull() && board[i][k.getCol()].getPiece().getColor() == k.getColor()) {
                if (counter == 0) {
                    pinned = board[i][k.getCol()].getPiece();
                    counter = 1;
                } else {
                    break;
                }
            } else if (board[i][k.getCol()].getPiece() == null) {
                long add = (1L << (i * 8 + k.getCol()));
                remove |= add;
                pushMap |= add;
                tmpList |= add;
            } else if (board[i][k.getCol()].getPiece().getColor() != k.getColor()) {
                if ((board[i][k.getCol()].getPiece().type == Type.ROOK || board[i][k.getCol()].getPiece().type == Type.QUEEN)) {
                    long add = (1L << (i * 8 + k.getCol()));
                    if (pinned != null) {
                        pinned.isPinned = true;
                        tmpList |= add;
                        pinned.pinnedMoves = tmpList;
                    } else {
                        attFound = true;
                        attackers.add(board[i][k.getCol()].getPiece());
                        captureMap |= add;
                    }
                }
                break;
            }
        }
        if (!attFound)
            pushMap ^= remove;

        if (attackers.size() >= 2) {
            ChessGame.numOfAttackers = 2;
            return true;
        }

        //Check up
        counter = 0;
        tmpList = 0L;
        remove = 0L;
        pinned = null;
        attFound = false;
        for (int i = k.getRow() - 1; i >= 0; i--) {
            if (board[i][k.getCol()].isFull() && board[i][k.getCol()].getPiece().getColor() == k.getColor()) {
                if (counter == 0) {
                    pinned = board[i][k.getCol()].getPiece();
                    counter = 1;
                } else {
                    break;
                }
            } else if (board[i][k.getCol()].getPiece() == null) {
                long add = (1L << (i * 8 + k.getCol()));
                remove |= add;
                pushMap |= add;
                tmpList |= add;
            } else if (board[i][k.getCol()].getPiece().getColor() != k.getColor()) {
                if ((board[i][k.getCol()].getPiece().type == Type.ROOK || board[i][k.getCol()].getPiece().type == Type.QUEEN)) {
                    long add = (1L << (i * 8 + k.getCol()));
                    if (pinned != null) {
                        pinned.isPinned = true;
                        tmpList |= add;
                        pinned.pinnedMoves = tmpList;
                    } else {
                        attFound = true;
                        attackers.add(board[i][k.getCol()].getPiece());
                        captureMap |= add;
                    }
                }
                break;
            }
        }
        if (!attFound)
            pushMap ^= remove;
        if (attackers.size() >= 2) {
            ChessGame.numOfAttackers = 2;
            return true;
        }
        //down right
        counter = 0;
        tmpList = 0L;
        remove = 0L;
        pinned = null;
        attFound = false;
        for (int i = k.getRow() + 1, j = k.getCol() + 1; i < 8 && j < 8; i++, j++) {
            if (board[i][j].isFull() && board[i][j].getPiece().getColor() == k.getColor()) {
                if (counter == 0) {
                    pinned = board[i][j].getPiece();
                    counter = 1;
                } else {
                    break;
                }
            } else if (board[i][j].getPiece() == null) {
                long add = (1L << (i * 8 + j));
                remove |= add;
                pushMap |= add;
                tmpList |= add;
            } else if (board[i][j].getPiece().getColor() != k.getColor()) {
                if ((board[i][j].getPiece().type == Type.BISHOP || board[i][j].getPiece().type == Type.QUEEN)) {
                    long add = (1L << (i * 8 + j));
                    if (pinned != null) {
                        pinned.isPinned = true;
                        tmpList |= add;
                        pinned.pinnedMoves = tmpList;
                    } else {
                        attFound = true;
                        attackers.add(board[i][j].getPiece());
                        captureMap |= add;
                    }
                }
                break;
            }
        }
        if (!attFound)
            pushMap ^= remove;
        if (attackers.size() >= 2) {
            ChessGame.numOfAttackers = 2;
            return true;
        }

        //up left
        counter = 0;
        tmpList = 0L;
        remove = 0L;
        pinned = null;
        attFound = false;
        for (int i = k.getRow() - 1, j = k.getCol() - 1; i >= 0 && j >= 0; i--, j--) {
            if (board[i][j].isFull() && board[i][j].getPiece().getColor() == k.getColor()) {
                if (counter == 0) {
                    pinned = board[i][j].getPiece();
                    counter = 1;
                } else {
                    break;
                }
            } else if (board[i][j].getPiece() == null) {
                long add = (1L << (i * 8 + j));
                remove |= add;
                pushMap |= add;
                tmpList |= add;
            } else if (board[i][j].getPiece().getColor() != k.getColor()) {
                if ((board[i][j].getPiece().type == Type.BISHOP || board[i][j].getPiece().type == Type.QUEEN)) {
                    long add = (1L << (i * 8 + j));
                    if (pinned != null) {
                        pinned.isPinned = true;
                        tmpList |= add;
                        pinned.pinnedMoves = tmpList;
                    } else {
                        attFound = true;
                        attackers.add(board[i][j].getPiece());
                        captureMap |= add;
                    }
                }
                break;
            }
        }
        if (!attFound)
            pushMap ^= remove;
        if (attackers.size() >= 2) {
            ChessGame.numOfAttackers = 2;
            return true;
        }

        //up right
        counter = 0;
        tmpList = 0L;
        remove = 0L;
        pinned = null;
        attFound = false;
        for (int i = k.getRow() - 1, j = k.getCol() + 1; i >= 0 && j < 8; i--, j++) {
            if (board[i][j].isFull() && board[i][j].getPiece().getColor() == k.getColor()) {
                if (counter == 0) {
                    pinned = board[i][j].getPiece();
                    counter = 1;
                } else {
                    break;
                }
            } else if (board[i][j].getPiece() == null) {
                long add = (1L << (i * 8 + j));
                remove |= add;
                pushMap |= add;
                tmpList |= add;
            } else if (board[i][j].getPiece().getColor() != k.getColor()) {
                if ((board[i][j].getPiece().type == Type.BISHOP || board[i][j].getPiece().type == Type.QUEEN)) {
                    long add = (1L << (i * 8 + j));
                    if (pinned != null) {
                        pinned.isPinned = true;
                        tmpList |= add;
                        pinned.pinnedMoves = tmpList;
                    } else {
                        attFound = true;
                        attackers.add(board[i][j].getPiece());
                        captureMap |= add;
                    }
                }
                break;
            }
        }
        if (!attFound)
            pushMap ^= remove;
        if (attackers.size() >= 2) {
            ChessGame.numOfAttackers = 2;
            return true;
        }

        //down left
        counter = 0;
        tmpList = 0L;
        remove = 0L;
        pinned = null;
        attFound = false;
        for (int i = k.getRow() + 1, j = k.getCol() - 1; i < 8 && j >= 0; i++, j--) {
            if (board[i][j].isFull() && board[i][j].getPiece().getColor() == k.getColor()) {
                if (counter == 0) {
                    pinned = board[i][j].getPiece();
                    counter = 1;
                } else {
                    break;
                }
            } else if (board[i][j].getPiece() == null) {
                long add = (1L << (i * 8 + j));
                remove |= add;
                pushMap |= add;
                tmpList |= add;
            } else if (board[i][j].getPiece().getColor() != k.getColor()) {
                if ((board[i][j].getPiece().type == Type.BISHOP || board[i][j].getPiece().type == Type.QUEEN)) {
                    long add = (1L << (i * 8 + j));
                    if (pinned != null) {
                        pinned.isPinned = true;
                        tmpList |= add;
                        pinned.pinnedMoves = tmpList;
                    } else {
                        attFound = true;
                        attackers.add(board[i][j].getPiece());
                        captureMap |= add;
                    }
                }
                break;
            }
        }
        if (!attFound)
            pushMap ^= remove;
        if (attackers.size() >= 2) {
            ChessGame.numOfAttackers = 2;
            return true;
        }
        // knight moves
        // Check up
        if (k.getRow() - 2 >= 0) {
            if (k.getCol() - 1 >= 0)
                if (board[k.getRow() - 2][k.getCol() - 1].getPiece() != null && board[k.getRow() - 2][k.getCol() - 1].getPiece().color != k.getColor() && board[k.getRow() - 2][k.getCol() - 1].getPiece().type == Type.KNIGHT) {
                    attackers.add(board[k.getRow() - 2][k.getCol() - 1].getPiece());
                    long add = (1L << ((k.getRow() - 2) * 8 + k.getCol() - 1));
                    captureMap |= add;
                }
            if (k.getCol() + 1 < 8)
                if (board[k.getRow() - 2][k.getCol() + 1].getPiece() != null && board[k.getRow() - 2][k.getCol() + 1].getPiece().color != k.getColor() && board[k.getRow() - 2][k.getCol() + 1].getPiece().type == Type.KNIGHT) {
                    attackers.add(board[k.getRow() - 2][k.getCol() + 1].getPiece());
                    long add = (1L << ((k.getRow() - 2) * 8 + k.getCol() + 1));
                    captureMap |= add;
                }
        }
        if (attackers.size() >= 2) {
            ChessGame.numOfAttackers = 2;
            return true;
        }

        //Check down
        if (k.getRow() + 2 < 8) {
            if (k.getCol() - 1 >= 0)
                if (board[k.getRow() + 2][k.getCol() - 1].getPiece() != null && board[k.getRow() + 2][k.getCol() - 1].getPiece().color != k.getColor() && board[k.getRow() + 2][k.getCol() - 1].getPiece().type == Type.KNIGHT) {
                    attackers.add(board[k.getRow() + 2][k.getCol() - 1].getPiece());
                    long add = (1L << ((k.getRow() + 2) * 8 + k.getCol() - 1));
                    captureMap |= add;
                }
            if (k.getCol() + 1 < 8)
                if (board[k.getRow() + 2][k.getCol() + 1].getPiece() != null && board[k.getRow() + 2][k.getCol() + 1].getPiece().color != k.getColor() && board[k.getRow() + 2][k.getCol() + 1].getPiece().type == Type.KNIGHT) {
                    attackers.add(board[k.getRow() + 2][k.getCol() + 1].getPiece());
                    long add = (1L << ((k.getRow() + 2) * 8 + k.getCol() + 1));
                    captureMap |= add;
                }
        }
        if (attackers.size() >= 2) {
            ChessGame.numOfAttackers = 2;
            return true;
        }

        //Check right
        if (k.getCol() + 2 < 8) {
            if (k.getRow() - 1 >= 0)
                if (board[k.getRow() - 1][k.getCol() + 2].getPiece() != null && board[k.getRow() - 1][k.getCol() + 2].getPiece().color != k.getColor() && board[k.getRow() - 1][k.getCol() + 2].getPiece().type == Type.KNIGHT) {
                    attackers.add(board[k.getRow() - 1][k.getCol() + 2].getPiece());
                    long add = (1L << ((k.getRow() - 1) * 8 + k.getCol() + 2));
                    captureMap |= add;
                }
            if (k.getRow() + 1 < 8)
                if (board[k.getRow() + 1][k.getCol() + 2].getPiece() != null && board[k.getRow() + 1][k.getCol() + 2].getPiece().color != k.getColor() && board[k.getRow() + 1][k.getCol() + 2].getPiece().type == Type.KNIGHT) {
                    attackers.add(board[k.getRow() + 1][k.getCol() + 2].getPiece());
                    long add = (1L << ((k.getRow() + 1) * 8 + k.getCol() + 2));
                    captureMap |= add;
                }
        }
        if (attackers.size() >= 2) {
            ChessGame.numOfAttackers = 2;
            return true;
        }

        //Check left
        if (k.getCol() - 2 >= 0) {
            if (k.getRow() - 1 >= 0)
                if (board[k.getRow() - 1][k.getCol() - 2].getPiece() != null && board[k.getRow() - 1][k.getCol() - 2].getPiece().color != k.getColor() && board[k.getRow() - 1][k.getCol() - 2].getPiece().type == Type.KNIGHT) {
                    attackers.add(board[k.getRow() - 1][k.getCol() - 2].getPiece());
                    long add = (1L << ((k.getRow() - 1) * 8 + k.getCol() - 2));
                    captureMap |= add;
                }
            if (k.getRow() + 1 < 8)
                if (board[k.getRow() + 1][k.getCol() - 2].getPiece() != null && board[k.getRow() + 1][k.getCol() - 2].getPiece().color != k.getColor() && board[k.getRow() + 1][k.getCol() - 2].getPiece().type == Type.KNIGHT) {
                    attackers.add(board[k.getRow() + 1][k.getCol() - 2].getPiece());
                    long add = (1L << ((k.getRow() + 1) * 8 + k.getCol() - 2));
                    captureMap |= add;
                }
        }
        if (attackers.size() >= 2) {
            ChessGame.numOfAttackers = 2;
            return true;
        }
        // pawn moves
        if (k.getColor()) {
            if (k.getRow() - 1 >= 0 && k.getCol() - 1 >= 0)
                if (board[k.getRow() - 1][k.getCol() - 1].isFull() && board[k.getRow() - 1][k.getCol() - 1].getPiece().getColor() != k.getColor() && board[k.getRow() - 1][k.getCol() - 1].getPiece().type == Type.PAWN) {
                    attackers.add(board[k.getRow() - 1][k.getCol() - 1].getPiece());
                    long add = (1L << ((k.getRow() - 1) * 8 + k.getCol() - 1));
                    captureMap |= add;
                }
            if (k.getRow() - 1 >= 0 && k.getCol() + 1 < 8)
                if (board[k.getRow() - 1][k.getCol() + 1].isFull() && board[k.getRow() - 1][k.getCol() + 1].getPiece().getColor() != k.getColor() && board[k.getRow() - 1][k.getCol() + 1].getPiece().type == Type.PAWN) {
                    attackers.add(board[k.getRow() - 1][k.getCol() + 1].getPiece());
                    long add = (1L << ((k.getRow() - 1) * 8 + k.getCol() + 1));
                    captureMap |= add;
                }
        } else {
            if (k.getRow() + 1 < 8 && k.getCol() - 1 >= 0)
                if (board[k.getRow() + 1][k.getCol() - 1].isFull() && board[k.getRow() + 1][k.getCol() - 1].getPiece().getColor() != k.getColor() && board[k.getRow() + 1][k.getCol() - 1].getPiece().type == Type.PAWN) {
                    attackers.add(board[k.getRow() + 1][k.getCol() - 1].getPiece());
                    long add = (1L << ((k.getRow() + 1) * 8 + k.getCol() - 1));
                    captureMap |= add;
                }
            if (k.getRow() + 1 < 8 && k.getCol() + 1 < 8)
                if (board[k.getRow() + 1][k.getCol() + 1].isFull() && board[k.getRow() + 1][k.getCol() + 1].getPiece().getColor() != k.getColor() && board[k.getRow() + 1][k.getCol() + 1].getPiece().type == Type.PAWN) {
                    attackers.add(board[k.getRow() + 1][k.getCol() + 1].getPiece());
                    long add = (1L << ((k.getRow() + 1) * 8 + k.getCol() + 1));
                    captureMap |= add;
                }
        }
        if (attackers.size() >= 2) {
            ChessGame.numOfAttackers = 2;
            return true;
        }
        ChessGame.numOfAttackers = attackers.size();
        return ChessGame.numOfAttackers > 0;
    }

    private static int countOnes(Long l) {
        String map = Long.toBinaryString(l);
        int result = 0;
        for (int i = 0; i < map.length(); i++) {
            if (map.charAt(i) == '1')
                result++;
        }
        return result;
    }

    public static void updateThreats(boolean color) {
        if (color) {
            GamePanel.threatMap = Piece.unsafeForBlack();
            whiteMobility = countOnes(GamePanel.threatMap);
        } else {
            GamePanel.threatMap = Piece.unsafeForWhite();
            blackMobility = countOnes(GamePanel.threatMap);
        }
    }

    static void updateBoards(Boolean color) {
        long WP = pieceTables.get("truePAWN");
        long WN = pieceTables.get("trueKNIGHT");
        long WB = pieceTables.get("trueBISHOP");
        long WR = pieceTables.get("trueROOK");
        long WQ = pieceTables.get("trueQUEEN");
        long WK = pieceTables.get("trueKING");
        long BP = pieceTables.get("falsePAWN");
        long BN = pieceTables.get("falseKNIGHT");
        long BB = pieceTables.get("falseBISHOP");
        long BR = pieceTables.get("falseROOK");
        long BQ = pieceTables.get("falseQUEEN");
        long BK = pieceTables.get("falseKING");
        if (color) {
            Piece.NOT_MY_PIECES = ~(WP | WN | WB | WR | WQ | WK | BK);
            Piece.ENEMY_PIECES = BP | BN | BB | BR | BQ;
        } else {
            Piece.NOT_MY_PIECES = ~(BP | BN | BB | BR | BQ | BK | WK);
            Piece.ENEMY_PIECES = WP | WN | WB | WR | WQ;
        }
        Piece.OCCUPIED = WP | WN | WB | WR | WQ | WK | BP | BN | BB | BR | BQ | BK;
        Piece.EMPTY = ~Piece.OCCUPIED;
    }

    public static void changeDPI() {
        String p = System.getProperty("user.dir");
        String[] c = {"REG", "ADD", "\"HKCU\\Software\\Microsoft\\Windows NT\\CurrentVersion\\AppCompatFlags\\Layers\"", "/V", "\"" + p + "\\Chess.exe\"", "/T", "REG_SZ", "/D", "\"~GDIDPISCALING DPIUNAWARE\"", "/F"};

        try {
            Runtime.getRuntime().exec(c);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean isPythonInstalled() {
        try {
            String[] s = {"python", "--version"};
            Process p = Runtime.getRuntime().exec(s);
            BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = in.readLine();
            return line != null && line.startsWith("Python");
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void main(String[] args) {
        isPython = isPythonInstalled();
        changeDPI();
        new GameFrame();
    }
}
