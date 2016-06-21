/**
 *
 */
package info.bunji.asyncutil;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.Assert;
import org.junit.Test;

import info.bunji.asyncutil.AsyncExecutor.Builder;
import rx.schedulers.Schedulers;


/**
 * @author f.kinoshita
 */
public class AsyncExecutorTest extends AsyncTestBase {

	class TestProcessWithException2 extends AsyncProcess<String> {

		@SuppressWarnings("null")
		public TestProcessWithException2() {
			//isInterrupted();
			String nullStr = null;
			nullStr.toString();

			// occur IllegalStateException()
			append("test");
		}

		@Override
		protected void execute() throws Exception {
			// do nothing.
		}
	}

	class TestProcessWithException3 extends AsyncProcess<String> {

		public TestProcessWithException3() {
			isInterrupted();

			// occur IllegalStateException()
			append(Arrays.asList("test1", "test2"));
		}

		@Override
		protected void execute() throws Exception {
			// do nothing.
		}
	}

	@Test
	public void testExecuteWithScheduler() {
		int size = 500;
		AsyncResult<String> results = AsyncExecutor.execute(new TestProcess(size), Schedulers.immediate());
		int count = 0;
		for (@SuppressWarnings("unused") String s : results) count++;

		assertThat(count, is(size));
	}

	@Test
	public void testExecuteWithSchedulerAndCancel()throws IOException {
		int size = 500;
		int  count = 0;
		try (AsyncResult<String> results = AsyncExecutor.execute(new TestProcessWithWait(size), Schedulers.immediate())) {
			for (@SuppressWarnings("unused") String s : results) {
				if (count++ > 50) {
					break;	// interrupt
				}
			}
		}
		assertThat(count, is(not(size)));
	}

	/**
	 * {@link info.bunji.asyncutil.AsyncExecutor#execute(info.bunji.asyncutil.AsyncProcess)} のためのテスト・メソッド。
	 */
	@Test
	public void testExecuteNoLimit() {
		int size = 500;
		AsyncResult<String> results = AsyncExecutor.execute(new TestProcess(size));

		int count = 0;

		for (@SuppressWarnings("unused") String s : results) count++;

		assertThat(count, is(size));
	}

	/**
	 * {@link info.bunji.asyncutil.AsyncExecutor#execute(info.bunji.asyncutil.AsyncProcess, int)} のためのテスト・メソッド。
	 */
	@Test
	public void testExecuteWithLimit() throws Exception {
		int size = 200;
		AsyncResult<String> results = AsyncExecutor.execute(new TestProcess(size), 50);

		int count = 0;
		for (@SuppressWarnings("unused") String s : results) {
			count++;
			Thread.sleep(1);
		}

		assertThat(count, is(size));
	}

	@Test
	public void testExecuteMulti() throws Exception {
		try (AsyncResult<String> results =
				AsyncExecutor.execute(new TestProcess2(AsyncExecutor.execute(new TestProcess(100))), 10)) {
			for (String result : results) {
				logger.debug(result);
				Thread.sleep(2);
			}
		}
	}

	/**
	 * {@link info.bunji.asyncutil.AsyncExecutor#execute(info.bunji.asyncutil.AsyncProcess, int)} のためのテスト・メソッド。
	 */
	@Test
	public void testExecuteBlocking() throws Exception {
		int size = 500;
		List<String> listResult = null;
		try (AsyncResult<String> results = AsyncExecutor.execute(new TestProcess(size))) {
			listResult = results.block();
		}

		assertThat(listResult.size(), is(size));
	}

	@Test(expected=NoSuchElementException.class)
	public void testHasNext() throws Exception {
		int size = 500;
		try (AsyncResult<String> results = AsyncExecutor.execute(new TestProcess(size))) {
			results.block();
			Iterator<String> it = results.iterator();
			it.next();
		}
	}

	@Test
	public void testExecuteCancel() throws Exception {
		int size = 3000;
		AsyncResult<String> results = AsyncExecutor.execute(new TestProcess(size), 99);

		int count = 0;
		for (@SuppressWarnings("unused") String s : results) {
			count++;
			if (count > 200) {
				results.close();
				break;
			}
//			Thread.sleep(2);
		}

		assertThat(count, is(not(size)));
	}

	@Test
	public void testExecuteException() throws Exception {
		int count = 0;
		try (AsyncResult<String> results = AsyncExecutor.execute(new TestProcessWithException())) {
			for (@SuppressWarnings("unused") String s : results) {
				count++;
				Thread.sleep(1);
			}
		} catch (Exception e) {
			assertThat(count, is(not(1000)));
			return;
		}
		Assert.fail("not exception");
	}

	@Test(expected=Exception.class)
	public void testExecuteException2() throws Exception {
		try (AsyncResult<String> results = AsyncExecutor.execute(new TestProcessWithException2())) {
		}
		Assert.fail("not exception");
	}

	@Test(expected=Exception.class)
	public void testExecuteException3() throws Exception {
		try (AsyncResult<String> results = AsyncExecutor.execute(new TestProcessWithException3())) {
		}
		Assert.fail("not exception");
	}

	@Test
	public void testExecuteWait() throws Exception {
		int count = 0;
		try (AsyncResult<String> results = AsyncExecutor.execute(new TestProcessWithWait(100))) {
			for (@SuppressWarnings("unused") String s : results) {
				count++;
//				Thread.sleep(30);
			}
		}
		assertThat(count, is(100));
	}

	@Test
	public void testExecuteParallel() throws Exception {
		List<AsyncProcess<String>> procList = new ArrayList<>();
		procList.add(new TestProcessWithWait(30));
		procList.add(new TestProcessWithWait(30));
		procList.add(new TestProcessWithWait(30));
		procList.add(new TestProcessWithWait(30));
//		procList.add(new TestProcessWithException());

		int count = 0;
		try (AsyncResult<String> results = AsyncExecutor.execute(procList)) {
			for (String result : results) {
				logger.debug(result);
				count++;
			}
		}
		assertThat(count, is(120));
	}

	@Test
	public void testExecuteParallel2() throws Exception {
		List<AsyncProcess<String>> procList = new ArrayList<>();
		procList.add(new TestProcessWithWait(30));
		procList.add(new TestProcessWithWait(30));
		procList.add(new TestProcessWithWait(30));
		procList.add(new TestProcessWithWait(30));
//		procList.add(new TestProcessWithException());

		int count = 0;
		try (AsyncResult<String> results = AsyncExecutor.execute(procList, Schedulers.trampoline())) {
			for (String result : results) {
				logger.debug(result);
				count++;
			}
		}
		assertThat(count, is(120));
	}

	@Test
	public void testExecuteFromBuilder() throws Exception {
		Builder builder = AsyncExecutor.builder()
							.queueLimit(50)
							.maxConcurrent(1)
							.scheduler(Schedulers.immediate());

		int count = 0;
		try (AsyncResult<String> results = builder.execute(new TestProcess(100))) {
			for (@SuppressWarnings("unused") String result : results) {
				count++;
			}
		}
		assertThat(count, is(100));
	}

	@Test
	public void testExecuteFromBuilder2() throws Exception {
		Builder builder = AsyncExecutor.builder();

		int count = 0;
		try (AsyncResult<String> results = builder.execute(new ArrayList<AsyncProcess<String>>())) {
			for (@SuppressWarnings("unused") String result : results) {
				count++;
			}
		}
		assertThat(count, is(0));
	}
}
