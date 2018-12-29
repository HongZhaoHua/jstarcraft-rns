package com.jstarcraft.recommendation.utility;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class UnitNumber implements Comparable<UnitNumber> {

	public final static double ZERO = 0D;

	public final static double ONE = 1D;

	public final static double KILO = 1000D;

	private static int ignore = 5;

	/** 单位(1代表千位,2代表百万位,3代表十亿位,以此类推) */
	private int unit;

	/** 值 */
	private double number;

	public UnitNumber() {
	}

	public UnitNumber(int unit, double number) {
		if (!Double.isFinite(number)) {
			throw new IllegalArgumentException();
		}
		if (number != ZERO) {
			this.unit = unit;
			this.number = number;
			this.adjust();
		}
	}

	private void adjust() {
		if (this.number == ZERO) {
			this.unit = 0;
			return;
		}
		while (Math.abs(this.number) < ONE) {
			this.unit--;
			this.number *= KILO;
		}
		while (Math.abs(this.number) >= KILO) {
			this.unit++;
			this.number /= KILO;
		}
	}

	public UnitNumber add(UnitNumber that) {
		int leftUnit = this.unit;
		double leftNumber = this.number;
		int rightUnit = that.unit;
		double rightNumber = that.number;

		if (leftNumber == 0D) {
			this.unit = rightUnit;
			this.number = rightNumber;
			return this;
		}
		if (rightNumber == 0D) {
			return this;
		}

		if (Math.abs(leftUnit - rightUnit) >= ignore) {
			if (leftUnit > rightUnit) {
				return this;
			} else {
				this.unit = rightUnit;
				this.number = rightNumber;
				return this;
			}
		} else {
			int unit = leftUnit > rightUnit ? leftUnit : rightUnit;
			// 加法必须在相同基准运算
			leftNumber = leftNumber * Math.pow(KILO, (leftUnit - unit));
			rightNumber = rightNumber * Math.pow(KILO, (rightUnit - unit));
			double number = leftNumber + rightNumber;
			this.unit = unit;
			this.number = number;
			this.adjust();
			return this;
		}
	}

	public UnitNumber subtract(UnitNumber that) {
		int leftUnit = this.unit;
		double leftNumber = this.number;
		int rightUnit = that.unit;
		double rightNumber = that.number;

		if (leftNumber == 0D) {
			this.unit = rightUnit;
			this.number = -rightNumber;
			return this;
		}
		if (rightNumber == 0D) {
			return this;
		}

		if (Math.abs(leftUnit - rightUnit) >= ignore) {
			if (leftUnit > rightUnit) {
				return this;
			} else {
				this.unit = rightUnit;
				this.number = -rightNumber;
				return this;
			}
		} else {
			int unit = leftUnit > rightUnit ? leftUnit : rightUnit;
			// 减法必须在相同基准运算
			leftNumber = leftNumber * Math.pow(KILO, (leftUnit - unit));
			rightNumber = rightNumber * Math.pow(KILO, (rightUnit - unit));
			double number = leftNumber - rightNumber;
			this.unit = unit;
			this.number = number;
			this.adjust();
			return this;
		}
	}

	public UnitNumber multiply(UnitNumber that) {
		int leftUnit = this.unit;
		double leftNumber = this.number;
		int rightUnit = that.unit;
		double rightNumber = that.number;

		if (leftNumber == ZERO) {
			return this;
		}
		if (rightNumber == ZERO) {
			this.number = ZERO;
			this.adjust();
			return this;
		}

		this.unit = leftUnit + rightUnit;
		this.number = leftNumber * rightNumber;
		this.adjust();
		return this;
	}

	public UnitNumber divide(UnitNumber that) {
		int leftUnit = this.unit;
		double leftNumber = this.number;
		int rightUnit = that.unit;
		double rightNumber = that.number;

		if (leftNumber == ZERO) {
			return this;
		}
		if (rightNumber == ZERO) {
			throw new ArithmeticException();
		}

		this.unit = leftUnit - rightUnit;
		this.number = leftNumber / rightNumber;
		this.adjust();
		return this;
	}

	public int getUnit() {
		return unit;
	}

	public double getNumber() {
		return number;
	}

	@Override
	public int compareTo(UnitNumber that) {
		if (this == that) {
			return 0;
		}

		if (this.number == 0D || that.number == 0D) {
			if (this.number < that.number) {
				return -1;
			} else if (this.number > that.number) {
				return 1;
			} else {
				return 0;
			}
		}

		if (this.unit < that.unit) {
			return -1;
		}
		if (this.unit > that.unit) {
			return 1;
		}

		if (this.number < that.number) {
			return -1;
		} else if (this.number > that.number) {
			return 1;
		} else {
			return 0;
		}
	}

	@Override
	public boolean equals(Object object) {
		if (this == object)
			return true;
		if (object == null)
			return false;
		if (getClass() != object.getClass())
			return false;
		UnitNumber that = (UnitNumber) object;
		EqualsBuilder equal = new EqualsBuilder();
		equal.append(this.unit, that.unit);
		equal.append(this.number, that.number);
		return equal.isEquals();
	}

	@Override
	public int hashCode() {
		HashCodeBuilder hash = new HashCodeBuilder();
		hash.append(unit);
		hash.append(number);
		return hash.toHashCode();
	}

	@Override
	public String toString() {
		return "[unit=" + this.unit + ",value=" + this.number + "]";
	}

	public static UnitNumber instaceOfAdd(UnitNumber left, UnitNumber right) {
		int leftUnit = left.unit;
		double leftValue = left.number;
		int rightUnit = right.unit;
		double rightValue = right.number;

		if (leftValue == 0D) {
			return right;
		}
		if (rightValue == 0D) {
			return left;
		}

		if (Math.abs(leftUnit - rightUnit) >= ignore) {
			if (leftUnit > rightUnit) {
				return left;
			} else {
				return right;
			}
		} else {
			int unit = leftUnit > rightUnit ? leftUnit : rightUnit;
			// 加法必须在相同基准运算
			leftValue = leftValue * Math.pow(KILO, (leftUnit - unit));
			rightValue = rightValue * Math.pow(KILO, (rightUnit - unit));
			double value = leftValue + rightValue;
			UnitNumber instance = new UnitNumber(unit, value);
			return instance;
		}
	}

	public static UnitNumber instaceOfSubtract(UnitNumber left, UnitNumber right) {
		int leftUnit = left.unit;
		double leftValue = left.number;
		int rightUnit = right.unit;
		double rightValue = right.number;

		if (leftValue == 0D) {
			return new UnitNumber(rightUnit, -rightValue);
		}
		if (rightValue == 0D) {
			return left;
		}

		if (Math.abs(leftUnit - rightUnit) >= ignore) {
			if (leftUnit > rightUnit) {
				return left;
			} else {
				return new UnitNumber(rightUnit, -rightValue);
			}
		} else {
			int unit = leftUnit > rightUnit ? leftUnit : rightUnit;
			// 减法必须在相同基准运算
			leftValue = leftValue * Math.pow(KILO, (leftUnit - unit));
			rightValue = rightValue * Math.pow(KILO, (rightUnit - unit));
			double value = leftValue - rightValue;
			UnitNumber instance = new UnitNumber(unit, value);
			return instance;
		}
	}

	public static UnitNumber instaceOfMultiply(UnitNumber left, UnitNumber right) {
		int leftUnit = left.unit;
		double leftValue = left.number;
		int rightUnit = right.unit;
		double rightValue = right.number;

		if (leftValue == 0D) {
			return new UnitNumber();
		}
		if (rightValue == 0D) {
			return new UnitNumber();
		}

		double value = leftValue * rightValue;
		UnitNumber instance = new UnitNumber(leftUnit + rightUnit, value);
		return instance;
	}

	public static UnitNumber instaceOfDivide(UnitNumber left, UnitNumber right) {
		int leftUnit = left.unit;
		double leftValue = left.number;
		int rightUnit = right.unit;
		double rightValue = right.number;

		if (leftValue == 0D) {
			return new UnitNumber();
		}
		if (rightValue == 0D) {
			throw new ArithmeticException();
		}

		double value = leftValue / rightValue;
		UnitNumber instance = new UnitNumber(leftUnit - rightUnit, value);
		return instance;
	}

	public static void setIgnore(int ignore) {
		assert ignore >= 1 && ignore <= 100;
		UnitNumber.ignore = ignore;
	}

	public static int getIgnore() {
		return UnitNumber.ignore;
	}

}
