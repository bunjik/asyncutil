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
	 * test parallel execute default setting.
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

//	/**
//	 *
//	 * @throws IOException
//	 */
//	@Test
//	public void testExecuteCancel()throws IOException {
//		int size = 100;
//		int count = 0;
//		int cancelCnt = 10;
//		StringProcess proc = new StringProcess(size, 5L);
//		try (AsyncResult<String> results = AsyncExecutor.execute(proc)) {
//			for (@SuppressWarnings("unused") String s : results) {
//				count++;
//				if (count == cancelCnt) {
//					//break;
//					results.close();
//				}
//			}
//		}
//
//		assertThat(count, greaterThanOrEqualTo(cancelCnt));
//		assertThat(count, lessThan(size));
//	}

//	@Test
//	public void testExecuteWithSchedulerAndCancel()throws IOException {
//		int size = 500;
//		int  count = 0;
//		try (AsyncResult<String> results = AsyncExecutor.execute(new TestProcessWithWait(size), Schedulers.immediate())) {
//			for (@SuppressWarnings("unused") String s : results) {
//				if (count++ > 50) {
//					break;	// interrupt
//				}
//			}
//		}
//		assertThat(count, is(not(size)));
//	}
//
//	/**
//	 * {@link info.bunji.asyncutil.AsyncExecutor#execute(info.bunji.asyncutil.AsyncProcess)} のためのテスト・メソッド。
//	 */
//	@Test
//	public void testExecuteNoLimit() {
//		int size = 500;
//		AsyncResult<String> results = AsyncExecutor.execute(new TestProcess(size));
//
//		int count = 0;
//
//		for (@SuppressWarnings("unused") String s : results) count++;
//
//		assertThat(count, is(size));
//	}
//
//	/**
//	 * {@link info.bunji.asyncutil.AsyncExecutor#execute(info.bunji.asyncutil.AsyncProcess, int)} のためのテスト・メソッド。
//	 */
//	@Test
//	public void testExecuteWithLimit() throws Exception {
//		int size = 200;
//		AsyncResult<String> results = AsyncExecutor.execute(new TestProcess(size), 50);
//
//		int count = 0;
//		for (@SuppressWarnings("unused") String s : results) {
//			count++;
//			Thread.sleep(1);
//		}
//
//		assertThat(count, is(size));
//	}
//
//	@Test
//	public void testExecuteMulti() throws Exception {
//		try (AsyncResult<String> results =
//				AsyncExecutor.execute(new TestProcess2(AsyncExecutor.execute(new TestProcess(100))), 10)) {
//			for (String result : results) {
//				logger.debug(result);
//				Thread.sleep(2);
//			}
//		}
//	}
//
//	/**
//	 * {@link info.bunji.asyncutil.AsyncExecutor#execute(info.bunji.asyncutil.AsyncProcess, int)} のためのテスト・メソッド。
//	 */
//	@Test
//	public void testExecuteBlocking() throws Exception {
//		int size = 500;
//		List<String> listResult = null;
//		try (AsyncResult<String> results = AsyncExecutor.execute(new TestProcess(size))) {
//			listResult = results.block();
//		}
//
//		assertThat(listResult.size(), is(size));
//	}
//
//	@Test(expected=NoSuchElementException.class)
//	public void testHasNext() throws Exception {
//		int size = 500;
//		try (AsyncResult<String> results = AsyncExecutor.execute(new TestProcess(size))) {
//			results.block();
//			Iterator<String> it = results.iterator();
//			it.next();
//		}
//	}
//
//	@Test
//	public void testExecuteCancel() throws Exception {
//		int size = 3000;
//		AsyncResult<String> results = AsyncExecutor.execute(new TestProcess(size), 99);
//
//		int count = 0;
//		for (@SuppressWarnings("unused") String s : results) {
//			count++;
//			if (count > 200) {
//				results.close();
//				break;
//			}
////			Thread.sleep(2);
//		}
//
//		assertThat(count, is(not(size)));
//	}
//
//	@Test
//	public void testExecuteException() throws Exception {
//		int count = 0;
//		try (AsyncResult<String> results = AsyncExecutor.execute(new TestProcessWithException())) {
//			for (@SuppressWarnings("unused") String s : results) {
//				count++;
//				Thread.sleep(1);
//			}
//		} catch (Exception e) {
//			assertThat(count, is(not(1000)));
//			return;
//		}
//		Assert.fail("not exception");
//	}
//
//	@Test(expected=Exception.class)
//	public void testExecuteException2() throws Exception {
//		try (AsyncResult<String> results = AsyncExecutor.execute(new TestProcessWithException2())) {
//		}
//		Assert.fail("not exception");
//	}
//
//	@Test(expected=Exception.class)
//	public void testExecuteException3() throws Exception {
//		try (AsyncResult<String> results = AsyncExecutor.execute(new TestProcessWithException3())) {
//		}
//		Assert.fail("not exception");
//	}
//
//	@Test
//	public void testExecuteWait() throws Exception {
//		int count = 0;
//		try (AsyncResult<String> results = AsyncExecutor.execute(new TestProcessWithWait(100))) {
//			for (@SuppressWarnings("unused") String s : results) {
//				count++;
////				Thread.sleep(30);
//			}
//		}
//		assertThat(count, is(100));
//	}
//
//	@Test
//	public void testExecuteParallel() throws Exception {
//		List<AsyncProcess<String>> procList = new ArrayList<>();
//		procList.add(new TestProcessWithWait(30));
//		procList.add(new TestProcessWithWait(30));
//		procList.add(new TestProcessWithWait(30));
//		procList.add(new TestProcessWithWait(30));
////		procList.add(new TestProcessWithException());
//
//		int count = 0;
//		try (AsyncResult<String> results = AsyncExecutor.execute(procList)) {
//			for (String result : results) {
//				logger.debug(result);
//				count++;
//			}
//		}
//		assertThat(count, is(120));
//	}
//
//	@Test
//	public void testExecuteParallel2() throws Exception {
//		List<AsyncProcess<String>> procList = new ArrayList<>();
//		procList.add(new TestProcessWithWait(30));
//		procList.add(new TestProcessWithWait(30));
//		procList.add(new TestProcessWithWait(30));
//		procList.add(new TestProcessWithWait(30));
////		procList.add(new TestProcessWithException());
//
//		int count = 0;
//		try (AsyncResult<String> results = AsyncExecutor.execute(procList, Schedulers.trampoline())) {
//			for (String result : results) {
//				logger.debug(result);
//				count++;
//			}
//		}
//		assertThat(count, is(120));
//	}
//
//	@Test
//	public void testExecuteFromBuilder() throws Exception {
//		Builder builder = AsyncExecutor.builder()
//							.queueLimit(50)
//							.maxConcurrent(1)
//							.scheduler(Schedulers.immediate());
//
//		int count = 0;
//		try (AsyncResult<String> results = builder.execute(new TestProcess(100))) {
//			for (@SuppressWarnings("unused") String result : results) {
//				count++;
//			}
//		}
//		assertThat(count, is(100));
//	}
//
//	@Test
//	public void testExecuteFromBuilder2() throws Exception {
//		Builder builder = AsyncExecutor.builder();
//
//		int count = 0;
//		try (AsyncResult<String> results = builder.execute(new ArrayList<AsyncProcess<String>>())) {
//			for (@SuppressWarnings("unused") String result : results) {
//				count++;
//			}
//		}
//		assertThat(count, is(0));
//	}
}
