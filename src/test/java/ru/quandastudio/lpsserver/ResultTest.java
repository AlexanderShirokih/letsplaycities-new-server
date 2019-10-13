package ru.quandastudio.lpsserver;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

public class ResultTest {

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testSuccess() {
		assertFalse(Result.success("test").hasErrors());
	}

	@Test
	public void testFail() {
		assertTrue(Result.error(new IllegalStateException("test")).hasErrors());
	}

	@Test
	public void testSuccessFromOptional() {
		assertFalse(Result.from(Optional.of("test"), "no errors").hasErrors());
	}

	@Test
	public void testFailFromOptional() {
		assertTrue(Result.from(Optional.ofNullable(null), "has errors").hasErrors());
	}

	@Test
	public void testFailFromEmptyOptional() {
		assertTrue(Result.from(Optional.empty(), "has errors").hasErrors());
	}

	@Test
	public void testGetError() {
		assertTrue(Result.from(Optional.empty(), "has errors").getError().getMessage().equals("has errors"));
	}

	@Test
	public void testGetData() {
		assertTrue(Result.success("test").getData().equals("test"));
	}
}
