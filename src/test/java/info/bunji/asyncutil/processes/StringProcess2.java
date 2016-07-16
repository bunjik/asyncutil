/**
 *
 */
package info.bunji.asyncutil.processes;

/**
 * @author f.kinoshita
 *
 */
public class StringProcess2 extends StringProcess1 {

	/**
	 * @param size
	 */
	public StringProcess2(int size) {
		super(size);
	}

	/**
	 * @param size
	 * @param interval
	 */
	public StringProcess2(int size, long interval) {
		super(size, interval);
	}

	/**
	 * @param size
	 * @param errThrowCnt
	 */
	public StringProcess2(int size, int errThrowCnt) {
		super(size, errThrowCnt);
	}

	/**
	 * @param size
	 * @param interval
	 * @param errThrowCnt
	 */
	public StringProcess2(int size, long interval, int errThrowCnt) {
		super(size, interval, errThrowCnt);
	}
}
