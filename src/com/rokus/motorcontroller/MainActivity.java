package com.rokus.motorcontroller;

import ioio.lib.api.PwmOutput;
import ioio.lib.api.TwiMaster;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.SeekBar;
import android.widget.TextView;

public class MainActivity extends IOIOActivity {

	private SeekBar seekBar1, seekBar2;
	private TextView xrot, yrot, zrot, _msg;
	private int i = 0;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		seekBar1 = (SeekBar) findViewById(R.id.seekBar1);
		seekBar2 = (SeekBar) findViewById(R.id.seekBar2);
		xrot = (TextView) findViewById(R.id.textView4);
		yrot = (TextView) findViewById(R.id.textView5);
		zrot = (TextView) findViewById(R.id.textView6);
		_msg = (TextView) findViewById(R.id.textView7);
	}

	class Looper extends BaseIOIOLooper {
		private PwmOutput servo1, servo2;
		private final IOIOWiiMotionPlus wmp_ = new IOIOWiiMotionPlus();
		protected TwiMaster wmpBus;
		private double phi_, theta_, psi_, dphi_, dtheta_, dpsi_, roll_, pitch_, yaw_, time_;
		
		// Declare file to write WMP data to
		File f = new File(Environment.getExternalStorageDirectory() + File.separator + "WMPdata");
		FileOutputStream fOut;
		OutputStreamWriter osw;
		
		@Override
		protected void setup() throws ConnectionLostException, InterruptedException {
			try {
				servo1 = ioio_.openPwmOutput( 7, 67);
				servo2 = ioio_.openPwmOutput(14, 67);

				// Initialize the I2C system, join the I2C bus
				wmpBus = ioio_.openTwiMaster(2,TwiMaster.Rate.RATE_400KHz,false);

				if (! wmp_.init(wmpBus)) {
					throw new Exception("Can't initialise WMP");
				}

				f.createNewFile();
				
			} catch (Exception e) {
				msg(e);
				Log.w("Main",e.getMessage());
			}
		} 

		@Override
		public void loop() throws ConnectionLostException {
			
			try {
				msg("Running loop " + i + ", dt = " + wmp_.dt_);
				try {
					if(wmp_.readData()) {
						
						phi_ = wmp_.getPhi();
						theta_ = wmp_.getTheta();
						psi_ = wmp_.getPsi();
						
						dphi_ = wmp_.getDPhi();
						dtheta_ = wmp_.getDTheta();
						dpsi_ = wmp_.getDPsi();
						
						roll_ = wmp_.getP();
						pitch_ = wmp_.getQ();
						yaw_ = wmp_.getR();
						
						time_ = wmp_.getTimeSeconds();
						

						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								xrot.setText(getString(R.string.theta_is) + String.format("%.2f", phi_));
								yrot.setText(getString(R.string.phi_is)   + String.format("%.2f", theta_));
								zrot.setText(getString(R.string.psi_is)   + String.format("%.2f", psi_));
							}
						});

						String line_of_data = i + ", " + time_ + ", " + phi_ + ", " + theta_ + ", " + psi_ + ", " + dphi_ + ", " + dtheta_ + ", " + dpsi_ + ", " + roll_ + ", " + pitch_ + ", " + yaw_ + "\n";
						fOut = new FileOutputStream(f,true);
						osw = new OutputStreamWriter(fOut);
						osw.write(line_of_data);
						osw.flush();
						osw.close();
						fOut.flush();
						fOut.close();
					}
				}
				catch (Exception e) {
					msg(e);
				}
				
				servo1.setPulseWidth(650 + seekBar1.getProgress() * 21);
				servo2.setPulseWidth(650 + seekBar2.getProgress() * 21);

				Thread.sleep(10);
				i++;
			} 
			catch (InterruptedException e) {
				ioio_.disconnect();
			}
		}
	}

	private void msg(Throwable e){
		msg("ERR: " + e.getMessage());
	}

	private void msg(final String msg){
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				_msg.setText("MSG: " + msg);
			}
		});
	}

	@Override
	protected IOIOLooper createIOIOLooper() {
		return new Looper();
	}
}



