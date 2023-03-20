public class Spot {
	private int row;
	private int col;
	private Piece piece = null;
	
	Spot(int row, int col, Piece p){
		this.row = row;
		this.col = col;
		this.piece = p;
		
	}

	public Spot(Spot s) {
		this.row = s.row;
		this.col = s.col;
		if (s.piece != null)
			this.piece = s.piece.copy();

	}
	
	public void assignPiece(Piece p) {
		this.piece = p;
	}
	

	public boolean isFull() {
		return this.piece != null;
	}
	
	public boolean isEmpty() {
		return this.piece == null;
	}
	
	public Piece getPiece() {
		return this.piece;
	}
	
	public void makeEmpty() {
		this.piece = null;
	}

	public String toString() {
		if (piece != null)
			return ""+piece.getClass();
		else
			return "null";
		
	}
	

}
