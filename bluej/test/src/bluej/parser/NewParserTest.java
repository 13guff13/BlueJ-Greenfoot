package bluej.parser;

import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;

import junit.framework.TestCase;

public class NewParserTest extends TestCase
{
	/**
	 * Test array as type parameter
	 */
	public void test1()
	{
		StringReader sr = new StringReader(
				"LinkedList<String[]>"
		);
		InfoParser ip = new InfoParser(sr);
		List ll = new LinkedList();
		assertTrue(ip.parseTypeSpec(false, ll));
		// 6 tokens: LinkedList, '<', String, '[', ']', '>'
		assertEquals(6, ll.size());
	}
	
	/**
	 * Test handling of '>>' sequence in type spec
	 */
	public void test2()
	{
		StringReader sr = new StringReader(
				"LinkedList<List<String[]>>"
		);
		InfoParser ip = new InfoParser(sr);
		List ll = new LinkedList();
		assertTrue(ip.parseTypeSpec(false, ll));
		// 8 tokens: LinkedList, '<', List, '<', String, '[', ']', '>>'
		assertEquals(8, ll.size());
	}
	
	/**
	 * Test multiple type parameters
	 */
	public void test3()
	{
		StringReader sr = new StringReader(
				"Map<String,Integer> v1; "
		);
		InfoParser ip = new InfoParser(sr);
		ip.parseStatement();
	}
	
	/**
	 * Test generic inner class of generic outer class
	 */
	public void test4()
	{
		StringReader sr = new StringReader(
				"Outer<String>.Inner<String> v8; "
		);
		InfoParser ip = new InfoParser(sr);
		ip.parseStatement();
	}
	
	/**
	 * Test wildcard type parameters
	 */
	public void test5()
	{
		StringReader sr = new StringReader(
				"A<?> v8; " +
				"A<? extends String> v9; " +
				"A<? super String> v10;"
		);
		InfoParser ip = new InfoParser(sr);
		ip.parseStatement();
		ip.parseStatement();
		ip.parseStatement();
	}
	
	/**
	 * Test less-than operator.
	 */
	public void test6()
	{
		StringReader sr = new StringReader(
				"b = (i < j);"
		);
		InfoParser ip = new InfoParser(sr);
		ip.parseStatement();
	}
	
	/**
	 * Test a funky statement.
	 */
	public void test7()
	{
		StringReader sr = new StringReader(
				"boolean.class.equals(T.class);"
		);
		InfoParser ip = new InfoParser(sr);
		ip.parseStatement();
	}
	
	/**
	 * Test a class declaration with a single type parameter.
	 */
	public void test8()
	{
		StringReader sr = new StringReader(
				"class A<T>{}"
		);
		InfoParser ip = new InfoParser(sr);
		ip.parseTypeDef();
	}
	
	/**
	 * Test a class declaration containing a semi-colon
	 */
	public void test9()
	{
		StringReader sr = new StringReader(
				"class A{;}"
		);
		InfoParser ip = new InfoParser(sr);
		ip.parseTypeDef();
	}
	
	/**
	 * Test a simple enum
	 */
	public void test10()
	{
		StringReader sr = new StringReader(
				"enum A {" +
				"    one, two, three;" +
				"    private int x;" +
				"}"
		);
		InfoParser ip = new InfoParser(sr);
		ip.parseTypeDef();
	}
}
