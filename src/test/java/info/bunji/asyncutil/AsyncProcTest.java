package info.bunji.asyncutil;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.FixMethodOrder;
import org.junit.Test;

import info.bunji.asyncutil.functions.ExecResult;
import info.bunji.asyncutil.functions.PostFunc;

@FixMethodOrder
public class AsyncProcTest extends AsyncTestBase {

	@Test(expected = IllegalArgumentException.class)
	public void testExecFunc_Null() throws Exception {
		new AsyncProc<>().setExecFunc(null);
	}

	@Test
	public void testSetExecFunc() throws Exception {
		int size = 10000;
		IntExecAction execFunc = spy(new IntExecAction(size));
		AsyncProc<Integer> proc = new AsyncProc<>(execFunc);
		try (ClosableResult<Integer> results = proc.run()) {
			assertThat(results.toList().size(), is(size));
		} finally {
			verify(execFunc, times(1)).execute();
		}
	}

	@Test(expected = IllegalStateException.class)
	public void testExecFunc_Dup() throws Exception {
		IntExecAction execFunc = new IntExecAction(10000);
		AsyncProc<Integer> proc = new AsyncProc<Integer>().setExecFunc(execFunc);
		proc.setExecFunc(execFunc);
	}

	//@Test(expected = IllegalArgumentException.class)
	@Test
	public void testPostFunc_Null() throws Exception {
		new AsyncProc<Integer>().setPostFunc(null);
	}

	@Test
	public void testRun_noArgs() throws Exception {
		int size = 10000;
		IntExecAction execFunc = spy(new IntExecAction(size));
		AsyncProc<Integer> proc = new AsyncProc<Integer>().setExecFunc(execFunc);
		try (ClosableResult<Integer> results = proc.run()) {
			assertThat(results.toList().size(), is(size));
		} finally {
			verify(execFunc, times(1)).execute();
		}
	}

	@Test
	public void testRun_withPostPostfunc() throws Exception {
		int size = 10000;
		IntExecAction execFunc = spy(new IntExecAction(size));
		PostFunc postFunc = spy(new TestPostAction());
		AsyncProc<Integer> proc = new AsyncProc<Integer>().setExecFunc(execFunc).setPostFunc(postFunc);
		try (ClosableResult<Integer> results = proc.run()) {
			assertThat(results.toList().size(), is(size));
		} finally {
			verify(execFunc, times(1)).execute();
			verify(postFunc, times(1)).execute(any(ExecResult.class));
		}
	}

	@Test
	public void testRun_withBufSize() throws Exception {
		int size = 10000;
		IntExecAction execFunc = spy(new IntExecAction(size));
		AsyncProc<Integer> proc = new AsyncProc<Integer>().setExecFunc(execFunc);
		try (ClosableResult<Integer> results = proc.run(512)) {
			assertThat(results.toList().size(), is(size));
		} finally {
			verify(execFunc, times(1)).execute();
		}
	}

	@Test
	public void testRun_withBufSizeAndDelay() throws Exception {
		int size = 10000;
		IntExecAction execFunc = spy(new IntExecAction(size));
		AsyncProc<Integer> proc = new AsyncProc<Integer>().setExecFunc(execFunc);
		try (ClosableResult<Integer> results = proc.run(512, true)) {
			assertThat(results.toList().size(), is(size));
		} finally {
			verify(execFunc, times(1)).execute();
		}
	}

	@Test
	public void testRun_interrupt() throws Exception {
		IntExecAction execFunc = spy(new IntExecAction(10000));
		PostFunc postFunc = spy(new TestPostAction());
		AsyncProc<Integer> proc = new AsyncProc<Integer>().setExecFunc(execFunc).setPostFunc(postFunc);
		try (ClosableResult<Integer> results = proc.run()) {
			for (int n : results) {
				if ((n % 1000) == 0) logger.debug("read {}", n);
				if (n > 1000) break;
			}
		} finally {
			verify(execFunc, times(1)).execute();
			verify(postFunc, times(1)).execute(any(ExecResult.class));
		}
	}

	@Test(expected = IllegalStateException.class)
	public void testRun_exception_noDelay() throws Exception {
		IntExecAction execFunc = spy(new IntExecAction(1000).setThrow(500));
		AsyncProc<Integer> proc = new AsyncProc<Integer>().setExecFunc(execFunc);
		int cnt = 0;
		try (ClosableResult<Integer> results = proc.run(false)) {
			for (int n : results) {
				if ((n % 100) == 0) {
					logger.debug("read {}", n);
				}
				Thread.sleep(2);
				cnt++;
			}
		} catch (Throwable t) {
			logger.error(t.getMessage(), t);
			throw t;
		} finally {
			assertThat(cnt, lessThan(500));
			verify(execFunc, times(1)).execute();
		}
	}

	@Test(expected = IllegalStateException.class)
	public void testRun_exception_delay() throws Exception {
		IntExecAction execFunc = spy(new IntExecAction(1000).setThrow(500));
		AsyncProc<Integer> proc = new AsyncProc<Integer>().setExecFunc(execFunc);
		int cnt = 0;
		try (ClosableResult<Integer> results = proc.run(true)) {
			for (int n : results) {
				if ((n % 100) == 0) {
					logger.debug("read {}", n);
				}
				Thread.sleep(2);
				cnt++;
			}
		} finally {
			assertThat(cnt, is(500));
			verify(execFunc, times(1)).execute();
		}
	}

	@Test
	public void test_exceptionInPostFunc() throws Exception {
		int size = 1000;
		IntExecAction execFunc = spy(new IntExecAction(size));
		PostFunc postFunc = spy(new PostFunc() {
			@Override
			public void execute(ExecResult result) {
				throw new IllegalStateException("exception in postFunc.");
			}
		});
		AsyncProc<Integer> proc = new AsyncProc<Integer>().setExecFunc(execFunc).setPostFunc(postFunc);
		try (ClosableResult<Integer> results = new ClosableResult<>(proc)) {
			assertThat(results.toList().size(), is(size));
		} finally {
			Thread.sleep(500);
			verify(execFunc, times(1)).execute();
			verify(postFunc, times(1)).execute(any(ExecResult.class));
		}
	}
}
