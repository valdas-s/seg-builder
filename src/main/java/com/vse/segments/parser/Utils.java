/*
 * This file is part of Segment-Builder.
 *
 * Segment-Builder is free software: you can redistribute it
 * and/or modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * Segment-Builder is distributed in the hope that it will be
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Segment-Builder. If not, see <http://www.gnu.org/licenses/>.
 */

package com.vse.segments.parser;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.MethodDescriptor;
import java.lang.reflect.Method;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

class Utils {

	public static Method findSetPropertyMethodForEnum(Class<?> c, String property,
			String enumValue) throws BuildException {
		try {
	
			String setterName = prepareIdentifier("set-" + property);
			MethodDescriptor md[] = Introspector.getBeanInfo(c).getMethodDescriptors();
			Method setter = null;
			for (int i = 0; i < md.length; i++) {
				Method m = md[i].getMethod();
	
				if (!m.getName().equals(setterName))
					continue;
	
				if (m.getParameterTypes().length != 1)
					continue;
				
				if (Enum.class.isAssignableFrom(m.getParameterTypes()[0])) {
					setter = m;
				}
			}
			
			if (setter == null) {
				return null;
			}
			
			Enum<?> value = resolveEnumValue(setter.getParameterTypes()[0], enumValue);
			return  (value != null) ? setter : null;
		} catch (IntrospectionException exc) {
			throw new BuildException(exc.toString(), exc);
		}
	}

	public static Enum<?> resolveEnumValue(Class<?> c, String value) {
		@SuppressWarnings({ "rawtypes", "unchecked" })
		Enum<?> e = Enum.valueOf((Class<Enum>)c, value);
		return e;
	}

	public static Method findSetPropertyMethodFor(Class<?> c, String property,
			Class<?> type) throws BuildException {
		try {

			List<Method> methods = new LinkedList<Method>();

			String setterName = prepareIdentifier("set-" + property);
			MethodDescriptor md[] = Introspector.getBeanInfo(c).getMethodDescriptors();
			for (int i = 0; i < md.length; i++) {
				Method m = md[i].getMethod();

				if (!m.getName().equals(setterName))
					continue;

				if (m.getParameterTypes().length != 1)
					continue;

				Class<?> expType = m.getParameterTypes()[0];
				if (expType.equals(type)
						|| unwrapsToPrimitiveTypeDirectly(type, expType))
					return m;

				methods.add(m);
			}

			for (int i = 0; i < methods.size(); i++) {
				Method m = methods.get(i);

				Class<?> expType = m.getParameterTypes()[0];
				if (expType.isAssignableFrom(type)
						|| unwrapsToPrimitiveType(type, expType))
					continue;

				methods.remove(m);
			}

			if (methods.size() > 1)
				throw new BuildException(
						"Ambiquity in setter methods for property " + property);

			if (methods.size() == 0)
				throw new BuildException("Don't know how to set property "
						+ property);

			return methods.size() > 0 ? methods.get(0) : null;

		} catch (IntrospectionException exc) {
			throw new BuildException(exc.toString(), exc);
		}
	}

	private static final Hashtable<Class<?>, Class<?>> primitiveMap = new Hashtable<Class<?>, Class<?>>();
	static {
		primitiveMap.put(Double.TYPE, Double.class);
		primitiveMap.put(Float.TYPE, Float.class);
		primitiveMap.put(Long.TYPE, Long.class);
		primitiveMap.put(Integer.TYPE, Integer.class);
		primitiveMap.put(Short.TYPE, Short.class);
		primitiveMap.put(Character.TYPE, Character.class);
		primitiveMap.put(Boolean.TYPE, Boolean.class);
	}

	public static boolean unwrapsToPrimitiveType(Class<?> from, Class<?> to) {
		if (from.isPrimitive())
			return false;
		if (!to.isPrimitive())
			return false;

		if (to.equals(Character.TYPE) && from.equals(String.class))
			return true;

		Class<?> wrapped = primitiveMap.get(to);
		return wrapped != null && wrapped.isAssignableFrom(from);
	}

	public static boolean unwrapsToPrimitiveTypeDirectly(Class<?> from, Class<?> to) {
		if (from.isPrimitive())
			return false;
		if (!to.isPrimitive())
			return false;

		Class<?> wrapped = primitiveMap.get(to);
		return wrapped != null && wrapped.equals(from);
	}

	public static String prepareIdentifier(String id) {
		StringBuffer s = new StringBuffer();
		StringTokenizer seg = new StringTokenizer(id, "-");
		while (seg.hasMoreTokens()) {
			String tok = seg.nextToken();
			if (s.length() != 0 || id.charAt(0) == '-')
				tok = tok.substring(0, 1).toUpperCase() + tok.substring(1);
			s.append(tok);
		}

		return s.toString();
	}
}
