/*
 * Copyright 2016 Fumiharu Kinoshita
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package info.bunji.asyncutil;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import rx.schedulers.Schedulers;


/**
 * @author f.kinoshita
 */
public class AsyncExecutorTest extends AsyncTestBase {

	/**
	 * test execute default setting.
	 *
	 * @throws IOException if an I/O error occurs
	 */
	@Test
	public void testExecuteDefault() throws IOException {
		int size = 100;
		StringProcess1 proc1 = spy(new StringProcess1(size));

		int count = 0;
		try (AsyncResult<String> results = AsyncExecutor.execute(proc1)) {
			for (@SuppressWarnings("unused") String s : results) {
				count++;
			}
		} finally {
			verify(proc1, times(1)).postProcess();
		}
		assertThat(count, is(size));
	}

	/**
	 * test exectute cancel.
	 *
	 * @throws IOException if an I/O error occurs
	 */
	@Test
	public void testExecuteCancel() throws IOException {
		int size = 500;
		int interruptCnt = 50;
		StringProcess1 proc1 = spy(new StringProcess1(size, 1L));
		//StringProcess1 proc1 = spy(new StringProcess1(size));

		int count = 0;
		try (AsyncResult<String> results = AsyncExecutor.execute(proc1)) {
			for (@SuppressWarnings("unused") String s : results) {
				count++;
				if (interruptCnt == count) {
					// break;
					results.close();
				}
			}
		} catch (Exception e) {
			logger.debug("exception ", e);
		} finally {
			verify(proc1, times(1)).postProcess();
		}
		assertThat(count, greaterThanOrEqualTo(interruptCnt));
		assertThat(count, lessThan(size));
	}

	/**
	 * test execute with scheduler.
	 *
	 * @throws IOException if an I/O error occurs
	 */
	@Test
	public void testExecuteWithScheduler() throws IOException {
		int size = 100;
		StringProcess1 proc1 = spy(new StringProcess1(size));

		int count = 0;
		try (AsyncResult<String> results = AsyncExecutor.execute(proc1, Schedulers.io())) {
			for (@SuppressWarnings("unused") String s : results) {
				count++;
			}
		} finally {
			verify(proc1, times(1)).postProcess();
		}
		assertThat(count, is(size));
	}

	/**
	 * test execute with queueLimit.
	 *
	 * @throws IOException if an I/O error occurs
	 */
	@Test
	public void testExecuteWithQueueLimit() throws IOException {
		int size = 100;
		int queueLimit = 30;
		StringProcess1 proc1 = spy(new StringProcess1(size));

		int count = 0;
		try (AsyncResult<String> results = AsyncExecutor.execute(proc1, queueLimit)) {
			for (@SuppressWarnings("unused") String s : results) {
				count++;
			}
		} finally {
			verify(proc1, times(1)).postProcess();
		}
		assertThat(count, is(size));
	}

	/**
	 * test execute with queueLimit and Scheduler.
	 *
	 * @throws IOException if an I/O error occurs
	 */
	@Test
	public void testExecuteWithQueueLimitAndScheduler() throws IOException {
		int size = 100;
		int queueLimit = 30;
		StringProcess1 proc1 = spy(new StringProcess1(size));

		int count = 0;
		try (AsyncResult<String> results = AsyncExecutor.execute(proc1, queueLimit, Schedulers.io())) {
			for (@SuppressWarnings("unused") String s : results) {
				count++;
			}
		} finally {
			verify(proc1, times(1)).postProcess();
		}
		assertThat(count, is(size));
	}

	/**
	 * test parallel execute default setting.
	 *
	 * @throws IOException if an I/O error occurs
	 */
	@Test
	public void testParallelExecuteDefault() throws IOException {
		int size = 100;
		StringProcess1 proc1 = spy(new StringProcess1(size));
		StringProcess2 proc2 = spy(new StringProcess2(size));

		int count = 0;
		try (AsyncResult<String> results = AsyncExecutor.execute(Arrays.asList(proc1, proc2))) {
			for (@SuppressWarnings("unused") String s : results) {
				count++;
			}
		} finally {
			verify(proc1, times(1)).postProcess();
			verify(proc2, times(1)).postProcess();
		}
		assertThat(count, is(size * 2));
	}

	/**
	 * test parallel exectute cancel.
	 *
	 * @throws IOException if an I/O error occurs
	 */
	@Test
	public void testParallelExecuteCancel() throws IOException {
		int size = 100;
		int interruptCnt = 50;
		StringProcess1 proc1 = spy(new StringProcess1(size, 3L));
		StringProcess2 proc2 = spy(new StringProcess2(size, 3L));

		int count = 0;
		try (AsyncResult<String> results = AsyncExecutor.execute(Arrays.asList(proc1, proc2))) {
			for (@SuppressWarnings("unused") String s : results) {
				count++;
				if (interruptCnt == count) {
					// break;
					results.close();
				}
			}
		} finally {
			verify(proc1, times(1)).postProcess();
			verify(proc2, times(1)).postProcess();
		}
		assertThat(count, greaterThanOrEqualTo(interruptCnt));
		assertThat(count, lessThan(size));
	}

	/**
	 * test parallel exectute cancel.
	 *
	 * @throws IOException if an I/O error occurs
	 */
	@Test
	public void testParallelExecuteCancel2() throws IOException {
		int size = 100;
		int interruptCnt = 10;
		StringProcess1 proc1 = spy(new StringProcess1(size, 10L));
		StringProcess2 proc2 = spy(new StringProcess2(size, 10L));

		int count = 0;
		try (AsyncResult<String> results = AsyncExecutor.execute(Arrays.asList(proc1, proc2), 1)) {
			for (@SuppressWarnings("unused") String s : results) {
				count++;
				if (interruptCnt == count) {
					//break;
					results.close();
				}
			}
		} finally {
			verify(proc1, times(1)).postProcess();
			verify(proc2, times(1)).postProcess();
		}
		assertThat(count, greaterThanOrEqualTo(interruptCnt));
		assertThat(count, lessThan(size));
	}

	/**
	 * test parallel execute with Scheduler.
	 *
	 * @throws IOException if an I/O error occurs
	 */
	@Test
	public void testParallelExecuteWithScheduler() throws IOException {
		int size = 100;
		StringProcess1 proc1 = spy(new StringProcess1(size));
		StringProcess2 proc2 = spy(new StringProcess2(size));

		int count = 0;
		try (AsyncResult<String> results = AsyncExecutor.execute(Arrays.asList(proc1, proc2), Schedulers.io())) {
			for (@SuppressWarnings("unused") String s : results) {
				count++;
			}
		} finally {
			verify(proc1, times(1)).postProcess();
			verify(proc2, times(1)).postProcess();
		}
		assertThat(count, is(size * 2));
	}

	/**
	 * test parallel execute with maxConcurrent.
	 *
	 * @throws IOException if an I/O error occurs
	 */
	@Test
	public void testParallelExecuteWithMaxConcurrent() throws IOException {
		int size = 100;
		int maxConcurrent = 1; // sequential execute
		StringProcess1 proc1 = spy(new StringProcess1(size));
		StringProcess2 proc2 = spy(new StringProcess2(size));
		List<StringProcess1> procList = Arrays.asList(proc1, proc2);

		int count = 0;
		int changeCnt = 0;
		try (AsyncResult<String> results = AsyncExecutor.execute(procList, maxConcurrent)) {
			String prevThreadName = "";
			for (String s : results) {
				String curThreadName = s.substring(0, s.lastIndexOf("-"));
				if (!prevThreadName.equals(curThreadName)) {
					// change execute thread name
					changeCnt++;
					prevThreadName = curThreadName;
				}
				//logger.debug(s);
				count++;
			}
		} finally {
			verify(proc1, times(1)).postProcess();
			verify(proc2, times(1)).postProcess();
		}
		assertThat(changeCnt, is(procList.size()));
		assertThat(count, is(size * procList.size()));
	}

	/**
	 * test parallel execute with maxConcurrent.
	 *
	 * @throws IOException if an I/O error occurs
	 */
	@Test
	public void testParallelExecuteWithMaxConcurrent2() throws IOException {
		int size = 100;
		int maxConcurrent = 10; // parallel execute
		StringProcess1 proc1 = spy(new StringProcess1(size, 1L));
		StringProcess2 proc2 = spy(new StringProcess2(size, 1L));
		List<StringProcess1> procList = Arrays.asList(proc1, proc2);

		int count = 0;
		int changeCnt = 0;
		try (AsyncResult<String> results = AsyncExecutor.execute(procList, maxConcurrent)) {
			String prevThreadName = "";
			for (String s : results) {
				String curThreadName = s.substring(0, s.lastIndexOf("-"));
				if (!prevThreadName.equals(curThreadName)) {
					// change execute thread name
					changeCnt++;
					prevThreadName = curThreadName;
				}
				//logger.debug(s);
				count++;
			}
		} finally {
			verify(proc1, times(1)).postProcess();
			verify(proc2, times(1)).postProcess();
		}
		assertThat(changeCnt, greaterThan(procList.size())); // execute parallel(result mixed)
		assertThat(count, is(size * procList.size()));
	}

	/**
	 * test parallel execute with maxConcurrent.(nagative value)
	 *
	 * @throws IOException if an I/O error occurs
	 */
	@Test(expected=IllegalArgumentException.class)
	public void testParallelExecuteWithMaxConcurrent3() throws IOException {
		int size = 100;
		int maxConcurrent = 0; // error argument
		StringProcess1 proc1 = spy(new StringProcess1(size, 1L));
		StringProcess2 proc2 = spy(new StringProcess2(size, 1L));
		List<StringProcess1> procList = Arrays.asList(proc1, proc2);
		try (AsyncResult<String> results = AsyncExecutor.execute(procList, maxConcurrent)) {
			for (@SuppressWarnings("unused") String s : results) {
				// do nothing.
			}
		} finally {
			verify(proc1, times(1)).postProcess();
			verify(proc2, times(1)).postProcess();
		}
	}

	/**
	 * test parallel execute with queueLimit.
	 *
	 * @throws IOException if an I/O error occurs
	 */
	@Test
	public void testParallelExecuteWithQueueLimit() throws IOException {
		int size = 100;
		int queueLimit = 30;
		StringProcess1 proc1 = spy(new StringProcess1(size));
		StringProcess2 proc2 = spy(new StringProcess2(size));

		int count = 0;
		try (AsyncResult<String> results = AsyncExecutor.execute(Arrays.asList(proc1, proc2), 2, queueLimit)) {
			for (@SuppressWarnings("unused") String s : results) {
				count++;
			}
		} finally {
			verify(proc1, times(1)).postProcess();
			verify(proc2, times(1)).postProcess();
		}
		assertThat(count, is(size * 2));
	}

	/**
	 * test chain async execute.
	 *
	 * @throws Exception if error occurs
	 */
	@Test
	public void testExecuteChain() throws Exception {
		int size = 100;
		int count = 0;
		IntegerProcess proc1 = null;
		ChainProcess   proc2 = null;
		try {
			proc1 = spy(new IntegerProcess(size, 1L));
			proc2 = spy(new ChainProcess(AsyncExecutor.execute(proc1)));
			try (AsyncResult<String> results = AsyncExecutor.execute(proc2)) {
				for (@SuppressWarnings("unused") String result : results) {
					count++;
				}
			}
		} finally {
			verify(proc1, times(1)).execute();
			verify(proc1, times(1)).postProcess();
			verify(proc2, times(1)).execute();
			verify(proc2, times(1)).postProcess();
			assertThat(count, is(size));
		}
	}
}
