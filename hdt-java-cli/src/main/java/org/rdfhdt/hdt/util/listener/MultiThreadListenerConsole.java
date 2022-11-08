package org.rdfhdt.hdt.util.listener;

import java.util.Map;
import java.util.TreeMap;

import org.rdfhdt.hdt.listener.MultiThreadListener;

public class MultiThreadListenerConsole implements MultiThreadListener {
	private static final String ERASE_LINE = "\r\033[K";

	private static String goBackNLine(int line) {
		return "\033[" + line + "A";
	}

	/**
	 * true if the system allow ascii sequence, false otherwise
	 */
	private static final boolean ALLOW_ASCII_SEQUENCE;

	static {
		String env;
		try {
			env = System.getenv("TERM");
		} catch (SecurityException e) {
			env = null;
		}

		ALLOW_ASCII_SEQUENCE = System.console() != null && !(env == null || env.isEmpty());
	}

	private final Map<String, String> threadMessages;
	private final boolean color;
	private int previous;

	public MultiThreadListenerConsole(boolean color) {
		this(color, ALLOW_ASCII_SEQUENCE);
	}

	public MultiThreadListenerConsole(boolean color, boolean asciiListener) {
		this.color = color;
		if (asciiListener) {
			threadMessages = new TreeMap<>();
		} else {
			threadMessages = null;
		}
	}

	public String color(int r, int g, int b) {
		if (!color) {
			return "";
		}
		int color = 16 + 36 * r + 6 * g + b;
		return "\033[38;5;" + color + "m";
	}

	public String colorReset() {
		return color ? "\033[0m" : "";
	}

	public String colorThread() {
		return color(5, 1, 1);
	}

	public String colorPercentage() {
		return color(1, 1, 5);
	}

	@Override
	public synchronized void unregisterAllThreads() {
		if (threadMessages == null) {
			return;
		}
		threadMessages.clear();
		notifyProgress(0, "-");
	}

	@Override
	public synchronized void registerThread(String threadName) {
		notifyProgress(threadName, 0, "-");
	}

	@Override
	public synchronized void unregisterThread(String threadName) {
		if (threadMessages == null) {
			return;
		}
		threadMessages.remove(threadName);
		render();
	}

	@Override
	public synchronized void notifyProgress(String thread, float level, String message) {
		String msg = colorPercentage() + "[" + level + "] " + colorReset() + message;
		if (threadMessages != null) {
			threadMessages.put(thread, msg);
			render();
		} else {
			System.out.println(colorThread() + "[" + thread + "]" + colorReset() + msg);
		}
	}

	private void render() {
		if (threadMessages == null) {
			return;
		}
		StringBuilder message = new StringBuilder();
		int lines = threadMessages.size();
		message.append("\r");
		// go back each line of the thread message
		if (previous != 0) {
			message.append(goBackNLine(previous));
		}
		// write each thread logs
		threadMessages.forEach((thread, msg) -> message
				.append(ERASE_LINE)
				.append(colorThread()).append("[").append(thread).append("]")
				.append(msg).append("\n"));
		// remove previous printing
		int toRemove = previous - lines;
		if (toRemove > 0) {
			message.append((ERASE_LINE + "\n").repeat(toRemove)).append(goBackNLine(toRemove));
		}
		previous = lines;

		System.out.print(message);
	}
}
