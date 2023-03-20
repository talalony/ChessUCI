import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.imageio.ImageIO;

public class Rook extends Piece {
	
	int[][] wRookFactor = {{0, 0, 0, 0, 0, 0, 0, 0},
            {5, 10, 10, 10, 10, 10, 10, 5},
            {-5, 0, 0, 0, 0, 0, 0, -5},
            {-5, 0, 0, 0, 0, 0, 0, -5},
            {-5, 0, 0, 0, 0, 0, 0, -5},
            {-5, 0, 0, 0, 0, 0, 0, -5},
            {-5, 0, 0, 0, 0, 0, 0, -5},
            {0, 0, 0, 5, 5, 0, 0, 0}};
	
	int [][] bRookFactor = {{0, 0, 0, 5, 5, 0, 0, 0},
			{-5, 0, 0, 0, 0, 0, 0, -5},
			{-5, 0, 0, 0, 0, 0, 0, -5},
			{-5, 0, 0, 0, 0, 0, 0, -5},
			{-5, 0, 0, 0, 0, 0, 0, -5},
			{-5, 0, 0, 0, 0, 0, 0, -5},
			{5, 10, 10, 10, 10, 10, 10, 5},
			{0, 0, 0, 0, 0, 0, 0, 0}};

	Rook(int row, int col, boolean color, int value) throws IOException {
		super(row, col, color, value);
		this.type = Type.ROOK;
		this.bitBoard =(color) ? ChessGame.WR : ChessGame.BR;
		if (color)
			this.image = ImageIO.read(Objects.requireNonNull(this.getClass().getResource("images/white_rook.png")));
		else
			this.image = ImageIO.read(Objects.requireNonNull(this.getClass().getResource("images/black_rook.png")));
		image = image.getScaledInstance(70, 70, Image.SCALE_SMOOTH);
		smallImage = image.getScaledInstance(28, 28, Image.SCALE_SMOOTH);
	}

	@Override
	 public long getMoves() {
		long R = (1L << (row*8+col));
        long i=R&-R;
		int iLocation=Long.numberOfTrailingZeros(i);
		return HAndVMoves(iLocation)&NOT_MY_PIECES;
	    }

	@Override
	protected int evalPiece(boolean raw, boolean endGame) {
		if (!raw) {
			if (color)
				return rawValue + wRookFactor[row][col];
			else
				return rawValue + bRookFactor[row][col];
		}
		else
			return rawValue;
	}

	@Override
	public Piece copy() {
		try {
			return new Rook(this.row, this.col, this.color, this.rawValue);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	
	@Override
	public void move(Spot[][] grid, int row, int col, boolean animate, int promote) {
		super.move(grid, row, col, animate, 0);
	}

}
