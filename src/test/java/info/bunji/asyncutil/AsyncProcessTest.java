/*
 * Copyright 2016-2018 Fumiharu Kinoshita
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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.junit.FixMethodOrder;
import org.junit.Test;

@FixMethodOrder
public class AsyncProcessTest extends AsyncTestBase {

	@Test
	public void testRun() throws Exception {
		int size = 10000;
		IntAsyncProcess proc = new IntAsyncProcess(size);
		try (ClosableResult<Integer> results = proc.run()) {
			assertThat(results.toList().size(), is(size));
		}
	}

	@Test
	public void testRun_withBufSize() throws Exception {
		int size = 2000;
		IntAsyncProcess proc = new IntAsyncProcess(size);
		try (ClosableResult<Integer> results = proc.run(256)) {
			assertThat(results.toList().size(), is(size));
		}
	}

	@Test
	public void testRun_withDelayError() throws Exception {
		int size = 10000;
		IntAsyncProcess proc = new IntAsyncProcess(size);
		try (ClosableResult<Integer> results = proc.run(true)) {
			assertThat(results.toList().size(), is(size));
		}
	}

	@Test(expected = IllegalStateException.class)
	public void testRun_delayErrorTrue() throws Exception {
		int size = 2000;
		IntAsyncProcess proc = new IntAsyncProcess(size).setThrow(512);
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
		IntAsyncProcess proc = new IntAsyncProcess(size).setThrow(512);
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
		IntAsyncProcess proc = new IntAsyncProcess(size);
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

	@Test(expected = IllegalStateException.class)
	public void testRun_interrupt_exception() throws Exception {
		int size = 2000;
		IntAsyncProcess proc = new IntAsyncProcess(size).setThrow(1000);
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

	@Test(expected = NoSuchElementException.class)
	public void testRun_nonElementAccess() throws Exception {
		int size = 100;
		IntAsyncProcess proc = new IntAsyncProcess(size);

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
		IntAsyncProcess proc = new IntAsyncProcess(size);
		try (ClosableResult<Integer> results = proc.run()) {
			results.iterator().next();
			results.iterator().remove();
		}
	}
}
