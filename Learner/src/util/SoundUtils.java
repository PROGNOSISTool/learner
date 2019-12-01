package util;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

/***
 * Copy pasted from http://stackoverflow.com/questions/3780406/how-to-play-a-sound-alert-in-a-java-application
 * Useful to notify when learning is done :)
 *
 */

public class SoundUtils {

	public static float SAMPLE_RATE = 8000f;

	public static void tone(int hz, int msecs) throws LineUnavailableException {
		tone(hz, msecs, 1.0);
	}

	public static void tone(int hz, int msecs, double vol)
			throws LineUnavailableException {
		byte[] buf = new byte[1];
		AudioFormat af = new AudioFormat(SAMPLE_RATE, // sampleRate
				8, // sampleSizeInBits
				1, // channels
				true, // signed
				false); // bigEndian
		SourceDataLine sdl = AudioSystem.getSourceDataLine(af);
		sdl.open(af);
		sdl.start();
		for (int i = 0; i < msecs * 8; i++) {
			double angle = i / (SAMPLE_RATE / hz) * 2.0 * Math.PI;
			buf[0] = (byte) (Math.sin(angle) * 127.0 * vol);
			sdl.write(buf, 0, 1);
		}
		sdl.drain();
		sdl.stop();
		sdl.close();
	}

	public static void success() {
		try {
			SoundUtils.tone(1000, 200, 100.0);
			Thread.sleep(100);
			SoundUtils.tone(100, 1000);
			Thread.sleep(100);
			SoundUtils.tone(5000, 100);
			Thread.sleep(100);
			SoundUtils.tone(400, 500);
			Thread.sleep(100);
			SoundUtils.tone(400, 500, 0.2);
		} catch (Exception e) {
			
		}
	}
	
	public static void failure(){
		try {
			SoundUtils.tone(450, 100);
			Thread.sleep(100);
			SoundUtils.tone(450, 100);
			Thread.sleep(100);
			SoundUtils.tone(450, 100);
			Thread.sleep(100);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void main(String args []) {
		SoundUtils.success();
	}
}