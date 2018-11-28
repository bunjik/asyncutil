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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Arrays;

import org.junit.FixMethodOrder;
import org.junit.Test;

/**
 *
 * @author f.kinoshita
 */
@FixMethodOrder
public class AbstractAsyncProcessTest extends AsyncTestBase {

	@Test
	public void testRun() throws Exception {
		final int size = 1000;
		AbstractAsyncProcess<Integer> proc = new AbstractAsyncProcess<Integer>() {
			@Override
			protected void execute() throws Exception {
				for (int i = 1; i <= size; ) {
					append((Iterable<Integer>) null); // do nothing.
					append(Arrays.asList(i++, i++));
				}
			}
		};
		try (ClosableResult<Integer> results = proc.run()) {
			assertThat(results.toList().size(), is(size));
		}
	}

	@Test
	public void testListener() throws Exception {
		final int size = 1000;
		AbstractAsyncProcess<Integer> proc = new AbstractAsyncProcess<Integer>() {
			@Override
			protected void execute() throws Exception {
				for (int i = 1; i <= size; i++) {
					append(i);
				}
			}
		};
		TestListener listener = spy(new TestListener());
		proc.addListener(listener);

		try (ClosableResult<Integer> results = proc.run()) {
			assertThat(results.toList().size(), is(size));
		} finally {
			verify(listener, times(1)).onStart();
			verify(listener, times(1)).onFinish();
		}
	}
}
