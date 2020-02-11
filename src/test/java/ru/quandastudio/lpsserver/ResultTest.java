package ru.quandastudio.lpsserver;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
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

	@Test
	public void testCheckFromSuccess() {
		assertTrue(Result.success("test").check((String test) -> test.equals("test"), "Should'nt happen").isSuccess());

		assertEquals("Filter failed!",
				Result.success("test").check((String test) -> false, "Filter failed!").getError().getMessage());
	}

	@Test
	public void testCheckFromError() {
		assertEquals("Error!",
				Result.<String>error("Error!").check((String msg) -> true, "Test").getError().getMessage());
		assertEquals("Error!",
				Result.<String>error("Error!").check((String msg) -> false, "Test").getError().getMessage());
	}

	@Test
	public void testMapFromSuccess() {
		assertEquals("123", Result.success(123).map((Integer i) -> i.toString()).getData());
	}

	@Test
	public void testMapFromError() {
		assertEquals("Error!",
				Result.<Integer>error("Error!").map((Integer i) -> i.toString()).getError().getMessage());
	}

	@Test
	public void testFlatMapFromSuccess() {
		assertEquals("123", Result.success(123).flatMap((Integer i) -> Result.success(String.valueOf(i))).getData());
	}

	@Test
	public void testFlatMapFromError() {
		assertEquals("Error!",
				Result.<Integer>error("Error!")
						.flatMap((Integer i) -> Result.success(String.valueOf(i)))
						.getError()
						.getMessage());
	}

	@Test
	public void testOr() {
		assertEquals("Success", Result.success("Success").or(Result.success("Second")).getData());
		assertEquals("Second", Result.error("Error").or(Result.success("Second")).getData());

		assertEquals("Success", Result.success("Success").or(Result.error("Error")).getData());
		assertEquals("Error!", Result.error("Error").or(Result.error("Error!")).getError().getMessage());
	}

	@Test
	public void testGetOr() {
		assertEquals("Success", Result.success("Success").getOr("Default"));
		assertEquals("Default", Result.error("Error").getOr("Default"));
	}

}
