import javax.swing.*;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class LevelFrame extends JFrame {

    Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();

    JCheckBox depth;

    JCheckBox time;
    SpinnerModel model = new SpinnerNumberModel(1,1,100,1);
    JSpinner spinner = new JSpinner(model);
    JLabel depthLabel;
    JLabel timeLabel;

    boolean isConfirmed = false;
    MyButton OK;
    MyButton can;

    public LevelFrame() {
        this.setTitle("Adjust Level");
        this.setSize(400, 250);
        this.setLocation((dim.width/2)+(dim.width/12), (dim.height/2)-(dim.height/6));
        this.setResizable(false);
        this.setLayout(null);

        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        this.addWindowListener( new WindowAdapter()
        {
            public void windowClosing(WindowEvent e)
            {
                GamePanel.isLevelFrameAlive = false;
                e.getWindow().dispose();
            }
        });

        ActionListener l = e -> {
            if (e.getSource() == depth) {
                spinner.setValue(1);

                time.setSelected(false);
                depthLabel.setVisible(true);

                timeLabel.setVisible(false);
            }
            if (e.getSource() == time) {
                spinner.setValue(1);

                depth.setSelected(false);
                depthLabel.setVisible(false);

                timeLabel.setVisible(true);
            }
        };
        ActionListener l1 = e -> {
            if (e.getSource() == OK) {
                isConfirmed = true;
                GamePanel.isLevelFrameAlive = false;
                this.dispose();
            }
            if (e.getSource() == can) {
                GamePanel.isLevelFrameAlive = false;
                this.dispose();
            }
        };

        depth = new JCheckBox("Fixed Search Depth");
        depth.setBounds(20, 20, 150,25);
        depth.setBorder(BorderFactory.createEmptyBorder());
        depth.setSelected(true);
        depth.addActionListener(l);

        time = new JCheckBox("Time Per Move");
        time.setBounds(20, 55, 150,25);
        time.setBorder(BorderFactory.createEmptyBorder());
        time.addActionListener(l);

        spinner.setBounds(260, 35, 60, 25);
        JFormattedTextField txt = ((JSpinner.NumberEditor) spinner.getEditor()).getTextField();
        ((NumberFormatter) txt.getFormatter()).setAllowsInvalid(false);

        depthLabel = new JLabel("Depth: ");
        depthLabel.setFont(new Font(depthLabel.getFont().getName(), Font.PLAIN, 16));
        depthLabel.setBounds(210, 35, 60, 25);

        timeLabel = new JLabel("Sec: ");
        timeLabel.setFont(new Font(timeLabel.getFont().getName(), Font.PLAIN, 16));
        timeLabel.setBounds(210, 35, 60, 25);
        timeLabel.setVisible(false);

        OK = new MyButton("Confirm");
        OK.setBounds(30, 170, 100, 30);
        OK.setFocusable(false);
        OK.setHoverBackgroundColor(Color.white.darker().darker());
        OK.setPressedBackgroundColor(Color.white.darker());
        OK.setForeground(Color.black);
        OK.setBackground(Color.gray.brighter());
        OK.setBorder(BorderFactory.createEmptyBorder());
        OK.addActionListener(l1);

        can = new MyButton("Cancel");
        can.setBounds(250, 170, 100, 30);
        can.setFocusable(false);
        can.setHoverBackgroundColor(Color.white.darker().darker());
        can.setPressedBackgroundColor(Color.white.darker());
        can.setForeground(Color.black);
        can.setBackground(Color.gray.brighter());
        can.setBorder(BorderFactory.createEmptyBorder());
        can.addActionListener(l1);

        this.add(time);
        this.add(depth);
        this.add(spinner);
        this.add(depthLabel);
        this.add(timeLabel);
        this.add(OK);
        this.add(can);
        this.setVisible(true);
        GamePanel.isLevelFrameAlive = true;
    }

//    public void paint(Graphics g) {
//        super.paint(g);  // fixes the immediate problem.
//        Graphics2D g2 = (Graphics2D) g;
//        Line2D lin = new Line2D.Float(190, 0, 190, 400);
//        g2.draw(lin);
//    }
}
