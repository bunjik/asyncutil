package info.bunji.asyncutil;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThan;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.spy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
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
public class ClosableResultTest extends AsyncTestBase {

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
	public void testExecute_normal() throws Exception {

		TestAsyncProc proc = spy(new TestAsyncProc().setDataCnt(512));

		int i = 0;
		ClosableResult<String> spyResults = null;
		try (ClosableResult<String> results = spy(new ClosableResult<>(proc))) {
			spyResults = results;
			for (String val : results) {
				i++;
				if ((i % 100) == 0) logger.debug("get [{}]", val);
			}
		} finally {
			logger.debug("get {} items.", i);
			verify(proc).execute();
			verify(proc).postProcess();
			verify(spyResults).close();
		}
	}

	@Test
	public void testExecute_normalWithBuSize() throws Exception {

		TestAsyncProc proc = spy(new TestAsyncProc().setDataCnt(512));

		int i = 0;
		ClosableResult<String> spyResults = null;
		try (ClosableResult<String> results = spy(new ClosableResult<>(proc, 64))) {
			spyResults = results;
			for (String val : results) {
				i++;
				if ((i % 100) == 0) logger.debug("get [{}]", val);
			}
		} finally {
			logger.debug("get {} items.", i);
			verify(proc).execute();
			verify(proc).postProcess();
			verify(spyResults).close();
		}
	}

	@Test(expected=IllegalArgumentException.class)
	public void testExecute_negativeBuSize() throws Exception {

		TestAsyncProc proc = spy(new TestAsyncProc().setDataCnt(512));

		int i = 0;
		try (ClosableResult<String> results = new ClosableResult<>(proc, 0)) {
			for (String val : results) {
				i++;
				if ((i % 100) == 0) logger.debug("get [{}]", val);
			}
		} finally {
			logger.debug("get {} items.", i);
			verify(proc, times(0)).execute();
			verify(proc, times(0)).postProcess();
		}
	}

	@Test
	public void testExecute_multi() throws Exception {
		TestAsyncProc proc1 = spy(new TestAsyncProc().setDataCnt(512));
		TestAsyncProc proc2 = spy(new TestAsyncProc().setDataCnt(512));
		List<TestAsyncProc> procList = Arrays.asList(proc1, proc2);

		int i = 0;
		ClosableResult<String> spyResults = null;
		try (ClosableResult<String> results = spy(new ClosableResult<String>(procList))) {
			spyResults = results;
			for (String val : results) {
				i++;
				if ((i % 100) == 0)	logger.debug("get [{}]", val);
			}
		} finally {
			logger.debug("get {} items.", i);
			assertThat(i, is(1024));
			verify(proc1).execute();
			verify(proc1).postProcess();
			verify(proc2).execute();
			verify(proc2).postProcess();
			verify(spyResults).close();
		}
	}

	@Test(expected=IllegalArgumentException.class)
	public void testExecute_errorEmptyProcessList() throws Exception {
		List<TestAsyncProc> procList = new ArrayList<>();

		int i = 0;
		try (ClosableResult<String> results = new ClosableResult<String>(procList)) {
			i = results.toList().size();
		} finally {
			logger.debug("get {} items.", i);
			assertThat(i, is(0));
		}
	}

	@Test(expected=IllegalArgumentException.class)
	public void testExecute_errorNullProcessList() throws Exception {
		int i = 0;
		try (ClosableResult<String> results = spy(new ClosableResult<String>((List<TestAsyncProc>) null))) {
			i = results.toList().size();
		} finally {
			logger.debug("get {} items.", i);
			assertThat(i, is(0));
		}
	}

	@Test
	public void testExecute_fromIterable() throws Exception {

		List<String> strList = new ArrayList<>();
		for (int i = 1; i <= 1000; i++) strList.add("" + i);

		int i = 0;
		ClosableResult<String> spyResults = null;
		try (ClosableResult<String> results = spy(new ClosableResult<>(strList))) {
			spyResults = results;
			for (String val : results) {
				i++;
				if ((i % 100) == 0) logger.debug("get [{}]", val);
			}
		} finally {
			logger.debug("get {} items.", i);
			assertThat(i, is(1000));
			verify(spyResults).close();
		}
	}

	@Test
	public void testExecute_fromIterableWithBufSize() throws Exception {

		List<String> strList = new ArrayList<>();
		for (int i = 1; i <= 1000; i++) strList.add("" + i);

		int i = 0;
		ClosableResult<String> spyResults = null;
		try (ClosableResult<String> results = spy(new ClosableResult<>(strList, 100))) {
			spyResults = results;
			i = spyResults.toList().size();
		} finally {
			logger.debug("get {} items.", i);
			assertThat(i, is(1000));
			verify(spyResults).close();
		}
	}

	@Test
	public void testExecute_toList() throws Exception {

		TestAsyncProc proc = spy(new TestAsyncProc().setDataCnt(512));

		int i = 0;
		ClosableResult<String> spyResults = null;
		try (ClosableResult<String> results = spy(new ClosableResult<>(proc))) {
			spyResults = results;
			List<String> items = spyResults.toList();
			i = items.size();
		} finally {
			logger.debug("get {} items.", i);
			verify(proc).execute();
			verify(proc).postProcess();
			verify(spyResults).close();
		}
	}

	@Test(expected=IllegalStateException.class)
	public void testExecute_noDelayError() throws Exception {

		TestAsyncProc proc = spy(new TestAsyncProc()
								.setDataCnt(1000)
								.setException(new IllegalStateException(name.getMethodName()), 500));
		int i = 0;
		ClosableResult<String> spyResults = null;
		try (ClosableResult<String> results = spy(new ClosableResult<>(proc, false))) {
			spyResults = results;
			for (Object val : results) {
				i++;
				if ((i % 100) == 0) logger.debug("get [{}]", val);
				Thread.sleep(5);
			}
		} catch (Throwable t) {
			assertThat(t.getMessage(), is(name.getMethodName()));
			assertThat(i, lessThan(500));
			throw t;
		} finally {
			logger.debug("get {} items.", i);
			verify(proc).execute();
			verify(proc).postProcess();
			verify(spyResults).close();
		}
	}

	@Test(expected=IllegalStateException.class)
	public void testExecute_delayError() throws Exception {

		TestAsyncProc proc = spy(new TestAsyncProc()
								.setDataCnt(1000)
								.setException(new IllegalStateException(name.getMethodName()), 500));
		int i = 0;
		ClosableResult<String> spyResults = null;
		try (ClosableResult<String> results = spy(new ClosableResult<>(proc))) {
			spyResults = results;
			for (Object val : results) {
				i++;
				if ((i % 100) == 0) logger.debug("get [{}]", val);
			}
		} catch (Throwable t) {
			assertThat(t.getMessage(), is(name.getMethodName()));
			assertThat(i, is(500));
			throw t;
		} finally {
			logger.debug("get {} items.", i);
			verify(proc).execute();
			verify(proc).postProcess();
			verify(spyResults).close();
		}
	}

	@Test
	public void testExecute_interrupt() throws Exception {

		TestAsyncProc proc = spy(new TestAsyncProc().setDataCnt(1000));
		int i = 0;
		ClosableResult<String> spyResults = null;
		try (ClosableResult<String> results = spy(new ClosableResult<>(proc))) {
			spyResults = results;
			for (Object val : results) {
				i++;
				if ((i % 100) == 0) logger.debug("get [{}]", val);
				if (i == 500) break;
			}
		} finally {
			Thread.sleep(100);
			logger.debug("get {} items.", i);
			verify(proc).execute();
			verify(proc).postProcess();
			verify(spyResults).close();
		}
	}

	@Test(expected=NoSuchElementException.class)
	public void testExecute_iteratorNoNext() throws Exception {

		TestAsyncProc proc = spy(new TestAsyncProc().setDataCnt(1000));
		int i = 0;
		ClosableResult<String> spyResults = null;
		try (ClosableResult<String> results = spy(new ClosableResult<>(proc))) {
			spyResults = results;
			i = results.toList().size();
			results.iterator().next();
		} finally {
			verify(proc).execute();
			verify(proc).postProcess();
			verify(spyResults).close();
		}
	}

	@Test
	public void testExecute_iterator() throws Exception {
		TestAsyncProc proc = spy(new TestAsyncProc().setDataCnt(10));
		int i = 0;
		ClosableResult<String> spyResults = null;
		try (ClosableResult<String> results = spy(new ClosableResult<>(proc))) {
			spyResults = results;
			Iterator<String> it = spyResults.iterator();
			for (int n = 0; n < 10; n++) {
				it.next();
				i++;
			}
		} finally {
			logger.debug("get {} items.", i);
			verify(proc).execute();
			verify(proc).postProcess();
			verify(spyResults).close();
		}

	}


	@Test
	public void testRun() throws Exception {

		TestAsyncProc proc = spy(new TestAsyncProc().setDataCnt(1000));

		int i = 0;
		ClosableResult<String> spyResults = null;
		try (ClosableResult<String> results = spy(proc.run())) {
			spyResults = results;
			i = results.toList().size();
		} finally {
			verify(proc).execute();
			verify(proc).postProcess();
			verify(spyResults).close();
		}
	}
}
