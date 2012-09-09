package com.kw.opencms.module.mojo.util;

import java.lang.reflect.Field;
import java.util.Map;

/**
 *
 * @author joseangel
 *
 */
public class SimpleBeanLoader
{
	/**
	 * Try to load every member of the bean using the expression of the propery in dot notation with an optional prefix,
	 * to get the corresponding value in the map.
	 *
	 * @param bean the object to be filled up with the values
	 * @param values map with keys that are in dot notation and agrees with bean interface
	 * @param prefix string for getting the correct map keys
	 * @return the modified bean, null if access error happens
	 */
	public static Object load(Object bean, Map< String, Object > values, String prefix)
	{
		for(Field member : bean.getClass().getDeclaredFields())
		{
			String key = prefix != null
				? prefix.concat(".").concat(member.getName())
				: member.getName();

			if(values.containsKey(key))
			{
				try
				{
					member.setAccessible(true);
					member.set(bean, values.get(key));
				}
				catch (IllegalArgumentException e)
				{
					e.printStackTrace();
					return null;
				}
				catch (IllegalAccessException e)
				{
					e.printStackTrace();
					return null;
				}
			}
		}
		return bean;
	}
}
