# JStarCraft Search

****

## 目录

* [介绍](#介绍)
* [特性](#特性)
* [安装](#安装)
    * [Maven依赖](#Maven依赖)
    * [Gradle依赖](#Gradle依赖)
* [使用](#使用)
    * [自动编解码](#自动编解码)
    * [命名规则](#命名规则)
        * [索引命名规则](#索引命名规则)
        * [排序命名规则](#排序命名规则)
        * [保存命名规则](#保存命名规则)
    * [自动更新](#自动更新)
* [架构](#架构)
* [概念](#概念)
* [示例](#示例)
* [对比](#对比)
* [版本](#版本)
* [参考](#参考)
* [协议](#协议)
* [作者](#作者)
* [致谢](#致谢)

****

## 介绍

JStarCraft Search是一款基于Lucene的检索工具,兼容JStarCraft ORM模块.
目标是**自动化管理**Lucene引擎,降低研发人员使用Lucene的难度.

****

## 特性

JStarCraft Search具有如下特点:
1. 支持对象与文档的自动转换;
2. 能够根据配置同步或异步的自动更新;

****

## 安装

#### Maven依赖

```maven
<dependency>
    <groupId>com.jstarcraft</groupId>
    <artifactId>rns</artifactId>
    <version>1.0</version>
</dependency>
```

#### Gradle依赖

```gradle
compile group: 'com.jstarcraft', name: 'rns', version: '1.0'
```

****

## 使用

#### 自动编解码

在需要索引/排序/存储的属性声明对应的注解

```java
/** 使用Lombok自动构建getter/setter方法 */
@Data
public class Mock {

    /** 需要用于索引的属性 */
    @SearchIndex
    private long index;

    /** 需要用于排序的属性 */
    @SearchSort
    private Instant sort;
    
    /** 需要用于存储的属性 */
    @SearchStore
    private Collection<String> store;

}
```

构建编解码器

```java
SearchCodec<Mock, Mock> codec = new SearchCodec<>(Mock.class, Mock.class);
```

将对象转换为文档

```java
Mock object = ...
Document document = codec.encode(object);
```

将对象转换为文档

```java
Document document = ...
Mock object  = codec.decode(document);
```

#### 命名规则

JStarCraft Search具有如下命名规则:
1. 支持多层对象嵌套,各层属性之间使用**.**分隔;
2. 数组(Array)属性会投影为**name[index]**形式(name为属性名称,index为元素位置);
3. 集合(Collection)属性会投影为**name[index]**形式(name为属性名称,index为元素位置);
4. 映射(Map)属性会投影为**name[index_key]**和**name[index_value]**形式(name为属性名称,index为键值位置);

###### 索引命名规则

```java

```

###### 排序命名规则

```java

````

###### 保存命名规则

```java

````

#### 自动更新

****

## 架构

****

## 概念

****

## 示例

****

## 对比

****

## 版本

****

## 参考

****

## 协议

****

## 作者

****

## 致谢

****