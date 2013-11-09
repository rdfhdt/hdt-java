package org.rdfhdt.hdt.header;

import java.io.IOException;
import java.io.InputStream;

import org.rdfhdt.hdt.listener.ProgressListener;
import org.rdfhdt.hdt.options.ControlInfo;

public interface HeaderPrivate extends Header {
	void load(InputStream input, ControlInfo ci, ProgressListener listener) throws IOException;
}
