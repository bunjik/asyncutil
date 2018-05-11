/**
 *
 */
package info.bunji.asyncutil;

import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	 * Test AsyncProcess class
	 ********************************************
	 */
	static class TestAsyncProc extends AsyncProcess<String> {

		private String prefix = "";
		private int dataCnt = 1000;
		private long interval = 0;
		private Exception t = null;
		private int errCnt = Integer.MIN_VALUE;

		public TestAsyncProc() {
			super();
		}

		public TestAsyncProc setDataCnt(int dataCnt) {
			this.dataCnt = dataCnt;
			return this;
		}

		public TestAsyncProc setPrefix(String prefix) {
			this.prefix = prefix;
			return this;
		}

		public TestAsyncProc setException(Exception t, int errCnt) {
			this.t = t;
			this.errCnt = errCnt;
			return this;
		}

		public TestAsyncProc setInterval(long interval) {
			this.interval = interval;
			return this;
		}

		@Override
		protected void execute() throws Exception {
			List<String> nullList = null;
			logger.debug("start execute().");
			int i = 0;
			try {
				while (i < dataCnt) {
					if (i == errCnt) {
						throw t;
					}
					//i++;
					//append(String.format("%s%05d", prefix, i));

					if (i < (dataCnt - 2) && i < (errCnt - 2)) {
						append(Arrays.asList(
									String.format("%s%05d", prefix, ++i),
									String.format("%s%05d", prefix, ++i)
								));
					} else {
						append(String.format("%s%05d", prefix, ++i));
					}

					if ((i % 200) == 0) {
						logger.trace("executing {}.", i);
						append(nullList);
					}

					if (interval > 0) Thread.sleep(interval);
				}
			} finally {
				logger.debug("execute finished. i={}", i);
			}
		}

		@Override
		protected void postProcess() {
			logger.debug("call postProcess() in {}", getClass().getSimpleName());
			super.postProcess();
		}
	}

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
			logger.debug("call executeInterval() : {}", count);
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
