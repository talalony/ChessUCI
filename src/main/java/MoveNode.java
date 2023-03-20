import java.util.Arrays;

public class MoveNode {
    int[] from;
    int[] to;
    MoveNode next;

    public MoveNode(int fromX, int fromY, int toX, int toY, MoveNode node) {
        from = new int[] {fromX, fromY};
        to = new int[] {toX, toY};
        next = node;
    }

    @Override
    public String toString() {
        String s = "";
        MoveNode tmp = this;
        while (tmp != null) {
            s += ""+(Arrays.toString(tmp.from))+(Arrays.toString(tmp.to));
            tmp = tmp.next;
        }
        return s;
    }

}
