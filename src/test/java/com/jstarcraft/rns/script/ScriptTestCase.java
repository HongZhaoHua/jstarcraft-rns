package com.jstarcraft.rns.script;

import java.io.File;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;
import org.luaj.vm2.LuaTable;

import com.jstarcraft.core.common.option.MapOption;
import com.jstarcraft.core.script.ScriptContext;
import com.jstarcraft.core.script.ScriptExpression;
import com.jstarcraft.core.script.ScriptScope;
import com.jstarcraft.core.script.groovy.GroovyExpression;
import com.jstarcraft.core.script.js.JsExpression;
import com.jstarcraft.core.script.kotlin.KotlinExpression;
import com.jstarcraft.core.script.lua.LuaExpression;
import com.jstarcraft.core.script.python.PythonExpression;
import com.jstarcraft.core.script.ruby.RubyExpression;
import com.jstarcraft.core.utility.StringUtility;

public class ScriptTestCase {

    private static final ClassLoader loader = ScriptTestCase.class.getClassLoader();

    /**
     * 使用BeanShell脚本与JStarCraft框架交互
     * 
     * @throws Exception
     */
    @Test
    public void testBeanShell() throws Exception {
        // 获取BeanShell脚本
        File file = new File(ScriptTestCase.class.getResource("Model.bsh").toURI());
        String script = FileUtils.readFileToString(file, StringUtility.CHARSET);

        // 设置BeanShell脚本使用到的Java类
        ScriptContext context = new ScriptContext();
        context.useClasses(Properties.class, Assert.class);
        context.useClass("Option", MapOption.class);
        context.useClasses("com.jstarcraft.ai.evaluate");
        context.useClasses("com.jstarcraft.rns.task");
        context.useClasses("com.jstarcraft.rns.model.benchmark");
        // 设置BeanShell脚本使用到的Java变量
        ScriptScope scope = new ScriptScope();
        scope.createAttribute("loader", loader);

        // 执行BeanShell脚本
        ScriptExpression expression = new GroovyExpression(context, scope, script);
        Map<String, Float> data = expression.doWith(Map.class);
        Assert.assertEquals(0.005825241F, data.get("precision"), 0F);
        Assert.assertEquals(0.011579763F, data.get("recall"), 0F);
        Assert.assertEquals(1.2708743F, data.get("mae"), 0F);
        Assert.assertEquals(2.425075F, data.get("mse"), 0F);
    }

    /**
     * 使用Groovy脚本与JStarCraft框架交互
     * 
     * @throws Exception
     */
    @Test
    public void testGroovy() throws Exception {
        // 获取Groovy脚本
        File file = new File(ScriptTestCase.class.getResource("Model.groovy").toURI());
        String script = FileUtils.readFileToString(file, StringUtility.CHARSET);

        // 设置Groovy脚本使用到的Java类
        ScriptContext context = new ScriptContext();
        context.useClasses(Properties.class, Assert.class);
        context.useClass("Option", MapOption.class);
        context.useClasses("com.jstarcraft.ai.evaluate");
        context.useClasses("com.jstarcraft.rns.task");
        context.useClasses("com.jstarcraft.rns.model.benchmark");
        // 设置Groovy脚本使用到的Java变量
        ScriptScope scope = new ScriptScope();
        scope.createAttribute("loader", loader);

        // 执行Groovy脚本
        ScriptExpression expression = new GroovyExpression(context, scope, script);
        Map<String, Float> data = expression.doWith(Map.class);
        Assert.assertEquals(0.005825241F, data.get("precision"), 0F);
        Assert.assertEquals(0.011579763F, data.get("recall"), 0F);
        Assert.assertEquals(1.2708743F, data.get("mae"), 0F);
        Assert.assertEquals(2.425075F, data.get("mse"), 0F);
    }

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
        context.useClasses(Properties.class, Assert.class);
        context.useClass("Option", MapOption.class);
        context.useClasses("com.jstarcraft.ai.evaluate");
        context.useClasses("com.jstarcraft.rns.task");
        context.useClasses("com.jstarcraft.rns.model.benchmark");
        // 设置JS脚本使用到的Java变量
        ScriptScope scope = new ScriptScope();
        scope.createAttribute("loader", loader);

        // 执行JS脚本
        ScriptExpression expression = new JsExpression(context, scope, script);
        Map<String, Float> data = expression.doWith(Map.class);
        Assert.assertEquals(0.005825241096317768F, data.get("precision"), 0F);
        Assert.assertEquals(0.011579763144254684F, data.get("recall"), 0F);
        Assert.assertEquals(1.270874261856079F, data.get("mae"), 0F);
        Assert.assertEquals(2.425075054168701F, data.get("mse"), 0F);
    }
    
    /**
     * 使用Kotlin脚本与JStarCraft框架交互
     * 
     * @throws Exception
     */
    @Test
    public void testKotlin() throws Exception {
        // 获取Kotlin脚本
        File file = new File(ScriptTestCase.class.getResource("Model.kt").toURI());
        String script = FileUtils.readFileToString(file, StringUtility.CHARSET);

        // 设置Kotlin脚本使用到的Java类
        ScriptContext context = new ScriptContext();
        context.useClasses(Properties.class, Assert.class);
        context.useClass("Option", MapOption.class);
        context.useClasses("com.jstarcraft.ai.evaluate");
        context.useClasses("com.jstarcraft.rns.task");
        context.useClasses("com.jstarcraft.rns.model.benchmark");
        // 设置Kotlin脚本使用到的Java变量
        ScriptScope scope = new ScriptScope();
        scope.createAttribute("loader", loader);

        // 执行Kotlin脚本
        ScriptExpression expression = new KotlinExpression(context, scope, script);
        Map<String, Float> data = expression.doWith(Map.class);
        Assert.assertEquals(0.005825241096317768F, data.get("precision"), 0F);
        Assert.assertEquals(0.011579763144254684F, data.get("recall"), 0F);
        Assert.assertEquals(1.270874261856079F, data.get("mae"), 0F);
        Assert.assertEquals(2.425075054168701F, data.get("mse"), 0F);
    }

    /**
     * 使用Lua脚本与JStarCraft框架交互
     * 
     * <pre>
     * Java 11执行单元测试会抛<b>Unable to make {member} accessible: module {A} does not '{operation} {package}' to {B}</b>异常
     * 是由于Java 9模块化导致
     * 需要使用JVM参数:--add-exports java.base/jdk.internal.loader=ALL-UNNAMED
     * </pre>
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
        context.useClasses(Properties.class, Assert.class);
        context.useClass("Option", MapOption.class);
        context.useClasses("com.jstarcraft.ai.evaluate");
        context.useClasses("com.jstarcraft.rns.task");
        context.useClasses("com.jstarcraft.rns.model.benchmark");
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
        context.useClasses(Properties.class, Assert.class);
        context.useClass("Option", MapOption.class);
        context.useClasses("com.jstarcraft.ai.evaluate");
        context.useClasses("com.jstarcraft.rns.task");
        context.useClasses("com.jstarcraft.rns.model.benchmark");
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

    /**
     * 使用Ruby脚本与JStarCraft框架交互
     * 
     * @throws Exception
     */
    @Test
    public void testRuby() throws Exception {
        // 获取Ruby脚本
        File file = new File(ScriptTestCase.class.getResource("Model.rb").toURI());
        String script = FileUtils.readFileToString(file, StringUtility.CHARSET);

        // 设置Ruby脚本使用到的Java类
        ScriptContext context = new ScriptContext();
        context.useClasses(Properties.class, Assert.class);
        context.useClass("Option", MapOption.class);
        context.useClasses("com.jstarcraft.ai.evaluate");
        context.useClasses("com.jstarcraft.rns.task");
        context.useClasses("com.jstarcraft.rns.model.benchmark");
        // 设置Ruby脚本使用到的Java变量
        ScriptScope scope = new ScriptScope();
        scope.createAttribute("loader", loader);

        // 执行Ruby脚本
        ScriptExpression expression = new RubyExpression(context, scope, script);
        Map<String, Double> data = expression.doWith(Map.class);
        Assert.assertEquals(0.005825241096317768D, data.get("precision"), 0D);
        Assert.assertEquals(0.011579763144254684D, data.get("recall"), 0D);
        Assert.assertEquals(1.270874261856079D, data.get("mae"), 0D);
        Assert.assertEquals(2.425075054168701D, data.get("mse"), 0D);
    }

}
