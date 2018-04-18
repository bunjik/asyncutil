/**
 *
 */
package info.bunji.asyncutil;

import java.util.Arrays;

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

		@Override
		protected void execute() throws Exception {
			logger.debug("start execute().");
			int i = 0;
			try {
				while (i < dataCnt) {
					if (i == errCnt) {
						throw t;
					}
					//i++;
					//String s = String.format("%05d", i);
					//append(s);
					append(Arrays.asList(
								String.format("%s%05d", prefix, ++i),
								String.format("%s%05d", prefix, ++i)
							));

					if ((i % 200) == 0) {
						logger.trace("executing {}.", i);
					}
					//Thread.sleep(1);
				}
			} finally {
				logger.debug("execute finished. i={} isCancelled={}", i, isCancelled());
			}
		}

		@Override
		protected void postProcess() {
			logger.debug("call postProcess()");
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

		public TestIntervalProc(long interval) {
			super(interval);
		}

		public TestIntervalProc setCycleCount(int cycleCount) {
			this.cycleCount = cycleCount;
			return this;
		}

		@Override
		protected boolean executeInterval() {
			count++;
			logger.debug("call executeInterval() : {}", count);
			return count < cycleCount;
		}

		@Override
		protected void postProcess() {
			logger.debug("call postProcess()");
			super.postProcess();
		}
	}
}
