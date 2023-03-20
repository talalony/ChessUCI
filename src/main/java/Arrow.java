import java.awt.*;

public class Arrow {
    int x1;
    int x2;
    int y1;
    int y2;

    public Arrow(int x1, int y1, int x2, int y2) {
        this.x1 = x1;
        this.y1 = y1;
        int dx = x2 - x1, dy = y2 - y1;
        double D = Math.sqrt(dx*dx + dy*dy);
        this.x2 = (int) (x2 + (x2 - x1) / D * (-30*1.25));
        this.y2 = (int) (y2 + (y2 - y1) / D * (-30*1.25));
        this.x1 = (int) (x1 + (x2 - x1) / D * (25*1.25));
        this.y1 = (int) (y1 + (y2 - y1) / D * (25*1.25));
    }

    public void drawArrow(Graphics2D g, int width) {
        int arrowHeight = 2*width;
        int arrowBaseWidth = (int)(1.25*width);
        g.setColor(new Color(236, 169, 13, 200));
        g.setStroke(new BasicStroke(width));
        g.drawLine(x1, y1, x2, y2);
        int dx = x2 - x1, dy = y2 - y1;
        double D = Math.sqrt(dx*dx + dy*dy);
        int X2 = (int) (x2 + (x2 - x1) / D * (arrowHeight*1.25-3));
        int Y2 = (int) (y2 + (y2 - y1) / D * (arrowHeight*1.25-3));
        dx = X2 - x1;
        dy = Y2 - y1;
        D = Math.sqrt(dx*dx + dy*dy);
        double xm = D - arrowHeight, xn = xm, ym = arrowBaseWidth, yn = -arrowBaseWidth, x;
        double sin = dy / D, cos = dx / D;

        x = xm*cos - ym*sin + x1;
        ym = xm*sin + ym*cos + y1;
        xm = x;

        x = xn*cos - yn*sin + x1;
        yn = xn*sin + yn*cos + y1;
        xn = x;

        int[] xpoints = {X2, (int) xm, (int) xn};
        int[] ypoints = {Y2, (int) ym, (int) yn};

        g.fillPolygon(xpoints, ypoints, 3);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof Arrow)) {
            return false;
        }
        Arrow a = (Arrow) o;
        return a.x1 == this.x1 && a.y1 == this.y1 && a.x2 == this.x2 && a.y2 == this.y2;
    }
}
