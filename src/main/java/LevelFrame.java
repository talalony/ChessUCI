import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class LevelFrame extends JFrame {

    Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();

    JCheckBox depth;

    JCheckBox time;
    SpinnerModel model = new SpinnerNumberModel(1,0,100,1);
    JSpinner spinner = new JSpinner(model);

    SpinnerModel modelS = new SpinnerNumberModel(0.9,0.1,1,0.1);
    JSpinner spinnerS = new JSpinner(modelS);

    boolean spinnerOrSpinnerS = true;
    JLabel depthLabel;
    JLabel timeLabel;

    boolean isConfirmed = false;
    MyButton OK;
    MyButton can;

    public LevelFrame() {
        this.setTitle("Adjust Level");
        ImageIcon I = new ImageIcon(ClassLoader.getSystemResource( "images/horse.png" ));
        this.setIconImage(I.getImage());
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

                if (!spinner.isDisplayable()) {
                    this.remove(spinnerS);
                    spinner.setValue(1);
                    this.add(spinner);
                    spinnerOrSpinnerS = true;
                    this.revalidate();
                    this.repaint();
                }
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

        ChangeListener c = e -> {
            if (!time.isSelected()) {
                if ((int)spinner.getValue() < 1)
                    spinner.setValue(1);
            }
            if (e.getSource() == spinner) {
                if ((int)spinner.getValue() < 1) {
                    this.remove(spinner);
                    spinnerS.setValue(0.9);
                    this.add(spinnerS);
                    spinnerOrSpinnerS = false;
                    this.revalidate();
                    this.repaint();
                }
            }
            if (e.getSource() == spinnerS) {
                if ((double)spinnerS.getValue() > 0.9) {
                    this.remove(spinnerS);
                    spinner.setValue(1);
                    this.add(spinner);
                    spinnerOrSpinnerS = true;
                    this.revalidate();
                    this.repaint();
                }
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

        spinnerS.setBounds(260, 35, 60, 25);
        JFormattedTextField txt1 = ((JSpinner.NumberEditor) spinnerS.getEditor()).getTextField();
        ((NumberFormatter) txt1.getFormatter()).setAllowsInvalid(false);

        spinner.addChangeListener(c);
        spinnerS.addChangeListener(c);

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

    public boolean isInt() {
        return spinnerOrSpinnerS;
    }
}
