import java.awt.*;
import java.util.*;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;

public class Pawn extends Piece {
	
	int[][] wPawnFactor = {{500, 500, 500, 500, 500, 500, 500, 500},
            {50, 50, 50, 50, 50, 50, 50, 50},
            {10, 10, 20, 30, 30, 20, 10, 10},
            {5, 5, 10, 25, 25, 10, 5, 5},
            {0, 0, 0, 20, 20, 0, 0, 0},
            {5, -5, -10, 0, 0, -10, -5, 5},
            {5, 10, 10, -20, -20, 10, 10, 5},
            {0, 0, 0, 0, 0, 0, 0, 0}};
	
	int[][] wPawnEndFactor = {{500, 500, 500, 500, 500, 500, 500, 500},
            {250, 250, 250, 250, 250, 250, 250, 250},
            {120, 120, 130, 150, 150, 130, 120, 120},
            {100, 100, 110, 120, 120, 110, 100, 100},
            {80, 80, 90, 100, 100, 90, 80, 80},
            {70, 70, 70, 80, 80, 70, 70, 70},
            {-5, -10, -10, -20, -20, -10, -10, -5},
            {0, 0, 0, 0, 0, 0, 0, 0}};
	
	int[][] bPawnFactor = {{0, 0, 0, 0, 0, 0, 0, 0},
			{5, 10, 10, -20, -20, 10, 10, 5},
			{5, -5, -10, 0, 0, -10, -5, 5},
			{0, 0, 0, 20, 20, 0, 0, 0},
			{5, 5, 10, 25, 25, 10, 5, 5},
			{10, 10, 20, 30, 30, 20, 10, 10},
			{50, 50, 50, 50, 50, 50, 50, 50},
			{500, 500, 500, 500, 500, 500, 500, 500}};
	
	int[][] bPawnEndFactor = {{0, 0, 0, 0, 0, 0, 0, 0},
			{-5, -10, -10, -20, -20, -10, -10, -5},
			{70, 70, 70, 80, 80, 70, 70, 70},
			{80, 80, 90, 100, 100, 90, 80, 80},
			{100, 100, 110, 120, 120, 110, 100, 100},
			{120, 120, 130, 150, 150, 130, 120, 120},
			{250, 250, 250, 250, 250, 250, 250, 250},
			{500, 500, 500, 500, 500, 500, 500, 500}};
	
	public boolean enPassant = false;

	Pawn(int row, int col, boolean color, int value) throws IOException {
		super(row, col, color, value);
		this.type = Type.PAWN;
		if (color)
			this.image = ImageIO.read(Objects.requireNonNull(this.getClass().getResource("images/white_pawn.png")));
		else
			this.image = ImageIO.read(Objects.requireNonNull(this.getClass().getResource("images/black_pawn.png")));
		image = image.getScaledInstance(70, 70, Image.SCALE_SMOOTH);
		smallImage = image.getScaledInstance(28, 28, Image.SCALE_SMOOTH);
	}

	@Override
	public long getMoves() {
		long BP = (1L << (row*8+col));
		long WP = (1L << (row*8+col));
		long result = 0L;
        if (color) {
	        //x1,y1,x2,y2
	        long PAWN_MOVES=(WP>>7)&NOT_MY_PIECES&OCCUPIED&~FILE_A;//capture right
	        long possibility=PAWN_MOVES&-PAWN_MOVES;
 			result |= possibility;

	        long enPassantBoard = (1L << (GamePanel.enPassant[0]*8+GamePanel.enPassant[1]));
	        PAWN_MOVES=(WP>>7)&NOT_MY_PIECES&~OCCUPIED&enPassantBoard&~FILE_A&RankMasks8[2];//enPassant capture right
	        possibility=PAWN_MOVES&-PAWN_MOVES;
			result |= possibility;

	        PAWN_MOVES=(WP>>9)&NOT_MY_PIECES&OCCUPIED&~FILE_H;//capture left
	        possibility=PAWN_MOVES&-PAWN_MOVES;
			result |= possibility;

	        enPassantBoard = (1L << (GamePanel.enPassant[0]*8+GamePanel.enPassant[1]));
	        PAWN_MOVES=(WP>>9)&NOT_MY_PIECES&~OCCUPIED&enPassantBoard&~FILE_H&RankMasks8[2];//enPassnat capture left
	        possibility=PAWN_MOVES&-PAWN_MOVES;
			result |= possibility;

	        PAWN_MOVES=(WP>>8)&EMPTY;//move 1 forward
	        possibility=PAWN_MOVES&-PAWN_MOVES;
			result |= possibility;

	        PAWN_MOVES=(WP>>16)&EMPTY&(EMPTY>>8)&RANK_4;//move 2 forward
	        possibility=PAWN_MOVES&-PAWN_MOVES;
			result |= possibility;
        }
        else {
        	//x1,y1,x2,y2
            long PAWN_MOVES=(BP<<7)&NOT_MY_PIECES&OCCUPIED&~FILE_H;//capture right
            long possibility=PAWN_MOVES&-PAWN_MOVES;
			result |= possibility;

            long enPassantBoard = (1L << (GamePanel.enPassant[0]*8+GamePanel.enPassant[1]));
            PAWN_MOVES=(BP<<7)&NOT_MY_PIECES&enPassantBoard&~OCCUPIED&~FILE_H&RankMasks8[5];//enPassant capture right
            possibility=PAWN_MOVES&-PAWN_MOVES;
			result |= possibility;

            PAWN_MOVES=(BP<<9)&NOT_MY_PIECES&OCCUPIED&~FILE_A;//capture left
            possibility=PAWN_MOVES&-PAWN_MOVES;
			result |= possibility;

            enPassantBoard = (1L << (GamePanel.enPassant[0]*8+GamePanel.enPassant[1]));
            PAWN_MOVES=(BP<<9)&NOT_MY_PIECES&enPassantBoard&~OCCUPIED&~FILE_A&RankMasks8[5];//enPasssnt capture left
            possibility=PAWN_MOVES&-PAWN_MOVES;
			result |= possibility;

            PAWN_MOVES=(BP<<8)&EMPTY;//move 1 forward
            possibility=PAWN_MOVES&-PAWN_MOVES;
			result |= possibility;

            PAWN_MOVES=(BP<<16)&EMPTY&(EMPTY<<8)&RANK_5;//move 2 forward
            possibility=PAWN_MOVES&-PAWN_MOVES;
			result |= possibility;

        }
       return result;
    }

	public void promote(Spot[][] grid, int promote) {
		String to = switch (promote) {
			case 1 -> "KNIGHT";
			case 2 -> "BISHOP";
			case 3 -> "ROOK";
			case 4 -> "QUEEN";
			default -> "";
		};
		Piece promoted = null;
		if (this.getRow() == 0 || this.getRow() == 7) {
			try {
				promoted = switch (promote) {
					case 1 -> new Knight(this.getRow(), this.getCol(), color, 320);
					case 2 -> new Bishop(this.getRow(), this.getCol(), color, 330);
					case 3 -> new Rook(this.getRow(), this.getCol(), color, 500);
					case 4 -> new Queen(this.getRow(), this.getCol(), color, 900);
					default -> throw new IllegalStateException("Unexpected value: " + promote);
				};
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (this.getColor()) {
			if (this.getRow() == 0) {
				grid[this.getRow()][this.getCol()].assignPiece(promoted);
				GamePanel.whitePieces.remove(this);
				GamePanel.whitePieces.add(promoted);
				long table = ChessGame.pieceTables.get(""+color+type);
				table ^= (1L << (this.row*8+this.col));
				ChessGame.pieceTables.put(""+color+type, table);
				table = ChessGame.pieceTables.get(""+color+to);
				table ^= (1L << (this.row*8+this.col));
				ChessGame.pieceTables.put(""+color+to, table);
				ChessGame.updateBoards(!color);
			}
		}
		else {
			if (this.getRow() == 7) {
				grid[this.getRow()][this.getCol()].assignPiece(promoted);
				GamePanel.blackPieces.remove(this);
				GamePanel.blackPieces.add(promoted);
				long table = ChessGame.pieceTables.get(""+color+type);
				table ^= (1L << (this.row*8+this.col));
				ChessGame.pieceTables.put(""+color+type, table);
				table = ChessGame.pieceTables.get(""+color+to);
				table ^= (1L << (this.row*8+this.col));
				ChessGame.pieceTables.put(""+color+to, table);
				ChessGame.updateBoards(!color);
			}
		}
	}
	
	public void move(Spot[][] grid, int row, int col, boolean animate, int promote) {
		super.move(grid, row, col, animate, 0);
		if (promote != 0)
			promote(grid, promote);
	}

	@Override
	protected int evalPiece(boolean raw, boolean endGame) {
		if (!raw) {
			if (color) {
				if (!endGame)
					return rawValue + wPawnFactor[row][col];
				else
					return rawValue + wPawnEndFactor[row][col];
			}
			else {
				if (!endGame)
					return rawValue + bPawnFactor[row][col];
				else
					return rawValue + bPawnEndFactor[row][col];
			}
		}
		else
			return rawValue;
	}

	@Override
	public Piece copy() {
		try {
			return new Pawn(this.row, this.col, this.color, this.rawValue);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

}
