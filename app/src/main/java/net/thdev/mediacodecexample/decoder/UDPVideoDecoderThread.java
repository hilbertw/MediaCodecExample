package net.thdev.mediacodecexample.decoder;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.channels.DatagramChannel;

public class UDPVideoDecoderThread extends Thread {
	private static final String VIDEO = "video/";
	private static final String TAG = "UdpVideoDecoder";



	private DatagramSocket udpSocket;
	private MediaCodec mDecoder;
	private boolean eosReceived=false;
	private DatagramChannel channel;

	public UDPVideoDecoderThread() {
		try {
			//udpSocket = new DatagramSocket(8000);
			channel = DatagramChannel.open();
			channel.socket().bind(new InetSocketAddress(8000));

		} catch (SocketException e) {
			Log.e("Udp:", "Socket Error:", e);
		} catch (IOException e) {
			Log.e("Udp Send:", "IO Error:", e);
		}
	}

	public boolean init(Surface surface) {



			

				MediaFormat format = new MediaFormat();
				String mime="video/hevc";


				format.setInteger(MediaFormat.KEY_WIDTH,640);
		        format.setInteger(MediaFormat.KEY_HEIGHT,480);
		        format.setInteger(MediaFormat.KEY_FRAME_RATE,25);
		        format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE,8<<10);
		        format.setString(MediaFormat.KEY_MIME,"video/hevc");

				mDecoder = MediaCodec.createDecoderByType(mime);
				try {

						mDecoder.configure(format, surface, null, 0 /* Decoder */);
						
				} catch (IllegalStateException e) {
						Log.e(TAG, "codec '" + mime + "' failed configuration. " + e);
						return false;
				}
					
				mDecoder.start();

		return true;
	}
 private int readSampleData(ByteBuffer message)
 {
	  int retcd =0;
	  assert (channel!=null);
	  assert(!message.isReadOnly());
	 try {
         //message.allocate(8<<10);
		 //DatagramPacket packet = new DatagramPacket(message.array(),message.capacity());
		 //Log.i("UDP client: ", "about to wait to receive");
		 //udpSocket.receive(packet);
		 //String text = new String(message.array(), 0, packet.getLength());
		 //Log.d("Received data", text);
		 //retcd=packet.getLength();
		  channel.receive(message);
           retcd = message.capacity();
		 Log.d("Received data", String.valueOf(retcd));

	 }catch (IOException e) {
		 Log.e(TAG, "error: UDP client has IOException"+e);
	 }
	 return retcd ;

 }
 int count;
 private int getSampleTime()
 {
 	return  count++;
 }
	@Override
	public void run() {
		BufferInfo info = new BufferInfo();
		ByteBuffer[] inputBuffers = mDecoder.getInputBuffers();
		mDecoder.getOutputBuffers();
		
		boolean isInput = true;
		boolean first = false;
		long startWhen = 0;
		
		while (true) {
			if (channel!=null) {
				int inputIndex = mDecoder.dequeueInputBuffer(10000);
				if (inputIndex >= 0) {
					// fill inputBuffers[inputBufferIndex] with valid data
					ByteBuffer inputBuffer = inputBuffers[inputIndex];
					
					int sampleSize = readSampleData(inputBuffer);
					
					if ( sampleSize > 0) {
						mDecoder.queueInputBuffer(inputIndex, 0, sampleSize, getSampleTime(), 0);
						
					} else {
						Log.d(TAG, "InputBuffer BUFFER_FLAG_END_OF_STREAM");
						mDecoder.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
						isInput = false;
					}
				}
			}
			
			int outIndex = mDecoder.dequeueOutputBuffer(info, 10000);
			switch (outIndex) {
			case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
				Log.d(TAG, "INFO_OUTPUT_BUFFERS_CHANGED");
				mDecoder.getOutputBuffers();
				break;
				
			case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
				Log.d(TAG, "INFO_OUTPUT_FORMAT_CHANGED format : " + mDecoder.getOutputFormat());
				break;
				
			case MediaCodec.INFO_TRY_AGAIN_LATER:
//				Log.d(TAG, "INFO_TRY_AGAIN_LATER");
				break;
				
			default:
				if (!first) {
					startWhen = System.currentTimeMillis();
					first = true;
				}
				try {
					long sleepTime = (info.presentationTimeUs / 1000) - (System.currentTimeMillis() - startWhen);
					Log.d(TAG, "info.presentationTimeUs : " + (info.presentationTimeUs / 1000) + " playTime: " + (System.currentTimeMillis() - startWhen) + " sleepTime : " + sleepTime);
					
					if (sleepTime > 0)
						Thread.sleep(sleepTime);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} 
				
				mDecoder.releaseOutputBuffer(outIndex, true /* Surface init */);
				break;
			}
			
			// All decoded frames have been rendered, we can stop playing now
			if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
				Log.d(TAG, "OutputBuffer BUFFER_FLAG_END_OF_STREAM");
				break;
			}
		}
		
		mDecoder.stop();
		mDecoder.release();

	}
	
	public void close() {
		 eosReceived = true;
	}
}
