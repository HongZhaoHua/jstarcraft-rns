package com.jstarcraft.rns.script;

import java.io.File;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.jstarcraft.ai.evaluate.ranking.PrecisionEvaluator;
import com.jstarcraft.ai.evaluate.ranking.RecallEvaluator;
import com.jstarcraft.ai.evaluate.rating.MAEEvaluator;
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

public class PythonTestCase {

    private static final ClassLoader loader = PythonTestCase.class.getClassLoader();

    @Test
    public void testPython() throws Exception {
        // 设置Python环境变量
        System.setProperty("python.console.encoding", StringUtility.CHARSET.name());

        // 获取Python脚本
        File file = new File(PythonTestCase.class.getResource("Model.py").toURI());
        String script = FileUtils.readFileToString(file, StringUtility.CHARSET);

        // 设置Python脚本使用到的Java类
        ScriptContext context = new ScriptContext();
        context.useClasses(Properties.class, Configurator.class);
        context.useClasses(RankingTask.class, RatingTask.class, RandomGuessModel.class);
        context.useClasses(Assert.class, PrecisionEvaluator.class, RecallEvaluator.class, MAEEvaluator.class, MSEEvaluator.class);
        // 设置Python脚本使用到的Java变量
        ScriptScope scope = new ScriptScope();
        scope.createAttribute("loader", loader);
        
        // 执行Python脚本
        ScriptExpression expression = new PythonExpression(context, scope, script);
        Map<String, Double> data = expression.doWith(Map.class);
        Assert.assertEquals(0.005825241096317768D, data.get("precision"), 0D);
        Assert.assertEquals(0.011579763144254684D, data.get("recall"), 0D);
        Assert.assertEquals(1.270874261856079D, data.get("mae"), 0D);
        Assert.assertEquals(2.425075054168701D, data.get("mse"), 0D);
    }

}
