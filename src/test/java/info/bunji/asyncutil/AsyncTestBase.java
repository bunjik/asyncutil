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

import info.bunji.asyncutil.functions.ExecFunc;
import info.bunji.asyncutil.functions.ListenerFunc;

/**
 ************************************************
 *
 * @author f.kinoshita
 ************************************************
 */
public abstract class AsyncTestBase {

	protected Logger logger = LoggerFactory.getLogger(getClass());

	@Rule
	public TestName name = new TestName();

	private ThreadLocal<Long> testTime = new ThreadLocal<>();

	@Before
	public void setup() {
		logger.info("*** start {}() ***", name.getMethodName());
		testTime.set(System.currentTimeMillis());
	}

	@After
	public void tearDown() {
		long elapsed = System.currentTimeMillis() - testTime.get();
		logger.info("*** finish {}() ({}ms) ***", name.getMethodName(), elapsed);
	}

	/**
	 ********************************************
	 * test long function.
	 ********************************************
	 */
	public static class TestIntFunc extends ExecFunc<Integer> {

		private Logger logger = LoggerFactory.getLogger(getClass());
		private int size;
		private long intervalMs = 0;
		private int throwTiming = Integer.MAX_VALUE;

		TestIntFunc(int size) {
			this.size = size;
		}

		TestIntFunc setException(int throwTiming) {
			this.throwTiming = throwTiming;
			return this;
		}

		TestIntFunc setInterval(long intervalMs) {
			this.intervalMs = intervalMs;
			return this;
		}

		@Override
		public void run() throws Exception {
			try {
				logger.debug("start execute.");
				for (int i = 1; i <= size; i++) {
					this.append(i);
					if (i >= throwTiming) throw new IllegalStateException("error sending=" + i);
					if (intervalMs > 0) Thread.sleep(intervalMs);
				}
				logger.debug("end execute.");
			} catch(Throwable t) {
				logger.debug("end execute with error.[{}]", t.getMessage());
				throw t;
			}
		}
	}


	public static class TestListenerFunc implements ListenerFunc {
		private final Logger logger = LoggerFactory.getLogger(getClass());
		private final String msg;

		public TestListenerFunc(String msg) {
			this.msg = msg;
		}

		@Override
		public void run() {
			logger.debug(msg);
		}
	}

	/**
	 ********************************************
	 * Test listener class
	 ********************************************
	 */
	public static class TestListener implements AbstractAsyncProcess.Listener {
		private final Logger logger = LoggerFactory.getLogger(getClass());

		@Override
		public void onStart() {
			logger.debug("call listener.onStart()");
		}

		@Override
		public void onFinish() {
			logger.debug("call listener.onFinish()");
		}
	}









//	/**
//	 ********************************************
//	 * Test AsyncProcess class
//	 ********************************************
//	 */
//	@Deprecated
//	static class TestAsyncProc extends AbstractAsyncProcess<String> {
//
//		private String prefix = "";
//		private int dataCnt = 1000;
//		private long interval = 0;
//		private Exception t = null;
//		private int errCnt = Integer.MIN_VALUE;
//
//		public TestAsyncProc() {
//			super();
//		}
//
//		public TestAsyncProc setDataCnt(int dataCnt) {
//			this.dataCnt = dataCnt;
//			return this;
//		}
//
//		public TestAsyncProc setPrefix(String prefix) {
//			this.prefix = prefix;
//			return this;
//		}
//
//		public TestAsyncProc setException(Exception t, int errCnt) {
//			this.t = t;
//			this.errCnt = errCnt;
//			return this;
//		}
//
//		public TestAsyncProc setInterval(long interval) {
//			this.interval = interval;
//			return this;
//		}
//
//		@Override
//		protected void execute() throws Exception {
//			List<String> nullList = null;
//			logger.debug("start execute().");
//			int i = 0;
//			try {
//				while (i < dataCnt) {
//					if (i == errCnt) {
//						throw t;
//					}
//					//i++;
//					//append(String.format("%s%05d", prefix, i));
//
//					if (i < (dataCnt - 2) && i < (errCnt - 2)) {
//						append(Arrays.asList(
//									String.format("%s%05d", prefix, ++i),
//									String.format("%s%05d", prefix, ++i)
//								));
//					} else {
//						append(String.format("%s%05d", prefix, ++i));
//					}
//
//					if ((i % 200) == 0) {
//						logger.trace("executing {}.", i);
//						append(nullList);
//					}
//
//					if (interval > 0) Thread.sleep(interval);
//				}
//			} finally {
//				logger.debug("execute finished. i={}", i);
//			}
//		}
//
//		@Override
//		protected void postProcess() {
//			logger.debug("call postProcess() in {}", getClass().getSimpleName());
//			super.postProcess();
//		}
//	}

	/**
	 ********************************************
	 * Test AsyncIntervalProcess class
	 ********************************************
	 */
	static class TestIntervalProc extends AsyncIntervalProcess<String> {

		private int cycleCount = 10;
		private int count = 0;
		private Exception t = null;
		private int errCnt = Integer.MIN_VALUE;

		public TestIntervalProc(long interval) {
			super(interval);
		}

		public TestIntervalProc setException(Exception t, int errCnt) {
			this.t = t;
			this.errCnt = errCnt;
			return this;
		}

		public TestIntervalProc setCycleCount(int cycleCount) {
			this.cycleCount = cycleCount;
			return this;
		}

		@Override
		protected boolean executeInterval() throws Exception {
			count++;
			//logger.debug("call executeInterval() : {}", count);
			if (errCnt == count) {
				throw t;
			}
			append("" + count);
			return count < cycleCount;
		}

		@Override
		protected void postProcess() {
			logger.debug("call postProcess()");
			super.postProcess();
		}
	}
}
