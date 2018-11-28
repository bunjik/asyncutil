package info.bunji.asyncutil;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.junit.FixMethodOrder;
import org.junit.Test;

import info.bunji.asyncutil.functions.ListenerFunc;

@FixMethodOrder
public class AsyncProcessTest extends AsyncTestBase {

	@Test
	public void testRun() throws Exception {
		int size = 1920;
		AsyncProcess<Integer> proc = AsyncProcess.create(new TestIntFunc(size));
		try (ClosableResult<Integer> results = proc.run()) {
			int cnt = 0;
			for (int n : results) {
				if ((n % 200) == 0) logger.debug("read {}", n);
				cnt++;
			}
			assertThat(cnt, is(size));
		}
	}

	@Test
	public void testRun2() throws Exception {
		int size = 1920;
		AsyncProcess<Integer> proc = AsyncProcess.create(new TestIntFunc(size));
		try (ClosableResult<Integer> results = proc.run()) {
			int cnt = 0;
			for (int n : results) {
				if ((n % 200) == 0) logger.debug("read {}", n);
				cnt++;
			}
			assertThat(cnt, is(size));
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void testRun_nullFunc() throws Exception {
		AsyncProcess<Integer> proc = AsyncProcess.create(null);
		try (ClosableResult<Integer> results = proc.run()) {
		}
	}

	@Test
	public void testRun_withBufSize() throws Exception {
		int size = 2000;
		AsyncProcess<Integer> proc = AsyncProcess.create(new TestIntFunc(size));
		try (ClosableResult<Integer> results = proc.run(256)) {
			int cnt = 0;
			for (int n : results) {
				if ((n % 50) == 0) logger.debug("read {}", n);
				cnt++;
			}
			assertThat(cnt, is(size));
		}
	}

	@Test(expected = IllegalStateException.class)
	public void testRun_delayErrorTrue() throws Exception {
		int size = 2000;
		AsyncProcess<Integer> proc = AsyncProcess.create(new TestIntFunc(size).setException(512));
		int cnt = 0;
		try (ClosableResult<Integer> results = proc.run(1000, true)) {
			for (int n : results) {
				if ((n % 200) == 0) logger.debug("read {}", n);
				cnt++;
			}
		} catch (Throwable t) {
			logger.error("error [{}]", t.getMessage());
			throw t;
		} finally {
			logger.debug("processed {}", cnt);
			assertThat(cnt, equalTo(512));
		}
	}

	@Test(expected = IllegalStateException.class)
	public void testRun_delayErrorFalse() throws Exception {
		int size = 2000;
		AsyncProcess<Integer> proc = AsyncProcess.create(new TestIntFunc(size).setException(512));
		int cnt = 0;
		try (ClosableResult<Integer> results = proc.run(false)) {
			for (int n : results) {
				if ((n % 200) == 0) logger.debug("read {}", n);
				cnt++;
			}
		} finally {
			logger.debug("processed {}", cnt);
			assertThat(cnt, lessThanOrEqualTo(512));
		}
	}

	@Test
	public void testRun_interrupt_close() throws Exception {
		int size = 2000;
		AsyncProcess<Integer> proc = AsyncProcess.create(new TestIntFunc(size));
		try (ClosableResult<Integer> results = proc.run(256)) {
			int cnt = 0;
			for (int n : results) {
				if ((n % 100) == 0) logger.debug("read {}", n);
				// close in loop
				if (n == 1000) results.close();
				cnt++;
			}
			assertThat(cnt, greaterThanOrEqualTo(1000));
			assertThat(cnt, lessThan(size));
		}
	}

	@Test
	public void testRun_interrupt_dispose() throws Exception {
		int size = 2000;
		AsyncProcess<Integer> proc = AsyncProcess.create(new TestIntFunc(size));
		try (ClosableResult<Integer> results = proc.run(256)) {
			int cnt = 0;
			for (int n : results) {
				if ((n % 100) == 0) logger.debug("read {}", n);
				// close in loop
				if (n == 1000) proc.dispose();
				cnt++;
			}
			assertThat(cnt, greaterThanOrEqualTo(1000));
			assertThat(cnt, lessThan(size));
		}
	}

	@Test(expected = IllegalStateException.class)
	public void testRun_interrupt_exception() throws Exception {
		int size = 2000;
		AsyncProcess<Integer> proc = AsyncProcess.create(new TestIntFunc(size).setException(1000));
		try (ClosableResult<Integer> results = proc.run(256)) {
			int cnt = 0;
			for (int n : results) {
				if ((n % 100) == 0) logger.debug("read {}", n);
				cnt++;
			}
			assertThat(cnt, greaterThanOrEqualTo(1000));
			assertThat(cnt, lessThan(size));
		}
	}

	@Test
	public void testRun_startListener() throws Exception {
		int size = 1000;
		ListenerFunc startFunc = spy(new TestListenerFunc("call startListener."));
		AsyncProcess<Integer> proc = AsyncProcess.create(new TestIntFunc(size)).doStart(startFunc);
		try (ClosableResult<Integer> results = proc.run()) {
			assertThat(results.toList().size(), is(size));
			verify(startFunc, times(1)).run();
		}
	}

	@Test
	public void testRun_finishListener() throws Exception {
		int size = 1000;
		TestListenerFunc finishFunc = spy(new TestListenerFunc("call finishListener."));
		AsyncProcess<Integer> proc = AsyncProcess.create(new TestIntFunc(size)).doFinish(finishFunc);
		try (ClosableResult<Integer> results = proc.run()) {
			assertThat(results.toList().size(), is(size));
		} finally {
			Thread.sleep(100);
			verify(finishFunc, times(1)).run();
		}
	}

	@Test(expected = NoSuchElementException.class)
	public void testRun_nonElementAccess() throws Exception {
		int size = 100;
		AsyncProcess<Integer> proc = AsyncProcess.create(new TestIntFunc(size));

		try (ClosableResult<Integer> results = proc.run()) {
			assertThat(results.toList().size(), is(size));

			Iterator<Integer> it = results.iterator();
			assertThat(it.hasNext(), is(false));
			it.next(); // NoSuchElementException
		}
	}

	@Test(expected = UnsupportedOperationException.class)
	public void testRun_iteratorRemove() throws Exception {
		int size = 100;
		AsyncProcess<Integer> proc = AsyncProcess.create(new TestIntFunc(size));
		try (ClosableResult<Integer> results = proc.run()) {
			results.iterator().next();
			results.iterator().remove();
		}
	}

	@Test(expected = IllegalStateException.class)
	public void testSetExecute_dup() throws Exception {
		int size = 100;
		AsyncProcess<Integer> proc = AsyncProcess.create(new TestIntFunc(size));
		proc.setExecute(new TestIntFunc(size));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testSetExecute_null() throws Exception {
		AsyncProcess<Integer> proc = new AsyncProcess<>();
		proc.setExecute(null);
	}
}
