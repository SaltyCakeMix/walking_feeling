/*
 * Started on 5-6-2022
 * Finished on
*/
import javax.swing.*;

public class Start {

	public static void main(String[] args) {
		JFrame window = new JFrame("Walking Feeling");
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		// create the jpanel to draw on.
		// this also initializes the game loop
		Board board = new Board();
		// add the jpanel to the windows
		window.setContentPane(board);

		window.setResizable(true);
		window.pack();
		window.setLocationRelativeTo(null);
		window.setVisible(true);               
	}
}