import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import javax.swing.*;
import java.io.*;
import javax.imageio.ImageIO;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;

public class Board extends JPanel implements Runnable, KeyListener, MouseListener, ComponentListener {
	private static final long serialVersionUID = 1L;
	
	// Window variables
	static int WIDTH = 500;
	static int HEIGHT = 400;
	final int SCALE = 2;
	private boolean running = false;
	private boolean resized = false;
	private Graphics2D g;
	private BufferedImage image;
	Thread thread;
	private final int FPS = 60;
	private double averageFPS;
	final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
	int introCounter = FPS*10;
	BufferedImage intro = new BufferedImage(32*4, 32*5/2,BufferedImage.TYPE_INT_ARGB);
	final float globalVolume = 0.5f;

	Data genData = new Data();
	Functions func = new Functions();
	Random rand = new Random();

	// Controls input
	ArrayList<Integer> keysHeld = new ArrayList<Integer>();
	ArrayList<Integer> keysPressed = new ArrayList<Integer>(); 
	ArrayList<Integer> mouseHeld = new ArrayList<Integer>();

	// Image loading
	BufferedImage[] sprPlayer = loadSprite("Player");
	BufferedImage[][] sprKey = new BufferedImage[4][];

	// Background variables
	final float[] layerScales = {1f/4f, 1f/2f, 1f, 2f};
	float[] layerOffset = new float[layerScales.length];
	ArrayList<ArrayList<Float>> layers = new ArrayList<ArrayList<Float>>();
	final float HILLMAX = 0.5f;
	final float HILLMIN = 0.3f;
	final float HILLRANGE = 0.03f;
	final float HILLDEV = 0.02f;
	final int HILLWIDTH = 64;
	Color[] grey = {
		new Color(40, 40, 40),
		new Color(120, 120, 120),
		new Color(200, 200, 200),
		new Color(255, 255, 255)
	};
	
	// Player variables
	boolean facingRight = true;
	int playerCounter = 0;
	final int playerFPS = 15;
	final int playerSize = 32;
	int x = 0;
	int streak = 0;
	int lastStreak = 0;
	
	// Generation variables
	int genID = -1;
	int genX = 0;
	NPCData genObject;
	int genFrame = 0;
	int genCounter = 0;
	BufferedImage[] sprGen;
	final int genSize = 64;
	final int interactRange = 64;
	int interactions = 0;
	String nextDialogue = "";
	int dialogueCounter = 0;
	int dialogueIndex = 0;
	boolean interactable = false;
	int keyCounter = 0;
	int genVoice = 0;
	boolean genFlipped = false;
	boolean[] genInteracted = new boolean[genData.parsedData.size()];
	Set<Integer> genLeft = new HashSet<Integer>();
	
	// Exit NPC
	boolean exitQueued = false;
	int exitCounter = -1;
	BufferedImage[] sprExit = loadSprite("Exit");
	
	// Particles
	ArrayList<int[]> footprints = new ArrayList<int[]>();
	int footprintCDTotal = 15;
	int footprintCD = 0;
	BufferedImage sprFootprint = loadImage("step");
	BufferedImage[] sprShadow = new BufferedImage[2];
	
	// SFX
	AudioManager SFXManager = new AudioManager(globalVolume);
	
	// Font
	@SuppressWarnings("unused")
	final Font font = new Font("Trebuchet MS", Font.PLAIN, (SCALE == 1 ? 16 : 12));
	final FontMetrics fm = getFontMetrics(font);
	
	public Board() {
		setPreferredSize(new Dimension(WIDTH * SCALE, HEIGHT * SCALE));
		setFocusable(true);
		requestFocus();	
		
		// Generates initial terrain, # of layers is determined by the number of coefficients inputed into the layerScales array
		for(int i = 0; i < layerScales.length; i++) {
			ArrayList<Float> layer = new ArrayList<Float>();
			layer.add(nextHeight(i));
			for(int j = 1; j < screenSize.getWidth() / SCALE / HILLWIDTH + 2; j++) {
				layer.add(nextNextHeight(i, layer.get(j - 1)));
			}
			
			layers.add(layer);
			layerOffset[i] = 0;
		}
		
		// Loads sound files
		try {
			Clip clip = AudioSystem.getClip();
			clip.open(AudioSystem.getAudioInputStream(new File("sound/intro.wav")));
			setVolume(clip, globalVolume);
			clip.start();
			clip.addLineListener(new LineListener() {
			      	public void update(LineEvent evt) {
			      		if (evt.getType() == LineEvent.Type.STOP) {
			    			try {
			    				clip.close();
			    				Clip loopClip = AudioSystem.getClip();
			    				loopClip.open(AudioSystem.getAudioInputStream(new File("sound/loop.wav")));
			    				loopClip.loop(Clip.LOOP_CONTINUOUSLY);
			    				setVolume(loopClip, globalVolume);
			    				loopClip.start();
			    			} catch(Exception e) {
			    				System.out.println("Could not load music loop.");
			    			};
			      		}
			      	}
	      		});
		} catch(Exception e) {
			System.out.println("Could not load music intro.");
		};
		SFXManager.loadFile("talk_low");		
		SFXManager.loadFile("talk_mid");
		SFXManager.loadFile("talk_hi");
		
		// Load key sprites
		String[] directions = {"right", "up", "left", "down"};
		for(int i = 0; i < sprKey.length; i++) {
			sprKey[i] = new BufferedImage[2];
			sprKey[i][0] = loadImage(directions[i]);
			sprKey[i][1] = loadImage(directions[i] + "Depress");
		};

		// Load shadow sprites
		sprShadow[0] = loadImage("shadow32");
		sprShadow[1] = loadImage("shadow64");
		
		// Drawing intro
		Graphics2D g = intro.createGraphics();
		g.drawImage(sprKey[0][0], 32*3, 	32*3/4, null);
		g.drawImage(sprKey[1][0], 32*3/2, 	0, null);
		g.drawImage(sprKey[2][0], 0, 		32*3/4, null);
		g.drawImage(sprKey[3][0], 32*3/2, 	32*3/2, null);
		g.dispose();
		
		Arrays.fill(genInteracted, false); // not used... yet
		for(int i = 0; i < genInteracted.length; i++) {
			genLeft.add(i);
		};
	}
	

	public void addNotify() {
		super.addNotify();
		if(thread == null) {
			thread = new Thread(this);
			thread.start();
		};
		addKeyListener(this);
		addMouseListener(this);
		addComponentListener(this);
	};
	

	public void run() {
		running = true;

		image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
		g = (Graphics2D) image.getGraphics();

		long startTime;
		long URDTimeMillis;
		long waitTime;
		long totalTime = 0;
		long takenTime = 0;
		int frameCount = 0;
		long targetTime = 1000 / FPS;

		while(running) {
			startTime = System.nanoTime();

			gameUpdate();
			gameRender();
			gameDraw();

			URDTimeMillis = (System.nanoTime() - startTime) / 1000000;
			waitTime = targetTime - URDTimeMillis;

			try {
				Thread.sleep(waitTime);
			} catch(Exception e) {
				System.out.println("Could not sleep thread.");
			};

			totalTime += System.nanoTime() - startTime;
			takenTime += waitTime;
			frameCount++;
			if(frameCount == FPS + 1) { // After a second's worth of frames, update fps
				averageFPS = 1000f / ((totalTime / frameCount) / 1000000f);
				System.out.println(averageFPS);
				System.out.println(1 - takenTime / 1000f);
				frameCount = 0;
				totalTime = 0;
				takenTime = 0;
			};
		};
	};

	private void gameUpdate() {
		// Player movement
		int movement = 0;
		if(keysHeld.contains(KeyEvent.VK_RIGHT) ^ keysHeld.contains(KeyEvent.VK_LEFT)) {
			// Spawning footprints
			if(footprintCD > 0) {
				--footprintCD;
			} else if(footprintCD == 0) {
				int[] footprint = {0, FPS*10};
				footprints.add(footprint);
				footprintCD = footprintCDTotal;
			};
			
			// Performing movement
			if(keysHeld.contains(KeyEvent.VK_LEFT)) {
				facingRight = false;
				for(int i = 0; i < layerOffset.length; i++) {
					layerOffset[i] += layerScales[i];
				}
				movement = (int)layerScales[layerScales.length - 1];
			} else {
				facingRight = true;
				for(int i = 0; i < layerOffset.length; i++) {
					layerOffset[i] -= layerScales[i];
				}
				movement = (int)-layerScales[layerScales.length - 1];
			};
			playerCounter++;
		} else {
			playerCounter = 0;
		};
		
		// Footprint system
		for(int i = 0; i < footprints.size(); i++) {
			int[] footprint = footprints.get(i);
			if(footprint[1] > 0) {
				footprint[0] += movement;
				footprint[1]--;
			} else {
				footprints.remove(i--);
			}
		};
		
		// Exiting
		if(keysPressed.contains(KeyEvent.VK_ESCAPE) && genID != -2) {
			exitQueued = true;
		};
		
		// Background generation
		if(movement != 0) {
			boolean doGen = false;
			for(int i = 0; i < layerOffset.length; i++) {
				ArrayList<Float> layer = layers.get(i);
				if(layerOffset[i] < 0) { // Moving to the right
					layerOffset[i] += HILLWIDTH;
					layer.remove(0); // Removes the first value and adds one to the end
					layer.add(nextNextHeight(i, layer.get(layer.size() - 1)));
					if(i == layerOffset.length - 1) { // Only applies to the front most layer
						doGen = true;
						if(streak++ < 0) {
							streak = 0;
						};
						x++;
					};
				} else if(layerOffset[i] > HILLWIDTH) { // Moving to the left
					layerOffset[i] -= HILLWIDTH;
					layer.remove(layer.size() - 1); // Removes the last value and inserts one to the beginning
					layer.add(0, nextNextHeight(i, layer.get(0)));
					if(i == layerOffset.length - 1) {
						doGen = true;
						if(streak-- > 0) {
							streak = 0;
						};
						x--;
					};
				};
			};
		
		// Biomes
		
		// Sky
		
		// Generating interactables
			if(doGen) {
				// Generation cleanup
				if(genID != -1 && (genX < -genSize || genX > screenSize.getWidth()/SCALE + genSize)) {
					genID = -1;
				};
				
				// Spawning new NPCs
				if(genID == -1 && !genLeft.isEmpty()) {
					if(exitQueued) {
						genID = -2;
						exitQueued = false;
					} else {
						genID = (int)genLeft.toArray()[rand.nextInt(genLeft.size())];
						if(keysHeld.contains(KeyEvent.VK_Z)) { // DEBUG
							genID = genData.parsedData.size() - 1;
						};
					};
						
					genObject = genData.getNPCData(genID);
					genFlipped = streak < 0 && genObject.flippable;
					genX = (streak < 0 ? -genSize : (int)screenSize.getWidth()/SCALE + genSize);
					
					sprGen = loadSprite(genObject.name);
					genFrame = genCounter = interactions = 0;
					nextDialogue = "";
					genVoice = rand.nextInt(3);
				};
			};
		};
		
		// Exit
		if(exitCounter == 0) {
			System.exit(0);
		} else if(exitCounter > 0) {
			exitCounter--;
		};
		
		// NPC
		if(genID != -1) {
			genX += movement;
			
			// Animation handler
			int comparator = 0;
			if(!genObject.animation.equals("Static")) {
				genCounter++;
				try {
					comparator = genObject.frames[Math.abs(genFrame)];
				} catch(Exception e) {
					comparator = genObject.frames[genObject.frames.length - 1];
				};
				
				if(genCounter > comparator) {
					if(genObject.animation.equals("Boomerang")) {
						if(genFrame >= sprGen.length - 1) {
							genFrame *= -1;
						};
						genFrame++;
					} else { // Loop
						genFrame++;
						genFrame = genFrame % (sprGen.length);
					};
					genCounter = 0;
				};
			};
			
			// Interaction handler
			if(Math.abs(genX - screenSize.getWidth()/SCALE/2) <= interactRange && interactions < genObject.dialogue.length) {
				// Close enough to interact, draws keys onto the screen
				interactable = true;
				keyCounter = (keyCounter + 1) % 120;
				
				if(genID == -2) { // Custom interaction for exit NPC
					if(keysPressed.contains(KeyEvent.VK_UP)) {
						nextDialogue = genObject.dialogue[interactions];
						dialogueCounter = dialogueIndex = 0;
						if(interactions == 2) {
							exitCounter = 300;
						};
						interactions++;
					};
					if(keysPressed.contains(KeyEvent.VK_DOWN) && interactions == 2) {
						nextDialogue = "Very well. There is no shame in clinging to this world."; // let me pretend that this code isnt fucked
						dialogueCounter = dialogueIndex = 0;
						interactions = 0;
					};
				} else { // Interaction for all other NPCs
					if(keysPressed.contains(KeyEvent.VK_UP)) {
						nextDialogue = genObject.dialogue[interactions];
						dialogueCounter = dialogueIndex = 0;
						genLeft.remove(genID);
						genInteracted[genID] = true;
						if(!genObject.name.equals("Spinning Square")) {
							interactions++;
						};
					};
				};
			} else {
				interactable = false;
				keyCounter = 0;
			};
		
			// Text display system
			if(!nextDialogue.isEmpty() && dialogueCounter <= FPS*10) {
				if(dialogueIndex < nextDialogue.length()) {
					comparator = FPS/20;
					if(dialogueIndex > 0) {
						switch(nextDialogue.charAt(dialogueIndex - 1)) {
							case '.':
							case '?':
							case ':':
								comparator = FPS/2;
								break;
							case '!':
							case ';':
							case ',':
								comparator = FPS/3;
								break;	
						};
					};
					if(dialogueCounter > comparator) {
						SFXManager.playSound(genVoice, globalVolume*0.5f);
						dialogueIndex++;
						dialogueCounter = 0;
					};
				}
				dialogueCounter++;
			};
		};
		keysPressed = new ArrayList<Integer>();
	};

	private void gameRender() {
		// Screen size update
		if(resized) {
			Rectangle r = this.getBounds();
			WIDTH = r.width / SCALE + 1;
			HEIGHT = r.height / SCALE + 1;

			image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
			g = image.createGraphics();
			resized = false;
		};
		
		// Draw background
		g.setColor(new Color(0, 0, 0));
		g.fillRect(0, 0, WIDTH, HEIGHT);
		
		for(int i = 0; i < layers.size(); i++) {
			ArrayList<Float> layer = layers.get(i);
			int offset = (int)(layerOffset[i] - (screenSize.getWidth() / SCALE - WIDTH)/2 - HILLWIDTH);
			g.setColor(grey[i]);
			
			for(int j = 0; j < layer.size() - 1; j++) {
				int adjOffset = offset + j*HILLWIDTH;
				if(adjOffset + HILLWIDTH > 0 && adjOffset < WIDTH) {
					float height1 = layer.get(j) * HEIGHT;
					float height2 = layer.get(j + 1) * HEIGHT;
					
					int[] xValues = {adjOffset, adjOffset, adjOffset + HILLWIDTH, adjOffset + HILLWIDTH};
					int[] yValues = {HEIGHT, (int)height1, (int)height2, HEIGHT};
					g.fillPolygon(xValues, yValues, 4);
				};
			}
		}
		
		// Draw objects
			// NPCs
		int index;
		if(genID != -1) {
			int renderX = (int)(genX - (screenSize.getWidth() / SCALE - WIDTH) / 2);
			// Draws the shadow
			g.drawImage(
				sprShadow[0],
				renderX - playerSize/2,
				(int)(HEIGHT * 0.8 - playerSize/2),
				null);
			
			// Draws the NPC
			g.drawImage(
				sprGen[Math.abs(genFrame)],
				renderX - (genFlipped ? -genSize : genSize)/2,
				(int)(HEIGHT * 0.8 - genSize*0.95),
				(genFlipped ? -genSize : genSize),
				genSize,
				null);

			// Draws the arrow key(s)
			if(interactable) {
				if(genID == -2 && interactions == 2) { // At exit NPC when making a decision
					g.setColor(new Color(255, 255, 255));
					int tempX = renderX - playerSize/2 - genSize / 2;
					int tempY = (int)(HEIGHT * 0.8 - 1.75*genSize);
					g.drawImage(
						sprKey[1][keyCounter / FPS],
						tempX,
						tempY,
						null);
					g.drawString("YES", (int)(tempX - fm.stringWidth("YES")/2 + playerSize/2), tempY - 10);
					
					tempX = renderX - playerSize/2 + genSize / 2;
					g.drawImage(
						sprKey[3][keyCounter / FPS],
						tempX,
						tempY,
						null);
					g.drawString("NO", (int)(tempX - fm.stringWidth("NO")/2 + playerSize/2), tempY - 10);
				} else { // Any other instance
					g.drawImage(
						sprKey[1][keyCounter / FPS],
						renderX - playerSize/2,
						(int)(HEIGHT * 0.8 - 1.75*genSize),
						null);
				};
			};
			
			// Draws dialogue
			if(!nextDialogue.isEmpty() && dialogueCounter < FPS*10) {
				g.setFont(font);
				g.setColor(grey[0]);
				String subStr = nextDialogue.substring(0, dialogueIndex);
				float textOpacity = Math.min(1, (FPS*10 - dialogueCounter) / (FPS*5f)); // solid for 5 seconds, then fades away for 5 seconds
				g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, textOpacity));
				g.drawString(subStr, (int)(renderX - fm.stringWidth(subStr)/2), (int)(HEIGHT * 0.8 + playerSize*1.2));
				g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
			};
		}; 
		
			// Player
		index = func.wrap(playerCounter * playerFPS / FPS, 0, sprPlayer.length);
				// Draws the shadow
		g.drawImage(
			sprShadow[0],
			(WIDTH - playerSize)/2,
			(int)(HEIGHT * 0.8),
			null);
				
				// Draws the footprints
		for(int[] footprint : footprints) {
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, footprint[1] / (FPS*10f)));
			g.drawImage(
				sprFootprint,
				WIDTH / 2 + footprint[0] - 2,
				(int)(HEIGHT * 0.8 + playerSize / 2 - 2),
				null);
		}
		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
		
				// Draws the player
		g.drawImage(
			sprPlayer[index],
			(WIDTH - (facingRight ? playerSize : -playerSize)) / 2,
			(int)(HEIGHT * 0.8 - playerSize / 2),
			(facingRight ? playerSize : -playerSize),
			playerSize,
			null);
		
		// Draw UI
		
		// Draw intro
		if(introCounter > 0) {
			float opacity = Math.min(1, introCounter / (FPS*5f));
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
			g.drawImage(
				intro,
				(WIDTH - intro.getWidth()) / 2,
				(int)(HEIGHT * 0.25 - intro.getHeight() / 2),
				null);
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
			introCounter--;
		};
	}

	private void gameDraw() {
		Graphics2D g2 = (Graphics2D) this.getGraphics();
		g2.drawImage(image, 0, 0, WIDTH*SCALE, HEIGHT*SCALE, null);
		g2.dispose();
	};

	public void keyPressed(KeyEvent e) { //stores which keys are being held when pressed
		int key = e.getKeyCode();

		if(!keysHeld.contains(key)) {
			keysHeld.add(key);
		};
		if(!keysPressed.contains(key)) {
			keysPressed.add(key);
		};
	};

	public void keyReleased(KeyEvent e) { //removes which keys are being held when released
		int key = e.getKeyCode();

		keysHeld.remove(Integer.valueOf(key));
	};

	public void mousePressed(MouseEvent e) { //same thing for mouse buttons
		int key = e.getButton();

		if(!mouseHeld.contains(key)) {
			mouseHeld.add(key);
		};
	}	

	public void mouseReleased(MouseEvent e) {
		int key = e.getButton();

		mouseHeld.remove(Integer.valueOf(key));
	}
	
	public void componentResized(ComponentEvent componentEvent) { //updates HEIGHT && width variables when window is resized
		resized = true;
	};
	
	public void componentHidden(ComponentEvent e) {}
	public void componentShown(ComponentEvent e) {}
	public void componentMoved(ComponentEvent e) {}
	public void keyTyped(KeyEvent e) {}
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
	public void mouseClicked(MouseEvent e) {}
	
	private BufferedImage[] loadSprite(String input) {
		ArrayList<BufferedImage> sprite = new ArrayList<BufferedImage>();
		for(int i = 1;; i++) {
			try {
				String index = Integer.toString(i);
				while(index.length() < 4) {
					index = "0" + index;
				};
				sprite.add( ImageIO.read(new File("images/" + input + index + ".png")) );
			} catch (IOException e) {
				break;
			};
		};
		return sprite.toArray(new BufferedImage[0]);
	}
	
	private BufferedImage loadImage(String input) {
		try {
			return ImageIO.read(new File("images/" + input + ".png"));
		} catch (IOException e) {
			System.out.println("Error opening image file: " + e.getMessage());
		};
		return new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
	}; 
	
	private float nextHeight(int i) {
		return 1f - (HILLMAX - (HILLMAX - HILLMIN) * i / (layerOffset.length - 1) + rand.nextFloat(-HILLRANGE, HILLRANGE));
	}
	
	private float nextNextHeight(int i, float adjacent) {
		float a = 1f - (HILLMAX - (HILLMAX - HILLMIN) * i / (layerOffset.length - 1));
		return func.clamp(adjacent + rand.nextFloat(-HILLDEV, HILLDEV), a - HILLRANGE, a + HILLRANGE);
	}
	
	public void setVolume(Clip clip, float volume) {
	    if (volume < 0f || volume > 1f)
	        throw new IllegalArgumentException("Volume not valid: " + volume);
	    FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);        
	    gainControl.setValue(20f * (float) Math.log10(volume));
	}
};