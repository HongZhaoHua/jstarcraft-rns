package com.jstarcraft.recommendation.utility;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

import com.jstarcraft.recommendation.utility.UnitNumber;

public class UnitNumberTestCase {

	@Test
	public void testIllegalArgument() {
		try {
			new UnitNumber(1, Double.NaN);
			Assert.fail();
		} catch (IllegalArgumentException exception) {
		}
		try {
			new UnitNumber(1, Double.POSITIVE_INFINITY);
			Assert.fail();
		} catch (IllegalArgumentException exception) {
		}
		try {
			new UnitNumber(1, Double.NEGATIVE_INFINITY);
			Assert.fail();
		} catch (IllegalArgumentException exception) {
		}
	}

	@Test
	public void testCompare() {
		{
			UnitNumber left = new UnitNumber(1, 1D);
			UnitNumber right = new UnitNumber();
			Assert.assertThat(left.compareTo(right), CoreMatchers.equalTo(1));
			Assert.assertThat(right.compareTo(left), CoreMatchers.equalTo(-1));
		}

		{
			UnitNumber left = new UnitNumber(1, -1D);
			UnitNumber right = new UnitNumber();
			Assert.assertThat(left.compareTo(right), CoreMatchers.equalTo(-1));
			Assert.assertThat(right.compareTo(left), CoreMatchers.equalTo(1));
		}

		{
			UnitNumber left = new UnitNumber(0, 0D);
			UnitNumber right = new UnitNumber();
			Assert.assertThat(left.compareTo(right), CoreMatchers.equalTo(0));
			Assert.assertThat(right.compareTo(left), CoreMatchers.equalTo(0));
		}

		{
			UnitNumber left = new UnitNumber(1, 1D);
			UnitNumber right = new UnitNumber(1, -1D);
			Assert.assertThat(left.compareTo(right), CoreMatchers.equalTo(1));
			Assert.assertThat(right.compareTo(left), CoreMatchers.equalTo(-1));
		}
	}

	@Test
	public void testAdd() {
		{
			UnitNumber left = new UnitNumber(5, 1D);
			UnitNumber right = new UnitNumber(0, 1D);
			UnitNumber number = UnitNumber.instaceOfAdd(left, right);
			Assert.assertThat(number, CoreMatchers.equalTo(left));
			Assert.assertThat(number, CoreMatchers.equalTo(left.add(right)));
		}

		{
			UnitNumber left = new UnitNumber(0, 1D);
			UnitNumber right = new UnitNumber(5, 1D);
			UnitNumber number = UnitNumber.instaceOfAdd(left, right);
			Assert.assertThat(number, CoreMatchers.equalTo(right));
			Assert.assertThat(number, CoreMatchers.equalTo(left.add(right)));
		}

		{
			UnitNumber left = new UnitNumber(1, 1D);
			UnitNumber right = new UnitNumber();
			UnitNumber number = UnitNumber.instaceOfAdd(left, right);
			Assert.assertThat(number, CoreMatchers.equalTo(left));
			Assert.assertThat(number, CoreMatchers.equalTo(left.add(right)));
		}

		{
			// 相当于1 kilo
			UnitNumber left = new UnitNumber(1, 1D);
			// 相当于0.999 kilo
			UnitNumber right = new UnitNumber(0, 999D);
			UnitNumber number = UnitNumber.instaceOfAdd(left, right);
			Assert.assertThat(number, CoreMatchers.equalTo(new UnitNumber(0, 1999D)));
			Assert.assertThat(number, CoreMatchers.equalTo(left.add(right)));
		}

		{
			// 相当于1 kilo
			UnitNumber left = new UnitNumber(1, 1D);
			// 相当于1 kilo
			UnitNumber right = new UnitNumber(0, 1000D);
			UnitNumber number = UnitNumber.instaceOfAdd(left, right);
			Assert.assertThat(number, CoreMatchers.equalTo(new UnitNumber(1, 2D)));
			Assert.assertThat(number, CoreMatchers.equalTo(left.add(right)));
		}

		{
			// 相当于999.999 kilo
			UnitNumber left = new UnitNumber(0, 999999D);
			// 相当于0.001 kilo
			UnitNumber right = new UnitNumber(0, 1D);
			UnitNumber number = UnitNumber.instaceOfAdd(left, right);
			Assert.assertThat(number, CoreMatchers.equalTo(new UnitNumber(1, 1000D)));
			Assert.assertThat(number, CoreMatchers.equalTo(left.add(right)));
		}
	}

	@Test
	public void testSubtract() {
		{
			UnitNumber left = new UnitNumber(5, 1D);
			UnitNumber right = new UnitNumber(0, 1D);
			UnitNumber number = UnitNumber.instaceOfSubtract(left, right);
			Assert.assertThat(number, CoreMatchers.equalTo(left));
			Assert.assertThat(number, CoreMatchers.equalTo(left.subtract(right)));
		}

		{
			UnitNumber left = new UnitNumber(0, 1D);
			UnitNumber right = new UnitNumber(5, 1D);
			UnitNumber number = UnitNumber.instaceOfSubtract(left, right);
			Assert.assertThat(number, CoreMatchers.equalTo(new UnitNumber(5, -1D)));
			Assert.assertThat(number, CoreMatchers.equalTo(left.subtract(right)));
		}

		{
			UnitNumber left = new UnitNumber(1, 1D);
			UnitNumber right = new UnitNumber();
			UnitNumber number = UnitNumber.instaceOfSubtract(left, right);
			Assert.assertThat(number, CoreMatchers.equalTo(left));
			Assert.assertThat(number, CoreMatchers.equalTo(left.subtract(right)));
		}

		{
			// 相当于1 kilo
			UnitNumber left = new UnitNumber(1, 1D);
			// 相当于0.999 kilo
			UnitNumber right = new UnitNumber(0, 999D);
			UnitNumber number = UnitNumber.instaceOfSubtract(left, right);
			Assert.assertThat(number, CoreMatchers.equalTo(new UnitNumber(0, 1.0000000000000009D)));
			Assert.assertThat(number, CoreMatchers.equalTo(left.subtract(right)));
		}

		{
			// 相当于1 kilo
			UnitNumber left = new UnitNumber(1, 1D);
			// 相当于1 kilo
			UnitNumber right = new UnitNumber(0, 1000D);
			UnitNumber number = UnitNumber.instaceOfSubtract(left, right);
			Assert.assertThat(number, CoreMatchers.equalTo(new UnitNumber()));
			Assert.assertThat(number, CoreMatchers.equalTo(left.subtract(right)));
		}

		{
			// 相当于1000 kilo
			UnitNumber left = new UnitNumber(1, 1000D);
			// 相当于0.001 kilo
			UnitNumber right = new UnitNumber(0, 1);
			UnitNumber number = UnitNumber.instaceOfSubtract(left, right);
			Assert.assertThat(number, CoreMatchers.equalTo(new UnitNumber(0, 999999D)));
			Assert.assertThat(number, CoreMatchers.equalTo(left.subtract(right)));
		}
	}

	@Test
	public void testMultiply() {
		{
			UnitNumber left = new UnitNumber();
			UnitNumber right = new UnitNumber(1, 1D);
			UnitNumber number = UnitNumber.instaceOfMultiply(left, right);
			Assert.assertThat(number, CoreMatchers.equalTo(new UnitNumber()));
			Assert.assertThat(number, CoreMatchers.equalTo(left.multiply(right)));
		}

		{
			UnitNumber left = new UnitNumber(1, 1D);
			UnitNumber right = new UnitNumber();
			UnitNumber number = UnitNumber.instaceOfMultiply(left, right);
			Assert.assertThat(number, CoreMatchers.equalTo(new UnitNumber()));
			Assert.assertThat(number, CoreMatchers.equalTo(left.multiply(right)));
		}

		{
			// 相当于1 kilo
			UnitNumber left = new UnitNumber(1, 1D);
			// 相当于0.5 kilo
			UnitNumber right = new UnitNumber(0, 500D);
			UnitNumber number = UnitNumber.instaceOfMultiply(left, right);
			Assert.assertThat(number, CoreMatchers.equalTo(new UnitNumber(0, 500000D)));
			Assert.assertThat(number, CoreMatchers.equalTo(left.multiply(right)));
		}

		{
			// 相当于1 kilo
			UnitNumber left = new UnitNumber(1, 1D);
			// 相当于2 kilo
			UnitNumber right = new UnitNumber(0, 2000D);
			UnitNumber number = UnitNumber.instaceOfMultiply(left, right);
			Assert.assertThat(number, CoreMatchers.equalTo(new UnitNumber(1, 2000D)));
			Assert.assertThat(number, CoreMatchers.equalTo(left.multiply(right)));
		}

		{
			// 相当于999.999 kilo
			UnitNumber left = new UnitNumber(0, 999999D);
			// 相当于0.001 kilo
			UnitNumber right = new UnitNumber(0, 1D);
			UnitNumber number = UnitNumber.instaceOfMultiply(left, right);
			Assert.assertThat(number, CoreMatchers.equalTo(new UnitNumber(1, 999.999D)));
			Assert.assertThat(number, CoreMatchers.equalTo(left.multiply(right)));
		}
	}

	@Test
	public void testDivide() {
		{
			UnitNumber left = new UnitNumber();
			UnitNumber right = new UnitNumber(1, 1D);
			UnitNumber number = UnitNumber.instaceOfDivide(left, right);
			Assert.assertThat(number, CoreMatchers.equalTo(new UnitNumber()));
			Assert.assertThat(number, CoreMatchers.equalTo(left.divide(right)));
		}

		{
			UnitNumber left = new UnitNumber(1, 1D);
			UnitNumber right = new UnitNumber();
			try {
				UnitNumber.instaceOfDivide(left, right);
				Assert.fail();
			} catch (ArithmeticException exception) {
			}
			try {
				left.divide(right);
				Assert.fail();
			} catch (ArithmeticException exception) {
			}
		}

		{
			// 相当于1 kilo
			UnitNumber left = new UnitNumber(1, 1D);
			// 相当于0.5 kilo
			UnitNumber right = new UnitNumber(0, 500D);
			UnitNumber number = UnitNumber.instaceOfDivide(left, right);
			Assert.assertThat(number, CoreMatchers.equalTo(new UnitNumber(0, 2D)));
			Assert.assertThat(number, CoreMatchers.equalTo(left.divide(right)));
		}

		{
			// 相当于1 kilo
			UnitNumber left = new UnitNumber(1, 1D);
			// 相当于2 kilo
			UnitNumber right = new UnitNumber(0, 2000D);
			UnitNumber number = UnitNumber.instaceOfDivide(left, right);
			Assert.assertThat(number, CoreMatchers.equalTo(new UnitNumber(1, 0.0005D)));
			Assert.assertThat(number, CoreMatchers.equalTo(left.divide(right)));
		}

		{
			// 相当于999.999 kilo
			UnitNumber left = new UnitNumber(0, 999999D);
			// 相当于0.001 kilo
			UnitNumber right = new UnitNumber(0, 1D);
			UnitNumber number = UnitNumber.instaceOfDivide(left, right);
			Assert.assertThat(number, CoreMatchers.equalTo(new UnitNumber(1, 999.999D)));
			Assert.assertThat(number, CoreMatchers.equalTo(left.divide(right)));
		}
	}

}
