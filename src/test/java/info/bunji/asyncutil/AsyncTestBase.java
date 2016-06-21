/**
 *
 */
package info.bunji.asyncutil;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author f.kinoshita
 *
 */
public abstract class AsyncTestBase {

	protected Logger logger = LoggerFactory.getLogger(getClass());

	@Rule
	public TestName name = new TestName();

	@Before
	public void setup() {
		logger.info("=== test start " + name.getMethodName() + "() ===");
	}

	@After
	public void tearDown() {
		logger.info("=== test finish " + name.getMethodName() + "() ===");
	}

	class TestProcess extends AsyncProcess<String> {
		private int size;

		public TestProcess(int size) {
			this.size = size;
		}

		@Override
		protected void execute() throws Exception {
			int step = 10;
			List<String> temp = new ArrayList<>();
			for (int i = 1; i <= size; i++) {
				temp.add(Thread.currentThread().getName() + "-" + i);
				if (temp.size() == step) {
					append(temp);
				}
				if (isInterrupted()) break;
			}
			if (!temp.isEmpty()) append(temp);
		}
	}

	class TestProcessInt extends AsyncProcess<Integer> {
		private int size;

		public TestProcessInt(int size) {
			this.size = size;
		}

		@Override
		protected void execute() throws Exception {
			int step = 10;
			List<Integer> temp = new ArrayList<>();
			for (int i = 1; i <= size; i++) {
				temp.add(i);
				if (temp.size() == step) {
					append(temp);
				}
				if (isInterrupted()) break;
			}
			if (!temp.isEmpty()) append(temp);
		}
	}


	class TestProcess2 extends AsyncProcess<String> {

		private Iterable<String> it;

		public TestProcess2(Iterable<String> it) {
			this.it = it;
		}

		@Override
		protected void execute() throws Exception {
			int step = 10;
			List<String> temp = new ArrayList<>();
			for (String item : it) {
				temp.add("test_" + item);
				if (temp.size() == step) {
					append(temp);
				}
				if (isInterrupted()) break;
			}
			if (!temp.isEmpty()) append(temp);
		}
	}

	class TestProcessWithWait extends AsyncProcess<String> {

		private int size;

		public TestProcessWithWait(int size) {
			this.size = size;
		}

		@Override
		protected void execute() throws Exception {
			for (int i = 1; i <= size; i++) {
				//String s = Thread.currentThread().getName() + "-" +  i;
				//logger.debug("add " + s);
				append(Thread.currentThread().getName() + "-" +  i);
				try {
					Thread.sleep(30);
				} catch(Exception e) {}
				if (isInterrupted()) break;
			}
		}
	}

	class TestProcessWithException extends AsyncProcess<String> {
		@Override
		protected void execute() throws Exception {
			for (int i = 0; i < 1000; i++) {
				append(Thread.currentThread().getName() + "-" +  i);
				if (i == 500) {
					throw new Exception("test");
				}
				if (isInterrupted()) break;
			}
		}
	}
}
