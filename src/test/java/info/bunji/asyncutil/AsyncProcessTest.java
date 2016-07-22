/**
 *
 */
package info.bunji.asyncutil;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

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
	 * {@link info.bunji.asyncutil.AsyncProcess#call(rx.Subscriber)} のためのテスト・メソッド。
	 **********************************
	 */
	@Test
	public void testCall() throws Exception {
		StringProcess1 asyncProc = PowerMockito.spy(new StringProcess1(2));

		try (AsyncResult<String> result = AsyncExecutor.execute(asyncProc)) {
			List<String> resultList = result.block();

			assertThat(resultList.size(), is(2));
		}
		verify(asyncProc, times(1)).postProcess();
	}

	/**
	 **********************************
	 * {@link info.bunji.asyncutil.AsyncProcess#execute()} のためのテスト・メソッド。
	 **********************************
	 */
	@Test
	public void testExecute() throws Exception {
		TestAsyncProcess asyncProc = spy(new TestAsyncProcess(Arrays.asList("item1")));

		List<String> result = AsyncExecutor.execute(asyncProc).block();

		assertThat(result.size(), is(1));
		verify(asyncProc, times(1)).execute();
		verify(asyncProc, times(1)).postProcess();
	}

	/**
	 **********************************
	 * {@link info.bunji.asyncutil.AsyncProcess#execute()} のためのテスト・メソッド。
	 **********************************
	 */
	@Test
	public void testExceptionInExecute() throws Exception {
		StringProcess1 asyncProc = spy(new StringProcess1(10));

		doThrow(new Exception("TestException")).when(asyncProc).execute();

		try (AsyncResult<String> result = AsyncExecutor.execute(asyncProc)) {
			result.iterator().next();
			fail();
		} catch(Exception e) {
			assertThat(e.getCause().getMessage(), is("TestException"));
		}

		//verify(asyncProc, times(1)).execute();
		verify(asyncProc, times(1)).postProcess();
	}

	/**
	 **********************************
	 * {@link info.bunji.asyncutil.AsyncProcess#append(java.util.Collection)} のためのテスト・メソッド。
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
	 * {@link info.bunji.asyncutil.AsyncProcess#append(java.util.Collection)} のためのテスト・メソッド。
	 **********************************
	 */
	@Test(expected=ProcessCanceledException.class)
	public void testAppendCollectionOfT2() throws Exception {
		TestAsyncProcess asyncProc = spy(new TestAsyncProcess());

		asyncProc.append(Arrays.asList("item1", "item2"));
	}

	/**
	 **********************************
	 * {@link info.bunji.asyncutil.AsyncProcess#append(java.util.Collection)} のためのテスト・メソッド。
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
	 * {@link info.bunji.asyncutil.AsyncProcess#append(java.util.Collection)} のためのテスト・メソッド。
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
	 * {@link info.bunji.asyncutil.AsyncProcess#append(java.lang.Object)} のためのテスト・メソッド。
	 **********************************
	 */
	@Test
	public void testAppendT() throws Exception {
		TestAsyncProcess asyncProc = spy(new TestAsyncProcess(Arrays.asList("item1")));

		List<String> result = AsyncExecutor.execute(asyncProc).block();

		assertThat(result.size(), is(1));
		verify(asyncProc, times(1)).execute();
		verify(asyncProc, times(1)).postProcess();
	}

	/**
	 **********************************
	 * {@link info.bunji.asyncutil.AsyncProcess#append(java.lang.Object)} のためのテスト・メソッド。
	 **********************************
	 */
	@Test(expected=ProcessCanceledException.class)
	public void testAppendT2() throws Exception {
		TestAsyncProcess asyncProc = spy(new TestAsyncProcess());

		asyncProc.append("item1");
	}

	/**
	 **********************************
	 * {@link info.bunji.asyncutil.AsyncProcess#isInterrupted()} のためのテスト・メソッド。
	 **********************************
	 */
	@Test
	public void testIsInterrupted() {
		TestAsyncProcess asyncProc = new TestAsyncProcess(Arrays.asList("item1"));

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
	 * {@link info.bunji.asyncutil.AsyncProcess#postProcess()} のためのテスト・メソッド。
	 **********************************
	 */
	@Test
	public void testPostProcess() throws Exception {
		StringProcess1 asyncProc = spy(new StringProcess1(1));
		try (AsyncResult<String> asyncResult = AsyncExecutor.execute(asyncProc)) {
			List<String> result = asyncResult.block();

			assertThat(result.size(), is(1));
		}
		verify(asyncProc, times(1)).postProcess();
	}
}
