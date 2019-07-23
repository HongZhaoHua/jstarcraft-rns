package com.jstarcraft.rns.search.converter;

import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedList;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import com.jstarcraft.rns.search.annotation.SearchIndex;
import com.jstarcraft.rns.search.annotation.SearchSort;
import com.jstarcraft.rns.search.annotation.SearchStore;

/**
 * 模仿复杂对象
 * 
 * @author Birdy
 *
 */
public class MockComplexObject {

    @SearchIndex
    @SearchSort
    @SearchStore
    private Integer id;

    @SearchIndex
    @SearchSort
    @SearchStore
    private String firstName;

    @SearchIndex
    @SearchSort
    @SearchStore
    private String lastName;

    @SearchStore
    private String[] names;

    @SearchIndex
    @SearchSort
    @SearchStore
    private int money;

    @SearchStore
    private int[] currencies;

    @SearchIndex
    @SearchSort
    @SearchStore
    private Instant instant;

    @SearchIndex
    @SearchSort
    @SearchStore
    private MockEnumeration race;

    @SearchStore
    private LinkedList<MockSimpleObject> list;

    @SearchStore
    private HashMap<Integer, MockSimpleObject> map;

    public MockComplexObject() {
    }

    public Integer getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public int getMoney() {
        return money;
    }

    public Instant getInstant() {
        return instant;
    }

    public MockEnumeration getRace() {
        return race;
    }

    public String[] toNames() {
        return names;
    }

    public int[] toCurrencies() {
        return currencies;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (object == null)
            return false;
        if (getClass() != object.getClass())
            return false;
        MockComplexObject that = (MockComplexObject) object;
        EqualsBuilder equal = new EqualsBuilder();
        equal.append(this.id, that.id);
        equal.append(this.firstName, that.firstName);
        equal.append(this.lastName, that.lastName);
        equal.append(this.names, that.names);
        equal.append(this.money, that.money);
        equal.append(this.currencies, that.currencies);
        equal.append(this.instant, that.instant);
        equal.append(this.race, that.race);
        return equal.isEquals();
    }

    @Override
    public int hashCode() {
        HashCodeBuilder hash = new HashCodeBuilder();
        hash.append(id);
        hash.append(firstName);
        hash.append(lastName);
        hash.append(names);
        hash.append(money);
        hash.append(currencies);
        hash.append(instant);
        hash.append(race);
        return hash.toHashCode();
    }

    @Override
    public String toString() {
        ToStringBuilder string = new ToStringBuilder(this);
        string.append(id);
        string.append(firstName);
        string.append(lastName);
        string.append(names);
        string.append(money);
        string.append(currencies);
        string.append(instant);
        string.append(race);
        return string.toString();
    }

    public static MockComplexObject instanceOf(Integer id, String firstName, String lastName, int money, Instant instant, MockEnumeration race) {
        MockComplexObject instance = new MockComplexObject();
        instance.id = id;
        instance.firstName = firstName;
        instance.lastName = lastName;
        instance.names = new String[] { firstName, lastName };
        instance.money = money;
        instance.currencies = new int[] { money };
        instance.instant = instant;
        instance.race = race;
        instance.list = new LinkedList<>();
        instance.map = new HashMap<>();
        for (int index = 0; index < money; index++) {
            MockSimpleObject object = MockSimpleObject.instanceOf(index, lastName);
            instance.list.add(object);
            instance.map.put(index, object);
        }
        return instance;
    }

}
