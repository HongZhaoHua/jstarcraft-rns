package com.jstarcraft.rns.script;

import java.io.File;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;
import org.luaj.vm2.LuaTable;

import com.jstarcraft.ai.evaluate.ranking.PrecisionEvaluator;
import com.jstarcraft.ai.evaluate.ranking.RecallEvaluator;
import com.jstarcraft.ai.evaluate.rating.MAEEvaluator;
import com.jstarcraft.ai.evaluate.rating.MSEEvaluator;
import com.jstarcraft.core.script.JsExpression;
import com.jstarcraft.core.script.LuaExpression;
import com.jstarcraft.core.script.PythonExpression;
import com.jstarcraft.core.script.ScriptContext;
import com.jstarcraft.core.script.ScriptExpression;
import com.jstarcraft.core.script.ScriptScope;
import com.jstarcraft.core.utility.Configurator;
import com.jstarcraft.core.utility.StringUtility;
import com.jstarcraft.rns.model.benchmark.RandomGuessModel;
import com.jstarcraft.rns.task.RankingTask;
import com.jstarcraft.rns.task.RatingTask;

public class ScriptTestCase {

    private static final ClassLoader loader = ScriptTestCase.class.getClassLoader();

    /**
     * 使用JS脚本与JStarCraft框架交互
     * 
     * @throws Exception
     */
    @Test
    public void testJs() throws Exception {
        // 获取JS脚本
        File file = new File(ScriptTestCase.class.getResource("Model.js").toURI());
        String script = FileUtils.readFileToString(file, StringUtility.CHARSET);

        // 设置JS脚本使用到的Java类
        ScriptContext context = new ScriptContext();
        context.useClasses(Properties.class, Configurator.class);
        context.useClasses(RankingTask.class, RatingTask.class, RandomGuessModel.class);
        context.useClasses(Assert.class, PrecisionEvaluator.class, RecallEvaluator.class, MAEEvaluator.class, MSEEvaluator.class);
        // 设置JS脚本使用到的Java变量
        ScriptScope scope = new ScriptScope();
        scope.createAttribute("loader", loader);

        // 执行JS脚本
        ScriptExpression expression = new JsExpression(context, scope, script);
        Map<String, Float> data = expression.doWith(Map.class);
        Assert.assertEquals(0.005825241F, data.get("precision"), 0F);
        Assert.assertEquals(0.011579763F, data.get("recall"), 0F);
        Assert.assertEquals(1.2708743F, data.get("mae"), 0F);
        Assert.assertEquals(2.425075F, data.get("mse"), 0F);
    }
    
    /**
     * 使用Lua脚本与JStarCraft框架交互
     * 
     * @throws Exception
     */
    @Test
    public void testLua() throws Exception {
        // 获取Lua脚本
        File file = new File(ScriptTestCase.class.getResource("Model.lua").toURI());
        String script = FileUtils.readFileToString(file, StringUtility.CHARSET);

        // 设置Lua脚本使用到的Java类
        ScriptContext context = new ScriptContext();
        context.useClasses(Properties.class, Configurator.class);
        context.useClasses(RankingTask.class, RatingTask.class, RandomGuessModel.class);
        context.useClasses(Assert.class, PrecisionEvaluator.class, RecallEvaluator.class, MAEEvaluator.class, MSEEvaluator.class);
        // 设置Lua脚本使用到的Java变量
        ScriptScope scope = new ScriptScope();
        scope.createAttribute("loader", loader);

        // 执行Lua脚本
        ScriptExpression expression = new LuaExpression(context, scope, script);
        LuaTable data = expression.doWith(LuaTable.class);
        Assert.assertEquals(0.005825241F, data.get("precision").tofloat(), 0F);
        Assert.assertEquals(0.011579763F, data.get("recall").tofloat(), 0F);
        Assert.assertEquals(1.2708743F, data.get("mae").tofloat(), 0F);
        Assert.assertEquals(2.425075F, data.get("mse").tofloat(), 0F);
    }

    /**
     * 使用Python脚本与JStarCraft框架交互
     * 
     * @throws Exception
     */
    @Test
    public void testPython() throws Exception {
        // 设置Python环境变量
        System.setProperty("python.console.encoding", StringUtility.CHARSET.name());

        // 获取Python脚本
        File file = new File(ScriptTestCase.class.getResource("Model.py").toURI());
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
