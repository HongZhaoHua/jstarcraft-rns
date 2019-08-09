package com.jstarcraft.rns.script;

import java.io.File;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.jstarcraft.ai.evaluate.ranking.AUCEvaluator;
import com.jstarcraft.ai.evaluate.ranking.MAPEvaluator;
import com.jstarcraft.ai.evaluate.ranking.MRREvaluator;
import com.jstarcraft.ai.evaluate.ranking.NDCGEvaluator;
import com.jstarcraft.ai.evaluate.ranking.NoveltyEvaluator;
import com.jstarcraft.ai.evaluate.ranking.PrecisionEvaluator;
import com.jstarcraft.ai.evaluate.ranking.RecallEvaluator;
import com.jstarcraft.ai.evaluate.rating.MAEEvaluator;
import com.jstarcraft.ai.evaluate.rating.MPEEvaluator;
import com.jstarcraft.ai.evaluate.rating.MSEEvaluator;
import com.jstarcraft.core.script.PythonExpression;
import com.jstarcraft.core.script.ScriptContext;
import com.jstarcraft.core.script.ScriptExpression;
import com.jstarcraft.core.script.ScriptScope;
import com.jstarcraft.core.utility.StringUtility;

public class PythonTestCase {

    @BeforeClass
    public static void setProperty() {
        System.setProperty("python.console.encoding", StringUtility.CHARSET.name());
    }

    @Test
    public void testRanking() throws Exception {
        Properties keyValues = new Properties();
        keyValues.load(this.getClass().getResourceAsStream("/data.properties"));
        keyValues.load(this.getClass().getResourceAsStream("/recommend/benchmark/randomguess-test.properties"));

        File file = new File(this.getClass().getResource("Recommend.py").toURI());
        String script = FileUtils.readFileToString(file, StringUtility.CHARSET);
        ScriptContext context = new ScriptContext();

        ScriptScope scope = new ScriptScope();
        scope.createAttribute("keyValues", new Properties[] { keyValues });
        scope.createAttribute("type", "ranking");
        ScriptExpression expression = new PythonExpression(context, scope, script);

        Map<String, Float> measures = expression.doWith(Map.class);
        Assert.assertThat(measures.get(AUCEvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.5205948F));
        Assert.assertThat(measures.get(MAPEvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.007114561F));
        Assert.assertThat(measures.get(MRREvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.023391832F));
        Assert.assertThat(measures.get(NDCGEvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.012065685F));
        Assert.assertThat(measures.get(NoveltyEvaluator.class.getSimpleName()), CoreMatchers.equalTo(91.31491F));
        Assert.assertThat(measures.get(PrecisionEvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.005825241F));
        Assert.assertThat(measures.get(RecallEvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.011579763F));
    }
    
    @Test
    public void testRating() throws Exception {
        Properties keyValues = new Properties();
        keyValues.load(this.getClass().getResourceAsStream("/data.properties"));
        keyValues.load(this.getClass().getResourceAsStream("/recommend/benchmark/randomguess-test.properties"));

        File file = new File(this.getClass().getResource("Recommend.py").toURI());
        String script = FileUtils.readFileToString(file, StringUtility.CHARSET);
        ScriptContext context = new ScriptContext();

        ScriptScope scope = new ScriptScope();
        scope.createAttribute("keyValues", new Properties[] { keyValues });
        scope.createAttribute("type", "rating");
        ScriptExpression expression = new PythonExpression(context, scope, script);

        Map<String, Float> measures = expression.doWith(Map.class);
        Assert.assertThat(measures.get(MAEEvaluator.class.getSimpleName()), CoreMatchers.equalTo(1.2708743F));
        Assert.assertThat(measures.get(MPEEvaluator.class.getSimpleName()), CoreMatchers.equalTo(0.9947887F));
        Assert.assertThat(measures.get(MSEEvaluator.class.getSimpleName()), CoreMatchers.equalTo(2.425075F));
    }

}
