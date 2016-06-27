package org.rdfhdt.hdt.header;

import org.rdfhdt.hdt.exceptions.NotFoundException;
import org.rdfhdt.hdt.hdt.HDTVocabulary;
import org.rdfhdt.hdt.triples.IteratorTripleString;
import org.rdfhdt.hdt.triples.TripleString;

public class HeaderUtil {
	
	private HeaderUtil() {}
	
	public static String cleanURI(CharSequence str) {
		String uri = str.toString();
		if(uri!=null && uri.length()>=2 && uri.charAt(0)=='<' && uri.charAt(uri.length()-1)=='>') {
			return uri.substring(1, uri.length()-1);
		}
		return uri;
	}
	
	private static String cleanLiteral(String uri) {
		if(uri!=null && uri.length()>=2 && uri.charAt(0)=='"' && uri.charAt(uri.length()-1)=='"') {
			return uri.substring(1, uri.length()-1);
		}
		return uri;
	}
	
	public static String getProperty(Header header, String subject, String predicate) throws NotFoundException {
		IteratorTripleString it = header.search(cleanURI(subject), cleanURI(predicate), "");
        if(it.hasNext()) {
                TripleString ts = it.next();
                return ts.getObject().toString();
        }
        throw new NotFoundException();
	}
	
	public static int getPropertyInt(Header header, String subject, String predicate) throws NotFoundException {
		String str = HeaderUtil.getProperty(header, subject, predicate);
		if(str!=null) {
			try {
				return Integer.parseInt(str);
			} catch(NumberFormatException e) {
				
			}
		}
		throw new NotFoundException();
	}
	
	public static long getPropertyLong(Header header, String subject, String predicate) throws NotFoundException {
		String str = HeaderUtil.getProperty(header, subject, predicate);
		if(str!=null) {
			try {
				return Long.parseLong(cleanLiteral(str));
			} catch(NumberFormatException e) {
				
			}
		}
		throw new NotFoundException();
	}
	
	public static String getSubject(Header header, String predicate, String object) throws NotFoundException {
		IteratorTripleString it = header.search("", predicate, object);
        if(it.hasNext()) {
                TripleString ts = it.next();
                return ts.getObject().toString();
        }
        throw new NotFoundException();
	}
	
	public static String getBaseURI(Header header) throws NotFoundException {
        return HeaderUtil.getSubject(header, HDTVocabulary.RDF_TYPE, HDTVocabulary.HDT_DATASET);
}
}
