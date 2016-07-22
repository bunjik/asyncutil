/**
 *
 */
package info.bunji.asyncutil.processes;

import info.bunji.asyncutil.AsyncProcess;

/**
 *
 *
 * @author f.kinoshita
 */
public class StringProcess1 extends AsyncProcess<String> {
	/** process item size */
	private int size = 100;
	/** item emit interval */
	private long intervral = 0;
	/** exception throw timing */
	private int errThrowCnt = -1;

	/**
	 **********************************
	 * @param size process item size
	 **********************************
	 */
	public StringProcess1(int size) {
		this(size, 0, -1);
	}

	/**
	 **********************************
	 * @param size process item size
	 * @param intevral item emit interval(ms)
	 **********************************
	 */
	public StringProcess1(int size, long interval) {
		this(size, interval, -1);
	}

	/**
	 **********************************
	 * @param size process item size
	 * @param errThrowCnt exception throw timing
	 **********************************
	 */
	public StringProcess1(int size, int errThrowCnt) {
		this(size, 0, errThrowCnt);
	}

	/**
	 **********************************
	 * @param size process item size
	 * @param intevral item emit interval(ms)
	 * @param errThrowCnt exception throw timing
	 **********************************
	 */
	public StringProcess1(int size, long interval, int errThrowCnt) {
		this.size = size;
		this.intervral = interval;
		this.errThrowCnt = errThrowCnt;
	}

	/**
	 **********************************
	 * {@inheritDoc}
	 **********************************
	 */
	@Override
	protected void execute() throws Exception {
		String prefix = Thread.currentThread().getName();
		for (int i = 1; i <= size; i++) {
			if (intervral > 0) {
				try {
					Thread.sleep(intervral);
				} catch (InterruptedException e) {
					// process canceled.
					break;
				}
			}
			if (errThrowCnt == i) {
				throw new Exception("Test Eception Occurred.");
			}
//			if (isInterrupted()) break;

//			String msg = prefix + "-" + i + " isFinished=" + isFinished;
//			String msg = prefix + "-" + i;
//			logger.debug(msg);
			append(prefix + "-" + i);
		}
	}
}
