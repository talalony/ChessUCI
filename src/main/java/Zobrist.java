import java.security.SecureRandom;

public class Zobrist {
	static long zArray[][][] = new long[2][6][64];
	static long zEnPassant[] = new long[8];
	static long zCastle[] = new long[4];
	static long zBlackMove;
	public static long random() {
		SecureRandom rand = new SecureRandom();
		return rand.nextLong();
	}
	public static void fillArray() {
		for (int color = 0; color < 2; color++) {
			for (int pieceType = 0; pieceType < 6; pieceType++) {
				for (int square = 0; square < 64; square++) {
					zArray[color][pieceType][square]=random();
				}
			}
		}
		for (int col = 0; col < 8; col++) {
			zEnPassant[col] = random();
		}
		for (int i = 0; i < 4; i++) {
			zCastle[i] = random();
		}
		zBlackMove = random();
	}
	
	public static long getZobristHash(boolean color, boolean wcks, boolean wcqs, boolean bcks, boolean bcqs) {
		long returnKey = 0;
		for (Piece p : GamePanel.whitePieces) {
			if (p.type == Type.PAWN)
				returnKey ^= zArray[0][0][p.getRow()*8+p.getCol()];
			if (p.type == Type.KNIGHT)
				returnKey ^= zArray[0][1][p.getRow()*8+p.getCol()];
			if (p.type == Type.BISHOP)
				returnKey ^= zArray[0][2][p.getRow()*8+p.getCol()];
			if (p.type == Type.ROOK)
				returnKey ^= zArray[0][3][p.getRow()*8+p.getCol()];
			if (p.type == Type.QUEEN)
				returnKey ^= zArray[0][4][p.getRow()*8+p.getCol()];
			if (p.type == Type.KING)
				returnKey ^= zArray[0][5][p.getRow()*8+p.getCol()];
		}
		for (Piece p : GamePanel.blackPieces) {
			if (p.type == Type.PAWN)
				returnKey ^= zArray[1][0][p.getRow()*8+p.getCol()];
			if (p.type == Type.KNIGHT)
				returnKey ^= zArray[1][1][p.getRow()*8+p.getCol()];
			if (p.type == Type.BISHOP)
				returnKey ^= zArray[1][2][p.getRow()*8+p.getCol()];
			if (p.type == Type.ROOK)
				returnKey ^= zArray[1][3][p.getRow()*8+p.getCol()];
			if (p.type == Type.QUEEN)
				returnKey ^= zArray[1][4][p.getRow()*8+p.getCol()];
			if (p.type == Type.KING)
				returnKey ^= zArray[1][5][p.getRow()*8+p.getCol()];
		}
		
		for (int col = 0; col < 8; col++) {
			if (col == GamePanel.enPassant[1] && GamePanel.enPassant[0] != 0)
				returnKey ^= zEnPassant[col];
		}
//		String castlingRights = GamePanel.lastMoveFen.split(" ")[3];
		if (wcks)
			returnKey ^= zCastle[0];
		if (wcqs)
			returnKey ^= zCastle[1];
		if (bcks)
			returnKey ^= zCastle[2];
		if (bcqs)
			returnKey ^= zCastle[3];
		if (!color)
			returnKey ^= zBlackMove;

		return returnKey;
	}

}
