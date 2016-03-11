package edu.wirch.driftbattlelauncher.arduino;

import java.util.Iterator;
import java.util.List;

import android.os.Handler;
import android.test.AndroidTestCase;
import edu.wirch.driftbattlelauncher.bt.BtSender;

public class ProgramExecutor extends AndroidTestCase {
	private final Handler delayHandler = new Handler();
	private final BtSender sender;
	private volatile DelayedProgramExecutor programRunner;

	public static class ProgramLine {
		private final byte[] commands;
		private final long delay;

		public static class Builder {
			private final ByteChainBuilder commands = new ByteChainBuilder();
			private long delay = 0;

			public Builder send(byte[] bytes) {
				commands.add(bytes);
				return this;
			}

			public Builder delay(long delay) {
				this.delay = delay;
				return this;
			}

			public ProgramLine build() {
				return new ProgramLine(commands.build(), delay);
			}
		}

		public ProgramLine(byte[] commands, long delay) {
			this.commands = commands;
			this.delay = delay;
		}

		public byte[] getCommands() {
			return commands;
		}
	}

	public static class Program implements Iterator<ProgramLine> {
		private int current;
		private final List<ProgramLine> programLine;
		private final ProgramLine cancelLine;

		public Program(List<ProgramLine> commands, ProgramLine cancelLine) {
			current = -1;
			this.programLine = commands;
			this.cancelLine = cancelLine;
		}

		@Override
		public boolean hasNext() {
			return current < programLine.size() - 1;
		}

		@Override
		public ProgramLine next() {
			current++;
			if (current >= programLine.size())
				return null;
			return programLine.get(current);
		}

		@Override
		public void remove() {
		}

		public ProgramLine getCancelLine() {
			return cancelLine;
		}

	}

	private class DelayedProgramExecutor implements Runnable {
		private final Program program;

		public DelayedProgramExecutor(Program program) {
			this.program = program;
		}

		@Override
		public void run() {
			if (program.hasNext()) {
				final ProgramLine programLine = program.next();

				sender.sendCommand(programLine.getCommands());

				if (program.hasNext()) {
					// next line
					delayHandler.postDelayed(this, programLine.delay);
				} else {
					programRunner = null;
				}
			}
		}

		public Program getProgram() {
			return program;
		}
	};

	public ProgramExecutor(BtSender sender) {
		this.sender = sender;
	}

	public void execute(Program program) {
		if (programRunner == null) {
			// only when IDLE
			programRunner = new DelayedProgramExecutor(program);
			delayHandler.post(programRunner);
		}
	}

	public void cancel() {
		if (programRunner != null) {
			delayHandler.removeCallbacks(programRunner);
			ProgramLine cancelLine = programRunner.getProgram().getCancelLine();
			programRunner = null;
			if (cancelLine != null) {
				sender.sendCommand(cancelLine.getCommands());
			}
		}
	}
}
