package no.steria.spytest.serializer;

import org.junit.Test;

import java.util.Arrays;

import static org.fest.assertions.Assertions.assertThat;

public class SerializerTest {

    private final ClassSerializer serializer = new ClassSerializer();

    @Test
    public void shouldReturnClassNameForEmptyClass() throws Exception {
        assertThat(serializer.asString(new EmptyClass())).isEqualTo("<no.steria.spytest.serializer.EmptyClass>");
    }

    @Test
    public void shouldHandleNullObjects() throws Exception {
        assertThat(serializer.asString(null)).isEqualTo("<null>");
    }

    @Test
    public void shouldDeserialiseNull() throws Exception {
        assertThat(serializer.asObject(serializer.asString(null))).isNull();
    }

    @Test
    public void shouldReturnClass() throws Exception {
        EmptyClass emptyClass = (EmptyClass) serializer.asObject(serializer.asString(new EmptyClass()));
        assertThat(emptyClass.getClass()).isEqualTo(EmptyClass.class);
    }

    @Test
    public void shouldParseSimpleStringField() throws Exception {
        ClassWithSimpleFields simpleFields = new ClassWithSimpleFields().setStringval("pedro");
        assertThat(serializer.asString(simpleFields)).isEqualTo("<no.steria.spytest.serializer.ClassWithSimpleFields;stringval=pedro;intval=0>");
    }

    @Test
    public void sholdDeserializeObjectWithFields() throws Exception {
        ClassWithSimpleFields simpleFields = new ClassWithSimpleFields().setStringval("pedro").setIntval(42);
        ClassWithSimpleFields cloned = (ClassWithSimpleFields) serializer.asObject(serializer.asString(simpleFields));

        assertThat(cloned.getIntval()).isEqualTo(42);
        assertThat(cloned.getStringval()).isEqualTo("pedro");
    }

    @Test
    public void shouldHandleNullInFields() throws Exception {
        ClassWithSimpleFields simpleFields = new ClassWithSimpleFields().setStringval(null).setIntval(42);
        ClassWithSimpleFields cloned = (ClassWithSimpleFields) serializer.asObject(serializer.asString(simpleFields));

        assertThat(cloned.getStringval()).isNull();

    }

    @Test
    public void shouldHandleControlCharacters() throws Exception {
        ClassWithSimpleFields simpleFields = new ClassWithSimpleFields().setStringval("a&;<>bc");
        String serialized = serializer.asString(simpleFields);
        ClassWithSimpleFields cloned = (ClassWithSimpleFields) serializer.asObject(serialized);

        assertThat(serialized).isEqualTo("<no.steria.spytest.serializer.ClassWithSimpleFields;stringval=a&amp&semi&lt&gtbc;intval=0>");
        assertThat(cloned.getStringval()).isEqualTo("a&;<>bc");
    }

    @Test
    public void shouldHandleArraysAndCollections() throws Exception {
        String arrval[] = {"a","b","c"};
        ClassWithCollection classWithCollection = new ClassWithCollection().setArrVal(arrval);

        String serialized = serializer.asString(classWithCollection);

        ClassWithCollection cloned = (ClassWithCollection) serializer.asObject(serialized);

        assertThat(cloned.getArrVal()).containsOnly("a","b","c");
    }

    @Test
    public void shouldHandleClassWithOtherClassInside() throws Exception {
        ClassWithOtherClass classWithOtherClass = new ClassWithOtherClass().setMyProperty(Arrays.asList(new ClassWithSimpleFields().setIntval(72).setStringval("Anders"),
                new ClassWithSimpleFields().setIntval(17).setStringval("John F")));

        String serializedValue = serializer.asString(classWithOtherClass);
        System.out.println(serializedValue);
        ClassWithOtherClass duplicate = (ClassWithOtherClass) serializer.asObject(serializedValue);

        assertThat(duplicate.getMyProperty()).hasSize(2);
        ClassWithSimpleFields classWithSimpleFields = duplicate.getMyProperty().get(0);
        assertThat(classWithSimpleFields.getStringval()).isEqualTo("Anders");

    }

    @Test
    public void shouldHandleLangClasses() throws Exception {
        String serialized = serializer.asString("abc");
        assertThat(serialized).isEqualTo("<java.lang.String;abc>");

        String duplicate = (String) serializer.asObject(serialized);

        assertThat(duplicate).isEqualTo("abc");
    }
}
