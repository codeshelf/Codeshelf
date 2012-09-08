/*******************************************************************************
 *  CodeShelf
 *  Copyright (c) 2005-2012, Jeffrey B. Williams, All rights reserved
 *  $Id: Sound.java,v 1.3 2012/09/08 03:03:24 jeffw Exp $
 *******************************************************************************/
package com.gadgetworks.codeshelf.application;

import java.io.IOException;
import java.io.InputStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

// --------------------------------------------------------------------------
/**
 *  @author jeffw
 */
public final class Sound {

	private static final Log	LOGGER							= LogFactory.getLog(Sound.class);

	private static final int	DEFAULT_EXTERNAL_BUFFER_SIZE	= 128000;
	private static final int	STD_SAMPLE_SIZE					= 16;
	private static final int	HALF_SAMPLE_SIZE				= 8;

	private Sound() {

	}

	public static void playSound() {

		boolean bForceConversion = false;
		boolean bBigEndian = false;
		int nSampleSizeInBits = STD_SAMPLE_SIZE;
		String strMixerName = null;
		int nExternalBufferSize = DEFAULT_EXTERNAL_BUFFER_SIZE;
		int nInternalBufferSize = AudioSystem.NOT_SPECIFIED;

		//File file = new File("resources/presence_changed.vaw");
		InputStream resourceStream = Sound.class.getClassLoader().getResourceAsStream("sounds/presence_changed.wav");

		try {
			AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(resourceStream);
			AudioFormat audioFormat = audioInputStream.getFormat();
			DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat, nInternalBufferSize);
			boolean bIsSupportedDirectly = AudioSystem.isLineSupported(info);
			if (!bIsSupportedDirectly || bForceConversion) {
				AudioFormat sourceFormat = audioFormat;
				AudioFormat targetFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
					sourceFormat.getSampleRate(),
					nSampleSizeInBits,
					sourceFormat.getChannels(),
					sourceFormat.getChannels() * (nSampleSizeInBits / HALF_SAMPLE_SIZE),
					sourceFormat.getSampleRate(),
					bBigEndian);
				audioInputStream = AudioSystem.getAudioInputStream(targetFormat, audioInputStream);
				audioFormat = audioInputStream.getFormat();
			}

			SourceDataLine line = getSourceDataLine(strMixerName, audioFormat, nInternalBufferSize);
			if (line != null) {
				line.start();

				int nBytesRead = 0;
				byte[] abData = new byte[nExternalBufferSize];
				while (nBytesRead != -1) {
					try {
						nBytesRead = audioInputStream.read(abData, 0, abData.length);
					} catch (IOException e) {
						LOGGER.error("", e);
					}
					if (nBytesRead >= 0) {
						@SuppressWarnings("unused")
						int nBytesWritten = line.write(abData, 0, nBytesRead);
					}
				}

				line.drain();
				/*
				 *	All data are played. We can close the shop.
				 */
				line.close();
			}
			
		} catch (UnsupportedAudioFileException e1) {
			LOGGER.error("", e1);
		} catch (IOException e1) {
			LOGGER.error("", e1);
		}
	}

	private static SourceDataLine getSourceDataLine(String inStrMixerName, AudioFormat inAudioFormat, int inNBufferSize) {
		/*
		 *	Asking for a line is a rather tricky thing.
		 *	We have to construct an Info object that specifies
		 *	the desired properties for the line.
		 *	First, we have to say which kind of line we want. The
		 *	possibilities are: SourceDataLine (for playback), Clip
		 *	(for repeated playback)	and TargetDataLine (for
		 *	 recording).
		 *	Here, we want to do normal playback, so we ask for
		 *	a SourceDataLine.
		 *	Then, we have to pass an AudioFormat object, so that
		 *	the Line knows which format the data passed to it
		 *	will have.
		 *	Furthermore, we can give Java Sound a hint about how
		 *	big the internal buffer for the line should be. This
		 *	isn't used here, signaling that we
		 *	don't care about the exact size. Java Sound will use
		 *	some default value for the buffer size.
		 */
		SourceDataLine line = null;
		DataLine.Info info = new DataLine.Info(SourceDataLine.class, inAudioFormat, inNBufferSize);
		try {
			if (inStrMixerName != null) {
				Mixer.Info mixerInfo = getMixerInfo(inStrMixerName);
				if (mixerInfo == null) {
					return null;
				}
				Mixer mixer = AudioSystem.getMixer(mixerInfo);
				line = (SourceDataLine) mixer.getLine(info);
			} else {
				line = (SourceDataLine) AudioSystem.getLine(info);
			}

			/*
			 *	The line is there, but it is not yet ready to
			 *	receive audio data. We have to open the line.
			 */
			line.open(inAudioFormat, inNBufferSize);
		} catch (LineUnavailableException e) {
			LOGGER.error("", e);
		}
		return line;
	}

	public static Mixer.Info getMixerInfo(String inStrMixerName) {
		Mixer.Info[] aInfos = AudioSystem.getMixerInfo();
		for (int i = 0; i < aInfos.length; i++) {
			if (aInfos[i].getName().equals(inStrMixerName)) {
				return aInfos[i];
			}
		}
		return null;
	}

}
