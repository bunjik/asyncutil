/**
 *
 */
package info.bunji.asyncutil;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.internal.util.reflection.Whitebox;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import rx.Subscriber;

/**
 * @author f.kinoshita
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Subscriber.class})
public class AsyncProcessTest extends AsyncTestBase {

	/**
	 * Test AsynProcess class
	 */
	static class TestAsyncProcess extends AsyncProcess<String> {

		private List<String> items = null;

		public TestAsyncProcess() {
		}

		public TestAsyncProcess(List<String> items) {
			this.items = new ArrayList<String>(items);
		}

		@Override
		protected void execute() throws Exception {
			if (items != null) {
				if (items.size() == 1) {
					append(items.get(0));
				} else {
					append(items);
				}
			}
		}
	}

	/**
	 **********************************
 	 * @throws IOException if an I/O error occurs
	 **********************************
	 */
	@Test
	public void testCall() throws IOException {
		StringProcess1 asyncProc = PowerMockito.spy(new StringProcess1(2));

		try (AsyncResult<String> result = AsyncExecutor.execute(asyncProc)) {
			List<String> resultList = result.block();

			assertThat(resultList.size(), is(2));
		}
		verify(asyncProc, times(1)).postProcess();
	}

	/**
	 **********************************
 	 * @throws Exception if error occurs
	 **********************************
	 */
	@Test
	public void testExecute() throws Exception {
		int size = 1;
		StringProcess1 asyncProc = spy(new StringProcess1(size));

		List<String> result = AsyncExecutor.execute(asyncProc).block();

		assertThat(result.size(), is(size));
		verify(asyncProc, times(1)).execute();
		verify(asyncProc, times(1)).postProcess();
	}

	/**
	 **********************************
 	 * @throws Exception if error occurs
	 **********************************
	 */
	@Test
	public void testExceptionInExecute() throws Exception {
		StringProcess1 asyncProc = spy(new StringProcess1(10, 0));

		try (AsyncResult<String> result = AsyncExecutor.execute(asyncProc)) {
			result.iterator().next();
			fail();
		} catch(Exception e) {
			assertThat(e.getCause().getMessage(), is("Test Exception Occurred."));
		}
		verify(asyncProc, times(1)).execute();
		verify(asyncProc, times(1)).postProcess();
	}

	/**
	 **********************************
 	 * @throws Exception if error occurs
	 **********************************
	 */
	@Test
	public void testAppendCollectionOfT() throws Exception {
		TestAsyncProcess asyncProc = spy(new TestAsyncProcess(Arrays.asList("item1", "item2")));

		List<String> result = AsyncExecutor.execute(asyncProc).block();

		assertThat(result.size(), is(2));
		verify(asyncProc, times(1)).execute();
		verify(asyncProc, times(1)).postProcess();
	}

	/**
	 **********************************
 	 * @throws Exception if error occurs
	 **********************************
	 */
	@Test(expected=ProcessCanceledException.class)
	public void testAppendCollectionOfT2() throws Exception {
		TestAsyncProcess asyncProc = spy(new TestAsyncProcess());

		asyncProc.append(Arrays.asList("item1", "item2"));
	}

	/**
	 **********************************
 	 * @throws Exception if error occurs
	 **********************************
	 */
	@Test(expected=ProcessCanceledException.class)
	public void testAppendCollectionOfT3() throws Exception {
		TestAsyncProcess asyncProc = new TestAsyncProcess();

		@SuppressWarnings("unchecked")
		Subscriber<String> subscriber = PowerMockito.mock(Subscriber.class);
		Whitebox.setInternalState(asyncProc, "subscriber", subscriber);

		// if subscriber.isUnsubscribed() == true
		when(subscriber.isUnsubscribed()).thenReturn(true);

		List<String> items = new ArrayList<>(Arrays.asList("item1", "item2"));
		asyncProc.append(items);
	}

	/**
	 **********************************
 	 * @throws Exception if error occurs
	 **********************************
	 */
	@Test
	public void testAppendCollectionOfT4() throws Exception {
		TestAsyncProcess asyncProc = new TestAsyncProcess();

		@SuppressWarnings("unchecked")
		Subscriber<String> subscriber = PowerMockito.mock(Subscriber.class);
		Whitebox.setInternalState(asyncProc, "subscriber", subscriber);

		// if subscriber.isUnsubscribed() == false
		when(subscriber.isUnsubscribed()).thenReturn(false);

		List<String> items = null;
		try {
			asyncProc.append(items);
		} catch (Exception e) {
			fail();
		}
	}

	/**
	 **********************************
 	 * @throws Exception if error occurs
	 **********************************
	 */
	@Test
	public void testAppendT() throws Exception {
		StringProcess1 asyncProc = spy(new StringProcess1(1));
		try (AsyncResult<String> result = AsyncExecutor.execute(asyncProc)) {
			List<String> list = result.block();
			assertThat(list.size(), is(1));
		}
		verify(asyncProc, times(1)).execute();
		verify(asyncProc, times(1)).postProcess();
	}

	/**
	 **********************************
	 **********************************
	 */
	@Test(expected=ProcessCanceledException.class)
	public void testAppendT2() {
		TestAsyncProcess asyncProc = spy(new TestAsyncProcess());

		asyncProc.append("item1");
	}

	/**
	 **********************************
	 **********************************
	 */
	@Test
	public void testIsInterrupted() {
		StringProcess1 asyncProc = new StringProcess1(1);

		// if subscriber == null
		Whitebox.setInternalState(asyncProc, "subscriber", null);
		assertThat(asyncProc.isInterrupted(), is(false));

		@SuppressWarnings("unchecked")
		Subscriber<String> subscriber = PowerMockito.mock(Subscriber.class);
		Whitebox.setInternalState(asyncProc, "subscriber", subscriber);

		// if subscriber.isUnsubscribed() == true
		when(subscriber.isUnsubscribed()).thenReturn(true);
		assertThat(asyncProc.isInterrupted(), is(true));

		// if subscriber.isUnsubscribed() == false
		when(subscriber.isUnsubscribed()).thenReturn(false);
		assertThat(asyncProc.isInterrupted(), is(false));
	}

	/**
	 **********************************
 	 * @throws IOException if error occurs
	 **********************************
	 */
	@Test
	public void testPostProcess() throws IOException {
		StringProcess1 asyncProc = spy(new StringProcess1(1));
		try (AsyncResult<String> asyncResult = AsyncExecutor.execute(asyncProc)) {
			List<String> result = asyncResult.block();

			assertThat(result.size(), is(1));
		}
		verify(asyncProc, times(1)).postProcess();
	}
}
