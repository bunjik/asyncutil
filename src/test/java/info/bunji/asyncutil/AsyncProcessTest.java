package info.bunji.asyncutil;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.spy;

import java.util.Iterator;
import java.util.NoSuchElementException;

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
	public void testRun() throws Exception {
		TestAsyncProc proc = spy(new TestAsyncProc().setDataCnt(128));
		try (ClosableResult<String> results = proc.run()) {
			assertThat(results.toList().size(), is(128));
		} finally {
			verify(proc).execute();
			verify(proc).postProcess();
		}
	}

	@Test(expected=IllegalStateException.class)
	public void testRun_delayErrorfalse() throws Exception {
		TestAsyncProc proc = spy(new TestAsyncProc().setDataCnt(128).setException(new IllegalStateException(), 64));
		int i = 0;
		try (ClosableResult<String> results = proc.run(false)) {
			for (String s : results) {
				i++;
				Thread.sleep(10);
			}
		} finally {
			logger.debug("processed {}", i);
			assertThat(i, lessThan(64));
			verify(proc).execute();
			verify(proc).postProcess();
		}
	}

	@Test(expected=IllegalStateException.class)
	public void testRun_delayErrorTrue() throws Exception {
		TestAsyncProc proc = spy(new TestAsyncProc().setDataCnt(128).setException(new IllegalStateException(), 64));
		int i = 0;
		try (ClosableResult<String> results = proc.run(true)) {
			for (@SuppressWarnings("unused") String s : results) {
				i++;
			}
		} finally {
			assertThat(i, is(64));
			verify(proc).execute();
			verify(proc).postProcess();
		}
	}

	@Test
	public void testRun_bufSize() throws Exception {
		TestAsyncProc proc = spy(new TestAsyncProc().setDataCnt(128));
		try (ClosableResult<String> results = proc.run(16)) {
			assertThat(results.toList().size(), is(128));
		} finally {
			verify(proc).execute();
			verify(proc).postProcess();
		}
	}

	@Test
	public void testRun_bufSize_delayError() throws Exception {
		TestAsyncProc proc = spy(new TestAsyncProc().setDataCnt(128));
		try (ClosableResult<String> results = proc.run(16, false)) {
			assertThat(results.toList().size(), is(128));
		} finally {
			verify(proc).execute();
			verify(proc).postProcess();
		}
	}

	@Test
	public void testWaitRead() throws Exception {
		TestAsyncProc proc = spy(new TestAsyncProc().setDataCnt(2048).setInterval(0));
		int i = 0;
		try (ClosableResult<String> results = proc.run(32)) {
			Thread.sleep(5000);
			results.close();
//			i = results.toList().size();
		} finally {
			logger.debug("processed {}", i);
			verify(proc).execute();
			verify(proc).postProcess();
		}
	}

	@Test
	public void testCancel() throws Exception {
		TestAsyncProc proc = spy(new TestAsyncProc().setDataCnt(512).setInterval(0));
		int i = 0;
		try (ClosableResult<String> results = proc.run()) {
			for (String s : results) {
				i++;
//				logger.debug(s);
				results.close();
			}
		} finally {
			logger.debug("processed {}", i);
			verify(proc).execute();
			verify(proc).postProcess();
		}
	}

	@Test
	public void testException() throws Exception {
		TestAsyncProc proc = spy(new TestAsyncProc().setDataCnt(200).setException(new IllegalStateException(), 100));
		int i = 0;
		try (ClosableResult<String> results = proc.run(false)) {
			for (String s : results) {
				i++;
				//logger.debug("{}", s);
			}
			logger.debug("finished process.");
			fail("exception not occurred.");
		} catch (Throwable t) {
			logger.debug("interrupted process. throw:{}", t.getClass().getSimpleName());
			assertThat(t, is(instanceOf(IllegalStateException.class)));
		} finally {
			logger.debug("processed {}", i);
			verify(proc).execute();
			verify(proc).postProcess();
		}
	}

	@Test(expected=NoSuchElementException.class)
	public void testNoSuchElement() throws Exception {
		TestAsyncProc proc = spy(new TestAsyncProc().setDataCnt(10));
		int i = 0;
		try (ClosableResult<String> results = proc.run(true)) {
			Iterator<String> it = results.iterator();
			while (it.hasNext()) {
				it.next();
				it.remove();
				i++;
			}
			it.next();
		} finally {
			logger.debug("processed {}", i);
			assertThat(i, is(10));
			verify(proc).execute();
			verify(proc).postProcess();
		}
	}

	@Test(expected=IllegalStateException.class)
	public void testDelayException() throws Exception {
		TestAsyncProc proc = spy(new TestAsyncProc().setDataCnt(200).setException(new IllegalStateException(), 100));
		int i = 0;
		try (ClosableResult<String> results = proc.run(true)) {
			for (String s : results) {
				i++;
			}
			logger.debug("process finished.");
			fail("exception not occurred.");
		} finally {
			logger.debug("processed {}", i);
			assertThat(i, is(100));
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
		TestListener listener1 = spy(new TestListener("bgn1", "end1"));
		TestListener listener2 = spy(new TestListener("bgn2", "end2"));
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
