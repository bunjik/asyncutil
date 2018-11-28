package info.bunji.asyncutil;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.mockito.Mockito.spy;

import org.junit.FixMethodOrder;
import org.junit.Test;

@FixMethodOrder
public class AsyncIntervalProcessTest extends AsyncTestBase {

	@Test
	public void testExecuteInterval() throws Exception {
		int count = 10;
		TestIntervalProc proc = spy(new TestIntervalProc(200).setCycleCount(count));
		try (ClosableResult<String> results = proc.run()) {
			assertThat(results.toList().size(), is(count));
		} finally {
			//verify(proc, times(count)).executeInterval();
		}
	}

	@Test
	public void testExecuteInterval_interrupt1() throws Exception {
		int count = 10;
		TestIntervalProc proc = spy(new TestIntervalProc(200).setCycleCount(count));
		int cnt = 0;
		try (ClosableResult<String> results = proc.run()) {
			for (@SuppressWarnings("unused") String s : results) {
				if (++cnt >= (count / 2)) {
					proc.dispose();
				}
			}
		} finally {
			assertThat(cnt, lessThan(count));
		}
	}

	@Test
	public void testExecuteInterval_interrupt2() throws Exception {
		int count = 10;
		TestIntervalProc proc = spy(new TestIntervalProc(200).setCycleCount(count));
		int cnt = 0;
		try (ClosableResult<String> results = proc.run()) {
			for (@SuppressWarnings("unused") String s : results) {
				if (++cnt >= (count / 2)) {
					break;
				}
			}
		} finally {
			assertThat(cnt, lessThan(count));
		}
	}

	@Test(expected = IllegalStateException.class)
	public void testExecuteInterval_exception() throws Exception {
		int count = 10;
		TestIntervalProc proc = spy(new TestIntervalProc(200)
											.setCycleCount(count)
											.setException(new IllegalStateException(), 5));
		int cnt = 0;
		try (ClosableResult<String> results = proc.run()) {
			for (@SuppressWarnings("unused") String s : results) {
				;
			}
		} finally {
			assertThat(cnt, lessThan(count));
		}
	}

	@Test(expected=IllegalArgumentException.class)
	public void testExecuteInterval_negativeInterval() throws Exception {
		new TestIntervalProc(-1);
	}






//	@Test
//	public void testExecuteInterval() throws Exception {
//
//		TestIntervalProc proc = spy(new TestIntervalProc(100).setCycleCount(10));
//		try (ClosableResult<String> results = proc.run()) {
//			results.toList();
//		} finally {
//			verify(proc, times(10)).executeInterval();
//			verify(proc).postProcess();
//		}
//	}
//
//	@Test
//	public void testExecuteInterval_interrupt() throws Exception {
//
//		TestIntervalProc proc = spy(new TestIntervalProc(500).setCycleCount(10));
//		try (ClosableResult<String> results = proc.run()) {
//			// do Nothing.
//  			Thread.sleep(1000);
//		} finally {
//			verify(proc).postProcess();
//		}
//	}
//
//	@Test(expected=IllegalStateException.class)
//	public void testExecuteInterval_interrupt2() throws Exception {
//
//		TestIntervalProc proc = spy(new TestIntervalProc(100).setCycleCount(10).setException(new IllegalStateException(), 5));
//		try (ClosableResult<String> results = proc.run(false)) {
//			// do Nothing.
//			for (String s : results) {
//	  			Thread.sleep(100);
//			}
//		} finally {
//			verify(proc, times(5)).executeInterval();
//			verify(proc).postProcess();
//		}
//	}
//
}
