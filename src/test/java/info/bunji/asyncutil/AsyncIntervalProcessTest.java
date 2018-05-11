package info.bunji.asyncutil;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.spy;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@FixMethodOrder
@RunWith(PowerMockRunner.class)
@PrepareForTest({ClosableResult.class})
public class AsyncIntervalProcessTest extends AsyncTestBase {

	@Test
	public void testExecuteInterval() throws Exception {

		TestIntervalProc proc = spy(new TestIntervalProc(100).setCycleCount(10));
		try (ClosableResult<String> results = proc.run()) {
			results.toList();
		} finally {
			verify(proc, times(10)).executeInterval();
			verify(proc).postProcess();
		}
	}

	@Test
	public void testExecuteInterval_interrupt() throws Exception {

		TestIntervalProc proc = spy(new TestIntervalProc(500).setCycleCount(10));
		try (ClosableResult<String> results = proc.run()) {
			// do Nothing.
  			Thread.sleep(1000);
		} finally {
			verify(proc).postProcess();
		}
	}

	@Test(expected=IllegalStateException.class)
	public void testExecuteInterval_interrupt2() throws Exception {

		TestIntervalProc proc = spy(new TestIntervalProc(100).setCycleCount(10).setException(new IllegalStateException(), 5));
		try (ClosableResult<String> results = proc.run(false)) {
			// do Nothing.
			for (String s : results) {
	  			Thread.sleep(100);
			}
		} finally {
			verify(proc, times(5)).executeInterval();
			verify(proc).postProcess();
		}
	}

	@Test(expected=IllegalArgumentException.class)
	public void testExecuteInterval_negativeInterval() throws Exception {
		new TestIntervalProc(-1);
	}
}
