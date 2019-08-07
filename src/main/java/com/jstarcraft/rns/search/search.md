# JStarCraft RNS Search教程

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

JStarCraft RNS Search是一款基于Lucene的检索工具,兼容JStarCraft ORM模块.
目标是**自动化管理**Lucene引擎,降低研发人员使用Lucene的难度.

****

## 特性

JStarCraft RNS Search具有如下特点:
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

    /** 需要索引的属性 */
    @SearchIndex
    private long index;

    /** 需要排序的属性 */
    @SearchSort
    private Instant sort;

    /** 需要存储的属性 */
    @SearchStore
    private Collection<String> store;

    /** 需要索引,排序,存储的属性 */
    @SearchIndex
    @SearchSort
    @SearchStore
    private float[] coordinate;

    /** 需要索引,排序,存储的属性 */
    @SearchIndex
    @SearchSort
    @SearchStore
    private Address address;

}

@Data
public class Address {

    /** 需要索引,排序,存储的属性 */
    @SearchIndex
    @SearchSort
    @SearchStore
    private String country;

    /** 需要索引,排序,存储的属性 */
    @SearchIndex
    @SearchSort
    @SearchStore
    private String province;

    /** 需要索引,排序,存储的属性 */
    @SearchIndex
    @SearchSort
    @SearchStore
    private String city;

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

将文档转换为对象

```java
Document document = ...
Mock object  = codec.decode(document);
```

#### 命名规则

在使用Lucene搜索时,需要了解JStarCraft RNS Search的命名规则.

JStarCraft RNS Search默认具有如下命名规则:
1. 名称支持多层嵌套,各层之间使用**`·`**分隔;
2. 存在4种形式的名称
    * `name`,可以用于任意类型属性;
    * `name[index]`,可以用于数组/集合/映射类型;
    * `name[index_key]`可以用于映射类型;
    * `name[index_value]`可以用于映射类型;

###### 索引命名规则

```java
Query query = IntPoint.newRangeQuery("coordinate", new float[] {-90F, -180F}, new float[] {90F, 180F});
```

###### 排序命名规则

```java
SortField country = new SortField("address.country", SortField.Type.STRING);
SortField city = new SortField("address.city", SortField.Type.STRING);
Sort sort = new Sort(country, city);
```

###### 存储命名规则

```java
Document document = ...
IndexableField latitude = document.getField("coordinate[0]");
IndexableField longitude = document.getField("coordinate[1]");
```

#### 自动更新

****

## 架构

****

## 概念

****

## 示例

[编解码示例](https://github.com/HongZhaoHua/jstarcraft-rns/blob/master/src/test/java/com/jstarcraft/rns/search/converter/ConverterTestCase.java)

[实时搜索示例](https://github.com/HongZhaoHua/jstarcraft-rns/blob/master/src/test/java/com/jstarcraft/rns/search/SearcherTestCase.java)

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