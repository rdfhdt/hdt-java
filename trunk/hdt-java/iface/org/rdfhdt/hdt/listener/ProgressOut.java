package org.rdfhdt.hdt.listener;

public class ProgressOut implements ProgressListener {

	private static ProgressListener instance;
	public static ProgressListener getInstance() {
		if(instance==null) {
			instance = new ProgressOut();
		}
		return instance;
	}
	
	@Override
	public void notifyProgress(float level, String message) {
		System.out.println(message+" "+level);
	}

}
