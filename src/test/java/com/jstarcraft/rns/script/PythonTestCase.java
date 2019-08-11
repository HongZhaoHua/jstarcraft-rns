package com.jstarcraft.rns.script;

import java.io.File;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.jstarcraft.ai.evaluate.Evaluator;
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
import com.jstarcraft.core.utility.Configurator;
import com.jstarcraft.core.utility.StringUtility;
import com.jstarcraft.rns.model.benchmark.RandomGuessModel;
import com.jstarcraft.rns.task.RankingTask;
import com.jstarcraft.rns.task.RatingTask;

import it.unimi.dsi.fastutil.objects.Object2FloatSortedMap;

public class PythonTestCase {

    private static final ClassLoader loader = PythonTestCase.class.getClassLoader();

    private static final String script;

    static {
        try {
            File file = new File(PythonTestCase.class.getResource("Model.py").toURI());
            script = FileUtils.readFileToString(file, StringUtility.CHARSET);
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    @BeforeClass
    public static void setProperty() {
        System.setProperty("python.console.encoding", StringUtility.CHARSET.name());
    }

    @Test
    public void testRanking() throws Exception {
        ScriptContext context = new ScriptContext();
        context.useClasses(Properties.class, Configurator.class, RankingTask.class, RatingTask.class, RandomGuessModel.class);
        ScriptScope scope = new ScriptScope();
        scope.createAttribute("loader", loader);
        scope.createAttribute("type", "ranking");
        ScriptExpression expression = new PythonExpression(context, scope, script);

        Object2FloatSortedMap<Class<? extends Evaluator>> measures = expression.doWith(Object2FloatSortedMap.class);
        Assert.assertEquals(0.5205948F, measures.get(AUCEvaluator.class), 0F);
        Assert.assertEquals(0.007114561F, measures.get(MAPEvaluator.class), 0F);
        Assert.assertEquals(0.023391832F, measures.get(MRREvaluator.class), 0F);
        Assert.assertEquals(0.012065685F, measures.get(NDCGEvaluator.class), 0F);
        Assert.assertEquals(91.31491F, measures.get(NoveltyEvaluator.class), 0F);
        Assert.assertEquals(0.005825241F, measures.get(PrecisionEvaluator.class), 0F);
        Assert.assertEquals(0.011579763F, measures.get(RecallEvaluator.class), 0F);
    }

    @Test
    public void testRating() throws Exception {
        ScriptContext context = new ScriptContext();
        context.useClasses(Properties.class, Configurator.class, RankingTask.class, RatingTask.class, RandomGuessModel.class);
        ScriptScope scope = new ScriptScope();
        scope.createAttribute("loader", loader);
        scope.createAttribute("type", "rating");
        ScriptExpression expression = new PythonExpression(context, scope, script);

        Object2FloatSortedMap<Class<? extends Evaluator>> measures = expression.doWith(Object2FloatSortedMap.class);
        Assert.assertEquals(1.2708743F, measures.get(MAEEvaluator.class), 0F);
        Assert.assertEquals(0.9947887F, measures.get(MPEEvaluator.class), 0F);
        Assert.assertEquals(2.425075F, measures.get(MSEEvaluator.class), 0F);
    }

}
