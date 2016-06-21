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

	//
	class TestProcess extends AsyncProcess<String> {
		private int size;

		public TestProcess(int size) {
			this.size = size;
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



}
