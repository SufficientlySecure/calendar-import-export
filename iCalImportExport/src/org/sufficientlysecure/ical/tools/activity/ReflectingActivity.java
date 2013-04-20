/**
 *  Copyright (C) 2013  Dominik Schürmann <dominik@dominikschuermann.de>
 *  Copyright (C) 2010-2011  Lukas Aichbauer
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.sufficientlysecure.ical.tools.activity;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

/**
 * {@link ReflectingActivity} is an {@link Activity} which automatically stores it's attributes via
 * reflection. This is done by overwritting onRetainConfigurationChange(), for storing, and
 * onCreate() for loading saved attributes. This can be very usefull for automatically storing
 * values for orientation change.
 * 
 * @author lukas
 * 
 */
public class ReflectingActivity extends Activity {
    /**
     * These classes will not be automatically stored.
     */
    public static final Class<?>[] ignoredAttributeClasses = new Class<?>[] { View.class };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Object obj = getLastNonConfigurationInstance();
        if (obj != null) {
            Mapping[] mappings = (Mapping[]) obj;
            List<Field> fields = getValidFields();
            for (int i = 0; i < fields.size(); i++) {
                try {
                    if (mappings[i].getValue() != null) {
                        fields.get(i).set(this, mappings[i].getValue());
                    }
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        List<Field> fields = getValidFields();
        Mapping[] mappings = new Mapping[fields.size()];
        for (int i = 0; i < mappings.length; i++) {
            Field field = fields.get(i);
            try {
                mappings[i] = new Mapping(field.getName(), field.get(this));
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return mappings;
    }

    private List<Field> getValidFields() {
        List<Field> validFields = new ArrayList<Field>();
        Field[] fields = this.getClass().getDeclaredFields();
        for (Field field : fields) {
            boolean inValid = false;

            for (Class<?> cls : ignoredAttributeClasses) {
                if (cls.isAssignableFrom(field.getType())
                        || Modifier.isStatic(field.getModifiers())) {
                    inValid = true;
                    continue;
                }
            }
            if (!inValid) {
                field.setAccessible(true);
                validFields.add(field);
            }
        }
        return validFields;
    }
}
