package com.rokus.motorcontroller;

import ioio.lib.api.TwiMaster;
import ioio.lib.api.exception.ConnectionLostException;
import android.util.Log;

public class IOIOWiiMotionPlus {

	private TwiMaster _twi;
	// 7bit addressing
	private static final int WII_WMP_ADDR_INIT = 0x53;
	private static final int WII_WMP_ADDR = 0x52;
	private final byte[] _dataBuffRaw = new byte[6];// array to store Wii MotionPlus data
	private final int[] _dataBuff = new int[6];
	private int _gyroX0, _gyroY0, _gyroZ0; //calibration zeroes
	private int gyroX_, gyroY_, gyroZ_; // integer valued output of gyro
	private double p_, q_, r_; // elements of the rotation vector in the body-fixed reference frame
	private double phd_, thd_, psd_; // yaw, pitch and roll rates, respectively
	private double phi_, theta_, psi_; // yaw, pitch and roll angles, respectively
	protected double t_, t0_, dt_, ct_;

	public boolean init(TwiMaster twi) throws ConnectionLostException, InterruptedException {
		_twi = twi;
		if(! initGyros()) {
			Log.w(Util.LOG_TAG, "Can't INTIALIZE Gyros. This might be normal if they are already initialized and the WMP hasn't been turned off.");
		}
		if(! calibrateGyrosZeroes()) {
			Log.e(Util.LOG_TAG, "Can't CALIBRATE Gyros");
			return false;
		}
		t0_ = System.nanoTime();
		return true;
	}

	private boolean initGyros() throws ConnectionLostException, InterruptedException {
		// We don't care about what the WMP returns, but we need to provide a buffer to avoid a
		// java.lang.NullPointerException
		// E/WII     ( 1164):      at java.lang.System.arraycopy(Native Method)
		// E/WII     ( 1164):      at ioio.lib.impl.TwiMasterImpl.dataReceived(TwiMasterImpl.java:141)
		boolean res = _twi.writeRead(WII_WMP_ADDR_INIT, false, new byte[]{(byte)0xFE, 0x04}, 2, _dataBuffRaw, 0);
		Thread.sleep(10);
		return res;
	}

	public boolean readData() throws ConnectionLostException, InterruptedException {
		// Send a request for data and read raw data
		if( _twi.writeRead(WII_WMP_ADDR, false, new byte[]{0x00}, 1, _dataBuffRaw, _dataBuffRaw.length)){
			for(int i=0; i<_dataBuffRaw.length; i++) {
				_dataBuff[i] = Util.fixJavaByte(_dataBuffRaw[i]);
			}

			//see http://wiibrew.org/wiki/Wiimote/Extension_Controllers#Wii_Motion_Plus for info on what each byte represents
			gyroZ_ = ((_dataBuff[3] >> 2) << 8) + _dataBuff[0] - _gyroZ0;
			gyroX_ = ((_dataBuff[4] >> 2) << 8) + _dataBuff[1] - _gyroX0;
			gyroY_ = ((_dataBuff[5] >> 2) << 8) + _dataBuff[2] - _gyroY0;
			dt_ = (System.nanoTime()-t_) * Math.pow(10, -9);
			t_ = System.nanoTime();
			
			p_ = gyroX_ * 0.00126766468; // body-X element of rotation vector in [rad/s]
			q_ = gyroY_ * 0.00126766468; // body-Y element
			r_ = gyroZ_ * 0.00126766468; // body-Z element
			
			// Conversion from rotation vector omega = [p;q;r] in the body fixed reference frame to derivatives
			// of the yaw, pitch and roll angles, respectively.
			
			phd_ = psi_/Math.cos(theta_) * q_ + Math.cos(psi_)/Math.cos(theta_) * r_;
			thd_ = Math.cos(psi_) * q_ - Math.sin(psi_) * r_;
			psd_ = p_ + Math.sin(psi_)*Math.tan(theta_) * q_ + Math.cos(psi_)*Math.tan(theta_) * r_;
			
			// Integration to obtain the new yaw, pitch and roll angles
			phi_ = phi_ + phd_*dt_;
			theta_ = theta_ + thd_*dt_;
			psi_ = psi_ + psd_*dt_;
			
			return true;
		}

		return false;
	}


	private boolean calibrateGyrosZeroes() throws ConnectionLostException, InterruptedException {
		long tempX=0, tempY=0, tempZ=0;
		_gyroX0 = 0; _gyroY0 = 0; _gyroZ0 = 0;
		phi_ = 0; theta_ = 0; psi_ = 0;
		t_ = System.nanoTime();
		for (int i=0; i<10; i++){
			if (! readData()) return false;
			
			tempX += p_;
			tempY += q_;
			tempZ += r_;

			Thread.sleep(10);
		}

		// average 10 readings
		_gyroX0 = (int)(tempX / 10);
		_gyroY0 = (int)(tempY / 10);
		_gyroZ0 = (int)(tempZ / 10);

		return true;
	}
	
	public double getTimeSeconds() {
		return (t_-t0_)*Math.pow(10, -9);
	}

	public double getP() {
		return p_;
	}

	public double getQ() {
		return q_;
	}

	public double getR() {
		return r_;
	}
	
	public double getDPhi() {
		return phd_;
	}
	
	public double getDTheta() {
		return thd_;
	}
	
	public double getDPsi() {
		return psd_;
	}
	
	public double getPhi() {
		return phi_;
	}
	
	public double getTheta() {
		return theta_;
	}
	
	public double getPsi() {
		return psi_;
	}
}
