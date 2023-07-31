import java.io.File;
import java.util.ArrayList;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;

public class AudioManager {
	float globalVolume;
	ArrayList<File> loadedFiles = new ArrayList<File>();
	
	public AudioManager(float globalVolume) {
		this.globalVolume = globalVolume;
	};
	
	public void loadFile(String input) {
		loadedFiles.add(new File("sound/" + input + ".wav"));
	};

	public void playSound(int i, float volume) {
		try {
			AudioInputStream stream = AudioSystem.getAudioInputStream(loadedFiles.get(i));
			Clip clip = AudioSystem.getClip();
			clip.open(stream);
			setVolume(clip, volume);
			clip.start();
			clip.addLineListener(new LineListener() {
			      public void update(LineEvent evt) {
			          if (evt.getType() == LineEvent.Type.STOP) {
			        	  clip.close();
			          }
			      }
		      });
		} catch(Exception e) {
			System.out.println("Could not play sound.");
		};
	};
	
	private void setVolume(Clip clip, float volume) {
	    if (volume < 0f || volume > 1f)
	        throw new IllegalArgumentException("Volume not valid: " + volume);
	    FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);        
	    gainControl.setValue(20f * (float) Math.log10(volume));
	}
}
