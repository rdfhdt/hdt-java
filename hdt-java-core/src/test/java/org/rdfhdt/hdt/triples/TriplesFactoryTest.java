package org.rdfhdt.hdt.triples;

import org.junit.Test;
import org.rdfhdt.hdt.hdt.HDTVocabulary;
import org.rdfhdt.hdt.options.ControlInformation;
import org.rdfhdt.hdt.options.HDTSpecification;
import org.rdfhdt.hdt.triples.impl.BitmapTriples;
import org.rdfhdt.hdt.triples.impl.TriplesList;

import java.io.IOException;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class TriplesFactoryTest {

	@Test
	public void createTempTriplesReturnsTriplesList() {
		TempTriples t = TriplesFactory.createTempTriples(new HDTSpecification());
		assertTrue(t instanceof TriplesList);
	}

	@Test
	public void createTriplesDefaultsToBitmapWhenFormatUnset() throws IOException {
		TriplesPrivate t = TriplesFactory.createTriples(new HDTSpecification());
		assertTrue(t instanceof BitmapTriples);
	}

	@Test
	public void createTriplesHonoursTriplesListFormat() throws IOException {
		HDTSpecification spec = new HDTSpecification();
		spec.set("triples.format", HDTVocabulary.TRIPLES_TYPE_TRIPLESLIST);
		assertTrue(TriplesFactory.createTriples(spec) instanceof TriplesList);
	}

	@Test
	public void createTriplesHonoursBitmapFormat() throws IOException {
		HDTSpecification spec = new HDTSpecification();
		spec.set("triples.format", HDTVocabulary.TRIPLES_TYPE_BITMAP);
		assertTrue(TriplesFactory.createTriples(spec) instanceof BitmapTriples);
	}

	@Test
	public void createTriplesFallsBackToBitmapForUnknownFormat() throws IOException {
		HDTSpecification spec = new HDTSpecification();
		spec.set("triples.format", "not-a-real-format");
		assertTrue(TriplesFactory.createTriples(spec) instanceof BitmapTriples);
	}

	@Test
	public void createFromControlInfoBitmap() throws IOException {
		ControlInformation ci = new ControlInformation();
		ci.setFormat(HDTVocabulary.TRIPLES_TYPE_BITMAP);
		assertTrue(TriplesFactory.createTriples(ci) instanceof BitmapTriples);
	}

	@Test
	public void createFromControlInfoTriplesList() throws IOException {
		ControlInformation ci = new ControlInformation();
		ci.setFormat(HDTVocabulary.TRIPLES_TYPE_TRIPLESLIST);
		assertTrue(TriplesFactory.createTriples(ci) instanceof TriplesList);
	}

	@Test
	public void createFromControlInfoUnknownFormatThrows() {
		ControlInformation ci = new ControlInformation();
		ci.setFormat("not-a-real-format");
		assertThrows(IllegalArgumentException.class, () -> TriplesFactory.createTriples(ci));
	}
}
