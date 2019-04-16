package info.bunji.asyncutil;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class ExecResultTest extends AsyncTestBase {

	@Test
	public void testExecResultLongLong() {
		long count = 1000;
		long execTime = 123;
		ExecResult result = new ExecResult(count, execTime);

		assertThat(result.getExecTime(), is(execTime));
		assertThat(result.getProcessed(), is(count));
		assertThat(result.getException(), is(nullValue()));
		assertThat(result.isSuccess(), is(true));
	}

	@Test
	public void testExecResultLongLongThrowable() {
		long count = 1000;
		long execTime = 123;
		Throwable ex = new NullPointerException();
		ExecResult result = new ExecResult(count, execTime, ex);

		assertThat(result.getExecTime(), is(execTime));
		assertThat(result.getProcessed(), is(count));
		assertThat(result.getException(), is(ex));
		assertThat(result.isSuccess(), is(false));
	}
}
