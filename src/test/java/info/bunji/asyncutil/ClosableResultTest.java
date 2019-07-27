package info.bunji.asyncutil;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;

import org.junit.FixMethodOrder;
import org.junit.Test;

@FixMethodOrder
public class ClosableResultTest extends AsyncTestBase {

	@SuppressWarnings("deprecation")
	@Test
	public void testRun_asyncProcess() throws Exception {
		int size = 10000;
		IntAsyncProcess proc = new IntAsyncProcess(size);
		try (ClosableResult<Integer> results = new ClosableResult<>(proc)) {
			assertThat(results.toList().size(), is(size));
		}
	}

	@SuppressWarnings("deprecation")
	@Test
	public void testRun_asyncProcess2() throws Exception {
		int size = 10000;
		IntAsyncProcess proc = new IntAsyncProcess(size);
		try (ClosableResult<Integer> results = new ClosableResult<>(proc, 256)) {
			assertThat(results.toList().size(), is(size));
		}
	}

	@SuppressWarnings("deprecation")
	@Test
	public void testRun_asyncProcess3() throws Exception {
		int size = 10000;
		IntAsyncProcess proc = new IntAsyncProcess(size);
		try (ClosableResult<Integer> results = new ClosableResult<>(proc, true)) {
			assertThat(results.toList().size(), is(size));
		}
	}

	@SuppressWarnings("deprecation")
	@Test
	public void testRun_asyncProcess4() throws Exception {
		int size = 10000;
		IntAsyncProcess proc = new IntAsyncProcess(size);
		try (ClosableResult<Integer> results = new ClosableResult<>(proc, 256, true)) {
			assertThat(results.toList().size(), is(size));
		}
	}

	@Test
	public void testExecuteAtion() throws Exception {
		int size = 10000;
		IntExecAction execAction = spy(new IntExecAction(size));
		try (ClosableResult<Integer> results = new ClosableResult<>(execAction)) {
			assertThat(results.toList().size(), is(size));
		} finally {
			verify(execAction, times(1)).execute();
		}
	}

	@Test
	public void testExecuteAtion_withBufSize() throws Exception {
		int size = 10000;
		IntExecAction execAction = spy(new IntExecAction(size));
		try (ClosableResult<Integer> results = new ClosableResult<>(execAction, 256)) {
			assertThat(results.toList().size(), is(size));
		} finally {
			verify(execAction, times(1)).execute();
		}
	}

	@Test
	public void testExecuteAtion_withDelayError() throws Exception {
		int size = 10000;
		IntExecAction execAction = spy(new IntExecAction(size));
		try (ClosableResult<Integer> results = new ClosableResult<>(execAction, true)) {
			assertThat(results.toList().size(), is(size));
		} finally {
			verify(execAction, times(1)).execute();
		}
	}

	@Test
	public void testExecuteAtion_withBufSizeAndDelayError() throws Exception {
		int size = 10000;
		IntExecAction execAction = spy(new IntExecAction(size));
		try (ClosableResult<Integer> results = new ClosableResult<>(execAction, 256, true)) {
			assertThat(results.toList().size(), is(size));
		} finally {
			verify(execAction, times(1)).execute();
		}
	}

	@Test
	public void testPostAtion() throws Exception {
		int size = 10000;
		IntExecAction execAction = spy(new IntExecAction(size));
		TestPostAction postAction = spy(new TestPostAction());
		try (ClosableResult<Integer> results = new ClosableResult<>(execAction, postAction)) {
			assertThat(results.toList().size(), is(size));
		} finally {
			Thread.sleep(100);
			verify(execAction, times(1)).execute();
			verify(postAction, times(1)).execute(any(ExecResult.class));
		}
	}

	@Test
	public void testPostAtion_withBufSize() throws Exception {
		int size = 10000;
		IntExecAction execAction = spy(new IntExecAction(size));
		TestPostAction postAction = spy(new TestPostAction());
		try (ClosableResult<Integer> results = new ClosableResult<>(execAction, postAction, 256)) {
			assertThat(results.toList().size(), is(size));
		} finally {
			Thread.sleep(100);
			verify(execAction, times(1)).execute();
			verify(postAction, times(1)).execute(any(ExecResult.class));
		}
	}

	@Test
	public void testPostAction_withDelayError() throws Exception {
		int size = 10000;
		IntExecAction execAction = spy(new IntExecAction(size));
		TestPostAction postAction = spy(new TestPostAction());
		try (ClosableResult<Integer> results = new ClosableResult<>(execAction, postAction, true)) {
			assertThat(results.toList().size(), is(size));
		} finally {
			Thread.sleep(100);
			verify(execAction, times(1)).execute();
			verify(postAction, times(1)).execute((ExecResult)any());
		}
	}

	@Test
	public void testPostAtion_withBufSizeAndDelayError() throws Exception {
		int size = 10000;
		IntExecAction execAction = spy(new IntExecAction(size));
		TestPostAction postAction = spy(new TestPostAction());
		try (ClosableResult<Integer> results = new ClosableResult<>(execAction, postAction, 256, true)) {
			assertThat(results.toList().size(), is(size));
		} finally {
			verify(execAction, times(1)).execute();
			Thread.sleep(500);
			verify(postAction, times(1)).execute(any(ExecResult.class));
		}
	}

	@Test
	public void testFromItetrable() throws Exception {
		int size = 1000;
		List<Integer> source = new ArrayList<>();
		for (int i = 1; i <= size; i++) {
			source.add(i);
		}
		try (ClosableResult<Integer> results = new ClosableResult<>(source)) {
			assertThat(results.toList().size(), is(size));
		}
	}

	@Test
	public void testFromItetrable_withBufSize() throws Exception {
		int size = 1000;
		List<Integer> source = new ArrayList<>();
		for (int i = 1; i <= size; i++) {
			source.add(i);
		}
		try (ClosableResult<Integer> results = new ClosableResult<>(source, 256)) {
			assertThat(results.toList().size(), is(size));
		}
	}
}
