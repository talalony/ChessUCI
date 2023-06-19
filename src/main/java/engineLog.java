import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.PrintStream;

public class engineLog extends JFrame {
    Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
    public engineLog() {
        this.add(new JLabel(" Outout"), BorderLayout.NORTH);
        ImageIcon I = new ImageIcon(ClassLoader.getSystemResource( "images/horse.png" ));
        this.setIconImage(I.getImage());
        JTextArea ta = new JTextArea();
        TextAreaOutputStream taos = new TextAreaOutputStream(ta, 60);
        PrintStream ps = new PrintStream(taos);
        System.setOut(ps);
        System.setErr(ps);

        this.add(new JScrollPane(ta));
        this.pack();
        this.setVisible(false);
        this.setSize(800,600);
        this.setLocation(dim.width/6, dim.height/5);
        this.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
    }
}
