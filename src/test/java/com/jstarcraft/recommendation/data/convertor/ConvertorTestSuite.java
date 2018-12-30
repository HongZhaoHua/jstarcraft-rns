package com.jstarcraft.recommendation.data.convertor;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({

		ArffDataConvertorTestCase.class,

		CsvDataConvertorTestCase.class,

		JsonDataConvertorTestCase.class })
public class ConvertorTestSuite {

}
