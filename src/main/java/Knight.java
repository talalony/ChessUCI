import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.imageio.ImageIO;

public class Knight extends Piece {
	
	int[][] wKnightFactor = {{-50, -40, -30, -30, -30, -30, -40, -50},
				            {-40, -20, 0, 0, 0, 0, -20, -40},
				            {-30, 0, 10, 15, 15, 10, 0, -30},
				            {-30, 5, 15, 20, 20, 15, 5, -30},
				            {-30, 0, 15, 20, 20, 15, 0, -30},
				            {-30, 5, 10, 15, 15, 10, 5, -30},
				            {-40, -20, 0, 5, 5, 0, -20, -40},
				            {-50, -40, -30, -30, -30, -30, -40, -50}};
	
	int[][] bKnightFactor = {{-50, -40, -30, -30, -30, -30, -40, -50, },
							{-40, -20, 0, 5, 5, 0, -20, -40},
							{-30, 5, 10, 15, 15, 10, 5, -30},
							{-30, 0, 15, 20, 20, 15, 0, -30},
							{-30, 5, 15, 20, 20, 15, 5, -30},
							{-30, 0, 10, 15, 15, 10, 0, -30},
							{-40, -20, 0, 0, 0, 0, -20, -40},
							{-50, -40, -30, -30, -30, -30, -40, -50}};

	Knight(int row, int col, boolean color, int value) throws IOException {
		super(row, col, color, value);
		this.type = Type.KNIGHT;
		this.bitBoard =(color) ? ChessGame.WN : ChessGame.BN;
		if (color)
			this.image = ImageIO.read(Objects.requireNonNull(this.getClass().getResource("/images/white_knight.png")));
		else
			this.image = ImageIO.read(Objects.requireNonNull(this.getClass().getResource("images/black_knight.png")));
		image = image.getScaledInstance(70, 70, Image.SCALE_SMOOTH);
		smallImage = image.getScaledInstance(28, 28, Image.SCALE_SMOOTH);
	}

	@Override
	public long getMoves() {
		long N = (1L << (row*8+col));
        long i=N&-N;
        long possibility;
		int iLocation=Long.numberOfTrailingZeros(i);
		if (iLocation>18)
		{
			possibility=KNIGHT_SPAN<<(iLocation-18);
		}
		else {
			possibility=KNIGHT_SPAN>>(18-iLocation);
		}
		if (iLocation%8<4)
		{
			possibility &=~FILE_GH&NOT_MY_PIECES;
		}
		else {
			possibility &=~FILE_AB&NOT_MY_PIECES;
		}
		return possibility;
    }

	@Override
	protected int evalPiece(boolean raw, boolean endGame) {
		if (!raw) {
			if (color)
				return rawValue + wKnightFactor[row][col];
			else
				return rawValue + bKnightFactor[row][col];
		}
		else
			return rawValue;
	}

	@Override
	public Piece copy() {
		try {
			return new Knight(this.row, this.col, this.color, this.rawValue);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

}
