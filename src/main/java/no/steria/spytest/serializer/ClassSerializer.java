package no.steria.spytest.serializer;

import org.joda.time.DateTime;
import org.joda.time.ReadableInstant;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ClassSerializer {
    private final DateTimeFormatter dateFormat = DateTimeFormat.forPattern("YYYYMMddHHmmssSSS");

    public String asString(Object object) {
        if (object == null) {
            return "<null>";
        }
        String classname = object.getClass().getName();
        String fieldsCode = computeFields(object);
        return "<" + classname + fieldsCode + ">";
    }

    private String computeFields(Object object) {
        Field[] declaredFields = object.getClass().getDeclaredFields();
        StringBuilder result = new StringBuilder();
        for (Field field : declaredFields) {
            result.append(";");
            result.append(field.getName());
            result.append("=");
            try {
                boolean access = field.isAccessible();
                if (!access) {
                    field.setAccessible(true);
                }
                Object fieldValue = field.get(object);
                String encodedValue = encodeValue(fieldValue);
                result.append(encodedValue);
                if (!access) {
                    field.setAccessible(false);
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        return result.toString();
    }



    public Object asObject(String serializedValue) {
        if ("<null>".equals(serializedValue)) {
            return null;
        }
        String[] parts = splitToParts(serializedValue);

        Object object = initObject(parts[0]);

        for (int i=1;i<parts.length;i++) {
            String[] fieldParts = parts[i].split("=");

            try {
                Field field = object.getClass().getDeclaredField(fieldParts[0]);

                setFieldValue(object, fieldParts[1], field);

            } catch (NoSuchFieldException | IllegalAccessError e) {
                throw new RuntimeException(e);
            }
        }

        return object;
    }

    protected String[] splitToParts(String serializedValue) {
        List<String> result = new ArrayList<>();

        int level = 0;
        int prevpos=0;
        for (int pos=0;pos<serializedValue.length();pos++) {
            Character c = serializedValue.charAt(pos);
            if (c == '<') {
                level++;
                if (level == 1) {
                    prevpos=pos+1;
                }
                continue;
            }
            if (c == '>') {
                level--;
                if (level == 0) {
                    result.add(serializedValue.substring(prevpos,pos));
                    prevpos=pos+1;
                }
                continue;
            }
            if (c == ';' && level == 1) {
                result.add(serializedValue.substring(prevpos,pos));
                prevpos=pos+1;
                continue;
            }
        }



        return result.toArray(new String[0]);
    }

    private void setFieldValue(Object object, String fieldValue, Field field) {
        Object value;
        value = objectValueFromString(fieldValue, field.getType());

        boolean access = field.isAccessible();
        if (!access) {
            field.setAccessible(true);
        }
        try {
            field.set(object, value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        if (!access) {
            field.setAccessible(false);
        }
    }

    private Object initObject(String classname) {
        try {
            return Class.forName(classname).newInstance();
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    protected Object objectValueFromString(String fieldValue, Class<?> type) {
        Object value;

        if ("&null".equals(fieldValue)) {
            value = null;
        } else if (fieldValue.startsWith("<")) {
            value = complexValueFromString(fieldValue,type);
        } else if (int.class.equals(type) || Integer.class.equals(type)) {
            value = Integer.parseInt(fieldValue);
        } else if (long.class.equals(type) || Long.class.equals(type)) {
            value = Long.parseLong(fieldValue);
        } else if (char.class.equals(type) || Character.class.equals(type)) {
            value = fieldValue.charAt(0);
        } else if (double.class.equals(type) || Double.class.equals(type)) {
            value = Double.parseDouble(fieldValue);
        } else if (Date.class.equals(type)) {
            value = dateFormat.parseLocalDateTime(fieldValue).toDate();
        } else if (DateTime.class.equals(type)) {
            value = dateFormat.parseDateTime(fieldValue);
        } else if (BigDecimal.class.equals(type)) {
            value = new BigDecimal(Double.parseDouble(fieldValue));
        } else {
            value = fieldValue.replaceAll("&amp","&").replaceAll("&semi",";").replaceAll("&lt","<").replaceAll("&gt",">");
        }
        return value;
    }

    private Object complexValueFromString(String fieldValue, Class<?> type) {
        String[] parts = splitToParts(fieldValue);
        if (!"array".equals(parts[0])) {
            throw new IllegalArgumentException("Not supported " + parts[0]);
        }
        Object arr = (Object[]) Array.newInstance(type.getComponentType(), parts.length - 1);

        for (int i=0;i<parts.length-1;i++) {
            Array.set(arr, i, objectValueFromString(parts[i + 1], type.getComponentType()));
        }

        return arr;
    }


    protected String encodeValue(Object fieldValue) {
        if (fieldValue instanceof Object[]) {
            Object[] arr=(Object[]) fieldValue;
            StringBuilder res = new StringBuilder("<array");
            for (Object objInArr : arr) {
                res.append(";");
                res.append(encodeValue(objInArr));
            }
            res.append(">");
            return res.toString();
        }
        if (fieldValue == null) {
            return "&null";
        }
        if (Date.class.equals(fieldValue.getClass())) {
            return dateFormat.print(new DateTime(fieldValue));
        }
        if (DateTime.class.equals(fieldValue.getClass())) {
            return dateFormat.print((ReadableInstant) fieldValue);
        }
        return fieldValue.toString().replaceAll("&","&amp").replaceAll(";","&semi").replaceAll("<","&lt").replaceAll(">","&gt");
    }

}
