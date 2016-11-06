/**
 *
 */
package info.bunji.asyncutil;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author f.kinoshita
 *
 */
public abstract class AsyncTestBase {

	protected Logger logger = LoggerFactory.getLogger(getClass());

	@Rule
	public TestName name = new TestName();

	@Before
	public void setup() {
		logger.info("*** test start " + name.getMethodName() + "() ***");
	}

	@After
	public void tearDown() {
		logger.info("*** test finish " + name.getMethodName() + "() ***");
	}

	/**
	 ********************************************
	 *
	 ********************************************
	 */
	static class StringProcess1 extends AsyncProcess<String> {
		/** process item size */
		private int size = 100;
		/** item emit interval */
		private long interval = 0;
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
		 * @param interval item emit interval(ms)
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
		 * @param interval item emit interval(ms)
		 * @param errThrowCnt exception throw timing
		 **********************************
		 */
		public StringProcess1(int size, long interval, int errThrowCnt) {
			this.size = size;
			this.interval = interval;
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
			for (int i = 0; i < size; i++) {
				if (interval > 0) {
					try {
						Thread.sleep(interval);
					} catch (InterruptedException e) {
						// process canceled.
						break;
					}
				}
				if (errThrowCnt == i) {
					throw new Exception("Test Exception Occurred.");
				}

//				String msg = prefix + "-" + i + " isFinished=" + isFinished;
//				String msg = prefix + "-" + i;
//				logger.debug(msg);
				append(prefix + "-" + i);
			}
		}
	}

	/**
	 ********************************************
	 *
	 ********************************************
	 */
	public class StringProcess2 extends StringProcess1 {

		/**
		 **********************************
		 * @param size process item size
		 **********************************
		 */
		public StringProcess2(int size) {
			super(size);
		}

		/**
		 **********************************
		 * @param size process item size
		 * @param interval item emit interval(ms)
		 **********************************
		 */
		public StringProcess2(int size, long interval) {
			super(size, interval);
		}

		/**
		 **********************************
		 * @param size process item size
		 * @param errThrowCnt exception throw timing
		 **********************************
		 */
		public StringProcess2(int size, int errThrowCnt) {
			super(size, errThrowCnt);
		}

		/**
		 **********************************
		 * @param size process item size
		 * @param interval item emit interval(ms)
		 * @param errThrowCnt exception throw timing
		 **********************************
		 */
		public StringProcess2(int size, long interval, int errThrowCnt) {
			super(size, interval, errThrowCnt);
		}
	}

	/**
	 *
	 */
	static class ChainProcess extends AsyncProcess<String> {
		private final Iterable<Integer> it;

		public ChainProcess(Iterable<Integer> it) {
			this.it = it;
		}

		@Override
		protected void execute() throws Exception {
			for (Integer item : it) {
				append("item_" + item);
			}
		}
	}

	/**
	 ********************************************
	 *
	 ********************************************
	 */
	public class StringProcess3 extends StringProcess1 {
		/** postProcess wait ms */
		private long  waitPostMs;

		/**
		 **********************************
		 * @param size process item size
		 * @param waitMs process wait millis
		 **********************************
		 */
		public StringProcess3(int size, long waitMs) {
			super(size);
			waitPostMs = waitMs;
		}


		@Override
		protected void postProcess() {
			logger.debug("begin postProcess.");
			try {
				Thread.sleep(waitPostMs);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			super.postProcess();
			logger.debug("end postProcess.");
		}
	}

	/**
	 ********************************************
	 *
	 ********************************************
	 */
	static class IntegerProcess extends AsyncProcess<Integer> {
		/** process item size */
		private int size = 100;
		/** item emit interval */
		private long interval = 0;
		/** exception throw timing */
		private int errThrowCnt = -1;

		/**
		 **********************************
		 * @param size process item size
		 **********************************
		 */
		public IntegerProcess(int size) {
			this(size, 0, -1);
		}

		/**
		 **********************************
		 * @param size process item size
		 * @param interval item emit interval(ms)
		 **********************************
		 */
		public IntegerProcess(int size, long interval) {
			this(size, interval, -1);
		}

		/**
		 **********************************
		 * @param size process item size
		 * @param errThrowCnt exception throw timing
		 **********************************
		 */
		public IntegerProcess(int size, int errThrowCnt) {
			this(size, 0, errThrowCnt);
		}

		/**
		 **********************************
		 * @param size process item size
		 * @param interval item emit interval(ms)
		 * @param errThrowCnt exception throw timing
		 **********************************
		 */
		public IntegerProcess(int size, long interval, int errThrowCnt) {
			this.size = size;
			this.interval = interval;
			this.errThrowCnt = errThrowCnt;
		}

		/**
		 **********************************
		 * {@inheritDoc}
		 **********************************
		 */
		@Override
		protected void execute() throws Exception {
			for (int i = 1; i <= size; i++) {
				if (interval > 0) {
					try {
						Thread.sleep(interval);
					} catch (InterruptedException e) {
						// process canceled.
						break;
					}
				}
				if (errThrowCnt == i) {
					throw new Exception("Test Exception Occurred.");
				}
				append(i);
			}
		}
	}
}
