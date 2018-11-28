package info.bunji.asyncutil;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.spy;

import java.util.ArrayList;
import java.util.List;

import org.junit.FixMethodOrder;
import org.junit.Test;

@FixMethodOrder
//@RunWith(PowerMockRunner.class)
//@PrepareForTest({ClosableResult.class})
public class ClosableResultTest extends AsyncTestBase {

	@Test
	public void testExecute_normal() throws Exception {
		int size = 1000;
		AsyncProcess<Integer> proc = spy(AsyncProcess.create(new TestIntFunc(size)));
		try (ClosableResult<Integer> results = new ClosableResult<>(proc)) {
			assertThat(results.toList().size(), is(size));
		} finally {
			verify(proc, times(1)).dispose();
		}
	}

	@Test
	public void testExecute_normalWithBuSize() throws Exception {
		int size = 1000;
		AsyncProcess<Integer> proc = spy(AsyncProcess.create(new TestIntFunc(size)));
		try (ClosableResult<Integer> results = new ClosableResult<>(proc, 64)) {
			assertThat(results.toList().size(), is(size));
		}
	}

	@Test(expected=IllegalArgumentException.class)
	public void testExecute_negativeBufSize() throws Exception {
		int size = 1000;
		AsyncProcess<Integer> proc = spy(AsyncProcess.create(new TestIntFunc(size)));
		try (ClosableResult<Integer> results = new ClosableResult<>(proc, -1)) {
			assertThat(results.toList().size(), is(size));
		} finally {
			//verify(proc, times(1)).dispose();
		}
	}

	@Test
	public void testCreate() throws Exception {
		int size = 1000;
		TestIntFunc func = spy(new TestIntFunc(size));
		try (ClosableResult<Integer> results = ClosableResult.create(func)) {
			assertThat(results.toList().size(), is(size));
		} finally {
			verify(func, times(1)).run();
		}
	}

	@Test
	public void testCreate_bufsize() throws Exception {
		int size = 1000;
		TestIntFunc func = spy(new TestIntFunc(size));
		try (ClosableResult<Integer> results = ClosableResult.create(func, 64)) {
			assertThat(results.toList().size(), is(size));
		} finally {
			//verify(func, times(1)).run();
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCreate_negative_bufsize() throws Exception {
		int size = 1000;
		TestIntFunc func = new TestIntFunc(size);
		try (ClosableResult<Integer> results = ClosableResult.create(func, -1)) {
			assertThat(results.toList().size(), is(size));
		}
	}

	@Test
	public void testCreate_delayError() throws Exception {
		int size = 1000;
		TestIntFunc func = spy(new TestIntFunc(size));
		try (ClosableResult<Integer> results = ClosableResult.create(func, true)) {
			assertThat(results.toList().size(), is(size));
		} finally {
			verify(func, times(1)).run();
		}
	}

	@Test
	public void testCreate_bufSize_delayError() throws Exception {
		int size = 1000;
		TestIntFunc func = spy(new TestIntFunc(size));
		try (ClosableResult<Integer> results = ClosableResult.create(func, 128, true)) {
			assertThat(results.toList().size(), is(size));
		} finally {
			verify(func, times(1)).run();
		}
	}

	@Test
	public void testFromIterable() throws Exception {
		int size = 1000;
		List<Integer> source = new ArrayList<>();
		for (int i = 1; i <= size; i++) source.add(i);

		try (ClosableResult<Integer> results = new ClosableResult<>(source)) {
			assertThat(results.toList().size(), is(size));
		}
	}

	@Test
	public void testFromIterable_bufSize() throws Exception {
		int size = 1000;
		List<Integer> source = new ArrayList<>();
		for (int i = 1; i <= size; i++) source.add(i);

		try (ClosableResult<Integer> results = new ClosableResult<>(source, 128)) {
			assertThat(results.toList().size(), is(size));
		}
	}











//	@Test
//	public void testExecute_multi() throws Exception {
//		TestAsyncProc proc1 = spy(new TestAsyncProc().setDataCnt(512));
//		TestAsyncProc proc2 = spy(new TestAsyncProc().setDataCnt(512));
//		List<TestAsyncProc> procList = Arrays.asList(proc1, proc2);
//
//		int i = 0;
//		ClosableResult<String> spyResults = null;
//		try (ClosableResult<String> results = spy(new ClosableResult<String>(procList))) {
//			spyResults = results;
//			for (String val : results) {
//				i++;
//				if ((i % 100) == 0) {
//					logger.debug("get [{}]", val);
//				}
//			}
//		} finally {
//			logger.debug("get {} items.", i);
//			assertThat(i, is(1024));
//			verify(proc1).execute();
//			verify(proc1).postProcess();
//			verify(proc2).execute();
//			verify(proc2).postProcess();
//			verify(spyResults).close();
//		}
//	}
//
//	@Test(expected = IllegalArgumentException.class)
//	public void testExecute_errorEmptyProcessList() throws Exception {
//		List<TestAsyncProc> procList = new ArrayList<>();
//
//		int i = 0;
//		try (ClosableResult<String> results = new ClosableResult<String>(procList)) {
//			i = results.toList().size();
//		} finally {
//			logger.debug("get {} items.", i);
//			assertThat(i, is(0));
//		}
//	}
//
//	@Test(expected = IllegalArgumentException.class)
//	public void testExecute_errorNullProcessList() throws Exception {
//		int i = 0;
//		try (ClosableResult<String> results = spy(new ClosableResult<String>((List<TestAsyncProc>) null))) {
//			i = results.toList().size();
//		} finally {
//			logger.debug("get {} items.", i);
//			assertThat(i, is(0));
//		}
//	}
//
//	@Test
//	public void testExecute_fromIterable() throws Exception {
//
//		List<String> strList = new ArrayList<>();
//		for (int i = 1; i <= 1000; i++) strList.add("" + i);
//
//		int i = 0;
//		ClosableResult<String> spyResults = null;
//		try (ClosableResult<String> results = spy(new ClosableResult<>(strList))) {
//			spyResults = results;
//			for (String val : results) {
//				i++;
//				if ((i % 100) == 0) logger.debug("get [{}]", val);
//			}
//		} finally {
//			logger.debug("get {} items.", i);
//			assertThat(i, is(1000));
//			verify(spyResults).close();
//		}
//	}
//
//	@Test
//	public void testExecute_fromIterableWithBufSize() throws Exception {
//
//		List<String> strList = new ArrayList<>();
//		for (int i = 1; i <= 1000; i++) strList.add("" + i);
//
//		int i = 0;
//		ClosableResult<String> spyResults = null;
//		try (ClosableResult<String> results = spy(new ClosableResult<>(strList, 100))) {
//			spyResults = results;
//			i = spyResults.toList().size();
//		} finally {
//			logger.debug("get {} items.", i);
//			assertThat(i, is(1000));
//			verify(spyResults).close();
//		}
//	}
//
//	@Test
//	public void testExecute_toList() throws Exception {
//
//		TestAsyncProc proc = spy(new TestAsyncProc().setDataCnt(512));
//
//		int i = 0;
//		ClosableResult<String> spyResults = null;
//		try (ClosableResult<String> results = spy(new ClosableResult<>(proc))) {
//			spyResults = results;
//			List<String> items = spyResults.toList();
//			i = items.size();
//		} finally {
//			logger.debug("get {} items.", i);
//			verify(proc).execute();
//			verify(proc).postProcess();
//			verify(spyResults).close();
//		}
//	}
//
//	@Test(expected=IllegalStateException.class)
//	public void testExecute_noDelayError() throws Exception {
//
//		TestAsyncProc proc = spy(new TestAsyncProc()
//								.setDataCnt(1000)
//								.setException(new IllegalStateException(name.getMethodName()), 500));
//		int i = 0;
//		ClosableResult<String> spyResults = null;
//		try (ClosableResult<String> results = spy(new ClosableResult<>(proc, false))) {
//			spyResults = results;
//			for (Object val : results) {
//				i++;
//				if ((i % 100) == 0) logger.debug("get [{}]", val);
//				Thread.sleep(5);
//			}
//		} catch (Throwable t) {
//			assertThat(t.getMessage(), is(name.getMethodName()));
//			assertThat(i, lessThan(500));
//			throw t;
//		} finally {
//			logger.debug("get {} items.", i);
//			verify(proc).execute();
//			verify(proc).postProcess();
//			verify(spyResults).close();
//		}
//	}
//
//	@Test(expected = IllegalStateException.class)
//	public void testExecute_delayError() throws Exception {
//
//		TestAsyncProc proc = spy(new TestAsyncProc()
//								.setDataCnt(1000)
//								.setException(new IllegalStateException(name.getMethodName()), 500));
//		int i = 0;
//		ClosableResult<String> spyResults = null;
//		try (ClosableResult<String> results = spy(new ClosableResult<>(proc))) {
//			spyResults = results;
//			for (Object val : results) {
//				i++;
//				if ((i % 100) == 0) logger.debug("get [{}]", val);
//			}
//		} catch (Throwable t) {
//			assertThat(t.getMessage(), is(name.getMethodName()));
//			assertThat(i, is(500));
//			throw t;
//		} finally {
//			logger.debug("get {} items.", i);
//			verify(proc).execute();
//			verify(proc).postProcess();
//			verify(spyResults).close();
//		}
//	}
//
//	@Test
//	public void testExecute_interrupt() throws Exception {
//
//		TestAsyncProc proc = spy(new TestAsyncProc().setDataCnt(1000));
//		int i = 0;
//		ClosableResult<String> spyResults = null;
//		try (ClosableResult<String> results = spy(new ClosableResult<>(proc))) {
//			spyResults = results;
//			for (Object val : results) {
//				i++;
//				if ((i % 100) == 0) {
//					logger.debug("get [{}]", val);
//				}
//				if (i == 500) {
//					break;
//				}
//			}
//		} finally {
//			Thread.sleep(100);
//			logger.debug("get {} items.", i);
//			verify(proc).execute();
//			verify(proc).postProcess();
//			verify(spyResults).close();
//		}
//	}
//
//	@Test(expected = NoSuchElementException.class)
//	public void testExecute_iteratorNoNext() throws Exception {
//
//		TestAsyncProc proc = spy(new TestAsyncProc().setDataCnt(1000));
//		int i = 0;
//		ClosableResult<String> spyResults = null;
//		try (ClosableResult<String> results = spy(new ClosableResult<>(proc))) {
//			spyResults = results;
//			i = results.toList().size();
//			results.iterator().next();
//		} finally {
//			verify(proc).execute();
//			verify(proc).postProcess();
//			verify(spyResults).close();
//		}
//	}
//
//	@Test
//	public void testExecute_iterator() throws Exception {
//		TestAsyncProc proc = spy(new TestAsyncProc().setDataCnt(10));
//		int i = 0;
//		ClosableResult<String> spyResults = null;
//		try (ClosableResult<String> results = spy(new ClosableResult<>(proc))) {
//			spyResults = results;
//			Iterator<String> it = spyResults.iterator();
//			for (int n = 0; n < 10; n++) {
//				it.next();
//				i++;
//			}
//		} finally {
//			logger.debug("get {} items.", i);
//			verify(proc).execute();
//			verify(proc).postProcess();
//			verify(spyResults).close();
//		}
//
//	}
//
//
//	@Test
//	public void testRun() throws Exception {
//
//		TestAsyncProc proc = spy(new TestAsyncProc().setDataCnt(1000));
//
//		int i = 0;
//		ClosableResult<String> spyResults = null;
//		try (ClosableResult<String> results = spy(proc.run())) {
//			spyResults = results;
//			i = results.toList().size();
//		} finally {
//			verify(proc).execute();
//			verify(proc).postProcess();
//			verify(spyResults).close();
//		}
//	}
}
