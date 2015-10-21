package purus;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import sun.audio.AudioPlayer;
import sun.audio.AudioStream;

public class PlayWav {

	 public static void Play(String soundPath){
		 try {
		 InputStream in = new FileInputStream(soundPath);
		 AudioStream audioStream = new AudioStream(in);
		 AudioPlayer.player.start(audioStream); 
		 } catch (IOException e1) {
		 }
	}
}
