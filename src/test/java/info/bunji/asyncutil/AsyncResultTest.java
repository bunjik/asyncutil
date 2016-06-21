/**
 *
 */
package info.bunji.asyncutil;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Iterator;
import java.util.List;

import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.internal.util.reflection.Whitebox;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import rx.Observable;
import rx.Subscriber;

/**
 * @author f.kinoshita
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Subscriber.class})
public class AsyncResultTest extends AsyncTestBase {

//	/**
//	 **********************************
//	 * {@link info.bunji.asyncutil.AsyncResult#AsyncResult(rx.Observable, int, rx.Scheduler)} のためのテスト・メソッド。
//	 **********************************
//	 */
//	@Test
//	public void testAsyncResult() {
//		fail("まだ実装されていません");
//	}

	/**
	 **********************************
	 * {@link info.bunji.asyncutil.AsyncResult#block()} のためのテスト・メソッド。
	 **********************************
	 */
	@Test
	public void testBlock() throws Exception {
		try (AsyncResult<String> result = AsyncExecutor.execute(new TestProcess(1000))) {
			List<String> list = result.block();
			assertThat(list.size(), is(1000));
		}
	}

	/**
	 **********************************
	 * {@link info.bunji.asyncutil.AsyncResult#close()} のためのテスト・メソッド。
	 **********************************
	 */
	@Test
	public void testClose() throws Exception {
		AsyncResult<String> asyncResult = spy(AsyncExecutor.execute(new TestProcess(500)));

		@SuppressWarnings("unchecked")
		Subscriber<String> subscriber = (Subscriber<String>) Whitebox.getInternalState(asyncResult, "subscriber");
		subscriber = PowerMockito.spy(subscriber);
		Whitebox.setInternalState(asyncResult, "subscriber", subscriber);
		when(subscriber.isUnsubscribed()).thenReturn(false);

		asyncResult.close();

		verify(asyncResult, times(1)).close();
		// called unsubscribe()
		verify(subscriber, times(1)).unsubscribe();
	}

	/**
	 **********************************
	 * {@link info.bunji.asyncutil.AsyncResult#close()} のためのテスト・メソッド。
	 **********************************
	 */
	@Test
	public void testClose2() throws Exception {
		@SuppressWarnings("unchecked")
		Observable<String> o = PowerMockito.mock(Observable.class);
		AsyncResult<String> asyncResult = spy(new AsyncResult<String>(o, -1));

		@SuppressWarnings("unchecked")
		Subscriber<String> subscriber = PowerMockito.mock(Subscriber.class);
		Whitebox.setInternalState(asyncResult, "subscriber", subscriber);
		when(subscriber.isUnsubscribed()).thenReturn(true);

		asyncResult.close();

		verify(asyncResult, times(1)).close();
		// not  called unsubscribe()
		verify(subscriber, times(0)).unsubscribe();
	}

	/**
	 **********************************
	 * {@link info.bunji.asyncutil.AsyncResult#iterator()} のためのテスト・メソッド。
	 **********************************
	 */
	@Test
	public void testHasNext() throws Exception {
		int count = 0;
		try (AsyncResult<String> result = AsyncExecutor.execute(new TestProcess(1000))) {
			Iterator<String> it = result.iterator();
			while(it.hasNext()) {
				it.next();
				count++;
			}
		}
		assertThat(count, is(1000));
	}

	/**
	 **********************************
	 * {@link info.bunji.asyncutil.AsyncResult#iterator()} のためのテスト・メソッド。
	 **********************************
	 */
	@Test
	public void testNext() throws Exception {
		int count = 0;
		try (AsyncResult<String> result = AsyncExecutor.execute(new TestProcess(100))) {
			Iterator<String> it = result.iterator();
			while(it.hasNext()) {
				count++;
				String item = it.next();
				assertThat(item, CoreMatchers.endsWith("-" + count));
			}
		}
		assertThat(count, is(100));
	}

	/**
	 **********************************
	 * {@link info.bunji.asyncutil.AsyncResult#iterator()} のためのテスト・メソッド。
	 **********************************
	 */
	@Test(expected=UnsupportedOperationException.class)
	public void testRemove() throws Exception {
		try (AsyncResult<String> result = AsyncExecutor.execute(new TestProcess(100))) {
			Iterator<String> it = result.iterator();
			while(it.hasNext()) {
				it.next();
				it.remove();
			}
		}
	}

}
