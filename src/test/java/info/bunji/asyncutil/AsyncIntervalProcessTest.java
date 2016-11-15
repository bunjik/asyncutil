package info.bunji.asyncutil;

import static org.mockito.Mockito.*;

import java.io.IOException;

import org.junit.Test;

import info.bunji.asyncutil.AsyncIntervalProcess.Listener;


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
	public void testExecuteInterval() throws IOException {
		int limit = 5;
		AsyncIntervalProcess<String> proc = spy(new TestIntervalProc(1000, limit));

		try (AsyncResult<String> result = AsyncExecutor.execute(proc)) {
			result.block();
		}
		verify(proc, times(limit)).executeInterval();
		verify(proc, times(1)).postProcess();
	}

	@Test
	public void testExecuteIntervalCancel() throws Exception {
		int limit = 5;
		int stop = 3;
		AsyncIntervalProcess<String> proc = spy(new TestIntervalProc(1000, limit));

		try (AsyncResult<String> result = AsyncExecutor.execute(proc)) {
			Thread.sleep(stop * 1000);
			result.close();
		}
		verify(proc, times(stop)).executeInterval();
		verify(proc, times(1)).postProcess();
	}

	@Test
	public void testExecuteIntervalEvent() throws Exception {
		int limit = 5;
		AsyncIntervalProcess<String> proc = new TestIntervalProc(1000, limit);
		Listener listener = spy(new AsyncIntervalProcess.Listener() {
			@Override
			public void onStart() {
				logger.debug("call onStart()");
			}
			@Override
			public void onFinish() {
				logger.debug("call onFinish()");
			}
			@Override
			public void onInterval() {
				logger.debug("call onInterval()");
			}
		});
		proc.setListener(listener);

		try (AsyncResult<String> result = AsyncExecutor.execute(proc)) {
			result.block();
		}

		verify(listener, times(1)).onStart();
		verify(listener, times(limit)).onInterval();
		verify(listener, times(1)).onFinish();
	}
}
