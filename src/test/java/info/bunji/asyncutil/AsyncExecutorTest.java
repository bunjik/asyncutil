/**
 *
 */
package info.bunji.asyncutil;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author f.kinoshita
 */
public class AsyncExecutorTest {

	private Logger logger = LoggerFactory.getLogger(getClass());

	@Rule
	public TestName name = new TestName();

	@Before
	public void setup() {
		logger.info("=== start " + name.getMethodName() + "() ===");
	}

	@After
	public void tearDown() {
		logger.info("=== finish " + name.getMethodName() + "() ===");
	}

	class TestProcess extends AsyncProcess<String> {
		private int size;

		public TestProcess(int size) {
			this.size = size;;
		}

		@Override
		protected void execute() throws Exception {
			int step = 10;
			List<String> temp = new ArrayList<>();
			for (int i = 0; i < size; i++) {
				temp.add("" + i);
				if (temp.size() == step) {
					append(temp);
				}
			}
			if (!temp.isEmpty()) append(temp);
		}
	}

	class TestProcessWithException extends AsyncProcess<String> {
		@Override
		protected void execute() throws Exception {
			int step = 10;
			List<String> temp = new ArrayList<>();
			for (int i = 0; i < 1000; i++) {
				temp.add("" + i);
				if (temp.size() == step) {
					append(temp);
				}
				if (i == 500) {
					throw new Exception("test");
				}
			}
			if (!temp.isEmpty()) append(temp);
		}
	}

	/**
	 * {@link info.bunji.asyncutil.AsyncExecutor#execute(info.bunji.asyncutil.AsyncProcess)} のためのテスト・メソッド。
	 */
	@Test
	public void testExecuteNoLimit() {
		int size = 500;
		AsyncResult<String> results = AsyncExecutor.execute(new TestProcess(size));

		int count = 0;
		for (String s : results) count++;

		assertThat(count, is(size));
	}

	/**
	 * {@link info.bunji.asyncutil.AsyncExecutor#execute(info.bunji.asyncutil.AsyncProcess, int)} のためのテスト・メソッド。
	 */
	@Test
	public void testExecuteWithLimit() throws Exception {
		int size = 500;
		AsyncResult<String> results = AsyncExecutor.execute(new TestProcess(size), 100);

		int count = 0;
		for (String s : results) {
			count++;
			Thread.sleep(1);
		}

		assertThat(count, is(size));
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
		int size = 500;
		AsyncResult<String> results = AsyncExecutor.execute(new TestProcess(size), 100);

		int count = 0;
		for (String s : results) {
			count++;
			if (count > 300) {
				results.close();
				break;
			}
			Thread.sleep(1);
		}

		assertThat(count, is(not(size)));
	}

	@Test
	public void testExecuteException() throws Exception {
		int count = 0;
		try (AsyncResult<String> results = AsyncExecutor.execute(new TestProcessWithException())) {
			for (String s : results) {
				count++;
				Thread.sleep(1);
			}
		} catch (Exception e) {
			assertThat(count, is(not(1000)));
			return;
		}
		Assert.fail("not exception");
	}

	@Test(expected=UnsupportedOperationException.class)
	public void testRemoveException() throws Exception {
		try (AsyncResult<String> results = AsyncExecutor.execute(new TestProcess(100))) {
			Iterator<String> it = results.iterator();
			it.next();
			it.remove();
		}
	}


}
