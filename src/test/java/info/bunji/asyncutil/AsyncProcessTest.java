package info.bunji.asyncutil;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.spy;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@FixMethodOrder
@RunWith(PowerMockRunner.class)
@PrepareForTest({ClosableResult.class})
public class AsyncProcessTest extends AsyncTestBase {

	/**
	 ********************************************
	 * Test listener class
	 ********************************************
	 */
	static class TestListener implements AsyncProcess.Listener {
		private final Logger logger = LoggerFactory.getLogger(getClass());
		private final String bgnMsg;
		private final String endMsg;
		private final long bgnTime;

		public TestListener(String bgnMsg, String endMsg) {
			this.bgnMsg = bgnMsg;
			this.endMsg = endMsg;
			bgnTime = System.currentTimeMillis();
		}

		@Override
		public void onStart() {
			logger.debug("■call {}.", bgnMsg);
		}

		@Override
		public void onFinish() {
			logger.debug("■call {}({}ms).", endMsg, System.currentTimeMillis() - bgnTime);
		}
	}

	@Test
	public void testCancel() throws Exception {
		TestAsyncProc proc = spy(new TestAsyncProc().setDataCnt(512));
		try (ClosableResult<String> results = proc.run()) {
			results.iterator().next();
			results.close();
			Thread.sleep(100);
		} finally {
			verify(proc).execute();
			verify(proc).postProcess();
		}
	}

	@Test
	public void testListener() throws Exception {

		TestAsyncProc proc = spy(new TestAsyncProc().setDataCnt(512));
		TestListener listener = spy(new TestListener("bgn", "end"));
		proc.addListener(listener);

		int i = 0;
		try (ClosableResult<String> results = proc.run()) {
			i = results.toList().size();
		} finally {
			logger.debug("get {} items.", i);
			assertThat(i, is(512));
			verify(proc).execute();
			verify(proc).postProcess();
			verify(listener).onStart();
			verify(listener).onFinish();
		}
	}

	@Test
	public void testListener_multi() throws Exception {

		TestAsyncProc proc = spy(new TestAsyncProc().setDataCnt(512));
		TestListener listener1 = spy(new TestListener("bgn", "end"));
		TestListener listener2 = spy(new TestListener("bgn", "end"));
		proc.addListener(listener1);
		proc.addListener(listener2);

		int i = 0;
		try (ClosableResult<String> results = proc.run()) {
			i = results.toList().size();
		} finally {
			logger.debug("get {} items.", i);
			assertThat(i, is(512));
			verify(proc).execute();
			verify(proc).postProcess();
			verify(listener1).onStart();
			verify(listener1).onFinish();
			verify(listener2).onStart();
			verify(listener2).onFinish();
		}
	}

	@Test
	public void testListener_duplicate() throws Exception {

		TestAsyncProc proc = spy(new TestAsyncProc().setDataCnt(512));
		TestListener listener1 = spy(new TestListener("bgn", "end"));
		proc.addListener(listener1);

		int i = 0;
		try (ClosableResult<String> results = proc.run()) {
			i = results.toList().size();
		} finally {
			logger.debug("get {} items.", i);
			assertThat(i, is(512));
			verify(proc).execute();
			verify(proc).postProcess();
			verify(listener1, times(1)).onStart();
			verify(listener1, times(1)).onFinish();
		}
	}

}
