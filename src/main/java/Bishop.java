import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.imageio.ImageIO;

public class Bishop extends Piece {
	
	static int[][] wBishopFactor = {{-20, -10, -10, -10, -10, -10, -10, -20},
            {-10, 0, 0, 0, 0, 0, 0, -10},
            {-10, 0, 5, 10, 10, 5, 0, -10},
            {-10, 5, 5, 10, 10, 5, 5, -10},
            {-10, 0, 10, 10, 10, 10, 0, -10},
            {-10, 10, 10, 10, 10, 10, 10, -10},
            {-10, 5, 0, 0, 0, 0, 5, -10},
            {-20, -10, -10, -10, -10, -10, -10, -20}};
	
	int[][] bBishopFactor = {{-20, -10, -10, -10, -10, -10, -10, -20},
			{-10, 5, 0, 0, 0, 0, 5, -10},
			{-10, 10, 10, 10, 10, 10, 10, -10},
			{-10, 0, 10, 10, 10, 10, 0, -10},
			{-10, 5, 5, 10, 10, 5, 5, -10},
			{-10, 0, 5, 10, 10, 5, 0, -10},
			{-10, 0, 0, 0, 0, 0, 0, -10},
			{-20, -10, -10, -10, -10, -10, -10, -20, }};

	Bishop(int row, int col, boolean color, int value) throws IOException {
		super(row, col, color, value);
		this.type = Type.BISHOP;
		this.bitBoard =(color) ? ChessGame.WB : ChessGame.BB;
		if (color)
			this.image = ImageIO.read(Objects.requireNonNull(getClass().getResource("images/white_bishop.png")));
		else
			this.image = ImageIO.read(Objects.requireNonNull(getClass().getResource("images/black_bishop.png")));
		image = image.getScaledInstance(70, 70, Image.SCALE_SMOOTH);
		smallImage = image.getScaledInstance(28, 28, Image.SCALE_SMOOTH);
	}

	@Override
	public long getMoves() {
		long B = (1L << (row*8+col));
        long i=B& -B;
		int iLocation=Long.numberOfTrailingZeros(i);
		return DAndAntiDMoves(iLocation)&NOT_MY_PIECES;
    }

	@Override
	protected int evalPiece(boolean raw, boolean endGame) {
		if (!raw) {
			if (color)
				return rawValue + wBishopFactor[row][col];
			else
				return rawValue + bBishopFactor[row][col];
		}
		else
			return rawValue;
	}

	@Override
	public Piece copy() {
		try {
			return new Bishop(this.row, this.col, this.color, this.rawValue);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

}
