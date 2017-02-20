/*
 * Copyright 2016, Stuart Douglas, and individual contributors as indicated
 * by the @authors tag.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.fakereplace.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class DescriptorUtils {

    public static Method getMethod(String name, String methodDesc, Class actual) {
        try {
            return actual.getMethod(name, argumentStringToClassArray(methodDesc, actual));
        } catch (Exception e) {
            // this should not happen
            throw new RuntimeException(e);
        }

    }

    public static String getDescriptor(Method method) {
        return "(" + classArrayToDescriptorString(method.getParameterTypes()) + ")" + classToStringRepresentation(method.getReturnType());
    }

    public static String getDescriptor(Constructor<?> method) {
        return "(" + classArrayToDescriptorString(method.getParameterTypes()) + ")V";
    }

    public static int maxLocalsFromParameters(String methodDescriptor) {
        String[] params = descriptorStringToParameterArray(methodDescriptor);
        int ret = params.length;
        for (String i : params) {
            if (i.equals("J") || i.equals("D")) {
                ++ret;
            }
        }
        return ret;
    }

    /**
     * returns an array of class types based on the method parameters this allows
     * getMethod to be called based on descriptor data We also pass the class
     * that the method We assume the descriptor is well formed
     */
    public static Class<?>[] argumentStringToClassArray(String methodDescriptor, Class<?> methodClass) throws ClassNotFoundException {
        int i = 1; // char 0 is a '('
        List<Class<?>> classes = new ArrayList<Class<?>>();
        int arraystart = -1;
        while (methodDescriptor.charAt(i) != ')') {
            Class<?> type = null;
            if (methodDescriptor.charAt(i) == '[') {
                if (arraystart == -1) {
                    arraystart = i;
                }
            } else {

                if (methodDescriptor.charAt(i) == 'L') {
                    i++;
                    int start = i;
                    while (methodDescriptor.charAt(i) != ';') {
                        ++i;
                    }
                    if (arraystart == -1) {
                        String className = methodDescriptor.substring(start, i).replace('/', '.');
                        try {
                            type = methodClass.getClassLoader().loadClass(className);
                        } catch (ClassNotFoundException e) {
                            type = Class.forName(className);
                        }
                    } else {
                        // apparently the array class syntax for
                        // getting an array class is the somewhat
                        // retarded Class.forName("[Ljava.lang.String;")
                        // a weird mix of internal and external formats
                        String className = methodDescriptor.substring(arraystart, i + 1).replace("/", ".");

                        try {
                            type = methodClass.getClassLoader().loadClass(className);
                        } catch (ClassNotFoundException e) {
                            type = Class.forName(className);
                        }
                    }

                } else {

                    if (arraystart == -1) {
                        type = primitiveType(methodDescriptor.charAt(i));
                    } else {
                        String className = methodDescriptor.substring(arraystart, i + 1);
                        try {
                            type = methodClass.getClassLoader().loadClass(className);
                        } catch (ClassNotFoundException e) {
                            type = Class.forName(className);
                        }
                    }

                }

                arraystart = -1;
                classes.add(type);
            }
            ++i;
        }
        Class<?>[] ret = new Class[classes.size()];
        for (i = 0; i < ret.length; ++i) {
            ret[i] = classes.get(i);
        }
        return ret;

    }

    /**
     * returns an array of String representations of the parameter types. Primitives are returned as their native
     * representations, while classes are returned in the form Ljava/lang/Integer
     * <p>
     * this is so the class I can be determined from an int for example
     */
    public static String[] descriptorStringToParameterArray(String methodDescriptor) {
        int i = 1; // char 0 is a '('
        List<String> ret = new ArrayList<String>();
        int arraystart = -1;
        while (methodDescriptor.charAt(i) != ')') {
            String type = null;
            if (methodDescriptor.charAt(i) == '[') {
                if (arraystart == -1) {
                    arraystart = i;
                }
            } else {
                if (methodDescriptor.charAt(i) == 'L') {
                    int start = i;
                    i++;
                    while (methodDescriptor.charAt(i) != ';') {
                        ++i;
                    }
                    if (arraystart == -1) {
                        type = methodDescriptor.substring(start, i);
                    } else {
                        // apparently the array class syntax for
                        // getting an array class is the somewhat
                        // retarded Class.forName("[Ljava.lang.String;")
                        // a weird mix of internal and external formats
                        type = methodDescriptor.substring(arraystart, i);
                    }
                } else {
                    if (arraystart == -1) {
                        type = methodDescriptor.charAt(i) + "";
                    } else {
                        type = methodDescriptor.substring(arraystart, i + 1);

                    }
                }
                arraystart = -1;
                ret.add(type);
            }
            ++i;
        }
        String[] r = new String[ret.size()];
        for (int j = 0; j < ret.size(); ++j) {
            r[j] = ret.get(j);
        }
        return r;

    }

    public static String methodSignitureToDescriptor(Class<?> returnType, Class<?>... params) {
        StringBuilder sb = new StringBuilder("(");
        sb.append(classArrayToDescriptorString(params));
        sb.append(")");
        sb.append(classToStringRepresentation(returnType));
        return sb.toString();
    }

    public static String getReturnType(String descriptor) {
        return descriptor.substring(descriptor.lastIndexOf(')') + 1);
    }

    public static String getReturnTypeInJvmFormat(String descriptor) {
        String rt = descriptor.substring(descriptor.lastIndexOf(')') + 1);
        if (rt.charAt(0) == 'L') {
            rt = rt.substring(1);
            rt = rt.substring(0, rt.length() - 1);
        }
        return rt;
    }

    /**
     * e.g. Ljava/lang/Object; to java/lang/Object
     */
    public static String getTypeStringFromDescriptorFormat(String descriptor) {
        descriptor = descriptor.substring(1);
        descriptor = descriptor.substring(0, descriptor.length() - 1);
        return descriptor;
    }

    public static String getArgumentString(String descriptor) {
        return descriptor.substring(0, descriptor.lastIndexOf(')') + 1);
    }

    public static String classArrayToDescriptorString(Class<?>... params) {
        if (params == null) {
            return "";
        }
        StringBuilder ret = new StringBuilder();
        for (Class<?> c : params) {
            ret.append(classToStringRepresentation(c));
        }
        return ret.toString();
    }

    public static String classToStringRepresentation(Class<?> c) {
        if (void.class.equals(c)) {
            return "V";
        } else if (byte.class.equals(c)) {
            return "B";
        } else if (char.class.equals(c)) {
            return "C";
        } else if (double.class.equals(c)) {
            return "D";
        } else if (float.class.equals(c)) {
            return "F";
        } else if (int.class.equals(c)) {
            return "I";
        } else if (long.class.equals(c)) {
            return "J";
        } else if (short.class.equals(c)) {
            return "S";
        } else if (boolean.class.equals(c)) {
            return "Z";
        } else if (c.isArray()) {
            return c.getName().replace(".", "/");
        } else {
            return extToInt(c.getName());
        }
    }

    static Class<?> primitiveType(char c) {
        switch (c) {
            case 'V':
                return void.class;

            case 'B':
                return byte.class;

            case 'C':
                return char.class;

            case 'D':
                return double.class;

            case 'F':
                return float.class;

            case 'I':
                return int.class;

            case 'J':
                return long.class;

            case 'S':
                return short.class;

            case 'Z':
                return boolean.class;

        }
        return null;
    }

    public static String extToInt(String className) {
        String repl = className.replace(".", "/");
        return 'L' + repl + ';';
    }

    public static boolean isPrimitive(String descriptor) {
        if (descriptor.length() == 1) {
            return true;
        }
        return false;
    }

    public static boolean isWide(String descriptor) {
        if (!isPrimitive(descriptor)) {
            return false;
        }
        char c = descriptor.charAt(0);
        if (c == 'D' || c == 'J') {
            return true;
        }
        return false;
    }
}
