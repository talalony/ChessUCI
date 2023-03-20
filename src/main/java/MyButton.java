import javax.swing.*;
import java.awt.*;

class MyButton extends JButton {

    private Color hoverBackgroundColor;
    private Color pressedBackgroundColor;

    private boolean onlyEdge = false;

    private boolean pressed = false;

    public MyButton() {
        this(null);
    }

    public MyButton(boolean onlyEdge) {
        this(null);
        this.onlyEdge = onlyEdge;
    }
    public MyButton(String text) {
        super(text);
        super.setContentAreaFilled(false);
    }
    public MyButton(String text, boolean onlyEdge) {
        super(text);
        super.setContentAreaFilled(false);
        this.onlyEdge = onlyEdge;

    }

    @Override
    protected void paintComponent(Graphics g2) {
        final Graphics2D g = (Graphics2D) g2;
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        if (getModel().isPressed()) {
            pressed = true;
            g.setColor(pressedBackgroundColor);
        }
        else if (getModel().isRollover()) {
            this.setCursor(new Cursor(Cursor.HAND_CURSOR));
            g.setColor(hoverBackgroundColor);
        }
        else {
            g.setColor(getBackground());
        }
        if (!onlyEdge)
            g.fillRect(0, 0, getWidth(), getHeight());
        else {
            int strokeWidth = 2;
            g.setStroke(new BasicStroke(strokeWidth));
            g.drawRect(strokeWidth, strokeWidth, getWidth()-2* strokeWidth, getHeight()-2* strokeWidth);
        }

        super.paintComponent(g);
    }

    @Override
    public void setContentAreaFilled(boolean b) {
    }

    public Color getHoverBackgroundColor() {
        return hoverBackgroundColor;
    }

    public void setHoverBackgroundColor(Color hoverBackgroundColor) {
        this.hoverBackgroundColor = hoverBackgroundColor;
    }

    public Color getPressedBackgroundColor() {
        return pressedBackgroundColor;
    }

    public void setPressedBackgroundColor(Color pressedBackgroundColor) {
        this.pressedBackgroundColor = pressedBackgroundColor;
    }
}