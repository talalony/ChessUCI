import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;

public class GameFrame extends JFrame {
	Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
	boolean fullScreen = false;
	public GameFrame() {
		this.setTitle("Chess");
		this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

		this.addWindowListener( new WindowAdapter()
		{
			public void windowClosing(WindowEvent e)
			{
				if (GamePanel.engine1 != null)
					GamePanel.engine1.close();
				if (GamePanel.engine2 != null)
					GamePanel.engine2.close();
				System.exit(0);
			}
		});
		this.setResizable(false);
//		int f = greatestCommonFactor(dim.width, dim.height);
//		if (dim.width/f == 16 && dim.height/f == 9){
//			this.setExtendedState(JFrame.MAXIMIZED_BOTH);
//			this.setUndecorated(true);
//			this.setAlwaysOnTop(true);
//			fullScreen = true;
//		}
		this.add(new GamePanel(this));
		this.pack();
		this.setVisible(true);
		this.setLocationRelativeTo(null);
	}

	public static int greatestCommonFactor(int width, int height) {
		return (height == 0) ? width : greatestCommonFactor(height, width % height);
	}

}