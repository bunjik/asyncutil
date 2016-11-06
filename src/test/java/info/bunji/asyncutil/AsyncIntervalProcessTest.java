package info.bunji.asyncutil;

import static org.mockito.Mockito.*;

import org.junit.Test;

public class AsyncIntervalProcessTest extends AsyncTestBase {

	static class TestIntervalProc extends AsyncIntervalProcess<String> {

		private int execCnt;
		private int curCnt = 0;

		public TestIntervalProc(long interval, int execCnt) {
			super(interval);
			this.execCnt = execCnt;
		}

		@Override
		protected boolean executeInterval() {
			logger.debug("exec {}/{}", ++curCnt, execCnt);
			return curCnt < execCnt;
		}
	}

	@Test(expected=IllegalArgumentException.class)
	public void testAsyncIntervelProcess() {
		new TestIntervalProc(0, 10);
	}

	@Test
	public void testExecuteInterval() {
		int limit = 10;
		AsyncIntervalProcess<String> proc = spy(new TestIntervalProc(1000, limit));

		AsyncExecutor.execute(proc).block();

		verify(proc, times(limit)).executeInterval();
		verify(proc, times(1)).postProcess();
	}

	@Test
	public void testExecuteIntervalCancel() throws Exception {
		int limit = 10;
		AsyncIntervalProcess<String> proc = spy(new TestIntervalProc(1000, limit));

		try (AsyncResult<String> result = AsyncExecutor.execute(proc)) {
			Thread.sleep(5000);
		}

		verify(proc, times(5)).executeInterval();
		verify(proc, times(1)).postProcess();
	}
}
